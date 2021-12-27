package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.utils.log.LogUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;

/**
 * description: 策略抽象类. 子类只需要实现 startCore() 方法. 策略核心逻辑
 *
 * @author: admin
 * @date: 2021/12/26/026-03:19:41
 */
@Data
@NoArgsConstructor
public abstract class Strategy {
    private String strategyName; // 策略名称, 线程同名
    private List<StockBean> stockPool; // 股票池. 使用东方财富股票代码

    public Strategy(String strategyName) {
        this.strategyName = strategyName;
        this.stockPool = initStockPool(); // 构建器自动初始化股票池!
    }

    /**
     * 策略开始处理执行. 核心实现
     */
    protected abstract void startCore() throws Exception;

    /**
     * 每个策略, 需要首先获取自身股票池
     */
    protected abstract List<StockBean> initStockPool();

    /**
     * 选股方法. 通常需要加上各大指数, 最终将构建股票池
     */
    protected abstract List<String> stockSelect() throws Exception;

    /**
     * 针对 buy 订单check逻辑. 检测成交是否完成等
     */
    protected abstract void checkBuyOrder(Order order, List<JSONObject> responses, String orderType);

    /**
     * 针对 sell 订单check逻辑. 检测成交是否完成等
     */
    protected abstract void checkSellOrder(Order order, List<JSONObject> responses, String orderType);

    /**
     * 针对 其余类型 订单check逻辑.较少 检测成交是否完成等
     */
    protected abstract void checkOtherOrder(Order order, List<JSONObject> responses, String orderType);

    /**
     * check. 默认实现为简单分发为3个抽象方法
     *
     * @param order
     * @param responses
     * @param orderType
     */
    public void checkOrder(Order order, List<JSONObject> responses, String orderType) {
        if ("buy".equals(orderType)) {
            checkBuyOrder(order, responses, orderType);
        } else if ("sell".equals(orderType)) {
            checkSellOrder(order, responses, orderType);
        } else {
            checkOtherOrder(order, responses, orderType);
        }
    }

    /**
     * 默认实现即 新建守护线程执行核心逻辑
     */
    public void startDealWith() {
        Thread strategyTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                startCore();
            }
        });
        strategyTask.setDaemon(true);
        strategyTask.setPriority(Thread.MAX_PRIORITY);
        strategyTask.setName(strategyName);
        strategyTask.start();
        log.warn("start: {} 开始执行策略生成订单...", strategyName);
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    private static final Log log = LogUtils.getLogger();

}
