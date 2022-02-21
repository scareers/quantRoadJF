package com.scareers.gui.ths.simulation.trader;

import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import com.rabbitmq.client.*;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.scareers.gui.ths.simulation.rabbitmq.SettingsOfRb.ths_trader_p2j_queue;

/**
 * 订单执行器! 将死循环线程安全优先级队列, 执行订单, 并获得响应!
 * 订单周期变化: wait_execute --> executing --> finish_execute
 * 并放入 成交监控队列
 *
 * @author admin
 */
@Getter
public class OrderExecutor {
    private static final Log log = LogUtil.getLogger();
    private static OrderExecutor INSTANCE;

    public static OrderExecutor getInstance(Trader trader) {
        if (INSTANCE == null) {
            INSTANCE = new OrderExecutor(trader);
        }
        return INSTANCE;
    }

    public static OrderExecutor getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    private Trader trader;
    private Order executingOrder; // 暂时保存正在执行的订单, 可随时查看正在执行的订单, 当订单一旦执行完成, 立即设置为null,直到下一订单开始
    private CopyOnWriteArrayList<Order> executedOrder = new CopyOnWriteArrayList<>(); // 保存执行顺序订单对象队列, 全部

    private OrderExecutor(Trader trader) {
        this.trader = trader;
    }

    public void start() {
        Thread orderExecuteTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    Order order = Trader.getOrdersWaitForExecution().take(); // 最高优先级订单, 将可能被阻塞
                    executingOrder = order;
                    executedOrder.add(order); // 添加入已执行订单队列, 无视执行结果
                    log.warn("order start execute: {} [{}] --> {}:{}", order.getOrderType(),
                            order.getPriority(), order.getRawOrderId(), order.getParams());
                    order.addLifePoint(Order.LifePointStatus.EXECUTING, "executing: 开始执行订单");
                    List<Response> responses = execOrderUtilSuccess(order);
                    executingOrder = null;
                    order.addLifePoint(Order.LifePointStatus.FINISH_EXECUTE, "finish_execute: 执行订单完成");
                    order.addLifePoint(Order.LifePointStatus.CHECKING,
                            "checking: 订单进入check队列,等待check完成");

                    order.setExecResponses(responses); // 响应字段设置
                    Trader.getOrdersWaitForCheckTransactionStatusMap().put(order, responses);
                    Trader.getOrdersAllMap().put(order, responses); // 也放入全订单队列
                }
            }
        });
        orderExecuteTask.setDaemon(true);
        orderExecuteTask.setPriority(Thread.MAX_PRIORITY);
        orderExecuteTask.setName("orderExecutor");
        orderExecuteTask.start();
        log.warn("start: orderExecutor 开始按优先级执行订单...");
    }

    /**
     * 执行订单, 执行器调用(执行器从待执行队列获取最高优先级订单后), 通常不手动调用.
     *
     * @param order
     * @return
     * @throws Exception
     * @key3
     * @warning
     * @see OrderExecutor
     */
    public List<Response> execOrderUtilSuccess(Order order)
            throws Exception {
        String orderMsg = order.toJsonStrForTrans();
        String rawOrderId = order.getRawOrderId();
        trader.sendMessageToPython(orderMsg);
        return comsumeUntilNotRetryingState(rawOrderId);
    }


    /**
     * retrying则持续等待, 否则返回执行结果, 可能 success, fail(执行正确, 订单本身原因失败)
     *
     * @param rawOrderId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Response> comsumeUntilNotRetryingState(String rawOrderId)
            throws IOException, InterruptedException {
        List<Response> responses = new CopyOnWriteArrayList<>(); // 保留响应解析成的JO
        final boolean[] finish = {false};
        Consumer consumer = new DefaultConsumer(trader.getChannelComsumer()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                String msg = new String(body, StandardCharsets.UTF_8);
                JSONObject message;
                try {
                    message = JSONUtilS.parseObj(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但解析为 JSONObject 失败: {}", msg);
                    trader.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                JSONObject rawOrderFromResponse;
                try {
                    rawOrderFromResponse = ((JSONObject) message.get("rawOrder"));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 失败: {}", message);
                    trader.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                if (rawOrderFromResponse == null) {
                    log.warn("nack: 收到来自python的消息, 但从响应获取 rawOrder 为null: {}", message);
                    trader.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }
                String rawOrderIdOfResponse = rawOrderFromResponse.getString("rawOrderId");
                if (!rawOrderId.equals(rawOrderIdOfResponse)) { // 需要是对应id
                    log.warn("nack: 收到来自python的消息, 但 rawOrderId 不匹配: should: {}, receive: {}", rawOrderId,
                            rawOrderIdOfResponse);
                    trader.getChannelComsumer().basicNack(envelope.getDeliveryTag(), false, true); // nack.
                    return;
                }

                log.info("java <-- python: {}", message);
                trader.getChannelComsumer().basicAck(envelope.getDeliveryTag(), false);
                responses.add(new Response(message)); // 可能null, 此时需要访问 responsesRaw

                Object state = message.get("state");
                if (!"retrying".equals(state.toString())) {
                    trader.getChannelComsumer().basicCancel(consumerTag);
                    finish[0] = true;
                }
            }
        };


        try {
            trader.getChannelComsumer().basicConsume(ths_trader_p2j_queue, false, consumer);
            while (!finish[0]) {
                Thread.sleep(1); // 阻塞直到 非!retrying状态
            }
        } catch (AlreadyClosedException e) {
            e.printStackTrace();
            CommonUtil.sendEmailSimple("Trader: rabbitmq通道关闭异常, 请尝试重启程序", e.getMessage(), false);
            System.exit(1); // 通道错误将退出程序, 重启即可修复
        }

        return responses;
    }

    /**
     * 是否正在执行 某只股票的卖单??
     *
     * @param stock
     * @return
     */
    public boolean executingSellOrderOf(String stock) {
        if (executingOrder == null) {
            return false;
        }
        return "sell".equals(executingOrder.getOrderType()) && executingOrder.getParams().get("stockCode").toString()
                .equals(stock);

    }

    /**
     * 是否正在执行 某只股票的买单??
     *
     * @param stock
     * @return
     */
    public boolean executingBuyOrderOf(String stock) {
        if (executingOrder == null) {
            return false;
        }
        return "buy".equals(executingOrder.getOrderType()) && executingOrder.getParams().get("stockCode").toString()
                .equals(stock);

    }

    /**
     * 当正在执行卖单时, 返回卖单的 股票代码参数, 否则返回null
     *
     * @param stock
     * @return
     */
    public String getStockArgWhenExecutingSellOrder() {
        Order orderTemp = executingOrder; // 暂时保存, 多线程尽量安全
        if (!"sell".equals(orderTemp.getOrderType())) {
            return null;
        }
        return orderTemp.getParams().get("stockCode").toString();
    }

    /**
     * 当正在执行买单时, 返回买单的 股票代码参数, 否则返回null
     *
     * @param stock
     * @return
     */
    public String getStockArgWhenExecutingBuyOrder() {
        Order orderTemp = executingOrder; // 暂时保存, 多线程尽量安全
        if (!"buy".equals(orderTemp.getOrderType())) {
            return null;
        }
        return orderTemp.getParams().get("stockCode").toString();
    }

    /**
     * 等待执行队列中存在这些股票的卖单.
     */
    public HashSet<String> hasSellOrderOfTheseStocksWaitExecute() {
        HashSet<String> res = new HashSet<>();
        for (Order order : Trader.getOrdersWaitForExecution()) { // 线程安全且迭代器安全
            if ("sell".equals(order.getOrderType())) {
                res.add(order.getParams().get("stockCode").toString());
            }
        }
        return res;
    }

    /**
     * 等待执行队列中存在这些股票的买单.
     */
    public HashSet<String> hasBuyOrderOfTheseStocksWaitExecute() {
        HashSet<String> res = new HashSet<>();
        for (Order order : Trader.getOrdersWaitForExecution()) { // 线程安全且迭代器安全
            if ("buy".equals(order.getOrderType())) {
                res.add(order.getParams().get("stockCode").toString());
            }
        }
        return res;
    }

    /**
     * 等待执行队列中存在这只股票的卖单.
     */
    public boolean hasSellOrderWaitExecuteOf(String stock) {
        for (Order order : Trader.getOrdersWaitForExecution()) { // 线程安全且迭代器安全
            if ("sell".equals(order.getOrderType())) {
                if (stock.equals(order.getParams().get("stockCode").toString())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 等待执行队列中存在这只股票的买单.
     */
    public boolean hasBuyOrderWaitExecuteOf(String stock) {
        for (Order order : Trader.getOrdersWaitForExecution()) { // 线程安全且迭代器安全
            if ("buy".equals(order.getOrderType())) {
                if (stock.equals(order.getParams().get("stockCode").toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}

