package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.plan;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 资讯精华列表展示;  操盘计划
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class ZiXunJingHuaPanelForPlan extends SimpleNewListPanel {
    public static ZiXunJingHuaPanelForPlan INSTANCE;

    public ZiXunJingHuaPanelForPlan(
            NewsTabPanel parentS) {
        super(parentS);
    }

    public static ZiXunJingHuaPanelForPlan getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new ZiXunJingHuaPanelForPlan(parentS);
        }
        return INSTANCE;
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<SimpleNewEm> newsForReviseByType;
        try {
            newsForReviseByType = SimpleNewEmDao.getNewsForTradePlanByType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE,
                    PlanReviewDateTimeDecider.getUniqueDatetime());
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


