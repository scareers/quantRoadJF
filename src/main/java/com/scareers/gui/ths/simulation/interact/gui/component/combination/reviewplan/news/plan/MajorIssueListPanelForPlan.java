package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.MajorIssueListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.tools.stockplan.news.bean.MajorIssue;
import com.scareers.tools.stockplan.news.bean.dao.MajorIssueDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 重大事件显示panel; 操盘计划
 *
 * @author: admin
 * @date: 2022/3/17/017-00:42:25
 */
public class MajorIssueListPanelForPlan extends MajorIssueListPanel {
    private static MajorIssueListPanelForPlan INSTANCE;

    public static MajorIssueListPanelForPlan getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new MajorIssueListPanelForPlan(parentS);
        }
        return INSTANCE;
    }

    public MajorIssueListPanelForPlan(
            NewsTabPanel parentS) {
        super(parentS);
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<MajorIssue> newsForReviseByType;
        try {
            newsForReviseByType = MajorIssueDao.getNewsForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, MajorIssue> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = MajorIssue.buildDfFromBeanList(newsForReviseByType);
    }
}
