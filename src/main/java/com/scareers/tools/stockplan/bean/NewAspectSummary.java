package com.scareers.tools.stockplan.bean;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONArray;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.utils.JSONUtilS;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 资讯面个人总结 bean
 *
 * @word: view 此处特指自定义预判观点看法...   point: 归纳的 利好/利空/中性/其他 点(方面,消息等)
 * @author: admin
 * @date: 2022/3/17/017-19:12:42
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "new_aspect_summary",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "type_Index", columnList = "type")})
public class NewAspectSummary {
    public static void main(String[] args) {

    }

    public static NewAspectSummary newInstance() {
        NewAspectSummary bean = new NewAspectSummary();
        bean.setGeneratedTime(DateUtil.date()); // now

        return null;
    }

    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id", unique = true)
    Long id;
    @Column(name = "type", length = 64)
    String type; // 操盘? 复盘?
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;

    @Column(name = "dateStr", length = 32)
    String dateStr; // 简单日期字符串, 特指对某一天的观点.  一般将以此字段读取

    // 核心字段: 预判看法列表 + trend预判 + 备注
    @Transient
    List<String> preJudgmentViews = new ArrayList<>(); // 预判观点, 核心字段
    @Column(name = "preJudgmentViews", columnDefinition = "longtext")
    String preJudgmentViewsJsonStr = "[]";
    @Column(name = "trend")
    Double trend; // -1.0 - 1.0 利空利好偏向自定义
    @Column(name = "remark", columnDefinition = "longtext")
    String remark; // 备注

    /*
    3大未来字段: 未来评分 / 评分原因 / 未来情景还原
     */
    @Column(name = "scoreFromFuture")
    Double scoreFromFuture; // 未来时间, 可来此评分!
    @Column(name = "scoreReason", columnDefinition = "longtext")
    String scoreReason; // 如此评分的原因
    @Transient
    List<String> futures = new ArrayList<>(); // 未来实际发生 描述 : 未来情景描述
    @Column(name = "futures", columnDefinition = "longtext")
    String futuresJsonStr = "[]"; // 未来情景描述, json字符串

    // 维护四大列表, 为了数据库便于维护, 不使用外键方式, 简单保存 jsonStr;
    // 这些字段均不保存到数据库
    @Transient
    List<String> goodPoints = new ArrayList<>(); // 利好因素
    @Transient
    List<String> badPoints = new ArrayList<>(); // 利空因素
    @Transient
    List<String> neutralPoints = new ArrayList<>(); // 中性因素
    @Transient
    List<String> otherPoints = new ArrayList<>(); // 其他因素

    // 与之对应的4大字符串. 这些字符串不手动设定, 当每次修改列表时, 将自动转换json, 自动设置!
    @Column(name = "goodPoints", columnDefinition = "longtext")
    String goodPointsJsonStr = "[]";
    @Column(name = "badPoints", columnDefinition = "longtext")
    String badPointsJsonStr = "[]";
    @Column(name = "neutralPoints", columnDefinition = "longtext")
    String neutralPointsJsonStr = "[]";
    @Column(name = "otherPoints", columnDefinition = "longtext")
    String otherPointsJsonStr = "[]";

    /**
     * 核心方法: 当bean单纯从数据库读取而来时, 对于那些 transient 的字段,
     * 应当从对应被序列化的字段, 计算而来!!!
     * 本方法在 常在Dao中 调用.
     * 当前共 6 个非序列化字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        // 预判
        setTransientAttrByJsonStr(preJudgmentViewsJsonStr, preJudgmentViews);
        setTransientAttrByJsonStr(futuresJsonStr, futures);
        setTransientAttrByJsonStr(goodPointsJsonStr, goodPoints);
        setTransientAttrByJsonStr(badPointsJsonStr, badPoints);
        setTransientAttrByJsonStr(neutralPointsJsonStr, neutralPoints);
        setTransientAttrByJsonStr(otherPointsJsonStr, otherPoints);
    }

    private void setTransientAttrByJsonStr(String jsonStr, List<String> transientAttr) {
        JSONArray raw = JSONUtilS.parseArray(jsonStr);
        if (raw.size() > 0) {
            transientAttr.clear();
            for (Object o : raw) {
                transientAttr.add(o.toString());
            }
        }
    }


    /*
    4大添加 观点方法, 一般调用这4个api, 而非直接访问!
     */

    public void addGoodPoint(String view) {
        goodPoints.add(view);
        goodPointsJsonStr = JSONUtilS.toJsonStr(goodPoints);
    }

    public void addBadPoint(String view) {
        badPoints.add(view);
        badPointsJsonStr = JSONUtilS.toJsonStr(badPoints);
    }

    public void addNeutralPoint(String view) {
        neutralPoints.add(view);
        neutralPointsJsonStr = JSONUtilS.toJsonStr(neutralPoints);
    }

    public void addOtherPoint(String view) {
        otherPoints.add(view);
        otherPointsJsonStr = JSONUtilS.toJsonStr(otherPoints);
    }

    /*
    预判添加
     */
    public void addPreJudgmentView(String view) {
        preJudgmentViews.add(view);
        preJudgmentViewsJsonStr = JSONUtilS.toJsonStr(preJudgmentViews);
    }

    /*
    未来情景描述 添加方法
     */
    public void addFuture(String futureState) {
        futures.add(futureState);
        futuresJsonStr = JSONUtilS.toJsonStr(futures);
    }


}
