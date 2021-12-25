package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;
import com.scareers.utils.log.LogUtils;

import java.util.List;


/**
 * description: 虚拟的策略, 随机生成订单, 放入队列执行. check 逻辑也相同
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
public class DummyStrategy extends Strategy {
    private static final Log log = LogUtils.getLogger();

    @Override
    protected void checkBuyOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected void checkSellOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected void checkOtherOrder(Order order, List<JSONObject> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toStringPretty());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行失败");
        }
        Trader.ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(Order.LifePointStatus.FINISH, "订单完成");
        Trader.ordersFinished.put(order, responses); // 先删除, 后添加
    }

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
        log.warn("start init stockPool: 开始初始化股票池...");
        List<StockBean> res = StockPoolForFSTransaction.stockPoolTest();
        log.warn("finish init stockPool: 完成初始化股票池...");
        return res;
    }

    public DummyStrategy(String strategyName) {
        super(strategyName);
    }
}
