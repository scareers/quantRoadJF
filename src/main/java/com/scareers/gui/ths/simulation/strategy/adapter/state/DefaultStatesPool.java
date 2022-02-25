package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.date.DateUtil;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelector;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelectorManual;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;

/**
 * description: 全局唯一状态池. 一般初始化一些数据量不大的全局唯一变量. 各种属性一般不可变
 *
 * @author: admin
 * @date: 2022/2/25/025-19:33:07
 */
public class DefaultStatesPool {
    private DefaultStatesPool() {
    }

    /*
        参数默认值配置
    */
    public static Double cdfRateForPositionHs = 1.5; // 默认值

    public static CopyOnWriteArraySet<String> todaySuspendStocks; // 今日停牌股票集合
    public static String stdPreTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1); // 标准的上一交易日, 个股需要check
    public static String stdPre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2); // 标准的上2交易日

    static {
        initTodaySuspendStocks();
    }

    private static void initTodaySuspendStocks() {
        todaySuspendStocks = new CopyOnWriteArraySet<>(EmDataApi.getSuspensionStockCodes(DateUtil.today(), 3000, 10));
    }

    /*
    默认分布, 需要调用 initSelector 后, 自动设置默认分布
     */
    public static LbHsSelector selector; // 唯一选股器
    public static List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    public static List<Double> pdfListOfHighSell; // 88数据
    public static List<Double> cdfListOfHighSell;

    public static void initSelector(LbHsSelector selector0) {
        selector = selector0;
        ticksOfHighSell = selector.getTicksOfHighSell();
        pdfListOfHighSell = selector.getWeightsOfHighSell();
        cdfListOfHighSell = selector.getCdfOfHighSell();
    }

    /**
     * 使用默认的手动选股已做测试
     */
    public static void initManualSelector() {
        LbHsSelectorManual lbHsSelectorManual = new LbHsSelectorManual();
        initSelector(lbHsSelectorManual);
    }
}
