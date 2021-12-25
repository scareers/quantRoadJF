package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;

import java.util.List;


/**
 * description: 虚拟的策略, 随机生成订单, 放入队列执行
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
public class DummyStrategy extends Strategy {
    @Override
    protected void startCore() throws Exception {
        while (true) {
            int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
            Thread.sleep(sleep * 1000);
            Order order = null;
            int type = RandomUtil.randomInt(12);
            if (type < 3) {
                order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGHEST);
            } else if (type < 6) {
                order = OrderFactory.generateSellOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGH);
            } else if (type < 8) {
                order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_MEDIUM);
            } else if (type < 10) {
                order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_LOWEST);
            } else {
                order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
            }
            Trader.putOrderToWaitExecute(order);
        }
    }

    @Override
    protected List<StockBean> initStockPool() {
        return StockPoolForFSTransaction.stockPoolTest();
    }

    public DummyStrategy(String strategyName) {
        super(strategyName);
    }
}
