package com.scareers.gui.ths.simulation.strategy;

import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;

import java.util.List;

/**
 * 策略适配器, 仅此5方法.方便查看核心逻辑
 *
 * @author admin
 */
public interface StrategyAdapter {
    void buyDecision() throws Exception;

    void sellDecision() throws Exception;

    /**
     * 针对 buy 订单check逻辑. 检测成交是否完成等  // 处理三大类型淡订单
     */
    void checkBuyOrder(Order order, List<Response> responses, String orderType);


    /**
     * 针对 sell 订单check逻辑. 检测成交是否完成等
     */
    void checkSellOrder(Order order, List<Response> responses, String orderType);


    /**
     * 针对 其余类型 订单check逻辑.较少 检测成交是否完成等
     */
    void checkOtherOrder(Order order, List<Response> responses, String orderType);
}
