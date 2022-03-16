package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.review;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;

/**
 * description: 分别单例;  复盘 tab 主类
 *
 * @author: admin
 * @date: 2022/3/16/016-18:04:03
 */
public class NewsTabPanelForRevise extends NewsTabPanel {
    public static NewsTabPanelForRevise INSTANCE;

    public static NewsTabPanelForRevise getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new NewsTabPanelForRevise(mainDisplayWindow);
        }
        return INSTANCE;
    }

    public NewsTabPanelForRevise(
            MainDisplayWindow mainDisplayWindow) {
        super(mainDisplayWindow);
    }

    @Override
    protected void addTabs() {
        tabbedPane.addTab("资讯精华", ZiXunJingHuaPanel.getInstance(this));
        tabbedPane.addTab("财经导读", CaiJingDaoDuPanel.getInstance(this));
    }
}
