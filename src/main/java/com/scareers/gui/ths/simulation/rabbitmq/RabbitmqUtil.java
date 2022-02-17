package com.scareers.gui.ths.simulation.rabbitmq;

import cn.hutool.log.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.scareers.utils.log.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/8/008-01:03:54
 */
public class RabbitmqUtil {
    private static final Log log = LogUtil.getLogger();

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
        log.info("connecting: 连接到rabbitmq...");
        return conn;
    }

    /**
     * 初始化 j2p/p2j 两个(交换机-队列-路由键) 双通 道.  --> Trader
     *
     * @param channel
     * @return
     */
    public static boolean initDualChannelForTrader(Channel channel) {
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

    /**
     * 初始化 j2p/p2j 两个(交换机-队列-路由键) 双通 道.  --> AccountStates
     *
     * @param channel
     * @return
     */
    public static boolean initDualChannelForAccountStates(Channel channel) {
        // java到python的队列
        try {
            channel.exchangeDeclare(ths_trader_j2p_exchange_as, "fanout", true);
            channel.queueDeclare(ths_trader_j2p_queue_as, true, false, false, null);
            channel.queueBind(ths_trader_j2p_queue_as, ths_trader_j2p_exchange_as, ths_trader_j2p_routing_key_as);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // python到java 的队列
        try {
            channel.exchangeDeclare(ths_trader_p2j_exchange_as, "fanout", true);
            channel.queueDeclare(ths_trader_p2j_queue_as, true, false, false, null);
            channel.queueBind(ths_trader_p2j_queue_as, ths_trader_p2j_exchange_as, ths_trader_p2j_routing_key_as);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
