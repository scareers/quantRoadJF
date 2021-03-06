package com.scareers.utils.charts;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.time.Day;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.text.TextUtilities;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.RoundingMode;
import java.text.*;
import java.util.List;
import java.util.*;

/**
 * description: 同花顺数据源的图表; 主要是分时图 和 3个周期的k线图
 *
 * @author: admin
 * @date: 2022/4/5/005-00:48:19
 */
public class ThsChart {
    /*
    k线常量
     */
    // k线颜色
    public static Color upColorKLine = new Color(255, 50, 50);
    public static Color downColorKLine = new Color(84, 252, 252);
    public static Color equalColorKLine = new Color(182, 255, 219);
    // y轴文字颜色: 同东财
    public static Color upColorLabelKLine = new Color(255, 92, 92);
    public static Color downColorLabelKLine = new Color(57, 195, 59);
    public static Color equalColorLabelKLine = new Color(128, 138, 138);

    public static Color bgColorKLine = new Color(7, 7, 7);
    //    public static Color bgColorKLine = Color.white;
    public static int gapOfTwoPlotOfKLine = 0; // 分时价格和成交量,两个图的gap
    public static final int weight1OfTwoPlotOfKLine = 4; // 两大weight, 控制 分时价格图 和成交量图, 高度比例
    public static final int weight2OfTwoPlotOfKLine = 1; // 两大weight+gap, 可以绝对判定 鼠标位置!

    /*
    分时图常量
     */
    // 成交量颜色
    public static Color upColorFs = new Color(240, 248, 136);
    public static Color downColorFs = new Color(84, 252, 252);
    public static Color equalColorFs = Color.white;
    public static Color volTickLabelPaint = Color.red; // 成交量y轴tick文字
    // 分时价格线和均线
    public static Color priceColorFs = Color.white;
    public static Color avgPriceColorFs = new Color(240, 248, 136);
    public static Color preCloseColorFs = new Color(128, 0, 0);
    public static Color bgColorFs = new Color(7, 7, 7);

    // 分时图形状控制!
    public static final int gapOfTwoPlotOfFs = 0; // 分时价格和成交量,两个图的gap
    public static final int weight1OfTwoPlotOfFs = 650; // 两大weight, 控制 分时价格图 和成交量图, 高度比例
    public static final int weight2OfTwoPlotOfFs = 200; // 两大weight+gap, 可以绝对判定 鼠标位置!


    public static void main(String[] args) {
//        kLineDemo();
//
        fsDemo();
    }

    private static void kLineDemo() {
        //         k线
        DataFrame<Object> industryDf = ThsDbApi.getIndustryByNameAndDate("电力", "2022-04-01");
        Console.log(industryDf);
//
        int marketCode = Integer.parseInt(industryDf.get(0, "marketCode").toString());
        String code = industryDf.get(0, "code").toString();

        DataFrame<Object> lastNKline = WenCaiDataApi.getLastNKline(marketCode, code, 0, 1, 60);
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(lastNKline, "日期"); // 日期列表;传递给监听器,设置横轴marker

        JFreeChart chart = createKLineOfThs(lastNKline, String.valueOf(code));

        ApplicationFrame frame = new ApplicationFrame("temp");
        ChartPanel chartPanel = new ChartPanel(chart);


        // 大小
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);

        chartPanel.addChartMouseListener(getCrossLineListenerForKLineXYPlot(timeTicks));


        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
    }


    /**
     * 分时图,将自动调用api, 读取 preClose; 也可自行提供
     */
    private static void fsDemo() {
        // 分时
        DataFrame<Object> industryDf = ThsDbApi.getIndustryByNameAndDate("电力", "2022-04-01");
        Console.log(industryDf);
        int marketCode = Integer.parseInt(industryDf.get(0, "marketCode").toString());
        String code = industryDf.get(0, "code").toString();
        DataFrame<Object> fs1M = WenCaiDataApi.getFS1M(33, "000001");
        DataFrame<Object> lastNKline = WenCaiDataApi.getLastNKline(33, "000001", 0, 0, 5);
        Double preClose = Double.valueOf(lastNKline.get(lastNKline.length() - 2, "收盘").toString());

        List<DateTime> timeTicks = DataFrameS.getColAsDateList(fs1M, "时间"); // 日期列表;传递给监听器,设置横轴marker


        JFreeChart chart = createFs1MOfThs(fs1M, preClose, "测试标题", true);
        ApplicationFrame frame = new ApplicationFrame("temp");

        ChartPanel chartPanel = new ChartPanel(chart);


        // 大小
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);

        chartPanel.addChartMouseListener(getCrossLineListenerForFsXYPlot(timeTicks));


        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
    }

    public static CrossLineListenerForFsXYPlot getCrossLineListenerForFsXYPlot(List<DateTime> dates) {
        // 默认y marker 文字在横线右侧之上
        return new CrossLineListenerForFsXYPlot(dates);
    }

    public static CrossLineListenerForKLineXYPlot getCrossLineListenerForKLineXYPlot(List<DateTime> dates) {
        // 默认y marker 文字在横线右侧之上
        return new CrossLineListenerForKLineXYPlot(dates);
    }

    /**
     * 日期	      开盘	      最高	      最低	      收盘	        成交量	            成交额
     * 0	20210407	1228.754	1259.509	1224.338	1248.329	4491210600 	29751900000.000
     * 1	20210408	1247.548	1247.548	1202.121	1203.991	4870522300 	34337664000.000
     * 2	20210409	1191.466	1226.937	1190.805	1203.788	3882218100 	27379241000.000
     * 实测 日/周/月 k线均可; 只需要日期列, 是单日日期即可
     *
     * @param dailyKLineDf
     * @param preClose
     * @param title
     * @param kLineYType
     * @return
     */
    public static JFreeChart createKLineOfThs(DataFrame<Object> dailyKLineDf, String title) {
        return createKlineCore(dailyKLineDf, title, "日期", "开盘", "收盘", "最高", "最低", "成交量");
    }

    /**
     * 日k线创建核心方法. 需要给定 5项数据+日期列名称.
     * 与同花顺相同, 使用 第一个数据的 close 值, 作为0%基准线, 在右侧显示百分比变化!
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
    private static JFreeChart createKlineCore(DataFrame<Object> dataFrame, String title,
                                              String dateTimeColName,
                                              String openColName,
                                              String closeColName,
                                              String highColName,
                                              String lowColName,
                                              String volColName
    ) {

        // 1.时间列以及4箱数据列;
        java.util.List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, dateTimeColName);

        java.util.List<Double> opens = DataFrameS.getColAsDoubleList(dataFrame, openColName);
        java.util.List<Double> closes = DataFrameS.getColAsDoubleList(dataFrame, closeColName);
        java.util.List<Double> highs = DataFrameS.getColAsDoubleList(dataFrame, highColName);
        java.util.List<Double> lows = DataFrameS.getColAsDoubleList(dataFrame, lowColName);
        // 2.以第一个close为基准, 其他值转换为百分比
        Double stdPrice = closes.get(0);

        // 3.成交量列
        List<Double> vols = DataFrameS.getColAsDoubleList(dataFrame, volColName);

        // 4.价格和成交量最高最低值, 以确定坐标轴范围
        double priceHigh = CommonUtil.maxOfListDouble(highs); // 价格最高
        double priceLow = CommonUtil.minOfListDouble(lows); // 价格最低
        double volMax = CommonUtil.maxOfListDouble(vols); // 成交量最高
        // double volMin = CommonUtil.minOfListDouble(vols); //成交量最低

        OHLCSeries seriesOfFourPrice = new OHLCSeries(""); // 开，高，低，收, 四项数据
        // 5. 4项价格数据序列, 注意, 时间单位是 Day! 因为日k线
        for (int i = 0; i < timeTicks.size(); i++) { // 以时间遍历, 添加四项价格
            seriesOfFourPrice.add(new Day(timeTicks.get(i)), opens.get(i), highs.get(i), lows.get(i), closes.get(i));
        }
        final OHLCSeriesCollection seriesCollection = new OHLCSeriesCollection();
        seriesCollection.addSeries(seriesOfFourPrice); // 数据集

        // 6.成交量序列
        TimeSeries seriesOfVol = new TimeSeries(""); // 对应时间成交量数据
        for (int i = 0; i < timeTicks.size(); i++) {
            seriesOfVol.add(new Day(timeTicks.get(i)), vols.get(i));
        }
        TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection(); // 保留成交量数据的集合
        timeSeriesCollection.addSeries(seriesOfVol);

        // 7. 设置K线图的渲染器
        final CandlestickRenderer candlestickRender = new CandlestickRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {

                //determine up or down candle
                XYDataset dataset = getPlot().getDataset();
                OHLCDataset highLowData = (OHLCDataset) dataset;
                int series = row, item = column;
                Number yOpen = highLowData.getOpen(series, item);
                Number yClose = highLowData.getClose(series, item);
                boolean isUpCandle = yClose.doubleValue() > yOpen.doubleValue();

                //return the same color as that used to fill the candle
                if (isUpCandle) {
                    return getUpPaint();
                } else {
                    return getDownPaint();
                }
            }
        };
        candlestickRender.setUseOutlinePaint(true); // 设置是否使用自定义的边框线，程序自带的边框线的颜色不符合中国股票市场的习惯
        candlestickRender.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);// 设置如何对K线图的宽度进行设定
        candlestickRender.setAutoWidthGap(0.5);//设置各个K线图之间的间隔: 例如0.001
        candlestickRender.setAutoWidthFactor(0.2);//
        candlestickRender.setCandleWidth(-1);//

        candlestickRender.setUpPaint(upColorKLine);//设置股票上涨的K线图颜色
        candlestickRender.setDownPaint(downColorKLine);//设置股票下跌的K线图颜色
        candlestickRender.setUseOutlinePaint(false);


        // 8.日期轴唯一横轴: DateAxis, 设置日期范围; end需要多一天
        DateAxis x1Axis = new DateAxis(); // 设置x轴，也就是时间轴
        x1Axis.setAutoRange(false); // 不采用自动设置时间范围, 自行计算
        try {
            // 设置时间范围，注意时间的最大值要比已有的时间最大值要多一天; 使用 offset方法
            DateTime lastDateTime = timeTicks.get(timeTicks.size() - 1);
            x1Axis.setRange(timeTicks.get(0), DateUtil.offset(lastDateTime,
                    DateField.DAY_OF_MONTH, 1));
        } catch (Exception e) {
            x1Axis.setAutoRange(true);
            e.printStackTrace();
        }
        // 9. 日起轴设定时间线, 使用 SegmentedTimeline 可排除某些日期!
        SegmentedTimeline timeline = SegmentedTimeline.newMondayThroughFridayTimeline();
        DateRange dateRangeAll = DateUtil.range(timeTicks.get(0),
                timeTicks.get(timeTicks.size() - 1), DateField.DAY_OF_MONTH); // 日期range;
        HashSet<DateTime> timeTickSet = new HashSet<>(timeTicks);
        for (DateTime dateTime : dateRangeAll) {
            if (!timeTickSet.contains(dateTime)) { // 相当于仅保留 数据中存在的日期, 其他全部排除掉
                timeline.addException(dateTime);
            }
        }
        x1Axis.setTimeline(timeline);//设置时间线显示的规则，用这个方法就摒除掉了周六和周日这些没有交易的日期(很多人都不知道有此方法)，使图形看上去连续

        // 10.日期x轴其他设置
        x1Axis.setAutoTickUnitSelection(false); //设置不采用自动选择刻度值
        x1Axis.setTickMarkPosition(DateTickMarkPosition.MIDDLE); //设置标记的位置
        x1Axis.setStandardTickUnits(DateAxis.createStandardDateTickUnits()); // 设置标准的时间刻度单位
        x1Axis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 30)); // 设置时间刻度的间隔，一般以周为单位
        x1Axis.setDateFormatOverride(new SimpleDateFormat("MM-dd"));//设置显示时间的格式

        // 11. k线Y轴 价格轴; 左右两边tick, 左价格, 右百分比
        // 设置k线图y轴参数
        NumberAxisYSupportTickToPreClose y1Axis = new NumberAxisYSupportTickToPreClose();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
        y1Axis.setAutoRange(false);//设置不采用自动设置数据范围
        y1Axis.setLabel(String.valueOf(stdPrice));
        y1Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
        double t = stdPrice - priceLow;
        double t1 = priceHigh - stdPrice;
        t = Math.abs(t);
        t1 = Math.abs(t1);
        double range = t1 > t ? t1 : t;//计算涨跌最大幅度
        DecimalFormat df1 = new DecimalFormat("#0.00");
        df1.setRoundingMode(RoundingMode.FLOOR);


        y1Axis.setRange(Double.valueOf(df1.format(stdPrice - range)),
                Double.valueOf(df1.format(stdPrice + range)));//设置y轴数据范围
        y1Axis.setNumberFormatOverride(df1);
        y1Axis.centerRange(stdPrice);
        NumberTickUnit numberTickUnit = new NumberTickUnit(Math.abs(range / 7));
        y1Axis.setTickUnit(numberTickUnit);
        NumberAxisYSupportTickMultiColor y2Axis = new NumberAxisYSupportTickMultiColor();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
        y2Axis.setAutoRange(false);//设置不采用自动设置数据范围
        y2Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));


        t = (priceLow - stdPrice) / stdPrice;
        t1 = (priceHigh - stdPrice) / stdPrice;
        t = Math.abs(t);
        t1 = Math.abs(t1);
        range = t1 > t ? t1 : t;
        y2Axis.setRange(-range, range);//设置y轴数据范围
        y2Axis.setTickLabelPaint(Color.red);
        DecimalFormat df2 = new DecimalFormat("##0.00%");
        df2.setRoundingMode(RoundingMode.FLOOR);
        y2Axis.setNumberFormatOverride(df2);
        NumberTickUnit numberTickUnit2 = new NumberTickUnit(Math.abs(range / 7));
        y2Axis.setTickUnit(numberTickUnit2);

        //12. 图表1实例化 -- k线图
        //生成画图细节 第一个和最后一个参数这里需要设置为null，否则画板加载不同类型的数据时会有类型错误异常
        //可能是因为初始化时，构造器内会把统一数据集合设置为传参的数据集类型，画图器可能也是同样一个道理
        XYPlot plot1 = new XYPlot(seriesCollection, x1Axis, null, candlestickRender);
        plot1.setBackgroundPaint(bgColorFs);//设置曲线图背景色
        plot1.setDomainGridlinesVisible(false);//不显示网格
        plot1.setRangeGridlinePaint(preCloseColorFs);//设置间距格线颜色为红色, 同昨收颜色
        plot1.setRangeAxis(0, y1Axis);
        plot1.setRangeAxis(1, y2Axis); // 左右两个tick

        // 13. 成交量渲染器
        XYBarRenderer xyBarRender = new XYBarRenderer() {
            private static final long serialVersionUID = 1L;// 为了避免出现警告消息，特设定此值

            @Override
            public Paint getItemPaint(int i, int j) { // 匿名内部类用来处理当日的成交量柱形图的颜色与K线图的颜色保持一致
                if (seriesCollection.getCloseValue(i, j) > seriesCollection.getOpenValue(i, j)) {
                    // 收盘价高于开盘价，股票上涨，选用股票上涨的颜色
                    return upColorKLine;

                } else if (seriesCollection.getCloseValue(i, j) < seriesCollection.getOpenValue(i, j)) {
                    return downColorKLine;
                } else {
                    return equalColorKLine;
                }
            }
        };
        xyBarRender.setBarPainter(new StandardXYBarPainter()); // 取消渐变
        xyBarRender.setMargin(0.25);// 设置柱形图之间的间隔
        xyBarRender.setShadowVisible(false); // 不显示阴影
        xyBarRender.setDrawBarOutline(false);


        // 14. 成交量y轴
        NumberAxis y3Axis = new NumberAxis();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
        y3Axis.setAutoRange(false);
        y3Axis.setRange(0, volMax * 1.05); // 从0开始显示
        y3Axis.setTickUnit(new NumberTickUnit((volMax * 1.05) / 5)); // 单位, 需要与范围匹配
        y3Axis.setTickLabelPaint(Color.red);
        y3Axis.setNumberFormatOverride(new NumberFormatCnForBigNumber()); // 数据轴数据标签的显示格式


        // 15. 成交量图表
        //建立第二个画图区域对象，主要此时的x轴设为了null值，因为要与第一个画图区域对象共享x轴
        XYPlot plot2 = new XYPlot(timeSeriesCollection, null, y3Axis, xyBarRender);
        plot2.setDomainGridlinesVisible(false);

        // 16. 共享x轴的图表: 将添加2个图表
        CombinedDomainXYPlot combineddomainxyplot = new CombinedDomainXYPlot(x1Axis);//建立一个恰当的联合图形区域对象，以x轴为共享轴
        combineddomainxyplot.add(plot1, weight1OfTwoPlotOfKLine);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
        combineddomainxyplot.add(plot2, weight2OfTwoPlotOfKLine);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域1/3
        combineddomainxyplot.setGap(gapOfTwoPlotOfKLine);//设置两个图形区域对象之间的间隔空间


        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, combineddomainxyplot, false);
        plot1.setBackgroundPaint(bgColorKLine);
        plot2.setBackgroundPaint(bgColorKLine);
        combineddomainxyplot.setBackgroundPaint(bgColorKLine);
        chart.setBackgroundPaint(bgColorKLine);
        chart.setTitle(
                new TextTitle(title, new Font("华文行楷", Font.BOLD | Font.ITALIC, 18), Color.red, Title.DEFAULT_POSITION,
                        Title.DEFAULT_HORIZONTAL_ALIGNMENT,
                        Title.DEFAULT_VERTICAL_ALIGNMENT, Title.DEFAULT_PADDING));

        return chart;
    }

    /**
     * 数字格式化, 常用于成交额等数据极大, 显示为 xx万 / xx亿 等
     */
    public static class NumberFormatCnForBigNumber extends NumberFormat {
        @Override
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
            return new StringBuffer(CommonUtil.formatNumberWithSuitable(number));
        }

        @Override
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
            return new StringBuffer(CommonUtil.formatNumberWithSuitable(number));
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return null;
        }
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
     * @param showAvgPrice : 是否显示均价线? 行业和概念的均价与 价格不再量级, 不显示, 其他均需要显示!
     * @return
     */
    public static JFreeChart createFs1MOfThs(DataFrame<Object> dataFrame, Double preClose,
                                             String title, boolean showAvgPrice) {
        try {
            List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, "时间");
            List<Double> prices = DataFrameS.getColAsDoubleList(dataFrame, "价格");
            List<Double> avgPrices = DataFrameS.getColAsDoubleList(dataFrame, "均价");
            List<Double> vols = DataFrameS.getColAsDoubleList(dataFrame, "成交量");

            double priceLow = CommonUtil.minOfListDouble(prices); // 价格最低
            double priceHigh = CommonUtil.maxOfListDouble(prices); // 价格最高

            TimeSeriesCollection lineSeriesCollection = new TimeSeriesCollection();
            TimeSeries seriesOfFsPrice = new TimeSeries("分时数据");
            TimeSeries seriesOfAvgPrice = new TimeSeries("均价");
            TimeSeries seriesOfPreClose = new TimeSeries("昨日收盘价");

            TimeSeriesCollection barSeriesCollection = new TimeSeriesCollection();//保留成交量数据的集合
            TimeSeries seriesOfVol = new TimeSeries("成交量");

            //循环写入数据
            for (int i = 0; i < prices.size(); i++) {
//                seriesOfFsPrice.add(new Millisecond(timeTicks.get(i)), prices.get(i));
//                seriesOfVol.add(new Millisecond(timeTicks.get(i)), vols.get(i));

                seriesOfFsPrice.add(new Minute(timeTicks.get(i)), prices.get(i));
                seriesOfVol.add(new Minute(timeTicks.get(i)), vols.get(i)); // 使用分钟, 成交量会更宽一些
                seriesOfAvgPrice.add(new Minute(timeTicks.get(i)), avgPrices.get(i)); // 使用分钟, 成交量会更宽一些
            }

            Date today = timeTicks.get(0); // 无视哪一天, 不重要, 就取解析结果第一个即可;
            seriesOfPreClose.add(new Day(today), preClose);
            seriesOfPreClose.add(new Day(DateUtil.offsetDay(today, 1)), preClose);

            //分时图数据
            lineSeriesCollection.addSeries(seriesOfFsPrice);
            if (showAvgPrice) {
                lineSeriesCollection.addSeries(seriesOfAvgPrice);
            }
            lineSeriesCollection.addSeries(seriesOfPreClose);

            //成交量数据
            barSeriesCollection.addSeries(seriesOfVol);


            // 设置均线图画图器
            XYLineAndShapeRenderer lineAndShapeRenderer = new XYLineAndShapeRenderer();
            lineAndShapeRenderer.setBaseItemLabelsVisible(true);
            lineAndShapeRenderer.setSeriesShapesVisible(0, false);//设置不显示数据点模型
            lineAndShapeRenderer.setSeriesShapesVisible(1, false);
            lineAndShapeRenderer.setSeriesPaint(0, priceColorFs);//设置均线颜色
            lineAndShapeRenderer.setSeriesPaint(1, preCloseColorFs);
            lineAndShapeRenderer.setSeriesPaint(1, avgPriceColorFs);
            lineAndShapeRenderer.setBaseSeriesVisibleInLegend(false);


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


            domainAxis.setAutoTickUnitSelection(false);//设置不采用自动选择刻度值
            domainAxis.setTickMarkPosition(DateTickMarkPosition.START);//设置标记的位置
            domainAxis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());// 设置标准的时间刻度单位

            domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 30));// 设置时间刻度的间隔
            domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));//设置时间格式


            SegmentedTimeline timeline = SegmentedTimeline
                    .newFifteenMinuteTimeline(); // 设置时间线显示的规则，用这个方法摒除掉周六和周日这些没有交易的日期
            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 31);
            calendar.set(Calendar.SECOND, 0);
            sda = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 59);
            da = calendar.getTime();
            timeline.addException(sda.getTime(), da.getTime()); // 排除非交易时间段
            domainAxis.setTimeline(timeline);


            // 设置k线图y轴参数
            NumberAxisYSupportTickToPreClose y1Axis = new NumberAxisYSupportTickToPreClose();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y1Axis.setAutoRange(false);//设置不采用自动设置数据范围
            y1Axis.setLabel(String.valueOf(preClose));
            y1Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
            double t = preClose - priceLow;
            double t1 = priceHigh - preClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            double range = t1 > t ? t1 : t;//计算涨跌最大幅度
            DecimalFormat df1 = new DecimalFormat("#0.00");
            df1.setRoundingMode(RoundingMode.FLOOR);


            y1Axis.setRange(Double.valueOf(df1.format(preClose - range)),
                    Double.valueOf(df1.format(preClose + range)));//设置y轴数据范围
            y1Axis.setNumberFormatOverride(df1);
            y1Axis.centerRange(preClose);
            NumberTickUnit numberTickUnit = new NumberTickUnit(Math.abs(range / 7));
            y1Axis.setTickUnit(numberTickUnit);


            NumberAxisYSupportTickMultiColor y2Axis = new NumberAxisYSupportTickMultiColor();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y2Axis.setAutoRange(false);//设置不采用自动设置数据范围
            y2Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));


            t = (priceLow - preClose) / preClose;
            t1 = (priceHigh - preClose) / preClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            range = t1 > t ? t1 : t;
            y2Axis.setRange(-range, range);//设置y轴数据范围
            y2Axis.setTickLabelPaint(Color.red);
            DecimalFormat df2 = new DecimalFormat("#0.00%");
            df2.setRoundingMode(RoundingMode.FLOOR);
            y2Axis.setNumberFormatOverride(df2);
            NumberTickUnit numberTickUnit2 = new NumberTickUnit(Math.abs(range / 7));
            y2Axis.setTickUnit(numberTickUnit2);


            //生成画图细节 第一个和最后一个参数这里需要设置为null，否则画板加载不同类型的数据时会有类型错误异常
            //可能是因为初始化时，构造器内会把统一数据集合设置为传参的数据集类型，画图器可能也是同样一个道理
            XYPlot plot1 = new XYPlot(lineSeriesCollection, domainAxis, null, lineAndShapeRenderer);
            plot1.setBackgroundPaint(bgColorFs);//设置曲线图背景色
            plot1.setDomainGridlinesVisible(false);//不显示网格
            plot1.setRangeGridlinePaint(preCloseColorFs);//设置间距格线颜色为红色, 同昨收颜色
            plot1.setRangeAxis(0, y1Axis);
            plot1.setRangeAxis(1, y2Axis);


            //设置柱状图参数
            XYBarRenderer barRenderer = new XYBarRenderer() {
                private static final long serialVersionUID = 1L;// 为了避免出现警告消息，特设定此值

                @Override
                public Paint getItemPaint(int i, int j) { // 匿名内部类用来处理当日的成交量柱形图的颜色与K线图的颜色保持一致
                    if (j == 0) {
                        if (prices.get(j) > preClose) {
                            return upColorFs;
                        } else if (prices.get(j) < preClose) {
                            return downColorFs;
                        } else {
                            return equalColorFs;
                        }
                    } else {
                        if (prices.get(j) > prices.get(j - 1)) {
                            return upColorFs;
                        } else if (prices.get(j) < prices.get(j - 1)) {
                            return downColorFs;
                        } else {
                            return equalColorFs;
                        }
                    }
                }
            };


            barRenderer.setDrawBarOutline(true);//设置显示边框线
            barRenderer.setBarPainter(new StandardXYBarPainter());//取消渐变效果
            barRenderer.setMargin(0.5);//设置柱形图之间的间隔
            barRenderer.setDrawBarOutline(false);
            barRenderer.setSeriesVisibleInLegend(false);//设置不显示legend（数据颜色提示)
            barRenderer.setShadowVisible(false);//设置没有阴影


            //设置柱状图y轴参数
            NumberAxis y3Axis = new NumberAxis();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
            y3Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));//设置y轴字体
            y3Axis.setAutoRange(true);//设置采用自动设置时间范围
            y3Axis.setTickLabelPaint(volTickLabelPaint);//设置y轴刻度值颜色
            y3Axis.setNumberFormatOverride(new NumberFormatCnForBigNumber()); // 数据轴数据标签的显示格式

            //这里不设置x轴，x轴参数依照k线图x轴为模板
            XYPlot plot2 = new XYPlot(barSeriesCollection, null, y3Axis, barRenderer);
            plot2.setBackgroundPaint(bgColorFs);//设置曲线图背景色
            plot2.setDomainGridlinesVisible(false);//不显示网格
            plot2.setRangeGridlinePaint(preCloseColorFs);//设置间距格线颜色为红色


            //建立一个恰当的联合图形区域对象，以x轴为共享轴
            CombinedDomainXYPlot domainXYPlot = new CombinedDomainXYPlot(domainAxis);//
            domainXYPlot.add(plot1, weight1OfTwoPlotOfFs);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
            domainXYPlot.add(plot2, weight2OfTwoPlotOfFs);
            domainXYPlot.setGap(gapOfTwoPlotOfFs);//设置两个图形区域对象之间的间隔空间

            plot1.setBackgroundPaint(bgColorFs);
            plot2.setBackgroundPaint(bgColorFs);
            domainXYPlot.setBackgroundPaint(bgColorFs);
            //生成图纸
            JFreeChart chart = new JFreeChart(title, new Font("微软雅黑", Font.BOLD, 24), domainXYPlot, true);
            chart.setBackgroundPaint(bgColorFs);
            chart.setTitle(
                    new TextTitle(title, new Font("华文行楷", Font.BOLD | Font.ITALIC, 18), Color.red,
                            Title.DEFAULT_POSITION,
                            Title.DEFAULT_HORIZONTAL_ALIGNMENT,
                            Title.DEFAULT_VERTICAL_ALIGNMENT, Title.DEFAULT_PADDING));
            return chart;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 支持分时图y轴数据刻度 显示多种颜色
     */
    private static class NumberAxisYSupportTickMultiColor extends NumberAxis {
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
                        g2.setPaint(upColorLabelKLine);
                    } else if (tick.getValue() == 0) {
                        g2.setPaint(equalColorLabelKLine);
                    } else {
                        g2.setPaint(downColorLabelKLine);
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
    private static class NumberAxisYSupportTickToPreClose extends NumberAxis {
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
                        g2.setPaint(upColorLabelKLine);
                    } else if (tickValue == yClose) {
                        g2.setPaint(equalColorLabelKLine);
                    } else {
                        g2.setPaint(downColorLabelKLine);
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


}
