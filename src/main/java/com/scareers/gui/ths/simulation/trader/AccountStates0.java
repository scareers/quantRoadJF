package com.scareers.gui.ths.simulation.trader;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.TraderUtil;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * 账号状态监控类. 当前 5项数据
 *
 * @author admin
 * @notification @key: 本类已弃用. 原始版本. 未将账号状态刷新 与 买卖api 分开
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
public class AccountStates0 {
    private static final Log log = LogUtil.getLogger();
    private static AccountStates0 INSTANCE; // 单例实现

    public static AccountStates0 getInstance(Trader trader, long flushInterval, long commonApiPriority,
                                             long priorityRaiseTimeThreshold,
                                             long priorityRaise) {
        // noti: 最多提升10次优先级, 每次提高 priorityRaise
        if (INSTANCE == null) {
            INSTANCE = new AccountStates0(trader, flushInterval, commonApiPriority, priorityRaiseTimeThreshold,
                    priorityRaise, 10);
        }
        return INSTANCE;
    }

    public static AccountStates0 getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }


    // 实例属性
    public ConcurrentHashMap<String, Double> nineBaseFundsData = new ConcurrentHashMap<>(); // get_account_funds_info
    public DataFrame<Object> currentHolds = null; // get_hold_stocks_info // 持仓
    public DataFrame<Object> canCancels = null; // get_unsolds_not_yet 当前可撤, 即未成交
    public DataFrame<Object> todayClinchs = null; // get_today_clinch_orders 今日成交:
    // 成交时间	证券代码	证券名称	操作	成交数量  成交均价	成交金额	合同编号	成交编号
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
    private long maxRaiseTimes;

    private AccountStates0(Trader trader, long flushInterval, long commonApiPriority, long priorityRaiseTimeThreshold,
                           long priorityRaise, long maxRaiseTimes) {
        this.trader = trader;
        this.flushInterval = flushInterval;
        this.commonApiPriority = commonApiPriority; // Order.PRIORITY_MEDIUM; // 5000, 不高
        this.priorityRaiseTimeThreshold = priorityRaiseTimeThreshold;
        this.priorityRaise = priorityRaise;
        this.maxRaiseTimes = maxRaiseTimes;
    }

    /**
     * 可设定监控某几项, 对应需要修改 alreadyInitialized().
     * 全部可能的账户状态刷新订单类型.  配合 EXCLUDE_ORDER_TYPES 排除某些类型, 剩下的实际执行.
     */
    public static final List<String> ORDER_TYPES = Arrays
            .asList(
                    "get_account_funds_info",
                    "get_hold_stocks_info",
                    "get_unsolds_not_yet",
                    "get_today_clinch_orders",
                    "get_today_consign_orders"
            ); // 常量,对应5项数据api

    /**
     * @noti 可设定强制排除这些
     */
    public static final List<String> EXCLUDE_ORDER_TYPES = Arrays
            .asList(
                    "get_unsolds_not_yet",
                    "get_today_consign_orders"
            ); // 常量,对应5项数据api

    public static final List<String> SHOULD_ORDER_TYPES =
            ORDER_TYPES.stream().filter(type -> !EXCLUDE_ORDER_TYPES.contains(type)).collect(Collectors.toList());

    /**
     * 等待已被第一次初始化, Map做size()检测, 其余做非null检测
     *
     * @return 第一次是否初始化完成.
     */
    public boolean alreadyInitialized() {
        for (String type : ORDER_TYPES) { // 全类型检查
            if (!EXCLUDE_ORDER_TYPES.contains(type)) { // 实际使用了的api
                if ("get_account_funds_info".equals(type)) {
                    if (!(nineBaseFundsData.size() > 0)) {
                        return false;
                    }
                }
                if ("get_hold_stocks_info".equals(type)) {
                    if (currentHolds == null) {
                        return false;
                    }
                }
                if ("get_unsolds_not_yet".equals(type)) {
                    if (canCancels == null) {
                        return false;
                    }
                }
                if ("get_today_clinch_orders".equals(type)) {
                    if (todayClinchs == null) {
                        return false;
                    }
                }
                if ("get_today_consign_orders".equals(type)) {
                    if (todayConsigns == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void waitFirstInitFinish() {
        try {
            waitUtil(this::alreadyInitialized, Integer.MAX_VALUE, 10,
                    "首次账户资金状态刷新完成"); // 等待第一次账户状态5信息获取完成. 首次优先级为 0L
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                    for (Order order : Trader.getOrdersWaitForExecution()) {
                        if (SHOULD_ORDER_TYPES.contains(order.getOrderType())) {
                            // 得到已在队列中的类型. 对其余类型进行补齐
                            alreadyInQueue.add(order.getOrderType());
                        }
                    }
                    for (String orderType : SHOULD_ORDER_TYPES) {
                        if (!alreadyInQueue.contains(orderType)) { // 不存在则 补充对应类型的 账户状态监控 订单
                            supplementStateOrder(orderType);
                        } else { // 账户监控订单, 若在队列中, 则需要检测一下 优先级, 若滞留时间长, 则考虑 提高优先级!
                            try {
                                tryRaisePriorityPossible(orderType);
                            } catch (Exception e) {

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

    private void supplementStateOrder(String orderType) throws Exception {
        switch (orderType) {
            // 第一次将以最高优先级刷新, 否则常态
            case "get_account_funds_info":
                if (nineBaseFundsData.size() == 0) {
                    flushNineBaseFundsDataImmediately();
                } else {
                    flushNineBaseFundsData(commonApiPriority);
                }
                break;
            case "get_hold_stocks_info":
                if (currentHolds == null) {
                    flushCurrentHoldsImmediately();
                } else {
                    flushCurrentHolds(commonApiPriority);
                }
                break;
            case "get_unsolds_not_yet":
                if (canCancels == null) {
                    flushCanCancelsImmediately();
                } else {
                    flushCanCancels(commonApiPriority);
                }
                break;
            case "get_today_clinch_orders":
                if (todayClinchs == null) {
                    flushTodayClinchsImmediately();
                } else {
                    flushTodayClinchs(commonApiPriority);
                }
                break;
            case "get_today_consign_orders":
                if (todayConsigns == null) {
                    flushTodayConsignsImmediately();
                } else {
                    flushTodayConsigns(commonApiPriority);
                }
                break;
            default:
                throw new Exception("error orderType");
        }
    }

    private void tryRaisePriorityPossible(String orderType) throws Exception {
        switch (orderType) {
            // 提高倍数优先级. 倍数为整数.
            case "get_account_funds_info":
                if (System.currentTimeMillis() - nineBaseFundsDataFlushTimestamp > priorityRaiseTimeThreshold) {
                    raisePriority("get_account_funds_info",
                            (System.currentTimeMillis() - nineBaseFundsDataFlushTimestamp) / priorityRaiseTimeThreshold);
                }
                break;
            case "get_hold_stocks_info":
                if (System
                        .currentTimeMillis() - currentHoldsFlushTimestamp > priorityRaiseTimeThreshold) {
                    raisePriority("get_hold_stocks_info",
                            (System.currentTimeMillis() - currentHoldsFlushTimestamp) / priorityRaiseTimeThreshold);
                }
                break;
            case "get_unsolds_not_yet":
                if (System
                        .currentTimeMillis() - canCancelsFlushTimestamp > priorityRaiseTimeThreshold) {
                    raisePriority("get_unsolds_not_yet",
                            (System.currentTimeMillis() - canCancelsFlushTimestamp) / priorityRaiseTimeThreshold);
                }
                break;
            case "get_today_clinch_orders":
                if (System
                        .currentTimeMillis() - todayClinchsFlushTimestamp > priorityRaiseTimeThreshold) {
                    raisePriority("get_today_clinch_orders",
                            (System.currentTimeMillis() - todayClinchsFlushTimestamp) / priorityRaiseTimeThreshold);
                }
                break;
            case "get_today_consign_orders":
                if (System
                        .currentTimeMillis() - todayConsignsFlushTimestamp > priorityRaiseTimeThreshold) {
                    raisePriority("get_today_consign_orders",
                            (System.currentTimeMillis() - todayClinchsFlushTimestamp) / priorityRaiseTimeThreshold);
                }
                break;
            default:
                throw new Exception("error orderType");
        }
    }

    /**
     * 相当于, 默认的 priorityRaiseTimeThreshold是 60000, 相当于每分钟, 提高一次优先级, 提高量是  priorityRaise.
     * 最多提高次数是 20
     *
     * @param get_account_funds_info
     * @param raiseRate
     */
    private void raisePriority(String get_account_funds_info, long raiseRate) {
        raiseRate = Math.min(raiseRate, maxRaiseTimes);
        for (Order order : Trader.getOrdersWaitForExecution()) {
            if (order.getOrderType().equals(get_account_funds_info)) {
                long newPriority = Math.max(commonApiPriority - raiseRate * priorityRaise, 0);
                if (newPriority < order.getPriority()) {
                    log.warn("raise priority: 账户状态监控api提高优先级: {}: {} --> {}", order.getOrderType(), order.getPriority()
                            , newPriority);
                    order.setPriority(newPriority);
                }
            }
        }
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
            trader.successFinishOrder(order, responses);
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
            trader.successFinishOrder(order, responses);
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

    /*
    api
     */

    /**
     * 给定股票, 从 当前持仓df中, 获取该股票的可用数量
     *
     * @param stock
     * @return
     */
    public int getAvailableOfStock(String stock) {
        List<Integer> availableRow = DataFrameS.getColAsIntegerList(currentHolds, "可用余额");
        List<String> stockCodes = DataFrameS.getColAsStringList(currentHolds, "证券代码");
        int index = stockCodes.indexOf(stock); // -1
        if (index > 0) {
            return availableRow.get(index);
        } else {
            return 0; // 可用数量返回0 是正常逻辑
        }
    }

    /**
     * 获取全部最新可用的map, 参考上一方法
     *
     * @return
     */
    public Map<String, Integer> getAvailablesOfStocksMap() {
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        List<Integer> availableRow = DataFrameS.getColAsIntegerList(currentHolds, "可用余额");
        List<String> stockCodes = DataFrameS.getColAsStringList(currentHolds, "证券代码");
        for (int i = 0; i < availableRow.size(); i++) {
            stringIntegerHashMap.put(stockCodes.get(i), availableRow.get(i));
        }
        return stringIntegerHashMap;
    }

    public Double getTotalAssets() {
        return nineBaseFundsData.get("总资产"); // 基本不会null
    }

    /**
     * 获取全部 冻结数量map
     *
     * @return
     */
    public Map<String, Integer> getFrozenOfStocksMap() {
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        List<Integer> availableRow = DataFrameS.getColAsIntegerList(currentHolds, "冻结数量");
        List<String> stockCodes = DataFrameS.getColAsStringList(currentHolds, "证券代码");
        for (int i = 0; i < availableRow.size(); i++) {
            stringIntegerHashMap.put(stockCodes.get(i), availableRow.get(i));
        }
        return stringIntegerHashMap;
    }

    /**
     * 获取最新可用现金
     *
     * @return
     */
    public Double getAvailableCash() {
        return nineBaseFundsData.get("可用金额");
    }
}
