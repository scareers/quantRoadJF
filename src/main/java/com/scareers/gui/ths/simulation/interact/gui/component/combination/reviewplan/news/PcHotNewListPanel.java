package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.editor.PcHotNewEditorPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.editor.SimpleNewEditorPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.tools.stockplan.news.bean.PcHotNewEm;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import org.jdesktop.swingx.JXTable;

import javax.persistence.Index;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;

/**
 * description: 东财pc, 最新热门资讯,弹窗.消息
 * 左边为编辑区, 右边表格显示 !
 *
 * @author: admin
 * @date: 2022/3/13/013-08:50:46
 */
@Getter
public abstract class PcHotNewListPanel extends DisplayPanel {

    protected PcHotNewEditorPanel editorPanel; // 编辑面
    protected PcHotNewEm currentBean;

    protected JXTable jTable;
    protected JScrollPane jScrollPane;

    protected JPanel buttonContainer; // 功能按钮容器
    protected JButton buttonFlushAll; // 全量刷新按钮
    protected JButton saveButton = ButtonFactory.getButton("保存");


    protected NewsTabPanel parentS; // 维护所属 newstab
    protected JXFindBarS jxFindBarSForTable;

    public PcHotNewListPanel(NewsTabPanel parentS) {
        this.parentS = parentS;
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
        jScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS); // 一般都需要
        buttonFlushAll = ButtonFactory.getButton("全量刷新");
        buttonFlushAll.setMaximumSize(new Dimension(60, 16));
        PcHotNewListPanel simpleNewListPanel = this;
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                simpleNewListPanel.update(); // 点击后 表格全量更新 df数据,
            }
        });

        saveButton.setMaximumSize(new Dimension(60, 16));

        jxFindBarSForTable = new JXFindBarS(Color.pink);

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.setBorder(null);
        buttonContainer.add(buttonFlushAll);
        buttonContainer.add(saveButton);
        buttonContainer.add(jxFindBarSForTable);

        // 包装一下, 将按钮放于表格上方
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());
        panelTemp.add(buttonContainer, BorderLayout.NORTH);
        panelTemp.add(jScrollPane, BorderLayout.CENTER);


        this.add(panelTemp, BorderLayout.CENTER);


        editorPanel = new PcHotNewEditorPanel(this);
        JPanel panel = new JPanel();
        panel.add(editorPanel);
        this.add(panel, BorderLayout.WEST); // 需要包装一下, 否则 editorPanel将被拉长

    }

    /**
     * 全量 从数据库读取新闻列表以更新 表格显示
     * 需要设置属性 beanMap, 以及 newDf 两大属性
     *
     * @return
     */
    public abstract void flushBeanMapAndShowDf();

    protected ConcurrentHashMap<Long, PcHotNewEm> beanMap;
    protected DataFrame<Object> newDf; // 持有原始df数据

    @Override
    public void update() {
        flushBeanMapAndShowDf(); // 刷新 beanMap和newDf

        if (newDf == null) {
            return;
        }

        if (jTable == null) { // 首次刷新

            Vector<Vector<Object>> datas = new Vector<>();
            for (int i = 0; i < newDf.length(); i++) {
                datas.add(new Vector<>(newDf.row(i)));
            }
            Vector<Object> cols = new Vector<>(newDf.columns());

            DefaultTableModel model = new DefaultTableModel(datas, cols) {
                @Override
                public boolean isCellEditable(int row, int column) {

                    return false; // 不可编辑!
//                    return true; //
                }
            };

            jTable = new JXTable();
            jTable.setSortable(true); // 默认排序

            jTable.setModel(model);
            removeEnterKeyDefaultAction();
            jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int row = jTable.getSelectedRow();
                    RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                    row = rowSorter.convertRowIndexToModel(row);

                    currentBean = beanMap.get(Long.parseLong(model.getValueAt(row, 0).toString()));
                    editorPanel.update(currentBean);
                }
            });

            jTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = jTable.getSelectedRow();
                    RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                    if (row < -1) {
                        return;
                    }
                    row = rowSorter.convertRowIndexToModel(row);

                    currentBean = beanMap.get(Long.parseLong(model.getValueAt(row, 0).toString()));
                    editorPanel.update(currentBean);
                    if (e.getClickCount() == 2) {
                        CommonUtil.openUrlWithDefaultBrowser(currentBean.getUrl());
                    }
                }
            });
            jTable.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        int row = jTable.getSelectedRow();
                        RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                        row = rowSorter.convertRowIndexToModel(row);

                        currentBean = beanMap
                                .get(Long.parseLong(model.getValueAt(row, 0).toString()));
                        editorPanel.update(currentBean);
                        CommonUtil.openUrlWithDefaultBrowser(currentBean.getUrl());
                    }
                }
            });
            initJTableStyle();
            jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
            fitTableColumns(jTable);
            jxFindBarSForTable.setSearchable(jTable.getSearchable());
        } else { // 不断更新时
            fullFlush();
        }

    }

    /**
     * 设置表样式
     */
    private void initJTableStyle() {
        // 表头框颜色和背景色
        jTable.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(COLOR_LIST_HEADER_FORE_EM);
        for (int i = 0; i < newDf.size(); i++) {
            //i是表头的列
            TableColumn column = jTable.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //表头文字居中
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        jTable.setForeground(COLOR_LIST_FLAT_EM);
        jTable.setBackground(COLOR_LIST_BK_EM);

        DefaultTableCellRenderer cellRendererOfTitle = new DefaultTableCellRenderer();
        cellRendererOfTitle.setForeground(COLOR_LIST_RAISE_EM);
        jTable.getColumn("title").setCellRenderer(cellRendererOfTitle);

        jTable.setRowHeight(30);
        jTable.setFont(new Font("微软雅黑", Font.PLAIN, 18));
    }

    private void removeEnterKeyDefaultAction() {
        ActionMap am = jTable.getActionMap();
        am.getParent().remove("selectNextRowCell"); // 取消默认的: 按下回车键将移动到下一行
        jTable.setActionMap(am);
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 全量刷新逻辑
     *
     * @param fullDf
     * @param model
     */
    protected void fullFlush() {
        DefaultTableModel model = (DefaultTableModel) jTable.getModel();
        Vector<Vector> oldDatas = model.getDataVector();
        Vector<Vector> newDatas = new Vector<>();
        for (int i = 0; i < newDf.length(); i++) {
            newDatas.add(new Vector<>(newDf.row(i)));
        }
        // 将自动增减行, 若新数据行少, 则多的行不会显示, 但本身存在
        model.setRowCount(newDatas.size());

        for (int i = 0; i < Math.min(oldDatas.size(), newDatas.size()); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                Object o = newDatas.get(i).get(j);
                if (o != null) {
                    if (!o.equals(oldDatas.get(i).get(j))) {
                        model.setValueAt(newDatas.get(i).get(j), i, j);
                    }
                }
            }
        }
    }


    /**
     * 表格列宽自适应
     *
     * @param myTable
     */
    protected void fitTableColumns(JTable myTable) {
        JTableHeader header = myTable.getTableHeader();
        int rowCount = myTable.getRowCount();

        Enumeration columns = myTable.getColumnModel().getColumns();

        int dummyIndex = 0;

        while (columns.hasMoreElements()) {
//        if (columns.hasMoreElements()) {
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

            int actualWidth = width + myTable.getIntercellSpacing().width + 2;
            actualWidth = Math.min(700, actualWidth); // 单列最大宽度
            column.setWidth(actualWidth); // 多5
//            break; // 仅第一列日期. 其他的平均

//            if (dummyIndex == 3) { // url
//                column.setWidth(5);
//            }
//            if (dummyIndex == 4) { // 具体内容
//                column.setWidth(5);
//            }
            if (dummyIndex == 5) { // 保存时间
                column.setWidth(10);
            }

            dummyIndex++;
        }
    }
}
