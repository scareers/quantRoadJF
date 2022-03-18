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
 * @noti 原则上: 每个日期 dateStr, 只有两类(type), 两个NewAspectSummary对象; 当日初始化后, 后续均基于其进行修改!
 * @word: view 此处特指自定义预判观点看法...   point: 归纳的 利好/利空/中性/其他 点(方面,消息等)
 * @author: admin
 * @noti 当前共 8 个非序列化字段, 均有对应的 jsonStr字段被保存. 从数据库读取后, DAO负责解析并初始化transient字段
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

    // bean类型列表
    public static String PLAN_TYPE = "plan";
    public static String REVIEW_TYPE = "review";

    // 消息类型列表
    public static String POINT_TYPE_GOOD = "利好";
    public static String POINT_TYPE_BAD = "利空";
    public static String POINT_TYPE_NEUTRAL = "中性";
    public static String POINT_TYPE_OTHER = "其他";

    /*
    最常用的初始化工厂方法
     */
    public static NewAspectSummary newInstance(String dateStr, String type) {
        NewAspectSummary bean = new NewAspectSummary();
        bean.setType(type);
        bean.setGeneratedTime(DateUtil.date());
        bean.setDateStr(dateStr); // 仅仅需要3个字段!
        return bean;
    }

    /*
    基本字段
     */
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id", unique = true)
    Long id;
    @Column(name = "type", length = 64)
    String type; // 操盘 plan? 复盘 review? 见类型列表
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;
    @Column(name = "dateStr", length = 32)
    String dateStr; // 简单日期字符串, 特指对某一天的观点.  一般将以此字段读取

    /*
    4类因素
     */

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


    /*
    总体评价字段
     */
    @Column(name = "trend")
    Double trend; // -1.0 - 1.0 总体利空利好偏向自定义
    @Column(name = "remark", columnDefinition = "longtext")
    String remark; // 总体备注

    /*
    预判相关 + 未来对预判打分
     */
    // 核心字段: 预判看法列表 + 未来实际情景列表 + 对预判评分列表 + 评分解析列表; 它们将一一对应
    // 增加预判项目时, 自动添加(初始化)对应的 情景/评分/评分解析 项目. 始终维持 4个列表项目相同
    @Transient
    List<String> preJudgmentViews = new ArrayList<>(); // 预判观点, 核心字段
    @Column(name = "preJudgmentViews", columnDefinition = "longtext")
    String preJudgmentViewsJsonStr = "[]";
    @Transient
    List<String> futures = new ArrayList<>(); // 未来情况列表
    @Column(name = "futures", columnDefinition = "longtext")
    String futuresJsonStr = "[]";
    @Transient
    List<Double> scoresOfPreJudgment = new ArrayList<>(); // 未来对预判进行评分, 范围 -100.0 - 100.0; 默认0
    @Column(name = "scoresOfPreJudgment", columnDefinition = "longtext")
    String scoresOfPreJudgmentJsonStr = "[]"; // 未来情景描述, json字符串
    @Transient
    List<String> scoreReasons = new ArrayList<>(); // 如此评分的原因
    @Column(name = "scoreReasons", columnDefinition = "longtext")
    String scoreReasonsJsonStr = "[]"; // 未来情景描述, json字符串

    @Column(name = "scoreSchemaOfPreJudgment", columnDefinition = "longtext")
    Double scoreSchemaOfPreJudgment; // 未来对总体预判的总评分


    /**
     * 核心方法: 当bean单纯从数据库读取而来时, 对于那些 transient 的字段,
     * 应当从对应被序列化的字段, 计算而来!!!
     * 本方法在 常在Dao中 调用.
     * 当前共 8 个非序列化字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        // 预判
        setTransientAttrByJsonStr(preJudgmentViewsJsonStr, preJudgmentViews);
        setTransientAttrByJsonStr(futuresJsonStr, futures);
        setTransientAttrByJsonStrOfDouble(scoresOfPreJudgmentJsonStr, scoresOfPreJudgment);
        setTransientAttrByJsonStr(scoreReasonsJsonStr, scoreReasons);

        // 新闻总结
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

    private void setTransientAttrByJsonStrOfDouble(String jsonStr, List<Double> transientAttr) {
        JSONArray raw = JSONUtilS.parseArray(jsonStr);
        if (raw.size() > 0) {
            transientAttr.clear();
            for (Object o : raw) {
                transientAttr.add(Double.parseDouble(o.toString()));
            }
        }
    }


    /*
    4大添加/更新 新闻总结 方法, 一般调用这4个api, 而非直接访问!
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

    public void updateGoodPoint(int index, String view) {
        goodPoints.set(index, view);
        goodPointsJsonStr = JSONUtilS.toJsonStr(goodPoints);
    }

    public void updateBadPoint(int index, String view) {
        badPoints.set(index, view);
        badPointsJsonStr = JSONUtilS.toJsonStr(badPoints);
    }

    public void updateNeutralPoint(int index, String view) {
        neutralPoints.set(index, view);
        neutralPointsJsonStr = JSONUtilS.toJsonStr(neutralPoints);
    }

    public void updateOtherPoint(int index, String view) {
        otherPoints.set(index, view);
        otherPointsJsonStr = JSONUtilS.toJsonStr(otherPoints);
    }

    public void removeGoodPoint(int index) {
        goodPoints.remove(index);
        goodPointsJsonStr = JSONUtilS.toJsonStr(goodPoints);
    }

    public void removeBadPoint(int index) {
        badPoints.remove(index);
        badPointsJsonStr = JSONUtilS.toJsonStr(badPoints);
    }

    public void removeNeutralPoint(int index) {
        neutralPoints.remove(index);
        neutralPointsJsonStr = JSONUtilS.toJsonStr(neutralPoints);
    }

    public void removeOtherPoint(int index) {
        otherPoints.remove(index);
        otherPointsJsonStr = JSONUtilS.toJsonStr(otherPoints);
    }

    /*
    预判及打分 添加api, 唯一public
     */

    /**
     * 预判添加, public;  将自动添加对应3项. 默认值
     */
    public void addPreJudgmentView(String view) {
        preJudgmentViews.add(view);
        preJudgmentViewsJsonStr = JSONUtilS.toJsonStr(preJudgmentViews);
        addFuture("");
        addScoreOfPreJudgment(0.0); // 默认得分 0.0
        addScoreReason("");
    }

    // 因未来情景, 未来打分, 打分原因 均应当随着预判观点添加而添加, 因此 private;

    /**
     * 未来情景描述 添加方法
     */
    private void addFuture(String futureState) {
        futures.add(futureState);
        futuresJsonStr = JSONUtilS.toJsonStr(futures);
    }

    /**
     * scoresOfPreJudgment 评分添加
     */
    private void addScoreOfPreJudgment(Double score) {
        scoresOfPreJudgment.add(score);
        scoresOfPreJudgmentJsonStr = JSONUtilS.toJsonStr(scoresOfPreJudgment);
    }

    /**
     * 评分原因添加
     */
    private void addScoreReason(String reason) {
        scoreReasons.add(reason);
        scoreReasonsJsonStr = JSONUtilS.toJsonStr(scoreReasons);
    }

    /*
    预判及打分 修改api, 需要给定 index 以及 view
     */
    public void updatePreJudgmentView(int index, String view) {
        preJudgmentViews.set(index, view);
        preJudgmentViewsJsonStr = JSONUtilS.toJsonStr(preJudgmentViews);
    }

    private void addFuture(int index, String futureState) {
        futures.set(index, futureState);
        futuresJsonStr = JSONUtilS.toJsonStr(futures);
    }

    private void addScoreOfPreJudgment(int index, Double score) {
        scoresOfPreJudgment.set(index, score);
        scoresOfPreJudgmentJsonStr = JSONUtilS.toJsonStr(scoresOfPreJudgment);
    }

    private void addScoreReason(int index, String reason) {
        scoreReasons.set(index, reason);
        scoreReasonsJsonStr = JSONUtilS.toJsonStr(scoreReasons);
    }

}
