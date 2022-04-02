package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.wencai;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.datasource.ths.wencai.WenCaiResult;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * description: 模拟问财api; 上搜索框, 中条件解析结果, 下 table显示; table每行可右键, 选择加入 行业概念/个股 等
 * 功能有待开发
 *
 * @author: admin
 * @date: 2022/3/28/028-21:20:34
 */
@Getter
public class WenCaiApiPanelForPlan extends DisplayPanel {
    private static WenCaiApiPanelForPlan INSTANCE;

    public static WenCaiApiPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new WenCaiApiPanelForPlan(mainDisplayWindow);
        }
        INSTANCE.update();
        return INSTANCE;
    }

    MainDisplayWindow mainDisplayWindow;
    protected JScrollPane jScrollPane;
    protected JXTable jTable;

    JXPanel wenCaiSearchPanel;

    // 搜索框和条件解析栏放在 NORTH, 两者同panel

    public WenCaiApiPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        initTableScrollPanel();
        initSearchAndConditionParsePanel();

        this.add(wenCaiSearchPanel, BorderLayout.NORTH);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.update();
    }

    JXFindBarS jxFindBarSForTable;
    JLabel tableLenthLabel; // 显示表格行数

    private void initSearchAndConditionParsePanel() {
        wenCaiSearchPanel = new JXPanel();
        wenCaiSearchPanel.setLayout(new BorderLayout());
        wenCaiSearchPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));

        // 1.问题输入搜索框
        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JTextField questionTextField = new JTextField();
        questionTextField.setColumns(80);
        questionTextField.setBackground(Color.black);
        questionTextField.setForeground(Color.gray);
        questionTextField.setCaretColor(Color.red);
        questionTextField.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        questionTextField.setBorder(null);
        questionTextField.setText("同花顺行业指数"); // todo: 测试
        questionTextField.requestFocus();

        JButton findButton = ButtonFactory.getButton("问财!  ");
        findButton.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        findButton.setForeground(Color.red);
        questionTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findButton.doClick(); // 按下问财按钮
                }
            }
        });
        JButton clearButton = ButtonFactory.getButton("清空");
        clearButton.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        clearButton.setForeground(Color.red);
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                questionTextField.setText("");
            }
        });
        questionPanel.add(findButton);
        questionPanel.add(questionTextField);
        questionPanel.add(clearButton);

        // 2.条件解析结果栏
        JPanel conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        conditionsPanel.setPreferredSize(new Dimension(2560, 80));

        // 3.jTable gui层面的搜索栏
        JPanel tableRelatePanel = new JPanel();
        tableRelatePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        jxFindBarSForTable = new JXFindBarS(Color.pink);

        tableLenthLabel = new JLabel();
        tableLenthLabel.setForeground(Color.green);
        tableRelatePanel.add(jxFindBarSForTable);
        tableRelatePanel.add(tableLenthLabel);

        // 4.添加
        wenCaiSearchPanel.add(questionPanel, BorderLayout.NORTH);
        wenCaiSearchPanel.add(conditionsPanel, BorderLayout.CENTER);
        wenCaiSearchPanel.add(tableRelatePanel, BorderLayout.SOUTH);

        WenCaiApiPanelForPlan panelTemp = this;
        findButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionTextField.getText();
                JDialog dialog = new JDialog(TraderGui.INSTANCE, "问财查询中...", true);
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        WenCaiResult wenCaiResult = WenCaiApi.wenCaiQueryResult(question);
                        if (wenCaiResult == null) {
                            return;
                        }

                        JSONArray conditionsParsed = wenCaiResult.getChunksInfo();
                        conditionsPanel.removeAll(); // 删除所有, 然后添加
                        if (conditionsParsed != null) {
                            for (Object condition : conditionsParsed) {
                                JLabel conditionLabel = new JLabel(condition.toString());
                                conditionLabel.setForeground(Color.red);
                                conditionLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
                                conditionLabel.setBackground(Color.black);
                                conditionLabel.setFont(new Font("微软雅黑", Font.PLAIN, 18));

                                JPanel jPanel = new JPanel();
                                jPanel.setLayout(new BorderLayout());
                                jPanel.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
                                jPanel.add(conditionLabel, BorderLayout.CENTER);
                                conditionsPanel.add(jPanel);
                            }
                        }
                        panelTemp.updateTable(wenCaiResult.getDfResult());
                        mainDisplayWindow.flushBounds(); // 调用此, 才能刷新显示label列表
                        ManiLog.put(StrUtil.format("问财查询成功: {}", question));
                        dialog.dispose();
                        questionTextField.requestFocus();
                    }
                }, true);
                JPanel jPanel1 = new JPanel();
                jPanel1.setLayout(new BorderLayout());
                JLabel jLabel = new JLabel(GuiCommonUtil.buildDialogShowStr("正在查询问财问题:", question));
                jPanel1.add(jLabel, BorderLayout.CENTER);
                dialog.setContentPane(jPanel1);
                dialog.setSize(new Dimension(500, 300));
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });


    }

    private void initTableScrollPanel() {
        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        JLabel label = new JLabel("数据获取中"); // 默认显示内容
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // 占位
//        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        jScrollPane.getViewport().setBackground(COLOR_CHART_BG_EM);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS); // 一般都需要
        jScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS); // 一般都需要

        JLabel tempLabel = new JLabel("暂无数据");
        tempLabel.setForeground(Color.red);
        jScrollPane.setViewportView(tempLabel);
    }


    /**
     * @return
     */
    public void updateTable(DataFrame newData) {
        this.newDf = newData;
        this.update();
    }

    protected DataFrame<Object> newDf; // 持有原始df数据

    public static HashSet<String> highlightColNameSet = new HashSet<>(Arrays.asList(
            "股票简称", "指数简称"
    ));

    JPopupMenu popupForTable; // 表格右键菜单
    int popupPointRow = -1; // 暂存表格右键菜单时, 鼠标点所在行; 在JTable的右键回调中设置

    private void initPopupForTable() {
        popupForTable = new JPopupMenu();
        popupForTable.setBackground(Color.white);
//        popupForTable.add(new JSeparator());
        JMenuItem generateIndustryItem1 = new JMenuItem("生成行业[当前行]");
        generateIndustryItem1.setForeground(Color.black);
        generateIndustryItem1.setBackground(Color.white);
        generateIndustryItem1.setBorder(null);
        generateIndustryItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                int index = rowSorter.convertRowIndexToModel(popupPointRow); // 模型中的row; 本质等于 newDf 的行数

                String name = null;
                try {
                    name = newDf.get(index, "指数简称").toString();
                } catch (Exception ex) {
                    ManiLog.put("生成行业[当前行]失败: 获取行业名称失败");
                    return;
                }
                IndustryConceptThsOfPlan bean;
                try {
                    bean = IndustryConceptThsOfPlanDao
                            .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                    IndustryConceptThsOfPlan.Type.INDUSTRY);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ManiLog.put("生成行业[当前行]失败: 生成bean过程失败!");
                    return;
                }
                ManiLog.put(StrUtil.format("生成行业[当前行]成功: {}", bean.getName()));
            }
        });
        JMenuItem generateIndustryItem2 = new JMenuItem("生成行业[已选择行]");
        generateIndustryItem2.setForeground(Color.black);
        generateIndustryItem2.setBackground(Color.white);
        generateIndustryItem2.setBorder(null);
        generateIndustryItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                int[] selectedRows = jTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                    int index = rowSorter.convertRowIndexToModel(selectedRow); // 模型中的row; 本质等于 newDf 的行数
                    String name = null;
                    try {
                        name = newDf.get(index, "指数简称").toString();
                    } catch (Exception ex) {
                        ManiLog.put("生成行业[当前行]失败: 获取行业名称失败");
                        continue;
                    }
                    IndustryConceptThsOfPlan bean;
                    try {
                        bean = IndustryConceptThsOfPlanDao
                                .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                        IndustryConceptThsOfPlan.Type.INDUSTRY);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        ManiLog.put("生成行业[当前行]失败: 生成bean过程失败!");
                        continue;
                    }
                    ManiLog.put(StrUtil.format("生成行业[当前行]成功: {}", bean.getName()));
                }

            }
        });
        popupForTable.add(generateIndustryItem1);
        popupForTable.add(generateIndustryItem2);
        popupForTable.add(new JSeparator());

        JMenuItem generateConceptItem1 = new JMenuItem("生成概念[当前行]");
        generateConceptItem1.setForeground(Color.red);
        generateConceptItem1.setBackground(Color.white);
        generateConceptItem1.setBorder(null);
        generateConceptItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                int index = rowSorter.convertRowIndexToModel(popupPointRow); // 模型中的row; 本质等于 newDf 的行数

                String name = null;
                try {
                    name = newDf.get(index, "指数简称").toString();
                } catch (Exception ex) {
                    ManiLog.put("生成概念[当前行]失败: 获取概念名称失败");
                    return;
                }
                IndustryConceptThsOfPlan bean;
                try {
                    bean = IndustryConceptThsOfPlanDao
                            .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                    IndustryConceptThsOfPlan.Type.CONCEPT);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ManiLog.put("生成概念[当前行]失败: 生成bean过程失败!");
                    return;
                }
                ManiLog.put(StrUtil.format("生成概念[当前行]成功: {}", bean.getName()));
            }
        });
        JMenuItem generateConceptItem2 = new JMenuItem("生成概念[已选择行]");
        generateConceptItem2.setForeground(Color.red);
        generateConceptItem2.setBackground(Color.white);
        generateConceptItem2.setBorder(null);
        generateConceptItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popupPointRow == -1 || jTable == null) {
                    return;
                }
                int[] selectedRows = jTable.getSelectedRows();
                for (int selectedRow : selectedRows) {
                    RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                    int index = rowSorter.convertRowIndexToModel(selectedRow); // 模型中的row; 本质等于 newDf 的行数
                    String name = null;
                    try {
                        name = newDf.get(index, "指数简称").toString();
                    } catch (Exception ex) {
                        ManiLog.put("生成概念[当前行]失败: 获取概念名称失败");
                        continue;
                    }
                    IndustryConceptThsOfPlan bean;
                    try {
                        bean = IndustryConceptThsOfPlanDao
                                .getOrInitBeanForPlan(name, PlanReviewDateTimeDecider.getUniqueDatetime(),
                                        IndustryConceptThsOfPlan.Type.CONCEPT);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        ManiLog.put("生成概念[当前行]失败: 生成bean过程失败!");
                        continue;
                    }
                    ManiLog.put(StrUtil.format("生成概念[当前行]成功: {}", bean.getName()));
                }
            }
        });

        popupForTable.add(generateConceptItem1);
        popupForTable.add(generateConceptItem2);
        popupForTable.add(new JSeparator());

        JMenuItem cancel = new JMenuItem("取消");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupForTable.setVisible(false);
            }
        });
        popupForTable.add(new JSeparator());
        popupForTable.add(cancel);
    }

    @Override
    public void update() {
        if (newDf == null) {
            return;
        }

        // 每次均新建表对象!

        Vector<Vector<Object>> datas = new Vector<>();
        for (int i = 0; i < newDf.length(); i++) {
            datas.add(new Vector<>(newDf.row(i)));
        }
        Vector<Object> cols = new Vector<>(newDf.columns());
        DefaultTableModel model = new DefaultTableModel(datas, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不可编辑!
            }
        };
        jTable = new JXTable();
        jTable.setModel(model);
        removeEnterKeyDefaultAction();
        jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jTable.getSelectedRow();

            }
        });
        initJTableStyle();
        jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fitTableColumns(jTable);

        jTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    popupPointRow = jTable.rowAtPoint(me.getPoint());
                    if (popupPointRow != -1) {
                        // final int column = jTable.columnAtPoint(me.getPoint());
                        if (popupForTable == null) {
                            initPopupForTable();
                        }
                        popupForTable.show(me.getComponent(), me.getX(), me.getY());
                    }
                }
            }
        });


        for (Object column : newDf.columns()) {
            if (highlightColNameSet.contains(column)) {
                DefaultTableCellRenderer cellRendererOfTitle = new DefaultTableCellRenderer();
                cellRendererOfTitle.setForeground(COLOR_LIST_RAISE_EM);
                jTable.getColumn(column).setCellRenderer(cellRendererOfTitle);
            }
        }

        jxFindBarSForTable.setSearchable(jTable.getSearchable());
        tableLenthLabel.setText(StrUtil.format("  结果行数: {}", newDf.length()));
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
            if (dummyIndex <= 20 && dummyIndex > 8) {
                column.setWidth(Math.min(actualWidth, 80)); // 12字段限制最大宽度
            } else {
                column.setWidth(Math.max(actualWidth, 80)); // 多5
            }
//            break; // 仅第一列日期. 其他的平均
//
//            if (dummyIndex == 5) {
//                column.setWidth(5); // 多5
//            }
//            if (dummyIndex == 8) {
//                column.setWidth(5); // 多5
//            }
//            if (dummyIndex == 9) {
//                column.setWidth(5); // 多5
//            }


            dummyIndex++;
        }
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
