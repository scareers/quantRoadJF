package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.review;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.NewsTabPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class ZiXunJingHuaPanel extends SimpleNewListPanel {
    public static ZiXunJingHuaPanel INSTANCE;

    public ZiXunJingHuaPanel(
            NewsTabPanel parentS) {
        super(parentS);
    }

    public static ZiXunJingHuaPanel getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new ZiXunJingHuaPanel(parentS);
        }
        return INSTANCE;
    }

    @Override
    public void flushBeanMapAndShowDf() {
        List<SimpleNewEm> newsForReviseByType;
        try {
            newsForReviseByType = SimpleNewEmDao.getNewsForReviseByType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE);
        } catch (SQLException e) {
            e.printStackTrace();
            // 此时使用老数据
            return;
        }
        ConcurrentHashMap<Long, SimpleNewEm> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = SimpleNewEmDao.buildDfFromBeanList(newsForReviseByType);
    }
}
