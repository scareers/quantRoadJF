package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: SimpleNewEm DAO -- 数据操作api
 *
 * @author: admin
 * @date: 2022/3/12/012-18:44:43
 */
public class SimpleNewEmDao {
    private static final Log log = LogUtil.getLogger();



    public static void main(String[] args) {
        //        List<SimpleNewEm> beans = getCaiJingDaoDuNewsPerPage(1);
        //        List<SimpleNewEm> beans = getZiXunJingHuaPerPage(1);
        //        saveToDbBatch(beans);
        /*
        "FROM Employee E WHERE E.id = 10";
        "FROM Employee E WHERE E.id > 10 ORDER BY E.salary DESC"

        "FROM Employee E WHERE E.id > 10 " +
               "ORDER BY E.firstName DESC, E.salary DESC ";

        "SELECT SUM(E.salary), E.firtName FROM Employee E " +
               "GROUP BY E.firstName";


         */

        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        SessionFactory sessionFactory = configuration.buildSessionFactory();
        Session session = sessionFactory.openSession();
        String hql = "FROM SimpleNewEm E WHERE E.id > :idmin ORDER BY E.dateTime DESC";
        Query query = session.createQuery(hql);
        query.setParameter("idmin", 10L); // 注意类型
        List beans = query.list();
        for (Object bean : beans) {
            SimpleNewEm b = (SimpleNewEm) bean;
            Console.log(b);
        }


        session.close();
        sessionFactory.close();
    }

    /**
     * 复盘时, 获取 财经导读: SimpleNewEm 列表;
     * 需要判定当前时间: 当时间早于 9:30,
     */
//    public static List<SimpleNewEm>

    /**
     * 获取
     *
     * @param type
     * @return
     */
    public static SimpleNewEm getNewestBeanOf(int type) {
        return null;

        // todo
    }








}
