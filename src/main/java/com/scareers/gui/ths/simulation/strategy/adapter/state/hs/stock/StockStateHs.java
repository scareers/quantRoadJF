package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.scareers.annotations.ExitMaybe;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.gui.ths.simulation.annotation.ManualModify;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.CustomizeStateArgsPoolHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.DefaultStateArgsPoolHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getQuoteHistorySingle;

/**
 * description: 个股基本数据状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:41
 */
@Data
@ExitMaybe
public class StockStateHs implements Serializable {
    private static final long serialVersionUID = 851171112125L;

    public static void main(String[] args) throws Exception {
        DefaultStateArgsPoolHs.initManualSelector();

        StockStateHs stockStateHs = new StockStateHs(SecurityBeanEm.createStock("000001"));
        Console.log(stockStateHs.preTradeDate);
        Console.log(stockStateHs.pre2TradeDate);
        Console.log(stockStateHs.preClosePrice);
        Console.log(stockStateHs.pre2ClosePrice);
        Console.log(stockStateHs.cdfRateForPosition);
    }

    // 半自动: 将在HsState构造器中关联. 或者在 copyFrom中关联; 使得各种类state, 可通过parent,访问其他种类state
    private transient HsState parent;
    // 传递
    protected transient SecurityBeanEm bean; // 股票bean

    /*
    自动初始化属性, 几乎不会改变
     */

    // 自动, 不变
    protected transient String stockCode; // 股票代码, 自动计算
    // 自动, 不变: 前1交易日,前2交易日, 和这两天收盘价. 尝试用 标准的两个交易日读取, 若失败, 则修改两个日期为非标准的(这在停牌时发生)
    protected String preTradeDate; // 前1交易日
    protected String pre2TradeDate; // 前2交易日
    protected Double preClosePrice; // 前1交易日收盘价, 前复权
    protected Double pre2ClosePrice; // 前2交易日收盘价

    // 自动, 不变, 读取实时数据Fetcher
    protected transient DataFrame<Object> fsData; // 当前分时图
    protected transient DataFrame<Object> fsTransData; // 当前分时成交数据
    protected Double newPriceTrans; // 最新成交价格, 从分时成交获取
    protected Double newPricePercentToPre2Close; // 相对于前2收盘价的涨跌幅

    // 自动, 不变, 读取LowBuyHighSellStrategyAdapter 的实时账户状态
    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
    public Integer actualAmountHighSelled; // 今日已经卖出总
    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量


    /*
    动态属性: 分为初始化后可由因子改变, 以及并不初始化需要因子计算设置的
     */

    /*
    动态[初始化] -- 常在 DefaultStateArgsPoolHs 设置默认值; 或在 CustomizeStateArgsPoolHs 手动配置值.
     */

    // 初始化设置; 后可由各种因子变更高卖分布
    // 高卖分布tick, 与pdf, cdf
    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected List<Double> pdfListOfHighSell; // 88数据
    protected List<Double> cdfListOfHighSell;

    // cdf倍率可 [读取配置]或默认, 可手动修改配置. 可因子改变
    @ManualModify
    protected Double cdfRateForPosition; // (cdf概率 * 的)倍率.
    @ManualModify
    protected Double manualMoveDistanceFinally; // "手动平移高卖分布最终",可手动配置 最终再额外平移高卖分布 的量, 默认0.0

    /*
    动态[不初始化]
     */

    // 由卖点因子判定并设定
    protected Boolean sellPointCurrent; // 当前是否为卖点 ?? 默认false
    // 由仓位决策因子设定
    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位 cdf
    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0

    public StockStateHs(SecurityBeanEm bean) {
        this.bean = bean;
        this.stockCode = bean.getSecCode(); // 不可变
        initTwoTradeDateAndClosePrice(); // 新对象
        initDistribution(); // 深复制
        this.cdfRateForPosition = CustomizeStateArgsPoolHs.cdfRateForPositionHsMap.getOrDefault(this.stockCode,
                DefaultStateArgsPoolHs.cdfRateForPositionHs);
        this.manualMoveDistanceFinally =
                CustomizeStateArgsPoolHs.manualMoveDistanceFinallyMap.getOrDefault(this.stockCode,
                        DefaultStateArgsPoolHs.manualMoveDistanceFinallyDefault);

        fsData = FsFetcher.getFsData(this.bean);
        fsTransData = FsTransactionFetcher.getFsTransData(this.bean);
        fsTransData = FsTransactionFetcher.getFsTransData(this.bean);
        newPriceTrans = FsTransactionFetcher.getNewestPrice(this.bean);
        if (pre2ClosePrice != null && newPriceTrans != null) { // 需要前2收盘和当前最新成交两个价格不为null
            newPricePercentToPre2Close = newPriceTrans / pre2ClosePrice - 1;
        }
        amountsTotalYc = LowBuyHighSellStrategyAdapter.yesterdayStockHoldsBeSellMap.get(this.stockCode);
        actualAmountHighSelled = LowBuyHighSellStrategyAdapter.actualAmountHighSelledMap.get(this.stockCode);
        availableAmountForHs = LowBuyHighSellStrategyAdapter.availableAmountForHsMap.get(this.stockCode);

    }



    private void initDistribution() {
        ticksOfHighSell = ObjectUtil.cloneByStream(DefaultStateArgsPoolHs.ticksOfHighSell);
        pdfListOfHighSell = ObjectUtil.cloneByStream(DefaultStateArgsPoolHs.pdfListOfHighSell);
        cdfListOfHighSell = ObjectUtil.cloneByStream(DefaultStateArgsPoolHs.cdfListOfHighSell);
    }

    /**
     * 尝试用 标准的两个交易日读取, 若失败, 则修改两个日期为非标准的(这在停牌时发生), 两个日期均可能比标准交易日更前
     * <p>
     *
     * @noti 极少数情况才会停牌, 因此这里用全日期k线, 更加健壮
     */
    @ExitMaybe
    private void initTwoTradeDateAndClosePrice() {
        preTradeDate = DefaultStateArgsPoolHs.stdPreTradeDate;
        pre2TradeDate = DefaultStateArgsPoolHs.stdPre2TradeDate;

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


    /**
     * 改进的cdf算法, 因move变为了 tick移动, pdf/cdf本身不会变化.
     * 遍历cdf, 同样使用直线折算方式计算. 参考 pdfHs
     * 同样去除 tickGap, 改用两tick减法
     * // 注意高卖tick由负数到正数, 递增.
     * // 简单的直线之间一点y值得计算方法
     *
     * @param valuePercentOfLow
     * @param weightsOfLow
     * @param value
     * @param tickGap
     * @return
     */
    public static Double cdfHs(List<Double> tickList, List<Double> cdfList,
                               Double chgValue) {
        Assert.isTrue(tickList.size() == cdfList.size());
        double pointCdf = 0.0;
        if (chgValue < tickList.get(0)) {
            return pointCdf; // 当给定值< 首个tick 返回0
        }
        if (chgValue > tickList.get(tickList.size() - 1)) {
            return 1.0; // 当给定值> 最大tick, 返回1.0
        }
        if (chgValue.equals(tickList.get(0))) {
            return cdfList.get(0); // 等于首个tick的情况. 以便后面逻辑全部使用 >前tick, <=后tick
        }
        int position = 0; // chgValue >此位置tick, <= 后一tick
        for (int i = 1; i < tickList.size(); i++) {
            if (chgValue > tickList.get(i)) {
                position++; // 从第二值开始遍历, 若比后tick大, 则增加position位置. 在0,1两tick之间,依然符合逻辑
            }
        }

        double preTick = tickList.get(position);
        double preCdf = cdfList.get(position);
        double backTick = tickList.get(position + 1);
        double backCdf = cdfList.get(position + 1);
        double gap = backTick - preTick;
        pointCdf = preCdf + (backTick - preTick) / gap * (backCdf - preCdf);
        return Math.min(pointCdf, 1.0);
    }

    /**
     * 高卖pdf函数, 给定 原始tick以及pdf. 给定涨跌幅, 遍历tick, 计算相关点pdf
     * 删除了 tickGap必须相等的要求. 直接用原始两点之间距离.
     * 要求 原pdfList总和为 1.0;
     * // 注意高卖tick由负数到正数, 递增.
     * // 简单的直线之间一点y值得计算方法
     *
     * @param tickList
     * @param pdfList
     * @param value
     * @param tickGap
     * @return
     */
    public static Double pdfHs(List<Double> tickList, List<Double> pdfList,
                               Double chgValue) {
        Assert.isTrue(tickList.size() == pdfList.size());

        double pointPdf = 0.0;
        if (chgValue < tickList.get(0) || chgValue > tickList.get(tickList.size() - 1)) {
            return pointPdf; // 当给定值< 首个tick 或者大于最后tick, 均返回0.0.
        }
        if (chgValue.equals(tickList.get(0))) {
            return pdfList.get(0); // 等于首个tick的情况. 以便后面逻辑全部使用 >前tick, <=后tick
        }
        int position = 0; // chgValue >此位置tick, <= 后一tick
        for (int i = 1; i < tickList.size(); i++) {
            if (chgValue > tickList.get(i)) {
                position++; // 从第二值开始遍历, 若比后tick大, 则增加position位置. 在0,1两tick之间,依然符合逻辑
            }
        }

        double preTick = tickList.get(position);
        double prePdf = pdfList.get(position);
        double backTick = tickList.get(position + 1);
        double backPdf = pdfList.get(position + 1);
        double gap = backTick - preTick;
        return prePdf + (chgValue - preTick) / gap * (backPdf - prePdf); // 简单的直线之间一点y值得计算方法
    }

    /*
     * 一些常用的影响方法, 例如 tick左右移(对应pdf右左移), pdf 变形, cdf重新计算 等
     */

    /**
     * 左右移动分布, pdf与cdf同时移动, 相当于右左移动 ticks // + 变为 -
     * 当 distance为 正数, 表示右移, 为负数, 则左移
     * <p>
     * // todo: 移动逻辑. 当distance为正,大盘向好, 应当右移减少仓位, 使得卖出仓位减少. 以求更高的价格卖出.
     * // todo: @noti, pdf右移, 原 -0.1-> 0.03, 0-> 0.05, 0.1-> 0.03 右移则: 0->0.03, 0.1-> 0.05; 等价于 tick也右移!!!!!
     * // todo: @noti: 因此 + distance; 大盘向好应当右移pdf分布, 等价于右移tick(而非相反)
     *
     * @param distance 00
     */
    public void parallelMoveDistribution(Double distance) {
        if (distance == null) {
            return;
        }
        ticksOfHighSell = ticksOfHighSell.stream().map(value -> value + distance).collect(Collectors.toList());
    }

    /**
     * 将当前分布进行转换, 转换为 以今日涨跌幅为 tick; 本方法获取 以今日涨跌幅为标准的tick列表
     * 因主板涨跌幅限制10%, 当前设置为 -11% - 11% 固定 47个tick, 0.005为距离
     * 被 getStdPdfOfTodayChgP() 调用, 获取标准的pdf
     *
     * @return
     * @see getStdPdfOfTodayChgP()
     */
    public List<Double> getStdTicksOfTodayChgP() {
        ArrayList<Double> res = new ArrayList<>();
        double start = -0.11;
        for (int i = 0; i < 47; i++) {
            res.add(start + i * 0.005);
        }
        return res;
    }

    /**
     * 与 getStdTicksOfTodayChgP() 配合使用, 遍历该标准tick列表, 假设值为X,
     * 以pre2收盘和 pre收盘作为基准, 当价格变为 preClose*(1+X)时, /pre2 -1, 即为原始tick中对应的值
     * 以原始tick, 在原始pdf中, 求得对应概率 ! --> 即为以今日涨跌幅为标准的涨跌幅
     *
     * @return
     */
    public List<Double> getStdPdfOfTodayChgP(List<Double> stdTicksOfTodayChgP) {
        List<Double> res = new ArrayList<>();
        for (Double todayChgP : stdTicksOfTodayChgP) {
            double rawTick = preClosePrice * (1 + todayChgP) / pre2ClosePrice - 1;
            res.add(pdfHs(this.getTicksOfHighSell(), this.getPdfListOfHighSell(), rawTick));
        }
        return res;
    }

    public List<Double> getStdCdfOfTodayChgP() {
        List<Double> todayChgPs = getStdTicksOfTodayChgP();
        List<Double> res = new ArrayList<>();
        for (Double todayChgP : todayChgPs) {
            double rawTick = preClosePrice * (1 + todayChgP) / pre2ClosePrice - 1;
            res.add(cdfHs(this.getTicksOfHighSell(), this.getCdfListOfHighSell(), rawTick));
        }
        return res;
    }

    private static final Log log = LogUtil.getLogger();

    public static Double getPreNDayClosePriceQfq(String stock, String preNTradeDate) throws Exception {
        try {
            // 已经缓存
            //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
            return Double.valueOf(getQuoteHistorySingle(true, SecurityBeanEm.createStock(stock),
                    preNTradeDate,
                    preNTradeDate, "101", "qfq", 3, 2000).row(0).get(2).toString());
        } catch (Exception e) {
            log.error("skip: data get fail: 获取股票前日收盘价失败 {}", stock);
            throw e;
        }
    }
}
