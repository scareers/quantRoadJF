package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import cn.hutool.core.lang.Console;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.SqlUtil;
import com.scareers.utils.Tqdm;
import com.scareers.utils.combinpermu.Generator;
import joinery.DataFrame;
import org.apache.commons.math3.util.MathUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.sqlapi.TushareApi.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/5  0005-15:13
 */
public class SingleKlineFormsBase {
    public static void main(String[] args) throws Exception {
        List<String> stocks = TushareApi.getStockListFromTushareExNotMain();
        stocks = stocks.subList(0, Math.min(SettingsOfSingleKlineBasePercent.stockAmountsBeCalc, stocks.size()));
        DataFrame<String> stockWithBoard = TushareApi.getStockListWithBoardFromTushare();

        List<List<String>> dateRanges = SettingsOfSingleKlineBasePercent.dateRanges;
        HashMap<String, List<List<String>>> stockWithStDateRanges = TushareApi.getStockWithStDateRanges();

        // 未关闭连接,可复用
        SqlUtil.execSql(SettingsOfSingleKlineBasePercent.sqlCreateSaveTable,
                SettingsOfSingleKlineBasePercent.ConnOfSaveTable, false);
        for (List<String> statDateRange : dateRanges) {
            Console.log("当前循环组: {}", statDateRange);
            // 不能关闭连接, 否则为 null, 引发空指针异常
            SqlUtil.execSql(
                    StrUtil.format(SettingsOfSingleKlineBasePercent.sqlDeleteExistDateRange,
                            StrUtil.format("('{}','{}')", statDateRange.get(0), statDateRange.get(1))),
                    SettingsOfSingleKlineBasePercent.ConnOfSaveTable, false);

            statsConclusionOfBatchFormsCommons(stocks, stockWithStDateRanges, stockWithBoard, statDateRange,
                    Arrays.asList(-0.05, 0.05), 82, Arrays.<Double>asList(-0.205, 0.205),
                    SettingsOfSingleKlineBasePercent.saveTablename);

//            MailUtil.send(SettingsCommon.receivers, StrUtil.format("部分解析完成: {}", statDateRange), "部分解析完成", false,
//                    null);
            break;
        }
//        MailUtil.send(SettingsCommon.receivers, "全部解析完成", "全部解析完成", false, null);
        Console.log("email success");
    }

    /**
     * main: 主逻辑函数, 单date_range 的分析.
     * 1.比起python的同方法, 少了 show_cdf_figure 参数显示图片
     * 2.线程池等待结束, 最优方案是使用 countDownLatch .await()
     * 3.原则: 定义时使用List, 实例化时使用ArrayList
     *
     * @param stocks
     * @param stockWithStDateRanges
     * @param statDateRange
     * @param bigChangeThreshold
     * @param bins
     * @param effectiveValueRange
     * @param saveTablename
     */
    public static void statsConclusionOfBatchFormsCommons(List<String> stocks,
                                                          HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                          DataFrame<String> stockWithBoard,
                                                          List<String> statDateRange, List<Double> bigChangeThreshold,
                                                          int bins, List<Double> effectiveValueRange,
                                                          String saveTablename) throws Exception {
        Console.log("构建结果字典");
        ConcurrentHashMap<String, List<Double>> results = new ConcurrentHashMap<>(8);
        ThreadPoolExecutor poolOfParse = new ThreadPoolExecutor(SettingsOfSingleKlineBasePercent.processAmount,
                SettingsOfSingleKlineBasePercent.processAmount * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        CountDownLatch latchOfParse = new CountDownLatch(stocks.size());
        Connection connOfParse = ConnectionFactory.getConnLocalTushare();
        AtomicInteger parseProcess = new AtomicInteger(0);
        for (String stock : Tqdm.tqdm(stocks, "process: ")) {
            Future<ConcurrentHashMap<String, List<Double>>> f = poolOfParse
                    .submit(new StockSingleParseTask(latchOfParse, stock, stockWithBoard, statDateRange,
                            stockWithStDateRanges, connOfParse)); // 全线程使用1个conn
            ConcurrentHashMap<String, List<Double>> resultTemp = f.get();
            for (String key : resultTemp.keySet()) {
                results.putIfAbsent(key, new ArrayList<>());
                results.get(key).addAll(resultTemp.get(key));
            }
            resultTemp = null;
            int value = parseProcess.incrementAndGet();
            if (value % 10 == SettingsOfSingleKlineBasePercent.gcControlEpoch) {
                System.gc();
            }
        }
        latchOfParse.await();
        poolOfParse.shutdown(); // 关闭线程池
        poolOfParse = null;
        connOfParse.close(); // 关闭唯一连接
        connOfParse = null;
        System.out.println();
        Console.log(results.size());
        Console.log(results.getClass().getName());
//        Console.log(results);

        Console.log("构建结果字典完成");
        Console.log("开始计算并保存");
        ArrayList<String> forNameRaws = new ArrayList<>(results.keySet());
        ThreadPoolExecutor poolOfCalc = new ThreadPoolExecutor(SettingsOfSingleKlineBasePercent.processAmount,
                SettingsOfSingleKlineBasePercent.processAmount * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        /**
         * 因java执行速度快, 因此考虑, 单个线程, 执行一个epoch; 而非 全进程执行一个epoch.
         * 且每次写入mysql为一个epoch的数据, 而非单条数据.
         */
        // 仅用于显示进度
        int totalEpochAmounts = (int) Math
                .ceil((double) results.size() / SettingsOfSingleKlineBasePercent.perEpochTaskAmounts);
        int process = 1;
        Connection connOfSave = SettingsOfSingleKlineBasePercent.ConnOfSaveTable;
        CountDownLatch latchOfCalcForEpoch = new CountDownLatch(totalEpochAmounts);
        for (int currentEpoch = 0; currentEpoch < Integer.MAX_VALUE; currentEpoch++) {
            int startIndex = currentEpoch * SettingsOfSingleKlineBasePercent.perEpochTaskAmounts;
            int endIndex = (currentEpoch + 1) * SettingsOfSingleKlineBasePercent.perEpochTaskAmounts;
            if (startIndex >= results.size()) {
                Console.log("计算并保存完成!");
                break;
            }

            List<String> formNamesCurrentEpoch = forNameRaws
                    .subList(startIndex, Math.min(endIndex, forNameRaws.size()));


            AtomicInteger processAnalyze = new AtomicInteger(0);//
            for (String formName : Tqdm.tqdm(formNamesCurrentEpoch,
                    StrUtil.format("process: {}/{}/{}", process, totalEpochAmounts, results.size()))) {
                try {
                    List<Double> resultSingle = results.get(formName);

                    Future<String> f = poolOfCalc.submit(new CalcStatResultAndSaveTask(latchOfCalcForEpoch,
                            connOfSave, formName,
                            stocks.size(), statDateRange, resultSingle, bigChangeThreshold, bins, effectiveValueRange,
                            saveTablename));
                    String finishedFormName = f.get();
                    results.remove(finishedFormName); // 删除key, 节省空间
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            latchOfCalcForEpoch.await(); // 本轮执行完毕
            process++;
            formNamesCurrentEpoch = null;
            //System.gc();
        }
        connOfSave.close();
        connOfSave = null;
    }
}

class CalcStatResultAndSaveTask implements Callable<String> {
    CountDownLatch latchOfCalcForEpoch;
    // 每轮计数, 多出来.  而少了图片显示参数
    Connection connOfSingleThread;

    String formName;
    int statStockCounts;
    List<String> statDateRange;
    List<Double> results; // 单形态的统计列表
    List<Double> bigChangeThreshold;
    int bins;
    List<Double> effectiveValueRange;
    String saveTablename;

    public CalcStatResultAndSaveTask(CountDownLatch latchOfCalcForEpoch, Connection connOfSingleThread,
                                     String formName, int statStockCounts,
                                     List<String> statDateRange, List<Double> singleResult,
                                     List<Double> bigChangeThreshold, int bins,
                                     List<Double> effectiveValueRange, String saveTablename) {
        this.latchOfCalcForEpoch = latchOfCalcForEpoch;
        this.connOfSingleThread = connOfSingleThread;

        this.formName = formName;
        this.statStockCounts = statStockCounts;
        this.statDateRange = statDateRange;
        this.results = singleResult;
        this.bigChangeThreshold = bigChangeThreshold;
        this.bins = bins;
        this.effectiveValueRange = effectiveValueRange;
        this.saveTablename = saveTablename;
    }

    @Override
    public String call() throws Exception {
        try {
            HashMap<String, Object> analyzeResultMap =
                    analyzeStatsResults(SettingsOfSingleKlineBasePercent.calcCdfAndFrequencyWithTick);
            // 精细分析也不需要保存 cdfwithtick. 过于冗余

            // 已经得到 分析结果, 需要注意 Map的Value 实际类别各不相同. 保存时需要一一对应
            int splitIndex = formName.lastIndexOf("__");
            String formNamePure = formName.substring(0, splitIndex);
            String statResultAlgorithm = formName.substring(splitIndex + 2);
            List<String> conditions = StrUtil.split(formNamePure, "__");
            String condition1 = null;
            String condition2 = null;
            String condition3 = null;
            String condition4 = null;
            String condition5 = null;
            String condition6 = null;
            String condition7 = null;

            for (String condition : conditions) {
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(0))) {
                    condition1 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(1))) {
                    condition2 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(2))) {
                    condition3 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(3))) {
                    condition4 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(4))) {
                    condition5 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(5))) {
                    condition6 = condition;
                }
                if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(6))) {
                    condition7 = condition;
                }
            }

            saveAnalyzeResult(analyzeResultMap, formNamePure, statDateRange, statResultAlgorithm, "",
                    connOfSingleThread,
                    saveTablename,
                    condition1, condition2,
                    condition3, condition4, condition5, condition6, condition7);
            return formName;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            latchOfCalcForEpoch.countDown();
            return formName;
        }
    }


    public static void saveAnalyzeResult(HashMap<String, Object> analyzeResultMap, String formNamePure,
                                         List<String> statDateRange, String statResultAlgorithm, String conditionsSet,
                                         Connection saveDb,
                                         String saveTablename,
                                         String condition1,
                                         String condition2,
                                         String condition3,
                                         String condition4,
                                         String condition5,
                                         String condition6,
                                         String condition7) throws SQLException {

        saveAnalyzeResult(analyzeResultMap, formNamePure, statDateRange, statResultAlgorithm, conditionsSet
                , saveDb, saveTablename, condition1, condition2, condition3, condition4, condition5, condition6,
                condition7,
                null, null, null, null,
                null); // 7条件的常态调用.

    }


    /**
     * * HashMap<String,Object> String为项目, Object为值   24个key
     * * <p>
     * * stat_stock_counts int
     * * total_counts int
     * * outliers_counts  int
     * * outliers_count_percent double
     * * effective_counts int
     * * effective_count_percent double
     * * zero_compare_counts ArrayList<Integer> 3
     * * zero_compare_counts_percent ArrayList<Double>  3
     * * bigchange_compare_counts  ArrayList<Integer> 3
     * * bigchange_compare_counts_percent  ArrayList<Double>  3
     * * min
     * * max
     * * std
     * * mean
     * * skew
     * * virtual_geometry_mean
     * * kurt    Double
     * * effective_value_range  List<Double>
     * * bins  int
     * * big_change_threshold  List<Double>
     * * tick_list List<Double>
     * * occurrences_list  List<Integer>
     * * frequency_list  ArrayList<Double>
     * * frequency_with_tick      List<List<Double>>
     * * cdf_list List<Double>
     * * cdf_with_tick    List<List<Double>>
     *
     * @param analyzeResultMap
     * @param formNamePure
     * @param statDateRange
     * @param statResultAlgorithm
     * @param conditionsSet
     * @param saveDb
     * @param saveTablename
     * @param condition1
     * @param condition2
     * @param condition3
     * @param condition4
     * @param condition5
     * @param condition6
     * @param condition7
     * @param condition8
     * @param condition9
     * @param condition10
     * @param selfNotes
     * @param formDescription
     */
    public static void saveAnalyzeResult(HashMap<String, Object> analyzeResultMap, String formNamePure,
                                         List<String> statDateRange, String statResultAlgorithm, String conditionsSet,
                                         Connection saveDb,
                                         String saveTablename,
                                         String condition1,
                                         String condition2,
                                         String condition3,
                                         String condition4,
                                         String condition5,
                                         String condition6,
                                         String condition7,
                                         String condition8,
                                         String condition9,
                                         String condition10,
                                         String selfNotes,
                                         String formDescription
    ) throws SQLException {
        if (analyzeResultMap == null) {
            return;
        }

        DataFrameSelf<Object> dfSaved = new DataFrameSelf<>();
        List<Object> row = new ArrayList<>();
//        Console.log(analyzeResultMap.keySet());
//        Console.log(analyzeResultMap);
        for (String key : analyzeResultMap.keySet()) {
            // zero_compare_counts ArrayList<Integer> 3
            // zero_compare_counts_percent ArrayList<Double>  3
            // bigchange_compare_counts  ArrayList<Integer> 3
            // bigchange_compare_counts_percent  ArrayList<Double>  3
            // 当key为以上四个, 则需要对字段进行 0,1,2 的拆分, 值也进行拆分. 总计分为12个字段. 方便数据库直接查询.
            if ("zero_compare_counts".equals(key) || "bigchange_compare_counts".equals(key)) {
                // 3 个Integer 的AL, 这里1拆为3; --> 0,1,2
                ArrayList<Integer> tempList = (ArrayList<Integer>) analyzeResultMap.get(key);
                for (int i = 0; i < 3; i++) {
                    String fieldName = key + "_" + i;
                    dfSaved.add(fieldName);
                    row.add(tempList.get(i));
                }
                continue;
            }
            // 同样为了应对数据表的字段 percnet..
            if ("zero_compare_counts_percent".equals(key) || "bigchange_compare_counts_percnet".equals(key)) {
                // 3 个Integer 的AL, 这里1拆为3; --> 0,1,2
                ArrayList<Double> tempList = (ArrayList<Double>) analyzeResultMap.get(key);
                for (int i = 0; i < 3; i++) {
                    String fieldName = key + "_" + i;
                    dfSaved.add(fieldName);
                    row.add(tempList.get(i));
                }
                continue;
            }

            dfSaved.add(key);
            Object value = analyzeResultMap.get(key);
            // Object 总体分为 ArrayList,List, List<List>, int, double 等.
            // 列表性结果, 则转换为 json, 字符串, int,double (包装类) 等不转换
            if (value instanceof List) {
                row.add(JSONUtil.toJsonStr(value));
            } else {
                // 此时 有可能为Double.NaN, 将sql错误, 修复一下
                if (value instanceof Double) {
                    // 注意 , 不能用 ==; 需要用 此静态方法, 判定是否为 NaN
                    if (Double.compare(Double.NaN, (Double) value) == 0) {
                        row.add(null); // 将NAN替换为null
                    } else {
                        row.add(value);  // @bugfix: 又特么是这里else忘了写
                    }
                } else {
                    row.add(value);
                }
            }
        }

        // 加入其他字段  // 列
        dfSaved.add("form_name");
        dfSaved.add("stat_date_range");
        dfSaved.add("stat_result_algorithm");
        dfSaved.add("self_notes");
        dfSaved.add("form_description");
        dfSaved.add("conditions_set");
        dfSaved.add("condition1");
        dfSaved.add("condition2");
        dfSaved.add("condition3");
        dfSaved.add("condition4");
        dfSaved.add("condition5");
        dfSaved.add("condition6");
        dfSaved.add("condition7");
        dfSaved.add("condition8");
        dfSaved.add("condition9");
        dfSaved.add("condition10");

        // 对应的 List<Object> 加入其他Object
        row.add(formNamePure);
        row.add(JSONUtil.toJsonStr(statDateRange)); // L<S>
        row.add(statResultAlgorithm);
        row.add(selfNotes);
        row.add(formDescription);
        row.add(conditionsSet);
        row.add(condition1);
        row.add(condition2);
        row.add(condition3);
        row.add(condition4);
        row.add(condition5);
        row.add(condition6);
        row.add(condition7);
        row.add(condition8);
        row.add(condition9);
        row.add(condition10);

        dfSaved.append(row); // 加入.
        dfSaved.toSql(saveTablename, saveDb, "append", null);
        // 连接未关闭
    }

    public static String objectToJsonStr(Object object) {
        return JSONUtil.toJsonStr(JSONUtil.parse(object));
    }

    /**
     * 对比python: 显示图像 和 保存图像的 参数未传递.
     *
     * @param calcCdfOrFrequencyWithTick
     * @return 分析结论, 所有字段及 类型:
     * <p>
     * HashMap<String,Object> String为项目, Object为值   24个key
     * <p>
     * stat_stock_counts int
     * total_counts int
     * outliers_counts  int
     * outliers_count_percent double
     * effective_counts int
     * effective_count_percent double
     * zero_compare_counts ArrayList<Integer> 3
     * zero_compare_counts_percent ArrayList<Double>  3
     * bigchange_compare_counts  ArrayList<Integer> 3
     * bigchange_compare_counts_percent  ArrayList<Double>  3
     * min
     * max
     * std
     * mean
     * skew
     * virtual_geometry_mean
     * kurt    Double
     * effective_value_range  List<Double>
     * bins  int
     * big_change_threshold  List<Double>
     * tick_list List<Double>
     * occurrences_list  List<Integer>
     * frequency_list  ArrayList<Double>
     * frequency_with_tick      List<List<Double>>
     * cdf_list List<Double>
     * cdf_with_tick    List<List<Double>>
     */
    public HashMap<String, Object> analyzeStatsResults(boolean calcCdfOrFrequencyWithTick) {
        List<Double> outliers = new ArrayList<>();
        List<Double> effectiveResults = new ArrayList<>();
        List<Integer> occurencesList = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            occurencesList.add(0); // 数量记录初始化
        }
        Double perRangeWidth = (effectiveValueRange.get(1) - effectiveValueRange.get(0)) / bins;
        List<Double> tickList = new ArrayList<>();
        for (int i = 0; i < bins + 1; i++) {// 初始化 tick列表.
            tickList.add(effectiveValueRange.get(0) + i * perRangeWidth);
        }
        for (int i = 0; i < results.size(); i++) {
            Double value = results.get(i);
            if (value < effectiveValueRange.get(0) || value > effectiveValueRange.get(1)) {
                outliers.add(value);
                continue;
            }
            effectiveResults.add(value);
            if (value.equals(effectiveValueRange.get(1))) { // 常规是前包后不包. 恰好等于最大限制的, 放在最后一个bin
                Integer count = occurencesList.get(bins - 1);
                occurencesList.set(bins - 1, count + 1);
                continue;
            }
            int index = (int) ((value - effectiveValueRange.get(0)) / perRangeWidth);
            Integer count = occurencesList.get(index);
            occurencesList.set(index, count + 1);
        }

        if (effectiveResults.size() <= 0) {
            return null; // 没有有效统计数值, 返回null, 调用方注意判断
        }

        HashMap<String, Object> conclusion = new HashMap<>();
        conclusion.put("stat_stock_counts", statStockCounts); // int
        conclusion.put("total_counts", results.size()); // int
        conclusion.put("outliers_counts", outliers.size()); // int
        conclusion.put("outliers_count_percent",
                (double) (int) conclusion.get("outliers_counts") / (int) conclusion.get(
                        "total_counts")); // double
        conclusion.put("effective_counts", occurencesList.stream().mapToInt(Integer::intValue).sum()); // int
        conclusion.put("effective_count_percent",
                (double) (int) conclusion.get("effective_counts") / (int) conclusion.get(
                        "total_counts")); // double

        ArrayList<Integer> zeroCompareCounts = new ArrayList<>();
        int ltZero = 0, eqZero = 0, gtZero = 0;
        for (Double i : effectiveResults) {
            if (i < 0.0) {
                ltZero++;
            } else if (i == 0.0) {
                eqZero++;
            } else {
                gtZero++;
            }
        }
        ArrayList<Double> zeroCompareCountsPercent = getDoubles(effectiveResults, conclusion, zeroCompareCounts, ltZero,
                eqZero,
                gtZero, "zero_compare_counts");
        conclusion.put("zero_compare_counts_percent", zeroCompareCountsPercent); //AL

        ArrayList<Integer> bigchangeCompareCounts = new ArrayList<>();
        int ltBigchange = 0, betweenBigchange = 0, gtBigchange = 0;
        for (Double i : effectiveResults) {
            if (i <= bigChangeThreshold.get(0)) {
                ltBigchange++;
            } else if (i >= bigChangeThreshold.get(1)) {
                gtBigchange++;
            } else {
                betweenBigchange++;
            }
        }
        ArrayList<Double> bigchangeCompareCountsPercent = getDoubles(effectiveResults, conclusion,
                bigchangeCompareCounts, ltBigchange, betweenBigchange, gtBigchange,
                "bigchange_compare_counts");
        // @noti: 这里故意拼写错误 percent成 percnet, 为了数据表字段将错就错
        conclusion.put("bigchange_compare_counts_percnet", bigchangeCompareCountsPercent); //AL

        DataFrame<Double> dfEffectiveResults = new DataFrame<>();
        dfEffectiveResults.add("value", effectiveResults);
        conclusion.put("mean", dfEffectiveResults.mean().get(0, 0));
//        conclusion.put("mean", 1);
        conclusion.put("std", dfEffectiveResults.stddev().get(0, 0));
        conclusion.put("min", dfEffectiveResults.min().get(0, 0));
        conclusion.put("max", dfEffectiveResults.max().get(0, 0));
        conclusion.put("skew", dfEffectiveResults.skew().get(0, 0));

        conclusion.put("kurt", dfEffectiveResults.kurt().get(0, 0));
//        conclusion.put("kurt", 1);

        // Double
        conclusion.put("virtual_geometry_mean", calcVirtualGeometryMeanRecursion(effectiveResults, 100, 1000));

//        conclusion_base_stat['virtual_geometry_mean'] = calc_virtual_geometry_mean_recursion(effective_results,
//                parts = 100,
//                single_max_lenth = 1000)

        conclusion.put("effective_value_range", effectiveValueRange);
        conclusion.put("bins", bins);
        conclusion.put("big_change_threshold", bigChangeThreshold);

        conclusion.put("tick_list", tickList);
        conclusion.put("occurrences_list", occurencesList);
        ArrayList<Double> frequencyList = new ArrayList<>();
        for (Integer i : occurencesList) {
            frequencyList.add((double) i / effectiveResults.size());
        }
        conclusion.put("frequency_list", frequencyList);
        // 不使用null, 因frequency_with_tick 将被转换为json字符串保存, 这里直接设定为 "",
        // 如果使用null, 保存时会报错: No value specified for parameter 6
        conclusion.put("frequency_with_tick", "");
        if (calcCdfOrFrequencyWithTick) {
            List<List<Double>> frequencyWithTick = getListWithTick(tickList, frequencyList);
            conclusion.put("frequency_with_tick", frequencyWithTick);
        }

        DataFrame<Double> dfTemp = new DataFrame<>();
        dfTemp.append(frequencyList);
        dfTemp = dfTemp.cumsum();
        List<Double> cdfList = dfTemp.col(0);
        conclusion.put("cdf_list", cdfList);
        conclusion.put("cdf_with_tick", "");
        if (calcCdfOrFrequencyWithTick) {
            List<List<Double>> cdfWithTick = getListWithTick(tickList, cdfList);
            conclusion.put("cdf_with_tick", cdfWithTick);
        }


        return conclusion;
    }

    /**
     * 见上一方法的两次调用. 本方法为 idea 提取的方法
     *
     * @param effectiveResults
     * @param conclusion
     * @param bigchangeCompareCounts
     * @param ltBigchange
     * @param betweenBigchange
     * @param gtBigchange
     * @param bigchange_compare_counts
     * @return
     */
    private ArrayList<Double> getDoubles(List<Double> effectiveResults, HashMap<String, Object> conclusion,
                                         ArrayList<Integer> bigchangeCompareCounts, int ltBigchange,
                                         int betweenBigchange, int gtBigchange, String bigchange_compare_counts) {
        bigchangeCompareCounts.add(ltBigchange);
        bigchangeCompareCounts.add(betweenBigchange);
        bigchangeCompareCounts.add(gtBigchange);
        conclusion.put(bigchange_compare_counts, bigchangeCompareCounts); //AL
        ArrayList<Double> bigchangeCompareCountsPercent = new ArrayList<>();
        for (Integer i : bigchangeCompareCounts) {
            bigchangeCompareCountsPercent.add((double) i / effectiveResults.size());
        }
        return bigchangeCompareCountsPercent;
    }

    private List<List<Double>> getListWithTick(List<Double> tickList, List<Double> cdfList) {
        List<List<Double>> cdfWithTick = new ArrayList<>();
        for (int i = 0; i < cdfList.size(); i++) {
            Double cdf = cdfList.get(i);
            Double tick = tickList.get(i + 1);
            cdfWithTick.add(Arrays.<Double>asList(cdf, tick));
        }
        return cdfWithTick;
    }

    /**
     * @param effectiveResults
     * @param splitParts
     * @param singleMaxLenth
     * @return noti: 对于lenth过长的, 当分拆时, 分拆为100部分, 的最后一部分, 可能数据量并没有前面99部分那么多, 这里做了一个近似
     * :param effective_results: 需要计算虚拟 几何日平均收益率 的列表;  分拆计算后, 结果维持不变.*********
     * :param parts: 如果列表过大, 大于 single_max_lenth, 则进行分拆递归计算, 分拆为 parts 部分, 当然, 也可能分拆的部分也过大了
     * :param single_max_lenth: 直接计算时的 最大长度.  他两没有必然关系
     */
    public static Double calcVirtualGeometryMeanRecursion(List<Double> effectiveResults, int splitParts,
                                                          int singleMaxLenth) {
        Double res;
        if (effectiveResults.size() == 0) {
            return null;
        }
        if (effectiveResults.size() <= singleMaxLenth) {
            res = geometryMean(effectiveResults);
        } else {
            // batchApproximateValues 最多100个数字. 单个部分 由递归计算而来.

            List<Double> batchApproximateValues = new ArrayList<>();
            int batchCounts = (int) Math.ceil((double) effectiveResults.size() / splitParts);
            for (int i = 0; i < splitParts; i++) {
                if (i * batchCounts >= effectiveResults.size()) { // 强制判定
                    continue;
                }
                // @bugfix: (i+1)*batchCounts 而非 i*(batchCounts+1)
                List<Double> batchResults = effectiveResults.subList(i * batchCounts, Math.min((i + 1) * batchCounts,
                        effectiveResults.size()));
                Double approximateSingleValue = calcVirtualGeometryMeanRecursion(batchResults, splitParts,
                        singleMaxLenth);
                if (approximateSingleValue != null) {
                    batchApproximateValues.add(approximateSingleValue);
                }
            }
            res = geometryMean(batchApproximateValues);
        }
        return res;
    }

    private static Double geometryMean(List<Double> effectiveResults) {
        Double res;
        DataFrame<Double> dfValues = new DataFrame<>();
        dfValues.add(effectiveResults);
        dfValues = dfValues.apply(value -> {
            return value + 1;
        });
        Double prod = dfValues.prod().get(0, 0);
        res = Math.pow(prod, (double) 1 / effectiveResults.size()) - 1;
        return res;
    }


}

/**
 * -- 面向对象与面向过程
 * 对应了python中的, 对单只股票进行解析的函数. java需要实现成类. 函数的参数, 则经过实例化对象时, 通过属性进行传递.
 * run() 方法才能直接访问这些属性, 相当于读取了函数参数
 * 该函数使用了 4个参数.
 * -- 与python对称
 * 原python代码, 单个线程返回值是 dict, 将多个dict, 汇总extend到 大的result;
 * 调用端, 大的 result使用 ConcurrentHashMap, 而 各个线程, 则将自身结果extend到Map;
 * https://blog.csdn.net/huyaowei789/article/details/102811729 参考.
 * 这里, 我们同样, 将 CountDownLatch计数器, 和 汇总的 result 的大Map, 作为参数传递!. 因此作为类的属性.
 * -- 实现接口与线程池调用
 * 如果使用单个线程返回值, 主线程拼接的方式, 则实现Callable<Object>, 泛型可以指定为准确的返回值, 方便future.get()
 * 如果单线程自行添加部分结果到 总Result, 则实现Runnable
 * 且汇总方式调用pool.execute即可, 而 返回值方式, 使用 pool.submit(), 它返回Future
 * -- run()实现注意
 * 需要 try{主要过程}, finally {latch.countDown()} 即保证绝对调用计数器-1, 使得即使某个线程异常, 也能阻塞结束,主线程能够继续运行
 * -- tqdm 显示进度
 * 则需要 单个线程返回值的方式. Future f = pool.submit(Callable); f.get() 转型
 */
class StockSingleParseTask implements Callable<ConcurrentHashMap<String, List<Double>>> {
    public static List<String> fieldsOfDfRaw = SettingsOfSingleKlineBasePercent.fieldsOfDfRaw;
    public static Class[] fieldsOfDfRawClass = SettingsOfSingleKlineBasePercent.fieldsOfDfRawClass;
    public static final List<String> conditionNames = SettingsOfSingleKlineBasePercent.conditionNames;
    public static final List<Double> upperShadowRangeList = SettingsOfSingleKlineBasePercent.upperShadowRangeList;
    public static final List<Double> lowerShadowRangeList = SettingsOfSingleKlineBasePercent.lowerShadowRangeList;
    public static final List<Double> entityRangeList = SettingsOfSingleKlineBasePercent.entityRangeList;
    public static final List<Double> todayOpenRangeList = SettingsOfSingleKlineBasePercent.todayOpenRangeList;
    public static final List<Double> pre5dayPercentRangeList = SettingsOfSingleKlineBasePercent.pre5dayPercentRangeList;
    public static final List<Double> volToPre5dayAvgRangeList =
            SettingsOfSingleKlineBasePercent.volToPre5dayAvgRangeList;

    Connection conn;

    //为了不大量重构代码, 本参数作为 stockWithBoard 字典版的缓存. 在很久之后才计算一次, 且线程安全, 类似于单例
    public static ConcurrentHashMap<String, String> stockWithBoardAsDict = null;
    // 读取静态属性省一下代码长度.

    // 以下2属性, 为java于python主要不同. 这里并没有使用 Future 对单线程返回值做操作. python也可以这样写. 但cdl是java独有
    CountDownLatch countDownLatch;
    String stock;
    List<String> statDateRange;
    DataFrame<String> stockWithBoard;
    HashMap<String, List<List<String>>> stockWithStDateRanges;


    public StockSingleParseTask() {
    }

    public StockSingleParseTask(CountDownLatch countDownLatch,
                                String stock,
                                DataFrame<String> stockWithBoard, List<String> statDateRange,
                                HashMap<String, List<List<String>>> stockWithStDateRanges, Connection conn) {
        this.countDownLatch = countDownLatch;
        this.stock = stock;
        this.stockWithBoard = stockWithBoard;
        this.statDateRange = statDateRange;
        this.stockWithStDateRanges = stockWithStDateRanges;
        this.conn = conn;
    }

    @Override
    public ConcurrentHashMap<String, List<Double>> call() {
        // 实际逻辑显然对应了python 的parse_single_stock() 函数
        // 使得不会返回null. 至少会返回空的字典
        ConcurrentHashMap<String, List<Double>> resultSingle = new ConcurrentHashMap<>(2 ^ 5);
        try {
            /*
            //模拟主要逻辑
            //注意这里线程池9线程, 如果以线程名作为key, 则最多9个key.没有bug. latch.count也是
            if (Thread.currentThread().getName().contains("pool-1-thread-9")) {
                Thread.sleep(30);
            }
            Thread.sleep((long) (Math.random() * 10));
            Thread.sleep(1);
            resultSingle.put("Test" + Thread.currentThread().getName(), Arrays.<Double>asList(1.0, 2.0));
             */

            // 开始主要逻辑
            // 添加结果到 线程安全的 总结果集
            List<String> statDateRangeFull = CommonUtils.changeStatRangeForFull(statDateRange);
            // 单个线程用一个 conn 对象, 用完close(), 否则线程池容量不够

            // 连接未关闭, 传递了 conn. 若不传递, 则临时从池子获取.
            DataFrame<Object> dfRaw = getStockPriceByTscodeAndDaterangeAsDfFromTushare(stock, "nofq",
                    SettingsOfSingleKlineBasePercent.fieldsOfDfRaw,
                    statDateRangeFull, conn);
            dfRaw = dfRaw.dropna();
            // 新知识: java不定参数等价于 数组.而非List
            dfRaw.convert(fieldsOfDfRawClass);
            HashSet<String> adjDates = getAdjdatesByTscodeFromTushare(stock, conn);
            resultSingle = baseFormAndOpenConditionAnalyzer(dfRaw, adjDates, stock, stockWithBoard,
                    stockWithStDateRanges, statDateRange,
                    conn); // 注意: dfRaw依据fulldates获取, 而这里要传递统计区间日期
            return resultSingle;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 必须计数减1, 使得主线程能够结束阻塞
            countDownLatch.countDown();
            return resultSingle;
        }
    }

    /**
     * 对应python 的 BaseFormAndOpenConditionAnalyzer 函数
     * 因为conn一直没有关闭, 这里传递conn对象. 直到单只股票分析完, 再关闭
     *
     * @param dfRaw
     * @param adjDates
     * @param stock
     * @param stockWithBoard
     * @param stockWithStDateRanges
     * @param statDateRange:        已经是 statDateRangeFull.
     * @return
     */
    public static final ConcurrentHashMap<String, List<Double>> baseFormAndOpenConditionAnalyzer(
            DataFrame<Object> dfRaw, HashSet<String> adjDates,
            String stock,
            DataFrame<String> stockWithBoard,
            HashMap<String, List<List<String>>> stockWithStDateRanges,
            List<String> statDateRange,
            Connection conn) {

        ConcurrentHashMap<String, List<Double>> resultTemp = new ConcurrentHashMap<>(2);
        if (dfRaw.length() < SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg) {
            return resultTemp;
        }
        for (int i = 4; i < dfRaw.length() - SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg; i++) {
            try {

                DataFrame<Object> dfWindowRaw = dfRaw.slice(i - 4,
                        i - 4 + SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg);
                String todayTemp = (String) dfWindowRaw
                        .get(5, fieldsOfDfRaw.indexOf("trade_date"));
                if (todayTemp.compareTo(statDateRange.get(0)) < 0 || todayTemp.compareTo(statDateRange.get(1)) >= 0) {
                    // 前包后不包
                    continue;
                }
//                Console.log(todayTemp); 到此处同, 是遍历了相同的todayTemp的
                DataFrame<Object> dfWindow;
                if (hasIntersectionBetweenAdjDatesAndDfWindow(adjDates, dfWindowRaw)) {
                    List<Object> colTradeDates = dfWindowRaw.col(fieldsOfDfRaw.indexOf("trade_date"));
                    List<String> dateRangeEqual = Arrays.asList((String) colTradeDates.get(0),
                            (String) colTradeDates.get(colTradeDates.size() - 1));
//                    Console.log(dateRangeEqual);
                    // @bugfix: 当获取等价后复权时,日期应当包含尾; 而原sqlApi实现包头不包尾.
                    // 不读取后复权sql时,不会影响,因此出现这种情况;  而python的sql是现写的, 没出现bug. :  < --> <=
                    // @bugfix: 已重构, 参数 excludeEndDate, 可以选择 设定. 此处设置 false, 则包尾
                    dfWindow = TushareApi.getStockPriceByTscodeAndDaterangeAsDfFromTushare0(stock, "hfq", fieldsOfDfRaw,
                            dateRangeEqual, conn, false);
                    dfWindow.convert(fieldsOfDfRawClass);
                } else {
                    dfWindow = dfWindowRaw;
                }
                dfWindow = dfWindow.dropna();
                //@bugfix: 注意是 length, 而非size()
                if (dfWindow.length() != SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg) {
//                    Console.log(todayTemp);
//                    Console.log(dfWindow);
//                    Console.log(dfWindowRaw);
                    continue; // 后复权数据,有所缺失
                }

                //                Console.log(todayTemp); 到此处同, 是遍历了相同的todayTemp的
                // 到这里不同了.  395 - 417  // 结果上 391 - 413 ; 恰好差4.       22天差距
//                Console.log(todayTemp);
                List<Object> pre5dayKlineRow = dfWindow.row(0);
                List<Object> yesterdayKlineRow = dfWindow.row(4);
                List<Object> todayKlineRow = dfWindow.row(5);
                List<Object> resultAccordingKlineRow =
                        dfWindow.row(SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 1);
                List<String> concreteTodayFormStrs = parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                        yesterdayKlineRow, todayKlineRow, stockWithStDateRanges, stockWithBoard);
//                System.out.println(concreteTodayFormStrs);
//                System.out.println(stock);
//                System.out.println(dfWindow);
//                System.exit(0);

                // 7条件判定完成 *******
                if (concreteTodayFormStrs.contains("-")) {
                    continue;
                }
                // 四个结果值
                Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
                Double singleResultAccordingNextOpen =
                        getPriceOfSingleKline(resultAccordingKlineRow, "open") / todayClose - 1;
                Double singleResultAccordingNextClose =
                        getPriceOfSingleKline(resultAccordingKlineRow, "close") / todayClose - 1;
                Double singleResultAccordingNextHigh =
                        getPriceOfSingleKline(resultAccordingKlineRow, "high") / todayClose - 1;
                Double singleResultAccordingNextLow =
                        getPriceOfSingleKline(resultAccordingKlineRow, "low") / todayClose - 1;

                ArrayList<List<Integer>> allIndexCombinations = new ArrayList<>();
                for (int j = 0; j < 7 + 1; j++) {// 单组合取 0,1,2,3,4,5,6,7 个; 8种
                    Generator.combination(0, 1, 2, 3, 4, 5, 6) // 全部可取索引, 7个,固定的
                            .simple(j)
                            .stream()
                            .forEach(allIndexCombinations::add);
                }
                //                Console.log(allIndexCombinations.size()); // 全索引组合128种
                String prefix = "Next" + (SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7);

                for (int j = 0; j < allIndexCombinations.size(); j++) {
                    List<Integer> singleCombination = allIndexCombinations.get(j);
                    String keyTemp = "";
                    for (int k = 0; k < singleCombination.size(); k++) {
                        // @bugfix: concreteTodayFormStrs.get(singleCombination.get(k) 写错了, singleCombination.get(k)
                        keyTemp += (concreteTodayFormStrs.get(singleCombination.get(k)) + "__");
                    }
                    if ("".equals(keyTemp)) {
                        keyTemp = "Dummy__";
                    }
//                    Console.log(keyTemp);
                    resultTemp.putIfAbsent(keyTemp + prefix + "Open", new ArrayList<>());
                    resultTemp.get(keyTemp + prefix + "Open").add(singleResultAccordingNextOpen);
                    resultTemp.putIfAbsent(keyTemp + prefix + "Close", new ArrayList<>());
                    resultTemp.get(keyTemp + prefix + "Close").add(singleResultAccordingNextClose);
                    resultTemp.putIfAbsent(keyTemp + prefix + "High", new ArrayList<>());
                    resultTemp.get(keyTemp + prefix + "High").add(singleResultAccordingNextHigh);
                    resultTemp.putIfAbsent(keyTemp + prefix + "Low", new ArrayList<>());
                    resultTemp.get(keyTemp + prefix + "Low").add(singleResultAccordingNextLow);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 打印此时的 dfwindow 前3行
                Console.log("发生了异常, dfWindow: ");
                Console.log(dfRaw.slice(i, i + 3));
            }
        }
        return resultTemp;
    }

    public static List<String> parseConditionsAsStrs(String stock, DataFrame<Object> dfWindow,
                                                     List<Object> pre5dayKlineRow,
                                                     List<Object> yesterdayKlineRow,
                                                     List<Object> todayKlineRow,
                                                     HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                     DataFrame<String> stockWithBoard) throws SQLException {
        return parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow, yesterdayKlineRow, todayKlineRow,
                stockWithStDateRanges, stockWithBoard, false, false);
    }

    /**
     * @param stock
     * @param dfWindow
     * @param pre5dayKlineRow
     * @param yesterdayKlineRow
     * @param todayKlineRow
     * @param stockWithStDateRanges
     * @param stockWithBoard
     * @param simpleMode            简单模式,使用 涨跌停限制计算今日是否涨停? 复杂模式: 读取数据库的涨跌停价格判定(但2008年前数据没有),则只能简单模式
     * @param skipPriceLimit        是否计算 当日是否涨停 的条件; 常态不会跳过.  某些特殊情况, 需要实时调用时,可以强制跳过, 则计算更快
     * @return 正式分析情况下, 以下两个flag均为false, 已经重载方法.
     */
    public static List<String> parseConditionsAsStrs(String stock, DataFrame<Object> dfWindow,
                                                     List<Object> pre5dayKlineRow,
                                                     List<Object> yesterdayKlineRow,
                                                     List<Object> todayKlineRow,
                                                     HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                     DataFrame<String> stockWithBoard,
                                                     boolean simpleMode,
                                                     boolean skipPriceLimit) throws SQLException {

        // 简单访问项目
        Double todayOpen = getPriceOfSingleKline(todayKlineRow, "open");
        Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
        Double todayHigh = getPriceOfSingleKline(todayKlineRow, "high");
        Double todayLow = getPriceOfSingleKline(todayKlineRow, "low");
        Double todayVol = getPriceOfSingleKline(todayKlineRow, "vol");
        Double yesterdayClose = getPriceOfSingleKline(yesterdayKlineRow, "close");

        Double upperPoint = Math.max(todayOpen, todayClose);
        Double lowerPoint = Math.min(todayOpen, todayClose);
        // 判定结果为 7条件.
        List<String> conditionsCalced = Arrays.asList("-", "-", "-", "-", "-", "-", "-");
        // 1.开盘涨跌百分比判定
        Double todayOpenPercent = todayOpen / yesterdayClose - 1;
        List<String> todayOpenPercentConditionList = getConditionNamesForNodeList(todayOpenRangeList,
                conditionNames.get(0));
        for (int i = 0; i < todayOpenPercentConditionList.size() - 1; i++) {
            if (todayOpenPercent < todayOpenRangeList.get(i + 1) && todayOpenPercent >= todayOpenRangeList.get(i)) {
                conditionsCalced.set(0, todayOpenPercentConditionList.get(i));
                break;
            }
        }

        // 2.实体判定
        Double entity_ = (todayClose - todayOpen) / yesterdayClose;
        List<String> entityConditionList = getConditionNamesForNodeList(entityRangeList, conditionNames.get(1));
        for (int i = 0; i < entityConditionList.size() - 1; i++) {
            if (entity_ < entityRangeList.get(i + 1) && entity_ >= entityRangeList.get(i)) {
                conditionsCalced.set(1, entityConditionList.get(i));
                break;
            }
        }
        // 3.上影线判定
        Double upperShadow = (todayHigh - upperPoint) / yesterdayClose;
        List<String> upperShadowConditionList = getConditionNamesForNodeList(upperShadowRangeList,
                conditionNames.get(2));
        for (int i = 0; i < upperShadowConditionList.size() - 1; i++) {
            if (upperShadow < upperShadowRangeList.get(i + 1) && upperShadow >= upperShadowRangeList.get(i)) {
                conditionsCalced.set(2, upperShadowConditionList.get(i));
                break;
            }
        }
        // 4.下影线判定
        Double lowerShadow = (lowerPoint - todayLow) / yesterdayClose;
        List<String> lowerShadowConditionList = getConditionNamesForNodeList(lowerShadowRangeList,
                conditionNames.get(3));
        for (int i = 0; i < lowerShadowConditionList.size() - 1; i++) {
            if (lowerShadow < lowerShadowRangeList.get(i + 1) && lowerShadow >= lowerShadowRangeList.get(i)) {
                conditionsCalced.set(3, lowerShadowConditionList.get(i));
                break;
            }
        }
        // 5. 涨跌停判定
        if (!skipPriceLimit) {
            boolean[] closeReachPriceLimit = isReachPriceLimit(stock, yesterdayKlineRow, todayKlineRow,
                    stockWithStDateRanges, stockWithBoard, simpleMode);
            boolean closeReachPriceLimitMax = closeReachPriceLimit[0];
            boolean closeReachPriceLimitMin = closeReachPriceLimit[1];
            if (closeReachPriceLimitMax) {
                if (closeReachPriceLimitMin) {
                    conditionsCalced.set(4, conditionNames.get(4) + "[1,1]"); // 不可能
                } else {
                    conditionsCalced.set(4, conditionNames.get(4) + "[1,0]");
                }
            } else {
                if (closeReachPriceLimitMin) {
                    conditionsCalced.set(4, conditionNames.get(4) + "[0,1]");
                } else {
                    conditionsCalced.set(4, conditionNames.get(4) + "[0,0]");
                }
            }
        } else {
            // 在传递参数, 强制不进行涨跌停判定情况下, 视为 未涨停且未跌停
            conditionsCalced.set(4, conditionNames.get(4) + "[0,0]");
        }
        // 6.前5日涨幅判定
        Double pre5dayPercent = todayClose / getPriceOfSingleKline(pre5dayKlineRow, "close") - 1;
        List<String> pre5dayPercentConditionList = getConditionNamesForNodeList(pre5dayPercentRangeList,
                conditionNames.get(5));
        for (int i = 0; i < pre5dayPercentConditionList.size() - 1; i++) {
            if (pre5dayPercent < pre5dayPercentRangeList.get(i + 1) && pre5dayPercent >= pre5dayPercentRangeList
                    .get(i)) {
                conditionsCalced.set(5, pre5dayPercentConditionList.get(i));
                break;
            }
        }

        // 7.今日成交量 / 前五日平均成交量: 使用 vol列, 而非amount金额. 注意需要 df_window 复权形式一样. 要么无复权,要么全部后复权
        Double volToPre5day = todayVol / (Double) dfWindow.slice(0, 5, fieldsOfDfRaw.indexOf("vol"),
                fieldsOfDfRaw.indexOf("vol") + 1).mean().get(0, 0);
        // .mean() 也得到了df, 这里只有唯一一个了
        List<String> volToPre5dayConditionList = getConditionNamesForNodeList(volToPre5dayAvgRangeList,
                conditionNames.get(6));
        for (int i = 0; i < volToPre5dayConditionList.size() - 1; i++) {
            if (volToPre5day < volToPre5dayAvgRangeList.get(i + 1) && volToPre5day >= volToPre5dayAvgRangeList.get(i)) {
                conditionsCalced.set(6, volToPre5dayConditionList.get(i));
                break;
            }
        }

        return conditionsCalced;
    }

    public static String getTradeDateOfSingleKline(List<Object> klineRow) {
        return (String) klineRow.get(fieldsOfDfRaw.indexOf("trade_date"));
    }

    public static Double getPriceOfSingleKline(List<Object> klineRow, String whichPrice) {
        return (Double) klineRow.get(fieldsOfDfRaw.indexOf(whichPrice));
    }

    public static boolean[] isReachPriceLimit(String stock,
                                              List<Object> yesterdayKlineRow,
                                              List<Object> todayKlineRow,
                                              HashMap<String, List<List<String>>> stockWithStDateRanges,
                                              DataFrame<String> stockWithBoard,
                                              boolean simpleMode) throws SQLException {
        // 详尽分析时, simpleMode 为false.   特殊情况手动调用时, 可以 true提高计算速度
        boolean priceReachPriceLimitMax = false;
        boolean priceReachPriceLimitmin = false;
        Object[] res_ = getReachPriceLimitDates("000153.SZ");
        HashSet<String> datesSetOfPriceLimitMax = (HashSet<String>) res_[0];
        HashSet<String> datesSetOfPriceLimitMin = (HashSet<String>) res_[1]; // 只可能为空集合
        List<String> effectiveCalcDateRange = (List<String>) res_[2]; // 可能为null
        String today = getTradeDateOfSingleKline(todayKlineRow);
        if (effectiveCalcDateRange != null && !simpleMode) {
            if (effectiveCalcDateRange.get(0)
                    .compareTo(today) <= 0 && effectiveCalcDateRange.get(1)
                    .compareTo(today) >= 0) {
                priceReachPriceLimitMax = datesSetOfPriceLimitMax.contains(today);
                priceReachPriceLimitmin = datesSetOfPriceLimitMin.contains(today);
                return new boolean[]{priceReachPriceLimitMax, priceReachPriceLimitmin};
            }
        }
        // 普通方法, 涨跌幅计算法.
        // @bugfix: python实现有bug, 此处直接else, 将导致, 有涨跌停记录, 但是未在区间内的日期, 统一返回 false,false
        // @bugfix: 对其他所有情况, 均应该采用 涨跌幅的方式计算是否涨跌停.
        Double priceLimit = calcPriceLimitOfStockOneDay(stock, today, stockWithBoard, stockWithStDateRanges);
        Double yesterdayClose = getPriceOfSingleKline(yesterdayKlineRow, "close");
        Double priceLimitMax = new BigDecimal((1 + priceLimit) * yesterdayClose).setScale(2,
                BigDecimal.ROUND_HALF_UP).doubleValue(); // 使用 BigDecimal的最经典 ROUND_HALF_UP, 就是常规意义的四舍五入
        Double priceLimitmin = new BigDecimal((1 - priceLimit) * yesterdayClose).setScale(2,
                BigDecimal.ROUND_HALF_UP).doubleValue(); // 使用 BigDecimal的最经典 ROUND_HALF_UP, 就是常规意义的四舍五入
        Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
        priceReachPriceLimitMax = todayClose >= priceLimitMax;
        priceReachPriceLimitmin = todayClose <= priceLimitmin;
        return new boolean[]{priceReachPriceLimitMax, priceReachPriceLimitmin};
    }

    public static Double calcPriceLimitOfStockOneDay(String stock, String today, DataFrame<String> stockWithBoard,
                                                     HashMap<String, List<List<String>>> stockWithStDateRanges) {
        if (isStToday(stock, today, stockWithStDateRanges)) {
            return 0.05;
        }
        // ts_code,market 两列的df

        if (stockWithBoardAsDict == null) { // 唯一计算一次
            stockWithBoardAsDict = new ConcurrentHashMap<>();
            for (int i = 0; i < stockWithBoard.length(); i++) {
                // 430057.BJ -- null , ConcurrentHashMap要求key/value都不为null.
                // 这里我们最好从源头, 把  stockWithBoard筛选股票的
                //Console.log("{} -- {}", (String) stockWithBoard.get(i, 0), (String) stockWithBoard.get(i, 1));
                stockWithBoardAsDict.put((String) stockWithBoard.get(i, 0), (String) stockWithBoard.get(i, 1));
            }
        }
        String board = stockWithBoardAsDict.get(stock);
        if ("科创板".equals(board)) {
            return 0.2;
        }
        if ("创业板".equals(board)) {
            if (today.compareTo("20200824") >= 0) {
                return 0.2;
            } else {
                return 0.1;
            }
        }
        return 0.1;
    }

    public static boolean isStToday(String stock, String today,
                                    HashMap<String, List<List<String>>> stockWithStDateRanges) {
        List<List<String>> stDateRanges = stockWithStDateRanges.get(stock);
        if (stDateRanges == null) {
            return false;
        }
        for (int i = 0; i < stDateRanges.size(); i++) {
            String startDate = stDateRanges.get(i).get(0);
            String endDate = stDateRanges.get(i).get(1);
            if (today.compareTo(startDate) >= 0 && today.compareTo(endDate) <= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * today_open_range_list = [0.205, 0.09, 0.05, 0.02, 0.005, -0.005, -0.02, -0.05, -0.09, -0.205]
     * 返回 OP[0.205,0.09]  等多个字符串的列表, 最终组合 form_name
     *
     * @param nodes
     * @param conditionRawStr
     * @return
     */
    public static List<String> getConditionNamesForNodeList(List<Double> nodes, String conditionRawStr) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            res.add(StrUtil.format("{}[{},{}]", conditionRawStr, nodes.get(i), nodes.get(i + 1)));
        }
        return res;
    }

    /**
     * 求 某股票的 所有 复权的日期, 和某个window中日期, 的交集. 可判定该区间是否复权过了.
     *
     * @param adjDates
     * @param dfWindowRaw
     * @return
     */
    public static boolean hasIntersectionBetweenAdjDatesAndDfWindow(HashSet<String> adjDates,
                                                                    DataFrame<Object> dfWindowRaw) {
        List<Object> colTradeDates = dfWindowRaw.col(fieldsOfDfRaw.indexOf("trade_date"));
        HashSet<String> datesSet = new HashSet<>();
        for (Object date : colTradeDates) {
            datesSet.add((String) date);
        }
        HashSet<String> intersection = new HashSet<>();
        intersection.addAll(datesSet);
        intersection.retainAll(adjDates); // 并集则再add, 差集则removeAll
        if (intersection.size() > 0) {
            return true;
        }
        return false;
    }


}




