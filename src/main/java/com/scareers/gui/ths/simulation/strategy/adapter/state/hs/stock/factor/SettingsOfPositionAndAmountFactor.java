package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.factor;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/21/021-22:18:57
 */
public class SettingsOfPositionAndAmountFactor {
    public static final String factorName = "PositionAndAmountFactor";
    public static final String nameCn = "买卖仓位决策及实际订单数量决定算法";
    public static final String description = "根据当前状态,决定若发生买卖订单,将使用的仓位相关信息,以及实际订单数量;即使当前非买卖点,依然计算";

    /*
    设置项
     */
    public static double tickGap = 0.005;
    public static double cdfRateForPosition = 1.5; // 高卖cdf倍率
}
