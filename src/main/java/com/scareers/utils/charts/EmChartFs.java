package com.scareers.utils.charts;

import cn.hutool.core.date.*;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.ManipulateLogPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.ReviseAccountWithOrder.BuySellPointRecord;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
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
import org.jfree.ui.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.RoundingMode;
import java.text.*;
import java.util.List;
import java.util.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_SCROLL_BAR_THUMB;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.dummyBuySellOperationSleep;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.dummyClinchOccurSleep;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.ReviseAccountWithOrder.prohibitCostPriceUpdateMap;
import static com.scareers.utils.CommonUtil.waitForever;

/**
 * description: 同花顺数据源的图表; 主要是分时图 和 3个周期的k线图
 *
 * @author: admin
 * @date: 2022/4/5/005-00:48:19
 */
public class EmChartFs {
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
    public static Color priceColorFsOfIndex = new Color(255, 128, 255); // 指数分时线颜色 -- 紫色
    public static Color priceColorFsOfStock = new Color(44, 157, 199);
    ; // 正股分时颜色 -- 蓝色 , 都抄同花顺
    public static Color avgPriceColorFs = new Color(240, 248, 136);
    public static Color preCloseColorFs = new Color(128, 0, 0);
    public static Color bgColorFs = new Color(7, 7, 7);

    // 分时图形状控制!
    public static final int gapOfTwoPlotOfFs = 0; // 分时价格和成交量,两个图的gap
    public static final int weight1OfTwoPlotOfFs = 650; // 两大weight, 控制 分时价格图 和成交量图, 高度比例
    public static final int weight2OfTwoPlotOfFs = 200; // 两大weight+gap, 可以绝对判定 鼠标位置!


    public static void main(String[] args) throws Exception {

//        kLineDemo();
//
        fsV2Demo();
//        String bondCode = "123134"; // 卡倍转债
//        SecurityBeanEm bondBean = SecurityBeanEm.createBond(bondCode);
//        String dateStr = "2022-06-06";
//        Console.log(bondBean.getName());
//        SecurityBeanEm indexBean = SecurityBeanEm.getShangZhengZhiShu();
//        SecurityBeanEm stockBean = SecurityBeanEm.createStock("300863");//卡倍忆
//        DynamicEmFs1MV2ChartForRevise dynamicChart = new DynamicEmFs1MV2ChartForRevise(bondBean,
//                dateStr, indexBean,
//                stockBean);
//
//        double timeRate = 5;
//        ThreadUtil.execAsync(new Runnable() {
//            @Override
//            public void run() {
//                List<DateTime> allFsTransTimeTicks = CommonUtil.generateMarketOpenTimeListHms(false);
//                for (int i = 1000; i < allFsTransTimeTicks.size(); i++) {
//                    Date tick = allFsTransTimeTicks.get(i);
//                    ThreadUtil.sleep((long) (1000 / timeRate));
//                    Console.log("即将刷新");
//                    dynamicChart.updateChartFsTrans(tick, dynamicChart.preClose * 1.01, null); // 重绘图表
//                }
//            }
//        }, true);
//
//
//        long timeX = getTimeX(dynamicChart.todayDummy, 9, 35, 30);
//        double y = 450.0;
//
//        XYPointerAnnotation annotation = new XYPointerAnnotation("B", timeX, y, Math.PI * 1.5);
////        XYPointerAnnotation annotation = new XYPointerAnnotation("B", timeX, y, 3.1415 / 2);
//        annotation.setPaint(Color.white);  // 文字颜色
//        annotation.setArrowPaint(Color.yellow); // 整个箭头颜色
//        float[] dashs = {2, 2}; // 箭头直线部分笔触
//        annotation.setArrowStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dashs, 0));
//        annotation.setArrowWidth(4); // 包含箭头整个宽度, 不能1
//        annotation.setArrowLength(5); // 箭头长度
//        annotation.setLabelOffset(3); // 文字和箭头 尾部的距离, 需要和 箭头长度匹配好看
////        annotation.setTextAnchor(TextAnchor.BASELINE_CENTER); // 文字位置
//        annotation.setTextAnchor(TextAnchor.BOTTOM_CENTER); // 文字位置
//
//        dynamicChart.showChartSimple(); // 显示
//
//        dynamicChart.plot1.addAnnotation(annotation);
//        waitForever();
    }

    private static long getTimeX(Date today, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 50);
        Date time = calendar.getTime();
        return time.getTime(); // 时间戳!
    }

    /**
     * 东财分时1分钟 分时图 -- 动态复盘,
     * 1.分时图使用v2版本数据, 即单日 241条!
     * 2.且需要提供分时成交数据, 以便模拟实盘的动态变化!
     * 3.将chart绘制逻辑实现为单个方法, 相关组件对象 保存为 属性! 以便动态修改
     * 4.数据从数据库获得!
     * 5.@noti: 成交量的y轴, 自动设置的 range; 无需计算
     */
    @Data
    @NoArgsConstructor
    public static class DynamicEmFs1MV2ChartForRevise {
        public static double redundancyPriceRangePercent = 0.002; // 价格上下限, 比最高最低价, 多出来的部分; 使得图表上下限更明显
        public static int redundancyPutDataAmount = 50; // 首次put时, 多添加历史n条数据
        public static int tickLogPanelWidthDefault = 420; // tick打印面板的总宽度,含滚动条

        // 基本属性

        SecurityBeanEm beanEm; // 东财资产对象, 转债bean
        //@add: 添加2线,构造器传递参数; 可null,
        SecurityBeanEm indexBean; // 对应大指数 --> 非转债均可null
        SecurityBeanEm stockBean; // 对应正股
        String dateStr; // 标准日期字符串 "2022-06-02";


        // 数据属性
        DataFrame<Object> fsDfV2Df; // 东财分时图v2 -- 完整
        DataFrame<Object> fsTransDf; // 东财分时成交! 这些数据本质上并不绘图, 用于动态更新最后一根图! -- 完整
        // @add: 对应4df, 东财数据库api实现对应api; 因价格被放进一个图中, 因此本质是求 展示涨跌幅;
        // @key: 将指数和正股的价格, 线转换为自身涨跌幅, 再 (等价转换为使用转债 preClose )为基准的价格, 才能放进一个chart
        DataFrame<Object> fsDfV2DfOfIndex; //
        DataFrame<Object> fsTransDfOfIndex; //
        DataFrame<Object> fsDfV2DfOfStock; // --> 仅转换了close 列
        DataFrame<Object> fsTransDfOfStock; //  --> 转换了price列

        // 4项数据完整列表
        List<DateTime> allFsTimeTicks; // 分时tick, 日期对象形式
        List<String> allFsDateStr; // 分时tick 字符串形式, 方便查找筛选
        List<Double> allPrices;
        // @todo: bugfix-- 实在找不出来原因
        // @todo: bugfix -- 已经解决: 因为使用 subList, 然后向子列表添加了数据项, 导致原列表也添加了元素; 应当新建列表对象添加
        // 备份 allPrices; 可能因为 allPrices 参与了Bar渲染器, 导致 allPrices会被 莫名修改? 因此使用备份,使得分时成交模式,能正确访问前一分钟收盘价
        // @noti: allPrices用于更新priceLow和High, allPricesTemp用于更新序列数据,allPricesTemp2用于成交量颜色
        List<Double> allPricesTemp;
        List<Double> allPricesTemp2; // 备份2, 仅用于成交量颜色渲染, 如果不用, 则颜色显示也将异常

        List<Double> allAvgPrices;
        List<Double> allVols;
        List<Double> allPricesOfIndex;
        List<Double> allPricesOfStock;

        List<String> allFsTransTimeTicks; // 所有分时成交的时间tick, 方便查找
        HashMap<String, Integer> allFsTransTimeTicksMap = new HashMap<>(); // 所有分时成交的时间tick,以及对应的索引, 方便查询, 以免遍历查询,太伤
        // @add:
        List<String> allFsTransTimeTicksOfIndex;
        HashMap<String, Integer> allFsTransTimeTicksMapOfIndex = new HashMap<>();
        List<String> allFsTransTimeTicksOfStock;
        HashMap<String, Integer> allFsTransTimeTicksMapOfStock = new HashMap<>();

        Double preClose; // 自动解析
        private Double preCloseOfIndex;
        private Double preCloseOfStock;


        // 核心属性
        String filterTimeTick = "09:10"; // 筛选时间tick, 默认值即不筛选数据; 通过改变此值后,调用updateFsDfShow(),更新显示df
        int filterIndex = -1; // 当设置 timeTick时, 将自动更新本属性, 代表了在全数据中, 当前对应筛选的index, 包含! // 通常sub需要+1
        int preFilterIndex = -2; // 记录此前的filterIndex, 调用筛选时更新; 在更新数据时, 对序列的更新, 可判定两个index的关系, 而add或者delete数据


        /**
         * 构造器
         * 参数可null, 则需要主动调用 init方法, 传递非null参数
         *
         * @param securityBeanEm
         * @param dateStr
         */
        public DynamicEmFs1MV2ChartForRevise(SecurityBeanEm beanEm, String dateStr, SecurityBeanEm indexBean,
                                             SecurityBeanEm stockBean) {
            init(beanEm, dateStr, indexBean, stockBean);
        }

        public void init(SecurityBeanEm beanEm, String dateStr, SecurityBeanEm indexBean,
                         SecurityBeanEm stockBean) {
            this.beanEm = beanEm;
            this.dateStr = dateStr;
            this.indexBean = indexBean;
            this.stockBean = stockBean;

            initDataAndAttrs(); // 自动初始化数据 以及 相关字段 // 主要时间消耗

            initChart(); // 初始化图表相关所有对象

            initTick3sLogPanel();

            // @add: 持仓成本线
            initCostPriceMarker();
        }

        /**
         * 3秒tick数据显示区; jScrollPaneForTickLog 将被加入显示区右侧
         */
        public void initTick3sLogPanel() {
            if (logTextPane == null) { // 全实例共用一个logPane; 只初始化一次
                ManipulateLogPanel displayForLog = new ManipulateLogPanel();
                logTextPane = displayForLog.getLogTextPane(); // 3stick显示框对象!
                logTextPane.setBackground(new Color(0, 0, 0));
                jScrollPaneForTickLog = new JScrollPane(logTextPane);
                jScrollPaneForTickLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                BasicScrollBarUIS
                        .replaceScrollBarUI(jScrollPaneForTickLog, COLOR_THEME_TITLE,
                                COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
            }
            firstPutting = true;
        }

        /**
         * 需主动调用, 载入两大df数据
         */
        public void initDataAndAttrs() {
            // 1.数据库两项df

            this.fsDfV2Df = EastMoneyDbApi
                    .getFs1MV2ByDateAndQuoteId(dateStr, beanEm.getQuoteId()); // 主要时间消耗少
            this.fsTransDf = EastMoneyDbApi
                    .getFsTransByDateAndQuoteId(dateStr, beanEm.getQuoteId()); // 主要时间消耗多
            // 1.2. 昨收 -- index/stock 的 close 和price, 将被用来缩放, 以便放在同一chart
            this.preClose = Double.valueOf(fsDfV2Df.get(0, "preClose").toString());

            this.fsDfV2DfOfIndex = EastMoneyDbApi
                    .getFs1MV2ByDateAndQuoteIdAdaptedOnlyClose(dateStr, indexBean.getQuoteId(), preClose); // 主要时间消耗少
            this.preCloseOfIndex = Double.valueOf(fsDfV2DfOfIndex.get(0, "preClose").toString());
            this.fsTransDfOfIndex = EastMoneyDbApi
                    .getFsTransByDateAndQuoteIdSAdapted(dateStr, indexBean.getQuoteId(), false, preCloseOfIndex,
                            preClose); //
            // 主要时间消耗多
            this.fsDfV2DfOfStock = EastMoneyDbApi
                    .getFs1MV2ByDateAndQuoteIdAdaptedOnlyClose(dateStr, stockBean.getQuoteId(), preClose); // 主要时间消耗少
            this.preCloseOfStock = Double.valueOf(fsDfV2DfOfStock.get(0, "preClose").toString());
            this.fsTransDfOfStock = EastMoneyDbApi
                    .getFsTransByDateAndQuoteIdSAdapted(dateStr, stockBean.getQuoteId(), false, preCloseOfStock,
                            preClose);


            // 2. 4项数据完整列表
            this.allFsTimeTicks = DataFrameS.getColAsDateList(fsDfV2Df, "date"); // Date形式, 241个tick
            this.allFsDateStr = DataFrameS.getColAsStringList(fsDfV2Df, "date"); // 字符串形式

            this.allPrices = new ArrayList<>(DataFrameS.getColAsDoubleList(fsDfV2Df, "close"));
            this.allPricesTemp = new ArrayList<>(DataFrameS.getColAsDoubleList(fsDfV2Df, "close")); // 备份
            this.allPricesTemp2 = new ArrayList<>(DataFrameS.getColAsDoubleList(fsDfV2Df, "close")); // 备份2,用于成交量颜色

            this.allAvgPrices = DataFrameS.getColAsDoubleList(fsDfV2Df, "avgPrice");
            // @add: 指数和正股只需要 等价price放入同一chart; 性质上几乎等同于 avgPrice 均价线; + 分时成交tick变化
            this.allPricesOfIndex = DataFrameS.getColAsDoubleList(fsDfV2DfOfIndex, "close");
            this.allPricesOfStock = DataFrameS.getColAsDoubleList(fsDfV2DfOfStock, "close");

            this.allVols = DataFrameS.getColAsDoubleList(fsDfV2Df, "vol");

            // 3.分时成交时间戳列表
            this.allFsTransTimeTicks = DataFrameS.getColAsStringList(fsTransDf, "time_tick"); // 时分秒
            for (int i = 0; i < allFsTransTimeTicks.size(); i++) {
                allFsTransTimeTicksMap.put(allFsTransTimeTicks.get(i), i);
            }
            // 3.1. 同样, 指数和正股的tick和索引, 也保存一下! 只需要注意访问价格从 转换后的价格列表访问即可
            this.allFsTransTimeTicksOfIndex = DataFrameS.getColAsStringList(fsTransDfOfIndex, "time_tick"); // 时分秒
            for (int i = 0; i < allFsTransTimeTicksOfIndex.size(); i++) {
                allFsTransTimeTicksMapOfIndex.put(allFsTransTimeTicksOfIndex.get(i), i);
            }
            this.allFsTransTimeTicksOfStock = DataFrameS.getColAsStringList(fsTransDfOfStock, "time_tick"); // 时分秒
            for (int i = 0; i < allFsTransTimeTicksOfStock.size(); i++) {
                allFsTransTimeTicksMapOfStock.put(allFsTransTimeTicksOfStock.get(i), i);
            }

            todayDummy = allFsTimeTicks.get(0); // 虚假的今天

            fsTransNewestPrice = null; // 默认初始分时成交价格, 显然是不对的
            priceLow = preClose * 0.99; // 默认图表价格下限
            priceHigh = preClose * 1.01; // 默认图表价格下限  // 指数和正股的 "价格"列, 已经适配了!
        }

        private static final Log log = LogUtil.getLogger();
        protected ValueMarkerS markerYForCostPrice; // 持仓成本线!

        private void initCostPriceMarker() {
            float[] dashs = {2, 3};
            BasicStroke basicStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashs,
                    0);

            // 2.价格图的ymarker横线
            markerYForCostPrice = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 初始化为最小值, 不会显示
            markerYForCostPrice.setType(ValueMarkerS.Type.HOLD_COST);
            markerYForCostPrice.setLabelOffsetType(LengthAdjustmentType.EXPAND);
            markerYForCostPrice.setPaint(Color.red); //线条颜色
            markerYForCostPrice.setStroke(basicStroke);
            markerYForCostPrice.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
            markerYForCostPrice.setLabelPaint(new Color(255, 143, 0)); // 同花顺
            markerYForCostPrice.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            markerYForCostPrice.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
            // markerY.setLabel(decimalFormatForPercent.format(markerValueY)); // 线条上显示的文本
        }


        /**
         * 执行筛选, 核心方法, 参数正确设置返回true
         * 参数要求: 任意可被 hutool 解析为date对象的字符串, 将被转换为 标准的 HH:mm 样式
         *
         * @param timeTick
         */
        public boolean setFilterTimeTick(String timeTick) {
            DateTime date = null;
            try {
                date = DateUtil.parse(timeTick);
            } catch (Exception e) {
                log.error("设置筛选时间tick失败, 应当可被hutool的DateUtil解析, 而参数为:{}", timeTick);
                return false;
            }
            this.filterTimeTick = DateUtil.format(date, "HH:mm");
            preFilterIndex = filterIndex; // 保留此前filter, 再更新现filter
            filterIndex = this.allFsDateStr.indexOf(this.dateStr + " " + this.filterTimeTick); // 修改index
            return true;
        }

        /**
         * 也可传递date筛选, 同样, 仅 时分 有效
         *
         * @param date
         * @return
         */
        public boolean setFilterTimeTick(Date date) {
            if (date == null) {
                log.error("设置筛选时间tick失败, 日期对象不可null");
                return false;
            }
            this.filterTimeTick = DateUtil.format(date, "HH:mm");
            preFilterIndex = filterIndex; // 保留此前filter, 再更新现filter
            if ("15:00".compareTo(filterTimeTick) < 0) {
                return false; // 超过3点无视
            }
            // @noti: 解决 分时成交有 13:00:xx, 但分时没有的问题, 强制修复bug;
            if ("13:00".compareTo(filterTimeTick) == 0) { //  因为分时去掉了 13:00; fs成交却有
                filterIndex = 120; // 一半?
            } else { // 常态!
                filterIndex = this.allFsDateStr.indexOf(this.dateStr + " " + this.filterTimeTick); // 修改index
            }
            return true;
        }

        /*
         图表相关属性
         */
        JFreeChart chart; // 图表对象

        // 两大序列集合
        TimeSeriesCollection lineSeriesCollection = new TimeSeriesCollection(); // 均价,价格,昨收 3序列集合
        TimeSeriesCollection barSeriesCollection = new TimeSeriesCollection(); // 成交量数据的集合
        // 4大数据序列, 更新数据, 调用 add 或者 delete方法. 前3者时间戳需要一致更新
        TimeSeries seriesOfFsPrice = new TimeSeries("分时数据");
        // @add. 新增指数和正股价格线 序列 -- 同样241
        TimeSeries seriesOfFsPriceOfIndex = new TimeSeries("指数分时数据");
        TimeSeries seriesOfFsPriceOfStock = new TimeSeries("正股分时数据");

        TimeSeries seriesOfAvgPrice = new TimeSeries("均价");
        TimeSeries seriesOfVol = new TimeSeries("成交量");
        TimeSeries seriesOfPreClose = new TimeSeries("昨日收盘价");

        // 价格图折线渲染器
        XYLineAndShapeRenderer lineAndShapeRenderer = buildPlot1LineRenderer();
        // 时间x轴
        DateAxis domainAxis;
        // y轴1--价格轴
        NumberAxisYSupportTickToPreClose y1Axis = new NumberAxisYSupportTickToPreClose();
        // y轴2--涨跌幅轴
        NumberAxisYSupportTickMultiColor y2Axis = new NumberAxisYSupportTickMultiColor();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
        // 价格图
        XYPlot plot1;
        // 成交量图柱状渲染器
        XYBarRenderer barRenderer;
        // 成交量图 y轴
        NumberAxis y3Axis = new NumberAxis();
        XYPlot plot2; // 成交量图

        // 价格上下限, 依然随着filter而可能改变, 带默认值 ,init中会初始化
        double priceLow;
        double priceHigh;

        // 方便记录today.
        Date todayDummy;


        /**
         * 依据当前筛选值, 更新5大数据序列的数据;  昨收不用更新
         */
        private void updateFiveSeriesData() {
            if (filterIndex == preFilterIndex) {
                return; // 未重新筛选
            }
            if (filterIndex <= -1) {
                // 此时应当清空5大序列, 算是特殊情况
                seriesOfFsPrice.clear();
                seriesOfFsPriceOfIndex.clear();
                seriesOfFsPriceOfStock.clear();
                seriesOfVol.clear();
                seriesOfAvgPrice.clear();
                return;
            }

            // 已经正常筛选, 此时 preFilterIndex, 至少会是 -1, 而不可能默认的 -2; filterIndex则至少为 0, 或者-1
            if (filterIndex > preFilterIndex) { // 新增数据, 数量为差值, 注意第一个应当是 preFilterIndex
                for (int i = preFilterIndex + 1; i < filterIndex + 1; i++) {
                    Minute tick = new Minute(allFsTimeTicks.get(i));
                    seriesOfFsPrice.addOrUpdate(tick, allPricesTemp.get(i));
                    seriesOfFsPriceOfIndex.addOrUpdate(tick, allPricesOfIndex.get(i)); // @add
                    seriesOfFsPriceOfStock.addOrUpdate(tick, allPricesOfStock.get(i)); // @add
                    seriesOfVol.addOrUpdate(tick, allVols.get(i)); // 使用分钟, 成交量会更宽一些
                    seriesOfAvgPrice.addOrUpdate(tick, allAvgPrices.get(i)); // 使用分钟, 成交量会更宽一些
                }
            } else if (filterIndex < preFilterIndex) {// 应删除数据
                // 注意, 该方法 前后都包括, 因此.
                seriesOfFsPrice.delete(filterIndex + 1, preFilterIndex);
                seriesOfFsPriceOfIndex.delete(filterIndex + 1, preFilterIndex);
                seriesOfFsPriceOfStock.delete(filterIndex + 1, preFilterIndex);
                seriesOfVol.delete(filterIndex + 1, preFilterIndex);
                seriesOfAvgPrice.delete(filterIndex + 1, preFilterIndex);
            } // 相等则不变
        }

        /**
         * 更新数据到下一个tick显示, 此时已经保证必有下一tick; 且整数分钟部分已经更新, 只需要更新多出来的1分钟的tick
         *
         * @update : 添加了2个参数, 即 指数和正股的最新分时成交价格
         * @noti : 价格都可能null, 注意判定
         */
        private void updateThreeSeriesDataFsTrans(Double fsTransPrice, double alreadySureVol,
                                                  Double fsTransPriceOfIndex, Double fsTransPriceOfStock
        ) {
            if (filterIndex + 1 >= allFsTimeTicks.size()) {
                return; // 已经没有下一tick可以更新, 收盘了
            }
            Minute tick = new Minute(allFsTimeTicks.get(filterIndex + 1)); // 新价格更新到下一tick
            if (fsTransPrice != null) {
                seriesOfFsPrice.addOrUpdate(tick, fsTransPrice); // 价格和成交量, 更新为给定参数
            }
            seriesOfVol.addOrUpdate(tick, alreadySureVol); //
            if (fsTransPriceOfIndex != null) {
                seriesOfFsPriceOfIndex.addOrUpdate(tick, fsTransPriceOfIndex); // 价格和成交量, 更新为给定参数
            }
            if (fsTransPriceOfStock != null) {
                seriesOfFsPriceOfStock.addOrUpdate(tick, fsTransPriceOfStock); // 价格和成交量, 更新为给定参数
            }

            try {
                seriesOfAvgPrice.addOrUpdate(tick, allAvgPrices.get(filterIndex)); // 均价更新为同前1均价, 单纯为了好看
            } catch (Exception e) {
                // 11:30 - 13:00 filterINdex为-1, 出错 ; 出错无视
            }
        }

        /**
         * 首次 初始化chart对象! 默认筛选将无数据;
         * 请调用 updateChart() 更新图像
         * // 初始状况, filterIndex = -1; preFilterIndex = -2;
         *
         * @return
         */
        private void initChart() {
            // 1.将4序列加入 2 序列集合 -- 执行一次
            lineSeriesCollection.addSeries(seriesOfFsPrice);
            lineSeriesCollection.addSeries(seriesOfAvgPrice);
            lineSeriesCollection.addSeries(seriesOfPreClose);
            // 1.1. 新增了2个序列 -- 指数和正股的 价格线(动态) -- 注意顺序为 3,4
            lineSeriesCollection.addSeries(seriesOfFsPriceOfIndex);
            lineSeriesCollection.addSeries(seriesOfFsPriceOfStock); // 默认初始化, 未添加数据罢了!

            // 2.昨收序列首次加载后将不再更新
            Date today = allFsTimeTicks.get(0); // 无视哪一天, 不重要, 就取解析结果第一个即可;
            seriesOfPreClose.add(new Day(today), preClose);
            seriesOfPreClose.add(new Day(DateUtil.offsetDay(today, 1)), preClose);
            barSeriesCollection.addSeries(seriesOfVol);

            // 3.序列加载数据
            updateFiveSeriesData();

            // 4.刷新价格上下限 -- 可无
            updatePriceLowAndHigh();

            // 5.x时间轴初始化      // @add: 5/6/7 不变
            initDomainDateTimeAxis();
            // 6.y1轴 -- 数字轴 -- 自定义类, 实现以昨收盘价为中心描写刻度数据 -- 价格轴
            initY1AxisOfPrice();
            // 7.y2轴, 类似, 双颜色区分. 百分比显示 -- 涨跌幅轴
            initY2AxisOfChgPct();

            // 8.plot1 对象创建
            initPlot1();

            // 9.(图2)成交量柱状图渲染器
            initBarRenderer();

            // 10. 成交量图 y轴 单纯数据轴
            initY3AxisForVol();
            // 11. plot 对象创建
            initPlot2();
            // 12. chart对象创建
            initChartFinal();
        }

        /**
         * 筛选更新图表, 将只更新数据序列, 上下界等 动态属性!
         */
        public void updateChart(Date date) {
            if (this.beanEm == null || this.dateStr == null) {
                return;
            }
            boolean b = this.setFilterTimeTick(date);
            if (b) {
                // 1.刷新价格上下限 -- 可无
                updatePriceLowAndHigh();
                // 2.更新两个y轴的上下界!
                updateY1AxisRange();
                updateY2AxisRange();
                // 3.序列数据在更新上下界后更新
                updateFiveSeriesData();
            } else {
                log.error("设置筛选tick失败, 无法更新图表");
            }
        }

        private Double fsTransNewestPrice; // 暂存分时成交最新价格, 仅仅用于设置 成交量的颜色

        /**
         * 针对分时成交3s的更新!
         * 给定时间对象:
         * 1.首先取到 HH:mm:ss,
         * 2.取整数分钟, 首先更新图表到 HH:mm, 即已过分钟全部更新
         * 3.对于秒数 ss, 则从 fsTransDf 数据中, 读取最新的 <=该秒数的, 最后一个 tick,的价格, 作为最新价格
         * 读取 HH:mm:00 - HH:mm:ss 的所有成交量数据(是否*10,要根据底层数据api而定), 求和, 作为最新分钟的成交量!
         * 而 均价的话, 则复制 前一个分钟的 均价, 而不用未来数据, 这样保持3数据序列一样长!
         *
         * @param date
         * @update: 增加了指数和正股价格线 且实时变化
         */
        public void updateChartFsTrans(Date date, Double costPriceMaybe,
                                       List<BuySellPointRecord> bsPoints) {
            if (this.beanEm == null || this.dateStr == null) {
                return;
            }

            // 1.整秒数
            updateChart(date); // 将完美更新整数秒
            boolean b = this.setFilterTimeTick(date);

            if (!b) {
                log.error("设置筛选tick失败, 无法更新图表");
                return;
                // 1.刷新价格上下限 -- 可无
            }
            String timeTickStr = DateUtil.format(date, "HH:mm:ss");
            if (timeTickStr.compareTo("15:00:00") > 0) { // @update: 原来有 ==
                return; // 3点后不再更新
            }

            // 2.首先查找fs成交数据对应合适的索引 -- 不超过给定的日期的最后一条数据, 要求 本分钟内的!
            Integer fsTransIndexShouldOfBond = getMaxExistsTimeTickIndexInOneMinute(date, timeTickStr,
                    allFsTransTimeTicksMap);
            Integer fsTransIndexShouldOfIndex = getMaxExistsTimeTickIndexInOneMinute(date, timeTickStr,
                    allFsTransTimeTicksMapOfIndex);
            Integer fsTransIndexShouldOfStock = getMaxExistsTimeTickIndexInOneMinute(date, timeTickStr,
                    allFsTransTimeTicksMapOfStock);

            // 3.计算最新价格, 注意可能null
            Double newestPrice = null;
            if (fsTransIndexShouldOfBond != null) {
                newestPrice = Double.parseDouble(fsTransDf.get(fsTransIndexShouldOfBond, "price").toString()); // 转债最新价格
            }
            Double newestPriceOfIndex = null;
            if (fsTransIndexShouldOfIndex != null) {
                newestPriceOfIndex = Double
                        .parseDouble(fsTransDfOfIndex.get(fsTransIndexShouldOfIndex, "price").toString()); // 指数最新价格
            }
            Double newestPriceOfStock = null;
            if (fsTransIndexShouldOfStock != null) {
                newestPriceOfStock =
                        Double.parseDouble(fsTransDfOfStock.get(fsTransIndexShouldOfStock, "price").toString());
                // 正股最新价格
            }

            // 3.1. 转债已出现的单分钟内成交量总和, 和最新价格
            if (newestPrice != null) {
                fsTransNewestPrice = newestPrice; // 成交量颜色控制!
            }
            double volSumOfBond = getVolSumOfCurrentInOneMinute(date, timeTickStr);


            // 4.更新! 此时必有下一分钟的 tick
            // 4.1. 先更新整数分钟, 比起直接调用 updateChart的整数分钟更新, 用最新数据更新了上下限的刷新
            updatePriceLowAndHighFsTrans(Arrays.asList(newestPrice, newestPriceOfIndex, newestPriceOfStock));
            // 同样更新两个y轴的上下界!
            updateY1AxisRange();
            updateY2AxisRange();

            // 序列数据在更新上下界后更新; ---  即整数部分
            updateFiveSeriesData();

            // 4.2. 实时部分, 更新 各种分时成交最新价, 到下一个tick!
            // @noti: 增加线时, 要实时动态更新, 需要改写本方法, 添加新的参数, 即新线的最新价格!
            updateThreeSeriesDataFsTrans(newestPrice, volSumOfBond, newestPriceOfIndex, newestPriceOfStock);

            // 4.3. @add: 持仓线更新
            tryFlushCostPriceMarker(costPriceMaybe);

            // 4.4. @add: 买卖点更新
            tryFlushBSPoints(bsPoints);

            // 5. 有数据打印分时tick信息到logPanel
            if (fsTransIndexShouldOfBond != null) {
                put(fsTransIndexShouldOfBond);
            }
        }

        /**
         * 更新时, 也要尝试刷新 成本价格线; 使用 rangeMarker实现
         *
         * @param costPriceMaybe
         */
        private void tryFlushCostPriceMarker(Double costPriceMaybe) {
            Long lastClinchTimeStamp = prohibitCostPriceUpdateMap.get(beanEm.getSecCode()); // 最新的成交现实时间戳
            if (lastClinchTimeStamp != null) {
                if (System
                        .currentTimeMillis() - lastClinchTimeStamp < (30 + dummyBuySellOperationSleep + dummyClinchOccurSleep)) {
                    return; // 刚刚成交过, 延迟1-2s再更新成本线! 模拟实盘; 延迟机制靠 LRU缓存的key过期机制
                }
            }

            plot1.removeRangeMarker(markerYForCostPrice);
            if (costPriceMaybe != null) {
                markerYForCostPrice.setValue(costPriceMaybe);
                markerYForCostPrice.setLabel(dfOfChgPct.format(costPriceMaybe / preClose - 1));
                plot1.addRangeMarker(markerYForCostPrice);
            }

        }

        List<XYPointerAnnotation> xYPointerAnnotations = new ArrayList<>();
        public static Font annotationFont = new Font("楷体", Font.BOLD, 12);
        public static float[] dashs = {2, 2}; // 箭头直线部分笔触
        public static Stroke annotationArrowStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10,
                dashs, 0);

        /**
         * bsPoints 是已经按照成交tick 升序排列;
         *
         * @param bsPoints
         */
        public void tryFlushBSPoints(List<BuySellPointRecord> bsPoints) {
            for (XYPointerAnnotation xYPointerAnnotation : xYPointerAnnotations) {
                plot1.removeAnnotation(xYPointerAnnotation);
            }
            xYPointerAnnotations.clear(); // 清除原来买卖点
            if (bsPoints == null) {
                return;
            }
            for (BuySellPointRecord bsPoint : bsPoints) {
                // 1.正常文字, 即 B1-B4, S1-S4
                String text = ("buy".equals(bsPoint.getBs()) ? "B" : "S") + bsPoint.getPositionDenominator();
                double y = bsPoint.getClinchPrice();
                long x = convertTickToAnnotationXValue(bsPoint.getClinchTick());
                double angle;
                Color textColor;
                TextAnchor textAnchor;
                if (text.startsWith("B")) {
                    angle = Math.PI * 0.5; // 向上箭头买
                    textColor = Color.red;
                    textAnchor = TextAnchor.BOTTOM_CENTER;
                } else {
                    angle = Math.PI * 1.5;
                    textColor = Color.green;
                    textAnchor = TextAnchor.TOP_CENTER;
                }
                XYPointerAnnotation annotation = new XYPointerAnnotation(text, x, y, angle);
                annotation.setPaint(textColor);  // 文字颜色
                annotation.setArrowPaint(textColor); // 整个箭头颜色, 同文字
                annotation.setArrowStroke(annotationArrowStroke); // 箭头直线部分笔触
                annotation.setArrowWidth(4); // 包含箭头整个宽度, 不能1
                annotation.setArrowLength(8); // 箭头长度
                annotation.setLabelOffset(15); // 文字和箭头 尾部的距离, 需要和 箭头长度匹配好看
                annotation.setTextAnchor(textAnchor); // 文字位置

                xYPointerAnnotations.add(annotation);
            }

            for (XYPointerAnnotation xYPointerAnnotation : xYPointerAnnotations) {
                plot1.addAnnotation(xYPointerAnnotation, true);
            }
        }

        /**
         * 将 时分秒tick, 转换为 x时间坐标轴里面的值, 能用于 文字标注!
         * --> 本质是时间戳!
         *
         * @param clinchTick
         * @return
         */
        private long convertTickToAnnotationXValue(String clinchTick) {
            DateTime parse = DateUtil
                    .parse(DateUtil.format(todayDummy, DatePattern.NORM_DATE_PATTERN) + " " + clinchTick);
            return parse.getTime();
        }

        /**
         * 计算本分钟内, 转债已有成交额综合
         *
         * @param date
         * @param timeTickStr
         * @return
         */
        public Double getVolSumOfCurrentInOneMinute(Date date, String timeTickStr) {
            String lowTimeLimit = DateUtil.format(date, "HH:mm") + ":00"; // 成交额计算下限时间, >=此时间
            // 3.2. 计算最新的该分钟当前已出现的成交量之和
            DataFrame<Object> selectDf = fsTransDf.select(new DataFrame.Predicate<Object>() {
                @Override
                public Boolean apply(List<Object> value) {
                    String timeTick = value.get(3).toString();
                    if (timeTick.compareTo(lowTimeLimit) >= 0 && timeTick.compareTo(timeTickStr) <= 0) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            // 已出现的成交量之和!
            try {
                return CommonUtil.sumOfListNumber(DataFrameS.getColAsDoubleList(selectDf, "vol"));
            } catch (Exception e) {
                return 0.0; // 出错返回0
            }
        }

        /**
         * 核心方法之一, 给定date, timeTickStr为最大时刻, 找到在本分钟内, 最新的一条数据, 所对应的 在分时成交df中的 索引! 无返回null
         * 索引查找使用 map查找; 实例化时 索引map已经构建好了!
         *
         * @param date
         * @param timeTickStr
         * @param tickToIndexMap
         * @return
         */
        public Integer getMaxExistsTimeTickIndexInOneMinute(Date date, String timeTickStr,
                                                            HashMap<String, Integer> tickToIndexMap) {
            Integer fsTransIndexShould = null;
            DateTime date0 = DateUtil.parse(timeTickStr);
            String formatLimitTick = DateUtil.format(date, "HH:mm" + ":00");
            while (true) {
                if (!(DateUtil.format(date0, "HH:mm:ss").compareTo(formatLimitTick) >= 0)) {
                    break;
                }
                // 需要找本分钟内的
                String tick = DateUtil.format(date0, "HH:mm:ss");
                Integer index0 = tickToIndexMap.get(tick);
                if (index0 != null) { // 找到了, 则退出
                    fsTransIndexShould = index0;
                    break;
                } else {
                    // 没有找到, 则 往前一秒!
                    date0 = DateUtil.offset(date0, DateField.SECOND, -1);
                }
            }
            return fsTransIndexShould;
        }


        public void initChartFinal() {
            // 11.建立一个恰当的联合图形区域对象，共享x轴 -- 需要提供高度权重
            CombinedDomainXYPlot domainXYPlot = new CombinedDomainXYPlot(domainAxis);

            domainXYPlot.add(plot1, weight1OfTwoPlotOfFs);//添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
            domainXYPlot.add(plot2, weight2OfTwoPlotOfFs);
            domainXYPlot.setGap(gapOfTwoPlotOfFs);//设置两个图形区域对象之间的间隔空间
            // 12.背景色强制
            plot1.setBackgroundPaint(bgColorFs);
            plot2.setBackgroundPaint(bgColorFs);
            domainXYPlot.setBackgroundPaint(bgColorFs);

            // 13.实例化 chart对象
            this.chart = new JFreeChart(null, new Font("微软雅黑", Font.BOLD, 24), domainXYPlot, true);
            this.chart.setBackgroundPaint(bgColorFs);
        }

        public void initPlot2() {
            // 10.plot2-- 成交量图, 这里不设置x轴，将与plot共用x轴
            plot2 = new XYPlot(barSeriesCollection, null, y3Axis, barRenderer);
            plot2.setBackgroundPaint(bgColorFs);//设置曲线图背景色
            plot2.setDomainGridlinesVisible(false);//不显示网格
            plot2.setRangeGridlinePaint(preCloseColorFs);//设置间距格线颜色为红色

        }

        public void initY3AxisForVol() {
            y3Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));//设置y轴字体
            y3Axis.setAutoRange(true);//设置采用自动设置时间范围
            y3Axis.setTickLabelPaint(volTickLabelPaint);//设置y轴刻度值颜色
            y3Axis.setNumberFormatOverride(new EmChartFs.NumberFormatCnForBigNumber()); // 数据轴数据标签的显示格式
        }

        public void initBarRenderer() {
            barRenderer = new XYBarRenderer() {
                private static final long serialVersionUID = 1L;// 为了避免出现警告消息，特设定此值

                @Override
                public Paint getItemPaint(int i, int j) { // 匿名内部类用来处理当日的成交量柱形图的颜色与K线图的颜色保持一致

                    List<Double> tempDoubles = allPricesTemp2
                            .subList(0, Math.min(filterIndex + 2, allPricesTemp2.size()));
                    try {
                        if (j > filterIndex) { // 分时成交模式下, 有1条柱子会大于 filterIndex; 应当使用最新价和上一做比较
                            if (tempDoubles.get(filterIndex) < fsTransNewestPrice) {
                                return upColorFs;
                            } else if (tempDoubles.get(filterIndex) > fsTransNewestPrice) {
                                return downColorFs;
                            } else {
                                return equalColorFs;
                            }
                        }

                        if (j == 0) {
                            if (tempDoubles.get(j) > preClose) {
                                return upColorFs;
                            } else if (tempDoubles.get(j) < preClose) {
                                return downColorFs;
                            } else {
                                return equalColorFs;
                            }
                        } else {
                            if (tempDoubles.get(j) > tempDoubles.get(j - 1)) {
                                return upColorFs;
                            } else if (tempDoubles.get(j) < tempDoubles.get(j - 1)) {
                                return downColorFs;
                            } else {
                                return equalColorFs;
                            }
                        }
                    } catch (Exception e) { // 当数据存在null时可正常~
                        return equalColorFs;
                    }
                }
            };
            barRenderer.setDrawBarOutline(false);//设置显示边框线
            barRenderer.setBarPainter(new StandardXYBarPainter());//取消渐变效果
            barRenderer.setMargin(0.6);//设置柱形图之间的间隔
            barRenderer.setDrawBarOutline(false);
            barRenderer.setSeriesVisibleInLegend(false);//设置不显示legend（数据颜色提示)
            barRenderer.setShadowVisible(false);//设置没有阴影

            // 解决十字线竖线, 价格图和成交量图 都线居中的问题!
            barRenderer.setBarAlignmentFactor(0.5);
        }

        public void initPlot1() {
            // 7. 图1: 价格图 -- 3条序列.
            //生成画图细节 第一个和最后一个参数这里需要设置为null，否则画板加载不同类型的数据时会有类型错误异常
            //可能是因为初始化时，构造器内会把统一数据集合设置为传参的数据集类型，画图器可能也是同样一个道理
            plot1 = new XYPlot(lineSeriesCollection, domainAxis, null, lineAndShapeRenderer);
            plot1.setBackgroundPaint(bgColorFs);// 设置曲线图背景色
            plot1.setDomainGridlinesVisible(false);// 不显示网格
            plot1.setRangeGridlinePaint(preCloseColorFs);// 设置间距格线颜色为红色, 同昨收颜色
            plot1.setRangeAxis(0, y1Axis);
            plot1.setRangeAxis(1, y2Axis); //两条y轴
        }

        public void initY2AxisOfChgPct() {
            y2Axis.setAutoRange(false);//设置不采用自动设置数据范围
            y2Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
            DecimalFormat df2 = new DecimalFormat("#0.00%");
            df2.setRoundingMode(RoundingMode.CEILING);
            y2Axis.setNumberFormatOverride(df2);
            y2Axis.setTickLabelPaint(Color.red);
            updateY2AxisRange(); // 首次更新y2轴range; chart更新时可单独调用
        }

        public void updateY2AxisRange() {
            double t;
            double t1;
            double range;
            t = (priceLow - preClose) / preClose;
            t1 = (priceHigh - preClose) / preClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            range = t1 > t ? t1 : t;
            y2Axis.setRange(-range, range);//设置y轴数据范围
            NumberTickUnit numberTickUnit2 = new NumberTickUnit(Math.abs(range / 7.0));
            y2Axis.setTickUnit(numberTickUnit2);
        }

        public void initY1AxisOfPrice() {
            y1Axis.setAutoRange(false); //不采用自动设置数据范围
            y1Axis.setLabel(String.valueOf(preClose)); // 标记
            y1Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
            y1Axis.centerRange(preClose);
            updateY1AxisRange(); // 首次更新y1轴range; chart更新时可单独调用
        }

        public void updateY1AxisRange() {
            DecimalFormat df1 = new DecimalFormat("#0.00");
            df1.setRoundingMode(RoundingMode.CEILING); // 向下或上舍入模式, 原实现是floor
            y1Axis.setNumberFormatOverride(df1);

            double t = preClose - priceLow;
            double t1 = priceHigh - preClose;
            t = Math.abs(t);
            t1 = Math.abs(t1);
            double range = t1 > t ? t1 : t; // 计算涨跌最大幅度
            y1Axis.setRange(Double.valueOf(df1.format(preClose - range)),
                    Double.valueOf(df1.format(preClose + range))); // 设置y轴数据范围
            NumberTickUnit numberTickUnit = new NumberTickUnit(Math.abs(range / 7.0));
            y1Axis.setTickUnit(numberTickUnit); // 设置显示多少个tick,越多越密集
        }

        /**
         * 给定 分钟tick, 转换为 时间x轴中的值, 能够用来显示 XYTextAnnotation
         */
        // todo
        public void initDomainDateTimeAxis() {
            // 5.1. 设置时间范围，注意，最大和最小时间设置时需要+ - 。否则时间刻度无法显示
            domainAxis = new DateAxis();
            domainAxis.setAutoRange(false); //设置不采用自动设置时间范围
            Calendar calendar = Calendar.getInstance();
            Date da = todayDummy;
            calendar.setTime(da);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 29);
            calendar.set(Calendar.SECOND, 0);
            Date sda = calendar.getTime();
            calendar.set(Calendar.HOUR_OF_DAY, 15);
            calendar.set(Calendar.MINUTE, 01);
            da = calendar.getTime();
            domainAxis.setRange(sda, da);//设置时间范围

            // 5.2. 时间刻度x轴城常规设置
            domainAxis.setAutoTickUnitSelection(false); // 设置不采用自动选择刻度值
            domainAxis.setTickMarkPosition(DateTickMarkPosition.START); // 设置标记的位置
            domainAxis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());// 设置标准的时间刻度单位
            domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MINUTE, 30));// 设置时间刻度的间隔
            domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));//设置时间格式

            // 5.3. x轴时间线设置, 仅显示全日期tick列表
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
        }

        public XYLineAndShapeRenderer buildPlot1LineRenderer() {
            XYLineAndShapeRenderer lineAndShapeRenderer = new XYLineAndShapeRenderer();
            lineAndShapeRenderer.setBaseItemLabelsVisible(true);
            lineAndShapeRenderer.setSeriesShapesVisible(0, false); //设置不显示数据点模型
            lineAndShapeRenderer.setSeriesShapesVisible(1, false);
            lineAndShapeRenderer.setSeriesShapesVisible(3, false); //设置不显示数据点模型
            lineAndShapeRenderer.setSeriesShapesVisible(4, false);
            lineAndShapeRenderer.setSeriesPaint(0, priceColorFs); // 设置价格颜色
            lineAndShapeRenderer.setSeriesPaint(1, avgPriceColorFs);
            lineAndShapeRenderer.setSeriesPaint(2, preCloseColorFs);
            lineAndShapeRenderer.setSeriesPaint(3, priceColorFsOfIndex);
            lineAndShapeRenderer.setSeriesPaint(4, priceColorFsOfStock);
            lineAndShapeRenderer.setSeriesStroke(0, new BasicStroke(0.6f));
            lineAndShapeRenderer.setSeriesStroke(1, new BasicStroke(0.6f));
            lineAndShapeRenderer.setSeriesStroke(2, new BasicStroke(2));
            lineAndShapeRenderer.setSeriesStroke(3, new BasicStroke(0.6f));
            lineAndShapeRenderer.setSeriesStroke(4, new BasicStroke(0.6f));
            lineAndShapeRenderer.setBaseSeriesVisibleInLegend(false);

            return lineAndShapeRenderer;
        }


        /**
         * 刷新价格上下限, 注意, 需要新的上下限, 绝对值> 原来的上下限, 才更新; 即上下限只会变大, 不会变小!
         * 这里简单使用一个多余出来的涨跌幅, 例如千分之5; --> redundancyPriceRangePercent 静态属性控制
         *
         * @return
         * @update 新增了两条线, 但是, 所用价格已经适配过, 这里把适配后的价格, 一起加入, 计算最大最小值即可!
         */
        public void updatePriceLowAndHigh() {
            // 价格都是正数, 注意了!, 更高更低才更新
            List<Double> prices = new ArrayList<>(allPrices.subList(0, filterIndex + 1)); // 显示数据, 使用 filterIndex 直接索引
            prices.addAll(allPricesOfIndex.subList(0, filterIndex + 1)); // 适配过的, 直接加入即可
            prices.addAll(allPricesOfStock.subList(0, filterIndex + 1));
            try {
                double priceHigh0 = CommonUtil.maxOfListDouble(prices); // 当数据全部为null时, 将出错, 而默认使用涨跌停;否则正常
                if (Math.abs(priceHigh0 * (1 + redundancyPriceRangePercent)) > Math.abs(priceHigh)) {
                    priceHigh = priceHigh0 * (1 + redundancyPriceRangePercent); // 更高
                }
            } catch (Exception e) {
                return;
            }
            try {
                double priceLow0 = CommonUtil.minOfListDouble(prices);
                if (Math.abs(priceLow0 * (1 - redundancyPriceRangePercent)) < Math.abs(priceLow)) {
                    priceLow = priceLow0 * (1 - redundancyPriceRangePercent); // 更低!
                }
            } catch (Exception e) {
            }
        }


        /**
         * 分时成交更新时, 刷新最高最低; 它需要提供最新 price
         *
         * @param seriesPricesWhoDiffYAxisRange 那些线的价格(已经适配后的), 可能影响到 y轴最大最小区间的; 当前是转债/指数/正股 价格, 因此3项!
         * @key3 bugfix: 此处愿实现, 项prices添加了一项数据, 导致了错误; 应当新建列表, 而非直接向子列表添加数据!
         */
        public void updatePriceLowAndHighFsTrans(List<Double> seriesPricesWhoDiffYAxisRange) {
            List<Double> prices = new ArrayList<>(allPrices.subList(0, filterIndex + 1)); // 显示数据, 使用 filterIndex 直接索引
            prices.addAll(allPricesOfIndex.subList(0, filterIndex + 1)); // 适配过的, 直接加入即可
            prices.addAll(allPricesOfStock.subList(0, filterIndex + 1));
            prices.addAll(seriesPricesWhoDiffYAxisRange); // 假装加入, 且同样受到 大的更大,小的更小的限制
            try {
                double priceHigh0 = CommonUtil.maxOfListDouble(prices); // 当数据全部为null时, 将出错, 而默认使用涨跌停;否则正常
                if (Math.abs(priceHigh0 * (1 + redundancyPriceRangePercent)) > Math.abs(priceHigh)) {
                    priceHigh = priceHigh0 * (1 + redundancyPriceRangePercent);
                }
            } catch (Exception e) {
                return;
            }
            try {
                double priceLow0 = CommonUtil.minOfListDouble(prices);
                if (Math.abs(priceLow0 * (1 - redundancyPriceRangePercent)) < Math.abs(priceLow)) {
                    priceLow = priceLow0 * (1 - redundancyPriceRangePercent);
                }
            } catch (Exception e) {
            }
        }


        public void showChartSimple() {
            JFrame frame = new JFrame("temp");

            ChartPanel chartPanel = new ChartPanel(chart);


            // 大小
            chartPanel.setPreferredSize(new Dimension(1200, 800));
            chartPanel.setMouseZoomable(false);
            chartPanel.setRangeZoomable(false);
            chartPanel.setDomainZoomable(false);

            chartPanel.addChartMouseListener(getCrossLineListenerForFsXYPlot(allFsTimeTicks));

            frame.setLayout(new BorderLayout());
//            frame.setContentPane(chartPanel);

            JPanel panelRight = new JPanel();
            panelRight.setPreferredSize(new Dimension(tickLogPanelWidthDefault, 1024));

            ManipulateLogPanel displayForLog = new ManipulateLogPanel();
            logTextPane = displayForLog.getLogTextPane(); // 操作.
            logTextPane.setBackground(new Color(0, 0, 0));

            panelRight.setLayout(new BorderLayout());
            jScrollPaneForTickLog = new JScrollPane(logTextPane);
            jScrollPaneForTickLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            BasicScrollBarUIS
                    .replaceScrollBarUI(jScrollPaneForTickLog, COLOR_THEME_TITLE,
                            COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
//            jScrollPane.getViewport().setBackground(Color.red);
            panelRight.add(jScrollPaneForTickLog, BorderLayout.CENTER);


            frame.add(panelRight, BorderLayout.EAST);
            frame.add(chartPanel, BorderLayout.CENTER);


            frame.pack(); // 显示.
            // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
            frame.setVisible(true);
        }

        public static JScrollPane jScrollPaneForTickLog; // 滚动包裹
        public static JTextPane logTextPane;

        public static JTextPane getLogTextPane() {
            return logTextPane;
        }

        public static void setLogTextPane(JTextPane logTextPane) {
            DynamicEmFs1MV2ChartForRevise.logTextPane = logTextPane;
        }

        public static JScrollPane getJScrollPaneForTickLog() {
            return jScrollPaneForTickLog;
        }

        public static void setJScrollPaneForTickLog(JScrollPane jScrollPaneForTickLog) {
            DynamicEmFs1MV2ChartForRevise.jScrollPaneForTickLog = jScrollPaneForTickLog;
        }

        /**
         * 单条分时成交数据 显示
         * // 列4567分别是 时间,价格,成交量,买卖方向
         * // 同花顺颜色规则: 时间灰白色, 价格>preClose 红, 否则绿,等于白; 买卖方向 买方红,卖方绿
         *
         * @param fsTransRow
         */
        private String lastShowFsTransTick = null; // 保留已经put过的最后一条时间, 保证不连续put相同tick
        DecimalFormat df2 = new DecimalFormat("#########.00");
        DecimalFormat df3 = new DecimalFormat("#########.000");
        DecimalFormat dfOfChgPct = new DecimalFormat("####0.00%");
        Double prePrice0 = null; // 保留上一次价格, 当前价格与之比较, 显示向上向下箭头!

        // 标准是否为第一次调用 put; 若是, 将读取历史n条数据, 先插入; 否则仅插入单条数据 见 redundancyPutDataAmount
        private boolean firstPutting = true;

        /**
         * tick数据插入组件; 新增功能, 首次可多添加n条历史数据! 具体逻辑见 putCore
         *
         * @param fsTransIndexShould
         */
        public void put(int fsTransIndexShould) {
            if (firstPutting) {
                logTextPane.setText(""); // 新建新的实例,都要清空静态属性log,
                DataFrame<Object> sliceDf = fsTransDf
                        .slice(Math.max(0, fsTransIndexShould - redundancyPutDataAmount), fsTransIndexShould);
                // 筛选合适的历史数据df; 循环调用
                // @speed: 20次插入常态耗时 1-2 ms
                for (int i = 0; i < sliceDf.length(); i++) {
                    putCore(sliceDf.row(i));
                }
                firstPutting = false; // 修改flag
            }

            List<Object> fsTransRow = fsTransDf.row(fsTransIndexShould);
            putCore(fsTransRow);
        }

        /**
         * 将单行tick数, 打印
         *
         * @param fsTransRow
         */
        public void putCore(List<Object> fsTransRow) {
            // 1. 保证不put相同行数据
            String timeTick = fsTransRow.get(3).toString();
            if (timeTick.equals(lastShowFsTransTick)) {
                return;
            }
            lastShowFsTransTick = timeTick;

            // 2.数据项解析
            Double price = Double.valueOf(fsTransRow.get(4).toString());
            int amountRate = 1; // 计算成交额时, 倍率; 即 成交量 * 价格 * 每手数量
            if (beanEm.isBond()) {
                amountRate = 10;
            } else if (beanEm.isStock()) {
                amountRate = 100;
            }
            Double amount = Double.parseDouble(fsTransRow.get(5).toString()) * price * amountRate; // 成交额
            Integer bs = Integer.valueOf(fsTransRow.get(6).toString());

            // 3.时间数据 打印
            Font font = new Font("Consolas", Font.PLAIN, 18); // 字符同宽
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet aset = sc
                    .addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(192, 192, 192));
            aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
            aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
            aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
            aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());

            try {
                Document document = logTextPane.getDocument();
                document.insertString(document.getLength(), timeTick, aset);
            } catch (BadLocationException e) {

            }
            logTextPane.setCaretPosition(logTextPane.getDocument().getLength());

            // 2.价格信息
            String priceInfo;
            if (beanEm.isShenBond()) {
                priceInfo = StrUtil.padPre(df3.format(price), 9, " "); // 深债价格3为小数
            } else {
                priceInfo = StrUtil.padPre(df2.format(price), 9, " ");
            }
            // 2.1. 涨跌幅信息
            double chgPct = price / preClose - 1;
            String chgPctInfo = StrUtil.padPre(dfOfChgPct.format(chgPct), 6, " ");
            priceInfo = priceInfo + "[" + chgPctInfo + "]";

            Color color0 = new Color(192, 192, 192);
            if (price > preClose) {
                color0 = new Color(255, 50, 50);

            } else if (price < preClose) {
                color0 = new Color(0, 230, 0);
            }
            if (prePrice0 != null && prePrice0 != 0) {
                if ((price - prePrice0) / preClose >= 0.0035) { // 涨跌幅大增
                    color0 = new Color(229, 11, 222);
                } else if ((price - prePrice0) / preClose <= -0.0035) {// 大降低
                    color0 = new Color(0, 100, 0);
                }
            }

            aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color0);
            aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
            aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
            aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
            aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());

            try {
                Document document = logTextPane.getDocument();
                document.insertString(document.getLength(), priceInfo, aset);
            } catch (BadLocationException e) {

            }
            logTextPane.setCaretPosition(logTextPane.getDocument().getLength());

            // 2.1. 价格上升或者下降, 箭头信息
            String arrowStr = "  "; // 默认箭头和颜色
            Color color2 = new Color(192, 192, 192);
            if (prePrice0 != null) {
                if (price > prePrice0) {
                    arrowStr = " ↑"; // 默认箭头和颜色
                    color2 = new Color(255, 50, 50);
                    if (bs == 1) {
                        color2 = Color.yellow; // 价格提高 , 成交额却是卖方向! 特殊颜色
                    }
                } else if (price < prePrice0) {
                    arrowStr = " ↓"; // 默认箭头和颜色
                    color2 = new Color(0, 230, 0);
                    if (bs == 2) {
                        color2 = Color.yellow; // 同上
                    }
                }
            }


            prePrice0 = price; // 保留
            aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color2);
            aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
            aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
            aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
            aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());

            try {
                Document document = logTextPane.getDocument();
                document.insertString(document.getLength(), arrowStr, aset);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            logTextPane.setCaretPosition(logTextPane.getDocument().getLength());

            // 3.成交额信息
            String amountInfo = StrUtil.padPre(CommonUtil.formatNumberWithWan(amount), 10, " ");
            Color color3 = new Color(192, 192, 192);
            if (bs == 2) {
                color3 = new Color(255, 50, 50);
            } else if (bs == 1) {
                color3 = new Color(0, 230, 0);
            }
            aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color3);
            aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
            aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
            aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
            aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());
            try {
                Document document = logTextPane.getDocument();
                document.insertString(document.getLength(), amountInfo + "\n", aset);
            } catch (BadLocationException e) {

            }
            logTextPane.setCaretPosition(logTextPane.getDocument().getLength());
        }
    }


    /**
     * 分时图,将自动调用api, 读取 preClose; 也可自行提供
     */
    private static void fsV2Demo() throws Exception {
        String bondCode = "113016"; // 小康转债
        String dateStr = "2022-06-02";
        SecurityBeanEm bondBean = SecurityBeanEm.createBond(bondCode);


        DataFrame<Object> fsDf = EastMoneyDbApi
                .getFs1MV2ByDateAndQuoteId(dateStr, bondBean.getQuoteId());

        for (int i = 100; i < fsDf.length(); i++) {
            fsDf.set(i, "close", null);
            fsDf.set(i, "vol", null);
            fsDf.set(i, "avgPrice", null);
        }

        JFreeChart chart = createFs1MV2OfEm(fsDf, "测试标题", true);
        ApplicationFrame frame = new ApplicationFrame("temp");

        ChartPanel chartPanel = new ChartPanel(chart);
        // 大小
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);

        List<DateTime> timeTicks = DataFrameS.getColAsDateList(fsDf, "date"); // 日期列表;传递给监听器,设置横轴marker
        chartPanel.addChartMouseListener(getCrossLineListenerForFsXYPlot(timeTicks));


        frame.setContentPane(chartPanel);
        frame.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        frame.setVisible(true);
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
    // todo: 需要修改列名
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
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, dateTimeColName);

        List<Double> opens = DataFrameS.getColAsDoubleList(dataFrame, openColName);
        List<Double> closes = DataFrameS.getColAsDoubleList(dataFrame, closeColName);
        List<Double> highs = DataFrameS.getColAsDoubleList(dataFrame, highColName);
        List<Double> lows = DataFrameS.getColAsDoubleList(dataFrame, lowColName);
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
     * 东财同花顺分时图 -- 使用东财数据库v2版本, 同样241条数据
     * 数据库列相关名称: date, close,avgPrice,vol
     *
     * @param dataFrame
     * @param preClose
     * @param title
     * @param kLineYType
     * @param showAvgPrice : 是否显示均价线? 行业和概念的均价与 价格不再量级, 不显示, 其他均需要显示!
     * @return
     * @noti : 东财 v2 分时数据, 自带昨收列, 无需传递参数, 自行解析
     */
    public static JFreeChart createFs1MV2OfEm(DataFrame<Object> dataFrame,
                                              String title, boolean showAvgPrice) {
        try {
            Double preClose = Double.valueOf(dataFrame.get(0, "preClose").toString());
            // 因东财分时采用了 olhs, 这里只取 close作为分时价格
            List<DateTime> timeTicks = DataFrameS.getColAsDateList(dataFrame, "date");
            List<Double> prices = DataFrameS.getColAsDoubleList(dataFrame, "close");
            List<Double> avgPrices = DataFrameS.getColAsDoubleList(dataFrame, "avgPrice");
            List<Double> vols = DataFrameS.getColAsDoubleList(dataFrame, "vol");

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

                    try {
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
                    } catch (Exception e) { // 当数据存在null时可正常~
                        return equalColorFs;
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
    public static class NumberAxisYSupportTickMultiColor extends NumberAxis {
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
    public static class NumberAxisYSupportTickToPreClose extends NumberAxis {
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
