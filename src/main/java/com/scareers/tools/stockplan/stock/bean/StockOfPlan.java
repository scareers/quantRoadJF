package com.scareers.tools.stockplan.stock.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.sqlapi.ThsDbApi.ThsConceptIndustryRelation;
import com.scareers.sqlapi.ThsDbApi.ThsSimpleStock;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
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

    // 3.核心自定义字段
    // 3.1.行情描述
    @Column(name = "bottomPriceApproximately")
    Double bottomPriceApproximately;  // 底部价格大约! 可自动计算当前从底部至今的涨幅, 手动设定
    @Column(name = "chgPFromBottom")
    Double chgPFromBottom;  // 底部至今涨幅, 自动计算!
    @Column(name = "pricePositionShortTerm", columnDefinition = "varchar(32)")
    String pricePositionShortTerm = PricePosition.UNKNOWN_POSITION; // 当前价格大概位置描述; 短期位置;
    @Column(name = "priceTrend", columnDefinition = "varchar(32)")
    String priceTrend = PriceTrend.UNKNOWN; // 价格趋势状态: 横盘/快升/快降/慢升/慢降
    @Column(name = "oscillationAmplitude", columnDefinition = "varchar(32)")
    String oscillationAmplitude = OscillationAmplitude.UNKNOWN; // 振荡幅度: 小/中/大
    @Column(name = "klineDescription", columnDefinition = "longtext")
    String klineDescription = OscillationAmplitude.UNKNOWN; // k线描述
    @Column(name = "fsDescription", columnDefinition = "longtext")
    String fsDescription = OscillationAmplitude.UNKNOWN; // 分时图描述

    // 3.2.炒作相关 -- 核心
    // 3.2.1: 主线支线
    @Transient
    HashMap<String, ArrayList<String>> lineTypeMap = new HashMap<>(); // 主线支线Map, 将自动收集并设定!
    @Column(name = "lineTypeMap", columnDefinition = "longtext")
    String lineTypeMapJsonStr = "{}"; // key: 主线1/其他; value: 行业/概念列表, 形如 电力_行业
    @Column(name = "lineTypeAmount", columnDefinition = "int")
    Integer lineTypeAmount; // 读取lineTypeMap的 value, 自动计算所有 主线支线  的概念行业数量, 出去未知类型的
    @Transient
    List<String> leaderStockOfList = new ArrayList<>(); // 是哪些行业/概念的龙头股! 元素为行业/概念名称
    @Column(name = "leaderStockOfList", columnDefinition = "longtext")
    String leaderStockOfListJsonStr = "[]"; //

    // 3.2.2: 炒作原因时间阶段
    @Column(name = "hypeReason", columnDefinition = "longtext")
    String hypeReason; // 炒作原因
    @Column(name = "hypeStartDate", columnDefinition = "date")
    Date hypeStartDate; // 本轮炒作大约开始时间
    @Column(name = "hypePhaseCurrent", columnDefinition = "varchar(32)")
    String hypePhaseCurrent = HypePhase.UNKNOWN; // 当前炒作阶段
    @Column(name = "hypeAdvantage", columnDefinition = "longtext")
    String hypeAdvantage = ""; // 炒作优势点
    @Column(name = "hypeDisadvantage", columnDefinition = "longtext")
    String hypeDisadvantage = ""; // 炒作劣势点
    @Column(name = "maxRiskPoint", columnDefinition = "longtext")
    String maxRiskPoint = ""; // 最大风险描述
    @Column(name = "warnings", columnDefinition = "longtext")
    String warnings; // 注意点


    /*
    总体评价字段
     */
    @Column(name = "trend")
    Double trend = 0.0; // -1.0 - 1.0 总体利空利好偏向自定义
    @Column(name = "specificDescription", columnDefinition = "longtext")
    String specificDescription; // 具体其他描述
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
        public static String HORIZONTAL_LINE_LOW = "横盘低点"; // 水平线
        public static String HORIZONTAL_LINE_HIGH = "横盘高点"; // 水平线
        public static String HORIZONTAL_LINE_COMMON = "横盘"; // 水平线

        public static String FAST_RAISE = "快升";
        public static String FAST_RAISE_ADJUST = "快升调整";
        public static String SLOW_RAISE = "慢升";
        public static String SLOW_RAISE_ADJUST = "慢升调整";
        public static String FAST_FALL = "快降";
        public static String FAST_FALL_ADJUST = "快降反弹";
        public static String SLOW_FALL = "慢降";
        public static String SLOW_FALL_ADJUST = "慢降反弹";
        public static String UNKNOWN = "未知";

        public static Vector<String> allPriceTrends = new Vector<>(Arrays.asList(
                HORIZONTAL_LINE_LOW,
                HORIZONTAL_LINE_HIGH,
                HORIZONTAL_LINE_COMMON,
                FAST_RAISE,
                FAST_RAISE_ADJUST,
                SLOW_RAISE,
                SLOW_RAISE_ADJUST,
                FAST_FALL,
                FAST_FALL_ADJUST,
                SLOW_FALL,
                SLOW_FALL_ADJUST,
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
        public static String HAZY = "朦胧";
        public static String INITIAL_START = "初启";
        public static String MAIN_RAISE = "主升";
        public static String MAIN_RAISE_ADJUSTMENT = "主升调整";
        public static String ADJUSTMENT = "调整";
        public static String BUILD_TOP_PERHAPS = "筑顶疑似";
        public static String EBB = "衰退";
        public static String REVIVAL = "再起";

        public static String UNKNOWN = "未知";

        public static Vector<String> allHypePhases = new Vector<>(Arrays.asList(
                HAZY,
                INITIAL_START,
                MAIN_RAISE,
                MAIN_RAISE_ADJUSTMENT,
                ADJUSTMENT,
                BUILD_TOP_PERHAPS,
                EBB,
                REVIVAL,
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
