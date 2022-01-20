package com.scareers.gui.ths.simulation.trader;

/**
 * ths 自动交易程序子系统:(主要面向过程编程)
 * 1.数据获取系统:
 * FsTransactionFetcher 获取dc实时成交数据,3s tick
 * StockApi 以及eastmoney包其他API, 访问dc API, 的其他数据项, 可参考 python efinance模块,
 * 2.订单生成系统: Order / OrderFactory
 * Order 作为基类, OrderFactory 作为快捷生产的工厂类
 * 3.订单发送与响应接收: 通过rabbitmq作为中间件, 与python交互. sendMessageToPython
 * 单个订单, 收发一次, 封装为一次交易过程, 串行. 类似rpc
 * 4.订单优先级执行器系统: PriorityBlockingQueue<Order> ordersWaitForExecution
 * 所有订单应进入优先级队列, 由订单执行器, 统一依据优先级调度, 常态优先执行buy/sell订单
 * 5.账户状态监控系统: AccountStates
 * 不断发送查询api, 以尽可能快速更新账户信息, 使得订单发送前较为合理资金调度
 * 6.成交状态监控系统: ordersWaitForCheckTransactionStatusMap  check系统
 * 某订单成功执行后进入成交状态监控队列, 将根据系统5的信息, 确定订单成交状况
 */

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.rabbitmq.client.*;
import com.scareers.datasource.eastmoney.fstransaction.FsTransactionFetcher;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.LifePointStatus;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.Strategy;
import com.scareers.utils.StrUtilS;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.connectToRbServer;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.initDualChannel;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.*;
import static com.scareers.utils.CommonUtil.waitForever;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: ths 自动交易程序
 * java发送消息 --> python执行 --> python发送响应 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @key3 实测gui用 getter访问字段将访问空队列,觉察不到变化. 需要将队列设置为 静态属性, 方可及时察觉到变化!
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
@Getter
@Setter
public class Trader {
    private static final Log log = LogUtil.getLogger();
    public static volatile Trader INSTANCE;
    public static volatile int allOrderAmount = 0;

    public static Trader getInstance() throws Exception {
        // todo: 待完成
        if (INSTANCE == null) {
            INSTANCE = new Trader(10000, Order.PRIORITY_MEDIUM, 60000, 2);
        }
        return INSTANCE;
    }

    public static void main(String[] args) throws Exception {
        main0();
    }

    public static void main0() throws Exception {
        Trader trader = Trader.getInstance();
        // TraderUtil.startPythonApp(); // 是否自启动python程序, 单机可用但无法查看python状态
        trader.handshake(); // 与python握手, 握手不通过订单执行器, 直接收发握手消息, 直到握手成功

        // 启动执行器, 将遍历优先级队列, 发送订单到python, 并获取响应
        trader.getOrderExecutor().start();
        // 启动成交状况检测, 对每个订单的响应, 进行处理. 成功或者重发等.   两者启动后方可启动 账户状态信息获取
        // check可能用到strategy, 此时strategy为null, 因此需要保证strategy被设置前, 不调用策略相关api, 此处仅调用账户信息相关api
        trader.getChecker().startCheckTransactionStatus();

        // 启动账户资金获取程序
        trader.getAccountStates().startFlush(); // 此时并未用到 strategy, 因此 check程序不会触发空指针异常
        waitUtil(trader.getAccountStates()::alreadyInitialized, 120 * 1000, 10,
                "首次账户资金状态刷新完成"); // 等待第一次账户状态5信息获取完成. 首次优先级为 0L
        // 直到此时才实例化策略对象, 绑定到 trader
        // todo
        Strategy mainStrategy = LowBuyHighSellStrategy.getInstance(trader, LowBuyHighSellStrategy.class.getName(),
                new ArrayList<>(), // 强制排除选股结果
                20, // 期望选股数量
                false, // 偏向更多选股结果
                Arrays.asList(0, 1) // 核心, 哪天买哪天卖的算法?
        ); // 核心策略对象, 达成与trader绑定 mainStrategy.bindSelf() ,无需显式调用

        // fs成交开始抓取, 股票池通常包含今日选股(for buy, 自动包含两大指数), 以及昨日持仓股票(for sell)
        FsTransactionFetcher fsTransactionFetcher =
                FsTransactionFetcher.getInstance(mainStrategy.getStockPool(), 10,
                        "15:10:00", 1000, 100, 32);
        trader.setFsTransactionFetcher(fsTransactionFetcher); // 需要显式绑定
        fsTransactionFetcher.startFetch();  // 策略所需股票池实时数据抓取. 核心字段: fsTransactionDatas

        // 需等待第一次fs抓取完成后, 通常很快, 主策略开始执行买卖
        waitUtil(() -> fsTransactionFetcher.getFirstTimeFinish().get(), 3600 * 1000, 100, "第一次tick数据抓取完成");
        mainStrategy.startDealWith();

        trader.manualInteractive(); // 开始交互, 必须死循环.
        trader.closeDualChannelAndConn(); // 关闭连接
        fsTransactionFetcher.stopFetch(); // 停止fs数据抓取, 非立即, 软关闭
    }

    // 属性: 4大队列, 将初始化为 空队列/map
    /**
     * 核心待执行订单优先级队列. 未指定容量, put将不会阻塞. take将可能阻塞
     */
    public static volatile PriorityBlockingQueue<Order> ordersWaitForExecution = new PriorityBlockingQueue<>();


    /**
     * 存放 所有曾出现过订单, 一旦订单加入 ordersWaitForExecution, 则加入此key,value为空列表.
     * 一旦获取到响应, 则设定value.
     */
    public static volatile Hashtable<Order, List<Response>> ordersAllMap = new Hashtable<>();


    /**
     * 核心检测订单执行状态线程安全Map. 将遍历队列元素, 当元素check通过, 则去除元素,订单彻底完成.
     * key:value--> 订单对象: 对应的线程安全响应列表
     */
    public static volatile Hashtable<Order, List<Response>> ordersWaitForCheckTransactionStatusMap = new Hashtable<>();


    /**
     * check 后, 将被放入完成队列. check信息, 将被放入 order.生命周期 check_transaction_status的描述中.
     * 最后将生命周期设置为finish, 放入此Map.
     *
     * @noti 某些重发的订单, 原始订单对象 应添加 RESENDED 生命周期后, 再放入本map, 含义为 "resended_finish"
     */
    public static volatile Hashtable<Order, List<Response>> ordersSuccessFinished = new Hashtable<>();

    /**
     * 重发后视为完成的原始订单,resended 生命周期的 payload, 带有当次重发的"新订单" 的 rawOrderId
     */
    public static volatile Hashtable<Order, List<Response>> ordersResendFinished = new Hashtable<>();


    // 各大子组件, 均单例模式
    public volatile OrderExecutor orderExecutor;
    public volatile Checker checker;
    public volatile AccountStates accountStates;
    public volatile Strategy strategy; // 将在 Strategy 的构造器中, 调用 this.trader.setStrategy(this), 达成关连
    public volatile FsTransactionFetcher fsTransactionFetcher; // 分时成交获取器, 需手动实例化后绑定

    // 通道, 自行初始化
    public volatile Channel channelComsumer; // 自行初始化
    public volatile Channel channelProducer;
    public volatile Connection connOfRabbitmq;

    /**
     * 参数为子组件构建参数, 后缀ForXy 表明了用于构建哪个子控件
     * ForAS --> AccountStates
     *
     * @param flushIntervalForAS
     * @param commonApiPriorityForAS
     * @param priorityRaiseTimeThresholdForAS
     * @param priorityRaiseForAS
     * @throws IOException
     * @throws TimeoutException
     */
    public Trader(long flushIntervalForAS, long commonApiPriorityForAS, long priorityRaiseTimeThresholdForAS,
                  long priorityRaiseForAS) throws IOException, TimeoutException {

        this.orderExecutor = OrderExecutor.getInstance(this);
        this.checker = Checker.getInstance(this);
        this.accountStates = AccountStates.getInstance(this, flushIntervalForAS, commonApiPriorityForAS,
                priorityRaiseTimeThresholdForAS, priorityRaiseForAS);
        this.initConnOfRabbitmqAndDualChannel(); // 初始化mq连接与双通道
        // this.strategy 将在 Strategy 的构造器中, 调用 this.trader.setStrategy(this), 达成互连
    }

    /*
     * 通信初始化与关闭
     */

    public void initConnOfRabbitmqAndDualChannel() throws IOException, TimeoutException {
        connOfRabbitmq = connectToRbServer();
        channelProducer = connOfRabbitmq.createChannel();
        initDualChannel(channelProducer);
        channelComsumer = connOfRabbitmq.createChannel();
        initDualChannel(channelComsumer);
    }

    public void closeDualChannelAndConn() throws IOException, TimeoutException {
        channelProducer.close();
        channelComsumer.close();
        connOfRabbitmq.close();
    }

    /**
     * 命令行交互, 弃用
     *
     * @throws Exception
     */
    private void manualInteractive() throws Exception {
//        Scanner input = new Scanner(System.in);
//        while (true) {
//            String info = input.next();
//            System.out.println(info);
//            if ("q".equals(info)) {
//                break;
//            } else if ("s".equals(info)) {
//            } else if ("g".equals(info)) {
//            }
//        }
        while (true) {
            waitForever();
        }
    }

    /*
     * 握手相关3方法
     */

    public void handshake() throws IOException, InterruptedException {
        log.warn("handshake start: 开始尝试与python握手");
        sendMessageToPython(channelProducer, buildHandshakeMsg());
        waitUtilPythonReady(channelComsumer);
        log.warn("handshake success: java<->python 握手成功");
    }

    public String buildHandshakeMsg() {
        JSONObject handshake = new JSONObject();
        handshake.set("handshakeJavaSide", "java get ready");
        handshake.set("handshakePythonSide", "and you?");
        handshake.set("timestamp", System.currentTimeMillis());
        return JSONUtil.toJsonStr(handshake);
    }


    /**
     * java端握手消息代码
     * handshake.set("handshakeJavaSide", "java get ready");
     * handshake.set("handshakePythonSide", "and you?");
     * handshake.set("timestamp", System.currentTimeMillis());
     * python 回复消息:
     * handshake_success_response = dict(
     * handshakeJavaSide="java get ready",
     * handshakePythonSide='python get ready',
     * timestamp=int(time.time() * 1000)
     * )
     *
     * @noti 历史遗留消息将被消费!正常ack 相当于 遗弃
     */
    private void waitUtilPythonReady(Channel channelComsumer) throws IOException, InterruptedException {
        final boolean[] handshakeSuccess = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message;
                try {
                    message = JSONUtil.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("ack: 历史遗留::json解析失败::自动消费清除: {}", msg);
                    channelComsumer.basicAck(envelope.getDeliveryTag(), false);
                    return;
                }

                if ("java get ready".equals(message.get("handshakeJavaSide"))) {
                    if ("python get ready".equals(message.get("handshakePythonSide"))) {
                        Long timeStamp = Long.valueOf(message.get("timestamp").toString());
                        log.warn("handshaking: 收到来自python的握手成功回复, 时间戳: {}", timeStamp);
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
            Thread.sleep(1);
        }
    }

    /*
     * 核心方法!!
     */

    /**
     * 将新订单放入执行队列, 等待执行. 添加生命周期 WAIT_EXECUTE
     *
     * @param order
     * @throws Exception
     */
    public void putOrderToWaitExecute(Order order) throws Exception {
        order.addLifePoint(LifePointStatus.WAIT_EXECUTE, "wait_execute: 放入执行队列,等待执行");
        ordersWaitForExecution.put(order);
        ordersAllMap.put(order, Arrays.asList()); // 暂无响应
        allOrderAmount++;
        log.info("order enqueue: {} ", order.toString());
//        log.info("order enqueued: {} ", ordersAllMap.size());
    }

    /**
     * 经由rabbitmq 发送任意消息到python, 常为订单,另有握手信息等
     *
     * @param channelProducer
     * @param jsonMsg
     * @throws IOException
     */
    public void sendMessageToPython(Channel channelProducer, String jsonMsg) throws IOException {
        log.info("java --> python: {}", jsonMsg);
        channelProducer.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                jsonMsg.getBytes(StandardCharsets.UTF_8));
    }


    /*
     * 订单结束相关处理方法
     */

    /**
     * 成功, 逻辑上正常的完成某订单. 此时业务逻辑上订单一般拥有完整生命周期.
     * 从 waitCheck 队列 --> finish 队列        // Map类型
     * FINISH 生命周期简单构造
     *
     * @param order
     * @param responses
     * @param description
     */
    public void successFinishOrder(Order order, List<Response> responses) {
        ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(Order.LifePointStatus.FINISH, "finish: 订单成功完成");
        ordersSuccessFinished.put(order, responses);
    }


    /**
     * 订单经由业务逻辑判定, 已经被重发, 策略后期应当跟踪重发的新订单!
     * 此时原订单视为 finish. 将被放入 ordersResendFinished 队列
     *
     * @param order
     * @param responses
     * @param descriptionForFinish
     * @noti 被重发的新订单的 rawOrderId, 将作为 RESENDED 生命周期的 payload 属性,保留访问
     */
    public void resendFinishOrder(Order order, List<Response> responses, String newOrderId) {
        ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(LifePointStatus.RESENDED,
                StrUtilS.format("resended: 原始订单已被重发,新订单id: {}", newOrderId),
                newOrderId);
        order.addLifePoint(Order.LifePointStatus.FINISH, "resended_finish: 订单已被重发! 订单完成");
        ordersResendFinished.put(order, responses);
    }


    /*
     * 重发方法, 以及立即已 重发_完成 方式, 完成订单快捷方法 reSendOrder + resendFinishOrder
     */

    /**
     * @param order 被重发的原始订单对象, 将保留3项属性, 其余8项属性更新.
     * @return 返回重发的新订单的 id.
     * @key3 核心重发订单方法. 将使用深拷贝方式, 对订单类型和参数 不进行改变!
     * @see Order.forResend()
     */
    public String reSendOrder(Order order, Long newOrderPriority) throws Exception {
        Order newOrder = order.forResend();
        if (newOrderPriority != null) {
            // 默认实现优先级将-1,也可给定具体值
            newOrder.setPriority(newOrderPriority);
        }
        putOrderToWaitExecute(newOrder);
        return newOrder.getRawOrderId();
    }

    public String reSendOrder(Order order) throws Exception {
        return reSendOrder(order, null);
    }

    /**
     * 快捷方法, 重发订单, 并且原始订单进入 resendFinish 队列, 完成之
     *
     * @param order
     */
    public void reSendAndFinishOrder(Order order, List<Response> responses, Long newOrderPriority) throws Exception {
        String newOrderId = reSendOrder(order, newOrderPriority);
        resendFinishOrder(order, responses, newOrderId);
    }

    /**
     * 将采用 优先级-1(增大1) 的默认方式, 设置新订单优先级
     *
     * @param order
     * @param responses
     * @throws Exception
     */
    public void reSendAndFinishOrder(Order order, List<Response> responses) throws Exception {
        String newOrderId = reSendOrder(order);
        resendFinishOrder(order, responses, newOrderId);
    }

    public static int getAllOrderAmount() {
        return allOrderAmount;
    }

    public static PriorityBlockingQueue<Order> getOrdersWaitForExecution() {
        return ordersWaitForExecution;
    }

    public static Hashtable<Order, List<Response>> getOrdersAllMap() {
        return ordersAllMap;
    }

    public static Hashtable<Order, List<Response>> getOrdersWaitForCheckTransactionStatusMap() {
        return ordersWaitForCheckTransactionStatusMap;
    }

    public static Hashtable<Order, List<Response>> getOrdersSuccessFinished() {
        return ordersSuccessFinished;
    }

    public static Hashtable<Order, List<Response>> getOrdersResendFinished() {
        return ordersResendFinished;
    }

    public static void setOrdersWaitForExecution(
            PriorityBlockingQueue<Order> ordersWaitForExecution) {
        Trader.ordersWaitForExecution = ordersWaitForExecution;
    }

    public static void setOrdersAllMap(
            Hashtable<Order, List<Response>> ordersAllMap) {
        Trader.ordersAllMap = ordersAllMap;
    }

    public static void setOrdersWaitForCheckTransactionStatusMap(
            Hashtable<Order, List<Response>> ordersWaitForCheckTransactionStatusMap) {
        Trader.ordersWaitForCheckTransactionStatusMap = ordersWaitForCheckTransactionStatusMap;
    }

    public static void setOrdersSuccessFinished(
            Hashtable<Order, List<Response>> ordersSuccessFinished) {
        Trader.ordersSuccessFinished = ordersSuccessFinished;
    }

    public static void setOrdersResendFinished(
            Hashtable<Order, List<Response>> ordersResendFinished) {
        Trader.ordersResendFinished = ordersResendFinished;
    }
}

