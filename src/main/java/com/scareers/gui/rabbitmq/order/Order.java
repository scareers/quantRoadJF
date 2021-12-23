package com.scareers.gui.rabbitmq.order;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * description: 核心订单对象 抽象类. 传递前调用 prepare() 方法, 转换为 json字符串传递
 * 订单对象生命周期:
 * --> new  (纯新生,无参数构造器new,尚未决定类型)
 * --> generated(类型,参数已准备好,可prepare)
 * --> wait_execute(入(执行队列)队后等待执行)
 * --> executing(已发送python,执行中,等待响应)
 * --> finish_execute(已接收到python响应)
 * --> check_transaction_status(确认成交状态中, 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
 * --> finish (订单彻底完成)
 *
 * @author: admin
 * @date: 2021/12/23/023-18:17:58
 */
@Data
public class Order {
    private static final Log log = LogUtils.getLogger();

    private String rawOrderId; //java全局唯一id
    private String orderType; // 核心订单类型, 对应python操作api
    private long timestamp; // 生成时间戳
    private Map<String, Object> params; // 订单api需要的其他参数map
    private List<LifePoint> lifePoints; // 有序列表, 各个生命周期情况, 生命周期由java进行管理, 无关python

    public Order() {
        this.rawOrderId = IdUtil.objectId();
        this.timestamp = System.currentTimeMillis();
        List<LifePoint> lifePoints = new ArrayList<>();
        lifePoints.add(new LifePoint("new")); // 新生
        this.lifePoints = lifePoints;
    }

    public Order(String orderType, Map<String, Object> params) {
        this.orderType = orderType;
        this.params = params;
    }

    public JSON prepare() {
        JSONObject order = new JSONObject();

        return order;
    }

    public static void main(String[] args) {
        Order x = new Order();
    }

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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LifePoint { // 生命周期中, 某一时刻点
        String state;
        String payload;
        String description;
        String notes;

        public LifePoint(String state) {
            this.state = state;
        }

        public LifePoint(String state, String description) {
            this.state = state;
            this.description = description;
        }

        public LifePoint(String state, String payload, String description) {
            this.state = state;
            this.payload = payload;
            this.description = description;
        }

        public JSON asJson() {
            JSONObject lifePoint = new JSONObject();
            lifePoint.set("state", getState());
            lifePoint.set("payload", getState());
            lifePoint.set("description", getState());
            lifePoint.set("notes", getState());
            return lifePoint;
        }
    }


}
