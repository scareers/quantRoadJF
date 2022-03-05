package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.DefaultStateArgsPoolHs;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.ths.simulation.TraderUtil;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelector;
import com.scareers.gui.ths.simulation.trader.AccountStates;
import com.scareers.gui.ths.simulation.trader.SettingsOfTrader;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.utils.CommonUtil.*;
import static com.scareers.utils.SqlUtil.execSql;


/**
 * description: 低买高卖策略
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
@Getter
@Setter
public class LowBuyHighSellStrategy extends Strategy {
    private static LowBuyHighSellStrategy INSTANCE;

    public static LowBuyHighSellStrategy getInstance(Trader trader, LbHsSelector lbHsSelector, String strategyName)
            throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new LowBuyHighSellStrategy(trader, lbHsSelector, strategyName);
        }
        return INSTANCE;
    }

    public static LowBuyHighSellStrategy getInstance() {
        return INSTANCE;
    }


    private static final Log log = LogUtil.getLogger();

    private DataFrame<Object> yesterdayStockHoldsBeSell; // 持仓数据二维数组, 含表头, 昨日收盘时.
    private ConcurrentHashMap<String, Double> yesterdayNineBaseFundsData; // 9项基本数据, 昨日收盘时

    private Trader trader;
    LbHsSelector lbHsSelector;
//    public ConcurrentHashMap<String, List<Double>> priceLimitMap = new ConcurrentHashMap<>(); // 股票池所有个股涨跌停,默认retry3次.股票池完成后初始化

    public LowBuyHighSellStrategy(Trader trader, LbHsSelector lbHsSelector, String strategyName
    ) throws Exception {
        Objects.requireNonNull(trader, "trader 不可null");
        this.trader = trader;
        this.lbHsSelector = lbHsSelector;
        this.adapter = new LowBuyHighSellStrategyAdapter(this, trader, 10); // 策略实际方法
        this.strategyName = strategyName; // 同super
        initSecurityPool(); // 构建器自动初始化股票池!

        bindSelf(); // trader绑定自身

        DefaultStateArgsPoolHs.initSelector(lbHsSelector); // @noti: 状态池设置. 到这里才执行了选股

        Console.log(lbHsSelector.getTicksOfHighSell());
        Console.log(lbHsSelector.getWeightsOfHighSell());
        Console.log(lbHsSelector.getCdfOfHighSell());
        Console.log(lbHsSelector.getTicksOfLowBuy());
        Console.log(lbHsSelector.getWeightsOfLowBuy());
        Console.log(lbHsSelector.getCdfOfLowBuy());
    }


    @Override
    protected void initSecurityPool() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        lbHsSelector.selectStock();

        List<String> yesterdayH = initYesterdayHolds();

        // 优化股票池添加逻辑
        List<SecurityBeanEm> selectBeans = SecurityBeanEm.createStockList(lbHsSelector.getSelectResults());
        SecurityPool.addToTodaySelectedStocks(selectBeans);// 今选
        List<SecurityBeanEm> yesterdayHoldBeans = SecurityBeanEm.createStockList(yesterdayH);
        SecurityPool.addToYesterdayHoldStocks(yesterdayHoldBeans); // 昨持

        selectBeans.forEach(SecurityBeanEm::initBksTheStockBelongTo); // 因此前懒加载, 此时需要主动加载板块信息
        yesterdayHoldBeans.forEach(SecurityBeanEm::initBksTheStockBelongTo);

        selectBeans.forEach(value -> SecurityPool.addToKeyBKs(value.getBkListBelongTo())); // 将相关板块加入keyBks
        yesterdayHoldBeans.forEach(value -> SecurityPool.addToKeyBKs(value.getBkListBelongTo()));

        SecurityPool.addToKeyIndexes(SecurityBeanEm.getTwoGlobalMarketIndexList());// 2大指数

        SecurityPool.flushPriceLimitMap();
        log.warn("stockPool added: 已将昨日收盘后持有股票和两大指数加入股票池! 新的股票池总大小: {}", SecurityPool.allSecuritySet.size());
        log.warn("finish init stockPool: 完成初始化股票池...");
    }

    @Override
    protected List<String> getSecurityTodaySelect() throws Exception {
        return lbHsSelector.getSelectResults();
    }

    public static Connection connOfYesterdayHoldsResult = ConnectionFactory.getConnLocalKlineForms();
    public static String tableNameOfYesterdayStockHoldsAndAccountsInfoBefore = "stock_yesterday_holds_and_account_info";

    /**
     * @return
     * @throws Exception
     * @noti: 逻辑上本函数通常在第一次获取账号信息后执行, 再次更新 this.stockPool
     */
    @Override
    protected List<String> initYesterdayHolds() throws Exception {
        execSql(StrUtil.format("create table if not exists\n {}" +
                        "(\n" +
                        "    trade_date                       varchar(128) null,\n" +
                        "    yesterday_holds                  longtext     null comment 'json, 昨日收盘持仓二维数组, 含表头',\n" +
                        "    yesterday_nine_account_fund_info longtext     null comment '9项基本数据',\n" +
                        "    record_time                      varchar(128) null comment '本条记录的时间, 需要注意一下. 字符串含毫秒'," +
                        " INDEX trade_date_index (trade_date ASC)" +
                        ")\n" +
                        "    comment '保存昨日收盘后, 持仓, 以及资金状态, 作为今日初始状态';\n", tableNameOfYesterdayStockHoldsAndAccountsInfoBefore)
                , connOfYesterdayHoldsResult);
        String today = DateUtil.today();
        String sql = StrUtil
                .format("select trade_date,yesterday_holds,yesterday_nine_account_fund_info,record_time from {} where " +
                                "trade_date='{}' limit 1",
                        tableNameOfYesterdayStockHoldsAndAccountsInfoBefore, today);
        DataFrame<Object> dfTemp = DataFrame.readSql(connOfYesterdayHoldsResult, sql);
        if (dfTemp.length() == 0) {
            log.warn("no record: 无昨日收盘持仓信息和账户资金数据 原始记录. 需要此刻初始化");
            // 等待首次信息更新, 本处不调用实际逻辑, 调用方保证 初始化完成
            waitUtil(trader.getAccountStates()::alreadyInitialized, 120 * 1000, 100, null, false);
            //AccountStates.nineBaseFundsData          // 当前的这两字段保存
            //AccountStates.currentHolds

            DataFrame<Object> dfSave = new DataFrame<Object>();
            dfSave.add("trade_date", Arrays.asList(today));
            dfSave.add("yesterday_holds",
                    Arrays.asList(
                            JSONUtilS.toJsonStr(DataFrameS.to2DList(AccountStates.currentHolds, true))));
            dfSave.add("yesterday_nine_account_fund_info",
                    Arrays.asList(JSONUtilS.toJsonStr(AccountStates.nineBaseFundsData)));
            dfSave.add("record_time", Arrays.asList(DateUtil.now()));
            DataFrameS.toSql(dfSave, tableNameOfYesterdayStockHoldsAndAccountsInfoBefore, connOfYesterdayHoldsResult,
                    "append", null);
            log.warn("save success: 保存成功: 昨日收盘持仓信息和账户资金数据原始记录");
            dfTemp = DataFrame.readSql(connOfYesterdayHoldsResult, sql); // 获取解析以便, 不使用直接赋值的方式
        }
        log.warn("recoed time: 昨日持仓与账户资金状况,获取时间为: {}", dfTemp.get(0, 3));
        yesterdayStockHoldsBeSell = TraderUtil.payloadArrayToDf(JSONUtilS.parseArray(dfTemp.get(0, 1).toString()));
        Map<String, Object> tempMap = JSONUtilS.parseObj(dfTemp.get(0, 2).toString());
        yesterdayNineBaseFundsData = new ConcurrentHashMap<>();
        tempMap.keySet().stream()
                .forEach(key -> yesterdayNineBaseFundsData.put(key, Double.valueOf(tempMap.get(key).toString())));
        log.warn("init success: 昨日持仓与账户资金状况, 初始化完成");
        List<String> stocksYesterdayHolds = DataFrameS.getColAsStringList(yesterdayStockHoldsBeSell,
                SettingsOfTrader.STR_SEC_CODE);
        log.warn("after yesterday close: 昨日收盘后持有股票数量: {} ;代码: {}", stocksYesterdayHolds.size(), stocksYesterdayHolds);
        log.warn("after yesterday close: 昨日收盘后账户9项基本资金数据:\n{}", yesterdayNineBaseFundsData);
        log.warn("after yesterday close: 昨日收盘后持有股票状态:\n{}", yesterdayStockHoldsBeSell);
        return stocksYesterdayHolds;
    }


    public void bindSelf() {
        this.trader.setStrategy(this);
    }
}
