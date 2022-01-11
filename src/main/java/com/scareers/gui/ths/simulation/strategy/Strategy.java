package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;

/**
 * description:
 * 策略抽象类. 子类只需要实现 startCore() 方法. 策略核心逻辑
 *
 * @author: admin
 * @date: 2021/12/26/026-03:19:41
 */
@Data
@NoArgsConstructor
public abstract class Strategy {
    protected String strategyName; // 策略名称, 线程同名
    protected List<SecurityBeanEm> stockPool; // 股票池. 使用东方财富股票代码

    protected Strategy(String strategyName) throws Exception {
        this.strategyName = strategyName;
        this.stockPool = initStockPool(); // 构建器自动初始化股票池!
    }

    /**
     * 策略开始处理执行. 策略逻辑.  默认实现为 死循环调用 买卖决策方法
     * 默认先尝试卖, 回笼资金, 后买. 影响不大.
     */
    protected void startCore() throws Exception {
        while (true) {
            sellDecision();
            buyDecision();
        }
    }

    /**
     * 买卖决策
     *
     * @throws Exception
     */
    protected abstract void buyDecision() throws Exception;

    protected abstract void sellDecision() throws Exception;

    /**
     * 针对 buy 订单check逻辑. 检测成交是否完成等  // 处理三大类型淡订单
     */
    protected abstract void checkBuyOrder(Order order, List<Response> responses, String orderType);

    /**
     * 针对 sell 订单check逻辑. 检测成交是否完成等
     */
    protected abstract void checkSellOrder(Order order, List<Response> responses, String orderType);

    /**
     * 针对 其余类型 订单check逻辑.较少 检测成交是否完成等
     */
    protected abstract void checkOtherOrder(Order order, List<Response> responses, String orderType);

    /**
     * 每个策略, 需要首先获取自身股票池, 一般将调用 stockSelect(), initYesterdayHolds(), + 两大指数
     */
    protected abstract List<SecurityBeanEm> initStockPool() throws Exception;

    /**
     * 选股方法. 通常需要加上各大指数, 最终更新到股票池.
     * 通常还需要等待获取昨日收盘后(今日盘前)持仓股票,进一步更新股票池  initYesterdayHolds(),
     * 但该方法通常需要等待第一次获取账户信息完成(数据库无数据时), 因此
     */
    protected abstract List<String> stockSelect() throws Exception;

    /**
     * 获取今日开盘前已经持仓股票列表. 将账号初始状态和昨日已经持仓状态, 保存到数据库.
     * 并将昨日持仓股票列表 加入 stockPool, 以便fs抓取!
     * 注意通常需要 waitUtil(AccountStates::alreadyInitialized, 120 * 1000, 100, "首次账户资金状态刷新完成");
     * 后执行. 或者需要数据库已经保存有 昨日收盘后持仓(尽量等 00:00证券公司完全刷新后,而非简单昨日收盘后)
     * 将昨日持仓更新到股票池. 将昨日收盘持仓和资金信息, 更新到属性
     * @return
     */
    protected abstract List<String> initYesterdayHolds() throws Exception;

    /**
     * check. 默认实现为简单分发为3个抽象方法
     *
     * @param order
     * @param responses
     * @param orderType
     */
    public void checkOrder(Order order, List<Response> responses, String orderType) {
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

    private static final Log log = LogUtil.getLogger();

}
