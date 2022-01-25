package com.scareers.datasource.eastmoney.stock;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.annotations.Cached;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.Tqdm;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.*;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.DEFAULT_TIMEOUT;
import static com.scareers.utils.JSONUtilS.jsonStrToDf;

/**
 * description: 东方财富股票 相关api; 可选底层http实现: hutool.http 或者 Kevin 库, 某些api仅其中之一可正常访问.
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class StockApi {
    private static final Log log = LogUtil.getLogger();
    public static ConcurrentHashMap<String, String> FS_DICT = new ConcurrentHashMap<>();
    public static Map<Object, Object> EASTMONEY_QUOTE_FIELDS = new ConcurrentHashMap<>();
    public static Map<String, Object> EASTMONEY_KLINE_FIELDS = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> MARKET_NUMBER_DICT = new ConcurrentHashMap<>();
    public static Cache<String, DataFrame<Object>> getQuoteHistorySingleCache = CacheUtil.newLRUCache(1024,
            24 * 3600 * 1000); // k线

    public static ThreadPoolExecutor poolExecutor;

    public static void checkPoolExecutor() {
        if (poolExecutor == null) {
            poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("klineGet-", null, true));

        }
    }


    public static void main(String[] args) throws Exception {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        //Console.log(getPriceLimitToday("000001", 2000));


        //7 23.50 ok
        Console.log(getFSTransaction(120, "000001", 1, 1000).toString(250));
        // 分时成交:
        // 02:11 昨日df
        // 06:00
        // 08:55 得到空df,
        // 09:10
        // 09:14:x 得到空df,
        // 09:15:x 得到空df,
        // 09:24:x 得到空df,
        // 09:25:x 单行记录 09:25:07 	3508.24	2924545	1 , 表示竞价结果, 意味着无竞价过程.
        // 09:30:x 正常添加记录df
        // 09:40:x 正常df
        // 11:30:x 正常df, 但最大时间可能超过 11:30:00, 少数.
        // 13:00:x 正常df, 第一个常为 13:00:01
        // 14:54:x 正常df
        // 14:57:x 正常df, 但 14:57:05后一直不会有新数据, 持续到收盘 15:00:00 有1-2条收盘数据
        // 14:59:x 正常df, 14:57:xx后无新数据
        // 15:00:x 正常df, 最后1-2条 15:00:00 tick
        // 23:50:x 正常df, 未重置刷新,同 15:00:x
        // 周六同周五收盘后


        Console.log(getRealtimeQuotes(Arrays.asList("stock", "可转债")));
        // 实时截面数据
        // 00:00
        // 02:10 昨日df
        // 08:55 能得到正确df,但 只有动态市盈率,昨日收盘和市值字段可用. 其他数值字段用 - 填充, 将解析失败
        // 09:10
        // 09:14:x 占位df
        // 09:15:x 涨跌幅,最新价,涨跌额填充,其余 -
        // 09:24:x 涨跌幅,最新价,涨跌额填充,其余 -
        // 09:25:x 正常全填充df
        // 09:30:x 正常df
        // 09:40:x 正常df
        // 11:30:x 正常df
        // 13:00:x 正常df
        // 14:54:x 正常df
        // 14:57:x 正常df
        // 14:59:x 正常df
        // 15:00:x 正常df
        // 23:50:x 正常df

        Console.log(getFs1MToday("000001", false, 0, 2000));
        // 1分钟分时图
        // 02:10 昨日df
        // 06:00
        // 08:55
        // 09:10
        // 09:14:x 得到空df,
        // 09:15:x 得到空df,
        // 09:24:x 得到空df,
        // 09:25:x 单行 9:31 的df.
        // 09:30:x 单行 9:31 的df.
        // 09:40:x 正常df, 分钟+1: 得到 41, 意味着此刻xx分钟 视为 xx+1 的分时图(但没有固定), 分时图xx分钟已固定
        // 11:30:x 正常df, 显然最大只能达到 11:30 (合理)
        // 13:00:x 于13:00:01, 立即开始出现 13:01 的数据, 规律不变.
        // 14:54:x 正常df, 算后一分
        // 14:57:x 正常df, 算后一分
        // 14:59:x 正常df, 算后一分, 此时已经出现 15:00 的分时图, 只是动态的,等待15:00:00 盖棺定论
        // 15:00:x 正常df, 不再有 15:01, 盖棺定论
        // 23:50:x 正常df, 同上截止于 15:00


        Console.log(getQuoteHistorySingle("000001", null, null, "101", "qfq", 3));
        // 日k线
        // 00:00
        // 02:10 截至昨天df
        // 08:55 截至昨天df
        // 09:14:x 截至昨天df
        // 09:15:x 截至昨天df
        // 09:24:x 截至昨天df
        // 09:25:x 已经出现含今日df
        // 09:30:x 含今日正常df
        // 09:40:x 含今日正常df
        // 11:30:x 含今日正常df
        // 13:00:x 含今日正常df
        // 14:54:x 含今日正常df
        // 14:57:x 含今日正常df
        // 14:59:x 含今日正常df
        // 15:00:x 含今日正常df
        // 21:00:x 含今日正常df
        // 23:50:x 含今日正常df


//        Console.log(getPreNTradeDateStrict("2021-01-08"));
//        Console.log(timer.intervalRestart());

    }

    static {
        initFSDICT();
        initEASTMONEYQUOTEFIELDS();
        initMARKETNUMBERDICT();
        initEASTMONEYKLINEFIELDS();
    }

    /**
     * # 股票、ETF、债券 K 线表头
     * EASTMONEY_KLINE_FIELDS = {
     * 'f51': '日期',
     * 'f52': '开盘',
     * 'f53': '收盘',
     * 'f54': '最高',
     * 'f55': '最低',
     * 'f56': '成交量',
     * 'f57': '成交额',
     * 'f58': '振幅',
     * 'f59': '涨跌幅',
     * 'f60': '涨跌额',
     * 'f61': '换手率'
     * <p>
     * }
     * <p>
     * 'f18': '昨日收盘', ???
     */
    private static void initEASTMONEYKLINEFIELDS() {
        String rawJsonFromPython = "{\"f51\": \"\\u65e5\\u671f\", \"f52\": \"\\u5f00\\u76d8\", \"f53\": \"\\u6536\\u76d8\", \"f54\": \"\\u6700\\u9ad8\", \"f55\": \"\\u6700\\u4f4e\", \"f56\": \"\\u6210\\u4ea4\\u91cf\", \"f57\": \"\\u6210\\u4ea4\\u989d\", \"f58\": \"\\u632f\\u5e45\", \"f59\": \"\\u6da8\\u8dcc\\u5e45\", \"f60\": \"\\u6da8\\u8dcc\\u989d\", \"f61\": \"\\u6362\\u624b\\u7387\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            EASTMONEY_KLINE_FIELDS.put(key, temp.get(key).toString());
        }
        // EASTMONEY_KLINE_FIELDS.put("f18", "昨日收盘");  // 无此字段
    }

    private static void initMARKETNUMBERDICT() {
        String rawJsonFromPython = "{\"0\": \"\\u6df1A\", \"1\": \"\\u6caaA\", \"105\": \"\\u7f8e\\u80a1\", \"106\": " +
                "\"\\u7f8e\\u80a1\", \"107\": \"\\u7f8e\\u80a1\", \"116\": \"\\u6e2f\\u80a1\", \"128\": \"\\u6e2f\\u80a1\", \"113\": \"\\u4e0a\\u671f\\u6240\", \"114\": \"\\u5927\\u5546\\u6240\", \"115\": \"\\u90d1\\u5546\\u6240\", \"8\": \"\\u4e2d\\u91d1\\u6240\", \"142\": \"\\u4e0a\\u6d77\\u80fd\\u6e90\\u671f\\u8d27\\u4ea4\\u6613\\u6240\", \"155\": \"\\u82f1\\u80a1\", \"90\": \"\\u677f\\u5757\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            MARKET_NUMBER_DICT.put(key, temp.get(key).toString());
        }
    }


    /**
     * 常态表头
     * ['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21', 'f13']
     * <p>
     * # 股票、债券榜单表头
     * EASTMONEY_QUOTE_FIELDS = {
     * 'f12': '代码',
     * 'f14': '名称',
     * 'f3': '涨跌幅',
     * 'f2': '最新价',
     * 'f15': '最高',
     * 'f16': '最低',
     * 'f17': '今开',
     * 'f4': '涨跌额',
     * 'f8': '换手率',
     * 'f10': '量比',
     * 'f9': '动态市盈率',
     * 'f5': '成交量',
     * 'f6': '成交额',
     * 'f18': '昨日收盘',
     * 'f20': '总市值',
     * 'f21': '流通市值',
     * 'f13': '市场编号'
     * }
     */
    private static void initEASTMONEYQUOTEFIELDS() {
        String rawJsonFromPython = "{\"f12\": \"\\u4ee3\\u7801\", \"f14\": \"\\u540d\\u79f0\", \"f3\": " +
                "\"\\u6da8\\u8dcc\\u5e45\", \"f2\": \"\\u6700\\u65b0\\u4ef7\", \"f15\": \"\\u6700\\u9ad8\", \"f16\": \"\\u6700\\u4f4e\", \"f17\": \"\\u4eca\\u5f00\", \"f4\": \"\\u6da8\\u8dcc\\u989d\", \"f8\": \"\\u6362\\u624b\\u7387\", \"f10\": \"\\u91cf\\u6bd4\", \"f9\": \"\\u52a8\\u6001\\u5e02\\u76c8\\u7387\", \"f5\": \"\\u6210\\u4ea4\\u91cf\", \"f6\": \"\\u6210\\u4ea4\\u989d\", \"f18\": \"\\u6628\\u65e5\\u6536\\u76d8\", \"f20\": \"\\u603b\\u5e02\\u503c\", \"f21\": \"\\u6d41\\u901a\\u5e02\\u503c\", \"f13\": \"\\u5e02\\u573a\\u7f16\\u53f7\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            EASTMONEY_QUOTE_FIELDS.put(key, temp.get(key).toString());
        }
    }

    /**
     * python json.dumps() 复制而来, java处理为 ConcurrentHashMap
     * 实际的市场参数可传递的列表是:
     * ['bond', '可转债', 'stock', '沪深A股', '沪深京A股', '北证A股', '北A', 'futures', '期货', '上证A股', '沪A',
     * '深证A股', '深A', '新股', '创业板', '科创板', '沪股通', '深股通', '风险警示板', '两网及退市',
     * '地域板块', '行业板块', '概念板块', '上证系列指数', '深证系列指数', '沪深系列指数', 'ETF', 'LOF',
     * '美股', '港股', '英股', '中概股', '中国概念股']
     * <p>
     * # ! Powerful
     * FS_DICT = {
     * # 可转债
     * 'bond': 'b:MK0354',
     * '可转债': 'b:MK0354',
     * 'stock': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048',
     * # 沪深A股
     * # 'stock': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23',
     * '沪深A股': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23',
     * '沪深京A股': 'm:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048',
     * '北证A股': 'm:0 t:81 s:2048',
     * '北A': 'm:0 t:81 s:2048',
     * # 期货
     * 'futures': 'm:113,m:114,m:115,m:8,m:142',
     * '期货': 'm:113,m:114,m:115,m:8,m:142',
     * <p>
     * '上证A股': 'm:1 t:2,m:1 t:23',
     * '沪A': 'm:1 t:2,m:1 t:23',
     * <p>
     * '深证A股': 'm:0 t:6,m:0 t:80',
     * '深A': 'm:0 t:6,m:0 t:80',
     * <p>
     * # 沪深新股
     * '新股': 'm:0 f:8,m:1 f:8',
     * <p>
     * '创业板': 'm:0 t:80',
     * '科创板': 'm:1 t:23',
     * '沪股通': 'b:BK0707',
     * '深股通': 'b:BK0804',
     * '风险警示板': 'm:0 f:4,m:1 f:4',
     * '两网及退市': 'm:0 s:3',
     * <p>
     * # 板块
     * '地域板块': 'm:90 t:1 f:!50',
     * '行业板块': 'm:90 t:2 f:!50',
     * '概念板块': 'm:90 t:3 f:!50',
     * <p>
     * # 指数
     * '上证系列指数': 'm:1 s:2',
     * '深证系列指数': 'm:0 t:5',
     * '沪深系列指数': 'm:1 s:2,m:0 t:5',
     * # ETF 基金
     * 'ETF': 'b:MK0021,b:MK0022,b:MK0023,b:MK0024',
     * # LOF 基金
     * 'LOF': 'b:MK0404,b:MK0405,b:MK0406,b:MK0407',
     * <p>
     * '美股': 'm:105,m:106,m:107',
     * '港股': 'm:128 t:3,m:128 t:4,m:128 t:1,m:128 t:2',
     * '英股': 'm:155 t:1,m:155 t:2,m:155 t:3,m:156 t:1,m:156 t:2,m:156 t:5,m:156 t:6,m:156 t:7,m:156 t:8',
     * '中概股': 'b:MK0201',
     * '中国概念股': 'b:MK0201'
     * <p>
     * }
     */
    private static void initFSDICT() {
        String rawJsonFromPython = "{\"bond\": \"b:MK0354\", \"\\u53ef\\u8f6c\\u503a\": \"b:MK0354\", \"stock\": " +
                "\"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u6caa\\u6df1A\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23\", \"\\u6caa\\u6df1\\u4eacA\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u5317\\u8bc1A\\u80a1\": \"m:0 t:81 s:2048\", \"\\u5317A\": \"m:0 t:81 s:2048\", \"futures\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u671f\\u8d27\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u4e0a\\u8bc1A\\u80a1\": \"m:1 t:2,m:1 t:23\", \"\\u6caaA\": \"m:1 t:2,m:1 t:23\", \"\\u6df1\\u8bc1A\\u80a1\": \"m:0 t:6,m:0 t:80\", \"\\u6df1A\": \"m:0 t:6,m:0 t:80\", \"\\u65b0\\u80a1\": \"m:0 f:8,m:1 f:8\", \"\\u521b\\u4e1a\\u677f\": \"m:0 t:80\", \"\\u79d1\\u521b\\u677f\": \"m:1 t:23\", \"\\u6caa\\u80a1\\u901a\": \"b:BK0707\", \"\\u6df1\\u80a1\\u901a\": \"b:BK0804\", \"\\u98ce\\u9669\\u8b66\\u793a\\u677f\": \"m:0 f:4,m:1 f:4\", \"\\u4e24\\u7f51\\u53ca\\u9000\\u5e02\": \"m:0 s:3\", \"\\u5730\\u57df\\u677f\\u5757\": \"m:90 t:1 f:!50\", \"\\u884c\\u4e1a\\u677f\\u5757\": \"m:90 t:2 f:!50\", \"\\u6982\\u5ff5\\u677f\\u5757\": \"m:90 t:3 f:!50\", \"\\u4e0a\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2\", \"\\u6df1\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:0 t:5\", \"\\u6caa\\u6df1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2,m:0 t:5\", \"ETF\": \"b:MK0021,b:MK0022,b:MK0023,b:MK0024\", \"LOF\": \"b:MK0404,b:MK0405,b:MK0406,b:MK0407\", \"\\u7f8e\\u80a1\": \"m:105,m:106,m:107\", \"\\u6e2f\\u80a1\": \"m:128 t:3,m:128 t:4,m:128 t:1,m:128 t:2\", \"\\u82f1\\u80a1\": \"m:155 t:1,m:155 t:2,m:155 t:3,m:156 t:1,m:156 t:2,m:156 t:5,m:156 t:6,m:156 t:7,m:156 t:8\", \"\\u4e2d\\u6982\\u80a1\": \"b:MK0201\", \"\\u4e2d\\u56fd\\u6982\\u5ff5\\u80a1\": \"b:MK0201\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            FS_DICT.put(key, temp.get(key).toString());
        }
    }

    /**
     * 获取当日某个股的涨跌停限价. [涨停价,跌停价]
     * 东方财富行情页面, 主api之一, 有很多字段, 包括盘口. 均使用此url, 可传递极多字段
     * http://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f43,f57,f58,f169,f170,f46,f44,f51,f168,f47,f164,f163,f116,f60,f45,f52,f50,f48,f167,f117,f71,f161,f49,f530,f135,f136,f137,f138,f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104,f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78,f110,f260,f261,f262,f263,f264,f267,f268,f250,f251,f252,f253,f254,f255,f256,f257,f258,f266,f269,f270,f271,f273,f274,f275,f127,f199,f128,f193,f196,f194,f195,f197,f80,f280,f281,f282,f284,f285,f286,f287,f292,f293,f181,f294,f295,f279,f288&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     * <p>
     * 其中 f51(涨停),f52(跌停) 是该股票涨跌停价格
     * jQuery112409228148447288975_1643015501069({"rc":0,"rt":4,"svr":181233083,"lt":1,"full":1,"data":{"f51":39.89,"f52":26.59}});
     * https://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f51,f52&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     *
     * @param stock
     * @return
     */
    public static List<Double> getPriceLimitToday(String stockSimpleCode, int timeout) throws Exception {
        SecurityBeanEm bean = SecurityBeanEm.createStock(stockSimpleCode);
        String secId = bean.getSecId(); // 获取准确的secId

        String url = "https://push2.eastmoney.com/api/qt/stock/get";
        List<Double> res = new ArrayList<>();
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
        params.put("invt", "2");
        params.put("fltt", "2");
        params.put("fields", "f51,f52");
        params.put("secid", secId);
        params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseKevin(url, params, timeout, 3);
        } catch (Exception e) {
            e.printStackTrace();

            log.error("get exception: 访问http失败: 获取涨跌停价: stock: {}", secId);
            return null;
        }
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        JSONObject resp = JSONUtil.parseObj(response);

        res.add(Double.valueOf(resp.getByPath("data.f51").toString())); // 涨停价
        res.add(Double.valueOf(resp.getByPath("data.f52").toString())); // 跌停价
        return res;
    }


    /**
     * 获取分时成交
     * // @using
     * https://push2.eastmoney.com/api/qt/stock/details/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1400&secid=0.000153&cb=jQuery112409885675811656662_1640099646776&_=1640099646777
     * // @noti: 升序, 但是最新 x 条数据
     *
     * @param lastRecordAmounts 单页数量,
     * @param stockCodeSimple   股票/指数简单代码, 不再赘述
     * @param market            0 深市  1 沪市    (上交所暂时 0)
     * @return 出错则返回空df, 不抛出异常
     * @noti: 8:55  details字段已经重置为 [] 空列表  todo: 时间限制更严格
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market, int timeout) throws Exception {
        List<String> columns = Arrays.asList("stock_code", "market", "time_tick", "price", "vol", "bs");
        DataFrame<Object> res;
        String keyUrl = "https://push2.eastmoney.com/api/qt/stock/details/get";
        String response;
        try {
            Map<String, Object> params = new HashMap<>(); // 参数map
            params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
            params.put("fields1", "f1,f2,f3,f4");
            params.put("fields2", "f51,f52,f53,f54,f55");
            params.put("pos", StrUtil.format("-{}", lastRecordAmounts));
            params.put("secid", StrUtil.format("{}.{}", market, stockCodeSimple));
            params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                    System.currentTimeMillis() - RandomUtil.randomInt(1000)));
            params.put("_", System.currentTimeMillis());

            response = getAsStrUseHutool(keyUrl, params, timeout, 1);
        } catch (Exception e) {
            log.error("get exception: 访问http失败: stock: {}.{}", market, stockCodeSimple);
            throw e;
        }

        try {
            res = jsonStrToDf(response, "(", ")", columns,
                    Arrays.asList("data", "details"), String.class, Arrays.asList(3),
                    Arrays.asList(stockCodeSimple, market));
        } catch (Exception e) {
            log.warn("get exception: 获取数据错误. stock: {}.{}", market, stockCodeSimple);
            log.warn("raw data: 原始响应字符串: {}", response);
            throw e;
        }
        return res;
    }


    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market) throws Exception {
        return getFSTransaction(lastRecordAmounts, stockCodeSimple, market, DEFAULT_TIMEOUT);
    }

    /**
     * efinance get_realtime_quotes 重载实现.  给定市场列表, 返回, 这些市场所有股票列表 截面数据
     * // ['f12', 'f14', 'f3', 'f2', 'f15', 'f16', 'f17', 'f4', 'f8', 'f10', 'f9', 'f5', 'f6', 'f18', 'f20', 'f21',
     * 'f13']
     * <p>
     * * ['bond', '可转债', 'stock', '沪深A股', '沪深京A股', '北证A股', '北A', 'futures', '期货', '上证A股', '沪A',
     * * '深证A股', '深A', '新股', '创业板', '科创板', '沪股通', '深股通', '风险警示板', '两网及退市',
     * * '地域板块', '行业板块', '概念板块', '上证系列指数', '深证系列指数', '沪深系列指数', 'ETF', 'LOF',
     * * '美股', '港股', '英股', '中概股', '中国概念股']
     *
     * @param markets
     * @return
     * @see StockApi.FS_DICT
     * @see StockApi.EASTMONEY_QUOTE_FIELDS
     * @see StockApi.MARKET_NUMBER_DICT
     */
    public static DataFrame<Object> getRealtimeQuotes(List<String> markets) {
        List<String> realMarketArgs = markets.stream().map(value -> FS_DICT.get(value)).collect(Collectors.toList());
        String marketArgsStr = StrUtil.join(",", realMarketArgs);
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
        String response = getAsStrUseKevin(url, params, 4000);
        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields, Arrays.asList("data", "diff"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(EASTMONEY_QUOTE_FIELDS);
        dfTemp = dfTemp.add("行情id", values ->
                values.get(16).toString() + "." + values.get(0).toString()
        );
        dfTemp = dfTemp.add("市场类型", values ->
                MARKET_NUMBER_DICT.get(values.get(16).toString())
        );
        dfTemp = dfTemp.rename("代码", "股票代码");
        dfTemp = dfTemp.rename("名称", "股票名称");
        return dfTemp;
    }

    /**
     * 获取k线. 复刻 efinance   get_quote_history
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	  涨跌幅	  涨跌额	 换手率	  股票代码	股票名称
     * <p>
     * 获取股票的 K 线数据
     * <p>
     * Parameters
     * ----------
     * stock_codes : Union[str,List[str]]
     * 股票代码、名称 或者 股票代码、名称构成的列表
     * beg : str, optional
     * 开始日期，默认为 ``'19000101'`` ，表示 1900年1月1日
     * end : str, optional
     * 结束日期，默认为 ``'20500101'`` ，表示 2050年1月1日
     * klt : int, optional
     * 行情之间的时间间隔，默认为 ``101`` ，可选示例如下
     * <p>
     * - ``1`` : 分钟
     * - ``5`` : 5 分钟
     * - ``15`` : 15 分钟
     * - ``30`` : 30 分钟
     * - ``60`` : 60 分钟
     * - ``101`` : 日
     * - ``102`` : 周
     * - ``103`` : 月
     * <p>
     * fqt : int, optional
     * 复权方式，默认为 ``1`` ，可选示例如下
     * <p>
     * - ``0`` : 不复权
     * - ``1`` : 前复权
     * - ``2`` : 后复权
     * <p>
     * Returns
     * -------
     * Union[DataFrame, Dict[str, DataFrame]]
     * 股票的 K 线数据
     * <p>
     * - ``DataFrame`` : 当 ``stock_codes`` 是 ``str`` 时
     * - ``Dict[str, DataFrame]`` : 当 ``stock_codes`` 是 ``List[str]`` 时
     * <p>
     * Examples
     * // @noti: 前后两日期都包含, 且天然升序!
     *
     * @param stockCodesSimple
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param isIndex          给的股票代码列表, 是否指数??
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static ConcurrentHashMap<String, DataFrame<Object>> getQuoteHistory(List<String> stockCodesSimple,
                                                                               String begDate,
                                                                               String endDate,
                                                                               String klType, String fq,
                                                                               int retrySingle,
                                                                               boolean isIndex,
                                                                               int timeoutOfReq,
                                                                               boolean useCache
    )
            throws ExecutionException, InterruptedException {

        log.info("klines gets batch: 线程池批量获取k线, 股票数量: {}", stockCodesSimple.size());
        checkPoolExecutor();
        HashMap<String, Future<DataFrame<Object>>> futures = new HashMap<>();
        for (String stock : stockCodesSimple) {
            futures.put(stock, poolExecutor.submit(new Callable<DataFrame<Object>>() {
                @Override
                public DataFrame<Object> call() throws Exception {
                    return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, isIndex,
                            timeoutOfReq, useCache);
                }
            }));
        }
        ConcurrentHashMap<String, DataFrame<Object>> res = new ConcurrentHashMap<>();
        for (String stock : Tqdm.tqdm(stockCodesSimple, "process: ")) {
            DataFrame<Object> dfTemp = futures.get(stock).get();
            if (dfTemp != null) {
                res.put(stock, dfTemp);
            }
        }
        Console.log();
        log.info("finish klines gets batch: 线程池批量获取k线完成");
        return res;
    }

    /**
     * 单股票k线:
     * 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  股票代码	股票名称
     *
     * @param stock       @noti: 绝对传递 simple模式, 是否指数由  isIndex 参数控制
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param quoteIdMode
     * @return
     */
    @Cached
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle, boolean isIndex, int timeout,
                                                          boolean useCache)
            throws Exception {
        if (begDate == null) {
            begDate = "19900101";
        }
        if (endDate == null) {
            endDate = "20500101";
        }
        begDate = begDate.replace("-", ""); // 标准化
        endDate = endDate.replace("-", "");

        String cacheKey = StrUtil.format("{}__{}__{}__{}__{}__{}__{}", stock, begDate, endDate, klType, fq, retrySingle,
                isIndex);

        DataFrame<Object> res = null;
        if (useCache) { // 必须使用caCache 且真的存在缓存
            res = getQuoteHistorySingleCache.get(cacheKey);
        }
        if (res != null) {
            return res;
        }
        String fieldsStr = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61";// k线字段
        List<String> fields = StrUtil.split(fieldsStr, ",");
        JSONArray quoteRes = querySecurityId(stock);
        SecurityBeanEm bean = new SecurityBeanEm(quoteRes);
        String quoteId = null;
        try { // 转换失败
            quoteId = isIndex ? bean.convertToIndex().getSecId() : bean.convertToStock().getSecId();
        } catch (Exception e) {
            throw e;
        }

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
            response = getAsStrUseHutool(url, params, timeout, retrySingle);
        } catch (Exception e) {
            return null;
        }
        DataFrame<Object> dfTemp = jsonStrToDf(response, null, null,
                fields.stream().map(value -> EASTMONEY_KLINE_FIELDS.get(value).toString())
                        .collect(Collectors.toList()),
                Arrays.asList("data", "klines"), String.class, Arrays.asList(),
                Arrays.asList());

        dfTemp = dfTemp.add("股票代码", values -> bean.getStockCodeSimple());
        res = dfTemp.add("股票名称", values -> bean.getName());
        getQuoteHistorySingleCache.put(cacheKey, res); // 将更新
        return res;
    }

    /**
     * 默认使用cache
     *
     * @param stock
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @param isIndex
     * @param timeout
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle, boolean isIndex, int timeout
    ) throws Exception {
        return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, isIndex, timeout, true);
    }

    /**
     * 默认使用cache
     *
     * @param stock
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retrySingle
     * @return
     * @throws Exception
     */
    public static DataFrame<Object> getQuoteHistorySingle(String stock, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retrySingle)
            throws Exception {
        return getQuoteHistorySingle(stock, begDate, endDate, klType, fq, retrySingle, false, 2000);
    }

    /**
     * @param stock
     * @param isIndex
     * @param retrySingle
     * @param timeout
     * @return 1分钟分时图当日; 当某分钟开始后(即0秒以后, fs将更新到当分钟 + 1. 例如当前 13 : 21 : 10, 则将更新到 13 : 22
     * @throws Exception
     */
    public static DataFrame<Object> getFs1MToday(String stock, boolean isIndex, int retrySingle, int timeout)
            throws Exception {
        return getQuoteHistorySingle(stock, null, null, "1", "qfq", retrySingle, isIndex, timeout);
    }

    /**
     * 给定一个具体日期, yyyy-MM-dd 形式, 因为em默认日期格式是这样, 方便比较.  返回上n个交易日的日期,
     * 原理上查询 上证指数 所有历史日k线, 获取得到日期列表. 倒序遍历即可
     * 返回  yyyy-MM-dd 形式
     *
     * @param todayDate
     * @return
     */
    public static String getPreNTradeDateStrict(String todayDate, int n)
            throws Exception {
        // 查询结果将被缓存.
        DataFrame<Object> dfTemp = getQuoteHistorySingle("000001", "19900101", "21000101", "101", "1", 3, true, 3000);
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

    public static String getPreNTradeDateStrict(String todayDate) throws Exception {
        return getPreNTradeDateStrict(todayDate, 1); // 默认获取today上一交易日
    }

}
