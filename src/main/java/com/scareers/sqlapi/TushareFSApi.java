package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description: 分时图,tushare 1分钟 数据 相关的api.
 * 0.因分时图可能保存在不同的数据库, 因此两大方法的 conn对象, 采用 传递方式; 且不负责关闭
 * 而不像TushareApi, 静态属性维护一个conn;
 *
 * @author: admin
 * @date: 2021/11/5  0005-3:04
 * <p>
 */
public class TushareFSApi {

    public static final String STR_DONSNOT_EXIST = "doesn't exist";
    public static final List<String> FS_ALL_FIELDS = Arrays.asList("trade_time", "open", "close", "high", "low", "vol",
            "amount");
    // using cache for one day fs1m of stock.
    // the first time: conn object init, so == 500ms;  normal first ==30ms, using cache==10ms
    public static Cache<String, DataFrame<Object>> stockPriceFsOneDayCache = CacheUtil.newLRUCache(32);
    public static Cache<String, DataFrame<Object>> stockPriceFsDateRangeCache = CacheUtil.newLRUCache(32);

    public static void main(String[] args) throws Exception {
        TimeInterval interval = new TimeInterval();
        interval.start();
//        getFs1mStockPriceOndDayAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(), "000001.SZ",
//                "20180130", null);
//        Console.log(interval.intervalRestart());
//        getFs1mStockPriceOndDayAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(), "000001.SZ",
//                "20180131", null);
//
//        Console.log(interval.intervalRestart());
//        getFs1mStockPriceOndDayAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(), "000001.SZ",
//                "20180131", null);
//
//        Console.log(interval.intervalRestart());
//
//
//        getFs1mStockPriceByDateRangeAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(),
//                "000001.SZ", "20181030",
//                "20190230", true, null);
//        Console.log(interval.intervalRestart());
//        getFs1mStockPriceByDateRangeAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(), "000001.SZ",
//                "20181130",
//                "20190230", true, null);
//        Console.log(interval.intervalRestart());
//        getFs1mStockPriceByDateRangeAsDfFromTushare(ConnectionFactory.getConnLocalTushare1MFromPool(), "000001.SZ",
//                "20181130",
//                "20190230", true, null);
//        Console.log(interval.intervalRestart());

    }

    @Cached(description = "warning: do not use conn arg build cacheKey! No use difference conn")
    public static DataFrame<Object> getFs1mStockPriceOndDayAsDfFromTushare(Connection conn, String tsCode,
                                                                           String whichDate, List<String> fields)
            throws Exception {
        if (fields == null) {
            fields = FS_ALL_FIELDS;
        }
        String cacheKey = tsCode + "_" + whichDate + "_" + String.join(",", fields);
        DataFrame<Object> res = stockPriceFsOneDayCache.get(cacheKey);

        if (res != null) {
            // 尝试读取缓存, 直接返回.  the python @lru_cache() has no time.  hutool cache  7ms
            return res;
        }

        String belongTablename = calcDateBelongToFsTablename(tsCode, whichDate);
        // 默认就是 2021-01-01 形式.  这里将参数 20210101 转换. 解析为DateTime, 恰好是 Date 子类
        // @noti: 因为DateUtil.formatDate 它会对 20190230 这种不正确的日期进行符合逻辑 的 等价转换为 2019-03-02
        // 因此, 这里 不使用这种实现, 使得 可以传递 不合逻辑的 日期.   仅仅单纯使用 字符串 - 的拼接
        String stdDateStr = StrUtil.sub(whichDate, 0, 4) + "-"
                + StrUtil.sub(whichDate, 4, 6) + "-"
                + StrUtil.sub(whichDate, 6, 8);

        try {
            res = DataFrame.readSql(conn, StrUtil.format(
                    "select {} from {} where trade_time>='{}' and trade_time<='{}' order by trade_time",
                    String.join(",", fields), belongTablename, stdDateStr + " 09:30:00",
                    stdDateStr + " 15:00:00"));
        } catch (Exception e) {
            e.printStackTrace();
            if (!e.getMessage().contains(STR_DONSNOT_EXIST)) {
                // 如果不是 数据表不存在的异常, 则抛出异常. 如果是数据表不存在, 则 返回null
                throw e;
            }
        }
        if (res == null) {
            return null;
        } else {
            stockPriceFsOneDayCache.put(cacheKey, res, DateUnit.HOUR.getMillis() * 3);
            return res;
        }
    }

    public static String calcDateBelongToFsTablename(String tsCode, String whichDate) {
        String year = StrUtil.sub(whichDate, 0, 4);
        return StrUtil.format("fs_1m_{}_{}_{}", tsCode, year + "0101", (Integer.parseInt(year) + 1) + "0101")
                .toLowerCase().replace('.', '_');
    }

    public static ArrayList<ArrayList<String>> calcStdDataRangesAccordingFetchDateRange(String startDate,
                                                                                        String endDate) {
        ArrayList<ArrayList<String>> dateRanges = new ArrayList<>();
        int startYear = Integer.parseInt(StrUtil.sub(startDate, 0, 4));
        int endYear = Integer.parseInt(StrUtil.sub(endDate, 0, 4));

        for (int year = startYear; year < endYear + 1; year++) {
            ArrayList<String> dr = new ArrayList<>();
            dr.add(year + "0101");
            dr.add((year + 1) + "0101");
            dateRanges.add(dr);
        }
        return dateRanges;
    }

    @Cached(description = "已缓存. 注意对 conn参数没有包含在缓存key, 使用不同的conn可能返回相同结果,造成bug;")
    public static DataFrame<Object> getFs1mStockPriceByDateRangeAsDfFromTushare(Connection conn,
                                                                                String tsCode, String startDate,
                                                                                String endDate,
                                                                                boolean includeEndDate,
                                                                                List<String> fields)
            throws SQLException {

        if (fields == null) {
            fields = FS_ALL_FIELDS;
        }
        String cacheKey = tsCode + "_" + startDate + "_" + endDate + "_"
                + String.join(",", fields) + "_" + includeEndDate;
        DataFrame<Object> res = stockPriceFsDateRangeCache.get(cacheKey);
        if (res != null) {
            // 尝试读取缓存, 直接返回.  the python @lru_cache() has no time.  hutool cache  7ms
            return res;
        }
        if (!includeEndDate) {
            endDate = (Integer.parseInt(endDate) - 1) + "";
        }
        // @noti: 因为DateUtil.formatDate 它会对 20190230 这种不正确的日期进行符合逻辑 的 等价转换为 2019-03-02
        // 因此, 这里 不使用这种实现, 使得 可以传递 不合逻辑的 日期.   仅仅单纯使用 字符串 - 的拼接
        //        String earlyestTick = DateUtil.formatDate(DateUtil.parse(startDate)) + " 09:30:00";
        String earlyestTick = StrUtil.sub(startDate, 0, 4) + "-"
                + StrUtil.sub(startDate, 4, 6) + "-"
                + StrUtil.sub(startDate, 6, 8)
                + " 09:30:00";
        String oldestTick = StrUtil.sub(endDate, 0, 4) + "-"
                + StrUtil.sub(endDate, 4, 6) + "-"
                + StrUtil.sub(endDate, 6, 8) + " 15:00:00";
        DataFrame<Object> resTotal = null;
        ArrayList<ArrayList<String>> dateRanges = calcStdDataRangesAccordingFetchDateRange(startDate, endDate);
        for (ArrayList<String> dateRange : dateRanges) {
            String startDateTemp = dateRange.get(0);
            String endDateTemp = dateRange.get(1);
            String tablename = StrUtil.format("fs_1m_{}_{}_{}", tsCode, startDateTemp, endDateTemp).toLowerCase()
                    .replace('.', '_');
            DataFrame<Object> resTemp = null;
            try {
                resTemp = DataFrame.readSql(conn, StrUtil.format(
                        "select {} from {} where trade_time>='{}' and trade_time<='{}' order by trade_time",
                        String.join(",", fields), tablename, earlyestTick, oldestTick));
            } catch (Exception e) {
                e.printStackTrace();
                if (!e.getMessage().contains(STR_DONSNOT_EXIST)) {
                    // 如果不是 数据表不存在的异常, 则抛出异常. 如果是数据表不存在, 则 返回null
                    throw e;
                }
            }
            if (resTemp == null) {
                continue;
            }
            if (resTotal == null) {
                // 第一次访问到数据, 则直接复制. 后面次则拼接
                resTotal = resTemp;
            } else {
                // @noti: 同样, 注意拼接后,要 赋值!!!!!!!!
                resTotal = resTotal.concat(resTemp);
            }
        }
        if (resTotal == null) {
            return null;
        } else {
            stockPriceFsDateRangeCache.put(cacheKey, resTotal, DateUnit.HOUR.getMillis() * 3);
            return resTotal;
        }
    }
}
