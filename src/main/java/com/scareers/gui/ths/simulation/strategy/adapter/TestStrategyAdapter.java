package com.scareers.gui.ths.simulation.strategy.adapter;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.TestStrategy;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.utils.log.LogUtil;
import eu.verdelhan.ta4j.Strategy;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/20/020-10:18:18
 */
public class TestStrategyAdapter implements StrategyAdapter {
    private static final Log log = LogUtil.getLogger();
    TestStrategy strategy;
    Trader trader;

    public TestStrategyAdapter(TestStrategy strategy) {
        this.strategy = strategy;
        this.trader = strategy.getTrader();
    }

    @Override
    public void buyDecision() throws Exception {
        int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
        Thread.sleep(sleep * 2000);
        Order order = null;
        int type = RandomUtil.randomInt(22);
        if (type < 8) {
            order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGHEST);
        } else if (type < 16) {
            order = OrderFactory.generateSellOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGH);
        } else if (type < 18) {
            order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_HIGH);
        } else if (type < 20) {
            order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_HIGH);
        } else {
            order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
        }
        strategy.getTrader().putOrderToWaitExecute(order);
    }

    @Override
    public void sellDecision() throws Exception {

    }

    @Override
    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkSellOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
//            log.warn("待执行订单数量: {}", trader.getOrdersWaitForExecution().size());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toString());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败");
        }
        trader.successFinishOrder(order, responses);
    }
}
