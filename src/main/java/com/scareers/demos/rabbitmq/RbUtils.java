package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.scareers.demos.rabbitmq.SettingsOfRb.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/14/014-13:44
 */
public class RbUtils {
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
     * 初始化 j2p/p2j 两个(交换机-队列-路由键) 双通道
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
