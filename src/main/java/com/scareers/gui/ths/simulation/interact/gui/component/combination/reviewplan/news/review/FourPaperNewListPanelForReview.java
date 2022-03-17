package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.review;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.FourPaperNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.tools.stockplan.bean.FourPaperNew;
import com.scareers.tools.stockplan.bean.dao.FourPaperNewDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 重大事件显示panel; 操盘计划
 *
 * @author: admin
 * @date: 2022/3/17/017-00:42:25
 */
public class FourPaperNewListPanelForReview extends FourPaperNewListPanel {
    private static FourPaperNewListPanelForReview INSTANCE;

    public static FourPaperNewListPanelForReview getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new FourPaperNewListPanelForReview(parentS);
        }
        return INSTANCE;
    }

    public FourPaperNewListPanelForReview(
            NewsTabPanel parentS) {
        super(parentS);
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<FourPaperNew> newsForReviseByType;
        try {
            newsForReviseByType = FourPaperNewDao.getNewsForReview();
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, FourPaperNew> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = FourPaperNew.buildDfFromBeanList(newsForReviseByType);
    }
}
