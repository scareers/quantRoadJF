/**
 * ths 自动交易程序子系统:(主要面向过程编程)
 * 1.数据获取系统:
 * 1.FsTransactionFetcher 获取dc实时成交数据,3stick. 数据存储于静态属性. 异步存储于mysql
 * 2.eastmoney包其他API, 访问dc API, 的其他数据项, 可参考 python efinance模块
 * 2.订单生成系统: Order 作为基类, OrderFactory 作为快捷生产的工厂类
 * 3.订单发送与响应接收(交易操作系统): 通过rabbitmq作为中间件, 与python交互.
 * 单个订单, 收发一次, 封装为一次交易过程, 串行.
 * 4.订单优先级执行器系统: 所有订单应进入优先级队列, 由订单执行器, 统一依据优先级调度, 常态优先执行buy/sell订单
 * 5.账户状态监控系统:
 * 不断发送查询api, 以尽可能快速更新账户信息, 使得订单发送前较为合理资金调度
 * 6.成交状态监控系统:
 * 某订单成功执行后进入成交状态监控队列, 将根据系统5的信息, 确定订单成交状况
 * 7.各大系统对应订单生命周期:
 * * --> new  (纯新生,无参数构造器new,尚未决定类型)
 * * --> generated(类型,参数已准备好,可prepare)
 * * --> wait_execute(入(执行队列)队后等待执行)
 * * --> executing(已发送python,执行中,等待响应)
 * * --> finish_execute(已接收到python响应)
 * * --> check_transaction_status(确认成交状态中, 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
 * * --> finish (订单彻底完成)
 */
package com.scareers.gui.ths.simulation;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.rabbitmq.client.*;
import com.scareers.datasource.eastmoney.fstransaction.FsTransactionFetcher;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.LifePointStatus;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.Strategy;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.connectToRbServer;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.initDualChannel;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.*;
import static com.scareers.utils.CommonUtil.waitUtil;

//import com.scareers.gui.ths.simulation.interact.gui.JFrameDemo;

/**
 * description: ths 自动交易程序
 * java发送消息 --> python执行 --> python发送结果 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
public class TraderMain {
    // python程序启动cmd命令.  PYTHONPATH 由该程序自行保证! --> sys.path.append()
    public static String pythonStartCMD = "C:\\keys\\Python37-32\\python.exe " +
            "C:/project/python/quantRoad/gui/ths_simulation_trade/main_simulation_trade.py";
    private static final Log log = LogUtil.getLogger();
    public static Channel channelComsumer;
    public static Channel channelProducer;
    public static Connection connOfRabbitmq;

    /**
     * 核心待执行订单优先级队列. 未指定容量, put将不会阻塞. take将可能阻塞
     */
    public static PriorityBlockingQueue<Order> ordersWaitForExecution = new PriorityBlockingQueue<>();
    /**
     * 核心检测订单执行状态线程安全Map. 将遍历队列元素, 当元素check通过, 则去除元素,订单彻底完成.
     * key:value--> 订单对象: 对应的线程安全响应列表
     */
    public static ConcurrentHashMap<Order, List<JSONObject>> ordersWaitForCheckTransactionStatusMap
            = new ConcurrentHashMap<>();
    /**
     * check 后, 将被放入完成队列. check信息, 将被放入 order.生命周期 check_transaction_status的描述中.
     * 最后将生命周期设置为finish, 放入此Map
     */
    public static ConcurrentHashMap<Order, List<JSONObject>> ordersFinished
            = new ConcurrentHashMap<>();
    public static long accountStatesFlushGlobalInterval = 10 * 1000; // 账户状态检测程序 slee

    /**
     * 主策略类. 管理具体实现
     */
    public static class MainStrategy {
        public static Strategy createStrategy() throws Exception {
            return new LowBuyHighSellStrategy(LowBuyHighSellStrategy.class.getName());
        }
    }

    public static void main(String[] args) throws Exception {
        Strategy mainStrategy = MainStrategy.createStrategy(); // 获取核心策略对象!, 该配置也在这里了.


        initConnOfRabbitmqAndDualChannel(); // 初始化mq连接与双通道
        // startPythonApp(); // 是否自启动python程序, 单机可用但无法查看python cmd
        handshake(); // 与python握手可控

        // 启动执行器, 将遍历优先级队列, 发送订单到python, 并获取响应
        OrderExecutor.start();

        // 启动成交状况检测, 对每个订单的响应, 进行处理. 成功或者重发等
        Checker.startCheckTransactionStatus(mainStrategy);

        // 启动账户资金获取程序
        AccountStates.startFlush();
        waitUtil(AccountStates::alreadyInitialized, 120 * 1000, 100, "首次账户资金状态刷新完成"); // 需要等待初始化完成!

        mainStrategy.initYesterdayHolds(); // 将昨日持仓更新到股票池.  将昨日收盘持仓和资金信息, 更新到静态属性

        // fs成交开始抓取, 股票池包含今日选股(for buy, 自动包含两大指数), 以及昨日持仓(for sell)
        FsTransactionFetcher fsTransactionFetcher =
                FsTransactionFetcher.createInstance(mainStrategy.getStockPool(), 10, "15:10:00", 1000, 100, 32);
        fsTransactionFetcher.startFetch();  // 策略所需股票池实时数据抓取. 核心字段: fsTransactionDatas
        waitUtil(() -> fsTransactionFetcher.getFirstTimeFinish().get(), 3600 * 1000, 100, "第一次tick数据抓取完成"); //
        // 正式启动主策略下单
//        ThreadUtil.execAsync(new Runnable() {
//            @SneakyThrows
//            @Override
//            public void run() {
//                JFrameDemo.main0(null);
//            }tyg
//        });

        mainStrategy.startDealWith();


        manualInteractive(); // 开始人工交互, 可以人工调用订单, 可以人工打印信息等, 可以 gui程序.  应阻塞!
        closeDualChannelAndConn(); // 关闭连接
        fsTransactionFetcher.stopFetch(); // 停止数据抓 取, 非立即.
    }

    private static void manualInteractive() throws Exception {

        while (true) {
            Scanner input = new Scanner(System.in);
            String info = input.next();
            System.out.println(info);
            if ("q".equals(info)) {
                break;
            } else if ("s".equals(info)) {
                AccountStates.showFields();
            } else if ("g".equals(info)) {
//                JFrameDemo.main0(null);
            }
        }
    }

    public static void successFinishOrder(Order order, List<JSONObject> responses, String description) {
        TraderMain.ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(Order.LifePointStatus.FINISH, description);
        TraderMain.ordersFinished.put(order, responses); // 先删除, 后添加
    }

    public static void successFinishOrder(Order order, List<JSONObject> responses) {
        successFinishOrder(order, responses, "订单完成");
    }


    /**
     * 账号状态监控类. 当前 5项数据
     * <p>
     * // @noti:5项数据的刷新均不保证立即执行, 即使  Immediately 也仅仅是以高优先级放入待执行队列. 实际执行由待执行队列进行调度
     * // @noti: 静态属性的赋值, 实际由 check 程序完成, 因此, 本子系统应当后于 执行调度程序 换 和check程序之后执行,
     * 且需要等待5项数据第一次更新!
     * // @noti: nineBaseFundsData线程安全, 可随意赋值修改. 其余4项  DataFrame<Object> 需要全量更新.
     * // @noti: 访问 DataFrame<Object> 时, 需要使用临时变量暂存, 即 dfo tempDf = 静态变量. 即使静态变量被更新, 依然不影响
     */
    public static class AccountStates {
        public static ConcurrentHashMap<String, Double> nineBaseFundsData = new ConcurrentHashMap<>(); // get_account_funds_info
        // {冻结金额=363.48, 总资产=177331.42, 可用金额=57078.94, 资金余额=57442.42, 可取金额=0.0, 当日盈亏比=-0.017, 当日盈亏=-3059.0, 股票市值=119889.0, 持仓盈亏=-7915.0}
        public static DataFrame<Object> currentHolds = null; // get_hold_stocks_info // 持仓
        public static DataFrame<Object> canCancels = null; // get_unsolds_not_yet 当前可撤, 即未成交
        public static DataFrame<Object> todayClinchs = null; // get_today_clinch_orders 今日成交
        public static DataFrame<Object> todayConsigns = null; // get_today_consign_orders 今日所有委托

        public static Long nineBaseFundsDataFlushTimestamp = null; // 五大接口刷新时间戳! long, 实际被刷新后更新.
        public static Long currentHoldsFlushTimestamp = null; // 均为 毫秒  System.currentTimeMillis()

        public static Long canCancelsFlushTimestamp = null;
        public static Long todayClinchsFlushTimestamp = null;
        public static Long todayConsignsFlushTimestamp = null;

        public static final List<String> orderTypes = Arrays
                .asList("get_account_funds_info", "get_hold_stocks_info", "get_unsolds_not_yet",
                        "get_today_clinch_orders", "get_today_consign_orders"); // 常量

        /**
         * 已被第一次初始化, 需要等待
         *
         * @return
         */
        public static boolean alreadyInitialized() {
            return nineBaseFundsData
                    .size() > 0 && currentHolds != null
                    && canCancels != null && todayClinchs != null && todayConsigns != null;
            // 五项不为null, 已被初始化
        }

        public static void showFields() {
            Console.log("AccountStates.nineBaseFundsData:\n{}\n", nineBaseFundsData);
            Console.log("AccountStates.currentHolds:\n{}\n", currentHolds.toString(100));
            Console.log("AccountStates.canCancels:\n{}\n", canCancels.toString(100));
            Console.log("AccountStates.todayClinchs:\n{}\n", todayClinchs.toString(100));
            Console.log("AccountStates.todayConsigns:\n{}\n", todayConsigns.toString(100));
        }


        /**
         * 账号状态相关api check逻辑, 几乎每个策略都相同, 无需 主策略实现特殊的check逻辑!
         *
         * @param order
         * @param responses
         * @param orderType
         * @throws Exception
         */
        public static void checkForAccountStates(Order order, List<JSONObject> responses, String orderType)
                throws Exception {
            switch (orderType) {
                case "get_account_funds_info":
                    AccountStates.updateNineBaseFundsData(order, responses);
                    break;
                case "get_hold_stocks_info":
                    AccountStates.updateCurrentHolds(order, responses);
                    break;
                case "get_unsolds_not_yet":
                    AccountStates.updateCanCancels(order, responses);
                    break;
                case "get_today_clinch_orders":
                    AccountStates.updateTodayClinchs(order, responses);
                    break;
                case "get_today_consign_orders":
                    AccountStates.updateTodayConsigns(order, responses);
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
        public static void startFlush() throws Exception {
            Thread accoutStatesFlushTask = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        // 刷新5项数据,对队列进行遍历,若存在对应类型订单, 则跳过, 否则下单

                        List<String> alreadyInQueue = new ArrayList<>();
                        Iterator iterator = ordersWaitForExecution.iterator();
                        while (iterator.hasNext()) {
                            Order order = (Order) iterator.next();
                            if (orderTypes.contains(order.getOrderType())) {
                                // 得到已在队列中的类型. 对其余类型进行补齐
                                alreadyInQueue.add(order.getOrderType());
                            }
                        }

                        for (String orderType : orderTypes) {
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

        public static String flushItem(String orderType, Long priority) throws Exception {
            if (priority == null) {
                priority = Order.PRIORITY_LOWEST;
            }
            Order order = OrderFactory.generateNoArgsQueryOrder(orderType, priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        /**
         * 10大下单方法. 可提供优先级
         *
         * @param priority
         * @return
         * @throws Exception
         */
        public static String flushNineBaseFundsData(Long priority) throws Exception {
            // 默认刷新, 不等待static
            Order order = OrderFactory.generateGetAccountFundsInfoOrder(true, false, priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        public static String flushCurrentHolds(Long priority) throws Exception {
            Order order = OrderFactory.generateGetHoldStocksInfoOrder(priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        public static String flushCanCancels(Long priority) throws Exception {
            Order order = OrderFactory.generateGetUnsoldsNotYetOrder(priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        public static String flushTodayClinchs(Long priority) throws Exception {
            Order order = OrderFactory.generateGetTodayClinchOrdersOrder(priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        public static String flushTodayConsigns(Long priority) throws Exception {
            // 默认访问分组!未成交/成交/撤单3组
            Order order = OrderFactory.generateGetTodayConsignOrdersOrder("分组", priority);
            putOrderToWaitExecute(order);
            return order.getRawOrderId();
        }

        public static String flushNineBaseFundsDataImmediately() throws Exception {
            return flushNineBaseFundsData(0L);
        }

        public static String flushCurrentHoldsImmediately() throws Exception {
            return flushCurrentHolds(0L);
        }

        public static String flushCanCancelsImmediately() throws Exception {
            return flushCanCancels(0L);
        }

        public static String flushTodayClinchsImmediately() throws Exception {
            return flushTodayClinchs(0L);
        }

        public static String flushTodayConsignsImmediately() throws Exception {
            return flushTodayConsigns(0L);
        }

        /**
         * update: 实际的更新操作, 将被 check程序调用, 真正的执行更新数据,此时已从python获取响应
         */
        public static void updateNineBaseFundsData(Order order, List<JSONObject> responses) throws Exception {
            JSONObject resFinal = TraderUtil.findFinalResponse(responses);
            if (resFinal == null) {
                log.error("flush fail: AccountStates.nineBaseFundsData: 响应不正确,全为retrying状态, 任务重入队列!!");
                OrderExecutor.reSendOrder(order); // 强制高优先级重入队列!因此队列中可能存在2个
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
                OrderExecutor.reSendOrder(order); // 强制高优先级重入队列!因此队列中可能存在2个
            }
        }


        /**
         * 最终的更新逻辑, 两字段赋值
         *
         * @param fieldName
         * @param newData
         */
        public static void realFlushFieldAndTimestamp(String fieldName, DataFrame<Object> newData) throws Exception {
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
        public static void updateDfFields(Order order, List<JSONObject> responses, String fieldName,
                                          String successDescription) throws Exception {
            JSONObject resFinal = TraderUtil.findFinalResponse(responses);

            if (resFinal == null) {
                log.error("flush fail: AccountStates.{}: 响应不正确,全为retrying状态, 相同新任务重入队列!!", fieldName);
                OrderExecutor.reSendOrder(order);
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
                    OrderExecutor.reSendOrder(order);
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
                OrderExecutor.reSendOrder(order);
            }
        }


        public static void updateCurrentHolds(Order order, List<JSONObject> responses) throws Exception {
            updateDfFields(order, responses, "currentHolds", "当前持仓股票列表");
        }

        public static void updateCanCancels(Order order, List<JSONObject> responses) throws Exception {
            updateDfFields(order, responses, "canCancels", "当前可撤单股票列表");
        }

        public static void updateTodayClinchs(Order order, List<JSONObject> responses) throws Exception {
            updateDfFields(order, responses, "todayClinchs", "今日成交订单列表");
        }

        public static void updateTodayConsigns(Order order, List<JSONObject> responses) throws Exception {
            updateDfFields(order, responses, "todayConsigns", "今日委托订单列表");
        }
    }

    /**
     * 对订单执行结果进行判定监控check!
     * 收到python响应后, 放入check Map, 等待 结果 check!
     * // @noti: 约定: 订单重发, 均构造类似订单
     * // @noti: 账号状态相关api, 几乎每个策略都相同, 无需 主策略实现特殊的check逻辑!
     * // @noti: 典型的: buy/sell 订单的check逻辑, 应当由 主策 略实现!
     */
    public static class Checker {
        public static void startCheckTransactionStatus(Strategy mainStrategy) {
            Thread checkTask = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        for (Iterator iterator = ordersWaitForCheckTransactionStatusMap.keySet().iterator(); iterator
                                .hasNext(); ) {
                            Order order = (Order) iterator.next();
                            List<JSONObject> responses = ordersWaitForCheckTransactionStatusMap.get(order);

                            String orderType = order.getOrderType();
                            if (AccountStates.orderTypes.contains(orderType)) {
                                AccountStates.checkForAccountStates(order, responses, orderType); // 账户状态更新 五类订单
                            }

                            // todo: 其余类型的订单, check 应当 由主策略决定, 因此实际由主策略实现. 这里仅分发!
                            mainStrategy.checkOrder(order, responses, orderType);
                        }
                    }
                }
            });
            checkTask.setDaemon(true);
            checkTask.setPriority(Thread.MAX_PRIORITY);
            checkTask.setName("checkTransStatus");
            checkTask.start();
            log.warn("check start: 开始check订单成交状况");
        }

    }


    public static void putOrderToWaitExecute(Order order) throws Exception {
        order.addLifePoint(LifePointStatus.WAIT_EXECUTE, "将被放入执行队列");
        log.info("order generated: 已生成订单等待执行: {} ", order.toJsonStr());
        ordersWaitForExecution.put(order);
    }


    /**
     * 订单执行器! 将死循环线程安全优先级队列, 执行订单, 并获得响应!
     * 订单周期变化: wait_execute --> executing --> finish_execute
     * 并放入 成交监控队列
     *
     * @return
     */
    public static class OrderExecutor {
        public static void start() {
            Thread orderExecuteTask = new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        Order order = ordersWaitForExecution.take(); // 最高优先级订单, 将可能被阻塞
                        log.info("order start execute: 开始执行订单: {} [{}] --> {}:{}",
                                order.getRawOrderId(), order.getPriority(), order.getOrderType(), order.getParams());
                        order.addLifePoint(LifePointStatus.EXECUTING, "开始执行订单");
                        List<JSONObject> responses = execOrderUtilSuccess(order);  // 响应列表, 常态仅一个元素. retrying才会多个
                        order.addLifePoint(LifePointStatus.FINISH_EXECUTE, "执行订单完成");
                        order.addLifePoint(LifePointStatus.WAIT_CHECK_TRANSACTION_STATUS, "订单进入check队列, " +
                                "等待check完成");
                        ordersWaitForCheckTransactionStatusMap.put(order, responses);
                    }
                }
            });
            orderExecuteTask.setDaemon(true);
            orderExecuteTask.setPriority(Thread.MAX_PRIORITY);
            orderExecuteTask.setName("orderExecutor");
            orderExecuteTask.start();
            log.warn("start: orderExecutor 开始按优先级执行订单...");
        }

        /**
         * // @key3: 核心重发订单方法. 将使用深拷贝方式, 对订单类型和参数 不进行改变!
         *
         * @param order
         */
        public static void reSendOrder(Order order, Long priority) throws Exception {
            Order newOrder = order.forResend();
            if (priority != null) { // 默认实现优先级将-1,增加1.  可直接传递
                newOrder.setPriority(priority);
            }
            putOrderToWaitExecute(newOrder);
        }

        public static void reSendOrder(Order order) throws Exception {
            reSendOrder(order, null);
        }
    }


    public static List<JSONObject> execOrderUtilSuccess(Order order)
            throws Exception {
        String orderMsg = order.toJsonStr();
        String rawOrderId = order.getRawOrderId();
        sendMessageToPython(channelProducer, orderMsg);
        return comsumeUntilNotRetryingState(rawOrderId);
    }


    public static void handshake() throws IOException, InterruptedException {
        sendMessageToPython(channelProducer, buildHandshakeMsg()); // 发送握手信息,
        waitUtilPythonReady(channelComsumer); // 等待python握手信息.
        log.warn("handshake success: java<->python 握手成功");
    }

    public static void startPythonApp() throws InterruptedException {
        ThreadUtil.execAsync(() -> {
            RuntimeUtil.execForStr(pythonStartCMD); // 运行python仿真程序
        }, true);
        Thread.sleep(1000); // 运行python仿真程序,并稍作等待
    }

    public static void initConnOfRabbitmqAndDualChannel() throws IOException, TimeoutException {
        connOfRabbitmq = connectToRbServer();
        channelProducer = connOfRabbitmq.createChannel();
        initDualChannel(channelProducer);
        channelComsumer = connOfRabbitmq.createChannel();
        initDualChannel(channelComsumer);
    }

    public static void closeDualChannelAndConn() throws IOException, TimeoutException {
        channelProducer.close();
        channelComsumer.close();
        connOfRabbitmq.close();
    }


    /**
     * java端握手消息代码
     * handshake.set("handshakeJavaSide", "java get ready");
     * handshake.set("handshakePythonSide", "and you?");
     * handshake.set("timestamp", System.currentTimeMillis());
     * <p>
     * python 回复消息:
     * handshake_success_response = dict(
     * handshakeJavaSide="java get ready",
     * handshakePythonSide='python get ready',
     * timestamp=int(time.time() * 1000)
     * )
     */
    private static void waitUtilPythonReady(Channel channelComsumer) throws IOException, InterruptedException {
        final boolean[] handshakeSuccess = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message = null;
                try {
                    message = JSONUtil.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("ack: 历史遗留::json解析失败::自动消费清除: {}", msg);
                    channelComsumer.basicAck(envelope.getDeliveryTag(), false); //
                    return;
                }

                if ("java get ready".equals(message.get("handshakeJavaSide"))) {
                    if ("python get ready".equals(message.get("handshakePythonSide"))) {
                        Long timeStamp = Long.valueOf(message.get("timestamp").toString());
                        log.warn("handshaking: 收到来自python的握手成功回复, 时间: {}", timeStamp);
                        channelComsumer.basicAck(envelope.getDeliveryTag(), false); //
                        channelComsumer.basicCancel(consumerTag);
                        handshakeSuccess[0] = true;
                        return;
                    } else {
                        log.error("ack: 握手消息::python尚未准备好::自动消费清除: {}", message);
                        channelComsumer.basicAck(envelope.getDeliveryTag(), false); //
                        return;
                    }
                }
                log.error("ack: 历史遗留::非握手消息::自动消费清除: {}", message);
                channelComsumer.basicAck(envelope.getDeliveryTag(), false); //
            }
        };
        channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);
        while (!handshakeSuccess[0]) {
            Thread.sleep(1); // 只能自行阻塞?
        }
    }

    public static String buildHandshakeMsg() {
        JSONObject handshake = new JSONObject();
        handshake.set("handshakeJavaSide", "java get ready");
        handshake.set("handshakePythonSide", "and you?");
        handshake.set("timestamp", System.currentTimeMillis());
        return JSONUtil.toJsonStr(handshake);
    }

    /**
     * retrying则持续等待, 否则返回执行结果, 可能 success, fail(执行正确, 订单本身原因失败),error(代码未实现)
     *
     * @param rawOrderId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<JSONObject> comsumeUntilNotRetryingState(String rawOrderId)
            throws IOException, InterruptedException {
        List<JSONObject> responses = new CopyOnWriteArrayList<>(); // 保留响应解析成的JO
        final boolean[] finish = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message = null;
                try {
                    message = JSONUtil.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但解析为 JSONObject 失败: {}", msg);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                JSONObject rawOrderFromResponse = null;
                try {
                    rawOrderFromResponse = ((JSONObject) message.get("rawOrder"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 失败: {}", message);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                if (rawOrderFromResponse == null) {
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 为null: {}", message);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                String rawOrderIdOfResponse = rawOrderFromResponse.getStr("rawOrderId");
                if (!rawOrderId.equals(rawOrderIdOfResponse)) { // 需要是对应id
                    log.warn("nack: 收到来自python的消息, 但 rawOrderId 不匹配: should: {}, receive: {}", rawOrderId,
                            rawOrderIdOfResponse);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                log.info("receive response ack: from python: {}", message);
                channelComsumer.basicAck(envelope.getDeliveryTag(), false);
                responses.add(message); // 可能null, 此时需要访问 responsesRaw

                Object state = message.get("state");
                if (!"retrying".equals(state.toString())) {
                    channelComsumer.basicCancel(consumerTag);
                    finish[0] = true;
                }
            }
        };
        // 消费者, 消费 p2j 的队列..
        // 将阻塞, 直到 取消消费?

        channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);
        while (!finish[0]) {
            Thread.sleep(1); // 只能自行阻塞?
        }
        return responses;
    }


    public static void sendMessageToPython(Channel channelProducer, String jsonMsg) throws IOException {
        log.info("send request: to python: {}", jsonMsg);
        channelProducer.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                jsonMsg.getBytes(StandardCharsets.UTF_8));
    }


}
// 540922819
