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
 * description: StockOfPlan Dao
 *
 * @author: admin
 * @date: 2022/3/29/029-00:26:20
 */
public class IndustryConceptThsOfPlanDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) throws SQLException {
//
        List<IndustryConceptThsOfPlan> beanListByNameAndType = getBeanListByNameAndType("三胎概念", "概念", "2022-04-01",
                null);
        Console.log(beanListByNameAndType);
        Console.log(beanListByNameAndType.size());


//        StockOfPlan bean = getOrInitBeanForPlan("三胎概念", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.CONCEPT);
//        Console.log(bean);
//
//        StockOfPlan bean2 = getOrInitBeanForPlan("电力", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.INDUSTRY);
//        Console.log(bean2);
//
//        StockOfPlan bean3 = getOrInitBeanForPlan("绿色电力", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.CONCEPT);
//        Console.log(bean3);
//
//        StockOfPlan bean4 = getOrInitBeanForPlan("俄乌冲突概念", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.CONCEPT);
//        Console.log(bean4);
//
//        StockOfPlan bean5 = getOrInitBeanForPlan("农业种植", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.CONCEPT);
//        Console.log(bean5);
//
//        StockOfPlan bean6 = getOrInitBeanForPlan("玉米", PlanReviewDateTimeDecider.getUniqueDatetime(),
//                StockOfPlan.Type.CONCEPT);
//        Console.log(bean6);

//        List<StockOfPlan> beans = getBeanListForPlan(DateUtil.date());
//        Console.log(beans);
//        DataFrame<Object> dataFrame = StockOfPlan.buildDfFromBeanList(beans);
//        Console.log(dataFrame);
    }

    /**
     * 根据name和type, 获取所有结果, dateStr倒序排列; 可给定 dateStrLimit 则取 dateStr >=它; 若null则全部
     * 无结果返回空列表
     *
     * @param name
     * @param type       行业 或者 概念
     * @param dateStrMin 可null, 否则标准日期字符串
     * @param dateStrMax 可null, 否则标准日期字符串; 日期上限, 不包含! 前包后不包
     * @return
     */
    public static List<IndustryConceptThsOfPlan> getBeanListByNameAndType(String name, String type,
                                                                          String dateStrMin, String dateStrMax) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql;
        if (dateStrMax == null) {
            dateStrMax = "2200-01-01";
        }
        if (dateStrMin == null) {
            hql = "FROM IndustryConceptThsOfPlan E WHERE E.type=:type and E.name=:name and dateStr<:dateStrMax order " +
                    "by dateStr " +
                    "desc ";
        } else {
            hql = "FROM IndustryConceptThsOfPlan E WHERE E.type=:type and E.name=:name and " +
                    "dateStr>=:dateStrMin and dateStr<:dateStrMax order by dateStr desc ";
        }
        Query query = session.createQuery(hql);
        query.setParameter("type", type);
        query.setParameter("name", name);
        query.setParameter("dateStrMax", dateStrMax);
        if (dateStrMin != null) {
            query.setParameter("dateStrMin", dateStrMin);
        }
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
     * 给定一个新建的bean, 自行保证是刚实例化, 可编辑字段都未自定义; // 若已修改则将被覆盖
     * 将从数据库, 查询 N天内 相同行业/概念, 并读取曾经"最新"的bean, 将参数bean的可编辑字段, 设置为过去N天内最新bean相同
     * --> 同步可编辑属性, 通过最新N同名bean(中的最新一个);
     * --> @key: 某些可编辑字段, 例如预判, 将不被同步, 预判需要自行设定
     * --> 仅更新, 且保存到数据库
     *
     * @param newBean
     * @param stdDateStr
     * @param preNDay
     * @return
     * @throws SQLException
     */
    public static IndustryConceptThsOfPlan syncEditableAttrByLatestNSameBean(IndustryConceptThsOfPlan newBean,
                                                                             String stdDateStr, int preNDay)
            throws SQLException {
        String preNTradeDateStrict = EastMoneyDbApi.getPreNTradeDateStrict(stdDateStr, preNDay);
        List<IndustryConceptThsOfPlan> oldBeans = IndustryConceptThsOfPlanDao
                .getBeanListByNameAndType(newBean.getName(), newBean.getType(), preNTradeDateStrict,
                        stdDateStr); // 曾经bean列表


        // @key: 对简单可编辑属性 赋值
        if (oldBeans.size() != 0) { // 存在历史
            // @key: 主要是trend 必须自行设定, 以及预判4字段

            IndustryConceptThsOfPlan lastBean = oldBeans.get(0); // 倒序, 第一个即最新

            newBean.setPricePositionLongTerm(lastBean.getPricePositionLongTerm());
            newBean.setPricePositionShortTerm(lastBean.getPricePositionShortTerm());
            newBean.setPriceTrend(lastBean.getPriceTrend());
            newBean.setOscillationAmplitude(lastBean.getOscillationAmplitude());
            newBean.setOscillationAmplitude(lastBean.getOscillationAmplitude());
            newBean.setLineType(lastBean.getLineType());
            newBean.setHypeReason(lastBean.getHypeReason());
            newBean.setHypeStartDate(lastBean.getHypeStartDate());
            newBean.setHypePhaseCurrent(lastBean.getHypePhaseCurrent());
            newBean.setSpecificDescription(lastBean.getSpecificDescription());

            newBean.setLeaderStockList(lastBean.getLeaderStockList());
            newBean.setLeaderStockListJsonStr(lastBean.getLeaderStockListJsonStr());

            newBean.setGoodAspects(lastBean.getGoodAspects());
            newBean.setBadAspects(lastBean.getBadAspects());
            newBean.setWarnings(lastBean.getWarnings());

            newBean.setRemark(lastBean.getRemark());
        }
        saveOrUpdateBean(newBean);
        return newBean;
    }


    /**
     * 为操盘计划, 获取适当日期的, 数据表中已存在的bean 列表
     *
     * @param dateStr
     * @return
     */
    public static List<IndustryConceptThsOfPlan> getBeanListForPlan(Date equivalenceNow) {
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
     * 删除单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void deleteBean(IndustryConceptThsOfPlan bean) {
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
    public static void deleteBeanBatch(List<IndustryConceptThsOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (IndustryConceptThsOfPlan bean : beans) {
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
     * 操盘计划:
     * 1.若今日为交易日:
     * 1.1.若时间<15:00, 盘中, 则获取今日的操盘计划 对象
     * 1.2.若时间>=15:00, 盘后, 应当获取 下一交易日bean
     * 2.若非交易日: 应当获取 下一交易日bean
     *
     * @param equivalenceNow 等价的now时间! 不默认使用 真实now
     */
    public static IndustryConceptThsOfPlan getOrInitBeanForPlan(String industryOrConceptName, Date equivalenceNow,
                                                                IndustryConceptThsOfPlan.Type type) {
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
