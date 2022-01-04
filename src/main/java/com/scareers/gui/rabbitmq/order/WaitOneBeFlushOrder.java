package com.scareers.gui.rabbitmq.order;

import java.util.Map;

/**
 * description: 等待某确定订单id 在交易软件中被刷新记录. 优先级高
 *
 * @author: admin
 * @date: 2021/12/23/023-19:37:36
 */
public class WaitOneBeFlushOrder extends Order {
    public static long DEFAULT_PRIORITY = Order.PRIORITY_HIGH;

    public WaitOneBeFlushOrder(Map<String, Object> params) {
        super("wait_one_exact_order_be_flushed", params); // 等待某订单被刷新.
        this.setPriority(DEFAULT_PRIORITY);
    }
}


