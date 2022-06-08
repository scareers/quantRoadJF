package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.utils.charts.EmChartKLine;
import lombok.Data;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;
import static com.scareers.utils.charts.ThsChart.getCrossLineListenerForKLineXYPlot;

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
    // 切换转债时, 应当更新本属性为新的实例; 单转债刷新过程, 则调用 其 updateKLine(...) 方法
    EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart;
    ChartPanel chartPanel;

    public EmKLineDisplayPanel() {
        this.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("暂无k线数据");
        jLabel.setPreferredSize(new Dimension(preferHeight, preferHeight));
        jLabel.setForeground(Color.red);
        jLabel.setBackground(COLOR_THEME_MINOR);
        this.add(jLabel, BorderLayout.CENTER);
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

    /**
     * 本质上是更新整个图表对象, 而非刷新图表对象
     */
    @Override
    public void update() {
        if (chartPanel == null) {
            if (this.dynamicKLineChart != null) { // 首次初始化
                chartPanel = new ChartPanel(this.dynamicKLineChart.getChart());
                // 大小
                chartPanel.setPreferredSize(new Dimension(preferHeight, preferHeight));
                chartPanel.setMouseZoomable(false);
                chartPanel.setRangeZoomable(false);
                chartPanel.setDomainZoomable(false);
                chartPanel
                        .addChartMouseListener(
                                getCrossLineListenerForKLineXYPlot(this.dynamicKLineChart.getAllDateTime()));
                this.removeAll();
                this.add(chartPanel, BorderLayout.CENTER);
                chartPanel.setVisible(true);
            } else {
                return;
            }
        }

        // 此后更新
        if (this.dynamicKLineChart != null) {
            chartPanel.setChart(dynamicKLineChart.getChart());
        }
    }
}
