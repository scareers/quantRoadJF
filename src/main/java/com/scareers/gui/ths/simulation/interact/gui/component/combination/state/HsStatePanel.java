package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.charts.ChartUtil;
import com.scareers.utils.charts.ValueMarkerS;
import org.jfree.chart.*;
import org.jfree.chart.axis.AxisLabelLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
    public static int preferHeight = 350;


    HsState state; // 首次展示的对象, 当调用update时, 更新该属性
    JPanel baseInfoPanel;

    JLabel stockCodeLabel = new JLabel("股票代码");
    JLabel stockCodeValueLabel = new JLabel();
    JLabel stockNameLabel = new JLabel("股票名称");
    JLabel stockNameValueLabel = new JLabel();
    JLabel factorLabel = new JLabel("影响因子");
    JLabel factorValueLabel = new JLabel();
    JLabel preDateLabel = new JLabel("前1交易日");
    JLabel preDateValueLabel = new JLabel();
    JLabel pre2DateLabel = new JLabel("前2交易日");
    JLabel pre2DateValueLabel = new JLabel();
    JLabel pre2ClosePriceLabel = new JLabel("前2日收盘价");
    JLabel preClosePriceLabel = new JLabel("前1日收盘价");
    JLabel preClosePriceValueLabel = new JLabel();
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


    public HsStatePanel(HsState state) {
        this.state = state;
        this.setBackground(Color.white);
        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        this.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(Color.white);
        baseInfoPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        baseInfoPanel.setPreferredSize(new Dimension(350, preferHeight));

        baseInfoPanel.setLayout(new GridLayout(17, 2, 1, 1));


        baseInfoPanel.add(stockCodeLabel);
        baseInfoPanel.add(stockCodeValueLabel);

        baseInfoPanel.add(stockNameLabel);
        baseInfoPanel.add(stockNameValueLabel);

        baseInfoPanel.add(factorLabel);
        baseInfoPanel.add(factorValueLabel);
        factorValueLabel.setBorder(BorderFactory.createLineBorder(Color.red, 1));


        baseInfoPanel.add(preDateLabel);
        baseInfoPanel.add(preDateValueLabel);
        baseInfoPanel.add(pre2DateLabel);
        baseInfoPanel.add(pre2DateValueLabel);

        baseInfoPanel.add(preClosePriceLabel);
        baseInfoPanel.add(preClosePriceValueLabel);
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


        initPdfChartPanel();
        cdfChartPanel = new ChartPanel(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getCdfListOfHighSell(),
                this.state.getTicksOfHighSell(), false));
        cdfChartPanel.setDomainZoomable(false);
        cdfChartPanel.setPreferredSize(new Dimension(500, preferHeight));

        this.update(); // 设置基本数据
        this.add(baseInfoPanel); // 左浮动
        this.add(pdfChartPanel); // 左浮动
        this.add(cdfChartPanel); // 左浮动
    }

    public void update(HsState state) {
        this.state = state;
        this.update();
    }

    @Override
    protected void update() {
        stockCodeValueLabel.setText(state.getStockCode());
        stockNameValueLabel.setText(state.getBean().getName());
        factorValueLabel.setText(toStringCheckNull(state.getFactorInfluenceMe()));
        preDateValueLabel.setText(state.getPreTradeDate());
        pre2DateValueLabel.setText(state.getPre2TradeDate());
        preClosePriceValueLabel.setText(toStringCheckNull(state.getPreClosePrice()));
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

        updatePdfChartPanel(); // 更新pdf图表. 并不重新实例化图表, 仅需要更新数据对象 XYSeries pdfXYSeries;

        cdfChartPanel.setChart(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getCdfListOfHighSell(),
                this.state.getTicksOfHighSell(), false));
    }

    private void updatePdfChartPanel() {
        List<Double> xs = this.state.getStdTicksOfTodayChgP();
        List<Double> ys = this.state.getStdPdfOfTodayChgP(xs);
        pdfXYSeries.clear();
        for (int i = 0; i < ys.size(); i++) {
            pdfXYSeries.add(xs.get(i), ys.get(i));
        }

        if (this.state.getNewPriceTrans() != null && this.state.getPreClosePrice() != null) {
            // 改变marker值
            double markerValueX = this.state.getNewPriceTrans() / this.state.getPreClosePrice() - 1; // 当前涨跌幅
            markerX.setValue(markerValueX);
            markerX.setLabel(ChartUtil.decimalFormatForPercent.format(markerValueX)); //线条上显示的文本

            double rawTick = this.state.getPreClosePrice() * (1 + markerValueX) / this.state.getPre2ClosePrice() - 1;
            Double markerValueY = HsState.pdfHs(this.state.getTicksOfHighSell(), this.state.getPdfListOfHighSell(),
                    rawTick);
            markerY.setValue(markerValueY);
            markerY.setLabel(ChartUtil.decimalFormatForPercent.format(markerValueY));
        } else {
            if (pdfXYPlot != null) {
                pdfXYPlot.removeDomainMarker(markerX); // 价格无效时删除
                pdfXYPlot.removeRangeMarker(markerY); // 价格无效时删除
            }
        }
    }

    XYSeries pdfXYSeries;
    XYSeriesCollection pdfDataSet;
    JFreeChart pdfChart;
    XYPlot pdfXYPlot;


    protected void initPdfChartPanel() {
        pdfXYSeries = new XYSeries("pdf");
        initDomainMarkerForCurrentPrice();

        updatePdfChartPanel();

        pdfDataSet = new XYSeriesCollection();
        pdfDataSet.addSeries(pdfXYSeries);

//        NumberAxis xAxis = new NumberAxis("涨跌幅");
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis("概率");
        yAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        pdfXYPlot = new XYPlot(pdfDataSet, xAxis, yAxis, renderer);
        pdfXYPlot.setOrientation(PlotOrientation.VERTICAL);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        renderer.setURLGenerator(new StandardXYURLGenerator());

        pdfXYPlot.addDomainMarker(markerX); // 直接添加价格marker, 将会在价格无效时删除
        pdfXYPlot.addRangeMarker(markerY); // 直接添加价格marker, 将会在价格无效时删除

        pdfChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                pdfXYPlot, false);
        pdfChart.setBackgroundPaint(ChartColor.WHITE);
        pdfChartPanel = new ChartPanel(pdfChart);
        pdfChartPanel.addChartMouseListener(ChartUtil.getCrossLineListenerForSingleXYPlot());
        pdfChartPanel.setDomainZoomable(false);
        pdfChartPanel.setRangeZoomable(false);
        pdfChartPanel.setMouseZoomable(false);
        pdfChartPanel.setPreferredSize(new Dimension(500, 270));
    }

    ValueMarkerS markerX; // 竖线
    ValueMarkerS markerY; // 横线

    /**
     * 为当前价格创建 marker, 且marker应实时更新, 放入 updatePdfChartPanel()逻辑
     */
    protected void initDomainMarkerForCurrentPrice() {
        BasicStroke basicStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
                new float[]{10, 5, 5, 5}, 0); // 虚线

        // 3.4: 同理, 创建y值 横向marker
        markerY = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerY.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER); // 标志类型
        markerY.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerY.setPaint(Color.green); //线条颜色
        markerY.setStroke(basicStroke); //粗细
        markerY.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerY.setLabelPaint(Color.red);


        markerX = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerX.setPaint(Color.green); //线条颜色
        markerX.setStroke(basicStroke); //粗细
        markerX.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerX.setLabelPaint(Color.red);


        markerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);

        markerY.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerY.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

    }


}
