package com.scareers.datasource.eastmoney.datacenter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.getAsStrUseHutool;
import static com.scareers.utils.JSONUtilS.jsonStrToDf;

/**
 * description: 东财数据中心api
 *
 * @author: admin
 * @date: 2022/2/25/025-19:12:51
 */
public class EmDataApi {
    public static Map<Object, Object> SuspensionFieldsMap = new ConcurrentHashMap<>(); // 停牌字段Map
    public static List<String> SuspensionFields; // 停牌字段
    public static Map<Object, Object> FutureMarketCloseDatesFieldsMap = new ConcurrentHashMap<>(); // 休市字段
    public static List<String> FutureMarketCloseDatesFields; // 休市字段
    public static List<String> EconomicCalendarFields; // 财经日历字段
    public static Map<Object, Object> EconomicCalendarFieldsMap = new ConcurrentHashMap<>(); // 财经日历字段

    static {
        initSuspensionFields();
        initFutureMarketCloseDatesFields();
        initEconomicCalendarFields();
    }


    public static void main(String[] args) {
//        Console.log("获取停牌股票代码列表");
//        Console.log(getSuspensionStockCodes(DateUtil.today(), 2000, 3));
//        Console.log(getSuspensions(DateUtil.today(), 2000, 3));
//
//        Console.log("获取近期未来休市安排");
//        Console.log(getFutureMarketCloseDates(3000, 3));


        Console.log("获取日期区间 财经日历");
        Console.log(getEconomicCalendarByDateRange("2022-03-08", "2022-03-11", 3000, 3));

    }

    /**
     * 今日停牌 股票列表 --> 数据中心 -- 停复牌
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112309079454213619864_1645787131827&sortColumns=SUSPEND_START_DATE&sortTypes=-1&pageSize=500&pageNumber=1&reportName=RPT_CUSTOM_SUSPEND_DATA_INTERFACE&columns=ALL&source=WEB&client=WEB&filter=(MARKET%3D%22%E5%85%A8%E9%83%A8%22)(DATETIME%3D%272022-02-25%27)
     *
     * @param date 必须 yyyy-MM-dd 形式
     * @return
     * @cols [资产代码, 资产名称, 停牌开始时间, 停牌结束时间, 停牌期限, 停牌原因, 所属市场, 停牌开始日期, 预计复牌日期, 交易市场代码, 资产类型代码] SuspensionFields
     */
    public static DataFrame<Object> getSuspensions(String date, int timeout, int retry) {
        String url = "https://datacenter-web.eastmoney.com/api/data/v1/get";

        HashMap<String, Object> params = new HashMap<>();
        params.put("callback", "jQuery112309079454213619864_" + (System.currentTimeMillis() - 1));
        params.put("sortColumns", "SUSPEND_START_DATE");
        params.put("sortTypes", "-1");
        params.put("pageSize", "5000");
        params.put("pageNumber", "1");
        params.put("reportName", "RPT_CUSTOM_SUSPEND_DATA_INTERFACE");
        params.put("columns", "ALL");
        params.put("source", "WEB");
        params.put("client", "WEB");
        params.put("filter", StrUtil.format("(MARKET=\"全部\")(DATETIME='{}')", date));

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }

        DataFrame<Object> dfTemp = jsonStrToDf(response, "(", ")",
                SuspensionFields,
                Arrays.asList("result", "data"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(SuspensionFieldsMap);
        return dfTemp;
    }

    /**
     * 仅获取停牌股票代码列表
     *
     * @param date
     * @param timeout
     * @param retry
     * @return
     */
    public static List<String> getSuspensionStockCodes(String date, int timeout, int retry) {
        DataFrame<Object> suspensions = getSuspensions(date, timeout, retry);
        if (suspensions == null) {
            log.error("getSuspensionStockCodes: 获取今日停牌数据失败, 返回空列表");
            return Arrays.asList();
        } else {
            return DataFrameS.getColAsStringList(suspensions, "资产代码");
        }
    }


    /**
     * 获取近期未来 休市安排 -- 数据中心/财经日历/休市安排
     * https://datacenter-web.eastmoney.com/api/data/get?type=RPTA_WEB_ZGXSRL&sty=ALL&ps=200&st=sdate&sr=-1&callback=jQuery11230209712528561385_1646443457043&_=1646443457044
     *
     * @return 失败null
     * @cols [结束日期, 节日描述, 休市市场, 开始日期, 未知]
     */
    public static DataFrame<Object> getFutureMarketCloseDates(int timeout, int retry) {
        String url = "https://datacenter-web.eastmoney.com/api/data/get";

        HashMap<String, Object> params = new HashMap<>();

        params.put("type", "RPTA_WEB_ZGXSRL");
        params.put("sty", "ALL");
        params.put("ps", "200");
        params.put("st", "sdate");
        params.put("sr", "-1");
        params.put("callback", "jQuery11230209712528561385_" + (System.currentTimeMillis() - 1));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }

        DataFrame<Object> dfTemp = jsonStrToDf(response, "(", ")",
                FutureMarketCloseDatesFields,
                Arrays.asList("result", "data"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(FutureMarketCloseDatesFieldsMap);
        return dfTemp;
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 获取东财财经日历, 需要明确给定 起始查询日期, 形如 yyyy-MM-dd
     * https://datacenter-web.eastmoney.com/api/data/get?callback=datatable184645&type=RPT_CPH_FECALENDAR&p=1&ps=50&st=START_DATE&sr=1&filter=(END_DATE%3E%3D%272022-03-01%27)(START_DATE%3C%3D%272022-03-08%27)&f1=(END_DATE%3E%3D%272022-03-01%27)(START_DATE%3C%3D%272022-03-08%27)&f2=&source=WEB&client=WEB&sty=START_DATE%2CEND_DATE%2CFE_CODE%2CFE_NAME%2CFE_TYPE%2CCONTENT%2CSTD_TYPE_CODE%2CSPONSOR_NAME%2CCITY&_=1647058787443
     *
     * @param startDate 形如 yyyy-MM-dd, 不可null
     * @param endDate   形如 yyyy-MM-dd, 不可null
     */
    public static DataFrame<Object> getEconomicCalendarByDateRange(String startDate, String endDate, int timeout,
                                                                   int retry) {
        Assert.isTrue(startDate.length() == 10);
        Assert.isTrue(endDate.length() == 10);
        String url = "https://datacenter-web.eastmoney.com/api/data/get";
        HashMap<String, Object> params = new HashMap<>();
        params.put("callback", "datatable184645");
        params.put("type", "RPT_CPH_FECALENDAR");
        params.put("p", "1");
        params.put("ps", "50");
        params.put("st", "START_DATE");
        params.put("sr", "1");
        params.put("filter", StrUtil.format("(END_DATE>='{}')(START_DATE<='{}')", startDate, endDate));
        params.put("f1", StrUtil.format("(END_DATE>='{}')(START_DATE<='{}')", startDate, endDate));
        params.put("f2", "");
        params.put("source", "WEB");
        params.put("client", "WEB");
        params.put("sty", "START_DATE,END_DATE,FE_CODE,FE_NAME,FE_TYPE,CONTENT,STD_TYPE_CODE,SPONSOR_NAME,CITY");
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }


        DataFrame<Object> dfTemp = jsonStrToDf(response, "(", ")",
                EconomicCalendarFields,
                Arrays.asList("result", "data"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(EconomicCalendarFieldsMap);
        return dfTemp;
    }

    private static void initSuspensionFields() {
        SuspensionFieldsMap.putAll(Dict.create()
                .set("SECURITY_CODE", "资产代码")
                .set("SECURITY_NAME_ABBR", "资产名称")
                .set("SUSPEND_START_TIME", "停牌开始时间")
                .set("SUSPEND_END_TIME", "停牌结束时间")
                .set("SUSPEND_EXPIRE", "停牌期限")
                .set("SUSPEND_REASON", "停牌原因")
                .set("TRADE_MARKET", "所属市场")
                .set("SUSPEND_START_DATE", "停牌开始日期")
                .set("PREDICT_RESUME_DATE", "预计复牌日期")
                .set("TRADE_MARKET_CODE", "交易市场代码")
                .set("SECURITY_TYPE_CODE", "资产类型代码")
        );
        SuspensionFields = Arrays.asList(
                "SECURITY_CODE", "SECURITY_NAME_ABBR", "SUSPEND_START_TIME", "SUSPEND_END_TIME",
                "SUSPEND_EXPIRE", "SUSPEND_REASON", "TRADE_MARKET",
                "SUSPEND_START_DATE", "PREDICT_RESUME_DATE", "TRADE_MARKET_CODE", "SECURITY_TYPE_CODE"
        );
    }

    private static void initFutureMarketCloseDatesFields() {
        FutureMarketCloseDatesFieldsMap.putAll(Dict.create()
                .set("edate", "结束日期") // 包括
                .set("holiday", "节日描述") //
                .set("mkt", "休市市场")
                .set("sdate", "开始日期") // 包括
                .set("xs", "未知") // 未知字段, 常为空
        );
        FutureMarketCloseDatesFields = Arrays.asList("edate", "holiday", "mkt", "sdate", "xs");

    }

    private static void initEconomicCalendarFields() {
        EconomicCalendarFieldsMap.putAll(Dict.create()
                .set("CITY", "城市")
                .set("CONTENT", "内容")
                .set("END_DATE", "结束日期")
                .set("FE_CODE", "日历代码")
                .set("FE_NAME", "日历名称")
                .set("FE_TYPE", "日历类型")
                .set("SPONSOR_NAME", "赞助商名称")
                .set("START_DATE", "开始日期")
                .set("STD_TYPE_CODE", "标准类型代码")
        );
        EconomicCalendarFields = Arrays.asList("CITY", "CONTENT", "END_DATE", "FE_CODE", "FE_NAME", "FE_TYPE",
                "SPONSOR_NAME", "START_DATE", "STD_TYPE_CODE");
    }
}
