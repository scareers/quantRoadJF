package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.bean.NewAspectSummary;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * description: NewAspectSummary 资讯面个人总结 bean dao
 *
 * @key3 : 所有直接从数据库读取bean的方法, 均需要调用 initTransientAttrsWhenBeanFromDb, 初始化8大 transient 字段
 * @author: admin
 * @date: 2022/3/17/017-23:25:09
 */
public class NewAspectSummaryDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) throws SQLException {
        NewAspectSummary bean = getOrInitBeanForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        Console.log(bean);

        NewAspectSummary bean2 = getOrInitBeanForReview(PlanReviewDateTimeDecider.getUniqueDatetime());
        Console.log(bean2);
    }

    /**
     * 根据id 获取 bean
     *
     * @param id
     * @return
     * @noti : 凡是 "查询数据库" 返回bean的方法, 均需要根据 数据库保存的数据, 初始化一些 未序列化字段!
     */
    public static NewAspectSummary getBeanById(long id) {
        Session session = sessionFactory.openSession();
        NewAspectSummary bean = session.get(NewAspectSummary.class, id);
        session.close();
        bean.initTransientAttrsWhenBeanFromDb();
        return bean;
    }

    /**
     * 保存或者更新单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void saveOrUpdateBean(NewAspectSummary bean) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(bean);
        transaction.commit();
        session.close();
    }

    /**
     * 为操盘计划, 从数据库获取 bean, 或者(实例化并, 并首次保存到数据库)! 返回bean
     * 操盘计划:
     * 1.若今日为交易日:
     * 1.1.若时间<15:00, 盘中, 则获取今日的操盘计划 对象
     * 1.2.若时间>=15:00, 盘后, 应当获取 下一交易日bean
     * 2.若非交易日: 应当获取 下一交易日bean
     *
     * @param equivalenceNow 等价的now时间! 不默认使用 真实now
     */
    public static NewAspectSummary getOrInitBeanForPlan(Date equivalenceNow) throws SQLException {
        String dateStr = decideDateStrForPlan(equivalenceNow);
        return getOrInitBeanByDateStr(dateStr);
    }

    /**
     * 为复盘, 从数据库获取 bean, 或者(实例化并, 并首次保存到数据库)! 返回bean
     * 复盘时:
     * 1.若今日为交易日:
     * 1.1.若时间<15:00, 盘中, 则获取上一交易日
     * 1.2.若时间>=15:00, 盘后, 应当获取 今日
     * 2.若非交易日: 应当获取 上一交易日bean
     *
     * @param equivalenceNow 等价的now时间! 不默认使用 真实now
     */
    public static NewAspectSummary getOrInitBeanForReview(Date equivalenceNow) throws SQLException {
        String dateStr = decideDateStrForReview(equivalenceNow);
        return getOrInitBeanByDateStr(dateStr);
    }

    private static NewAspectSummary getOrInitBeanByDateStr(String dateStr) {
        Session session = sessionFactory.openSession();
        String hql = "FROM NewAspectSummary E WHERE E.type = :type and E.dateStr = :dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("type", NewAspectSummary.PLAN_TYPE);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        if (beans.size() == 0) {
            // 需要实例化并保存
            NewAspectSummary bean = NewAspectSummary.newInstance(dateStr, NewAspectSummary.PLAN_TYPE);
            saveOrUpdateBean(bean);
            return bean;
        } else {
            // 读取第一个(一般且唯一)bean结果
            NewAspectSummary bean = (NewAspectSummary) beans.get(0);
            bean.initTransientAttrsWhenBeanFromDb();
            return bean;
        }
    }

    private static String decideDateStrForPlan(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) {
                return EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 明日
            }
        }
        return today;
    }

    private static String decideDateStrForReview(Date equivalenceNow) throws SQLException {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) {
                return today;
            }
        }
        return EastMoneyDbApi.getPreNTradeDateStrict(today, 1); // 上1日;
    }


}
