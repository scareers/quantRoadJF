package com.scareers.gui.ths.simulation.trader;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONObject;
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
 * @noti: 5项数据的刷新均不保证立即执行, 即使  Immediately 也仅仅是以0最高优先级放入待执行队列. 实际执行由待执行队列进行调度
 * @noti: 静态属性的赋值, 实际由 check 程序完成, 因此, 本子系统应当后于 执行调度程序 和 check程序之后执行,
 * 且需要等待5项数据第一次更新, 以保证后期逻辑不会访问到 null值(或空Map/List)
 * @noti: nineBaseFundsData线程安全, 可随意赋值修改. 其余4项  DataFrame<Object> 需要全量更新.
 * @noti: 访问 DataFrame<Object> 时, 需要使用临时变量暂存, 即 dfo tempDf = 静态变量.
 * 即使静态变量在处理期间被更新, 依然不影响, 防止遍历时等bug
 */
@Getter
@Setter
public class AccountStates {
    public static long accountStatesFlushGlobalInterval = 10 * 1000; // 账户状态检测程序 sleep
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

    public AccountStates(Trader trader) {
        this.trader = trader;
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

    /**
     * 刷新账户信息. 仅将订单放入队列.
     *
     * @throws Exception
     */
    public void startFlush() throws Exception {
        Thread accoutStatesFlushTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    // 刷新5项数据,对队列进行遍历,若存在对应类型订单, 则跳过, 否则下单

                    List<String> alreadyInQueue = new ArrayList<>();
                    Iterator iterator = trader.getOrdersWaitForExecution().iterator();
                    while (iterator.hasNext()) {
                        Order order = (Order) iterator.next();
                        if (ORDER_TYPES.contains(order.getOrderType())) {
                            // 得到已在队列中的类型. 对其余类型进行补齐
                            alreadyInQueue.add(order.getOrderType());
                        }
                    }

                    for (String orderType : ORDER_TYPES) {
                        if (!alreadyInQueue.contains(orderType)) { // 不存在则添加
                            switch (orderType) {
                                // 第一次将以最高优先级刷新
                                case "get_account_funds_info":
                                    if (nineBaseFundsData.size() == 0) {
                                        flushNineBaseFundsDataImmediately();
                                    } else {
                                        flushNineBaseFundsData(Order.PRIORITY_MEDIUM / 2);
                                    }
                                    break;
                                case "get_hold_stocks_info":
                                    if (currentHolds == null) {
                                        flushCurrentHoldsImmediately();
                                    } else {
                                        flushCurrentHolds(Order.PRIORITY_MEDIUM / 2);
                                    }
                                    break;
                                case "get_unsolds_not_yet":
                                    if (canCancels == null) {
                                        flushCanCancelsImmediately();
                                    } else {
                                        flushCanCancels(Order.PRIORITY_MEDIUM / 2);
                                    }
                                    break;
                                case "get_today_clinch_orders":
                                    if (todayClinchs == null) {
                                        flushTodayClinchsImmediately();
                                    } else {
                                        flushTodayClinchs(Order.PRIORITY_MEDIUM / 2);
                                    }
                                    break;
                                case "get_today_consign_orders":
                                    if (todayConsigns == null) {
                                        flushTodayConsignsImmediately();
                                    } else {
                                        flushTodayConsigns(Order.PRIORITY_MEDIUM / 2);
                                    }
                                    break;
                                default:
                                    throw new Exception("error orderType");
                            }
                        }
                    }
                    Thread.sleep(accountStatesFlushGlobalInterval);
                }
            }
        });

        accoutStatesFlushTask.setDaemon(true);
        accoutStatesFlushTask.setPriority(Thread.MAX_PRIORITY);
        accoutStatesFlushTask.setName("accoutStateFlush");
        accoutStatesFlushTask.start();
        log.warn("accoutStatesFlush start: 开始持续更新账户资金股票等状况");
    }

    public String flushItem(String orderType, Long priority) throws Exception {
        if (priority == null) {
            priority = Order.PRIORITY_LOWEST;
        }
        Order order = OrderFactory.generateNoArgsQueryOrder(orderType, priority);
        trader.putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

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
        // 默认访问分组!未成交/成交/撤单3组
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

    private static final Log log = LogUtil.getLogger();

    /**
     * update: 实际的更新操作, 将被 check程序调用(switch分发形式), 真正的执行更新数据, 此时已从python获取响应
     */
    public void updateNineBaseFundsData(Order order, List<Response> responses) throws Exception {
        Response resFinal = TraderUtil.findFinalResponse(responses);
        if (resFinal == null) {
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应不正确,全为retrying状态, 任务重入队列!!");
            trader.reSendOrder(order); // 强制高优先级重入队列!因此队列中可能存在2个
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
        } else {
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应状态非success, 任务重入队列!!");
            trader.reSendOrder(order); // 强制高优先级重入队列!因此队列中可能存在2个
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
            //case "nineBaseFundsData": // 不会出现
            //nineBaseFundsData = newData;
            //nineBaseFundsDataFlushTimestamp = System.currentTimeMillis();
            //break;
            default:
                throw new Exception("error fieldName");
        }
    }

    /**
     * 四大df字段,更新逻辑相同, 均为读取payload, 转换df后更新
     * 注意, 重入队列的订单, 已经是新的订单对象, 非原订单 !!
     *
     * @param order
     * @param responses
     * @param fieldName
     */
    public void updateDfFields(Order order, List<JSONObject> responses, String fieldName,
                               String successDescription) throws Exception {
        JSONObject resFinal = TraderUtil.findFinalResponse(responses);

        if (resFinal == null) {
            log.error("flush fail: AccountStates.{}: 响应不正确,全为retrying状态, 相同新任务重入队列!!", fieldName);
            this.trader.getOrderExecutor().reSendOrder(order);
            return;
        }
        // 响应正确, 该响应2种情况: 即使无数据, 仅返回表头, 也解析为 dfo, 赋值.
        // 4大df字段基本相同, 仅描述不同
            /*
                response = dict(state="success", description='当前并没有持仓股票',
                        payload=results_lines, rawOrder=order)
                response = dict(state="success", description=f'获取账号股票持仓信息成功',
                        payload=results_lines, rawOrder=order)
             */
        if ("success".equals(resFinal.getStr("state"))) {
            DataFrame<Object> dfTemp = TraderUtil.payloadArrayToDf(resFinal); // 解析必然!
            if (dfTemp == null) {
                log.error("flush fail: AccountStates.{}: payload为null, 相同新任务重入队列!!", fieldName);
                this.trader.getOrderExecutor().reSendOrder(order);
                // 强制高优先级重入队列!因此队列中可能存在2个
                return;
            }
            if (dfTemp.size() == 0) {
                log.warn("empty df: 当前持仓数据为空");
            }
            realFlushFieldAndTimestamp(fieldName, dfTemp);
            log.debug("flush success: AccountStates.{}: 已更新{}", fieldName, successDescription);
        } else {
            log.error("flush fail: AccountStates.{}: 响应状态非success, 相同新任务重入队列!!", fieldName);
            this.trader.getOrderExecutor().reSendOrder(order);
        }
    }


    public void updateCurrentHolds(Order order, List<JSONObject> responses) throws Exception {
        updateDfFields(order, responses, "currentHolds", "当前持仓股票列表");
    }

    public void updateCanCancels(Order order, List<JSONObject> responses) throws Exception {
        updateDfFields(order, responses, "canCancels", "当前可撤单股票列表");
    }

    public void updateTodayClinchs(Order order, List<JSONObject> responses) throws Exception {
        updateDfFields(order, responses, "todayClinchs", "今日成交订单列表");
    }

    public void updateTodayConsigns(Order order, List<JSONObject> responses) throws Exception {
        updateDfFields(order, responses, "todayConsigns", "今日委托订单列表");
    }
}
