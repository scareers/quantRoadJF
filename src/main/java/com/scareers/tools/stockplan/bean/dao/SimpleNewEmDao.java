package com.scareers.tools.stockplan.bean.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.bean.MajorIssueItem;
import com.scareers.tools.stockplan.bean.MajorIssueItem.MajorIssueBatch;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * description: SimpleNewEm DAO -- 数据操作api
 *
 * @author: admin
 * @date: 2022/3/12/012-18:44:43
 */
public class SimpleNewEmDao {
    private static final Log log = LogUtil.getLogger();
    public static SessionFactory sessionFactory;


    static {
        Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
        sessionFactory = configuration.buildSessionFactory();
    }

    public static void main(String[] args) throws SQLException {
//        List<SimpleNewEm> caiJingDaoDuNewsForRevise = getNewsForReviseByType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE);
//        Console.log(caiJingDaoDuNewsForRevise.size());
//        Console.log(caiJingDaoDuNewsForRevise.get(0));
//        for (SimpleNewEm simpleNewEm : caiJingDaoDuNewsForRevise) {
//            Console.log(simpleNewEm.getDateTime());
//            Console.log(simpleNewEm.getTitle());
//        }

//        Console.log(getBeanById(10));

//        Console.log(getCompanyGoodNewsOf("3月15日"));
//        Console.log(getCompanyMajorIssuesOf("3月14日"));
//        Console.log(getNewsFeedsOf("3月15日"));
//        Console.log(getFourPaperNewsOf("3月16日"));


    }



    /**
     * 复盘时, 获取 合理的 时间区间的 新闻列表; 需要给定类型
     * 逻辑: 判定当前时间
     * 1. 判定今日是否交易日?
     * 1.1: 今日是交易日, 时间区间: 上一确定交易日15:00, 到 Min(15:00, now) // 15:00后的新闻应当视为 计划, 而非复盘!!
     * 1.2: 今日非交易日: 上一确定交易日15:00, 到 now
     */
    public static List<SimpleNewEm> getNewsForReviseByType(int type) throws SQLException {
        // 合理计算 复盘时 应当抓取的 新闻发布 时间区间!
        DateTime startDateTime = decideStartDateTimeForRevise();
        DateTime endDateTime = decideEndDateTimeForRevise();
        // hibernate API, 访问数据库
        Session session = sessionFactory.openSession();
        String hql = "FROM SimpleNewEm E WHERE E.type = :type and E.dateTime>=:startDateTime " +
                "and E.dateTime<=:endDateTime " +
                "ORDER BY E.dateTime DESC"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("type", type); // 注意类型
        query.setParameter("startDateTime", Timestamp.valueOf(startDateTime.toLocalDateTime())); // 注意类型
        query.setParameter("endDateTime", Timestamp.valueOf(endDateTime.toLocalDateTime())); // 注意类型
        List beans = query.list();
        List<SimpleNewEm> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((SimpleNewEm) bean);
        }
        session.close();
        return res;
    }

    /**
     * 操盘计划时, 获取 合理的时间区间的 新闻列表: 同样需要给定类型
     * 逻辑: 判定当前时间
     * 1. 判定今日是否交易日?
     * 1.1: 今日是交易日:
     * --> 若当前时间 <=15:00, 计划应当视为今日计划;  新闻区间为 : 上一个交易日 15:00 - now !!!!!
     * --> 若当前时间 >=15:00, 应当视为开始下一个交易日计划; 新闻区间为: 今日15:00 到 now
     * 1.2: 今日非交易日:
     * -->所有时间, 视为为 下一交易日做准备, 新闻区间为: 上一交易日15:00 - now
     */
    public static List<SimpleNewEm> getNewsForTradePlanByType(int type) throws SQLException {
        // 合理计算 复盘时 应当抓取的 新闻发布 时间区间!
        List<DateTime> dateTimeRange = decideDateTimeRangeForTradePlan();
        DateTime startDateTime = dateTimeRange.get(0);
        DateTime endDateTime = dateTimeRange.get(1);
        // hibernate API, 访问数据库
        Session session = sessionFactory.openSession();
        String hql = "FROM SimpleNewEm E WHERE E.type = :type and E.dateTime>=:startDateTime " +
                "and E.dateTime<=:endDateTime " +
                "ORDER BY E.dateTime DESC"; // 访问发布时间在区间内的新闻列表, 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("type", type); // 注意类型
        query.setParameter("startDateTime", Timestamp.valueOf(startDateTime.toLocalDateTime())); // 注意类型
        query.setParameter("endDateTime", Timestamp.valueOf(endDateTime.toLocalDateTime())); // 注意类型
        List beans = query.list();
        List<SimpleNewEm> res = new ArrayList<>();
        for (Object bean : beans) {
            res.add((SimpleNewEm) bean);
        }
        session.close();
        return res;
    }


    /**
     * 决定复盘时, 查看新闻的日期区间 开始
     *
     * @return
     * @throws SQLException
     */
    public static DateTime decideStartDateTimeForRevise() throws SQLException {
        String today = DateUtil.today();
        String preTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        return DateUtil.parse(preTradeDate + " 15:00:00"); // 上一交易日收盘开始
    }

    // 决定复盘时, 查看新闻的日期区间 结束
    public static DateTime decideEndDateTimeForRevise() throws SQLException {
        String today = DateUtil.today();
        DateTime now = DateUtil.date();
        DateTime endDateTime = now;
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(now, true) >= 15) { // 此时超过15点, 依旧以15点为上限
                endDateTime = DateUtil.parse(today + " 15:00:00");
            }
        }
        return endDateTime;
    }

    /**
     * 决定操盘计划时, 查看新闻的日期区间 开始
     *
     * @return
     * @throws SQLException
     */
    public static List<DateTime> decideDateTimeRangeForTradePlan() throws SQLException {
        List<DateTime> res = new ArrayList<>();
        String today = DateUtil.today();
        Boolean tradeDate = EastMoneyDbApi.isTradeDate(today);

        DateTime now = DateUtil.date();
        String preTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1); // 上一交易日
        String nextTradeDate = EastMoneyDbApi.getPreNTradeDateStrict(today, -1); // 下一交易日

        if (tradeDate) {
            if (DateUtil.hour(now, true) >= 15) { // 今日15:00到now
                res.add(DateUtil.parse(today + " 15:00:00"));
                res.add(now);
            } else { // 上一个交易日 15:00 - now !!!!!
                res.add(DateUtil.parse(preTradeDate + " 15:00:00"));
                res.add(now);
            }
        } else { // 上一交易日15:00 - now
            res.add(DateUtil.parse(preTradeDate + " 15:00:00"));
            res.add(now);
        }
        return res;
    }

    /**
     * 根据id获取bean
     *
     * @param id
     * @return
     */
    public static SimpleNewEm getBeanById(long id) {
        Session session = sessionFactory.openSession();
        SimpleNewEm simpleNewEm = session.get(SimpleNewEm.class, id);
        session.close();
        return simpleNewEm;
    }

    /**
     * 保存单个bean
     *
     * @param id
     * @return
     */
    public static void updateBean(SimpleNewEm bean) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.update(bean);
        session.getTransaction().commit();
        log.warn("SimpleNewEm: 更新成功, id: {}", bean.getId());
        session.close();
    }


    /*
    四大类特殊新闻对象
     */

    /**
     * 财经导读中, 获取4类特殊新闻对象
     *
     * @param dateStr 形如 "x月y日", 不需要标准2位
     * @param prefix  标题前缀即可
     * @return
     */
    public static SimpleNewEm getSpecialNewFromCaiJingDaoDu(String dateStr, String prefix) {
        Session session = sessionFactory.openSession();
        String hql = "FROM SimpleNewEm E WHERE E.type = :type and E.title like :title"; // 访问发布时间在区间内的新闻列表,
        // 类型==1, 即财经导读
        Query query = session.createQuery(hql);
        query.setParameter("type", SimpleNewEm.CAI_JING_DAO_DU_TYPE); // 注意类型
        query.setParameter("title", StrUtil.format("{}{}%", dateStr, prefix));
        List beans = query.list();
        SimpleNewEm res = null;
        if (beans.size() > 0) {
            res = (SimpleNewEm) beans.get(0); // 理论上有且仅有1个
        }
        session.close();
        return res;
    }

    /**
     * 给定日期字符串, 获取 该日晚间上市公司利好消息一览
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getCompanyGoodNewsOf(String dateStr) {
        return getSpecialNewFromCaiJingDaoDu(dateStr, "晚间上市公司利好消息一览");
    }



    /**
     * 给定日期字符串, 获取 晚间央视新闻联播财经内容集锦
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getNewsFeedsOf(String dateStr) {
        // 3月15日晚间央视新闻联播财经内容集锦
        // 3月15日晚间央视新闻联播财经内容集锦%
        return getSpecialNewFromCaiJingDaoDu(dateStr, "晚间央视新闻联播财经内容集锦");
    }

    /**
     * 给定日期字符串, 获取 国内四大证券报纸、重要财经媒体头版头条内容精华摘要
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getFourPaperNewsOf(String dateStr) {
        return getSpecialNewFromCaiJingDaoDu(dateStr, "国内四大证券报纸、重要财经媒体头版头条内容精华摘要");
    }

    public static String buildDateStr(DateTime date) { // 构造日期字符串, 作为新闻标题判定
        return StrUtil.format("{}月{}日", date.getField(DateField.MONTH),
                date.getField(DateField.DAY_OF_MONTH));
    }

    // 对应4个传递 DateTime作为参数
    public static SimpleNewEm getCompanyGoodNewsOf(DateTime date) {
        return getCompanyGoodNewsOf(buildDateStr(date));
    }



    public static SimpleNewEm getNewsFeedsOf(DateTime date) {
        return getNewsFeedsOf(buildDateStr(date));
    }

    public static SimpleNewEm getFourPaperNewsOf(DateTime date) {
        return getFourPaperNewsOf(buildDateStr(date));
    }
}
