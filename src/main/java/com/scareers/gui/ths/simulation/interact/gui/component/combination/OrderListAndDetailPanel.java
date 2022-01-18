package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.OrderListCellRendererS;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.OrderSimple;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_GRAY_COMMON;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;

/**
 * description: 显示 Order(某)列表 即某一具体Order细节 即响应列表. 组合 panel
 * 需要指定类型, 以便死循环遍历更新数据! 不同的类型将遍历不同Trader属性
 *
 * @author: admin
 * @date: 2022/1/19/019-00:06:06
 */
public class OrderListAndDetailPanel {
    public enum Type {
        ORDERS_WAIT_FOR_EXECUTION,
        ORDER_ALL_MAP
    }

    private volatile OrderSimple selectedOrder; // 唯一被选中订单
    private boolean monitorStarted; // 监控订单的线程是否启动
    private boolean selectedOrderChanged = false; // 监控订单的线程是否启动
    MainDisplayWindow mainDisplayWindow;

    private boolean stopFlushList = false; // 可设置此flag, 停止死循环更新 JList
    private boolean stoped = false; // 是否已被停止, 将 waitUtil此flag, 代表 JList的刷新已被真正停止. 见 stopFlushJList 方法

    private void changeToOrdersWaitForExecution() throws Exception {
        JList<OrderSimple> jList = getOrderSimpleJList();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(jList, BorderLayout.WEST); // 添加列表
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_TITLE);

        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
        orderContent.setDividerLocation(500);

        OrderDetailPanel orderDetailPanel = getDetailPanel();
//        orderDetailPanel.setPreferredSize(new Dimension(700, 10));
        orderContent.setLeftComponent(orderDetailPanel); // 添加响应

        OrderResponsePanel responsePanel = getResponsePanel();
        orderContent.setRightComponent(responsePanel); // 添加响应
//        responsePanel.setPreferredSize(new Dimension(10000, 10000));


        jList.addListSelectionListener(new ListSelectionListener() {
            @SneakyThrows
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return; // 若是数据更新调整, 则无视
                }
                ThreadUtil.execAsync(new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        while (true) {
                            int index = e.getLastIndex();
                            try {
                                selectedOrder = jList.getModel().getElementAt(index);
                                selectedOrderChanged = true;
                            } catch (Exception ex) {
                                Thread.sleep(100);
                                continue;
                            }
                            break; // 仅设置1次
                        }
                    }
                }, true);
            }
        });

        panel.add(orderContent, BorderLayout.CENTER); // 添加详情
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setSize(300, 10000);
                orderContent.setDividerLocation(0.35); // 分割位置百分比
                orderContent.setBounds(0, 0, panel.getWidth() - 300, panel.getHeight()); // 占满
            }
        });

        jList.setSelectedIndex(0); // 选择第一个

        if (monitorStarted) {
            return;
        }
        ThreadUtil.execAsync(new Runnable() { // 开始监控 selectedOrder
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    if (selectedOrder == null) {
                        Thread.sleep(100);
                        continue;
                    }
                    String orderId = selectedOrder.getRawOrderId();

                    while (true) {
                        if (selectedOrderChanged) {
                            selectedOrderChanged = false;
                            break;
                        }
                        Order currentOrder = null;
                        // 从最新队列读取
                        for (Order order : Trader.getInstance().getOrdersAllMap().keySet()) {
//                        for (Order order : Trader.getInstance().getOrdersWaitForExecution()) {
                            if (order.getRawOrderId().equals(orderId)) {
                                currentOrder = order;
                            }
                        } // 查找具体
                        if (currentOrder != null) {
                            orderDetailPanel.updateText(currentOrder);
                            responsePanel.updateText(currentOrder);
                        }
                        Thread.sleep(100); // 不断刷新响应
                    }
                }
            }
        }, true);
        mainDisplayWindow.setCenterPanel(panel);
    }

    private OrderDetailPanel getDetailPanel() {
        return new OrderDetailPanel();
    }

    private OrderResponsePanel getResponsePanel() {
        return new OrderResponsePanel();
    }

    private JList<Order.OrderSimple> getOrderSimpleJList() throws Exception {
        Vector<OrderSimple> simpleOrders = getOrderSimplesAccordingType();
        DefaultListModelS<Order.OrderSimple> model = new DefaultListModelS<>();
        model.flush(simpleOrders);

        JList<Order.OrderSimple> jList = new JList<>(model);
        jList.setCellRenderer(new OrderListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每半秒刷新model
                    Vector<Order.OrderSimple> simpleOrders = Order
                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersAllMap().keySet()));
//                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersWaitForExecution()));
                    if (simpleOrders.size() == 0) {
                        simpleOrders.add(Order.OrderSimple.getDummyOrderSimple());
                    }
                    model.flush(simpleOrders);
                    Thread.sleep(100);
                }
            }
        }, true);
        return jList;
    }

    private Vector<OrderSimple> getOrderSimplesAccordingType() throws Exception {
        Trader trader = Trader.getInstance();
        PriorityBlockingQueue<Order> orders = trader.getOrdersWaitForExecution();

        Vector<OrderSimple> simpleOrders = Order.ordersForDisplay(new ArrayList<>(orders));

        if (simpleOrders.size() == 0) {
            simpleOrders.add(OrderSimple.getDummyOrderSimple());
        }
        return simpleOrders;
    }
}
