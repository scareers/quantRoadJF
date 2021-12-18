package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public static void main(String[] args) {

    }


    public static void execBuySellOrder(Channel channelProducer,
                                        Channel channelComsumer,
                                        String type, String stockCode, Number amounts,
                                        Double price,
                                        boolean timer, List<String> otherKeys,
                                        List<Object> otherValues) throws IOException {
        JSONObject order = generateBuySellOrder(type, stockCode, amounts, price, timer, otherKeys, otherValues);
        String orderMsg = orderAsJsonStr(order);
        channelProducer.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                orderMsg.getBytes(StandardCharsets.UTF_8));


        Consumer consumer = new DefaultConsumer(channelComsumer) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                Map<String, Object> message = null;
                try {
                    message = JSONUtil.parseObj(msg, orderJsonStrConfig);
                } catch (Exception e) {
                    e.printStackTrace(); // 某一天调试是, response返回了None, 导致 null, json解析不了. 要求必须 {开头
                }
                // json解析, 自动将 \\u  unicode字符解析为汉字
                Console.log(message);
                channelComsumer.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // 消费者, 消费 p2j 的队列..
        channelComsumer.basicConsume(ths_trader_p2j_queue, false, consumer);

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
