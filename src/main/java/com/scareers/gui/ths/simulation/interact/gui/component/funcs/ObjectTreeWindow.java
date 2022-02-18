package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.FsFetcherListAndDataPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.order.OrderListAndDetailPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.TreeCellRendererS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

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

        ObjectTreeWindow temp = this; // 添加监听器. 树形变化时, 刷新主展示窗口size
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                temp.getMainDisplayWindow()
                        .setBounds(temp.getWidth(), 0, temp.getMainPane().getWidth() - temp.getWidth(),
                                temp.getMainPane().getHeight());
            }
        });
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
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_MINOR, COLOR_SCROLL_BAR_THUMB); // 替换自定义barUi

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
        DefaultMutableTreeNode fsFetcherNode = new DefaultMutableTreeNode("FsFetcher");
        DefaultMutableTreeNode strategyNode = new DefaultMutableTreeNode("Strategy");

        // 4大队列/map
        DefaultMutableTreeNode queues = new DefaultMutableTreeNode("Queues!"); // 父包含
        DefaultMutableTreeNode ordersWaitForExecution = new DefaultMutableTreeNode("ordersWaitForExecution");
        DefaultMutableTreeNode ordersWaitForCheckTransactionStatusMap = new DefaultMutableTreeNode(
                "ordersWaitForCheckTransactionStatusMap");
        DefaultMutableTreeNode ordersSuccessFinished = new DefaultMutableTreeNode("ordersSuccessFinished");
        DefaultMutableTreeNode ordersResendFinished = new DefaultMutableTreeNode("ordersResendFinished");
        DefaultMutableTreeNode ordersFailedFinished = new DefaultMutableTreeNode("ordersFailedFinallyNeedManualHandle");
        DefaultMutableTreeNode ordersAllMap = new DefaultMutableTreeNode("ordersAllMap");
        queues.add(ordersWaitForExecution);
        queues.add(ordersWaitForCheckTransactionStatusMap);
        queues.add(ordersSuccessFinished);
        queues.add(ordersResendFinished);
        queues.add(ordersFailedFinished);
        queues.add(ordersAllMap);

        traderNode.add(queues);
        traderNode.add(orderExecutorNode);
        traderNode.add(checkerNode);
        traderNode.add(accountStatesNode);
        traderNode.add(fsTransactionFetcherNode);
        traderNode.add(fsFetcherNode);
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


    public void dispatch(String treePath) {
        // 1. 5类队列
        if (TreePathConstants.ORDERS_WAIT_FOR_EXECUTION.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_WAIT_FOR_EXECUTION);
        } else if (TreePathConstants.ORDER_ALL_MAP.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDER_ALL_MAP);
        } else if (TreePathConstants.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP);
        } else if (TreePathConstants.ORDERS_SUCCESS_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_SUCCESS_FINISHED);
        } else if (TreePathConstants.ORDERS_FAILED_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_FAILED_FINISHED);
        } else if (TreePathConstants.ORDERS_RESEND_FINISHED.equals(treePath)) {
            changeToDisplayOrderList(OrderListAndDetailPanel.Type.ORDERS_RESEND_FINISHED);
            // 2.1分钟分时图
        } else if (TreePathConstants.FS_FETCHER.equals(treePath)) {
            changeToDisplayFs1MData();
            // 2.2.账户状态
        } else if (TreePathConstants.FS_TRANSACTION_FETCHER.equals(treePath)) {
            changeToDisplayFs1MData();
            // 2.2.账户状态
        } else {
            System.out.println(treePath);
        }
    }

    private void changeToDisplayOrderList(OrderListAndDetailPanel.Type type) {
        OrderListAndDetailPanel
                .getInstance(this.getMainDisplayWindow())
                .changeType(type)
                .showInMainDisplayWindow();
    }

    private void changeToDisplayFs1MData() {
        FsFetcherListAndDataPanel.getInstance(getMainDisplayWindow(), 300) // 此处决定资产列表的宽度
                .showInMainDisplayWindow();
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
         * [对象查看, Trader, FsFetcher]
         * [对象查看, Trader, Strategy]
         * [对象查看, Trader, Queues!, ordersWaitForExecution]
         * [对象查看, Trader, Queues!, ordersAllMap]
         * [对象查看, Trader, Queues!, ordersWaitForCheckTransactionStatusMap]
         * [对象查看, Trader, Queues!, ordersSuccessFinished]
         * [对象查看, Trader, Queues!, ordersResendFinished]
         * [对象查看, Trader, Queues!, ordersFailedFinallyNeedManualHandle]
         */
        public static final String OBJECT_OBSERVER = "[对象查看]";
        public static final String TRADER = "[对象查看, Trader]";
        public static final String QUEUES = "[对象查看, Trader, Queues!]";

        public static final String ORDER_EXECUTOR = "[对象查看, Trader, OrderExecutor]";
        public static final String CHECKER = "[对象查看, Trader, Checker]";
        public static final String ACCOUNT_STATES = "[对象查看, Trader, AccountStates]";
        public static final String FS_TRANSACTION_FETCHER = "[对象查看, Trader, FsTransactionFetcher]";
        public static final String FS_FETCHER = "[对象查看, Trader, FsFetcher]";
        public static final String STRATEGY = "[对象查看, Trader, Strategy]";

        public static final String ORDERS_WAIT_FOR_EXECUTION = "[对象查看, Trader, Queues!, ordersWaitForExecution]";
        public static final String ORDER_ALL_MAP = "[对象查看, Trader, Queues!, ordersAllMap]";
        public static final String ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP = "[对象查看, Trader, Queues!, " +
                "ordersWaitForCheckTransactionStatusMap]";
        public static final String ORDERS_SUCCESS_FINISHED = "[对象查看, Trader, Queues!, ordersSuccessFinished]";
        public static final String ORDERS_RESEND_FINISHED = "[对象查看, Trader, Queues!, ordersResendFinished]";
        public static final String ORDERS_FAILED_FINISHED = "[对象查看, Trader, Queues!, " +
                "ordersFailedFinallyNeedManualHandle]";

    }
}

