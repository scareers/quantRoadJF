//package com.scareers.gui.ths.simulation.strategy.adapter.state;
//
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.lang.Assert;
//import cn.hutool.core.util.ObjectUtil;
//import com.scareers.datasource.eastmoney.SecurityBeanEm;
//import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
//import com.scareers.gui.ths.simulation.strategy.adapter.factor.base.SettingsOfBaseDataFactor;
//import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelector;
//import joinery.DataFrame;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.ToString;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;
//
///**
// * description: 表示个股欲低买高卖时, 所有影响仓位的 参数集;
// *
// * @noti 当添加任意属性, 应当同时完成 首次初始化 和 copyFrom 两大方法对应字段的 设置/复制
// * @key3 注意当所有属性传递而来, 应当均 使用 "深复制" 语义. 否则将修改原始默认分布, 严重bug;
// * @key3 只可以使用工厂方法 创建默认状态.
// * @author: admin
// * @date: 2022/2/20/020-16:57:17
// */
//@Getter
//@Setter
//@ToString
//public class HsState {
//    protected HsFactor factorInfluenceMe; //  被哪个因子影响而刷新?
//    protected HsState preState; // 维护前一状态对象.
//
//    protected SecurityBeanEm bean; // 哪只股票的状态?
//    //    stock, pre2ClosePrice, stockBean
//    protected String stockCode; // 简单代码
//    protected Double pre2ClosePrice; // 前2天收盘价.
//    protected Double preClosePrice; // 前日收盘价, 主要用于(对比前2收盘价后)折算分布到今日涨跌幅
//    protected String pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2); // 该属性不变
//    protected String preTradeDate = getPreNTradeDateStrict(DateUtil.today(), 1); // 该属性不变
//
//    protected Boolean sellPointCurrent; // 当前是否为卖点 ?? 默认false
//    protected DataFrame<Object> fsData; // 当前分时图, 显示时显示最后一行.
//    protected DataFrame<Object> fsTransData; // 当前分时成交数据
//
//    protected Double newPriceTrans; // 最新成交价格, 从分时成交获取
//    protected Double newPricePercentToPre2Close; // 相对于前2收盘价的close
//
//    protected Double indexPricePercentThatTime; // 对应大盘指数涨跌幅当前
//
//
//    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
//    public Integer actualAmountHighSelled; // 今日已经卖出总
//    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量
//
//
//    /**
//     * 高卖分布tick, 与pdf, cdf
//     */
//    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
//    protected List<Double> pdfListOfHighSell; // 88数据
//    protected List<Double> cdfListOfHighSell;
//    protected Double tickGap = 0.005; // 分布tick, 暂时固定不变,
//
//    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位cdf
//    protected Double cdfRateForPosition; // cdf概率 * 的倍率.
//    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0
//
//    private HsState() {
//    }
//
//    /*
//     * 一些常用的影响方法, 例如 tick左右移(对应pdf右左移), pdf 变形, cdf重新计算 等
//     */
//
//    /**
//     * 左右移动pdf, 相当于右左移动 ticks // + 变为 -
//     * 当 distance为 正数, 表示右移, 为负数, 则左移
//     * <p>
//     * // todo: 移动逻辑. 当distance为正,大盘向好, 应当右移减少仓位, 使得卖出仓位减少. 以求更高的价格卖出.
//     * // todo: @noti, pdf右移, 原 -0.1-> 0.03, 0-> 0.05, 0.1-> 0.03 右移则: 0->0.03, 0.1-> 0.05; 等价于 tick也右移!!!!!
//     * // todo: @noti: 因此 + distance; 大盘向好应当右移pdf分布, 等价于右移tick(而非相反)
//     *
//     * @param distance 00
//     */
//    public void movePdf(Double distance) {
//        if (distance == null) {
//            return;
//        }
//        ticksOfHighSell = ticksOfHighSell.stream().map(value -> value + distance).collect(Collectors.toList());
//    }
//
//    /**
//     * 将当前分布进行转换, 转换为 以今日涨跌幅为 tick; 本方法获取 以今日涨跌幅为标准的tick列表
//     * 因主板涨跌幅限制10%, 当前设置为 -11% - 11% 固定 47个tick, 0.005为距离
//     * 被 getStdPdfOfTodayChgP() 调用, 获取标准的pdf
//     *
//     * @return
//     * @see getStdPdfOfTodayChgP()
//     */
//    public List<Double> getStdTicksOfTodayChgP() {
//        ArrayList<Double> res = new ArrayList<>();
//        double start = -0.11;
//        for (int i = 0; i < 47; i++) {
//            res.add(start + i * 0.005);
//        }
//        return res;
//    }
//
//    /**
//     * 与 getStdTicksOfTodayChgP() 配合使用, 遍历该标准tick列表, 假设值为X,
//     * 以pre2收盘和 pre收盘作为基准, 当价格变为 preClose*(1+X)时, /pre2 -1, 即为原始tick中对应的值
//     * 以原始tick, 在原始pdf中, 求得对应概率 ! --> 即为以今日涨跌幅为标准的涨跌幅
//     *
//     * @return
//     */
//    public List<Double> getStdPdfOfTodayChgP(List<Double> stdTicksOfTodayChgP) {
//        List<Double> res = new ArrayList<>();
//        for (Double todayChgP : stdTicksOfTodayChgP) {
//            double rawTick = preClosePrice * (1 + todayChgP) / pre2ClosePrice - 1;
//            res.add(pdfHs(this.getTicksOfHighSell(), this.getPdfListOfHighSell(), rawTick));
//        }
//        return res;
//    }
//
//    public List<Double> getStdCdfOfTodayChgP() {
//        List<Double> todayChgPs = getStdTicksOfTodayChgP();
//        List<Double> res = new ArrayList<>();
//        for (Double todayChgP : todayChgPs) {
//            double rawTick = preClosePrice * (1 + todayChgP) / pre2ClosePrice - 1;
//            res.add(cdfHs(this.getTicksOfHighSell(), this.getCdfListOfHighSell(), rawTick));
//        }
//        return res;
//    }
//
//    /*
//    静态方法
//     */
//
//
//    /**
//     * todo: 可考虑单股票仅维护单个状态对象, 创建对象池. 但是无法保证多线程之下 逻辑安全. 因此目前使用无脑深复制实现
//     *
//     * @param bean
//     * @param selector
//     * @return
//     */
//    public static HsState createDefaultHsState(SecurityBeanEm bean, LbHsSelector selector) {
//        Assert.isTrue(bean.isStock());
//        HsState state = new HsState();
//        state.setBean(bean); // 唯一, 无需深复制
//        state.setStockCode(bean.getSecCode()); // 唯一, 无需深复制
//        state.setPre2ClosePrice( // 两个价格在此设置. 从 basedataFactor分离出来. 因初始状态展现pdf/cdf需要close价格
//                SettingsOfBaseDataFactor.getPreNDayClosePriceQfq(state.getStockCode(), state.getPre2TradeDate()));
//        state.setPreClosePrice(
//                SettingsOfBaseDataFactor.getPreNDayClosePriceQfq(state.getStockCode(), state.getPreTradeDate()));
//
//        // preState 为null, 自行设置
//        // 股票代码null
//        // 前2日收盘价null
//        // pre2TradeDate 自动设置.
//        // preClosePrice null
//        // sellPointCurrent null
//        // state.setSellPointCurrent(false);
//        // fsData为null
//        // fsTransData为null
//        // newPriceTrans为null
//        // newPricePercentToPre2Close为null
//        // indexPricePercentThatTime 默认0.0, 使得不至于报错失败. 可接受
//        // cdfProbabilityOfCurrentPricePercent null
//        // cdfRateForPosition null, 需要设置
//        // totalPositionNormalized null, 需要设置
//        // amountsTotalYc; // null
//        // actualAmountHighSelled; // null
//        // availableAmountForHs; // null
//
//        state.setTicksOfHighSell(ObjectUtil.cloneByStream(selector.getTicksOfHighSell()));
//        state.setPdfListOfHighSell(ObjectUtil.cloneByStream(selector.getWeightsOfHighSell()));
//        state.setCdfListOfHighSell(ObjectUtil.cloneByStream(selector.getCdfOfHighSell()));
//        // tickGap == 0.005 固定不变
//        return state;
//    }
//
//    public static HsState copyFrom(HsState oldState) {
//        HsState state = new HsState();
//        // preState 不copy ****** 自行设置 *******
//        state.setBean(oldState.getBean()); // bean 不变, 不需要深复制
//        state.setStockCode(oldState.getStockCode()); // 股票代码不变
//        state.setPre2ClosePrice(ObjectUtil.cloneByStream(oldState.getPre2ClosePrice()));
//        state.setPreClosePrice(ObjectUtil.cloneByStream(oldState.getPreClosePrice()));
//        // pre2TradeDate 自动设置.
//        state.setSellPointCurrent(ObjectUtil.cloneByStream(oldState.getSellPointCurrent()));
//        state.setFsData(oldState.getFsData()); // 分时1M数据 df 不会改变. 不复制.
//        state.setFsTransData(oldState.getFsTransData()); // 分时成交数据 df 不会改变. 不复制.
//        state.setNewPriceTrans(ObjectUtil.cloneByStream(oldState.getNewPriceTrans()));
//        state.setNewPricePercentToPre2Close(ObjectUtil.cloneByStream(oldState.getNewPricePercentToPre2Close()));
//        state.setIndexPricePercentThatTime(ObjectUtil.cloneByStream(oldState.getIndexPricePercentThatTime()));
//        state.setCdfProbabilityOfCurrentPricePercent(
//                ObjectUtil.cloneByStream(oldState.getCdfProbabilityOfCurrentPricePercent()));
//        state.setCdfRateForPosition(ObjectUtil.cloneByStream(oldState.getCdfRateForPosition()));
//        state.setTotalPositionNormalized(ObjectUtil.cloneByStream(oldState.getTotalPositionNormalized()));
//        state.setAmountsTotalYc(ObjectUtil.cloneByStream(oldState.getAmountsTotalYc()));
//        state.setActualAmountHighSelled(ObjectUtil.cloneByStream(oldState.getActualAmountHighSelled()));
//        state.setAvailableAmountForHs(ObjectUtil.cloneByStream(oldState.getAvailableAmountForHs()));
//
//        state.setTicksOfHighSell(ObjectUtil.cloneByStream(oldState.getTicksOfHighSell()));
//        state.setPdfListOfHighSell(ObjectUtil.cloneByStream(oldState.getPdfListOfHighSell()));
//        state.setCdfListOfHighSell(ObjectUtil.cloneByStream(oldState.getCdfListOfHighSell()));
//        // 完全复制过来
//        // 当被因子影响后, 设置因子字段, 并以复制来的状态为基础更新.
//        return state;
//    }
//
//
//    /**
//     * 改进的cdf算法, 因move变为了 tick移动, pdf/cdf本身不会变化.
//     * 遍历cdf, 同样使用直线折算方式计算. 参考 pdfHs
//     * 同样去除 tickGap, 改用两tick减法
//     * // 注意高卖tick由负数到正数, 递增.
//     * // 简单的直线之间一点y值得计算方法
//     *
//     * @param valuePercentOfLow
//     * @param weightsOfLow
//     * @param value
//     * @param tickGap
//     * @return
//     */
//    public static Double cdfHs(List<Double> tickList, List<Double> cdfList,
//                               Double chgValue) {
//        Assert.isTrue(tickList.size() == cdfList.size());
//        double pointCdf = 0.0;
//        if (chgValue < tickList.get(0)) {
//            return pointCdf; // 当给定值< 首个tick 返回0
//        }
//        if (chgValue > tickList.get(tickList.size() - 1)) {
//            return 1.0; // 当给定值> 最大tick, 返回1.0
//        }
//        if (chgValue.equals(tickList.get(0))) {
//            return cdfList.get(0); // 等于首个tick的情况. 以便后面逻辑全部使用 >前tick, <=后tick
//        }
//        int position = 0; // chgValue >此位置tick, <= 后一tick
//        for (int i = 1; i < tickList.size(); i++) {
//            if (chgValue > tickList.get(i)) {
//                position++; // 从第二值开始遍历, 若比后tick大, 则增加position位置. 在0,1两tick之间,依然符合逻辑
//            }
//        }
//
//        double preTick = tickList.get(position);
//        double preCdf = cdfList.get(position);
//        double backTick = tickList.get(position + 1);
//        double backCdf = cdfList.get(position + 1);
//        double gap = backTick - preTick;
//        pointCdf = preCdf + (backTick - preTick) / gap * (backCdf - preCdf);
//        return Math.min(pointCdf, 1.0);
//    }
//
//    /**
//     * 高卖pdf函数, 给定 原始tick以及pdf. 给定涨跌幅, 遍历tick, 计算相关点pdf
//     * 删除了 tickGap必须相等的要求. 直接用原始两点之间距离.
//     * 要求 原pdfList总和为 1.0;
//     * // 注意高卖tick由负数到正数, 递增.
//     * // 简单的直线之间一点y值得计算方法
//     *
//     * @param tickList
//     * @param pdfList
//     * @param value
//     * @param tickGap
//     * @return
//     */
//    public static Double pdfHs(List<Double> tickList, List<Double> pdfList,
//                               Double chgValue) {
//        Assert.isTrue(tickList.size() == pdfList.size());
//
//        double pointPdf = 0.0;
//        if (chgValue < tickList.get(0) || chgValue > tickList.get(tickList.size() - 1)) {
//            return pointPdf; // 当给定值< 首个tick 或者大于最后tick, 均返回0.0.
//        }
//        if (chgValue.equals(tickList.get(0))) {
//            return pdfList.get(0); // 等于首个tick的情况. 以便后面逻辑全部使用 >前tick, <=后tick
//        }
//        int position = 0; // chgValue >此位置tick, <= 后一tick
//        for (int i = 1; i < tickList.size(); i++) {
//            if (chgValue > tickList.get(i)) {
//                position++; // 从第二值开始遍历, 若比后tick大, 则增加position位置. 在0,1两tick之间,依然符合逻辑
//            }
//        }
//
//        double preTick = tickList.get(position);
//        double prePdf = pdfList.get(position);
//        double backTick = tickList.get(position + 1);
//        double backPdf = pdfList.get(position + 1);
//        double gap = backTick - preTick;
//        return prePdf + (chgValue - preTick) / gap * (backPdf - prePdf); // 简单的直线之间一点y值得计算方法
//    }
//
//}
