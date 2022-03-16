package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.tools.stockplan.bean.MajorIssueItem;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.utils.log.LogUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/16/016-23:43:25
 */
public class MajorIssueItemDao {
    public static void main(String[] args) throws SQLException {
        Console.log(getNewsForTradePlanByDate("2022-03-15"));
        Console.log(getNewsForTradePlanByDateGrouped("2022-03-15"));
    }

    /**
     * 给定日期, 返回当日所有 公司重大事项
     *
     * @param dateStr 标准日期字符串
     * @return
     * @throws SQLException
     */
    public static List<MajorIssueItem> getNewsForTradePlanByDate(String dateStr) throws SQLException {
        // hibernate API, 访问数据库
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        String hql = "FROM MajorIssueItem E WHERE E.dateStr=:dateStr"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("dateStr", dateStr); // 注意类型
        List beans = query.list();
        List<MajorIssueItem> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((MajorIssueItem) bean);
        }
        session.close();
        return res;
    }

    /**
     * 给定日期, 返回当日所有 公司重大事项; 按照原始顺序(新闻中出现的顺序) 已经分类好!
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<MajorIssueItem.MajorIssueBatch> getNewsForTradePlanByDateGrouped(String dateStr)
            throws SQLException {
        // hibernate API, 访问数据库
        List<MajorIssueItem> items = getNewsForTradePlanByDate(dateStr);

        List<String> types = new ArrayList<>(); // 维持顺序
        for (MajorIssueItem item : items) {
            if (!types.contains(item.getType())) {
                types.add(item.getType());
            }
        }

        HashMap<String, ArrayList<MajorIssueItem>> mapped = new HashMap<>();
        for (MajorIssueItem item : items) {
            mapped.putIfAbsent(item.getType(), new ArrayList<>());
            mapped.get(item.getType()).add(item);
        }

        List<MajorIssueItem.MajorIssueBatch> res = new ArrayList<>();
        for (String type : types) {
            res.add(new MajorIssueItem.MajorIssueBatch(mapped.get(type), type));
        }
        return res;
    }

    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static MajorIssueItem getBeanById(long id) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        MajorIssueItem simpleNewEm = session.get(MajorIssueItem.class, id);
        session.close();
        return simpleNewEm;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(MajorIssueItem bean) {
        Session session = SimpleNewEmDao.sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("MajorIssueItem: 更新成功, id: {}", bean.getId());
        session.close();
    }

    private static final Log log = LogUtil.getLogger();
}
