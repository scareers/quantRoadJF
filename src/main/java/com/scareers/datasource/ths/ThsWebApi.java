package com.scareers.datasource.ths;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 多数来自于同花顺web端api; 参考akshare
 *
 * @author: admin
 * @date: 2022/3/20/020-13:50:34
 */
public class ThsWebApi {
    public static void main(String[] args) {
//        DataFrame<Object> dataFrame = getIncludeStocksByIndustryOrConceptCode("881121", 5);
        DataFrame<Object> dataFrame = getIncludeStocksByIndustryOrConceptCode("885611", 5);
        Console.log(dataFrame.columns());

//        Console.log(dataFrame.length());
//        Console.log(dataFrame.toString(1000));
    }


    public static String vCode; // 只需要调用一次初始化,

    /**
     * 执行js代码, 初始化vCode; 常态仅需要调用一次;
     *
     * @return
     * @throws Exception
     */
    private static String getVCode() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("js"); // 得到脚本引擎

        String str = ResourceUtil.readUtf8Str("ths/wencai/ths.js"); // 将会自动查找类路径下; 别绝对路径
        engine.eval(str);
        Invocable inv = (Invocable) engine;
        Object test2 = inv.invokeFunction("v");
        vCode = test2.toString();
        return test2.toString();
    }

    /**
     * 死循环初始化
     */
    private static void checkVCode() {
        while (vCode == null) {
            try {
                vCode = getVCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 给定概念/行业 简单代码, 返回成分股列表df. 若单页失败, 则统一返回null
     * 可给定单页重试次数
     *
     * @param simpleCode
     * @return
     * @cols [序号, 代码, 名称, 现价, 涨跌幅(%), 涨跌, 涨速(%), 换手(%), 量比, 振幅(%), 成交额, 流通股, 流通市值, 市盈率, 加自选]
     */
    public static DataFrame<Object> getIncludeStocksByIndustryOrConceptCode(String simpleCode, int retryForPerPage) {
        checkVCode();
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
        request.header("Cookie", StrUtil.format("v={}", vCode));
        String body = request.execute().body();
        return Jsoup.parse(body);
    }

}
