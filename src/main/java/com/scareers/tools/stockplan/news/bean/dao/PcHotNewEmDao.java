package com.scareers.tools.stockplan.news.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.PcHotNewEm;
import com.scareers.utils.log.LogUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * description: PcHotNewEmDao DAO -- 数据操作api
 *
 * @author: admin
 * @date: 2022/3/12/012-18:44:43
 */
public class PcHotNewEmDao {
    private static final Log log = LogUtil.getLogger();
    public static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();


    public static void main(String[] args) throws SQLException {
//        List<PcHotNewEm> newsForReviseByType = getNewsForTradePlanByType(DateUtil.date());
//        Console.log(newsForReviseByType);

        PcHotNewEm beanById = getBeanById(1);
        Console.log(beanById);

    }


    /**
     * @key3 : 以东财推送时间, 为核心时间进行筛选
     * 复盘时, 获取 合理的 时间区间的 新闻列表; 需要给定类型
     * 逻辑: 判定当前时间
     * 1. 判定今日是否交易日?
     * 1.1: 今日是交易日, 时间区间: 上一确定交易日15:00, 到 Min(15:00, now) // 15:00后的新闻应当视为 计划, 而非复盘!!
     * 1.2: 今日非交易日: 上一确定交易日15:00, 到 now
     */
    public static List<PcHotNewEm> getNewsForReviseByType(Date equivalenceNow) throws SQLException {
        // 合理计算 复盘时 应当抓取的 新闻发布 时间区间!
        DateTime startDateTime = decideStartDateTimeForRevise(equivalenceNow);
        DateTime endDateTime = decideEndDateTimeForRevise(equivalenceNow);
        // hibernate API, 访问数据库
        Session session = sessionFactory.openSession();
        String hql = "FROM PcHotNewEm E WHERE  E.pushtime>=:startDateTime " +
                "and E.pushtime<=:endDateTime " +
                "ORDER BY E.pushtime DESC"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("startDateTime", DateUtil.format(startDateTime, DatePattern.NORM_DATETIME_PATTERN)); // 注意类型
        query.setParameter("endDateTime", DateUtil.format(endDateTime, DatePattern.NORM_DATETIME_PATTERN)); // 注意类型
        List beans = query.list();
        List<PcHotNewEm> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((PcHotNewEm) bean);
        }
        session.close();
        return res;
    }

    /**
     * 操盘计划时, 获取 合理的时间区间的 新闻列表: 同样需要给定类型
     * 逻辑: 判定当前时间
     * 1. 判定今日是否交易日?
     * 1.1: 今日是交易日:
     * --> 若当前时间 <=15:00, 计划应当视为今日计划;  新闻区间为 : 上一个交易日 15:00 - now !!!!!
     * --> 若当前时间 >=15:00, 应当视为开始下一个交易日计划; 新闻区间为: 今日15:00 到 now
     * 1.2: 今日非交易日:
     * -->所有时间, 视为为 下一交易日做准备, 新闻区间为: 上一交易日15:00 - now
     */
    public static List<PcHotNewEm> getNewsForTradePlanByType(Date equivalenceNow) throws SQLException {
        // 合理计算 复盘时 应当抓取的 新闻发布 时间区间!
        List<DateTime> dateTimeRange = decideDateTimeRangeForTradePlan(equivalenceNow);
        DateTime startDateTime = dateTimeRange.get(0);
        DateTime endDateTime = dateTimeRange.get(1);
        // hibernate API, 访问数据库
        Session session = sessionFactory.openSession();
        String hql = "FROM PcHotNewEm E WHERE  E.pushtime>=:startDateTime " +
                "and E.pushtime<=:endDateTime " +
                "ORDER BY E.pushtime DESC"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读

        Query query = session.createQuery(hql);
        query.setParameter("startDateTime", DateUtil.format(startDateTime, DatePattern.NORM_DATETIME_PATTERN)); // 注意类型
        query.setParameter("endDateTime", DateUtil.format(endDateTime, DatePattern.NORM_DATETIME_PATTERN)); // 注意类型
        List beans = query.list();
        List<PcHotNewEm> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((PcHotNewEm) bean);
        }
        session.close();

        // @update: 因为爬虫机制, 这里强制去重一下, 对结果集相同title的, 只保留1. --- 去重机制
        List<PcHotNewEm> res2 = new ArrayList<>();
        HashSet<String> titles = new HashSet<>(); // 保留已加入res2的title
        for (PcHotNewEm re : res) {
            if (!titles.contains(re.getTitle())) { //
                res2.add(re);
                titles.add(re.getTitle());
            }
        }
        return res2;
    }

    /**
     * 决定复盘时, 查看新闻的日期区间 开始
     *
     * @return
     * @throws SQLException
     */
    public static DateTime decideStartDateTimeForRevise(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        String preTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        return DateUtil.parse(preTradeDate + " 15:00:00"); // 上一交易日收盘开始
    }

    // 决定复盘时, 查看新闻的日期区间 结束
    public static DateTime decideEndDateTimeForRevise(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        Date now = equivalenceNow;
        Date endDateTime = now;
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(now, true) >= 15) { // 此时超过15点, 依旧以15点为上限
                endDateTime = DateUtil.parse(today + " 15:00:00");
            }
        }
        return DateUtil.date(endDateTime);
    }

    /**
     * 决定操盘计划时, 查看新闻的日期区间 开始
     *
     * @return
     * @throws SQLException
     */
    public static List<DateTime> decideDateTimeRangeForTradePlan(Date equivalenceNow) throws SQLException {
        List<DateTime> res = new ArrayList<>();
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        Boolean tradeDate = EastMoneyDbApi.isTradeDate(today);

        DateTime now = DateUtil.date(equivalenceNow);
        String preTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1); // 上一交易日
        // String nextTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 下一交易日

        if (tradeDate) {
            if (DateUtil.hour(now, true) >= 15) { // 今日15:00到now
                res.add(DateUtil.parse(today + " 15:00:00"));
                res.add(now);
            } else { // 上一个交易日 15:00 - now !!!!!
                res.add(DateUtil.parse(preTradeDate + " 15:00:00"));
                res.add(now);
            }
        } else { // 上一交易日15:00 - now
            res.add(DateUtil.parse(preTradeDate + " 15:00:00"));
            res.add(now);
        }
        return res;
    }

    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static PcHotNewEm getBeanById(long id) {
        Session session = sessionFactory.openSession();
        PcHotNewEm simpleNewEm = session.get(PcHotNewEm.class, id);
        session.close();
        return simpleNewEm;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(PcHotNewEm bean) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("PcHotNewEm: 更新成功, id: {}", bean.getId());
        session.close();
    }
}
