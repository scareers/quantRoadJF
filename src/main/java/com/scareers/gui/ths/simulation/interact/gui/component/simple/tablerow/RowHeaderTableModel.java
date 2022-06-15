package com.scareers.gui.ths.simulation.interact.gui.component.simple.tablerow;

import javax.swing.table.AbstractTableModel;

/**
 * description:
 * 用于显示表头RowHeader的JTable的TableModel，不实际存储数据
 *
 * @author: admin
 * @date: 2022/6/15/015-22:36:57
 */
public class RowHeaderTableModel extends AbstractTableModel {
    private int rowCount;//当前JTable的行数，与需要加RowHeader的TableModel同步

    public RowHeaderTableModel(int rowCount) {
        this.rowCount = rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return row;
    }
}
