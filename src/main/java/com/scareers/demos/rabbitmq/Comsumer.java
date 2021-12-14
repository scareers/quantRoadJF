package com.scareers.demos.rabbitmq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

import static com.scareers.demos.rabbitmq.RbUtils.connectToRbServer;
import static com.scareers.demos.rabbitmq.RbUtils.initDualChannel;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_p2j_exchange;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_p2j_queue;

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
                String msg = new String(body, "UTF-8");
                System.out.println(StrUtil.format("received: {}  ; current: {}", msg,
                        System.currentTimeMillis()));
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // 消费者, 消费 p2j 的队列..
        channel.basicConsume(ths_trader_p2j_queue, false, consumer);
    }
}
