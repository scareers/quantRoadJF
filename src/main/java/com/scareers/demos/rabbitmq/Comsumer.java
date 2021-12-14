package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.*;

import java.io.IOException;

import static com.scareers.demos.rabbitmq.RbUtils.connectToRbServer;

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

        AMQP.Exchange.DeclareOk result = channel.exchangeDeclare("test_exchange1", "fanout", true);
        AMQP.Queue.DeclareOk result2 = channel.queueDeclare("test_queue1", true, false, false, null);
        Console.log(result);
        Console.log(result2);
        channel.queueBind("test_queue1", "test_exchange1", "test_key1");

        System.out.println(" Waiting for message....");
        // 创建消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, "UTF-8");
                System.out.println(StrUtil.format("received: {}  ; current: {}", msg,
                        System.currentTimeMillis()));
            }
        };

        // 开始获取消息
        // String queue, boolean autoAck, Consumer callback
        channel.basicConsume("test_queue1", false, consumer);
    }
}
