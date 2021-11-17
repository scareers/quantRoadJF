package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.calcVirtualGeometryMeanRecursion;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.getCompareCountsAndPercentList;

/**
 * description: 所有k线相关分析, 十分常用的帮助函数
 * 不再用Integer, 统一用Long.
 *
 * @author: admin
 * @date: 2021/11/17/017-9:03
 */
public class KeyFuncOfKlineCommons {
    public static void main(String[] args) {
        Console.log();
    }

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
     * @return Map<String, Object>  统计的项目名称 : 结果;  结果用Object, 可用 hutool.  JSONUtil 解析
     */
    public static Map<String, Object> simpleStatAnalyzeByCountsList(List<Long> countList,
                                                                    List<Double> tickList,
                                                                    Double referenceValue,
                                                                    List<Double> smallLargeThreshold,
                                                                    boolean calcVirtualGeometryMean) {
        Assert.isTrue(countList.size() == tickList.size());
        List<Double> effectiveResults = new ArrayList<>(); // 构造模拟的全数据列表.
        for (int i = 0; i < countList.size(); i++) {
            Long count = countList.get(i);
            Double value = tickList.get(i);
            for (int j = 0; j < count; j++) {
                effectiveResults.add(value);
            }
        }
        if (effectiveResults.size() <= 0) {
            return null;
        }

        HashMap<String, Object> conclusion = new HashMap<>();
        conclusion.put("total_counts", effectiveResults.size()); // 总数

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
            ArrayList<Double> zeroCompareCountsPercent = getCompareCountsAndPercentList(
                    effectiveResults,
                    conclusion,
                    referenceCompareCounts,
                    ltReference,
                    eqReference,
                    gtReference, "reference_compare_counts");
            conclusion.put("reference_compare_counts_percent", zeroCompareCountsPercent); //AL
            conclusion.put("reference_value", smallLargeThreshold); // 紧密的参数还是保存一下, 同样可能为null; 原来并未保存 0.0
        }

        if (smallLargeThreshold != null) { // 同样也需要传递 小大值阈值二列表
            ArrayList<Integer> smallLargeCompareCounts = new ArrayList<>();
            int ltSmall = 0, betweenSmallLarge = 0, gtLarge = 0;
            for (Double i : effectiveResults) {
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
        dfTemp.append(frequencyList);
        dfTemp = dfTemp.cumsum();
        List<Double> cdfList = dfTemp.col(0);
        conclusion.put("cdf_list", cdfList); // 累计概率密度函数
        // conclusion.put("cdf_with_tick", ""); // 同理不再考虑保存
        return conclusion;
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
