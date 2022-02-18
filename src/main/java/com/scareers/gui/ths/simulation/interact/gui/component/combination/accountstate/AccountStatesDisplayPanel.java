package com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate;

import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.display.AccountStatesItemDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.trader.AccountStates;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.display.AccountStatesItemDisplayPanel.CURRENT_HOLD_TITLE;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/18/018-17:43:37
 */
public class AccountStatesDisplayPanel extends JPanel {
    private static AccountStatesDisplayPanel INSTANCE;

    public static AccountStatesDisplayPanel getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new AccountStatesDisplayPanel(mainDisplayWindow);
        }
        return INSTANCE;
    }

    /**
     * 5Panel显示5类账户状态. 未刷新的因为数据项为null, 显示获取中
     */
    AccountStatesItemDisplayPanel nineBaseFundsDataPanel;
    AccountStatesItemDisplayPanel currentHoldsPanel;
    AccountStatesItemDisplayPanel canCancelsPanel;
    AccountStatesItemDisplayPanel todayClinchsPanel;
    AccountStatesItemDisplayPanel todayConsignsPanel;
    MainDisplayWindow mainDisplayWindow;

    volatile boolean addedSubComps = false;

    /*
        public ConcurrentHashMap<String, Double> nineBaseFundsData = new ConcurrentHashMap<>(); // get_account_funds_info
    public DataFrame<Object> currentHolds = null; // get_hold_stocks_info // 持仓
    public DataFrame<Object> canCancels = null; // get_unsolds_not_yet 当前可撤, 即未成交
    public DataFrame<Object> todayClinchs = null; // get_today_clinch_orders 今日成交:
    // 成交时间	证券代码	证券名称	操作	成交数量  成交均价	成交金额	合同编号	成交编号
    public DataFrame<Object> todayConsigns = null; // get_today_consign_orders 今日所有委托
     */

    public AccountStatesDisplayPanel(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;

        GridLayout layout = new GridLayout(3, 2);
        layout.setVgap(10);
        layout.setHgap(20);
        this.setLayout(layout);
        this.add(new JLabel("数据获取中"));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        AccountStatesDisplayPanel panel = this;
        // 7.更新选择的股票以显示 对应的内容. 为了实时刷新的效果, 这里 持续刷新
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                nineBaseFundsDataPanel = new AccountStatesItemDisplayPanel("账户资金状况");
                currentHoldsPanel = new AccountStatesItemDisplayPanel(CURRENT_HOLD_TITLE.toString());
                todayClinchsPanel = new AccountStatesItemDisplayPanel("今日成交");
                canCancelsPanel = new AccountStatesItemDisplayPanel("当前可撤");
                todayConsignsPanel = new AccountStatesItemDisplayPanel("今日委托");

                while (true) {
                    if (addedSubComps) {
                        nineBaseFundsDataPanel.update(AccountStates.getNineBaseFundsData());
                        currentHoldsPanel.update(AccountStates.getCurrentHolds());
                        canCancelsPanel.update(AccountStates.getCanCancels());
                        todayClinchsPanel.update(AccountStates.getTodayClinchs());
                        todayConsignsPanel.update(AccountStates.getTodayConsigns());
                    } else {
                        if (AccountStates.getInstance().alreadyInitialized()) {

                            panel.removeAll();
                            panel.add(nineBaseFundsDataPanel);
                            panel.add(currentHoldsPanel);
                            panel.add(todayClinchsPanel);
                            panel.add(canCancelsPanel);
                            panel.add(todayConsignsPanel);
                            addedSubComps = true;
                        }
                    }
                    Thread.sleep(100);
                }
            }
        }, true);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
