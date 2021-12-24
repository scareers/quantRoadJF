/**
 * ths 自动交易程序子系统:(主要面向过程编程)
 * 1.数据获取系统:
 * 1.FSTransactionFetcher 获取dc实时成交数据,3stick. 数据存储于静态属性. 异步存储于mysql
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

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.rabbitmq.client.*;
import com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.rabbitmq.order.Order.LifePoint;
import com.scareers.gui.rabbitmq.order.Order.LifePointStatus;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.log.LogUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.rabbitmq.OrderFactory.generateCancelConcreteOrder;
import static com.scareers.gui.rabbitmq.SettingsOfRb.*;

/**
 * description: ths 自动交易程序
 * java发送消息 --> python执行 --> python发送结果 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
public class Trader {
    // python程序启动cmd命令.  PYTHONPATH 由该程序自行保证! --> sys.path.append()
    public static String pythonStartCMD = "C:\\keys\\Python37-32\\python.exe " +
            "C:/project/python/quantRoad/gui/ths_simulation_trade/main_simulation_trade.py";
    private static final Log log = LogUtils.getLogger();
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
    public static ConcurrentHashMap<Order, List<JSONObject>> ordersWaitForCheckTransactionStatusMap = new LinkedBlockingQueue<>();


    public static void main(String[] args) throws Exception {
        ThreadUtil.execAsync(() -> {
            try {
                FSTransactionFetcher.startFetch();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("error: 数据获取程序出现错误!");
            }
        }, true); // 数据获取程序运行

        initConnOfRabbitmqAndDualChannel(); // 初始化mq连接与双通道
        // startPythonApp(); // 是否自启动python程序
        handshake(); // 握手可控
        // 等待第一次抓取完成.
        CommonUtils.waitUtil(() -> FSTransactionFetcher.firstTimeFinish.get(), 10000, 100); // 等待第一次完成

        // 启动执行器, 将遍历优先级队列, 发送订单到python, 并获取响应
        startOrderExecutor();
        Thread.sleep(200);
        startDummyStrategy();
        Thread.sleep(200);
        startCheckTransactionStatus();
        Thread.sleep(200);

        Thread.sleep(1000000000); // 等待退出.

        closeDualChannelAndConn(); // 关闭连接
        FSTransactionFetcher.stopFetch(); // 停止数据抓取
    }

    private static void startCheckTransactionStatus() {
        for (Iterator iterator = ordersWaitForCheckTransactionStatusMap.iterator(); iterator.hasNext(); ) {
            Order order = iterator.next();
        }

    }

    /**
     * 虚拟的策略, 随机生成订单, 放入队列执行
     */
    public static void startDummyStrategy() {
        Thread strategyTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
                    Thread.sleep(sleep * 1000);
                    Order order = null;
                    int type = RandomUtil.randomInt(12);
                    if (type < 3) {
                        order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGHEST);
                    } else if (type < 6) {
                        order = OrderFactory.generateSellOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGH);
                    } else if (type < 8) {
                        order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_MEDIUM);
                    } else if (type < 10) {
                        order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_LOWEST);
                    } else {
                        order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
                    }
                    order.getLifePoints().add(new LifePoint(LifePointStatus.WAIT_EXECUTE, "将被放入执行队列"));
                    log.info("order generated: 已生成订单: {}", order.toJsonStr());
                    ordersWaitForExecution.put(order);
                }
            }
        });

        strategyTask.setDaemon(true);
        strategyTask.setPriority(Thread.MAX_PRIORITY);
        strategyTask.setName("dummyStrategy");
        strategyTask.start();
        log.warn("start: dummyStrategy 开始执行策略生成订单...");
    }

    /**
     * 订单执行器! 将死循环线程安全优先级队列, 执行订单, 并获得响应!
     * 订单周期变化: wait_execute --> executing --> finish_execute
     * 并放入 成交监控队列
     *
     * @return
     */
    public static void startOrderExecutor() {
        Thread orderExecuteTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    Order order = ordersWaitForExecution.take(); // 最高优先级订单, 将可能被阻塞
                    log.info("order start execute: 开始执行订单: {} [{}] --> {}:{}",
                            order.getRawOrderId(), order.getPriority(), order.getOrderType(), order.getParams());
                    order.getLifePoints().add(new LifePoint(LifePointStatus.EXECUTING, "开始执行订单"));
                    List<JSONObject> responses = execOrderUtilSuccess(order);  // 响应列表, 常态仅一个元素. retrying才会多个
                    order.getLifePoints().add(new LifePoint(LifePointStatus.FINISH_EXECUTE, "执行订单完成"));
                    order.getLifePoints().add(new LifePoint(LifePointStatus.CHECK_TRANSACTION_STATUS, "订单进入check队列, " +
                            "等待check完成"));
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


    public static List<JSONObject> execOrderUtilSuccess(Order order)
            throws Exception {
        String orderMsg = order.toJsonStr();
        String rawOrderId = order.getRawOrderId();
        sendMessageToPython(channelProducer, orderMsg);
        return comsumeUntilSuccessState(rawOrderId);
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


    public static List<JSONObject> comsumeUntilSuccessState(String rawOrderId)
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


    public static Connection connectToRbServer() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        // 连接IP
        factory.setHost(host);
        // 连接端口
        factory.setPort(port);
        // 虚拟机
        factory.setVirtualHost(virtualHost);
        // 用户
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setRequestedHeartbeat(0);

        // 建立连接
        Connection conn = factory.newConnection();
        log.info("connecting: 连接到rabbitmq...");
        return conn;
    }

    /**
     * 初始化 j2p/p2j 两个(交换机-队列-路由键) 双通 道
     *
     * @param channel
     * @return
     */
    public static boolean initDualChannel(Channel channel) {
        // java到python的队列
        try {
            channel.exchangeDeclare(ths_trader_j2p_exchange, "fanout", true);
            channel.queueDeclare(ths_trader_j2p_queue, true, false, false, null);
            channel.queueBind(ths_trader_j2p_queue, ths_trader_j2p_exchange, ths_trader_j2p_routing_key);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // python到java 的队列
        try {
            channel.exchangeDeclare(ths_trader_p2j_exchange, "fanout", true);
            channel.queueDeclare(ths_trader_p2j_queue, true, false, false, null);
            channel.queueBind(ths_trader_p2j_queue, ths_trader_p2j_exchange, ths_trader_p2j_routing_key);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
