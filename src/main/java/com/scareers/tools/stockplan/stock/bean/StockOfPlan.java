package com.scareers.tools.stockplan.stock.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
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
import java.util.*;

/**
 * description: 操盘计划时, 核心个股对象! 数据库有大量默认字段; 整合显示,否则字段太多;
 * 基本字段从 ths.stock_list 数据表得来; 数据来源于问财, 主要包含基本属性字段+涨跌停字段
 *
 * @word: hype: 炒作 / hazy 朦胧 / ebb 退潮 / revival 复兴再起
 * @author: admin
 * @date: 2022/3/28/028-21:38:16
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "test_stock",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "type_Index", columnList = "type")})
public class StockOfPlan {
    public static void main(String[] args) {

    }

    /*
    基本字段: 都来自与 ths. industry_list 数据表
     */
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id")
    Long id;
    // 基本信息字段
    @Column(name = "code", columnDefinition = "varchar(32)")
    String code; // 简单代码
    @Column(name = "marketCode", columnDefinition = "int")
    Integer marketCode; // 市场代码
    @Column(name = "stockCode", columnDefinition = "varchar(32)")
    String stockCode; // 股票代码完整
    @Column(name = "name", columnDefinition = "varchar(32)")
    String name; // 股票名称
    @Column(name = "concepts", columnDefinition = "longtext")
    String concepts; // 所属概念, ; 分割
    @Column(name = "conceptAmount", columnDefinition = "int")
    Integer conceptAmount; // 概念数量
    @Column(name = "industries", columnDefinition = "longtext")
    String industries; // 所属行业, 形式:  一级-二级-三级


    @Column(name = "dateStr", columnDefinition = "varchar(32)")
    String dateStr; // 该行业原始数据抓取时的日期

    // 基本数据字段
    @Column(name = "marketValue")
    Double marketValue; // 总市值
    @Column(name = "circulatingMarketValue")
    Double circulatingMarketValue; // 流通市值
    @Column(name = "pe")
    Double pe; //
    @Column(name = "chgP")
    Double chgP; //
    @Column(name = "open")
    Double open; //
    @Column(name = "high")
    Double high; //
    @Column(name = "low")
    Double low; // 最低
    @Column(name = "close")
    Double close; // 收盘价前复权默认
    @Column(name = "turnover")
    Double turnover; //
    @Column(name = "amplitude")
    Double amplitude; // 振幅
    @Column(name = "volRate")
    Double volRate; // 量比


    // 涨停相关字段

    @Column(name = "highLimitType", columnDefinition = "varchar(32)")
    String highLimitType; // 涨停类型, 例如"放量涨停"
    @Column(name = "highLimitReason", columnDefinition = "longtext")
    String highLimitReason;
    @Column(name = "highLimitBlockadeAmount")
    Double highLimitBlockadeAmount;
    @Column(name = "highLimitBlockadeCMVRate")
    Double highLimitBlockadeCMVRate; // 涨停封单额/流通市值
    @Column(name = "highLimitBlockadeVolumeRate")
    Double highLimitBlockadeVolumeRate; // 涨停封成比  封单量/成交量
    @Column(name = "highLimitFirstTime", columnDefinition = "varchar(32)")
    String highLimitFirstTime;
    @Column(name = "highLimitLastTime", columnDefinition = "varchar(32)")
    String highLimitLastTime;
    @Column(name = "highLimitAmountType", columnDefinition = "longtext")
    String highLimitAmountType;
    @Column(name = "highLimitBrokeTimes", columnDefinition = "int")
    Integer highLimitBrokeTimes;
    @Column(name = "highLimitContinuousDays", columnDefinition = "int")
    Integer highLimitContinuousDays;
    @Column(name = "highLimitDetail", columnDefinition = "longtext")
    String highLimitDetail;


    // 跌停相关字段
    @Column(name = "lowLimitType", columnDefinition = "varchar(32)")
    String lowLimitType; // 涨停类型, 例如"放量涨停"
    @Column(name = "lowLimitReason", columnDefinition = "longtext")
    String lowLimitReason;
    @Column(name = "lowLimitBlockadeAmount")
    Double lowLimitBlockadeAmount;
    @Column(name = "lowLimitBlockadeVolCMVRate")
    Double lowLimitBlockadeVolCMVRate;
    @Column(name = "lowLimitBlockadeVolumeRate")
    Double lowLimitBlockadeVolumeRate; // 涨停封成比  封单量/成交量
    @Column(name = "lowLimitFirstTime", columnDefinition = "varchar(32)")
    String lowLimitFirstTime;
    @Column(name = "lowLimitLastTime", columnDefinition = "varchar(32)")
    String lowLimitLastTime;
    @Column(name = "lowLimitBrokeTimes", columnDefinition = "int")
    Integer lowLimitBrokeTimes;
    @Column(name = "lowLimitContinuousDays", columnDefinition = "int")
    Integer lowLimitContinuousDays;
    @Column(name = "lowLimitDetails", columnDefinition = "longtext")
    String lowLimitDetails;

    // 自定义字段
    // 1.生成时间和修改时间
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;

    // 2.个股所属的 行业/概念, 若存在行业概念bean, 则收集对应的 trend 属性, 作为Map, 并且以平均值和标准差衡量 关联行业概念 trend
    @Transient
    HashMap<String, Double> relatedTrendMap = new HashMap<>(); // 关联概念trend字典
    @Column(name = "relatedTrendMap", columnDefinition = "longtext")
    String relatedTrendMapJsonStr = "{}";// key为 名称__行业 或者 名称__概念; 注意split
    @Column(name = "relatedTrendsAvg")
    Double relatedTrendsAvg = 0.0; // 关联 概念trend 平均值
    @Column(name = "relatedTrendsStd")
    Double relatedTrendsStd = 0.0; // 关联概念trend 标准差; 两者简单衡量个股所属行业概念 trend 总体状况;
    // 2.2. 关联trend2, 则计算 所属概念/行业的 relatedTrendsDiscount, 它衡量了 所属概念的 相关概念的影响
    @Transient
    HashMap<String, Double> relatedTrendMap2 = new HashMap<>();
    @Column(name = "relatedTrendMap2", columnDefinition = "longtext")
    String relatedTrendMap2JsonStr = "{}";
    @Column(name = "relatedTrendsAvg2")
    Double relatedTrendsAvg2 = 0.0;
    @Column(name = "relatedTrendsStd2")
    Double relatedTrendsStd2 = 0.0;

    // 3.


    /* 新增基本字段: 主要涨跌停相关的; 均为自动计算自动载入, 且为最新数据
                        + "vol double  null,"
                        + "volRate double  null,"
                        + "ddeNetVol double  null,"
                        + "ddeNetAmount double  null,"
                        + "totalMarketValue double  null,"
                        + "circulatingMarketValue double  null,"
                        + "includeStockAmount int  null,"
                        + "upAmount int  null,"
                        + "downAmount int  null," // 需要推断计算
                        + "upPercent double  null,"
                        + "lowLimitAmount int  null,"
                        + "lowLimitPercent double  null,"
                        + "highLimitAmount int  null,"
                        + "highLimitPercent double  null,"
                        + "lineLowLimitAmount int  null,"
                        + "lineHighLimitAmount int  null,"
                        + "lineHighLimitPercent double  null,"
     */
    // 新增12属性, 用4个label显示;  显示文字均为 [xx,yy,zz] 形式; 但是表格df中 分开列显示
    Double volRate; // 量比
    Double ddeNetAmount; // 主力净额
    Double circulatingMarketValue; // 流通市值

    Integer includeStockAmount; // 成分股数量
    Integer upAmount; // 上涨家数
    Double upPercent; // 上涨家数占比   // 下跌家数也没用上

    Integer highLimitAmount; // 涨停数量
    Double highLimitPercent; // 涨停占比
    Integer lineHighLimitAmount; // 一字涨停数量

    Integer lowLimitAmount; // 跌停数量
    Double lowLimitPercent; // 跌停占比
    Integer lineLowLimitAmount; // 一字跌停数量


    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;

    // 自动计算: 相关性较高的概念列表(名称表示)  // 因行业互斥, 无相关性较高的行业列表
    // 相关性与成分股
    @Transient
    List<ThsConceptIndustryRelation> relatedConceptList = new ArrayList<>(); // 关系列表对象, 算法见 initRelationList()
    @Column(name = "relatedConceptList", columnDefinition = "longtext")
    String relatedConceptListJsonStr = "[]"; // 该列表只保留字符串; 调用 ThsConceptIndustryRelation的两个方法, 从json相互转化
    @Transient
    List<ThsConceptIndustryRelation> relatedIndustryList = new ArrayList<>(); // 关系列表对象, 算法见initRelationList()
    @Column(name = "relatedIndustryList", columnDefinition = "longtext")
    String relatedIndustryListJsonStr = "[]"; // 该列表只保留字符串; 调用 ThsConceptIndustryRelation的两个方法, 从json相互转化

    // @key3: 新增字段: 当有其他概念行业存在时, 设置了trend后, 如果本对象, 关联到该行业/概念,
    // 则应当将对方 的trend, 记录到本属性字典中. 本属性将在任意 实例更新trend值后, 自动计算全部并设置保存! // gui实现
    // 不区分行业或者对象, key:value -> 关联行业或概念: 对方trend
    // relatedTrendsDiscount 则以一定权重(关联性越高,则权重越高), 折算所有关联trend, 加总得到 关联行业概念的 trend加成;
    @Transient
    HashMap<String, Double> relatedTrendMap = new HashMap<>(); // 关联概念trend字典
    @Column(name = "relatedTrendMap", columnDefinition = "longtext") // key为 名称__行业 或者 名称__概念; 注意split
            String relatedTrendMapJsonStr = "{}";
    @Column(name = "relatedTrendsDiscount")
    Double relatedTrendsDiscount = 0.0; // 关联概念trend折算加成


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
    @Column(name = "hypeStartDate", columnDefinition = "date")
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
    Double trend = 0.0; // -1.0 - 1.0 总体利空利好偏向自定义
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


    public void initRelationList() {
        this.relatedConceptList = ThsDbApi.getMaxRelationshipOfConcept(this.name,
                dateStr, 10, 0.6, 0.4, true);
        List<JSONObject> jsons = new ArrayList<>();
        for (ThsConceptIndustryRelation thsConceptIndustryRelation : relatedConceptList) {
            jsons.add(thsConceptIndustryRelation.toJsonObject());
        }
        this.relatedConceptListJsonStr = JSONUtilS.toJsonPrettyStr(jsons); // 初始化为字符串

        this.relatedIndustryList = ThsDbApi.getMaxRelationshipOfIndustryLevel2(this.name,
                dateStr, 10, 0.6, 0.4, true);
        List<JSONObject> jsons2 = new ArrayList<>();
        for (ThsConceptIndustryRelation thsConceptIndustryRelation : relatedIndustryList) {
            jsons2.add(thsConceptIndustryRelation.toJsonObject());
        }
        this.relatedIndustryListJsonStr = JSONUtilS.toJsonPrettyStr(jsons2); // 初始化为字符串
    }

    public void initIncludeStockList() {
        this.includeStockList = ThsDbApi.getConceptOrIndustryIncludeStocks(name, dateStr);
        if (this.includeStockList == null) {
            this.includeStockList = new ArrayList<>(); // null则设置为空
        }
        List<JSONObject> jsons = new ArrayList<>();
        for (ThsSimpleStock thsSimpleStock : includeStockList) {
            jsons.add(thsSimpleStock.toJsonObject());
        }
        this.includeStockListJsonStr = JSONUtilS.toJsonPrettyStr(jsons); // 初始化为字符串
    }

    /**
     * 从数据表获取bean时, 需要自动填充 transient 字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        initRelatedConceptListWhenBeanFromDb();
        initRelatedIndustryListWhenBeanFromDb();
        initIncludeStockListWhenBeanFromDb();
        initLeaderStockListWhenBeanFromDb();
        initRelatedTrendMapWhenBeanFromDb();
    }

    private void initRelatedConceptListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.relatedConceptListJsonStr);
        List<ThsConceptIndustryRelation> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsConceptIndustryRelation.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.relatedConceptList = res; // 一次性更新
    }

    private void initRelatedIndustryListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.relatedIndustryListJsonStr);
        List<ThsConceptIndustryRelation> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsConceptIndustryRelation.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.relatedIndustryList = res; // 一次性更新
    }

    private void initIncludeStockListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.includeStockListJsonStr);
        List<ThsSimpleStock> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsSimpleStock.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.includeStockList = res; // 一次性更新
    }

    private void initLeaderStockListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.leaderStockListJsonStr);
        List<ThsSimpleStock> res = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            res.add(ThsSimpleStock.createFromJsonObject(objects.getJSONObject(i)));
        }
        this.leaderStockList = res; // 一次性更新
    }

    private void initRelatedTrendMapWhenBeanFromDb() {
        JSONObject objects = JSONUtilS.parseObj(this.relatedTrendMapJsonStr);
        HashMap<String, Double> res = new HashMap<>();
        for (String s : objects.keySet()) {
            res.put(s, objects.getDouble(s));
        }
        this.relatedTrendMap = res; // 一次性更新
    }

    /*
    数据api, 主要同时更新 list和对应的json; 按需实现
     */
    public void updateLeaderStockList(List<ThsSimpleStock> newList) {
        List<JSONObject> jsonObjectList = new ArrayList<>();
        for (ThsSimpleStock thsSimpleStock : newList) {
            jsonObjectList.add(thsSimpleStock.toJsonObject());
        }
        this.leaderStockListJsonStr = JSONUtilS.toJsonPrettyStr(jsonObjectList);
        this.leaderStockList = newList;
    }

    /**
     * 此时RelatedTrendMap已经设置好, 更新对应 jsonStr 字段
     */
    public void updateRelatedTrendMapJsonStr() {
        this.relatedTrendMapJsonStr = JSONUtilS.toJsonPrettyStr(this.relatedTrendMap);
    }

    /**
     * 此时RelatedTrendMap已经设置好, 使用自定义算法, 计算 关联概念行业 折算trend 因子;
     * 这里简单 进行加法
     */
    public void calcRelatedTrendsDiscount() {
        Double res = 0.0;
        for (String s : this.relatedTrendMap.keySet()) {
            List<String> nameAndType = StrUtil.split(s, "__");
            String name = nameAndType.get(0);
            String type = nameAndType.get(1);
            Double trend = this.relatedTrendMap.get(s);
            if ("行业".equals(type)) {
                for (ThsConceptIndustryRelation relation : this.relatedIndustryList) {
                    if (relation.getNameB().equals(name)) {
                        double relationValue = relation.calcRelationValue(0.3, 0.7); // 关系值,偏向b一点
                        res += relationValue * trend;
                        break; // 只可能1
                    }
                }
            } else if ("概念".equals(type)) {
                for (ThsConceptIndustryRelation relation : this.relatedConceptList) {
                    if (relation.getNameB().equals(name)) {
                        double relationValue = relation.calcRelationValue(0.3, 0.7); // 关系值,偏向b一点
                        res += relationValue * trend;
                        break; // 只可能1次
                    }
                }
            }
        }
        this.relatedTrendsDiscount = CommonUtil.roundHalfUP(res, 3); // 保留3位小数
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

        public static Vector<String> allLineTypes = new Vector<>(
                Arrays.asList(
                        MAIN_LINE_1,
                        MAIN_LINE_2,
                        MAIN_LINE_3,
                        BRANCH_LINE_1,
                        BRANCH_LINE_2,
                        BRANCH_LINE_3,
                        SPECIAL_LINE,
                        CARE_LINE,
                        OTHER_LINE
                )
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

        public static Vector<String> allPricePositions = new Vector<>(Arrays.asList(
                HIGH_POSITION,
                MEDIUM_HIGH_POSITION,
                LOW_POSITION,
                MEDIUM_LOW_POSITION,
                MEDIUM_POSITION,
                UNKNOWN_POSITION
        ));
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

        public static Vector<String> allPriceTrends = new Vector<>(Arrays.asList(
                HORIZONTAL_LINE,
                FAST_RAISE,
                SLOW_RAISE,
                FAST_FALL,
                SLOW_FALL,
                UNKNOWN
        ));
    }

    /**
     * 震荡程度描述; 该幅度 通常基于 价格涨跌幅, 而非绝对数值
     */
    public static class OscillationAmplitude {
        public static String BIG_AMPLITUDE = "大";
        public static String SMALL_AMPLITUDE = "小";
        public static String MEDIUM_AMPLITUDE = "中";
        public static String UNKNOWN = "未知";

        public static Vector<String> allOscillationAmplitudes = new Vector<>(Arrays.asList(
                BIG_AMPLITUDE,
                SMALL_AMPLITUDE,
                MEDIUM_AMPLITUDE,
                UNKNOWN
        ));
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

        public static Vector<String> allHypePhases = new Vector<>(Arrays.asList(
                HAZY_PHASE,
                INITIAL_START_PHASE,
                SPEED_UP_PHASE,
                ADJUSTMENT_PHASE,
                EBB_PHASE,
                REVIVAL_PHASE,
                UNKNOWN
        ));
    }

    public static Double tryParseDoubleOfLastLine(DataFrame<Object> dataFrame, Object colName) {
        try {
            return Double.valueOf(dataFrame.get(dataFrame.length() - 1, colName).toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer tryParseIntegerOfLastLine(DataFrame<Object> dataFrame, Object colName) {
        try {
            return Integer.valueOf(dataFrame.get(dataFrame.length() - 1, colName).toString());
        } catch (Exception e) {
            return null;
        }
    }
}
