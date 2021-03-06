package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.charts.ThsChart;
import joinery.DataFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;
import static com.scareers.utils.charts.ThsChart.getCrossLineListenerForFsXYPlot;

/**
 * description: 同花顺fs图展示
 * update()方法需要更新 df, preClose,title
 *
 * @author: admin
 * @date: 2022/4/5/005-06:30:09
 */
public class ThsFsDisplayPanel extends DisplayPanel {
    DataFrame<Object> thsFsDf;
    String title = "暂无标题";
    double preClose;

    JFreeChart chart;
    ChartPanel chartPanel;

    public ThsFsDisplayPanel() {
        this.setLayout(new BorderLayout());
        JLabel jLabel = new JLabel("暂无分时数据");
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
    public void update(DataFrame<Object> thsFsDf, String title, double preClose) {
        this.thsFsDf = thsFsDf;
        if (!StrUtil.isBlank(title)) {
            this.title = title;
        }
        this.preClose = preClose;
        this.update();
    }

    @Override
    public void update() {
        if (this.thsFsDf == null || this.thsFsDf.length() == 0) {
            return;
        }
        List<DateTime> timeTicks = DataFrameS.getColAsDateList(thsFsDf, "时间"); // 日期列表;传递给监听器,设置横轴marker
        chart = ThsChart.createFs1MOfThs(thsFsDf, preClose, title, true);
        if (chartPanel == null) {
            chartPanel = new ChartPanel(this.chart);
            chartPanel.setMouseZoomable(false);
            chartPanel.setRangeZoomable(false);
            chartPanel.setDomainZoomable(false);
            chartPanel.addChartMouseListener(getCrossLineListenerForFsXYPlot(timeTicks));
            this.removeAll();
            this.add(chartPanel, BorderLayout.CENTER);
        }
        chartPanel.setVisible(false);
        chartPanel.setChart(chart); // 更新显示
        chartPanel.setVisible(true);
    }
}
