package com.scareers.gui.ths.simulation.strategy.adapter;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactorChain;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.buysellpoint.SellPointDecideFactorHs;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.position.PositionAndAmountFactorHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.bk.BkStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.index.IndexStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.index.factor.IndexPricePercentFactorHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.other.OtherStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.stock.StockStateHs;
import com.scareers.gui.ths.simulation.trader.AccountStates;
import com.scareers.gui.ths.simulation.trader.SettingsOfTrader;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;
import static com.scareers.utils.CommonUtil.sendEmailSimple;

/**
 * 优化机制:
 * 1.执行队列中, 单次仅可 唯一买卖订单! 相当于可以不使用订单, 弱化了优先级的效果. 但能使得生成订单实时状态不是过时的.
 */

public class LowBuyHighSellStrategyAdapter implements StrategyAdapter {
    private long maxCheckSellOrderTime = 2 * 60 * 1000; // 卖单超过此check时间发送失败邮件, 直接进入失败队列, 需要手动确认
    private long maxCheckBuyOrderTime = 2 * 60 * 1000; // 买单超过此check时间发送失败邮件, 直接进入失败队列, 需要手动确认
    public static double tickGap = 0.005;

    // 高卖参数
    public static double indexBelongThatTimePriceEnhanceArgHighSell = 0.0;  // 指数当时价格加成--高卖
    public static double positionCalcKeyArgsOfCdfHighSell = 1.2; // cdf 倍率
    public static double execHighSellThreshold = -0.02; // 价格>=此值(百分比)才考虑卖出
    public static int continuousRaiseTickCountThreshold = 1; // 连续上升n个,本分钟下降

    // 低买参数
    public static double indexBelongThatTimePriceEnhanceArgLowBuy = 0.0;
    public static double positionUpperLimit = 1.4; // 上限
    public static double positionCalcKeyArgsOfCdf = 1.6; // cdf倍率
    public static double execLowBuyThreshold = -0.0;
    public static double continuousFallTickCountThreshold = 1;

    // 卖点点提前机制, 秒数:条件百分比 设定
    // 高卖时, 在 9:40:xx 不等待 9:41 的分时图确定生成 更低价分时, 而在 9:40:xx 就推断未来 9:41最终分时价格是降低的
    // 该map为 key:value --> 当前秒数 --> 需要fs成交中,降低价格次数 / (降低+上涨) 的百分比 > value, 才视为提前生成卖点
    public static ConcurrentHashMap<Integer, Double> highSellBeforehandThresholdMap; // 静态块实现

    static {
        initHighSellBeforehandThresholdMap();
    }

    private static void initHighSellBeforehandThresholdMap() {
        highSellBeforehandThresholdMap = new ConcurrentHashMap<>();
        // 注意, key 控制 当前秒数<key, 不包含
        highSellBeforehandThresholdMap.put(10, 2.0); // 必须 价格降低次数 /(价格升高+降低) >= 此百分比, 显然2.0意味着不可能
        highSellBeforehandThresholdMap.put(20, 1.0);
        highSellBeforehandThresholdMap.put(30, 0.9);
        highSellBeforehandThresholdMap.put(40, 0.7);
        highSellBeforehandThresholdMap.put(50, 0.6);
        highSellBeforehandThresholdMap.put(60, 0.0); // 最后10s视为绝对符合条件. 10和60并未用到
    }

    // 暂时保存 某个stock, 当前价格相当于前2日(高卖时) 或前1日(低买时) 的价格变化百分比. 仅片刻意义,
    private volatile double newPercent;

    LowBuyHighSellStrategy strategy;
    Trader trader;
    String pre2TradeDate; // yyyy-MM-dd
    String preTradeDate; // yyyy-MM-dd

    /*
    高卖相关属性 ***********
     */

    int hsPerSleep; // 高卖决策每轮显式sleep, 减少cpu消耗

    // 高卖可用数量/已卖数量 机制简述:
    // 1.当某股票有卖单在执行(或执行队列中)时, 不论是否运行生成同股票卖单, actualAmountHighSelledMap/availableAmountForHsMap为推断值,由生成卖单时更新
    // 2.当股票卖单执行完毕, 在checking时, 允许新卖单出现. 此时 也为推断值, 由生成卖单时更新后不变
    // 3.当股票卖单不论成功与否, check完毕时, 均读取最新数据为可用值, 两者为实际值(此时可能有新的卖单执行,数量基本不会误判), AS更新实际数据
    // 4.当股票不存在卖单在执行队列, 执行中, checking中, 均读取最新数据为可用值, 两者为实际值; AS更新实际数据
    // 昨日收盘持仓状况, 初始化后逻辑上不变
    public static Hashtable<String, Integer> yesterdayStockHoldsBeSellMap = new Hashtable<>();
    // 记录高卖操作, 今日总已卖出的数量. 某些情况下是实际值, 某些情况下是推断值, 见 sellDecision() 文档说明
    public static Hashtable<String, Integer> actualAmountHighSelledMap = new Hashtable<>();
    // 等价的每一轮卖出决策之前, 各股票的剩余可用(可卖)数量; 同样某些情况是最新数据, 某些情况时推断值
    // @key3: 恒等式: yesterdayStockHoldsBeSellMap = actualAmountHighSelledMap + availableAmountForHsMap
    public static Hashtable<String, Integer> availableAmountForHsMap = new Hashtable<>();


    // 使用"冻结数量"表示今日曾买过数量,初始化. 每当实际执行买单, 无视成交状况, 全部增加对应value
    Hashtable<String, Integer> todayStockHoldsAlreadyBuyMap = new Hashtable<>();


    public LowBuyHighSellStrategyAdapter(LowBuyHighSellStrategy strategy, Trader trader, int hsPerSleep) {
        this.strategy = strategy;
        this.trader = trader;
        pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2);
        preTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1);
        this.hsPerSleep = hsPerSleep;
    }

    /**
     * 高卖相关初始化, 完全可靠
     */
    private void initForHsDecision() {
        initYesterdayHoldMapForSell(); // 初始化昨日持仓map
        flushActualAmountHighSelledAndAvailableAmountFromAS();
    }


    /**
     * 初始化昨日收盘后股票持仓数量情况, 由 strategy 提供数据
     */
    private void initYesterdayHoldMapForSell() {
        DataFrame<Object> yesterdayStockHoldsBeSell = strategy.getYesterdayStockHoldsBeSell();
        List<String> stockCol = DataFrameS
                .getColAsStringList(yesterdayStockHoldsBeSell, SettingsOfTrader.STR_SEC_CODE);
        List<Integer> balanceCol = DataFrameS
                .getColAsIntegerList(yesterdayStockHoldsBeSell, SettingsOfTrader.STR_SEC_BALANCE);
        Assert.isTrue(stockCol.size() == balanceCol.size());

        for (int i = 0; i < stockCol.size(); i++) {
            yesterdayStockHoldsBeSellMap.put(stockCol.get(i), balanceCol.get(i));
        }
    }

    /**
     * 使用 as 最新数据,
     * 刷新今日实际已经卖出过的股票数量, 和当前最新可用数量 [实际]
     * 对单只股票, 实际卖出过 == 昨日收盘持仓 - 此刻账户信息持仓中可用数量
     *
     * @noti 因T+1, 即使今日买入了某只股票, 也是不可用的, 所以此算法足够健壮
     */
    private void flushActualAmountHighSelledAndAvailableAmountFromAS() {
        Map<String, Integer> map = trader.getAccountStates().getAvailableAmountOfStocksMap();
        for (String key : map.keySet()) {
            if (yesterdayStockHoldsBeSellMap.containsKey(key)) {
                availableAmountForHsMap.put(key, map.get(key));
                actualAmountHighSelledMap
                        .put(key, yesterdayStockHoldsBeSellMap.get(key) - availableAmountForHsMap.get(key));
            }
        }
    }


    @Override
    public void buyDecision() throws Exception {

    }

    /**
     * 方法名称: 刷新今日已卖出和当前可用Map; 确定性或者推断性质
     * 今日已卖出Map和当前可用Map 的刷新逻辑
     * 高卖可用数量/已卖数量 机制简述:
     * 1.当某股票有卖单在执行(或执行队列中)时, 不论是否运行生成同股票卖单, actualAmountHighSelledMap/availableAmountForHsMap为推断值,由生成卖单时更新
     * 2.当股票卖单执行完毕, 在checking时, 允许新卖单出现. 此时 也为推断值, 由生成卖单时更新后不变
     * 3.当股票卖单不论成功与否, check完毕时, 均读取最新数据为可用值, 两者为实际值(此时可能有新的卖单执行,数量基本不会误判), AS更新实际数据
     * 4.当股票不存在卖单在执行队列, 执行中, checking中, 均读取最新数据为可用值, 两者为实际值; AS更新实际数据
     */
    private void flashActualHighSelledAndCurrentAvailableCertaintyOrInferential() {
        // actualAmountHighSelledMap 和 availableAmountForHsMap; init时保证有所有key

        // 1. 某股票有卖出订单在执行队列
        HashSet<String> hasSellOrderWaitExecute = trader.getOrderExecutor().hasSellOrderOfTheseStocksWaitExecute();
        // 2. 某股票的卖出订单正在执行
        String executingSellStock = trader.getOrderExecutor().getStockArgWhenExecutingSellOrder();
        // 3. 某股票的卖出订单正在等待checking完成
        HashSet<String> checkingStocks = Trader.hasSellOrderOfTheseStocksWaitChecking();

        Map<String, Integer> availableAmountOfStocksMapNewest = // 最新可用数量数据
                trader.getAccountStates().getAvailableAmountOfStocksMap();
        for (String stockHs : availableAmountOfStocksMapNewest.keySet()) { // 两Map有相同数量key, 且已被正确初始化, 固定不变
            if (hasSellOrderWaitExecute.contains(stockHs) || stockHs.equals(executingSellStock) || checkingStocks
                    .contains(stockHs)) {
                // 当股票属于 3种状态之一, 不强制刷新最新数据;
                // 此时的 实际卖出与可用, 应当由卖出决策函数修改为 "逻辑正确".
                continue;
            }
            // 此时应当采用最新数据
            availableAmountForHsMap.put(stockHs, availableAmountOfStocksMapNewest.get(stockHs));
            actualAmountHighSelledMap
                    .put(stockHs,
                            yesterdayStockHoldsBeSellMap.getOrDefault(stockHs, 0) - availableAmountForHsMap
                                    .getOrDefault(stockHs, 0));
            // todo : bugfix null
        }
    }

    /**
     * 1.注意: 执行队列订单唯一,(所有买单和卖单)
     * 2.关于可卖数量的刷新机制: 数据更新不及时的问题不可绝对避免, 只能尽量避免.
     * --> 1.初始化时, 可直接读取到初始的可卖数量, 并计算到等价今日已经卖出数量
     * --> 2.单股票卖单唯一.
     * --> 3.当卖单在执行时, 已卖出数量 = 原已卖出 + 卖单数量, 此时不会生成新的卖单, 可用数量更新为 原可用 - 卖单数量
     * --> 4.当卖单执行成功在checking时(等待全部成交), 已卖出数量 = 原已卖出 + 卖单数量, 可用数量更新为 原可用 - 卖单数量
     * --> 5.当卖单执行失败,或者其他状态已经"完成"时, 即已不在checking队列, 此时应当读取真实数据:
     * --> 5.此时 可用数量 == 账户状态获取最新数据, 已卖出数量 = 昨收总 - 此时最新可用. // 即若卖单各种原因失败, 两项数据使用最新实际值.
     * --> 6.当执行队列和正在执行队列和checking队列 无某股票卖单时, 每轮均按照 5 的方式更新为最新的数据. 因此人类手工干预卖出的, 能被正确识别
     * --> 7.卖出决策最开始, 都应当刷新此2值: 等价实际已卖出, 当前可卖出.  // 可以是最新数据, 可以是推断数据. 视情况
     *
     * @throws Exception
     */
    private boolean flushed = false;

    @Override
    public void sellDecision() throws Exception {
        if (!flushed) {
            initForHsDecision();
            flushed = true;
        }

        Thread.sleep(hsPerSleep);
        if (yesterdayStockHoldsBeSellMap.size() == 0) { // 开始决策后才会被正确初始化
            log.error("show: 昨日无股票持仓, 因此无需执行卖出决策");
            return;
        }
        // todo: 强制卖出 14:57

        // 第一步需要刷新本轮, 每只股票的 实际已卖出(等价或实际) 和 当前可用(推断或最新)
        flashActualHighSelledAndCurrentAvailableCertaintyOrInferential();

        for (String stock : yesterdayStockHoldsBeSellMap.keySet()) {
            SecurityBeanEm beanEm = SecurityBeanEm.createStock(stock);
            StockStateHs stockStateHs = new StockStateHs(beanEm);
            HsState hsState = new HsState(null, new IndexStateHs(beanEm), new BkStateHs(), stockStateHs,
                    new OtherStateHs());

            HsFactorChain factorChain = new HsFactorChain(hsState);
            factorChain.addFactor(new SellPointDecideFactorHs());
            factorChain.addFactor(new IndexPricePercentFactorHs());
            factorChain.addFactor(new PositionAndAmountFactorHs());
            factorChain.applyFactorInfluence();

            stockWithStatesInfByFactorsHs.put(stock, factorChain.getHsStates());
        }

    }

    /**
     * 股票: 某时刻因子影响每一步状态列表.
     */
    public static Hashtable<String, List<HsState>> stockWithStatesInfByFactorsHs = new Hashtable<>();


    @Override
    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {

    }


    /**
     * 对 sell order 的 check逻辑!
     * Checker 将死循环监控所有订单, 因此, check逻辑并不需要保证将 order 移除队列到 finish队列
     * 依据python api文档: 响应分为四大类状态. 其中:
     * exception状态意味着无法处理. (通常直接进入最终失败队列)
     * retrying意味着正在尝试(一半不会以此结束, 都会得到 success 或者 fail)
     * success 以某种逻辑上的成功执行完成
     * fail 以某种逻辑上的失败执行完成, 可尝试修改订单重试, 也可进入最终失败队列, 视情况 而定
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
        String state = response.getString("state");
        if ("success".equals(state)) {
            if (order.getLastLifePoint().getStatus() != Order.LifePointStatus.CHECKING) { // 第一次
                String notes = response.getString("notes");
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
                if (!order.isAfterAuctionFirst()) { // 非集合竞价后首订单, 依据check延时设定
                    long checkTimeElapsed = DateUtil
                            .between(order.getLastLifePoint().getGenerateTime(), DateUtil.date(),
                                    DateUnit.MS, true); // checking 状态持续了多少 ms??
                    if (checkTimeElapsed > maxCheckSellOrderTime) {
                        orderHasNotMatchAll(order, responses);
                    } // 否则继续checking
                } else {
                    if ("09:35:00".compareTo(DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN)) < 0) {
                        orderHasNotMatchAll(order, responses); // 集合竞价后首卖单超时时间固定
                    }// 否则继续checking
                }
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

    private void orderHasNotMatchAll(Order order, List<Response> responses) {
        log.error("checked: response [success] but check fail: {}ms 内未能全部成交 {} [{}]",
                maxCheckSellOrderTime,
                order.getOrderType(),
                order.getParams());
        order.addLifePoint(Order.LifePointStatus.CHECKED,
                StrUtil.format("执行成功[check失败]: check失败, {}ms 内未能全部成交 {} [{}]", maxCheckSellOrderTime,
                        order.getOrderType(),
                        order.getParams()));
        trader.failFinishOrder(order, responses);
        try {
            sendEmailSimple(
                    StrUtil.format("Trader.Checker: 订单执行成功但{}ms内未能全部成交,注意手动确认!", maxCheckSellOrderTime),
                    StrUtil.format("order 对象: \n{}", order.toStringPretty()), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 成交时间	证券代码	证券名称	操作	成交数量	成交均价	成交金额	合同编号	成交编号
    public boolean orderAlreadyMatchAllBuyOrSell(Order order, Response response) {
        String orderId = response.getString("orderId");
        DataFrame<Object> clinchsDf = AccountStates.getTodayClinchs();
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
        if (clinchAmount >= Integer.parseInt(response.getString("amounts")) / 100 * 100) {
            return true;
        }
        return false;
    }

    @Override
    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getString("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtilS.parseArray(responses).toString());
            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败");
        }
        trader.successFinishOrder(order, responses);
    }

    private static final Log log = LogUtil.getLogger();
}
