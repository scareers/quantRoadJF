package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * description: tushare 相关的简单的 sql api
 *
 * @author admin
 * noti:
 * 1. 使用reqdSql 获取到 df后, 若调用 df.convert() 尝试转换每列类型, 是比较消耗时间的. 因此一般api不调用. 实际逻辑决定是否调用
 * 2. 还是 conn 是否关闭的问题:
 * 对于某些, 无参数, 但是访问连接, 获取结果的函数, 因未传递 conn参数, 不需要考虑复用问题. 因此, 这里统一 从连接池获取, 最后关闭
 * 即 无 conn 参数的方法, 从连接池拿到conn, 理应关闭.  不考虑该对象复用问题
 */
public class TushareApi {
    public static final String STOCK_LIST_TABLENAME = "sds_stock_list_tu_stock";
    public static final String STOCK_NAMECHANGE_TABLENAME = "sds_stock_name_change_tu_stock";
    public static final String STOCK_PRICE_DAILY_TABLENAME_TEMPLATE = "sds_stock_price_daily_{}_tu_stock";
    public static final List<String> NOT_MAIN_BOARDS = Arrays.asList(null, "CDR", "创业板", "科创板");// 另有主板,中小板
    public static Cache<String, Object[]> stockPriceLimitMaxMinCache = CacheUtil.newLRUCache(32);

    public static void main(String[] args) throws Exception {
        TimeInterval interval = new TimeInterval();
        interval.start();
//        Console.log(getStockListFromTushare(false, false, true, NOT_MAIN_BOARDS).size());
//        Console.log(
//                getStockPriceByTscodeAndDaterangeAsDfFromTushare("000001.SZ", "nofq", null,
//                        Arrays.asList("20190101", "20200101"), null));
//        Console.log(getStockWithStDateRanges());

//        Console.log(getAdjdatesByTscodeFromTushare("000001.SZ", ConnectionFactory.getConnLocalTushareFromPool()));

//        Connection conn = ConnectionFactory.getConnLocalTushareFromPool();
//        Object[] res = getReachPriceLimitDates("999999.SZ");
//        Console.log((HashSet<String>) res[0]);
//        Console.log((HashSet<String>) res[1]);
//        Console.log((List<String>) res[2]);
//        Console.log(interval.intervalRestart());
//        Object[] res0 = getReachPriceLimitDates("000153.SZ");
//        Console.log((HashSet<String>) res0[0]);
//        Console.log((HashSet<String>) res0[1]);
//        Console.log((List<String>) res0[2]);
//        Console.log(interval.intervalRestart());

    }


    public static DataFrame<String> getStockListWithBoardFromTushare() throws Exception {
        String sql = StrUtil.format("select ts_code,market from {} order by ts_code", STOCK_LIST_TABLENAME);
        Connection connLocalTushareFromPool = ConnectionFactory.getConnLocalTushareFromPool();
        DataFrame<Object> res = DataFrame.readSql(connLocalTushareFromPool, sql);
        // python返回字典, 这里返回 df. 注意
        connLocalTushareFromPool.close();
        // 后面将放在ConcurrentHashMap, 要求不为null
        res = res.select(values -> values.get(0) != null && values.get(1) != null);
        return res.cast(String.class);
    }

    public static DataFrame<String> getStockListWithCnNameFromTushare() throws Exception {
        //        使用传统 sql查询获取
        //        ResultSet rs = execSqlQuery(sql_, ConnectionFactory.getConnLocalTushare());
        //        ArrayList<String> stockListTemp = new ArrayList<>();
        //        while (rs.next()) {
        //            stockListTemp.add(rs.getString("ts_code"));
        //        }
        //        Console.log(stockListTemp);
        //        Console.log(stockListTemp.size());
        String sql = StrUtil.format("select ts_code,name from {} order by ts_code", STOCK_LIST_TABLENAME);
        Connection conn = ConnectionFactory.getConnLocalTushareFromPool();
        DataFrame<String> res = DataFrame.readSql(conn, sql).cast(String.class);
        conn.close();
        return res;
    }

    public static List<String> getStockListFromTushare(boolean excludeCurrentSt, boolean excludeBj,
                                                       boolean excludeBoards, List<String> exboards) throws Exception {

        DataFrame<String> dfStockList = getStockListWithCnNameFromTushare();
        if (excludeCurrentSt) {
            dfStockList = dfStockList.select(value -> !value.get(1).contains("ST"));
        }

        if (excludeBj) {
            dfStockList = dfStockList.select(value -> !value.get(0).endsWith("BJ"));
        }

        if (excludeBoards && exboards != null) {
            DataFrame<String> dfWithBoard = getStockListWithBoardFromTushare();
            // 全部股票, 排除掉声明的 板
            dfWithBoard = dfWithBoard.select(value -> !exboards.contains(value.get(1)));

            DataFrame<String> finalDfWithBoard = dfWithBoard;
            dfStockList = dfStockList.select(value -> finalDfWithBoard.col(0).contains(value.get(0)));
        }
        return dfStockList.col(0);
    }

    public static List<String> getStockListFromTushareExNotMain() throws Exception {
        return getStockListFromTushare(false, true, true, NOT_MAIN_BOARDS);
    }

    public static DataFrame<Object> getStockPriceByTscodeAsDfFromTushare(String tsCode, String fq, List<String> fields)
            throws Exception {
        return getStockPriceByTscodeAndDaterangeAsDfFromTushare(tsCode, fq, fields, null, null);
    }

    /**
     * 特殊: 该函数传递了 conn;  原则上一般不关闭conn;  若conn参数为null, 则尝试从pool获取, 最后会关闭临时conn.
     *
     * @param tsCode
     * @param fq
     * @param fields
     * @param dateRange
     * @param conn
     * @return
     * @throws SQLException
     */
    public static DataFrame<Object> getStockPriceByTscodeAndDaterangeAsDfFromTushare(String tsCode, String fq,
                                                                                     List<String> fields,
                                                                                     List<String> dateRange,
                                                                                     Connection conn)
            throws SQLException {
        // 这是默认实现, 经常调用, 包前不包尾
        return getStockPriceByTscodeAndDaterangeAsDfFromTushare0(tsCode, fq, fields, dateRange, conn, true);
    }

    public static DataFrame<Object> getStockPriceByTscodeAndDaterangeAsDfFromTushare0(String tsCode, String fq,
                                                                                      List<String> fields,
                                                                                      List<String> dateRange,
                                                                                      Connection conn,
                                                                                      boolean excludeEndDate)
            throws SQLException {
        String tablename = StrUtil.format(STOCK_PRICE_DAILY_TABLENAME_TEMPLATE, fq);
        String fieldStr = null;
        if (fields == null) {
            fieldStr = "*";
        } else {
            fieldStr = StrUtil.join(",", fields);
        }
        if (dateRange == null) {
            // 默认全部
            dateRange = Arrays.asList("19000101", "21000101");
        }
        Connection connTemp;
        if (conn == null) {
            connTemp = ConnectionFactory.getConnLocalTushareFromPool();
        } else {
            connTemp = conn;
        }

        String sql = StrUtil.format("select {} from {} where ts_code='{}' "
                        + " and trade_date>='{}'"
                        + " and trade_date<'{}'"
                        + " order by trade_date",
                fieldStr, tablename, tsCode, dateRange.get(0), dateRange.get(1));
        if (!excludeEndDate) {
            sql = sql.replace("and trade_date<", "and trade_date<=");
        }
        DataFrame<Object> res = DataFrame.readSql(connTemp, sql);
        if (conn == null) {
            // 未传递conn, 则需要关闭掉 强行获取的连接
            connTemp.close();
        }
        return res;
    }

    public static HashMap<String, List<List<String>>> getStockWithStDateRanges() throws SQLException {
        String sql = StrUtil.format("select ts_code,start_date,end_date,change_reason\n" +
                "    from {}\n" +
                "    where (change_reason = 'ST' OR change_reason = '*ST')", STOCK_NAMECHANGE_TABLENAME);
        HashMap<String, List<List<String>>> stockWithStDateRanges = new HashMap<>();
        Connection connLocalTushareFromPool = ConnectionFactory.getConnLocalTushareFromPool();
        DataFrame<Object> df = DataFrame.readSql(connLocalTushareFromPool, sql);
        connLocalTushareFromPool.close();
        for (int i = 0; i < df.length(); i++) {
            List<Object> row = df.row(i);
            String stock = (String) row.get(0);
            List<String> singleDateRange = null;
            if (row.get(2) == null) {
                singleDateRange = Arrays.asList((String) row.get(1), "21000101");
            } else {
                singleDateRange = Arrays.asList((String) row.get(1), (String) row.get(2));
            }

            stockWithStDateRanges.computeIfAbsent(stock, k -> new ArrayList<List<String>>());
            stockWithStDateRanges.get(stock).add(singleDateRange);
        }
        return stockWithStDateRanges;
    }

    public static HashSet<String> getAdjdatesByTscodeFromTushare(String stock, Connection conn) throws SQLException {
        String sqlGetAdjFactors = StrUtil.format("select trade_date,adj_factor from sds_stock_adj_factor_tu_stock " +
                "where ts_code='{}' " +
                "order by trade_date", stock);
        DataFrame<Object> dfAdjFactors = DataFrame.readSql(conn, sqlGetAdjFactors);
        //Console.log(dfAdjFactors.types()); // 默认全部是String
        dfAdjFactors = dfAdjFactors.convert(String.class, Double.class);
        HashSet<String> res = new HashSet<>();
//        Console.log(dfAdjFactors);
        if (dfAdjFactors.length() <= 1) {
            return res;
            // 只有一天的记录, 则无视掉
        }
        Double factor_pre = (Double) dfAdjFactors.row(0).get(1);
        for (int i = 1; i < dfAdjFactors.length(); i++) {// 注意第一天
            List<Object> row = dfAdjFactors.row(i);
            String date = (String) row.get(0);
            Double factor = (Double) row.get(1);
            if (!factor.equals(factor_pre)) {
                res.add(date);
            }
            factor_pre = factor;
        }
        return res;
    }

    /**
     * 计算某只股票的所有涨停日期和跌停日期, 得到集合. 并计算对应的 有效的 计算 日期区间
     * -- 该函数没传递conn, 手动获取/关闭conn
     * <p>
     * Console.log((HashSet<String>) res[0]);
     * Console.log((HashSet<String>) res[1]);
     * Console.log((List<String>) res[2]);
     *
     * @param stock
     * @return Object[], 元素0为HashSet<String>,保存涨停日期, 1为跌停日期集合. 2 为本函数有效的计算日期区间,可返回null
     */
    @Cached(description = "缓存了股票涨跌停日期集合")
    public static Object[] getReachPriceLimitDates(String stock) throws SQLException {
        Object[] resCached = stockPriceLimitMaxMinCache.get(stock);
        if (resCached != null) {
            return resCached;
        }

        String sqlGetPriceLimit = StrUtil.format("select trade_date, up_limit, down_limit " +
                "        from sds_stock_pricelimit_tu_stock " +
                "    where ts_code = '{}' order by trade_date", stock);
        Connection conn = ConnectionFactory.getConnLocalTushareFromPool();
        DataFrame<Object> dfPriceLimitPerStock = DataFrame.readSql(conn, sqlGetPriceLimit);

        DataFrame<Object> dfDailyPriceNofqAll = getStockPriceByTscodeAndDaterangeAsDfFromTushare(stock, "nofq",
                Arrays.asList("trade_date", "close"), null, conn);
        conn.close(); // 已经用完, 归还连接池
        HashSet<String> datesSetOfPriceLimitMax = new HashSet<>();
        HashSet<String> datesSetOfPriceLimitMin = new HashSet<>();
        List<String> effectiveCalcDateRange = null;
        if (dfPriceLimitPerStock.length() <= 5) {
            Object[] res = {datesSetOfPriceLimitMax, datesSetOfPriceLimitMin, effectiveCalcDateRange};
            stockPriceLimitMaxMinCache.put(stock, res, DateUnit.HOUR.getMillis() * 3);
            return res;
        }
        // 少数情况下, 前5天包含了发行价, 这里去掉, 比如科创板.
        //    相应的, 涨跌停判定日期区间, 也减少这5天
        dfPriceLimitPerStock = dfPriceLimitPerStock.slice(5, dfPriceLimitPerStock.length());
        if (dfPriceLimitPerStock.length() > 0 && dfDailyPriceNofqAll.length() > 0) {
            DataFrame<Object> dfJoined = dfPriceLimitPerStock
                    .joinOn(dfDailyPriceNofqAll, DataFrame.JoinType.INNER, "trade_date");
            dfJoined = dfJoined.dropna();
            dfJoined.convert(String.class, Double.class, Double.class, String.class, Double.class);
            //            trade_date_left	    up_limit	  down_limit	  trade_date_right	close 5列,注意索引
//            Console.log(dfJoined);
            for (int i = 0; i < dfJoined.length(); i++) {
                // 4列依次 trade_date, up_limit,down_limit,close. 注意下标
                if ((Double) dfJoined.get(i, 1) <= (Double) dfJoined.get(i, 4)) {
                    datesSetOfPriceLimitMax.add((String) dfJoined.get(i, 0));
                }
                if ((Double) dfJoined.get(i, 2) >= (Double) dfJoined.get(i, 4)) {
                    datesSetOfPriceLimitMin.add((String) dfJoined.get(i, 0));
                }
            }
            effectiveCalcDateRange = Arrays.<String>asList((String) dfJoined.get(0, 0),
                    (String) dfJoined.get(dfJoined.length() - 1, 0));
            Object[] res = {datesSetOfPriceLimitMax, datesSetOfPriceLimitMin, effectiveCalcDateRange};
            stockPriceLimitMaxMinCache.put(stock, res, DateUnit.HOUR.getMillis() * 3);
            return res;
        }
        Object[] res = {datesSetOfPriceLimitMax, datesSetOfPriceLimitMin, effectiveCalcDateRange};
        stockPriceLimitMaxMinCache.put(stock, res, DateUnit.HOUR.getMillis() * 3);
        return res;
    }
}
