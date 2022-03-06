package com.scareers.utils.charts;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.*;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.beans.JavaBean;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_CHART_CROSS_LINE_EM;
import static com.scareers.utils.charts.ChartUtil.decimalFormatForPercent;

/**
 * description: 十字交叉监听, 要求 时间序列, 即x轴为时间
 *
 * @author: admin
 * @date: 2022/2/24/024-17:39:46
 */
@Setter
@Getter
public class CrossLineListenerForTimeSeriesXYPlot extends CrossLineListenerForSingleNumberXYPlot {
    public CrossLineListenerForTimeSeriesXYPlot() {
        super();
    }

    @Override
    protected void setMarkerXLabelPosition(ValueMarkerS markerX) {
        markerX.setLabelAnchor(RectangleAnchor.BOTTOM);
        markerX.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
    }


    protected String getMarkerXLabel(long markerValueX) {
//        return "xx:yy";
        return DateUtil.date(markerValueX).toString(DatePattern.NORM_TIME_PATTERN);
    }

    Integer totalAmount;
    java.util.List items;

    ChartPanel chartPanel;
    XYPlot plot;

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {

        // 2.ChartPanel 对象
        // 3.事件发生的坐标, 对比屏幕中的数据区域, 所占百分比位置, 对应两个坐标轴值range百分比, 求出鼠标点对应的 x,y值
        // 3.1: 求鼠标当前位置, 对应的x值
        if (chartPanel == null) {
            chartPanel = (ChartPanel) event.getTrigger().getSource();
        }
        double cursorX = event.getTrigger().getX(); // 鼠标位置
        double minX = chartPanel.getScreenDataArea().getMinX();
        double maxX = chartPanel.getScreenDataArea().getMaxX(); // 图最大最小y

        double cursorY = event.getTrigger().getY(); // 鼠标位置
        double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
        double minY = chartPanel.getScreenDataArea().getMinY();
        if (plot == null) {
            plot = (XYPlot) event.getChart().getPlot();
        }

//        // 去掉了==, 范围更小, 更灵敏一点
//        if (!(minX < cursorX && cursorX < maxX) || !(minY < cursorY && cursorY < maxY)) {
//            plot.removeDomainMarker(markerX);
//            plot.removeRangeMarker(markerY);
//            return; // 超过范围则移除十字星
//        }

        // 1.只监听 XYPlot 上的鼠标移动, 其他类型无视; 获取 xyplot对象
        if (event.getEntity() == null) {
            return;
        }
        if (!(event.getEntity() instanceof PlotEntity)) {
            return;
        }
        if (!(event.getChart().getPlot() instanceof XYPlot)) {
            return;
        }


        double percentX = (maxX - cursorX) / (maxX - minX); // 从下到上部分百分比, 后面计算 value Range同百分比的x值即可
//        TimeSeriesCollection dataset = (TimeSeriesCollection) plot.getDataset();
//        TimeSeries series = (TimeSeries) dataset.getSeries().get(0);
//        if (totalAmount == null) {
//            totalAmount = series.getItems().size();
//        }
//        int percentIndex = totalAmount - NumberUtil.round(totalAmount * percentX, 0).intValue() - 1;
//        percentIndex = Math.min(Math.max(percentIndex, 0), totalAmount - 1);
//
//        if (items == null) {
//            items = series.getItems();
//        }
//        TimeSeriesDataItem item = (TimeSeriesDataItem) (items.get(percentIndex));
//        long markerValueX = item.getPeriod().getFirstMillisecond();

        DateAxis rangeAxisx = (DateAxis)plot.getDomainAxis();
        Range rangex = rangeAxisx.getRange();
        Double markerValueX = rangex.getUpperBound() - rangex.getLength() * percentX;

        // 3.2: 删除所有DomainMarkers, 新建对应x值得 Marker并设置. 可得到十字竖线
        markerX.setValue(markerValueX);
        markerX.setLabel(getMarkerXLabel(markerValueX.longValue()));


        // 3.3: 同理, 求出鼠标对应y值
        double percentY = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
        ValueAxis rangeAxis = plot.getRangeAxis();
        Range range = rangeAxis.getRange();
        Double markerValueY = range.getLowerBound() + range.getLength() * percentY; // 同百分比取得marker位置
        // 3.4: 同理, 创建y值 横向marker
        markerY.setValue(markerValueY);
        markerY.setLabel(getMarkerYLabel(markerValueY));

        if (!added) {

            plot.addDomainMarker(markerX);
            plot.addRangeMarker(markerY);
            added = true;
        }


    }

    boolean added = false;
}
