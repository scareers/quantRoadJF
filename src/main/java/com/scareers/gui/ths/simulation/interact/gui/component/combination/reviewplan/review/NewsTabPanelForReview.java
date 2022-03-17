package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.review;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;

/**
 * description: 分别单例;  复盘 tab 主类
 *
 * @author: admin
 * @date: 2022/3/16/016-18:04:03
 */
public class NewsTabPanelForReview extends NewsTabPanel {
    public static NewsTabPanelForReview INSTANCE;

    public static NewsTabPanelForReview getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new NewsTabPanelForReview(mainDisplayWindow);
        }
        return INSTANCE;
    }

    public NewsTabPanelForReview(
            MainDisplayWindow mainDisplayWindow) {
        super(mainDisplayWindow);
    }

    @Override
    protected void addTabs() {
        tabbedPane.addTab("资讯精华", ZiXunJingHuaPanelForReview.getInstance(this));
        tabbedPane.addTab("财经导读", CaiJingDaoDuPanelForReview.getInstance(this));
        tabbedPane.addTab("重大事项", MajorIssueListPanelForReview.getInstance(this));
        tabbedPane.addTab("利好消息", CompanyGoodNewListPanelForReview.getInstance(this));
        tabbedPane.addTab("新闻联播", NewsFeedListPanelForReview.getInstance(this));
        tabbedPane.addTab("四大报媒", FourPaperNewListPanelForReview.getInstance(this));
    }
}
