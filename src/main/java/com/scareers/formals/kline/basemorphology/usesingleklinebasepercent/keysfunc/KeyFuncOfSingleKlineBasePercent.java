package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.combinpermu.Generator;
import joinery.DataFrame;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.sqlapi.TushareApi.getReachPriceLimitDates;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/10  0010-4:29
 */
public class KeyFuncOfSingleKlineBasePercent {
    public static List<Double> getTickListByBinsAndEffectiveValueRange(List<Double> effectiveValueRange, int bins,
                                                                       double perRangeWidth) {
        // double perRangeWidth = (effectiveValueRange.get(1) - effectiveValueRange.get(0)) / bins;
        List<Double> tickList = new ArrayList<>();
        for (int i = 0; i < bins + 1; i++) {// 初始化 tick列表.
            tickList.add(CommonUtils.roundHalfUP(effectiveValueRange.get(0) + i * perRangeWidth, 3));
        }
        return tickList;
    }

    // java修改后, 本函数为 多条记录同时分析. 当然逻辑上,只是简单循环单条分析List<Double> .
    public static HashMap<String, HashMap<String, Object>> analyzeStatsResultsStatic(List<String> formNameRaws,
                                                                                     ConcurrentHashMap<String,
                                                                                             List<Double>> results,
                                                                                     int statStockCounts,
                                                                                     List<Double> bigChangeThreshold,
                                                                                     int bins,
                                                                                     List<Double> effectiveValueRange,
                                                                                     boolean calcCdfOrFrequencyWithTick) {

        HashMap<String, HashMap<String, Object>> res = new HashMap<>();
        for (String formName : formNameRaws) {
            List<Double> resultSingle = results.get(formName); // 单条结果
            HashMap<String, Object> conclusion = analyzeListDoubleSingle(resultSingle, statStockCounts,
                    bigChangeThreshold,
                    bins, effectiveValueRange,
                    calcCdfOrFrequencyWithTick);
            if (conclusion == null) {
                continue; // 没有有效统计数值, 则conclusion为null. 这里直接skip掉.  后面res.get(key) , 也要判定一下是否为null
            }
            res.put(formName, conclusion);
        }
        return res;
    }

    // 被上函数调用, 仅仅分析 List<Double> 的逻辑, 不含其他任何
    public static HashMap<String, Object> analyzeListDoubleSingle(List<Double> resultSingle,
                                                                  int statStockCounts, List<Double> bigChangeThreshold,
                                                                  int bins, List<Double> effectiveValueRange,
                                                                  boolean calcCdfOrFrequencyWithTick) {
//

        List<Double> outliers = new ArrayList<>();
        List<Double> effectiveResults = new ArrayList<>();
        List<Integer> occurencesList = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            occurencesList.add(0);
            // 数量记录初始化
        }
        double perRangeWidth = (effectiveValueRange.get(1) - effectiveValueRange.get(0)) / bins;
        List<Double> tickList = getTickListByBinsAndEffectiveValueRange(effectiveValueRange, bins, perRangeWidth);
        for (int i = 0; i < resultSingle.size(); i++) {
            Double value = resultSingle.get(i);
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
            return null;
        }

        HashMap<String, Object> conclusion = new HashMap<>();
        conclusion.put("stat_stock_counts", statStockCounts); // int
        conclusion.put("total_counts", resultSingle.size()); // int
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
        ArrayList<Double> zeroCompareCountsPercent = getCompareCountsAndPercentList(effectiveResults, conclusion, zeroCompareCounts,
                ltZero,
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
        ArrayList<Double> bigchangeCompareCountsPercent = getCompareCountsAndPercentList(effectiveResults, conclusion,
                bigchangeCompareCounts, ltBigchange, betweenBigchange, gtBigchange,
                "bigchange_compare_counts");
        // @noti: 这里故意拼写错误 percent成 percnet, 为了数据表字段将错就错
        conclusion.put("bigchange_compare_counts_percnet", bigchangeCompareCountsPercent); //AL

        KeyFuncOfKlineCommons.baseStatValueByDF(effectiveResults, conclusion);

        // Double
        conclusion.put("virtual_geometry_mean", calcVirtualGeometryMeanRecursion(effectiveResults, 100, 1000));


        conclusion.put("effective_value_range", effectiveValueRange);
        conclusion.put("bins", bins);
        conclusion.put("big_change_threshold", bigChangeThreshold);

        // conclusion.put("tick_list", tickList); //不再保存tickList
        // conclusion.put("tick_list", null); // 或者设置null
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
    public static ArrayList<Double> getCompareCountsAndPercentList(List<Double> effectiveResults, HashMap<String, Object> conclusion,
                                                                   ArrayList<Integer> bigchangeCompareCounts, int ltBigchange,
                                                                   int betweenBigchange, int gtBigchange,
                                                                   String bigchange_compare_counts) {
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

    private static List<List<Double>> getListWithTick(List<Double> tickList, List<Double> cdfList) {
        List<List<Double>> cdfWithTick = new ArrayList<>();
        for (int i = 0; i < cdfList.size(); i++) {
            Double cdf = cdfList.get(i);
            Double tick = tickList.get(i + 1);
            cdfWithTick.add(Arrays.<Double>asList(cdf, tick));
        }
        return cdfWithTick;
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


    public static DataFrameSelf<Object> prepareSaveDfForAnalyzeResult(HashMap<String, Object> analyzeResultMapSingle,
                                                                      String formNamePure,
                                                                      List<String> statDateRange,
                                                                      String statResultAlgorithm,
                                                                      String conditionsSet,
                                                                      String condition1,
                                                                      String condition2,
                                                                      String condition3,
                                                                      String condition4,
                                                                      String condition5,
                                                                      String condition6,
                                                                      String condition7) throws SQLException {

        return prepareSaveDfForAnalyzeResult(analyzeResultMapSingle, formNamePure, statDateRange, statResultAlgorithm,
                conditionsSet
                , condition1, condition2, condition3, condition4, condition5, condition6,
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
     * @param analyzeResultMapSingle
     * @param formNamePure
     * @param statDateRange
     * @param statResultAlgorithm
     * @param conditionsSet
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
    // 本函数是针对单条结果的, 并不执行保存逻辑, 返回 单条记录的 df; 调用方可拼接多次实现批量保存
    public static DataFrameSelf<Object> prepareSaveDfForAnalyzeResult(HashMap<String, Object> analyzeResultMapSingle,
                                                                      String formNamePure,
                                                                      List<String> statDateRange,
                                                                      String statResultAlgorithm,
                                                                      String conditionsSet,
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
        if (analyzeResultMapSingle == null) {
            return null;
        }

        DataFrameSelf<Object> dfSaved = new DataFrameSelf<>();
        List<Object> row = new ArrayList<>();
        for (String key : analyzeResultMapSingle.keySet()) {
            // zero_compare_counts ArrayList<Integer> 3
            // zero_compare_counts_percent ArrayList<Double>  3
            // bigchange_compare_counts  ArrayList<Integer> 3
            // bigchange_compare_counts_percent  ArrayList<Double>  3
            // 当key为以上四个, 则需要对字段进行 0,1,2 的拆分, 值也进行拆分. 总计分为12个字段. 方便数据库直接查询.
            if ("zero_compare_counts".equals(key) || "bigchange_compare_counts".equals(key)) {
                // 3 个Integer 的AL, 这里1拆为3; --> 0,1,2
                ArrayList<Integer> tempList = (ArrayList<Integer>) analyzeResultMapSingle.get(key);
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
                ArrayList<Double> tempList = (ArrayList<Double>) analyzeResultMapSingle.get(key);
                for (int i = 0; i < 3; i++) {
                    String fieldName = key + "_" + i;
                    dfSaved.add(fieldName);
                    row.add(tempList.get(i));
                }
                continue;
            }

            dfSaved.add(key);
            Object value = analyzeResultMapSingle.get(key);
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
        //dfSaved.toSql(saveTablename, saveDb, "append", null);
        return dfSaved;
        // 连接未关闭
    }

    /**
     * 根据给定的 [] 7条件字符串数组.  返回 0-7 全组合, 共计 128种组合, 的具体 形态名称, 以 __ 结尾
     *
     * @param concreteTodayFormStrs
     * @return
     */
    //                Console.log(allIndexCombinations.size()); // 全索引组合128种
    public static ArrayList<List<Integer>> allIndexCombinations = null; // 缓存加速

    public static List<String> getAllFormNamesByConcreteFormStrs(List<String> concreteTodayFormStrs) {
        if (allIndexCombinations == null) {
            allIndexCombinations = new ArrayList<>();
            for (int j = 0; j < 7 + 1; j++) {// 单组合取 0,1,2,3,4,5,6,7 个; 8种
                Generator.combination(0, 1, 2, 3, 4, 5, 6) // 全部可取索引, 7个,固定的
                        .simple(j)
                        .stream()
                        .forEach(allIndexCombinations::add);
            }
        }

        List<String> allFormNames__ = new ArrayList<>();
        for (int j = 0; j < allIndexCombinations.size(); j++) {
            List<Integer> singleCombination = allIndexCombinations.get(j);
            String keyTemp = "";
            for (int k = 0; k < singleCombination.size(); k++) {
                keyTemp += (concreteTodayFormStrs.get(singleCombination.get(k)) + "__");
            }
            if ("".equals(keyTemp)) {
                keyTemp = "Dummy__";
            }
            allFormNames__.add(keyTemp);
        }
        return allFormNames__;
    }

    public static List<String> fieldsOfDfRaw = SettingsOfSingleKlineBasePercent.fieldsOfDfRaw;
    public static final List<String> conditionNames = SettingsOfSingleKlineBasePercent.conditionNames;
    public static final List<Double> upperShadowRangeList = SettingsOfSingleKlineBasePercent.upperShadowRangeList;
    public static final List<Double> lowerShadowRangeList = SettingsOfSingleKlineBasePercent.lowerShadowRangeList;
    public static final List<Double> entityRangeList = SettingsOfSingleKlineBasePercent.entityRangeList;
    public static final List<Double> todayOpenRangeList = SettingsOfSingleKlineBasePercent.todayOpenRangeList;
    public static final List<Double> pre5dayPercentRangeList = SettingsOfSingleKlineBasePercent.pre5dayPercentRangeList;
    public static final List<Double> volToPre5dayAvgRangeList =
            SettingsOfSingleKlineBasePercent.volToPre5dayAvgRangeList;

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

    public static Double getPriceOfSingleKline(List<Object> klineRow, String whichPrice) {
        return (Double) klineRow.get(fieldsOfDfRaw.indexOf(whichPrice));
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

    public static boolean[] isReachPriceLimit(String stock,
                                              List<Object> yesterdayKlineRow,
                                              List<Object> todayKlineRow,
                                              HashMap<String, List<List<String>>> stockWithStDateRanges,
                                              DataFrame<String> stockWithBoard,
                                              boolean simpleMode) throws SQLException {
        // 详尽分析时, simpleMode 为false.   特殊情况手动调用时, 可以 true提高计算速度
        boolean priceReachPriceLimitMax = false;
        boolean priceReachPriceLimitmin = false;
        Object[] res_ = getReachPriceLimitDates(stock);
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

    public static String getTradeDateOfSingleKline(List<Object> klineRow) {
        return (String) klineRow.get(fieldsOfDfRaw.indexOf("trade_date"));
    }

    //为了不大量重构代码, 本参数作为 stockWithBoard 字典版的缓存. 在很久之后才计算一次, 且线程安全, 类似于单例
    public static ConcurrentHashMap<String, String> stockWithBoardAsDict = null; // 缓存到这里了

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
        intersection.retainAll(adjDates);
        // 并集则再add, 差集则removeAll
        if (intersection.size() > 0) {
            return true;
        }
        return false;
    }

}

