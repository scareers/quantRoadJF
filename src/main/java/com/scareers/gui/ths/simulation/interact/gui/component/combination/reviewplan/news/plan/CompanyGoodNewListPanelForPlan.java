package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.CompanyGoodNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.tools.stockplan.bean.CompanyGoodNew;
import com.scareers.tools.stockplan.bean.dao.CompanyGoodNewDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/17/017-13:19:12
 */
public class CompanyGoodNewListPanelForPlan extends CompanyGoodNewListPanel {
    private static CompanyGoodNewListPanelForPlan INSTANCE;

    public static CompanyGoodNewListPanelForPlan getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new CompanyGoodNewListPanelForPlan(parentS);
        }
        return INSTANCE;
    }

    public CompanyGoodNewListPanelForPlan(
            NewsTabPanel parentS) {
        super(parentS);
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<CompanyGoodNew> newsForReviseByType;
        try {
            newsForReviseByType = CompanyGoodNewDao.getNewsForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, CompanyGoodNew> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = CompanyGoodNew.buildDfFromBeanList(newsForReviseByType);
    }
}
