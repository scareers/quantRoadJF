package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.SneakyThrows;

/**
 * description: 策略抽象类. 子类只需要实现 startCore() 方法. 策略核心逻辑
 *
 * @author: admin
 * @date: 2021/12/26/026-03:19:41
 */
public abstract class Strategy {
    private String strategyName; // 策略名称, 线程同名

    public Strategy(String strategyName) {
        this.strategyName = strategyName;
    }

    public Strategy() {
    }

    /**
     * 策略开始处理执行. 核心实现
     */
    protected abstract void startCore() throws Exception;

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
