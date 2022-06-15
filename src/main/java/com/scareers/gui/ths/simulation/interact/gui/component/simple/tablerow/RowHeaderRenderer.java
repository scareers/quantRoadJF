package com.scareers.gui.ths.simulation.interact.gui.component.simple.tablerow;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * description:
 * * 用于显示RowHeader的JTable的渲染器，可以实现动态增加，删除行，在Table中增加、删除行时RowHeader
 * * 一起变化。当选择某行时，该行颜色会发生变化
 *
 * @author: admin
 * @date: 2022/6/15/015-22:35:34
 */
public class RowHeaderRenderer extends JLabel implements TableCellRenderer, ListSelectionListener {
    JTable reftable;//需要添加rowHeader的JTable
    JTable tableShow;//用于显示rowHeader的JTable
    public RowHeaderRenderer(JTable reftable,JTable tableShow)
    {
        this.reftable = reftable;
        this.tableShow=tableShow;
        //增加监听器，实现当在reftable中选择行时，RowHeader会发生颜色变化
        ListSelectionModel listModel=reftable.getSelectionModel();
        listModel.addListSelectionListener(this);
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object obj,
                                                   boolean isSelected, boolean hasFocus, int row, int col)
    {
        ((RowHeaderTableModel)table.getModel()).setRowCount(reftable.getRowCount());
        JTableHeader header = reftable.getTableHeader();
        this.setOpaque(true);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));//设置为TableHeader的边框类型
        setHorizontalAlignment(CENTER);//让text居中显示
        setBackground(header.getBackground());//设置背景色为TableHeader的背景色
        if ( isSelect(row) )    //当选取单元格时,在row header上设置成选取颜色
        {
            setForeground(Color.red);
            setBackground(Color.lightGray);
        }
        else
        {
            setForeground(Color.red);
//            setForeground(header.getForeground());
        }
        setFont(header.getFont());
        setText(String.valueOf(row+1));
        return this;
    }
    @Override
    public void valueChanged(ListSelectionEvent e){
        this.tableShow.repaint();
    }
    private boolean isSelect(int row)
    {
        int[] sel = reftable.getSelectedRows();
        for (int value : sel) {
            if (value == row) {
                return true;
            }
        }
        return false;
    }
}