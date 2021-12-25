package com.scareers.gui.ths.simulation.strategy;

import com.scareers.datasource.eastmoney.fstransaction.StockBean;

import java.util.List;

/**
 * description: 低买高卖
 *
 * @author: admin
 * @date: 2021/12/22/022-20:54:25
 */
public class LowBuyHighSellStrategy extends Strategy {
    public LowBuyHighSellStrategy(String strategyName) {
        super(strategyName);
    }

    @Override
    protected void startCore() throws Exception {
        // 低买高卖
    }

    @Override
    protected List<StockBean> initStockPool() {
        // return StockPoolForFSTransaction.stockPoolTest();
        return null;
    }
}
