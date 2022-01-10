package com.scareers.gui.ths.simulation.trader;

import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import lombok.SneakyThrows;

import java.util.List;

/**
 * 订单执行器! 将死循环线程安全优先级队列, 执行订单, 并获得响应!
 * 订单周期变化: wait_execute --> executing --> finish_execute
 * 并放入 成交监控队列
 *
 * @return
 */
public class OrderExecutor {
    private static final Log log = LogUtil.getLogger();

    private Trader trader;

    public OrderExecutor(Trader trader) {
        this.trader = trader;
    }

    public void start() {
        Thread orderExecuteTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    Order order = trader.getOrdersWaitForExecution().take(); // 最高优先级订单, 将可能被阻塞
                    log.info("order start execute: 开始执行订单: {} [{}] --> {}:{}",
                            order.getRawOrderId(), order.getPriority(), order.getOrderType(), order.getParams());
                    order.addLifePoint(Order.LifePointStatus.EXECUTING, "开始执行订单");
                    List<JSONObject> responses = trader.execOrderUtilSuccess(order);  // 响应列表, 常态仅一个元素. retrying才会多个
                    order.addLifePoint(Order.LifePointStatus.FINISH_EXECUTE, "执行订单完成");
                    order.addLifePoint(Order.LifePointStatus.WAIT_CHECK_TRANSACTION_STATUS, "订单进入check队列, " +
                            "等待check完成");
                    trader.getOrdersWaitForCheckTransactionStatusMap().put(order, responses);
                }
            }
        });
        orderExecuteTask.setDaemon(true);
        orderExecuteTask.setPriority(Thread.MAX_PRIORITY);
        orderExecuteTask.setName("orderExecutor");
        orderExecuteTask.start();
        log.warn("start: orderExecutor 开始按优先级执行订单...");
    }

    /**
     * // @key3: 核心重发订单方法. 将使用深拷贝方式, 对订单类型和参数 不进行改变!
     *
     * @param order
     */
    public void reSendOrder(Order order, Long priority) throws Exception {
        Order newOrder = order.forResend();
        if (priority != null) { // 默认实现优先级将-1,增加1.  可直接传递
            newOrder.setPriority(priority);
        }
        trader.putOrderToWaitExecute(newOrder);
    }

    public void reSendOrder(Order order) throws Exception {
        reSendOrder(order, null);
    }
}

