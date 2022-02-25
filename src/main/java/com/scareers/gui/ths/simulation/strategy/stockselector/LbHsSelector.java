package com.scareers.gui.ths.simulation.strategy.stockselector;

import java.util.List;

/**
 * description: 低买高卖类 股票选择器抽象类
 * 除实现 StockSelect接口以外,
 * 需要提供的核心属性/机制如下:
 * 1.低买tick列表
 * 2.低买概率列表 -- 与tick对应
 * 3.高卖tick列表
 * 4.高卖概率列表 -- 与tick对应
 * 5.与 两大分布相关的其他方法
 *
 * @author: admin
 * @date: 2022/2/19/019-18:53:16
 */
public abstract class LbHsSelector implements StockSelect {


    protected List<Double> ticksOfLowBuy; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
    protected List<Double> weightsOfLowBuy; // 44数据
    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected List<Double> weightsOfHighSell; // 88数据
    protected List<Double> cdfOfLowBuy;
    protected List<Double> cdfOfHighSell;

    public abstract List<Double> getTicksOfLowBuy();
    public abstract List<Double> getTicksOfHighSell();
    public abstract List<Double> getWeightsOfLowBuy();
    public abstract List<Double> getWeightsOfHighSell();
    public abstract List<Double> getCdfOfLowBuy();
    public abstract List<Double> getCdfOfHighSell();
}
