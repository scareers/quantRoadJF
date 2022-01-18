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
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_GRAY_COMMON;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: 显示 Order(某)列表 即某一具体Order细节 即响应列表. 组合 panel
 * 需要指定类型, 以便死循环遍历更新数据! 不同的类型将遍历不同Trader属性
 *
 * @author: admin
 * @date: 2022/1/19/019-00:06:06
 */
public class OrderListAndDetailPanel extends JPanel {
    // 本类对象池, 单类型单例
    public static ConcurrentHashMap<Type, OrderListAndDetailPanel> objectPool = new ConcurrentHashMap<>();
    // 股票列表池, 分为不同 Type. 将读取 objectPool 所有key(已注册的类型), 对对应key更新列表, 保存入 map.
    public static ConcurrentHashMap<Type, Vector<OrderSimple>> orderListPool = new ConcurrentHashMap<>();

    static {
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                // 等待 有控件被注册
                waitUtil(() -> objectPool.size() != 0, Integer.MAX_VALUE, 1, null, false);
                while (true) { // 开始更新所有注册控件类型的相关数据列表
                    for (Type typeRegistered : objectPool.keySet()) {
                        Vector<OrderSimple> simpleOrders = new Vector<>();
                        if (typeRegistered == Type.ORDERS_WAIT_FOR_EXECUTION) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.getInstance().getOrdersWaitForExecution()));
                        } else if (typeRegistered == Type.ORDER_ALL_MAP) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.getInstance().getOrdersAllMap().keySet()));
                        }// todo: 其他类型

                        if (simpleOrders.size() == 0) {
                            simpleOrders.add(OrderSimple.getDummyOrderSimple());
                        }
                        orderListPool.put(typeRegistered, simpleOrders); // 真实更新数据池
                    }
                    // 数据更新频率
                    Thread.sleep(1);
                }
            }
        }, true);
    }

    public static synchronized OrderListAndDetailPanel getInstance(Type type, MainDisplayWindow mainDisplayWindow) {
        if (orderListPool.size() == 0) { // 首次,将开始更新 orderListPool,仅执行一次

        }
        return null;
    }

    public enum Type {
        ORDERS_WAIT_FOR_EXECUTION,
        ORDER_ALL_MAP
    }


    private volatile OrderSimple selectedOrder; // 唯一被选中订单
    MainDisplayWindow mainDisplayWindow; // 主显示区
    private Type type; // 类型

    private volatile boolean changeSelect = false;

    private OrderListAndDetailPanel() {
    }

    public OrderListAndDetailPanel(Type type, MainDisplayWindow mainDisplayWindow) {
        super();
        this.type = type;
        this.mainDisplayWindow = mainDisplayWindow;
        objectPool.put(type, this); // 注册, 此时 orderListPool 将不断更新数据

        // 1.主容器
        this.setLayout(new BorderLayout());

        // 2.JList显示列表
        JList<OrderSimple> jList = getOrderSimpleJList();
        this.add(jList, BorderLayout.WEST); // 添加列表
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_TITLE);

        // 3.JSplitPane 分割显示 Order详情 及 响应对象
        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
        orderContent.setDividerLocation(500);

        // 3.1. Order详情控件
        OrderDetailPanel orderDetailPanel = getDetailPanel();
        orderContent.setLeftComponent(orderDetailPanel);

        // 3.2. Order响应控件
        OrderResponsePanel responsePanel = getResponsePanel();
        orderContent.setRightComponent(responsePanel);

        // 4.添加分割面板
        this.add(orderContent, BorderLayout.CENTER);


        // 5.jList 绑定监听. 选择项目后, 死循环不断更新 orderDetailPanel 与 responsePanel 内容,
        // 此时 Order对象固定,不会因 JList发生改变而改变
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
                        try {
                            selectedOrder = jList.getModel().getElementAt(e.getLastIndex());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            changeSelect = true;
                        }
                    }
                }, true);
            }
        });

        // 6.主panel 添加尺寸改变监听. 改变 jList 和 orderContent尺寸
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setBounds(0, 0, 300, getHeight()); // 固定宽 300
                orderContent.setDividerLocation(0.32); // 分割位置百分比
                orderContent.setBounds(300, 0, getWidth() - 300, getHeight()); // 其余占满
            }
        });
        // 7.初始选择首个 Order, 监听器中含有 等待实际被选中的逻辑, 将等待实际被选中
        jList.setSelectedIndex(0); // 选择第一个

        // 8.开始线程, 不断根据 selectedOrder 而刷新 订单详情+订单响应界面的数据
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    if (selectedOrder == null) { // 等待订单设定
                        Thread.sleep(1);
                        continue;
                    }
                    // 成功选择订单,或者改变订单.  获取订单id
                    changeSelect = false; // 将死循环遍历当前订单, 直到被其他线程 修改 changeSelect
                    String orderId = selectedOrder.getRawOrderId(); // 这里逻辑上已经控制了Order可能的类型
                    while (true) {
                        if (changeSelect) {
                            // 若选择改变, 则跳出刷新该订单, 将会读取 新的订单
                            // 该 flag 显然在 JList 选择监听中修改
                            break;
                        }
                        List<Order> allOrder = // 因此直接在全map中查找. 且像等待队列一类, 可能出现点击后消失, 找不到订单
                                new ArrayList<>(Trader.getInstance().getOrdersAllMap().keySet());
                        Order currentOrder = null;
                        for (Order order : allOrder) {
                            if (order.getRawOrderId().equals(orderId)) {
                                currentOrder = order; // 找到唯一的 Order 对象
                                break;
                            }
                        }
                        if (currentOrder != null) {
                            orderDetailPanel.updateText(currentOrder);
                            responsePanel.updateText(currentOrder);
                        }
                        Thread.sleep(10); // 不断刷新
                    }
                }
            }
        }, true);

        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }


    private OrderDetailPanel getDetailPanel() {
        return new OrderDetailPanel();
    }

    private OrderResponsePanel getResponsePanel() {
        return new OrderResponsePanel();
    }

    private JList<Order.OrderSimple> getOrderSimpleJList() {
        Vector<OrderSimple> simpleOrders = orderListPool // 尝试获取, 若尚未更新则显示dummy. 理论上极快就能更新
                .getOrDefault(this.type, new Vector<>(Arrays.asList(OrderSimple.getDummyOrderSimple())));

        DefaultListModelS<Order.OrderSimple> model = new DefaultListModelS<>();
        model.flush(simpleOrders);

        JList<Order.OrderSimple> jList = new JList<>(model);
        jList.setCellRenderer(new OrderListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);

        // 单例单线程
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    Vector<OrderSimple> simpleOrders = orderListPool
                            .getOrDefault(type, new Vector<>(Arrays.asList(OrderSimple.getDummyOrderSimple())));
                    model.flush(simpleOrders);
                    Thread.sleep(100);
                }
            }
        }, true);

        return jList;
    }


//    private volatile OrderSimple selectedOrder; // 唯一被选中订单
//    private boolean monitorStarted; // 监控订单的线程是否启动
//    private boolean selectedOrderChanged = false; // 监控订单的线程是否启动
//    MainDisplayWindow mainDisplayWindow;
//
//    private boolean stopFlushList = false; // 可设置此flag, 停止死循环更新 JList
//    private boolean stoped = false; // 是否已被停止, 将 waitUtil此flag, 代表 JList的刷新已被真正停止. 见 stopFlushJList 方法
//
//    private void changeToOrdersWaitForExecution() throws Exception {
//        JList<OrderSimple> jList = getOrderSimpleJList();
//
//        JPanel panel = new JPanel();
//        panel.setLayout(new BorderLayout());
//        panel.add(jList, BorderLayout.WEST); // 添加列表
//        jList.setPreferredSize(new Dimension(300, 10000));
//        jList.setBackground(COLOR_THEME_TITLE);
//
//        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
//        orderContent.setDividerLocation(500);
//
//        OrderDetailPanel orderDetailPanel = getDetailPanel();
////        orderDetailPanel.setPreferredSize(new Dimension(700, 10));
//        orderContent.setLeftComponent(orderDetailPanel); // 添加响应
//
//        OrderResponsePanel responsePanel = getResponsePanel();
//        orderContent.setRightComponent(responsePanel); // 添加响应
////        responsePanel.setPreferredSize(new Dimension(10000, 10000));
//
//
//        jList.addListSelectionListener(new ListSelectionListener() {
//            @SneakyThrows
//            @Override
//            public void valueChanged(ListSelectionEvent e) {
//                if (e.getValueIsAdjusting()) {
//                    return; // 若是数据更新调整, 则无视
//                }
//                ThreadUtil.execAsync(new Runnable() {
//                    @SneakyThrows
//                    @Override
//                    public void run() {
//                        while (true) {
//                            int index = e.getLastIndex();
//                            try {
//                                selectedOrder = jList.getModel().getElementAt(index);
//                                selectedOrderChanged = true;
//                            } catch (Exception ex) {
//                                Thread.sleep(100);
//                                continue;
//                            }
//                            break; // 仅设置1次
//                        }
//                    }
//                }, true);
//            }
//        });
//
//        panel.add(orderContent, BorderLayout.CENTER); // 添加详情
//        panel.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                jList.setSize(300, 10000);
//                orderContent.setDividerLocation(0.35); // 分割位置百分比
//                orderContent.setBounds(0, 0, panel.getWidth() - 300, panel.getHeight()); // 占满
//            }
//        });
//
//        jList.setSelectedIndex(0); // 选择第一个
//
//        if (monitorStarted) {
//            return;
//        }
//        ThreadUtil.execAsync(new Runnable() { // 开始监控 selectedOrder
//            @SneakyThrows
//            @Override
//            public void run() {
//                while (true) {
//                    if (selectedOrder == null) {
//                        Thread.sleep(100);
//                        continue;
//                    }
//                    String orderId = selectedOrder.getRawOrderId();
//
//                    while (true) {
//                        if (selectedOrderChanged) {
//                            selectedOrderChanged = false;
//                            break;
//                        }
//                        Order currentOrder = null;
//                        // 从最新队列读取
//                        for (Order order : Trader.getInstance().getOrdersAllMap().keySet()) {
////                        for (Order order : Trader.getInstance().getOrdersWaitForExecution()) {
//                            if (order.getRawOrderId().equals(orderId)) {
//                                currentOrder = order;
//                            }
//                        } // 查找具体
//                        if (currentOrder != null) {
//                            orderDetailPanel.updateText(currentOrder);
//                            responsePanel.updateText(currentOrder);
//                        }
//                        Thread.sleep(100); // 不断刷新响应
//                    }
//                }
//            }
//        }, true);
//        mainDisplayWindow.setCenterPanel(panel);
//    }
//
//    private OrderDetailPanel getDetailPanel() {
//        return new OrderDetailPanel();
//    }
//
//    private OrderResponsePanel getResponsePanel() {
//        return new OrderResponsePanel();
//    }
//
//    private JList<Order.OrderSimple> getOrderSimpleJList() throws Exception {
//        Vector<OrderSimple> simpleOrders = getOrderSimplesAccordingType();
//        DefaultListModelS<Order.OrderSimple> model = new DefaultListModelS<>();
//        model.flush(simpleOrders);
//
//        JList<Order.OrderSimple> jList = new JList<>(model);
//        jList.setCellRenderer(new OrderListCellRendererS());
//        jList.setForeground(COLOR_GRAY_COMMON);
//        ThreadUtil.execAsync(new Runnable() {
//            @SneakyThrows
//            @Override
//            public void run() {
//                while (true) { // 每半秒刷新model
//                    Vector<Order.OrderSimple> simpleOrders = Order
//                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersAllMap().keySet()));
////                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersWaitForExecution()));
//                    if (simpleOrders.size() == 0) {
//                        simpleOrders.add(Order.OrderSimple.getDummyOrderSimple());
//                    }
//                    model.flush(simpleOrders);
//                    Thread.sleep(100);
//                }
//            }
//        }, true);
//        return jList;
//    }
//
//    private Vector<OrderSimple> getOrderSimplesAccordingType() throws Exception {
//        Trader trader = Trader.getInstance();
//        PriorityBlockingQueue<Order> orders = trader.getOrdersWaitForExecution();
//
//        Vector<OrderSimple> simpleOrders = Order.ordersForDisplay(new ArrayList<>(orders));
//
//        if (simpleOrders.size() == 0) {
//            simpleOrders.add(OrderSimple.getDummyOrderSimple());
//        }
//        return simpleOrders;
//    }
}
