package com.scareers.gui.ths.simulation.strategy.adapter.state.sub;

import cn.hutool.core.date.DateUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import joinery.DataFrame;

import java.util.List;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;

/**
 * description: 个股基本数据状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:41
 */
public class StockStateHs {
    protected SecurityBeanEm bean;
    protected String stockCode; // 简单代码

    protected Double preClosePrice; // 前日收盘价, 主要用于(对比前2收盘价后)折算分布到今日涨跌幅
    protected Double pre2ClosePrice; // 前2天收盘价.

    protected String preTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1); // 该属性不变
    protected String pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2); // 该属性不变

    protected DataFrame<Object> fsData; // 当前分时图, 显示时显示最后一行.
    protected DataFrame<Object> fsTransData; // 当前分时成交数据

    protected Double newPriceTrans; // 最新成交价格, 从分时成交获取
    protected Double newPricePercentToPre2Close; // 相对于前2收盘价的close

    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
    public Integer actualAmountHighSelled; // 今日已经卖出总
    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量

    /**
     * 高卖分布tick, 与pdf, cdf
     */
    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected List<Double> pdfListOfHighSell; // 88数据
    protected List<Double> cdfListOfHighSell;
    protected Double tickGap = 0.005; // 分布tick, 暂时固定不变,

    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位cdf
    protected Double cdfRateForPosition; // cdf概率 * 的倍率.
    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0
}
