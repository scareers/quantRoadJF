package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.factor;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.StockStateHs;
import com.scareers.gui.ths.simulation.trader.Trader;


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
    public HsState influence(HsState state0) {
        StockStateHs state = state0.getStockStateHs();
        // 1. 此时高卖分布的tick, pdf, cdf已设置好, 被更前端的因子影响. 直接使用 currentPricePercent进行计算
        // 2. 设置cdf中的 具体概率
        state.setCdfProbabilityOfCurrentPricePercent(
                StockStateHs.cdfHs(
                        state.getTicksOfHighSell(), state.getCdfListOfHighSell(),
                        state.getChgPToPre2Close() // 当前涨跌幅,(相对于前2收)
                ));
        state.setCdfRateForPosition(SettingsOfPositionAndAmountFactor.cdfRateForPosition);
        state.setTotalPositionNormalized(
                Math.min(1.0, state.getCdfRateForPosition() * state.getCdfProbabilityOfCurrentPricePercent()));

        if (!state.getSellPointCurrent()) {
            return state0; // 直接返回
        }
        double shouldSellAmountTotal = state.getTotalPositionNormalized() * state.getAmountsTotalYc().doubleValue();

        if (state.getActualAmountHighSelled() < shouldSellAmountTotal) {

            // 三项数据: 此刻卖点应当总卖出 / 原总持仓  --  [早已经成功卖出]
            int amount = (int) (NumberUtil
                    .round((shouldSellAmountTotal - state.getActualAmountHighSelled()) / 100, 0).doubleValue()) * 100;

            // 四舍五入价格. 100 整数
            if (amount + state.getActualAmountHighSelled() > state.getAmountsTotalYc()) {
                amount = (state.getAmountsTotalYc() - state.getActualAmountHighSelled()) / 100 * 100;
            }

            if (amount < 100) {
//                        log.warn("sell decision: 卖点出现,但折算卖出数量<100,不执行卖出, {} -> {}/{} ; already [{}], actual [{}]",
//                                stock,
//                                shouldSellAmountTotal,
//                                amountsTotal,
//                                sellAlready, amount);
            } else {
                // 卖出
                log.warn("sell decision: 卖点出现, {} -> 理论[{}] 总仓[{}] 此前已卖参考[{}] 可用参考[{}] 订单实际[{}]",
                        state.getStockCode(),
                        shouldSellAmountTotal,
                        state.getAmountsTotalYc(),
                        state.getActualAmountHighSelled(),
                        state.getAvailableAmountForHs(),
                        amount
                );

                Double price = null;
                String nowStr = DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN);
                boolean flag = nowStr.compareTo("09:25:00") > 0 && nowStr.compareTo("09:30:00") < 0;
                if (flag) {
                    price = SecurityPool.getPriceLimitMap().get(state.getStockCode()).get(1); // 跌停价
                }
                Order order = OrderFactory
                        .generateSellOrderQuick(state.getStockCode(), amount, price, Order.PRIORITY_HIGH);
                if (flag) {
                    order.setAfterAuctionFirst(); // 设置为竞价后首个订单
                }
                Trader.getInstance().putOrderToWaitExecute(order);
                // todo: 这里一旦生成卖单, 将视为全部成交, 加入到已经卖出的部分
                // 若最终成交失败, 2分钟后check将失败, 订单离开checking队列, 可用数量将采用AS最新数据及时更新.
                LowBuyHighSellStrategyAdapter.actualAmountHighSelledMap
                        .put(state.getStockCode(), amount + state.getActualAmountHighSelled());
            }
        } else { //  新卖点,但没必要卖出更多.(多因为当前价格已经比上一次低, 导致仓位更低)
//                    log.warn("sell decision: 卖点出现,但早已卖出更多仓位,不执行卖出. {} -> {}/{} ; already [{}]", stock,
//                            shouldSellAmountTotal, amountsTotal, sellAlready);
        }


        return state0;
    }


}
