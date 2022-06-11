package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.date.DateTime;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.EmKLineDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.EmChartFs;
import com.scareers.utils.charts.EmChartKLine;
import joinery.DataFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * description: 全局快捷键设置!
 * 1.智能查找的按键绑定
 * 2.复盘暂停按钮space按下
 *
 * @author: admin
 * @date: 2022/6/9/009-11:30:52
 */
public class GuiGlobalKeyListener {
    /**
     * 空格键: 复盘界面的暂停键判定
     */
    public static void tryPauseRevise(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getID() == KeyEvent.KEY_PRESSED) { // 按下空格键
            // 1.如果处于复盘界面
            BondGlobalSimulationPanel instance = checkInBondGlobalSimulationPanel();
            if (instance != null) {
                FuncButton pauseRebootReviseButton = instance.getPauseRebootReviseButton();
                // 暂停/重启 按钮可见, 就暂停/重启
                if (pauseRebootReviseButton.isVisible()) {
                    pauseRebootReviseButton.doClick();
                }
            }
        } // 按下空格, 复盘界面暂停实现
    }

    /**
     * enter键: 复盘界面,当 日k线有十字线时, 尝试打开对应日期的分时图 -- 弹窗
     *
     * @param e
     */
    public static void tryOpenFs1mV2OnReviseDailyKline(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getID() == KeyEvent.KEY_PRESSED) {
            BondGlobalSimulationPanel instance = checkInBondGlobalSimulationPanel();
            if (instance != null) {
                EmKLineDisplayPanel dailyKLineDisplayPanel = instance.getDailyKLineDisplayPanel();
                if (dailyKLineDisplayPanel != null) {
                    int crossLineIndex = dailyKLineDisplayPanel.getCrossLineIndex();
                    if (crossLineIndex != -1) { // 索引正常, 读取日期, 显示 分时图
                        EmChartKLine.DynamicEmKLineChartForRevise dynamicKLineChart = dailyKLineDisplayPanel
                                .getDynamicKLineChart();
                        if (dynamicKLineChart != null && dynamicKLineChart.isInited()) {
                            //
                            List<String> allDateStr = dynamicKLineChart.getAllDateStr();
                            if (crossLineIndex >= 0 && crossLineIndex <= allDateStr.size()) {
                                String dateStr = allDateStr.get(crossLineIndex); // 终于拿到了日期;
                                // 对话框, 显示k线
                                SecurityBeanEm beanEm = dynamicKLineChart.getBeanEm();
                                DataFrame<Object> fs1mDf = null;
                                try {
                                    fs1mDf = EastMoneyDbApi // 分时图数据拿到了
                                            .getFs1MV2ByDateAndQuoteId(dateStr, beanEm.getQuoteId());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    CommonUtil.notifyError("分时1Mv2数据获取失败, 2022-06-02以后才有数据!");
                                }
                                if (fs1mDf != null) {
                                    JFreeChart chart = EmChartFs.createFs1MV2OfEm(fs1mDf, "", true);
                                    JDialog dialog = new JDialog(TraderGui.INSTANCE,
                                            beanEm.getName() + ": " + dateStr, true);
                                    dialog.setSize(new Dimension(1200, 800)); // 不能是prefersize,需要size
                                    ChartPanel chartPanel = new ChartPanel(chart);
                                    // 大小
                                    chartPanel.setPreferredSize(new Dimension(1200, 800));
                                    chartPanel.setMouseZoomable(false);
                                    chartPanel.setRangeZoomable(false);
                                    chartPanel.setDomainZoomable(false);
                                    List<DateTime> timeTicks = DataFrameS
                                            .getColAsDateList(fs1mDf, "date"); // 日期列表;传递给监听器,设置横轴marker
                                    chartPanel.addChartMouseListener(
                                            EmChartFs.getCrossLineListenerForFsXYPlot(timeTicks));

                                    dialog.setContentPane(chartPanel);
                                    dialog.setLocationRelativeTo(TraderGui.INSTANCE);
                                    dialog.setVisible(true);
                                }
                            }
                        }
                    }

                }
            }


        }
    }

    public static BondGlobalSimulationPanel checkInBondGlobalSimulationPanel() {
        if (TraderGui.INSTANCE.functionGuiCurrent.equals(TraderGui.FunctionGuiCurrent.BOND_REVISE)) {
            BondGlobalSimulationPanel instance = BondGlobalSimulationPanel.getInstance();
            return instance;
        }
        return null;
    }

    /**
     * F1 - F4: 复盘时 买入 1/1, 1/2, 1/3, 1/4
     *
     * @param e
     */
    public static void tryReviseBuyButton(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_F1 || e.getKeyCode() == KeyEvent.VK_F2 || e
                    .getKeyCode() == KeyEvent.VK_F3 || e.getKeyCode() == KeyEvent.VK_F4) {
                BondGlobalSimulationPanel instance = checkInBondGlobalSimulationPanel();
                if (instance != null) {
                    if (e.getKeyCode() == KeyEvent.VK_F1) {
                        instance.getBuy1Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F2) {
                        instance.getBuy2Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F3) {
                        instance.getBuy3Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F4) {
                        instance.getBuy4Button().doClick();
                    }
                }
            }
        }
    }

    /**
     * F5 - F8: 复盘时 卖入 1/1, 1/2, 1/3, 1/4
     *
     * @param e
     */
    public static void tryReviseSellButton(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_F5 || e.getKeyCode() == KeyEvent.VK_F6 || e
                    .getKeyCode() == KeyEvent.VK_F7 || e.getKeyCode() == KeyEvent.VK_F8) {
                BondGlobalSimulationPanel instance = checkInBondGlobalSimulationPanel();
                if (instance != null) {
                    if (e.getKeyCode() == KeyEvent.VK_F5) {
                        instance.getSell1Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F6) {
                        instance.getSell2Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F7) {
                        instance.getSell3Button().doClick();
                    } else if (e.getKeyCode() == KeyEvent.VK_F8) {
                        instance.getSell4Button().doClick();
                    }
                }
            }
        }
    }

    /**
     * F12: 复盘时打开账户窗口!
     *
     * @param e
     */
    public static void tryReviseOpenAccountDialogButton(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (e.getKeyCode() == KeyEvent.VK_F12) {
                BondGlobalSimulationPanel instance = checkInBondGlobalSimulationPanel();
                if (instance != null) {
                    instance.getOpenAccountButton().doClick();
                }
            }
        }
    }

}
