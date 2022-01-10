package com.scareers.gui.ths.simulation.trader;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Objects;

/**
 * 订单执行器! 将死循环线程安全优先级队列, 执行订单, 并获得响应!
 * 订单周期变化: wait_execute --> executing --> finish_execute
 * 并放入 成交监控队列
 *
 * @author admin
 */
public class OrderExecutor {
    private static final Log log = LogUtil.getLogger();
    private static OrderExecutor INSTANCE;

    public static OrderExecutor getInstance(Trader trader) {
        if (INSTANCE == null) {
            INSTANCE = new OrderExecutor(trader);
        }
        return INSTANCE;
    }

    public static OrderExecutor getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

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
                    log.info("order start execute: 开始执行订单: {} [{}] --> {}:{}", order.getOrderType(),
                            order.getPriority(), order.getRawOrderId(), order.getParams());
                    order.addLifePoint(Order.LifePointStatus.EXECUTING, "executing: 开始执行订单");
                    List<Response> responses = trader.execOrderUtilSuccess(order);
                    order.addLifePoint(Order.LifePointStatus.FINISH_EXECUTE, "finish_execute: 执行订单完成");
                    order.addLifePoint(Order.LifePointStatus.WAIT_CHECK_TRANSACTION_STATUS,
                            "wait_check_transaction_status: 订单进入check队列,等待check完成");
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


}

