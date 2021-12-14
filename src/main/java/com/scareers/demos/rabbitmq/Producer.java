package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.scareers.utils.StrUtil;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.demos.rabbitmq.RbUtils.connectToRbServer;
import static com.scareers.demos.rabbitmq.RbUtils.initDualChannel;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_j2p_exchange;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_j2p_routing_key;


/**
 * description: rabbitmq 纯api.
 *
 * @noti: 不使用 amqp整合 springboot, 自行调用底层api, 能获得更多自由度. 且依赖性更低, 更加灵活. 代码量不会过大.
 * @author: admin
 * @date: 2021/12/14/014-12:52
 */
public class Producer {
    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();
        initDualChannel(channel);

        for (int i = 0; i < 5; i++) {
            // 发送消息
            String msg = StrUtil.format("time: {}", System.currentTimeMillis());
            Console.log("java 端生产消息: {}", msg);
            // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
            channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                    msg.getBytes());
        }


        channel.close();
        conn.close();
    }
}
