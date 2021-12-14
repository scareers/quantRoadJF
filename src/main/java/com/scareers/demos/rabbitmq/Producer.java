package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.scareers.utils.StrUtil;

import java.util.HashMap;

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

            // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
            String orderType;
            int state = RandomUtil.randomInt(10);
            if (state < 5) {
                orderType = "buy";
            } else {
                orderType = "sell"; // 一般
            }

            HashMap<String, Object> order = new HashMap<>();
            order.put("order_type", orderType);
            order.put("stock_code", "600090");
            order.put("amounts", 100);
            order.put("price", null);
            order.put("timer", true);
            JSONConfig config = JSONConfig.create();
            config.setOrder(true);
            config.setIgnoreNullValue(false); // 默认会把null值省略掉, 这里显然应该false
            String msg = JSONUtil.toJsonStr(order, config);
            Console.log("java 端生产消息: {}", msg);

            channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                    msg.getBytes());
        }


        channel.close();
        conn.close();
    }
}
