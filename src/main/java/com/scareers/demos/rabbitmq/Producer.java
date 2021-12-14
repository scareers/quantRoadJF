package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.scareers.utils.StrUtil;

import static com.scareers.demos.rabbitmq.RbUtils.connectToRbServer;


/**
 * description: rabbitmq 纯api.
 *
 * @noti: 不使用 amqp整合 springboot, 自行调用底层api, 能获得更多自由度. 且依赖性更低, 更加灵活. 代码量不会过大.
 * @author: admin
 * @date: 2021/12/14/014-12:52
 */
public class Producer {
    private final static String EXCHANGE_NAME = "java_test3";

    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();

        AMQP.Exchange.DeclareOk result = channel.exchangeDeclare("test_exchange1", "fanout", true);
        AMQP.Queue.DeclareOk result2 = channel.queueDeclare("test_queue1", true, false, false, null);
        Console.log(result);
        Console.log(result2);
        channel.queueBind("test_queue1", "test_exchange1", "test_key1");

        for (int i = 0; i < 10; i++) {
            // 发送消息
            String msg = StrUtil.format("time: {}", System.currentTimeMillis());

            // String exchange, String routingKey, BasicProperties props, byte[] body
            channel.basicPublish("test_exchange1", "test_key1", null, msg.getBytes());
        }

        channel.close();
        conn.close();
    }
}
