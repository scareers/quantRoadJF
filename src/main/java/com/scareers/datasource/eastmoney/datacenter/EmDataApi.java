package com.scareers.datasource.eastmoney.datacenter;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
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

    public static int maxPageOfCaiJingDaoDu = 25; // 财经导读最大页数量
    public static int maxPageOfZiXunJingHua = 25; // 资讯精华最大页数量

    public static Cache<Integer, List<SimpleNewEm>> caiJingDaoDuNewsPerPageCache = CacheUtil
            .newLRUCache(25, 1 * 60 * 1000); // 财经导读单页新闻缓存, 1分钟
    public static Cache<Integer, List<SimpleNewEm>> ziXunJingHuaPerPageCache = CacheUtil
            .newLRUCache(25, 1 * 60 * 1000); // 资讯精华, 1分钟

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


//        Console.log("获取日期区间 财经日历");
//        Console.log(getEconomicCalendarByDateRange("2022-03-08", "2022-03-11", 3000, 3));

//        Console.log(getCaiJingDaoDuNewsPerPage(1));

        Console.log(getAllAnnouncementOfOneDay("2022-04-27", 251, 10000, 2));
        Console.log(getAnnouncementOfOneStockDateTimes("300010", 1, 10000, 2));

    }

    /**
     * 给定股票代码, 和页码, 返回股票公告, 页码从1开始; 每页固定 50条公告; 的发布时间列表
     * https://np-anotice-stock.eastmoney.com/api/security/ann?cb=jQuery1123016109221510953065_1650990560519&sr=-1
     * &page_size=50&page_index=1&ann_type=A&client_source=web&stock_list=300010&f_node=0&s_node=0
     *
     * @param dateStr
     * @param page
     * @param timeout
     * @param retry
     * @return
     */
    public static List<Date> getAnnouncementOfOneStockDateTimes(String stockCodeSimple, int page, int timeout,
                                                                     int retry) {
        String url = "https://np-anotice-stock.eastmoney.com/api/security/ann";
        HashMap<String, Object> params = new HashMap<>();
        params.put("cb", "jQuery1123016109221510953065_" + (System.currentTimeMillis() - 1));
        params.put("sr", "-1");
        params.put("page_size", "50");
        params.put("page_index", page);
        params.put("ann_type", "A");
        params.put("client_source", "web");
        params.put("stock_list", stockCodeSimple);
        params.put("f_node", 0);
        params.put("s_node", 0);


        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }

        int start = 0, end = 0;
        start = response.indexOf("(") + 1;
        end = response.lastIndexOf(")");
        response = response.substring(start, end);

        JSONObject jsonObject = JSONUtilS.parseObj(response);

        List<Date> res = new ArrayList<>();
        try {
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    String noticeDate = jsonObject1.getString("notice_date");
                    res.add(DateUtil.parse(noticeDate));
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {
            return null;
        }

        return res;
    }

    /**
     * 给定单个日期字符串, 从东财数据中心, 获取所有当日发布了公告的公司名称 集合; 无内容, 自行查看;
     * 需要给定页码, 从 1 开始; 每页50条
     * https://np-anotice-stock.eastmoney.com/api/security/ann?cb=jQuery112309403262652933302_1650988901859&sr=-1&page_size=50&page_index=1&ann_type=SHA%2CCYB%2CSZA%2CBJA&client_source=web&f_node=0&s_node=0&begin_time=2022-04-26&end_time=2022-04-26
     * <p>
     * {
     * "data": {
     * "list": [
     * {
     * "art_code": "AN202204261561801876",
     * "codes": [
     * {
     * "ann_type": "A,BJA",
     * "inner_code": "40415376513752",
     * "market_code": "0",
     * "short_name": "七丰精工",
     * "stock_code": "873169"
     * }
     * ],
     * "columns": [
     * {
     * "column_code": "001003001001004",
     * "column_name": "独立董事述职报告"
     * }
     * ],
     * "display_time": "2022-04-26 22:53:19:020",
     * "eiTime": "2022-04-26 22:52:39:000",
     * "language": "0",
     * "notice_date": "2022-04-26 00:00:00",
     * "title": "七丰精工:2021年度独立董事述职报告",
     * "title_ch": "七丰精工:2021年度独立董事述职报告",
     * "title_en": ""
     * }
     *
     * @param dateStr
     */
    public static HashSet<String> getAllAnnouncementOfOneDay(String dateStr, int page, int timeout,
                                                             int retry) {
        String url = "https://np-anotice-stock.eastmoney.com/api/security/ann";

        HashMap<String, Object> params = new HashMap<>();
        params.put("cb", "jQuery112309403262652933302_" + (System.currentTimeMillis() - 1));
        params.put("sr", "-1");
        params.put("page_size", "50");
        params.put("page_index", page);
        params.put("ann_type", "SHA,CYB,SZA,BJA");
        params.put("f_node", "0");
        params.put("s_node", "0");
        params.put("client_source", "web");
        params.put("begin_time", dateStr);
        params.put("end_time", dateStr);


        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }

        int start = 0, end = response.length();
        start = response.indexOf("(") + 1;
        end = response.lastIndexOf(")");
        response = response.substring(start, end);

        JSONObject jsonObject = JSONUtilS.parseObj(response);

        HashSet<String> res = new HashSet<>();
        try {
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < jsonArray.size(); i++) {
                try {
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    String companyName = jsonObject1.getJSONArray("codes").getJSONObject(0).getString("short_name");
                    res.add(companyName);
                } catch (Exception e) {

                }
            }
        } catch (Exception e) {
            return null;
        }

        return res;
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

    /**
     * 东财财经要闻列表, 别名 "资讯精华"
     * 入口:
     * 网页版 -- 财经 -- 焦点 -- 资讯精华
     * https://finance.eastmoney.com/a/cywjh.html 同样最多25页
     * <p>
     * pc版本
     * 资讯 -- 财经要闻
     * https://eminfo.eastmoney.com/pc_news/FastNews/GetImportantNewsList  数量有限且无法增加
     *
     * @param page
     * @return
     */
    @TimeoutCache(timeout = "1 * 60 * 1000")
    public static List<SimpleNewEm> getZiXunJingHuaPerPage(int page) {
        List<SimpleNewEm> res = ziXunJingHuaPerPageCache.get(page);
        if (res != null) {
            return res;
        }
        Assert.isTrue(page > 0 && page <= maxPageOfZiXunJingHua); // 只有25页数据
        res = new ArrayList<>();
        // 该url 若不带 _{page}, 则显示第一页;
        String url = StrUtil.format("https://finance.eastmoney.com/a/cywjh_{}.html", page);
        String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (asStrUseHutool == null) {
            log.error("访问东财资讯精华新闻列表失败, 页码: {}, 将返回 空列表", page);
            return res;
        }
        parseEmZiXunListCommon(res, asStrUseHutool);
        res.forEach(bean -> bean.setType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE));
        ziXunJingHuaPerPageCache.put(page, res);
        return res;
    }


    /**
     * 东财财经导读单页新闻列表!
     * <p>
     * 四类特殊新闻:
     * 3月9日晚间沪深上市公司重大事项公告最新快递
     * 3月9日晚间央视新闻联播财经内容集锦
     * 3月9日晚间上市公司利好消息一览(附名单)
     * 3月9日国内四大证券报纸、重要财经媒体头版头条内容精华摘要
     * 来源
     * * 东方财富 -- 财经频道 -- 财经导读 -- 新闻列表
     * * https://finance.eastmoney.com/a/ccjdd_1.html
     *
     * @param page 给定页码, 1-25均可, 得到单页 20条新闻对象列表
     */
    @TimeoutCache(timeout = "1 * 60 * 1000")
    public static List<SimpleNewEm> getCaiJingDaoDuNewsPerPage(int page) {
        List<SimpleNewEm> res = caiJingDaoDuNewsPerPageCache.get(page);
        if (res != null) {
            return res;
        }
        Assert.isTrue(page > 0 && page <= maxPageOfCaiJingDaoDu); // 只有25页数据
        res = new ArrayList<>();
        // 该url 若不带 _{page}, 则显示第一页;
        String url = StrUtil.format("https://finance.eastmoney.com/a/ccjdd_{}.html", page);
        String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (asStrUseHutool == null) {
            log.error("访问东财财经导读新闻列表失败, 页码: {}, 将返回 空列表", page);
            return res;
        }
        parseEmZiXunListCommon(res, asStrUseHutool);
        res.forEach(bean -> bean.setType(SimpleNewEm.CAI_JING_DAO_DU_TYPE));
        caiJingDaoDuNewsPerPageCache.put(page, res);
        return res;
    }


    /**
     * 东财 财经 新闻, 多数新闻列表, 解析逻辑相同. 均是找到 id为 newsListContent 的列表, 访问 li标签
     *
     * @param res
     * @param asStrUseHutool
     */
    private static void parseEmZiXunListCommon(List<SimpleNewEm> res, String asStrUseHutool) {
        Document document = Jsoup.parse(asStrUseHutool);
        Element newsList = document.getElementById("newsListContent");  // 20条新闻容器
        Elements liEle = newsList.getElementsByTag("li"); // 每个 li标签代表一条内容, 且id为 newsTr0 到19

        for (Element element : liEle) {
            // url和文章标题, 在 li / div[.text] / p[.title] / a标签, url为href属性, 文章标题为 a标签文本内容
            Elements aTags = element.getElementsByTag("a");
            Element a = aTags.get(aTags.size() - 1); // 某些新闻没有配图片, 目标a标签为第1个,否则为第二个; 但都是最后一个

            // 时间: 在 li / div[.text] / p[.time]
            Element timeEle = element.getElementsByClass("time").get(0);
            DateTime parse = DateUtil.parse(timeEle.text(), "MM月dd日 HH:mm");

            // 需要修改年份, 默认解析到 1900年
            if (DateUtil.month(parse) > DateUtil.month(DateUtil.date())) {
                // 这代表跨年, 此时year - 1
                parse.setField(DateField.YEAR, DateUtil.year(DateUtil.date()) - 1);
            } else {
                parse.setField(DateField.YEAR, DateUtil.year(DateUtil.date()));
            }


            // 详细内容: 在 li / div[.text] / p[.info] 的 title 属性;
            Element newTitleEle = element.getElementsByClass("info").get(0);
            res.add(new SimpleNewEm(parse, a.text(), a.attr("href"), newTitleEle.attr("title")));
        }
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
