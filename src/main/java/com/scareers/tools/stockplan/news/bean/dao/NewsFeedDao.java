package com.scareers.tools.stockplan.news.bean.dao;

import cn.hutool.core.date.*;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.NewsFeed;
import com.scareers.utils.log.LogUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/16/016-23:43:25
 */
public class NewsFeedDao {
    public static void main(String[] args) throws SQLException {
        Console.log(getNewsForTradePlanByDate("2022-03-15"));
    }

    /**
     * 给定日期, 返回当日所有 公司利好消息
     *
     * @param dateStr 标准日期字符串
     * @return
     * @throws SQLException
     */
    public static List<NewsFeed> getNewsForTradePlanByDate(String dateStr) {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql = "FROM NewsFeed E WHERE E.dateStr=:dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        List<NewsFeed> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((NewsFeed) bean);
        }
        session.close();
        return res;
    }


    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static NewsFeed getBeanById(long id) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        NewsFeed bean = session.get(NewsFeed.class, id);
        session.close();
        return bean;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(NewsFeed bean) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("NewsFeed: 更新成功, id: {}", bean.getId());
        session.close();
    }

    /**
     * 为操盘计划, 获取适当日期的 新闻联播集锦 --> 相同于 公司重大公告
     * 逻辑:
     * 1.当今日是交易日
     * 当时间<=15:00, 则获取 上一交易日
     * 当时间>=15:00, 则获取 今日 (但是今日很可能没出数据), 此时为明天做操盘计划
     * 2.当今日非交易日
     * 获取上一交易日, 为下一交易日做计划
     *
     * @param dateStr
     * @return
     */
    public static List<NewsFeed> getNewsForPlan(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) { // 超过下午3点
                return getNewsForTradePlanByDate(today);
            }
        }
        // 其他情况均获取上一交易日. 因为非交易日没有数据
        String preDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1); // 上一确定交易日

        List<NewsFeed> res = new ArrayList<>();
        DateRange range = new DateRange(DateUtil.parse(preDate), DateUtil.parse(today), DateField.DAY_OF_MONTH,
                1, true, false); // 此时不包含今日


        for (DateTime dateTime : range) { // 这里将是 上一交易日 到 绝对的 昨天, 0点.
            res.addAll(getNewsForTradePlanByDate(DateUtil.format(dateTime, DatePattern.NORM_DATE_PATTERN)));
        }
        return res;
    }


    private static final Log log = LogUtil.getLogger();
}
