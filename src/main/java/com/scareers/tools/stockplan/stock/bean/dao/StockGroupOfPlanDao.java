package com.scareers.tools.stockplan.stock.bean.dao;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.stock.bean.StockGroupOfPlan;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.*;

/**
 * description: 个股组 dao
 *
 * @author: admin
 * @date: 2022/4/9/009-17:26:47
 */
public class StockGroupOfPlanDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) throws Exception {
//        StockGroupOfPlan bean = getOrInitBeanForPlan("测试组1", "描述", DateUtil.date());
//        Console.log(bean);

//        deleteBean(bean);

        List<StockGroupOfPlan> beans = getOrInitStockGroupsForPlan(DateUtil.date());
        Console.log(beans);


    }

    /**
     * 获取今日股票组bean列表; 当size为0, 将自动根据昨日列表初始化(逻辑上只可能发生一次), 随后自行修改;
     *
     * @return
     */
    public static List<StockGroupOfPlan> getOrInitStockGroupsForPlan(Date equivalenceNow) {
        String dateStr = decideDateStrForPlan(equivalenceNow);
        List<StockGroupOfPlan> beans = getStockGroupListByDateStr(dateStr);
        if (beans.size() != 0) {
            return beans;
        }

        // 获取上一交易日bean列表
        List<StockGroupOfPlan> beansPreDay = getStockGroupListByDateStr(
                EastMoneyDbApi.getPreNTradeDateStrict(dateStr, 1));

        boolean hasAllStockGroup = false; // 是否存在 "所有股票"组? 不存在将强制创建

        List<StockGroupOfPlan> beansNew = new ArrayList<>();
        for (StockGroupOfPlan bean : beansPreDay) {
            StockGroupOfPlan newBean = StockGroupOfPlan
                    .newInstance(bean.getName(), bean.getDescription(), dateStr, bean.getIncludeStockCodes());
            beansNew.add(newBean);
            if ("所有股票".equals(bean.getName())) {
                hasAllStockGroup = true; // 记录已经存在 所有股票组
            }
        }
        if (!hasAllStockGroup) {
            HashSet<String> allStock = new HashSet<>();
            for (StockGroupOfPlan stockGroupOfPlan : beansNew) {
                allStock.addAll(stockGroupOfPlan.getIncludeStockCodes());
            }

            beansNew.add(StockGroupOfPlan
                    .newInstance("所有股票", "逻辑上包含所有股票,但需要自行保证!", dateStr, allStock));
        }
        saveOrUpdateBeanBatch(beansNew);
        beansNew.sort(new Comparator<StockGroupOfPlan>() {
            @Override
            public int compare(StockGroupOfPlan o1, StockGroupOfPlan o2) {
                if ("所有股票".equals(o1.getName())) {
                    return 1; // 保证所有股票在最前方
                } else if ("所有股票".equals(o2.getName())) {
                    return -1;
                }
                return o1.getName().compareTo(o2.getName()); // 其余的则按照名称
            }
        });

        return beansNew;
    }

    /**
     * 给定确定日期字符串, 返回当日所有存在的股票组列表; 当没有则返回空列表, 不返回null
     *
     * @param dateStr
     * @return
     */
    public static List<StockGroupOfPlan> getStockGroupListByDateStr(String dateStr) {
        Session session = sessionFactory.openSession();
        String hql = "FROM StockGroupOfPlan E where E.dateStr = :dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        if (beans == null || beans.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<StockGroupOfPlan> res = new ArrayList<>();
        for (Object bean : beans) {
            StockGroupOfPlan bean1 = (StockGroupOfPlan) bean;
            bean1.initTransientAttrsWhenBeanFromDb();
            res.add(bean1);
        }
        return res;
    }


    /**
     * 保存或者更新单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void saveOrUpdateBean(StockGroupOfPlan bean) {
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
    public static void saveOrUpdateBeanBatch(Collection<StockGroupOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (StockGroupOfPlan bean : beans) {
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
     * 删除单个bean, 除去  组: 所有股票
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void deleteBean(StockGroupOfPlan bean) {
        if ("所有股票".equals(bean.getName())) {
            return; // 所有股票组, 不会被删除!
        }
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
    public static void deleteBeanBatch(List<StockGroupOfPlan> beans) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 1;
        for (StockGroupOfPlan bean : beans) {
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
    public static StockGroupOfPlan getOrInitBeanForPlan(String name, String description, Date equivalenceNow) {
        String dateStr = decideDateStrForPlan(equivalenceNow);
        //String dateStr = DateUtil.today();
        return getOrInitBeanByDateStr(name, description, dateStr);
    }

    /**
     * 注意: 创建新对象若失败, 将返回 null!
     *
     * @param code
     * @param dateStr
     * @param industryConceptList
     * @return
     */
    private static StockGroupOfPlan getOrInitBeanByDateStr(String name, String description,
                                                           String dateStr) {
        Session session = sessionFactory.openSession();
        String hql = "FROM StockGroupOfPlan E WHERE E.name = :name and E.dateStr = :dateStr";
        Query query = session.createQuery(hql);
        query.setParameter("name", name); // name 唯一
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        if (beans.size() == 0) {
            // 需要实例化并保存
            StockGroupOfPlan bean = null;
            try {
                bean = StockGroupOfPlan.newInstance(name, description, dateStr, null);
                saveOrUpdateBean(bean);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bean;
        } else {
            // 读取第一个(一般且唯一)bean结果
            StockGroupOfPlan bean = (StockGroupOfPlan) beans.get(0);
            bean.initTransientAttrsWhenBeanFromDb();
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
