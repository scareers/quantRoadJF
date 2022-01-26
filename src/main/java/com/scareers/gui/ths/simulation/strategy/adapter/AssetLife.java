package com.scareers.gui.ths.simulation.strategy.adapter;

import com.scareers.datasource.eastmoney.SecurityBeanEm;

/**
 * description: 对于单只股票(需要卖,或者需要买入,仅限于股票), 记录整个Trader周期, 所有数据.
 * 例如 每分钟tick, 若发生买卖点,相关的 决定仓位的参数值,和仓位.
 * 例如 卖点列表
 *
 * @author: admin
 * @date: 2022/1/26/026-14:22:34
 */
public class AssetLife {
    // 静态属性
    // 1.默认仓位计算参数. 实际每个Asset的对应参数可根据这些参数做一些变化
    // 1.1. 低买默认参数
    public static final double tickGapDefault = 0.005; // 分布tick间隔, 千分之5
    public static final double positionUpperLimitDefault = 1.4; // 默认最大仓位比例. 1 / 低买股票池数量 * 1.4, 即平均仓位的仓位倍率
    public static final double positionCalcKeyArgsOfCdfDefault = 1.4; // 仓位计算时,需要增大比例, 才可能最终达成 100%仓位
    public static final double execLowBuyThresholdDefault = +0.005; // 只有价格低于此值(比例) 才可能执行低买
    public static final int continuousFallTickCountThresholdDefault = 1; // 分时图此前至少应当下跌多少,本分钟大概率上升, 才低买

    // 1.2. 高卖默认参数
    public static final double positionCalcKeyArgsOfCdfHighSell = 1.4; // 高卖仓位cdf计算 倍率
    public static final double execHighSellThreshold = -0.02; // 只有价格高于此才可能高卖
    public static final double continuousRaiseTickCountThreshold = 1; // fs连续上升n分钟以上,本分钟下降, 才可能高卖

    // 1.2. 指数当时tick涨跌幅加成倍率
    public static final double indexBelongThatTimePriceEnhanceArgLowBuy = 1; // fs连续上升n分钟以上,本分钟下降, 才可能高卖
    public static final double indexBelongThatTimePriceEnhanceArgHighSell = 1; // fs连续上升n分钟以上,本分钟下降, 才可能高卖

    // FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay.calcEquivalenceCdfUsePriceOfLowBuy
    // FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay
    //                                // stock_code,market,time_tick,price,vol,bs
    //                                .calcEquivalenceCdfUsePriceOfHighSell(newPercent, indexPricePercentThatTime,
    //                                        indexBelongThatTimePriceEnhanceArgHighSell);

    // lowPrice - indexBelongThatTimePriceEnhanceArgLowBuy * indexPriceThatTime; // @v2
    // highPrice - indexBelongThatTimePriceEnhanceArgHighSell * indexPriceThatTime; // @v2


    // 基本实例属性
    String stockCodeSimpe;
    SecurityBeanEm stockBean;

    // 仓位计算参数

}
