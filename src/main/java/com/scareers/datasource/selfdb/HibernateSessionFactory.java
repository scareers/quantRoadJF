package com.scareers.datasource.selfdb;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * description: hibernate 使用数据库连接配置; 单例模式
 *
 * @author: admin
 * @date: 2022/3/17/017-19:56:06
 */
public class HibernateSessionFactory {
    private static SessionFactory sessionFactoryOfEastMoney;

    /**
     * eastmoney 数据库 回话工厂连接
     *
     * @return
     */
    public static SessionFactory getSessionFactoryOfEastMoney() {
        if (sessionFactoryOfEastMoney == null) {
            Configuration configuration = new Configuration().configure("hibernate_eastmoney.cfg.xml");
            sessionFactoryOfEastMoney = configuration.buildSessionFactory();
        }
        return sessionFactoryOfEastMoney;
    }
}
