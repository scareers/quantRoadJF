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
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

        IndustryConceptThsOfPlan bean3 = getOrInitBeanForPlan("绿色电力", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.CONCEPT);
        Console.log(bean3);

        IndustryConceptThsOfPlan bean4 = getOrInitBeanForPlan("俄乌冲突概念", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.CONCEPT);
        Console.log(bean4);

        IndustryConceptThsOfPlan bean5 = getOrInitBeanForPlan("农业种植", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.CONCEPT);
        Console.log(bean5);

        IndustryConceptThsOfPlan bean6 = getOrInitBeanForPlan("玉米", PlanReviewDateTimeDecider.getUniqueDatetime(),
                IndustryConceptThsOfPlan.Type.CONCEPT);
        Console.log(bean6);

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
    public static List<IndustryConceptThsOfPlan> getBeanListForPlan(Date equivalenceNow)  {
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
            IndustryConceptThsOfPlan bean1 = (IndustryConceptThsOfPlan) bean;
            bean1.initTransientAttrsWhenBeanFromDb();
            res.add(bean1);
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
     * 批量保存或者更新bean; 遍历, 但一定数量后(一批), 立即保存并清除缓存;
     *
     * @param bean
     */
    public static void saveOrUpdateBeanBatch(Collection<IndustryConceptThsOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (IndustryConceptThsOfPlan bean : beans) {
            session.saveOrUpdate(bean);
            i++;
            if (i % 10 == 0) {
                session.flush(); // 保持同步数据库
                session.clear(); // 保持清除缓存内存占用
            }
        }
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
        //String dateStr = DateUtil.today();
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
            // todo: 因为爬虫保存相同数据到下一交易日, 导致 关系和成分股可能是昨天数据, 因此, 需要保留随时刷新的机制,重设关系
//            bean.initRelationList(); // 初始化关系列表
//            bean.initIncludeStockList(); // 初始化成分股列表

            return bean;
        }
    }

    @SneakyThrows
    private static String decideDateStrForPlan(Date equivalenceNow)  {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) {
                return EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 明日
            }
        }
        return today;
    }
}
