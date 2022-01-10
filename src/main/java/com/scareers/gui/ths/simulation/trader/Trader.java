package com.scareers.gui.ths.simulation.trader;

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

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
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
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.connectToRbServer;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.initDualChannel;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.*;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: ths 自动交易程序
 * java发送消息 --> python执行 --> python发送结果 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
@Setter
@Getter
public class Trader {
    // python程序启动cmd命令.  PYTHONPATH 由该程序自行保证! --> sys.path.append()
    private static String pythonStartCMD = "C:\\keys\\Python37-32\\python.exe " +
            "C:/project/python/quantRoad/gui/ths_simulation_trade/main_simulation_trade.py";
    private static final Log log = LogUtil.getLogger();
    private static Channel channelComsumer;
    private static Channel channelProducer;
    private static Connection connOfRabbitmq;


    public static void main(String[] args) throws Exception {
        Trader trader = new Trader();
        Strategy mainStrategy = new LowBuyHighSellStrategy(
                LowBuyHighSellStrategy.class.getName(), trader); // 获取核心策略对象, 达成与trader关联

        // trader.startPythonApp(); // 是否自启动python程序, 单机可用但无法查看python cmd
        trader.handshake(); // 与python握手可控

        // 启动执行器, 将遍历优先级队列, 发送订单到python, 并获取响应
        trader.getOrderExecutor().start();

        // 启动成交状况检测, 对每个订单的响应, 进行处理. 成功或者重发等
        trader.getChecker().startCheckTransactionStatus(mainStrategy);

        // 启动账户资金获取程序
        trader.getAccountStates().startFlush();
        waitUtil(trader.getAccountStates()::alreadyInitialized, 120 * 1000, 100,
                "首次账户资金状态刷新完成"); // 需要等待初始化完成!

        mainStrategy.initYesterdayHolds(); // 将昨日持仓更新到股票池.  将昨日收盘持仓和资金信息, 更新到静态属性

        // fs成交开始抓取, 股票池包含今日选股(for buy, 自动包含两大指数), 以及昨日持仓(for sell)
        FsTransactionFetcher fsTransactionFetcher =
                FsTransactionFetcher.getInstance(mainStrategy.getStockPool(), 10, "15:10:00", 1000, 100, 32);

        fsTransactionFetcher.startFetch();  // 策略所需股票池实时数据抓取. 核心字段: fsTransactionDatas
        waitUtil(() -> fsTransactionFetcher.getFirstTimeFinish().get(), 3600 * 1000, 100, "第一次tick数据抓取完成"); //

        mainStrategy.startDealWith();


        trader.manualInteractive(); // 开始人工交互, 可以人工调用订单, 可以人工打印信息等, 可以 gui程序.  应阻塞!
        trader.closeDualChannelAndConn(); // 关闭连接
        fsTransactionFetcher.stopFetch(); // 停止数据抓 取, 非立即.
    }

    // 属性: 3大队列, 将初始化为空队列/map
    /**
     * 核心待执行订单优先级队列. 未指定容量, put将不会阻塞. take将可能阻塞
     */
    private PriorityBlockingQueue<Order> ordersWaitForExecution;
    /**
     * 核心检测订单执行状态线程安全Map. 将遍历队列元素, 当元素check通过, 则去除元素,订单彻底完成.
     * key:value--> 订单对象: 对应的线程安全响应列表
     */
    private ConcurrentHashMap<Order, List<Response>> ordersWaitForCheckTransactionStatusMap;

    /**
     * check 后, 将被放入完成队列. check信息, 将被放入 order.生命周期 check_transaction_status的描述中.
     * 最后将生命周期设置为finish, 放入此Map.
     *
     * @noti 某些重发的订单, 原始订单对象 应添加 RESENDED 生命周期后, 再放入本map, 含义为 "resended_finish"
     */
    private ConcurrentHashMap<Order, List<Response>> ordersSuccessFinished;
    /**
     * 重发后视为完成的原始订单,resended 生命周期的 payload, 带有当次重发的"新订单" 的 rawOrderId
     */
    private ConcurrentHashMap<Order, List<Response>> ordersResendFinished;

    // 各大子组件, 均单例模式
    private OrderExecutor orderExecutor;
    private Checker checker;
    private AccountStates accountStates;
    private Strategy strategy; // 将在 Strategy 的构造器中, 调用 this.trader.setStrategy(this), 达成互连

    /**
     * 构造器需要
     *
     * @param orderExecutor
     * @param checker
     * @param accountStates
     */
    public Trader() throws IOException, TimeoutException {
        this.ordersWaitForExecution = new PriorityBlockingQueue<>();
        this.ordersWaitForCheckTransactionStatusMap = new ConcurrentHashMap<>();
        this.ordersSuccessFinished = new ConcurrentHashMap<>();
        this.orderExecutor = new OrderExecutor(this);
        this.checker = Checker.getInstance(this);
        this.accountStates = new AccountStates(this);
        this.initConnOfRabbitmqAndDualChannel(); // 初始化mq连接与双通道
        // this.strategy 将在 Strategy 的构造器中, 调用 this.trader.setStrategy(this), 达成互连
    }

    /**
     * @param order 被重发的原始订单对象, 将保留3项属性, 其余8项属性更新.
     * @return 返回重发的新订单的 id.
     * @key3 核心重发订单方法. 将使用深拷贝方式, 对订单类型和参数 不进行改变!
     * @see Order.forResend()
     */
    public String reSendOrder(Order order, Long priority) throws Exception {
        Order newOrder = order.forResend();
        if (priority != null) { // 默认实现优先级将-1
            newOrder.setPriority(priority);
        }
        putOrderToWaitExecute(newOrder);
        return newOrder.getRawOrderId();
    }

    public String reSendOrder(Order order) throws Exception {
        return reSendOrder(order, null);
    }

    private void manualInteractive() throws Exception {

        while (true) {
            Scanner input = new Scanner(System.in);
            String info = input.next();
            System.out.println(info);
            if ("q".equals(info)) {
                break;
            } else if ("s".equals(info)) {
                accountStates.showFields();

            } else if ("g".equals(info)) {
//                JFrameDemo.main0(null);
            }
        }
    }


    public void putOrderToWaitExecute(Order order) throws Exception {
        order.addLifePoint(LifePointStatus.WAIT_EXECUTE, "wait_execute: 放入执行队列,等待执行");
        log.info("order generated: 生成订单放入执行队列: {} ", order.toJsonStr());
        ordersWaitForExecution.put(order);
    }


    public List<Response> execOrderUtilSuccess(Order order)
            throws Exception {
        String orderMsg = order.toJsonStr();
        String rawOrderId = order.getRawOrderId();
        sendMessageToPython(channelProducer, orderMsg);
        return comsumeUntilNotRetryingState(rawOrderId);
    }


    public void handshake() throws IOException, InterruptedException {
        sendMessageToPython(channelProducer, buildHandshakeMsg()); // 发送握手信息,
        waitUtilPythonReady(channelComsumer); // 等待python握手信息.
        log.warn("handshake success: java<->python 握手成功");
    }

    public void startPythonApp() throws InterruptedException {
        ThreadUtil.execAsync(() -> {
            RuntimeUtil.execForStr(pythonStartCMD); // 运行python仿真程序
        }, true);
        Thread.sleep(1000); // 运行python仿真程序,并稍作等待
    }

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
    private void waitUtilPythonReady(Channel channelComsumer) throws IOException, InterruptedException {
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

    public String buildHandshakeMsg() {
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
    public List<Response> comsumeUntilNotRetryingState(String rawOrderId)
            throws IOException, InterruptedException {
        List<Response> responses = new CopyOnWriteArrayList<>(); // 保留响应解析成的JO
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
                responses.add(new Response(message)); // 可能null, 此时需要访问 responsesRaw

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
            Thread.sleep(1); // 阻塞直到 非!retrying状态
        }
        return responses;
    }


    /**
     * 经由rabbitmq 发送任意消息到python, 常为订单,另有握手信息等
     *
     * @param channelProducer
     * @param jsonMsg
     * @throws IOException
     */
    public void sendMessageToPython(Channel channelProducer, String jsonMsg) throws IOException {
        log.info("--> python: {}", jsonMsg);
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
    public void successFinishOrder(Order order, List<Response> responses, String description) {
        ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(Order.LifePointStatus.FINISH, description);
        ordersSuccessFinished.put(order, responses);
    }

    public void successFinishOrder(Order order, List<Response> responses) {
        successFinishOrder(order, responses, "finish: 订单成功完成");
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
    public void resendFinishOrder(Order order, List<Response> responses, String newOrderId,
                                  String descriptionForFinish) {
        ordersWaitForCheckTransactionStatusMap.remove(order);
        order.addLifePoint(LifePointStatus.RESENDED,
                StrUtilS.format("resended: 原始订单已被重发,新订单id: {}", newOrderId),
                newOrderId);
        order.addLifePoint(Order.LifePointStatus.FINISH, descriptionForFinish);
        ordersResendFinished.put(order, responses);
    }

    public void resendFinishOrder(Order order, List<Response> responses, String newOrderId) {
        resendFinishOrder(order, responses, "resended_finish: 订单已被重发! 订单完成");
    }


}

