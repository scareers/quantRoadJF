package com.scareers.gui.rabbitmq.order;

import java.util.Map;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/23/023-19:37:36
 */
public class BuyOrder extends Order {
    public BuyOrder(Map<String, Object> params) {
        super("buy", params);
    }
}
