package com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.display;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;

/**
 * description: AccountStatesItemDisplayPanel : 含 jTable 与 jLabel 两个显示组件. 同一时间仅单个有效显示
 * 账户状态的某一类数据显示.
 *
 * @author: admin
 * @date: 2022/2/18/018-17:02:42
 */
public class AccountStatesItemDisplayPanel extends DisplayPanel {
    Object newData;
    private JTable jTable;
    private JScrollPane jScrollPane;
    private JLabel jLabel;

    public void update(Object newData) {
        this.newData = newData;
        this.update();
    }

    public AccountStatesItemDisplayPanel() {
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        jLabel = new JLabel("数据不存在"); // 默认显示内容
        jLabel.setForeground(Color.red);

        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        jScrollPane.setViewportView(jLabel); // 占位
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi

        this.add(jScrollPane, BorderLayout.CENTER);
    }


    @Override
    protected void update() {
        if (newData == null) {
            return;
        }

        if (newData instanceof DataFrame) { // 账户状态: 各种df数据
            DataFrame<Object> newDf = (DataFrame) newData;

            if (jTable == null) { // 首次刷新
                Vector<Vector<Object>> datas = new Vector<>();
                for (int i = 0; i < newDf.length(); i++) {
                    datas.add(new Vector<>(newDf.row(i)));
                }
                Vector<Object> cols = new Vector<>(newDf.columns());
                DefaultTableModel model = new DefaultTableModel(datas, cols);

                jTable = new JTable();
                jTable.setModel(model);
                jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
                fitTableColumns(jTable);
            } else { // 不断更新时
                DefaultTableModel model = (DefaultTableModel) jTable.getModel();
                fullFlushDfData(newDf, model);
                fitTableColumns(jTable);
            }
        } else if (newData instanceof ConcurrentHashMap) { // 账户状态: 9项资金数据
            ConcurrentHashMap<String, Double> newMap = (ConcurrentHashMap) newData;
            String content = jsonStrToHtmlFormat(JSONUtil.toJsonPrettyStr(newMap));
            jLabel.setText(content);
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

        Enumeration<TableColumn> columns = myTable.getColumnModel().getColumns();


//        while (columns.hasMoreElements()) {
        if (columns.hasMoreElements()) {
            TableColumn column = columns.nextElement();
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
     * @param newDf
     * @param model
     */
    protected void fullFlushDfData(DataFrame<Object> newDf, DefaultTableModel model) {
        Vector<Vector> oldDatas = model.getDataVector();
        Vector<Vector> newDatas = new Vector<>();
        for (int i = 0; i < newDf.length(); i++) {
            newDatas.add(new Vector<>(newDf.row(i)));
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
}
