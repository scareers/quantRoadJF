package com.scareers.gui.ths.simulation.strategy.adapter.state.sub;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.scareers.annotations.ExitMaybe;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.strategy.adapter.state.GlobalStatesPool;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelectorManual;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;
import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getQuoteHistorySingle;

/**
 * description: 个股基本数据状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:41
 */
@Data
public class StockStateHs {
    public static void main(String[] args) throws Exception {
        GlobalStatesPool.initManualSelector();

        StockStateHs stockStateHs = new StockStateHs(SecurityBeanEm.createStock("002761"));
        Console.log(stockStateHs.preTradeDate);
        Console.log(stockStateHs.pre2TradeDate);
        Console.log(stockStateHs.preClosePrice);
        Console.log(stockStateHs.pre2ClosePrice);
    }

    // 传递
    protected SecurityBeanEm bean; // 股票bean
    // 自动
    protected String stockCode; // 股票代码, 自动获得

    // 自动
    // 前1交易日,前2交易日, 和这两天收盘价. 尝试用 标准的两个交易日读取, 若失败, 则修改两个日期为非标准的(这在停牌时发生)
    protected String preTradeDate; // 前1交易日
    protected String pre2TradeDate; // 前2交易日
    protected Double preClosePrice; // 前1交易日收盘价, 前复权
    protected Double pre2ClosePrice; // 前2交易日收盘价

    // 动态
    protected DataFrame<Object> fsData; // 当前分时图
    protected DataFrame<Object> fsTransData; // 当前分时成交数据
    protected Double newPriceTrans; // 最新成交价格, 从分时成交获取
    protected Double newPricePercentToPre2Close; // 相对于前2收盘价的涨跌幅

    // 动态
    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
    public Integer actualAmountHighSelled; // 今日已经卖出总
    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量

    /**
     * 高卖分布tick, 与pdf, cdf
     */
    // 初始化设置后可变, 高卖分布
    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected List<Double> pdfListOfHighSell; // 88数据
    protected List<Double> cdfListOfHighSell;

    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位 cdf
    protected Double cdfRateForPosition; // (cdf概率 * 的)倍率.
    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0

    public StockStateHs(SecurityBeanEm bean) {
        this.bean = bean;
        this.stockCode = bean.getSecCode(); // 不可变
        initTwoTradeDateAndClosePrice(); // 新对象
        initDistribution(); // 深复制
    }

    private void initDistribution() {
        ticksOfHighSell = ObjectUtil.cloneByStream(GlobalStatesPool.ticksOfHighSell);
        pdfListOfHighSell = ObjectUtil.cloneByStream(GlobalStatesPool.pdfListOfHighSell);
        cdfListOfHighSell = ObjectUtil.cloneByStream(GlobalStatesPool.cdfListOfHighSell);
    }

    /**
     * 尝试用 标准的两个交易日读取, 若失败, 则修改两个日期为非标准的(这在停牌时发生), 两个日期均可能比标准交易日更前
     * <p>
     *
     * @noti 极少数情况才会停牌, 因此这里用全日期k线, 更加健壮
     */
    @ExitMaybe
    private void initTwoTradeDateAndClosePrice() {
        preTradeDate = GlobalStatesPool.stdPreTradeDate;
        pre2TradeDate = GlobalStatesPool.stdPre2TradeDate;

        try {
            // 有停牌/新股前2天, 将会失败, 概率不高. 这种情况下降获取所有历史k线, 读取今日前的两个交易日期
            preClosePrice = getPreNDayClosePriceQfq(stockCode, preTradeDate);
            pre2ClosePrice = getPreNDayClosePriceQfq(stockCode, pre2TradeDate);
        } catch (Exception e) {
            DataFrame<Object> allDailyKLine = getQuoteHistorySingle(true, bean,
                    null,
                    null, "101", "qfq", 3, 3000);
            if (allDailyKLine == null) {
                log.error("initTwoTradeDateAndClosePrice: 获取所有历史k线失败");
                System.exit(1);
            }
            if (allDailyKLine.length() == 1 || allDailyKLine.length() == 0) {
                log.error("initTwoTradeDateAndClosePrice: 疑似新股, 所有日k线数据少于2");
                System.exit(1);
            }
            String today = DateUtil.today();
            List<String> twoDate = new ArrayList<>(2);
            for (int i = allDailyKLine.length() - 1; i >= 0; i--) {
                String date0 = allDailyKLine.get(i, "日期").toString();
                if (date0.compareTo(today) < 0) {
                    twoDate.add(date0);
                }
                if (twoDate.size() == 2) {
                    break;
                }
            }

            if (twoDate.size() < 2) {
                log.error("initTwoTradeDateAndClosePrice: 未能找到个股前1和2交易日期, 退出程序");
                System.exit(1);
            }
            preTradeDate = twoDate.get(0);
            pre2TradeDate = twoDate.get(1);

            try {
                preClosePrice = getPreNDayClosePriceQfq(stockCode, preTradeDate);
                pre2ClosePrice = getPreNDayClosePriceQfq(stockCode, pre2TradeDate);
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("initTwoTradeDateAndClosePrice: 最终未能找到个股前1和2交易日期, 退出程序");
                System.exit(1);
            }
        }

    }

    private static final Log log = LogUtil.getLogger();

    public static Double getPreNDayClosePriceQfq(String stock, String preNTradeDate) throws Exception {
        Double pre2ClosePrice = null;
        try {
            // 已经缓存
            //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
            pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(true, SecurityBeanEm.createStock(stock),
                    preNTradeDate,
                    preNTradeDate, "101", "qfq", 3, 2000).row(0).get(2).toString());
        } catch (Exception e) {
            log.error("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
            throw e;
        }
        return pre2ClosePrice;
    }
}
