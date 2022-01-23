package com.scareers.gui.ths.simulation.rabbitmq;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.gui.ths.simulation.OrderFactory.generateSellOrderQuick;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.connectToRbServer;
import static com.scareers.gui.ths.simulation.rabbitmq.RabbitmqUtil.initDualChannel;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.ths_trader_j2p_exchange;
import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.ths_trader_j2p_routing_key;


/**
 * description: rabbitmq 纯api.
 * // @noti: 每个python下单key_api, 均对应本类一个静态下单方法
 * // @noti: java端, 订单使用 JSONObject实现, 取 Map<String,Object> 的语义!!!!!!!
 * // ---> public class JSONObject implements JSON, JSONGetter<String>, JSONObject
 *
 * @noti: 不使用 amqp整合 springboot, 自行调用底层api, 能获得更多自由度. 且依赖性更低, 更加灵活. 代码量不会过大.
 * @author: admin
 * @date: 2021/12/14/014-12:52
 */
public class ProducerSimple {
    private static final Log log = LogUtil.getLogger();
    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();
        initDualChannel(channel);

        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
                    Order order = generateSellOrderQuick("600090", 100, 1.25);
                    String msg;
                    msg = order.toJsonStrForTrans();
                    Thread.sleep(2000);
                    log.info("-->");
                    channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                            msg.getBytes(StandardCharsets.UTF_8));


                }

            }
        }, false);

//        channel.close();
//        conn.close();
    }


}
