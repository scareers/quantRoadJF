package com.scareers.gui.ths.simulation.strategy.stockselector;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SettingsOfEastMoney;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.KlineFormsApi;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getRealtimeQuotes;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.*;
import static com.scareers.utils.CommonUtil.subtractionOfList;
import static com.scareers.utils.CommonUtil.sumEqualApproximately;
import static com.scareers.utils.SqlUtil.execSql;


/**
 * description: 基于原始研究结果的低买高卖 选股方法
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
@Getter
@Setter
public class LbHsSelector implements StockSelect {
    // mysql连接
    public static Connection connOfStockSelectResult = ConnectionFactory.getConnLocalKlineForms();
    private static final Log log = LogUtil.getLogger();

    // -------->  静态属性, 设置项
    public static int stockSelectedExecAmounts = 100000; // 选股遍历股票数量, 方便debug, 设置为很大然后无视
    // 当今日选股结果记录数量>此值,视为已执行选股.今日不再执行, 当然也可手动强制执行全量选股
    public static long hasStockSelectResultTodayThreshold = 1000; // 常态要么 0(无结果), 要么 3200+ 所有主板执行过选股
    public static List<String> marketListSelect = Arrays.asList("沪深主板"); // 在那些市场选股?, 参考东财api
    // 用于适配东财的k线api, 与 tushare的k线数据表. 因调用相关函数使用研究时函数, 需要表头适配 dfRaw.rename() 改列名
    public static Map<String, Object> fieldsRenameDict = Dict.create().set("日期", "trade_date").set("开盘", "open")
            .set("收盘", "close")
            .set("最高", "high").set("最低", "low").set("成交量", "vol").set("成交额", "amount");
    public static String sqlCreateStockSelectResultSaveTableTemplate = "create table if not exists " +
            "`{}`\n" +
            "(\n" +
            "    id           int auto_increment comment 'id'\n" +
            "        primary key,\n" +
            "    trade_date   varchar(1024) null comment 'today: 选股日期',\n" +
            "    ts_code      varchar(1024) null comment '某只股票',\n" +
            "    form_set_ids longtext      null comment '该股票,该日, 所属的形态集合, 即被那些形态集合选中. json字符串Long列表',\n" +
            "    self_notes   varchar(2048) null comment '其他备注',\n" +
            "\n" +
            "    INDEX trade_date_index (trade_date ASC),\n" +
            "    INDEX ts_code_index (ts_code ASC)\n" +
            ")\n" +
            "    comment '选股结果: 日期-股票-所属形态集合id列表';\n";


    // -------->  构造器所需传递属性
    // 手动额外强制不选中的股票列表. 仅简单排除股票池, 不对其他任何逻辑造成影响, 取前6位为代码
    private List<String> forceManualExcludeStocks; // 需要设置
    private int suitableSelectStockCount;  // @key5: 建议最终选股结果的数量, 将自动调控 profit 参数放松放宽筛选条件
    // @key5: 筛选formset的 profit 阈值>=, 设置初始值后,将自动适配, 以使得选股数量接近  suitableSelectStockCount
    // 当找到合适的参数, 是否偏好更多选股结果? 若是, 则最终选股>=suitableSelectStockCount,
    // 否则, 比较两个距离的大小, 选择更加接近的一方.  即可能不论true或者false, 选股结果相同.
    private boolean preferenceMoreStock = false;
    private List<Integer> keyInts; // 核心设定  0,1  必须此设定, 低买高卖形式,0,1表示次日买后日卖

    // -------->  直接赋予默认值属性, 一般无需修改
    private double profitLimitOfFormSetIdFilter = 0.013; // 将自动适配, 此处设置默认值作为适配起点. @key: 适配后, 核心属性
    private double profitAdjustTick = 0.00025; // 自动适配时, profit参数 往大/小 调整的数量, 这里 0.025%. 越小计算量越大越精细

    // -------->  init 自动计算初始化属性
    private List<Long> useFormSetIds;  // @key5: 策略使用到的 集合池, 其分布将依据选股结果进行加权!!
    private HashMap<Long, Double> formSetDistributionWeightMapFinal; // 最终选股结果后, formSet分布权重 Map
    private HashMap<String, Integer> stockSelectCountMapFinal; // 选股结果, value是出现次数

    //  四项, 分布和tick
    //  计算cdf 见 virtualCdfAsPositionForHighSell / virtualCdfAsPositionForLowBuy 静态方法 keyfuncs.positiondecision
    private List<Double> ticksOfLow1GlobalFinal = null; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
    private List<Double> weightsOfLow1GlobalFinal = null; // 44数据
    private List<Double> ticksOfHigh1GlobalFinal = null; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    private List<Double> weightsOfHigh1GlobalFinal = null; // 88数据

    private String stockSelectResultSaveTableName; // stock_select_result_of_lbhs_trader_{}b{}s 结果保存表
    private List<String> stockSelectedToday;

    public LbHsSelector(List<String> forceManualExcludeStocks, // 需要设置手动排除的股票.
                        int suitableSelectStockCount, // 期望的选股结果数量
                        boolean preferenceMoreStock, // 更喜欢更多的股票选择结果
                        List<Integer> keyInts// 核心设定, 0,1表示次日买后日卖, 以此类推. 影响选股结果保存表结果
    ) throws Exception {
        this.forceManualExcludeStocks = forceManualExcludeStocks;
        this.suitableSelectStockCount = suitableSelectStockCount;
        this.preferenceMoreStock = preferenceMoreStock;
        this.keyInts = keyInts;
        this.stockSelectResultSaveTableName = StrUtil.format("stock_select_result_of_lbhs_trader_{}b{}s",
                keyInts.get(0), keyInts.get(1)); // 立即初始化表明保存结果
        selectStock(); // 构建器自动初始化股票池!
    }

    @Override
    public List<String> getSelectResults() throws Exception {
        if (stockSelectedToday == null) {
            selectStock();
        }
        return stockSelectedToday;
    }

    @Override
    public void selectStock() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        select(); // 选股for buy

        // 两大属性已经初始化(即使空): formSetDistributionWeightMapFinal ,stockSelectCountMapFinal
        log.warn("stock select result: 选股结果: \n------->\n{}\n", stockSelectCountMapFinal.keySet());
        log.warn("stock select result: 选股数量: {}", stockSelectCountMapFinal.size());
        log.warn("stock select result: 自适应选股参数: profitLimitOfFormSetIdFilter {}", profitLimitOfFormSetIdFilter);

        // 需要再初始化 formSetId 综合分布! 依据 formSetDistributionWeightMapFinal 权重map!.
        initFinalDistributionPdf(); // 计算等价分布,
        log.warn("finish calc distribution: 完成计算全局加权低买高卖双分布");
        stockSelectedToday = new ArrayList<>(stockSelectCountMapFinal.keySet());
    }

    /**
     * 选股逻辑, 注意此处并未使用返回值, 将选股结果存储于属性
     *
     * @return
     * @throws Exception
     */
    protected List<String> select() throws Exception {
        execSql(StrUtil.format(sqlCreateStockSelectResultSaveTableTemplate, stockSelectResultSaveTableName),
                connOfStockSelectResult); // 不存在则建表, 保存选股结果
        String today = DateUtil.today();
        String sqlIsStockSelectedToday = StrUtil.format("select count(*) from `{}` where trade_date='{}'",
                stockSelectResultSaveTableName, today); // 选股结果表保存针对单只股票, 那些 form_set_ids 选中了它?
        DataFrame<Object> dfTemp = DataFrame.readSql(connOfStockSelectResult, sqlIsStockSelectedToday);
        long resultCountOfToday = Long.valueOf(dfTemp.get(0, 0).toString()); // 曾选股结果
        if (resultCountOfToday <= hasStockSelectResultTodayThreshold) { // 全量更新今日全部选股结果
            stockSelect0(); // 真实今日选股并存入数据库, 需要从各大分析研究程序调用对应函数
        }

        // 获取选股结果, api 均已缓存
        // List<String> getStockSelectResultOfTradeDate  获取单form_set 某日期 选股结果列表
        HashMap<Long, List<String>> stockSelectResult = KlineFormsApi.getStockSelectResultOfTradeDate(today, keyInts,
                stockSelectResultSaveTableName);
        HashMap<Long, HashSet<String>> stockSelectResultAsSet = new HashMap<>();
        stockSelectResult.keySet().stream().forEach(value -> stockSelectResultAsSet.put(value,
                new HashSet<>(stockSelectResult.get(value)))); // 转换为hashset更快

        // 自动调整参数 profitLimitOfFormSetIdFilter, 使得选股数量接近 suitableSelectStockCount
        decideTheMostSuitableFormSetIds(stockSelectResultAsSet);
        // 此时参数已调整好, 最后获取最终确定的 选股结果!!
        confirmStockSelectResult(stockSelectResultAsSet);
        // 确定后两大静态属性已经初始化(即使空): formSerDistributionWeightMapFinal ,stockSelectCountMapFinal
        return null;
    }

    /**
     * 更新全局加权  分布四项列表
     * ticksOfLow1GlobalFinal = ticksOfLow1Global; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
     * weightsOfLow1GlobalFinal = weightsOfLow1Global; // 44数据
     * ticksOfHigh1GlobalFinal = ticksOfHigh1Global; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
     * weightsOfHigh1GlobalFinal = weightsOfHigh1Global; // 88数据
     *
     * @throws Exception
     * @noti: 调用方保证, 所有 formSet 的 tick和weight 列表size相同!
     */
    private void initFinalDistributionPdf() throws Exception {
        List<Double> ticksOfLow1Global = null; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
        List<Double> weightsOfLow1Global = null; // 44数据
        List<Double> ticksOfHigh1Global = null; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
        List<Double> weightsOfHigh1Global = null; // 88数据

        for (Long formSetId : formSetDistributionWeightMapFinal.keySet()) {
            List<List<Double>> distributionSingle =
                    KlineFormsApi.getLowBuyAndHighSellDistributionByFomsetid(formSetId, keyInts);
            if (ticksOfLow1Global == null) {
                ticksOfLow1Global = distributionSingle.get(0);
            }
            if (ticksOfHigh1Global == null) {
                ticksOfHigh1Global = distributionSingle.get(2);
            }
            Assert.isTrue(ticksOfLow1Global.size() == distributionSingle.get(0).size());
            Assert.isTrue(ticksOfHigh1Global.size() == distributionSingle.get(2).size());

            double weight = formSetDistributionWeightMapFinal.get(formSetId);
            // 权重列表 进行再次加权!
            List<Double> weightsTwiceOfLow1 =
                    distributionSingle.get(1).stream().map(value -> value * weight)
                            .collect(Collectors.toList());
            List<Double> weightsTwiceOfHigh1 =
                    distributionSingle.get(3).stream().map(value -> value * weight)
                            .collect(Collectors.toList());
            // 加总到最终权重列表!
            if (weightsOfLow1Global == null) {
                weightsOfLow1Global = weightsTwiceOfLow1;
            }
            if (weightsOfHigh1Global == null) {
                weightsOfHigh1Global = weightsTwiceOfHigh1;
                continue; // 首次不需要叠加
            }

            Assert.isTrue(weightsOfLow1Global.size() == weightsTwiceOfLow1.size());
            Assert.isTrue(weightsOfHigh1Global.size() == weightsTwiceOfHigh1.size());

            // 权重叠加
            List<Double> weightsOfLow1GlobalTemp = new ArrayList<>();
            for (int i = 0; i < weightsOfLow1Global.size(); i++) {
                weightsOfLow1GlobalTemp.add(weightsOfLow1Global.get(i) + weightsTwiceOfLow1.get(i));
            }
            weightsOfLow1Global = weightsOfLow1GlobalTemp;

            List<Double> weightsOfHigh1GlobalTemp = new ArrayList<>();
            for (int i = 0; i < weightsOfHigh1Global.size(); i++) {
                weightsOfHigh1GlobalTemp.add(weightsOfHigh1Global.get(i) + weightsTwiceOfHigh1.get(i));
            }
            weightsOfHigh1Global = weightsOfHigh1GlobalTemp;
        }

        // 更新全局分布!!!
        ticksOfLow1GlobalFinal = ticksOfLow1Global; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
        weightsOfLow1GlobalFinal = weightsOfLow1Global; // 44数据
        ticksOfHigh1GlobalFinal = ticksOfHigh1Global; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
        weightsOfHigh1GlobalFinal = weightsOfHigh1Global; // 88数据


        Assert.isTrue(sumEqualApproximately(weightsOfLow1GlobalFinal, 1.0, 0.001));//权重和1
        Assert.isTrue(sumEqualApproximately(weightsOfHigh1GlobalFinal, 1.0, 0.001));//权重和1
        log.info("show: 低买tick: {}", ticksOfLow1GlobalFinal);
        log.info("show: 低买分布: {}", weightsOfLow1GlobalFinal);
        log.info("show: 高卖tick: {}", ticksOfHigh1GlobalFinal);
        log.info("show: 高卖分布: {}", weightsOfHigh1GlobalFinal);
    }


    private void initUseFormSetIds() throws FileNotFoundException {
        // 初始化使用到的 形态集合id列表.
        // 这里提供文件名称, classpath文件. json. 解析到 profit字段最大的一些 formSetId

        String jsonStr = ResourceUtil.readUtf8Str("results/IndexRealTimeRaiseFallParameter/" +
                "singleTableGroupByFormsetidAvg_scale1.41.61.2" +
                ".json");
        JSONObject datas = JSONUtil.parseObj(jsonStr);
        Map<String, Map<String, Double>> gatherMap = new HashMap<>(); // 汇总, key为  数据表$$form_set_id
        for (String tableName : datas.keySet()) {
            JSONObject argGroupData = datas.getJSONObject(tableName);
            for (String forSetIdStr : argGroupData.keySet()) {
                String gatherKey = tableName + "$$" + forSetIdStr;
                HashMap<String, Object> value = new HashMap<>(argGroupData.getJSONObject(forSetIdStr));
                HashMap<String, Double> realValue = new HashMap<>();
                value.keySet().stream().forEach(key -> realValue.put(key, Double.valueOf(value.get(key).toString())));
                gatherMap.put(gatherKey, realValue);
            }
        }

        List<String> formSetsStrs = new ArrayList<>(gatherMap.keySet());
        formSetsStrs =
                formSetsStrs.stream().sorted(Comparator.comparing(value -> (gatherMap.get(value).get("profit"))))
                        .collect(Collectors.toList());// 排序, 依据profit
        Collections.reverse(formSetsStrs); // 倒排


        long gt15Count = formSetsStrs.stream()
                .mapToDouble(value -> Double.valueOf(gatherMap.get(value).get("profit").toString()))
                .filter(value -> value >= profitLimitOfFormSetIdFilter).count();
        formSetsStrs = formSetsStrs.subList(0, (int) gt15Count); // 筛选>= 0.015的
        List<Long> formSetIds =
                formSetsStrs.stream().map(valur -> Long.valueOf(valur.substring(valur.indexOf("$$") + 2)))
                        .distinct().collect(Collectors.toList());
        useFormSetIds = formSetIds;
        log.debug("init useFormSetIds success: 选中形态集合数量: {}", useFormSetIds.size());
    }


    private void decideTheMostSuitableFormSetIds(HashMap<Long, HashSet<String>> stockSelectResultAsSet)
            throws FileNotFoundException {
        // @key:
        // 1.给定形态集合 列表,  各自有对应的选股结果. 首先, 我们依据股票出现次数, 配合股票数量,得到最终整合的选股结果!
        // 2.得到确定的选股结果后, 每只股票出现了多少次? 总和.  formSet的权重, 由 其选股结果中, 被最终选择到了的股票 数量 / 总数
        // 3.即可得到  {formSetId: 策略最终加权形态集合的 加权权重.}
        // 4.后期可依据权重确定 加权概率分布!!!!!!!!!
        // decideTheMostSuitableFormSetIds

        int selectCount = getStockSelectCountCurrentFilterArgOfProfit(stockSelectResultAsSet);
        if (selectCount > suitableSelectStockCount) {
            int selectCountPre = selectCount;
            while (true) {
                profitLimitOfFormSetIdFilter += profitAdjustTick; // 更加严格的调整, 使得选股结果更少!
                int selectCountLess = getStockSelectCountCurrentFilterArgOfProfit(stockSelectResultAsSet);
                if (selectCountPre >= suitableSelectStockCount && selectCountLess < suitableSelectStockCount) {
                    // 此时已达到临界, 再 profitLimitOfFormSetIdFilter 如果选小的,则不调整, 否则, 倒回去
                    if (preferenceMoreStock) { // 偏好更多股票选中?
                        profitLimitOfFormSetIdFilter -= profitAdjustTick; // 回滚1tick参数.
                        break;
                    }

                    if (selectCountPre - suitableSelectStockCount < suitableSelectStockCount - selectCountLess) {
                        // 多的更接近, 则参数回滚,得到更多结果
                        profitLimitOfFormSetIdFilter -= profitAdjustTick; // 回滚1tick参数.
                    }
                    break; // 退出
                } else {
                    // 此时应当继续.
                    selectCountPre = selectCountLess;
                }
            }
        } else if (selectCount < suitableSelectStockCount) { // 往大调整以增加选股数量
            int selectCountPre = selectCount;
            while (true) {
                profitLimitOfFormSetIdFilter -= profitAdjustTick; // 更加宽松的调整, 使得选股结果更多!
                int selectCountMore = getStockSelectCountCurrentFilterArgOfProfit(stockSelectResultAsSet);
                if (selectCountPre < suitableSelectStockCount && selectCountMore >= suitableSelectStockCount) {
                    if (!preferenceMoreStock) { // 偏好更少股票选中?
                        profitLimitOfFormSetIdFilter += profitAdjustTick; // 回滚1tick参数.
                        break;
                    }
                    //此时已达到临界, 再 profitLimitOfFormSetIdFilter 如果选小的, 则不调整, 否则, 倒回去
                    if (suitableSelectStockCount - selectCountPre < selectCountMore - suitableSelectStockCount) {
                        profitLimitOfFormSetIdFilter += profitAdjustTick; // 少的更接近, 则参数回滚
                    }
                    break; // 退出
                } else {
                    // 此时应当继续.
                    selectCountPre = selectCountMore;
                }
            }
        } // 若相等则不调整  profitLimitOfFormSetIdFilter
    }

    private int getStockSelectCountCurrentFilterArgOfProfit(HashMap<Long, HashSet<String>> stockSelectResultAsSet
    )
            throws FileNotFoundException {
        initUseFormSetIds();
        // profitLimitOfFormSetIdFilter 自动调整
        // 初始化使用的形态id列表, 已经去重, 默认实现为筛选  见: profitLimitOfFormSetIdFilter
        // 计算每只股票有多少个formset 选中了?
        HashMap<String, Integer> stockSelectCountMap = new HashMap<>();
        for (Long forSetId : useFormSetIds) {
            HashSet<String> stockSelectedSet = stockSelectResultAsSet.get(forSetId);
            if (stockSelectedSet == null) {
                log.info("formSetId no stockSelectResult: 无选股结果: {}", forSetId);
                continue;
            }
            for (String stock : stockSelectedSet) {
                stockSelectCountMap.putIfAbsent(stock, 0); // 初始0次
                stockSelectCountMap.put(stock, stockSelectCountMap.get(stock) + 1); // 计数+1
            }
        }
        return stockSelectCountMap.size();
    }

    private void confirmStockSelectResult(HashMap<Long, HashSet<String>> stockSelectResultAsSet)
            throws FileNotFoundException {
        initUseFormSetIds(); // profitLimitOfFormSetIdFilter 自动调整
        // 初始化使用的形态id列表, 已经去重, 默认实现为筛选  见: profitLimitOfFormSetIdFilter
        // 计算每只股票有多少个formset 选中了?
        HashMap<String, Integer> stockSelectCountMap = new HashMap<>();
        HashMap<Long, Double> formSerDistributionWeightMap = new HashMap<>();
        for (Long forSetId : useFormSetIds) {
            HashSet<String> stockSelectedSet = stockSelectResultAsSet.get(forSetId);
            if (stockSelectedSet == null) {
                log.info("formSetId no stockSelectResult: 无选股结果: {}", forSetId);
                continue;
            }
            for (String stock : stockSelectedSet) {
                stockSelectCountMap.putIfAbsent(stock, 0); // 初始0次
                stockSelectCountMap.put(stock, stockSelectCountMap.get(stock) + 1); // 计数+1
            }
        }
        // 总次数, 开始计算 formset 分布的权重
        double totalCount = stockSelectCountMap.values().stream().mapToDouble(value -> value.doubleValue()).sum();

        // 分布权重, 可综合所有形态集合的分布,合成总分布
        for (Long forSetId : useFormSetIds) {
            HashSet<String> stockSelectedSet = stockSelectResultAsSet.get(forSetId);
            if (stockSelectedSet == null) {
                formSerDistributionWeightMap.put(forSetId, 0.0); // 无选股结果, 则分布权重 0.0
                continue;
            }
            formSerDistributionWeightMap.put(forSetId, stockSelectedSet.size() / totalCount); // 权重
        }
        // 此时已确定选股结果, 以及今日 formSet 的分布权重

        for (String stockForceExclude : forceManualExcludeStocks) {
            stockSelectCountMap.remove(stockForceExclude.substring(0, 6));
        }

        formSetDistributionWeightMapFinal = formSerDistributionWeightMap;
        stockSelectCountMapFinal = stockSelectCountMap;
        Assert.isTrue(sumEqualApproximately(formSetDistributionWeightMapFinal.values(), 1.0, 0.005));//权重和1

    }


    /**
     * 单日实际选股逻辑,
     *
     * @see com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell
     * :320 行
     */
    private void stockSelect0() throws Exception {
        log.warn("stock select today: 执行今日选股主逻辑. keyInts: {}", keyInts);
        execSql(StrUtil.format(sqlCreateStockSelectResultSaveTableTemplate, stockSelectResultSaveTableName),
                connOfStockSelectResult); // 不存在则建表

        List<String> allStocks = DataFrameS
                .getColAsStringList(getRealtimeQuotes(marketListSelect), SettingsOfEastMoney.STR_SEC_CODE);

        // 开始选股. 首先获取近 几日 window数据, 当然, 这里我们多获取1日, 截取最后 n个.  截取2个月
        String today = DateUtil.today(); // yyyy-MM-dd
        String pre7TradeDate = EmQuoteApi.getPreNTradeDateStrict(today, 7); // 6足够, 冗余1.  // yyyy-MM-dd
        String pre1TradeDate = EmQuoteApi.getPreNTradeDateStrict(today, 1); // 6足够, 冗余1.  // yyyy-MM-dd , 已经缓存
        // 所有主板股票 3000+, 近2个月 日k线, 前复权.  key为stock, value为df
        log.warn("stock amounts: 已获取两市主板股票数量: {}", allStocks.size());
        ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> datasMap0 =
                EmQuoteApi.getQuoteHistoryBatch(
                        SecurityBeanEm.createStockList(new ArrayList<>(allStocks)
                                .subList(0, Math.min(stockSelectedExecAmounts, allStocks.size()))),
                        pre7TradeDate,
                        pre1TradeDate, // @noti: 若使用today, 则盘中选股将出现今日日期结果
                        "101", "1", 3, 2000, false);

        ConcurrentHashMap<String, DataFrame<Object>> datasMap = new ConcurrentHashMap<>();
        for (SecurityBeanEm beanEm : datasMap0.keySet()) {
            datasMap.put(beanEm.getSecCode(), datasMap0.get(beanEm));
        }
        // int windowUsePeriodsCoreArg = keyInts.get(1) + 7; // 等价于原来高卖那一天. 这里8, 理论上, 应当获取最后6日数据, 拼接几行空值
        for (String stock : datasMap.keySet()) {
            DataFrame<Object> dfRaw = datasMap.get(stock); // dfRaw的日期默认为 yyyy-MM-dd, 虽然起止参数是 yyyyMMdd 形式
            dfRaw = dfRaw.rename(new HashMap<>(fieldsRenameDict));
            if (dfRaw.length() < 6) { // 两个月至少需要 6天数据, 有7天数据, 1天冗余. 即7天内不能停牌1天以上
                continue;
            }
            if (!(dfRaw.get(dfRaw.length() - 1, 0).toString().equals(pre1TradeDate))) {
                continue; // 该股票上一交易日, 并非上一个开盘日, 例如停牌的无视
            }
            DataFrame<Object> dfWindow = dfRaw.slice(dfRaw.length() - 6, dfRaw.length());
            dfWindow = dfWindow.resetIndex();
            //for (int i = 0; i <= windowUsePeriodsCoreArg - dfWindow.length(); i++) { // @update: 不需要
            //    dfWindow = dfWindow.append(Arrays.asList()); // 添加几行空值, 模拟未来数据, 以便调用原方法. 无需强行等列的al
            //}
            List<Object> pre5dayKlineRow = dfWindow.row(0);
            List<Object> yesterdayKlineRow = dfWindow.row(4);
            List<Object> todayKlineRow = dfWindow.row(5);
            boolean[] reachPriceLimit = calcReachPriceLimit(todayKlineRow, yesterdayKlineRow, dfWindow);
            List<String> concreteTodayFormStrs = parseConditionsAsStrsSimple(dfWindow, pre5dayKlineRow,
                    yesterdayKlineRow, todayKlineRow, reachPriceLimit);
            List<String> allForms = getAllFormNamesByConcreteFormStrsWithoutSuffix(concreteTodayFormStrs);

            List<Long> belongToFormsetIds = calcBelongToFormSets(formSetsMapFromDBAsHashSet, allForms);
            if (belongToFormsetIds.size() == 0) {
                continue; // 如果id列表空,显然不需要浪费时间计算 15个结果值.
            }
            saveStockSelectResult(stock, today, // 注意这里保存的是 真正的 today 的日期. 10位
                    belongToFormsetIds,
                    stockSelectResultSaveTableName,
                    connOfStockSelectResult);
            log.debug("stock select result saving: {} -- {} ", today, stock);
        }
        log.warn("stock select finish: 完成执行今日选股主逻辑. keyInts: {}", keyInts);
    }

    private boolean[] calcReachPriceLimit(List<Object> todayKlineRow, List<Object> yesterdayKlineRow,
                                          DataFrame<Object> dfWindow) {
        /*
          	trade_date	open	close	high	 low	   vol	      amount	   振幅	  涨跌幅	  涨跌额	 换手率	昨日收盘	  资产代码	资产名称
            0	2021-12-17	4.54	4.46 	4.54	4.45	182884	81887987.00 	2.00 	-0.89	-0.04	1.15	0   	000976	华铁股份
         */

        Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
        Double yesterdayClose = getPriceOfSingleKline(yesterdayKlineRow, "close");
        List<Object> colName = dfWindow.col("资产名称");
        String stockName = colName.get(5).toString();
        Double priceLimit = 0.1; // 全是主板
        if (stockName.contains("ST")) {
            priceLimit = 0.05;
        }
        Double priceLimitMax = NumberUtil.round(yesterdayClose * (1 + priceLimit), 2).doubleValue();
        Double priceLimitMin = NumberUtil.round(yesterdayClose * (1 - priceLimit), 2).doubleValue();

        boolean[] res = {false, false};
        if (priceLimitMax.equals(todayClose)) {
            res[0] = true;
        }
        if (priceLimitMin.equals(todayClose)) {
            res[1] = true;
        }
        return res;
    }


    public void bindSelf() {
        this.trader.setStrategy(this);
    }


}

