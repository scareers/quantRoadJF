package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.GeneralSituationOverviewPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;

/**
 * description: 分别单例 ; 操盘计划 tab 主类
 *
 * @author: admin
 * @date: 2022/3/16/016-18:04:03
 */
public class NewsTabPanelForPlan extends NewsTabPanel {
    public static NewsTabPanelForPlan INSTANCE;

    public static NewsTabPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new NewsTabPanelForPlan(mainDisplayWindow);
        }
        return INSTANCE;
    }

    public NewsTabPanelForPlan(
            MainDisplayWindow mainDisplayWindow) {
        super(mainDisplayWindow);
    }

    @Override
    protected void addTabs() {
        tabbedPane.addTab("资讯精华", ZiXunJingHuaPanelForPlan.getInstance(this));
        tabbedPane.addTab("财经导读", CaiJingDaoDuPanelForPlan.getInstance(this));
        tabbedPane.addTab("重大事项", MajorIssueListPanelForPlan.getInstance(this));
        tabbedPane.addTab("利好消息", CompanyGoodNewListPanelForPlan.getInstance(this));
        tabbedPane.addTab("新闻联播", NewsFeedListPanelForPlan.getInstance(this));
        tabbedPane.addTab("四大报媒", FourPaperNewListPanelForPlan.getInstance(this));
        tabbedPane.addTab("大势行情", GeneralSituationOverviewPanel.getInstance(this).getContainerJScrollPane());
    }
}
