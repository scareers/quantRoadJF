package com.scareers.gui.rabbitmq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.XML;
import cn.hutool.log.Log;
import com.rabbitmq.client.*;
import com.scareers.utils.log.LogUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.rabbitmq.SettingsOfRb.*;
import static com.scareers.gui.rabbitmq.Producer.*;

/**
 * description: rabbitmq 工具类. 对于每个order api, 使用串行方式调用
 * 即 java发送消息 --> python执行 --> python发送结果 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
public class RbUtils {
    // python程序启动cmd命令.  PYTHONPATH 由该程序自行保证! --> sys.path.append()
    public static String pythonStartCMD = "C:\\keys\\Python37-32\\python.exe " +
            "C:/project/python/quantRoad/gui/ths_simulation_trade/main_simulation_trade.py";
    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        Connection conn = connectToRbServer();
        Channel channelProducer = conn.createChannel();
        initDualChannel(channelProducer);
        Channel channelComsumer = conn.createChannel();
        initDualChannel(channelComsumer);

        ThreadUtil.execute(() -> {
            RuntimeUtil.execForStr(pythonStartCMD); // 运行python仿真程序
        });
        Thread.sleep(1000); // 稍作等待
        sendMessageToPython(channelProducer, buildHandshakeMsg());

        waitUtilPythonReady(channelComsumer);


        // 确认收到启动成功的消息.

        TimeInterval timer = DateUtil.timer();
        timer.start();

        execBuySellOrder(channelProducer, channelComsumer, "buy",
                "000001", 100, null, true, null, null);

        Console.log(timer.intervalRestart());

        Console.log("ok");
        channelProducer.close();
        channelComsumer.close();
        conn.close();
    }

    private static void waitUtilPythonReady(Channel channelComsumer) throws IOException {
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {


            }
        };
        channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);
    }

    public static String buildHandshakeMsg() {
        JSONObject handshake = new JSONObject();
        handshake.set("handshake_java_side", "java get ready");
        handshake.set("handshake_python_side", "and you?");
        handshake.set("timestamp", System.currentTimeMillis());
        return JSONUtil.toJsonStr(handshake);
    }


    public static List<JSONObject> comsumeUntilSuccessState(Channel channelComsumer, String rawOrderId)
            throws IOException, InterruptedException {

        List<JSONObject> responses = new ArrayList<>(); // 保留响应解析成的JO

        final boolean[] finish = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message = null;
                try {
                    message = JSONUtil.parseObj(msg, orderJsonStrConfig);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但解析为 JSONObject 失败: {}", msg);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                JSONObject rawOrderFromResponse = null;
                try {
                    rawOrderFromResponse = ((JSONObject) message.get("raw_order"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但从响应获取 raw_order 失败: {}", message);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                if (rawOrderFromResponse == null) {
                    log.warn("nack: 收到来自python的消息, 但从响应获取 raw_order 为null: {}", message);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                String rawOrderIdOfResponse = rawOrderFromResponse.getStr("raw_order_id");
                if (!rawOrderId.equals(rawOrderIdOfResponse)) { // 需要是对应id
                    log.warn("nack: 收到来自python的消息, 但 raw_order_id不匹配: should: {}, receive: {}", rawOrderId,
                            rawOrderIdOfResponse);
                    channelComsumer.basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                log.info("ack: 确认收到来自python消息: {}", message);
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

        String consumerTag = channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);
        while (!finish[0]) {
            Thread.sleep(1); // 只能自行阻塞?
        }
        return responses;
    }


    public static List<JSONObject> execBuySellOrder(Channel channelProducer,
                                                    Channel channelComsumer,
                                                    String type, String stockCode, Number amounts,
                                                    Double price,
                                                    boolean timer, List<String> otherKeys,
                                                    List<Object> otherValues) throws IOException, InterruptedException {
        JSONObject order = generateBuySellOrder(type, stockCode, amounts, price, timer, otherKeys, otherValues);
        String orderMsg = orderAsJsonStr(order);
        String rawOrderId = order.getStr("raw_order_id");
        sendMessageToPython(channelProducer, orderMsg);
        List<JSONObject> res = comsumeUntilSuccessState(channelComsumer, rawOrderId);
        return res;
    }

    public static void sendMessageToPython(Channel channelProducer, String jsonMsg) throws IOException {
        log.info("send: 发送消息到python: {}", jsonMsg);
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
        log.info("连接到rabbitmq...");
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