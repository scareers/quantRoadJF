package com.scareers.gui.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.scareers.gui.rabbitmq.Producer.orderJsonStrConfig;
import static com.scareers.gui.rabbitmq.RbUtils.connectToRbServer;
import static com.scareers.gui.rabbitmq.RbUtils.initDualChannel;
import static com.scareers.gui.rabbitmq.SettingsOfRb.ths_trader_p2j_queue;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/14/014-12:59
 */
public class Comsumer {
    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();
        initDualChannel(channel);

        System.out.println(" Waiting for message....");
        // 创建消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
//                String msg = new String(body, StandardCharsets.);
                String msg = new String(body, StandardCharsets.UTF_8);
                Map<String, Object> message = null;
                try {
                    message = JSONUtil.parseObj(msg, orderJsonStrConfig);
                } catch (Exception e) {
                    e.printStackTrace(); // 某一天调试是, response返回了None, 导致 null, json解析不了. 要求必须 {开头
                }
                // json解析, 自动将 \\u  unicode字符解析为汉字
                Console.log(message);
//                System.out.println(StrUtil.format("received: {}  ; current: {}", msg,
//                        System.currentTimeMillis()));
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // 消费者, 消费 p2j 的队列..
        channel.basicConsume(ths_trader_p2j_queue, false, consumer);
    }
}
