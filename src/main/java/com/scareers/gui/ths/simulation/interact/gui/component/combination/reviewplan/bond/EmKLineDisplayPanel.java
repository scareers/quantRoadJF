package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.charts.ThsChart;
import joinery.DataFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
public class EmKLineDisplayPanel extends DisplayPanel {
    DataFrame<Object> thsFsDf;
    String title = "暂无标题";

    JFreeChart chart;
    ChartPanel chartPanel;

    public EmKLineDisplayPanel() {
        this.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("暂无k线数据");
        jLabel.setForeground(Color.red);
        jLabel.setBackground(COLOR_THEME_MINOR);
        this.add(jLabel, BorderLayout.CENTER);

        this.update();
    }

    /**
     * 调用
     *
     * @param thsFsDf
     * @param title
     * @param preClose
     */
    public void update(DataFrame<Object> thsFsDf, String title) {
        this.thsFsDf = thsFsDf;
        if (!StrUtil.isBlank(title)) {
            this.title = title;
        }
        this.update();
    }

    @Override
    public void update() {
        if (this.thsFsDf == null || this.thsFsDf.length() == 0) {
            return;
        }
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(thsFsDf, "日期"); // 日期列表;传递给监听器,设置横轴marker
        chart = ThsChart.createKLineOfThs(thsFsDf, title);
        if (chartPanel == null) {
            chartPanel = new ChartPanel(this.chart);
            chartPanel.setMouseZoomable(false);
            chartPanel.setRangeZoomable(false);
            chartPanel.setDomainZoomable(false);
            chartPanel.addChartMouseListener(getCrossLineListenerForKLineXYPlot(timeTicks));
            this.removeAll();
            this.add(chartPanel, BorderLayout.CENTER);
        }
        chartPanel.setVisible(false);
        chartPanel.setChart(chart); // 更新显示
        chartPanel.setVisible(true);
    }
}
