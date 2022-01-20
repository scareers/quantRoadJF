package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;

import java.util.List;

/**
 * 策略适配器, 专注于 策略核心逻辑. 将被 Strategy 对应方法调用. 实现类通常带有属性 Strategy
 *
 * @key1 不包含股票池初始化相关
 */
public interface StrategyAdapter {
    void buyDecision() throws Exception;

    void sellDecision() throws Exception;

    void checkBuyOrder(Order order, List<Response> responses, String orderType);

    void checkSellOrder(Order order, List<Response> responses, String orderType);

    void checkOtherOrder(Order order, List<Response> responses, String orderType);
}
