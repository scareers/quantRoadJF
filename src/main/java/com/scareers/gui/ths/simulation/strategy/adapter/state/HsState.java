package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.HsStatePanel;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelector;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getPreNTradeDateStrict;

/**
 * description: 表示个股欲低买高卖时, 所有影响仓位的 参数集;
 *
 * @noti 当添加任意属性, 应当同时完成 首次初始化 和 copyFrom 两大方法对应字段的 设置/复制
 * @key3 注意当所有属性传递而来, 应当均 使用 "深复制" 语义. 否则将修改原始默认分布, 严重bug;
 * @key3 只可以使用工厂方法 创建默认状态.
 * @author: admin
 * @date: 2022/2/20/020-16:57:17
 */
@Getter
@Setter
@ToString
public class HsState {
    protected HsFactor factorInfluenceMe; //  被哪个因子影响而刷新?

    protected SecurityBeanEm bean; // 哪只股票的状态?
    //    stock, pre2ClosePrice, stockBean
    protected String stockCode; // 简单代码
    protected Double pre2ClosePrice; // 前2天收盘价.
    protected String pre2TradeDate = getPreNTradeDateStrict(DateUtil.today(), 2); // 该属性不变

    protected Boolean sellPointCurrent; // 当前是否为卖点 ?? 默认false
    protected DataFrame<Object> fsData; // 当前分时图, 显示时显示最后一行.
    protected DataFrame<Object> fsTransData; // 当前分时成交数据

    protected Double newPriceTrans; // 最新成交价格, 从分时成交获取
    protected Double newPricePercentToPre2Close; // 相对于前2收盘价的close

    protected Double indexPricePercentThatTime; // 对应大盘指数涨跌幅当前


    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
    public Integer actualAmountHighSelled; // 今日已经卖出总
    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量


    /**
     * 高卖分布tick, 与pdf, cdf
     */
    protected List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected List<Double> weightsOfHighSell; // 88数据
    protected List<Double> cdfOfHighSell;
    protected Double tickGap = 0.005; // 分布tick, 暂时固定不变,

    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位cdf
    protected Double cdfRateForPosition; // cdf概率 * 的倍率.
    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0

    private HsState() {
    }

    /*
     * 一些常用的影响方法, 例如 tick左右移(对应pdf右左移), pdf 变形, cdf重新计算 等
     */

    /**
     * 左右移动pdf, 相当于右左移动 ticks // + 变为 -
     * 当 distance为 正数, 表示右移, 为负数, 则左移
     *
     * @param distance 00
     */
    public void movePdf(Double distance) {
        ticksOfHighSell = ticksOfHighSell.stream().map(value -> value - distance).collect(Collectors.toList());
    }

    /*
    静态方法
     */

    /**
     * todo: 可考虑单股票仅维护单个状态对象, 创建对象池. 但是无法保证多线程之下 逻辑安全. 因此目前使用无脑深复制实现
     *
     * @param bean
     * @param selector
     * @return
     */
    public static HsState createDefaultHsState(SecurityBeanEm bean, LbHsSelector selector) {
        Assert.isTrue(bean.isStock());
        HsState state = new HsState();
        state.setBean(bean); // 唯一, 无需深复制
        // 股票代码null
        // 前2日收盘价null
        // pre2TradeDate 自动设置.
        state.setSellPointCurrent(false);
        // fsData为null
        // fsTransData为null
        // newPriceTrans为null
        // newPricePercentToPre2Close为null
        // indexPricePercentThatTime 默认0.0, 使得不至于报错失败. 可接受
        // cdfProbabilityOfCurrentPricePercent null
        // cdfRateForPosition null, 需要设置
        // totalPositionNormalized null, 需要设置
        // amountsTotalYc; // null
        // actualAmountHighSelled; // null
        // availableAmountForHs; // null

        state.setTicksOfHighSell(ObjectUtil.cloneByStream(selector.getTicksOfHighSell()));
        state.setWeightsOfHighSell(ObjectUtil.cloneByStream(selector.getWeightsOfHighSell()));
        state.setCdfOfHighSell(ObjectUtil.cloneByStream(selector.getCdfOfHighSell()));
        // tickGap == 0.005 固定不变
        return state;
    }

    public static HsState copyFrom(HsState oldState) {
        HsState state = new HsState();
        state.setBean(oldState.getBean()); // bean 不变, 不需要深复制
        state.setStockCode(oldState.getStockCode()); // 股票代码不变
        state.setPre2ClosePrice(ObjectUtil.cloneByStream(oldState.getPre2ClosePrice()));
        // pre2TradeDate 自动设置.
        state.setSellPointCurrent(ObjectUtil.cloneByStream(oldState.getSellPointCurrent()));
        state.setFsData(oldState.getFsData()); // 分时1M数据 df 不会改变. 不复制.
        state.setFsTransData(oldState.getFsTransData()); // 分时成交数据 df 不会改变. 不复制.
        state.setNewPriceTrans(ObjectUtil.cloneByStream(oldState.getNewPriceTrans()));
        state.setNewPricePercentToPre2Close(ObjectUtil.cloneByStream(oldState.getNewPricePercentToPre2Close()));
        state.setIndexPricePercentThatTime(ObjectUtil.cloneByStream(oldState.getIndexPricePercentThatTime()));
        state.setCdfProbabilityOfCurrentPricePercent(
                ObjectUtil.cloneByStream(oldState.getCdfProbabilityOfCurrentPricePercent()));
        state.setCdfRateForPosition(ObjectUtil.cloneByStream(oldState.getCdfRateForPosition()));
        state.setTotalPositionNormalized(ObjectUtil.cloneByStream(oldState.getTotalPositionNormalized()));
        state.setAmountsTotalYc(ObjectUtil.cloneByStream(oldState.getAmountsTotalYc()));
        state.setActualAmountHighSelled(ObjectUtil.cloneByStream(oldState.getActualAmountHighSelled()));
        state.setAvailableAmountForHs(ObjectUtil.cloneByStream(oldState.getAvailableAmountForHs()));

        state.setTicksOfHighSell(ObjectUtil.cloneByStream(oldState.getTicksOfHighSell()));
        state.setWeightsOfHighSell(ObjectUtil.cloneByStream(oldState.getWeightsOfHighSell()));
        state.setCdfOfHighSell(ObjectUtil.cloneByStream(oldState.getCdfOfHighSell()));
        // 完全复制过来
        // 当被因子影响后, 设置因子字段, 并以复制来的状态为基础更新.
        return state;
    }

//    /**
//     * 给定state, 创建其展示 Panel. 可给定preState, 以对比数据变化,展示不同效果
//     *
//     * @param state
//     * @param preState
//     * @return
//
//     */
//    public static HsStatePanel createPanelForHsState(HsState state, HsState preState) {
//        return new HsStatePanel(state, preState);
//    }

}
