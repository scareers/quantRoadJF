package com.scareers.gui.ths.simulation.strategy.adapter;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.FSBacktestOfLowBuyNextHighSell;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import static com.scareers.datasource.eastmoney.stock.StockApi.getPreNTradeDateStrict;
import static com.scareers.datasource.eastmoney.stock.StockApi.getQuoteHistorySingle;
import static com.scareers.keyfuncs.positiondecision.PositionOfHighSellByDistribution.virtualCdfAsPositionForHighSell;
import static com.scareers.utils.CommonUtil.sendEmailSimple;

/**
 * description:
 * /**
 * *         回测框架相关设定项
 * *         // cdf时tick距离. 千分之5
 * *         tickGap = 0.005;
 * *         // 常规低买参数
 * *         positionUpperLimit = 1.4;
 * *         positionCalcKeyArgsOfCdf = 1.6;
 * *         execLowBuyThreshold = +0.005;
 * *         continuousFallTickCountThreshold = 1;
 * *
 * *         // 指数当时tick加成
 * *         indexBelongThatTimePriceEnhanceArgLowBuy = -0.5;
 * *         indexBelongThatTimePriceEnhanceArgHighSell = -0.5;
 * *
 * *         // 开盘强卖参数
 * *         forceSellOpenWeakStock = false;
 * *         weakStockOpenPercentThatDayThreshold = -0.02;
 * *         weakStockOpenPercentTodayThreshold = -0.07;
 * *
 * *         // 常规高卖参数
 * *         positionCalcKeyArgsOfCdfHighSell = 1.2;
 * *         execHighSellThreshold = -0.02;
 * *         continuousRaiseTickCountThreshold = 1;
 *
 * @author admin
 */

public class LowBuyHighSellStrategyAdapter implements StrategyAdapter {
    public static double tickGap = 0.005;

    // 高卖参数
    public static double indexBelongThatTimePriceEnhanceArgHighSell = -0.5;  // 指数当时价格加成--高卖
    public static double positionCalcKeyArgsOfCdfHighSell = 1.5; // cdf 倍率
    public static double execHighSellThreshold = -0.02; // 价格>=此值(百分比)才考虑卖出
    public static int continuousRaiseTickCountThreshold = 1; // 连续上升n个,本分钟下降

    SecurityBeanEm shangZhengZhiShu = SecurityBeanEm.createIndexList(Arrays.asList("000001")).get(0);
    Double shangZhengZhiShuPreClose = null; // 上证昨日收盘点数
    SecurityBeanEm shenZhengChengZhi = SecurityBeanEm.createIndexList(Arrays.asList("399001")).get(0);
    Double shenZhengChengZhiPreClose = null;

    LowBuyHighSellStrategy strategy;
    Trader trader;
    String pre2TradeDate; // yyyy-MM-dd
    String preTradeDate; // yyyy-MM-dd
    // 暂时保存 某个stock, 当前价格相当于前2日(高卖时) 或前1日(低买时) 的价格变化百分比. 仅片刻意义.
    double newPercent;

    // 记录高卖操作, 实际卖出的数量. 该 table, 将监控 成交记录, 以更新各自卖出数量
    Hashtable<String, Integer> actualHighSelled = new Hashtable<>();

    public LowBuyHighSellStrategyAdapter(LowBuyHighSellStrategy strategy,
                                         Trader trader) throws Exception {
        this.strategy = strategy;
        this.trader = trader;
        pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2);
        preTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1);
    }

    @Override
    public void buyDecision() throws Exception {
//        int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
//        Thread.sleep(sleep * 2000);
//        Order order = null;
//        int type = RandomUtil.randomInt(22);
//        if (type < 8) {
//            order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGHEST);
//        } else if (type < 16) {
//            order = OrderFactory.generateSellOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGH);
//        } else if (type < 18) {
//            order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_HIGH);
//        } else if (type < 20) {
//            order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_HIGH);
//        } else {
//            order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
//        }
//        trader.putOrderToWaitExecute(order);
    }

    @Override
    public void sellDecision() throws Exception {
        DataFrame<Object> yesterdayStockHoldsBeSell = strategy.getYesterdayStockHoldsBeSell();

        // 证券代码	 证券名称	 股票余额	 可用余额	冻结数量	  成本价	   市价	       盈亏	盈亏比例(%)	   当日盈亏	当日盈亏比(%)	       市值	仓位占比(%)	交易市场	持股天数
        for (int i = 0; i < yesterdayStockHoldsBeSell.length(); i++) {
            try { // 捕获异常
                List<Object> line = yesterdayStockHoldsBeSell.row(i);
                String stock = line.get(0).toString();
                SecurityBeanEm stockBean = new SecurityBeanEm(stock).convertToStock();
                int amountsTotal = Integer.parseInt(line.get(2).toString()); // 原始总持仓, 今日开卖

                // 1. 读取前日收盘价
                Double pre2ClosePrice = 0.0;
                try {
                    //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  股票代码	股票名称
                    pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(stock, pre2TradeDate, pre2TradeDate,
                            "101", "qfq", 3,
                            false, 2000).row(0).get(2).toString());
                } catch (Exception e) {
                    log.error("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
                    e.printStackTrace();
                    continue;
                }
                // 2. 判定当前是否是卖点? todo: 开市时间才能验证
//                if (!isSellPoint(stock, pre2ClosePrice, stockBean)) {
//                    log.warn("当前股票非卖点 {}", stock);
//                    continue;
//                }


                double indexPricePercentThatTime = getCurrentIndexChangePercent(stockBean.getMarket());
                // 3.此刻是卖点, 计算以最新价格的 应当卖出的仓位 (相对于原始持仓)
                Double cdfCalcPrice =
                        FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay
                                // stock_code,market,time_tick,price,vol,bs
                                .calcEquivalenceCdfUsePriceOfHighSell(newPercent, indexPricePercentThatTime,
                                        indexBelongThatTimePriceEnhanceArgHighSell);
                // cdf使用 high 计算.  价格使用 sellPrice 计算
                Double cdfOfPoint = virtualCdfAsPositionForHighSell(strategy.getTicksOfHigh1GlobalFinal(),
                        strategy.getWeightsOfHigh1GlobalFinal(), cdfCalcPrice,
                        tickGap);
                // @key3: 高卖仓位折算 * 倍率
                Double epochTotalPosition = Math.min(1.0, positionCalcKeyArgsOfCdfHighSell * cdfOfPoint);

                double shouldSellAmountTotal = epochTotalPosition * amountsTotal;
                int sellAlready = actualHighSelled.getOrDefault(stock, 0);
                if (sellAlready < shouldSellAmountTotal) { // 四舍五入
                    // 三项数据: 此刻卖点应当总卖出 / 原总持仓  --  [早已经成功卖出]
                    int amount = (int) (NumberUtil
                            .round((shouldSellAmountTotal - sellAlready) / 100, 0).doubleValue()) * 100;

                    // 四舍五入价格. 100 整数
                    if (amount + sellAlready > amountsTotal) {
                        amount = (amountsTotal - sellAlready) / 100 * 100;
                    }
                    if (amount < 100) {
//                        log.warn("sell decision: 卖点出现,但折算卖出数量<100,不执行卖出, {} -> {}/{} ; already [{}], actual [{}]",
//                                stock,
//                                shouldSellAmountTotal,
//                                amountsTotal,
//                                sellAlready, amount);
                    } else {
                        // 卖出
                        log.warn("sell decision: 卖点出现, {} -> {}/{} ; already [{}], actual [{}]", stock,
                                shouldSellAmountTotal,
                                amountsTotal,
                                sellAlready, amount);

                        Order order = OrderFactory.generateSellOrderQuick(stock, amount, null, Order.PRIORITY_HIGH);
                        trader.putOrderToWaitExecute(order);

                        actualHighSelled.put(stock, amount + sellAlready); // todo: 这里假装 卖出订单立即所有成交. 应当检测
                    }

                } else { //  新卖点,但没必要卖出更多.(多因为当前价格已经比上一次低, 导致仓位更低)
//                    log.warn("sell decision: 卖点出现,但早已卖出更多仓位,不执行卖出. {} -> {}/{} ; already [{}]", stock,
//                            shouldSellAmountTotal, amountsTotal, sellAlready);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 卖点判定.
     * 1.读取(真)分时图,
     * 2.判定前几分钟分时图 连续上升 n
     * 3.判定本分钟价格 比上一分钟 降低. (过半分钟的时间根据比例, 之前固定返回false)
     *
     * @return
     * @see SettingsOfFSBacktest
     */
    public boolean isSellPoint(String stock, Double pre2ClosePrice, SecurityBeanEm stockBean) throws Exception {
        // 获取今日分时图
        // 2022-01-20 11:30	17.24	17.22	17.24	17.21	10069	17340238.00 	0.17	-0.12	-0.02	0.01	000001	平安银行
        DataFrame<Object> fsDf = trader.getFsFetcher().getFsDatas().get(stockBean);
        final String nowStr = DateUtil.now().substring(0, DateUtil.now().length() - 3);
        // 对 fsDf进行筛选, 筛选 不包含本分钟的. 因底层api会生成最新那一分钟的. 即 13:34:31, 分时图已包含 13:35, 我们需要 13:34及以前
        fsDf = fsDf.select(value -> value.get(0).toString().compareTo(nowStr) <= 0); // 比直接去掉最后一条严谨
        // 计算连续上涨数量
        List<Double> closes = DataFrameS.getColAsDoubleList(fsDf, "收盘");
        int continuousRaise = 0;
        for (int i = closes.size() - 1; i >= 1; i--) {
            if (closes.get(i) >= closes.get(i - 1)) {
                continuousRaise++;
            } else {
                break;
            }
        }
        if (continuousRaise < continuousRaiseTickCountThreshold) { // 连续上升必须>=阈值
            return false;
        }

        double lastFsClose = closes.get(closes.size() - 1); // 作为 计算最新一分钟 价格的基准, 计算涨跌
        // 0	000010    	0          	09:15:09 	3.8  	177 	4
        /*
            stock_code,market,time_tick,price,vol,bs
         */
        DataFrame<Object> fsTransDf =
                trader.getFsTransactionFetcher().getFsTransactionDatas()
                        .get(new SecurityBeanEm(stock).convertToStock());
        String tickWithSecond0 = nowStr.substring(nowStr.length() - 5) + ":00";
        DataFrame<Object> fsLastMinute = fsTransDf
                .select(value -> value.get(2).toString().compareTo(tickWithSecond0) >= 0);
        // 获取最新一分钟所有 成交记录. 价格列
        if (fsLastMinute.size() == 0) { // 未能获取到最新一分钟数据,返回false
            return false;
        }
        List<Double> pricesLastMinute = DataFrameS.getColAsDoubleList(fsLastMinute, 3);
        newPercent = pricesLastMinute.get(pricesLastMinute.size() - 1) / pre2ClosePrice - 1;
        if (newPercent < execHighSellThreshold) {
            return false; // 价格必须足够高, 才可能卖出
        }
        // 计算对比  lastFsClose, 多少上升, 多少下降?
        int countOfLower = 0; // 最新一分钟, 价格比上一分钟收盘更低 的数量
        for (Double price : pricesLastMinute) {
            if (price < lastFsClose) {
                countOfLower++;
            }
        }
        if (((double) countOfLower) / pricesLastMinute.size() > 0.4) { // 判定该分钟close有很大可能价格跌, 视为卖点
            return true;
        }
        return false;
    }

    private double getCurrentIndexChangePercent(int market) throws Exception {
        DataFrame<Object> dfTemp;
        if (market == 0) { // 深证成指
            if (shenZhengChengZhiPreClose == null) { // 上证指数昨日收盘
                DataFrame<Object> df0 = StockApi.getQuoteHistorySingle("399001", preTradeDate, preTradeDate,
                        "101", "qfq",
                        3, true, 2000, true);
                shenZhengChengZhiPreClose = Double.valueOf(df0.get(0, 2).toString());// 收盘
            }
            // stock_code,market,time_tick,price,vol,bs
            dfTemp =
                    trader.getFsTransactionFetcher().getFsTransactionDatas().get(shenZhengChengZhi);
        } else {
            if (shangZhengZhiShuPreClose == null) { // 上证指数昨日收盘
                DataFrame df0 = StockApi.getQuoteHistorySingle("000001", preTradeDate, preTradeDate,
                        "101", "qfq",
                        3, true, 2000, true);
                shangZhengZhiShuPreClose = Double.valueOf(df0.get(0, 2).toString());// 收盘, 这是分时图
            }

            dfTemp =
                    trader.getFsTransactionFetcher().getFsTransactionDatas().get(shangZhengZhiShu);
        }
//        Console.log(dfTemp);
        // 分时成交则是3
        return Double.parseDouble(dfTemp.get(dfTemp.length() - 1, 3).toString()) / shangZhengZhiShuPreClose - 1;
    }

    @Override
    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }


    /*
     *                     response = dict(state='success', description='卖出订单执行成功',
     *                                     orderId=latest_order[headers.index("合同编号")],
     *                                     stockCode=stockCode,
     *                                     price=actual_price,
     *                                     priceNote=priceNote,
     *                                     amounts=amounts,
     *                                     payload=confirm_info_dict,
     *                                     notes="注意: 通过查询今日全部订单确定的订单成功id.",
     *                                     rawOrder=order)
     *
     *           response = dict(state="fail", failReason=fail_reason.FAIL_ORDER_ARGS_ERROR,
     *                         description='订单对象不合法[订单3参数设定错误]',
     *                         payload=args_invalid_reason,  # list
     *                         warning='请将订单修改为合法格式再行传递',
     *                         rawOrder=order)
     *
     *           return dict(state="fail",
     *                     failReason=fail_reason.FAIL_ORDER_AMOUNT_ZERO_MAYBE,
     *                     description='数量被自动填充为0, 可能因可买卖数量不足1手',  # list
     *                     warning='请检测可买卖数量是否不小于1手',
     *                     rawOrder=order)
     *
     *                      response = dict(state='fail',
     *                                 failReason=fail_reason.FAIL_ORDER_CONFIRM,
     *                                 description=info_for_type_determine,  # list
     *                                 rawOrder=order)  # 0.0
     *              response = dict(state='success', description='卖出订单执行成功', orderId=orderId,
     *                         stockCode=stockCode,
     *                         price=actual_price,
     *                         priceNote=priceNote,
     *                         amounts=amounts,
     *                         rawOrder=order)
     *
     *                     response = dict(state='fail',
     *                         failReason=fail_reason.FAIL_ORDER_COMMIT,
     *                         description=failReason,
     *                         stockCode=stockCode,
     *                         price=actual_price,
     *                         priceNote=priceNote,
     *                         amounts=amounts,
     *                         rawOrder=order)
     */

    /**
     * 对 sell order 的 check逻辑!
     * Checker 将死循环监控所有订单, 因此, check逻辑并不需要保证将 order 移除队列到 finish队列
     * 依据python api文档: 响应分为四大类状态. 其中:
     * exception状态意味着无法处理. (通常直接进入最终失败队列)
     * retrying意味着正在尝试(一半不会以此结束, 都会得到 success 或者 fail)
     * success 以某种逻辑上的成功执行完成
     * fail 以某种逻辑上的失败执行完成, 可尝试修改订单重试, 也可进入最终失败队列, 视情况而定
     * <p>
     * --------- 响应分类
     * dispatch 将可能发送 exception 状态响应(2种),重试的retrying 响应, 以及重试达到上限的 fail 响应
     * sell 自身api 可能响应如上.
     *
     * @param order
     * @param responses
     * @param orderType
     * @key2 对应卖单的check逻辑, 当python执行成功后, 订单进入check队列. (执行器已去执行其他任务)
     */

    private long maxCheckSellOrderTime = 10 * 1000; // 超过此check时间发送失败邮件, 直接进入失败队列, 需要手动确认

    @SneakyThrows
    @Override
    public void checkSellOrder(Order order, List<Response> responses, String orderType) {
        Thread.sleep(1);
        if (order.getLastLifePoint().getStatus() != Order.LifePointStatus.CHECKING) { // 首次的逻辑
            order.addLifePoint(Order.LifePointStatus.CHECKING, "执行成功: 开始checking");
            if (responses.size() > 1) {
                log.warn("total retrying times maybe: 订单共计重试 {} 次", responses.size() - 1);
            }
        }

        Response response = responses.get(responses.size() - 1);
        String state = response.getStr("state");
        if ("success".equals(state)) {
            if (order.getLastLifePoint().getStatus() != Order.LifePointStatus.CHECKING) { // 第一次
                String notes = response.getStr("notes");
                if (notes != null && notes.contains("通过查询今日全部订单确定的订单成功id")) {
                    log.warn("success noti: {}", notes);
                } else {
                    log.warn("success start checking: {} {}", order.getOrderType(), order.getParams());
                }
            }

            if (orderAlreadyMatchAllBuyOrSell(order, response)) {
                log.warn("checked ok: response [success]: 已全部成交: {} [{}]", order.getOrderType(), order.getParams());
                order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功: 已全部成交");
                trader.successFinishOrder(order, responses);
            } else {
                long checkTimeElapsed = DateUtil.between(order.getLastLifePoint().getGenerateTime(), DateUtil.date(),
                        DateUnit.MS, true); // checking 状态持续了多少 ms??
//                Console.log("{} {}", checkTimeElapsed > maxCheckSellOrderTime, order.getRawOrderId());
                if (checkTimeElapsed > maxCheckSellOrderTime) {
                    log.error("checked: response [success] but check fail: {}ms 内未能全部成交 {} [{}]",
                            maxCheckSellOrderTime,
                            order.getOrderType(),
                            order.getParams());
                    order.addLifePoint(Order.LifePointStatus.CHECKED,
                            StrUtil.format("执行成功[check失败]: check失败, {}ms 内未能全部成交 {} [{}]", maxCheckSellOrderTime,
                                    order.getOrderType(),
                                    order.getParams()));
                    trader.reSendAndFinishOrder(order, responses); // check失败, 不加入 checked生命周期, 直接进入失败队列, 需要人为观察
                    try {
                        sendEmailSimple(
                                StrUtil.format("Trader.Checker: 订单执行成功但{}ms内未能全部成交,注意手动确认!", maxCheckSellOrderTime),
                                StrUtil.format("order 对象: \n{}", order.toStringPretty()), true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } // 继续checking
            }
        } else if ("fail".equals(state)) {
            // todo
            log.error("checked: response[fail]: {} [{}]", order.getOrderType(), order.getParams());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败 todo");
            trader.failFinishOrder(order, responses);
        } else if ("exception".equals(state)) { // 极快,省掉 CHECKING 生命周期
            log.error("checked: response[exception]: {} [{}]", order.getOrderType(), order.getParams());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行异常: state为exception");
            trader.failFinishOrder(order, responses);
        } else { // 未知响应状态.
            log.error("checked: response[unknown]: 未知响应状态 {} [{}]", order.getOrderType(),
                    order.getParams());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行错误: 未知响应状态:" + state);
            trader.failFinishOrder(order, responses);
        }
    }

    // 成交时间	证券代码	证券名称	操作	成交数量	成交均价	成交金额	合同编号	成交编号
    public boolean orderAlreadyMatchAllBuyOrSell(Order order, Response response) {
        String orderId = response.getStr("orderId");
        DataFrame<Object> clinchsDf = trader.getAccountStates().getTodayClinchs();
        List<String> ids = DataFrameS.getColAsStringList(clinchsDf, "合同编号");
        List<Integer> amounts = DataFrameS.getColAsIntegerList(clinchsDf, "成交数量");
        int clinchAmount = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).equals(orderId)) {// 同花顺订单id.
                clinchAmount += amounts.get(i);
            }
        }
        // 判定某 order , 当前是否已经全部成交. 买卖但逻辑相同, 均对合同编号筛选, 求和所有成交数量.
        // 缺陷在于 trader.getAccountStates().getTodayClinchs()刷新及时性
        if (clinchAmount >= Integer.parseInt(response.getStr("amounts")) / 100 * 100) {
            return true;
        }
        return false;
    }

    @Override
    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toString());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败");
        }
        trader.successFinishOrder(order, responses);
    }

    private static final Log log = LogUtil.getLogger();
}
