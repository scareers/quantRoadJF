package com.scareers.datasource.eastmoney.quotecenter;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import com.scareers.annotations.CanCache;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.bean.IndexBkHandicap;
import com.scareers.datasource.eastmoney.quotecenter.bean.StockHandicap;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.Tqdm;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.getAsStrUseHutool;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.STR_SEC_CODE;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.STR_SEC_NAME;
import static com.scareers.utils.JSONUtilS.jsonStrToDf;

/**
 * description: 东方财富股票 相关api; 可选底层http实现: hutool.http 或者 Kevin 库, 某些api仅其中之一可正常访问.
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class EmQuoteApi {
    private static final Log log = LogUtil.getLogger();
    public static Map<String, Object> MARKETS_ARGS_DICT = new ConcurrentHashMap<>();
    public static Map<Object, Object> EASTMONEY_QUOTE_FIELDS = new ConcurrentHashMap<>();
    public static Map<String, Object> EASTMONEY_KLINE_FIELDS = new ConcurrentHashMap<>();
    public static Map<String, Object> MARKET_NUMBER_DICT = new ConcurrentHashMap<>();
    public static final List<String> fSTransactionCols = Arrays.asList("stock_code", "market", "time_tick", "price",
            "vol", "bs"); // 分时成交数据列名称

    public static Cache<String, DataFrame<Object>> quoteHistorySingleCache = CacheUtil.newLRUCache(1024,
            4 * 3600 * 1000); // 历史k线,分时图等
    public static Cache<String, List<Double>> stockPriceLimitCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 个股今日涨跌停
    public static Cache<String, List<Double>> stockPreCloseAndTodayOpenCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 个股昨收和今开盘价
    public static Cache<String, String> preNTradeDateStrictCache = CacheUtil.newLRUCache(1024,
            3600 * 1000); // 某个日期的上n个交易日?
    public static ThreadPoolExecutor poolExecutor; // 可能的线程池


    public static void main(String[] args) throws Exception {
        Console.log(MARKETS_ARGS_DICT.keySet());
//
//
        Console.log("个股今日涨跌停:");
        Console.log(getStockPriceLimitToday("000001", 2000, 1, true));
        Console.log("个股昨收今开:");
        Console.log(getStockPreCloseAndTodayOpen("000001", 2000, 1, true));

        Console.log("个股盘口数据:");
        Console.log(getStockHandicap("002432", 2000, 1));

        Console.log("指数/板块昨收今开");
        Console.log(getPreCloseAndTodayOpenOfIndexOrBK(SecurityBeanEm.createBK("bk1030"), 2000, 3));
        Console.log("指数/板块盘口数据:");
        Console.log(getIndexOrBKHandicap(SecurityBeanEm.createBK("bk1030"), 2000, 2));

        Console.log("分时成交数据:");
        Console.log(getFSTransaction(10, SecurityBeanEm.createBK("BK1030"), 1, 2000).toString(250));
        Console.log(getFSTransaction(10, SecurityBeanEm.createStock("000001"), 1, 2000).toString(250));
        Console.log(getFSTransaction(10, SecurityBeanEm.createIndex("000001"), 1, 2000).toString(250));

        Console.log("各市场实时行情截面数据");
        Console.log(getRealtimeQuotes(Arrays.asList("沪深A股")));
        Console.log(getRealtimeQuotes(Arrays.asList("两网及退市")));
        Console.log(getRealtimeQuotes(Arrays.asList("风险警示板")));
        Console.log(getRealtimeQuotes(Arrays.asList("所有板块")));
        Console.log(getRealtimeQuotes(Arrays.asList("上证主板", "深证主板")));
        Console.log(getRealtimeQuotes(Arrays.asList("可转债")));


        Console.log("历史行情k线数据 -- 可分时数据");
        Console.log(getQuoteHistorySingle(SecurityBeanEm.createIndex("000001"), null, null, "1", "1", 3, 3000));
        Console.log("批量历史行情k线数据 -- 可分时数据");
        Console.log(getQuoteHistoryBatch(SecurityBeanEm.getTwoGlobalMarketIndexList(), null, null, "1", "1", 3, 3000,
                false));

        Console.log("1分钟分时图数据");
        Console.log(getFs1MToday(SecurityBeanEm.createStock("000001"), 3, 2000));


        Console.log("给定日期的上 n 个交易日:");
        Console.log(getPreNTradeDateStrict(DateUtil.today(), 1));
        Console.log(getPreTradeDateStrict(DateUtil.today()));
        Console.log(getPreNTradeDateStrict(DateUtil.today(), 2));


    }

    static {
        initFSDICT();
        initEASTMONEYQUOTEFIELDS();
        initMARKETNUMBERDICT();
        initEASTMONEYKLINEFIELDS();

        initFlushTimesMap(); // 刷新时间警告提示
        notifyFlushTimes();
    }

    /**
     * 成对出现. 设置每种api的 当日新数据刷新时间临界, 以及未到时间之前的提示信息
     *
     * @see initFlushTimesMap, notifyFlushTimes
     */
    public static ConcurrentHashMap<String, DateTime> flushTimeMap;
    public static ConcurrentHashMap<String, String> flushTimeInfoMap;

    /**
     * 将初始化各项数据刷新时间
     */
    private static void initFlushTimesMap() {
        flushTimeMap = new ConcurrentHashMap<>();
        flushTimeInfoMap = new ConcurrentHashMap<>();

        // 个股涨跌停价格今日刷新时间, 以及 提示声音/log信息
        flushTimeMap.put("getStockPriceLimitToday", DateUtil.parse(DateUtil.today() + " 09:00:00"));
        flushTimeInfoMap.put("getStockPriceLimitToday", "警告: 个股涨跌停价格数据尚未开始刷新, 预计9点整开始刷新");

        // 个股昨收今开价格: 昨收肯定存在. 因此该时间点代表 今开尚未刷新
        flushTimeMap.put("getStockPreCloseAndTodayOpen", DateUtil.parse(DateUtil.today() + " 09:25:05"));
        flushTimeInfoMap.put("getStockPreCloseAndTodayOpen", "警告: 今日开盘价格数据尚未开始刷新, 预计9点30分开始刷新");

        // 个股实时盘口数据
        flushTimeMap.put("getStockHandicap", DateUtil.parse(DateUtil.today() + " 09:15:05"));
        flushTimeInfoMap.put("getStockHandicap", "警告: 盘口数据尚未开始刷新, 预计9点15分开始刷新");

        // 个股/指数实时盘口数据
        flushTimeMap.put("getFSTransaction1", DateUtil.parse(DateUtil.today() + " 09:15:05"));
        flushTimeInfoMap.put("getFSTransaction1", "警告: 个股成交数据尚未开始刷新, 预计9点15分开始刷新");
        flushTimeMap.put("getFSTransaction2", DateUtil.parse(DateUtil.today() + " 09:25:05"));
        flushTimeInfoMap.put("getFSTransaction2", "警告: 指数成交数据尚未开始刷新, 预计9点25分开始刷新");

        // 市场截面数据
        flushTimeMap.put("getRealtimeQuotes", DateUtil.parse(DateUtil.today() + " 09:25:05"));
        flushTimeInfoMap.put("getRealtimeQuotes", "警告: 市场截面数据尚未开始刷新, 预计9点25分开始刷新");

        // 1分钟分时数据
        flushTimeMap.put("getFs1MToday", DateUtil.parse(DateUtil.today() + " 09:25:05"));
        flushTimeInfoMap.put("getFs1MToday", "警告: 1分钟分时数据尚未开始刷新, 预计9点25分开始刷新");

    }

    /**
     * 将检测本类包含的各个api, 当日数据是否刷新 ??
     * 将采用 语音播报 以及 log /邮件 等形式提示
     * 例如分时成交可能需要每个交易日 8:00 以后才可刷新.(假设)
     */
    private static void notifyFlushTimes() {
        for (String api : flushTimeMap.keySet()) {
            DateTime dateTime = flushTimeMap.get(api);
            if (DateUtil.between(dateTime, DateUtil.date(), DateUnit.MS, false) <= 0) {
                String info = flushTimeInfoMap.get(api);

                // 提示方式
                log.warn("EM API未到刷新时间: {}", info);
                Tts.playSound(info, true);
            }
        }
    }


    private static void checkPoolExecutor() {
        if (poolExecutor == null) {
            poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("EM.EmQuoteApi-", null, true));

        }
    }


    /**
     * 获取当日某个股的涨跌停限价. [涨停价,跌停价]. 调用盘口 api;
     * 一般而言并不会出现 "-" 导致解析错误; 解析错误时, -1.0代替
     *
     * @param stock
     * @return
     */
    @TimeoutCache(timeout = "1 * 3600 * 1000")
    public static List<Double> getStockPriceLimitToday(String stockCodeSimple, int timeout, int retry, boolean useCache)
            throws Exception {
        // String cacheKey = stockCodeSimple; // 缓存key为 个股代码
        List<Double> res = stockPriceLimitCache.get(stockCodeSimple);
        if (useCache && res != null && !res.contains(-1.0)) {
            return res;
        }
        res = new ArrayList<>();
        JSONObject resp = getStockHandicapCore(stockCodeSimple, "f51,f52", timeout, retry);
        try {
            res.add(Double.valueOf(resp.get("data.f51").toString())); // 涨停价
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        try {
            res.add(Double.valueOf(JSONUtilS.getByPath(resp, "data.f52").toString())); // 跌停价
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        stockPriceLimitCache.put(stockCodeSimple, res);
        return res;
    }

    /**
     * 获取个股昨收今开.
     * 1小时有效期缓存.
     * 通常昨收不会有问题, 今开在开盘以前为 "-", 将解析错误, 使用-1.0 替代
     *
     * @param stockCodeSimple
     * @param timeout
     * @return
     * @throws Exception
     */
    @TimeoutCache(timeout = "1 * 3600 * 1000")
    public static List<Double> getStockPreCloseAndTodayOpen(String stockCodeSimple, int timeout, int retry,
                                                            boolean useCache)
            throws Exception {
        List<Double> res = stockPreCloseAndTodayOpenCache.get(stockCodeSimple);
        if (res != null && !res.contains(-1.0) && useCache) { // 注意可能并未刷新, 因此需要加上此限制条件
            return res;
        }
        JSONObject resp = getStockHandicapCore(stockCodeSimple, "f60,f46", timeout, retry);
        res = new ArrayList<>();
        try {
            res.add(Double.valueOf(JSONUtilS.getByPath(resp,"data.f60").toString())); // 昨收
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        try {
            res.add(Double.valueOf(JSONUtilS.getByPath(resp,"data.f46").toString())); // 今开
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        stockPreCloseAndTodayOpenCache.put(stockCodeSimple, res);
        return res;
    }

    /**
     * 个股实时盘口数据, 包含买卖盘, 常规行情项等.
     * StockHandicap 代表盘口数据截面.
     * 盘口数据常规刷新间隔为 3s;
     *
     * @param stockCodeSimple
     * @param fields
     * @param timeout
     * @return
     * @see getStockHandicapCore
     */
    public static StockHandicap getStockHandicap(String stockCodeSimple, int timeout, int retry) {
        JSONObject resp;
        try {
            resp = getStockHandicapCore(stockCodeSimple, StockHandicap.fieldsStr, timeout, retry);
        } catch (Exception e) {
            log.error("get exception: 获取个股实时盘口数据失败: stock: {}", stockCodeSimple);
            return null;
        }
        JSONObject rawJson = (JSONObject) resp.get("data");
        if (rawJson == null) {
            log.error("data字段为null: 获取个股实时盘口数据失败: stock: {}", stockCodeSimple);
            return null;
        }
        return new StockHandicap(rawJson);
    }


    /**
     * private, 不使用缓存. 由各个具体字段api, 自行决定缓存机制
     * <p>
     * 实时核心盘口数据. 该api可访问极多数据项.
     * 东方财富行情页面, 主api之一, 有很多字段, 包括盘口. 均使用此url, 可传递极多字段
     * http://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f43,f57,f58,f169,f170,f46,f44,f51,f168,f47,f164,f163,f116,f60,f45,f52,f50,f48,f167,f117,f71,f161,f49,f530,f135,f136,f137,f138,f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104,f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78,f110,f260,f261,f262,f263,f264,f267,f268,f250,f251,f252,f253,f254,f255,f256,f257,f258,f266,f269,f270,f271,f273,f274,f275,f127,f199,f128,f193,f196,f194,f195,f197,f80,f280,f281,f282,f284,f285,f286,f287,f292,f293,f181,f294,f295,f279,f288&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     * <p>
     * 其中 f51(涨停),f52(跌停) 是该股票涨跌停价格
     * jQuery112409228148447288975_1643015501069({"rc":0,"rt":4,"svr":181233083,"lt":1,"full":1,"data":{"f51":39.89,"f52":26.59}});
     * https://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f51,f52&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     * <p>
     * {
     * "rc": 0,
     * "rt": 4,
     * "svr": 181669437,
     * "lt": 1,
     * "full": 1,
     * "data": {
     * "f43": 8.74, 最新
     * "f44": 8.76, 最高
     * "f45": 8.67, 最低
     * "f46": 8.7,  今开
     * "f47": 376829, 总手
     * "f48": 328719168.0,  金额
     * "f49": 221532, 外盘
     * "f50": 0.96,  量比
     * "f51": 9.56,  涨停
     * "f52": 7.82,  跌停
     * "f55": 1.415091382, 公司核心数据-收益(三)
     * "f57": "600000", 资产代码
     * "f58": "浦发银行", 名称
     * "f60": 8.69, 昨收
     * "f62": 3,
     * "f71": 8.72, 均价
     * "f78": 0,
     * "f80": "[{\"b\":202202110930,\"e\":202202111130},{\"b\":202202111300,\"e\":202202111500}]", 交易时间
     * "f84": 29352168006.0, 总股本
     * "f85": 29352168006.0, 流通股本
     * "f86": 1644566391,
     * "f92": 18.7324153, 每股净资产
     * "f104": 191137000000.0,
     * "f105": 41536000000.0, 净利润
     * "f107": 1,
     * "f110": 1,
     * "f111": 2,
     * "f116": 256537948372.44, 总市值
     * "f117": 256537948372.44, 流通市值
     * "f127": "银行",
     * "f128": "上海板块",
     * "f135": 129358320.0,
     * "f136": 129068671.0,
     * "f137": 289649.0,
     * "f138": 38749279.0,
     * "f139": 51406071.0,
     * "f140": -12656792.0,
     * "f141": 90609041.0,
     * "f142": 77662600.0,
     * "f143": 12946441.0,
     * "f144": 99927841.0,
     * "f145": 109458394.0,
     * "f146": -9530553.0,
     * "f147": 88260797.0,
     * "f148": 79019893.0,
     * "f149": 9240904.0,
     * "f161": 155297,  内盘
     * "f162": 4.63,  市盈率[动]
     * "f163": 4.4,
     * "f164": 4.65,
     * "f167": 0.47, 市净率
     * "f168": 0.13, 换手 0.13%
     * "f169": 0.05,  涨跌价格值
     * "f170": 0.58, // 涨幅%
     * "f173": 7.25,  ROE%
     * "f177": 577,
     * "f183": 143484000000.0,  总营收
     * "f184": -3.5278455736, 总营收同比
     * "f185": -7.165526798087, 净利润同比
     * "f186": 0.0,  毛利率%
     * "f187": 29.3614619052, 净利率
     * "f188": 91.6841375217, 负债率
     * "f189": 19991110,
     * "f190": 6.300658948334, 每股未分配利润(元)
     * "f191": -41.62, 委比
     * "f192": -25487, 委差
     * "f193": 0.09,
     * "f194": -3.85,
     * "f195": 3.94,
     * "f196": -2.9,
     * "f197": 2.81,
     * "f199": 90,
     * "f250": "-",
     * "f251": "-",
     * "f252": "-",
     * "f253": "-",
     * "f254": "-",
     * "f255": 0,
     * "f256": "-",
     * "f257": 0,
     * "f258": "-",
     * "f262": "110059",
     * "f263": 1,
     * "f264": "浦发转债",
     * "f266": 107.32,
     * "f267": 106.98,
     * "f268": -0.32,
     * "f269": "-",
     * "f270": 0,
     * "f271": "-",
     * "f273": "-",
     * "f274": "-",
     * "f275": "-",
     * "f280": "-",
     * "f281": "-",
     * "f282": "-",
     * "f284": 0,
     * "f285": "-",
     * "f286": 0,
     * "f287": "-",
     * "f292": 5,
     * "f31": 8.78, // 卖5
     * "f32": 7546, // 卖5量
     * "f33": 8.77, // 卖4
     * "f34": 5681,
     * "f35": 8.76, // 卖3
     * "f36": 11189,
     * "f37": 8.75, // 卖2
     * "f38": 10413,
     * "f39": 8.74, // 卖1
     * "f40": 8531, // 卖1量
     * "f19": 8.73, // 买1
     * "f20": 3553, // 买1量
     * "f17": 8.72,
     * "f18": 4841,
     * "f15": 8.71,
     * "f16": 1406,
     * "f13": 8.7,
     * "f14": 6429,
     * "f11": 8.69, // 买5
     * "f12": 1644 // 买5量
     * }
     * }
     *
     * @param stockSimpleCode
     * @param fields          访问的字段列表, 逗号分割
     * @param timeout
     * @return 解析的json响应. 具体字段的访问由调用方决定. date.字段名: Double.valueOf(resp.getByPath("data.f51").toString())
     * @throws Exception
     */
    private static JSONObject getStockHandicapCore(String stockSimpleCode, String fields, int timeout, int retry)
            throws Exception {
        SecurityBeanEm bean = SecurityBeanEm.createStock(stockSimpleCode);
        String secId = bean.getSecId(); // 获取准确的secId

        String url = "https://push2.eastmoney.com/api/qt/stock/get";
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("invt", "2");
        params.put("fltt", "2");
        params.put("fields", fields);
        params.put("secid", secId);
        params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            e.printStackTrace();

            log.error("get exception: 访问http失败: 获取涨跌停价: stock: {}", secId);
            return null;
        }
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        return JSONUtilS.parseObj(response);
    }

    /**
     * 获取指数或者板块昨收今开, 使用指数/板块盘口api
     * 类似个股, 用-1.0 代表 - , 即暂无数据.
     *
     * @param indexSimpleCode
     * @param timeout
     * @return
     * @throws Exception
     */
    public static List<Double> getPreCloseAndTodayOpenOfIndexOrBK(SecurityBeanEm bean, int timeout, int retry) {
        JSONObject resp = getIndexOrBKHandicapCore(bean, "f60,f46", timeout, retry); // 字段同个股. 昨收今开
        List<Double> res = new ArrayList<>();
        try {
            res.add(Double.parseDouble(JSONUtilS.getByPath(resp,"data.f60").toString()) / 100); // 昨收 , 注意/100
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        try {
            res.add(Double.parseDouble(JSONUtilS.getByPath(resp,"data.f46").toString()) / 100); // 今开
        } catch (Exception e) {
            e.printStackTrace();
            res.add(-1.0);
        }
        return res;
    }

    /**
     * 获取指数或者板块实时盘口数据
     *
     * @param stockCodeSimple
     * @param timeout
     * @param retry
     * @return
     */
    public static IndexBkHandicap getIndexOrBKHandicap(SecurityBeanEm bean, int timeout, int retry) {
        JSONObject resp;
        try {
            resp = getIndexOrBKHandicapCore(bean, IndexBkHandicap.fieldsStr, timeout, retry);
        } catch (Exception e) {
            log.error("get exception: 获取指数/板块实时盘口数据失败: index/bk: {}", bean.getSecId());
            return null;
        }
        JSONObject rawJson = (JSONObject) resp.get("data");
        if (rawJson == null) {
            log.error("get exception: 获取指数/板块实时盘口数据失败: index/bk: {}", bean.getSecId());
            return null;
        }
        return new IndexBkHandicap(rawJson);
    }

    /**
     * 指数流入流出全
     * http://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&cb=jQuery35103898435803189497_1644838786831&fields=f135%2Cf136%2Cf137%2Cf138%2Cf139%2Cf140%2Cf141%2Cf142%2Cf143%2Cf144%2Cf145%2Cf146%2Cf147%2Cf148%2Cf149&secid=1.000001&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1644838786832
     * 个股资金8项, 主流流入是大单+超大单. 净流入是减法. 因此使用8项数据计算
     * http://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f138,f139,f141,f142,f144,f145,f147,f148&secid=1.600000&cb=jQuery112409761295519601547_1644839078324&_=1644839078325
     */

    /**
     * 指数/板块实时行情. 盘口
     * 东财行情页面, 指数比板块多4个字段. 但实测板块依旧可以获取
     * http://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&cb=jQuery351037846734899553613_1644751180897&fields=f58%2Cf107%2Cf57%2Cf43%2Cf59%2Cf169%2Cf170%2Cf152%2Cf46%2Cf60%2Cf44%2Cf45%2Cf47%2Cf48%2Cf19%2Cf532%2Cf39%2Cf161%2Cf49%2Cf171%2Cf50%2Cf86%2Cf600%2Cf601%2Cf154%2Cf84%2Cf85%2Cf168%2Cf108%2Cf116%2Cf167%2Cf164%2Cf92%2Cf71%2Cf117%2Cf292%2Cf113%2Cf114%2Cf115%2Cf119%2Cf120%2Cf121%2Cf122&secid=1.000001&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1644751180898
     * 数据实例
     * {
     * "rc": 0,
     * "rt": 4,
     * "svr": 182995791,
     * "lt": 1,
     * "full": 1,
     * "data": {
     * "f43": 346295, 最新
     * "f44": 350015, 最高
     * "f45": 345933, 最低
     * "f46": 347228, 今开
     * "f47": 361356091, 成交量
     * "f48": 426297925632.0, 成交额
     * "f49": 168609531, 外盘
     * "f50": 107,
     * "f57": "000001",
     * "f58": "上证指数",
     * "f59": 2,
     * "f60": 348591, 昨收
     * "f71": "-",
     * "f84": 4629436239233.0,
     * "f85": 4061217883902.0,
     * "f86": 1644566379,
     * "f92": "-",
     * "f107": 1,
     * "f108": "-",
     * "f113": 285, 涨家数
     * "f114": 1765, 跌家数
     * "f115": 44, 平家数
     * "f116": 49300990877782.0,
     * "f117": 41749793711638.0, 流通市值
     * "f119": 302, 5日涨跌幅, /100百分比
     * "f120": -326, 20日涨跌幅
     * "f121": -198, 60日
     * "f122": -486, 今年
     * "f152": 2,
     * "f154": 4,
     * "f161": 192746560, 内盘
     * "f164": "-",
     * "f167": "-",
     * "f168": 89, 换手率, /100百分比, 0.89%
     * "f169": -2296, 涨跌点数, 注意 /100, 实际 22.96点
     * "f170": -66, 涨跌幅度, 同样 /100且百分比,  -0.66%
     * "f171": 117, 振幅, /100百分比  1.17%
     * "f292": 5,
     * "f39": "-",
     * "f40": "-",
     * "f19": "-",
     * "f20": "-",
     * "f600": "-",
     * "f601": "-"
     * }
     * }
     *
     * @param indexCodeSimple
     * @param fields
     * @param timeout
     * @return
     * @throws Exception
     */
    private static JSONObject getIndexOrBKHandicapCore(SecurityBeanEm bean, String fields, int timeout, int retry) {
        Assert.isTrue(bean.isIndex() || bean.isBK()); // 需要是板块或者指数
        String secId = bean.getSecId(); // 获取准确的secId
        String url = "https://push2.eastmoney.com/api/qt/stock/get";
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("invt", "2");
        params.put("fltt", "1"); // 与股票相比, 仅本参数由2变1
        params.put("fields", fields);
        params.put("secid", secId);
        params.put("cb", StrUtil.format("jQuery351037846734899553613_{}", // callback有变化
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get exception: 访问http失败: 获取指数板块行情: index/bk: {}", secId);
            return null;
        }
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        return JSONUtilS.parseObj(response);
    }


    /**
     * 获取3类资产的分时成交
     * // @using
     * https://push2.eastmoney.com/api/qt/stock/details/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1400&secid=0.000153&cb=jQuery112409885675811656662_1640099646776&_=1640099646777
     * // @noti: 该url结果为升序, 但是最新 x 条数据
     * 数据刷新时间点:
     * --> 02:00 得到昨日(上个交易日) 的完整数据df
     * --> 08:55 数据字段刷新为[], 可得到空df  ********* 重置点1
     * --> 09:15:0x  指数依旧空[], 个股则可以获取集合竞价数据. 集合竞价每次价格改变, 则生成一条记录. ****** 早盘集合竞价
     * --> 09:25:0x   指数可获得一条集合竞价结果数据. 个股则整个竞价记录
     * --> 09:30:x 正常df
     * --> 11:30:x 正常df, 但最大时间可能超过 11:30:00 少数几条记录
     * --> 13:00:x 正常df, 第一个常为 13:00:01
     * --> 14:57:0x 正常df,个股/指数/板块 14:57:0x 后一直不会有新数据,
     * -->          持续到收盘 15:00:00 有1-2条收盘集合竞价数据; ************* 尾盘集合竞价
     * --> 15:00:x 正常df, 最后大约1-2条 15:00:00 tick
     * --> 周六同周五收盘后, 数据不变
     *
     * @param lastRecordAmounts 单页数量, 最新多少条数据?
     * @param bean              资产对象
     * @return 出错则抛出异常.
     * @noti: 8:55  details字段已经重置为 [] 空列表
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts,
                                                     SecurityBeanEm bean,
                                                     int retry, int timeout) {
        String keyUrl = "https://push2.eastmoney.com/api/qt/stock/details/get";
        String response;

        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("fields1", "f1,f2,f3,f4");
        params.put("fields2", "f51,f52,f53,f54,f55");
        params.put("pos", StrUtil.format("-{}", lastRecordAmounts));
        params.put("secid", bean.getSecId());
        params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());
        try {
            response = getAsStrUseHutool(keyUrl, params, timeout, retry);
        } catch (Exception e) {
            log.error("Fs成交 tick数据获取失败, 返回null: stock: {} -- {}", bean.getSecId(), bean.getName());
            return null;
        }

        DataFrame<Object> res;
        try {
            res = jsonStrToDf(response, "(", ")", fSTransactionCols,
                    Arrays.asList("data", "details"), String.class, Arrays.asList(3),
                    Arrays.asList(bean.getSecCode(), bean.getMarket()));
        } catch (Exception e) {
            log.warn("get exception: 获取数据错误. stock: {} -- {}", bean.getSecId(), bean.getName());
            log.warn("raw data: 原始响应字符串: {}", response);
            throw e;
        }
        return res;
    }


    /**
     * efinance get_realtime_quotes 重载实现.  给定市场列表, 返回, 这些市场所有股票列表 截面数据
     * 可传递市场:
     * [可转债, 沪深京A股, 沪深A股, 上证A股, 深证A股, 北证A股, 上证主板, 深证主板, 创业板, 科创板, 沪股通, 深股通, 风险警示板, 两网及退市,
     * 地域板块, 行业板块, 概念板块, 所有板块, 上证系列指数, 深证系列指数, 沪深系列指数, ETF, LOF, 美股, 港股, 英股, 中概股, 期货]
     * <p>
     * Quotes: 行情
     * 列:
     * 资产代码	 资产名称	涨跌幅	最新价	最高 最低	 今开	 涨跌额	         换手率	          量比	          动态市盈率
     * 成交量	                 成交额	          昨日收盘	           总市值	          流通市值	市场编号	    行情id	市场类型
     * <p>
     * 数据刷新时间点:
     * // 02:10 昨日df
     * // 08:55 能得到正确df,但 只有动态市盈率,昨日收盘和市值字段可用. 其他数值字段用 - 填充, 将解析失败. 即占位df
     * // 09:14:x 占位df
     * // 09:15:x 涨跌幅,最新价,涨跌额填充,其余 -
     * // 09:24:x 涨跌幅,最新价,涨跌额填充,其余 -
     * // 09:25:x 正常全填充df
     * // 09:30:x 正常df
     *
     * @param markets
     * @return
     * @noti 个股市场重复了语义也可以正常工作
     * @see EmQuoteApi.FS_DICT
     * @see EmQuoteApi.EASTMONEY_QUOTE_FIELDS
     * @see EmQuoteApi.MARKET_NUMBER_DICT
     */
    public static DataFrame<Object> getRealtimeQuotes(List<String> markets) {
        List<String> realMarketArgs =
                markets.stream().map(value -> MARKETS_ARGS_DICT.get(value).toString()).collect(Collectors.toList());
        String marketArgsStr = StrUtil.join(",", realMarketArgs);
        // @bugfix: 实测市场列表参数, python将自动把 " "转换+, 不会出现bug. 而java的自动转换是 %20. 将出现错误?
//        marketArgsStr = marketArgsStr.replace(" ", "+");
        //['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21', 'f13']
        String url = "http://push2.eastmoney.com/api/qt/clist/get";
        List<String> fields = Arrays.asList("f12", "f14", "f3", "f2", "f15", "f16", "f17", "f4", "f8",
                "f10", "f9", "f5", "f6", "f18", "f20", "f21", "f13");
        String fieldsStr = StrUtil.join(",", fields);
        HashMap<String, Object> params = new HashMap<>();
        params.put("pn", "1");
        params.put("pz", "1000000");
        params.put("po", "1");
        params.put("np", "1");
        params.put("fltt", "2");
        params.put("invt", "2");
        params.put("fid", "f3");
        params.put("fs", marketArgsStr);
        params.put("fields", fieldsStr);

        String response = getAsStrUseHutool(url, params, 4000);
        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields, Arrays.asList("data", "diff"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(EASTMONEY_QUOTE_FIELDS);
        dfTemp = dfTemp.add("市场名称", values ->
                MARKET_NUMBER_DICT.get(values.get(16).toString())
        );
        return dfTemp;
    }

    /**
     * 批量资产获取k线. 复刻 efinance   get_quote_history
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	  涨跌幅	  涨跌额	 换手率	  资产代码	资产名称
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> getQuoteHistoryBatch(
            List<SecurityBeanEm> beanEmList,
            String begDate,
            String endDate,
            String klType, String fq,
            int retrySingle,
            int timeoutOfReq,
            boolean useCache
    )
            throws ExecutionException, InterruptedException {

        log.info("klines gets batch: 线程池批量获取k线, 股票数量: {}", beanEmList.size());
        checkPoolExecutor();
        HashMap<SecurityBeanEm, Future<DataFrame<Object>>> futures = new HashMap<>();
        for (SecurityBeanEm beanEm : beanEmList) {
            futures.put(beanEm, poolExecutor.submit(new Callable<DataFrame<Object>>() {
                @Override
                public DataFrame<Object> call() throws Exception {
                    return getQuoteHistorySingle(useCache, beanEm, begDate, endDate, klType, fq, retrySingle,
                            timeoutOfReq);
                }
            }));
        }
        ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> res = new ConcurrentHashMap<>();
        for (SecurityBeanEm beanEm : Tqdm.tqdm(beanEmList, "process: ")) {
            DataFrame<Object> dfTemp = futures.get(beanEm).get();
            if (dfTemp != null) {
                res.put(beanEm, dfTemp);
            }
        }
        log.info("finish klines gets batch: 线程池批量获取k线完成");
        return res;
    }


    /**
     * 单资产历史k线: 字段:
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
     * <p>
     * 数据刷新时间:
     * // 日k线
     * // 02:10 截至昨天df
     * // 08:55 截至昨天df
     * // 09:24:x 截至昨天df
     * // 09:25:x 已经出现含今日df. 此后动态变化
     * // 23:50:x 含今日正常df
     *
     * @param useCache 指定是否使用缓存. 默认缓存有效期为4小时,且数量存在上限, 不使用缓存则强制http访问
     * @param secCode  资产简单数字代码, 板块为 BK 开头
     * @param secType  secCode代表 的资产类型, 目前支持 股票/指数/板块(BK开头)
     * @param begDate  开始日期, null则默认19900101, 格式可 yyyy-MM-dd, 将被转换
     * @param endDate  结束日期, 默认 21000101, 同上
     * @param klType   代表k线类型(不同周期)的字符串, 1/5/15/30/60代表各分钟. 101/102/103 代表日/周/月
     * @param fq       复权方式, 0 / 1/ 2 代表 不/前/后复权
     * @param retry    http访问重试次数
     * @param timeout  http访问超时
     * @return 异常返回 null
     * @throws Exception
     */
    @CanCache
    @TimeoutCache(timeout = "4 * 3600 * 1000")
    public static DataFrame<Object> getQuoteHistorySingle(boolean useCache,
                                                          SecurityBeanEm bean, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retry,
                                                          int timeout) {
        if (begDate == null) {
            begDate = "19900101";
        }
        if (endDate == null) {
            endDate = "21000101";
        }
        if (begDate.length() > 8) {
            begDate = begDate.replace("-", ""); // 标准化
        }
        if (endDate.length() > 8) {
            endDate = endDate.replace("-", "");
        }

        String cacheKey = StrUtil
                .format("{}__{}__{}__{}__{}__{}__{}", bean.getSecCode(), begDate, endDate, klType, fq,
                        bean.getSecType());

        DataFrame<Object> res = null;
        if (useCache) { // 必须使用 caCache 且真的存在缓存
            res = quoteHistorySingleCache.get(cacheKey);
        }
        if (res != null) {
            return res;
        }

        String fieldsStr = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61"; // k线字段
        List<String> fields = StrUtil.split(fieldsStr, ",");
        String quoteId = bean.getSecId();

        HashMap<String, Object> params = new HashMap<>();
        params.put("fields1", "f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13");
        params.put("fields2", fieldsStr);
        params.put("beg", begDate);
        params.put("end", endDate);
        params.put("rtntype", "6");
        params.put("secid", quoteId);
        params.put("klt", klType);
        params.put("fqt", fq);

        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get";
        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }


        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields.stream().map(value -> EASTMONEY_KLINE_FIELDS.get(value).toString())
                        .collect(Collectors.toList()),
                Arrays.asList("data", "klines"), String.class, Arrays.asList(),
                Arrays.asList());

        dfTemp = dfTemp.add(STR_SEC_CODE, values -> bean.getSecCode());
        res = dfTemp.add(STR_SEC_NAME, values -> bean.getName());
        quoteHistorySingleCache.put(cacheKey, res); // 将更新
        return res;
    }

    /**
     * 默认使用 cache, 这针对非分时图.  分时图将默认不使用cache
     *
     * @param secCode
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retry
     * @param isIndex
     * @param timeout
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getQuoteHistorySingle(SecurityBeanEm bean, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retry,
                                                          int timeout) {
        return getQuoteHistorySingle(true, bean, begDate, endDate, klType, fq, retry, timeout);
    }

    /**
     * 1分钟分时数据, 本身使用 getQuoteHistorySingle().
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
     *
     * <p>
     * 数据刷新时刻:
     * // 1分钟分时图
     * // 02:10 昨日df
     * // 09:24:x 得到空df,
     * // 09:25:x 单行 9:31 的df.
     * // 09:30:x 单行 9:31 的df.
     * // 09:40:x 正常df, 分钟+1: 得到 41, 意味着此刻xx分钟 视为 xx+1 的分时图(但没有固定), 分时图xx分钟已固定
     * // 11:30:x 正常df, 显然最大只能达到 11:30 (合理)
     * // 13:00:x 于13:00:01, 立即开始出现 13:01 的数据, 规律不变.
     * // 14:59:x 正常df, 算后一分, 此时已经出现 15:00 的分时图, 只是动态的,等待15:00:00 盖棺定论
     * // 15:00:x 正常df, 不再有 15:01, 盖棺定论
     *
     * @param stock
     * @param isIndex
     * @param retrySingle
     * @param timeout
     * @return 1分钟分时图当日; 当某分钟开始后(即0秒以后, fs将更新到当分钟 + 1. 例如当前 13 : 21 : 10, 则将更新到 13 : 22
     * @throws Exception
     * @noti 两大指数大约 6,7 秒开始出现下一分钟分时, 个股大约 1-2s 之间, 个股极快
     */
    public static DataFrame<Object> getFs1MToday(SecurityBeanEm bean, int retrySingle,
                                                 int timeout,
                                                 boolean useCache) {
        DataFrame<Object> res = getQuoteHistorySingle(useCache, bean, null, null, "1", "qfq", retrySingle, timeout);
        if (res == null) {
            log.error("Fs1M kline获取失败, 返回null: stock: {} -- {}", bean.getSecCode(), bean.getName());
        }
        return res;
    }

    /**
     * 分时图将默认不使用cache,比较实时
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
     *
     * @param bean
     * @param retrySingle
     * @param timeout
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getFs1MToday(SecurityBeanEm bean, int retrySingle,
                                                 int timeout) {
        return getFs1MToday(bean, retrySingle, timeout, false);
    }


    /**
     * 给定一个具体日期, yyyy-MM-dd 形式, 因为em默认日期格式是这样, 方便比较.  返回上n个交易日的日期,
     * 原理上查询 上证指数 所有历史日k线, 获取得到日期列表. 倒序遍历即可
     * 返回  yyyy-MM-dd 形式
     *
     * @param todayDate
     * @return yyyy-MM-dd
     */
    @TimeoutCache(timeout = "3600 * 1000")
    public static String getPreNTradeDateStrict(String todayDate, int n) {
        String cacheKey = StrUtil.format("{}__{}", todayDate, n);
        String res = preNTradeDateStrictCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        // 查询结果将被缓存.
        DataFrame<Object> dfTemp = getQuoteHistorySingle(true, SecurityBeanEm.SHANG_ZHENG_ZHI_SHU, "19900101",
                "21000101", "101", "1", 3,
                3000); // 使用缓存
        List<String> dates = DataFrameS.getColAsStringList(dfTemp, "日期");
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (dates.get(i).compareTo(todayDate) < 0) { // 倒序, 第一个小于 给定日期的, 即为 严格意义的上一个交易日
                // 此时i为  上 1 交易日. 因此..
                try {
                    return dates.get(i + 1 - n);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null; // 索引越界
                }
            }
        }
        return null;
    }

    /**
     * 获取上一交易日
     * 返回  yyyy-MM-dd 形式
     *
     * @param todayDate
     * @return
     * @throws Exception
     */
    public static String getPreTradeDateStrict(String todayDate) throws Exception {
        return getPreNTradeDateStrict(todayDate, 1); // 默认获取today上一交易日
    }


    /*
    字段相关静态属性
     */


    /**
     * 股票、ETF、债券 (历史) K 线表头
     * 待定添加:
     * 'f18': '昨日收盘', ???
     */
    private static void initEASTMONEYKLINEFIELDS() {
        EASTMONEY_KLINE_FIELDS = Dict.create()
                .set("f51", "日期")
                .set("f52", "开盘")
                .set("f53", "收盘")
                .set("f54", "最高")
                .set("f55", "最低")
                .set("f56", "成交量")
                .set("f57", "成交额")
                .set("f58", "振幅")
                .set("f59", "涨跌幅")
                .set("f60", "涨跌额")
                .set("f61", "换手率")
        ;
    }

    /**
     * 市场类型代码: 名称.  将在获取单个市场所有标的行情时用到
     */
    private static void initMARKETNUMBERDICT() {
        MARKET_NUMBER_DICT = Dict.create()
                .set("0", "深A")
                .set("1", "沪A")
                .set("8", "中金所")
                .set("90", "板块")
                .set("105", "美股")
                .set("106", "美股")
                .set("107", "美股")
                .set("113", "上期所")
                .set("114", "大商所")
                .set("115", "郑商所")
                .set("116", "港股")
                .set("128", "港股")
                .set("142", "上海能源期货交易所")
                .set("155", "英股")
        ;
    }


    /**
     * 常态行情表头: getRealtimeQuotes 实时截面数据api的表头
     */
    private static void initEASTMONEYQUOTEFIELDS() {
        Dict temp = Dict.create()
                .set("f12", STR_SEC_CODE)
                .set("f14", STR_SEC_NAME)
                .set("f3", "涨跌幅")
                .set("f2", "最新")
                .set("f15", "最高")
                .set("f16", "最低")
                .set("f17", "今开")
                .set("f4", "涨跌额")
                .set("f8", "换手率")
                .set("f10", "量比")
                .set("f9", "动态市盈率")
                .set("f5", "成交量")
                .set("f6", "成交额")
                .set("f18", "昨收")
                .set("f20", "总市值")
                .set("f21", "流通市值")
                .set("f13", "市场编号");
        EASTMONEY_QUOTE_FIELDS.putAll(temp);
    }

    /**
     * 实时截面数据可以传递的市场参数 --> 已经优化修改!!
     * 实际的市场参数可传递的列表是: 注意语义别重复了
     * [可转债, 沪深京A股, 沪深A股, 上证A股, 深证A股, 北证A股, 上证主板, 深证主板, 创业板, 科创板, 沪股通, 深股通, 风险警示板, 两网及退市,
     * 地域板块, 行业板块, 概念板块, 所有板块, 上证系列指数, 深证系列指数, 沪深系列指数, ETF, LOF, 美股, 港股, 英股, 中概股, 期货]
     */
    private static void initFSDICT() {
        MARKETS_ARGS_DICT = Dict.create()
                // 可转债
                .set("可转债", "b:MK0354")

                // 股票
                .set("沪深京A股", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048")
                .set("沪深A股", "m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23") // 沪深A, 不含京
                .set("上证A股", "m:1+t:2,m:1+t:23")
                .set("深证A股", "m:0+t:6,m:0+t:80")
                .set("北证A股", "m:0+t:81+s:2048")

                .set("上证主板", "m:1+t:2")
                .set("深证主板", "m:0+t:6")
                .set("沪深主板", "m:1+t:2,m:0+t:6")
                .set("创业板", "m:0+t:80")
                .set("科创板", "m:1+t:23")

                .set("沪股通", "b:BK0707")
                .set("深股通", "b:BK0804")

                .set("风险警示板", "m:0+f:4,m:1+f:4")
                .set("两网及退市", "m:0+s:3")

                // 板块
                .set("地域板块", "m:90+t:1+f:!50")
                .set("行业板块", "m:90+t:2+f:!50")
                .set("概念板块", "m:90+t:3+f:!50")
                .set("所有板块", "m:90+t:3+f:!50,m:90+t:2+f:!50,m:90+t:1+f:!50") // 三者相加

                // 指数
                .set("上证系列指数", "m:1+s:2")
                .set("深证系列指数", "m:0+t:5")
                .set("沪深系列指数", "m:1+s:2,m:0+t:5") // 相加

                // 基金
                .set("ETF", "b:MK0021,b:MK0022,b:MK0023,b:MK0024")
                .set("LOF", "b:MK0404,b:MK0405,b:MK0406,b:MK0407")

                // 美英港
                .set("美股", "m:105,m:106,m:107")
                .set("港股", "m:128+t:3,m:128+t:4,m:128+t:1,m:128+t:2")
                .set("英股", "m:155+t:1,m:155+t:2,m:155+t:3,m:156+t:1,m:156+t:2,m:156+t:5,m:156+t:6,m:156+t:7,m:156+t:8")
                .set("中概股", "b:MK0201") // 期货, 组合5大期货

                // 期货
                .set("期货", "m:113,m:114,m:115,m:8,m:142") // 期货, 组合各大期货所?
        ;

    }

}
