package com.scareers.keyfuncs;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.lang.WeightRandom.WeightObj;
import cn.hutool.core.util.RandomUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.utils.charts.ChartUtil.listOfDoubleAsLineChartSimple;

/**
 * description: Low, High等相关分布, 决定买卖点出现时, 的仓位.
 * // 使用模拟实验决定相关参数设置, 而非正面实现逻辑
 * ----------- 假设
 * 1.假定每种情况下, Low1/2/3 均出现一次, 限定最多每只股票 买入/卖出 3次?
 * <p>
 * ----------- 问题
 * 1.给定 Low1, Low2, Low3 分布,
 *
 * @author: admin
 * @date: 2021/11/25/025-9:51
 */
public class PositionByDistribution {
    public static void main(String[] args) throws IOException {
        WeightRandom<Double> distributionOfLow1 = getDistributionsOfLow1();
        for (int i = 0; i < 100; i++) {
            Console.log(distributionOfLow1.next());
        }
        List<Object> weightsOfLow1 = Arrays.asList(2., 4.0, 20., 15., 10., 5., 4., 3., 2., 0.5);
        List<Object> valuePercentOfLow1 = Arrays
                .asList(-0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1);
        listOfDoubleAsLineChartSimple(weightsOfLow1, false, null, valuePercentOfLow1);
    }

    public static WeightRandom<Double> getDistributionsOfLow1() {
        List<Double> valuePercentOfLow1 = Arrays
                .asList(-0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1);
        List<Double> weightsOfLow1 = Arrays.asList(2., 4.0, 20., 15., 10., 5., 4., 3., 2., 0.5); // 权重之和可以不是1, 互相成比例即可

        Assert.isTrue(valuePercentOfLow1.size() == weightsOfLow1.size());
        // 构建 WeightObj<Double> 列表. 以构建随机器

        List<WeightObj<Double>> weightObjs = new ArrayList<>();
        for (int i = 0; i < valuePercentOfLow1.size(); i++) {
            weightObjs.add(new WeightObj<>(valuePercentOfLow1.get(i), weightsOfLow1.get(i)));
        }
        return RandomUtil.weightRandom(weightObjs);
    }
}
