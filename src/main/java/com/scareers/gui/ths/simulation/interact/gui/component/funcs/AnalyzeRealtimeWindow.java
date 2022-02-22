package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.io.resource.ResourceUtil;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.AccountStatesDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.order.OrderListAndDetailPanel;
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
import java.util.Objects;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.util.ImageScaler.zoomBySize;

/**
 * description: 实时分析
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
@Getter
@Setter
public class AnalyzeRealtimeWindow extends FuncFrameS {
    private static AnalyzeRealtimeWindow INSTANCE;

    public static AnalyzeRealtimeWindow getInstance(Type type, String title, TraderGui mainWindow,
                                                    FuncButton belongBtn, boolean resizable, boolean closable,
                                                    boolean maximizable,
                                                    boolean iconifiable, int autoMaxWidthOrHeight,
                                                    int autoMinWidthOrHeight,
                                                    double preferScale,
                                                    int funcToolsWidthOrHeight, boolean halfWidthOrHeight,
                                                    Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new AnalyzeRealtimeWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
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

//    public static AnalyzeRealtimeWindow getInstance() {
//        Objects.requireNonNull(INSTANCE);
//        return INSTANCE; // 可null
//    }

    private AnalyzeRealtimeWindow(Type type, String title, TraderGui mainWindow,
                                  FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                                  boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                  double preferScale,
                                  int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);

        AnalyzeRealtimeWindow temp = this; // 添加监听器. 树形变化时, 刷新主展示窗口size
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
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("实时分析");

        DefaultMutableTreeNode traderNode = new DefaultMutableTreeNode("股票");

        // 买入卖出队列
        DefaultMutableTreeNode sellNode = new DefaultMutableTreeNode("卖出队列");
        DefaultMutableTreeNode buyNode = new DefaultMutableTreeNode("买入队列");
        traderNode.add(sellNode);
        traderNode.add(buyNode);

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
        GuiCommonUtil.selectTreeNode(tree, TreePathConstants.SELL_QUEUE);
        return tree;
    }


    public void dispatch(String treePath) {
        // 1. 5类队列
        if (TreePathConstants.SELL_QUEUE.equals(treePath)) {
            changeToSellQueue();
        } else {
            System.out.println(treePath);
        }
    }

    private void changeToSellQueue() {
        SellStockListAndHsStatePanel
                .getInstance(this.getMainDisplayWindow(), 300)
                .showInMainDisplayWindow();
    }


    public static class TreePathConstants { // 路径常量, 字符串配置
        /**
         * [实时分析]
         * [实时分析, 股票]
         * [实时分析, 股票, 买入队列]
         * [实时分析, 股票, 卖出队列]
         */
        public static final String REALTIME_ANALYZE = "[实时分析]";
        public static final String STOCK_NODE = "[实时分析, 股票]";
        public static final String BUY_QUEUE = "[实时分析, 股票, 买入队列]";
        public static final String SELL_QUEUE = "[实时分析, 股票, 卖出队列]";


    }
}

