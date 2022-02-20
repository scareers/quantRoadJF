package com.scareers.gui.ths.simulation.rabbitmq;

import cn.hutool.core.lang.Console;
import com.scareers.utils.JSONUtilS;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.connectToRbServer;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.initDualChannelForTrader;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.ths_trader_p2j_queue;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/14/014-12:59
 */
public class ComsumerSimple {
    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();
        initDualChannelForTrader(channel);

        System.out.println(" Waiting for message....");
        // 创建消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                Map<String, Object> message = null;
                try {
                    message = JSONUtilS.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace(); // 例如 response返回了None,
                }
                // json解析, 自动将 \\u  unicode字符解析为汉字
                Console.log(message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // 消费者, 消费 p2j 的队列..
        channel.basicConsume(ths_trader_p2j_queue, false, consumer);
    }
}
