package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONUtil;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.*;

/**
 * description: 所有k线相关分析, 十分常用的帮助函数
 * 不再用Integer, 统一用Long.
 *
 * @author: admin
 * @date: 2021/11/17/017-9:03
 */
public class KeyFuncOfKlineCommons {
    public static void main(String[] args) {
        DataFrame<Object> res;
        res = simpleStatAnalyzeByValueListAsDF(ListUtil.of(0.05, 0.1, 0.1, 0.2, 0.2, 0.25, 0.3, 0.3, 0.35), 2,
                ListUtil.of(0.1, 0.3)
                , 0.2, ListUtil.of(0.15, 0.25), true);
        Console.log();
        Console.log(JSONUtil.toJsonPrettyStr(res.columns()));


        Map<String, Object> resMap = simpleStatAnalyzeByValueList(
                ListUtil.of(0.05, 0.1, 0.1, 0.15, 0.18, 0.2, 0.2, 0.25, 0.27, 0.3, 0.3), 2,
                ListUtil.of(0.1, 0.3)
                , 0.2, ListUtil.of(0.15, 0.25), true);
        Console.log(JSONUtil.toJsonPrettyStr(resMap));

    }

    /**
     * // 工具方法, 直接将分析结果转换为 df(单行), 若需要Map, 调用 simpleStatAnalyzeByCountsList, 参数一样
     * 结果列:  对应数据表列
     * *
     * * counts_list, // 参数项目
     * * tick_list,
     * * bins,
     * * reference_value,
     * * small_large_threshold,
     * <p>
     * * samlllarge_compare_counts_percent_0, // 对比值简单统计项目
     * * samlllarge_compare_counts_percent_1,
     * * samlllarge_compare_counts_percent_2,
     * * reference_compare_counts_percent_0,
     * * reference_compare_counts_percent_1,
     * * reference_compare_counts_percent_2,
     * * samlllarge_compare_counts_0,
     * * samlllarge_compare_counts_1,
     * * samlllarge_compare_counts_2,
     * * reference_compare_counts_0,
     * * reference_compare_counts_1,
     * * reference_compare_counts_2,
     * <p>
     * * std,  // 基本统计项目
     * * mean,
     * * max,
     * * min,
     * * kurt,
     * * skew
     * * virtual_geometry_mean,
     * * effective_counts,
     * <p>
     * * frequency_list, // 参数简单计算项目
     * * cdf_list,
     *
     * @param countList
     * @param tickList
     * @param referenceValue
     * @param smallLargeThreshold
     * @param calcVirtualGeometryMean
     * @return
     */
    public static DataFrame<Object> simpleStatAnalyzeByCountsListAsDF(List<Long> countList,
                                                                      List<Double> tickList,
                                                                      Double referenceValue,
                                                                      List<Double> smallLargeThreshold,
                                                                      boolean calcVirtualGeometryMean) {
        Map<String, Object> analyzeResultMap = simpleStatAnalyzeByCountsList(countList, tickList, referenceValue,
                smallLargeThreshold,
                calcVirtualGeometryMean);
        return ConvertSimpleStatAnalyzeResultToDF(analyzeResultMap);
    }

    /**
     * 对比 countList, 使用valueList, 新增字段
     * effective_value_range  // 参数项
     * total_counts  // 包含非有效值的 总数
     * outliers_counts
     * outliers_count_percent
     * effective_count_percent // 有效值百分比
     *
     * @param valueList
     * @param bins
     * @param effectiveValueRange
     * @param referenceValue
     * @param smallLargeThreshold
     * @param calcVirtualGeometryMean
     * @return
     */
    // 工具方法, 直接将分析结果转换为 df(单行), 若需要Map, 调用 simpleStatAnalyzeByValueList, 参数一样
    public static DataFrame<Object> simpleStatAnalyzeByValueListAsDF(List<Double> valueList,
                                                                     int bins,
                                                                     List<Double> effectiveValueRange,
                                                                     Double referenceValue,
                                                                     List<Double> smallLargeThreshold,
                                                                     boolean calcVirtualGeometryMean) {
        Map<String, Object> analyzeResultMap = simpleStatAnalyzeByValueList(valueList, bins, effectiveValueRange,
                referenceValue,
                smallLargeThreshold,
                calcVirtualGeometryMean);
        return ConvertSimpleStatAnalyzeResultToDF(analyzeResultMap);
    }

    /*
        给定计数 返回的字段:



     */

    /**
     * 计数型统计分析: 给定 数数的list, 以及相一一对应的tick 的list.  模拟一个实际值的list.
     * 数据类型选择了最具有普适应的 Long, 和 Double
     * 典型用于 分时最低值出现的时间 分布分析等
     * 与之对应的, 是给定 每个实际值(离散变量)列表, 而非计数, 直接进行统计分析--参考 simpleStatAnalyzeByValueList
     * 本方法类似于 simpleStatAnalyzeByValueList 的, 将所有数据 分区间计数, 后的实现
     * <p>
     * -- 说明
     * 1.因为已经给定 tickList, 传递而来的计数应当有效, 不需要再有 effectiveValueRange 这种设定, 判定数值的有效性
     * 2.返回值, 只计算 数学统计相关, 其他参数之类的无视, 自行在调用方添加
     *
     * @param countList               计数列表
     * @param tickList                对应tick列表
     * @param referenceValue          例如常态情况下, 对涨跌幅传递0.0, 可对>=<0.0的数量作出统计. 传递null则不计算相关项.也可传递其他
     *                                // 例如涨跌幅则 0.0, 分时时间则 120.0
     * @param smallLargeThreshold     可以计算 较小值/较大值 分为三个区间后, 相应的 counts列表, 和percent列表(3个值) == 原来的bigChangeThreshold
     *                                // 例如涨跌幅 -0.05,+0.05算大值.  分时出现时间则  60.0 , 180.0 算小值,大值. 大小值均包含临界点
     * @param calcVirtualGeometryMean 是否计算 virtual_geometry_mean. 涨跌幅的几何平均值有意义, 分时图出现时间则没有意义
     * @return Map<String, Object>  统计的项目名称 : 结果;  结果用Object, 可用 hutool.  JSONUtil.toJsonPrettyStr 显示字符串
     */
    public static Map<String, Object> simpleStatAnalyzeByCountsList(List<Long> countList,
                                                                    List<Double> tickList,
                                                                    List<Double> effectiveResults,// 新增,可传递.
                                                                    Double referenceValue,
                                                                    List<Double> smallLargeThreshold,
                                                                    boolean calcVirtualGeometryMean) {
        Assert.isTrue(countList.size() == tickList.size());
        if (effectiveResults == null) {
            // 当纯用 countList和tickList时, 该参数应当计算出来.
            // 当被 valueList 相关方法调用时, 该参数应当直接传递过来
            effectiveResults = new ArrayList<>(); // 构造模拟的全数据列表.
            for (int i = 0; i < countList.size(); i++) {
                Long count = countList.get(i);
                Double value = tickList.get(i);
                for (int j = 0; j < count; j++) {
                    effectiveResults.add(value);
                }
            }
        }
        if (effectiveResults.size() <= 0) {
            return null;
        }

        HashMap<String, Object> conclusion = new HashMap<>();
        conclusion.put("effective_counts", effectiveResults.size()); // 总数, 放在这里添加. 更加合理

        if (referenceValue != null) {// referenceValue 可能为null, 则不计算  reference_compare_counts和percent结果
            ArrayList<Integer> referenceCompareCounts = new ArrayList<>(); // < = > 给定对比值 的计数列表
            int ltReference = 0, eqReference = 0, gtReference = 0;
            for (double i : effectiveResults) {
                if (i < referenceValue) {
                    ltReference++;
                } else if (i > referenceValue) {
                    gtReference++;
                } else {
                    eqReference++;
                }
            }
            ArrayList<Double> referenceCompareCountsPercent = getCompareCountsAndPercentList(
                    effectiveResults,
                    conclusion,
                    referenceCompareCounts,
                    ltReference,
                    eqReference,
                    gtReference, "reference_compare_counts");
            conclusion.put("reference_compare_counts_percent", referenceCompareCountsPercent); //AL
            conclusion.put("reference_value", referenceValue); // 紧密的参数还是保存一下, 同样可能为null; 原来并未保存 0.0
        }

        if (smallLargeThreshold != null) { // 同样也需要传递 小大值阈值二列表
            ArrayList<Integer> smallLargeCompareCounts = new ArrayList<>();
            int ltSmall = 0, betweenSmallLarge = 0, gtLarge = 0;
            for (Double i : effectiveResults) {
                // Console.log(i, smallLargeThreshold.get(0), smallLargeThreshold.get(1));
                if (i <= smallLargeThreshold.get(0)) {
                    ltSmall++;
                } else if (i >= smallLargeThreshold.get(1)) {
                    gtLarge++;
                } else {
                    betweenSmallLarge++;
                }
            }
            ArrayList<Double> smallLargeCompareCountsPercent = getCompareCountsAndPercentList(effectiveResults,
                    conclusion,
                    smallLargeCompareCounts, ltSmall, betweenSmallLarge, gtLarge,
                    "samlllarge_compare_counts");
            conclusion.put("samlllarge_compare_counts_percent", smallLargeCompareCountsPercent); //AL percent拼写已经修复
            conclusion.put("small_large_threshold", smallLargeThreshold); // 紧密的参数还是保存一下, 同样可能为null
        }

        baseStatValueByDF(effectiveResults, conclusion);

        // Double
        if (calcVirtualGeometryMean) {
            conclusion.put("virtual_geometry_mean", calcVirtualGeometryMeanRecursion(effectiveResults, 100, 1000));
        }

        conclusion.put("bins", tickList.size()); // 很正常
        conclusion.put("counts_list", countList);
        conclusion.put("tick_list", tickList); // 两大参数保存一下. 一般区间不多, 能接受

        ArrayList<Double> frequencyList = new ArrayList<>();
        for (Long i : countList) {
            frequencyList.add((double) i / effectiveResults.size());
        }
        conclusion.put("frequency_list", frequencyList);
        // conclusion.put("frequency_with_tick", ""); // 因为保存了 tick_list, 统一不再考虑保存 frequency_with_tick 的zip列表

        DataFrame<Double> dfTemp = new DataFrame<>();
        dfTemp.add("temp", frequencyList);
        dfTemp = dfTemp.cumsum();
        List<Double> cdfList = dfTemp.col(0);
        conclusion.put("cdf_list", cdfList); // 累计概率密度函数
        // conclusion.put("cdf_with_tick", ""); // 同理不再考虑保存
        return conclusion;
    }

    public static Map<String, Object> simpleStatAnalyzeByCountsList(List<Long> countList,
                                                                    List<Double> tickList,
                                                                    Double referenceValue,
                                                                    List<Double> smallLargeThreshold,
                                                                    boolean calcVirtualGeometryMean) {
        // 不传递 effectiveList 的重载方法, 设定为null即可.  用计数列表时,一般调用此接口
        return simpleStatAnalyzeByCountsList(countList, tickList, null, referenceValue, smallLargeThreshold,
                calcVirtualGeometryMean);
    }

    /**
     * //@noti: 相当于 用  valueList,bins,effectiveValueRange 计算出来countList和tickList, 然后多保存几个相关字段
     * //@noti: tickList.subList(1, tickList.size()),  纯计算出来的tickList是比bins多1的, 这里用区间上限代表区间值
     * 参照 simpleStatAnalyzeByCountsList 是给定了计数List,
     * 本方法 给定原始值的列表, 然后依据给定的 tickList, 计算出来 countList, 然后调用之!.
     * 同样, 保存时保存了 countList,tickList, 没有保存 原始的 valueList.
     * 本方法典型用于 离散涨跌幅列表, 分区间的分析.
     * 在分区间时, 值为 小tick<=value < 本tick.
     * <p>
     * noti: 常态需要给定有效值区间及bins数量, 才能计算tickList!.   也可直接给定 tickList, 参考重载方法
     *
     * @param valueList
     * @param referenceValue
     * @param smallLargeThreshold
     * @param calcVirtualGeometryMean
     * @return
     */
    public static Map<String, Object> simpleStatAnalyzeByValueList(List<Double> valueList,
                                                                   int bins,
                                                                   List<Double> effectiveValueRange,
                                                                   Double referenceValue,
                                                                   List<Double> smallLargeThreshold,
                                                                   boolean calcVirtualGeometryMean) {
        List<Double> outliers = new ArrayList<>();
        List<Double> effectiveResults = new ArrayList<>();
        List<Long> countList = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            countList.add(0L);
            // 数量记录初始化
        }
        // @noti: 因为浮点数误差,double在5位小数前基本都能准确表示,, 计算perRangeWidth时,保留5位, 能够使得tick足够准确,
        // 同理, 在面对恰好为 tick的值时, 也不会由于浮点数误差, 导致tick无法依照意愿分配到 前一或者后一tick. 能够在速度和准确取得平衡
        double perRangeWidth = NumberUtil.round((effectiveValueRange.get(1) - effectiveValueRange.get(0)) / bins, 5)
                .doubleValue();
        List<Double> tickList = getTickListByBinsAndEffectiveValueRange(effectiveValueRange, bins, perRangeWidth);
        for (int i = 0; i < valueList.size(); i++) {
            Double value = valueList.get(i);
            if (value < effectiveValueRange.get(0) || value > effectiveValueRange.get(1)) {
                outliers.add(value);
                continue;
            }
            effectiveResults.add(value);
            if (value.equals(effectiveValueRange.get(0))) {
                // 常规是前包后不包. 恰好等于最大限制的, 放在最后一个bin
                // @update: 为了适用分时tick, 将此修改为 前不包,后包. 某个tick值代表不大于的所有值. 最小值放入第一个tick
                Long count = countList.get(0);
                countList.set(0, count + 1);
                continue;
            }
            int index = (int) Math.ceil((NumberUtil.round((value - effectiveValueRange.get(0)) / perRangeWidth, 5)
                    .doubleValue())) - 1; // 注意逻辑还是比较绕, 基本上不能少
//            Console.log(value, effectiveValueRange.get(0), perRangeWidth,
//                    NumberUtil.round((value - effectiveValueRange.get(0)) / perRangeWidth, 5)
//                            .doubleValue(), index);

            Long count = countList.get(index);
            countList.set(index, count + 1);
        }

        if (effectiveResults.size() <= 0) {
            return null; // 没有有效数据, 不记录
        }
        HashMap<String, Object> conclusion = new HashMap<>();
        // 直接调用 countList 方法, 拿到结果;  已经进行null判定, 不可能为null, 直接putAll
        conclusion.putAll(simpleStatAnalyzeByCountsList(countList, tickList.subList(1, tickList.size()),
                effectiveResults, // 传递原始数据
                referenceValue,
                smallLargeThreshold,
                calcVirtualGeometryMean));
        // 使用 valueList , 还需要添加上, 其他一些字段!!.
        conclusion.put("total_counts", valueList.size()); // int
        conclusion.put("outliers_counts", outliers.size()); // int
        conclusion.put("outliers_count_percent", (double) outliers.size() / valueList.size()); // double
        conclusion.put("effective_count_percent",
                (double) effectiveResults.size() / valueList.size()); // double

        conclusion.put("effective_value_range", effectiveValueRange); // bins已有.  此为新增
        return conclusion;
    }

    /**
     * // 将以上两方法解析结果, 转换为 多列,仅仅1行的 df.
     * // 当需要添加其他记录字段时,  只需要将此返回值, 添加新的 "列"即可
     * // df = ConvertSimpleStatAnalyzeResultToDF()
     * // df.add("新的字段名", Arrays.asList("对应的字段值"));
     *
     * @param simpleStatAnalyzeResult
     * @return
     */
    public static DataFrame<Object> ConvertSimpleStatAnalyzeResultToDF(Map<String, Object> simpleStatAnalyzeResult) {
        if (simpleStatAnalyzeResult == null) {
            return null;
        }

        DataFrame<Object> dfSaved = new DataFrame<>();
        List<Object> row = new ArrayList<>();
        for (String key : simpleStatAnalyzeResult.keySet()) {
            // reference_compare_counts ArrayList<Integer> 3
            // reference_compare_counts_percent ArrayList<Double>  3
            // samlllarge_compare_counts  ArrayList<Integer> 3
            // samlllarge_compare_counts_percent  ArrayList<Double>  3
            // 当key为以上四个, 则需要对字段进行 0,1,2 的拆分, 值也进行拆分. 总计分为12个字段. 方便数据库直接查询.
            if ("reference_compare_counts".equals(key) || "samlllarge_compare_counts".equals(key)) {
                // 3 个Integer 的AL, 这里1拆为3; --> 0,1,2
                ArrayList<Integer> tempList = (ArrayList<Integer>) simpleStatAnalyzeResult.get(key);
                for (int i = 0; i < 3; i++) {
                    String fieldName = key + "_" + i;
                    dfSaved.add(fieldName); // 添加列名
                    row.add(tempList.get(i)); // 添加行, 对应列的值
                }
                continue;
            }
            // 同样为了应对数据表的字段 percnet..
            if ("reference_compare_counts_percent".equals(key) || "samlllarge_compare_counts_percent".equals(key)) {
                // 3 个Double 的AL, 这里1拆为3; --> 0,1,2
                ArrayList<Double> tempList = (ArrayList<Double>) simpleStatAnalyzeResult.get(key);
                for (int i = 0; i < 3; i++) {
                    String fieldName = key + "_" + i;
                    dfSaved.add(fieldName);
                    row.add(tempList.get(i));
                }
                continue;
            }

            dfSaved.add(key);
            Object value = simpleStatAnalyzeResult.get(key);
            // Object 总体分为 ArrayList,List, List<List>, int, double 等.
            // 列表性结果, 则转换为 json, 字符串, int,double (包装类) 等不转换
            if (value instanceof List) {
                row.add(JSONUtil.toJsonPrettyStr(value)); // 好看字符串
            } else {
                // 此时 有可能为Double.NaN, 将sql错误, 修复一下
                if (value instanceof Double) {
                    // 注意 , 不能用 ==; 需要用 此静态方法, 判定是否为 NaN
                    if (Double.compare(Double.NaN, (Double) value) == 0) {
                        row.add(null); // 将NAN替换为null
                    } else {
                        row.add(value);  // @bugfix: 又特么是这里else忘了写. 这里就不优化了, 逻辑清晰
                    }
                } else {
                    row.add(value);
                }
            }
        }
        dfSaved.append(row); // 加入行
        return dfSaved;
    }


    /**
     * 一个double列表的 6项基本统计量, 使用 构建临时df 的方法计算
     *
     * @param effectiveResults
     * @param conclusion
     */
    public static void baseStatValueByDF(List<Double> effectiveResults, HashMap<String, Object> conclusion) {
        DataFrame<Double> dfEffectiveResults = new DataFrame<>(); // 基本统计量, 调用df的方法, 构建临时的df
        dfEffectiveResults.add("value", effectiveResults);
        conclusion.put("mean", dfEffectiveResults.mean().get(0, 0));
        conclusion.put("std", dfEffectiveResults.stddev().get(0, 0));
        conclusion.put("min", dfEffectiveResults.min().get(0, 0));
        conclusion.put("max", dfEffectiveResults.max().get(0, 0));
        conclusion.put("skew", dfEffectiveResults.skew().get(0, 0));
        conclusion.put("kurt", dfEffectiveResults.kurt().get(0, 0));
    }

}
