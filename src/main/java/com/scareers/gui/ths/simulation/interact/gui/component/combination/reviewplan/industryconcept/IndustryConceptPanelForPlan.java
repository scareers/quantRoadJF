package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * description: 行业和概念 操盘计划: 使用同花顺行业和概念
 * 重点是 显示和编辑 IndustryConceptThsOfPlan 对象列表
 *
 * @author: admin
 * @date: 2022/3/28/028-21:20:34
 */
@Getter
public class IndustryConceptPanelForPlan extends DisplayPanel {
    private static IndustryConceptPanelForPlan INSTANCE;

    public static IndustryConceptPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new IndustryConceptPanelForPlan(mainDisplayWindow);
        }
        INSTANCE.update();
        return INSTANCE;
    }

    MainDisplayWindow mainDisplayWindow;
    protected IndustryConceptThsOfPlanEditorPanel editorPanel; // 编辑面
    protected IndustryConceptThsOfPlan currentBean;

    protected JXTable jTable;
    protected JScrollPane jScrollPane;

    protected JPanel buttonContainer; // 功能按钮容器
    protected JButton buttonFlushAll; // 全量刷新按钮

    public IndustryConceptPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
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
        jScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS); // 一般都需要
        initButtons();

        // 包装一下, 将按钮放于表格上方
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());
        panelTemp.add(buttonContainer, BorderLayout.NORTH);
        panelTemp.add(jScrollPane, BorderLayout.CENTER);
        this.add(panelTemp, BorderLayout.CENTER);

        editorPanel = new IndustryConceptThsOfPlanEditorPanel(this);
        JPanel panel = new JPanel();
        panel.add(editorPanel);
        this.add(panel, BorderLayout.WEST); // 需要包装一下, 否则 editorPanel将被拉长
    }

    private void initButtons() {
        // 1.全量刷新显示
        buttonFlushAll = ButtonFactory.getButton("全量刷新");
        buttonFlushAll.setMaximumSize(new Dimension(60, 16));
        IndustryConceptPanelForPlan panelForPlan = this;
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelForPlan.update(); // 点击后 表格全量更新 df数据,
            }
        });

        // 2.单个正在编辑的bean保存
        JButton saveEditingBeanButton = ButtonFactory.getButton("保存编辑");
        saveEditingBeanButton.setMaximumSize(new Dimension(60, 16));
        saveEditingBeanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndustryConceptThsOfPlanEditorPanel.tryAutoSaveEditedBean(panelForPlan.editorPanel, "概念行业");
            }
        });

        // 3.批量新增: 将以各种方式, 批量增加bean; 例如: 昨日全部, 或者 行业全列表, 概念全列表
        // @noti: 该功能对话框, 可选择将 "合理的可编辑字段", 初始赋值为 历史中有记录的最新一个相同行业/概念bean 的对应字段
        // 这样省去大量相同的自定义过程; 只需要修改不同的
        // 这些可变字段不会被复制过来: 预判相关4字段
        // @noti: 将自动设置关联trend
        JButton addBeansBatchButton = ButtonFactory.getButton("批量添加");
        addBeansBatchButton.setMaximumSize(new Dimension(60, 16));
        addBeansBatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 弹出对话框, 批量选择, 新增今日 bean 列表到数据库; 随后调用 update() 刷新显示


                // 0.获取今日已有bean, 以下列表都将不显示今天已存在的bean, 排除掉
                List<IndustryConceptThsOfPlan> existsBeanList = IndustryConceptThsOfPlanDao
                        .getBeanListForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());

                // 1.读取概念列表, 二级/三级行业列表, 仅带涨跌幅, 和二三级行业 字段, 方便排序

                // todo: 补齐
                // 2.读取昨日所有已存在bean, 其名称和类型, 将被默认添加到 今日列表


                // 3. 同样, 添加/删除 按钮, 确定保存按钮.




                panelForPlan.update(); // 更新列表,使得 flushWithRelatedTrends 访问 beanMap有效
                IndustryConceptThsOfPlanEditorPanel.flushWithRelatedTrends(panelForPlan.editorPanel);
                panelForPlan.editorPanel.update(); // 将更新显示自动设置字段
                panelForPlan.update(); // 更新显示
            }
        });


        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.setBorder(null);
        buttonContainer.add(buttonFlushAll);
        buttonContainer.add(saveEditingBeanButton);
    }

    /**
     * 全量 从数据库读取 bean列表 以更新 表格显示
     * 需要设置属性 beanMap, 以及 newDf 两大属性
     *
     * @return
     */
    public void flushBeanMapAndShowDf() {
        List<IndustryConceptThsOfPlan> newsForReviseByType;
        newsForReviseByType =
                IndustryConceptThsOfPlanDao.getBeanListForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        ConcurrentHashMap<Long, IndustryConceptThsOfPlan> tempMap = new ConcurrentHashMap<>();
        newsForReviseByType.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;

        this.newDf = IndustryConceptThsOfPlan.buildDfFromBeanList(newsForReviseByType);
    }

    protected ConcurrentHashMap<Long, IndustryConceptThsOfPlan> beanMap;
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
                }
            };
            jTable = new JXTable();
            jTable.setModel(model);
            removeEnterKeyDefaultAction();
            jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int row = jTable.getSelectedRow();
                    currentBean = beanMap.get(Long.parseLong(model.getValueAt(row, 0).toString()));
                    editorPanel.update(currentBean);
                }
            });
            initJTableStyle();
            jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
            // jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            fitTableColumns(jTable);
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
        jTable.getColumn("名称").setCellRenderer(cellRendererOfTitle);

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
            column.setWidth(Math.max(actualWidth, 80)); // 多5
//            break; // 仅第一列日期. 其他的平均

            if (dummyIndex == 5) {
                column.setWidth(5); // 多5
            }
            if (dummyIndex == 8) {
                column.setWidth(5); // 多5
            }
            if (dummyIndex == 9) {
                column.setWidth(5); // 多5
            }

            dummyIndex++;
        }
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
