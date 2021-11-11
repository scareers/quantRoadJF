package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import java.util.Arrays;
import java.util.List;

/**
 * description: 本脚本, 依据截止今日的数据, 而决定明日买后日卖,或者后日卖大后日卖, 以此类推. 本脚本分析不同 形态集合下的 明后日分布
 * -- highArg /LowArg 决定出形态集,  然后对集合的未来2/3/4.. 天进行四项数据基本分析,并保存到数据库
 * 1.数据流程:  主程序全数据解析 -- FilterSimple简单筛选出几十万条 -- 本脚本, 对不同参数下, 0b1s/1b2s 等的分布研究结果
 *
 * @author: admin
 * @date: 2021/11/11  0011-8:06
 */
public class FormNameSetsOptimized {
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
    List<Integer> intTableList =

}
