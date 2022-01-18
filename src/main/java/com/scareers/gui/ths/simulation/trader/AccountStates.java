package com.scareers.gui.ths.simulation.trader;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.TraderUtil;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号状态监控类. 当前 5项数据
 *
 * @author admin
 * @noti 5项数据的刷新均不保证立即执行, 即使  Immediately 也仅仅是以0最高优先级放入待执行队列. 实际执行由待执行队列进行调度
 * @noti 静态属性的赋值, 实际由 check 程序完成, 因此, 本子系统应当后于 执行调度程序 和 check程序之后执行,
 * 且需要等待5项数据第一次更新, 以保证后期逻辑不会访问到 null值(或空Map/List)
 * @noti nineBaseFundsData线程安全, 可随意赋值修改. 其余4项  DataFrame<Object> 需要全量更新.
 * @noti 访问 DataFrame<Object> 时, 需要使用临时变量暂存, 即 dfo tempDf = 静态变量.
 * 即使静态变量在处理期间被更新, 依然不影响, 防止遍历时等bug
 * @impl 机制1: 将死循环遍历订单队列, 补齐不存在的状态api类型订单. 循环带sleep
 * 机制2: 某api若长时间未刷新数据, 则优先级可略提高, 以便先于同类执行.
 * 机制3: commonApiPriority 控制常态 账户状态刷新api 的优先级
 */
@Getter
@Setter
public class AccountStates {
    private static final Log log = LogUtil.getLogger();
    private static AccountStates INSTANCE; // 单例实现

    public static AccountStates getInstance(Trader trader, long flushInterval, long commonApiPriority,
                                            long priorityRaiseTimeThreshold,
                                            long priorityRaise) {
        if (INSTANCE == null) {
            INSTANCE = new AccountStates(trader, flushInterval, commonApiPriority, priorityRaiseTimeThreshold,
                    priorityRaise);
        }
        return INSTANCE;
    }

    public static AccountStates getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    public static final List<String> ORDER_TYPES = Arrays
            .asList("get_account_funds_info", "get_hold_stocks_info", "get_unsolds_not_yet",
                    "get_today_clinch_orders", "get_today_consign_orders"); // 常量,对应5项数据api

    // 实例属性
    public ConcurrentHashMap<String, Double> nineBaseFundsData = new ConcurrentHashMap<>(); // get_account_funds_info
    public DataFrame<Object> currentHolds = null; // get_hold_stocks_info // 持仓
    public DataFrame<Object> canCancels = null; // get_unsolds_not_yet 当前可撤, 即未成交
    public DataFrame<Object> todayClinchs = null; // get_today_clinch_orders 今日成交
    public DataFrame<Object> todayConsigns = null; // get_today_consign_orders 今日所有委托

    public Long nineBaseFundsDataFlushTimestamp = null; // 五大接口刷新时间戳! long, 实际被刷新后更新.
    public Long currentHoldsFlushTimestamp = null; // 均为 毫秒  System.currentTimeMillis()
    public Long canCancelsFlushTimestamp = null;
    public Long todayClinchsFlushTimestamp = null;
    public Long todayConsignsFlushTimestamp = null;

    // 核心trader, 以便访问其他组件
    private Trader trader;
    // 账户状态检测程序 刷新 sleep 间隔.
    private long flushInterval;
    // 常态情况(非第一次), 状态更新api的优先级. 5项api设置相等, 以便使得执行更加均匀     -- 常规优先级.
    private long commonApiPriority;
    // 某项数据超过多久未更新? 将提高一点优先级, 先于同类执行 , 单位 ms
    private long priorityRaiseTimeThreshold;
    // 当某一项数据长时间没有更新, 优先级变更为 commonApiPriority - priorityRaise, 提高优先级,先于同类执行.优先级提高程度尽量小.例如
    private long priorityRaise;

    private AccountStates(Trader trader, long flushInterval, long commonApiPriority, long priorityRaiseTimeThreshold,
                          long priorityRaise) {
        this.trader = trader;
        this.flushInterval = flushInterval;
        this.commonApiPriority = commonApiPriority; // Order.PRIORITY_MEDIUM; // 5000, 不高
        this.priorityRaiseTimeThreshold = priorityRaiseTimeThreshold;
        this.priorityRaise = priorityRaise;
    }

    /**
     * 等待已被第一次初始化, Map做size()检测, 其余做非null检测
     *
     * @return 第一次是否初始化完成.
     */
    public boolean alreadyInitialized() {
        return nineBaseFundsData
                .size() > 0 && currentHolds != null
                && canCancels != null && todayClinchs != null && todayConsigns != null;
    }


    /**
     * 刷新账户信息主逻辑,可控制不同种类更新频率等. 将订单放入队列(可不同优先级).
     * 当某项信息刷新时间间隔过久, 优先级将比常规提高2. 使得下一轮优先调用
     *
     * @impl 刷新5项数据, 对订单队列进行遍历, 若存在对应类型订单, 则跳过, 否则下单.
     * @noti 订单重发时(账户状态5订单), 队列中可能存在两个同类型订单, 但重发订单优先级更高1. 不算bug
     */
    public void startFlush() {
        Thread accoutStatesFlushTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    List<String> alreadyInQueue = new ArrayList<>();
                    for (Order order : trader.getOrdersWaitForExecution()) {
                        if (ORDER_TYPES.contains(order.getOrderType())) {
                            // 得到已在队列中的类型. 对其余类型进行补齐
                            alreadyInQueue.add(order.getOrderType());
                        }
                    }
                    for (String orderType : ORDER_TYPES) {
                        if (!alreadyInQueue.contains(orderType)) { // 不存在则添加
                            switch (orderType) {
                                // 第一次将以最高优先级刷新, 否则常态
                                case "get_account_funds_info":
                                    if (nineBaseFundsData.size() == 0) {
                                        flushNineBaseFundsDataImmediately();
                                    } else {
                                        if (System
                                                .currentTimeMillis() - nineBaseFundsDataFlushTimestamp > priorityRaiseTimeThreshold) {
                                            flushNineBaseFundsData(Math.max(commonApiPriority - priorityRaise, 0));
                                        }
                                        flushNineBaseFundsData(commonApiPriority);
                                    }
                                    break;
                                case "get_hold_stocks_info":
                                    if (currentHolds == null) {
                                        flushCurrentHoldsImmediately();
                                    } else {
                                        if (System
                                                .currentTimeMillis() - currentHoldsFlushTimestamp > priorityRaiseTimeThreshold) {
                                            flushCurrentHolds(Math.max(commonApiPriority - priorityRaise, 0));
                                        }
                                        flushCurrentHolds(commonApiPriority);
                                    }
                                    break;
                                case "get_unsolds_not_yet":
                                    if (canCancels == null) {
                                        flushCanCancelsImmediately();
                                    } else {
                                        if (System
                                                .currentTimeMillis() - canCancelsFlushTimestamp > priorityRaiseTimeThreshold) {
                                            flushCanCancels(Math.max(commonApiPriority - priorityRaise, 0));
                                        }
                                        flushCanCancels(commonApiPriority);
                                    }
                                    break;
                                case "get_today_clinch_orders":
                                    if (todayClinchs == null) {
                                        flushTodayClinchsImmediately();
                                    } else {
                                        if (System
                                                .currentTimeMillis() - todayClinchsFlushTimestamp > priorityRaiseTimeThreshold) {
                                            flushTodayClinchs(Math.max(commonApiPriority - priorityRaise, 0));
                                        }
                                        flushTodayClinchs(commonApiPriority);
                                    }
                                    break;
                                case "get_today_consign_orders":
                                    if (todayConsigns == null) {
                                        flushTodayConsignsImmediately();
                                    } else {
                                        if (System
                                                .currentTimeMillis() - todayConsignsFlushTimestamp > priorityRaiseTimeThreshold) {
                                            flushTodayConsigns(Math.max(commonApiPriority - priorityRaise, 0));
                                        }
                                        flushTodayConsigns(commonApiPriority);
                                    }
                                    break;
                                default:
                                    throw new Exception("error orderType");
                            }
                        }
                    }
                    Thread.sleep(flushInterval);
                }
            }
        });

        accoutStatesFlushTask.setDaemon(true);
        accoutStatesFlushTask.setPriority(Thread.MAX_PRIORITY);
        accoutStatesFlushTask.setName("accoutStateFlush");
        accoutStatesFlushTask.start();
        log.warn("accoutStatesFlush start: 开始持续更新账户资金股票等状况");
    }

    /*
     * 下单以刷新账户5状态
     */

    /**
     * 10大下单方法. 可提供优先级
     *
     * @param priority
     * @return
     * @throws Exception
     */
    public String flushNineBaseFundsData(Long priority) throws Exception {
        // 默认刷新, 不等待static
        Order order = OrderFactory.generateGetAccountFundsInfoOrder(true, false, priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushCurrentHolds(Long priority) throws Exception {
        Order order = OrderFactory.generateGetHoldStocksInfoOrder(priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushCanCancels(Long priority) throws Exception {
        Order order = OrderFactory.generateGetUnsoldsNotYetOrder(priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushTodayClinchs(Long priority) throws Exception {
        Order order = OrderFactory.generateGetTodayClinchOrdersOrder(priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushTodayConsigns(Long priority) throws Exception {
        // 默认访问分组形式, 通常未成交/成交/撤单3组, 若某些证券公司不支持分组, 由python api保证!
        Order order = OrderFactory.generateGetTodayConsignOrdersOrder("分组", priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushNineBaseFundsDataImmediately() throws Exception {
        return flushNineBaseFundsData(0L);
    }

    public String flushCurrentHoldsImmediately() throws Exception {
        return flushCurrentHolds(0L);
    }

    public String flushCanCancelsImmediately() throws Exception {
        return flushCanCancels(0L);
    }

    public String flushTodayClinchsImmediately() throws Exception {
        return flushTodayClinchs(0L);
    }

    public String flushTodayConsignsImmediately() throws Exception {
        return flushTodayConsigns(0L);
    }

    /*
     * check 逻辑,将调用更新逻辑
     */

    /**
     * 账号状态相关api check逻辑, 几乎每个策略都相同, 由 AccountStatas类自行实现, 无需主策略实现特殊的check逻辑
     * 将被 Checker调用
     *
     * @param order
     * @param responses
     * @param orderType
     * @throws Exception
     * @see Checker
     */
    public void checkForAccountStates(Order order, List<Response> responses, String orderType)
            throws Exception {
        switch (orderType) {
            case "get_account_funds_info":
                updateNineBaseFundsData(order, responses);
                break;
            case "get_hold_stocks_info":
                updateCurrentHolds(order, responses);
                break;
            case "get_unsolds_not_yet":
                updateCanCancels(order, responses);
                break;
            case "get_today_clinch_orders":
                updateTodayClinchs(order, responses);
                break;
            case "get_today_consign_orders":
                updateTodayConsigns(order, responses);
                break;
            default:
                throw new Exception("error orderType");
        }
    }


    /*
     * 5大字段具体更新逻辑
     */

    /**
     * update: 实际的更新操作, 将被 check程序调用(switch分发形式), 真正的执行更新数据, 此时已从python获取响应
     */
    public void updateNineBaseFundsData(Order order, List<Response> responses) throws Exception {
        Response resFinal = TraderUtil.findFinalResponse(responses);
        if (resFinal == null) {
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应不正确,全为retrying状态, 任务重入队列!!");
            // 强制高1优先级重入队列!因此队列中可能存在2个,因死循环可能已放入但未执行
            trader.reSendAndFinishOrder(order, responses);
            return;
        }
        // 响应正确, 该响应唯一情况:
        // response = dict(state="success", description='获取账号9项资金数据成功', payload=result, rawOrder=order)
        if ("success".equals(resFinal.getStr("state"))) {
            Map<String, Object> results = resFinal.getJSONObject("payload");
            for (String key : results.keySet()) {
                nineBaseFundsData.put(key, Double.valueOf(results.get(key).toString()));
            }
            log.debug("flush success: AccountStates.nineBaseFundsData: 已更新账户9项基本资金数据");
            nineBaseFundsDataFlushTimestamp = System.currentTimeMillis();
            trader.successFinishOrder(order,responses);
        } else {
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应状态非success, 任务重入队列!!");
            trader.reSendAndFinishOrder(order, responses); // 强制高优先级重入队列!因此队列中可能存在2个
        }
    }


    /**
     * 四大df字段,更新逻辑相同, 均为读取payload, 转换df后更新
     *
     * @param order
     * @param responses
     * @param fieldName
     * @warning 重入队列的订单, 已经是新的订单对象, 非原订单 !!
     */
    public void updateDfFields(Order order, List<Response> responses, String fieldName,
                               String successDescription) throws Exception {
        Response resFinal = TraderUtil.findFinalResponse(responses);
        if (resFinal == null) {
            log.error("flush fail: AccountStates.{}: 响应不正确,全为retrying状态, 相同新任务重入队列!!", fieldName);
            trader.reSendAndFinishOrder(order, responses);
            return;
        }
        if ("success".equals(resFinal.getStr("state"))) {
            DataFrame<Object> dfTemp = TraderUtil.payloadArrayToDf(resFinal);
            if (dfTemp == null) {
                log.error("flush fail: AccountStates.{}: payload为null, 相同新任务重入队列!!", fieldName);
                trader.reSendAndFinishOrder(order, responses);
                return;
            }
            if (dfTemp.size() == 0) {
                log.warn("empty df: {}", fieldName);
            }
            realFlushFieldAndTimestamp(fieldName, dfTemp);
            trader.successFinishOrder(order,responses);
            log.debug("flush success: AccountStates.{}: 已更新{}", fieldName, successDescription);
        } else {
            log.error("flush fail: AccountStates.{}: 响应状态非success, 相同新任务重入队列!!", fieldName);
            trader.reSendAndFinishOrder(order, responses);
        }
    }

    /**
     * 最终的更新逻辑, 两字段赋值
     *
     * @param fieldName
     * @param newData
     */
    public void realFlushFieldAndTimestamp(String fieldName, DataFrame<Object> newData) throws Exception {
        switch (fieldName) {
            case "currentHolds":
                currentHolds = newData;
                currentHoldsFlushTimestamp = System.currentTimeMillis();
                break;
            case "canCancels":
                canCancels = newData;
                canCancelsFlushTimestamp = System.currentTimeMillis();
                break;
            case "todayClinchs":
                todayClinchs = newData;
                todayClinchsFlushTimestamp = System.currentTimeMillis();
                break;
            case "todayConsigns":
                todayConsigns = newData;
                todayConsignsFlushTimestamp = System.currentTimeMillis();
                break;
            //case "nineBaseFundsData": // 不会出现, 非 df
            default:
                throw new Exception("error fieldName");
        }
    }


    public void updateCurrentHolds(Order order, List<Response> responses) throws Exception {
        updateDfFields(order, responses, "currentHolds", "当前持仓股票列表");
    }

    public void updateCanCancels(Order order, List<Response> responses) throws Exception {
        updateDfFields(order, responses, "canCancels", "当前可撤单股票列表");
    }

    public void updateTodayClinchs(Order order, List<Response> responses) throws Exception {
        updateDfFields(order, responses, "todayClinchs", "今日成交订单列表");
    }

    public void updateTodayConsigns(Order order, List<Response> responses) throws Exception {
        updateDfFields(order, responses, "todayConsigns", "今日委托订单列表");
    }
}
