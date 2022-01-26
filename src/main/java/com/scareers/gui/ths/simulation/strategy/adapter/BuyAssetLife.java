package com.scareers.gui.ths.simulation.strategy.adapter;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/26/026-15:10:30
 */
public class BuyAssetLife {
    // 静态属性
    // 1.默认仓位计算参数. 实际每个Asset的对应参数可根据这些参数做一些变化
    public static final double tickGapDefault = 0.005; // 分布tick间隔, 千分之5
    // 1.1. 低买默认参数
    public static final double positionUpperLimitDefault = 1.4; // 默认最大仓位比例. 1 / 低买股票池数量 * 1.4, 即平均仓位的仓位倍率
    public static final double positionCalcKeyArgsOfCdfDefault = 1.4; // 仓位计算时,需要增大比例, 才可能最终达成 100%仓位
    public static final double execLowBuyThresholdDefault = +0.005; // 只有价格低于此值(比例) 才可能执行低买
    public static final int continuousFallTickCountThresholdDefault = 1; // 分时图此前至少应当下跌多少,本分钟大概率上升, 才低买

    // 1.2. 指数当时tick涨跌幅加成倍率
    // 1.2.1. 说明
    // 低买等价价格最早实现: lowPrice - indexBelongThatTimePriceEnhanceArgLowBuy * indexPriceThatTime; // @v2
    // --> @see: FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay.calcEquivalenceCdfUsePriceOfLowBuy
    // --> 低买: 当倍率和当时大盘指数+时, 意味着大盘很好, 此时等价价格较小, 因低买分布tick是从大到小的, 这意味着等价价格右移, 即增大低买仓位,快速买入,以免来不及
    // --> 因此调节倍率为 负数, 可将大盘影响反向反馈
    public static final double indexBelongThatTimePriceEnhanceArgLowBuy = 0.1; // fs连续上升n分钟以上,本分钟下降, 才可能高卖

}
