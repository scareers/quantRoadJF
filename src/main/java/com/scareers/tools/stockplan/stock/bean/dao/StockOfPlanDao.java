package com.scareers.tools.stockplan.stock.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao;
import com.scareers.tools.stockplan.stock.bean.StockGroupOfPlan;
import com.scareers.tools.stockplan.stock.bean.StockOfPlan;
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
 * description: 个股dao
 *
 * @author: admin
 * @date: 2022/4/9/009-17:26:47
 */
public class StockOfPlanDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) throws Exception {
        List<IndustryConceptThsOfPlan> beanListForPlan = IndustryConceptThsOfPlanDao
                .getBeanListForPlan(DateUtil.date());
        StockOfPlan orInitBeanForPlan = getOrInitBeanForPlan("000568", DateUtil.date(), null);
        if (orInitBeanForPlan != null) {

            Console.log(orInitBeanForPlan);
            orInitBeanForPlan.initIndustryConceptRelatedAttrs(beanListForPlan);
            Console.log(orInitBeanForPlan);

        }


    }

    /**
     * 保存或者更新单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void saveOrUpdateBean(StockOfPlan bean) {
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
    public static void saveOrUpdateBeanBatch(Collection<StockOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (StockOfPlan bean : beans) {
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
     * 删除单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void deleteBean(StockOfPlan bean) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.delete(bean);
        transaction.commit();
        session.close();
    }

    /**
     * 删除多个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void deleteBeanBatch(List<StockOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (StockOfPlan bean : beans) {
            session.delete(bean);
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
     * 注意: 创建新对象若失败, 将返回 null!
     *
     * @param equivalenceNow 等价的now时间! 不默认使用 真实now
     * @key3 industryConceptList可null 则不会初始化 行业概念相关字段; 需要后期自行刷新调用! // 当新建对象时.
     */
    public static StockOfPlan getOrInitBeanForPlan(String code, Date equivalenceNow,
                                                   List<IndustryConceptThsOfPlan> industryConceptList) {
        String dateStr = decideDateStrForPlan(equivalenceNow);
        //String dateStr = DateUtil.today();
        return getOrInitBeanByDateStr(code, dateStr, industryConceptList);
    }

    /**
     * 注意: 创建新对象若失败, 将返回 null!
     *
     * @param code
     * @param dateStr
     * @param industryConceptList
     * @return
     */
    private static StockOfPlan getOrInitBeanByDateStr(String code, String dateStr,
                                                      List<IndustryConceptThsOfPlan> industryConceptList) {
        Session session = sessionFactory.openSession();
        String hql = "FROM StockOfPlan E WHERE E.code = :code and E.dateStr = :dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("code", code);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        if (beans.size() == 0) {
            // 需要实例化并保存
            StockOfPlan bean = null;
            try {
                bean = StockOfPlan.newInstance(code, dateStr, industryConceptList);
                saveOrUpdateBean(bean);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bean;
        } else {
            // 读取第一个(一般且唯一)bean结果
            StockOfPlan bean = (StockOfPlan) beans.get(0);
            bean.initTransientAttrsWhenBeanFromDb();
            return bean;
        }
    }

    /**
     * 为操盘计划, 获取适当日期的, 指定股票组的, 数据表中已存在的个股bean 列表
     * 若股票组null, 则默认获取 所有股票
     *
     * @param dateStr
     * @return
     */
    public static List<StockOfPlan> getBeanListForPlan(Date equivalenceNow, StockGroupOfPlan stockGroup) {
        String dateStrForPlan = decideDateStrForPlan(equivalenceNow);
        return getBeansByDateAndGroup(dateStrForPlan, stockGroup);
    }


    /**
     * 若股票组null, 则默认获取 所有股票
     *
     * @param dateStr 标准日期字符串
     * @return
     * @throws SQLException
     */
    public static List<StockOfPlan> getBeansByDateAndGroup(String dateStr, StockGroupOfPlan stockGroup) {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        Query query;
        if (stockGroup == null) {
            String hql = "FROM StockOfPlan E WHERE E.dateStr=:dateStr";
            query = session.createQuery(hql);
            query.setParameter("dateStr", dateStr);
        } else {
            String hql = "FROM StockOfPlan E WHERE E.dateStr=:dateStr and E.code in :codes";
            query = session.createQuery(hql);
            query.setParameter("dateStr", dateStr);
            query.setParameter("codes", stockGroup.getIncludeStockCodes());
        }
        List beans = query.list();
        List<StockOfPlan> res = new ArrayList<>();
        for (Object bean : beans) {
            StockOfPlan bean1 = (StockOfPlan) bean;
            bean1.initTransientAttrsWhenBeanFromDb();
            res.add(bean1);
        }
        session.close();
        return res;
    }


    /**
     * 当此刻今日是交易日:
     * 时间>15, 下一交易日
     * <15, 今日
     * 当今日非交易日,
     * 下一交易日
     *
     * @param equivalenceNow
     * @return
     */
    @SneakyThrows
    public static String decideDateStrForPlan(Date equivalenceNow) {
        String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) {
                return EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 明日
            } else {
                return today;
            }
        }
        return EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 明日;
    }
}
