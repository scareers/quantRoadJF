package com.scareers.utils.charts;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.StockStateHs;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.*;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.text.TextUtilities;
import org.jfree.ui.*;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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


        DataFrame<Object> industryDf = ThsDbApi.getIndustryByNameAndDate("电力", "2022-04-01");
        Console.log(industryDf);
//
        int marketCode = Integer.parseInt(industryDf.get(0, "marketCode").toString());
        String code = industryDf.get(0, "code").toString();
//        DataFrame<Object> fs1M = WenCaiDataApi.getFS1M(marketCode, code);
//        Console.log(fs1M);
//
//        /*
//         *     	      日期	      开盘	      最高	      最低	      收盘	        成交量	              成交额
//         *    0	20070831	976.728 	1032.163	940.157 	1017.768	1787555400 	32589345000.000
//         *    1	20070928	1028.107	1123.050	1004.033	1113.713	2044824800 	39127837000.000
//         */
//        DataFrame<Object> lastNKline = WenCaiDataApi.getLastNKline(marketCode, code, 0, 0, 20);
//        Console.log(lastNKline.toString(100));
//
//        JFreeChart chart = createFs1MOfThs(fs1M, 1368.816, "标题");
//        showChartSimple(chart);
        DataFrame<Object> lastNKline = WenCaiDataApi.getLastNKline(marketCode, code, 0, 1, 241);
        Console.log(lastNKline);


        JFreeChart chart = createKLineOfThs(lastNKline, null, "标题", KLineYType.VALUE);
        showChartSimple(chart);

//        String stock = "000001";
//        DataFrame<Object> fs1MToday = EmQuoteApi.getFs1MToday(SecurityBeanEm.createStock(stock), 3, 3000);
//        JFreeChart chart = createFs1MKLineOfEm(fs1MToday,
//                EmQuoteApi.getStockBondPreCloseAndTodayOpen(SecurityBeanEm.createStock(stock), 2000, 3, true).get(0),
//                "000001",
//                KLineYType.PERCENT);
//        showChartSimple(chart);


    }

    public static void demo2() throws Exception {

        int initAmount = 500;
        String stock = "600798";
        String date = EmQuoteApi.getPreNTradeDateStrict(DateUtil.today(), 2);
        Double preClose = StockStateHs.getPreNDayClosePriceQfq(stock, date);
        // "sec_code", "market", "time_tick", "price", "vol", "bs"
        DataFrame<Object> fsDf = EmQuoteApi
                .getFSTransaction(6000, SecurityBeanEm.createStock(stock), 3, 3000);
        Console.log(fsDf.toString(5000));

        List<Double> prices = DataFrameS.getColAsDoubleList(fsDf, "price").stream().map(value -> value / preClose - 1)
                .collect(
                        Collectors.toList());
        List<Double> initPrices = prices.subList(0, initAmount);

        List<DateTime> timeTicks = DataFrameS.getColAsDateList(fsDf, "time_tick");
        List<DateTime> xsInit = timeTicks.subList(0, initAmount);

        final TimeSeries datas = new TimeSeries("price");
        for (int i = 0; i < initAmount; i++) {
            datas.add(new Second(xsInit.get(i)), initPrices.get(i));
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(datas);

//        ThreadUtil.execAsync(new Runnable() {
//            @Override
//            public void run() {
//                int i = 0;
//                while (true) {
//                    Integer index = initAmount + i;
//                    if (index >= prices.size()) {
//                        Console.log("数据结束");
//                        break;
//                    }
//                    datas.addOrUpdate(new Second(timeTicks.get(index)), prices.get(index));
//                    ThreadUtil.sleep(100);
//                    System.out.println("数据刷新");
//                    i++;
//                }
//            }
//        }, false);


        DateAxis timeAxis = new DateAxis(null);
        timeAxis.setLowerMargin(0.02);  // reduce the default margins
        timeAxis.setUpperMargin(0.02);

        timeAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        timeAxis.setTickUnit(new DateTickUnit(DateTickUnitType.SECOND, 3));//设置时间刻度的间隔，一般以周为单位
        // 1秒
        SegmentedTimeline timeline = new SegmentedTimeline(1000, 5000, 0);
        timeline.setStartTime(xsInit.get(0).millisecond());

        DateRange dateRangeAll = DateUtil.range(timeTicks.get(0),
                timeTicks.get(timeTicks.size() - 1),
                DateField.SECOND); // 所有每s钟的时间tick,
        List<DateTime> excludes = new ArrayList<>();
        for (DateTime dateTime : dateRangeAll) {
            if (!timeTicks.contains(dateTime)) {
                excludes.add(dateTime);
            }
        }
        timeline.addExceptions(excludes);

        timeAxis.setTimeline(timeline);

        NumberAxis valueAxis = new NumberAxis(null);
        valueAxis.setAutoRangeIncludesZero(false);  // override default
        timeAxis.setVisible(false);
        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);
        XYToolTipGenerator toolTipGenerator = null;
        XYURLGenerator urlGenerator = null;


        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true,
                false);
        renderer.setBaseToolTipGenerator(toolTipGenerator);
        renderer.setURLGenerator(urlGenerator);
        plot.setRenderer(renderer);

        plot.setRangeZeroBaselineVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                plot, false);


        ApplicationFrame frame = new ApplicationFrame("temp");


        ChartPanel chartPanel = new ChartPanel(chart);
        // 大小
        chartPanel.setPreferredSize(new java.awt.Dimension(1800, 1000));

        chartPanel.addChartMouseListener(new CrossLineListenerForTimeSeriesXYPlot());


        chartPanel.setDisplayToolTips(true);
        chartPanel.setToolTipText("提示信息");
        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
    }

    public static void demoOfFsTransData() throws Exception {
        int initAmount = 200;
        String stock = "000001";

        String date = EmQuoteApi.getPreNTradeDateStrict(DateUtil.today(), 2);
        Double preClose = StockStateHs.getPreNDayClosePriceQfq(stock, date);
        // "sec_code", "market", "time_tick", "price", "vol", "bs"
        DataFrame<Object> fsDf = EmQuoteApi
                .getFSTransaction(6000, SecurityBeanEm.createStock("000001"), 3, 3000);


        List<Double> prices = DataFrameS.getColAsDoubleList(fsDf, "price").stream().map(value -> value / preClose - 1)
                .collect(
                        Collectors.toList());
        List<Double> initPrices = prices.subList(0, initAmount);
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(fsDf, "time_tick");

        List<DateTime> xsInit = timeTicks.subList(0, initAmount);


        final TimeSeries datas = new TimeSeries("price");
        for (int i = 0; i < initAmount; i++) {
            datas.add(new Second(xsInit.get(i)), initPrices.get(i));
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(datas);


        DateAxis x1Axis = new DateAxis(); //设置x轴，也就是时间轴
        x1Axis.setAutoRange(false); //设置是否采用自动设置时间范围

        try {
            x1Axis.setRange(timeTicks.get(0),
                    timeTicks.get(timeTicks.size() - 1)); // 设置时间范围，注意时间的最大值要比已有的时间最大值要多一天
        } catch (Exception e) {
            x1Axis.setAutoRange(true);
            e.printStackTrace();
        }

        SegmentedTimeline timeline = SegmentedTimeline.newFifteenMinuteTimeline();
        // 排除掉不存在的tick
        DateRange dateRangeAll = DateUtil.range(timeTicks.get(0),
                timeTicks.get(timeTicks.size() - 1),
                DateField.SECOND); // 所有每s钟的时间tick,
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
//        x1Axis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());//设置标准的时间刻度单位
//        x1Axis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 1));//设置时间刻度的间隔，一般以周为单位
        x1Axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));//设置显示时间的格式


//        NumberAxis xAxis = new NumberAxis("x");
//        xAxis.setRange(0, 5000);
//        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis("y");
        yAxis.setAutoRangeIncludesZero(false);
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        XYPlot plot = new XYPlot(dataset, x1Axis, yAxis, renderer);

        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (true) {
                    Integer index = initAmount + i;
                    if (index >= prices.size()) {
                        Console.log("数据结束");
                        break;
                    }
                    datas.addOrUpdate(new Second(timeTicks.get(index)), prices.get(index));
                    ThreadUtil.sleep(200);
                    System.out.println("数据刷新");
                    i++;
                }
            }
        }, false);

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

        chartPanel.addChartMouseListener(getCrossLineListenerForSingleNumberXYPlot());
        chartPanel.setDisplayToolTips(true);
        chartPanel.setToolTipText("提示信息");
        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
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

        chartPanel.addChartMouseListener(getCrossLineListenerForSingleNumberXYPlot());
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
        Console.log(timeTicks);
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
     * //         *     	      日期	      开盘	      最高	      最低	      收盘	        成交量	              成交额
     * //         *    0	20070831	976.728 	1032.163	940.157 	1017.768	1787555400 	32589345000.000
     * //         *    1	20070928	1028.107	1123.050	1004.033	1113.713	2044824800 	39127837000.000
     * @param dataFrame
     * @param preClose
     * @param title
     * @param kLineYType
     * @return
     */
    public static JFreeChart createKLineOfThs(DataFrame<Object> dataFrame, Double preClose,
                                                 String title, KLineYType kLineYType) {
        return createKlineCore(dataFrame, preClose, title, "日期", "开盘", "收盘", "最高", "最低", "成交量", kLineYType);
    }

    /**
     * 同花顺分时图, 非k线图; 坐标轴使用 涨跌幅百分比 坐标
     * 时间	      价格	      成交额	   均价	     成交量
     * 0	09:30	1361.471	70277980 	6.485	10836443
     * 1	09:31	1355.017	274092870	5.562	51076425
     * 2	09:32	1354.467	201235350	5.913	30360420
     *
     * @param dataFrame
     * @param preClose
     * @param title
     * @param kLineYType
     * @return
     */
    public static JFreeChart createFs1MOfThs(DataFrame<Object> dataFrame, Double preClose,
                                             String title) {
        return timeSharingChartPlant(dataFrame, preClose, title);

    }

    // todo
    public static JFreeChart timeSharingChartPlant(DataFrame<Object> dataFrame, Double yClose,
                                                   String title) {
        String path = null;
        try {
            // 是否确定需要百分比化
            List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, "时间");
            List<Double> prices = DataFrameS.getColAsDoubleList(dataFrame, "价格");
            List<Double> vols = DataFrameS.getColAsDoubleList(dataFrame, "成交量");

            double mLow = CommonUtil.minOfListDouble(prices); // 价格最低
            double mHigh = CommonUtil.maxOfListDouble(prices); // 价格最高

            TimeSeriesCollection lineSeriesConllection = new TimeSeriesCollection();
            TimeSeries serise1 = new TimeSeries("分时数据");
            TimeSeries serise2 = new TimeSeries("昨日收盘价");

            TimeSeriesCollection barSeriesCollection = new TimeSeriesCollection();//保留成交量数据的集合
            TimeSeries serise3 = new TimeSeries("成交量");

            Date today = timeTicks.get(0);//今天


            //循环写入数据
            for (int i = 0; i < prices.size(); i++) {
                serise1.add(new Millisecond(timeTicks.get(i)), prices.get(i));
                serise3.add(new Millisecond(timeTicks.get(i)), vols.get(i));
            }

            serise2.add(new Day(today), yClose);

            Date tomorrow = new Date(today.getTime() + 86400000); // 明天
            serise2.add(new Day(tomorrow), yClose);

            //分时图数据
            lineSeriesConllection.addSeries(serise1);
            lineSeriesConllection.addSeries(serise2);

            //成交量数据
            barSeriesCollection.addSeries(serise3);


            //设置均线图画图器
            XYLineAndShapeRenderer lineAndShapeRenderer = new XYLineAndShapeRenderer();
            lineAndShapeRenderer.setBaseItemLabelsVisible(true);
            lineAndShapeRenderer.setSeriesShapesVisible(0, false);//设置不显示数据点模型
            lineAndShapeRenderer.setSeriesShapesVisible(1, false);
            lineAndShapeRenderer.setSeriesPaint(0, Color.WHITE);//设置均线颜色
            lineAndShapeRenderer.setSeriesPaint(1, Color.RED);


            //设置k线图x轴，也就是时间轴
            DateAxis domainAxis = new DateAxis();
            domainAxis.setAutoRange(false);//设置不采用自动设置时间范围
            //设置时间范围，注意，最大和最小时间设置时需要+ - 。否则时间刻度无法显示
            Calendar calendar = Calendar.getInstance();
            Date da = today;
            calendar.setTime(da);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 29);
            calendar.set(Calendar.SECOND, 0);
            Date sda = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 15);
            calendar.set(Calendar.MINUTE, 01);
            da = calendar.getTime();
            domainAxis.setRange(sda, da);//设置时间范围


            DateFormat df = new SimpleDateFormat("HH:mm");
            domainAxis.setAutoTickUnitSelection(false);//设置不采用自动选择刻度值
            domainAxis.setTickMarkPosition(DateTickMarkPosition.START);//设置标记的位置
            domainAxis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());// 设置标准的时间刻度单位


            domainAxis.setTickUnit(new DateTickUnit(DateTickUnit.MINUTE, 30));// 设置时间刻度的间隔
            domainAxis.setDateFormatOverride(df);//设置时间格式


            SegmentedTimeline timeline = SegmentedTimeline
                    .newFifteenMinuteTimeline();//设置时间线显示的规则，用这个方法摒除掉周六和周日这些没有交易的日期


            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 31);
            calendar.set(Calendar.SECOND, 0);
            sda = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 59);
            da = calendar.getTime();


            timeline.addException(sda.getTime(), da.getTime());//排除非交易时间段
            domainAxis.setTimeline(timeline);


            //设置k线图y轴参数
            NumberAxisY1 y1Axis = new NumberAxisY1();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y1Axis.setAutoRange(false);//设置不采用自动设置数据范围
            y1Axis.setLabel(String.valueOf(yClose));
            y1Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
            double t = yClose - mLow;
            double t1 = mHigh - yClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            double range = t1 > t ? t1 : t;//计算涨跌最大幅度
            DecimalFormat df1 = new DecimalFormat("#0.00");
            df1.setRoundingMode(RoundingMode.FLOOR);


            y1Axis.setRange(Double.valueOf(df1.format(yClose - range)),
                    Double.valueOf(df1.format(yClose + range)));//设置y轴数据范围
            y1Axis.setNumberFormatOverride(df1);
            y1Axis.centerRange(yClose);
            NumberTickUnit numberTickUnit = new NumberTickUnit(Math.abs(range / 7));
            y1Axis.setTickUnit(numberTickUnit);


            NumberAxisY2 y2Axis = new NumberAxisY2();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y2Axis.setAutoRange(false);//设置不采用自动设置数据范围
            y2Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));


            t = (mLow - yClose) / yClose;
            t1 = (mHigh - yClose) / yClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            range = t1 > t ? t1 : t;
            y2Axis.setRange(-range, range);//设置y轴数据范围
            y2Axis.setTickLabelPaint(Color.RED);
            DecimalFormat df2 = new DecimalFormat("#0.00%");
            df2.setRoundingMode(RoundingMode.FLOOR);
            y2Axis.setNumberFormatOverride(df2);
            NumberTickUnit numberTickUnit2 = new NumberTickUnit(Math.abs(range / 7));
            y2Axis.setTickUnit(numberTickUnit2);


            //生成画图细节 第一个和最后一个参数这里需要设置为null，否则画板加载不同类型的数据时会有类型错误异常
            //可能是因为初始化时，构造器内会把统一数据集合设置为传参的数据集类型，画图器可能也是同样一个道理
            XYPlot plot = new XYPlot(lineSeriesConllection, domainAxis, null, lineAndShapeRenderer);
            plot.setBackgroundPaint(Color.BLACK);//设置曲线图背景色
            plot.setDomainGridlinesVisible(false);//不显示网格
            plot.setRangeGridlinePaint(Color.RED);//设置间距格线颜色为红色
            plot.setRangeAxis(0, y1Axis);
            plot.setRangeAxis(1, y2Axis);


            //设置柱状图参数
            XYBarRenderer barRenderer = new XYBarRenderer();


            barRenderer.setDrawBarOutline(true);//设置显示边框线
            barRenderer.setBarPainter(new StandardXYBarPainter());//取消渐变效果
            barRenderer.setMargin(0.5);//设置柱形图之间的间隔
            barRenderer.setSeriesPaint(0, Color.YELLOW);//设置柱子内部颜色
            barRenderer.setSeriesOutlinePaint(0, Color.YELLOW);//设置柱子边框颜色
            barRenderer.setSeriesVisibleInLegend(false);//设置不显示legend（数据颜色提示)
            barRenderer.setShadowVisible(false);//设置没有阴影


            //设置柱状图y轴参数
            NumberAxis y3Axis = new NumberAxis();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y3Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));//设置y轴字体
            y3Axis.setAutoRange(true);//设置采用自动设置时间范围
            y3Axis.setTickLabelPaint(Color.ORANGE);//设置y轴刻度值颜色


            //这里不设置x轴，x轴参数依照k线图x轴为模板
            XYPlot plot2 = new XYPlot(barSeriesCollection, null, y3Axis, barRenderer);
            plot2.setBackgroundPaint(Color.BLACK);//设置曲线图背景色
            plot2.setDomainGridlinesVisible(false);//不显示网格
            plot2.setRangeGridlinePaint(Color.RED);//设置间距格线颜色为红色


            //建立一个恰当的联合图形区域对象，以x轴为共享轴
            CombinedDomainXYPlot domainXYPlot = new CombinedDomainXYPlot(domainAxis);//
            domainXYPlot.add(plot, 2);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
            domainXYPlot.add(plot2, 1);
            domainXYPlot.setGap(2);//设置两个图形区域对象之间的间隔空间

            plot.setBackgroundPaint(Color.black);
            plot2.setBackgroundPaint(Color.black);
            domainXYPlot.setBackgroundPaint(Color.black);
            //生成图纸
            JFreeChart chart = new JFreeChart(title, new Font("微软雅黑", Font.BOLD, 24), domainXYPlot, true);
            chart.setBackgroundPaint(Color.black);
            return chart;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 此内部类专门为分时图提供，已解决分时图y轴数据刻度无法显示多种颜色的情况
     *
     * @author t
     */
    private static class NumberAxisY2 extends NumberAxis {
        @Override
        protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea,
                                                   Rectangle2D dataArea, RectangleEdge edge) {
            AxisState state = new AxisState(cursor);
            if (isAxisLineVisible()) {
                drawAxisLine(g2, cursor, dataArea, edge);
            }


            List ticks = refreshTicks(g2, state, dataArea, edge);
            state.setTicks(ticks);
            g2.setFont(getTickLabelFont());
            Iterator iterator = ticks.iterator();
            while (iterator.hasNext()) {
                ValueTick tick = (ValueTick) iterator.next();
                if (isTickLabelsVisible()) {
                    if (tick.getValue() > 0) {
                        g2.setPaint(Color.RED);
                    } else if (tick.getValue() == 0) {
                        g2.setPaint(Color.GRAY);
                    } else {
                        g2.setPaint(Color.GREEN);
                    }


                    float[] anchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
                    TextUtilities
                            .drawRotatedString(tick.getText(), g2, anchorPoint[0], anchorPoint[1], tick.getTextAnchor(),
                                    tick.getAngle(), tick.getRotationAnchor());
                }


                if (((isTickMarksVisible()) && (tick.getTickType()
                        .equals(TickType.MAJOR))) || ((isMinorTickMarksVisible()) && (tick.getTickType()
                        .equals(TickType.MINOR)))) {
                    double ol = getTickMarkOutsideLength();


                    double il = getTickMarkInsideLength();


                    float xx = (float) valueToJava2D(tick.getValue(), dataArea, edge);


                    Line2D mark = null;
                    g2.setStroke(getTickMarkStroke());
                    g2.setPaint(getTickMarkPaint());
                    if (edge == RectangleEdge.LEFT) {
                        mark = new Line2D.Double(cursor - ol, xx, cursor + il, xx);
                    } else if (edge == RectangleEdge.RIGHT) {
                        mark = new Line2D.Double(cursor + ol, xx, cursor - il, xx);
                    } else if (edge == RectangleEdge.TOP) {
                        mark = new Line2D.Double(xx, cursor - ol, xx, cursor + il);
                    } else if (edge == RectangleEdge.BOTTOM) {
                        mark = new Line2D.Double(xx, cursor + ol, xx, cursor - il);
                    }
                    g2.draw(mark);
                }


            }


            double used = 0.0D;
            if (isTickLabelsVisible()) {
                if (edge == RectangleEdge.LEFT) {
                    used += findMaximumTickLabelWidth(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorLeft(used);
                } else if (edge == RectangleEdge.RIGHT) {
                    used = findMaximumTickLabelWidth(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorRight(used);
                } else if (edge == RectangleEdge.TOP) {
                    used = findMaximumTickLabelHeight(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorUp(used);
                } else if (edge == RectangleEdge.BOTTOM) {
                    used = findMaximumTickLabelHeight(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorDown(used);
                }
            }


            return state;
        }
    }


    /**
     * 此内部类专门为分时图提供，以解决分时图y轴无法以昨日收盘价为中心来描写刻度数据的问题
     *
     * @author t
     */
    private static class NumberAxisY1 extends NumberAxis {
        @Override
        protected AxisState drawTickMarksAndLabels(Graphics2D g2, double cursor, Rectangle2D plotArea,
                                                   Rectangle2D dataArea, RectangleEdge edge) {
            AxisState state = new AxisState(cursor);
            if (isAxisLineVisible()) {
                drawAxisLine(g2, cursor, dataArea, edge);
            }


            List ticks = refreshTicks(g2, state, dataArea, edge);
            //昨日收盘价
            double yClose = Double.valueOf(getLabel());
            //获取两个价位
            NumberTick tick1 = (NumberTick) ticks.get(0);
            NumberTick tick2 = (NumberTick) ticks.get(1);


            //获取价位差值，而每个差值都是约等于
            Double tick1Val = Double.valueOf(tick1.getText());
            Double tick2Val = Double.valueOf(tick2.getText());
            Double range = tick2Val - tick1Val;


            //重置ticks集合，将昨日收盘价置于中间刻度，因设置刻度时与国内股票分时图刻度规则有差异，例：国内为上下7个刻度，加上中间的昨日收盘价，一起为15个刻度
            //而这里设置7个刻度则没有写入昨日收盘价的中间刻度，所以这里重置ticks集合，长度为ticks集合size + 1，中间为昨日收盘价，然后以价位差值从中间开始往上
            //下两个方向推，则可以得到合适的且平均的刻度价位
            int ticksSize = 14;
            NumberTick[] nticks = new NumberTick[ticksSize + 1];
            NumberTick tickCenter = new NumberTick(yClose, String.valueOf(yClose), tick1.getTextAnchor(),
                    tick1.getRotationAnchor(), tick1.getAngle());
            //定位中间刻度，昨日收盘价
            nticks[ticksSize / 2] = tickCenter;
            double t = yClose;
            //计算向下的价位，并写入集合中
            for (int i = ticksSize / 2 - 1; i >= 0; i--) {
                t = t - range;
                NumberTick tickF = new NumberTick(t, String.valueOf(t), tick1.getTextAnchor(),
                        tick1.getRotationAnchor(), tick1.getAngle());
                nticks[i] = tickF;
            }
            t = yClose;
            //计算向上的价位，并写入集合中
            for (int i = ticksSize / 2 + 1; i < ticksSize + 1; i++) {
                t = t + range;
                NumberTick tickF = new NumberTick(t, String.valueOf(t), tick1.getTextAnchor(),
                        tick1.getRotationAnchor(), tick1.getAngle());
                nticks[i] = tickF;
            }
            ticks = new ArrayList();
            for (NumberTick ti : nticks) {
                ticks.add(ti);
            }
            state.setTicks(ticks);
            g2.setFont(getTickLabelFont());
            Iterator iterator = ticks.iterator();


            while (iterator.hasNext()) {
                ValueTick tick = (ValueTick) iterator.next();
                double tickValue = Double.valueOf(tick.getText());
                float[] anchorPoint = calculateAnchorPoint(tick, cursor, dataArea, edge);
                if (isTickLabelsVisible()) {
                    if (tickValue > yClose) {
                        g2.setPaint(Color.RED);
                    } else if (tickValue == yClose) {
                        g2.setPaint(Color.GRAY);
                    } else {
                        g2.setPaint(Color.GREEN);
                    }
                    DecimalFormat df1 = new DecimalFormat("#0.00");


                    TextUtilities.drawRotatedString(df1.format(tickValue), g2, anchorPoint[0], anchorPoint[1],
                            tick.getTextAnchor(), tick.getAngle(), tick.getRotationAnchor());
                }


                if (((isTickMarksVisible()) && (tick.getTickType()
                        .equals(TickType.MAJOR))) || ((isMinorTickMarksVisible()) && (tick.getTickType()
                        .equals(TickType.MINOR)))) {
                    double ol = getTickMarkOutsideLength();


                    double il = getTickMarkInsideLength();


                    float xx = (float) valueToJava2D(tick.getValue(), dataArea, edge);


                    Line2D mark = null;
                    g2.setStroke(getTickMarkStroke());
                    g2.setPaint(getTickMarkPaint());
                    if (edge == RectangleEdge.LEFT) {
                        mark = new Line2D.Double(cursor - ol, xx, cursor + il, xx);
                    } else if (edge == RectangleEdge.RIGHT) {
                        mark = new Line2D.Double(cursor + ol, xx, cursor - il, xx);
                    } else if (edge == RectangleEdge.TOP) {
                        mark = new Line2D.Double(xx, cursor - ol, xx, cursor + il);
                    } else if (edge == RectangleEdge.BOTTOM) {
                        mark = new Line2D.Double(xx, cursor + ol, xx, cursor - il);
                    }
                    g2.draw(mark);
                }


            }


            double used = 0.0D;
            if (isTickLabelsVisible()) {
                if (edge == RectangleEdge.LEFT) {
                    used += findMaximumTickLabelWidth(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorLeft(used);
                } else if (edge == RectangleEdge.RIGHT) {
                    used = findMaximumTickLabelWidth(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorRight(used);
                } else if (edge == RectangleEdge.TOP) {
                    used = findMaximumTickLabelHeight(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorUp(used);
                } else if (edge == RectangleEdge.BOTTOM) {
                    used = findMaximumTickLabelHeight(ticks, g2, plotArea, isVerticalTickLabels());


                    state.cursorDown(used);
                }
            }


            return state;
        }


        @Override
        protected AxisState drawLabel(String label, Graphics2D g2, Rectangle2D plotArea, Rectangle2D dataArea,
                                      RectangleEdge edge, AxisState state) {


            AffineTransform t;
            Shape rotatedLabelBounds;
            double labelx;
            double labely;
            if (state == null) {
                throw new IllegalArgumentException("Null 'state' argument.");
            }
            //此y轴不提供y轴数据标题
            label = "";
            if ((label == null) || (label.equals(""))) {
                return state;
            }


            Font font = getLabelFont();
            RectangleInsets insets = getLabelInsets();
            g2.setFont(font);
            g2.setPaint(getLabelPaint());
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D labelBounds = TextUtilities.getTextBounds(label, g2, fm);


            if (edge == RectangleEdge.TOP) {
                t = AffineTransform
                        .getRotateInstance(getLabelAngle(), labelBounds.getCenterX(), labelBounds.getCenterY());


                rotatedLabelBounds = t.createTransformedShape(labelBounds);
                labelBounds = rotatedLabelBounds.getBounds2D();
                labelx = dataArea.getCenterX();
                labely = state.getCursor() - insets.getBottom() - (labelBounds.getHeight() / 2.0D);


                TextUtilities.drawRotatedString(label, g2, (float) labelx, (float) labely, TextAnchor.CENTER,
                        getLabelAngle(), TextAnchor.CENTER);


                state.cursorUp(insets.getTop() + labelBounds.getHeight() + insets.getBottom());
            } else if (edge == RectangleEdge.BOTTOM) {
                t = AffineTransform
                        .getRotateInstance(getLabelAngle(), labelBounds.getCenterX(), labelBounds.getCenterY());


                rotatedLabelBounds = t.createTransformedShape(labelBounds);
                labelBounds = rotatedLabelBounds.getBounds2D();
                labelx = dataArea.getCenterX();
                labely = state.getCursor() + insets.getTop() + labelBounds.getHeight() / 2.0D;


                TextUtilities.drawRotatedString(label, g2, (float) labelx, (float) labely, TextAnchor.CENTER,
                        getLabelAngle(), TextAnchor.CENTER);


                state.cursorDown(insets.getTop() + labelBounds.getHeight() + insets.getBottom());
            } else if (edge == RectangleEdge.LEFT) {
                t = AffineTransform.getRotateInstance(getLabelAngle() - 1.570796326794897D, labelBounds.getCenterX(),
                        labelBounds.getCenterY());


                rotatedLabelBounds = t.createTransformedShape(labelBounds);
                labelBounds = rotatedLabelBounds.getBounds2D();
                labelx = state.getCursor() - insets.getRight() - (labelBounds.getWidth() / 2.0D);


                labely = dataArea.getCenterY();
                TextUtilities.drawRotatedString(label, g2, (float) labelx, (float) labely, TextAnchor.CENTER,
                        getLabelAngle() - 1.570796326794897D, TextAnchor.CENTER);


                state.cursorLeft(insets.getLeft() + labelBounds.getWidth() + insets.getRight());
            } else if (edge == RectangleEdge.RIGHT) {
                t = AffineTransform.getRotateInstance(getLabelAngle() + 1.570796326794897D, labelBounds.getCenterX(),
                        labelBounds.getCenterY());


                rotatedLabelBounds = t.createTransformedShape(labelBounds);
                labelBounds = rotatedLabelBounds.getBounds2D();
                labelx = state.getCursor() + insets.getLeft() + labelBounds.getWidth() / 2.0D;


                labely = dataArea.getY() + dataArea.getHeight() / 2.0D;
                TextUtilities.drawRotatedString(label, g2, (float) labelx, (float) labely, TextAnchor.CENTER,
                        getLabelAngle() + 1.570796326794897D, TextAnchor.CENTER);


                state.cursorRight(insets.getLeft() + labelBounds.getWidth() + insets.getRight());
            }


            return state;
        }
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
    public static CrossLineListenerForSingleNumberXYPlot getCrossLineListenerForSingleNumberXYPlot() {
        // 默认y marker 文字在横线右侧之上
        return new CrossLineListenerForSingleNumberXYPlot();
    }

    public static void changeMarkerYTextRightBottom(ValueMarkerS markerY) {
        // 修改y marker文字在横线右侧之下
        markerY.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
        markerY.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
    }
}
