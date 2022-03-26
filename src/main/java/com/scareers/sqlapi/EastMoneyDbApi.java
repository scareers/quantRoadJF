package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;

import java.security.AlgorithmConstraints;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static com.scareers.tools.stockplan.bean.SimpleNewEm.buildBeanListFromDfWithId;

/**
 * description: 东财本地数据库api
 *
 * @author: admin
 * @date: 2022/3/5/005-11:25:35
 */
public class EastMoneyDbApi {
    public static Connection connection = ConnectionFactory.getConnLocalEastmoney();
    public static Connection connectionFsTrans = ConnectionFactory.getConnLocalFSTransactionFromEastmoney();
    private static Cache<String, Boolean> isTradeDateCache = CacheUtil.newLRUCache(2048);
    private static Pattern stdDatePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}"); // 标准的日期表达式
    public static Cache<String, String> preNTradeDateStrictCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 某个日期的上n个交易日?

    public static void main(String[] args) throws Exception {



//        Console.log(isTradeDate("20220304"));

//        Console.log(getLatestSaveBeanByType(1, 10));
//        List<SimpleNewEm> latestSaveBeanByType = getLatestSaveBeanByType(1, 10);
//        for (SimpleNewEm simpleNewEm : latestSaveBeanByType) {
//            Console.log(simpleNewEm);
//        }

//        Console.log(getPreNTradeDateStrict(DateUtil.today(), 3));
//        Console.log(getPreNTradeDateStrict("20220318", -2));
    }



    /**
     * 是否标准日期形式
     *
     * @param date
     * @return
     */
    private static boolean isStdDatePattern(String date) {
        return stdDatePattern.matcher(date).matches();
    }

    /**
     * 判定是否是 上交所SSE 交易日
     *
     * @param date 任意可被hutool解析的日期形式
     * @return
     */
    public static Boolean isTradeDate(String date) throws SQLException {
        if (!isStdDatePattern(date)) { // 匹配标准形式, 否则解析
            date = DateUtil.parse(date).toString(DatePattern.NORM_DATE_PATTERN); // 标准化
        }
        Boolean res = isTradeDateCache.get(date);
        if (res != null) {
            return res;
        }
        String sql = StrUtil.format("select is_open from trade_dates where date='{}'", date);
        DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
        if ("1".equals(dataFrame.get(0, 0).toString())) {
            res = Boolean.TRUE;
        } else {
            res = Boolean.FALSE;
        }
        isTradeDateCache.put(date, res);
        return res;
    }

    /**
     * 给定任一日期, 返回确定的 前 N 个交易日. 参数日期可以非交易日
     *
     * @param todayDate
     * @param n
     * @return
     * @throws SQLException
     */
    @TimeoutCache(timeout = "3600 * 1000")
    public static String getPreNTradeDateStrict(String todayDate, int n) throws SQLException {
        if (!isStdDatePattern(todayDate)) { // 匹配标准形式, 否则解析
            todayDate = DateUtil.parse(todayDate).toString(DatePattern.NORM_DATE_PATTERN); // 标准化
        }

        String cacheKey = StrUtil.format("{}__{}", todayDate, n);
        String res = preNTradeDateStrictCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        if (n == 0) { // 为0返回自身
            return todayDate;
        } else if (n > 0) { // >0表示前n个交易日! 以往逻辑.
            String sql = StrUtil
                    .format("select date from trade_dates where is_open=1 and date<='{}' order by date desc " +
                            "limit {}", todayDate, n + 2); // +1即可
            DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
            List<String> dates = DataFrameS.getColAsStringList(dataFrame, "date");

            int index = n - 1;
            if (dates.get(0).equals(todayDate)) {
                // 给定日期是交易日, 则索引需要 + 1
                index += 1;
            }
            try {
                return dates.get(index); // 本身就是倒序的, 且必然是交易日, 因此返回索引
            } catch (Exception e) {
                e.printStackTrace();
                return null; // 索引越界
            }
        } else { // n 为负数, 未来交易日!
            String sql = StrUtil
                    .format("select date from trade_dates where is_open=1 and date>='{}' order by date " +
                            "limit {}", todayDate, Math.abs(n) + 2); // +1即可
            DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
            List<String> dates = DataFrameS.getColAsStringList(dataFrame, "date");
            Console.log(dates);

            int index = Math.abs(n) - 1;
            if (dates.get(0).equals(todayDate)) {
                // 给定日期是交易日, 则索引需要 + 1
                index += 1;
            }
            try {
                return dates.get(index); // 本身就是倒序的, 且必然是交易日, 因此返回索引
            } catch (Exception e) {
                e.printStackTrace();
                return null; // 索引越界
            }
        }


    }


    /**
     * 资讯表: simple_new,
     * 获取某类型 最新(被保存)1条记录; 主要将着眼于 更早的记录是否被保存过.
     * 以 saveTime属性 判定 , 而非 dateTime 属性判定 !!!
     *
     * @param type
     * @return
     */
    public static List<SimpleNewEm> getLatestSaveBeanByType(int type, int limit) throws SQLException {
        String sql = StrUtil
                .format("select * from simple_new where type={} order by dateTime desc limit {} ", type, limit);
        return buildBeanListFromDfWithId(DataFrameS.readSql(connection, sql));
    }


    /*
    分时成交相关api: 因为单日一个数据表, 所以必然需要决定访问那一日的分时成交数据; 因分时成交数据包含指数,个股,板块, 使用 quoteId区分
     */

    /**
     * 标准api, 给定 日期(决定表名) 以及 quoteId 资产唯一标识, 获取某资产某一日的分时成交数据
     *
     * @param date
     * @param quoteId
     * @return
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteId(String date, String quoteId) {
        String sql = StrUtil.format("select * from `{}` where quoteId='{}'", date, quoteId);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connectionFsTrans, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return dataFrame;
    }

}
