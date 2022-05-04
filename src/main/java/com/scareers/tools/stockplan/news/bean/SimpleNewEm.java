package com.scareers.tools.stockplan.news.bean;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 东方财富 -- 财经频道 -- 所有的基本新闻类型
 * 简单新闻对象;
 * 东财 其他新闻列表, 格式相同
 * https://finance.eastmoney.com/a/ccjdd_1.html
 *
 * @author admin
 * @noti: 1.使用了hibernate框架, 也有df批量保存
 * @noti 相等逻辑为 日期相同且标题相同: 假设 不同类型新闻有同一条新闻, 一般只有一条记录
 */
@Getter
@Setter
@ToString
public class SimpleNewEm {
    public static List<String> dfSimpleCols = Arrays
            .asList("dateTime", "title", "url", "detailTitle", "saveTime", "type", "urlRawHtml",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            );
    public static int CAI_JING_DAO_DU_TYPE = 1; // 财经导读 类型
    public static int ZI_XUN_JING_HUA_TYPE = 0; // 资讯精华

    /*
    静态方法
     */
    private static final Log log = LogUtil.getLogger();

    private long id;  // 数据库自动生成的id

    // 自动
    Date dateTime; // 时间, 精确到分钟, 数据库不使用datetime字段, 使用字符串
    String title; // 东财提取的简单标题
    String url; // 新闻具体url
    String detailTitle; // 原文章具体标题
    Date saveTime; // 保存到数据库时间, 约等于 爬虫运行时刻; 该字段主要用于获取 最后500条爬取的 新闻.
    // 必须设定
    //     * 1.新闻类型
    Integer type; // 整数表示类型; 0.资讯精华  1.财经导读 待增加

    // 需要调用方法访问 url
    String urlRawHtml; // 访问url, 得到的原始html内容. 大多数为纯文字不带样式, 勉强能看;

    // 需要手动设定, 等价于自身对此新闻评价等
    String briefly; // 简述
    String relatedObject; // 相关 "对象", 例如大盘报道, 行业新闻, 个股新闻等等
    Double trend; // 偏向: 利空或者利好, -1.0 - 1.0;  0.0 代表绝对无偏向
    String remark; // 备注
    Date lastModified; // 手动修改最后时间;
    Boolean marked = false; // 重要标记


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

    public SimpleNewEm() {

    }


    /**
     * 将新闻列表转换为 df, 以便保存; 该方法作为爬虫调用,跳过了 hibernate 框架!, 速度更快!
     *
     * @param newEms 一般要求id未设定
     * @return 本方法 并未设置 saveTime; 本方法常态由爬虫调用, 爬虫遍历设置 saveTime字段; 保证该字段含义
     * @key DAO 有类似方法, 但是将数据库查询结果转换为df! 将设置所有字段!
     * @noti 单个bean操作自行使用 hibernate api;
     * <p>
     * 使用hibernate 批量保存使用事务批量提交而非自动提交
     * try {
     * session = HibernateUtil.getSession(); // 获取Session
     * session.beginTransaction(); // 开启事物
     * Medicine medicine = null; // 创建药品对象
     * // 循环获取药品对象
     * for (int i = 0; i < ms.size(); i++) {
     * medicine = (Medicine) ms.get(i); // 获取药品
     * session.save(medicine); // 保存药品对象
     * // 批插入的对象立即写入数据库并释放内存
     * if (i % 10 == 0) {
     * session.flush();
     * session.clear();
     * }
     * }
     * session.getTransaction().commit(); // 提交事物
     * } catch (Exception e) {
     * e.printStackTrace(); // 打印错误信息
     * session.getTransaction().rollback(); // 出错将回滚事物
     * } finally {
     * HibernateUtil.closeSession(session); // 关闭Session
     * }
     */
    public static DataFrame<Object> buildDfFromBeanListWithoutIdAndSaveTime(List<SimpleNewEm> news) {
        DataFrame<Object> res = new DataFrame<>(dfSimpleCols);
        for (SimpleNewEm bean : news) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getDateTime());
            row.add(bean.getTitle());
            row.add(bean.getUrl());
            row.add(bean.getDetailTitle());
            row.add(bean.getSaveTime());
            row.add(bean.getType());
            row.add(bean.getUrlRawHtml());
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
     * 将 df列表, 转换 为bean列表, 逻辑与上相反;
     *
     * @param rawDf 从simple_new 表, 获取的全字段df , 存在自动id自动
     * @return
     */
    public static List<SimpleNewEm> buildBeanListFromDfWithId(DataFrame<Object> rawDf) {
        List<SimpleNewEm> res = new ArrayList<>();
        if (rawDf == null) {
            return res;
        }
        for (int i = 0; i < rawDf.length(); i++) {
            SimpleNewEm bean = new SimpleNewEm();
            bean.setId(Long.parseLong(rawDf.get(i, "id").toString()));
            bean.setDateTime(Timestamp.valueOf(rawDf.get(i, "dateTime").toString()));
            bean.setTitle(rawDf.get(i, "title").toString());
            bean.setUrl(rawDf.get(i, "url").toString());
            bean.setDetailTitle(CommonUtil.toStringOrNull(rawDf.get(i, "detailTitle")));
            bean.setSaveTime(Timestamp.valueOf(rawDf.get(i, "saveTime").toString()));
            bean.setType(Integer.valueOf(rawDf.get(i, "type").toString()));
            bean.setUrlRawHtml(CommonUtil.toStringOrNull(rawDf.get(i, "urlRawHtml")));
            bean.setBriefly(CommonUtil.toStringOrNull(rawDf.get(i, "briefly")));
            bean.setRelatedObject(CommonUtil.toStringOrNull(rawDf.get(i, "relatedObject")));
            Object trend = rawDf.get(i, "trend");
            if (trend != null) {
                bean.setTrend(Double.valueOf(rawDf.get(i, "trend").toString()));
            }
            bean.setRemark(CommonUtil.toStringOrNull(rawDf.get(i, "remark")));
            Object lastModified = rawDf.get(i, "lastModified");
            if (lastModified != null) {
                bean.setLastModified(Timestamp.valueOf(rawDf.get(i, "lastModified").toString()));
            }
            String marked = rawDf.get(i, "marked").toString(); // 默认false, 对应0
            if ("0".equals(marked)) {
                bean.setMarked(false);
            } else {
                bean.setMarked(true);
            }
            res.add(bean);
        }
        return res;
    }

    public static List<String> allCol = Arrays
            .asList("id", "dateTime", "title", "url", "detailTitle", "saveTime", "type", "urlRawHtml",
                    "briefly", "relatedObject", "trend", "remark", "lastModified", "marked"
            ); // 多了id列

    /**
     * list bean 转换为全字段完整df
     *
     * @param news
     * @return
     */
    public static DataFrame<Object> buildDfFromBeanList(List<SimpleNewEm> news) {
        DataFrame<Object> res = new DataFrame<>(allCol);
        for (SimpleNewEm bean : news) {
            List<Object> row = new ArrayList<>();
            row.add(bean.getId());
            row.add(bean.getDateTime());
            row.add(bean.getTitle());
            row.add(bean.getUrl());
            row.add(bean.getDetailTitle());
            row.add(bean.getSaveTime());
            row.add(bean.getType());
            row.add(bean.getUrlRawHtml());
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
     * 可手动访问url, 并设置 原始新闻页面 html;
     */
    public void accessUrlHtml() {
        urlRawHtml = EastMoneyUtil.getAsStrUseHutool(url, null, 5000, 3);
    }

    /**
     * 公司利好标题: x月x日 + "晚间上市公司利好消息一览(附名单)"
     *
     * @return
     */
    public boolean isCompanyGoodNew() {
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
    public boolean isNewsFeed() {
        return title.contains("晚间央视新闻联播财经内容集锦");
    }

    /**
     * 四大报和重要媒体: x月x日 + "国内四大证券报纸、重要财经媒体头版头条内容精华摘要"
     *
     * @return
     */
    public boolean isFourPaperNew() {
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return this.dateTime.hashCode() | this.title.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleNewEm) {
            SimpleNewEm other = (SimpleNewEm) obj;
            return this.title.equals(other.title) && this.dateTime.equals(other.dateTime);
        } else {
            return false;
        }

    }
}
