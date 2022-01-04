package com.scareers.gui.rabbitmq;

import cn.hutool.core.lang.Assert;
import com.scareers.gui.rabbitmq.order.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-1:13
 */
public class OrderFactory {
    /**
     * 构造买卖order基本函数.  order使用 JSONObject 类型.
     *
     * @param type      订单类型: 买/卖
     * @param stockCode 股票代码
     * @param amounts   数量Number
     * @param price     价格
     * @return
     */
    private static Order generateBuySellOrder(String type,
                                              String stockCode,
                                              Number amounts,
                                              Double price) throws Exception {
        Assert.isTrue(Arrays.asList("buy", "sell").contains(type));
        Map<String, Object> params = new HashMap<>();
        params.put("stockCode", stockCode);
        params.put("amounts", amounts);
        params.put("price", price);
        if ("buy".equals(type)) {
            return new BuyOrder(params);
        } else {
            return new SellOrder(params);
        }
    }

    /**
     * 快捷生成买入订单
     *
     * @param stockCode
     * @param amounts
     * @param price
     * @return
     */
    public static Order generateBuyOrderQuick(String stockCode,
                                              Number amounts,
                                              Double price) throws Exception {
        return generateBuySellOrder("buy", stockCode, amounts, price);
    }

    public static Order generateBuyOrderQuick(String stockCode,
                                              Number amounts,
                                              Double price, Long priority) throws Exception {
        Order res = generateBuyOrderQuick(stockCode, amounts, price);
        res.setPriority(priority);
        return res;
    }

    /**
     * 快捷生成卖出订单
     *
     * @param stockCode
     * @param amounts
     * @param price
     * @return
     */
    public static Order generateSellOrderQuick(String stockCode,
                                               Number amounts,
                                               Double price) throws Exception {
        return generateBuySellOrder("sell", stockCode, amounts, price);
    }

    public static Order generateSellOrderQuick(String stockCode,
                                               Number amounts,
                                               Double price, Long priority) throws Exception {
        Order res = generateSellOrderQuick(stockCode, amounts, price);
        res.setPriority(priority);
        return res;
    }


    public static Order generateWaitOneBeFlushedOrder(String waitFlushOrderId,
                                                      int timeoutThreshold,
                                                      boolean canCancelOnly) throws Exception {
        Assert.isTrue(waitFlushOrderId != null);
        Map<String, Object> params = new HashMap<>();
        params.put("waitFlushOrderId", waitFlushOrderId);
        params.put("timeoutThreshold", timeoutThreshold);
        params.put("canCancelOnly", canCancelOnly);
        return new WaitOneBeFlushOrder(params);
    }

    public static Order generateWaitOneBeFlushedOrderQuick(String waitFlushOrderId,
                                                           int timeoutThreshold,
                                                           boolean canCancelOnly, Long priority) throws Exception {
        Order order = generateWaitOneBeFlushedOrder(waitFlushOrderId, timeoutThreshold, canCancelOnly);
        order.setPriority(priority);
        return order;
    }

    /**
     * 撤单单一id 的订单. id是同花顺交易软件id, 而非java生成的订单objectId
     *
     * @param thsRawOrderId 交易软件自动生成的订单id
     * @return
     */
    public static Order generateCancelConcreteOrder(Object thsRawOrderId) {
        Objects.requireNonNull(thsRawOrderId);
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", thsRawOrderId.toString());
        return new Order("cancel_a_concrete_order", params);
    }

    public static Order generateCancelConcreteOrder(Object thsRawOrderId, Long priority) {
        Order res = generateCancelConcreteOrder(thsRawOrderId);
        res.setPriority(priority);
        return res;
    }


    /**
     * 三种批量撤单函数.  cancel_all, /buy /sell
     * 快捷: generateCancelAllOrder/generateCancelBuyOrder/generateCancelSellOrder
     *
     * @param type
     * @param stockCode
     * @return
     */
    private static Order generateCancelBatchOrder(String type, String stockCode, String waitFlushOrderId) {
        Assert.isTrue(Arrays.asList("buy", "sell", "all").contains(type)); // 三种批量撤单类型
        Map<String, Object> params = new HashMap<>();
        params.put("stockCode", stockCode);
        params.put("waitFlushOrderId", waitFlushOrderId); // 可显式等待某id被刷新
        return new Order("cancel_" + type, params);
    }

    private static Order generateCancelBatchOrder(String type, String stockCode) {
        Assert.isTrue(Arrays.asList("buy", "sell", "all").contains(type)); // 三种批量撤单类型
        Map<String, Object> params = new HashMap<>();
        params.put("stockCode", stockCode);
        return new Order("cancel_" + type, params);
    }

    public static Order generateCancelAllOrder(String stockCode) {
        return generateCancelBatchOrder("all", stockCode);
    }

    public static Order generateCancelAllOrder(String stockCode, Long priority) {
        Order res = generateCancelAllOrder(stockCode);
        res.setPriority(priority);
        return res;
    }

    public static Order generateCancelAllOrder(String stockCode, String waitFlushOrderId) {
        return generateCancelBatchOrder("all", stockCode, waitFlushOrderId);
    }

    public static Order generateCancelAllOrder(String stockCode, String waitFlushOrderId, Long priority) {
        Order res = generateCancelAllOrder(stockCode, waitFlushOrderId);
        res.setPriority(priority);
        return res;
    }

    public static Order generateCancelBuyOrder(String stockCode) {
        return generateCancelBatchOrder("buy", stockCode);
    }

    public static Order generateCancelBuyOrder(String stockCode, Long priority) {
        Order res = generateCancelBuyOrder(stockCode);
        res.setPriority(priority);
        return res;
    }

    public static Order generateCancelBuyOrder(String stockCode, String waitFlushOrderId) {
        return generateCancelBatchOrder("buy", stockCode, waitFlushOrderId);
    }

    public static Order generateCancelBuyOrder(String stockCode, String waitFlushOrderId, Long priority) {
        Order res = generateCancelBuyOrder(stockCode, waitFlushOrderId);
        res.setPriority(priority);
        return res;
    }

    public static Order generateCancelSellOrder(String stockCode) {
        return generateCancelBatchOrder("sell", stockCode);
    }

    public static Order generateCancelSellOrder(String stockCode, Long priority) {
        Order res = generateCancelSellOrder(stockCode);
        res.setPriority(priority);
        return res;
    }

    public static Order generateCancelSellOrder(String stockCode, String waitFlushOrderId) {
        return generateCancelBatchOrder("sell", stockCode, waitFlushOrderId);
    }

    public static Order generateCancelSellOrder(String stockCode, String waitFlushOrderId, Long priority) {
        Order res = generateCancelSellOrder(stockCode, waitFlushOrderId);
        res.setPriority(priority);
        return res;
    }

    /**
     * 构造无特别参数查询函数, 因 目前两个查询函数均不需要参数, 因此 NoArgs
     * 当前可使用查询api:
     * //----> get_hold_stocks_info    账号股票持仓汇总数据
     * //----> get_account_funds_info  9项账号资金数据
     * //----> get_unsolds_not_yet  获取当前尚未完全成交的挂单
     * //----> get_today_clinch_orders  获取所有今日成交订单列表
     * //----> get_today_consign_orders  获取所有今日委托过的订单列表
     *
     * @param orderType
     * @return
     */
    public static Order generateDefaultArgsQueryOrder(String orderType) {
        Objects.requireNonNull(orderType);
        return new QueryDefaultArgsOrder(orderType);
    }

    public static Order generateDefaultArgsQueryOrder(String orderType, Long priority) {
        Order res = generateDefaultArgsQueryOrder(orderType);
        res.setPriority(priority);
        return res;
    }
}


