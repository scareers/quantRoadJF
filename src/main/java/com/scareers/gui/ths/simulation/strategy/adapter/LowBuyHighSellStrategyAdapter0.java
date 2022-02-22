//package com.scareers.gui.ths.simulation.strategy.adapter;
//
//import cn.hutool.core.date.DateField;
//import cn.hutool.core.date.DatePattern;
//import cn.hutool.core.date.DateUnit;
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.util.NumberUtil;
//import cn.hutool.core.util.RandomUtil;
//import cn.hutool.core.util.StrUtil;
//import com.alibaba.fastjson.JSONObject;
//import com.scareers.datasource.eastmoney.SecurityPool;
//import com.scareers.gui.ths.simulation.trader.AccountStates;
//import com.scareers.utils.JSONUtilS;
//import cn.hutool.log.Log;
//import com.scareers.datasource.eastmoney.SecurityBeanEm;
//import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
//import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
//import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
//import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.FSBacktestOfLowBuyNextHighSell;
//import com.scareers.gui.ths.simulation.OrderFactory;
//import com.scareers.gui.ths.simulation.Response;
//import com.scareers.gui.ths.simulation.order.Order;
//import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
//import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
//import com.scareers.gui.ths.simulation.trader.Trader;
//import com.scareers.pandasdummy.DataFrameS;
//import com.scareers.utils.log.LogUtil;
//import joinery.DataFrame;
//import lombok.SneakyThrows;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;
//import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getQuoteHistorySingle;
//import static com.scareers.keyfuncs.positiondecision.PositionOfHighSellByDistribution.virtualCdfAsPositionForHighSell;
//import static com.scareers.keyfuncs.positiondecision.PositionOfLowBuyByDistribution.virtualCdfAsPositionForLowBuy;
//import static com.scareers.utils.CommonUtil.sendEmailSimple;
//
///**
// * description:
// * /**
// * *         回测框架相关设定项
// * *         // cdf时tick距离. 千分之5
// * *         tickGap = 0.005;
// * *         // 常规低买参数
// * *         positionUpperLimit = 1.4;
// * *         positionCalcKeyArgsOfCdf = 1.6;
// * *         execLowBuyThreshold = +0.005;
// * *         continuousFallTickCountThreshold = 1;
// * *
// * *         // 指数当时tick加成
// * *         indexBelongThatTimePriceEnhanceArgLowBuy = -0.5;
// * *         indexBelongThatTimePriceEnhanceArgHighSell = -0.5;
// * *
// * *         // 开盘强卖参数
// * *         forceSellOpenWeakStock = false;
// * *         weakStockOpenPercentThatDayThreshold = -0.02;
// * *         weakStockOpenPercentTodayThreshold = -0.07;
// * *
// * *         // 常规高卖参数
// * *         positionCalcKeyArgsOfCdfHighSell = 1.2;
// * *         execHighSellThreshold = -0.02;
// * *         continuousRaiseTickCountThreshold = 1;
// *
// * @author admin
// */
//
//public class LowBuyHighSellStrategyAdapter implements StrategyAdapter {
//    private long maxCheckSellOrderTime = 2 * 60 * 1000; // 卖单超过此check时间发送失败邮件, 直接进入失败队列, 需要手动确认
//    private long maxCheckBuyOrderTime = 2 * 60 * 1000; // 买单超过此check时间发送失败邮件, 直接进入失败队列, 需要手动确认
//
//    public static double tickGap = 0.005;
//
//    // 高卖参数
//    public static double indexBelongThatTimePriceEnhanceArgHighSell = 0.0;  // 指数当时价格加成--高卖
//    public static double positionCalcKeyArgsOfCdfHighSell = 1.2; // cdf 倍率
//    public static double execHighSellThreshold = -0.02; // 价格>=此值(百分比)才考虑卖出
//    public static int continuousRaiseTickCountThreshold = 1; // 连续上升n个,本分钟下降
//
//    // 低买参数
//    public static double indexBelongThatTimePriceEnhanceArgLowBuy = 0.0;
//    public static double positionUpperLimit = 1.4; // 上限
//    public static double positionCalcKeyArgsOfCdf = 1.6; // cdf倍率
//    public static double execLowBuyThreshold = -0.0;
//    public static double continuousFallTickCountThreshold = 1;
//
//    // 卖点点提前机制, 秒数:条件百分比 设定
//    // 高卖时, 在 9:40:xx 不等待 9:41 的分时图确定生成 更低价分时, 而在 9:40:xx 就推断未来 9:41最终分时价格是降低的
//    // 该map为 key:value --> 当前秒数 --> 需要fs成交中,降低价格次数 / (降低+上涨) 的百分比 > value, 才视为提前生成卖点
//    public static ConcurrentHashMap<Integer, Double> highSellBeforehandThresholdMap; // 静态块实现
//
//    static {
//        initHighSellBeforehandThresholdMap();
//    }
//
//    private static void initHighSellBeforehandThresholdMap() {
//        highSellBeforehandThresholdMap = new ConcurrentHashMap<>();
//        // 注意, key 控制 当前秒数<key, 不包含
//        highSellBeforehandThresholdMap.put(10, 2.0); // 必须 价格降低次数 /(价格升高+降低) >= 此百分比, 显然2.0意味着不可能
//        highSellBeforehandThresholdMap.put(20, 1.0);
//        highSellBeforehandThresholdMap.put(30, 0.9);
//        highSellBeforehandThresholdMap.put(40, 0.7);
//        highSellBeforehandThresholdMap.put(50, 0.6);
//        highSellBeforehandThresholdMap.put(60, 0.0); // 最后10s视为绝对符合条件. 10和60并未用到
//    }
//
//    LowBuyHighSellStrategy strategy;
//    Trader trader;
//    String pre2TradeDate; // yyyy-MM-dd
//    String preTradeDate; // yyyy-MM-dd
//
//    // 暂时保存 某个stock, 当前价格相当于前2日(高卖时) 或前1日(低买时) 的价格变化百分比. 仅片刻意义,
//    private volatile double newPercent;
//
//    // 记录高卖操作, 实际卖出的数量. 该 table, 将监控 成交记录, 以更新各自卖出数量
//    Hashtable<String, Integer> actualHighSelled = new Hashtable<>();
//    Hashtable<String, Integer> yesterdayStockHoldsBeSellMap = new Hashtable<>();
//
//    // 使用"冻结数量"表示今日曾买过数量,初始化. 每当实际执行买单, 无视成交状况, 全部增加对应value
//    Hashtable<String, Integer> todayStockHoldsAlreadyBuyMap = new Hashtable<>();
//
//    public LowBuyHighSellStrategyAdapter(LowBuyHighSellStrategy strategy, Trader trader) {
//        this.strategy = strategy;
//        this.trader = trader;
//        pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2);
//        preTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1);
//    }
//
//
//    private void initYesterdayHoldMapForSell() {
//        for (int i = 0; i < strategy.getYesterdayStockHoldsBeSell().length(); i++) {
//            List<Object> line = strategy.getYesterdayStockHoldsBeSell().row(i);
//            String stock = line.get(0).toString();
//            int amountsTotal = Integer.parseInt(line.get(2).toString()); // 原始总持仓, 今日开卖
//            yesterdayStockHoldsBeSellMap.put(stock, amountsTotal);
//        }
//    }
//
//    private void initActualHighSelled() {
//        Map<String, Integer> map = trader.getAccountStates().getAvailableAmountOfStocksMap();
//        for (String key : map.keySet()) {
//            if (yesterdayStockHoldsBeSellMap.containsKey(key)) {
//                actualHighSelled.put(key, yesterdayStockHoldsBeSellMap.get(key) - map.get(key));
//            }
//        }
//    }
//
//    private void initTodayAlreadyBuyMap() { // todayStockHoldsAlreadyBuyMap
//        Map<String, Integer> map = trader.getAccountStates().getFrozenOfStocksMap();
//        todayStockHoldsAlreadyBuyMap.putAll(map);
//    }
//
//    @Override
//    public void buyDecision() throws Exception {
//        if (todayStockHoldsAlreadyBuyMap.size() == 0) {
//            initTodayAlreadyBuyMap();
//        }
//        for (String stock : strategy.getLbHsSelector().getSelectResults()) {
//            SecurityBeanEm stockBean = SecurityBeanEm.createStock(stock);
//
//            // 1. 读取昨日收盘价
//            Double preClosePrice = 0.0;
//            try {
//                //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
//                preClosePrice =
//                        Double.valueOf(getQuoteHistorySingle(SecurityBeanEm.createStock(stock), preTradeDate,
//                                preTradeDate, "101", "qfq", 3, 2000).row(0).get(2).toString());
//            } catch (Exception e) {
//                log.error("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
//                e.printStackTrace();
//                continue;
//            }
//
//            // 2.买单也互斥
//            if (hasMutualExclusionOfBuySellOrder(stock, "buy")) {
//                continue;
//            }
//            // 3.买点判定
//            if (!isBuyPoint(stock, preClosePrice, stockBean)) {
//                continue;
//            }
//
//            double indexPricePercentThatTime = getCurrentIndexChangePercent(stockBean.getMarket());
//            // 4.此刻是买点, 计算以最新价格的 应当买入的仓位 (相对于原始持仓)
//            Double cdfCalcPrice =
//                    FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay.calcEquivalenceCdfUsePriceOfLowBuy(
//                            newPercent, indexPricePercentThatTime,
//                            indexBelongThatTimePriceEnhanceArgLowBuy);
//            Double cdfOfPoint = virtualCdfAsPositionForLowBuy(
//                    strategy.getLbHsSelector().getTicksOfLowBuy(),
//                    strategy.getLbHsSelector().getWeightsOfLowBuy(),
//                    cdfCalcPrice,
//                    tickGap);
//            // @key2: 新的总仓位, 按照全资产计算. 本身是自身cdf仓位 * 1/(选股数量)
//            Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint / strategy.getLbHsSelector()
//                    .getSelectResults().size();
//            epochTotalPosition = Math.min(epochTotalPosition, positionUpperLimit); // 强制设定的上限 1.4
//            // 总资产, 使用 AccountStates 实时获取最新!
//            Double totalAssets = trader.getAccountStates().getTotalAssets(); // 最新总资产
//            Double shouldMarketValue = totalAssets * epochTotalPosition; // 应当的最新市值.
//
//            Double newestPrice = FsTransactionFetcher.getNewestPrice(stockBean);
//            if (newestPrice == null) {
//                log.warn("股票最新成交价格 无数据: {}", stockBean.getName());
//                continue;
//            }
//            double shouldTotalAmount =
//                    shouldMarketValue / newestPrice;
//            Integer alreadyBuyAmount = todayStockHoldsAlreadyBuyMap.getOrDefault(stock, 0);
//
//            // 应当买入的数量, int形式, floor   100整数倍
//            int shouldBuyAmount = (int) Math.floor((shouldTotalAmount - alreadyBuyAmount) / 100) * 100;
//            if (shouldBuyAmount < 100) { // 不足 100无视
//                continue;
//            }
//
//            // 应当买入! 但是需要判定现金是否充足 ?!
//            Double availableCash = trader.getAccountStates().getAvailableCash();
//            int maxCanBuyAmount = (int) Math // 100整数倍
//                    .floor((availableCash / (newestPrice)) / 100) * 100;
//            if (shouldBuyAmount <= maxCanBuyAmount) { // 可正常全部买入
//                actualBuy(stock, shouldBuyAmount,
//                        todayStockHoldsAlreadyBuyMap.getOrDefault(stock, 0), maxCanBuyAmount, shouldBuyAmount);
//            } else { // 只可买入部分, 或者 0.
//                // @noti: 这里的重要决策是, 是否应当下单买入部分, 然后尝试资金调度.
//                // 或者不买入部分, 等待资金调度成功后,将被下次判定全部买入
//                // @key3: 这里采用的方法是: 买入部分,即maxCanBuyAmount, 然后调用资金调度函数,尝试回笼资金.
//                // 但本次买点执行完成. 悬挂的剩余部分, 应当由下次合理的买点进行分配, 本次买点不再关心!
//                // 简而言之: 当下现金最优. 并尝试调度资金(可能造成卖出后现金空闲一段时间).但不可避免
//
//                if (maxCanBuyAmount >= 100) {
//                    // 下单最大可买入, 并尝试调度资金. 资金调度优先级为高
//                    actualBuy(stock, shouldBuyAmount,
//                            todayStockHoldsAlreadyBuyMap.getOrDefault(stock, 0), maxCanBuyAmount, maxCanBuyAmount);
//                }
//                tryCashSchedule(stock,
//                        shouldBuyAmount * newestPrice - availableCash); //
//                // 均需要尝试调度现金, 因为现金已经不够了.
//            }
//        }
//    }
//
//    /**
//     * @param forBuyStock 因买入该股票现金不足而尝试调度现金
//     * @param expectCash  期望回笼的资金量, 不保证全部被回笼.
//     * @key3 尝试现金调度(昨日持仓股票尚未出现卖点时, 强制卖出部分股票以回笼现金),
//     * 当买入决策应当买入某股票时, 现金不足, 而尝试卖出部分其他股票, 回笼现金.
//     * @impl 应当对股票池中(所有)股票实现 schedule priority 逻辑, 计算该股票 被尝试资金调度时的优先级.
//     * 仅当 forBuyStock 优先级更高时, 尝试卖出 优先级更低的昨日持仓股票(可能多只以补足);
//     */
//    private void tryCashSchedule(String forBuyStock, double expectCash) {
//        if (RandomUtil.randomInt(1000) == 0) {
//            log.error("尝试资金调度,暂未实现");
//        }
//    }
//
//
//    public void actualBuy(String stock, double shouldBuyAmount, Integer todayAlreadyBuy, int maxCanBuyAmount,
//                          int actualBuyAmount) throws Exception {
//        // 卖出
//        log.warn("buy decision: 买点出现, {} -> 理论[{}] 此前已买参考[{}] 最大可买参考[{}] 订单实际[{}]",
//                stock,
//                shouldBuyAmount,
//                todayAlreadyBuy,
//                maxCanBuyAmount,
//                actualBuyAmount
//        );
//
//        Order order = OrderFactory.generateBuyOrderQuick(stock, actualBuyAmount, null, Order.PRIORITY_HIGH);
//        trader.putOrderToWaitExecute(order);
//
//        // todo: 这里一旦生成买单, 将视为全部成交, 加入到已经买入的部分
//        // 若最终成交失败, 一段时间(可设定)后check将失败, 需要手动处理!
//        todayStockHoldsAlreadyBuyMap.put(stock, todayAlreadyBuy + actualBuyAmount);
//    }
//
//
//    @Override
//    public void sellDecision() throws Exception {
//        Thread.sleep(1);
//        if (yesterdayStockHoldsBeSellMap.size() == 0) { // 开始决策后才会被正确初始化
//            initYesterdayHoldMapForSell(); // 初始化昨日持仓map
//            initActualHighSelled(); // 由此初始化今日已经实际卖出过的部分. 原本设定为0, 但可能程序重启, 导致该值应当用昨收-重启时最新的可用
//        }
//        flashActualHighSelledWhenNoSellOrderInQueue(); // 刷新实际已卖,分股票
//
//        for (String stock : yesterdayStockHoldsBeSellMap.keySet()) {
//            try { // 捕获异常
//                SecurityBeanEm stockBean = SecurityBeanEm.createStock(stock);
//                int amountsTotal = yesterdayStockHoldsBeSellMap.get(stock);
//
//                // 1. 读取前日收盘价
//                Double pre2ClosePrice;
//                try {
//                    //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
//                    pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(SecurityBeanEm.createStock(stock),
//                            pre2TradeDate,
//                            pre2TradeDate, "101", "qfq", 3, 2000).row(0).get(2).toString());
//                } catch (Exception e) {
//                    log.error("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
//                    e.printStackTrace();
//                    continue;
//                }
//
//                // 1.x: sell订单,单股票互斥: 在等待队列和check队列中查找所有sell订单, 判定其 stockCode参数是否为本stock,若存在则互斥跳过
//                if (hasMutualExclusionOfBuySellOrder(stock, "sell")) {
//                    // log.warn("Mutual Sell Order: 卖单互斥: {}", stock);
//                    continue;
//                }
//                // todo: 强制卖出 14:57
//
//
//                // 2. 判定当前是否是卖点?
//                if (!isSellPoint(stock, pre2ClosePrice, stockBean)) {
//                    // log.warn("当前股票非卖点 {}", stock);
//                    continue;
//                }
//
//
//                double indexPricePercentThatTime = getCurrentIndexChangePercent(stockBean.getMarket());
//                // 3.此刻是卖点, 计算以最新价格的 应当卖出的仓位 (相对于原始持仓)
//                Double cdfCalcPrice =
//                        FSBacktestOfLowBuyNextHighSell.BacktestTaskOfPerDay
//                                // stock_code,market,time_tick,price,vol,bs
//                                .calcEquivalenceCdfUsePriceOfHighSell(newPercent, indexPricePercentThatTime,
//                                        indexBelongThatTimePriceEnhanceArgHighSell);
//                // cdf使用 high 计算.  价格使用 sellPrice 计算
//                Double cdfOfPoint = virtualCdfAsPositionForHighSell(
//                        strategy.getLbHsSelector().getTicksOfHighSell(),
//                        strategy.getLbHsSelector().getWeightsOfHighSell(), cdfCalcPrice,
//                        tickGap);
//                // @key3: 高卖仓位折算 * 倍率
//                Double epochTotalPosition = Math.min(1.0, positionCalcKeyArgsOfCdfHighSell * cdfOfPoint);
//
//                double shouldSellAmountTotal = epochTotalPosition * amountsTotal;
//                int sellAlready = actualHighSelled.getOrDefault(stock, 0);
//                if (sellAlready < shouldSellAmountTotal) { // 四舍五入
//                    // 三项数据: 此刻卖点应当总卖出 / 原总持仓  --  [早已经成功卖出]
//                    int amount = (int) (NumberUtil
//                            .round((shouldSellAmountTotal - sellAlready) / 100, 0).doubleValue()) * 100;
//
//                    // 四舍五入价格. 100 整数
//                    if (amount + sellAlready > amountsTotal) {
//                        amount = (amountsTotal - sellAlready) / 100 * 100;
//                    }
//                    int available = trader.getAccountStates().getAvailableOfStock(stock);
//                    amount = Math.min(amount, available);
//                    if (amount < 100) {
////                        log.warn("sell decision: 卖点出现,但折算卖出数量<100,不执行卖出, {} -> {}/{} ; already [{}], actual [{}]",
////                                stock,
////                                shouldSellAmountTotal,
////                                amountsTotal,
////                                sellAlready, amount);
//                    } else {
//                        // 卖出
//                        log.warn("sell decision: 卖点出现, {} -> 理论[{}] 总仓[{}] 此前已卖参考[{}] 可用参考[{}] 订单实际[{}]",
//                                stock,
//                                shouldSellAmountTotal,
//                                amountsTotal,
//                                sellAlready,
//                                available,
//                                amount
//                        );
//
//                        Double price = null;
//                        String nowStr = DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN);
//                        boolean flag = nowStr.compareTo("09:25:00") > 0 && nowStr.compareTo("09:30:00") < 0;
//                        if (flag) {
//                            price = SecurityPool.getPriceLimitMap().get(stock).get(1); // 跌停价
//                        }
//                        Order order = OrderFactory.generateSellOrderQuick(stock, amount, price, Order.PRIORITY_HIGH);
//                        if (flag) {
//                            order.setAfterAuctionFirst(); // 设置为竞价后订单
//                        }
//                        trader.putOrderToWaitExecute(order);
//                        // todo: 这里一旦生成卖单, 将视为全部成交, 加入到已经卖出的部分
//                        // 若最终成交失败, 2分钟后check将失败, 需要手动处理!
//                        actualHighSelled.put(stock, amount + sellAlready);
//                    }
//                } else { //  新卖点,但没必要卖出更多.(多因为当前价格已经比上一次低, 导致仓位更低)
////                    log.warn("sell decision: 卖点出现,但早已卖出更多仓位,不执行卖出. {} -> {}/{} ; already [{}]", stock,
////                            shouldSellAmountTotal, amountsTotal, sellAlready);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * 当执行队列存在某股票sell订单时, 或者
//     * checking队列 存在执行成功的sell订单时(正在checking),
//     * 视为有该股票 sell 订单, 不刷新   ActualHighSelled 更新实际已经卖出.
//     * 其余的均刷新, 逻辑是 用 昨日收盘数量 - 当前可用
//     */
//    private void flashActualHighSelledWhenNoSellOrderInQueue() {
//        Set<String> hasSellOrderInQueueStocks = new HashSet<>();
//        for (Order order : Trader.getOrdersWaitForExecution()) { // 无需保证, 线程安全且迭代器安全
//            if ("sell".equals(order.getOrderType())) {
//                hasSellOrderInQueueStocks.add(order.getParams().get("stockCode").toString());
//            }
//        }
//
//        for (Order order : new ArrayList<>(
//                Trader.getOrdersWaitForCheckTransactionStatusMap().keySet())) { // 无需保证, 线程安全且迭代器安全
//            if ("sell".equals(order.getOrderType())) {
//                if (order.execSuccess()) { //正在等待checking, 将不加入
//                    hasSellOrderInQueueStocks.add(order.getParams().get("stockCode").toString());
//                } // 执行成功的
//            }
//        }
//
//        Map<String, Integer> map = trader.getAccountStates().getAvailableAmountOfStocksMap();
//        for (String stock : yesterdayStockHoldsBeSellMap.keySet()) {
//            if (!hasSellOrderInQueueStocks.contains(stock)) {
//                // 对于不存在卖单的, 刷新实际的已卖数量,而非使用 强制视为全部成交卖单机制 --> 实际刷新
//                try {
//
//                    actualHighSelled.put(stock, yesterdayStockHoldsBeSellMap.get(stock) - map.get(stock));
//                } catch (Exception e) {
//
//                }
//            }
//        }
//    }
//
//
//    /**
//     * 卖单互斥. 即在等待队列和check队列, 不能存在其他 相同stock的卖单!
//     *
//     * @param stock
//     * @return
//     */
//    private boolean hasMutualExclusionOfBuySellOrder(String stock, String buyOrsell) {
//        for (Order order : Trader.getOrdersWaitForExecution()) { // 无需保证, 线程安全且迭代器安全
//            if (buyOrsell.equals(order.getOrderType()) && order.getParams().get("stockCode").equals(stock)) {
//                return true;
//            }
//        }
//        for (Order order : new ArrayList<>(
//                Trader.getOrdersWaitForCheckTransactionStatusMap().keySet())) { // 无需保证, 线程安全且迭代器安全
//            if (buyOrsell.equals(order.getOrderType()) && order.getParams().get("stockCode").equals(stock)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    private boolean isBuyPoint(String stock, Double preClosePrice, SecurityBeanEm stockBean) throws Exception {
////        log.info("isbuypoint");
//        // 获取今日分时图
//        // 2022-01-20 11:30	17.24	17.22	17.24	17.21	10069	17340238.00 	0.17	-0.12	-0.02	0.01	000001	平安银行
//        DataFrame<Object> fsDf = FsFetcher.getFsData(stockBean);
//        final String nowStr = DateUtil.now().substring(0, DateUtil.now().length() - 3);
//        // 对 fsDf进行筛选, 筛选 不包含本分钟的. 因底层api会生成最新那一分钟的. 即 13:34:31, 分时图已包含 13:35, 我们需要 13:34及以前
//        if (fsDf.length() == 0) { // 不到 9:25
//            return false;
//        }
//        fsDf = dropAfter1Fs(fsDf, nowStr); // 将最后1行 fs记录去掉
//
//        // 计算连续下降数量
//        List<Double> closes = DataFrameS.getColAsDoubleList(fsDf, "收盘");
//        int continuousFall = 0;
//        for (int i = closes.size() - 1; i >= 1; i--) {
//            if (closes.get(i) <= closes.get(i - 1)) {
//                continuousFall++;
//            } else {
//                break;
//            }
//        }
//        if (DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN).compareTo("09:31:00") <= 0) {
//            continuousFall = Integer.MAX_VALUE; // 9:30:xx 只会有 9:31 的fs数据, 第一条fs图,此前视为连续上升.
//        }
//        if (continuousFall < continuousFallTickCountThreshold) { // 连续上升必须>=阈值
//            return false;
//        }
//
//        double lastFsClose = closes.get(closes.size() - 1); // 作为 计算最新一分钟 价格的基准, 计算涨跌
//        // 0	000010    	0          	09:15:09 	3.8  	177 	4
//        /*
//            stock_code,market,time_tick,price,vol,bs
//         */
//        DataFrame<Object> fsTransDf = FsTransactionFetcher.getFsTransData(SecurityBeanEm.createStock(stock));
//        // 最后的有记录的时间, 前推 60s
//        String lastFsTransTick = fsTransDf.get(fsTransDf.length() - 1, 2).toString(); // 15:00:00
//        String tickWithSecond0 =
//                DateUtil.parse(DateUtil.today() + " " + lastFsTransTick).offset(DateField.MINUTE, -1)
//                        .toString(DatePattern.NORM_TIME_PATTERN); // 有记录的最后的时间tick, 减去1分钟.
//        // 筛选最后一分钟记录
//        DataFrame<Object> fsLastMinute = getCurrentMinuteAll(fsTransDf, tickWithSecond0);
//
//
//        // 获取最新一分钟所有 成交记录. 价格列
//        if (fsLastMinute.size() == 0) { // 未能获取到最新一分钟数据,返回false
//            return false;
//        }
//        List<Double> pricesLastMinute = DataFrameS.getColAsDoubleList(fsLastMinute, 3);
//        if (pricesLastMinute.size() == 0) { // 没有数据再等等
//            return false;
//        }
//        newPercent = pricesLastMinute.get(pricesLastMinute.size() - 1) / preClosePrice - 1;
//        if (newPercent > execLowBuyThreshold) {
//            return false; // 价格必须足够低, 才可能买入
//        }
//        // 计算对比  lastFsClose, 多少上升, 多少下降?
//        int countOfHigher = 0; // 最新一分钟, 价格比上一分钟收盘更高 的数量
//        for (Double price : pricesLastMinute) {
//            if (price > lastFsClose) {
//                countOfHigher++;
//            }
//        }
//        if (((double) countOfHigher) / pricesLastMinute.size() > 0.5) { // 判定该分钟close有很大可能价格涨, 视为买点
//            return true;
//        }
//        return false;
//    }
//
//
//    /**
//     * 卖点判定机制.
//     * 1.集合竞价 9:25 后才可能出现卖点
//     * 2.集合竞价专属卖点! 唯一. 将于 9:25:xx (可能)产生, 该类订单 将在 otherRawMessages 属性中, 添加 afterAuctionFirst=True
//     * 注意对应的 sellCheck 逻辑也应当判定此类订单, 其等待成交的时长不应是固定1分钟, 而是 持续到例如 9:31为止!
//     * 见Order新增 isAfterAuctionFirst() 方法
//     * 3. 9:30-9:31之间, 连续上升条件视为达成, 且上个分时close, 取值为 9:32的open值, 其余逻辑同普通情况
//     * 4.一般情况:
//     * 判定前几分钟分时图 连续上升 n分钟, >=阈值
//     * 判定本分钟价格必须 < 上一分钟分时close.
//     * 取当前秒数, 若为0-9s, 返回false,不可能下单.
//     * 10-20s, 需要 上升成交记录数 /(上升+下降) >= 1.0; 时间越长, 该百分比限制越不严格, 直到 50-59s, 直接返回true.
//     * 见 highSellBeforehandThresholdMap 静态属性
//     *
//     * @return
//     * @see SettingsOfFSBacktest
//     */
//    public boolean isSellPoint(String stock, Double pre2ClosePrice, SecurityBeanEm stockBean) throws Exception {
//        // 获取今日分时图
//        // 2022-01-20 11:30	17.24	17.22	17.24	17.21	10069	17340238.00 	0.17	-0.12	-0.02	0.01	000001	平安银行
//        // 数据池获取分时图, 因 9:25:xx 后将有 9:31 单条记录. 因此lenth<0时, 直接返回false
//        DataFrame<Object> fsCurrent = FsFetcher.getFsData(stockBean);
//        if (fsCurrent.length() == 0) {
//            return false; // 9:25:0x 之前  // 一般是 9:25:02左右
//        }
//        String nowTime = DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN);
//        if (nowTime.compareTo("09:25:00") <= 0) {
//            return false; // 集合竞价之前
//        }
//        //System.out.println(nowTime); // 09:28:10
//        if (nowTime.compareTo("09:25:00") > 0 && nowTime.compareTo("09:30:00") < 0) {
//            // 集合竞价结束后的五分钟, 应当 集合竞价处理
//            return true; // 固定返回true, 将整个5分钟均视为卖点, 但因相同股票卖单互斥, 因此不会重复下单.
//        }
//
//        // 此时已经 9:30:0+ 开盘
//        final String nowStr = DateUtil.date().toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN); // 2022-01-20 11:30
//        if (fsCurrent.length() == 0) {
//            return false;
//        }
//        DataFrame<Object> fsDf = dropAfter1Fs(fsCurrent, nowStr);
//        // 对 fsDf进行筛选, 筛选 不包含本分钟的. 因底层api会生成最新那一分钟的. 即 13:34:31, 分时图已包含 13:35, 我们需要 13:34及以前
//        // 将最后1行 fs记录去掉
//
//        // 计算连续上涨数量
//        int continuousRaise = 0;
//        List<Double> closes = DataFrameS.getColAsDoubleList(fsDf, "收盘");
//        if (nowTime.compareTo("09:31:00") <= 0) {
//            continuousRaise = Integer.MAX_VALUE; // 9:30:xx 只会有 9:31 的fs数据, 第一条fs图,此前视为连续上升.
//        } else {
//            for (int i = closes.size() - 1; i >= 1; i--) {
//                if (closes.get(i) >= closes.get(i - 1)) {
//                    continuousRaise++;
//                } else {
//                    break;
//                }
//            }
//        }
//        if (continuousRaise < continuousRaiseTickCountThreshold) { // 连续上升必须>=阈值
//            return false;
//        }
//        double lastFsClose;
//        if (nowTime.compareTo("09:31:00") <= 0) {
//            // 9:30:xx
//            List<Double> temp = DataFrameS.getColAsDoubleList(fsDf, "开盘"); // 此时未筛选 9:31
//            lastFsClose = temp.get(temp.size() - 1); // 使用 第一条分时图的开盘, 视为 9:30那一刻的收盘!
//        } else {
//            lastFsClose = closes.get(closes.size() - 1); // 作为 计算最新一分钟 价格的基准, 计算涨跌
//        }
//
//        // 0	000010    	0          	09:15:09 	3.8  	177 	4
//        /*
//            stock_code,market,time_tick,price,vol,bs
//         */
//        DataFrame<Object> fsTransDf =
//                FsTransactionFetcher.getFsTransactionDatas()
//                        .get(SecurityBeanEm.createStock(stock));
//        String tickWithSecond0 = nowTime.substring(0, 5) + ":00"; // 本分钟.开始时刻
//        // 筛选fs图最近一分钟所有记录,
//        DataFrame<Object> fsLastMinute = getCurrentMinuteAll(fsTransDf, tickWithSecond0);
//
//        // 获取最新一分钟所有 成交记录. 价格列
//        if (fsLastMinute.size() <= 0) { // 未能获取到最新一分钟数据,返回false
//            return false;
//        }
//        List<Double> pricesLastMinute = DataFrameS.getColAsDoubleList(fsLastMinute, 3);
//        if (pricesLastMinute.size() <= 0) {
//            return false;
//        }
//        if (pricesLastMinute.get(pricesLastMinute.size() - 1) >= lastFsClose) {
//            return false; // 最新价格必须 < 上一分时收盘, 否则无视.
//        }
//
//        newPercent = pricesLastMinute.get(pricesLastMinute.size() - 1) / pre2ClosePrice - 1;
//        if (newPercent < execHighSellThreshold) {
//            return false; // 价格必须足够高, 才可能卖出
//        }
//
//
//        // 计算对比  lastFsClose, 多少上升, 多少下降? // 此时已经确定最新价格更低了
//        int countOfLower = 0; // 最新一分钟, 价格比上一分钟收盘更低 的数量
//        int countOfHigher = 0; // 最新一分钟, 价格比上一分钟收盘更低 的数量
//        for (Double price : pricesLastMinute) {
//            if (price < lastFsClose) {
//                countOfLower++;
//            } else if (price > lastFsClose) {
//                countOfHigher++;
//            }
//        }
//        if (countOfLower + countOfHigher == 0) {// 本分钟价格一点没变
//            return false;
//        }
//        int currentSecond = Integer.parseInt(nowTime.substring(6, 8)); // 秒数
//        if (currentSecond < 10) {
//            return false; // 0-9s, 不会出现卖点
//        } else if (currentSecond < 20) { // 前10s
//            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(20);
//        } else if (currentSecond < 30) { // 前10s
//            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(30);
//        } else if (currentSecond < 40) { // 前10s
//            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(40);
//        } else if (currentSecond < 50) { // 前10s
//            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(50);
//        } else {
//            return true;
//        }
//    }
//
//    private DataFrame<Object> getCurrentMinuteAll(DataFrame<Object> fsTransDf, String tickWithSecond0) {
//        int rowStart = 0;
//        for (int i = fsTransDf.length() - 1; i >= 0; i--) {
//            if (fsTransDf.row(i).get(2).toString().compareTo(tickWithSecond0) < 0) {// 找到第一个小
//                rowStart = i + 1;
//                break;
//            }
//        }
//        return fsTransDf.slice(rowStart, fsTransDf.length());
//    }
//
//    /**
//     * 因东方财富 securitylist 获取, xx:01s 就将获取到 xx+1 的分时图, 因此需要去掉最新一分钟的分时图.
//     * 例如在  14:32:32, 去掉 14:33,保留到14:32, 这里不使用slice[0,-1], 而倒序遍历判定. 若使用 select 将性能瓶颈
//     *
//     * @param fsDf
//     * @return
//     */
//    private DataFrame<Object> dropAfter1Fs(DataFrame<Object> fsDf, String nowStr) {
//        if (fsDf.length() == 1) { // 9:30:0x 期间仅自身.返回值不被使用,会有其他处理方式
//            return fsDf;
//        }
//        int endRow = fsDf.length();
//        for (int i = fsDf.length() - 1; i >= 0; i--) {
//            if (fsDf.row(i).get(0).toString().compareTo(nowStr) <= 0) {
//                endRow = i; // 第一个<=的, 将>的全部排除. 比select快
//                break;
//            }
//        }
//        return fsDf.slice(0, endRow + 1);
//    }
//
//    private double getCurrentIndexChangePercent(int market) throws Exception {
//        DataFrame<Object> dfTemp;
//        if (market == 0) { // 深证成指
//            // stock_code,market,time_tick,price,vol,bs
//            dfTemp = FsTransactionFetcher.getShenZhengChengZhiFs();
//            return Double.parseDouble(dfTemp.get(dfTemp.length() - 1, 3).toString()) / getSzczPreClose() - 1;
//        } else {
//            dfTemp =
//                    FsTransactionFetcher.getShangZhengZhiShuFs();
//            return Double.parseDouble(dfTemp.get(dfTemp.length() - 1, 3).toString()) / getSzzsPreClose() - 1;
//        }
////        Console.log(dfTemp);
//        // 分时成交则是3
//    }
//
//    public static Double getSzzsPreClose() {
//        return EmQuoteApi.getPreCloseOfIndexOrBK(SecurityBeanEm.SHANG_ZHENG_ZHI_SHU, 2000, 3, true); // 本身就将尝试使用缓存
//    }
//
//    public static Double getSzczPreClose() {
//        return EmQuoteApi.getPreCloseOfIndexOrBK(SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI, 2000, 3, true); // 本身就将尝试使用缓存
//    }
//
//    @Override
//    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {
//        checkOtherOrder(order, responses, orderType);
//    }
//
//
//    /*
//     *                     response = dict(state='success', description='卖出订单执行成功',
//     *                                     orderId=latest_order[headers.index("合同编号")],
//     *                                     stockCode=stockCode,
//     *                                     price=actual_price,
//     *                                     priceNote=priceNote,
//     *                                     amounts=amounts,
//     *                                     payload=confirm_info_dict,
//     *                                     notes="注意: 通过查询今日全部订单确定的订单成功id.",
//     *                                     rawOrder=order)
//     *
//     *           response = dict(state="fail", failReason=fail_reason.FAIL_ORDER_ARGS_ERROR,
//     *                         description='订单对象不合法[订单3参数设定错误]',
//     *                         payload=args_invalid_reason,  # list
//     *                         warning='请将订单修改为合法格式再行传递',
//     *                         rawOrder=order)
//     *
//     *           return dict(state="fail",
//     *                     failReason=fail_reason.FAIL_ORDER_AMOUNT_ZERO_MAYBE,
//     *                     description='数量被自动填充为0, 可能因可买卖数量不足1手',  # list
//     *                     warning='请检测可买卖数量是否不小于1手',
//     *                     rawOrder=order)
//     *
//     *                      response = dict(state='fail',
//     *                                 failReason=fail_reason.FAIL_ORDER_CONFIRM,
//     *                                 description=info_for_type_determine,  # list
//     *                                 rawOrder=order)  # 0.0
//     *              response = dict(state='success', description='卖出订单执行成功', orderId=orderId,
//     *                         stockCode=stockCode,
//     *                         price=actual_price,
//     *                         priceNote=priceNote,
//     *                         amounts=amounts,
//     *                         rawOrder=order)
//     *
//     *                     response = dict(state='fail',
//     *                         failReason=fail_reason.FAIL_ORDER_COMMIT,
//     *                         description=failReason,
//     *                         stockCode=stockCode,
//     *                         price=actual_price,
//     *                         priceNote=priceNote,
//     *                         amounts=amounts,
//     *                         rawOrder=order)
//     */
//
//    /**
//     * 对 sell order 的 check逻辑!
//     * Checker 将死循环监控所有订单, 因此, check逻辑并不需要保证将 order 移除队列到 finish队列
//     * 依据python api文档: 响应分为四大类状态. 其中:
//     * exception状态意味着无法处理. (通常直接进入最终失败队列)
//     * retrying意味着正在尝试(一半不会以此结束, 都会得到 success 或者 fail)
//     * success 以某种逻辑上的成功执行完成
//     * fail 以某种逻辑上的失败执行完成, 可尝试修改订单重试, 也可进入最终失败队列, 视情况 而定
//     * <p>
//     * --------- 响应分类
//     * dispatch 将可能发送 exception 状态响应(2种),重试的retrying 响应, 以及重试达到上限的 fail 响应
//     * sell 自身api 可能响应如上.
//     *
//     * @param order
//     * @param responses
//     * @param orderType
//     * @key2 对应卖单的check逻辑, 当python执行成功后, 订单进入check队列. (执行器已去执行其他任务)
//     */
//
//    @SneakyThrows
//    @Override
//    public void checkSellOrder(Order order, List<Response> responses, String orderType) {
//        Thread.sleep(1);
//        if (order.getLastLifePoint().getStatus() != Order.LifePointStatus.CHECKING) { // 首次的逻辑
//            order.addLifePoint(Order.LifePointStatus.CHECKING, "执行成功: 开始checking");
//            if (responses.size() > 1) {
//                log.warn("total retrying times maybe: 订单共计重试 {} 次", responses.size() - 1);
//            }
//        }
//
//        Response response = responses.get(responses.size() - 1);
//        String state = response.getString("state");
//        if ("success".equals(state)) {
//            if (order.getLastLifePoint().getStatus() != Order.LifePointStatus.CHECKING) { // 第一次
//                String notes = response.getString("notes");
//                if (notes != null && notes.contains("通过查询今日全部订单确定的订单成功id")) {
//                    log.warn("success noti: {}", notes);
//                } else {
//                    log.warn("success start checking: {} {}", order.getOrderType(), order.getParams());
//                }
//            }
//
//            if (orderAlreadyMatchAllBuyOrSell(order, response)) {
//                log.warn("checked ok: response [success]: 已全部成交: {} [{}]", order.getOrderType(), order.getParams());
//                order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功: 已全部成交");
//                trader.successFinishOrder(order, responses);
//            } else {
//                if (!order.isAfterAuctionFirst()) { // 非集合竞价后首订单, 依据check延时设定
//                    long checkTimeElapsed = DateUtil
//                            .between(order.getLastLifePoint().getGenerateTime(), DateUtil.date(),
//                                    DateUnit.MS, true); // checking 状态持续了多少 ms??
//                    if (checkTimeElapsed > maxCheckSellOrderTime) {
//                        orderHasNotMatchAll(order, responses);
//                    } // 否则继续checking
//                } else {
//                    if ("09:35:00".compareTo(DateUtil.date().toString(DatePattern.NORM_TIME_PATTERN)) < 0) {
//                        orderHasNotMatchAll(order, responses); // 集合竞价后首卖单超时时间固定
//                    }// 否则继续checking
//                }
//            }
//        } else if ("fail".equals(state)) {
//            // todo
//            log.error("checked: response[fail]: {} [{}]", order.getOrderType(), order.getParams());
//            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败 todo");
//            trader.failFinishOrder(order, responses);
//        } else if ("exception".equals(state)) { // 极快,省掉 CHECKING 生命周期
//            log.error("checked: response[exception]: {} [{}]", order.getOrderType(), order.getParams());
//            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行异常: state为exception");
//            trader.failFinishOrder(order, responses);
//        } else { // 未知响应状态.
//            log.error("checked: response[unknown]: 未知响应状态 {} [{}]", order.getOrderType(),
//                    order.getParams());
//            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行错误: 未知响应状态:" + state);
//            trader.failFinishOrder(order, responses);
//        }
//    }
//
//    private void orderHasNotMatchAll(Order order, List<Response> responses) {
//        log.error("checked: response [success] but check fail: {}ms 内未能全部成交 {} [{}]",
//                maxCheckSellOrderTime,
//                order.getOrderType(),
//                order.getParams());
//        order.addLifePoint(Order.LifePointStatus.CHECKED,
//                StrUtil.format("执行成功[check失败]: check失败, {}ms 内未能全部成交 {} [{}]", maxCheckSellOrderTime,
//                        order.getOrderType(),
//                        order.getParams()));
//        trader.failFinishOrder(order, responses);
//        try {
//            sendEmailSimple(
//                    StrUtil.format("Trader.Checker: 订单执行成功但{}ms内未能全部成交,注意手动确认!", maxCheckSellOrderTime),
//                    StrUtil.format("order 对象: \n{}", order.toStringPretty()), true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    // 成交时间	证券代码	证券名称	操作	成交数量	成交均价	成交金额	合同编号	成交编号
//    public boolean orderAlreadyMatchAllBuyOrSell(Order order, Response response) {
//        String orderId = response.getString("orderId");
//        DataFrame<Object> clinchsDf = AccountStates.getTodayClinchs();
//        List<String> ids = DataFrameS.getColAsStringList(clinchsDf, "合同编号");
//        List<Integer> amounts = DataFrameS.getColAsIntegerList(clinchsDf, "成交数量");
//        int clinchAmount = 0;
//        for (int i = 0; i < ids.size(); i++) {
//            if (ids.get(i).equals(orderId)) {// 同花顺订单id.
//                clinchAmount += amounts.get(i);
//            }
//        }
//        // 判定某 order , 当前是否已经全部成交. 买卖但逻辑相同, 均对合同编号筛选, 求和所有成交数量.
//        // 缺陷在于 trader.getAccountStates().getTodayClinchs()刷新及时性
//        if (clinchAmount >= Integer.parseInt(response.getString("amounts")) / 100 * 100) {
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
//        JSONObject response = responses.get(responses.size() - 1);
//        if ("success".equals(response.getString("state"))) {
//            log.info("执行成功: {}", order.getRawOrderId());
//            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行成功");
//        } else {
//            log.error("执行失败: {}", order.getRawOrderId());
//            log.info(JSONUtilS.parseArray(responses).toString());
//            order.addLifePoint(Order.LifePointStatus.CHECKED, "执行失败");
//        }
//        trader.successFinishOrder(order, responses);
//    }
//
//    private static final Log log = LogUtil.getLogger();
//}
