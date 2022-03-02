package com.scareers.gui.ths.simulation.trader;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.TraderUtil;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.rabbitmq.PythonSimulationClient;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.utils.CommonUtil.waitForever;
import static com.scareers.utils.CommonUtil.waitUtil;

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
    public static CopyOnWriteArrayList<PythonSimulationClient> clientPool;

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        AccountStates accountStates = AccountStates.getInstance(null, 5000, 10000L,
                10000, 2);
        accountStates.handshake(); // 同样握手. 逻辑同trader, 仅仅通道不一样
        // 启动账户资金获取程序
        accountStates.startFlush(); // 此时并未用到 strategy, 因此 check程序不会触发空指针异常
        accountStates.waitFirstInitFinish(); // 此时并未用到 strategy, 因此 check程序不会触发空指针异常

        Console.log(getNineBaseFundsData());
        Console.log(getCurrentHolds());
        Console.log(getCanCancels());
        Console.log(getTodayClinchs());
        Console.log(getTodayConsigns());
        waitForever();
    }


    public static AccountStates getInstance(Trader trader, long flushInterval, long commonApiPriority,
                                            long priorityRaiseTimeThreshold,
                                            long priorityRaise) throws IOException, TimeoutException {
        // noti: 最多提升10次优先级, 每次提高 priorityRaise
        if (INSTANCE == null) {
            clientPool = PythonSimulationClient.createAccountStatesFlushClientPool(); // 将初始化连接池, 单例
            INSTANCE = new AccountStates(trader, flushInterval, commonApiPriority, priorityRaiseTimeThreshold,
                    priorityRaise, 10);
        }
        return INSTANCE;
    }

    public static AccountStates getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
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

    // 60s一次, 清除所有订单Map中,一分钟之前的,账户状态刷新api订单
    public static final int clearOrdersInTraderAllMapInterval = 60 * 1000;

    /*
    @noti:
     属性: 订单队列, 因账户状态刷新程序为死循环一直刷新, 因此, 只需要 等待执行队列, 和 finished 队列即可.
     将机制简单化. --> 因此只需要执行器 即可, 不需要 checker
     且执行器并未定义为类, 简单循环即可
     */
    /**
     * 核心待执行订单优先级队列. 未指定容量, put将不会阻塞. take将可能阻塞
     */
    public static volatile PriorityBlockingQueue<Order> ordersWaitForExecution = new PriorityBlockingQueue<>();
    public static volatile Hashtable<Order, List<Response>> ordersFinished = new Hashtable<>();

    public static volatile ConcurrentHashMap<String, Double> nineBaseFundsData = new ConcurrentHashMap<>();// get_account_funds_info
    public static volatile DataFrame<Object> currentHolds = null; // get_hold_stocks_info // 持仓
    public static volatile DataFrame<Object> canCancels = null; // get_unsolds_not_yet 当前可撤, 即未成交
    public static volatile DataFrame<Object> todayClinchs = null; // get_today_clinch_orders 今日成交:
    // 成交时间	证券代码	证券名称	操作	成交数量  成交均价	成交金额	合同编号	成交编号
    public static volatile DataFrame<Object> todayConsigns = null; // get_today_consign_orders 今日所有委托

    // 实例属性, 维持数据
    public static volatile Long nineBaseFundsDataFlushTimestamp = null; // 五大接口刷新时间戳! long, 实际被刷新后更新.
    public static volatile Long currentHoldsFlushTimestamp = null; // 均为 毫秒  System.currentTimeMillis()
    public static volatile Long canCancelsFlushTimestamp = null;
    public static volatile Long todayClinchsFlushTimestamp = null;
    public static volatile Long todayConsignsFlushTimestamp = null;

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

    private AccountStates(Trader trader, long flushInterval, long commonApiPriority, long priorityRaiseTimeThreshold,
                          long priorityRaise, long maxRaiseTimes) throws IOException, TimeoutException {
        this.trader = trader;
        if (this.trader != null) {
            this.trader.setAccountStates(this);
        }
        this.flushInterval = flushInterval;
        this.commonApiPriority = commonApiPriority; // Order.PRIORITY_MEDIUM; // 5000, 不高
        this.priorityRaiseTimeThreshold = priorityRaiseTimeThreshold;
        this.priorityRaise = priorityRaise;
        this.maxRaiseTimes = maxRaiseTimes;

    }

    public void clearClose() {
        PythonSimulationClient.closeClientPool(clientPool);
    }

    /**
     * 握手, 将对每个client 握手. 且非异步.
     */
    public void handshake() throws IOException, InterruptedException {
        PythonSimulationClient.clientPoolHandshake(clientPool);
    }


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
        // 这里隐式定义 Executor 的逻辑, 类似 Trader.OrderExecutor
        Executor.getInstance(this).start(); // 执行器执行订单

        Thread accoutStatesFlushTask = new Thread(new Runnable() { // 生成订单
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    List<String> alreadyInQueue = new ArrayList<>();
                    for (Order order : ordersWaitForExecution) {
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
                            } catch (Exception ignored) {

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
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                DateTime now = DateUtil.date();
                ArrayList<Order> orders = new ArrayList<>(Trader.ordersAllMap.keySet());
                for (Order order : orders) {
                    if (ORDER_TYPES.contains(order.getOrderType())) {
                        if (DateUtil.between(order.getGenerateTime(), now, DateUnit.MS,
                                false) > clearOrdersInTraderAllMapInterval) {
                            Trader.ordersAllMap.remove(order);
                        }
                    }
                }
                log.info("AccountStates: clear orders in TraderAllMap before {}s",
                        clearOrdersInTraderAllMapInterval / 1000);
                ThreadUtil.sleep(clearOrdersInTraderAllMapInterval);
            }
        }, true);
        log.warn("accoutStatesFlush start: 开始持续更新账户资金股票等状况");
    }

    /**
     * 从客户端池, 获取当前空闲的客户端, 以备执行订单,
     *
     * @return
     * @key3 若当前并没有客户端空闲, 则将阻塞线程 ! 由于 synchronized, 因此可多线程之下
     */
    public synchronized PythonSimulationClient getFreeClientFromPool() {
        return PythonSimulationClient.getFreeClientFromPool(clientPool);
    }

    /**
     * 类似 Trader的 OrderExecutor, 单例且隶属于AS.
     */
    @Getter
    public static class Executor {
        private static Executor INSTANCE;

        public static Executor getInstance(AccountStates as) {
            if (INSTANCE == null) {
                INSTANCE = new Executor(as);
            }
            return INSTANCE;
        }

        private AccountStates as;

        private Executor(AccountStates as) {
            this.as = as;
        }

//        public void start() {
//            Thread orderExecuteTask = new Thread(new Runnable() {
//                @SneakyThrows
//                @Override
//                public void run() {
//                    while (true) {
//                        Order order = ordersWaitForExecution.take(); // 最高优先级订单, 将可能被阻塞
//                        executingOrder = order;
//                        log.warn("[as] order start execute: {} [{}] --> {}:{}", order.getOrderType(),
//                                order.getPriority(), order.getRawOrderId(), order.getParams());
//                        order.addLifePoint(Order.LifePointStatus.EXECUTING, "executing: 开始执行订单");
//
//                        PythonSimulationClient client = as.getFreeClientFromPool(); // 将阻塞, 直到某个客户端可用
//                        List<Response> responses = client.execOrderUtilSuccess(order);
//
//                        executingOrder = null;
//
//                        order.addLifePoint(Order.LifePointStatus.FINISH_EXECUTE, "finish_execute: 执行订单完成");
//
//                        // 这里直接进行check, 而非在 Checker类中执行
//                        // @key: 执行数据更新和警告. 不重发订单等. 数据的及时更新依赖于 自动优先级提高的机制
//                        as.checkForAccountStates(order, responses, order.getOrderType());
//
//                        order.addLifePoint(Order.LifePointStatus.FINISH, "finish: [账户状态刷新订单]执行完成, 简单finish");
//                        order.setExecResponses(responses); // 响应字段设置
//                        ordersFinished.put(order, responses); //
//                        Trader.ordersAllMap.put(order, responses); // 注意应当更新响应
//                    }
//                }
//            });
//            orderExecuteTask.setDaemon(true);
//            orderExecuteTask.setPriority(Thread.MAX_PRIORITY);
//            orderExecuteTask.setName("orderExecutor");
//            orderExecuteTask.start();
//            log.warn("start: orderExecutor 开始按优先级执行订单...");
//        }

        public static ExecutorService threadPoolExecutor = ThreadUtil.newExecutor(4, 8, Integer.MAX_VALUE);

        public void start() {
            Thread orderExecuteTask = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        Order order = ordersWaitForExecution.take(); // 最高优先级订单, 将可能被阻塞
                        PythonSimulationClient client = as.getFreeClientFromPool(); // 将阻塞, 直到某个客户端可用, 耗时
                        log.warn("[as] order start execute: {} [{}] --> {}:{}", order.getOrderType(),
                                order.getPriority(), order.getRawOrderId(), order.getParams());
                        order.addLifePoint(Order.LifePointStatus.EXECUTING, "executing: 开始执行订单");

                        // 异步执行订单
                        threadPoolExecutor.execute(new Runnable() {
                            @SneakyThrows
                            @Override
                            public void run() {
                                List<Response> responses = client.execOrderUtilSuccess(order);
                                order.addLifePoint(Order.LifePointStatus.FINISH_EXECUTE, "finish_execute: 执行订单完成");

                                // 这里直接进行check, 而非在 Checker类中执行
                                // @key: 执行数据更新和警告. 不重发订单等. 数据的及时更新依赖于 自动优先级提高的机制
                                as.checkForAccountStates(order, responses, order.getOrderType());

                                order.addLifePoint(Order.LifePointStatus.FINISH, "finish: [账户状态刷新订单]执行完成, 简单finish");
                                order.setExecResponses(responses); // 响应字段设置
                                ordersFinished.put(order, responses); //
                                Trader.ordersAllMap.put(order, responses); // 注意应当更新响应
                            }
                        });


                    }
                }
            });
            orderExecuteTask.setDaemon(true);
            orderExecuteTask.setPriority(Thread.MAX_PRIORITY);
            orderExecuteTask.setName("orderExecutor");
            orderExecuteTask.start();
            log.warn("start: orderExecutor 开始按优先级执行订单...");
        }


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
        for (Order order : ordersWaitForExecution) {
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

    public void putOrderToWaitExecute(Order order) throws Exception {
        order.addLifePoint(Order.LifePointStatus.WAIT_EXECUTE, "wait_execute: 放入执行队列,等待执行");
        ordersWaitForExecution.put(order);
        Trader.ordersAllMap.put(order, Arrays.asList()); // as的订单们, 也加入到 Trader 的 allMap
        log.info("[as] order enqueue: {} ", order.toString());
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
        putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushCurrentHolds(Long priority) throws Exception {
        Order order = OrderFactory.generateGetHoldStocksInfoOrder(priority);
        putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushCanCancels(Long priority) throws Exception {
        Order order = OrderFactory.generateGetUnsoldsNotYetOrder(priority);
        putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushTodayClinchs(Long priority) throws Exception {
        Order order = OrderFactory.generateGetTodayClinchOrdersOrder(priority);
        putOrderToWaitExecute(order);
        return order.getRawOrderId();
    }

    public String flushTodayConsigns(Long priority) throws Exception {
        // 默认访问分组形式, 通常未成交/成交/撤单3组, 若某些证券公司不支持分组, 由python api保证!
        Order order = OrderFactory.generateGetTodayConsignOrdersOrder("分组", priority);
        putOrderToWaitExecute(order);
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
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应不正确,全为retrying状态, 忽略本次刷新!!");
            raisePriority(order.getOrderType(), 1); // 将尝试提高同api优先级,若队列中无新
            // 强制高1优先级重入队列!因此队列中可能存在2个,因死循环可能已放入但未执行
            return;
        }
        // 响应正确, 该响应唯一情况:
        // response = dict(state="success", description='获取账号9项资金数据成功', payload=result, rawOrder=order)
        if ("success".equals(resFinal.getString("state"))) {
            Map<String, Object> results = resFinal.getJSONObject("payload");
            for (String key : results.keySet()) {
                nineBaseFundsData.put(key, Double.valueOf(results.get(key).toString()));
            }
            log.debug("flush success: AccountStates.nineBaseFundsData: 已更新账户9项基本资金数据");
            nineBaseFundsDataFlushTimestamp = System.currentTimeMillis();
        } else {
            log.error("flush fail: AccountStates.nineBaseFundsData: 响应状态非success, 任务重入队列!!");
            raisePriority(order.getOrderType(), 1); // 将尝试提高同api优先级,若队列中无新
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
            log.error("flush fail: AccountStates.{}: 响应不正确,全为retrying状态, 忽略本次刷新!!", fieldName);
            raisePriority(order.getOrderType(), 1); // 将尝试提高同api优先级,若队列中无新
            return;
        }
        if ("success".equals(resFinal.getString("state"))) {
            DataFrame<Object> dfTemp = TraderUtil.payloadArrayToDf(resFinal);
            if (dfTemp == null) {
                log.error("flush fail: AccountStates.{}: payload为null, 忽略本次刷新!!", fieldName);
                raisePriority(order.getOrderType(), 1); // 将尝试提高同api优先级,若队列中无新
                return;
            }
            if (dfTemp.size() == 0) {
                log.warn("empty df: {}", fieldName);
            }
            realFlushFieldAndTimestamp(fieldName, dfTemp);
            log.debug("flush success: AccountStates.{}: 已更新{}", fieldName, successDescription);
        } else {
            log.error("flush fail: AccountStates.{}: 响应状态非success, 忽略本次刷新!!", fieldName);
            raisePriority(order.getOrderType(), 1); // 将尝试提高同api优先级,若队列中无新
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
    数据api
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
    public Map<String, Integer> getAvailableAmountOfStocksMap() {
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        List<Integer> availableRow = DataFrameS
                .getColAsIntegerList(currentHolds, SettingsOfTrader.STR_AVAILABLE_AMOUNT);
        List<String> stockCodes = DataFrameS.getColAsStringList(currentHolds, SettingsOfTrader.STR_SEC_CODE);
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


    public static List<String> getOrderTypes() {
        return ORDER_TYPES;
    }

    public static List<String> getExcludeOrderTypes() {
        return EXCLUDE_ORDER_TYPES;
    }

    public static List<String> getShouldOrderTypes() {
        return SHOULD_ORDER_TYPES;
    }

    public static PriorityBlockingQueue<Order> getOrdersWaitForExecution() {
        return ordersWaitForExecution;
    }

    public static Hashtable<Order, List<Response>> getOrdersFinished() {
        return ordersFinished;
    }

    public static ConcurrentHashMap<String, Double> getNineBaseFundsData() {
        return nineBaseFundsData;
    }

    public static DataFrame<Object> getCurrentHolds() {
        return currentHolds;
    }

    public static DataFrame<Object> getCanCancels() {
        return canCancels;
    }

    public static DataFrame<Object> getTodayClinchs() {
        return todayClinchs;
    }

    public static DataFrame<Object> getTodayConsigns() {
        return todayConsigns;
    }

    public static Long getNineBaseFundsDataFlushTimestamp() {
        return nineBaseFundsDataFlushTimestamp;
    }

    public static Long getCurrentHoldsFlushTimestamp() {
        return currentHoldsFlushTimestamp;
    }

    public static Long getCanCancelsFlushTimestamp() {
        return canCancelsFlushTimestamp;
    }

    public static Long getTodayClinchsFlushTimestamp() {
        return todayClinchsFlushTimestamp;
    }

    public static Long getTodayConsignsFlushTimestamp() {
        return todayConsignsFlushTimestamp;
    }
}
