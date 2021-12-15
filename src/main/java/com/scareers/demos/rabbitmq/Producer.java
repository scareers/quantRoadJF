package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.util.HashMap;
import java.util.List;

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
    public static JSONConfig orderJsonStrConfig;

    static {
        orderJsonStrConfig = JSONConfig.create();
        orderJsonStrConfig.setOrder(true);
        orderJsonStrConfig.setIgnoreNullValue(false);
    }

    public static void main(String[] args) throws Exception {
        // 建立连接
        Connection conn = connectToRbServer();
        Channel channel = conn.createChannel();
        initDualChannel(channel);

        for (int i = 0; i < 5; i++) {
            // 发送消息

            // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
            String msg;
            int state = RandomUtil.randomInt(10);
            if (state < 5) {
                msg = generateSimpleSellOrderAsStr("600090", 100, 1.30, true);
            } else {
                msg = generateSimpleBuyOrderAsStr("600090", 100, 1.30, true);
            }
            Console.log("java 端生产消息: {}", msg);
            channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                    msg.getBytes());
        }

        channel.close();
        conn.close();
    }


    /**
     * 构造order基本函数.  order使用 HashMap<String,Object> 类型.
     *
     * @param orderType   订单类型
     * @param stockCode   股票代码
     * @param amounts     数量Number
     * @param price       价格
     * @param timer       是否打印耗时日志
     * @param otherKeys   其他key 列表
     * @param otherValues 对应的其他 value列表, 注意需要lenth长度一样. (虽然能写成按照lenth短的来)
     * @return
     */
    public static HashMap<String, Object> generateOrder(String orderType, String stockCode, Number amounts,
                                                        Double price,
                                                        boolean timer, List<String> otherKeys,
                                                        List<Object> otherValues) {
        HashMap<String, Object> order = new HashMap<>();
        order.put("order_type", orderType);
        order.put("stock_code", stockCode);
        order.put("amounts", amounts);
        order.put("price", price);
        order.put("timer", timer);
        if (otherKeys != null && otherValues != null) {
            assert otherKeys.size() == otherValues.size();
            for (int i = 0; i < otherKeys.size(); i++) {
                order.put(otherKeys.get(i), otherValues.get(i));
            }
        }
        return order;
    }

    public static String orderAsJsonStr(HashMap<String, Object> order) {
        return JSONUtil.toJsonStr(order, orderJsonStrConfig);
    }

    public static String generateOrderAsJsonStr(String orderType, String stockCode, Number amounts,
                                                Double price,
                                                boolean timer, List<String> otherKeys,
                                                List<Object> otherValues) {
        HashMap<String, Object> order = generateOrder(orderType, stockCode,
                amounts, price, timer, otherKeys, otherValues);
        return orderAsJsonStr(order);
    }

    public static String generateOrderAsPrettyJsonStr(String orderType, String stockCode, Number amounts,
                                                      Double price,
                                                      boolean timer, List<String> otherKeys,
                                                      List<Object> otherValues) {
        HashMap<String, Object> order = generateOrder(orderType, stockCode,
                amounts, price, timer, otherKeys, otherValues);
        return JSONUtil.toJsonPrettyStr(order);
    }


    public static String generateSimpleBuyOrderAsStr(String stockCode, Number amounts,
                                                     Double price,
                                                     boolean timer) {
        return generateOrderAsJsonStr("buy", stockCode, amounts,
                price, timer, null, null);
    }

    public static String generateSimpleSellOrderAsStr(String stockCode, Number amounts,
                                                      Double price,
                                                      boolean timer) {
        return generateOrderAsJsonStr("sell", stockCode, amounts,
                price, timer, null, null);
    }


}
