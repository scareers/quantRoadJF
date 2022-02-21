package com.scareers.gui.ths.simulation.strategy.adapter.factor.position;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;

import java.util.List;


/**
 * description: 高卖: 仓位以及最终订单数量 决策算法
 *
 * @author: admin
 * @date: 2022/2/21/021-22:16:41
 */
public class PositionAndAmountFactorHs extends HsFactor {
    public PositionAndAmountFactorHs() {
        super(SettingsOfPositionAndAmountFactor.factorName, SettingsOfPositionAndAmountFactor.nameCn,
                SettingsOfPositionAndAmountFactor.description);
    }

    @Override
    public HsState influence(HsState state) {
        // 1. 此时高卖分布的tick, pdf, cdf已设置好, 被更前端的因子影响. 直接使用 currentPricePercent进行计算

        Double cdfProbabilityOfCurrentPricePercent = virtualCdfAsPositionForHighSell(
                state.getTicksOfHighSell(), state.getWeightsOfHighSell(),
                state.getNewPricePercentToPre2Close(), // 当前涨跌幅,(相对于前2收)
                state.getTickGap());


        return state;
    }

    /**
     * 原版的cdf算法, 直接搬运.
     * todo: 直接使用 cdf分布计算更快 !
     *
     * @param valuePercentOfLow
     * @param weightsOfLow
     * @param value
     * @param tickGap
     * @return
     */
    public static Double virtualCdfAsPositionForHighSell(List<Double> valuePercentOfLow, List<Double> weightsOfLow,
                                                         Double value, Double tickGap) {

        double total = 0.0;
        for (int i = 0; i < valuePercentOfLow.size(); i++) {
            Double tick = valuePercentOfLow.get(i);
            if (tick < value) { // 前面的全部加入. 知道 本value在的 区间tick内
                total += weightsOfLow.get(i); // 相等时也需要加入, 因此先+
                continue; // 继续往后
            }
            // 然后还要加入一部分..  直到>
            if (i == 0) {
                break;  // 会出现索引越界,注意
            }
            Double tickPre = valuePercentOfLow.get(i - 1);
            //假设单区间内, 概率也平均叠加, 因此, 应当加入的部分是: 0到终点处概率,  * tick距离开始的百分比
            total += weightsOfLow.get(i - 1) + (weightsOfLow.get(i) - weightsOfLow
                    .get(i - 1)) * (Math.abs((value - tickPre)) / tickGap);
            break; //一次即可跳出
        }
//        double sum = sumOfListNumberUseLoop(weightsOfLow);
//        double res = total / sum;
////        Console.com.scareers.log(res);
//        return res; // 求和可能了多次
        return Math.min(total, 1.0);
    }
}
