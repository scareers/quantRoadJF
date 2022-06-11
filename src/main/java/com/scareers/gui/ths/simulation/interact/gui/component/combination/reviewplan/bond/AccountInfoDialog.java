package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.utils.CommonUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

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
    public static int accountInfoPanelHeight = 105; // 左部分宽度!
    public static int accountMoneyPanelHeight = 105; // 左部分宽度!
    public static boolean modalS = true; // 是否模态
    public static long updateSleep = 1000;
    public static Color commonLabelBackGroundColor = new Color(43, 43, 43); // 常态背景色: 黑色
    public static Color commonLabelForeGroundColor = new Color(136, 117, 169); // 常态字颜色: 白色
    public static Color commonLabelForeGroundColor2 = new Color(154, 119, 49); // 常态字颜色: 白色
    public static DecimalFormat decimalFormatForPercent = new DecimalFormat("####0.00%"); // 两位百分比
    public static DecimalFormat decimalFormatForMoney = new DecimalFormat("####0.00"); // 元

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

    private void initAllAccountPanel() {
        allAccountPanel = new JPanel();
    }

    private void initAccountHoldBondPanel() {
        accountHoldBondPanel = new JPanel();
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
        alreadyCommissionTotalValueLabel.setText(formatDouble(decimalFormatForMoney, alreadyCommissionTotal, ""));
        currentTotalProfitPercentValueLabel
                .setText(formatDouble(decimalFormatForPercent, currentTotalProfitPercent, ""));
        // 2.1. 4大非原始属性的 数据计算
        if (totalAssets != null && cash != null) {
            currentMarketValueValueLabel.setText(decimalFormatForMoney.format(totalAssets - cash));
            currentPositionPercentValueLabel
                    .setText(decimalFormatForPercent.format((totalAssets - cash) / totalAssets));
        } else {
            currentMarketValueValueLabel.setText("");
            currentPositionPercentValueLabel.setText("");
        }
        if (totalAssets != null && initMoney != null) {
            currentFloatProfitValueLabel.setText(decimalFormatForMoney.format(totalAssets - initMoney));
        } else {
            currentFloatProfitValueLabel.setText("");
        }
        if (alreadyCommissionTotal != null) { // 两个百分号收尾, 万分之多少
            alreadyCommissionTotalPercentValueLabel
                    .setText(decimalFormatForPercent.format(alreadyCommissionTotal / initMoney * 100) + "%");
        } else {
            currentFloatProfitValueLabel.setText("");
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
