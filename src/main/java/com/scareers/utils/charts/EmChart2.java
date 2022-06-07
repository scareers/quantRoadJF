package com.scareers.utils.charts;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.ManipulateLogPanel;
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
import java.util.stream.Collectors;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_SCROLL_BAR_THUMB;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;
import static com.scareers.utils.CommonUtil.waitForever;
import static com.scareers.utils.charts.EmChart.getCrossLineListenerForFsXYPlot;

/**
 * description: 同花顺数据源的图表; 主要是分时图 和 3个周期的k线图
 *
 * @author: admin
 * @date: 2022/4/5/005-00:48:19
 */
public class EmChart2 {
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
    public static final int weight1OfTwoPlotOfFs = 4; // 两大weight, 控制 分时价格图 和成交量图, 高度比例
    public static final int weight2OfTwoPlotOfFs = 1; // 两大weight+gap, 可以绝对判定 鼠标位置!


    public static void main(String[] args) throws Exception {

//        kLineDemo();
//
//        fsV2Demo();

//        String bondCode = "002761"; // 浙江建投
//        SecurityBeanEm bondBean = SecurityBeanEm.createStock(bondCode);
//        String bondCode = "113016"; // 小康转债
        String bondCode = "123134"; // 卡贝转债
        SecurityBeanEm bondBean = SecurityBeanEm.createBond(bondCode);
        String dateStr = "2022-06-02";
        Console.log(bondBean.getName());
        DynamicEmFs1MV2ChartForRevise dynamicChart = new DynamicEmFs1MV2ChartForRevise(bondBean, dateStr, null, null);


        // 简单的分时图更新:
//        ThreadUtil.execAsync(new Runnable() {
//            @Override
//            public void run() {
//                for (Date tick : dynamicChart.getAllFsTimeTicks()) {
//                    ThreadUtil.sleep(1000);
//                    Console.log("即将刷新");
//                    dynamicChart.updateChart(tick); // 重绘图表
//                }
//
//            }
//        }, true);

//        // 分时成交更新!
        double timeRate = 5;
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                List<DateTime> allFsTransTimeTicks = CommonUtil.generateMarketOpenTimeListHms(false);
                for (int i = 200; i < allFsTransTimeTicks.size(); i++) {
                    Date tick = allFsTransTimeTicks.get(i);
                    ThreadUtil.sleep((long) (1000 / timeRate));
                    Console.log("即将刷新");
                    dynamicChart.updateChartFsTrans(tick); // 重绘图表
                }
            }
        }, true);


        dynamicChart.showChartSimple(); // 显示
        waitForever();
    }

    /**
     * 东财分时1分钟 分时图 -- 动态复盘,
     * 1.分时图使用v2版本数据, 即单日 241条!
     * 2.且需要提供分时成交数据, 以便模拟实盘的动态变化!
     * 3.将chart绘制逻辑实现为单个方法, 相关组件对象 保存为 属性! 以便动态修改
     * 4.数据从数据库获得!
     */
    @Data
    @NoArgsConstructor
    public static class DynamicEmFs1MV2ChartForRevise {
        public static double redundancyPriceRangePercent = 0.002; // 价格上下限, 比最高最低价, 多出来的部分; 使得图表上下限更明显
        public static int redundancyPutDataAmount = 20; // 首次put时, 多添加历史n条数据
        public static int tickLogPanelWidthDefault = 402; // tick打印面板的总宽度,含滚动条

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

            fsTransNewestPrice = preClose; // 默认初始价格
            priceLow = preClose * 0.99; // 默认图表价格下限
            priceHigh = preClose * 1.01; // 默认图表价格下限  // 指数和正股的 "价格"列, 已经适配了!
        }

        /**
         * @param rawPrices
         * @param selfPreClose
         * @param referPreClose
         * @return
         * @key3 : 极少的静态方法之一; 给定指数或者股票原始价格列表, 给定其昨收, 并给定转债昨收;
         * 将 原始价格, 转换为 能够放进同一 chart 的 对应价格; 保留涨跌幅的 一致!!
         */
        public static List<Double> convertPriceForInSameChart(List<Double> rawPrices, double selfPreClose,
                                                              double referPreClose) {
            return rawPrices.stream().map(value -> value / selfPreClose * referPreClose).collect(Collectors.toList());
        }

        private static final Log log = LogUtil.getLogger();

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
        EmChart.NumberAxisYSupportTickToPreClose y1Axis = new EmChart.NumberAxisYSupportTickToPreClose();
        // y轴2--涨跌幅轴
        EmChart.NumberAxisYSupportTickMultiColor y2Axis = new EmChart.NumberAxisYSupportTickMultiColor();//设置Y轴，为数值,后面的设置，参考上面的y轴设置
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
         * 依据当前筛选值, 更新3大数据序列的数据;  昨收不用更新
         */
        private void updateThreeSeriesData() {
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
         */
        private void updateThreeSeriesDataFsTrans(double fsTransPrice, double alreadySureVol) {
            Minute tick = new Minute(allFsTimeTicks.get(filterIndex + 1));
            seriesOfFsPrice.addOrUpdate(tick, fsTransPrice); // 价格和成交量, 更新为给定参数
            seriesOfVol.addOrUpdate(tick, alreadySureVol); //
            try {
                seriesOfAvgPrice.addOrUpdate(tick, allAvgPrices.get(filterIndex)); // 均价更新为同前1均价, 单纯为了好看
            } catch (Exception e) {
                // 11:30 - 13:00 filterINdex为-1, 出错
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
            // 1.1. 新增了2个序列 -- 指数和正股的 价格线(动态)
            lineSeriesCollection.addSeries(seriesOfFsPriceOfIndex);
            lineSeriesCollection.addSeries(seriesOfFsPriceOfStock); // 默认初始化, 未添加数据罢了!
            lineSeriesCollection.addSeries(seriesOfAvgPrice);
            lineSeriesCollection.addSeries(seriesOfPreClose);

            // 2.昨收序列首次加载后将不再更新
            Date today = allFsTimeTicks.get(0); // 无视哪一天, 不重要, 就取解析结果第一个即可;
            seriesOfPreClose.add(new Day(today), preClose);
            seriesOfPreClose.add(new Day(DateUtil.offsetDay(today, 1)), preClose);
            barSeriesCollection.addSeries(seriesOfVol);

            // 3.序列加载数据
            updateThreeSeriesData();

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
        public void updateChart() {
            if (this.beanEm == null || this.dateStr == null) {
                return;
            }
            // 1.刷新价格上下限 -- 可无
            updatePriceLowAndHigh();
            // 2.更新两个y轴的上下界!
            updateY1AxisRange();
            updateY2AxisRange();

            // 3.序列数据在更新上下界后更新
            updateThreeSeriesData();
        }

        public void updateChart(String timeTick) {
            if (this.beanEm == null || this.dateStr == null) {
                return;
            }
            boolean b = this.setFilterTimeTick(timeTick);
            if (b) {
                // 1.刷新价格上下限 -- 可无
                updatePriceLowAndHigh();
                // 2.更新两个y轴的上下界!
                updateY1AxisRange();
                updateY2AxisRange();

                // 3.序列数据在更新上下界后更新
                updateThreeSeriesData();
            } else {
                log.error("设置筛选tick失败, 无法更新图表");
            }
        }

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
                updateThreeSeriesData();
            } else {
                log.error("设置筛选tick失败, 无法更新图表");
            }
        }

        private double fsTransNewestPrice; // 暂存分时成交最新价格

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
         */
        public void updateChartFsTrans(Date date) {
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
            if (timeTickStr.compareTo("15:00:00") >= 0) {
                return; // 3点后不再更新
            }

            // 2.首先查找fs成交数据对应合适的索引 -- 不超过给定的日期的最后一条数据, 要求 30s内
            Integer fsTransIndexShould = null;
            DateTime date0 = DateUtil.parse(timeTickStr);
            String foundTick = null;
            while (DateUtil.format(date0, "HH:mm:ss").compareTo(DateUtil.format(date, "HH:mm" + ":00")) >= 0) {
                // 需要找本分钟内的
                String tick = DateUtil.format(date0, "HH:mm:ss");
                Integer index0 = allFsTransTimeTicksMap.get(tick);
                if (index0 != null) { // 找到了, 则退出
                    fsTransIndexShould = index0;
                    foundTick = tick;
                    break;
                } else {
                    // 没有找到, 则 往前一秒!
                    date0 = DateUtil.offset(date0, DateField.SECOND, -1);
                }
            }
            if (fsTransIndexShould == null) {
                //log.warn("未找到本分钟内,有分时成交数据: {}", timeTickStr);
                return;
            }

            // 3.数据解析
            // 3.1. 计算最新价格
            double newestPrice = Double.parseDouble(fsTransDf.get(fsTransIndexShould, "price").toString()); // 最新价格
            fsTransNewestPrice = newestPrice;
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
            Double volSum = CommonUtil.sumOfListNumber(DataFrameS.getColAsDoubleList(selectDf, "vol"));

            // 4.更新! 此时必有下一分钟的 tick
            // 4.1. 先更新整数分钟, 比起直接调用 updateChart的整数分钟更新, 用最新数据更新了上下限的刷新
            updatePriceLowAndHighFsTrans(newestPrice);
            // 同样更新两个y轴的上下界!
            updateY1AxisRange();
            updateY2AxisRange();
            // 序列数据在更新上下界后更新; ---  即整数部分
            updateThreeSeriesData();
            // 4.2. 实时部分, 更新到下一个tick
            updateThreeSeriesDataFsTrans(newestPrice, volSum);

            // todo: bug需要修复: 按照原来设置, 应当 allPrices.get(filterIndex), 但不知为何, allPrices 数据会被改变!
            // todo: 因此, 被迫使用了双份数据, allPricesTemp 是深备份的原始 allPrices, 直接从df获取;
            // todo: 可能因为 allPrices
//            Console.log("{}[{}] -- {}[{}]", allFsDateStr.get(filterIndex),allPricesTemp.get(filterIndex), foundTick,
//                    newestPrice);


            put(fsTransIndexShould);
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
            y3Axis.setNumberFormatOverride(new EmChart.NumberFormatCnForBigNumber()); // 数据轴数据标签的显示格式
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
            NumberTickUnit numberTickUnit2 = new NumberTickUnit(Math.abs(range / 7));
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
            NumberTickUnit numberTickUnit = new NumberTickUnit(Math.abs(range / 7));
            y1Axis.setTickUnit(numberTickUnit); // 设置显示多少个tick,越多越密集
        }

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
            lineAndShapeRenderer.setSeriesPaint(0, priceColorFs); // 设置价格颜色
            lineAndShapeRenderer.setSeriesPaint(1, avgPriceColorFs);
            lineAndShapeRenderer.setSeriesPaint(2, preCloseColorFs);
            lineAndShapeRenderer.setSeriesStroke(0, new BasicStroke(0.6f));
            lineAndShapeRenderer.setSeriesStroke(1, new BasicStroke(0.6f));
            lineAndShapeRenderer.setSeriesStroke(2, new BasicStroke(2));
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
         * @key3 bugfix: 此处愿实现, 项prices添加了一项数据, 导致了错误; 应当新建列表, 而非直接向子列表添加数据!
         */
        public void updatePriceLowAndHighFsTrans(double price) {
            List<Double> prices = new ArrayList<>(allPrices.subList(0, filterIndex + 1)); // 显示数据, 使用 filterIndex 直接索引
            prices.add(price); // 假装加入, 且同样受到 大的更大,小的更小的限制
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


}
