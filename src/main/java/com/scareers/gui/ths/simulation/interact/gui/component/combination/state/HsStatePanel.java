package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.BkStateHsPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.IndexStateHsPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.OtherStateHsPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub.StockStateHsPanel;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
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
    public static int preferHeight = 300;

    // 十大状态属性
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

    // 基本数据展示panel --> 显示四类基本数据状态
    JPanel baseInfoPanel; // 基本数据的展示, 分为4大状态4大小组件, 如下4个Panel展示
    JPanel factorPanel; // 展示因子,在上. 4大状态在下
    JPanel stateBasePanel; // 展示因子,在上. 4大状态在下
    IndexStateHsPanel indexStateHsPanel; // 股票状态在左, 其余3在右垂直flow
    BkStateHsPanel bkStateHsPanel;
    StockStateHsPanel stockStateHsPanel;
    OtherStateHsPanel otherStateHsPanel; // 4 类状态展示组件, 放于 baseInfoPanel


    /*
     * 两图表Panel
     */

    // pdf图表
    ChartPanel pdfChartPanel; // 容器Panel
    XYSeries pdfXYSeries; // 折线图数据
    XYSeries pdfXYSeriesPre; // 前1状态的线数据序列y值折线图
    XYSeriesCollection pdfDataSet; // xy数据集, 可添加多条series, 显示多条折线
    JFreeChart pdfChart; // chart对象
    XYPlot pdfXYPlot; // plot对象
    ValueMarkerS markerXOfCurrentPriceInPdf; // 竖线
    ValueMarkerS markerYOfCurrentPriceInPdf; // 横线
    BasicStroke strokeOfCurrentPriceInPdfMarker = new BasicStroke(
            1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
            new float[]{10, 5, 5, 5}, 0); // 当前价格虚线. 数组为 实线长度-虚线长度 循环
    BasicStroke gridVertStrokeOfPdf = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
            new float[]{1, 1}, 0); // 纵向网格虚线,pdf
    BasicStroke gridHoriStrokeOfPdf = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f,
            null, 0); // 横向网格实线,pdf

    // cdf图表
    ChartPanel cdfChartPanel;


    public HsStatePanel(HsState state) {
        setAllStates(state); // 10大状态设置
        this.setBackground(COLOR_CHART_BG_EM);
        this.setLayout(new GridLayout(1, 3, 5, 0)); // 网格布局
        this.setPreferredSize(new Dimension(1024, preferHeight));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        initBaseInfoPanel(); // 基本信息展示
        initPdfChartPanel(); // pdf图表
        initCdfChartPanel(); // cdf图表

        this.update(); // 首次更新, 仅包含两大图表数据
        this.add(baseInfoPanel);  // 添加子组件
        this.add(pdfChartPanel);
        this.add(cdfChartPanel);
    }

    private void initCdfChartPanel() {
        cdfChartPanel = new ChartPanel(
                ChartUtil.listOfDoubleAsLineChartSimple(this.state.getStockStateHs().getCdfListOfHighSell(),
                        this.state.getStockStateHs().getTicksOfHighSell(), false));
        cdfChartPanel.setDomainZoomable(false);
        // cdfChartPanel.setPreferredSize(new Dimension(500, preferHeight));
    }

    /**
     * 基本信息展示panel:
     * 上下分: 上为 因子内容展示
     * 下部分,又左右分: 左为股票状态,
     * 右上下分三份, 竖直flow.
     */
    private void initBaseInfoPanel() {
        baseInfoPanel = new JPanel();
        baseInfoPanel.setBackground(COLOR_CHART_BG_EM);
        baseInfoPanel.setBorder(BorderFactory.createLineBorder(COLOR_TEXT_INACTIVATE_EM, 1));
        baseInfoPanel.setLayout(new BorderLayout()); // 2*2网格

        factorPanel = new JPanel();
        factorPanel.setBackground(COLOR_CHART_BG_EM);
        factorPanel.setLayout(new GridLayout(3, 1, 1, 1));
        String content1 = "null";
        String content2 = "null";
        String content3 = "null";
        if (state.getFactorInfluenceMe() != null) {
            content1 = state.getFactorInfluenceMe().getName();
            content2 = state.getFactorInfluenceMe().getNameCn();
            content3 = state.getFactorInfluenceMe().getDescription();

        }

        factorPanel.add(getDefaultJLabel(content1,Color.red));
        factorPanel.add(getDefaultJLabel(content2,Color.green));
        factorPanel.add(getDefaultJLabel(content3,Color.green));

        stateBasePanel = new JPanel();
        stateBasePanel.setLayout(new GridLayout(1, 2, 1, 1)); // 2*2网格

        indexStateHsPanel = new IndexStateHsPanel(this.indexStateHs, this.preIndexStateHs);
        bkStateHsPanel = new BkStateHsPanel(this.bkStateHs, this.preBkStateHs);
        stockStateHsPanel = new StockStateHsPanel(this.stockStateHs, this.preStockStateHs);
        otherStateHsPanel = new OtherStateHsPanel(this.otherStateHs, this.preOtherStateHs);

        // 其余3种状态在右, grid

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP)); // 上浮动的3项状态
        rightPanel.add(indexStateHsPanel);
        rightPanel.add(bkStateHsPanel);
        rightPanel.add(otherStateHsPanel);

//        baseInfoPanel.add(new JScrollPane(stockStateHsPanel)); // 股票数据多, 在左.
        stateBasePanel.add(stockStateHsPanel); // 股票数据多, 在左.
        stateBasePanel.add(rightPanel);


        baseInfoPanel.add(factorPanel, BorderLayout.NORTH);
        baseInfoPanel.add(stateBasePanel, BorderLayout.CENTER);
    }

    /**
     * 设置十大状态属性
     *
     * @param state
     */
    private void setAllStates(HsState state) {
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
    }

    /**
     * 核心更新数据方法. 更新属性, 更新子控件
     *
     * @param state
     */
    public void update(HsState state) {
        setAllStates(state);
        // 更新四大panel
        this.indexStateHsPanel.update(this.indexStateHs, this.preIndexStateHs);
        this.stockStateHsPanel.update(this.stockStateHs, this.preStockStateHs);

        this.update();
    }


    @Override
    protected void update() {
        // 更新两大pdf图表数据
        updatePdfChartPanel(); // 更新pdf图表. 并不重新实例化图表, 仅需要更新数据对象 XYSeries pdfXYSeries;
        cdfChartPanel
                .setChart(ChartUtil.listOfDoubleAsLineChartSimple(this.state.getStockStateHs().getCdfListOfHighSell(),
                        this.state.getStockStateHs().getTicksOfHighSell(), false));
    }


    /**
     * 初始化 pdf 图表相关.
     */
    protected void initPdfChartPanel() {
        initMarkerForCurrentPriceInPdf(); // 当前价格的 marker 对象初始化, 初始值Double.MIN_VALUE, 将不可见

        // 数据集合两大数据序列
        pdfXYSeries = new XYSeries("pdf");
        pdfXYSeriesPre = new XYSeries("pdfPre");
        updatePdfChartPanel(); // 将设置(或更新)两个y轴值, 且更新 当前价格双marker值

        pdfDataSet = new XYSeriesCollection();
        pdfDataSet.addSeries(pdfXYSeries);
        pdfDataSet.addSeries(pdfXYSeriesPre);

        // x轴
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setRange(new Range(-0.12, 0.12));
        xAxis.setTickUnit(new NumberTickUnit(0.02, ChartUtil.decimalFormatForPercent));
        xAxis.setAxisLinePaint(COLOR_CHART_AXIS_LINE_EM);
        xAxis.setTickLabelPaint(COLOR_TEXT_INACTIVATE_EM);

        // y轴对象
        NumberAxis yAxis = new NumberAxis("概率");
        yAxis.setLabelLocation(AxisLabelLocation.HIGH_END);
        yAxis.setTickUnit(new NumberTickUnit(0.005, ChartUtil.decimalFormatForPercent));
        yAxis.setAxisLinePaint(COLOR_CHART_AXIS_LINE_EM);
        yAxis.setTickLabelPaint(COLOR_TEXT_INACTIVATE_EM);

        // 渲染器: lines控制网格线, shapes控制点显示
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator()); // 提示框
        renderer.setURLGenerator(new StandardXYURLGenerator());
        renderer.setSeriesPaint(0, Color.yellow); // 两条y折线颜色
        renderer.setSeriesPaint(1, Color.white);

        // plot对象: 组装
        pdfXYPlot = new XYPlot(pdfDataSet, xAxis, yAxis, renderer);
        pdfXYPlot.setOrientation(PlotOrientation.VERTICAL);
        pdfXYPlot.setBackgroundPaint(COLOR_CHART_BG_EM); // 整体背景色
        pdfXYPlot.setDomainGridlinePaint(COLOR_CHART_GRID_LINE_EM); // 网格颜色
        pdfXYPlot.setRangeGridlinePaint(COLOR_CHART_GRID_LINE_EM);
        pdfXYPlot.setDomainGridlineStroke(gridVertStrokeOfPdf); // 虚线
        pdfXYPlot.setRangeGridlineStroke(gridHoriStrokeOfPdf); // 实线
        pdfXYPlot.addDomainMarker(markerXOfCurrentPriceInPdf); // 直接添加价格marker, 将会在价格无效时删除
        pdfXYPlot.addRangeMarker(markerYOfCurrentPriceInPdf);

        // chart对象
        pdfChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                pdfXYPlot, false);
        pdfChart.setBackgroundPaint(COLOR_CHART_BG_EM);

        // chartPanel对象
        pdfChartPanel = new ChartPanel(pdfChart);
        pdfChartPanel.addChartMouseListener(ChartUtil.getCrossLineListenerForSingleNumberXYPlot()); // 鼠标十字线 ****
        pdfChartPanel.setDomainZoomable(false);
        pdfChartPanel.setRangeZoomable(false);
        pdfChartPanel.setMouseZoomable(false); // 不可放大缩小
        // pdfChartPanel.setPreferredSize(new Dimension(500, preferHeight)); // 不设置默认size,应随布局自动调节
    }


    /**
     * pdf图表中,为当前价格创建 双marker, 且marker应实时更新, 放入 updatePdfChartPanel()逻辑
     */
    protected void initMarkerForCurrentPriceInPdf() {
        // 3.4: 同理, 创建y值 横向marker
        markerYOfCurrentPriceInPdf = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerYOfCurrentPriceInPdf.setType(ValueMarkerS.Type.CURRENT_PRICE); // 标志类型
        markerYOfCurrentPriceInPdf.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerYOfCurrentPriceInPdf.setPaint(COLOR_CURRENT_PRICE_MARKER); //线条颜色
        markerYOfCurrentPriceInPdf.setStroke(strokeOfCurrentPriceInPdfMarker); // 笔触控制粗细,虚线等
        markerYOfCurrentPriceInPdf.setLabelFont(new Font("Consolas", 0, 10)); //文本格式
        markerYOfCurrentPriceInPdf.setLabelPaint(Color.red);


        markerXOfCurrentPriceInPdf = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerXOfCurrentPriceInPdf.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerXOfCurrentPriceInPdf.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerXOfCurrentPriceInPdf.setPaint(COLOR_CURRENT_PRICE_MARKER); //线条颜色
        markerXOfCurrentPriceInPdf.setStroke(strokeOfCurrentPriceInPdfMarker); // 粗细
        markerXOfCurrentPriceInPdf.setLabelFont(new Font("Consolas", 0, 10)); //文本格式
        markerXOfCurrentPriceInPdf.setLabelPaint(Color.red);


        markerXOfCurrentPriceInPdf.setLabelAnchor(RectangleAnchor.TOP_RIGHT); // 上右
        markerXOfCurrentPriceInPdf.setLabelTextAnchor(TextAnchor.TOP_LEFT);

        markerYOfCurrentPriceInPdf.setLabelAnchor(RectangleAnchor.TOP_RIGHT); // 下右上
        markerYOfCurrentPriceInPdf.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
    }


    /**
     * 更新pdf图表
     */
    private void updatePdfChartPanel() {
        // 1. 最新pdf分布线,x/y轴. x轴可能更新
        List<Double> xs = this.stockStateHs.getStdTicksOfTodayChgP();
        List<Double> ys = this.stockStateHs.getStdPdfOfTodayChgP(xs);
        pdfXYSeries.clear();
        for (int i = 0; i < ys.size(); i++) {
            pdfXYSeries.add(xs.get(i), ys.get(i));
        }
        // 2. 尝试前一状态分布线, 逻辑相同
        if (this.preStockStateHs != null) {
            List<Double> xsPre = this.preStockStateHs.getStdTicksOfTodayChgP();
            List<Double> ysPre = this.preStockStateHs.getStdPdfOfTodayChgP(xs);
            pdfXYSeriesPre.clear();
            for (int i = 0; i < xsPre.size(); i++) {
                pdfXYSeriesPre.add(xsPre.get(i), ysPre.get(i));
            }
        }

        // 3.今日涨跌幅
        if (this.stockStateHs.getChgPToday() != null) {
            // 改变marker值
            double markerValueX = this.stockStateHs.getChgPToday(); // 当前涨跌幅
            markerXOfCurrentPriceInPdf.setValue(markerValueX);
            markerXOfCurrentPriceInPdf.setLabel(ChartUtil.decimalFormatForPercent.format(markerValueX)); //线条上显示的文本

            // 当前价格折算相对前日收盘涨跌幅, 计算 pdf
            double rawTick = this.stockStateHs.getChgPToPre2Close();
            Double markerValueY = StockStateHs.pdfHs(this.stockStateHs.getTicksOfHighSell(),
                    this.stockStateHs.getPdfListOfHighSell(),
                    rawTick);
            markerYOfCurrentPriceInPdf.setValue(markerValueY); // 设置横线值
            markerYOfCurrentPriceInPdf.setLabel(ChartUtil.decimalFormatForPercent.format(markerValueY));
        } else {
            // 不存在今日涨跌幅, 则尝试删除两个marker
            if (pdfXYPlot != null) {
                pdfXYPlot.removeDomainMarker(markerXOfCurrentPriceInPdf); // 价格无效时删除
                pdfXYPlot.removeRangeMarker(markerYOfCurrentPriceInPdf); // 价格无效时删除
            }
        }
    }



    /*
    常用规静态方法
     */

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

    /**
     * 当本状态某属性, 与上一状态相同属性不同时, 切换内容显示label 和名称label的颜色
     */
    public static void changeColorWhenTextDiff(JLabel titleLabel, JLabel contentLabel, Color newColor,
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

}
