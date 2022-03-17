package com.scareers.tools.stockplan.bean;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao.buildDateStr;
import static com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao.getSpecialNewFromCaiJingDaoDu;

/**
 * description: x月x日 新闻联播 新闻; 解析逻辑稍微复杂.
 * 核心内容仅仅  标题 和 内容两项, 其余均省略
 *
 * @noti : 比起 CompanyGoodNew, 少了 "name", "quoteUrl"  两大属性
 * @author: admin
 * @date: 2022/3/17/017-12:26:46
 */
@Data
public class NewsFeed {
    public static List<String> dfSimpleCols = Arrays // 没有id 和 saveTime
            .asList("title", "content", "dateStr",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );
    public static List<String> dfAllCols = Arrays
            .asList("id", "title", "content", "dateStr", "saveTime",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );
    // 解析设置
    String title;
    String content;
    String dateStr; // 纯日期无时间, 字符串

    // 保存到数据库时设置
    Timestamp saveTime; // 保存到数据库时间, 约等于 爬虫运行时刻; 该字段主要用于获取 最后500条爬取的 新闻.
    // 可自定义设置! // 类似 SimpleNewEm, 没有了 相关对象
    String briefly; // 简述
    Double trend; // 偏向: 利空或者利好, -1.0 - 1.0;  0.0 代表绝对无偏向
    String remark; // 备注
    Timestamp lastModified; // 手动修改最后时间;
    Boolean marked = false; // 是否重要?标记
    private long id;  // 数据库自动生成的id

    public static void main(String[] args) {
//        Console.log(getNewsFeedsOf("3月14日"));

        List<NewsFeed> companyGoodNews = parseNewsFeeds(getNewsFeedsOf("3月14日"));
        companyGoodNews.forEach(Console::log);
    }

    /**
     * 给定日期字符串, 获取 晚间央视新闻联播财经内容集锦
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getNewsFeedsOf(String dateStr) {
        // 3月15日晚间央视新闻联播财经内容集锦
        // 3月15日晚间央视新闻联播财经内容集锦%
        return getSpecialNewFromCaiJingDaoDu(dateStr, "晚间央视新闻联播财经内容集锦");
    }

    public static SimpleNewEm getNewsFeedsOf(DateTime date) {
        return getNewsFeedsOf(buildDateStr(date));
    }

    /**
     * df 与bean list 的相互转换
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanListWithoutIdAndSaveTime(List<NewsFeed> items) {
        DataFrame<Object> res = new DataFrame<>(dfSimpleCols);
        for (NewsFeed bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getBriefly());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }

    /**
     * list bean 转换为全字段完整df
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanList(List<NewsFeed> items) {
        DataFrame<Object> res = new DataFrame<>(dfAllCols);
        for (NewsFeed bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getSaveTime());
            row.add(bean.getBriefly());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }

    /**
     * 新闻 -- 新闻联播 重要消息 --> 解析逻辑稍微复杂
     * <p>
     * --> 主要分为两大部分
     * 1. 主容器 id 为 ContentBody
     * 2. 所有子控件为 p 控件
     * 3. 第一个p控件为广告控件
     * 4. 倒数2p 为广告+文章来源控件
     * <p>
     * 5.中间部分两大块内容:
     * 5.1: 两部分的分割控件为 2个连续的p控件, p1是 "国内联播快讯", p2是 "央视网消息(新闻联播):"
     * 5.2: 分割线之前的第一部分:
     * "央视网消息(新闻联播):" 为开始的p控件, 其前1控件为 标题, 本控件一直到 下一标题控件, 均为内容控件
     * 5.3: 分割线之后第二部分:
     * 均为 2个p一组, 第一个为标题, 第二个是内容
     *
     * @param bean
     * @return 访问http失败将返回null
     */
    public static List<NewsFeed> parseNewsFeeds(SimpleNewEm newsFeedBean) {
        Assert.isTrue(newsFeedBean.isNewsFeed());
        String url = newsFeedBean.getUrl();
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        String rawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (rawHtml == null) {
            return null;
        }
        String dateStr = DateUtil.format(newsFeedBean.getDateTime(), DatePattern.NORM_DATE_PATTERN);

        Document document = Jsoup.parse(rawHtml);
        Element contentBody = document.getElementById("ContentBody");
        Elements children = contentBody.children(); // 儿子, 为 单个h3 + 多个双数p;  可多组

        // 1.查找两个p控件, 作为分割线!
        int splitIndex = -1;
        for (int i = 0; i < children.size() - 1; i++) { // 检查两个连续
            Element p1 = children.get(i);
            Element p2 = children.get(i + 1);
            if (p1.text().contains("国内联播快讯") && p2.text().contains("央视网消息(新闻联播)")) {
                splitIndex = i;
            }
        }

        // 2.第一部分必然存在, 解析它
        int part1StartIndex = 0;
        int part1EndIndex = splitIndex - 1; // 包含


        // 2.1. 找到 "央视网消息(新闻联播)" 开头的标签, 其前一个为 标题, 到下一个标题之前为 内容
        List<Integer> titleIndexes = new ArrayList<>();
        for (int i = part1StartIndex; i < part1EndIndex; i++) {
            if (StrUtil.trim(children.get(i).text()).startsWith("央视网消息(新闻联播)")) {
                if (i - 1 >= 0 && i - 1 <= part1EndIndex - 1) {
                    titleIndexes.add(i - 1); // 标题索引
                }
            }
        }

        List<NewsFeed> items = new ArrayList<>();

        // 2.2. 依据标题索引, 区分标题和内容
        for (int i = 0; i < titleIndexes.size(); i++) {
            int titleIndex1 = titleIndexes.get(i);
            int titleIndex2;
            if (i == titleIndexes.size() - 1) { // 最后一个标题的索引
                titleIndex2 = part1EndIndex + 1;
            } else {
                titleIndex2 = titleIndexes.get(i + 1); // 常态
            }

            String title = StrUtil.trim(children.get(titleIndex1).text());
            // 构建内容, 它可能是多个 p标签的内容合集;
            // 自定义每个p标签分割使用 \n;
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = titleIndex1 + 1; j < titleIndex2; j++) {
                stringBuilder.append(StrUtil.trim(children.get(j).text()));
                stringBuilder.append("\n");
            }
            String content = stringBuilder.toString();
            NewsFeed item = new NewsFeed();
            item.setTitle(title);
            item.setContent(content);
            item.setDateStr(dateStr);
            items.add(item);
        }

        // 2.3. 判定是否有第二部分
        if (splitIndex == -1) {
            return items; // 没有第二部分
        }


        int part2StartIndex = splitIndex + 2; // 第二部分开始
        int part2EndIndex = children.size() - 1; // 第二部分结束, 包含, 虽然最后两个是广告

        for (int i = part2StartIndex; i < part2EndIndex; i += 2) {
            if (i + 1 >= children.size()) {
                break;
            }
            Element p1 = children.get(i);
            Element p2 = children.get(i + 1);
            if (p1.text().contains("文章来源") || p2.text().contains("文章来源")) {
                break;
            }
            NewsFeed item = new NewsFeed();
            item.setTitle(StrUtil.trim(p1.text()));
            item.setContent(StrUtil.trim(p2.text()));
            item.setDateStr(dateStr);
            items.add(item);
        }
        return items;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public Timestamp getSaveTime() {
        return saveTime;
    }

    public void setSaveTime(Timestamp saveTime) {
        this.saveTime = saveTime;
    }

    public String getBriefly() {
        return briefly;
    }

    public void setBriefly(String briefly) {
        this.briefly = briefly;
    }

    public Double getTrend() {
        return trend;
    }

    public void setTrend(Double trend) {
        this.trend = trend;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * name/title/dateStr 相等含义
     *
     * @return
     */
    @Override
    public int hashCode() {
        return this.title.hashCode() | this.dateStr.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MajorIssue) {
            MajorIssue o1 = (MajorIssue) o;
            if (this.title.equals(o1.title) && this.dateStr.equals(o1.dateStr)) {
                return true;
            }
        }
        return false;
    }

    private static final Log log = LogUtil.getLogger();
}
