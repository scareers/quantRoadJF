package com.scareers.gui.ths.simulation.order;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.*;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 核心订单对象 抽象类. 传递前调用 prepare() 方法, 转换为 json字符串传递
 * 1.订单对象生命周期: 见 LifePointStatus
 * --> new  (纯新生,无参数构造器new,尚未决定类型)
 * --> generated(类型,参数已准备好,可prepare)
 * --> wait_execute(入(执行队列)队后等待执行)
 * --> executing(已发送python,执行中,等待响应)
 * --> finish_execute(已接收到python响应)
 * --> check_transaction_status(确认成交状态中, 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
 * --> finish (订单彻底完成)
 * 2.Order.equals /hashCode 方法 取决于 rawOrderId(确定全局唯一), 而 compareTo 取决于 priority(为优先级队列)
 * // Serializable 支持  ObjectUtil.cloneByStream(obj) 进行深拷贝
 * 3.11大属性更改时:
 * private String rawOrderId; //java全局唯一id, new时自动生成. 重发时自动生成新的
 * private String orderType; // 核心订单类型, 对应python操作api
 * private long timestamp; // 生成时间戳
 * private Map<String, Object> params; // 订单api需要的其他参数map
 * private List<LifePoint> lifePoints; // 有序列表, 各个生命周期情况, 生命周期由java进行管理, 无关python
 * private boolean timer; // 是否记录执行时间, 过于常用 , 默认 true, 通常需要手动修改
 * private Map<String, Object> otherRawMessages; // 通常需要手动设定,手动修改
 * private Long priority; // 优先级, 越低则优先级越高.   默认优先级最低10000.
 * private Long resendTimes; // 某情况下check后的重发对象,可能多次重发, 记录重发次数, 默认0
 * private List<Map<String, Object>> execResponses;
 * private String parentOrder; // 若为重发, 则指定父订单为原订单, 默认 "" , 非null
 *
 * @author: admin
 * @date: 2021/12/23/023-18:17:58
 */
@Data
@AllArgsConstructor
@Builder
public class Order implements Comparable, Serializable {
    private static final long serialVersionUID = 123121545L;
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

    private String rawOrderId; //java全局唯一id, 决定了equals和hashcode
    private String orderType; // 核心订单类型, 对应python操作api
    private long timestamp; // 生成时间戳
    private Map<String, Object> params; // 订单api需要的其他参数map
    private List<LifePoint> lifePoints; // 有序列表, 各个生命周期情况, 生命周期由java进行管理, 无关python
    private boolean timer; // 是否记录执行时间, 过于常用 , 默认 true, 通常需要手动修改
    private Map<String, Object> otherRawMessages; // 通常需要手动设定,手动修改
    private Long priority; // 优先级, 越低则优先级越高.   默认优先级最低10000.
    private Long resendTimes; // 某情况下check后的重发对象,可能多次重发, 记录重发次数, 默认0
    private List<Map<String, Object>> execResponses;
    private String parentOrder; // 若为重发, 则指定父订单为原订单, 默认 "" , 非null
    // 被python执行后的响应列表. 常规仅单个元素, retrying状态下可能多个,默认空al, 无论python做何响应, 应当添加.

    public static void main(String[] args) throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("test_param_key", "test_param_key");
        Order x = new Order("test_order_type", 100L);
        Console.log(x.toJsonPrettyStr());
        Console.log(x.deepCopy());
        Console.log(x.toJsonPrettyStr().equals(x.deepCopy().toJsonPrettyStr()));

    }


    private Order() {
        // 常态仅 orderType null, 参数空map
        this(IdUtil.objectId(),
                null,
                System.currentTimeMillis(),
                new HashMap<>(),
                null,
                true, // 应当属于params, 但过于常用, 转换为基本属性
                new HashMap<>(),
                PRIORITY_LOWEST, // 默认最低优先级
                0L,
                new ArrayList<>(),
                "" //
        );
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

    public Order(String orderType) {
        this(orderType, null);
    }

    public Order(String orderType, long priority) {
        this(orderType, null, priority);
    }

    public Order(String orderType, Map<String, Object> params, Long priority) {
        this(orderType, params);
        if (priority != null) {
            this.priority = priority;
        }
    }


    public static List<String> paramsKeyExcludes = Arrays.asList(
            "rawOrderId", "orderType", "timestamp", "priority", "lifePoints", "timer", "otherRawMessages",
            "resendTimes", "execResponses", "parentOrder"
    ); // params 的key不应包含这些key, 它们均表示Order本身属性

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
        order.set("resendTimes", resendTimes);
        order.set("execResponses", execResponses); // 初始空
        order.set("parentOrder", parentOrder); // null
        return order;
    }

    private void checkParamsKeySet() throws Exception {
        for (String paramName : params.keySet()) {
            if (paramsKeyExcludes.contains(paramName)) {
                throw new Exception(StrUtil.format("参数map错误: key错误: {}", paramName));
            }
        }
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Order) {
            return this.rawOrderId.equals(((Order) obj).getRawOrderId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return rawOrderId.hashCode();
    }


    @SneakyThrows
    @Override
    public int compareTo(Object o) { // 优先级比较!
        if (o instanceof Order) {
            return this.priority.compareTo(((Order) o).priority);
        } else {
            throw new Exception("Order 对象只能与Order对象进行比较");
        }
    }


    /**
     * 添加生命周期对象, 对应了 LifePoint 4个构造器
     *
     * @param status
     */
    public void addLifePoint(LifePointStatus status) {
        this.getLifePoints().add(new LifePoint(status));
    }

    public void addLifePoint(LifePointStatus status, String description) {
        this.getLifePoints().add(new LifePoint(status, description));
    }

    public void addLifePoint(LifePointStatus status, String description, String payload) {
        this.getLifePoints().add(new LifePoint(status, description, payload));
    }

    public void addLifePoint(LifePointStatus status, String description, String payload, String notes) {
        this.getLifePoints().add(new LifePoint(status, description, payload, notes));
    }

    /**
     * 纯深拷贝. private, 常规逻辑应当使用 deepCopyToNewOrder, 所有状态将被初始化,
     *
     * @return
     */
    private Order deepCopy() {
        return ObjectUtil.cloneByStream(this);
    }

    /**
     * // @key2: 订单深拷贝方法, 对某些属性进行合理的改变
     * 典型用于, 当需要重发订单时, 需要对原订单进行拷贝, 但是几乎只保留 订单类型和参数, 其余属性需要刷新!
     * --> 保留的属性: 订单类型, 参数
     * --> 刷新为初始化的属性: 全局id, 生成时间戳, 生命周期列表,优先级-1,
     * // @key: 该方法并未使用 新建对象, 仅修改订单类型和参数的方式. 直接copy较为健壮
     *
     * @return 新订单对象!
     */
    public Order deepCopyToNewOrder() {
        Order res = ObjectUtil.cloneByStream(this);
        res.setRawOrderId(IdUtil.objectId());
        res.setTimestamp(System.currentTimeMillis());
        List<LifePoint> lifePoints = new ArrayList<>();
        lifePoints.add(new LifePoint(LifePointStatus.NEW, "new订单对象,尚未决定类型")); // 新生
        lifePoints.add(new LifePoint(LifePointStatus.GENERATED,
                StrUtil.format("{} : 订单生成完成,订单对象已确定类型", res.getOrderType()))); // 生成
        res.setLifePoints(lifePoints);
        res.setPriority(Math.max(0L, res.getPriority() - 1)); // 优先级提高1 (数字 -1)
        res.setResendTimes(res.getResendTimes() + 1); // 重发次数+1
        res.setExecResponses(new ArrayList<>()); // 执行记录空
        res.setParentOrder(this.getRawOrderId());
        return res;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LifePoint implements Serializable { // 生命周期中, 某一时刻点
        private static final long serialVersionUID = 1454451855L;

        Long timestamp; // 此生命阶段生成时间戳, 自动new时生成
        LifePointStatus status;
        String description;
        String payload;
        String notes;

        public LifePoint(LifePointStatus status) {
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }

        public LifePoint(LifePointStatus status, String description) {
            this(status);
            this.description = description;
        }

        public LifePoint(LifePointStatus status, String description, String payload) {
            this(status, description);
            this.payload = payload;
        }

        public LifePoint(LifePointStatus status, String description, String payload, String notes) {
            this(status, description, payload);
            this.notes = notes;
        }


        public JSON asJson() {
            JSONObject lifePoint = new JSONObject();
            lifePoint.set("status", status.toString());
            lifePoint.set("timestamp", timestamp);
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
     * --> wait_check_transaction_status(等待确认成交状态, 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
     * --> checking(checking中, 例如等待成交中., 例如完全成交, 部分成交等, 仅buy/sell存在. 查询订单直接确认)
     * --> finish (订单彻底完成)
     */
    public enum LifePointStatus implements Serializable {
        NEW("new"),
        GENERATED("generated"),
        WAIT_EXECUTE("wait_execute"),
        EXECUTING("executing"),
        FINISH_EXECUTE("finish_execute"),
        WAIT_CHECK_TRANSACTION_STATUS("wait_check_transaction_status"),
        CHECKING("checking"),
        FINISH("finish");

        private static final long serialVersionUID = 101241855L;

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
