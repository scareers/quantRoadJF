package com.scareers.tools.stockplan.bean;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.annotations.TimeoutCache;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.utils.log.LogUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 东方财富 -- 财经频道 -- 财经导读 -- 新闻列表
 * 简单新闻对象;
 * 东财 其他新闻列表, 格式相同
 * https://finance.eastmoney.com/a/ccjdd_1.html
 *
 * @author admin
 */
@Data
@AllArgsConstructor
public class SimpleNewEm {
    public static int maxPageOfCaiJingDaoDu = 25; // 财经导读最大页数量

    DateTime dateTime; // 时间, 精确到分钟
    String title; // 东财提取的简单标题
    String url; // 新闻具体url
    String detailTitle; // 原文章具体标题

    String urlRawHtml; // 访问url, 得到的原始html内容. 大多数为纯文字不带样式, 勉强能看;

    /**
     * 构造器不自动访问url
     *
     * @param dateTime
     * @param title
     * @param url
     * @param detailTitle
     */
    public SimpleNewEm(DateTime dateTime, String title, String url, String detailTitle) {
        this.dateTime = dateTime;
        this.title = title;
        this.url = url;
        this.detailTitle = detailTitle;
    }

    /**
     * 可手动访问url, 并设置 原始新闻页面 html;
     */
    public void setUrlHtml() {
        urlRawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 5000, 3);
    }

    /*
    四种典型新闻: 公司利好, 公司重要公告, 新闻联播集锦, 四大报媒
     */

    /**
     * 公司利好标题: x月x日 + "晚间上市公司利好消息一览(附名单)"
     *
     * @return
     */
    public boolean isCompanyGoodNews() {
        return title.contains("晚间上市公司利好消息一览"); // 省略附名单
    }

    /**
     * 重大事项: x月x日 + "晚间沪深上市公司重大事项公告最新快递"
     *
     * @return
     */
    public boolean isCompanyMajorIssues() {
        return title.contains("晚间沪深上市公司重大事项公告最新快递");
    }

    /**
     * 新闻联播集锦: x月x日 + "晚间央视新闻联播财经内容集锦"
     *
     * @return
     */
    public boolean isNewsFeeds() {
        return title.contains("晚间央视新闻联播财经内容集锦");
    }

    /**
     * 四大报和重要媒体: x月x日 + "国内四大证券报纸、重要财经媒体头版头条内容精华摘要"
     *
     * @return
     */
    public boolean isFourPaperNews() {
        return title.contains("国内四大证券报纸、重要财经媒体头版头条内容精华摘要");
    }

    /**
     * 标题包含
     *
     * @param subString
     * @return
     */
    public boolean titleContain(String subString) {
        return title.contains(subString);
    }

    /*
    静态方法
     */
    private static final Log log = LogUtil.getLogger();

    public static Cache<Integer, List<SimpleNewEm>> caiJingDaoDuNewsPerPageCache = CacheUtil
            .newLRUCache(25, 1 * 60 * 1000); // 财经导读单页新闻缓存, 1分钟
    public static Cache<Integer, List<SimpleNewEm>> ziXunJingHuaPerPageCache = CacheUtil
            .newLRUCache(25, 1 * 60 * 1000); // 资讯精华, 1分钟


    /**
     * 给定日期字符串, 获取 该日晚间上市公司利好消息一览
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getCompanyGoodNewsOf(String dateStr) {
        for (int i = 1; i <= maxPageOfCaiJingDaoDu; i++) {
            List<SimpleNewEm> news = SimpleNewEm.getCaiJingDaoDuNewsPerPage(i);
            for (SimpleNewEm simpleNewEm : news) {
                if (simpleNewEm.isCompanyGoodNews() && simpleNewEm.titleContain(dateStr)) {
                    return simpleNewEm;
                }
            }
        }
        return null;
    }

    /**
     * 给定日期字符串, 获取 晚间沪深上市公司重大事项公告最新快递
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getCompanyMajorIssuesOf(String dateStr) {
        for (int i = 1; i <= maxPageOfCaiJingDaoDu; i++) {
            List<SimpleNewEm> news = SimpleNewEm.getCaiJingDaoDuNewsPerPage(i);
            for (SimpleNewEm simpleNewEm : news) {
                if (simpleNewEm.isCompanyMajorIssues() && simpleNewEm.titleContain(dateStr)) {
                    return simpleNewEm;
                }
            }
        }
        return null;
    }

    /**
     * 给定日期字符串, 获取 晚间央视新闻联播财经内容集锦
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getNewsFeedsOf(String dateStr) {
        for (int i = 1; i <= maxPageOfCaiJingDaoDu; i++) {
            List<SimpleNewEm> news = SimpleNewEm.getCaiJingDaoDuNewsPerPage(i);
            for (SimpleNewEm simpleNewEm : news) {
                if (simpleNewEm.isNewsFeeds() && simpleNewEm.titleContain(dateStr)) {
                    return simpleNewEm;
                }
            }
        }
        return null;
    }

    /**
     * 给定日期字符串, 获取 国内四大证券报纸、重要财经媒体头版头条内容精华摘要
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getFourPaperNewsOf(String dateStr) {
        for (int i = 1; i <= maxPageOfCaiJingDaoDu; i++) {
            List<SimpleNewEm> news = SimpleNewEm.getCaiJingDaoDuNewsPerPage(i);
            for (SimpleNewEm simpleNewEm : news) {
                if (simpleNewEm.isFourPaperNews() && simpleNewEm.titleContain(dateStr)) {
                    return simpleNewEm;
                }
            }
        }
        return null;
    }

    private static String buildDateStr(DateTime date) { // 构造日期字符串, 作为新闻标题判定
        return StrUtil.format("{}月{}日", date.getField(DateField.MONTH),
                date.getField(DateField.DAY_OF_MONTH));
    }

    // 对应4个传递 DateTime作为参数
    public static SimpleNewEm getCompanyGoodNewsOf(DateTime date) {
        return getCompanyGoodNewsOf(buildDateStr(date));
    }

    public static SimpleNewEm getCompanyMajorIssuesOf(DateTime date) {
        return getCompanyMajorIssuesOf(buildDateStr(date));
    }

    public static SimpleNewEm getNewsFeedsOf(DateTime date) {
        return getNewsFeedsOf(buildDateStr(date));
    }

    public static SimpleNewEm getFourPaperNewsOf(DateTime date) {
        return getFourPaperNewsOf(buildDateStr(date));
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
        caiJingDaoDuNewsPerPageCache.put(page, res);
        return res;
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
        Assert.isTrue(page > 0 && page <= maxPageOfCaiJingDaoDu); // 只有25页数据
        res = new ArrayList<>();
        // 该url 若不带 _{page}, 则显示第一页;
        String url = StrUtil.format("https://finance.eastmoney.com/a/cywjh_{}.html", page);
        String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (asStrUseHutool == null) {
            log.error("访问东财资讯精华新闻列表失败, 页码: {}, 将返回 空列表", page);
            return res;
        }
        parseEmZiXunListCommon(res, asStrUseHutool);
        ziXunJingHuaPerPageCache.put(page, res);
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
            parse.setField(DateField.YEAR, DateUtil.year(DateUtil.date())); // 需要修改年份, 默认解析到 1900年

            // 详细内容: 在 li / div[.text] / p[.info] 的 title 属性;
            Element newTitleEle = element.getElementsByClass("info").get(0);
            res.add(new SimpleNewEm(parse, a.text(), a.attr("href"), newTitleEle.attr("title")));
        }
    }


}
