package com.scareers.gui.ths.simulation.interact.gui.component.simple.tablerow;

import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * // todo: JXTable 使用起来有bug! 未知原因待解决
 * ---------> 自定义带行号的 表格, 继承 JXTable; 改代码时(增加行号功能), 只需要 新增此类实例, 包裹原表格即可!
 * 即只需要在 原滚动panel实例化后, 调用  原scrollPane.setRowHeaderView(new RowHeaderJTableS(原表对象, 40)); 即可
 * 用于显示RowHeader的JTable，只需要将其加入JScrollPane的RowHeaderView即可为JTable生成行标题
 * 即显示 1-n 的行号;
 */
public class RowHeaderJTableS extends JTable {
    public static void main(String[] args) {
        JFrame mainWindow = new JFrame();

        DefaultTableModel model = new DefaultTableModel(50, 10);
        JTable table = new JTable(model);
        /*将table加入JScrollPane*/
        JScrollPane scrollPane = new JScrollPane(table);
        /*将rowHeaderTable作为row header加入JScrollPane的RowHeaderView区域*/
        scrollPane.setRowHeaderView(new RowHeaderJTableS(table, 40));
        mainWindow.getContentPane().add(scrollPane, BorderLayout.CENTER);
        mainWindow.setVisible(true);
        mainWindow.setSize(400, 200);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private JTable refTable;//需要添加rowHeader的JTable

    /**
     * 为JTable添加RowHeader，
     *
     * @param refTable     需要添加rowHeader的JTable
     * @param columnWideth rowHeader的宽度
     */
    public RowHeaderJTableS(JTable refTable, int columnWidth) {
        super(new RowHeaderTableModel(refTable.getRowCount()));
        this.refTable = refTable;
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);//不可以调整列宽
        this.getColumnModel().getColumn(0).setPreferredWidth(columnWidth);
        this.setDefaultRenderer(Object.class, new RowHeaderRenderer(refTable, this));//设置渲染器
        this.setPreferredScrollableViewportSize(new Dimension(columnWidth, 0));
    }
}