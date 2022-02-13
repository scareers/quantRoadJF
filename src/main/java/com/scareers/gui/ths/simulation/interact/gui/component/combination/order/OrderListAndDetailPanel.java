package com.scareers.gui.ths.simulation.interact.gui.component.combination.order;

import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.OrderListCellRendererS;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.OrderPo;
import com.scareers.gui.ths.simulation.trader.AccountStates;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: 显示 Order(某)列表 即某一具体Order细节 即响应列表. 组合 panel
 * 需要指定类型, 以便死循环遍历更新数据! 不同的类型将遍历不同Trader属性
 *
 * @author: admin
 * @date: 2022/1/19/019-00:06:06
 */
public class OrderListAndDetailPanel extends JPanel {
    public static volatile OrderListAndDetailPanel INSTANCE;
    // @noti: 使用单例模式. 当在 5种类型切换时, 仅仅切换 Type, 而jList和两个order展示, 将实时根据Type获取数据
    public static volatile Type currentDataFlushType = Type.ORDER_ALL_MAP; // 默认all
    // 股票列表池, 分为不同 Type. 将读取 objectPool 所有key(已注册的类型), 对对应key更新列表, 保存入 map.
    public static volatile Vector<OrderPo> currentOrderListShouldDisplay = new Vector<>(
            Arrays.asList(Order.OrderPo.getDummyOrderSimple()));
    public static int remainDisplayCount = 20; // 显示最新20全部,以及此前的所有非账户信息监控

    /**
     * 单例模式
     *
     * @param type
     * @param mainDisplayWindow
     * @return
     */
    public static OrderListAndDetailPanel getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            // 首次调用, 将开始更新数据
            ThreadUtil.execAsync(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    //等待 有控件被注册
                    try {
                        waitUtil(() -> {
                            try {
                                return Trader.allOrderAmount > 0;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return false;
                        }, Integer.MAX_VALUE, 1, "等待首个订单生成", true);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                    while (true) { // 开始更新所有注册控件类型的相关数据列表
                        Vector<Order.OrderPo> simpleOrders = new Vector<>();
                        if (currentDataFlushType == Type.ORDERS_WAIT_FOR_EXECUTION) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersWaitForExecution));
                        } else if (currentDataFlushType == Type.ORDER_ALL_MAP) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersAllMap.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(
                                            Trader.ordersWaitForCheckTransactionStatusMap.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_SUCCESS_FINISHED) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersSuccessFinished.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_RESEND_FINISHED) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersResendFinished.keySet()));
                        } else if (currentDataFlushType == Type.ORDERS_FAILED_FINISHED) {
                            simpleOrders = Order.ordersForDisplay(
                                    new ArrayList<>(Trader.ordersFailedFinallyNeedManualHandle.keySet()));
                        } else {
                            System.out.println("未知类型");
                        }
                        if (simpleOrders.size() == 0) {
                            simpleOrders.add(OrderPo.getDummyOrderSimple());
                        }
                        Collections.sort(simpleOrders); // 有序
                        if (simpleOrders.size() > remainDisplayCount) {
                            Vector<OrderPo> temp = new Vector<>();
                            for (int i = 0; i < simpleOrders.size() - remainDisplayCount; i++) {
                                if (AccountStates.ORDER_TYPES.contains(simpleOrders.get(i).getOrderType())) {
                                    continue;
                                }
                                temp.add(simpleOrders.get(i)); // 前面的非账户监控的所有
                            }
                            temp.addAll(
                                    simpleOrders
                                            .subList(simpleOrders.size() - remainDisplayCount, simpleOrders.size()));
                            currentOrderListShouldDisplay = temp;
                        } else {
                            currentOrderListShouldDisplay = simpleOrders;// 真实更新数据池
                        }
                        Thread.sleep(10);
                    }
                }
            }, true);
            INSTANCE = new OrderListAndDetailPanel(Type.ORDER_ALL_MAP, mainDisplayWindow); // 默认所有订单,自行调用changeType
        }
        return INSTANCE;
    }

    /**
     * 5类队列
     */
    public enum Type {
        ORDERS_WAIT_FOR_EXECUTION,
        ORDER_ALL_MAP,
        ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP,
        ORDERS_SUCCESS_FINISHED,
        ORDERS_RESEND_FINISHED,
        ORDERS_FAILED_FINISHED
    }


    MainDisplayWindow mainDisplayWindow; // 主显示区
    private volatile Type type; // 类型
    volatile JList<Order.OrderPo> jList;


    private OrderListAndDetailPanel(Type type, MainDisplayWindow mainDisplayWindow) {
        super();
        this.type = type;
        this.mainDisplayWindow = mainDisplayWindow;
        currentDataFlushType = this.type;
        this.setBorder(BorderFactory.createLineBorder(Color.black));
        mainDisplayWindow.setBackground(COLOR_THEME_MINOR);

        // 1.主容器
        this.setLayout(new BorderLayout());

        // 2.JList显示列表
        jList = getOrderSimpleJList();
        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        JScrollPane jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setBorder(null);
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jList);
        jScrollPaneForList.getViewport().setBackground(SettingsOfGuiGlobal.COLOR_THEME_MINOR);
        this.add(jScrollPaneForList, BorderLayout.WEST); // 添加列表
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义barUi

        // 3.JSplitPane 分割显示 Order详情 及 响应对象
        JSplitPane orderContent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // box 存放 order详情和响应
        orderContent.setBorder(null);
        orderContent.setDividerLocation(500);
        orderContent.setDividerSize(5);

        // 3.1. Order详情控件
        OrderDetailPanel orderDetailPanel = getDetailPanel();
        orderContent.setLeftComponent(orderDetailPanel);

        // 3.2. Order响应控件
        OrderResponsePanel responsePanel = getResponsePanel();
        orderContent.setRightComponent(responsePanel);


        // 3.3 分割线对象: @noti: 设置分割线颜色 必须重写ui类和 divider类才行
        BasicSplitPaneUI ui = new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);
                        Dimension size = getSize();
                        g.setColor(COLOR_SPLIT_PANE_DIVIDER_BACK); //
                        g.fillRect(0, 0, size.width, size.height);
                    }
                };
                return divider;
            }
        };
        orderContent.setUI(ui);

        // 4.添加分割面板
        this.add(orderContent, BorderLayout.CENTER);


        // 6.主panel 添加尺寸改变监听. 改变 jList 和 orderContent尺寸
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setBounds(0, 0, 300, getHeight()); // 固定宽 300
                orderContent.setDividerLocation(0.45); // 分割位置百分比
                orderContent.setBounds(300, 0, getWidth() - 300, getHeight()); // 其余占满
            }
        });


        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    List<Order> allOrder = // 因此直接在全map中查找. 且像等待队列一类, 可能出现点击后消失, 找不到订单
                            new ArrayList<>(Trader.ordersAllMap.keySet());
                    Order currentOrder = null;
                    if (jList.getSelectedIndex() == -1) {
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }
                    String orderId = null; // 避免变化, 及时JList已经刷新, 也能展示当时点击的订单而非刷新后
                    try {
                        orderId = currentOrderListShouldDisplay.get(jList.getSelectedIndex())
                                .getRawOrderId();
                        // 因实时改变,需要防止越界
                    } catch (Exception e) {
                        // System.out.println("出错");
                        // @noti: AWT 框架自动显示错误信息. 用户侧不显示. 常出现索引列表越界错误.. 因为列表内容时动态的
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }
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
        }, true);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    /**
     * 修改类型达成展示列表的改变,
     *
     * @param newType
     * @return
     * @key3 实测
     */
    @SneakyThrows
    public OrderListAndDetailPanel changeType(Type newType) {
        this.type = newType;
        currentDataFlushType = this.type; // 将更换刷新数据源
        jList.setSelectedIndex(0); // 选择第一个.
        return this;
    }

    private OrderDetailPanel getDetailPanel() {
        return new OrderDetailPanel();
    }

    private OrderResponsePanel getResponsePanel() {
        return new OrderResponsePanel();
    }

    /**
     * 只需要 切换 type, 则达成 JList的数据改变
     *
     * @return
     */
    private JList<OrderPo> getOrderSimpleJList() {
        DefaultListModelS<Order.OrderPo> model = new DefaultListModelS<>();
        model.flush(currentOrderListShouldDisplay);

        JList<Order.OrderPo> jList = new JList<>(model);
        jList.setCellRenderer(new OrderListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);

        // 单例单线程
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(currentOrderListShouldDisplay);
                    Thread.sleep(10);
                }
            }
        }, true);

        return jList;
    }


}
