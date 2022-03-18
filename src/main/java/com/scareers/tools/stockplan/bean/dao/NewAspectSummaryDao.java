package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.tools.stockplan.bean.NewAspectSummary;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * description: NewAspectSummary 资讯面个人总结 bean dao
 *
 * @author: admin
 * @date: 2022/3/17/017-23:25:09
 */
public class NewAspectSummaryDao {
    private static SessionFactory sessionFactory = HibernateSessionFactory.getSessionFactoryOfEastMoney();

    public static void main(String[] args) {
        NewAspectSummary bean = new NewAspectSummary();
        bean.setRemark("测试bean");
        bean.setGeneratedTime(DateUtil.date());
        bean.addBadPoint("测试观点1");

        saveOrUpdateBean(bean);

        NewAspectSummary newAspectSummary = getBeanById(3L);
        Console.log(newAspectSummary.getBadPoints());
        Console.log(newAspectSummary.getBadPointsJsonStr());
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
     * 为操盘计划, 从数据库获取 bean, 或者实例化并, 并首次保存到数据库! 返回bean
     */
    public static NewAspectSummary getOrInitBeanForPlan() {

    }


}
