package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 财经导读新闻列表展示;  操盘计划
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class CaiJingDaoDuPanelForPlan extends SimpleNewListPanel {
    public static CaiJingDaoDuPanelForPlan INSTANCE;

    public CaiJingDaoDuPanelForPlan(
            NewsTabPanel parentS) {
        super(parentS);
    }

    public static CaiJingDaoDuPanelForPlan getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new CaiJingDaoDuPanelForPlan(parentS);
        }
        return INSTANCE;
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<SimpleNewEm> newsForReviseByType;
        try {
            newsForReviseByType = SimpleNewEmDao.getNewsForTradePlanByType(SimpleNewEm.CAI_JING_DAO_DU_TYPE);
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
