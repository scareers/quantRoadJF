package com.scareers.gui.ths.simulation.rabbitmq;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.trader.OrderExecutor;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.ths.simulation.trader.SettingsOfTrader.accountStateFlushClientAmount;
import static com.scareers.gui.ths.simulation.trader.SettingsOfTrader.totalClientAmount;

/**
 * description: 依据客户端id, 维护着与一个 python模拟炒作客户端的连接.
 *
 * @author: admin
 * @date: 2022/3/2/002-04:22:05
 */
@Getter
@Setter
public class PythonSimulationClient {
    private static final Log log = LogUtil.getLogger();
    public static Connection connection;
    public static Channel channelProducer;
    public static Channel channelComsumer;


    static {
        try {
            connection = RabbitmqUtil.connectToRbServer();
            channelProducer = connection.createChannel();
            channelComsumer = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * 依据 SettingsOfRb 中配置, 获取 1-N 号客户端, 作为账户状态刷新 客户端池
     *
     * @return
     */
    public static CopyOnWriteArrayList<PythonSimulationClient> createAccountStatesFlushClientPool() {
        Assert.isTrue(accountStateFlushClientAmount >= 1);
        CopyOnWriteArrayList<PythonSimulationClient> res = new CopyOnWriteArrayList<>();
        for (int i = 1; i <= accountStateFlushClientAmount; i++) {
            res.add(new PythonSimulationClient(i));
        }
        return res;
    }

    /**
     * 依据 SettingsOfRb 中配置, 获取 N+1 - M 号客户端, 作为 Trader 客户端池, 主要执行 买卖等api
     *
     * @return
     */
    public static CopyOnWriteArrayList<PythonSimulationClient> createTraderClientPool() {
        Assert.isTrue(totalClientAmount > accountStateFlushClientAmount);
        CopyOnWriteArrayList<PythonSimulationClient> res = new CopyOnWriteArrayList<>();
        for (int i = accountStateFlushClientAmount + 1; i <= totalClientAmount; i++) {
            res.add(new PythonSimulationClient(i));
        }
        return res;
    }

    int id;
    // 双通道6变量, 自动初始化
    String j2pExchangeName;
    String j2pQueueName;
    String j2pRoutingKey;
    // python 发送消息到 java 的单通道设定
    String p2jExchangeName;
    String p2jQueueName;
    String p2jRoutingKey;

    // 标志自身是否空闲! 只有空闲状态, 才能被调度执行订单
    // @key: 只有在 初始化完成, 以及 执行某个订单完成后, free为true!
    private volatile boolean free; // true表示空闲,可执行订单, false表示不可

    public PythonSimulationClient(int correspondPythonClientId) {
        this.free = false; // 初始化过程非空闲

        // 绑定id
        this.id = correspondPythonClientId; // 关联到的python 客户端id;
        // 初始化双通道6常量.
        this.j2pExchangeName = SettingsOfRb.ths_trader_j2p_exchange_prefix + id;
        this.j2pQueueName = SettingsOfRb.ths_trader_j2p_queue_prefix + id;
        this.j2pRoutingKey = SettingsOfRb.ths_trader_j2p_routing_key_prefix + id;
        this.p2jExchangeName = SettingsOfRb.ths_trader_p2j_exchange_prefix + id;
        this.p2jQueueName = SettingsOfRb.ths_trader_p2j_queue_prefix + id;
        this.p2jRoutingKey = SettingsOfRb.ths_trader_p2j_routing_key_prefix + id;

        Assert.isTrue(connection != null);
        Assert.isTrue(channelProducer != null);
        Assert.isTrue(channelComsumer != null);

        this.initDualChannel(channelProducer); // 初始化通道配置.重复并不会耗时
        this.initDualChannel(channelComsumer);

        this.free = true; // 空闲
    }

    public void close() {
        if (connection.isOpen()) {
            try {
                channelProducer.close();
                channelComsumer.close();
                connection.close();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    public void initDualChannel(Channel channel) {
        // java到python的队列
        try {
            channel.exchangeDeclare(this.j2pExchangeName, "fanout", true);
            channel.queueDeclare(this.j2pQueueName, true, false, false, null);
            channel.queueBind(this.j2pQueueName, this.j2pExchangeName, this.j2pRoutingKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // python到java 的队列
        try {
            channel.exchangeDeclare(this.p2jExchangeName, "fanout", true);
            channel.queueDeclare(this.p2jQueueName, true, false, false, null);
            channel.queueBind(this.p2jQueueName, this.p2jExchangeName, this.p2jRoutingKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handshake() throws IOException, InterruptedException {
        this.free = false;
        log.warn("[client_{}] handshake start: 开始尝试与python握手", this.id);
        sendMessageToPython(buildHandshakeMsg());
        waitUtilPythonReady(channelComsumer);
        log.warn("[client_{}] handshake success: java<->python 握手成功", this.id);
        this.free = true;
    }

    /**
     * 握手消息
     *
     * @return
     */
    private String buildHandshakeMsg() {
        JSONObject handshake = new JSONObject();
        handshake.put("handshakeJavaSide", "java get ready");
        handshake.put("handshakePythonSide", "and you?");
        handshake.put("timestamp", System.currentTimeMillis());
        return JSONUtilS.toJsonStr(handshake);
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
        this.free = false;
        final boolean[] handshakeSuccess = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message;
                try {
                    message = JSONUtilS.parseObj(msg);
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
        channelComsumer.basicConsume(p2jQueueName, false, consumer);
        while (!handshakeSuccess[0]) {
            Thread.sleep(1);
        }
    }

    public synchronized void sendMessageToPython(String jsonMsg) throws IOException {
        this.free = false;
        log.info("[client_{}] java --> python: {}", id, jsonMsg);
        channelProducer
                .basicPublish(this.j2pExchangeName, this.j2pRoutingKey, MINIMAL_PERSISTENT_BASIC,
                        jsonMsg.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 执行订单, 执行器调用(执行器从待执行队列获取最高优先级订单后), 通常不手动调用.
     *
     * @param order
     * @return
     * @throws Exception
     * @key3
     * @warning
     * @see OrderExecutor
     */
    public List<Response> execOrderUtilSuccess(Order order)
            throws Exception {
        this.free = false;
        String orderMsg = order.toJsonStrForTrans();
        String rawOrderId = order.getRawOrderId();
        this.sendMessageToPython(orderMsg);
        List<Response> responses = comsumeUntilNotRetryingState(rawOrderId, this);
        this.free = true;
        return responses;
    }

    public Connection getConnection() {
        return connection;
    }

    public Channel getChannelProducer() {
        return channelProducer;
    }

    public Channel getChannelComsumer() {
        return channelComsumer;
    }

    /**
     * retrying则持续等待, 否则返回执行结果, 可能 success, fail(执行正确, 订单本身原因失败)
     *
     * @param rawOrderId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Response> comsumeUntilNotRetryingState(String rawOrderId, PythonSimulationClient client)
            throws IOException, InterruptedException {
        this.free = false;
        List<Response> responses = new CopyOnWriteArrayList<>(); // 保留响应解析成的JO
        final boolean[] finish = {false};
        Consumer consumer = new DefaultConsumer(this.getChannelComsumer()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message;
                try {
                    message = JSONUtilS.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但解析为 JSONObject 失败: {}", msg);
                    client.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                JSONObject rawOrderFromResponse;
                try {
                    rawOrderFromResponse = ((JSONObject) message.get("rawOrder"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 失败: {}", message);
                    client.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                if (rawOrderFromResponse == null) {
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 为null: {}", message);
                    client.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                String rawOrderIdOfResponse = rawOrderFromResponse.getString("rawOrderId");
                if (!rawOrderId.equals(rawOrderIdOfResponse)) { // 需要是对应id
                    log.warn("nack: 收到来自python的消息, 但 rawOrderId 不匹配: should: {}, receive: {}", rawOrderId,
                            rawOrderIdOfResponse);
                    client.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                log.info("[as] java <-- python: {}", message);
                client.getChannelComsumer().basicAck(envelope.getDeliveryTag(), false);
                responses.add(new Response(message)); // 可能null, 此时需要访问 responsesRaw

                Object state = message.get("state");
                if (!"retrying".equals(state.toString())) {
                    client.getChannelComsumer().basicCancel(consumerTag);
                    finish[0] = true;
                }
            }
        };

        try {
            client.getChannelComsumer().basicConsume(client.getP2jQueueName(), false, consumer);
            while (!finish[0]) {
                Thread.sleep(1); // 阻塞直到 非!retrying状态
            }
        } catch (AlreadyClosedException e) {
            e.printStackTrace();
            CommonUtil.sendEmailSimple("AccountStates: rabbitmq通道关闭异常, 请尝试重启程序", e.getMessage(), false);
            System.exit(1); // 通道错误将退出程序, 重启即可修复
        }

        return responses;
    }

    public static void closeClientPool(CopyOnWriteArrayList<PythonSimulationClient> clientPool) {
        for (PythonSimulationClient client : clientPool) {
            client.close();
        }
    }

    public static void clientPoolHandshake(CopyOnWriteArrayList<PythonSimulationClient> clientPool)
            throws IOException, InterruptedException {
        for (PythonSimulationClient pythonSimulationClient : clientPool) {
            pythonSimulationClient.handshake(); // 顺序握手,没必要异步. 几乎只会消耗在 最慢启动的python客户端,
        }
    }

    public static PythonSimulationClient getFreeClientFromPool(
            CopyOnWriteArrayList<PythonSimulationClient> clientPool) {
        while (true) {
            ArrayList<PythonSimulationClient> freeClients = new ArrayList<>();
            for (PythonSimulationClient client : clientPool) {
                if (client.isFree()) {
                    freeClients.add(client);
                }
            }
            if (freeClients.size() > 0) {
                PythonSimulationClient client = RandomUtil.randomEle(freeClients);
                client.setFree(false);
                return client;
            } else {
                ThreadUtil.sleep(10); //
            }
        }
    }

}
