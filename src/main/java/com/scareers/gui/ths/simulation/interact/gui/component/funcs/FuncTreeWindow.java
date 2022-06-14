package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.AccountStatesDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.order.OrderListAndDetailPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept.IndustryConceptPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan.NewsTabPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.stock.StockPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.wencai.WenCaiApiPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.FsFetcherListAndDataPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.FsTransFetcherListAndDataPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.SellStockListAndHsStatePanel;
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
 * description: 功能树器, 树形控件. 显示trader相关属性值状况
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
@Getter
@Setter
public class FuncTreeWindow extends FuncFrameS {
    private static FuncTreeWindow INSTANCE;

    public static FuncTreeWindow getInstance(Type type, String title, TraderGui mainWindow,
                                             FuncButton belongBtn, boolean resizable, boolean closable,
                                             boolean maximizable,
                                             boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                             double preferScale,
                                             int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new FuncTreeWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
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
        INSTANCE.flushBounds();
        INSTANCE.getMainDisplayWindow().flushBounds();
        return INSTANCE;
    }

//    public static FuncTreeWindow getInstance() {
//        return INSTANCE; // 可null
//    }

    private FuncTreeWindow(Type type, String title, TraderGui mainWindow,
                           FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                           boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                           double preferScale,
                           int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);

        FuncTreeWindow temp = this; // 添加监听器. 树形变化时, 刷新主展示窗口size
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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("功能树");

        /*
        1.trader 相关
         */
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


        /*
        2.买入卖出等股票列表
         */

        DefaultMutableTreeNode analyzeNode = new DefaultMutableTreeNode("实时分析");
        DefaultMutableTreeNode stockNode = new DefaultMutableTreeNode("股票");
        // 买入卖出队列
        DefaultMutableTreeNode sellNode = new DefaultMutableTreeNode("卖出队列");
        DefaultMutableTreeNode buyNode = new DefaultMutableTreeNode("买入队列");
        stockNode.add(sellNode);
        stockNode.add(buyNode);
        analyzeNode.add(stockNode);

        /*
        3.复盘报告,与操盘计划
         */
        DefaultMutableTreeNode reviewAndPlanNode = new DefaultMutableTreeNode("复盘操盘");
        DefaultMutableTreeNode tradePlanNode = new DefaultMutableTreeNode("操盘计划");
        // 资讯面
        DefaultMutableTreeNode importantNewsNode2 = new DefaultMutableTreeNode("大势与资讯");
        DefaultMutableTreeNode wencaiApiNode = new DefaultMutableTreeNode("问财接入");
        DefaultMutableTreeNode industryAndConceptNode = new DefaultMutableTreeNode("行业与概念");
        DefaultMutableTreeNode stockPlanNode = new DefaultMutableTreeNode("个股计划");
        DefaultMutableTreeNode bondSimulationNode = new DefaultMutableTreeNode("转债仿真");
        tradePlanNode.add(importantNewsNode2);
        tradePlanNode.add(wencaiApiNode);
        tradePlanNode.add(industryAndConceptNode);
        tradePlanNode.add(stockPlanNode);
        tradePlanNode.add(bondSimulationNode);

        reviewAndPlanNode.add(tradePlanNode);


        root.add(traderNode);
        root.add(analyzeNode);
        root.add(reviewAndPlanNode);


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
                if (e.getNewLeadSelectionPath() != null) {
                    dispatch(e.getNewLeadSelectionPath().toString());
                }
            }
        });
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.SELL_QUEUE);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.ORDER_ALL_MAP);
        //GuiCommonUtil.selectTreeNode(tree, TreePathConstants.PLAN_IMPORTANT_NEWS);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.INDUSTRY_AND_CONCEPT);
//        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.STOCK_PLAN);
        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.BOND_SIMULATION);
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
            // 2.分时成交
        } else if (TreePathConstants.FS_TRANSACTION_FETCHER.equals(treePath)) {
            changeToDisplayFsTransData();
            // 2.2.账户状态
        } else if (TreePathConstants.ACCOUNT_STATES.equals(treePath)) {
            changeToDisplayAccountStates();
            // 卖出队列
        } else if (TreePathConstants.SELL_QUEUE.equals(treePath)) {
            changeToSellQueue();
            // 操盘计划 -- 资讯面
        } else if (TreePathConstants.PLAN_IMPORTANT_NEWS.equals(treePath)) {
            changeToPlanImportantNews();
            // 操盘计划 -- 行业和概念
        } else if (TreePathConstants.PLAN_WENCAI_API.equals(treePath)) {
            changeToWenCaiApiForPlan();
        } else if (TreePathConstants.INDUSTRY_AND_CONCEPT.equals(treePath)) {
            changeToIndustryConceptPanelForPlan();
        } else if (TreePathConstants.STOCK_PLAN.equals(treePath)) {
            changeToStockPlanPanel();
        } else if (TreePathConstants.BOND_SIMULATION.equals(treePath)) {
            changeToBondGlobalSimulationPanel();
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

    private void changeToDisplayFsTransData() {
        FsTransFetcherListAndDataPanel.getInstance(getMainDisplayWindow(), 300) // 此处决定资产列表的宽度
                .showInMainDisplayWindow();
    }

    private void changeToDisplayAccountStates() {
        AccountStatesDisplayPanel.getInstance(getMainDisplayWindow()).showInMainDisplayWindow();
    }

    private void changeToSellQueue() {
        SellStockListAndHsStatePanel
                .getInstance(this.getMainDisplayWindow(), 300)
                .showInMainDisplayWindow();
    }


    private void changeToPlanImportantNews() {
        NewsTabPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToIndustryConceptPanelForPlan() {
        IndustryConceptPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToWenCaiApiForPlan() {
        WenCaiApiPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToStockPlanPanel() {
        StockPanelForPlan
                .getInstance(this.getMainDisplayWindow())
                .showInMainDisplayWindow();
    }

    private void changeToBondGlobalSimulationPanel() {
        BondGlobalSimulationPanel
                .getInstance(this.getMainDisplayWindow(), 380)
                .showInMainDisplayWindow();
        if (TraderGui.INSTANCE != null) { // 切换gui功能状态
            TraderGui.INSTANCE.setFunctionGuiCurrent(TraderGui.FunctionGuiCurrent.BOND_REVISE);
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    ThreadUtil.sleep(1000);
                    TraderGui.INSTANCE.objectsBtn.doClick(); // 关闭侧边栏
                }
            }, true);
        }

        // CTRL+Z 点击


    }


    public static class TreePathConstants { // 路径常量, 字符串配置
        /**
         * [功能树]
         * [功能树, Trader]
         * [功能树, Trader, Queues!]
         * [功能树, Trader, OrderExecutor]
         * [功能树, Trader, Checker]
         * [功能树, Trader, AccountStates]
         * [功能树, Trader, FsTransactionFetcher]
         * [功能树, Trader, FsFetcher]
         * [功能树, Trader, Strategy]
         * [功能树, Trader, Queues!, ordersWaitForExecution]
         * [功能树, Trader, Queues!, ordersAllMap]
         * [功能树, Trader, Queues!, ordersWaitForCheckTransactionStatusMap]
         * [功能树, Trader, Queues!, ordersSuccessFinished]
         * [功能树, Trader, Queues!, ordersResendFinished]
         * [功能树, Trader, Queues!, ordersFailedFinallyNeedManualHandle]
         */
        public static final String OBJECT_OBSERVER = "[功能树]";
        public static final String TRADER = "[功能树, Trader]";
        public static final String QUEUES = "[功能树, Trader, Queues!]";

        public static final String ORDER_EXECUTOR = "[功能树, Trader, OrderExecutor]";
        public static final String CHECKER = "[功能树, Trader, Checker]";
        public static final String ACCOUNT_STATES = "[功能树, Trader, AccountStates]";
        public static final String FS_TRANSACTION_FETCHER = "[功能树, Trader, FsTransactionFetcher]";
        public static final String FS_FETCHER = "[功能树, Trader, FsFetcher]";
        public static final String STRATEGY = "[功能树, Trader, Strategy]";

        public static final String ORDERS_WAIT_FOR_EXECUTION = "[功能树, Trader, Queues!, ordersWaitForExecution]";
        public static final String ORDER_ALL_MAP = "[功能树, Trader, Queues!, ordersAllMap]";
        public static final String ORDERS_WAIT_FOR_CHECK_TRANSACTION_STATUS_MAP = "[功能树, Trader, Queues!, " +
                "ordersWaitForCheckTransactionStatusMap]";
        public static final String ORDERS_SUCCESS_FINISHED = "[功能树, Trader, Queues!, ordersSuccessFinished]";
        public static final String ORDERS_RESEND_FINISHED = "[功能树, Trader, Queues!, ordersResendFinished]";
        public static final String ORDERS_FAILED_FINISHED = "[功能树, Trader, Queues!, " +
                "ordersFailedFinallyNeedManualHandle]";

        /**
         * [实时分析]
         * [实时分析, 股票]
         * [实时分析, 股票, 买入队列]
         * [实时分析, 股票, 卖出队列]
         */
        public static final String REALTIME_ANALYZE = "[功能树, 实时分析]";
        public static final String STOCK_NODE = "[功能树, 实时分析, 股票]";
        public static final String BUY_QUEUE = "[功能树, 实时分析, 股票, 买入队列]";
        public static final String SELL_QUEUE = "[功能树, 实时分析, 股票, 卖出队列]";

        /**
         * [功能树, 复盘操盘]
         * [功能树, 复盘操盘, 操盘计划]
         * [功能树, 复盘操盘, 操盘计划, 大势与资讯]
         * "[功能树, 复盘操盘, 操盘计划, 问财接入]";
         * "[功能树, 复盘操盘, 操盘计划, 行业与概念]";
         * "[功能树, 复盘操盘, 操盘计划, 个股计划]";
         */

        public static final String REVIEW_AND_PLAN = "[功能树, 复盘操盘]";
        public static final String PLAN_IMPORTANT_NEWS = "[功能树, 复盘操盘, 操盘计划, 大势与资讯]";
        public static final String PLAN_WENCAI_API = "[功能树, 复盘操盘, 操盘计划, 问财接入]";
        public static final String INDUSTRY_AND_CONCEPT = "[功能树, 复盘操盘, 操盘计划, 行业与概念]";
        public static final String STOCK_PLAN = "[功能树, 复盘操盘, 操盘计划, 个股计划]";
        public static final String BOND_SIMULATION = "[功能树, 复盘操盘, 操盘计划, 转债仿真]";

        public static final String TRADE_PLAN = "[功能树, 复盘操盘, 操盘计划]";

    }
}

