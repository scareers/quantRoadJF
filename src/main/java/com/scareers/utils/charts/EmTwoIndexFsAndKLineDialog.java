package com.scareers.utils.charts;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * description: 对话框, 简单显示两大指数的 k线和分时; 2*2布局!
 *
 * @author: admin
 * @date: 2022/6/15/015-19:00:00
 */
public class EmTwoIndexFsAndKLineDialog extends JDialog {
    public static void main(String[] args) throws Exception {
        TraderGui gui = new TraderGui();
        TraderGui.INSTANCE = gui;
        EmTwoIndexFsAndKLineDialog dialog = new EmTwoIndexFsAndKLineDialog(TraderGui.INSTANCE, "xx", true);
        dialog.update("2022-06-15", 200);
        dialog.setVisible(true);


    }

    public static double scale = 0.8;
    SecurityBeanEm shenIndex;
    SecurityBeanEm huIndex;


    public EmTwoIndexFsAndKLineDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.setResizable(true);
        this.setSize((int) (TraderGui.screenW * scale), (int) (TraderGui.screenH * scale));

        initContentPanelS(); // 主面板

        this.setContentPane(contentPanelS);
        this.setLocationRelativeTo(TraderGui.INSTANCE);
        GuiCommonUtil.addEscNotVisibleCallbackToJDialog(this);

        // 两大指数
        this.shenIndex = SecurityBeanEm.getShenZhengChengZhi();
        this.huIndex = SecurityBeanEm.getShangZhengZhiShu();
    }


    JPanel contentPanelS;
    EmChartKLine.DynamicEmKLineChartForRevise shenKLine;
    EmChartKLine.DynamicEmKLineChartForRevise huKLine;
    JFreeChart shenFs;
    JFreeChart huFs;

    ChartPanel chartPanelShenKLine;
    ChartPanel chartPanelShenFs;
    ChartPanel chartPanelHuKLine;
    ChartPanel chartPanelHuFs;

    public void initContentPanelS() {
        contentPanelS = new JPanel();
        contentPanelS.setLayout(new GridLayout(2, 3, -1, -1));


        crossLineListenerForKLineXYPlotShen = EmChartKLine.getCrossLineListenerForKLineXYPlot(Arrays.asList());
        crossLineListenerForKLineXYPlotHu = EmChartKLine.getCrossLineListenerForKLineXYPlot(Arrays.asList());
        crossLineListenerForFsXYPlotShen = EmChartFs.getCrossLineListenerForFsXYPlot(Arrays.asList());
        crossLineListenerForFsXYPlotHu = EmChartFs.getCrossLineListenerForFsXYPlot(Arrays.asList());

        chartPanelShenKLine = buildKLineChartPanel();
        chartPanelHuKLine = buildKLineChartPanel();
        chartPanelShenFs = buildKLineChartPanel(); // 4 空panel
        chartPanelHuFs = buildKLineChartPanel();

        chartPanelShenKLine.addChartMouseListener(crossLineListenerForKLineXYPlotShen);
        chartPanelHuKLine.addChartMouseListener(crossLineListenerForKLineXYPlotHu);
        chartPanelShenFs.addChartMouseListener(crossLineListenerForFsXYPlotShen);
        chartPanelHuFs.addChartMouseListener(crossLineListenerForFsXYPlotHu);

        contentPanelS.add(chartPanelHuKLine);
        contentPanelS.add(chartPanelHuFs);
        contentPanelS.add(chartPanelShenKLine);
        contentPanelS.add(chartPanelShenFs);

    }

    CrossLineListenerForKLineXYPlot crossLineListenerForKLineXYPlotShen;
    CrossLineListenerForKLineXYPlot crossLineListenerForKLineXYPlotHu;
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlotShen;
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlotHu;

    public void update(String dateStr, int hopeKLineAmount) {
        shenKLine = new EmChartKLine.DynamicEmKLineChartForRevise(
                shenIndex, dateStr, hopeKLineAmount);
        huKLine = new EmChartKLine.DynamicEmKLineChartForRevise(
                huIndex, dateStr, hopeKLineAmount);

        DataFrame<Object> fsDfShen = EastMoneyDbApi
                .getFs1MV2ByDateAndQuoteId(dateStr, shenIndex.getQuoteId());
        shenFs = EmChartFs.createFs1MV2OfEm(fsDfShen, "深证成指", true);

        DataFrame<Object> fsDfHu = EastMoneyDbApi
                .getFs1MV2ByDateAndQuoteId(dateStr, huIndex.getQuoteId());
        huFs = EmChartFs.createFs1MV2OfEm(fsDfHu, "上证指数", true);


        chartPanelHuKLine.setChart(huKLine.getChart());
        chartPanelShenKLine.setChart(shenKLine.getChart());
        chartPanelShenFs.setChart(shenFs);
        chartPanelHuFs.setChart(huFs);

        crossLineListenerForKLineXYPlotShen.setTimeTicks(shenKLine.getAllDateTime());
        crossLineListenerForKLineXYPlotHu.setTimeTicks(huKLine.getAllDateTime());
        crossLineListenerForFsXYPlotShen.setTimeTicks(DataFrameS.getColAsDateList(fsDfShen, "date"));
        crossLineListenerForFsXYPlotHu.setTimeTicks(DataFrameS.getColAsDateList(fsDfHu, "date"));
    }

    public ChartPanel buildKLineChartPanel() {
        ChartPanel chartPanel = new ChartPanel(null);
        // 大小
        // chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);
        return chartPanel;
    }
}
