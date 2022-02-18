package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/18/018-14:35:10
 */

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fs.FsFetcher;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.SecurityListAndTablePanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.utils.charts.ChartUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jfree.chart.*;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * 表格展示df 的Panel
 * 1.buttonContainer 展示于表格上方, 默认仅有全量刷新按钮. flow left布局, 可添加按钮
 *
 * @author admin
 */
@Getter
public abstract class SecurityDfDisplayPanel extends SecurityDisplayPanel {
    private JTable jTable;
    private JScrollPane jScrollPane;

    private JPanel buttonContainer; // 功能按钮容器
    private JButton buttonFlushAll; // 全量刷新按钮
    private boolean fullFlushFlag = false; // 强制全量刷新flag. 刷新一次自动false. 点击刷新按钮将自动全量刷新一次.

    private SecurityBeanEm preBean;

    /**
     * @param parent 仅用于位置修复
     */
    SecurityListAndTablePanel parent;

    public SecurityDfDisplayPanel(
            SecurityListAndTablePanel parent,
            int listWidth) {
        this.parent = parent;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        JLabel label = new JLabel("数据获取中"); // 默认显示内容
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // 占位
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi

        buttonFlushAll = ButtonFactory.getButton("全量刷新");
        buttonFlushAll.setMaximumSize(new Dimension(60, 20));
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fullFlushFlag = true; // 下一次将全量更新
            }
        });

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.add(buttonFlushAll);
        buttonContainer.setBorder(null);
        this.add(buttonContainer, BorderLayout.NORTH);
        this.add(jScrollPane, BorderLayout.CENTER);

        this.setBounds(listWidth, 0, parent.getWidth() - listWidth, parent.getHeight());
        parent.getMainDisplayWindow().flushBounds();
    }

    /**
     * 核心方法, 需要实现展示的数据df
     *
     * @return
     */
    public abstract DataFrame<Object> getShowDf(SecurityBeanEm currentBean);

    @Override
    public void update() {
        DataFrame<Object> fsDataOfStock = getShowDf(newBean);
        if (fsDataOfStock == null) {
            return;
        }

        if (jTable == null) { // 首次刷新
            Vector<Vector<Object>> datas = new Vector<>();
            for (int i = 0; i < fsDataOfStock.length(); i++) {
                datas.add(new Vector<>(fsDataOfStock.row(i)));
            }
            Vector<Object> cols = new Vector<>(fsDataOfStock.columns());
            DefaultTableModel model = new DefaultTableModel(datas, cols);

            jTable = new JTable();
            jTable.setModel(model);
            jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
            fitTableColumns(jTable);
        } else { // 不断更新时
            DefaultTableModel model = (DefaultTableModel) jTable.getModel();
            if (newBean == preBean) { // 股票选中没变, 考虑增加行
                if (fullFlushFlag) {
                    fullFlushFlag = false;
                    fullFlush(fsDataOfStock, model);
                    fitTableColumns(jTable);
                    log.info("已全量刷新");
                } else { // 增量刷新, 无需更新列宽
                    if (fsDataOfStock.length() > model.getRowCount()) { // 因保证必然有序, 所以只添加新的行.
                        int oldRowCount = model.getRowCount();
                        // 添加数据
                        for (int i = oldRowCount; i < fsDataOfStock.length(); i++) {
                            model.addRow(fsDataOfStock.row(i).toArray()); // 该方法将会自动增加行数,无需手动调用增加行数
                        }
                    }
                }
            } else { // 股票选择变化, 则全量更新
                fullFlush(fsDataOfStock, model);
                fitTableColumns(jTable);
            }
            preBean = newBean;
        }
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 表格列宽自适应
     *
     * @param myTable
     */
    protected void fitTableColumns(JTable myTable) {
        JTableHeader header = myTable.getTableHeader();
        int rowCount = myTable.getRowCount();

        Enumeration columns = myTable.getColumnModel().getColumns();


//        while (columns.hasMoreElements()) {
        if (columns.hasMoreElements()) {
            TableColumn column = (TableColumn) columns.nextElement();
            int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
            int width = (int) myTable.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(myTable, column.getIdentifier()
                            , false, false, -1, col).getPreferredSize().getWidth();
            for (int row = 0; row < rowCount; row++) {
                int preferedWidth = (int) myTable.getCellRenderer(row, col).getTableCellRendererComponent(myTable,
                        myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
                width = Math.max(width, preferedWidth);
            }
            header.setResizingColumn(column); // 此行很重要
            column.setWidth(width + myTable.getIntercellSpacing().width + 5); // 多5
//            break; // 仅第一列日期. 其他的平均
        }
    }

    /**
     * 全量刷新逻辑
     *
     * @param fsDataOfStock
     * @param model
     */
    protected void fullFlush(DataFrame<Object> fsDataOfStock, DefaultTableModel model) {
//        Vector<Vector<Object>> datas = new Vector<>();
//        for (int i = 0; i < fsDataOfStock.length(); i++) {
//            datas.add(new Vector<>(fsDataOfStock.row(i)));
//        }
//        Vector<Object> cols = new Vector<>(fsDataOfStock.columns());
//        model.setDataVector(datas, cols);


        Vector<Vector> oldDatas = model.getDataVector();
        Vector<Vector> newDatas = new Vector<>();
        for (int i = 0; i < fsDataOfStock.length(); i++) {
            newDatas.add(new Vector<>(fsDataOfStock.row(i)));
        }
        // 将自动增减行, 若新数据行少, 则多的行不会显示, 但本身存在
        model.setRowCount(newDatas.size());

        for (int i = 0; i < Math.min(oldDatas.size(), newDatas.size()); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                if (!newDatas.get(i).get(j).equals(oldDatas.get(i).get(j))) {
                    model.setValueAt(newDatas.get(i).get(j), i, j);
                }
            }
        }
    }

    /**
     * fs图显示对话框
     *
     * @param owner
     * @param parentComponent
     * @deprecated 可显示分时图. 整个程序使用网页版东财以人工观察行情. 自己不需要封装控件. 本方法仅代码保留
     */
    @SneakyThrows
    private void showFs1MDialog() {
        if (preBean == null) {
            return;
        }
        String title = StrUtil.format("分时图 - {} [{}]", preBean.getSecCode(), preBean.getName());
        final JDialog dialog = new JDialog(TraderGui.INSTANCE, title, false);
        dialog.setSize((int) (TraderGui.INSTANCE.getWidth() * 0.8), (int) (TraderGui.INSTANCE.getHeight() * 0.8));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(TraderGui.INSTANCE);

        DataFrame<Object> dataDf = FsFetcher.getFsData(preBean);

        Double preClose;
        if (!preBean.isIndex()) {
            preClose = StockApi.getStockPreCloseAndTodayOpen(preBean.getSecCode(), 2000, 1, true).get(0);
            // 昨收
        } else {
            preClose = StockApi.getPreCloseAndTodayOpenOfIndexOrBK(preBean, 2000, 3).get(0);// 昨收
        }

        JFreeChart chart = ChartUtil
                .createFs1MKLineOfEm(dataDf, preClose,
                        StrUtil.format("{} [{}]", preBean.getSecCode(), preBean.getName()),
                        ChartUtil.KLineYType.PERCENT);
        chart.setBackgroundPaint(ChartColor.WHITE);
//            ChartUtil.showChartSimple(chart);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                XYPlot plot = (XYPlot) event.getChart().getPlot();
                try {
                    double yValue = plot.getRangeCrosshairValue();
                    double xValue = plot.getDomainCrosshairValue();


                    Marker yMarker = new ValueMarker(yValue);
                    yMarker.setPaint(Color.darkGray);
                    plot.addRangeMarker(yMarker);

                    Marker xMarker = new ValueMarker(xValue);
                    xMarker.setPaint(Color.darkGray);
                    plot.addDomainMarker(xMarker);

                    chartPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {


            }
        });

        dialog.setContentPane(chartPanel);
        dialog.setVisible(true);
    }
}
