package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;

import javax.persistence.Column;
import javax.persistence.Transient;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.*;

/**
 * description: 账户信息对话框.
 * 1.将显示当前账户基本信息
 * 2.将显示当前账户资金信息
 * 3.显示数据库中, 全部账户的列表(基本信息, 表格显示), 排除当前账户
 * 4.表格, 显示 账户包含的所有订单信息!
 * 5.复选框, 勾选情况下, 订单列表表格, 锁定显示当前账户订单列表, 非选择状态下, 切换账户列表, 则订单列表变更为当前选中账户的!
 *
 * @author: admin
 * @date: 2022/6/11/011-10:24:40
 */
@Getter
@Setter
public class AccountInfoDialog extends JDialog {
    public static double scale = 0.7; // 对话框, 默认为全屏幕 宽高 的部分; 将居中;
    public static int leftPanelWidth = 710; // 左部分宽度!
    public static int accountHoldBondPanelHeight = 700;
    public static int accountInfoPanelHeight = 105; // 左部分宽度!
    public static int accountMoneyPanelHeight = 105; // 左部分宽度!
    public static boolean modalS = true; // 是否模态
    public static long updateSleep = 1000;
    public static Color commonLabelBackGroundColor = new Color(43, 43, 43); // 常态背景色: 黑色
    public static Color commonLabelForeGroundColor = new Color(136, 117, 169); // 常态字颜色: 白色
    public static Color commonLabelForeGroundColor2 = new Color(154, 119, 49); // 常态字颜色: 白色
    public static DecimalFormat decimalFormatForPercent = new DecimalFormat("####0.00%"); // 两位百分比
    public static DecimalFormat decimalFormatForMoney = new DecimalFormat("####0.00"); // 元
    public static List<String> bondTableColNames = Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比",
            "总计盈亏", "市值", "仓位"); // 转债持仓表格列

    private static AccountInfoDialog INSTANCE;

    public static AccountInfoDialog getInstance(Frame owner, String title, boolean modal,
                                                BondGlobalSimulationPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new AccountInfoDialog(owner, title, modal, parentS);

            // 首次, 也将启动死循环 update(), 更新显示
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        // sleep给一定随机性
                        INSTANCE.update();
                        ThreadUtil.sleep(RandomUtil.randomInt((int) (updateSleep * 0.7), (int) (updateSleep * 1.3)));
                    }
                }
            }, true);
        }
        return INSTANCE;
    }

    BondGlobalSimulationPanel parentS;
    JPanel contentPanelS;

    private AccountInfoDialog(Frame owner, String title, boolean modal, BondGlobalSimulationPanel parentS) {
        super(owner, title, modal);
        this.parentS = parentS;
        this.setResizable(true);

        this.setSize((int) (TraderGui.screenW * scale), (int) (TraderGui.screenH * scale));
        this.setLocationRelativeTo(TraderGui.INSTANCE);
        initContentPanelS(); // 主面板
        this.setContentPane(contentPanelS);
        this.setLocationRelativeTo(TraderGui.INSTANCE);

        GuiCommonUtil.addEscNotVisibleCallbackToJDialog(this);
    }

    JPanel accountInfoPanel; // 左上
    JPanel accountMoneyPanel; // 左中
    JPanel allAccountPanel; // 左下
    JPanel accountHoldBondPanel; // 右上
    JPanel allOrderPanel; // 右下;

    /**
     * 布局: 左上: 账户基本信息, 左中: 账户资金信息 左下: 所有账户表格列表;
     * 右中:当前账户当前持仓状况表格; 右下: 账户订单表格列表!
     */
    public void initContentPanelS() {
        contentPanelS = new JPanel();
        contentPanelS.setLayout(new BorderLayout());

        // 1.左: 高度 1,1,2
        JPanel panelLeft = new JPanel();
        panelLeft.setPreferredSize(new Dimension(leftPanelWidth, 4096));
        panelLeft.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, -1, -1));

        initAccountInfoPanel();
        initAccountMoneyPanel();
        initAllAccountPanel();
        panelLeft.add(accountInfoPanel);
        panelLeft.add(accountMoneyPanel);
        panelLeft.add(allAccountPanel);
        panelLeft.setBorder(BorderFactory.createLineBorder(Color.black, 1));

        // 2.右: 高度平分
        JPanel panelRight = new JPanel();
        panelRight.setLayout(new GridLayout(2, 1, -1, -1));

        initAccountHoldBondPanel();
        initAllOrderPanel();

        panelRight.add(accountHoldBondPanel);
        panelRight.add(allOrderPanel);

        // 3.组装左右
        contentPanelS.add(panelLeft, BorderLayout.WEST);
        contentPanelS.add(panelRight, BorderLayout.CENTER);
    }

    // 6 *2 == 12
    JLabel startRealTimeLabel = getCommonLabel("开始真实时间");
    JLabel startRealTimeValueLabel = getCommonLabel();
    JLabel stopRealTimeLabel = getCommonLabel("结束真实时间");
    JLabel stopRealTimeValueLabel = getCommonLabel();
    JLabel accountNameRemarkLabel = getCommonLabel("备注信息");
    JLabel accountNameRemarkValueLabel = getCommonLabel();
    JLabel reviseStartDateTimeStrLabel = getCommonLabel("复盘日期时间");
    JLabel reviseStartDateTimeStrValueLabel = getCommonLabel();
    JLabel reviseStopTimeStrLabel = getCommonLabel("复盘结束时间");
    JLabel reviseStopTimeStrValueLabel = getCommonLabel();
    JLabel innerObjectTypeLabel = getCommonLabel("内部状态");
    JLabel innerObjectTypeValueLabel = getCommonLabel();

    /**
     * 6 * 2 布局
     */
    private void initAccountInfoPanel() {
        accountInfoPanel = new JPanel();
        accountInfoPanel.setLayout(new GridLayout(3, 4, -1, 2));
        accountInfoPanel.setPreferredSize(new Dimension(leftPanelWidth, accountInfoPanelHeight));
        accountInfoPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1, true));

        accountInfoPanel.add(startRealTimeLabel);
        accountInfoPanel.add(startRealTimeValueLabel);
        startRealTimeValueLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        startRealTimeValueLabel.setForeground(Color.red);
        accountInfoPanel.add(reviseStartDateTimeStrLabel);
        accountInfoPanel.add(reviseStartDateTimeStrValueLabel);
        reviseStartDateTimeStrValueLabel.setFont(new Font("宋体", Font.PLAIN, 15));
        reviseStartDateTimeStrValueLabel.setForeground(Color.red);
        accountInfoPanel.add(stopRealTimeLabel);
        accountInfoPanel.add(stopRealTimeValueLabel);
        accountInfoPanel.add(reviseStopTimeStrLabel);
        accountInfoPanel.add(reviseStopTimeStrValueLabel);
        accountInfoPanel.add(accountNameRemarkLabel);
        accountInfoPanel.add(accountNameRemarkValueLabel);
        accountInfoPanel.add(innerObjectTypeLabel);
        accountInfoPanel.add(innerObjectTypeValueLabel);
    }


    // 9*2 == 18
    JLabel initMoneyLabel = getCommonLabel("初始总资金", commonLabelForeGroundColor2);
    JLabel initMoneyValueLabel = getCommonLabelRightAlign();
    JLabel cashLabel = getCommonLabel("当前现金", commonLabelForeGroundColor2);
    JLabel cashValueLabel = getCommonLabelRightAlign();
    JLabel totalAssetsLabel = getCommonLabel("当前总资产", commonLabelForeGroundColor2);
    JLabel totalAssetsValueLabel = getCommonLabelRightAlign();
    JLabel currentMarketValueLabel = getCommonLabel("当前总市值", commonLabelForeGroundColor2);
    JLabel currentMarketValueValueLabel = getCommonLabelRightAlign(); // @key
    JLabel currentFloatProfitLabel = getCommonLabel("总计浮盈亏", commonLabelForeGroundColor2);
    JLabel currentFloatProfitValueLabel = getCommonLabelRightAlign(); // @key
    JLabel alreadyCommissionTotalLabel = getCommonLabel("总计手续费", commonLabelForeGroundColor2);
    JLabel alreadyCommissionTotalValueLabel = getCommonLabelRightAlign();
    JLabel currentPositionPercentLabel = getCommonLabel("当前总仓位", commonLabelForeGroundColor2);
    JLabel currentPositionPercentValueLabel = getCommonLabelRightAlign(); // @key
    JLabel currentTotalProfitPercentLabel = getCommonLabel("利润百分比", commonLabelForeGroundColor2);
    JLabel currentTotalProfitPercentValueLabel = getCommonLabelRightAlign();//
    JLabel alreadyCommissionTotalPercentLabel = getCommonLabel("手续费百分比", commonLabelForeGroundColor2);
    JLabel alreadyCommissionTotalPercentValueLabel = getCommonLabelRightAlign();// @key

    /**
     * 6 * 3 布局
     */
    private void initAccountMoneyPanel() {
        accountMoneyPanel = new JPanel();
        accountMoneyPanel.setLayout(new GridLayout(3, 6, 10, 2));
        accountMoneyPanel.setPreferredSize(new Dimension(leftPanelWidth, accountMoneyPanelHeight));
        accountMoneyPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1, true));

        accountMoneyPanel.add(totalAssetsLabel);
        accountMoneyPanel.add(totalAssetsValueLabel);
        accountMoneyPanel.add(cashLabel);
        accountMoneyPanel.add(cashValueLabel);
        accountMoneyPanel.add(initMoneyLabel);
        accountMoneyPanel.add(initMoneyValueLabel);
        accountMoneyPanel.add(currentMarketValueLabel);
        accountMoneyPanel.add(currentMarketValueValueLabel);
        accountMoneyPanel.add(currentFloatProfitLabel);
        accountMoneyPanel.add(currentFloatProfitValueLabel);
        accountMoneyPanel.add(alreadyCommissionTotalLabel);
        accountMoneyPanel.add(alreadyCommissionTotalValueLabel);
        accountMoneyPanel.add(currentPositionPercentLabel);
        accountMoneyPanel.add(currentPositionPercentValueLabel);
        accountMoneyPanel.add(currentTotalProfitPercentLabel);
        accountMoneyPanel.add(currentTotalProfitPercentValueLabel);
        accountMoneyPanel.add(alreadyCommissionTotalPercentLabel);
        accountMoneyPanel.add(alreadyCommissionTotalPercentValueLabel);

    }


    /**
     * 使用 表格, 展示当前持仓情况; 主要6大map
     * 另外需要手动计算: 市值, 单债仓位, 转债名称, 浮动市值, 当前单债仓位
     * 表格列依次: 转债代码,转债名称, 当前数量, 成本价, 最新价, 已实现盈利元, 当前持仓盈亏百分比, 总计浮动盈亏, 浮动市值, 当前单债仓位
     * 共计 10 列
     */
    private void initAccountHoldBondPanel() {
        accountHoldBondPanel = new JPanel();
        accountHoldBondPanel.setPreferredSize(new Dimension(4096, accountHoldBondPanelHeight));
        accountHoldBondPanel.setLayout(new BorderLayout());

        initSecurityEmJXTable(); // 持仓表格
        accountHoldBondPanel.add(jScrollPaneForHoldBonds, BorderLayout.CENTER); // 滚动组件加入
    }

    JScrollPane jScrollPaneForHoldBonds;

    private void initJTableWrappedJScrollPane() {
        jScrollPaneForHoldBonds = new JScrollPane();
        jScrollPaneForHoldBonds.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForHoldBonds.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForHoldBonds.setViewportView(jXTableForHoldBonds); // 滚动包裹转债列表
        jScrollPaneForHoldBonds.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForHoldBonds, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }

    protected volatile JXTable jXTableForHoldBonds; //  转债展示列表控件

    DefaultTableModel holdBondsModel;

    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private void initSecurityEmJXTable() {
        // 1.构造model
        // Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比", "总计盈亏", "市值", "仓位");
        Vector<Vector<Object>> datas = new Vector<>(); // 空数据, 仅提供列信息
        Vector<Object> cols = new Vector<>(bondTableColNames);
        holdBondsModel = new DefaultTableModel(datas, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不可编辑!
            }

            @Override
            public Class getColumnClass(int column) { // 返回列值得类型, 使得能够按照数据排序, 否则默认按照字符串排序
                if (column == 0 || column == 1) { // 名称代码
                    return String.class;
                } else if (column == 2) { // 涨跌幅成交额
                    return Integer.class;
                } else { // 索引列 3 - 9 都是Double
                    return Double.class;
                }
            }
        };

        // 2.实例化table, 设置model
        jXTableForHoldBonds = new JXTable();
        // Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比", "总计盈亏", "市值", "仓位");
        jXTableForHoldBonds.setSortOrder("涨跌幅", SortOrder.DESCENDING);
        jXTableForHoldBonds.setModel(holdBondsModel);
        removeEnterKeyDefaultAction(jXTableForHoldBonds); // 按下enter不下移, 默认行为
        addTableHighlighters(); // 高亮
        jXTableForHoldBonds.setSortOrder(6, SortOrder.DESCENDING); // 默认当前持仓部分, 盈利百分比倒序
        jXTableForHoldBonds.setAutoCreateRowSorter(true);

        // 3.切换选择的回调绑定
        // this.fitTableColumns(jXTableForBonds); // 默认平均
        initJTableStyle();
        setTableColCellRenders(); // 设置显示render
        jXTableForHoldBonds.setGridColor(Color.black); // 不显示网格
        jXTableForHoldBonds.setSelectionBackground(new Color(210, 210, 210)); // 同花顺
    }

    /**
     * 设置表格各列的 cellRender; 主要的作用是设置 文本格式!
     */
    private void setTableColCellRenders() {
        Color selectedBack = new Color(210, 210, 210);
        // Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比", "总计盈亏", "市值", "仓位");
        jXTableForHoldBonds.getColumn(0).setCellRenderer(new TableCellRendererForBondTable(selectedBack));
        jXTableForHoldBonds.getColumn(1).setCellRenderer(new TableCellRendererForBondTable(selectedBack));
        jXTableForHoldBonds.getColumn(2).setCellRenderer(new TableCellRendererForBondTable(selectedBack));
        jXTableForHoldBonds.getColumn(3).setCellRenderer(new TableCellRendererForBondTable(selectedBack));
        jXTableForHoldBonds.getColumn(4).setCellRenderer(new TableCellRendererForBondTable(selectedBack));
        jXTableForHoldBonds.getColumn(5).setCellRenderer(new TableCellRendererForBondTableFor2Scale(selectedBack));
        jXTableForHoldBonds.getColumn(6).setCellRenderer(new TableCellRendererForBondTableForPercent(selectedBack));
        jXTableForHoldBonds.getColumn(7).setCellRenderer(new TableCellRendererForBondTableFor2Scale(selectedBack));
        jXTableForHoldBonds.getColumn(8).setCellRenderer(new TableCellRendererForBondTableForBigNumber(selectedBack));
        jXTableForHoldBonds.getColumn(9).setCellRenderer(new TableCellRendererForBondTableForPercent(selectedBack));
    }

    private void initJTableStyle() {
        // 1.表头框颜色和背景色
        jXTableForHoldBonds.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(Color.white);
        TableColumnModel columnModel = jXTableForHoldBonds.getTableHeader().getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            //i是表头的列
            TableColumn column = jXTableForHoldBonds.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //表头文字居中
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        // 2.表格自身文字颜色和背景色
        jXTableForHoldBonds.setBackground(Color.black);
        jXTableForHoldBonds.setForeground(Color.white);

        // 4. 单行高 和字体
        jXTableForHoldBonds.setRowHeight(30);
        jXTableForHoldBonds.setFont(new Font("微软雅黑", Font.PLAIN, 18));

        // 5.单选,大小,无边框, 加放入滚动
        jXTableForHoldBonds.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        // jXTableForBonds.setPreferredSize(new Dimension(jListWidth, 10000));
        jXTableForHoldBonds.setBorder(null);
        initJTableWrappedJScrollPane(); // 表格被包裹
    }

    public static Color commonForeColor = Color.white; // 普通的字颜色, 转债代码和名称 使用白色
    public static Color amountForeColor = new Color(2, 226, 224); // 文字颜色 : 成交额
    public static Color upForeColor = new Color(255, 50, 50); // 向上的红色 : 涨跌幅
    public static Color downForeColor = new Color(0, 230, 0); // 向下的绿色: 涨跌幅
    public static Color selectedBackColor = new Color(64, 0, 128); // 选中时背景色
    public static Color holdBondForeColor = new Color(206, 14, 95); // 持仓的文字颜色: 代码和名称!


    private void addTableHighlighters() {
        // Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比", "总计盈亏", "市值", "仓位");

        // 1.总计盈亏 列, 与 0比较大小, 显示 红色 绿色!
        SingleColGt0HighLighterPredicate singleColGt0HighLighterPredicate = new SingleColGt0HighLighterPredicate(7);
        ColorHighlighter colorHighlighter1 = new ColorHighlighter(singleColGt0HighLighterPredicate, null,
                upForeColor, null, upForeColor);
        SingleColLt0HighLighterPredicate singleColLt0HighLighterPredicate = new SingleColLt0HighLighterPredicate(7);
        ColorHighlighter colorHighlighter2 = new ColorHighlighter(singleColLt0HighLighterPredicate, null,
                downForeColor, null, downForeColor);

        jXTableForHoldBonds.setHighlighters(
                colorHighlighter1,
                colorHighlighter2
        );

    }


    private void initAllAccountPanel() {
        allAccountPanel = new JPanel();
    }


    private void initAllOrderPanel() {
        allOrderPanel = new JPanel();
    }

    /**
     * 更新 账户信息显示, 主要更新 当前账户信息!
     */
    public void update() {
        if (this.parentS == null) {
            return;
        }
        ReviseAccountWithOrder account = this.parentS.getAccount();
        if (account == null) {
            return;
        }

        this.setTitle("账户: " + account.getStartRealTime() + " --> " + account.getAccountNameRemark());

        // @key: 变量声明, 在同步块中, 只获取这些数据, 同步块外, 再统一设置, 同样能保证数据截面一致性! 且不会因同步块中设置gui文本拖慢性能
        // 1. 账户基本信息
        String startRealTime = null;
        String stopRealTime = null;
        String accountNameRemark = null;
        String reviseStartDateTimeStr = null;
        String reviseStopTimeStr = null;
        String innerObjectType = null;

        // 2.账户资金信息
        Double initMoney = null;
        Double cash = null;
        Double totalAssets = null;
        Double alreadyCommissionTotal = null;
        Double currentTotalProfitPercent = null;


        synchronized (BondGlobalSimulationPanel.accountLock) {
            // 1.
            startRealTime = account.getStartRealTime();
            stopRealTime = account.getStopRealTime();
            accountNameRemark = account.getAccountNameRemark();
            reviseStartDateTimeStr = account.getReviseStartDateTimeStr();
            reviseStopTimeStr = account.getReviseStopTimeStr();
            innerObjectType = account.getInnerObjectType();

            // 2.
            initMoney = account.getInitMoney();
            cash = account.getCash();
            totalAssets = account.getTotalAssets();
            currentTotalProfitPercent = account.getCurrentTotalProfitPercent();
            alreadyCommissionTotal = account.getAlreadyCommissionTotal();
        }

        // 1.
        startRealTimeValueLabel.setText(CommonUtil.toStringCheckNull(startRealTime, ""));
        stopRealTimeValueLabel.setText(CommonUtil.toStringCheckNull(stopRealTime, ""));
        accountNameRemarkValueLabel.setText(CommonUtil.toStringCheckNull(accountNameRemark, ""));
        reviseStartDateTimeStrValueLabel.setText(CommonUtil.toStringCheckNull(reviseStartDateTimeStr, ""));
        reviseStopTimeStrValueLabel.setText(CommonUtil.toStringCheckNull(reviseStopTimeStr, ""));
        innerObjectTypeValueLabel.setText(CommonUtil.toStringCheckNull(innerObjectType, ""));


        // 2.更新账户资金信息
        initMoneyValueLabel.setText(formatDouble(decimalFormatForMoney, initMoney, ""));
        cashValueLabel.setText(formatDouble(decimalFormatForMoney, cash, ""));
        totalAssetsValueLabel.setText(formatDouble(decimalFormatForMoney, totalAssets, ""));
        if (totalAssets != null && initMoney != null) {
            if (totalAssets > initMoney) {
                totalAssetsValueLabel.setForeground(Color.red);
            } else if (totalAssets < initMoney) {
                totalAssetsValueLabel.setForeground(Color.green);
            } else {
                totalAssetsValueLabel.setForeground(Color.white);
            }
        }
        alreadyCommissionTotalValueLabel.setText(formatDouble(decimalFormatForMoney, alreadyCommissionTotal, ""));
        currentTotalProfitPercentValueLabel
                .setText(formatDouble(decimalFormatForPercent, currentTotalProfitPercent, ""));
        if (currentTotalProfitPercent != null) {
            if (currentTotalProfitPercent > 0) {
                currentTotalProfitPercentValueLabel.setForeground(Color.red);
            } else if (currentTotalProfitPercent < 0) {
                currentTotalProfitPercentValueLabel.setForeground(Color.green);
            } else {
                currentTotalProfitPercentValueLabel.setForeground(Color.white);
            }
        }
        // 2.1. 4大非原始属性的 数据计算
        if (totalAssets != null && cash != null) {
            currentMarketValueValueLabel.setText(decimalFormatForMoney.format(totalAssets - cash));
            double positionPercent = (totalAssets - cash) / totalAssets;
            currentPositionPercentValueLabel
                    .setText(decimalFormatForPercent.format(positionPercent));
            if (positionPercent < 0.2) {
                currentPositionPercentValueLabel.setForeground(Color.white); // 仓位轻到重: 白黄橙红
            } else if (positionPercent < 0.5) {
                currentPositionPercentValueLabel.setForeground(Color.yellow);
            } else if (positionPercent < 0.8) {
                currentPositionPercentValueLabel.setForeground(Color.orange);
            } else {
                currentPositionPercentValueLabel.setForeground(Color.red);
            }
        } else {
            currentMarketValueValueLabel.setText("");
            currentPositionPercentValueLabel.setText("");
        }
        if (totalAssets != null && initMoney != null) {
            double profit = totalAssets - initMoney;
            currentFloatProfitValueLabel.setText(decimalFormatForMoney.format(profit));
            if (profit > 0) {
                currentFloatProfitValueLabel.setForeground(Color.red);
            } else if (profit < 0) {
                currentFloatProfitValueLabel.setForeground(Color.green);
            } else {
                currentFloatProfitValueLabel.setForeground(Color.white);
            }
        } else {
            currentFloatProfitValueLabel.setText("");
        }
        if (alreadyCommissionTotal != null) { // 两个百分号收尾, 万分之多少
            alreadyCommissionTotalPercentValueLabel
                    .setText(decimalFormatForPercent.format(alreadyCommissionTotal / initMoney * 100) + "%");
            if (alreadyCommissionTotal / initMoney >= 0.01) {
                alreadyCommissionTotalPercentValueLabel.setForeground(Color.red); // 手续费达到初始资金1%以上, 红色
            }
        } else {
            currentFloatProfitValueLabel.setText("");
        }

        // 尝试刷新持仓转债表格
        tryFlushJxTableForHoldBonds(account);
    }

    private void tryFlushJxTableForHoldBonds(ReviseAccountWithOrder account) {
        // Arrays.asList("代码", "名称", "数量", "成本价", "最新价", "已确定盈利", "持仓盈利百分比", "总计盈亏", "市值", "仓位");
        // 0.排序保存
        int sortedColumnIndex = jXTableForHoldBonds.getSortedColumnIndex(); // 唯一主要的排序列!
        SortOrder sortOrder = null;
        if (sortedColumnIndex != -1) {
            sortOrder = jXTableForHoldBonds.getSortOrder(sortedColumnIndex); // 什么顺序
        }


        // 1.访问全部转债
        HashSet<String> allBonds = new HashSet<>(account.getHoldBondsAmountMap().keySet());
        allBonds.addAll(account.getBondCostPriceMap().keySet());
        allBonds.addAll(account.getHoldBondsCurrentPriceMap().keySet());
        allBonds.addAll(account.getBondAlreadyProfitMap().keySet());
        allBonds.addAll(account.getHoldBondsGainPercentMap().keySet());
        allBonds.addAll(account.getHoldBondsTotalProfitMap().keySet());

        // 2.构造结果数组
        Vector<Vector> datas = new Vector<>();
        for (String bond : allBonds) {
            String name = null;
            try {
                name = SecurityBeanEm.createBond(bond).getName();
            } catch (Exception e) {

            }

            Integer amount = account.getHoldBondsAmountMap().getOrDefault(bond, 0);
            Double cost = account.getBondCostPriceMap().getOrDefault(bond, 0.0);
            Double currentPrice = account.getHoldBondsCurrentPriceMap().getOrDefault(bond, 0.0);
            Double alreadyProfit = account.getBondAlreadyProfitMap().getOrDefault(bond, 0.0);
            Double holdGainPercent = account.getHoldBondsGainPercentMap().getOrDefault(bond, 0.0);
            Double totalProfit = account.getHoldBondsTotalProfitMap().getOrDefault(bond, 0.0);
            Double marketValue = amount * currentPrice;
            Double position = marketValue / account.getTotalAssets();

            List<Object> row = Arrays
                    .asList(bond, name, amount, cost, currentPrice, alreadyProfit, holdGainPercent, totalProfit,
                            marketValue,
                            position);
            datas.add(new Vector(row));
        }
        // 3.新行数
        holdBondsModel.setRowCount(datas.size());
        // 4.遍历赋值
        for (int i = 0; i < datas.size(); i++) {
            for (int j = 0; j < holdBondsModel.getColumnCount(); j++) {
                holdBondsModel.setValueAt(datas.get(i).get(j), i, j);
            }
        }


        // 5.排序恢复!
        if (sortedColumnIndex != -1) {
            jXTableForHoldBonds.setSortOrder(sortedColumnIndex, sortOrder);
        }

    }

    /**
     * 对数字应用格式, 返回字符串, 如果null, 返回给定默认字符串
     *
     * @param decimalFormat
     * @param value
     * @param defaultStr
     * @return
     */
    public static String formatDouble(DecimalFormat decimalFormat, Double value, String defaultStr) {
        if (value == null) {
            return defaultStr;
        }
        return decimalFormat.format(value);
    }

    public static JLabel getCommonLabel(String text, Color foreColor, boolean leftAlignment) {
        JLabel jLabel = new JLabel(text);
        jLabel.setForeground(foreColor);
        if (leftAlignment) {
            jLabel.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        jLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));
        jLabel.setBackground(commonLabelBackGroundColor);
        return jLabel;
    }

    public static JLabel getCommonLabel(String text, Color foreColor) {
        return getCommonLabel(text, foreColor, true);
    }

    public static JLabel getCommonLabelRightAlign(String text, Color foreColor) {
        return getCommonLabel(text, foreColor, false);
    }

    public static JLabel getCommonLabel(String text) {
        return getCommonLabel(text, commonLabelForeGroundColor);
    }

    public static JLabel getCommonLabelRightAlign(String text) {
        return getCommonLabelRightAlign(text, commonLabelForeGroundColor);
    }

    public static JLabel getCommonLabel() {
        return getCommonLabel("");
    }

    public static JLabel getCommonLabelRightAlign() {
        return getCommonLabelRightAlign("");
    }
}
