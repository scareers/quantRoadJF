package com.scareers.utils.charts;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.scareers.utils.CommonUtil;
import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_CHART_CROSS_LINE_EM;
import static com.scareers.utils.charts.ChartUtil.decimalFormatForPercent;
import static com.scareers.utils.charts.ThsChart.*;

/**
 * description: 十字交叉监听器. 同花顺分时图使用; 通过gap和2个weight, 计算鼠标在图中的位置 位于 两个子plot什么位置!
 *
 * @author: admin
 * @date: 2022/2/24/024-17:39:46
 */
@Setter
@Getter
public class CrossLineListenerForFsXYPlot implements ChartMouseListener {
    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
    }
    /*
        public static final int gapOfTwoPlotOfFs = 5; // 分时价格和成交量,两个图的gap
    public static final int weight1OfTwoPlotOfFs = 3; // 两大weight, 控制 分时价格图 和成交量图, 高度比例
    public static final int weight2OfTwoPlotOfFs = 1; // 两大weight+gap, 可以绝对判定 鼠标位置!
     */

    protected ValueMarkerS markerX; // 竖线唯一,
    protected ValueMarkerS markerYForPricePlot; // 横线可能两个. 只显示1个
    protected ValueMarkerS markerYForVolPlot;

    List<DateTime> timeTicks;
    int xAmount;

    public void setTimeTicks(List<DateTime> timeTicks) {
        this.timeTicks = timeTicks;
        this.xAmount = timeTicks.size();
    }

    public void reportXIndex(int currentXIndex){

    }

    public CrossLineListenerForFsXYPlot(List<DateTime> timeTicks) {
        // 给定日期列表!, 设置x轴竖线marker
        setTimeTicks(timeTicks);

        BasicStroke basicStroke = new BasicStroke(1);
        // 1.x竖线 唯一
        markerX = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER); // 标志类型
        markerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerX.setPaint(COLOR_CHART_CROSS_LINE_EM); //线条颜色
        markerX.setStroke(basicStroke); //粗细
        markerX.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerX.setLabelPaint(Color.red);
        setMarkerXLabelPosition(markerX);

        // 2.价格图的ymarker横线
        markerYForPricePlot = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerYForPricePlot.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerYForPricePlot.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerYForPricePlot.setPaint(COLOR_CHART_CROSS_LINE_EM); //线条颜色

        markerYForPricePlot.setStroke(basicStroke); //粗细
        // markerY.setLabel(decimalFormatForPercent.format(markerValueY)); //线条上显示的文本
        markerYForPricePlot.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerYForPricePlot.setLabelPaint(Color.red);
        markerYForPricePlot.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerYForPricePlot.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

        // 3.成交量图的ymarker横线
        markerYForVolPlot = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerYForVolPlot.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerYForVolPlot.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerYForVolPlot.setPaint(COLOR_CHART_CROSS_LINE_EM); //线条颜色
        markerYForVolPlot.setStroke(basicStroke); //粗细
        // markerY.setLabel(decimalFormatForPercent.format(markerValueY)); //线条上显示的文本
        markerYForVolPlot.setLabelFont(new Font("SansSerif", 0, 10)); //文本格式
        markerYForVolPlot.setLabelPaint(Color.red);
        markerYForVolPlot.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerYForVolPlot.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);

    }

    protected void setMarkerXLabelPosition(ValueMarkerS markerX) {
        markerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
        // 1.只监听 CombinedDomainXYPlot/XYPlot 上的鼠标移动, 其他类型无视;
        if (event.getEntity() == null) {
            return;
        }
        if (!(event.getEntity() instanceof PlotEntity)) {
            return;
        }
        if (!(event.getChart().getPlot() instanceof CombinedDomainXYPlot)) {
            return;
        }
        // 2.ChartPanel 对象
        ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();
        // 2.1. 两个子图
        CombinedDomainXYPlot plot = (CombinedDomainXYPlot) event.getChart().getPlot(); // 必须匹配分时图的plot类型
        List subplots = plot.getSubplots();
        XYPlot pricePlot = (XYPlot) subplots.get(0);
        XYPlot volPlot = (XYPlot) subplots.get(1); // 两个子图

        // 3.鼠标范围
        // 3.1: 求鼠标当前位置, 对应的x值
        double cursorX = event.getTrigger().getX(); // 鼠标位置x
        double minX = chartPanel.getScreenDataArea().getMinX();
        double maxX = chartPanel.getScreenDataArea().getMaxX(); // 图最大最小y

        double cursorY = event.getTrigger().getY(); // 鼠标位置y
        double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
        double minY = chartPanel.getScreenDataArea().getMinY();

        // 4.鼠标移除范围, 删除marker
        if (!(minX <= cursorX && cursorX <= maxX) || !(minY <= cursorY && cursorY <= maxY)) {
            try {
                pricePlot.removeDomainMarker(markerX); // 移除x轴
                volPlot.removeDomainMarker(markerX); // 移除x轴
                pricePlot.removeRangeMarker(markerYForPricePlot);
                volPlot.removeRangeMarker(markerYForVolPlot);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return; // 超过范围则移除十字星
        }

        // 5. x marker 因共用时间横轴, 同简单情况设置!
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        double percentX = (maxX - cursorX) / (maxX - minX); // x右侧百分比
        // 应当取值的索引, 已经 -1
        int index = (int) CommonUtil.roundHalfUP((1 - percentX) * xAmount, 0) - 1;
        if (index >= 0 && index < timeTicks.size()) {
            DateTime dateTime = timeTicks.get(index);
            // @key: 这里同样右移30s, 分时图tick1分钟, 恰好在中间\
            // todo: 价格和成交量的错位还是没有解决
            // @todo: 根本原因: 分时图是折线图, 它的标准tick, 价格线对应的是一个点, 是"tick支配的起点", 而成交量每根线是个"区域"
            // 如果要把线放在成交量区域的中心, 需要+30s, 但必然错过了折线的起点!
            // @bugfix: 已经解决: barRenderer.setBarAlignmentFactor(0.5); 可设置柱状图画的位置.
//            markerX.setValue(DateUtil.offsetSecond(dateTime, 0).getTime());
            markerX.setValue(dateTime.getTime());
            markerX.setLabel(DateUtil.format(dateTime, "HH:mm"));
            reportXIndex(index);
            // 更新markerX
            pricePlot.removeDomainMarker(markerX);
            volPlot.removeDomainMarker(markerX);
            pricePlot.addDomainMarker(markerX);
            volPlot.addDomainMarker(markerX);
        }

        // 6.设置ymarker, 两个plot 只可能同时显示一个!

        // 6.1. 首先需要计算, 鼠标处于 图1, 还是图2, 还是 gap中
        double totalHeight = maxY - minY;
        // 计算单位1的 weight, 实际高度是多少 ?
        double perWeight = (totalHeight - gapOfTwoPlotOfFs) / (weight1OfTwoPlotOfFs + weight2OfTwoPlotOfFs);
        // 百分比1: 从下到上的百分比, 计算 成交量图 的 上边线 的 百分比临界
        double percentOfVolPlotTop = (perWeight * weight2OfTwoPlotOfFs) / totalHeight;
        // 百分比2: 从下到上的百分比, 计算 gap 的 上边线 的 百分比临界
        double percentOfGapTop = (perWeight * weight2OfTwoPlotOfFs + gapOfTwoPlotOfFs) / totalHeight;

        // 6.2: 当前鼠标的百分比!
        double percentYFromBottom = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
        if (percentYFromBottom <= 0 || percentYFromBottom >= 1) {
            return;
        }

        if (percentYFromBottom < percentOfVolPlotTop) {
            // 当前鼠标位于成交量图
            // 1.价格图marker删除
            pricePlot.removeRangeMarker(markerYForPricePlot);
            // 2.成交量从0开始, 刚好为百分比
            ValueAxis rangeAxis = volPlot.getRangeAxis();
            Range range = rangeAxis.getRange();
            Double markerValueY =
                    range.getLowerBound() + range
                            .getLength() * (percentYFromBottom / percentOfVolPlotTop); // 同百分比取得marker位置

            // 注意, 实际百分比, 应当转换为 成交量图上沿
            volPlot.removeRangeMarker(markerYForVolPlot);
            markerYForVolPlot.setValue(markerValueY);
            markerYForVolPlot.setLabel(CommonUtil.formatNumberWithSuitable(markerValueY));
            volPlot.addRangeMarker(markerYForVolPlot);
        } else if (percentYFromBottom < percentOfGapTop) {
            // 当前鼠标位于gap中
            pricePlot.removeRangeMarker(markerYForPricePlot);
            volPlot.removeRangeMarker(markerYForVolPlot);
        } else {
            // 当前鼠标位于价格图
            volPlot.removeRangeMarker(markerYForVolPlot);
            double topToBottomPercent = 1 - percentYFromBottom; // 当前鼠标离上总百分比
            double pricePlotPercent = 1 - percentOfGapTop; // 价格图总占百分比


            // 1.值设置! 获取 getRangeAxis(0), 正常设置价格
            ValueAxis rangeAxis = pricePlot.getRangeAxis(0); // 我们获取百分比价格, 而非数值价格
            Range range = rangeAxis.getRange();
            Double markerValueY =
                    range.getUpperBound() - range
                            .getLength() * (topToBottomPercent / pricePlotPercent); // 同百分比取得marker位置

            // 注意, 实际百分比, 应当转换为 成交量图上沿
            pricePlot.removeRangeMarker(markerYForPricePlot);
            markerYForPricePlot.setValue(markerValueY);
            // 2.label设置, 我们使用百分比, 它需要读取纵坐标2
            ValueAxis rangeAxis2 = pricePlot.getRangeAxis(1); // 我们获取百分比价格, 而非数值价格
            Range range2 = rangeAxis2.getRange();
            Double markerValueY2 =
                    range2.getUpperBound() - range2
                            .getLength() * (topToBottomPercent / pricePlotPercent); // 同百分比取得marker位置
            markerYForPricePlot.setLabel(getMarkerYLabel(markerValueY2));
            pricePlot.addRangeMarker(markerYForPricePlot);

        }
    }

    String getMarkerYLabel(Double markerValueY) {
        return decimalFormatForPercent.format(markerValueY);
    }

    protected String getMarkerXLabel(Double markerValueX) {
        return decimalFormatForPercent.format(markerValueX);
    }
}
