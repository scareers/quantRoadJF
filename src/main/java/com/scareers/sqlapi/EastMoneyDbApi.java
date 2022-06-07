package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.annotations.Cached;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.ths.simulation.rabbitmq.ProducerSimple;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static com.scareers.tools.stockplan.news.bean.SimpleNewEm.buildBeanListFromDfWithId;

/**
 * description: 东财本地数据库api
 *
 * @author: admin
 * @date: 2022/3/5/005-11:25:35
 */
public class EastMoneyDbApi {
    public static Connection connection = ConnectionFactory.getConnLocalEastmoney();
    public static Connection connectionFsTrans = ConnectionFactory.getConnLocalFSTransactionFromEastmoney();
    public static Connection connectionFs1M = ConnectionFactory.getConnLocalFS1MFromEastmoney();
    private static Cache<String, Boolean> isTradeDateCache = CacheUtil.newLRUCache(2048);
    private static Pattern stdDatePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}"); // 标准的日期表达式
    public static Cache<String, String> preNTradeDateStrictCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 某个日期的上n个交易日?
    private static Cache<String, HashSet<String>> allConceptNameByDateCache = CacheUtil.newLRUCache(256, 60);
    private static Cache<String, HashMap<String, Double>> allPreCloseByDateCache = CacheUtil.newLRUCache(32);
    private static Cache<String, DataFrame<Object>> fsTransByDateAndQuoteIdXCache = CacheUtil
            .newLRUCache(1024); // 字段顺序同爬虫
    private static Cache<String, DataFrame<Object>> fsTransByDateAndQuoteIdRawCache = CacheUtil
            .newLRUCache(1024); // 字段顺序数据表原始
    private static Cache<String, DataFrame<Object>> fs1MV2ByDateAndQuoteIdRawCache = CacheUtil
            .newLRUCache(1024); // 字段顺序数据表原始

    public static void main(String[] args) throws Exception {


//        Console.log(isTradeDate("20220304"));

//        Console.log(getLatestSaveBeanByType(1, 10));
//        List<SimpleNewEm> latestSaveBeanByType = getLatestSaveBeanByType(1, 10);
//        for (SimpleNewEm simpleNewEm : latestSaveBeanByType) {
//            Console.log(simpleNewEm);
//        }

//        Console.log(getPreNTradeDateStrict(DateUtil.today(), 3));
//        Console.log(getPreNTradeDateStrict("20220318", -2));

//        HashSet<String> allBkNameByDate = getAllBkNameByDateRange("2022-05-10", "2022-05-16");
//        Console.log(allBkNameByDate);
//        Console.log(allBkNameByDate.size());

//        Console.log(getBondRecordAmountByDateStr("2022-06-02"));
//        Console.log(getAllPreCloseByDate("2022-06-02"));

//        TimeInterval timer = DateUtil.timer();
//        timer.start();
//        DataFrame<Object> fsTransByDateAndQuoteId = getFsTransByDateAndQuoteId("2022-06-06", "0.000001");
//        Console.log(timer.intervalRestart());
//        getFsTransByDateAndQuoteId("2022-06-06", "0.000001");
//        Console.log(timer.intervalRestart());
//        getFsTransByDateAndQuoteId("2022-06-06", "0.000001");
//        Console.log(timer.intervalRestart());
//        getFsTransByDateAndQuoteId("2022-06-06", "0.000001");
//        Console.log(timer.intervalRestart());

        loadFs1MAndFsTransDataToCache(SecurityBeanEm.createBondList(Arrays.asList("小康转债", "卡倍转债"), false),
                "2022-06-02");
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
    @SneakyThrows
    public static String getPreNTradeDateStrict(String todayDate, int n) {
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

    /*
    转债列表
     */

    /**
     * 给定日期字符串, 返回当日有多少条记录在数据库; 如果>0表示爬虫爬过, 不再重复
     *
     * @param dateStr
     * @return
     */
    public static Integer getBondRecordAmountByDateStr(String dateStr) {
        String sql = StrUtil.format("select count(*) from bond_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return Integer.parseInt(dataFrame.get(0, 0).toString());
    }

    /**
     * 给定日期字符串, 返回有记录的, 所有转债 代码名称! 可能null
     *
     * @param dateStr
     * @return
     */
    public static List<String> getAllBondCodeByDateStr(String dateStr) {
        String sql = StrUtil.format("select secCode from bond_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        // 主动去重, 不使用hs, 保持顺序
        List<String> secCodes = DataFrameS.getColAsStringList(dataFrame, "secCode");
        List<String> res = new ArrayList<>();
        HashSet<String> forOrder = new HashSet<>();
        for (String secCode : secCodes) {
            if (!forOrder.contains(secCode)) {
                res.add(secCode); // 达成去重
            }
            forOrder.add(secCode);
        }
        return res;
    }

    //    select count(*)
//    from `2022-06-06` `2022-06-062`
//    where date = '2022-06-06 15:00';

    /**
     * 给定日期, 获取全资产收盘价 Map; 返回值 key为 quoteId
     * 从 1分钟fs图数据库获取
     *
     * @param dateStr
     * @return
     */
    @Cached
    public static HashMap<String, Double> getAllPreCloseByDate(String dateStr) {
        String sql = StrUtil.format("select quoteId,close from `{}` where date = '{} 15:00'", dateStr, dateStr);
        DataFrame<Object> dataFrame = null;

        HashMap<String, Double> res = allPreCloseByDateCache.get(dateStr);
        if (res != null) {
            return res;
        } else {
            res = new HashMap<>(); // 初始化
        }
        try {
            dataFrame = DataFrame.readSql(connectionFs1M, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        if (dataFrame != null) {
            for (int i = 0; i < dataFrame.length(); i++) {
                Object quoteId = dataFrame.get(i, 0);
                Object close = dataFrame.get(i, 1);

                try {
                    res.put(quoteId.toString(), Double.valueOf(close.toString()));
                } catch (NumberFormatException e) {

                }
            }
        }
        allPreCloseByDateCache.put(dateStr, res);
        return res;
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
     * @key3 : 为了保证与爬虫的df 数据列顺序一样, 这里显式固定了返回的列顺序
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteIdS(String date, String quoteId) {
        return getFsTransByDateAndQuoteIdS(date, quoteId, false);
    }

    /**
     * 标准api, 给定 日期(决定表名) 以及 quoteId 资产唯一标识, 获取某资产某一日的分时成交数据
     * 可排除早盘竞价!
     *
     * @param date
     * @param quoteId
     * @return
     * @key3 : 为了保证与爬虫的df 数据列顺序一样, 这里显式固定了返回的列顺序
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteIdS(String date, String quoteId, boolean excludeBid) {
        String cacheKey = StrUtil.format("{}__{}__{}", date, quoteId, excludeBid);
        DataFrame<Object> res = fsTransByDateAndQuoteIdXCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String sql = StrUtil.format("select sec_code,market,time_tick,price,vol,bs,id,secName,quoteId from `{}` where" +
                " quoteId='{}'", date, quoteId);
        if (excludeBid) {
            sql = StrUtil
                    .format("select sec_code,market,time_tick,price,vol,bs,id,secName,quoteId from `{}` where quoteId='{}' and time_tick>='09:30:00'",
                            date, quoteId);
        }
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connectionFsTrans, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        fsTransByDateAndQuoteIdXCache.put(cacheKey, dataFrame);
        return dataFrame;
    }

    /**
     * @key3 : 数据库中原序
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteId(String date, String quoteId) {
        return getFsTransByDateAndQuoteId(date, quoteId, false);
    }

    /**
     * @key3 : 数据库中原序
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteId(String date, String quoteId, boolean excludeBid) {
        String cacheKey = StrUtil.format("{}__{}__{}", date, quoteId, excludeBid);
        DataFrame<Object> res = fsTransByDateAndQuoteIdRawCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String sql = StrUtil.format("select * from `{}` where" +
                " quoteId='{}'", date, quoteId);
        if (excludeBid) {
            sql = StrUtil
                    .format("select * from `{}` where quoteId='{}' and time_tick>='09:30:00'",
                            date, quoteId);
        }
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connectionFsTrans, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        fsTransByDateAndQuoteIdRawCache.put(cacheKey, dataFrame);
        return dataFrame;
    }

    /**
     * date仅仅限制表名称!!!  分时1分钟, 普通版本
     *
     * @param date
     * @param quoteId
     * @return
     */
    public static DataFrame<Object> getFs1MByDateAndQuoteId(String date, String quoteId) {
        String sql = StrUtil.format("select * from `{}` where quoteId='{}'", date, quoteId);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connectionFs1M, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return dataFrame;
    }

    /**
     * date仅仅限制表名称!!!  分时1分钟, 普通版本
     *
     * @param date
     * @param quoteId
     * @return
     */
    public static DataFrame<Object> getFs1MV2ByDateAndQuoteId(String date, String quoteId) {
        String cacheKey = date + quoteId;
        DataFrame<Object> res = fs1MV2ByDateAndQuoteIdRawCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String sql = StrUtil.format("select * from `{}` where quoteId='{}'", date + "_v2", quoteId);
        DataFrame<Object> dataFrame;
        // fs1MV2ByDateAndQuoteIdRawCache
        try {
            dataFrame = DataFrame.readSql(connectionFs1M, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        fs1MV2ByDateAndQuoteIdRawCache.put(cacheKey, res);
        return dataFrame;
    }

    /**
     * 载入 FS1Mv2 和 两类 fs成交数据 到缓存! 耗时可能较久; 建议异步调用
     */
    public static volatile boolean loading = true; // 专门适配的flag ; 类似加锁执行效果, 且多线程不阻塞

    public static void loadFs1MAndFsTransDataToCache(List<SecurityBeanEm> beanList, String dateStr) {
        if (loading) {
            return; // 正在载入
        }
        loading = true; // 载入
        try {
            for (SecurityBeanEm beanEm : beanList) {
                EastMoneyDbApi.getFsTransByDateAndQuoteId(dateStr, beanEm.getQuoteId());
                EastMoneyDbApi.getFsTransByDateAndQuoteIdS(dateStr, beanEm.getQuoteId());
                EastMoneyDbApi.getFs1MV2ByDateAndQuoteId(dateStr, beanEm.getQuoteId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 给定时间字符串区间 (前包后不包), 读取爬虫记录的概念列表,     标准: 2022-03-06 18:28:10 , 因大小判定,可只要年月日
     * getAllBkNameByDate("2022-05-10", "2022-05-16");
     * 因只有 self_record_time 字段, 访问所有该字段位于 时间区间的;
     * 返回 全部板块名称集合
     * 简而言之因为爬虫运行时间不确定, 导致概念列表可能稍有延迟. 不够精确, 勉强能用
     */
    @TimeoutCache
    public static HashSet<String> getAllBkNameByDateRange(String timeStart, String timeEnd) {
        HashSet<String> res = allConceptNameByDateCache.get(timeStart + timeEnd);
        if (res != null) {
            return res;
        }
        String sql = StrUtil.format("select name from bk_list where " +
                "self_record_time>='{}' and self_record_time<'{}'", timeStart, timeEnd);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        List<String> conceptCol = DataFrameS.getColAsStringList(dataFrame, "name");
        // json 列表字符串 解析, 保存时保存的json

        res = new HashSet<>(conceptCol);
        allConceptNameByDateCache.put(timeStart + timeEnd, res);
        return res;
    }
}
