package com.scareers.tools.stockplan.news.bean;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import joinery.DataFrame;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao.buildDateStr;
import static com.scareers.tools.stockplan.news.bean.dao.SimpleNewEmDao.getSpecialNewFromAllType;

/**
 * description: x月x日 上市公司重大利好新闻 解析结果
 *
 * @noti 所有字段与 MajorIssue相同, 除了因为没有分类,导致没有 String type 属性.
 * @author: admin
 * @date: 2022/3/17/017-12:26:46
 */
@Data
public class CompanyGoodNew {
    public static List<String> dfSimpleCols = Arrays // 没有id 和 saveTime
            .asList("name", "quoteUrl", "title", "content", "dateStr",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );
    public static List<String> dfAllCols = Arrays
            .asList("id", "name", "quoteUrl", "title", "content", "dateStr", "saveTime",
                    "briefly", "trend", "remark", "lastModified", "marked"
            );
    // 解析设置
    String name;
    String quoteUrl;
    String title;
    String content;
    String dateStr; // 纯日期无时间, 字符串
    // 保存到数据库时设置
    Date saveTime; // 保存到数据库时间, 约等于 爬虫运行时刻; 该字段主要用于获取 最后500条爬取的 新闻.
    // 可自定义设置! // 类似 SimpleNewEm, 没有了 相关对象
    String briefly; // 简述
    Double trend; // 偏向: 利空或者利好, -1.0 - 1.0;  0.0 代表绝对无偏向
    String remark; // 备注
    Date lastModified; // 手动修改最后时间;
    Boolean marked = false; // 是否重要?标记
    private long id;  // 数据库自动生成的id

    public static void main(String[] args) {
        List<CompanyGoodNew> companyGoodNews = parseCompanyGoodNews(getCompanyGoodNewsOf("3月14日"));
        companyGoodNews.forEach(Console::log);
    }

    /**
     * 给定日期字符串, 获取 该日晚间上市公司利好消息一览
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getCompanyGoodNewsOf(String dateStr) {
        return getSpecialNewFromAllType(dateStr, "晚间上市公司利好消息一览");
    }

    // 对应4个传递 DateTime作为参数
    public static SimpleNewEm getCompanyGoodNewsOf(DateTime date) {
        return getCompanyGoodNewsOf(buildDateStr(date));
    }
    // String type; // 没有类型, 全是利好

    /**
     * df 与bean list 的相互转换
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanListWithoutIdAndSaveTime(List<CompanyGoodNew> items) {
        DataFrame<Object> res = new DataFrame<>(dfSimpleCols);
        for (CompanyGoodNew bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getName());
            row.add(bean.getQuoteUrl());
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
    public static DataFrame<Object> buildDfFromBeanList(List<CompanyGoodNew> items) {
        DataFrame<Object> res = new DataFrame<>(dfAllCols);
        for (CompanyGoodNew bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getName());
            row.add(bean.getQuoteUrl());
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
     * 新闻 -- 晚间上市公司利好
     * 主容器 id 为 ContentBody
     * 子控件全为 p 标签; 一般是 2个为一对;
     * 前2p标签为 广告 + "利好消息一览"
     * 随后每2个p标签一组, 为 标题和详细信息.
     * 尾部2p标签, 同样是广告 和 文章来源.
     *
     * @param bean
     * @return 访问http失败将返回null
     */
    public static List<CompanyGoodNew> parseCompanyGoodNews(SimpleNewEm companyGoodNewBean) {
        Assert.isTrue(companyGoodNewBean.isCompanyGoodNew());
        String url = companyGoodNewBean.getUrl();
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        String rawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (rawHtml == null) {
            return null;
        }
        String dateStr = DateUtil.format(companyGoodNewBean.getDateTime(), DatePattern.NORM_DATE_PATTERN);

        Document document = Jsoup.parse(rawHtml);
        Element contentBody = document.getElementById("ContentBody");
        Elements children = contentBody.children(); // 儿子, 为 单个h3 + 多个双数p;  可多组

        // 1.查找 "以下为利好的消息汇总" 标签索引作为开始, 健壮一点, 虽然一般索引为 1;
        int startIndex = 0; // 若失败则从第一个开始
        for (Element child : children) {
            startIndex++; // 此时常态 变为1;
            if (child.text().contains("以下为利好的消息汇总")) {
                break; // 此时 startIndex 就是第一条消息的标题 索引
            }
        }

        List<CompanyGoodNew> items = new ArrayList<>();

        // 2.解析. 每2个p为一组.  当 p1或者p2 包含文章来源字样, 结束解析
        for (int i = startIndex; i < children.size(); i += 2) {
            if (i + 1 >= children.size()) {
                break;
            }
            Element p1 = children.get(i);
            Element p2 = children.get(i + 1);
            if (p1.text().contains("文章来源") || p2.text().contains("文章来源")) {
                break;
            }

            Elements a1 = p1.getElementsByTag("a");
            if (a1.size() == 0) {
                // 某些股票不使用a标签, 直接span标签;例如赣锋锂业
                a1 = p1.getElementsByTag("span");
            }
            CompanyGoodNew item = new CompanyGoodNew();
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
            item.setContent(StrUtil.trim(p2.text()));
            item.setDateStr(dateStr);
            items.add(item);


        }
        return items;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuoteUrl() {
        return quoteUrl;
    }

    public void setQuoteUrl(String quoteUrl) {
        this.quoteUrl = quoteUrl;
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
        return this.name.hashCode() | this.title.hashCode() | this.dateStr.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MajorIssue) {
            MajorIssue o1 = (MajorIssue) o;
            if (this.name.equals(o1.name) && this.title.equals(o1.title) && this.dateStr.equals(o1.dateStr)) {
                return true;
            }
        }
        return false;
    }
}
