package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import lombok.Getter;
import lombok.Setter;

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


    private void initAccountInfoPanel() {
        accountInfoPanel = new JPanel();
    }

    private void initAccountMoneyPanel() {
        accountMoneyPanel = new JPanel();
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


}
