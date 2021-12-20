package com.scareers.gui.rabbitmq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.Arrays;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-1:13
 */
public class OrderFactory {
    public static JSONConfig orderJsonStrConfig;

    static {
        orderJsonStrConfig = JSONConfig.create();
        orderJsonStrConfig.setOrder(true); // 使用链表map而已. 不保证转换字符串后有序
        orderJsonStrConfig.setIgnoreNullValue(false); // 保留所有null值
    }

    /**
     * 构造买卖order基本函数.  order使用 JSONObject 类型.
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
    public static JSONObject generateBuySellOrder(String type, String stockCode, Number amounts,
                                                  Double price,
                                                  boolean timer, List<String> otherKeys,
                                                  List<Object> otherValues) {
        assert Arrays.asList("buy", "sell").contains(type);
        JSONObject order = new JSONObject();
        order.set("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.set("timestamp", System.currentTimeMillis());
        order.set("order_type", type); // 恰好api也为buy/sell

        order.set("stock_code", stockCode);
        order.set("amounts", amounts);
        order.set("price", price);
        order.set("timer", timer);

        if (otherKeys != null && otherValues != null) {
            assert otherKeys.size() == otherValues.size();
            for (int i = 0; i < otherKeys.size(); i++) {
                order.set(otherKeys.get(i), otherValues.get(i));
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
    public static JSONObject generateBuyOrderQuick(String stockCode,
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
    public static JSONObject generateSellOrderQuick(String stockCode,
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
    public static JSONObject generateCancelConcreteOrder(Object thsRawOrderId,
                                                         boolean timer) {
        assert thsRawOrderId != null;
        JSONObject order = new JSONObject();
        order.set("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.set("timestamp", System.currentTimeMillis());
        order.set("order_type", "cancel_a_concrete_order"); // 对应的python api, 撤单某个具体id的订单
        order.set("order_id", thsRawOrderId.toString()); // 不可null, 可 Double
        order.set("timer", timer); // 时间字段
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
    public static JSONObject generateCancelBatchOrder(String type, String stockCode,
                                                      boolean timer) {
        assert Arrays.asList("buy", "sell", "all").contains(type); // 三种批量撤单类型
        JSONObject order = new JSONObject();
        order.set("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.set("timestamp", System.currentTimeMillis());
        order.set("order_type", "cancel_" + type); // 对应的python api
        order.set("stock_code", stockCode); // 可null
        order.set("timer", timer); // 时间字段
        return order;
    }

    public static JSONObject generateCancelAllOrder(String stockCode,
                                                    boolean timer) {
        return generateCancelBatchOrder("all", stockCode, timer);
    }

    public static JSONObject generateCancelBuyOrder(String stockCode,
                                                    boolean timer) {
        return generateCancelBatchOrder("buy", stockCode, timer);
    }

    public static JSONObject generateCancelSellOrder(String stockCode,
                                                     boolean timer) {
        return generateCancelBatchOrder("sell", stockCode, timer);
    }

    /**
     * 构造无特别参数查询函数, 因 目前两个查询函数均不需要参数, 因此 NoArgs
     * 当前可使用查询api:
     * //----> get_hold_stocks_info    账号股票持仓汇总数据
     * //----> get_account_funds_info  9项账号资金数据
     * //----> get_unsolds_not_yet  获取当前尚未完全成交的挂单
     *
     * @param orderType
     * @param timer
     * @return
     */
    public static JSONObject generateNoArgsQueryOrder(String orderType,
                                                      boolean timer) {
        JSONObject order = new JSONObject();
        order.set("raw_order_id", IdUtil.objectId()); // 核心id采用 objectid
        order.set("timestamp", System.currentTimeMillis());
        order.set("order_type", orderType); // 对应的python api
        order.set("timer", timer); // 时间字段
        return order;
    }

    /**
     * 使用默认配置, 转换json
     *
     * @param order
     * @return
     */
    public static String orderAsJsonStr(JSONObject order) {
        return JSONUtil.toJsonStr(order, orderJsonStrConfig);
    }
}
