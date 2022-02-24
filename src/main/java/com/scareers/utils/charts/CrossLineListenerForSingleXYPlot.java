package com.scareers.utils.charts;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.ui.LengthAdjustmentType;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import java.awt.*;

import static com.scareers.utils.charts.ChartUtil.decimalFormatForPercent;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/24/024-17:39:46
 */
public class CrossLineListenerForSingleXYPlot implements ChartMouseListener {
    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
    }

    ValueMarkerS markerX;
    ValueMarkerS markerY;

    public CrossLineListenerForSingleXYPlot() {
        markerX = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerX.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER); // 标志类型
        markerX.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerX.setPaint(Color.red); //线条颜色
        markerX.setStroke(new BasicStroke(1.0F)); //粗细
        markerX.setLabelFont(new Font("SansSerif", 0, 8)); //文本格式
        markerX.setLabelPaint(Color.red);
        markerX.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        markerX.setLabelTextAnchor(TextAnchor.TOP_LEFT);

        markerY = new ValueMarkerS(Double.MIN_VALUE); // 水平线的值, 昨日收盘
        markerY.setType(ValueMarkerS.Type.MOUSE_CROSS_MARKER);
        markerY.setLabelOffsetType(LengthAdjustmentType.EXPAND);
        markerY.setPaint(Color.red); //线条颜色
        markerY.setStroke(new BasicStroke(1.0F)); //粗细
        // markerY.setLabel(decimalFormatForPercent.format(markerValueY)); //线条上显示的文本
        markerY.setLabelFont(new Font("SansSerif", 0, 8)); //文本格式
        markerY.setLabelPaint(Color.red);
        markerY.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
        markerY.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
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
        XYPlot plot = (XYPlot) event.getChart().getPlot();

        // 2.ChartPanel 对象
        ChartPanel chartPanel = (ChartPanel) event.getTrigger().getSource();

        // 3.事件发生的坐标, 对比屏幕中的数据区域, 所占百分比位置, 对应两个坐标轴值range百分比, 求出鼠标点对应的 x,y值
        // 3.1: 求鼠标当前位置, 对应的x值
        double cursorX = event.getTrigger().getX(); // 鼠标位置
        double minX = chartPanel.getScreenDataArea().getMinX();
        double maxX = chartPanel.getScreenDataArea().getMaxX(); // 图最大最小y
        double percentX = (maxX - cursorX) / (maxX - minX); // 从下到上部分百分比, 后面计算 value Range同百分比的x值即可
        ValueAxis domainAxis = plot.getDomainAxis();
        Range rangeX = domainAxis.getRange();
        Double markerValueX = rangeX.getUpperBound() - rangeX.getLength() * percentX; // 同百分比取得marker位置

        // 3.2: 删除所有DomainMarkers, 新建对应x值得 Marker并设置. 可得到十字竖线
        plot.removeDomainMarker(markerX);
        markerX.setValue(markerValueX);
        markerX.setLabel(decimalFormatForPercent.format(markerValueX));
        plot.addDomainMarker(markerX);

        // 3.3: 同理, 求出鼠标对应y值
        double cursorY = event.getTrigger().getY(); // 鼠标位置
        double maxY = chartPanel.getScreenDataArea().getMaxY(); // 图最大最小y
        double minY = chartPanel.getScreenDataArea().getMinY();
        double percentY = (maxY - cursorY) / (maxY - minY); // 从下到上部分百分比, 后面计算 value Range同百分比的y值即可
        ValueAxis rangeAxis = plot.getRangeAxis();
        Range range = rangeAxis.getRange();
        Double markerValueY = range.getLowerBound() + range.getLength() * percentY; // 同百分比取得marker位置
        // 3.4: 同理, 创建y值 横向marker
        plot.removeRangeMarker(markerY);
        markerY.setValue(markerValueY);
        markerY.setLabel(decimalFormatForPercent.format(markerValueX));
        plot.addRangeMarker(markerY);


    }
}
