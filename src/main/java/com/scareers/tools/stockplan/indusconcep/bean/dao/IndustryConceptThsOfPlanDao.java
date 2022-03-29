package com.scareers.tools.stockplan.indusconcep.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.news.bean.MajorIssue;
import com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao;
import joinery.DataFrame;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * description: IndustryConceptThsOfPlan Dao
 *
 * @author: admin
 * @date: 2022/3/29/029-00:26:20
 */
public class IndustryConceptThsOfPlanDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) throws SQLException {
        IndustryConceptThsOfPlan bean = getOrInitBeanForPlan("三胎概念", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.CONCEPT);
        Console.log(bean);

        IndustryConceptThsOfPlan bean2 = getOrInitBeanForPlan("电力", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.INDUSTRY);
        Console.log(bean2);

//        List<IndustryConceptThsOfPlan> beans = getBeanListForPlan(DateUtil.date());
//        Console.log(beans);
//        DataFrame<Object> dataFrame = IndustryConceptThsOfPlan.buildDfFromBeanList(beans);
//        Console.log(dataFrame);
    }


    /**
     * 为操盘计划, 获取适当日期的, 数据表中已存在的bean 列表
     *
     * @param dateStr
     * @return
     */
    public static List<IndustryConceptThsOfPlan> getBeanListForPlan(Date equivalenceNow) throws SQLException {
        String dateStrForPlan = decideDateStrForPlan(equivalenceNow);
        return getBeansByDate(dateStrForPlan);
    }

    /**
     * 给定日期, 返回当日所有 概念,行业 bean
     *
     * @param dateStr 标准日期字符串
     * @return
     * @throws SQLException
     */
    public static List<IndustryConceptThsOfPlan> getBeansByDate(String dateStr) {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql = "FROM IndustryConceptThsOfPlan E WHERE E.dateStr=:dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr);
        List beans = query.list();
        List<IndustryConceptThsOfPlan> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((IndustryConceptThsOfPlan) bean);
        }
        session.close();
        return res;
    }

    /**
     * 根据id 获取 bean
     *
     * @param id
     * @return
     * @noti : 凡是 "查询数据库" 返回bean的方法, 均需要根据 数据库保存的数据, 初始化一些 未序列化字段!
     */
    public static IndustryConceptThsOfPlan getBeanById(long id) {
        Session session = sessionFactory.openSession();
        IndustryConceptThsOfPlan bean = session.get(IndustryConceptThsOfPlan.class, id);
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
    public static void saveOrUpdateBean(IndustryConceptThsOfPlan bean) {
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
    public static IndustryConceptThsOfPlan getOrInitBeanForPlan(String industryOrConceptName, Date equivalenceNow,
                                                                IndustryConceptThsOfPlan.Type type)
            throws SQLException {
        String dateStr = decideDateStrForPlan(equivalenceNow);
        return getOrInitBeanByDateStr(industryOrConceptName, dateStr, type);
    }


    private static IndustryConceptThsOfPlan getOrInitBeanByDateStr(String industryOrConceptName, String dateStr,
                                                                   IndustryConceptThsOfPlan.Type type) {
        Session session = sessionFactory.openSession();
        String hql = "FROM IndustryConceptThsOfPlan E WHERE E.name = :name and E.dateStr = :dateStr and E.type=:type";
        Query query = session.createQuery(hql);
        query.setParameter("name", industryOrConceptName);
        query.setParameter("dateStr", dateStr); // 注意类型
        if (type.equals(IndustryConceptThsOfPlan.Type.INDUSTRY)) {
            query.setParameter("type", "行业"); // 注意类型
        } else {
            query.setParameter("type", "概念"); // 注意类型
        }
        List beans = query.list();
        if (beans.size() == 0) {
            // 需要实例化并保存
            IndustryConceptThsOfPlan bean = IndustryConceptThsOfPlan.newInstance(industryOrConceptName, dateStr,
                    type);
            saveOrUpdateBean(bean);
            return bean;
        } else {
            // 读取第一个(一般且唯一)bean结果
            IndustryConceptThsOfPlan bean = (IndustryConceptThsOfPlan) beans.get(0);
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
}
