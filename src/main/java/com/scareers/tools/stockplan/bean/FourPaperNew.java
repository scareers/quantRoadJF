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
 * 四大证券报(主要媒体) 头条精华
 * 对应url解析逻辑更加接近 MajorIssue
 *
 * @author admin
 * @noti : 比MajorIssue, 删除name字段, quoteUrl字段改为url字段, (因为单条是新闻, 而非股票行情); type保留,但含义为报纸类型, 增加 relatedObject属性, 因为它是新闻,
 * 有针对对象; 类似 SimpleNewEm
 */
@Data
public class FourPaperNew {
    public static List<String> dfSimpleCols = Arrays // 没有id 和 saveTime
            .asList("url", "title", "content", "dateStr", "type",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            );
    public static List<String> dfAllCols = Arrays
            .asList("id", "url", "title", "content", "dateStr", "type", "saveTime",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            );
    // 解析设置
    String url;
    String title;
    String content;
    String dateStr; // 纯日期无时间, 字符串
    String type; // 报纸来源类型
    // 保存到数据库时设置
    Timestamp saveTime; // 保存到数据库时间, 约等于 爬虫运行时刻; 该字段主要用于获取 最后500条爬取的 新闻.
    // 可自定义设置! // 类似 SimpleNewEm, 没有了 相关对象
    String briefly; // 简述
    String relatedObject; // 相关 "对象", 例如大盘报道, 行业新闻, 个股新闻等等
    Double trend; // 偏向: 利空或者利好, -1.0 - 1.0;  0.0 代表绝对无偏向
    String remark; // 备注
    Timestamp lastModified; // 手动修改最后时间;
    Boolean marked = false; // 是否重要?标记
    private long id;  // 数据库自动生成的id

    public static void main(String[] args) {
        Console.log(parseFourPaperNews(getFourPaperNewsOf(DateUtil.date())));

        DateTime now = DateUtil.date();
        Console.log(DateUtil.year(now));
        Console.log(DateUtil.month(now));
        Console.log(DateUtil.dayOfMonth(now));
        Console.log(DateUtil.dayOfWeek(now));
        Console.log(DateUtil.hour(now,true));
        Console.log(DateUtil.minute(now));
        Console.log(DateUtil.second(now));
    }

    /**
     * 给定日期字符串, 获取 国内四大证券报纸、重要财经媒体头版头条内容精华摘要
     *
     * @param dateStr
     * @return
     */
    public static SimpleNewEm getFourPaperNewsOf(String dateStr) {
        return getSpecialNewFromCaiJingDaoDu(dateStr, "国内四大证券报纸、重要财经媒体头版头条内容精华摘要");
    }

    public static SimpleNewEm getFourPaperNewsOf(DateTime date) {
        Console.log(buildDateStr(date));
        return getFourPaperNewsOf(buildDateStr(date));
    }

    /**
     * df 与bean list 的相互转换
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanListWithoutIdAndSaveTime(List<FourPaperNew> items) {
        DataFrame<Object> res = new DataFrame<>(dfSimpleCols);
        for (FourPaperNew bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getUrl());
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getType());
            row.add(bean.getBriefly());
            row.add(bean.getRelatedObject());
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
    public static DataFrame<Object> buildDfFromBeanList(List<FourPaperNew> items) {
        DataFrame<Object> res = new DataFrame<>(dfAllCols);
        for (FourPaperNew bean : items) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getUrl());
            row.add(bean.getTitle());
            row.add(bean.getContent());
            row.add(bean.getDateStr());
            row.add(bean.getType());
            row.add(bean.getSaveTime());
            row.add(bean.getBriefly());
            row.add(bean.getRelatedObject());
            row.add(bean.getTrend());
            row.add(bean.getRemark());
            row.add(bean.getLastModified());
            row.add(bean.getMarked());
            res.append(row);
        }
        return res;
    }

    /**
     * 新闻 -- 四大证券报精华解析
     * 主容器 id 为 ContentBody
     * 子控件为 h3 控件 + 多个p控件
     * h3控件为报纸分类
     * 随后的子p控件为 2个一组,
     * 第一个p为新闻标题, 子标签 a, href为url
     * 第二个p控件为信新闻简短内容
     * 尾部两个p控件为 广告+文章来源
     *
     * @param bean
     * @return 访问http失败将返回null
     */
    public static List<FourPaperNewBatch> parseFourPaperNews(SimpleNewEm fourPaperNewBean) {
        Assert.isTrue(fourPaperNewBean.isFourPaperNew());
        String url = fourPaperNewBean.getUrl();
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        String rawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 3000, 3);
        if (rawHtml == null) {
            return null;
        }
        String dateStr = DateUtil.format(fourPaperNewBean.getDateTime(), DatePattern.NORM_DATE_PATTERN);

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
        List<FourPaperNewBatch> res = new ArrayList<>();

        for (int i = 0; i < h3Indexes.size() - 1; i++) {
            String typeText = StrUtil.trim(children.get(h3Indexes.get(i)).text());
            int startIndex = h3Indexes.get(i) + 1; // 包含
            int endIndex = h3Indexes.get(i + 1) - 1; // 包含
            List<FourPaperNew> items = new ArrayList<>();
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
                FourPaperNew item = new FourPaperNew();


                Element a = a1.get(0);
                item.setUrl(a.attr("href"));
                item.setTitle(StrUtil.trim(p1.text()));
                item.setContent(StrUtil.trim(children.get(j + 1).text()));
                item.setDateStr(dateStr);
                item.setType(typeText);
                items.add(item);
            }
            res.add(new FourPaperNewBatch(items, typeText));
        }

        return res;
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
        if (o instanceof FourPaperNew) {
            FourPaperNew o1 = (FourPaperNew) o;
            if (this.title.equals(o1.title) && this.dateStr.equals(o1.dateStr)) {
                return true;
            }
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRelatedObject(String relatedObject) {
        this.relatedObject = relatedObject;
    }

    public String getUrl() {
        return url;
    }

    public String getRelatedObject() {
        return relatedObject;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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


    /**
     * 某一类的集合, 不使用Map, 使得有序
     */
    @Data
    @AllArgsConstructor
    public static class MajorIssueBatch {
        List<FourPaperNew> items;
        String type; //
    }

    /**
     * 某一类的集合, 不使用Map, 使得有序
     */
    @Data
    @AllArgsConstructor
    public static class FourPaperNewBatch {
        List<FourPaperNew> items;
        String type; //
    }
}