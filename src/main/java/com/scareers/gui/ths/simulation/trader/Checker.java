package com.scareers.gui.ths.simulation.trader;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.utils.log.LogUtil;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * 对订单执行结果进行判定监控check!
 * 收到python响应后, 放入check Map, 等待 结果 check!
 *
 * @author admin
 * @noti 约定: 订单重发, 均构造类似订单
 * @noti 账号状态相关api, 几乎每个策略都相同, 无需 主策略实现特殊的check逻辑!
 * @noti 典型的: buy/sell 订单的check逻辑, 应当由 主策 略实现!
 */
public class Checker {
    private static final Log log = LogUtil.getLogger();
    private static Checker INSTANCE;

    /**
     * 单例模式实现工厂方法, 应先调用此, 后才可调用无参方法
     *
     * @param trader
     * @return
     */
    public static Checker getInstance(Trader trader) {
        if (INSTANCE == null) {
            INSTANCE = new Checker(trader);
        }
        return INSTANCE;
    }

    public static Checker getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    private Trader trader;

    private Checker(Trader trader) {
        this.trader = trader;
    }

    public void startCheckTransactionStatus() {
        Thread checkTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    for (Order order : new CopyOnWriteArrayList<>(Trader.getOrdersWaitForCheckTransactionStatusMap().keySet())) {
                        List<Response> responses = Trader.getOrdersWaitForCheckTransactionStatusMap().get(order);
                        String orderType = order.getOrderType();
                        if (AccountStates.ORDER_TYPES.contains(orderType)) {
                            // 若是账户状态相关订单, 则交由  AccountStates 进行check
                            trader.getAccountStates().checkForAccountStates(order, responses, orderType);
                        } else {
                            trader.getStrategy().checkOrder(order, responses, orderType);
                        }
                    }
                }
            }
        });
        checkTask.setDaemon(true);
        checkTask.setPriority(Thread.MAX_PRIORITY);
        checkTask.setName("checkTransStatus");
        checkTask.start();
        log.warn("check start: 开始check订单成交状况");
    }
}