package com.scareers.gui.ths.simulation.strategy.adapter;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.OrderFactory;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy;
import com.scareers.gui.ths.simulation.strategy.StrategyAdapter;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.List;

import static com.scareers.datasource.eastmoney.stock.StockApi.getPreNTradeDateStrict;
import static com.scareers.datasource.eastmoney.stock.StockApi.getQuoteHistorySingle;
import static com.scareers.gui.ths.simulation.strategy.LowBuyHighSellStrategy.STR_SEC_CODE;

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
    public static double indexBelongThatTimePriceEnhanceArgHighSell = -0.5;  // 指数当时价格加成
    public static double positionCalcKeyArgsOfCdfHighSell = 1.5; // cdf 倍率
    public static double execHighSellThreshold = -0.02; // 价格>此值(百分比)才考虑卖出
    public static int continuousRaiseTickCountThreshold = 1; // 连续上升n个,本分钟下降


    LowBuyHighSellStrategy strategy;
    Trader trader;
    String pre2Date; // yyyy-MM-dd

    public LowBuyHighSellStrategyAdapter(LowBuyHighSellStrategy strategy,
                                         Trader trader) throws Exception {
        this.strategy = strategy;
        this.trader = trader;
        pre2Date = getPreNTradeDateStrict(DateUtil.today());
    }

    @Override
    public void buyDecision() throws Exception {
        int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
        Thread.sleep(sleep * 2000);
        Order order = null;
        int type = RandomUtil.randomInt(22);
        if (type < 8) {
            order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGHEST);
        } else if (type < 16) {
            order = OrderFactory.generateSellOrderQuick("600090", 100, 1.2, Order.PRIORITY_HIGH);
        } else if (type < 18) {
            order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_HIGH);
        } else if (type < 20) {
            order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_HIGH);
        } else {
            order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
        }
        trader.putOrderToWaitExecute(order);
    }

    @Override
    public void sellDecision() throws Exception {
        DataFrame<Object> yesterdayStockHoldsBeSell = strategy.getYesterdayStockHoldsBeSell();
        // 证券代码	 证券名称	 股票余额	 可用余额	冻结数量	  成本价	   市价	       盈亏	盈亏比例(%)	   当日盈亏	当日盈亏比(%)	       市值	仓位占比(%)	交易市场	持股天数
        for (int i = 0; i < yesterdayStockHoldsBeSell.size(); i++) {
            List<Object> line = yesterdayStockHoldsBeSell.row(i);
            String stock = line.get(0).toString();
            int amountsTotal = Integer.parseInt(line.get(2).toString()); // 原始总持仓, 今日开卖
            double costPrice = Double.parseDouble(line.get(5).toString()); // 成本价.

            // 1. 读取前日收盘价
            Double pre2ClosePrice = 0.0;
            try {
                //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  股票代码	股票名称
                pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(stock, pre2Date, pre2Date,
                        "101", "1", 3,
                        false, 2000).row(0).get(2).toString());
            } catch (Exception e) {
                log.warn("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
                continue;
            }

            // 2. 判定当前是否是卖点?


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
    public boolean isSellPoint(String stock) throws Exception {
        // 获取今日分时图
        // 2022-01-20 11:30	17.24	17.22	17.24	17.21	10069	17340238.00 	0.17	-0.12	-0.02	0.01	000001	平安银行
        DataFrame<Object> fsDf = getQuoteHistorySingle(stock, null, null, // 日期区间无,默认今日最新
                "1", "1", 3,
                false, 2000, false); // 实时分时图
        DateTime lastFsTick = DateUtil.parse(fsDf.get(fsDf.length() - 1, 0).toString().strip()); // 该格式支持


        return false;
    }

    @Override
    public void checkBuyOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkSellOrder(Order order, List<Response> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    public void checkOtherOrder(Order order, List<Response> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
//            log.warn("待执行订单数量: {}", trader.getOrdersWaitForExecution().size());
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
