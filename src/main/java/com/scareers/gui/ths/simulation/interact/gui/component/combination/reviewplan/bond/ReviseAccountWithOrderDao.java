package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 复盘账号订单 dao
 *
 * @author: admin
 * @date: 2022/6/10/010-06:48:05
 */
public class ReviseAccountWithOrderDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();


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
     * 保存或者更新单个bean
     *
     * @param id
     * @return
     * @noti 某些未序列化字段, 将不被保存到数据库;
     */
    public static void saveOrUpdateBean(ReviseAccountWithOrder bean) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(bean);
        transaction.commit();
        session.close();
    }
}
