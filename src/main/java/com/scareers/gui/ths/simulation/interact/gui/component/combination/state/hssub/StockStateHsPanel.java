package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.StockStateHs;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_CHART_BG_EM;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.HsStatePanel.changeColorWhenTextDiff;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.HsStatePanel.getDefaultJLabel;
import static com.scareers.utils.CommonUtil.toStringCheckNull;


/**
 * description: 高卖个股状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class StockStateHsPanel extends DisplayPanel {
    StockStateHs stockStateHs;
    StockStateHs preStockStateHs;


// 放到主baseInfo, 因为该属性在 HsState上面, 而非 StockStateHs上面
//    JLabel factorLabel = getDefaultJLabel("影响因子", Color.yellow);
//    JLabel factorValueLabel = getDefaultJLabel(Color.yellow);


    JLabel stockCodeLabel = getDefaultJLabel("股票代码");
    JLabel stockCodeValueLabel = getDefaultJLabel();
    JLabel stockNameLabel = getDefaultJLabel("股票名称");
    JLabel stockNameValueLabel = getDefaultJLabel();

    JLabel preDateLabel = getDefaultJLabel("前1交易日");
    JLabel preDateValueLabel = getDefaultJLabel();
    JLabel pre2DateLabel = getDefaultJLabel("前2交易日");
    JLabel pre2DateValueLabel = getDefaultJLabel();

    JLabel pre2ClosePriceLabel = getDefaultJLabel("前2日收盘价");
    JLabel preClosePriceLabel = getDefaultJLabel("前1日收盘价");
    JLabel preClosePriceValueLabel = getDefaultJLabel();
    JLabel pre2ClosePriceValueLabel = getDefaultJLabel();

    JLabel isSellPointLabel = getDefaultJLabel("当前为卖点");
    JLabel isSellPointValueLabel = getDefaultJLabel();

    JLabel newPriceLabel = getDefaultJLabel("最新成交价格");
    JLabel newPriceValueLabel = getDefaultJLabel();

    JLabel chgPercentToPre2cLabel = getDefaultJLabel("最新/前2收涨跌幅");
    JLabel chgPercentToPre2cValueLabel = getDefaultJLabel();


    JLabel cdfProbabilityLabel = getDefaultJLabel("仓位cdf[原始]");
    JLabel cdfProbabilityValueLabel = getDefaultJLabel();
    JLabel cdfRateLabel = getDefaultJLabel("仓位卖出倍率");
    JLabel cdfRateValueLabel = getDefaultJLabel();
    JLabel totalPositionNormalizedLabel = getDefaultJLabel("理论标准化仓位值");
    JLabel totalPositionNormalizedValueLabel = getDefaultJLabel();
    JLabel totalAmountYcLabel = getDefaultJLabel("昨收总持仓数量");
    JLabel totalAmountYcValueLabel = getDefaultJLabel();
    JLabel actualAmountSelledLabel = getDefaultJLabel("今日已卖出数量");
    JLabel actualAmountSelledValueLabel = getDefaultJLabel();
    JLabel availabelAmountLabel = getDefaultJLabel("当前可卖数量");
    JLabel availabelAmountValueLabel = getDefaultJLabel("当前可卖数量");


    public StockStateHsPanel(StockStateHs stockStateHs, StockStateHs preStockStateHs) {
        this.stockStateHs = stockStateHs;
        this.preStockStateHs = preStockStateHs;


        this.setBackground(COLOR_CHART_BG_EM);
        // this.setBorder(BorderFactory.createLineBorder(COLOR_TEXT_INACTIVATE_EM, 1));
        this.setLayout(new GridLayout(15, 2, 1, 1));

        this.add(stockCodeLabel);
        this.add(stockCodeValueLabel);

        this.add(stockNameLabel);
        this.add(stockNameValueLabel);

        this.add(preDateLabel);
        this.add(preDateValueLabel);
        this.add(pre2DateLabel);
        this.add(pre2DateValueLabel);

        this.add(preClosePriceLabel);
        this.add(preClosePriceValueLabel);
        this.add(pre2ClosePriceLabel);
        this.add(pre2ClosePriceValueLabel);

        this.add(isSellPointLabel);
        this.add(isSellPointValueLabel);

        this.add(newPriceLabel);
        this.add(newPriceValueLabel);

        this.add(chgPercentToPre2cLabel);
        this.add(chgPercentToPre2cValueLabel);

        this.add(cdfProbabilityLabel);
        this.add(cdfProbabilityValueLabel);

        this.add(cdfRateLabel);
        this.add(cdfRateValueLabel);

        this.add(totalPositionNormalizedLabel);
        this.add(totalPositionNormalizedValueLabel);

        this.add(totalAmountYcLabel);
        this.add(totalAmountYcValueLabel);

        this.add(actualAmountSelledLabel);
        this.add(actualAmountSelledValueLabel);

        this.add(availabelAmountLabel);
        this.add(availabelAmountValueLabel);

        this.update();
    }


    @Override
    protected void update() {

        // 初始化自动设置
        stockCodeValueLabel.setText(this.stockStateHs.getStockCode());
        stockNameValueLabel.setText(this.stockStateHs.getBean().getName());
        preDateValueLabel.setText(this.stockStateHs.getPreTradeDate());
        pre2DateValueLabel.setText(this.stockStateHs.getPre2TradeDate());
        preClosePriceValueLabel.setText(toStringCheckNull(this.stockStateHs.getPreClosePrice()));
        pre2ClosePriceValueLabel.setText(toStringCheckNull(this.stockStateHs.getPre2ClosePrice()));
        // 动态设置, 可对比显示不同颜色
        isSellPointValueLabel.setText(toStringCheckNull(this.stockStateHs.getSellPointCurrent()));
        newPriceValueLabel.setText(toStringCheckNull(this.stockStateHs.getNewPriceTrans()));
        chgPercentToPre2cValueLabel.setText(toStringCheckNull(this.stockStateHs.getNewPricePercentToPre2Close()));
        cdfProbabilityValueLabel
                .setText(toStringCheckNull(this.stockStateHs.getCdfProbabilityOfCurrentPricePercent()));
        cdfRateValueLabel.setText(toStringCheckNull(this.stockStateHs.getCdfRateForPosition()));
        totalPositionNormalizedValueLabel
                .setText(toStringCheckNull(this.stockStateHs.getTotalPositionNormalized()));
        totalAmountYcValueLabel.setText(toStringCheckNull(this.stockStateHs.getAmountsTotalYc()));
        actualAmountSelledValueLabel.setText(toStringCheckNull(this.stockStateHs.getActualAmountHighSelled()));
        availabelAmountValueLabel.setText(toStringCheckNull(this.stockStateHs.getAvailableAmountForHs()));
        if (this.preStockStateHs != null) {
            changeColorWhenTextDiff(isSellPointLabel, isSellPointValueLabel, Color.red,
                    this.stockStateHs.getSellPointCurrent(),
                    this.preStockStateHs.getSellPointCurrent());
            changeColorWhenTextDiff(newPriceLabel, newPriceValueLabel, Color.red,
                    this.stockStateHs.getNewPriceTrans(),
                    this.preStockStateHs.getNewPriceTrans());
            changeColorWhenTextDiff(chgPercentToPre2cLabel, chgPercentToPre2cValueLabel, Color.red,
                    this.stockStateHs.getNewPricePercentToPre2Close(),
                    this.preStockStateHs.getNewPricePercentToPre2Close());
            changeColorWhenTextDiff(cdfProbabilityLabel, cdfProbabilityValueLabel, Color.red,
                    this.stockStateHs.getCdfProbabilityOfCurrentPricePercent(),
                    this.preStockStateHs.getCdfProbabilityOfCurrentPricePercent());
            changeColorWhenTextDiff(cdfRateLabel, cdfRateValueLabel, Color.red,
                    this.stockStateHs.getCdfRateForPosition(),
                    this.preStockStateHs.getCdfRateForPosition());
            changeColorWhenTextDiff(totalPositionNormalizedLabel, totalPositionNormalizedValueLabel, Color.red,
                    this.stockStateHs.getTotalPositionNormalized(),
                    this.preStockStateHs.getTotalPositionNormalized());
            changeColorWhenTextDiff(totalAmountYcLabel, totalAmountYcValueLabel, Color.red,
                    this.stockStateHs.getAmountsTotalYc(),
                    this.preStockStateHs.getAmountsTotalYc());
            changeColorWhenTextDiff(actualAmountSelledLabel, actualAmountSelledValueLabel, Color.red,
                    this.stockStateHs.getActualAmountHighSelled(),
                    this.preStockStateHs.getActualAmountHighSelled());
            changeColorWhenTextDiff(availabelAmountLabel, availabelAmountValueLabel, Color.red,
                    this.stockStateHs.getAvailableAmountForHs(),
                    this.preStockStateHs.getAvailableAmountForHs());

        }

    }

    public void update(StockStateHs stockStateHs, StockStateHs preStockStateHs) {
        this.stockStateHs = stockStateHs;
        this.preStockStateHs = preStockStateHs;
        this.update();
    }
}
