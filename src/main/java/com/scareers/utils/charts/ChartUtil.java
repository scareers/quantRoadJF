package com.scareers.utils.charts;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 约定:给定df, 则 每一列, 视为同一y数据.! 即不同行可视为不同个体. 一列视为不同个体的相同属性
 * dataset.addValue(((Number) df.get(j, i)).doubleValue(), columns[i].toString(), indexes[j].toString());
 * 因此 第二参数为 列名称, 第三参数为行索引.
 * <p>
 * 目前实现折线图和条形图. 所有逻辑基本一模一样, 仅仅构造器唯一一个类型不一样
 *
 * @author: admin
 * @date: 2021/11/9  0009-20:37
 */
public class ChartUtil {
    private ChartUtil() {

    }

    public static void main(String[] args) throws Exception {
        demoOfXYPlotAndShiZiAndDynamicData();

//        String stock = "000001";
//        DataFrame<Object> fs1MToday = EmQuoteApi.getFs1MToday(SecurityBeanEm.createStock(stock), 3, 3000);
//        JFreeChart chart = createFs1MKLineOfEm(fs1MToday,
//                EmQuoteApi.getStockPreCloseAndTodayOpen(stock, 2000, 3, true).get(0), "000001", KLineYType.PERCENT);
//        showChartSimple(chart);


    }


    public static void demoOfXYPlotAndShiZiAndDynamicData() {
        final XYSeries datas = new XYSeries("Firefox");
        int amount = 20; // 点个数
        List<Double> ys = CommonUtil.range(amount).stream().map(value -> RandomUtil.randomDouble()).collect(
                Collectors.toList());
        List<Integer> xs = CommonUtil.range(amount);

        for (int i = 0; i < amount; i++) {
            datas.add(xs.get(i), ys.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(datas);


        NumberAxis xAxis = new NumberAxis("x");
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis("y");
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true) {

                    datas.addOrUpdate(amount + i, RandomUtil.randomDouble());
                    ThreadUtil.sleep(2000);
                    System.out.println("数据刷新");
                    i++;
                }
            }
        }, true);

        plot.setOrientation(PlotOrientation.VERTICAL);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        renderer.getBaseToolTipGenerator().generateToolTip(dataset, 0, 3);


        renderer.setURLGenerator(new StandardXYURLGenerator());

        JFreeChart chart = new JFreeChart("title", JFreeChart.DEFAULT_TITLE_FONT,
                plot, false);

        ApplicationFrame frame = new ApplicationFrame("temp");
        ChartPanel chartPanel = new ChartPanel(chart);
        // 大小
        chartPanel.setPreferredSize(new java.awt.Dimension(1200, 1000));

        chartPanel.addChartMouseListener(getCrossLineListenerForSingleXYPlot());
        chartPanel.setDisplayToolTips(true);
        chartPanel.setToolTipText("提示信息");
        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
    }

    /**
     * 给定double列表生成图表, 可显示.  柱状图3方法
     *
     * @param doubles0
     * @param xUseColValues
     * @throws IOException
     */
    public static JFreeChart listOfDoubleAsBarChartSimple(List<Double> doubles0,
                                                          List<Object> xUseColValues, boolean show)
            throws IOException {
        ArrayList<Object> doubles = new ArrayList<>(doubles0);
        DataFrame<Object> dataFrame = new DataFrame<>();
        dataFrame.add("temp_col", doubles);
        dataFrame.add("xLabels", xUseColValues);
        return dfAsBarChartSimple(dataFrame, "xLabels", show);
    }

    public static JFreeChart dfAsBarChartSimple(DataFrame<Object> df, boolean show)
            throws IOException {
        return dfAsBarChartSimple(df, null, show);
    }

    public static JFreeChart dfAsBarChartSimple(DataFrame<Object> df, String xUseCol, boolean show)
            throws IOException {
        CategoryDataset dataset = createDefaultCategoryDataset(df, xUseCol);
        JFreeChart barChart = ChartFactory
                .createBarChart(null, null, null, dataset, PlotOrientation.VERTICAL, true, true,
                        false);
        if (show) {
            showChartSimple(barChart);
        }
        return barChart;
    }

    /**
     * 对应的, 折线图3方法. 几乎一模一样
     *
     * @param doubles0
     * @param xUseColValues
     * @param show
     * @return
     * @throws IOException
     */
    public static JFreeChart listOfDoubleAsLineChartSimple(List<Double> doubles0,
                                                           List xUseColValues, boolean show) {
        ArrayList<Object> doubles = new ArrayList<>(doubles0);
        DataFrame<Object> dataFrame = new DataFrame<>();
        dataFrame.add("temp_col", doubles);
        dataFrame.add("xLabels", xUseColValues);
        return dfAsLineChartSimple(dataFrame, "xLabels", show);
    }

    public static JFreeChart dfAsLineChartSimple(DataFrame<Object> df, boolean show)
            throws IOException {
        return dfAsLineChartSimple(df, null, show);
    }

    public static JFreeChart dfAsLineChartSimple(DataFrame<Object> df, String xUseCol, boolean show) {
        CategoryDataset dataset = createDefaultCategoryDataset(df, xUseCol);
        JFreeChart barChart = ChartFactory
                .createLineChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, true,
                        false);
        if (show) {
            showChartSimple(barChart);
        }
        return barChart;
    }

    /**
     * K线纵轴类型,
     */
    public enum KLineYType {
        VALUE, // 直接显示价格值为y轴
        PERCENT // 转换为百分比显示y轴
    }

    /**
     * k线创建核心方法. 需要给定 5项数据+日期列名称. 可选给定一个基准值作为k线中横线,例如1分钟分时图的 昨日收盘价
     *
     * @param dataFrame       数据df
     * @param stdValue        昨收, 可null. 为null时, 价格图将位于最小最大+1% 之间. 不为null时, 则以此值为基准.上下平均作图
     * @param dateTimeColName 6大列名称. @noti: 日期列的格式需要 hutool 可以解析, 其他列需要能解析Double
     * @param openColName
     * @param closeColName
     * @param highColName
     * @param lowColName
     * @param volColName
     * @param kLineYType      y轴显示类型, 价格值或者百分比. 百分比时需要提供非0的 stdValue
     * @return
     */
    private static JFreeChart createKlineCore(DataFrame<Object> dataFrame, Double stdValue, String title,
                                              String dateTimeColName,
                                              String openColName,
                                              String closeColName,
                                              String highColName,
                                              String lowColName,
                                              String volColName,
                                              KLineYType kLineYType) {
        // 是否确定需要百分比化
        boolean percentizeFlag = kLineYType == KLineYType.PERCENT && stdValue != null && !stdValue.equals(0.0);
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, dateTimeColName);
        List<Double> opens = DataFrameS.getColAsDoubleList(dataFrame, openColName);
        List<Double> closes = DataFrameS.getColAsDoubleList(dataFrame, closeColName);
        List<Double> highs = DataFrameS.getColAsDoubleList(dataFrame, highColName);
        List<Double> lows = DataFrameS.getColAsDoubleList(dataFrame, lowColName);
        if (percentizeFlag) {
            opens = opens.stream().map(value -> value / stdValue - 1).collect(Collectors.toList());
            closes = closes.stream().map(value -> value / stdValue - 1).collect(Collectors.toList());
            highs = highs.stream().map(value -> value / stdValue - 1).collect(Collectors.toList());
            lows = lows.stream().map(value -> value / stdValue - 1).collect(Collectors.toList());
        }
        List<Double> vols = DataFrameS.getColAsDoubleList(dataFrame, volColName);

        double highValue = CommonUtil.maxOfListDouble(highs); // 价格最高
        double minValue = CommonUtil.minOfListDouble(lows); // 价格最低
        double high2Value = CommonUtil.maxOfListDouble(vols); // 成交量最高
        double min2Value = CommonUtil.minOfListDouble(vols);//成交量最低

        OHLCSeries series = new OHLCSeries(""); // 开，高，低，收, 四项数据
        // 1.4项价格数据序列
        for (int i = 0; i < timeTicks.size(); i++) { // 以时间遍历, 添加四项价格
            series.add(new Minute(timeTicks.get(i)), opens.get(i), highs.get(i), lows.get(i),
                    closes.get(i));
        }
        final OHLCSeriesCollection seriesCollection = new OHLCSeriesCollection();//保留K线数据的数据集，必须申明为final，后面要在匿名内部类里面用到
        seriesCollection.addSeries(series);

        // 2.成交量序列
        TimeSeries series2 = new TimeSeries("");// 对应时间成交量数据
        for (int i = 0; i < timeTicks.size(); i++) { // 以时间遍历, 添加四项价格
            series2.add(new Minute(timeTicks.get(i)), vols.get(i));
        }
        TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(); //保留成交量数据的集合
        timeSeriesCollection.addSeries(series2);

        final CandlestickRenderer candlestickRender = new CandlestickRenderer(); // 设置K线图的画图器，必须申明为final，后面要在匿名内部类里面用到
        candlestickRender.setUseOutlinePaint(true); //设置是否使用自定义的边框线，程序自带的边框线的颜色不符合中国股票市场的习惯
        candlestickRender.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);//设置如何对K线图的宽度进行设定
        candlestickRender.setAutoWidthGap(0.001);//设置各个K线图之间的间隔
        candlestickRender.setUpPaint(Color.RED);//设置股票上涨的K线图颜色
        candlestickRender.setDownPaint(Color.GREEN);//设置股票下跌的K线图颜色

        DateAxis x1Axis = new DateAxis();//设置x轴，也就是时间轴
        x1Axis.setAutoRange(false);//设置是否采用自动设置时间范围
        try {
            x1Axis.setRange(timeTicks.get(0),
                    timeTicks.get(timeTicks.size() - 1));//设置时间范围，注意时间的最大值要比已有的时间最大值要多一天
        } catch (Exception e) {
            x1Axis.setAutoRange(true);
            e.printStackTrace();
        }

        SegmentedTimeline timeline = SegmentedTimeline
                .newFifteenMinuteTimeline();
        // 排除掉不存在的tick
        DateRange dateRangeAll = DateUtil.range(timeTicks.get(0),
                timeTicks.get(timeTicks.size() - 1),
                DateField.MINUTE); // 所有每分钟的时间tick,
        List<DateTime> excludes = new ArrayList<>();
        for (DateTime dateTime : dateRangeAll) {
            if (!timeTicks.contains(dateTime)) {
                excludes.add(dateTime);
            }
        }
        timeline.addExceptions(excludes);
        x1Axis.setTimeline(timeline);//设置时间线显示的规则，用这个方法就摒除掉了周六和周日这些没有交易的日期(很多人都不知道有此方法)，使图形看上去连续

        x1Axis.setAutoTickUnitSelection(false);//设置不采用自动选择刻度值
        x1Axis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);//设置标记的位置
        x1Axis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());//设置标准的时间刻度单位
        x1Axis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 30));//设置时间刻度的间隔，一般以周为单位
        x1Axis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));//设置显示时间的格式

        NumberAxis y1Axis = new NumberAxis(); //  设定y轴，就是数字轴

        //设定y轴值的范围，比最低值要低一些，比最大值要大一些，大的部分为昨收的百分之一
        if (stdValue != null) {
            if (!percentizeFlag) {
                double yHeight = Math.max(Math.abs(stdValue - highValue), Math.abs(stdValue - minValue));
                y1Axis.setRange(stdValue * 0.99 - yHeight, stdValue * 1.01 + yHeight); // 大约正中间
                y1Axis.setTickUnit(new NumberTickUnit(stdValue * 0.02, new DecimalFormat("####0.00"))); // 设置刻度显示的密度
            } else {
                double yHeight = Math.max(Math.abs(highValue), Math.abs(minValue));
                y1Axis.setRange(0 - yHeight, 0 + yHeight); // 大约正中间
                y1Axis.setTickUnit(new NumberTickUnit(yHeight / 5, new DecimalFormat("####0.00%"))); // 设置刻度显示的密度
            }
        } else {
            y1Axis.setRange(minValue * 0.99, highValue * 1.01); // 大约正中间
            y1Axis.setTickUnit(new NumberTickUnit(minValue * 0.05, new DecimalFormat("####0.00"))); // 设置刻度显示的密度
        }
        XYPlot plot1 = new XYPlot(seriesCollection, x1Axis, y1Axis, candlestickRender);//设置画图区域对象

        XYBarRenderer xyBarRender = new XYBarRenderer() {
            private static final long serialVersionUID = 1L;//为了避免出现警告消息，特设定此值

            @Override
            public Paint getItemPaint(int i, int j) {//匿名内部类用来处理当日的成交量柱形图的颜色与K线图的颜色保持一致
                if (seriesCollection.getCloseValue(i, j) > seriesCollection
                        .getOpenValue(i, j)) {//收盘价高于开盘价，股票上涨，选用股票上涨的颜色
                    return candlestickRender.getUpPaint();
                } else {
                    return candlestickRender.getDownPaint();
                }
            }
        };
        xyBarRender.setMargin(0.1);//设置柱形图之间的间隔
        NumberAxis y2Axis = new NumberAxis();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
        y2Axis.setAutoRange(false);
        y2Axis.setRange(min2Value * 0.9, high2Value * 1.1);
        y2Axis.setTickUnit(new NumberTickUnit((high2Value * 1.1 - min2Value * 0.9) / 4));
        XYPlot plot2 = new XYPlot(timeSeriesCollection, null, y2Axis,
                xyBarRender);//建立第二个画图区域对象，主要此时的x轴设为了null值，因为要与第一个画图区域对象共享x轴
        CombinedDomainXYPlot combineddomainxyplot = new CombinedDomainXYPlot(x1Axis);//建立一个恰当的联合图形区域对象，以x轴为共享轴

        if (stdValue != null) {
            // 昨收横线
            double markerValue = percentizeFlag ? 0.0 : stdValue;
            ValueMarker valuemarker = new ValueMarker(markerValue); // 水平线的值, 昨日收盘
            valuemarker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
            valuemarker.setPaint(Color.red); //线条颜色
            valuemarker.setStroke(new BasicStroke(2.0F)); //粗细
            valuemarker.setLabel("昨收"); //线条上显示的文本
            valuemarker.setLabelFont(new Font("SansSerif", 0, 11)); //文本格式
            valuemarker.setLabelPaint(Color.red);
            valuemarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
            valuemarker.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
            plot1.addRangeMarker(valuemarker);
        }

        combineddomainxyplot.add(plot1, 2);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
        combineddomainxyplot.add(plot2, 1);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域1/3
        combineddomainxyplot.setGap(10);//设置两个图形区域对象之间的间隔空间
        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, combineddomainxyplot, false);
    }

    /**
     * 东方财富 1分钟分时图chart创建, api数据形式如下
     * 日期	   开盘	   收盘	   最高	   最低	  成交量	        成交额	  振幅	  涨跌幅	  涨跌额	 换手率	  资产代码	资产名称
     * 0	2022-02-11 09:31	17.03	17.13	17.19	17.03	53583	91625540.00	0.94	0.82 	0.14 	0.03	000001	平安银行
     * 1	2022-02-11 09:32	17.15	17.26	17.30	17.15	48710	83935378.00	0.88	0.76 	0.13 	0.03	000001	平安银行
     * 2	2022-02-11 09:33	17.26	17.21	17.26	17.21	18280	31506777.00	0.29	-0.29	-0.05	0.01	000001	平安银行
     *
     * @param dataFrame 分时图数据.
     * @return
     * @see
     */
    public static JFreeChart createFs1MKLineOfEm(DataFrame<Object> dataFrame, Double preClose,
                                                 String title, KLineYType kLineYType) {
        return createKlineCore(dataFrame, preClose, title, "日期", "开盘", "收盘", "最高", "最低", "成交量", kLineYType);
    }


    /**
     * 简单显示chart对象
     *
     * @param barChart
     */
    public static void showChartSimple(JFreeChart barChart) {
        ApplicationFrame frame = new ApplicationFrame("temp");
        ChartPanel chartPanel = new ChartPanel(barChart);
        // 大小
        chartPanel.setPreferredSize(new java.awt.Dimension(1200, 1000));

        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);

        //        ChartUtilities.saveChartAsPNG(new File(root), chart, 520, 250); // 保存图片
    }


    public static void saveChartAsPNG(File file, JFreeChart chart, int width, int height) throws IOException {
        ChartUtilities.saveChartAsPNG(file, chart, width, height); // 保存图片
    }

    /**
     * 折线图和条形图均使用此类数据集.
     *
     * @param df      数据df. 自动判定是否为数字, 将展示数字的各列
     * @param xUseCol 可指定某列为x轴值. 否则使用index
     * @return
     */
    public static CategoryDataset createDefaultCategoryDataset(DataFrame<Object> df, String xUseCol) {
        if (df.length() == 0) {
            throw new IllegalArgumentException("df lenth 应该大于0");
        }
        df = df.convert();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Object[] columns = df.columns().toArray();
//
        ArrayList<Object> xs = new ArrayList<>();
        if (xUseCol == null) { // 未给定列作为 x轴各刻度时, 使用index
            Object[] indexes = df.index().toArray();
            for (Object index : indexes) {
                xs.add(index.toString());
            }
        } else {
            List<Object> xsCol = df.col(xUseCol);
            for (Object o : xsCol) {
                xs.add(o.toString());
            }
            df = df.drop(xUseCol); // 去掉x轴值
        }

        for (int i = 0; i < df.size(); i++) {
            if (!Number.class.isAssignableFrom(df.types().get(i))) {
                continue; // 不是数字跳过
            }
            List<Object> col = df.col(i);
            for (int j = 0; j < col.size(); j++) {
                dataset.addValue(((Number) df.get(j, i)).doubleValue(), columns[i].toString(), xs.get(j).toString());
            }
        }
        return dataset;
    }

    public static DecimalFormat decimalFormatForPercent = new DecimalFormat();

    static {
        decimalFormatForPercent.applyPattern("####0.00%");
    }

    /**
     * 针对 XYPlot 图表(单图), 鼠标移动时的十字线功能 监听器
     *
     * @return
     */
    public static ChartMouseListener getCrossLineListenerForSingleXYPlot() {
        return new CrossLineListenerForSingleXYPlot();
//        return new ChartMouseListener() {
//
//
//            @Override
//            public void chartMouseClicked(ChartMouseEvent event) {
//            }
//
//
//
//            @Override
//            public void chartMouseMoved(ChartMouseEvent event) {
//                // 1.只监听 XYPlot 上的鼠标移动, 其他类型无视; 获取 xyplot对象
//                if (event.getEntity() == null) {
//                    return;
//                }
//                if (!(event.getEntity() instanceof PlotEntity)) {
//                    return;
//                }
//                if (!(event.getChart().getPlot() instanceof XYPlot)) {
//                    return;
//                }
//                XYPlot plot = (XYPlot) event.getChart().getPlot();
//
//                // 2.ChartPanel 对象
//                ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();
//
//                // 3.事件发生的坐标, 对比屏幕中的数据区域, 所占百分比位置, 对应两个坐标轴值range百分比, 求出鼠标点对应的 x,y值
//                // 3.1: 求鼠标当前位置, 对应的x值
//                double cursorX = event.getTrigger().getX(); // 鼠标位置
//                double minX = chartPanel.getScreenDataArea().getMinX();
//                double maxX = chartPanel.getScreenDataArea().getMaxX(); // 图最大最小y
//                double percentX = (maxX - cursorX) / (maxX - minX); // 从下到上部分百分比, 后面计算 value Range同百分比的x值即可
//                ValueAxis domainAxis = plot.getDomainAxis();
//                Range rangeX = domainAxis.getRange();
//                Double markerValueX = rangeX.getUpperBound() - rangeX.getLength() * percentX; // 同百分比取得marker位置
//
//                // 3.2: 删除所有DomainMarkers, 新建对应x值得 Marker并设置. 可得到十字竖线
//                ValueMarkerS valuemarkerX = new ValueMarkerS(markerValueX); // 水平线的值, 昨日收盘
//                valuemarkerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER); // 标志类型
//                valuemarkerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
//                valuemarkerX.setPaint(Color.red); //线条颜色
//                valuemarkerX.setStroke(new BasicStroke(1.0F)); //粗细
//                valuemarkerX.setLabel(decimalFormatForPercent.format(markerValueX)); //线条上显示的文本
//                valuemarkerX.setLabelFont(new Font("SansSerif", 0, 8)); //文本格式
//                valuemarkerX.setLabelPaint(Color.red);
//                valuemarkerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
//                valuemarkerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);
//                Collection domainMarkers = plot.getDomainMarkers(Layer.FOREGROUND);
//                for (Object domainMarker : domainMarkers) {
//                    if (domainMarker instanceof ValueMarkerS) {
//                        ValueMarkerS markerS = (ValueMarkerS) domainMarker;
//                        if (markerS.getType() == ValueMarkerS.Type.MOUSE_CROSS_MARKER) {
//                            plot.removeDomainMarker(markerS);
//                        }
//                    }
//                }
//                plot.addDomainMarker(valuemarkerX);
//
//                // 3.3: 同理, 求出鼠标对应y值
//                double cursorY = event.getTrigger().getY(); // 鼠标位置
//                double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
//                double minY = chartPanel.getScreenDataArea().getMinY();
//                double percentY = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
//                ValueAxis rangeAxis = plot.getRangeAxis();
//                Range range = rangeAxis.getRange();
//                Double markerValueY = range.getLowerBound() + range.getLength() * percentY; // 同百分比取得marker位置
//                // 3.4: 同理, 创建y值 横向marker
//                ValueMarkerS valuemarker = new ValueMarkerS(markerValueY); // 水平线的值, 昨日收盘
//                valuemarker.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
//                valuemarker.setLabelOffsetType(LengthAdjustmentType.EXPAND);
//                valuemarker.setPaint(Color.red); //线条颜色
//                valuemarker.setStroke(new BasicStroke(1.0F)); //粗细
//                valuemarker.setLabel(decimalFormatForPercent.format(markerValueY)); //线条上显示的文本
//                valuemarker.setLabelFont(new Font("SansSerif", 0, 8)); //文本格式
//                valuemarker.setLabelPaint(Color.red);
//                valuemarker.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
//                valuemarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
//                Collection rangeMarkers = plot.getRangeMarkers(Layer.FOREGROUND);
//                for (Object rangeMarker : rangeMarkers) {
//                    if (rangeMarker instanceof ValueMarkerS) {
//                        ValueMarkerS markerS = (ValueMarkerS) rangeMarker;
//                        if (markerS.getType() == ValueMarkerS.Type.MOUSE_CROSS_MARKER) {
//                            plot.removeRangeMarker(markerS);
//                        }
//                    }
//                }
//                plot.addRangeMarker(valuemarker);
//
//
//            }
//        };
    }
}
