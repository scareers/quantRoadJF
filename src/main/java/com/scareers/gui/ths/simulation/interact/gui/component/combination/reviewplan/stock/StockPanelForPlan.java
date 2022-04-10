package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.stock;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.ThsFsDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.ThsKLineDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept.IndustryConceptThsOfPlanEditorPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.wencai.WenCaiApiPanelForPlan;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.tools.stockplan.stock.bean.StockGroupOfPlan;
import com.scareers.tools.stockplan.stock.bean.StockOfPlan;
import com.scareers.tools.stockplan.stock.bean.dao.StockGroupOfPlanDao;
import com.scareers.tools.stockplan.stock.bean.dao.StockOfPlanDao;
import com.scareers.tools.stockplan.stock.bean.selector.MultiConceptSelector;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.*;
import org.jdesktop.swingx.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * description: 个股展示页面; 最上显示当前股票组列表;
 *
 * @author: admin
 * @date: 2022/3/28/028-21:20:34
 */
@Getter
public class StockPanelForPlan extends DisplayPanel {
    private static StockPanelForPlan INSTANCE;


    public static StockPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new StockPanelForPlan(mainDisplayWindow);
        }
        INSTANCE.update();
        return INSTANCE;
    }

    MainDisplayWindow mainDisplayWindow;
    protected List<StockGroupOfPlan> stockGroupList; // 当前所有 股票组
    protected StockGroupOfPlan currentStockGroup; // 当前选中股票组; 表格将展示其个股; 默认 "所有股票组"

    protected StockOfPlanEditorPanel editorPanel; // 个股编辑面板,唯一
    protected StockOfPlan currentBean; // 当前选中个股对象!

    protected JXTable jTable; // 个股列表展示表格
    protected JScrollPane jScrollPaneForTable;

    protected JPanel buttonContainer; // 功能按钮容器, 包括对按钮组的操作!
    protected JPanel stockGroupListContainer; // 股票组按钮容器! 切换股票组, 使用 单选按钮组实现, 不使用RadioButton,更加灵活!
    protected JPanel buttonAndStockGroupContainer; // 将包含以上两个容器!

    protected JButton buttonFlushAll; // 全量刷新按钮, 受股票组限制

    protected JXCollapsiblePane klineCollapsiblePane; // k线图折叠面板对象
    protected JPanel klineDisplayContainerPanel; // k线图容器

    public StockPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        initJScrollPaneForTable(); // 表格包裹面板初始化


        // 底部增加可折叠面板, 显示 概念行业 的分时图/k线图! --> ths 4图
        initKlineDisplayPanel();

        // 包装一下, 将按钮放于表格上方
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());
        panelTemp.add(klineCollapsiblePane, BorderLayout.SOUTH); // 可折叠展示k线, 放在南边

        initStockGroupListContainer(); // 股票组初始化
        initButtonContainerAndButtons(); // 按钮组初始化
        buttonAndStockGroupContainer = new JPanel(); // 包含以上2容器
        buttonAndStockGroupContainer.setBorder(null);
        buttonAndStockGroupContainer.setLayout(new BorderLayout());
        buttonAndStockGroupContainer.add(stockGroupListContainer, BorderLayout.NORTH);
        buttonAndStockGroupContainer.add(buttonContainer, BorderLayout.CENTER);

        panelTemp.add(buttonAndStockGroupContainer, BorderLayout.NORTH);
        panelTemp.add(jScrollPaneForTable, BorderLayout.CENTER);
        this.add(panelTemp, BorderLayout.CENTER);

        editorPanel = new StockOfPlanEditorPanel(this);
        JPanel panel = new JPanel();
        panel.add(editorPanel.getEditorContainerScrollPane()); // 不直接编辑器, 而容器滚动面板
        this.add(panel, BorderLayout.WEST); // 需要包装一下, 否则 editorPanel将被拉长

        // 异步进行
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                updateStockGroupForce(); // 异步刷新 股票组
            }
        }, true);
    }

    /**
     * 股票组面板, 股票组将懒加载, 见下 updateStockGroupForce()
     */
    private void initStockGroupListContainer() {
        stockGroupListContainer = new JPanel(); // 股票组面板, 用于切换股票组, 控制表格显示
        stockGroupListContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        stockGroupListContainer.setBorder(BorderFactory.createLineBorder(Color.black, 1, true));
//        stockGroupListContainer.setPreferredSize(new Dimension(2560, 80));
        stockGroupListContainer.add(ButtonFactory.getButton("初始化股票组中"));
    }

    /**
     * 强制全量刷新股票组; 包括属性和 股票组面板(按钮组)
     */
    private void updateStockGroupForce() {
        // 强制获取最新股票组列表
        stockGroupList = StockGroupOfPlanDao.getOrInitStockGroupsForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        stockGroupListContainer.removeAll(); // 清空
        StockGroupButton first = null;
        for (StockGroupOfPlan stockGroupBean : stockGroupList) {
            StockGroupButton stockGroupButton = new StockGroupButton(StrUtil.sub(stockGroupBean.getName(), 0, 6),
                    stockGroupBean, this);
            if (first == null) {
                first = stockGroupButton;
            }
            stockGroupButton.setBackground(COLOR_THEME_MINOR); // 次色
            stockGroupButton.setBorderPainted(false); // 无边框
            stockGroupButton.setForeground(COLOR_GRAY_COMMON); // 常态字灰

            stockGroupListContainer.add(stockGroupButton); // 添加新

        }
        if (first != null) {
            first.doClick(); // 点击所有股票
        }
    }

    /**
     * 股票组切换按钮! 持有 StockGroupOfPlan bean; 当点击时, 将切换父亲面板的 currentStockGroup 为自身 bean
     */
    public static class StockGroupButton extends FuncButton {
        StockGroupOfPlan changeToBean;
        StockPanelForPlan parentPanel;

        public StockGroupButton(String text, StockGroupOfPlan changeToBean, StockPanelForPlan parentPanel) {
            super(text);
            this.changeToBean = changeToBean;
            this.parentPanel = parentPanel;
            this.setToolTipText(changeToBean.getName() + "\n\n" + changeToBean.getDescription());
            this.addActionListener(e -> {
                parentPanel.currentStockGroup = changeToBean; // 修改选择的股票组;
                ThreadUtil.execAsync(parentPanel::update, true); // 更新显示
                ManiLog.put(StrUtil.format("已切换股票组: {}", changeToBean.getName()));
            });
        }
    }

    private void initJScrollPaneForTable() {
        jScrollPaneForTable = new JScrollPane();
        jScrollPaneForTable.setBorder(null);
        JLabel label = new JLabel("数据获取中"); // 默认显示内容
        label.setForeground(Color.red);
        jScrollPaneForTable.setViewportView(label); // 占位
        jScrollPaneForTable.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForTable, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPaneForTable.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS); // 一般都需要
        jScrollPaneForTable.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS); // 一般都需要
    }

    ThsFsDisplayPanel fsDisplayPanel; // 分时图
    ThsKLineDisplayPanel dailyKLineDisplayPanel; // 日k线
    ThsKLineDisplayPanel weeklyKLineDisplayPanel; // 周k线
    ThsKLineDisplayPanel monthlyKLineDisplayPanel; // 月k线

    private void initKlineDisplayPanel() {
        klineCollapsiblePane = new JXCollapsiblePane();
        klineCollapsiblePane.setLayout(new BorderLayout());

        klineDisplayContainerPanel = new JPanel();
        klineDisplayContainerPanel.setLayout(new GridLayout(1, 4, -1, -1)); // 4份 k线
        // 4大k线
        fsDisplayPanel = new ThsFsDisplayPanel();
        fsDisplayPanel.setPreferredSize(new Dimension(300, 300));
        dailyKLineDisplayPanel = new ThsKLineDisplayPanel();
        weeklyKLineDisplayPanel = new ThsKLineDisplayPanel();
        monthlyKLineDisplayPanel = new ThsKLineDisplayPanel();


        klineDisplayContainerPanel.add(fsDisplayPanel);
        klineDisplayContainerPanel.add(dailyKLineDisplayPanel);
        klineDisplayContainerPanel.add(weeklyKLineDisplayPanel);
        klineDisplayContainerPanel.add(monthlyKLineDisplayPanel);

        klineCollapsiblePane.add("Center", klineDisplayContainerPanel);
        klineCollapsiblePane.setAnimated(true);
        klineCollapsiblePane.setCollapsed(false); // 默认展开
    }

    /**
     * 当切换选项, 也应该自动切换分时和k线显示内容. 见 jTable的监听
     */
    private void updateKLineAndFsDisplay() {
        if (this.currentBean == null) {
            return;
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                poolOfFsAndKlineUpdate.submit(() -> updateFsDisplay());
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(0, dailyKLineDisplayPanel, "-日K", "更新日k线失败: {}"));
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(1, weeklyKLineDisplayPanel, "-周K", "更新周k线失败: {}"));
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(2, monthlyKLineDisplayPanel, "-月K", "更新月k线失败: {}"));
            }
        }, true);
    }

    ThreadPoolExecutor poolOfFsAndKlineUpdate = new ThreadPoolExecutor(4, 6, 1000, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(Integer.MAX_VALUE));

    private void updateFsDisplay() {
        try {
            DataFrame<Object> fs1M = WenCaiDataApi
                    .getFS1M(this.currentBean.getMarketCode(), this.currentBean.getCode());
            Double preClose = 10.0;
            try {
                DataFrame<Object> last5DailyKLine = WenCaiDataApi
                        .getLastNKline(this.currentBean.getMarketCode(), this.currentBean.getCode(), 0, 0, 2);
                preClose = Double.valueOf(last5DailyKLine.get(last5DailyKLine.length() - 2, "收盘").toString());
            } catch (NumberFormatException e) {
                preClose = Double.valueOf(fs1M.get(0, "收盘").toString());
                ManiLog.put(StrUtil.format("获取昨日收盘价失败,使用第一条分时图close替代: {}", currentBean.getName()));
            }
            fsDisplayPanel.update(fs1M, currentBean.getName() + "-分时", preClose);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            ManiLog.put(StrUtil.format("更新分时图失败: {}", currentBean.getName()));
        }
    }

    public static final int preferKLinePeriods = 60; // k线 画多少根

    private void updateKLine(int i, ThsKLineDisplayPanel dailyKLineDisplayPanel, String s, String s2) {
        try {
            DataFrame<Object> lastNKline = WenCaiDataApi
                    .getLastNKline(currentBean.getMarketCode(), currentBean.getCode(), i, 1
                            , preferKLinePeriods);
            dailyKLineDisplayPanel.update(lastNKline, currentBean.getName() + s);
        } catch (Exception e) {
            e.printStackTrace();
            ManiLog.put(StrUtil.format(s2, currentBean.getName()));
        }
    }

    private void initButtonContainerAndButtons() {
        // 1.1.全量刷新显示
        buttonFlushAll = ButtonFactory.getButton("全量刷新");
        buttonFlushAll.setMaximumSize(new Dimension(60, 16));
        StockPanelForPlan panelForPlan = this;
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelForPlan.update(); // 点击后 表格全量更新 df数据,
            }
        });

        // 2.1.添加股票组
        JButton addStockGroupButton = ButtonFactory.getButton("添加股票组");
        addStockGroupButton.setMaximumSize(new Dimension(60, 16));
        addStockGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StockGroupEditDialog dialog = new StockGroupEditDialog("添加股票组", null);
                dialog.setVisible(true);
            }
        });

        // 100.测试按钮
        JButton testButton = ButtonFactory.getButton("测试");
        testButton.setMaximumSize(new Dimension(60, 16));
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.setBorder(null);
        buttonContainer.add(buttonFlushAll);

        // 分隔符
        JLabel spliter = new JLabel("||");
        spliter.setBackground(COLOR_THEME_MINOR);
        spliter.setForeground(Color.red);
        buttonContainer.add(spliter); // 分割符, 后面为股票组相关功能; 前面为个股相关

        buttonContainer.add(addStockGroupButton);
        buttonContainer.add(testButton);
    }

    /**
     * 股票组编辑对话框; 股票组的 添加,修改, 均使用相同格式对话框
     */
    public static class StockGroupEditDialog extends JDialog {
        StockGroupOfPlan stockGroupBean; // 持有股票组bean, 当添加时, 为null, 新建bean并设置保存

        public StockGroupEditDialog(String title, StockGroupOfPlan stockGroupBean) {
            super(TraderGui.INSTANCE, title, false);
            this.stockGroupBean = stockGroupBean;

            // 按下esc关闭对话框, 实测不能modal模式, 否则监听无效
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            StockGroupEditDialog dialog = this;
            this.getRootPane().registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            dialog.setContentPane(panel);


            //添加控件到对话框
            JScrollPane jScrollPane = new JScrollPane();
            jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
            BasicScrollBarUIS
                    .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
            jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
            jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setViewportView(getMainPanel());
            panel.add(jScrollPane, BorderLayout.CENTER);

            //显示对话框（setVisible()方法会阻塞，直到对话框关闭）
            dialog.setSize(1500, 1200);
            dialog.setLocationRelativeTo(dialog.getParent());
        }



        // 3. 成分股编辑 panel.
        WenCaiApiPanelForPlan stocksSelectPanel;

        private JPanel getMainPanel() {
            JPanel jPanel = new JPanel();
            jPanel.setBorder(null);
            jPanel.setLayout(new BorderLayout());

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new BorderLayout());

//            JPanel
            // todo

            jPanel.add(new JLabel("测试"));
            return jPanel;
        }

        private void initStockEditPanel() {
            stocksSelectPanel = WenCaiApiPanelForPlan.getInstance(MainDisplayWindow.getInstance());
        }
    }


    /**
     * 全量 从数据库读取 bean列表 以更新 表格显示
     * 需要设置属性 beanMap, 以及 newDf 两大属性
     *
     * @return
     */
    public void flushBeanMapAndShowDf() {
        List<StockOfPlan> beans = StockOfPlanDao
                .getBeanListForPlan(PlanReviewDateTimeDecider.getUniqueDatetime(), this.currentStockGroup);
        ConcurrentHashMap<Long, StockOfPlan> tempMap = new ConcurrentHashMap<>();
        beans.forEach(value -> tempMap.put(value.getId(), value));
        this.beanMap = tempMap;
        this.newDf = StockOfPlan.buildDfFromBeanList(beans);
    }

    protected ConcurrentHashMap<Long, StockOfPlan> beanMap;
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
                int preRow = -10;

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    int row = jTable.getSelectedRow();
                    if (row != preRow) {
                        preRow = row;
                    } else {
                        return; // 实际更新了行, 才调用后面逻辑
                    }
                    Object valueAt;
                    try {
                        valueAt = model.getValueAt(row, 0);
                    } catch (Exception ex) {
                        return;
                    }
                    currentBean = beanMap.get(Long.parseLong(valueAt.toString()));
                    editorPanel.update(currentBean);
                    updateKLineAndFsDisplay(); // k线展示也改变
                }
            });
            initJTableStyle();
            jScrollPaneForTable.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
            jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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
//        jTable.getColumn("名称").setCellRenderer(cellRendererOfTitle);

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
            if (dummyIndex <= 20 && dummyIndex > 8) {
                column.setWidth(Math.min(actualWidth, 80)); // 12字段限制最大宽度
            } else {
                column.setWidth(Math.max(actualWidth, 80)); // 多5
            }
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
