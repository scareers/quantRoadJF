package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.OrderDetailPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.OrderListCellRendererS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.TreeCellRendererS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.order.Order.OrderSimple;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.util.ImageScaler.zoomBySize;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
@Getter
@Setter
public class ObjectTreeWindow extends FuncFrameS {
    private static ObjectTreeWindow INSTANCE;

    public static ObjectTreeWindow getInstance(Type type, String title, TraderGui mainWindow,
                                               FuncButton belongBtn, boolean resizable, boolean closable,
                                               boolean maximizable,
                                               boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                               double preferScale,
                                               int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new ObjectTreeWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
            INSTANCE.setIconifiable(false);
            INSTANCE.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    INSTANCE.getCorePanel().getMainDisplayWindow().flushBounds(true); // 编辑界面就像第一次刷新
                }
            });
            INSTANCE.setBorder(new LineBorder(COLOR_MAIN_DISPLAY_BORDER, 1));
            ((BasicInternalFrameUI) INSTANCE.getUI()).setNorthPane(null);
        }
        INSTANCE.getFuncTools().setVisible(false);
//        BasicInternalFrameUI ui = (BasicInternalFrameUI) INSTANCE.getUI();
//        BasicInternalFrameTitlePane titlePane = (BasicInternalFrameTitlePane)ui.getNorthPane();
//        titlePane.selectedTitleColor
//        selectedTitleColor = UIManager.getColor("InternalFrame.activeTitleBackground");
//        selectedTextColor = UIManager.getColor("InternalFrame.activeTitleForeground");
//        notSelectedTitleColor = UIManager.getColor("InternalFrame.inactiveTitleBackground");
//        notSelectedTextColor = UIManager.getColor("InternalFrame.inactiveTitleForeground");
        return INSTANCE;
    }

    public static ObjectTreeWindow getInstance() {
        return INSTANCE; // 可null
    }

    private ObjectTreeWindow(Type type, String title, TraderGui mainWindow,
                             FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                             boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                             double preferScale,
                             int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    JTree tree;

    @Override
    public void initCenterPanel() { // 抽象方法
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        tree = buildTree();
        tree.setBackground(COLOR_THEME_MINOR);
        jPanel.add(tree, BorderLayout.WEST);
        tree.setLocation(0, 0);
        JScrollPane jScrollPane = new JScrollPane(jPanel);
        jScrollPane.setBorder(null);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jScrollPane, BorderLayout.CENTER);
        this.centerPanel = panel;

        this.add(this.centerPanel, BorderLayout.CENTER);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // 工具栏可重写(两组按钮)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }

    private JTree buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("对象查看");

        DefaultMutableTreeNode traderNode = new DefaultMutableTreeNode("Trader");


        // 5大子对象,可扩展
        DefaultMutableTreeNode orderExecutorNode = new DefaultMutableTreeNode("OrderExecutor");
        DefaultMutableTreeNode checkerNode = new DefaultMutableTreeNode("Checker");
        DefaultMutableTreeNode accountStatesNode = new DefaultMutableTreeNode("AccountStates");
        DefaultMutableTreeNode fsTransactionFetcherNode = new DefaultMutableTreeNode("FsTransactionFetcher");
        DefaultMutableTreeNode strategyNode = new DefaultMutableTreeNode("Strategy");

        // 4大队列/map
        DefaultMutableTreeNode queues = new DefaultMutableTreeNode("Queues!"); // 父包含
        DefaultMutableTreeNode ordersWaitForExecution = new DefaultMutableTreeNode("ordersWaitForExecution");
        DefaultMutableTreeNode ordersWaitForCheckTransactionStatusMap = new DefaultMutableTreeNode(
                "ordersWaitForCheckTransactionStatusMap");
        DefaultMutableTreeNode ordersSuccessFinished = new DefaultMutableTreeNode("ordersSuccessFinished");
        DefaultMutableTreeNode ordersResendFinished = new DefaultMutableTreeNode("ordersResendFinished");
        queues.add(ordersWaitForExecution);
        queues.add(ordersWaitForCheckTransactionStatusMap);
        queues.add(ordersSuccessFinished);
        queues.add(ordersResendFinished);

        traderNode.add(queues);
        traderNode.add(orderExecutorNode);
        traderNode.add(checkerNode);
        traderNode.add(accountStatesNode);
        traderNode.add(fsTransactionFetcherNode);
        traderNode.add(strategyNode);

        root.add(traderNode);

        final JTree tree = new JTree(root);
        // 添加选择事件

        TreeCellRendererS renderer = new TreeCellRendererS();
        renderer.setBackgroundNonSelectionColor(COLOR_THEME_MINOR);
        renderer.setBackgroundSelectionColor(COLOR_TREE_ITEM_SELECTED);
        renderer.setBorderSelectionColor(Color.red);
        renderer.setClosedIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FOLDER_CLOSE_PATH)).getImage(), 16, 16)));
        renderer.setOpenIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FOLDER_OPEN_PATH)).getImage(), 16, 16)));
        renderer.setLeafIcon(new ImageIcon(
                zoomBySize(new ImageIcon(ResourceUtil.getResource(ICON_FILE0_PATH)).getImage(), 15, 15)));
        renderer.setFont(new Font("微软雅黑", Font.BOLD, 14));
        renderer.setTextSelectionColor(COLOR_GRAY_COMMON);
        renderer.setTextNonSelectionColor(COLOR_GRAY_COMMON);

        tree.setCellRenderer(renderer);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @SneakyThrows
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                dispatch(e.getNewLeadSelectionPath().toString());
            }
        });
        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.ORDERS_WAIT_FOR_EXECUTION);
        return tree;
    }


    public void dispatch(String treePath) throws Exception {
        if (TreePathConstants.ORDERS_WAIT_FOR_EXECUTION.equals(treePath)) {
            // private PriorityBlockingQueue<Order> ordersWaitForExecution;
            changeToOrdersWaitForExecution(); // 均为切换 mainDisplay 显示界面的方法.
        } else {
            System.out.println(treePath);
        }
    }

    /**
     * 等待执行的订单队列展示.
     * JPanel --> 左 JList显示列表, 右详细内容.
     * 均需要重设主界面的 CenterPanel (默认空panel)
     */
    private void changeToOrdersWaitForExecution() throws Exception {
        JList<OrderSimple> jList = getOrderSimpleJListOfOrdersWaitForExecution();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(jList, BorderLayout.WEST); // 添加列表
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_TITLE);
        OrderDetailPanel orderDetailPanel = getDetailPanel();
        panel.add(orderDetailPanel, BorderLayout.CENTER); // 添加详情


        jList.addListSelectionListener(new ListSelectionListener() {
            @SneakyThrows
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ThreadUtil.execAsync(new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        while (true) {
                            int index = e.getLastIndex();
                            OrderSimple orderSimple = null;
                            try {
                                orderSimple = jList.getModel().getElementAt(index);
                            } catch (Exception ex) {
                                Thread.sleep(100);
                                continue;
                            }
                            String orderId = orderSimple.getRawOrderId();
                            Order currentOrder = null;
                            // 从最新队列读取
                            for (Order order : Trader.getInstance().getOrdersWaitForExecution()) {
                                if (order.getRawOrderId().equals(orderId)) {
                                    currentOrder = order;
                                }
                            } // 查找具体
                            if (currentOrder != null) {
                                orderDetailPanel.setText(currentOrder.toStringPretty());
                            }
                            break; // 静态
                        }
                    }
                }, true);
            }
        });
        jList.setSelectedIndex(0); // 选择第一个

        this.getMainDisplayWindow().setCenterPanel(panel);
    }

    private OrderDetailPanel getDetailPanel() {
        return new OrderDetailPanel();
    }

    private JList<OrderSimple> getOrderSimpleJListOfOrdersWaitForExecution() throws Exception {
        Trader trader = Trader.getInstance();
        PriorityBlockingQueue<Order> orders = trader.getOrdersWaitForExecution();
        Vector<OrderSimple> simpleOrders = Order.ordersForDisplay(new ArrayList<>(orders));
        if (simpleOrders.size() == 0) {
            simpleOrders.add(OrderSimple.getDummyOrderSimple());
        }

        DefaultListModelS<OrderSimple> model = new DefaultListModelS<>();
        model.flush(simpleOrders);
        JList<OrderSimple> jList = new JList<>(model);
        jList.setCellRenderer(new OrderListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每半秒刷新model
                    Vector<OrderSimple> simpleOrders = Order
                            .ordersForDisplay(new ArrayList<>(Trader.getInstance().getOrdersWaitForExecution()));
                    if (simpleOrders.size() == 0) {
                        simpleOrders.add(OrderSimple.getDummyOrderSimple());
                    }
                    model.flush(simpleOrders);
                    Thread.sleep(1000);
                }
            }
        }, true);
        return jList;
    }


    public static class TreePathConstants { // 路径常量, 字符串配置
        /**
         * [对象查看]
         * [对象查看, Trader]
         * [对象查看, Trader, Queues!]
         * [对象查看, Trader, OrderExecutor]
         * [对象查看, Trader, Checker]
         * [对象查看, Trader, AccountStates]
         * [对象查看, Trader, FsTransactionFetcher]
         * [对象查看, Trader, Strategy]
         * [对象查看, Trader, Queues!, ordersWaitForExecution]
         * [对象查看, Trader, Queues!, ordersWaitForCheckTransactionStatusMap]
         * [对象查看, Trader, Queues!, ordersSuccessFinished]
         * [对象查看, Trader, Queues!, ordersResendFinished]
         */
        public static final String OBJECT_OBSERVER = "[对象查看]";
        public static final String TRADER = "[对象查看, Trader]";
        public static final String QUEUES = "[对象查看, Trader, Queues!]";

        public static final String ORDER_EXECUTOR = "[对象查看, Trader, OrderExecutor]";
        public static final String CHECKER = "[对象查看, Trader, Checker]";
        public static final String ACCOUNT_STATES = "[对象查看, Trader, AccountStates]";
        public static final String FS_TRANSACTION_FETCHER = "[对象查看, Trader, FsTransactionFetcher]";
        public static final String STRATEGY = "[对象查看, Trader, Strategy]";

        public static final String ORDERS_WAIT_FOR_EXECUTION = "[对象查看, Trader, Queues!, ordersWaitForExecution]";
        public static final String ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP = "[对象查看, Trader, Queues!, " +
                "ordersWaitForCheckTransactionStatusMap]";
        public static final String ORDERS_SUCCESS_FINISHED = "[对象查看, Trader, Queues!, ordersSuccessFinished]";
        public static final String ORDERS_RESEND_FINISHED = "[对象查看, Trader, Queues!, ordersResendFinished]";

    }
}

