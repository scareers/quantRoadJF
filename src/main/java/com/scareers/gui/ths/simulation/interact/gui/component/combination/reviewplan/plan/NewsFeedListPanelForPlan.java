package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsFeedListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.tools.stockplan.bean.NewsFeed;
import com.scareers.tools.stockplan.bean.dao.NewsFeedDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/17/017-13:19:12
 */
public class NewsFeedListPanelForPlan extends NewsFeedListPanel {
    private static NewsFeedListPanelForPlan INSTANCE;

    public static NewsFeedListPanelForPlan getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new NewsFeedListPanelForPlan(parentS);
        }
        return INSTANCE;
    }

    public NewsFeedListPanelForPlan(
            NewsTabPanel parentS) {
        super(parentS);
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<NewsFeed> newsForReviseByType;
        try {
            newsForReviseByType = NewsFeedDao.getNewsForTradePlanForPlan();
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, NewsFeed> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = NewsFeed.buildDfFromBeanList(newsForReviseByType);
    }
}
