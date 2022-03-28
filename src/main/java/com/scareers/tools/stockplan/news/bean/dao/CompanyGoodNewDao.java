package com.scareers.tools.stockplan.news.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.CompanyGoodNew;
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
public class CompanyGoodNewDao {
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
    public static List<CompanyGoodNew> getNewsForTradePlanByDate(String dateStr) {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql = "FROM CompanyGoodNew E WHERE E.dateStr=:dateStr"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        List<CompanyGoodNew> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((CompanyGoodNew) bean);
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
    public static CompanyGoodNew getBeanById(long id) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        CompanyGoodNew bean = session.get(CompanyGoodNew.class, id);
        session.close();
        return bean;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(CompanyGoodNew bean) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("CompanyGoodNew: 更新成功, id: {}", bean.getId());
        session.close();
    }

    /**
     * 为操盘计划, 获取适当日期的 利好公告 --> 相同于 公司重大公告
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
    public static List<CompanyGoodNew> getNewsForPlan(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) { // 超过下午3点
                return getNewsForTradePlanByDate(today);
            }
        }
        // 其他情况均获取上一交易日. 因为非交易日没有数据
        String preDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        return getNewsForTradePlanByDate(preDate);
    }


    /**
     * 为复盘, 获取适当日期的 利好公告 --> 相同于 公司重大事件
     * 逻辑:
     * 1.当今日是交易日
     * 获取上一交易日, 为上一交易日复盘
     * 2.当今日非交易日
     * 获取上 2 交易日, 为上一交易日复盘
     *
     * @param dateStr
     * @return
     */
    public static List<CompanyGoodNew> getNewsForReview(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            return getNewsForTradePlanByDate(EastMoneyDbApi.getPreNTradeDateStrict(today, 1));
        } else {
            return getNewsForTradePlanByDate(EastMoneyDbApi.getPreNTradeDateStrict(today, 2));
        }
    }

    private static final Log log = LogUtil.getLogger();
}
