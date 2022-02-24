package com.scareers.gui.ths.simulation.interact.gui.component.combination.accountstate.display;

import cn.hutool.core.util.ObjectUtil;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    // 当前持仓数据的title, 以及根据哪一列, 设置行文字 红绿
    public static Object CURRENT_HOLD_TITLE = "当前持仓";
    public static int todayProfitColIndex = 9;

    private JLabel titleLabel = new JLabel();
    private String title = "";


    Object newData;
    private JTable jTable;
    private JScrollPane jScrollPane;
    private JLabel jLabel;

    public void update(Object newData) {
        // 对 newData 做深复制
        this.newData = newData;
        this.update();
    }

    public AccountStatesItemDisplayPanel(String title) {
        this.title = title;
        this.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
        this.setLayout(new BorderLayout());

        jLabel = new JLabel("数据不存在"); // 默认显示内容
        jLabel.setForeground(Color.red);

        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        jScrollPane.setViewportView(jLabel); // 占位
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi

        titleLabel.setText(title);
        titleLabel.setForeground(Color.red);
        titleLabel.setPreferredSize(new Dimension(4096, 20));
        this.add(jScrollPane, BorderLayout.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);
    }

    /**
     * 给定表格JTable, 给定某一列索引, 若某一行该列值 >0 , 则整行文字颜色为 红, == 则白, < 则绿色
     *
     * @param table
     * @param index
     */
    public static void setJTableColorAccordingColValueCompareToZero(JTable table, int index) {
        try {
            DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {
                //重写getTableCellRendererComponent 方法
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                               boolean hasFocus, int row, int column) {
                    // @key: 需要把视图的 row转换为 模型里面真实的row, 得到真正的数据! 有 行列号, 转换为view/model中 4大方法
                    row = table.convertRowIndexToModel(row);

                    //##################### 这里是你需要看需求修改的部分
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    setBackground(COLOR_THEME_MINOR);
                    double accordingValue = Double.parseDouble(model.getDataVector().get(row).get(index).toString());
                    if (accordingValue < 0.0) {
                        setForeground(Color.green);
                    } else if (accordingValue > 0.0) {
                        setForeground(Color.red);
                    } else {
                        setForeground(Color.white);
                    }
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            };
            //对每行的每一个单元格
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                table.getColumn(table.getColumnName(i)).setCellRenderer(dtcr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void update() {
        if (newData == null) {
            return;
        }

        if (newData instanceof DataFrame) { // 账户状态: 各种df数据
            DataFrame<Object> newDf = (DataFrame<Object>) newData;
            if (title.equals(CURRENT_HOLD_TITLE)) {
                List<Object> codes = newDf.col("证券代码").stream().map(value->String.valueOf(value.toString())).collect(
                        Collectors.toList());
                newDf.convert();
                for (int i = 0; i < newDf.length(); i++) {
                    newDf.set(i, "证券代码", codes.get(i));
                }
            }
            if (jTable == null) { // 首次刷新
                Vector<Vector<Object>> datas = new Vector<>();
                for (int i = 0; i < newDf.length(); i++) {
                    datas.add(new Vector<>(newDf.row(i)));
                }
                Vector<Object> cols = new Vector<>(newDf.columns());
                DefaultTableModel model = new DefaultTableModel(datas, cols) {
                    @Override
                    public Class getColumnClass(int column) {
                        Class returnValue;
                        if ((column >= 0) && (column < getColumnCount())) {
                            returnValue = getValueAt(0, column).getClass();
//                            try {
//                                Double.parseDouble(getValueAt(0, column).toString()); // 尝试转换Double, 失败则视为Object
//                                return Double.class;
//                            } catch (Exception e) {
//                                returnValue = Object.class;
//                            }
                        } else {
                            returnValue = Object.class;
                        }
                        return returnValue;
                    }
                };

                jTable = new JTable();
                jTable.setGridColor(Color.black);
                jTable.setBackground(COLOR_THEME_MINOR);
                jTable.setModel(model);
                jTable.setRowSorter(new TableRowSorter<DefaultTableModel>(model));

                jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
            } else { // 不断更新时
                DefaultTableModel model = (DefaultTableModel) jTable.getModel();
                fullFlushDfData(newDf, model);
            }
            fitTableColumns(jTable);
            if (title.equals(CURRENT_HOLD_TITLE)) {
                setJTableColorAccordingColValueCompareToZero(jTable, todayProfitColIndex); //首次设置
            }
        } else if (newData instanceof ConcurrentHashMap) { // 账户状态: 9项资金数据
            ConcurrentHashMap<String, Double> newMap = (ConcurrentHashMap) newData;
            String content = jsonStrToHtmlFormat(JSONUtilS.toJsonPrettyStr(newMap));
            jLabel.setText(content);
            if (title.equals(CURRENT_HOLD_TITLE)) {
                setJTableColorAccordingColValueCompareToZero(jTable, todayProfitColIndex); //首次设置
            }
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
