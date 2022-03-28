package com.scareers.tools.stockplan.indusconcep.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.sqlapi.ThsDbApi.ThsConceptIndustryRelation;
import com.scareers.sqlapi.ThsDbApi.ThsSimpleStock;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * description: 操盘计划时, 使用的同花顺行业对象 bean
 * 源数据参考 industry_list 数据表
 *
 * @word: hype: 炒作 / hazy 朦胧 / ebb 退潮 / revival 复兴再起
 * @author: admin
 * @date: 2022/3/28/028-21:38:16
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "plan_of_industry_and_concept",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "type_Index", columnList = "type")})
public class IndustryThsOfPlan {
    public static void main(String[] args) {
        IndustryThsOfPlan bean = IndustryThsOfPlan.newInstance("电力", "2022-03-28");
        Console.log(bean);
    }

    /**
     * 最常用的初始化工厂方法:
     * 当给定日期没有记录时, 将尝试访问所有记录, 应用最后一条, 构建具有基本字段的bean
     */
    public static IndustryThsOfPlan newInstance(String industryName, String dateStr) {
        IndustryThsOfPlan bean = new IndustryThsOfPlan();
        DataFrame<Object> dfTemp = ThsDbApi.getIndustryByNameAndDate(industryName, dateStr);
        // @cols [id, chgP, close, code, industryIndex, industryType, name, marketCode, indexCode, dateStr]
        if (dfTemp == null || dfTemp.length() == 0) {
            dfTemp = ThsDbApi.getIndustryAllRecordByName(industryName); // 获取所有记录后, 将获取最后一条
        }
        bean.setName(industryName);
        bean.setType(CommonUtil.toStringOrNull(dfTemp.get(dfTemp.length() - 1, "industryType")));
        bean.setCode(CommonUtil.toStringOrNull(dfTemp.get(dfTemp.length() - 1, "code")));
        bean.setIndexCode(CommonUtil.toStringOrNull(dfTemp.get(dfTemp.length() - 1, "indexCode")));
        bean.setDateStr(CommonUtil.toStringOrNull(dfTemp.get(dfTemp.length() - 1, "dateStr")));

        Double chgP = null;
        try {
            chgP = Double.valueOf(dfTemp.get(dfTemp.length() - 1, "chgP").toString());
        } catch (NumberFormatException e) {
        }
        bean.setChgP(chgP);
        bean.setGeneratedTime(DateUtil.date());

        bean.initRelationList(); // 初始化关系列表
        bean.initIncludeStockList(); // 初始化成分股列表
        return bean; // 只有基本字段
    }


    /*
    基本字段: 都来自与 ths. industry_list 数据表
     */
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id")
    Long id;
    @Column(name = "name", columnDefinition = "varchar(32)")
    String name; // 行业名称
    @Column(name = "type", columnDefinition = "varchar(32)")
    String type; // "二级行业" 或者 "三级行业"
    @Column(name = "code", columnDefinition = "varchar(32)")
    String code; // 简单代码
    @Column(name = "indexCode", columnDefinition = "varchar(32)")
    String indexCode; // 完整代码, 一般有 .TI 后缀
    @Column(name = "dateStr", columnDefinition = "varchar(32)")
    String dateStr; // 该行业原始数据抓取时的日期
    @Column(name = "chgP", length = 64)
    Double chgP; // 最新涨跌幅

    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;

    // 自动计算: 相关性较高的概念列表(名称表示)  // 因行业互斥, 无相关性较高的行业列表
    // 相关性与成分股
    @Transient
    List<ThsConceptIndustryRelation> relatedConceptList = new ArrayList<>(); // 关系列表对象, 算法见initRelationList()
    @Column(name = "relatedConceptList", columnDefinition = "longtext")
    String relatedConceptListJsonStr = "[]"; // 该列表只保留字符串; 调用 ThsConceptIndustryRelation的两个方法, 从json相互转化
    @Transient
    List<ThsSimpleStock> includeStockList = new ArrayList<>(); // 成分股列表
    @Column(name = "includeStockList", columnDefinition = "longtext")
    String includeStockListJsonStr = "[]"; // 成分股列表json字符串


    /*
    核心自定义字段: 未指明长短期, 默认短期
     */
    // 1.行情描述
    @Column(name = "pricePositionShortTerm", columnDefinition = "varchar(32)")
    String pricePositionShortTerm = PricePosition.UNKNOWN_POSITION; // 当前价格大概位置描述; 短期位置; 默认未知
    @Column(name = "pricePositionLongTerm", columnDefinition = "varchar(32)")
    String pricePositionLongTerm = PricePosition.UNKNOWN_POSITION; // 当前价格大概位置描述; 长期位置;  两个位置字段可取值相同.
    @Column(name = "priceTrend", columnDefinition = "varchar(32)")
    String priceTrend = PriceTrend.UNKNOWN; // 价格状态: 横盘/快升/快降/慢升/慢降
    @Column(name = "oscillationAmplitude", columnDefinition = "varchar(32)")
    String oscillationAmplitude = OscillationAmplitude.UNKNOWN; // 振荡幅度: 小/中/大
    // 2.炒作相关 -- 核心
    @Column(name = "lineType", columnDefinition = "varchar(32)")
    String lineType = LineType.OTHER_LINE; // 主线还是支线? 默认其他线
    @Column(name = "hypeReason", columnDefinition = "longtext")
    String hypeReason; // 炒作原因
    @Column(name = "hypeStartDate", columnDefinition = "datetime")
    Date hypeStartDate; // 本轮炒作大约开始时间
    @Column(name = "hypePhaseCurrent", columnDefinition = "varchar(32)")
    String hypePhaseCurrent = HypePhase.UNKNOWN; // 当前炒作阶段
    @Column(name = "specificDescription", columnDefinition = "longtext")
    String specificDescription; // 具体其他描述
    // 半自动: 龙头股设定;  需要自行手动设定!
    @Transient
    List<ThsSimpleStock> leaderStockList = new ArrayList<>(); // 龙头股列表
    @Column(name = "leaderStockList", columnDefinition = "longtext")
    String leaderStockListJsonStr = "[]"; // 龙头股列表json字符串

    // 利好利空
    @Column(name = "goodAspects", columnDefinition = "longtext")
    String goodAspects; // 利好消息, 为了方便, 这里自行将内容 分段分点, 程序上不维护列表
    @Column(name = "badAspects", columnDefinition = "longtext")
    String badAspects; // 利空
    @Column(name = "warnings", columnDefinition = "longtext")
    String warnings; // 注意点

    /*
    总体评价字段
     */
    @Column(name = "trend")
    Double trend; // -1.0 - 1.0 总体利空利好偏向自定义
    @Column(name = "remark", columnDefinition = "longtext")
    String remark; // 总体备注

    /*
     * 预判及未来打分: 为了简便, 自行分点, 这里只用长字符串表达整个预判内容等
     */
    @Column(name = "preJudgmentViews", columnDefinition = "longtext")
    String preJudgmentViews = "";
    @Column(name = "futures", columnDefinition = "longtext")
    String futures = "";
    @Column(name = "scoreOfPreJudgment")
    Double scoreOfPreJudgment = 0.0; // 未来对预判进行评分, 范围 -100.0 - 100.0; 默认0
    @Column(name = "scoreReason", columnDefinition = "longtext")
    String scoreReason = ""; // 得分原因


    private void initRelationList() {
        this.relatedConceptList = ThsDbApi.getMaxRelationshipOfConcept(this.name,
                dateStr, 10, 0.6, 0.4, true);
        List<JSONObject> jsons = new ArrayList<>();
        for (ThsConceptIndustryRelation thsConceptIndustryRelation : relatedConceptList) {
            jsons.add(thsConceptIndustryRelation.toJsonObject());
        }
        this.relatedConceptListJsonStr = JSONUtilS.toJsonStr(jsons); // 初始化为字符串
    }

    private void initIncludeStockList() {
        this.includeStockList = ThsDbApi.getConceptOrIndustryIncludeStocks(name, dateStr);
        if (this.includeStockList == null) {
            this.includeStockList = new ArrayList<>(); // null则设置为空
        }
        List<JSONObject> jsons = new ArrayList<>();
        for (ThsSimpleStock thsSimpleStock : includeStockList) {
            jsons.add(thsSimpleStock.toJsonObject());
        }
        this.includeStockListJsonStr = JSONUtilS.toJsonStr(jsons); // 初始化为字符串
    }

    /**
     * 从数据表获取bean时, 需要自动填充 transient 字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        initRelatedConceptListWhenBeanFromDb();
        initIncludeStockListWhenBeanFromDb();
        initLeaderStockListWhenBeanFromDb();
    }

    public void initRelatedConceptListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.relatedConceptListJsonStr);
        List<ThsConceptIndustryRelation> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsConceptIndustryRelation.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.relatedConceptList = res; // 一次性更新
    }

    public void initIncludeStockListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.includeStockListJsonStr);
        List<ThsSimpleStock> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsSimpleStock.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.includeStockList = res; // 一次性更新
    }

    public void initLeaderStockListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.leaderStockListJsonStr);
        List<ThsSimpleStock> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsSimpleStock.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.leaderStockList = res; // 一次性更新
    }


    /**
     * 行业主线支线类型, 这里不使用枚举, 直接使用字符串
     */
    public static class LineType {
        public static String MAIN_LINE_1 = "主线1";
        public static String MAIN_LINE_2 = "主线2";
        public static String MAIN_LINE_3 = "主线3";

        public static String BRANCH_LINE_1 = "支线1";
        public static String BRANCH_LINE_2 = "支线2";
        public static String BRANCH_LINE_3 = "支线3";

        public static String SPECIAL_LINE = "特殊线";
        public static String CARE_LINE = "关注线";
        public static String OTHER_LINE = "其他线";

        public static List<String> allLineTypes = Arrays.asList(
                MAIN_LINE_1,
                MAIN_LINE_2,
                MAIN_LINE_3,
                BRANCH_LINE_1,
                BRANCH_LINE_2,
                BRANCH_LINE_3,
                SPECIAL_LINE,
                CARE_LINE,
                OTHER_LINE
        );
    }

    /**
     * 行业价格大概位置描述
     */
    public static class PricePosition {
        public static String HIGH_POSITION = "高位";
        public static String MEDIUM_HIGH_POSITION = "中高位";
        public static String LOW_POSITION = "低位";
        public static String MEDIUM_LOW_POSITION = "中低位";
        public static String MEDIUM_POSITION = "中位";

        public static String UNKNOWN_POSITION = "未知";

        public static List<String> allPricePositions = Arrays.asList(
                HIGH_POSITION,
                MEDIUM_HIGH_POSITION,
                LOW_POSITION,
                MEDIUM_LOW_POSITION,
                MEDIUM_POSITION,
                UNKNOWN_POSITION
        );
    }

    /**
     * 趋势描述
     */
    public static class PriceTrend {
        public static String HORIZONTAL_LINE = "横盘"; // 水平线
        public static String FAST_RAISE = "快升";
        public static String SLOW_RAISE = "慢升";
        public static String FAST_FALL = "快降";
        public static String SLOW_FALL = "慢降";
        public static String UNKNOWN = "未知";

        public static List<String> allPriceTrends = Arrays.asList(
                HORIZONTAL_LINE,
                FAST_RAISE,
                SLOW_RAISE,
                FAST_FALL,
                SLOW_FALL,
                UNKNOWN
        );
    }

    /**
     * 震荡程度描述; 该幅度 通常基于 价格涨跌幅, 而非绝对数值
     */
    public static class OscillationAmplitude {
        public static String BIG_AMPLITUDE = "大";
        public static String SMALL_AMPLITUDE = "小";
        public static String MEDIUM_AMPLITUDE = "中";
        public static String UNKNOWN = "未知";

        public static List<String> allOscillationAmplitudes = Arrays.asList(
                BIG_AMPLITUDE,
                SMALL_AMPLITUDE,
                MEDIUM_AMPLITUDE,
                UNKNOWN
        );
    }

    /**
     * 表示当前 炒作进行到的阶段
     */
    public static class HypePhase {
        public static String HAZY_PHASE = "朦胧"; //
        public static String INITIAL_START_PHASE = "初启";
        public static String SPEED_UP_PHASE = "加速";
        public static String ADJUSTMENT_PHASE = "调整";
        public static String EBB_PHASE = "衰退";
        public static String REVIVAL_PHASE = "再起";

        public static String UNKNOWN = "未知";

        public static List<String> allHypePhases = Arrays.asList(
                HAZY_PHASE,
                INITIAL_START_PHASE,
                SPEED_UP_PHASE,
                ADJUSTMENT_PHASE,
                EBB_PHASE,
                REVIVAL_PHASE,
                UNKNOWN
        );
    }
}
