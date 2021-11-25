package com.scareers.utils.charts;


import cn.hutool.core.lang.Console;
import joinery.DataFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.category.CategoryDataset;

import java.io.File;
import java.io.IOException;

import static com.scareers.utils.charts.ChartUtil.createDefaultCategoryDataset;

public class BarChartForDf extends ApplicationFrame {
    JFreeChart barChart;
    int width;
    int height;
    File file;
    String xUseCol;

    /**
     * 完整参数构造器
     *
     * @param df
     * @param width
     * @param height
     * @param applicationTitle
     * @param chartTitle
     * @param categoryAxisLabel
     * @param valueAxisLabel
     * @param legend
     * @param tooltips
     * @param urls
     * @param file
     * @throws IOException
     */
    public BarChartForDf(DataFrame<Object> df, int width, int height, String applicationTitle, String chartTitle,
                         String categoryAxisLabel,
                         String valueAxisLabel,
                         boolean legend, boolean tooltips, boolean urls, File file, String xUseCol)
            throws IOException {
        super(applicationTitle);
        this.width = width;
        this.height = height;
        this.file = file;
        this.xUseCol = xUseCol;

        barChart = ChartFactory.createBarChart(
                chartTitle,
                // 横轴标题
                categoryAxisLabel,
                // 纵轴标题
                valueAxisLabel,
                // CategoryDataset数据集对象
                createDataset(df),
                // 朝向垂直,全部默认
                PlotOrientation.VERTICAL,
                // 图例,工具
                legend, tooltips, urls);
        // 初始化各项属性.  显示和保存调用下两个函数
    }

    public void showIt() {
        ChartPanel chartPanel = new ChartPanel(barChart);
        // 大小
        chartPanel.setPreferredSize(new java.awt.Dimension(width, height));
        // 设置内容
        setContentPane(chartPanel);
        this.pack(); // 显示.
        // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
        UIUtils.centerFrameOnScreen(this);
        this.setVisible(true);
    }

    public void saveIt() throws IOException {
        if (file != null) {
            // 给定File, 才保存.
            ChartUtils.saveChartAsPNG(file, barChart, this.width, this.width);
            Console.log("save success: {}", file.toString());
        } else {
            Console.log("未提供 File, 无法保存");
        }
    }

    private CategoryDataset createDataset(DataFrame<Object> df) {
        return createDefaultCategoryDataset(df, xUseCol);
    }

    public BarChartForDf(DataFrame<Object> df, String applicationTitle, String chartTitle,
                         String categoryAxisLabel,
                         String valueAxisLabel,
                         boolean legend, boolean tooltips, boolean urls, File file, String xUseCol) throws IOException {
        this(df, 800, 600, applicationTitle, chartTitle,
                categoryAxisLabel, valueAxisLabel, legend, tooltips, urls, file, xUseCol);
    }

    public BarChartForDf(DataFrame<Object> df,
                         String categoryAxisLabel,
                         String valueAxisLabel,
                         boolean legend, boolean tooltips, boolean urls, File file, String xUseCol) throws IOException {
        this(df, "simple window title", "null title", categoryAxisLabel, valueAxisLabel, legend, tooltips, urls,
                file, xUseCol);
    }

    public BarChartForDf(DataFrame<Object> df,

                         boolean legend, boolean tooltips, boolean urls, File file, String xUseCol) throws IOException {
        this(df, "simple window title", "null title", "x_value",
                "y_value", legend, tooltips, urls,
                file, xUseCol);
    }

    public BarChartForDf(DataFrame<Object> df,
                         File file, String xUseCol) throws IOException {
        this(df, "simple window title", "null title",
                "x_value", "y_value", true, true, false, file, xUseCol);
    }

    public BarChartForDf(DataFrame<Object> df, String xUseCol) throws IOException {
        this(df, "simple window title", "null title",
                "x_value", "y_value", true, true, false, null, xUseCol);
    }
}
