package com.scareers.gui.ths.simulation.order;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.*;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.*;

import java.io.Serializable;
import java.util.*;

/**
 * description: 核心订单对象 抽象类.
 *
 * @noti rawOrderId决定 equals和hashCode, 以便Map处理.  priority决定 compareTo,以便优先级队列处理
 * @author: admin
 * @date: 2021/12/23/023-18:17:58
 * @see LifePointStatus
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
    private DateTime generateTime; // 生成时间
    private Map<String, Object> params; // 订单api需要的其他参数map
    private List<LifePoint> lifePoints; // 有序列表, 各个生命周期情况, 生命周期由java进行管理, 无关python
    private boolean timer; // 是否记录执行时间, 过于常用 , 默认 true, 几乎不需要修改
    private Map<String, Object> otherRawMessages; // 通常需要手动设定,手动修改
    private Long priority; // 优先级, 越低则优先级越高.   默认优先级最低10000.
    private Long resendTimes; // 某情况下check后的重发对象,可能多次重发, 记录重发次数, 默认0
    private List<Map<String, Object>> execResponses;
    // 被python执行后的响应列表. 常规仅单个元素, retrying状态下可能多个,默认空al, 无论python做何响应, 应当添加.
    private String parentOrder; // 若为重发, 则指定父订单为原订单, 默认 null

    public static void main(String[] args) throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("test_param_key", "test_param_key");
        Order x = new Order("test_order_type", params);

        Console.log(x.toJsonStrForTrans());
        Console.log(x.toJsonPrettyStrForTrans());
        Console.log(x.toString());
    }


    private Order() {
        // 常态仅 orderType null, 参数空map
        this(IdUtil.objectId(),
                null,
                new DateTime(),
                new HashMap<>(),
                null,
                true, // 应当属于params, 但过于常用, 转换为基本属性
                new HashMap<>(),
                PRIORITY_LOWEST, // 默认最低优先级
                0L,
                new ArrayList<>(),
                null //
        );
        List<LifePoint> lifePoints = new ArrayList<>();
        lifePoints.add(new LifePoint(LifePointStatus.NEW, "new: 订单对象,尚未决定类型")); // 新生
        this.lifePoints = lifePoints;
    }

    public Order(String orderType, Map<String, Object> params) {
        this();
        Objects.requireNonNull(orderType); // orderType 绝对不可null
        this.orderType = orderType;
        if (params != null) { // params 可null, 自动生成空map
            this.params = params;
        }
        this.lifePoints.add(new LifePoint(LifePointStatus.GENERATED, StrUtil.format("{}: 订单对象已确定类型",
                orderType)));
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
            "rawOrderId", "orderType", "generateTime", "priority", "lifePoints", "timer", "otherRawMessages",
            "resendTimes", "execResponses", "parentOrder"
    ); // params 的key不应包含这些key, 它们均表示Order本身属性

    public JSON prepare() throws Exception {
        return prepare(false); // 将参数作为key放入order对象
    }

    /**
     * 若 forDisplay, 则将参数作为 params={} 显示, 并不展开, 默认重载方法展开, 为了传递给python api
     *
     * @param forDisplay
     * @return
     * @throws Exception
     */
    public JSON prepare(boolean forDisplay) throws Exception {
        JSONObject order = new JSONObject();
        order.set("rawOrderId", rawOrderId);
        if (orderType == null) {
            throw new Exception("订单对象尚未生成完成,不可prepare()");
        }
        order.set("orderType", orderType);
        order.set("generateTime", generateTime.toString(DatePattern.NORM_DATETIME_MS_PATTERN));
        order.set("priority", priority);
        checkParamsKeySet();
        if (forDisplay) {
            order.set("params", params);
        } else {
            order.putAll(params);
        }
        ArrayList<JSON> lifePointsJSON = new ArrayList<>();
        for (LifePoint i : lifePoints) {
            lifePointsJSON.add(i.asJson());
        }
        order.set("lifePoints", lifePointsJSON);
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
        return JSONUtil.toJsonStr(prepare(true)); // 直观显示
    }

    public String toStringPretty() throws Exception {
        return JSONUtil.toJsonPrettyStr(prepare(true)); // 直观显示
    }

    public String toJsonStrForTrans() throws Exception { // 传递给python
        return JSONUtil.toJsonStr(prepare(), orderJsonStrConfig);
    }

    public String toJsonPrettyStrForTrans() throws Exception { // 传递给python
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
     * @noti: 重发逻辑: 11项属性, 仅 orderType/params/timer 未修改, 其余8项合理初始化
     */
    public Order forResend() {
        Order res = ObjectUtil.cloneByStream(this);
        res.setRawOrderId(IdUtil.objectId());
        res.setGenerateTime(new DateTime());
        List<LifePoint> lifePoints = new ArrayList<>();
        lifePoints.add(new LifePoint(LifePointStatus.NEW, "new: 订单对象,尚未决定类型")); // 新生
        lifePoints.add(new LifePoint(LifePointStatus.GENERATED, StrUtil.format("{}: 订单对象已确定类型",
                orderType)));
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

        DateTime generateTime; // 此生命阶段生成时间戳, 自动new时生成
        LifePointStatus status;
        String description;
        String payload;
        String notes;

        public LifePoint(LifePointStatus status) {
            this.status = status;
            this.generateTime = new DateTime();
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
            lifePoint.set("generateTime", generateTime.toString(DatePattern.NORM_DATETIME_MS_PATTERN));
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
     * --> resended 已经重发了, 该状态特殊情况, 重发后正常进入finish状态!
     * --> finish (订单彻底完成)
     */
    public enum LifePointStatus implements Serializable {
        NEW("new"),
        GENERATED("generated"),
        WAIT_EXECUTE("wait_execute"),
        EXECUTING("executing"),
        FINISH_EXECUTE("finish_execute"),

        WAIT_CHECKING("checking[wait checking]"),
        CHECKED("checked"),

        RESENDED("resended"), //  已被重发的订单,随后finish
        FINISH("finish"); // 正常完成订单

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

    /**
     * 用于订单极简单显示
     */
    @Setter
    @Getter
    public static class OrderSimple implements Comparable {
        private static final long serialVersionUID = 78921545L;

        String orderType;
        DateTime generateTime;
        String rawOrderId;
        Map<String, Object> params;

        private static OrderSimple DUMMY_ORDER_SIMPLE;

        public static OrderSimple getDummyOrderSimple() {
            if (DUMMY_ORDER_SIMPLE == null) {
                DUMMY_ORDER_SIMPLE = new OrderSimple(new Order("当前尚无订单"));
            }
            return DUMMY_ORDER_SIMPLE;
        }


        public OrderSimple(Order order) {
            this.orderType = order.getOrderType();
            this.generateTime = order.getGenerateTime();
            this.rawOrderId = order.getRawOrderId();
            this.params = order.getParams();
        }

        @Override
        public String toString() { // 显示 [时间] 类型
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            builder.append(generateTime.toString("HH:mm:ss.SSS")); // 简单形式
            builder.append("]"); // 简单形式
            builder.append(" "); // 简单形式
            builder.append(orderType);
            return builder.toString();
        }

        public String toToolTip() { // 提示文字, 显示
            JSONObject toolTip = new JSONObject();
            toolTip.set("generateTime", generateTime.toString("HH:mm:ss.SSS"));
            toolTip.set("orderType", orderType);
            toolTip.set("rawOrderId", rawOrderId);
            toolTip.set("params", params);

            return JSONUtil.toJsonPrettyStr(toolTip);
        }

        @SneakyThrows
        @Override
        public int compareTo(Object o) {
            if (o instanceof OrderSimple) {
                OrderSimple orderSimpleDisplay = (OrderSimple) o;
                return this.generateTime.compareTo(((OrderSimple) o).generateTime); // 默认使用时间排序
            } else {
                throw new Exception("OrderSimple cant not compareTo other types");
            }
        }
    }


    private static final Log log = LogUtil.getLogger();

    /**
     * 获取简单展示列表, gui JList 的model使用
     *
     * @param orders
     * @return
     */
    public static Vector<OrderSimple> ordersForDisplay(List<Order> orders) {
        Vector res = new Vector();
        for (Order order : orders) {
            res.add(new OrderSimple(order));
        }
        return res;
    }
}
