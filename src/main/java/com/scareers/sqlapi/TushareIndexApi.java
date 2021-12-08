package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * description: tushare 指数相关的简单的 sql api
 * tushare 阉割了api, 该数据表指数日数据暂时只有 2大指数
 */
public class TushareIndexApi {
    // 不带conn的方法, 均使用次静态连接, 且不关闭次连接. 此连接一直存活.  需要显式调用静态方法 TushareApi.connClose()!
    public static Connection connLocalTushare;
    public static Log log = LogFactory.get();

    static {
        connLocalTushare = ConnectionFactory.getConnLocalTushare(); // 可选择连接是否从连接池获取.
    }

    public static Cache<String, Double> indexClosePriceOneDayCache = CacheUtil.newLRUCache(2048);
    public static Cache<String, HashMap<String, Double>> indexClosesPriceDateRangeCache = CacheUtil.newLRUCache(1024);


    public static void main(String[] args) throws Exception {
        TimeInterval interval = new TimeInterval();
        interval.start();
//        指数日线行情
        Console.log(getIndexDailyClosesByDateRange("000001.SH", Arrays.asList("20200101", "20210101")));
        Console.log(interval.intervalRestart());
        Console.log(getIndexDailyClosesByDateRange("000001.SH", Arrays.asList("20200101", "20210101")));
        Console.log(interval.intervalRestart());
        Console.log(getIndexDailyClosesByDateRange("000001.SH", Arrays.asList("20200101", "20210101")));
        Console.log(interval.intervalRestart());
        Console.log(getIndexDailyClosesByDateRange("000001.SH", Arrays.asList("20200101", "20210101")));
        Console.log(interval.intervalRestart());

        Console.log(getIndexDailyClosesByTradeDate("000001.SH", "20200106"));
        Console.log(interval.intervalRestart());

        Console.log(getIndexDailyClosesByTradeDate("000001.SH", "20200106"));
        Console.log(interval.intervalRestart());

        Console.log(getIndexDailyClosesByTradeDate("000001.SH", "20200106"));
        Console.log(interval.intervalRestart());
    }

    /**
     * 给定日期区间, 返回map.  key:value-> 日期:close价格
     *
     * @param dateRange
     * @param indexCode 目前只支持两大指数. 000001.SH  399001.SZ            @2021/12/8
     * @return
     */
    public static HashMap<String, Double> getIndexDailyClosesByDateRange(String indexCode, List<String> dateRange)
            throws SQLException {
        String cacheKey = StrUtil.format("{}__{}", indexCode, dateRange);
        HashMap<String, Double> res = indexClosesPriceDateRangeCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new HashMap<>();
        String sql = StrUtil.format("select trade_date,close\n" +
                "from sds_index_daily_tu_index sidti\n" +
                "where ts_code = '{}'\n" +
                "  and trade_date >= '{}'\n" +
                "  and trade_date < '{}'", indexCode, dateRange.get(0), dateRange.get(1));
        DataFrame<Object> dfTemp = DataFrame.readSql(connLocalTushare, sql);
        for (int i = 0; i < dfTemp.length(); i++) {
            List<Object> row = dfTemp.row(i);
            res.put(row.get(0).toString(), Double.valueOf(row.get(1).toString()));
        }
        indexClosesPriceDateRangeCache.put(cacheKey, res);
        return res;
    }

    /**
     * 指数单日 close
     *
     * @param indexCode
     * @param tradeDate
     * @return
     * @throws SQLException
     */
    public static Double getIndexDailyClosesByTradeDate(String indexCode, String tradeDate)
            throws SQLException {
        String cacheKey = StrUtil.format("{}__{}", indexCode, tradeDate);
        Double res = indexClosePriceOneDayCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        String sql = StrUtil.format("select close\n" +
                "from sds_index_daily_tu_index sidti\n" +
                "where ts_code = '{}'\n" +
                "  and trade_date = '{}'\n", indexCode, tradeDate);
        DataFrame<Object> dfTemp = DataFrame.readSql(connLocalTushare, sql);
        if (dfTemp.length() < 1) {
            return null;
        }
        res = Double.valueOf(dfTemp.get(0, 0).toString());
        indexClosePriceOneDayCache.put(cacheKey, res);
        return res;
    }

}