package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.CrossLineListenerForKLineXYPlot;
import com.scareers.utils.charts.CrossLineXIndexChangeCallback;
import com.scareers.utils.charts.EmChartKLine;
import joinery.DataFrame;
import lombok.Data;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 使用东财数据的 K线显示面板
 * 核心为多参数 update() 方法; 本质上, 需要提供 昨日及之前的 k线数据, 以及 "今日当前" 的 4项数据 !! 先更新过去完整数据, 再更新今日数据
 * update()方法需要更新 df, preClose,title
 *
 * @author: admin
 * @date: 2022/4/5/005-06:30:09
 */
@Data
public class EmKLineDisplayPanel extends DisplayPanel {
    public static final int preferHeight = 300;
    public static final int infoPanelWidth = 50; // 信息显示panel宽度
    // 切换转债时, 应当更新本属性为新的实例; 单转债刷新过程, 则调用 其 updateKLine(...) 方法
    EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart;
    ChartPanel chartPanel;

    // 信息panel, 多个label 竖直重叠, 网格1列
    JPanel jPanelOfCurrentKLineInfo;

    // 9项数据!
    JLabel labelOfDate = getCommonLabel("日期");
    JLabel labelOfDateValue = getCommonLabel();
    JLabel labelOfChgPct = getCommonLabel("涨跌幅");
    JLabel labelOfChgPctValue = getCommonLabel();
    JLabel labelOfAmplitude = getCommonLabel("振幅");
    JLabel labelOfAmplitudeValue = getCommonLabel();
    JLabel labelOfOpen = getCommonLabel("开盘");
    JLabel labelOfOpenValue = getCommonLabel();
    JLabel labelOfHigh = getCommonLabel("最高");
    JLabel labelOfHighValue = getCommonLabel();
    JLabel labelOfLow = getCommonLabel("最低");
    JLabel labelOfLowValue = getCommonLabel();
    JLabel labelOfClose = getCommonLabel("收盘");
    JLabel labelOfCloseValue = getCommonLabel();
    JLabel labelOfAmount = getCommonLabel("成交额");
    JLabel labelOfAmountValue = getCommonLabel();
    JLabel labelOfTurnover = getCommonLabel("换手率");
    JLabel labelOfTurnoverValue = getCommonLabel();

    public EmKLineDisplayPanel() {
        this.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("暂无k线数据");
        jLabel.setPreferredSize(new Dimension(4096, preferHeight));
        jLabel.setForeground(Color.red);
        jLabel.setBackground(COLOR_THEME_MINOR);
        this.add(jLabel, BorderLayout.CENTER);

        // 原始api的列有这些! 都可以显示
        // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
        jPanelOfCurrentKLineInfo = new JPanel(); // 配合十字线, 显示当前那根k线的信息.
        jPanelOfCurrentKLineInfo.setPreferredSize(new Dimension(infoPanelWidth, 4096));
        JLabel jLabel2 = new JLabel("我是信息");
        jPanelOfCurrentKLineInfo.setLayout(new BorderLayout());
        jPanelOfCurrentKLineInfo.add(jLabel2, BorderLayout.CENTER);
        this.add(jPanelOfCurrentKLineInfo, BorderLayout.EAST);


    }

    public static JLabel getCommonLabel() {
        return getCommonLabel("");
    }

    public static JLabel getCommonLabel(String text) {
        JLabel jlabel = new JLabel();
        jlabel.setText(text);
        return jlabel;
    }

    /**
     * 本质上是更新整个图表对象, 而非刷新图表对象
     *
     * @noti : 调用方负责实例化动态图表 的逻辑;
     */
    public void update(EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart) {
        this.dynamicKLineChart = dynamicKLineChart; // 更新动态chart对象!
        this.update();
    }

    CrossLineListenerForKLineXYPlot crossLineListenerForKLineXYPlot0;

    /**
     * 本质上是更新整个图表对象, 而非刷新图表对象
     */
    @Override
    public void update() {
        if (chartPanel == null) {
            if (this.dynamicKLineChart != null && this.dynamicKLineChart.isInited()) { // 首次初始化
                chartPanel = new ChartPanel(this.dynamicKLineChart.getChart());
                // 大小
                chartPanel.setPreferredSize(new Dimension(4096, preferHeight));
                chartPanel.setMouseZoomable(false);
                chartPanel.setRangeZoomable(false);
                chartPanel.setDomainZoomable(false);
                crossLineListenerForKLineXYPlot0 =
                        EmChartKLine.getCrossLineListenerForKLineXYPlot(this.dynamicKLineChart.getAllDateTime());
                crossLineListenerForKLineXYPlot0.setXIndexChangeCallback(buildCrossLineXChangeCallback());
                chartPanel
                        .addChartMouseListener(
                                crossLineListenerForKLineXYPlot0);
                this.add(chartPanel, BorderLayout.CENTER);
                chartPanel.setVisible(true);
            } else {
                return;
            }
        }

        // 此后更新
        if (this.dynamicKLineChart != null && this.dynamicKLineChart.isInited()) {
            // 需要设置新的时间tick, 保证十字线正常!
            crossLineListenerForKLineXYPlot0.setTimeTicks(this.dynamicKLineChart.getAllDateTime());
            chartPanel.setChart(dynamicKLineChart.getChart());
        }
    }

    /**
     * 十字线x 索引改变回调
     *
     * @return
     */
    public CrossLineXIndexChangeCallback buildCrossLineXChangeCallback() {
        return new CrossLineXIndexChangeCallback() {
            @Override
            public void call(int newIndex) {
                if (dynamicKLineChart == null || !dynamicKLineChart.isInited()) {
                    return; // 需要动态图表已经初始化, 即有数据!
                }
                DataFrame<Object> klineDfBeforeToday = dynamicKLineChart.getKlineDfBeforeToday();
                if (newIndex < klineDfBeforeToday.length()) {
                    CommonUtil.notifyKey(klineDfBeforeToday.row(newIndex).toString());
                }
            }
        };
    }
}
