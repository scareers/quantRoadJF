package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;
import joinery.DataFrame;

import static com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher.fsTransactionDatas;

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
    public void startCore() throws Exception {
        // 低买高卖
    }
}
