package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

import static com.scareers.utils.CommonUtil.toStringCheckNull;

/**
 * description: 单个 HsState 展示面板
 *
 * @implement 为了更加方便精细控制, 对于最简单的信息展示, 使用 GridLayout + JLabel 简单展示.
 * @implement 对于pdf与cdf, 使用图片显示
 * @implement 对于分时数据, 分时成交, 仅显示最新几行数据. 或其他简单显示?
 * @author: admin
 * @date: 2022/2/22/022-17:21:15
 */
public class HsStatePanel extends DisplayPanel {
    HsState state; // 首次展示的对象, 当调用update时, 更新该属性
    HsState preState;

    JPanel baseInfoPanel;

    /*
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

    protected Double indexPricePercentThatTime = 0.0; // 对应大盘指数涨跌幅当前


    public Integer amountsTotalYc; // yesterday close; 总可卖出数量
    public Integer actualAmountHighSelled; // 今日已经卖出总
    public Integer availableAmountForHs; // 当前可用(可以卖出)的数量



    protected java.util.List<Double> ticksOfHighSell; // [-0.215, -0.21, -0.205, -0.2, -0.195, -0.19, -0.185, ..
    protected java.util.List<Double> weightsOfHighSell; // 88数据
    protected List<Double> cdfOfHighSell;
    protected Double tickGap = 0.005; // 分布tick, 暂时固定不变,

    protected Double cdfProbabilityOfCurrentPricePercent; // 仓位cdf
    protected Double cdfRateForPosition; // cdf概率 * 的倍率.
    protected Double totalPositionNormalized; // 理应的仓位总值, 标准化<=1.0
     */
    public HsStatePanel(HsState state, HsState preState) {
        this.state = state;
        this.preState = preState;
        this.setBackground(Color.white);

        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(Color.white);

        baseInfoPanel.setLayout(new GridLayout(15, 2, 1, 1));

        baseInfoPanel.add(new JLabel("股票代码"));
        baseInfoPanel.add(new JLabel(state.getStockCode()));
        baseInfoPanel.add(new JLabel("股票名称"));
        baseInfoPanel.add(new JLabel(state.getBean().getName()));
        baseInfoPanel.add(new JLabel("影响因子"));
        baseInfoPanel
                .add(new JLabel(toStringCheckNull(state.getFactorInfluenceMe())));
//        toStringCheckNull
        baseInfoPanel.add(new JLabel("前2交易日"));
        baseInfoPanel.add(new JLabel(state.getPre2TradeDate()));
        baseInfoPanel.add(new JLabel("前2日收盘价"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getPre2ClosePrice())));
        baseInfoPanel.add(new JLabel("当前为卖点"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getSellPointCurrent())));
        // todo: 分时图和分时成交数据
        //
        baseInfoPanel.add(new JLabel("最新成交价格"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getNewPriceTrans())));
        baseInfoPanel.add(new JLabel("最新价相对前2收盘涨跌幅"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getNewPricePercentToPre2Close())));
        baseInfoPanel.add(new JLabel("对应大盘当前涨跌幅"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getIndexPricePercentThatTime())));

        baseInfoPanel.add(new JLabel("仓位cdf[原始]"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getCdfProbabilityOfCurrentPricePercent())));
        baseInfoPanel.add(new JLabel("仓位卖出倍率"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getCdfRateForPosition())));
        baseInfoPanel.add(new JLabel("理论标准化仓位值"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getTotalPositionNormalized())));

        baseInfoPanel.add(new JLabel("昨收总持仓数量"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getAmountsTotalYc())));
        baseInfoPanel.add(new JLabel("今日已卖出数量"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getActualAmountHighSelled())));
        baseInfoPanel.add(new JLabel("当前可卖数量"));
        baseInfoPanel.add(new JLabel(toStringCheckNull(state.getAvailableAmountForHs())));

        this.add(baseInfoPanel); // 左浮动
        this.update();
    }

    public void update(HsState state, HsState preState) {
        this.state = state;
        this.preState = preState;
        this.update();
    }

    @Override
    protected void update() {
        System.out.println("HsStatePanel 内容已更新, 暂未实现");
    }
}
