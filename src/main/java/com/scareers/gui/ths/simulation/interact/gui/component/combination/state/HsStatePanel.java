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

    JLabel stockCodeLabel = new JLabel("股票代码");
    JLabel stockCodeValueLabel = new JLabel();
    JLabel stockNameLabel = new JLabel("股票名称");
    JLabel stockNameValueLabel = new JLabel();
    JLabel factorLabel = new JLabel("影响因子");
    JLabel factorValueLabel = new JLabel();
    JLabel pre2DateLabel = new JLabel("前2交易日");
    JLabel pre2DateValueLabel = new JLabel();
    JLabel pre2ClosePriceLabel = new JLabel("前2日收盘价");
    JLabel pre2ClosePriceValueLabel = new JLabel();
    JLabel isSellPointLabel = new JLabel("当前为卖点");
    JLabel isSellPointValueLabel = new JLabel();
    JLabel newPriceLabel = new JLabel("最新成交价格");
    JLabel newPriceValueLabel = new JLabel();
    JLabel chgPercentToPre2cLabel = new JLabel("最新价相对前2收盘涨跌幅");
    JLabel chgPercentToPre2cValueLabel = new JLabel();
    JLabel indexPercentLabel = new JLabel("对应大盘当前涨跌幅");
    JLabel indexPercentValueLabel = new JLabel();
    JLabel cdfProbabilityLabel = new JLabel("仓位cdf[原始]");
    JLabel cdfProbabilityValueLabel = new JLabel();
    JLabel cdfRateLabel = new JLabel("仓位卖出倍率");
    JLabel cdfRateValueLabel = new JLabel();
    JLabel totalPositionNormalizedLabel = new JLabel("理论标准化仓位值");
    JLabel totalPositionNormalizedValueLabel = new JLabel();
    JLabel totalAmountYcLabel = new JLabel("昨收总持仓数量");
    JLabel totalAmountYcValueLabel = new JLabel();
    JLabel actualAmountSelledLabel = new JLabel("今日已卖出数量");
    JLabel actualAmountSelledValueLabel = new JLabel();
    JLabel availabelAmountLabel = new JLabel("当前可卖数量");
    JLabel availabelAmountValueLabel = new JLabel("当前可卖数量");


    public HsStatePanel(HsState state, HsState preState) {
        this.state = state;
        this.preState = preState;
        this.setBackground(Color.white);
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setPreferredSize(new Dimension(500, 300));

        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(Color.white);

        baseInfoPanel.setLayout(new GridLayout(15, 2, 1, 1));


        baseInfoPanel.add(stockCodeLabel);
        baseInfoPanel.add(stockCodeValueLabel);

        baseInfoPanel.add(stockNameLabel);
        baseInfoPanel.add(stockNameValueLabel);

        baseInfoPanel.add(factorLabel);
        baseInfoPanel.add(factorValueLabel);

        baseInfoPanel.add(pre2DateLabel);
        baseInfoPanel.add(pre2DateValueLabel);

        baseInfoPanel.add(pre2ClosePriceLabel);
        baseInfoPanel.add(pre2ClosePriceValueLabel);

        baseInfoPanel.add(isSellPointLabel);
        baseInfoPanel.add(isSellPointValueLabel);


        baseInfoPanel.add(newPriceLabel);
        baseInfoPanel.add(newPriceValueLabel);

        baseInfoPanel.add(chgPercentToPre2cLabel);
        baseInfoPanel.add(chgPercentToPre2cValueLabel);

        baseInfoPanel.add(indexPercentLabel);
        baseInfoPanel.add(indexPercentValueLabel);


        baseInfoPanel.add(cdfProbabilityLabel);
        baseInfoPanel.add(cdfProbabilityValueLabel);

        baseInfoPanel.add(cdfRateLabel);
        baseInfoPanel.add(cdfRateValueLabel);


        baseInfoPanel.add(totalPositionNormalizedLabel);
        baseInfoPanel.add(totalPositionNormalizedValueLabel);


        baseInfoPanel.add(totalAmountYcLabel);
        baseInfoPanel.add(totalAmountYcValueLabel);


        baseInfoPanel.add(actualAmountSelledLabel);
        baseInfoPanel.add(actualAmountSelledValueLabel);

        baseInfoPanel.add(availabelAmountLabel);
        baseInfoPanel.add(availabelAmountValueLabel);

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
        stockCodeValueLabel.setText(state.getStockCode());
        stockNameValueLabel.setText(state.getBean().getName());
        factorValueLabel.setText(toStringCheckNull(state.getFactorInfluenceMe()));
        pre2DateValueLabel.setText(state.getPre2TradeDate());
        pre2ClosePriceValueLabel.setText(toStringCheckNull(state.getPre2ClosePrice()));
        isSellPointValueLabel.setText(toStringCheckNull(state.getSellPointCurrent()));
        newPriceValueLabel.setText(toStringCheckNull(state.getNewPriceTrans()));
        chgPercentToPre2cValueLabel.setText(toStringCheckNull(state.getNewPricePercentToPre2Close()));
        indexPercentValueLabel.setText(toStringCheckNull(state.getIndexPricePercentThatTime()));
        cdfProbabilityValueLabel.setText(toStringCheckNull(state.getCdfProbabilityOfCurrentPricePercent()));
        cdfRateValueLabel.setText(toStringCheckNull(state.getCdfRateForPosition()));
        totalPositionNormalizedValueLabel.setText(toStringCheckNull(state.getTotalPositionNormalized()));
        totalAmountYcValueLabel.setText(toStringCheckNull(state.getAmountsTotalYc()));
        actualAmountSelledValueLabel.setText(toStringCheckNull(state.getActualAmountHighSelled()));
        availabelAmountValueLabel.setText(toStringCheckNull(state.getAvailableAmountForHs()));
    }
}
