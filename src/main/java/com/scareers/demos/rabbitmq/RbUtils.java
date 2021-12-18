package com.scareers.demos.rabbitmq;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.*;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.demos.rabbitmq.SettingsOfRb.*;
import static com.scareers.demos.rabbitmq.Producer.*;

/**
 * description: rabbitmq 工具类. 对于每个order api, 使用串行方式调用
 * 即 java发送消息 --> python执行 --> python发送结果 --> java收到retrying继续等待,直到success --> java执行完毕.
 * 将 API 封装为串行
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
public class RbUtils {
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        Connection conn = connectToRbServer();
        Channel channelProducer = conn.createChannel();
        initDualChannel(channelProducer);

        Channel channelComsumer = conn.createChannel();
        initDualChannel(channelComsumer);

        System.out.println(timer.intervalRestart());
        execBuySellOrder(channelProducer, channelComsumer, "buy",
                "600090", 100, null, true, null, null);


        Thread.sleep(5000);

        execBuySellOrder(channelProducer, channelComsumer, "buy",
                "600090", 100, null, true, null, null);


        Console.log("ok");
        channelProducer.close();
        channelComsumer.close();
        conn.close();

    }

    public static List<JSONObject> comsumeUntilSuccessState(Channel channelComsumer)
            throws IOException, InterruptedException {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        List<JSONObject> responses = new ArrayList<>(); // 保留响应解析成的JO
        List<String> reponsesRaw = new ArrayList<>(); // 保留响应原始字符串

        final boolean[] finish = {false};
        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @SneakyThrows
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                reponsesRaw.add(msg);
                JSONObject message = null;
                try {
                    message = JSONUtil.parseObj(msg, orderJsonStrConfig);
                } catch (Exception e) {
                    e.printStackTrace(); // 某一天调试是, response返回了None, 导致 null, json解析不了. 要求必须 {开头
                }
                responses.add(message); // 可能null, 此时需要访问 responsesRaw
                Console.log("确认收到来自python消息: {}", message);
                channelComsumer.basicAck(envelope.getDeliveryTag(), false);

                if (message != null) {
                    Object state = message.get("state");
                    if (!"retrying".equals(state.toString())) {
                        channelComsumer.basicCancel(consumerTag);
                        finish[0] = true;
                    }
                }
            }


        };
        // 消费者, 消费 p2j 的队列..
        // 将阻塞, 直到 取消消费?

        String consumerTag = channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);
        while (!finish[0]) {
            Thread.sleep(1); // 只能自行阻塞?
        }
        Console.log("执行完成耗时: {}", timer.intervalRestart());
        return responses;
    }

    public static void execBuySellOrder(Channel channelProducer,
                                        Channel channelComsumer,
                                        String type, String stockCode, Number amounts,
                                        Double price,
                                        boolean timer, List<String> otherKeys,
                                        List<Object> otherValues) throws IOException, InterruptedException {
        JSONObject order = generateBuySellOrder(type, stockCode, amounts, price, timer, otherKeys, otherValues);
        String orderMsg = orderAsJsonStr(order);
        channelProducer.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                orderMsg.getBytes(StandardCharsets.UTF_8));
        List<JSONObject> reponses = comsumeUntilSuccessState(channelComsumer);
        Console.log(reponses);
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
