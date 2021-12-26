package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.utils.log.LogUtils;
import com.sun.jna.platform.win32.WinBase;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.getAsStr;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.DEFAULT_TIMEOUT;
import static com.scareers.utils.JsonUtil.jsonStrToDf;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class StockApi {
    private static final Log log = LogUtils.getLogger();
    public static ConcurrentHashMap<String, String> FS_DICT = new ConcurrentHashMap<>();

    static {
        initFSDICT();
    }

    /**
     * python json.dumps() 复制而来, java处理为 ConcurrentHashMap
     * 实际的市场参数可传递的列表是:
     * ['bond', '可转债', 'stock', '沪深A股', '沪深京A股', '北证A股', '北A', 'futures', '期货', '上证A股', '沪A',
     * '深证A股', '深A', '新股', '创业板', '科创板', '沪股通', '深股通', '风险警示板', '两网及退市',
     * '地域板块', '行业板块', '概念板块', '上证系列指数', '深证系列指数', '沪深系列指数', 'ETF', 'LOF',
     * '美股', '港股', '英股', '中概股', '中国概念股']
     */
    private static void initFSDICT() {
        String rawJsonFromPython = "{\"bond\": \"b:MK0354\", \"\\u53ef\\u8f6c\\u503a\": \"b:MK0354\", \"stock\": " +
                "\"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u6caa\\u6df1A\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23\", \"\\u6caa\\u6df1\\u4eacA\\u80a1\": \"m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048\", \"\\u5317\\u8bc1A\\u80a1\": \"m:0 t:81 s:2048\", \"\\u5317A\": \"m:0 t:81 s:2048\", \"futures\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u671f\\u8d27\": \"m:113,m:114,m:115,m:8,m:142\", \"\\u4e0a\\u8bc1A\\u80a1\": \"m:1 t:2,m:1 t:23\", \"\\u6caaA\": \"m:1 t:2,m:1 t:23\", \"\\u6df1\\u8bc1A\\u80a1\": \"m:0 t:6,m:0 t:80\", \"\\u6df1A\": \"m:0 t:6,m:0 t:80\", \"\\u65b0\\u80a1\": \"m:0 f:8,m:1 f:8\", \"\\u521b\\u4e1a\\u677f\": \"m:0 t:80\", \"\\u79d1\\u521b\\u677f\": \"m:1 t:23\", \"\\u6caa\\u80a1\\u901a\": \"b:BK0707\", \"\\u6df1\\u80a1\\u901a\": \"b:BK0804\", \"\\u98ce\\u9669\\u8b66\\u793a\\u677f\": \"m:0 f:4,m:1 f:4\", \"\\u4e24\\u7f51\\u53ca\\u9000\\u5e02\": \"m:0 s:3\", \"\\u5730\\u57df\\u677f\\u5757\": \"m:90 t:1 f:!50\", \"\\u884c\\u4e1a\\u677f\\u5757\": \"m:90 t:2 f:!50\", \"\\u6982\\u5ff5\\u677f\\u5757\": \"m:90 t:3 f:!50\", \"\\u4e0a\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2\", \"\\u6df1\\u8bc1\\u7cfb\\u5217\\u6307\\u6570\": \"m:0 t:5\", \"\\u6caa\\u6df1\\u7cfb\\u5217\\u6307\\u6570\": \"m:1 s:2,m:0 t:5\", \"ETF\": \"b:MK0021,b:MK0022,b:MK0023,b:MK0024\", \"LOF\": \"b:MK0404,b:MK0405,b:MK0406,b:MK0407\", \"\\u7f8e\\u80a1\": \"m:105,m:106,m:107\", \"\\u6e2f\\u80a1\": \"m:128 t:3,m:128 t:4,m:128 t:1,m:128 t:2\", \"\\u82f1\\u80a1\": \"m:155 t:1,m:155 t:2,m:155 t:3,m:156 t:1,m:156 t:2,m:156 t:5,m:156 t:6,m:156 t:7,m:156 t:8\", \"\\u4e2d\\u6982\\u80a1\": \"b:MK0201\", \"\\u4e2d\\u56fd\\u6982\\u5ff5\\u80a1\": \"b:MK0201\"}\n";
        Map<String, Object> temp = JSONUtil.parseObj(rawJsonFromPython);
        for (String key : temp.keySet()) {
            FS_DICT.put(key, temp.get(key).toString());
        }
    }


    public static void main(String[] args) throws Exception {
        List<StockBean> stocks = new StockPoolForFSTransaction().createStockPool();
        for (StockBean stock : stocks) {
            Console.log(getFSTransaction(100, stock.getStockCodeSimple(), stock.getMarket()));
        }
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
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market, int timeout) {
        List<String> columns = Arrays.asList("stock_code", "market", "time_tick", "price", "vol", "bs");
        DataFrame<Object> res = new DataFrame<>(columns);
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

            response = getAsStr(keyUrl, params, timeout);
        } catch (Exception e) {
            // e.printStackTrace();
            log.error("get exception: 访问http失败: stock: {}.{}", market, stockCodeSimple);
            return res;
        }

        try {
            res = jsonStrToDf(response, "(", ")", columns,
                    Arrays.asList("data", "details"), String.class, Arrays.asList(3),
                    Arrays.asList(stockCodeSimple, market));
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("get exception: 获取数据错误. stock: {}.{}", market, stockCodeSimple);
            log.warn("raw data: 原始响应字符串: {}", response);
        }
        return res;
    }


    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market) {
        return getFSTransaction(lastRecordAmounts, stockCodeSimple, market, DEFAULT_TIMEOUT);
    }

    /**
     * efinance get_realtime_quotes 重载实现.  给定市场列表, 返回, 这些市场所有股票列表 截面数据
     *
     * @param markets
     * @return
     */
    public static DataFrame<Object> getRealtimeQuotes(List<String> markets) {

    }


}
