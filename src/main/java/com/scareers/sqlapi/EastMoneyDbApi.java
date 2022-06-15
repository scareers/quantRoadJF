package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.annotations.Cached;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.kLineAmountHope;
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
    // 使用lru缓存,载入后, 首次读取, 将会比较慢; 影响gui体验; 这里直接使用hashMap, 但无法限制容量, 注意!
    private static Cache<String, DataFrame<Object>> fsTransByDateAndQuoteIdXCache = CacheUtil
            .newLRUCache(1024); // 字段顺序同爬虫
    private static Cache<String, DataFrame<Object>> fsTransByDateAndQuoteIdXAdaptedCache = CacheUtil
            .newLRUCache(2048); // 字段顺序同爬虫
    private static Cache<String, DataFrame<Object>> fsTransByDateAndQuoteIdRawCache = CacheUtil
            .newLRUCache(1024); // 字段顺序数据表原始
    private static Cache<String, DataFrame<Object>> fs1MV2ByDateAndQuoteIdRawCache = CacheUtil
            .newLRUCache(1024); // 字段顺序数据表原始
    private static Cache<String, DataFrame<Object>> fs1MV2ByDateAndQuoteIdAdaptedCache = CacheUtil
            .newLRUCache(2048); // 字段顺序数据表原始
    private static Cache<String, Double> fs1MV2PreCloseCache = CacheUtil
            .newLRUCache(2048); // 字段顺序数据表原始

    private static Cache<String, DataFrame<Object>> BkDailyKlineAllOfOneDayCache = CacheUtil
            .newLRUCache(256); // 单日所有板块, 日k线数据!

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

//        List<String> x = getNewestHotEmPcNewTitleSet(20);
//        Console.log(x);

//        loadFs1MAndFsTransAndKLineDataToCache(SecurityBeanEm.createBondList(Arrays.asList("小康转债", "卡倍转债"), false),
//                "2022-06-02");
        DataFrame<Object> x = getBkDailyKlineAllOfOneDay("2022-06-14");
        Console.log(x.length());
        Console.log(x);
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
    @Cached
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
        try {
            res = DataFrame.readSql(connectionFsTrans, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        if (res != null) {
            fsTransByDateAndQuoteIdXCache.put(cacheKey, res);
        }
        return res;
    }

    /**
     * 分时成交数据, 适配一下preClose, 使得不同资产可依据涨跌幅, 放进一个chart里面!!, 列顺序同爬虫数据列
     *
     * @param date
     * @param quoteId
     * @param selfPreClose  分时成交不自带自身的昨收, 因此需要传递!
     * @param adaptPreClose 适配的目标昨收; 例如转债为主的chart图表, 将指数价格转换放进去, 则需要将指数价格转换, 算法简单
     * @return
     * @key3 : 因为转债有太多只了, 如果将结果缓存, 则指数对于多只转债, 结果太多了, 不缓存; 每次均读取原始数据, 重新计算
     */
    public static DataFrame<Object> getFsTransByDateAndQuoteIdSAdapted(String date, String quoteId,
                                                                       boolean excludeBid, double selfPreClose,
                                                                       double adaptPreClose) {
        //        // fsTransByDateAndQuoteIdXAdaptedCache
        String cacheKey = StrUtil.format("{}_{}_{}_{}_{}", date, quoteId, excludeBid, selfPreClose, adaptPreClose);
        DataFrame<Object> res = fsTransByDateAndQuoteIdXAdaptedCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = DataFrameS.copy(getFsTransByDateAndQuoteIdS(date, quoteId, excludeBid)); // 复制了
        if (res != null) { // 其他列不变, 只需要改变 price列; 索引是 3;
            for (int i = 0; i < res.length(); i++) {
                Double price = Double.valueOf(res.get(i, "price").toString());
                res.set(i, "price", price / selfPreClose * adaptPreClose);
            }
            fsTransByDateAndQuoteIdXAdaptedCache.put(cacheKey, res);
        }
        return res;
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
        try {
            res = DataFrame.readSql(connectionFsTrans, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        if (res != null) {
            fsTransByDateAndQuoteIdRawCache.put(cacheKey, res);
        }
        return res;
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
        // fs1MV2ByDateAndQuoteIdRawCache
        try {
            res = DataFrame.readSql(connectionFs1M, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        if (res != null) {
            fs1MV2ByDateAndQuoteIdRawCache.put(cacheKey, res);
        }
        return res;
    }

    /**
     * 分时1分钟v2, 自带有自身preClose, 所以只需要提供 需要适配的价格即可; 仅仅适配close价格!
     *
     * @param date
     * @param quoteId
     * @return
     */
    public static DataFrame<Object> getFs1MV2ByDateAndQuoteIdAdaptedOnlyClose(String date, String quoteId,
                                                                              double adaptPreClose) {
        String cacheKey = StrUtil.format("{}_{}_{}", date, quoteId, adaptPreClose);
        DataFrame<Object> res = fs1MV2ByDateAndQuoteIdAdaptedCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        res = DataFrameS.copy(getFs1MV2ByDateAndQuoteId(date, quoteId));
        if (res != null) {
            Double selfPreClose = Double.valueOf(res.get(0, "preClose").toString());
            for (int i = 0; i < res.length(); i++) {
                Double price = Double.valueOf(res.get(i, "close").toString());
                res.set(i, "close", price / selfPreClose * adaptPreClose);
            }
            fs1MV2ByDateAndQuoteIdAdaptedCache.put(cacheKey, res);
        }
        return res;
    }

    /**
     * 给定日期 和 quoteId, 查询 preClose; 从 fs1m v2表中, 访问 preClose列!!, limit1即可
     *
     * @return
     */
    public static Double getPreCloseOf(String dateStr, String quoteId) {
        String cacheKey = dateStr + quoteId;
        Double res = fs1MV2PreCloseCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String sql = StrUtil.format("select preClose from `{}` where quoteId='{}' limit 1", dateStr + "_v2", quoteId);
        DataFrame<Object> dfTemp = null;
        try {
            dfTemp = DataFrame.readSql(connectionFs1M, sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (dfTemp == null) {
            return null;
        }
        try {
            res = Double.valueOf(dfTemp.get(0, 0).toString());
        } catch (NumberFormatException e) {

        }
        if (res != null) {
            fs1MV2PreCloseCache.put(cacheKey, res);
        }
        return res; // 依然可能null
    }

    /**
     * 载入 FS1Mv2 和 两类 fs成交数据 到缓存! 耗时可能较久; 建议异步调用; 给定转债bean, 会自动获取对应大指数和股票bean, 载入数据
     */
    public static volatile boolean loading = false; // 专门适配的flag ; 类似加锁执行效果, 且多线程不阻塞

    public static void loadFs1MAndFsTransAndKLineDataToCache(List<SecurityBeanEm> beanList, String dateStr) {
        if (loading) {
            CommonUtil.notifyCommon("分时数据载入缓存: 正在载入中, 不可重复载入");
            return; // 正在载入
        }
        loading = true; // 载入
        try {
            for (SecurityBeanEm beanEm : beanList) {
                DataFrame<Object> dfTemp = EastMoneyDbApi
                        .getFsTransByDateAndQuoteId(dateStr, beanEm.getQuoteId());
                dfTemp = EastMoneyDbApi.getFsTransByDateAndQuoteIdS(dateStr, beanEm.getQuoteId());
                dfTemp = EastMoneyDbApi.getFs1MV2ByDateAndQuoteId(dateStr, beanEm.getQuoteId());
            }
            // 正股和两指数载入! -->
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondUtil.flushBondToStockAndIndexMap(); // 其他线程已经启动,则直接完成, 不消耗太多时间!
                }
            }, true);
            CommonUtil.waitUtil(new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    return BondUtil.bondToStockBeanMap.size() > 300 && !BondUtil.flushingMap; // 有结果且刷新完成
                }
            }, Integer.MAX_VALUE, 1, "全转债-股票映射刷新完毕", true);
            // 载入缓存 正股指数 fs 和fs成交  (已适配价格)
            for (SecurityBeanEm beanEm : beanList) {
                DataFrame<Object> fs1M = EastMoneyDbApi
                        .getFs1MV2ByDateAndQuoteId(dateStr, beanEm.getQuoteId());
                if (fs1M == null) {
                    continue;
                }


                SecurityBeanEm stock = BondUtil.getStockBeanByBond(beanEm);
                SecurityBeanEm index = BondUtil.getIndexBeanByBond(beanEm);

                Double preCloseOfBond = null;
                try {
                    preCloseOfBond = Double.valueOf(fs1M.get(0, "preClose").toString());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (preCloseOfBond == null) {
                    continue;
                }

                if (stock != null) {
                    EastMoneyDbApi.getFs1MV2ByDateAndQuoteIdAdaptedOnlyClose(dateStr, stock.getQuoteId(),
                            preCloseOfBond);

                    DataFrame<Object> stockFs1MRawDf = EastMoneyDbApi
                            .getFs1MV2ByDateAndQuoteId(dateStr, stock.getQuoteId());
                    if (stockFs1MRawDf != null && stockFs1MRawDf.length() > 0) {
                        Double preCloseOfStock = null;
                        try {
                            preCloseOfStock = Double.valueOf(stockFs1MRawDf.get(0, "preClose").toString());
                        } catch (NumberFormatException e) {

                        }
                        if (preCloseOfStock != null) {
                            EastMoneyDbApi.getFsTransByDateAndQuoteIdSAdapted(dateStr, stock.getQuoteId(), false,
                                    preCloseOfStock, preCloseOfBond);
                        }
                    }

                }
                if (index != null) {
                    EastMoneyDbApi.getFs1MV2ByDateAndQuoteIdAdaptedOnlyClose(dateStr, index.getQuoteId(),
                            preCloseOfBond); // fs1m载入

                    DataFrame<Object> indexFs1MRawDf = EastMoneyDbApi
                            .getFs1MV2ByDateAndQuoteId(dateStr, index.getQuoteId());
                    if (indexFs1MRawDf != null && indexFs1MRawDf.length() > 0) {
                        Double preCloseOfIndex = null;
                        try {
                            preCloseOfIndex = Double.valueOf(indexFs1MRawDf.get(0, "preClose").toString());
                        } catch (NumberFormatException e) {

                        }
                        if (preCloseOfIndex != null) {
                            EastMoneyDbApi.getFsTransByDateAndQuoteIdSAdapted(dateStr, index.getQuoteId(), false,
                                    preCloseOfIndex, preCloseOfBond);
                        }
                    }
                }
            }

            // @update: 还要使用东财api,  从网络而非数据库访问历史k线!!
            String dateStart = EastMoneyDbApi.getPreNTradeDateStrict(dateStr, kLineAmountHope);
            String yesterday = EastMoneyDbApi.getPreNTradeDateStrict(dateStr, 1); // 获取昨日前的
            EmQuoteApi.getQuoteHistoryBatch(beanList, dateStart, yesterday, "101", "1", 1, 4000, true);
            // @noti: 调用参数同 k线动态图表的构造器里面的调用方式, 以免参数不同缓存读不到
//            klineDfBeforeToday = EmQuoteApi
//                    .getQuoteHistorySingle(true, beanEm, dateStart, yesterday, "101", "1", 1, 4000);

        } catch (Exception e) {
            e.printStackTrace();
            CommonUtil.notifyError("载入转债/正股/指数的分时数据等到缓存 失败");
            loading = false;
            return;
        }
        loading = false;
        CommonUtil.notifyKey("已完成载入转债列表的分时数据等到缓存");
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

    /**
     * 东财pc, 最新热门资讯, 从数据库获取最后爬取的 500条; 用以爬虫去重
     * 倒序, 符合东财pc显示逻辑
     *
     * @return
     */
    public static List<String> getNewestHotEmPcNewTitleSet(int limit) {
        String sql = StrUtil.format("select title from pc_new_hots order by pushtime desc limit {}", limit);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return DataFrameS.getColAsStringList(dataFrame, "title");
    }

    /**
     * 从 bk_kline_daily 数据表, 给定日期, 筛选所有当日的 全部板块数据!
     *
     * @param dateStr
     * @return
     */
    @Cached
    public static DataFrame<Object> getBkDailyKlineAllOfOneDay(String dateStr) {
        DataFrame<Object> res = BkDailyKlineAllOfOneDayCache.get(dateStr);
        if (res != null) {
            return res;
        }
        String sql = StrUtil.format("select * from bk_kline_daily where date='{}'", dateStr);
        try {
            res = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        BkDailyKlineAllOfOneDayCache.put(dateStr, res);
        return res;
    }
}
