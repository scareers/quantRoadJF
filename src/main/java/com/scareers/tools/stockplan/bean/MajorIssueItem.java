package com.scareers.tools.stockplan.bean;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
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
 * 重大事项解析结果
 * 股票名称/行情url/重大事项标题/ 重大事项详细
 *
 * @author admin
 */
@Data
public class MajorIssueItem {
    public static void main(String[] args) {
        Console.log(parseCompanyMajorIssuesNew(getCompanyMajorIssuesOf("3月14日")));
    }

    /**
     * 给定日期字符串, 获取 晚间沪深上市公司重大事项公告最新快递
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getCompanyMajorIssuesOf(String dateStr) {
        return getSpecialNewFromCaiJingDaoDu(dateStr, "晚间沪深上市公司重大事项公告最新快递");
    }

    public static SimpleNewEm getCompanyMajorIssuesOf(DateTime date) {
        return getCompanyMajorIssuesOf(buildDateStr(date));
    }

    /**
     * name/title/dateStr 相等含义
     *
     * @return
     */
    @Override
    public int hashCode() {
        return this.name.hashCode() | this.title.hashCode() | this.dateStr.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MajorIssueItem) {
            MajorIssueItem o1 = (MajorIssueItem) o;
            if (this.name.equals(o1.name) && this.title.equals(o1.title) && this.dateStr.equals(o1.dateStr)) {
                return true;
            }
        }
        return false;
    }

    private long id;  // 数据库自动生成的id

    // 解析设置
    String name;
    String quoteUrl;
    String title;
    String content;
    String dateStr; // 纯日期无时间, 字符串
    String type; // 重大事项类型, 解析而来

    // 保存到数据库时设置
    Timestamp saveTime; // 保存到数据库时间, 约等于 爬虫运行时刻; 该字段主要用于获取 最后500条爬取的 新闻.

    // 可自定义设置! // 类似 SimpleNewEm, 没有了 相关对象
    String briefly; // 简述
    Double trend; // 偏向: 利空或者利好, -1.0 - 1.0;  0.0 代表绝对无偏向
    String remark; // 备注
    Timestamp lastModified; // 手动修改最后时间;
    Boolean marked = false; // 是否重要?标记


    /**
     * 某一类的集合, 不使用Map, 使得有序
     */
    @Data
    @AllArgsConstructor
    public static class MajorIssueBatch {
        List<MajorIssueItem> items;
        String type; //
    }


    public static List<String> dfSimpleCols = Arrays // 没有id 和 saveTime
            .asList("name", "quoteUrl", "title", "content", "dateStr", "type",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );

    /**
     * df 与bean list 的相互转换
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanListWithoutIdAndSaveTime(List<MajorIssueItem> items) {
        DataFrame<Object> res = new DataFrame<>(dfSimpleCols);
        for (MajorIssueItem bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getName());
            row.add(bean.getQuoteUrl());
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getType());
            row.add(bean.getBriefly());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }

    public static List<String> dfAllCols = Arrays
            .asList("id", "name", "quoteUrl", "title", "content", "dateStr", "type", "saveTime",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );

    /**
     * list bean 转换为全字段完整df
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanList(List<MajorIssueItem> items) {
        DataFrame<Object> res = new DataFrame<>(dfAllCols);
        for (MajorIssueItem bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getName());
            row.add(bean.getQuoteUrl());
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getType());
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
     * 新闻 -- 重大事项解析逻辑
     * 主容器 id 为 ContentBody
     * 子控件为 h3 控件 + 多个p控件
     * h3控件为消息分类
     * 随后的子p控件为 2个一组,
     * 第一个p为股票名称和信息简介, 子标签 a, href属性代表个股行情页面, id为 形如 stock_1.600771
     * 第二个p控件为信息详细描述, 可能有其他股票名称加粗
     *
     * @param bean
     * @return 访问http失败将返回null
     */
    public static List<MajorIssueItem.MajorIssueBatch> parseCompanyMajorIssuesNew(SimpleNewEm companyMajorIssuesBean) {
        Assert.isTrue(companyMajorIssuesBean.isCompanyMajorIssues());
        String url = companyMajorIssuesBean.getUrl();
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        String rawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (rawHtml == null) {
            return null;
        }
        String dateStr = DateUtil.format(companyMajorIssuesBean.getDateTime(), DatePattern.NORM_DATE_PATTERN);

        Document document = Jsoup.parse(rawHtml);
        Element contentBody = document.getElementById("ContentBody");
        Elements children = contentBody.children(); // 儿子, 为 单个h3 + 多个双数p;  可多组

        // 1. 查找 为h3的 索引列表
        List<Integer> h3Indexes = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            if ("h3".equals(children.get(i).tagName())) {
                h3Indexes.add(i);
            }
        }
        // 2.注意最后两个p为广告和文章来源
        h3Indexes.add(children.size() - 2); // 这里合适

        // 3.解析
        List<MajorIssueBatch> res = new ArrayList<>();

        for (int i = 0; i < h3Indexes.size() - 1; i++) {
            String typeText = StrUtil.trim(children.get(h3Indexes.get(i)).text());
            int startIndex = h3Indexes.get(i) + 1; // 包含
            int endIndex = h3Indexes.get(i + 1) - 1; // 包含
            List<MajorIssueItem> items = new ArrayList<>();
            for (int j = startIndex; j <= endIndex; j += 2) {
                Element p2 = children.get(j + 1);
                if (p2.text().contains("文章来源")) {
                    break; // 末尾
                }
                Element p1 = children.get(j);
                // 两个一组p遍历
                Elements a1 = p1.getElementsByTag("a");
                if (a1.size() == 0) {
                    // 某些股票不使用a标签, 直接span标签;例如赣锋锂业
                    a1 = p1.getElementsByTag("span");
                }
                MajorIssueItem item = new MajorIssueItem();

                if (a1.size() == 0) {
                    // 某些时候 p> strong 里面直接包裹, 且不会有 url;
                    String text = StrUtil.trim(p1.text());
                    int colonIndex;
                    if (text.contains(":")) {
                        colonIndex = text.indexOf(":"); // 尝试从 : 分割
                    } else {
                        colonIndex = 4; // 否则取前4 作为名字
                    }
                    item.setName(text.substring(0, colonIndex));
                    item.setQuoteUrl("");
                    item.setTitle(text.substring(colonIndex + 1));
                } else {
                    Element a = a1.get(0);
                    item.setName(StrUtil.trim(a.text()));
                    item.setQuoteUrl(a.attr("href"));
                    item.setTitle(StrUtil.trim(p1.text()));
                }
                item.setContent(StrUtil.trim(children.get(j + 1).text()));
                item.setDateStr(dateStr);
                item.setType(typeText);
                items.add(item);
            }
            res.add(new MajorIssueBatch(items, typeText));
        }

        return res;
    }
}