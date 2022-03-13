package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description: SimpleNewEm DAO -- 数据操作api
 *
 * @author: admin
 * @date: 2022/3/12/012-18:44:43
 */
public class SimpleNewEmDao {
    private static final Log log = LogUtil.getLogger();
    public static SessionFactory sessionFactory;
    public static List<String> allCol = Arrays
            .asList("id", "dateTime", "title", "url", "detailTitle", "saveTime", "type", "urlRawHtml",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            ); // 多了id列

    static {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
    }

    public static void main(String[] args) throws SQLException {
//        List<SimpleNewEm> caiJingDaoDuNewsForRevise = getNewsForReviseByType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE);
//        Console.log(caiJingDaoDuNewsForRevise.size());
//        Console.log(caiJingDaoDuNewsForRevise.get(0));
//        for (SimpleNewEm simpleNewEm : caiJingDaoDuNewsForRevise) {
//            Console.log(simpleNewEm.getDateTime());
//            Console.log(simpleNewEm.getTitle());
//        }

        Console.log(getBeanById(10));
    }


    /**
     * 复盘时, 获取 合理的 财经导读 新闻列表
     * 逻辑: 判定当前时间
     * 1. 判定今日是否交易日?
     * 1.1: 今日是交易日, 时间区间: 上一确定交易日15:00, 到 Min(15:00, now) // 15:00后的新闻应当视为 计划, 而非复盘!!
     * 1.2: 今日非交易日: 上一确定交易日15:00, 到 now
     */
    public static List<SimpleNewEm> getNewsForReviseByType(int type) throws SQLException {
        // 合理计算 复盘时 应当抓取的 新闻发布 时间区间!
        DateTime startDateTime = decideStartDateTimeForRevise();
        DateTime endDateTime = decideEndDateTimeForRevise();
        // hibernate API, 访问数据库
        Session session = sessionFactory.openSession();
        String hql = "FROM SimpleNewEm E WHERE E.type = :type and E.dateTime>=:startDateTime " +
                "and E.dateTime<=:endDateTime " +
                "ORDER BY E.dateTime DESC"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("type", type); // 注意类型
        query.setParameter("startDateTime", Timestamp.valueOf(startDateTime.toLocalDateTime())); // 注意类型
        query.setParameter("endDateTime", Timestamp.valueOf(endDateTime.toLocalDateTime())); // 注意类型
        List beans = query.list();
        List<SimpleNewEm> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((SimpleNewEm) bean);
        }
        session.close();
        return res;
    }

    /**
     * 决定复盘时, 查看新闻的日期区间 开始
     *
     * @return
     * @throws SQLException
     */
    public static DateTime decideStartDateTimeForRevise() throws SQLException {
        String today = DateUtil.today();
        String preTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        return DateUtil.parse(preTradeDate + " 15:00:00"); // 上一交易日收盘开始
    }

    // 决定复盘时, 查看新闻的日期区间 结束
    public static DateTime decideEndDateTimeForRevise() throws SQLException {
        String today = DateUtil.today();
        DateTime now = DateUtil.date();
        DateTime endDateTime = now;
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(now, true) >= 15) { // 此时超过15点, 依旧以15点为上限
                endDateTime = DateUtil.parse(today + " 15:00:00");
            }
        }
        return endDateTime;
    }

    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static SimpleNewEm getBeanById(long id) {
        Session session = sessionFactory.openSession();
        SimpleNewEm simpleNewEm = session.get(SimpleNewEm.class, id);
        session.close();
        return simpleNewEm;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(SimpleNewEm bean) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("SimpleNewEm: 更新成功, id: {}", bean.getId());
        session.close();
    }

    /**
     * list bean 转换为全字段完整df
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanList(List<SimpleNewEm> news) {
        DataFrame<Object> res = new DataFrame<>(allCol);
        for (SimpleNewEm bean : news) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getDateTime());
            row.add(bean.getTitle());
            row.add(bean.getUrl());
            row.add(bean.getDetailTitle());
            row.add(bean.getSaveTime());
            row.add(bean.getType());
            row.add(bean.getUrlRawHtml());
            row.add(bean.getBriefly());
            row.add(bean.getRelatedObject());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }

}
