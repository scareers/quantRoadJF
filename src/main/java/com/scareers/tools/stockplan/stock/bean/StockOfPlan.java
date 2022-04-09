package com.scareers.tools.stockplan.stock.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.tools.stockplan.stock.bean.selector.StockSelector;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 操盘计划时, 核心个股对象! 数据库有大量默认字段; 整合显示,否则字段太多;
 * 基本字段从 ths.stock_list 数据表得来; 数据来源于问财, 主要包含基本属性字段+涨跌停字段
 * --> 当前字段总数: 84 // 20220409
 *
 * @word: hype: 炒作 / hazy 朦胧 / ebb 退潮 / revival 复兴再起
 * @author: admin
 * @date: 2022/3/28/028-21:38:16
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "plan_of_stock",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "code_Index", columnList = "code")})
public class StockOfPlan {
    public static void main(String[] args) throws Exception {
        List<IndustryConceptThsOfPlan> beanListForPlan = IndustryConceptThsOfPlanDao
                .getBeanListForPlan(DateUtil.date());
        StockOfPlan bean = newInstance("000568", "2022-04-09", beanListForPlan);

        Console.log(bean.getRelatedTrendMap2());
        Console.log(bean.getRelatedTrendsAvg());
        Console.log(bean.getRelatedTrendsStd());
        Console.log(bean.getRelatedTrendMap());
        Console.log(bean.getLeaderStockOfList());
        Console.log(bean.getLeaderStockOfListJsonStr());


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

    /**
     * 核心工厂方法; 需要提供 6位个股代码, 以及 dateStr; 该日期字符串为爬虫stock_list使用的字符串
     * // @key: dateStr: 含义是为哪一个交易日而生成的计划个股bean?? 爬虫已保证自动也保存为次日数据; 因此可正确访问
     * // 因为基本数据需要严谨性, 因此, 若数据访问失败, 并不会访问最新数据作为后备;
     *
     * @param code
     * @param dateStr
     * @param industryConceptList 需要提供当前最新的, 为同一交易日操盘计划的, 最新的行业概念bean 列表; 调用方自行实现列表获取逻辑
     * @return
     * @noti : 若初始化单个bean, 建议无需提供 industryConceptList, 或者提供缓存;
     * 后期自行调用 initRelatedTrendMaps/initLineTypeMapAndLeaderStockOf 方法
     */
    public static StockOfPlan newInstance(String code, String dateStr,
                                          List<IndustryConceptThsOfPlan> industryConceptList) throws Exception {
        StockOfPlan bean = new StockOfPlan();
        DataFrame<Object> dfTemp = ThsDbApi.getStockByCodeAndDate(code, dateStr);
        if (dfTemp == null || dfTemp.length() == 0) {
            throw new Exception(StrUtil.format("创建StockOfPlan失败, 获取基本数据失败: {} {}", code, dateStr));
        }
        int lastRow = dfTemp.length() - 1;
        // 基本信息字段
        bean.setCode(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "code")));
        bean.setMarketCode(tryParseIntegerOfLastLine(dfTemp, "marketCode"));
        bean.setStockCode(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "stockCode")));
        bean.setName(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "name")));
        bean.setConcepts(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "concepts")));
        bean.setConceptAmount(tryParseIntegerOfLastLine(dfTemp, "conceptAmount"));
        bean.setIndustries(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "industries")));
        bean.setDateStr(dateStr);
        // 基本数据字段
        bean.setMarketValue(tryParseDoubleOfLastLine(dfTemp, "marketValue"));
        bean.setCirculatingMarketValue(tryParseDoubleOfLastLine(dfTemp, "circulatingMarketValue"));
        bean.setPe(tryParseDoubleOfLastLine(dfTemp, "pe"));
        bean.setChgP(tryParseDoubleOfLastLine(dfTemp, "chgP"));
        bean.setOpen(tryParseDoubleOfLastLine(dfTemp, "open"));
        bean.setHigh(tryParseDoubleOfLastLine(dfTemp, "high"));
        bean.setLow(tryParseDoubleOfLastLine(dfTemp, "low"));
        bean.setClose(tryParseDoubleOfLastLine(dfTemp, "close"));
        bean.setTurnover(tryParseDoubleOfLastLine(dfTemp, "turnover"));
        bean.setAmplitude(tryParseDoubleOfLastLine(dfTemp, "amplitude"));
        bean.setVolRate(tryParseDoubleOfLastLine(dfTemp, "volRate"));

        // 涨停相关字段
        bean.setHighLimitType(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitType")));
        bean.setHighLimitReason(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitReason")));
        bean.setHighLimitBlockadeAmount(tryParseDoubleOfLastLine(dfTemp, "highLimitBlockadeAmount"));
        bean.setHighLimitBlockadeCMVRate(tryParseDoubleOfLastLine(dfTemp, "highLimitBlockadeCMVRate"));
        bean.setHighLimitBlockadeVolumeRate(tryParseDoubleOfLastLine(dfTemp, "highLimitBlockadeVolumeRate"));
        bean.setHighLimitFirstTime(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitFirstTime")));
        bean.setHighLimitLastTime(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitLastTime")));
        bean.setHighLimitAmountType(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitAmountType")));
        bean.setHighLimitBrokeTimes(tryParseIntegerOfLastLine(dfTemp, "highLimitBrokeTimes"));
        bean.setHighLimitContinuousDays(tryParseIntegerOfLastLine(dfTemp, "highLimitContinuousDays"));
        bean.setHighLimitDetail(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "highLimitDetail")));

        // 跌停相关字段
        bean.setLowLimitType(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "lowLimitType")));
        bean.setLowLimitReason(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "lowLimitReason")));
        bean.setLowLimitBlockadeAmount(tryParseDoubleOfLastLine(dfTemp, "lowLimitBlockadeAmount"));
        bean.setLowLimitBlockadeVolCMVRate(tryParseDoubleOfLastLine(dfTemp, "lowLimitBlockadeVolCMVRate"));
        bean.setLowLimitBlockadeVolumeRate(tryParseDoubleOfLastLine(dfTemp, "lowLimitBlockadeVolumeRate"));
        bean.setLowLimitFirstTime(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "lowLimitFirstTime")));
        bean.setLowLimitLastTime(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "lowLimitLastTime")));
        bean.setLowLimitBrokeTimes(tryParseIntegerOfLastLine(dfTemp, "lowLimitBrokeTimes"));
        bean.setLowLimitContinuousDays(tryParseIntegerOfLastLine(dfTemp, "lowLimitContinuousDays"));
        bean.setLowLimitDetails(CommonUtil.toStringOrNull(dfTemp.get(lastRow, "lowLimitDetails")));

        bean.setGeneratedTime(DateUtil.date()); // 生成时间

        // 其他自动初始化! 应当设定为可强制刷新!
        bean.initIndustryConceptRelatedAttrs(industryConceptList);
        return bean;
    }

    /**
     * 当概念行业更新, 则个股这些字段应当使用最新数据, 重新自动计算!
     *
     * @param industryConceptList
     */
    public void initIndustryConceptRelatedAttrs(List<IndustryConceptThsOfPlan> industryConceptList) {
        this.initRelatedTrendMaps(industryConceptList);
        this.initLineTypeMapAndLeaderStockOf(industryConceptList);
    }


    /**
     * 设置底部大约价格, 前复权; 与此同时, 将自动计算 底部到当前涨跌幅, 使用 close 属性!
     *
     * @param bottomPriceApproximately
     */
    public void setBottomPriceApproximately(double bottomPriceApproximately) {
        this.bottomPriceApproximately = bottomPriceApproximately;
        if (this.close == null || bottomPriceApproximately == 0.0) {
            return;
        }
        this.chgPFromBottom = CommonUtil.roundHalfUP(this.close / bottomPriceApproximately - 1, 3);
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
    String dateStr; // 视为对哪一个交易日做计划?

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
    String highLimitReason; // 涨停原因
    @Column(name = "highLimitBlockadeAmount")
    Double highLimitBlockadeAmount; // 封单额
    @Column(name = "highLimitBlockadeCMVRate")
    Double highLimitBlockadeCMVRate; // 封单市值比
    @Column(name = "highLimitBlockadeVolumeRate")
    Double highLimitBlockadeVolumeRate; // 封成比  封单量/成交量
    @Column(name = "highLimitFirstTime", columnDefinition = "varchar(32)")
    String highLimitFirstTime; // 最早涨停时间
    @Column(name = "highLimitLastTime", columnDefinition = "varchar(32)")
    String highLimitLastTime; // 最晚涨停时间
    @Column(name = "highLimitAmountType", columnDefinition = "longtext")
    String highLimitAmountType; // 几天几板? 跌停没有 *******
    @Column(name = "highLimitBrokeTimes", columnDefinition = "int")
    Integer highLimitBrokeTimes; // 炸板次数
    @Column(name = "highLimitContinuousDays", columnDefinition = "int")
    Integer highLimitContinuousDays; // 几连涨停?
    @Column(name = "highLimitDetail", columnDefinition = "longtext")
    String highLimitDetail; // 涨停详细数据


    // 跌停相关字段
    @Column(name = "lowLimitType", columnDefinition = "varchar(32)")
    String lowLimitType; // 跌停类型, 例如"放量涨停"
    @Column(name = "lowLimitReason", columnDefinition = "longtext")
    String lowLimitReason; // 跌停原因
    @Column(name = "lowLimitBlockadeAmount")
    Double lowLimitBlockadeAmount; // 封单额
    @Column(name = "lowLimitBlockadeVolCMVRate")
    Double lowLimitBlockadeVolCMVRate; // 封单市值比
    @Column(name = "lowLimitBlockadeVolumeRate")
    Double lowLimitBlockadeVolumeRate; // 封成比
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

    // 1.1.@update: 增加该股票 "被选择理由" "选股方法" -- 即 怎么选择得到的该个股 相关字段
    @Transient
    HashMap<String, JSONObject> selectorMap = new HashMap<>();// 选股器Map; key为选股方法名称, value是 json对象表示的, 选股方法对该股的选择情况; 使用json提供很好的灵活性
    @Column(name = "selectorMap", columnDefinition = "longtext")
    String selectorMapJsonStr = "{}";
    @Transient
    List<StockSelector> selectors = new ArrayList<>(); // 维护相关选股器指针; 在 addSelectorRes时添加; 不被序列化

    // 2.个股所属的 行业/概念, 若存在行业概念bean, 则收集对应的 trend 属性, 作为Map, 并且以平均值和标准差衡量 关联行业概念 trend
    @Transient
    HashMap<String, Double> relatedTrendMap = new HashMap<>(); // 关联概念trend字典
    @Column(name = "relatedTrendMap", columnDefinition = "longtext")
    String relatedTrendMapJsonStr = "{}";// key为 名称__行业 或者 名称__概念; 注意split
    @Column(name = "relatedTrendsAvg")
    Double relatedTrendsAvg = 0.0; // 关联 概念trend 平均值
    @Column(name = "relatedTrendsStd")
    Double relatedTrendsStd = 0.0; // 关联概念trend 标准差; 两者简单衡量个股所属行业概念 trend 总体状况;
    // 2.2. 关联trend2, 计算 所属概念/行业的 relatedTrendsDiscount, 它衡量了 所属概念的 相关概念的影响
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
    Double bottomPriceApproximately;  // 底部价格大约! 可自动计算当前从底部至今的涨幅, 手动设定, 常态前复权
    @Column(name = "chgPFromBottom")
    Double chgPFromBottom;  // 底部至今涨幅, 自动计算! ---> @auto
    @Column(name = "pricePositionShortTerm", columnDefinition = "varchar(32)")
    String pricePositionShortTerm = PricePosition.UNKNOWN; // 当前价格大概位置描述; 短期位置;
    @Column(name = "priceTrend", columnDefinition = "varchar(32)")
    String priceTrend = PriceTrend.UNKNOWN; // 价格趋势状态: 横盘/快升/快降/慢升/慢降
    @Column(name = "oscillationAmplitude", columnDefinition = "varchar(32)")
    String oscillationAmplitude = OscillationAmplitude.UNKNOWN; // 振荡幅度: 小/中/大
    @Column(name = "klineDescription", columnDefinition = "longtext")
    String klineDescription = ""; // k线描述
    @Column(name = "fsDescription", columnDefinition = "longtext")
    String fsDescription = ""; // 分时图描述

    // 3.2. 炒作相关 -- 核心
    // 3.2.1: 主线支线 -- 自动设定  @auto
    @Transient
    HashMap<String, ArrayList<String>> lineTypeMap = new HashMap<>(); // 主线支线Map, 将自动收集并设定!
    @Column(name = "lineTypeMap", columnDefinition = "longtext")
    String lineTypeMapJsonStr = "{}"; // key: 主线1/其他; value: 行业/概念列表, 形如 电力_行业
    @Column(name = "lineTypeAmount", columnDefinition = "int")
    Integer lineTypeAmount = 0; // 读取lineTypeMap的 value, 自动计算所有 主线支线  的概念行业数量, 包含未知线
    @Transient
    List<String> leaderStockOfList = new ArrayList<>(); // 是哪些行业/概念的龙头股! 元素为行业/概念名称; 自动计算
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
    String maxRiskPoint = ""; // 最大风险描述, 红色
    @Column(name = "warnings", columnDefinition = "longtext")
    String warnings; // 其他注意点

    // 3.2.3: 核心计划字段
    // 1. 炒作评分
    @Column(name = "hypeLogicStrengthScore")
    Double hypeLogicStrengthScore = 0.0; // @key1: 炒作逻辑强度, 整体评价对该个股炒作未来的预期, 常态 -1.0 -  1.0
    @Column(name = "hypeContinuousScore")
    Double hypeContinuousScore = 0.5; // @key1: 炒作连续性, 即剩余炒作时间预期 0 - 1.0
    @Column(name = "hypeRiskScore")
    Double hypeRiskScore = 0.5; // @key1: 炒作风险评分, 0 - 1.0;

    // 2. 操作计划参数! -- Plan 核心
    @Column(name = "participatePriority")
    Double participatePriority = 0.0; // @key3: 参与优先级, 直接控制参与意愿, 更大值代表越关注该股票, 参与该股票意愿越强! -1.0-1,负数代表避免
    @Column(name = "mainType", columnDefinition = "varchar(32)")
    String maniType = ManiType.MAINTAIN; // 计划执行操作: 买入/卖出/观察维持; 默认维持
    @Column(name = "maniBuyPercentThreshold")
    Double maniBuyPercentThreshold = 0.0; // @key3: 假设买入, 在下一交易日的该涨跌幅之下, 执行买入! 取值 -1.0 - 1.0; 实际取值-0.1-0.1
    @Column(name = "buyPositionPercent")
    Double buyPositionPercent = 0.2; // @key3: 假设买入, 仓位大约值; 0-1.0; 默认20%轻仓; 占总资产仓位
    @Column(name = "maniSellPercentThreshold")
    Double maniSellPercentThreshold = 0.01; // @key3: 假设卖出, 在下一交易日的该涨跌幅之上, 执行卖出! 取值 -1.0 - 1.0; 实际取值-0.1-0.1
    @Column(name = "sellPositionPercent")
    Double sellPositionPercent = 1.0; // @key3: 假设卖出, 卖出仓位占当前持仓百分比(非总资产百分比); 0-1.0; 默认全仓卖出
    @Column(name = "stopLossPercent")
    Double stopLossPercent = -0.08; // @key2: 强制止损点; -1.0-1.0; 实际涨跌停限制; 默认 -8% 强制止损
    @Column(name = "stopProfitPercent")
    Double stopProfitPercent = 1.0; // @key2: 强制止赢点; -1.0-1.0; 实际涨跌停限制; 默认 涨停不止盈! 1.0不可能达到

    /*
    总体评价字段
     */
    @Column(name = "trend")
    Double trend = 0.0; // -1.0 - 1.0 个股trend代表对个股总体评价,
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

    /*
    自动计算属性 方法
     */

    /**
     * 初始化 相关行业/概念 两个trendMap, 及相关字段; 自动计算
     * 自身不控制是否访问数据库最新的 行业概念bean; 由调用方自行提供! 只负责自动计算
     *
     * @param industryConceptList
     */
    private void initRelatedTrendMaps(List<IndustryConceptThsOfPlan> industryConceptList) {
        if (industryConceptList == null || industryConceptList.size() == 0) {
            return;
        }

        HashMap<String, Double> map1 = new HashMap<>();
        HashMap<String, Double> map2 = new HashMap<>();
        for (IndustryConceptThsOfPlan industryConceptThsOfPlan : industryConceptList) {
            if (this.code == null) {
                return;
            }
            List<ThsDbApi.ThsSimpleStock> includeStockList = industryConceptThsOfPlan.getIncludeStockList();

            Set<String> codeSet =
                    includeStockList.stream().map(ThsDbApi.ThsSimpleStock::getCode).collect(Collectors.toSet());
            if (codeSet.contains(this.code)) { // 自身是当前遍历行业的 成分股, 因此收集它的 trend和关联trend

                String key = StrUtil.format("{}__{}", industryConceptThsOfPlan.getName(),
                        industryConceptThsOfPlan.getType());
                map1.put(key, industryConceptThsOfPlan.getTrend());
                map2.put(key, industryConceptThsOfPlan.getRelatedTrendsDiscount());
            }
        }

        this.relatedTrendMap = map1;
        this.relatedTrendMap2 = map2;
        this.relatedTrendMapJsonStr = JSONUtilS.toJsonPrettyStr(relatedTrendMap);
        this.relatedTrendMap2JsonStr = JSONUtilS.toJsonPrettyStr(relatedTrendMap2);
        this.relatedTrendsAvg = CommonUtil.avgOfListNumberUseLoop(relatedTrendMap.values());
        this.relatedTrendsAvg2 = CommonUtil.avgOfListNumberUseLoop(relatedTrendMap2.values());
        this.relatedTrendsStd = CommonUtil.stdOfListNumberUseLoop(relatedTrendMap2.values(), relatedTrendsAvg);
        this.relatedTrendsStd2 = CommonUtil.stdOfListNumberUseLoop(relatedTrendMap2.values(), relatedTrendsAvg);
    }

    /**
     * 同上, 给定最新的 行业概念bean列表, 自动计算 当前个股所属 主线支线, 以及是谁的龙头股, 相关属性
     */
    private void initLineTypeMapAndLeaderStockOf(List<IndustryConceptThsOfPlan> industryConceptList) {
        if (industryConceptList == null || industryConceptList.size() == 0) {
            return;
        }

        HashMap<String, ArrayList<String>> lineTypeMap = new HashMap<>();
        List<String> leaderStockOfList = new ArrayList<>(); // 新对象, 而不直接add, 保证数据最新

        for (IndustryConceptThsOfPlan industryConceptThsOfPlan : industryConceptList) {
            List<ThsDbApi.ThsSimpleStock> includeStockList = industryConceptThsOfPlan.getIncludeStockList();

            Set<String> codeSet =
                    includeStockList.stream().map(ThsDbApi.ThsSimpleStock::getCode).collect(Collectors.toSet());
            String industryConceptStr = StrUtil.format("{}__{}", industryConceptThsOfPlan.getName(),
                    industryConceptThsOfPlan.getType());
            if (codeSet.contains(this.code)) { // 自身是当前遍历行业的 成分股, 因此收集它的 trend和关联trend

                lineTypeMap.putIfAbsent(industryConceptThsOfPlan.getLineType(), new ArrayList<>());
                lineTypeMap.get(industryConceptThsOfPlan.getLineType()).add(industryConceptStr);
            }

            List<ThsDbApi.ThsSimpleStock> leaderStockList = industryConceptThsOfPlan.getLeaderStockList();
            Set<String> codeSet2 =
                    leaderStockList.stream().map(ThsDbApi.ThsSimpleStock::getCode).collect(Collectors.toSet());
            if (codeSet2.contains(this.code)) { // 我是他龙头股
                leaderStockOfList.add(industryConceptStr); // 添加到所属龙头股列表
            }

        }

        this.lineTypeMap = lineTypeMap;
        this.lineTypeMapJsonStr = JSONUtilS.toJsonPrettyStr(lineTypeMap);
        this.lineTypeAmount = lineTypeMap.keySet().size();
        this.leaderStockOfList = leaderStockOfList;
        this.leaderStockOfListJsonStr = JSONUtilS.toJsonPrettyStr(leaderStockOfList);
    }


    /**
     * 从数据表获取bean时, 需要自动填充 transient 字段: 当前 5
     * 它无视你是否再最新刷新相关字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        this.initRelatedTrendMapsWhenBeanFromDb();
        this.initLeaderStockOfListWhenBeanFromDb();
        this.initLineTypeMapWhenBeanFromDb();
        this.initSelectorMapWhenBeanFromDb();
    }

    private void initRelatedTrendMapsWhenBeanFromDb() {
        JSONObject objects = JSONUtilS.parseObj(this.relatedTrendMapJsonStr);
        HashMap<String, Double> map = new HashMap<>();
        for (String s : objects.keySet()) {
            map.put(s, objects.getDouble(s));
        }
        this.relatedTrendMap = map; // 一次性更新

        JSONObject objects2 = JSONUtilS.parseObj(this.relatedTrendMap2JsonStr);
        HashMap<String, Double> map2 = new HashMap<>();
        for (String s : objects2.keySet()) {
            map2.put(s, objects2.getDouble(s));
        }
        this.relatedTrendMap2 = map2; // 一次性更新
    }

    private void initLeaderStockOfListWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.leaderStockOfListJsonStr);
        List<String> stringList = new ArrayList<>();
        for (Object object : objects) {
            stringList.add(object.toString());
        }
        this.leaderStockOfList = stringList;
    }

    private void initLineTypeMapWhenBeanFromDb() {
        JSONObject objects = JSONUtilS.parseObj(this.lineTypeMapJsonStr);
        HashMap<String, ArrayList<String>> res = new HashMap<>();
        for (String key : objects.keySet()) {
            ArrayList<String> lineTypes = new ArrayList<>();
            JSONArray jsonArray = objects.getJSONArray(key);
            for (Object o : jsonArray) {
                lineTypes.add(o.toString());
            }

            res.put(key, lineTypes);
        }
        this.lineTypeMap = res;
    }

    private void initSelectorMapWhenBeanFromDb() {
        JSONObject objects = JSONUtilS.parseObj(this.selectorMapJsonStr);
        HashMap<String, JSONObject> res = new HashMap<>();
        for (String key : objects.keySet()) {
            res.put(key, objects.getJSONObject(key));
        }
        this.selectorMap = res;
    }


    /*
    数据api, 主要同时更新 list和对应的json; 按需实现
     */

    /**
     * 添加选股结果
     *
     * @param selector
     */
    public void addSelectorRes(StockSelector selector) {
        this.selectorMap.put(selector.getName(), selector.getSelectResultOf(this.code));
        selectors.add(selector);
        this.selectorMapJsonStr = JSONUtilS.toJsonPrettyStr(selectorMap);
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

        public static String UNKNOWN = "未知";

        public static Vector<String> allPricePositions = new Vector<>(Arrays.asList(
                HIGH_POSITION,
                MEDIUM_HIGH_POSITION,
                LOW_POSITION,
                MEDIUM_LOW_POSITION,
                MEDIUM_POSITION,
                UNKNOWN
        ));
    }

    /**
     * 操作类型: 买/卖/观察
     */
    public static class ManiType {
        public static String BUY = "买入";
        public static String SELL = "卖出";
        public static String OBSERVE = "观察";
        public static String MAINTAIN = "维持";
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


}
