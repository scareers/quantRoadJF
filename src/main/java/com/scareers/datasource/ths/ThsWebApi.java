package com.scareers.datasource.ths;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.log.Log;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 多数来自于同花顺web端api; 参考akshare
 * web api, 因网络因素, 十分不稳定. 平均速度比python快2倍以上, 但首次访问稳定性较差, 可能很耗时
 *
 * @author: admin
 * @date: 2022/3/20/020-13:50:34
 */
public class ThsWebApi {
    public static void main(String[] args) throws Exception {
//        DataFrame<Object> dataFrame = getIncludeStocksByIndustryOrConceptCode("881121", 5);
//        DataFrame<Object> dataFrame = getIncludeStocksByIndustryOrConceptCode("885595", 5);
//        Console.log(dataFrame);
//        Console.log(dataFrame.columns());

//        Console.log(dataFrame.length());
//        Console.log(dataFrame.toString(1000));

//        TimeInterval timer = DateUtil.timer();
//        timer.start();
//        DataFrame<Object> conceptListFromWeb = getConceptListFromWeb();
//
//
//        Console.log(conceptListFromWeb);
//        Console.log(conceptListFromWeb.length());
//        Console.log(conceptListFromWeb.columns());
//        Console.log(DataFrameS.getColAsStringList(conceptListFromWeb, "url"));

        Console.log(getIncludeStocksByIndustryOrConceptCode("308864",3));

        Console.log(HttpUtil.get("https://q.10jqka.com.cn/gn/detail/field/264648/order/desc/page/1/ajax/1/code/308864"));
    }


    private static final Log log = LogUtil.getLogger();

    /**
     * 给定概念/行业 简单代码, 返回成分股列表df. 若单页失败, 则统一返回null
     * 概念代码, 需要是 web版本的代码, 一般30开头, 而非客户端代码, 88开头
     * 可给定单页重试次数
     *
     * @param simpleCode
     * @return
     * @cols [序号, 代码, 名称, 现价, 涨跌幅(%), 涨跌, 涨速(%), 换手(%), 量比, 振幅(%), 成交额, 流通股, 流通市值, 市盈率, 加自选]
     */
    public static DataFrame<Object> getIncludeStocksByIndustryOrConceptCode(String simpleCode, int retryForPerPage) {
        Document document1 = getDocument(StrUtil
                .format("http://q.10jqka.com.cn/thshy/detail/field/199112/order/desc/page/1/ajax/1/code" +
                        "/{}", simpleCode));
        Elements changePages = document1.getElementsByAttributeValue("class", "changePage");
        List<Element> as = new ArrayList<>(); // 找到所有页面切换a标签
        for (Element changePage : changePages) {
            if (changePage.tagName().equals("a")) {
                as.add(changePage);
            }
        }
        // 读取最后1个a标签的page属性
        int pageNum = 1;
        if (as.size() > 0) {
            Element element = as.get(as.size() - 1);
            pageNum = Integer.parseInt(element.attr("page"));
        }

        DataFrame<Object> resTotal = null;

        for (int page = 1; page < pageNum + 1; page++) {


            Document document;
            Elements tables = null;
            int times = 0;
            while (times < retryForPerPage) { // 重试
                times++;
                document = getDocument(
                        StrUtil.format("http://q.10jqka.com.cn/thshy/detail/field/199112/order/desc/page/{}/ajax/1" +
                                "/code/{}", page, simpleCode));
                tables = document.getElementsByTag("table");
                if (tables.size() > 0) {
                    break;
                }
            }
            if (tables.size() == 0) {
                log.error("获取单页成分股失败: {} --> 页码: {}", simpleCode, page);
                return null;
            }
            Element table = tables.get(0);// 第一个table

            // 1.构建表头: thead/tr -> th列表
            List<String> columns = new ArrayList<>();
            Element thead = table.getElementsByTag("thead").get(0);
            Element tr = thead.getElementsByTag("tr").get(0);
            Elements ths = tr.getElementsByTag("th");
            for (Element th : ths) {
                columns.add(StrUtil.trim(th.text()));
            }
            // 2.读取每行: tbody / tr列表 -> td列表
            DataFrame<Object> res = new DataFrame<>(columns);
            Element tbody = table.getElementsByTag("tbody").get(0);
            Elements rowsEle = tbody.getElementsByTag("tr");
            for (Element rowEle : rowsEle) {
                Elements td = rowEle.getElementsByTag("td");
                List<Object> row = new ArrayList<>();
                for (Element element : td) {
                    row.add(StrUtil.trim(element.text()));
                }
                res = res.append(row);
            }

            // 3.拼接单页结果
            if (resTotal == null) {
                resTotal = res;
            } else {
                resTotal = resTotal.concat(res);
            }


        }
        return resTotal;
    }

    private static Document getDocument(String format) {

        String url = format;
        HttpRequest request = new HttpRequest(url);
        request.setMethod(Method.GET);
        request.header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36");
        request.header("Cookie",
        "__utmz=156575163.1646917474.1.1.utmcsr=yuanchuang.10jqka.com.cn|utmccn=(referral)|utmcmd=referral|utmcct=/; __utma=156575163.717940102.1646917474.1646917474.1647060731.2; spversion=20130314; user_status=0; historystock=600242%7C*%7C000001%7C*%7C600745%7C*%7C603456%7C*%7C688408; log=; Hm_lvt_78c58f01938e4d85eaf619eae71b4ed1=1647714944,1647755161,1647768501,1647772285; Hm_lpvt_78c58f01938e4d85eaf619eae71b4ed1=1647772290; v=AyTEI9EkGqC7sW7s7Q2rnjsj9SkTvUg4CuHcaz5FsO-y6coXZs0Yt1rxrPSN"
        );
        request.header("host", "q.10jqka.com.cn");
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        request.header("Accept-Language", "zh-CN,zh;q=0.9");
        request.header("Cache-Control", "max-age=0");
        request.header("Connection", "keep-alive");
        request.header("Upgrade-Insecure-Requests", "1");
        String body = request.execute().body();

        return Jsoup.parse(body);
    }

    /**
     * [日期, 概念名称, 驱动事件, 龙头股, 成分股数量, url, webCode]
     * 同花顺概念时间表. 这里的 webCode, 与问财的代码不同
     *
     * @return
     */
    public static DataFrame<Object> getConceptListFromWeb() {
        // 1.首先决定总页数, 访问第一页, 查找总页数
        String url = "http://q.10jqka.com.cn/gn/index/field/addtime/order/desc/page/1/ajax/1/";
        Document document1 = getDocument(url);
        Elements pageInfos = document1.getElementsByAttributeValue("class", "page_info");
        Element span = null;
        for (Element pageInfo : pageInfos) {
            if ("span".equals(pageInfo.tagName())) {
                span = pageInfo;
                break;
            }
        }

        int totalPage = Integer
                .parseInt(StrUtil.split(span.text(), "/").get(1)); // <span class="page_info">1/33</span>


        DataFrame<Object> res = null;
        for (int page = 1; page < totalPage + 1; page++) {
            Document document = getDocument(
                    StrUtil.format("http://q.10jqka.com.cn/gn/index/field/addtime/order/desc/page/{}/ajax/1/",
                            page));
            Element table = document.getElementsByTag("table").get(0);

            DataFrame<Object> dataFrame = DataFrameS.parseHtmlTable(table);
            Element tbody = table.getElementsByTag("tbody").get(0);
            Elements trs = tbody.getElementsByTag("tr");
            List<Object> urls = new ArrayList<>();
            List<Object> webCodes = new ArrayList<>();
            for (Element tr : trs) {
                String innerUrl = tr.getElementsByTag("td").get(1).getElementsByTag("a").get(0).attr("href");
                urls.add(innerUrl);
                webCodes.add(StrUtil.split(innerUrl, "/").get(6));
                //  http://q.10jqka.com.cn/gn/detail/code/300100/
            }
            dataFrame = dataFrame.add("url", urls);
            dataFrame = dataFrame.add("webCode", webCodes);
            if (res == null) {
                res = dataFrame;
            } else {
                res = res.concat(dataFrame);
            }

        }
        return res;
    }
}
