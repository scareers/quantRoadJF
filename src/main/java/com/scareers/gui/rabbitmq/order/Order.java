package com.scareers.gui.rabbitmq.order;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.*;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 核心订单对象 抽象类. 传递前调用 prepare() 方法, 转换为 json字符串传递
 * 订单对象生命周期: 见 LifePointStatus
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
@AllArgsConstructor
@Builder
public class Order {
    // 转换为jsonStr时配置
    public static JSONConfig orderJsonStrConfig;
    public static long PRIORITY_LOWEST = 10000; // 5大优先级常量
    public static long PRIORITY_LOW = 9000;
    public static long PRIORITY_MEDIUM = 5000;
    public static long PRIORITY_HIGH = 1000;
    public static long PRIORITY_HIGHEST = 0;

    static {
        orderJsonStrConfig = JSONConfig.create();
        orderJsonStrConfig.setOrder(true); // 使用链表map而已. 不保证转换字符串后有序
        orderJsonStrConfig.setIgnoreNullValue(false); // 保留所有null值
    }

    private String rawOrderId; //java全局唯一id
    private String orderType; // 核心订单类型, 对应python操作api
    private long timestamp; // 生成时间戳
    private Map<String, Object> params; // 订单api需要的其他参数map
    private List<LifePoint> lifePoints; // 有序列表, 各个生命周期情况, 生命周期由java进行管理, 无关python
    private boolean timer; // 是否记录执行时间, 过于常用 , 默认 true, 通常需要手动修改
    private Map<String, Object> otherRawMessages; // 通常需要手动设定,手动修改
    private long priority; // 优先级, 越低则优先级越高.   默认优先级最低10000.

    public static void main(String[] args) throws Exception {
        Order x = new BuyOrder(new HashMap<>());
        Console.log(x.toJsonPrettyStr());
    }


    public Order() {
        // 常态仅 orderType null, 参数空map
        this(IdUtil.objectId(),
                null,
                System.currentTimeMillis(),
                new HashMap<>(),
                null,
                true,
                new HashMap<>(),
                PRIORITY_LOWEST);
        List<LifePoint> lifePoints = new ArrayList<>();
        lifePoints.add(new LifePoint(LifePointStatus.NEW, "new订单对象,尚未决定类型")); // 新生
        this.lifePoints = lifePoints;
    }

    public Order(String orderType, Map<String, Object> params) {
        this();
        Objects.requireNonNull(orderType); // orderType 绝对不可null
        this.orderType = orderType;
        if (params != null) { // params 可null, 自动生成空map
            this.params = params;
        }
        this.lifePoints.add(new LifePoint(LifePointStatus.GENERATED, StrUtil.format("生成完成,订单对象已确定类型: {}", orderType)));
    }

    public Order(String orderType, Map<String, Object> params, long priority) {
        this(orderType, params);
        this.priority = priority;
    }

    public Order(String orderType) {
        this(orderType, null);
    }

    public Order(String orderType, long priority) {
        this(orderType, null, priority);
    }


    public JSON prepare() throws Exception {
        JSONObject order = new JSONObject();
        order.set("rawOrderId", rawOrderId);
        if (orderType == null) {
            throw new Exception("订单对象尚未生成完成,不可prepare()");
        }
        order.set("orderType", orderType);
        order.set("timestamp", timestamp);
        order.set("priority", priority);
        checkParamsKeySet();
        order.putAll(params);
        order.set("lifePoints", lifePoints.stream().map(value -> value.asJson()).collect(Collectors.toList()));
        order.set("timer", timer);
        order.set("otherRawMessages", otherRawMessages);
        return order;
    }

    @SneakyThrows
    @Override
    public String toString() {
        return toJsonPrettyStr();
    }

    public String toJsonStr() throws Exception {
        return JSONUtil.toJsonStr(prepare(), orderJsonStrConfig);
    }

    public String toJsonPrettyStr() throws Exception {
        return JSONUtil.toJsonPrettyStr(prepare());
    }


    private void checkParamsKeySet() throws Exception {
        for (String paramName : params.keySet()) {
            if ("rawOrderId".equals(rawOrderId) || "orderType".equals(rawOrderId) || "timestamp".equals(
                    rawOrderId)) {
                throw new Exception(StrUtil.format("参数map错误: key错误: {}", paramName));
            }
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LifePoint { // 生命周期中, 某一时刻点
        LifePointStatus status;
        String description;
        String payload;
        String notes;

        public LifePoint(LifePointStatus status) {
            this.status = status;
        }

        public LifePoint(LifePointStatus status, String description) {
            this.status = status;
            this.description = description;
        }

        public LifePoint(LifePointStatus status, String description, String payload) {
            this.status = status;
            this.description = description;
            this.payload = payload;
        }

        public JSON asJson() {
            JSONObject lifePoint = new JSONObject();
            lifePoint.set("status", status.toString());
            lifePoint.set("description", description);
            lifePoint.set("payload", payload);
            lifePoint.set("notes", notes);
            return lifePoint;
        }
    }

    /**
     * 订单对象生命周期:  LifePointStatus
     * --> new  (纯新生,无参数构造器new,尚未决定类型)
     * --> generated(类型,参数已准备好,可prepare)
     * --> wait_execute(入(执行队列)队后等待执行)
     * --> executing(已发送python,执行中,等待响应)
     * --> finish_execute(已接收到python响应)
     * --> check_transaction_status(确认成交状态中, 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
     * --> finish (订单彻底完成)
     */
    public enum LifePointStatus {
        NEW("new"),
        GENERATED("generated"),
        WAIT_EXECUTE("wait_execute"),
        EXECUTING("executing"),
        FINISH_EXECUTE("finish_execute"),
        CHECK_TRANSACTION_STATUS("check_transaction_status"),
        FINISH("finish"),
        ;

        private String description; // 文字描述

        LifePointStatus(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }


    private static final Log log = LogUtils.getLogger();
}
