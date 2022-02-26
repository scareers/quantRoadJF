package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.bk.BkStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.index.IndexStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.other.OtherStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.StockStateHs;
import com.scareers.utils.charts.ChartUtil;
import com.scareers.utils.charts.ValueMarkerS;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLabelLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

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
    IndexStateHs indexStateHs; // 首次展示的对象, 当调用update时, 更新该属性
    BkStateHs bkStateHs; // 首次展示的对象, 当调用update时, 更新该属性
    StockStateHs stockStateHs;
    OtherStateHs otherStateHs;

    HsState preState; // 首次展示的对象, 当调用update时, 更新该属性
    IndexStateHs preIndexStateHs; // 首次展示的对象, 当调用update时, 更新该属性
    BkStateHs preBkStateHs; // 首次展示的对象, 当调用update时, 更新该属性
    StockStateHs preStockStateHs;
    OtherStateHs preOtherStateHs;


    JPanel baseInfoPanel; // 基本数据的展示, 分为4大状态4大小组件, 如下4个Panel展示
    IndexStateHsPanel indexStateHsPanel;
    BkStateHsPanel bkStateHsPanel;
    StockStateHsPanel stockStateHsPanel;
    OtherStateHsPanel otherStateHsPanel; // 4 类状态展示组件, 放于 baseInfoPanel


    ChartPanel pdfChartPanel;
    ChartPanel cdfChartPanel;

    /**
     * 全部JLabel默认样式
     *
     * @return
     */
    public static JLabel getDefaultJLabel(String content) {
        JLabel label = new JLabel(content);
        label.setForeground(COLOR_TEXT_INACTIVATE_EM);
        return label;
    }

    public static JLabel getDefaultJLabel(String content, Color color) {
        JLabel label = new JLabel(content);
        label.setForeground(color);
        return label;
    }

    public static JLabel getDefaultJLabel() {
        return getDefaultJLabel("");
    }

    public static JLabel getDefaultJLabel(Color color) {
        return getDefaultJLabel("", color);
    }


    public HsStatePanel(HsState state) {
        this.state = state;
        this.indexStateHs = this.state.getIndexStateHs();
        this.bkStateHs = this.state.getBkStateHs();
        this.stockStateHs = this.state.getStockStateHs();
        this.otherStateHs = this.state.getOtherStateHs();

        this.preState = state.getPreState();
        if (this.preState != null) {
            this.preIndexStateHs = this.preState.getIndexStateHs();
            this.preBkStateHs = this.preState.getBkStateHs();
            this.preStockStateHs = this.preState.getStockStateHs();
            this.preOtherStateHs = this.preState.getOtherStateHs();
        }


        this.setBackground(COLOR_CHART_BG_EM);
//        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.setLayout(new GridLayout(1, 3, 5, 0));

        // this.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(COLOR_CHART_BG_EM);
        baseInfoPanel.setBorder(BorderFactory.createLineBorder(COLOR_TEXT_INACTIVATE_EM, 1));
        baseInfoPanel.setPreferredSize(new Dimension(350, preferHeight));
        baseInfoPanel.setLayout(new GridLayout(2, 2, 1, 1));

        indexStateHsPanel = new IndexStateHsPanel(this.indexStateHs, this.preIndexStateHs);
        bkStateHsPanel = new BkStateHsPanel();
        stockStateHsPanel = new StockStateHsPanel(this.stockStateHs, this.preStockStateHs);
        otherStateHsPanel = new OtherStateHsPanel();

        baseInfoPanel.add(indexStateHsPanel);
        baseInfoPanel.add(bkStateHsPanel);
        baseInfoPanel.add(stockStateHsPanel);
        baseInfoPanel.add(otherStateHsPanel);


        initPdfChartPanel();
        cdfChartPanel = new ChartPanel(
                ChartUtil.listOfDoubleAsLineChartSimple(this.state.getStockStateHs().getCdfListOfHighSell(),
                        this.state.getStockStateHs().getTicksOfHighSell(), false));
        cdfChartPanel.setDomainZoomable(false);
        cdfChartPanel.setPreferredSize(new Dimension(500, preferHeight));

        this.update(); // 设置基本数据
        this.add(baseInfoPanel); // 左浮动
        this.add(pdfChartPanel); // 左浮动
        this.add(cdfChartPanel); // 左浮动
    }

    public void update(HsState state) {
        this.state = state;
        this.indexStateHs = this.state.getIndexStateHs();
        this.bkStateHs = this.state.getBkStateHs();
        this.stockStateHs = this.state.getStockStateHs();
        this.otherStateHs = this.state.getOtherStateHs();

        this.preState = state.getPreState();
        if (this.preState != null) {
            this.preIndexStateHs = this.preState.getIndexStateHs();
            this.preBkStateHs = this.preState.getBkStateHs();
            this.preStockStateHs = this.preState.getStockStateHs();
            this.preOtherStateHs = this.preState.getOtherStateHs();
        }

        this.indexStateHsPanel.update(this.indexStateHs, this.preIndexStateHs);
        this.stockStateHsPanel.update(this.stockStateHs, this.preStockStateHs);

        this.update();
    }

    /**
     * 当本状态某属性, 与上一状态相同属性不同时, 切换内容显示label 和名称label的颜色
     */
    protected static void changeColorWhenTextDiff(JLabel titleLabel, JLabel contentLabel, Color newColor,
                                                  Object preValue,
                                                  Object newValue) {
        if (preValue == null) {
            if (newValue != null) {
                titleLabel.setForeground(newColor);
                contentLabel.setForeground(newColor);
            }
        } else {
            if (!preValue.equals(newValue)) {
                titleLabel.setForeground(newColor);
                contentLabel.setForeground(newColor);
            }
        }

    }

    @Override
    protected void update() {
        // 初始化自动设置
        updatePdfChartPanel(); // 更新pdf图表. 并不重新实例化图表, 仅需要更新数据对象 XYSeries pdfXYSeries;

        cdfChartPanel
                .setChart(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getStockStateHs().getCdfListOfHighSell(),
                        this.state.getStockStateHs().getTicksOfHighSell(), false));
    }

    private void updatePdfChartPanel() {
        // 1. 最新pdf分布线
        List<Double> xs = this.state.getStockStateHs().getStdTicksOfTodayChgP();
        List<Double> ys = this.state.getStockStateHs().getStdPdfOfTodayChgP(xs);
        pdfXYSeries.clear();
        for (int i = 0; i < ys.size(); i++) {
            pdfXYSeries.add(xs.get(i), ys.get(i));
        }
        // 2. 尝试前一状态分布线
        if (this.preState != null) {
            List<Double> xsPre = this.preState.getStockStateHs().getStdTicksOfTodayChgP();
            List<Double> ysPre = this.preState.getStockStateHs().getStdPdfOfTodayChgP(xs);
            pdfXYSeriesPre.clear();
            for (int i = 0; i < xsPre.size(); i++) {
                pdfXYSeriesPre.add(xsPre.get(i), ysPre.get(i));
            }
        }

        if (this.state.getStockStateHs().getNewPriceTrans() != null && this.state.getStockStateHs()
                .getPreClosePrice() != null) {
            // 改变marker值
            double markerValueX = this.state.getStockStateHs().getNewPriceTrans() / this.state.getStockStateHs()
                    .getPreClosePrice() - 1; // 当前涨跌幅
            markerX.setValue(markerValueX);
            markerX.setLabel(ChartUtil.decimalFormatForPercent.format(markerValueX)); //线条上显示的文本

            double rawTick = this.state.getStockStateHs().getPreClosePrice() * (1 + markerValueX) / this.state
                    .getStockStateHs().getPre2ClosePrice() - 1;
            Double markerValueY = StockStateHs.pdfHs(this.state.getStockStateHs().getTicksOfHighSell(),
                    this.state.getStockStateHs().getPdfListOfHighSell(),
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
    XYSeries pdfXYSeriesPre; // 前1状态的线数据序列
    XYSeriesCollection pdfDataSet;
    JFreeChart pdfChart;
    XYPlot pdfXYPlot;


    protected void initPdfChartPanel() {
        initDomainMarkerForCurrentPrice(); // 当前价格的marker对象

        pdfXYSeries = new XYSeries("pdf");
        pdfXYSeriesPre = new XYSeries("pdfPre");
        updatePdfChartPanel();

        pdfDataSet = new XYSeriesCollection();
        pdfDataSet.addSeries(pdfXYSeries);
        pdfDataSet.addSeries(pdfXYSeriesPre);

//        NumberAxis xAxis = new NumberAxis("涨跌幅");
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setRange(new Range(-0.12, 0.12));
        xAxis.setTickUnit(new NumberTickUnit(0.02, ChartUtil.decimalFormatForPercent));
        xAxis.setAxisLinePaint(COLOR_CHART_AXIS_LINE_EM);
        xAxis.setTickLabelPaint(COLOR_TEXT_INACTIVATE_EM);

        NumberAxis yAxis = new NumberAxis("概率");
        yAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        yAxis.setTickUnit(new NumberTickUnit(0.005, ChartUtil.decimalFormatForPercent));
        yAxis.setAxisLinePaint(COLOR_CHART_AXIS_LINE_EM);
        yAxis.setTickLabelPaint(COLOR_TEXT_INACTIVATE_EM);


        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        pdfXYPlot = new XYPlot(pdfDataSet, xAxis, yAxis, renderer);
        pdfXYPlot.setOrientation(PlotOrientation.VERTICAL);
        pdfXYPlot.setBackgroundPaint(COLOR_CHART_BG_EM);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        renderer.setURLGenerator(new StandardXYURLGenerator());
        renderer.setSeriesPaint(0, Color.yellow);
        renderer.setSeriesPaint(1, Color.white); //
        pdfXYPlot.setDomainGridlinePaint(COLOR_CHART_GRID_LINE_EM);
        pdfXYPlot.setRangeGridlinePaint(COLOR_CHART_GRID_LINE_EM); // 网格颜色
        BasicStroke gridVertStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
                new float[]{1, 1}, 0); // 纵向网格虚线
        pdfXYPlot.setDomainGridlineStroke(gridVertStroke); // 虚线
        BasicStroke gridHoriStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
                null, 0); // 横向网格实线
        pdfXYPlot.setRangeGridlineStroke(gridHoriStroke); // 实线


        pdfXYPlot.addDomainMarker(markerX); // 直接添加价格marker, 将会在价格无效时删除
        pdfXYPlot.addRangeMarker(markerY); // 直接添加价格marker, 将会在价格无效时删除

        pdfChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                pdfXYPlot, false);
        pdfChart.setBackgroundPaint(COLOR_CHART_BG_EM);
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
        markerY.setPaint(Color.red); //线条颜色
        markerY.setStroke(basicStroke); //粗细
        markerY.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerY.setLabelPaint(Color.red);


        markerX = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerX.setPaint(Color.red); //线条颜色
        markerX.setStroke(basicStroke); //粗细
        markerX.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerX.setLabelPaint(Color.red);


        markerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);

        markerY.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerY.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

    }


}
