package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.swing.*;
import java.awt.*;

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
    public static double scale = 0.8; // 对话框, 默认为全屏幕 宽高 的部分; 将居中;
    public static int leftPanelWidth = 350; // 左部分宽度!
    public static boolean modalS = true; // 是否模态
    public static Color commonLabelBackGroundColor = Color.black; // 常态背景色: 黑色
    public static Color commonLabelForeGroundColor = Color.white; // 常态字颜色: 白色


    BondGlobalSimulationPanel parentS;
    ReviseAccountWithOrder account;
    JPanel contentPanelS;

    public AccountInfoDialog(Frame owner, String title, boolean modal, BondGlobalSimulationPanel parentS,
                             ReviseAccountWithOrder account) {
        super(owner, title, modal);
        this.parentS = parentS;
        this.account = account;
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
        panelLeft.setLayout(new GridLayout(2, 1, -1, -1));
        JPanel panelLeftTop = new JPanel();
        panelLeftTop.setLayout(new GridLayout(2, 1, -1, -1));

        initAccountInfoPanel();
        initAccountMoneyPanel();
        panelLeftTop.add(accountInfoPanel);
        panelLeftTop.add(accountMoneyPanel);

        initAllAccountPanel();
        panelLeft.add(panelLeftTop);
        panelLeft.add(allAccountPanel);

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
    JLabel startRealTimeValueLabel = getCommonLabelRightAlign();
    JLabel stopRealTimeLabel = getCommonLabel("结束真实时间");
    JLabel stopRealTimeValueLabel = getCommonLabelRightAlign();
    JLabel accountNameRemarkLabel = getCommonLabel("备注信息");
    JLabel accountNameRemarkValueLabel = getCommonLabelRightAlign();
    JLabel reviseStartDateTimeStrLabel = getCommonLabel("复盘日期时间");
    JLabel reviseStartDateTimeStrValueLabel = getCommonLabelRightAlign();
    JLabel reviseStopTimeStrLabel = getCommonLabel("复盘结束时间");
    JLabel reviseStopTimeStrValueLabel = getCommonLabelRightAlign();
    JLabel innerObjectTypeLabel = getCommonLabel("内部状态");
    JLabel innerObjectTypeValueLabel = getCommonLabelRightAlign();

    /**
     * 6 * 2 布局
     */
    private void initAccountInfoPanel() {
        accountInfoPanel = new JPanel();
        accountInfoPanel.setLayout(new GridLayout(2, 6, 0, 0));

        accountInfoPanel.add(startRealTimeLabel);
        accountInfoPanel.add(startRealTimeValueLabel);
        accountInfoPanel.add(stopRealTimeLabel);
        accountInfoPanel.add(stopRealTimeValueLabel);
        accountInfoPanel.add(accountNameRemarkLabel);
        accountInfoPanel.add(accountNameRemarkValueLabel);
        accountInfoPanel.add(reviseStartDateTimeStrLabel);
        accountInfoPanel.add(reviseStartDateTimeStrValueLabel);
        accountInfoPanel.add(reviseStopTimeStrLabel);
        accountInfoPanel.add(reviseStopTimeStrValueLabel);
        accountInfoPanel.add(innerObjectTypeLabel);
        accountInfoPanel.add(innerObjectTypeValueLabel);
    }


    // 9*2 == 18
    JLabel initMoneyLabel = getCommonLabel("初始总资金");
    JLabel initMoneyValueLabel = getCommonLabelRightAlign();
    JLabel cashLabel = getCommonLabel("当前现金");
    JLabel cashValueLabel = getCommonLabelRightAlign();
    JLabel totalAssetsLabel = getCommonLabel("当前总资产");
    JLabel totalAssetsValueLabel = getCommonLabelRightAlign();
    JLabel currentMarketValueLabel = getCommonLabel("当前总市值");
    JLabel currentMarketValueValueLabel = getCommonLabelRightAlign();
    JLabel currentFloatProfitLabel = getCommonLabel("总计浮盈亏");
    JLabel currentFloatProfitValueLabel = getCommonLabelRightAlign();
    JLabel alreadyCommissionTotalLabel = getCommonLabel("总计手续费"); // @key: 非account属性, 总手续费 / 原始总资金 == 手续费占比
    JLabel alreadyCommissionTotalValueLabel = getCommonLabelRightAlign(); // @key: 非account属性, 总手续费 / 原始总资金 == 手续费占比
    JLabel currentPositionPercentLabel = getCommonLabel("当前总仓位"); // @key: 非account属性, 用 总资产 - 初始资金即可! == 盈利
    JLabel currentPositionPercentValueLabel = getCommonLabelRightAlign();// @key: 非account属性, 用 总资产 - 初始资金即可! == 盈利
    JLabel currentTotalProfitPercentLabel = getCommonLabel("利润百分比"); // @key: 非account属性, 用 总资产 - 现金 == 总市值
    JLabel currentTotalProfitPercentValueLabel = getCommonLabelRightAlign();// @key: 非account属性, 用 总资产 - 现金 == 总市值
    JLabel alreadyCommissionTotalPercentLabel = getCommonLabel("手续费百分比"); // @key: 非account属性, 用 总市值 /总资产 == 当前持仓 总仓位
    JLabel alreadyCommissionTotalPercentValueLabel = getCommonLabelRightAlign();// @key: 非account属性, 用 总市值 /总资产 == 当前持仓 总仓位

    /**
     * 6 * 3 布局
     */
    private void initAccountMoneyPanel() {
        accountMoneyPanel = new JPanel();
        accountMoneyPanel.setLayout(new GridLayout(3, 6, 0, 0));

        accountMoneyPanel.add(initMoneyLabel);
        accountMoneyPanel.add(initMoneyValueLabel);
        accountMoneyPanel.add(cashLabel);
        accountMoneyPanel.add(cashValueLabel);
        accountMoneyPanel.add(totalAssetsLabel);
        accountMoneyPanel.add(totalAssetsValueLabel);
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

    public static JLabel getCommonLabel(String text, Color foreColor, boolean leftAlignment) {
        JLabel jLabel = new JLabel(text);
        jLabel.setForeground(foreColor);
        jLabel.setBackground(commonLabelBackGroundColor);
        if (leftAlignment) {
            jLabel.setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        }
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
