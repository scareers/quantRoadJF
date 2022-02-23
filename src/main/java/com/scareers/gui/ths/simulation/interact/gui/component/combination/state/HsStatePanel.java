package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.charts.ChartUtil;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

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
    JLabel chgPercentToPre2cLabel = new JLabel("最新/前2收涨跌幅");
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

    ChartPanel pdfChartPanel;
    ChartPanel cdfChartPanel;


    public HsStatePanel(HsState state, HsState preState) {
        this.state = state;
        this.preState = preState;
        this.setBackground(Color.white);
        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        this.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(Color.white);
        baseInfoPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        baseInfoPanel.setPreferredSize(new Dimension(350, 270));

        baseInfoPanel.setLayout(new GridLayout(15, 2, 1, 1));


        baseInfoPanel.add(stockCodeLabel);
        baseInfoPanel.add(stockCodeValueLabel);

        baseInfoPanel.add(stockNameLabel);
        baseInfoPanel.add(stockNameValueLabel);

        baseInfoPanel.add(factorLabel);
        baseInfoPanel.add(factorValueLabel);
        factorValueLabel.setBorder(BorderFactory.createLineBorder(Color.red, 1));


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


        pdfChartPanel =
                new ChartPanel(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getWeightsOfHighSell(),
                        this.state.getTicksOfHighSell(), false));
        pdfChartPanel.setDomainZoomable(false);
        pdfChartPanel.setPreferredSize(new Dimension(500, 270));

        cdfChartPanel =
                new ChartPanel(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getCdfOfHighSell(),
                        this.state.getTicksOfHighSell(), false));
        cdfChartPanel.setDomainZoomable(false);
        cdfChartPanel.setPreferredSize(new Dimension(500, 270));

        pdfChartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                // System.out.println("图中点击");
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {


//                JPanel src = (JPanel) event.getTrigger().getSource();
//                src.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//                JFreeChart chart = event.getChart();
//
//                if (chart == null) {
//                    return;
//                }
                CategoryItemEntity categoryItemEntity = (CategoryItemEntity) event.getEntity();
                if (categoryItemEntity == null) {
                    return;
                }
                CategoryDataset my = categoryItemEntity.getDataset();
//
//                Comparable sindex = ce.getColumnKey();
//                Comparable iindex = ce.getRowKey();
//
//
//                System.out.println("sindex = " + sindex);
//                System.out.println("iindex = " + iindex);
//                System.out.println("x = " + my.getValue(iindex, sindex));

                ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();
                // src.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                double cursorY = event.getTrigger().getY(); // 鼠标位置
                double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
                double minY = chartPanel.getScreenDataArea().getMinY();
                double percent = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
                CategoryPlot plot = (CategoryPlot) event.getChart().getPlot();
                ValueAxis rangeAxis = plot.getRangeAxis();
                Range range = rangeAxis.getRange();
                double markerY = range.getLowerBound() + range.getLength() * percent; // 同百分比取得marker位置


//                double markerValue = my.getValue(iindex, sindex).doubleValue();
                double markerValue = markerY;
                ValueMarker valuemarker = new ValueMarker(markerValue); // 水平线的值, 昨日收盘
                valuemarker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
                valuemarker.setPaint(Color.red); //线条颜色
                valuemarker.setStroke(new BasicStroke(1.0F)); //粗细
                valuemarker.setLabel("测试"); //线条上显示的文本
                valuemarker.setLabelFont(new Font("SansSerif", 0, 11)); //文本格式
                valuemarker.setLabelPaint(Color.red);
                valuemarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                valuemarker.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
                event.getChart().getCategoryPlot().addRangeMarker(valuemarker);

            }
        });

        this.update(); // 设置基本数据
        this.add(baseInfoPanel); // 左浮动
        this.add(pdfChartPanel); // 左浮动
        this.add(cdfChartPanel); // 左浮动


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

        pdfChartPanel.setChart(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getWeightsOfHighSell(),
                this.state.getTicksOfHighSell(), false));

        cdfChartPanel.setChart(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getCdfOfHighSell(),
                this.state.getTicksOfHighSell(), false));
    }

//    protected JFreeChart getChart() {
//        JFreeChart chart = ChartUtil.listOfDoubleAsLineChartSimple(this.state.getWeightsOfHighSell(),
//                this.state.getTicksOfHighSell(), false);
//
//
//    }
}
