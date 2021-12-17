package com.scareers.demos.rabbitmq;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rabbitmq.client.MessageProperties.MINIMAL_PERSISTENT_BASIC;
import static com.scareers.demos.rabbitmq.RbUtils.connectToRbServer;
import static com.scareers.demos.rabbitmq.RbUtils.initDualChannel;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_j2p_exchange;
import static com.scareers.demos.rabbitmq.SettingsOfRb.ths_trader_j2p_routing_key;


/**
 * description: rabbitmq 纯api.
 * // @noti: 每个python下单key_api, 均对应本类一个静态下单方法
 * // @noti: java端, 订单使用 JSONObject实现, 是 Map<String,Object> 的子类
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

        for (int i = 0; i < 1; i++) {
            // 发送消息


            // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
            String msg;
            msg = generateSimpleSellOrderAsStr("600090", 100, 1.25, true);
            channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                    msg.getBytes(StandardCharsets.UTF_8));
        }

        for (int i = 0; i < 1; i++) {
            // 发送消息

            // 生产者, 生产到 j2p 队列!!  --> 注意该静态属性, 表示 需要ack 的发送, 否则将重发
            String msg2;
            msg2 = orderAsJsonStr(generateCancelOneOrder("2510897966"));

            Console.log("java 端生产消息: {}", msg2);
            channel.basicPublish(ths_trader_j2p_exchange, ths_trader_j2p_routing_key, MINIMAL_PERSISTENT_BASIC,
                    msg2.getBytes(StandardCharsets.UTF_8));
        }

        channel.close();
        conn.close();
    }


    /**
     * 构造买卖order基本函数.  order使用 HashMap<String,Object> 类型.
     *
     * @param type        订单类型: 买/卖
     * @param stockCode   股票代码
     * @param amounts     数量Number
     * @param price       价格
     * @param timer       是否打印耗时日志
     * @param otherKeys   其他key 列表
     * @param otherValues 对应的其他 value列表, 注意需要lenth长度一样. (虽然能写成按照lenth短的来)
     * @return
     */
    public static Map<String, Object> generateBuySellOrder(String type, String stockCode, Number amounts,
                                                           Double price,
                                                           boolean timer, List<String> otherKeys,
                                                           List<Object> otherValues) {
        assert Arrays.asList("buy", "sell").contains(type);
        HashMap<String, Object> order = new HashMap<>();
        order.put("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.put("order_type", type); // 恰好api也为buy/sell

        order.put("stock_code", stockCode);
        order.put("amounts", amounts);
        order.put("price", price);
        order.put("timer", timer);

        if (otherKeys != null && otherValues != null) {
            assert otherKeys.size() == otherValues.size();
            for (int i = 0; i < otherKeys.size(); i++) {
                order.put(otherKeys.get(i), otherValues.get(i));
            }
        } // 保留的其余 键值对
        return order;
    }

    /**
     * 快捷生成买入订单, 多余键值对为 null
     *
     * @param stockCode
     * @param amounts
     * @param price
     * @param timer
     * @return
     */
    public static Map<String, Object> generateBuyOrderQuick(String stockCode,
                                                            Number amounts,
                                                            Double price,
                                                            boolean timer) {
        return generateBuySellOrder("buy", stockCode, amounts, price, timer, null, null);
    }

    /**
     * 快捷生成卖出订单, 多余键值对为 null
     *
     * @param stockCode
     * @param amounts
     * @param price
     * @param timer
     * @return
     */
    public static Map<String, Object> generateSellOrderQuick(String stockCode,
                                                             Number amounts,
                                                             Double price,
                                                             boolean timer) {
        return generateBuySellOrder("sell", stockCode, amounts, price, timer, null, null);
    }


    /**
     * 撤单单一id 的订单. id是同花顺交易软件id, 而非java生成的订单objectId
     *
     * @param thsRawOrderId 交易软件自动生成的订单id
     * @param timer
     * @return
     */
    public static Map<String, Object> generateCancelConcreteOrder(Object thsRawOrderId,
                                                                  boolean timer) {
        assert thsRawOrderId != null;
        HashMap<String, Object> order = new HashMap<>();
        order.put("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.put("order_type", "cancel_a_concrete_order"); // 对应的python api, 撤单某个具体id的订单
        order.put("order_id", thsRawOrderId); // 不可null
        order.put("timer", timer); // 时间字段
        return order;
    }


    /**
     * 三种批量撤单函数.  cancel_all, /buy /sell
     * 快捷: generateCancelAllOrder/generateCancelBuyOrder/generateCancelSellOrder
     *
     * @param type
     * @param stockCode
     * @param timer
     * @return
     */
    public static Map<String, Object> generateCancelBatchOrder(String type, String stockCode,
                                                               boolean timer) {
        assert Arrays.asList("buy", "sell", "all").contains(type); // 三种批量撤单类型
        HashMap<String, Object> order = new HashMap<>();
        order.put("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.put("order_type", "cancel_" + type); // 对应的python api
        order.put("stock_code", stockCode); // 可null
        order.put("timer", timer); // 时间字段
        return order;
    }

    public static Map<String, Object> generateCancelAllOrder(String stockCode,
                                                             boolean timer) {
        return generateCancelBatchOrder("all", stockCode, timer);
    }

    public static Map<String, Object> generateCancelBuyOrder(String stockCode,
                                                             boolean timer) {
        return generateCancelBatchOrder("buy", stockCode, timer);
    }

    public static Map<String, Object> generateCancelSellOrder(String stockCode,
                                                              boolean timer) {
        return generateCancelBatchOrder("sell", stockCode, timer);
    }

    /**
     * 构造无特别参数查询函数, 因 目前两个查询函数均不需要参数, 因此 NoArgs
     * 当前可使用查询api:
     * //----> get_hold_stocks_info    账号股票持仓汇总数据
     * //----> get_account_funds_info  9项账号资金数据
     *
     * @param orderType
     * @param timer
     * @return
     */
    public static Map<String, Object> generateNoArgsQueryOrder(String orderType,
                                                               boolean timer) {
        HashMap<String, Object> order = new HashMap<>();
        order.put("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.put("order_type", orderType); // 对应的python api
        order.put("timer", timer); // 时间字段
        return order;
    }

    public static String orderAsJsonStr(HashMap<String, Object> order) {
        return JSONUtil.toJsonStr(order, orderJsonStrConfig);
    }

}
