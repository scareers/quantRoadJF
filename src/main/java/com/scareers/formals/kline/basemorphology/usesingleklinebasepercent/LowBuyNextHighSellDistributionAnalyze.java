package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent.binsList;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent.effectiveValusRanges;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.analyzeListDoubleSingle;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.prepareSaveDfForAnalyzeResult;
import static com.scareers.utils.CommonUtils.intersectionOfSet;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 本脚本, 依据截止今日的数据, 而决定明日买后日卖,或者后日卖大后日卖, 以此类推. 本脚本分析不同 形态集合下的 明后日分布
 * -- highArg /LowArg 决定出形态集,  然后对集合的未来2/3/4.. 天进行四项数据基本分析,并保存到数据库
 * 1.数据流程:  主程序全数据解析 -- FilterSimple简单筛选出几十万条 -- 本脚本, 对不同参数下, 0b1s/1b2s 等的分布研究结果
 *
 * @author: admin
 * @date: 2021/11/11  0011-8:06
 * <p>
 * """
 * 3.进一步验证分析脚本
 * <p>
 * // 对应了 java 的  LowBuyNextHighSellDistributionAnalyze
 * <p>
 * 1.保存到数据库时, 多出的字段等, 放在condition1-10里面
 * len(selected_form_amounts)           初始被选中数量, 简单求交集来的      放在 condition3
 * len(calced_forms)                      实际被选中数量,  经过了 历史数据的筛选   放在 condition4
 * <p>
 * # 嵌套循环条件
 * high_key_args = (0.035, 1)  # 初步筛选 nexthigh> ?     对next1 的high的限制     放在condition1
 * low_key_args = (-0.03, -0.02)  # 初步筛选 nextlow> ?    对next0 的low 的限制     放在condition2
 * # 计算项目,  8个.
 * int_table = 0      #     计算保存哪个表        8个计算项目, 以 stat_result_algorithm 列区分
 * algorithm_raw = 'High' # 的哪种算法.
 * <p>
 * filter_dict_    形态的强制筛选参数字典. str       放在 condition5
 * <p>
 * 实际选中的forms , 保存在 form_description.    form列None
 * 2.逻辑是:
 * 依据 high_key_args 和  low_key_args, 得到选中forms, 这很耗时,
 * 双层循环
 * <p>
 * 然后, 计算8种计算项, 并保存结果到数据库.
 * <p>
 * 期间:
 * 对 实际选中的forms结果 保存到 form_description; 不再保存成文件
 * 图片 是频数分布图片, 保存到 figure_save_basedir文件夹
 * <p>
 * <p>
 * """
 */
public class LowBuyNextHighSellDistributionAnalyze {
    // 核心设定: 元素1和2分别为 低卖,高卖的 两个时间. 0代表明日, 一次类推
    // 后面的 数据读取表, 结果保存表, 均受此核心设定决定!!!!!! 只需更改这个唯一设定即可  , 设置可 明日买,大后天卖.
    public static List<Integer> correspondingFilterAlgos = Arrays.asList(0, 1);
    public static List<String> validateDateRangeList = Arrays.asList("20210218", "21000101"); //注意和保存在数据库的json字符串保持一致,
    public static String tablenameSaveAnalyze = StrUtil.format("next{}b{}s_of_single_kiline",
            correspondingFilterAlgos.get(0), correspondingFilterAlgos.get(1)); // next0b1s_of_single_kiline
    public static String tablenameLowBuy = StrUtil.format("filtered_single_kline_from_next{}__excybkcb",
            correspondingFilterAlgos.get(0)); // 哪天低买?简单筛选后的表名称
    public static String tablenameHighSell = StrUtil.format("filtered_single_kline_from_next{}__excybkcb",
            correspondingFilterAlgos.get(1));// 哪天高卖?简单筛选后的表名称
    public static List<String> algorithmRawList = Arrays.asList("Open", "Close", "High", "Low");
    public static Connection connection = ConnectionFactory.getConnLocalKlineForms();
    public static String validateDateRange = JSONUtil.toJsonStr(validateDateRangeList); ////同上,方便参数传递而已

    public static String sqlCreateSaveTable = getSqlCreateSaveTable();
    // 卖点当天 最低价限定
    public static List<List<Double>> highKeyArgsList = Arrays.asList(
            // 跨度 0.015
            Arrays.asList(0.0, 0.015),
            Arrays.asList(0.015, 0.03),
            Arrays.asList(0.03, 0.045),
            Arrays.asList(0.045, 0.06),
            Arrays.asList(0.06, 1.0),
            // 跨度 0.03
            Arrays.asList(0.01, 0.04),
            Arrays.asList(0.04, 0.07),
            Arrays.asList(0.07, 1.0),
            // 跨度 0.04
            Arrays.asList(0.01, 0.05),
            Arrays.asList(0.05, 1.0),
            // 交叉 0.03
            Arrays.asList(0.02, 0.05),
            Arrays.asList(0.03, 0.06),
            // 交叉 0.04
            Arrays.asList(0.0, 0.04),
            Arrays.asList(0.03, 0.07),
            // 1 尾
            Arrays.asList(0.01, 1.0),
            Arrays.asList(0.02, 1.0),
            Arrays.asList(0.03, 1.0),
            Arrays.asList(0.04, 1.0)
    );

    // 买点当日 最低价限定; 基本与 high 一一对应反向;   总之: low,high, 是做筛选的参数. 后面两个是计算分析分布的参数
    public static List<List<Double>> lowKeyArgsList = Arrays.asList(
            Arrays.asList(-1.0, -0.06),
            Arrays.asList(-0.06, -0.045),
            Arrays.asList(-0.045, -0.03),
            Arrays.asList(-0.03, -0.015),
            Arrays.asList(-0.015, -0.0),

            Arrays.asList(-0.04, -0.01),
            Arrays.asList(-0.07, -0.04),
            Arrays.asList(-1.0, -0.07),

            Arrays.asList(-0.05, -0.01),
            Arrays.asList(-1.0, -0.05),

            Arrays.asList(-0.05, -0.02),
            Arrays.asList(-0.06, -0.03),

            Arrays.asList(-0.04, -0.0),
            Arrays.asList(-0.07, -0.03),

            Arrays.asList(-1.0, -0.01),
            Arrays.asList(-1.0, -0.02),
            Arrays.asList(-1.0, -0.03),
            Arrays.asList(-1.0, -0.04)
    );

    public static HashMap<String, Double> forceFilterFormArgs = getForceFilterFormArgs();

    private static HashMap<String, Double> getForceFilterFormArgs() {
        HashMap<String, Double> res = new HashMap<>();
        res.put("low_limit_of_CGO", -100000.0); // 下限限制, 去掉该值及以下. 极小负数则相当于无筛选功能
        res.put("low_limit_of_US", -100000.0);
        res.put("low_limit_of_OP", -100000.0);
        res.put("low_limit_of_LS", -100000.0);
        res.put("low_limit_of_P5DP", -100000.0);
        res.put("low_limit_of_VTP5D", -100000.0);

        res.put("high_limit_of_CGO", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_US", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_OP", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_LS", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_P5DP", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_VTP5D", 100000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        return res;
    }

    private static HashMap<String, Double> getForceFilterFormArgsRaw() { // 默认无筛选, 定死
        HashMap<String, Double> res = new HashMap<>();
        res.put("low_limit_of_CGO", -1000000.0); // 下限限制, 去掉该值及以下. 极小负数则相当于无筛选功能
        res.put("low_limit_of_US", -1000000.0);
        res.put("low_limit_of_OP", -1000000.0);
        res.put("low_limit_of_LS", -1000000.0);
        res.put("low_limit_of_P5DP", -1000000.0);
        res.put("low_limit_of_VTP5D", -1000000.0);

        res.put("high_limit_of_CGO", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_US", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_OP", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_LS", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_P5DP", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        res.put("high_limit_of_VTP5D", 1000000.0); // 上限限制, 去掉该值及以上的, 极大正数,相当于无筛选功能
        return res;
    }


    private static String getSqlCreateSaveTable() {
        String s = StrUtil.format(SettingsOfSingleKlineBasePercent.sqlCreateSaveTableRaw, tablenameSaveAnalyze);
        s = s.replace("form_name                          varchar(512)  null comment '形态名称.形如 条件[参数区间]__条件2[参数区间]", "" +
                "form_name                          longtext  null comment '形态名称.形如 条件[参数区间]__条件2[参数区间]");
        return s;
    }

    public static Log log = LogFactory.get();

    public static void main(String[] args) throws Exception {
        execSql(sqlCreateSaveTable, connection); // 创建结论保存表

        for (List<Double> highArgs : highKeyArgsList) {
            // 高卖限制
            DataFrame<Object> dfOfHighLimitConditon = getHighConditionLimitDf(tablenameHighSell, highArgs,
                    validateDateRange);
            for (List<Double> lowArgs : lowKeyArgsList) {
                log.info(StrUtil.format("HighSell selected forms count: {}", dfOfHighLimitConditon.length()));
                DataFrame<Object> dfOfLowLimitConditon = getLowConditionLimitDf(tablenameLowBuy, lowArgs,
                        validateDateRange); // 低买
                log.info(StrUtil.format("LowBuy selected forms count: {}", dfOfLowLimitConditon.length()));

                HashSet<String> selectedForms = getSelectFormsSet(dfOfHighLimitConditon, dfOfLowLimitConditon);

                for (Integer intTable : correspondingFilterAlgos) {
                    for (String algorithmRaw : algorithmRawList) {
                        String resultAlgorithm = StrUtil.format("Next{}{}", intTable, algorithmRaw);
                        String info = StrUtil
                                .format("LowBuy {}, HighSell {}, -- {}", lowArgs, highArgs, resultAlgorithm);
                        log.info(StrUtil.format("start: {}", info));
                        String resultTableName = StrUtil.format("filtered_single_kline_from_next{}__excybkcb",
                                intTable); // 通常对作为条件的两个表, 都做四项计算

                        HashMap<String, List<Object>> calcedForms = conditionOptimizeTrying(selectedForms,
                                resultTableName, resultAlgorithm, connection,
                                forceFilterFormArgs, validateDateRange);
                        List<Double> finalEarnings = new ArrayList<>(); // 相比python, 需要自行计算出来calcedForms,更快
                        for (String key : calcedForms.keySet()) {
                            Double earing = (Double) calcedForms.get(key).get(0);
                            Integer counts = (Integer) calcedForms.get(key).get(1);
                            for (int i = 0; i < counts; i++) {
                                finalEarnings.add(earing); // 对全部形态, 符合条件下, 几何日收益率的等价汇总
                            }
                        }

                        HashSet<String> actualCalcedFormSet = new HashSet<>(calcedForms.keySet());
                        HashMap<String, Object> resultSingle = analyzeListDoubleSingle(finalEarnings, 10000,
                                SettingsOfSingleKlineBasePercent.bigChangeThreshold, binsList.get(intTable),
                                effectiveValusRanges.get(intTable), false);
                        Integer selectedFormCounts = selectedForms.size();
                        DataFrameSelf<Object> dfSingle = prepareSaveDfForAnalyzeResult(resultSingle,
                                JSONUtil.toJsonStr(actualCalcedFormSet), // python字段放在form描述里面, java没有描述字段, 放在formname字段
                                validateDateRangeList, resultAlgorithm, null, JSONUtil.toJsonStr(highArgs),
                                JSONUtil.toJsonStr(lowArgs), selectedFormCounts.toString(),
                                String.valueOf(calcedForms.size()), JSONUtil.toJsonStr(forceFilterFormArgs), null,
                                null);
                        DataFrameSelf.toSql(dfSingle, tablenameSaveAnalyze, connection, "append", null);
                        Console.log(resultSingle);
                        Console.log("selected forms counts: {}", selectedForms.size());
                        Console.log("actual selected counts: {}", calcedForms.size());
                    }
                }
            }


        }


    }

    /**
     * 相比python, 2个返回值, java将 finalEarnings舍弃, 由调用方, 从 calcedForms 返回值自行计算得来, 且逻辑会更快
     *
     * @param selectedForms
     * @param resultTableName
     * @param resultAlgorithm
     * @param connection
     * @param forceFilterFormArgs
     * @param validateDateRange
     * @return
     * @throws SQLException
     */
    public static HashMap<String, List<Object>> conditionOptimizeTrying(HashSet<String> selectedForms,
                                                                        String resultTableName,
                                                                        String resultAlgorithm, Connection connection,
                                                                        // 少了print
                                                                        HashMap<String, Double> forceFilterFormArgs,
                                                                        String validateDateRange)
            throws SQLException {

        HashMap<String, Double> forceFilterFormArgsRaw = getForceFilterFormArgsRaw();
        forceFilterFormArgsRaw.putAll(forceFilterFormArgs);

        // List<Double> finalEarnings = new ArrayList<>();
        HashMap<String, List<Object>> calcedForms = new HashMap<>(); // key:value:  某种具体形态: (折算复利收益率,次数)
        for (String formName : selectedForms) {
            if (forceFilterByLowAndHighLimit(formName, forceFilterFormArgsRaw)) {
                continue; // 被强制筛选掉了. 默认设定没有筛选能力 ; python代码已经修复
            }
            DataFrame<Object> df_ = new DataFrame<>();
            df_ = DataFrame.readSql(connection, StrUtil.format("            select  \n" +
                    "                stat_date_range,\n" +
                    "                    virtual_geometry_mean,\n" +
                    "                    mean,\n" +
                    "                    effective_counts,\n" +
                    "                    total_counts,\n" +
                    "                    zero_compare_counts_percent_0,\n" +
                    "                    zero_compare_counts_percent_2\n" +
                    "             from {}\n" +
                    "             where form_name ='{}'\n" +
                    "                   and stat_result_algorithm = '{}'\n" +
                    "             order by stat_date_range", resultTableName, formName, resultAlgorithm));
            df_ = df_.convert();
            if (df_.length() == 0) {
                continue;
            }
            List<Object> colOfStatDateRange = df_.col("stat_date_range");
            if (!colOfStatDateRange.get(colOfStatDateRange.size() - 1).equals(validateDateRange)) {
                continue; // 最后一个日期区间, 不是需要验证的区间
            }
            List<Object> colOfEarnings = df_.col("virtual_geometry_mean");
            List<Object> colOfEffectiveCounts = df_.col("effective_counts");
            Double earning = (Double) colOfEarnings.get(colOfEarnings.size() - 1);
            Integer counts = (Integer) colOfEffectiveCounts.get(colOfEffectiveCounts.size() - 1);

            List<String> childrenCalced = isParentOfRecord(formName, calcedForms);
            boolean hasChildCalcedAlready = childrenCalced.size() > 0;
            if (havaParentInRecord(formName, calcedForms)) {
                continue; // 当父形态已被计算, 则本形态不再计算加入
            } else if (hasChildCalcedAlready) { // 有子形态计算过了, 则删除所有子形态,
                for (String child : childrenCalced) {
                    calcedForms.remove(child);
                }
            }
            // 此时, 有自行他计算过了已被删除, 所有情况都需要加入本(父)形态
            calcedForms.put(formName, Arrays.asList(earning, counts)); // 添加本父形态
        }
        return calcedForms;
    }

    private static boolean havaParentInRecord(String formName, HashMap<String, List<Object>> calcedForms) {
        HashSet<String> formNameSplitSet = new HashSet<>(StrUtil.split(formName, "__"));
        for (String formNameCalced : calcedForms.keySet()) {
            HashSet<String> formNameCalcedSplitSet = new HashSet<>(StrUtil.split(formNameCalced, "__"));
            HashSet<String> interaction = intersectionOfSet(formNameSplitSet, formNameCalcedSplitSet);
            if (formNameCalcedSplitSet.equals(interaction)) {
                // HashSet.equals 底层调用 containsAll
                return true;
            }
        }
        return false;
    }

    private static List<String> isParentOfRecord(String formName, HashMap<String, List<Object>> calcedForms) {
        List<String> children = new ArrayList<>();
        HashSet<String> formNameSplitSet = new HashSet<>(StrUtil.split(formName, "__"));
        for (String formNameCalced : calcedForms.keySet()) {
            HashSet<String> formNameCalcedSplitSet = new HashSet<>(StrUtil.split(formNameCalced, "__"));
            HashSet<String> interaction = intersectionOfSet(formNameSplitSet, formNameCalcedSplitSet);
            if (formNameSplitSet.equals(interaction)) {
                // HashSet.equals 底层调用 containsAll
                children.add(formNameCalced);
            }
        }
        return children;
    }

    public static boolean forceFilterByLowAndHighLimit(String formName,
                                                       HashMap<String, Double> forceFilterFormArgsActual) {
        List<String> conditions = Arrays.asList("CGO", "US", "OP", "LS", "P5DP", "VTP5D");// 对6条件进行强制筛选
        for (String condition : conditions) {
            Double lowLimitOfCondition = getLowLimit(formName, condition);
            if (lowLimitOfCondition == null) {
                continue; // 如果形态中没有明确指明该条件限制值, 则无视掉, 通过筛选
            }
            if (lowLimitOfCondition <= forceFilterFormArgsActual.get("low_limit_of_" + condition)) {
                return true; // 一旦low限制<=设置, 则强行筛选掉
            }

            Double highLimitOfCondition = getHighLimit(formName, condition); // 逻辑类似
            if (highLimitOfCondition == null) {
                continue;
            }
            if (lowLimitOfCondition >= forceFilterFormArgsActual.get("high_limit_of_" + condition)) {
                return true; // 一旦low限制<=设置, 则强行筛选掉
            }
        }
        return false; // 默认没有被筛选掉
    }

    private static Double getHighLimit(String formName, String condition) {
        String condition_ = condition + "[";
        int pos = formName.indexOf(condition_);
        if (pos != -1) {
            String formNameTemp = StrUtil.sub(formName, pos, formName.length());
            int pos1 = formNameTemp.indexOf("]");
            int pos2 = formNameTemp.indexOf(",");
            String upperLimit = StrUtil.sub(formNameTemp, pos2 + 1, pos1);
            return Double.valueOf(upperLimit);
        }
        return null;
    }

    private static Double getLowLimit(String formName, String condition) {
        String condition_ = condition + "[";
        int pos = formName.indexOf(condition_);
        if (pos != -1) {
            String formNameTemp = StrUtil.sub(formName, pos, formName.length());
            int pos1 = formNameTemp.indexOf("[");
            int pos2 = formNameTemp.indexOf(",");
            String lowLimit = StrUtil.sub(formNameTemp, pos1 + 1, pos2);
            return Double.valueOf(lowLimit);
        }
        return null;
    }


    public static HashSet<String> getSelectFormsSet(DataFrame<Object> dfOfHighLimitConditon,
                                                    DataFrame<Object> dfOfLowLimitConditon) {
        // 求 "form_name" 列, 字符串列表的交集.
        HashSet<String> lowSet = new HashSet<>();
        for (Object form : dfOfLowLimitConditon.col("form_name")) {
            lowSet.add((String) form);
        }
        HashSet<String> highSet = new HashSet<>();
        for (Object form : dfOfHighLimitConditon.col("form_name")) {
            highSet.add((String) form);
        }

        HashSet<String> res = new HashSet<>();
        res.addAll(lowSet);
        res.retainAll(highSet); // 交集.
        return res;
    }

    /**
     * @param tablenameHighSell 表名
     * @param highArgs          需要 卖出时当天high 大于>
     * @param validateDateRange 筛选时不包括此日期区间的数据, 避免引入未来数据. 通常是最后一个日期区间
     * @return
     * @throws SQLException
     */
    public static Cache<String, DataFrame<Object>> highConditionLimitDfCache = CacheUtil.newLRUCache(128);
    public static Cache<String, DataFrame<Object>> lowConditionLimitDfCache = CacheUtil.newLRUCache(128);

    @Cached(notes = "注意:只对 highArgs 参数作为key进行缓存. 因为一般本脚本不会并行, tablename高卖和排除的日期区间, 都是固定的!!!")
    public static DataFrame<Object> getHighConditionLimitDf(String tablenameHighSell, List<Double> highArgs,
                                                            String validateDateRange) throws SQLException {
        // 缓存仅仅使用 highargs 转换为 String 做key
        String cacheKey = StrUtil.join(",", highArgs);
        DataFrame<Object> df = highConditionLimitDfCache.get(cacheKey);
        if (df != null) {
            return df;
        }

        df = DataFrame.readSql(connection, StrUtil.format(
                "select *\n" +
                        "        from (select form_name,\n" +
                        "                     avg(effective_counts)                   as avgcounts,\n" +
                        "                     avg(mean)                               as mean,\n" +
                        "                     avg(zero_compare_counts_percent_2)      as zero2\n" +
                        "        \n" +
                        "              FROM {}\n" +
                        "              where\n" +
                        "                condition5 = 'PL[0,0]'\n" +
                        "                and\n" +
                        "                stat_result_algorithm = 'Next{}High'\n" +
                        "                and stat_date_range!='{}'\n" +
                        "              group by form_name\n" +
                        "              order by zero2 desc) as temp\n" +
                        "        where mean >= {}\n" +
                        "        and mean < {}\n" +
                        "          and avgcounts > 0"
                , tablenameHighSell, correspondingFilterAlgos.get(1), validateDateRange, highArgs.get(0),
                highArgs.get(1)
        ));
        df = df.convert();
        highConditionLimitDfCache.put(cacheKey, df);
        return df;
    }

    @Cached(notes = "注意:只对 lowArgs 参数作为key进行缓存. 因为一般本脚本不会并行, tablename高卖和排除的日期区间, 都是固定的!!!")
    public static DataFrame<Object> getLowConditionLimitDf(String tablenameLowBuy, List<Double> lowArgs,
                                                           String validateDateRange) throws SQLException {
        // 缓存仅仅使用 highargs 转换为 String 做key
        String cacheKey = StrUtil.join(",", lowArgs);
        DataFrame<Object> df = lowConditionLimitDfCache.get(cacheKey);
        if (df != null) {
            return df;
        }

        df = DataFrame.readSql(connection, StrUtil.format(
                "select *\n" +
                        "        from (select form_name,\n" +
                        "                     avg(effective_counts)                   as avgcounts,\n" +
                        "                     avg(mean)                               as mean,\n" +
                        "                     avg(zero_compare_counts_percent_2)      as zero2\n" +
                        "        \n" +
                        "              FROM {}\n" +
                        "              where\n" +
                        "                condition5 = 'PL[0,0]'\n" +
                        "                and\n" +
                        "                stat_result_algorithm = 'Next{}Low'\n" +
                        "                and stat_date_range!='{}'\n" +
                        "              group by form_name\n" +
                        "              order by zero2 desc) as temp\n" +
                        "        where mean >= {}\n" +
                        "        and mean < {}\n" +
                        "          and avgcounts > 0"
                , tablenameLowBuy, correspondingFilterAlgos.get(0), validateDateRange, lowArgs.get(0),
                lowArgs.get(1)
        ));
        df = df.convert();
        lowConditionLimitDfCache.put(cacheKey, df);
        return df;
    }


}
