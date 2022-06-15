package com.scareers.utils.charts;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;
import lombok.SneakyThrows;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Arrays;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_LIST_BK_EM;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.*;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.getReviseTimeBondListOverviewDataDf;

/**
 * description: 对话框, 首先决定 日期, 随后, 左边显示该日收盘, 所有板块(全部,行业,概念,风格) 的收盘行情状态,表格显示
 * 右边分上下, 上k线,下分时;
 * 带4个按钮, 执行筛选, 筛选全部/行业/概念/风格! 表格是单例
 *
 * @author: admin
 * @date: 2022/6/15/015-19:00:00
 */
public class EmAllBkDisplayDialog extends JDialog {
    public static void main(String[] args) throws Exception {
        TraderGui gui = new TraderGui();
        TraderGui.INSTANCE = gui;
        EmAllBkDisplayDialog dialog = new EmAllBkDisplayDialog(TraderGui.INSTANCE, "xx", true);
        SecurityBeanEm bk0 = SecurityBeanEm.createBK("教育");
        Console.log(bk0.isAreaBK());
        dialog.update("2022-06-15", bk0, 150);
        dialog.setVisible(true);


    }

    public static double scale = 0.8;
    public static int leftPanelWidth = 300; // 表格在左宽度!


    public EmAllBkDisplayDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        this.setResizable(true);
        this.setSize((int) (TraderGui.screenW * scale), (int) (TraderGui.screenH * scale));

        initContentPanelS(); // 主面板

        this.setContentPane(contentPanelS);
        this.setLocationRelativeTo(TraderGui.INSTANCE);
        GuiCommonUtil.addEscNotVisibleCallbackToJDialog(this);
    }


    JPanel contentPanelS;
    JPanel leftPanel;
    JPanel rightPanel;
    EmChartKLine.DynamicEmKLineChartForRevise kLineChart;
    JFreeChart fsChart;
    ChartPanel chartPanelKLine;
    ChartPanel chartPanelFs;

    CrossLineListenerForKLineXYPlot crossLineListenerForKLineXYPlot;
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlot;

    public void initContentPanelS() {
        contentPanelS = new JPanel();
        contentPanelS.setLayout(new BorderLayout());

        crossLineListenerForKLineXYPlot = EmChartKLine.getCrossLineListenerForKLineXYPlot(Arrays.asList());
        crossLineListenerForFsXYPlot = EmChartFs.getCrossLineListenerForFsXYPlot(Arrays.asList());

        chartPanelKLine = buildKLineChartPanel();
        chartPanelFs = buildKLineChartPanel(); // 4 空panel

        chartPanelKLine.addChartMouseListener(crossLineListenerForKLineXYPlot);
        chartPanelFs.addChartMouseListener(crossLineListenerForFsXYPlot);


        initLeftPanel();
        initRightPanel();

        contentPanelS.add(leftPanel, BorderLayout.WEST);
        contentPanelS.add(rightPanel, BorderLayout.CENTER);
    }

    JPanel buttonContainer; // 放切换 板块类型的 4个按钮: 全部,行业,概念,风格!
    JXTable jXTableForBonds;

    private void initLeftPanel() {
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(leftPanelWidth, 4096));

        initSecurityEmJXTable();
        leftPanel.add(jXTableForBonds, BorderLayout.CENTER);

        initButtons();

    }

    /**
     * 4大按钮! + 2大切换日期(向前1向后1) 的按钮
     */
    private void initButtons() {
        buttonContainer = new JPanel();


    }

    /**
     * // @NOTI: 因东财 web api, 对 pc版本的 概念板块 和 风格板块, 融合在一起, 无法区分, 因此没有 STYLE 类型表示风格板块
     */
    public enum BkType {
        ALL,
        INDUSTRY,
        CONCEPT,
        AREA
    }

    /**
     * 给定日期, 给定板块类型(上enum), 返回 对应所有板块, 相关数据, 组成 df!
     *
     * @return
     */
    public static DataFrame<Object> getDfOfBksForDisplay() {

    }


    JScrollPane jScrollPaneForList;

    private void initJTableWrappedJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jXTableForBonds); // 滚动包裹转债列表
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }


    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private void initSecurityEmJXTable() {
        // 1.构造model
        Vector<Vector<Object>> datas = new Vector<>(); // 空数据, 仅提供列信息
        Vector<Object> cols = new Vector<>(bondTableColNames);
        DefaultTableModel model = new DefaultTableModel(datas, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不可编辑!
            }

            @Override
            public Class getColumnClass(int column) { // 返回列值得类型, 使得能够按照数据排序, 否则默认按照字符串排序
                if (column == 0 || column == 1) { // 名称代码
                    return String.class;
                } else if (column == 2 || column == 3 || column == 4 || column == 5) { // 涨跌幅成交额
                    return Double.class;
                } else {
                    return Object.class;
                }
            }
        };

        // 2.实例化table, 设置model
        jXTableForBonds = new JXTable();
        jXTableForBonds.setSortOrder("涨跌幅", SortOrder.DESCENDING);
        jXTableForBonds.setModel(model);
        removeEnterKeyDefaultAction(jXTableForBonds); // 按下enter不下移, 默认行为

        addTableHighlighters(); // 高亮

        jXTableForBonds.setSortOrder(2, SortOrder.DESCENDING); // 默认涨跌幅降序
        jXTableForBonds.setAutoCreateRowSorter(true);

        // 3.切换选择的回调绑定
        jXTableForBonds.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            private volatile int preIndex = -2;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jXTableForBonds.getSelectedRow();
                if (row == -1) {
                    return;
                }

                try {
                    row = jXTableForBonds.convertRowIndexToModel(row);
                } catch (Exception exx) {
                    return;
                }

                if (row == preIndex) {
                    return;
                }

                preIndex = row;

                String bondCode = null;
                try {
                    bondCode = model.getValueAt(row, 0).toString();
                } catch (Exception ex) {
                    return; // 行数底层实现有bug
                }
                try {
                    SecurityBeanEm bond = SecurityBeanEm.createBond(bondCode);
//                    setSelectedBean(bond);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    CommonUtil.notifyError("表格切换选中行,回调函数执行失败");
                }
            }
        });
        fitTableColumns(jXTableForBonds);
//        if (jxFindBarS != null) { // 双边都有判定设置
//            jxFindBarS.setSearchable(this.jXTableForBonds.getSearchable());
//        }
        initJTableStyle();
        setTableColCellRenders(); // 设置显示render
        jXTableForBonds.setGridColor(Color.black); // 不显示网格

    }

    /**
     * 设置表格各列的 cellRender
     */
    private void setTableColCellRenders() {
        jXTableForBonds.getColumn(0).setCellRenderer(new TableCellRendererForBondTable());
        jXTableForBonds.getColumn(1).setCellRenderer(new TableCellRendererForBondTable());
        jXTableForBonds.getColumn(2).setCellRenderer(new TableCellRendererForBondTableForPercent());
        jXTableForBonds.getColumn(3).setCellRenderer(new TableCellRendererForBondTableForBigNumber());
        jXTableForBonds.getColumn(4).setCellRenderer(new TableCellRendererForBondTableForPercent());
        jXTableForBonds.getColumn(5).setCellRenderer(new TableCellRendererForBondTableForBigNumber());
    }


    public static Color commonForeColor = Color.white; // 普通的字颜色, 转债代码和名称 使用白色
    public static Color amountForeColor = new Color(2, 226, 224); // 文字颜色 : 成交额
    public static Color amountForeColorShort = Color.orange; // 文字颜色 : 成交额
    public static Color upForeColor = new Color(255, 50, 50); // 向上的红色 : 涨跌幅
    public static Color downForeColor = new Color(0, 230, 0); // 向下的绿色: 涨跌幅
    public static Color selectedBackColor = new Color(64, 0, 128); // 选中时背景色
    public static Color holdBondForeColor = new Color(206, 14, 95); // 持仓的文字颜色: 代码和名称!

    HoldBondHighLighterPredicate holdBondHighLighterPredicate;
    ColorHighlighter holdBondHighlighter;

    /**
     * 默认设置是: 全背景黑色, 全字体 白色, 选中背景 深蓝!
     * 高亮对象均在此基础上更改
     * 转债列表高亮设置
     */
    private void addTableHighlighters() {
        // 1.成交额列蓝色
        HighlightPredicate.ColumnHighlightPredicate columnHighlightPredicate = new HighlightPredicate.ColumnHighlightPredicate(
                3);
        ColorHighlighter amountColForeHighlighter = new ColorHighlighter(columnHighlightPredicate, null,
                amountForeColor, null, amountForeColor);
        // 1.2.@add: 短额 黄色
        HighlightPredicate.ColumnHighlightPredicate columnHighlightPredicate0 = new HighlightPredicate.ColumnHighlightPredicate(
                5);
        ColorHighlighter amountColForeHighlighterShort = new ColorHighlighter(columnHighlightPredicate0, null,
                amountForeColorShort, null, amountForeColorShort);

        // 2.涨跌幅>0 则文字 偏红色 , <0, 则偏绿色
        ChgPctGt0HighLighterPredicate chgPctGt0HighLighterPredicate = new ChgPctGt0HighLighterPredicate(2);
        ColorHighlighter chgPctGt0Highlighter = new ColorHighlighter(chgPctGt0HighLighterPredicate, null,
                upForeColor, null, upForeColor);
        ChgPctLt0HighLighterPredicate chgPctLt0HighLighterPredicate = new ChgPctLt0HighLighterPredicate(2);
        ColorHighlighter chgPctLt0Highlighter = new ColorHighlighter(chgPctLt0HighLighterPredicate, null,
                downForeColor, null, downForeColor);

        // 2.2.@add: 短速同理
        ChgPctGt0HighLighterPredicate chgPctGt0HighLighterPredicate1 = new ChgPctGt0HighLighterPredicate(4);
        ColorHighlighter chgPctGt0HighlighterShort = new ColorHighlighter(chgPctGt0HighLighterPredicate1, null,
                upForeColor, null, upForeColor);
        ChgPctLt0HighLighterPredicate chgPctLt0HighLighterPredicate2 = new ChgPctLt0HighLighterPredicate(4);
        ColorHighlighter chgPctLt0HighlighterShort = new ColorHighlighter(chgPctLt0HighLighterPredicate2, null,
                downForeColor, null, downForeColor);

        // 3.持仓转债 代码和名称 变深红 , 两者均为属性, 为了实时修改!
        holdBondHighLighterPredicate = new HoldBondHighLighterPredicate(null);
        holdBondHighlighter = new ColorHighlighter(holdBondHighLighterPredicate, null,
                holdBondForeColor, null, holdBondForeColor);

        jXTableForBonds.setHighlighters(
                amountColForeHighlighter,
                amountColForeHighlighterShort,
                chgPctGt0Highlighter,
                chgPctLt0Highlighter,
                chgPctGt0HighlighterShort,
                chgPctLt0HighlighterShort,
                holdBondHighlighter
        );

    }

    /**
     * 设置转债列表表格样式
     */
    private void initJTableStyle() {
        // 1.表头框颜色和背景色
        jXTableForBonds.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(Color.white);
        TableColumnModel columnModel = jXTableForBonds.getTableHeader().getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            //i是表头的列
            TableColumn column = jXTableForBonds.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //表头文字居中
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        // 2.表格自身文字颜色和背景色
        jXTableForBonds.setBackground(Color.black);
        jXTableForBonds.setForeground(Color.white);
        // 4. 单行高 和字体
        jXTableForBonds.setRowHeight(25);
        jXTableForBonds.setFont(new Font("微软雅黑", Font.PLAIN, 16));

        // 5.单选,大小,无边框, 加放入滚动
        jXTableForBonds.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jXTableForBonds.setPreferredSize(new Dimension(leftPanelWidth, 10000));
        jXTableForBonds.setBorder(null);
        initJTableWrappedJScrollPane(); // 表格被包裹
    }


    /**
     * 全量刷新逻辑
     *
     * @param fullDf
     * @param model
     */
    protected void fullFlushBondListTable(DataFrame<Object> newestDf) {
        int sortedColumnIndex = jXTableForBonds.getSortedColumnIndex(); // 唯一主要的排序列!
        SortOrder sortOrder = null;
        if (sortedColumnIndex != -1) {
            sortOrder = jXTableForBonds.getSortOrder(sortedColumnIndex); // 什么顺序
        }

        DefaultTableModel model = (DefaultTableModel) jXTableForBonds.getModel();
        // 老数据
        Vector<Vector> oldDatas = model.getDataVector();

        // 新数据
        Vector<Vector> newDatas = new Vector<>();
        for (int i = 0; i < newestDf.length(); i++) {
            newDatas.add(new Vector<>(newestDf.row(i)));
        }
        // 行改变 :将自动增减行, 若新数据行少, 则多的行不会显示, 但本身存在
        model.setRowCount(newDatas.size());

//        model.setDataVector(newDatas, new Vector<>(bondTableColNames));

        for (int i = 0; i < newDatas.size(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                model.setValueAt(newDatas.get(i).get(j), i, j);
            }
        }

        // 排序恢复!
        if (sortedColumnIndex != -1) {
            jXTableForBonds.setSortOrder(sortedColumnIndex, sortOrder);
        }
    }

    private void initRightPanel() {
        rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayout(2, 1, 0, 0));

        rightPanel.add(chartPanelKLine);
        rightPanel.add(chartPanelFs);

    }

    public void update(String dateStr, SecurityBeanEm currentBk, int hopeKLineAmount) {
        // 1.更新表格, 暂未实现

        // 2.更新图表
        kLineChart = new EmChartKLine.DynamicEmKLineChartForRevise(
                currentBk, dateStr, hopeKLineAmount);
        DataFrame<Object> fsDf = EastMoneyDbApi
                .getFs1MV2ByDateAndQuoteId(dateStr, currentBk.getQuoteId());
        fsChart = EmChartFs.createFs1MV2OfEm(fsDf, currentBk.getName(), true);

        chartPanelKLine.setChart(kLineChart.getChart());
        chartPanelFs.setChart(fsChart);

        crossLineListenerForKLineXYPlot.setTimeTicks(kLineChart.getAllDateTime());
        crossLineListenerForFsXYPlot.setTimeTicks(DataFrameS.getColAsDateList(fsDf, "date"));
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
