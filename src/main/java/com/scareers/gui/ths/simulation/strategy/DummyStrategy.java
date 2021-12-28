package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.sqlapi.KlineFormsApi;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.stock.StockApi.getRealtimeQuotes;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.*;
import static com.scareers.utils.CommonUtils.equalApproximately;
import static com.scareers.utils.CommonUtils.subtractionOfList;
import static com.scareers.utils.SqlUtil.execSql;


/**
 * description: 虚拟的策略, 随机生成订单, 放入队列执行. check 逻辑也相同
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
public class DummyStrategy extends Strategy {
    // 手动额外强制不选中的股票列表. 仅简单排除股票池, 不对其他任何逻辑造成影响, 取前6位为代码
//    public static List<String> forceManualExcludeStocks = Arrays.asList("002028.sz");
    public static List<String> forceManualExcludeStocks = Arrays.asList();
    public static int stockSelectedExecAmounts = 100000; // 选股遍历股票数量, 方便debug
    public static List<Long> useFormSetIds;  // @key5: 策略使用到的 集合池, 其分布将依据选股结果进行加权!!
    // @key5: 筛选formset的 profit阈值>=, 设置初始值后,将自动适配, 以使得选股数量接近  suitableSelectStockCount
    public static double profitLimitOfFormSetIdFilter = 0.013; // 自动适配
    public static double profitAdjustTick = 0.00025; // 自动适配时, profit参数 往大/小 调整的数量
    public static int suitableSelectStockCount = 30;  // @key5: 建议最终选股结果的数量, 将自动调控 profit参数放松放宽筛选条件
    // 当找到合适的参数, 是否偏好更多选股结果? 若是, 则最终选股>=suitableSelectStockCount,
    // 否则, 比较两个距离的大小, 选择更加接近的一方.  即可能不论true或者false, 选股结果相同!!
    public static boolean preferenceMoreStock = false;

    public static HashMap<Long, Double> formSerDistributionWeightMapFinal; // 最终选股结果后, formSet分布权重Map
    public static HashMap<String, Integer> stockSelectCountMapFinal; // 选股结果, value是出现次数
    public static List<Double> ticksOfLow1GlobalFinal = null; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
    public static List<Double> weightsOfLow1GlobalFinal = null; // 44数据
    public static List<Double> ticksOfHigh1GlobalFinal = null; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    public static List<Double> weightsOfHigh1GlobalFinal = null; // 88数据

    // 当今日选股结果记录数量>此值,视为已执行选股.今日不再执行, 当然也可手动强制执行全量选股
    public static long hasStockSelectResultTodayThreshold = 1000;
    public static String SIMPLE_DATE_FORMAT = "yyyyMMdd";
    public static final List<Integer> keyInts = Arrays.asList(0, 1); // 核心设定  0,1  必须此设定
    public static String stockSelectResultSaveTableName = StrUtil.format("stock_select_result_of_lbhs_trader_{}b{}s",
            keyInts.get(0), keyInts.get(1));
    public static Connection connOfStockSelectResult = ConnectionFactory.getConnLocalKlineForms();
    public static final List<String> fieldsOfDfRaw = Arrays
            // @update: 新增了 amount列, 对主程序没有影响, 但是在 lbhs时, 可以读取到 amount 列, 成交额比成交量方便计算百分比
            .asList("trade_date", "open", "close", "high", "low", "vol", "amount"); // 股票日k线数据列
    // 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	  振幅	  涨跌幅	  涨跌额	 换手率	  股票代码	股票名称
    public static Map<String, Object> fieldsRenameDict = Dict.create().set("日期", "trade_date").set("开盘", "open")
            .set("收盘", "close")
            .set("最高", "high").set("最低", "low").set("成交量", "vol").set("成交额", "amount");


    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws Exception {
        new DummyStrategy("xx");
    }

    @Override
    protected void checkBuyOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected void checkSellOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected void checkOtherOrder(Order order, List<JSONObject> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toStringPretty());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行失败");
        }
        Trader.successFinishOrder(order, responses);
    }

    @Override
    protected void startCore() throws Exception {
        while (true) {
            int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
            Thread.sleep(sleep * 1000);
            Order order = null;
            int type = RandomUtil.randomInt(12);
            if (type < 3) {
                order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGHEST);
            } else if (type < 6) {
                order = OrderFactory.generateSellOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGH);
            } else if (type < 8) {
                order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_MEDIUM);
            } else if (type < 10) {
                order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_LOWEST);
            } else {
                order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
            }
            Trader.putOrderToWaitExecute(order);
        }
    }


    @Override
    protected List<StockBean> initStockPool() throws Exception {
        log.warn("start init stockPool: 开始尝试初始化股票池...");
        // 两大静态属性已经初始化(即使空): formSerDistributionWeightMapFinal ,stockSelectCountMapFinal
        stockSelect();
        log.warn("stock select result: 最终选股结果: \n------->\n{}\n", stockSelectCountMapFinal.keySet());
        log.warn("stock select result: 最终选股数量: {}", stockSelectCountMapFinal.size());

        // 需要再初始化 formSetId 综合分布! 依据 formSerDistributionWeightMapFinal 权重map!.
        initFinalDistribution(); // 计算等价分布
        log.warn("finish calc distribution: 完成计算全局加权低买高卖双分布");
        List<StockBean> res = StockPoolForFSTransaction // 已经加入两大指数, 构建股票池. Bean
                .stockListFromSimpleStockList(new ArrayList<>(stockSelectCountMapFinal.keySet()));
        log.warn("finish init stockPool: 完成初始化股票池...");
        return res;
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
    private void initFinalDistribution() throws Exception {
        List<Double> ticksOfLow1Global = null; // [0.11, 0.105, 0.1, 0.095, 0.09, 0.085, 0.08, 0.075, ...
        List<Double> weightsOfLow1Global = null; // 44数据
        List<Double> ticksOfHigh1Global = null; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
        List<Double> weightsOfHigh1Global = null; // 88数据

        for (Long formSetId : formSerDistributionWeightMapFinal.keySet()) {
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

            double weight = formSerDistributionWeightMapFinal.get(formSetId);
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

        Assert.isTrue(equalApproximately(weightsOfLow1GlobalFinal, 1.0, 0.001));//权重和1
        Assert.isTrue(equalApproximately(weightsOfHigh1GlobalFinal, 1.0, 0.001));//权重和1
        log.info("show: 低买tick: {}", ticksOfLow1GlobalFinal);
        log.info("show: 低买分布: {}", weightsOfLow1GlobalFinal);
        log.info("show: 高卖tick: {}", ticksOfHigh1GlobalFinal);
        log.info("show: 高卖分布: {}", weightsOfHigh1GlobalFinal);
    }


    @Override
    protected List<String> stockSelect() throws Exception {
        execSql(StrUtil.format(sqlCreateStockSelectResultSaveTableTemplate, stockSelectResultSaveTableName),
                connOfStockSelectResult); // 不存在则建表
        String today = DateUtil.today();
        String sqlIsStockSelectedToday = StrUtil.format("select count(*) from `{}` where trade_date='{}'",
                stockSelectResultSaveTableName, today);
        DataFrame<Object> dfTemp = DataFrame.readSql(connOfStockSelectResult, sqlIsStockSelectedToday);
        long resultCountOfToday = Long.valueOf(dfTemp.get(0, 0).toString());
        if (resultCountOfToday <= hasStockSelectResultTodayThreshold) { // 全量更新今日全部选股结果
            stockSelect0(); // 真实今日选股并存入数据库, 需要从各大分析研究程序调用对应函数
        }
        // 获取选股结果, api均已缓存
        // List<String> getStockSelectResultOfTradeDate  获取单form_set 某日期 选股结果列表
        HashMap<Long, List<String>> stockSelectResult = KlineFormsApi.getStockSelectResultOfTradeDate(today, keyInts,
                stockSelectResultSaveTableName);
        HashMap<Long, HashSet<String>> stockSelectResultAsSet = new HashMap<>();
        stockSelectResult.keySet().stream().forEach(value -> stockSelectResultAsSet.put(value,
                new HashSet<>(stockSelectResult.get(value)))); // 转换hs


        // 自动调整参数 profitLimitOfFormSetIdFilter, 使得选股数量接近 suitableSelectStockCount
        decideTheMostSuitableFormSetIds(stockSelectResultAsSet);
        // 此时参数已调整好, 最后获取最终确定的 选股结果!!
        confirmStockSelectResult(stockSelectResultAsSet);
        // 确定后两大静态属性已经初始化(即使空): formSerDistributionWeightMapFinal ,stockSelectCountMapFinal

        return null;
    }


    private static void initUseFormSetIds() throws FileNotFoundException {
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
        initUseFormSetIds(); // profitLimitOfFormSetIdFilter 自动调整
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

        formSerDistributionWeightMapFinal = formSerDistributionWeightMap;
        stockSelectCountMapFinal = stockSelectCountMap;
        Assert.isTrue(equalApproximately(formSerDistributionWeightMapFinal.values(), 1.0, 0.005));//权重和1


    }

    /**
     * 单日选股逻辑, 参考:
     *
     * @see com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell
     * :320 行
     */
    private void stockSelect0() throws Exception {
        log.warn("stock select today: 执行今日选股主逻辑. keyInts: {}", keyInts);
        execSql(StrUtil.format(sqlCreateStockSelectResultSaveTableTemplate, stockSelectResultSaveTableName),
                connOfStockSelectResult); // 不存在则建表
        List<String> allHSAstock = DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("沪深A股")), "股票代码");
        List<String> pioneerMarket = DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("创业板")), "股票代码");
        List<String> scientificCreationMarket =
                DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("科创板")),
                        "股票代码");
        HashSet<String> mainboardStocks = subtractionOfList(new ArrayList<>(subtractionOfList(allHSAstock,
                pioneerMarket)),
                scientificCreationMarket); // 两次差集操作, 获取所有主板股票
        // 开始选股. 首先获取近 几日 window数据, 当然, 这里我们多获取1日, 截取最后 n个.  截取2个月
        String today = DateUtil.today(); // yyyy-MM-dd
        String pre7TradeDate = StockApi.getPreNTradeDateStrict(today, 7); // 6足够, 冗余1.  // yyyy-MM-dd
        String pre1TradeDate = StockApi.getPreNTradeDateStrict(today, 1); // 6足够, 冗余1.  // yyyy-MM-dd , 已经缓存
        // 所有主板股票 3000+, 近2个月 日k线, 前复权.  key为stock, value为df
        log.warn("stock amounts: 已获取两市主板股票数量: {}", mainboardStocks.size());
        ConcurrentHashMap<String, DataFrame<Object>> datasMap =
                StockApi.getQuoteHistory(
                        new ArrayList<>(mainboardStocks)
                                .subList(0, Math.min(stockSelectedExecAmounts, mainboardStocks.size())),
                        pre7TradeDate.replace("-", ""),
                        pre1TradeDate.replace("-", ""), // @noti: 若使用today, 则盘中选股将出现今日日期结果
                        "101", "1", 3, false);

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
          	trade_date	open	close	high	 low	   vol	      amount	   振幅	  涨跌幅	  涨跌额	 换手率	昨日收盘	  股票代码	股票名称
            0	2021-12-17	4.54	4.46 	4.54	4.45	182884	81887987.00 	2.00 	-0.89	-0.04	1.15	0   	000976	华铁股份
         */

        Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
        Double yesterdayClose = getPriceOfSingleKline(yesterdayKlineRow, "close");
        List<Object> colName = dfWindow.col("股票名称");
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


    public DummyStrategy(String strategyName) throws Exception {
        super(strategyName);
    }

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
}
