package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.review;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 财经导读新闻列表展示;  复盘
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class CaiJingDaoDuPanelForReview extends SimpleNewListPanel {
    public static CaiJingDaoDuPanelForReview INSTANCE;

    public CaiJingDaoDuPanelForReview(
            NewsTabPanel parentS) {
        super(parentS);
    }

    public static CaiJingDaoDuPanelForReview getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new CaiJingDaoDuPanelForReview(parentS);
        }
        return INSTANCE;
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<SimpleNewEm> newsForReviseByType;
        try {
            newsForReviseByType = SimpleNewEmDao.getNewsForReviseByType(SimpleNewEm.CAI_JING_DAO_DU_TYPE);
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, SimpleNewEm> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = SimpleNewEm.buildDfFromBeanList(newsForReviseByType);
    }
}
