package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.stock.bean.selector.MultiConceptSelector;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
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
 * description: 行业和概念 操盘计划: 使用同花顺行业和概念
 * 重点是 显示和编辑 StockOfPlan 对象列表
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

    protected JXCollapsiblePane klineCollapsiblePane;
    protected JPanel klineDisplayContainerPanel;

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


        // 包装一下, 将按钮放于表格上方
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());

        // @update: 在底部增加可折叠面板, 显示 概念行业 的分时图/k线图! --> ths 4图
        klineCollapsiblePane = new JXCollapsiblePane();
        klineCollapsiblePane.setLayout(new BorderLayout());
        initKlineDisplayPanel();
        klineCollapsiblePane.add("Center", klineDisplayContainerPanel);
        klineCollapsiblePane.setAnimated(true);
        klineCollapsiblePane.setCollapsed(false); // 默认展开

        panelTemp.add(klineCollapsiblePane, BorderLayout.SOUTH); // 可折叠展示k线, 放在南边
        initButtons();
        panelTemp.add(buttonContainer, BorderLayout.NORTH);
        panelTemp.add(jScrollPane, BorderLayout.CENTER);
        this.add(panelTemp, BorderLayout.CENTER);

        editorPanel = new IndustryConceptThsOfPlanEditorPanel(this);
        JPanel panel = new JPanel();
        panel.add(editorPanel.getEditorContainerScrollPane()); // 不直接编辑器, 而容器滚动面板
        this.add(panel, BorderLayout.WEST); // 需要包装一下, 否则 editorPanel将被拉长
    }

    ThsFsDisplayPanel fsDisplayPanel; // 分时图
    ThsKLineDisplayPanel dailyKLineDisplayPanel; // 日k线
    ThsKLineDisplayPanel weeklyKLineDisplayPanel; // 周k线
    ThsKLineDisplayPanel monthlyKLineDisplayPanel; // 月k线

    private void initKlineDisplayPanel() {
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
                Set<IndustryConceptSimple> existsSimpleBeans =
                        existsBeanList.stream()
                                .map(value -> new IndustryConceptSimple(value.getName(), value.getType()))
                                .collect(Collectors.toSet()); // 整个过程逻辑上不再变化

                // 1.读取概念列表, 二级/三级行业列表, 默认按照涨跌幅排序,仅显示名称. 要看行情去同花顺看去,我不管
                List<String> conceptNameList = ThsDbApi
                        .getConceptNameList(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> concepts = conceptNameList.stream()
                        .map(value -> new IndustryConceptSimple(value, "概念")).collect(Collectors.toList());
                List<String> industryNameListLevel2 = ThsDbApi
                        .getIndustryNameListLevel2(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> industryLevel2s = industryNameListLevel2.stream()
                        .map(value -> new IndustryConceptSimple(value, "行业")).collect(Collectors.toList());
                List<String> industryNameListLevel3 = ThsDbApi
                        .getIndustryNameListLevel3(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> industryLevel3s = industryNameListLevel3.stream()
                        .map(value -> new IndustryConceptSimple(value, "行业")).collect(Collectors.toList());

                // 3.对话框显示, tab面板, 3tab, 概念,23级行业; 添加时自行判定是否已经存在于今日bean和右方列表
                // 按钮"添加所有昨日" 则自动读取昨日bean, 全部添加
                JDialog dialog = new JDialog(TraderGui.INSTANCE, "添加新行业/概念");
                // 按下esc关闭对话框, 实测不能modal模式, 否则监听无效
                KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                dialog.getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

                //创建JDialog
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                dialog.setContentPane(panel);

                JPanel addBeansPanel = initAddBeansPanel(concepts, industryLevel2s, industryLevel3s,
                        existsSimpleBeans, dialog);

                //添加控件到对话框
                JScrollPane jScrollPane = new JScrollPane();
                jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
                BasicScrollBarUIS
                        .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
                jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
                jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                jScrollPane.setViewportView(addBeansPanel);
                panel.add(addBeansPanel, BorderLayout.CENTER);

                //显示对话框（setVisible()方法会阻塞，直到对话框关闭）
                dialog.setSize(1000, 800);
//                    dialog.setLocation();
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });

        // 4.批量删除table所选bean, 已经支持排序
        JButton deleteTableSelectRowsButton = ButtonFactory.getButton("删除所选");
        deleteTableSelectRowsButton.setMaximumSize(new Dimension(60, 16));
        deleteTableSelectRowsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jTable == null) {
                    return;
                }

                ArrayList<IndustryConceptThsOfPlan> beansBeDelete = new ArrayList<>();
                int[] selectedRows = jTable.getSelectedRows();
                DefaultTableModel model = (DefaultTableModel) jTable.getModel();
                RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
                for (int selectedRow : selectedRows) {
                    // @key3: 将视图的被选中的index, 转换为 model中的; 再从model中读取数据!
                    selectedRow = rowSorter.convertRowIndexToModel(selectedRow);
                    Object valueAt = model.getValueAt(selectedRow, 0);// id在第一列
                    if (valueAt == null) {
                        continue;
                    }
                    Long id;
                    try {
                        id = Long.valueOf(valueAt.toString());
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    IndustryConceptThsOfPlan bean = panelForPlan.beanMap.get(id);
                    if (bean != null) {
                        beansBeDelete.add(bean);
                    }
                }
                List<String> beanNameList = beansBeDelete.stream().map(IndustryConceptThsOfPlan::getName)
                        .collect(Collectors.toList());

                int opt = JOptionPane.showConfirmDialog(TraderGui.INSTANCE,
                        GuiCommonUtil.buildDialogShowStr("将删除以下行业/概念,是否确定?", beanNameList.toString()),
                        "即将删除",
                        JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    //确认继续操作
                    IndustryConceptThsOfPlanDao.deleteBeanBatch(beansBeDelete);
                    panelForPlan.editorPanel.update();
                    panelForPlan.update();
                    ManiLog.put(StrUtil.format("已删除: {}",
                            beanNameList));
                }


            }
        });

        // 5.手动 自动刷新 关联trend Map, 和关联trend
        JButton flushRelatedTrendMapButton = ButtonFactory.getButton("刷新关联trend");
        flushRelatedTrendMapButton.setMaximumSize(new Dimension(60, 16));
        flushRelatedTrendMapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndustryConceptThsOfPlanEditorPanel.flushWithRelatedTrends(panelForPlan.editorPanel);
                panelForPlan.editorPanel.update(); // 将更新显示自动设置字段
                panelForPlan.update(); // 更新列表
            }
        });

        // 6.多主线加身 选股, 控制可折叠面板.
        JButton multiConceptSelectorButton = ButtonFactory.getButton("多主线选股");
        multiConceptSelectorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (beanMap == null) {
                    return;
                }

                JDialog dialog = new JDialog(TraderGui.INSTANCE, "多线加身结果", false);
                // 按下esc关闭对话框, 实测不能modal模式, 否则监听无效
                KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                dialog.getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

                //创建JDialog
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                dialog.setContentPane(panel);

                JLabel jLabel = new JLabel();
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> lineTypeMap = new HashMap<>();
                        for (IndustryConceptThsOfPlan bean0 : beanMap.values()) {
                            lineTypeMap.put(bean0.getName(), bean0.getLineType());
                        }

                        MultiConceptSelector multiConceptSelector = new MultiConceptSelector(lineTypeMap);
                        multiConceptSelector.stockSelect();
                        jLabel.setText(GuiCommonUtil.jsonStrToHtmlFormat(multiConceptSelector.resToJson()));
                    }
                }, true);


                jLabel.setForeground(Color.orange);
                jLabel.setBackground(COLOR_THEME_MINOR);
                //添加控件到对话框
                JScrollPane jScrollPane = new JScrollPane();
                jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
                BasicScrollBarUIS
                        .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
                jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
                jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                jScrollPane.setViewportView(jLabel);
                panel.add(jScrollPane, BorderLayout.CENTER);

                //显示对话框（setVisible()方法会阻塞，直到对话框关闭）
                dialog.setSize(500, 800);
//                    dialog.setLocation();
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });


        // 7.折叠/展示k线, 控制可折叠面板.
        JButton buttonOfKLineDisplay = ButtonFactory.getButton("分时k线");
        buttonOfKLineDisplay.setAction(klineCollapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        buttonOfKLineDisplay.setText("分时k线");
        buttonOfKLineDisplay.setForeground(Color.red);


        // 100.测试按钮
        JButton testButton = ButtonFactory.getButton("测试");
        testButton.setMaximumSize(new Dimension(60, 16));
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JXPanel jxPanel = new JXPanel();
                jxPanel.setLayout(new BorderLayout());

                JXDialog jxDialog = new JXDialog(jxPanel);

                DefaultListModel model = new DefaultListModel();
                model.addAll(Arrays.asList("三胎概念", "电力", "电力2"));
                JXList jxList = new JXList(model);
                JXFindBarS findbar = new JXFindBarS(jxList.getSearchable());

                jxPanel.add(jxList, BorderLayout.CENTER);
                jxPanel.add(findbar, BorderLayout.NORTH);


                jxDialog.setSize(1000, 800);
                jxDialog.setVisible(true);
            }
        });

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.setBorder(null);
        buttonContainer.add(buttonFlushAll);
        buttonContainer.add(saveEditingBeanButton);
        buttonContainer.add(addBeansBatchButton);
        buttonContainer.add(deleteTableSelectRowsButton);
        buttonContainer.add(flushRelatedTrendMapButton);
        buttonContainer.add(multiConceptSelectorButton);
        buttonContainer.add(buttonOfKLineDisplay);


        buttonContainer.add(testButton);
    }

    /**
     * 新增bean时, 默认将 读取曾存在的同名最后一个bean, 将非特殊字段, 设置为新bean的字段!
     *
     * @param concepts
     * @param industryLevel2s
     * @param industryLevel3s
     * @return
     */
    private JPanel initAddBeansPanel(List<IndustryConceptSimple> concepts,
                                     List<IndustryConceptSimple> industryLevel2s,
                                     List<IndustryConceptSimple> industryLevel3s,
                                     Set<IndustryConceptSimple> existsSimpleBeans, // 今日已存在的bean, 将不被添加
                                     JDialog parentDialog
    ) {

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());

        // 1.左侧tabpane
        JTabbedPane jTabbedPaneLeft = new JTabbedPane();
        jTabbedPaneLeft.setPreferredSize(new Dimension(440, 775));

        // 1.1. 概念列表
        DefaultListModel model1 = new DefaultListModel();
        model1.addAll(concepts);
        JXList jxListOfConceptList = new JXList(model1);
        jxListOfConceptList.setForeground(Color.orange);
        jxListOfConceptList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane1, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.setViewportView(jxListOfConceptList);
//        jScrollPane1.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfConceptList.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.2. 二级行业列表: 逻辑一模一样
        DefaultListModel model2 = new DefaultListModel();
        model2.addAll(industryLevel2s);
        JXList jxListOfIndustryListLevel2 = new JXList(model2);
        jxListOfIndustryListLevel2.setForeground(Color.orange);
        jxListOfIndustryListLevel2.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane2 = new JScrollPane();
        jScrollPane2.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane2, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPane2.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane2.setViewportView(jxListOfIndustryListLevel2);
//        jScrollPane2.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfIndustryListLevel2.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.3. 三级行业
        // 1.2. 二级行业列表: 逻辑一模一样
        DefaultListModel model3 = new DefaultListModel();
        model3.addAll(industryLevel3s);
        JXList jxListOfIndustryListLevel3 = new JXList(model3);
        jxListOfIndustryListLevel3.setForeground(Color.orange);
        jxListOfIndustryListLevel3.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane3 = new JScrollPane();
        jScrollPane3.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane3, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPane3.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane3.setViewportView(jxListOfIndustryListLevel3);
//        jScrollPane3.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfIndustryListLevel3.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.4. 添加3个tab
        jTabbedPaneLeft.addTab("所有概念", jScrollPane1);
        jTabbedPaneLeft.addTab("二级行业", jScrollPane2);
        jTabbedPaneLeft.addTab("三级行业", jScrollPane3);


        // 2.两个按钮, 添加 和 删除按钮
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new VerticalFlowLayout()); // 居中对齐
        JButton addButton = ButtonFactory.getButton("添加->");
        JButton deleteButton = ButtonFactory.getButton("<-删除");
        JButton addYesterdayAll = ButtonFactory.getButton("添加昨日所有");
        JButton saveButton = ButtonFactory.getButton("确定新增");
        saveButton.setBackground(Color.red);
        saveButton.setForeground(Color.black);
        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(addYesterdayAll);
        buttonsPanel.add(saveButton);
        buttonsPanel.setSize(new Dimension(60, 800));

        // 3.右列表: 本次实际 将增加的bean列表, IndustryConceptSimple 表示
        DefaultListModel model4 = new DefaultListModel(); // 默认空
        JXList newBeanList = new JXList(model4);
        newBeanList.setForeground(Color.orange);
        newBeanList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPaneRight = new JScrollPane();
        jScrollPaneRight.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneRight, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPaneRight.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPaneRight.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneRight.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneRight.setViewportView(newBeanList);
        jScrollPaneRight.setPreferredSize(new Dimension(440, 800));
        try {
            newBeanList.setSelectedIndex(0);
        } catch (Exception e) {
        }

        /*

        // 6.
        JButton testButton = ButtonFactory.getButton("测试");
        testButton.setMaximumSize(new Dimension(60, 16));
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JXPanel jxPanel = new JXPanel();
                jxPanel.setLayout(new BorderLayout());

                JXDialog jxDialog = new JXDialog(jxPanel);

                DefaultListModel model = new DefaultListModel();
                model.addAll(Arrays.asList("三胎概念", "电力", "电力2"));
                JXList jxList = new JXList(model);
                JXFindBarS findbar = new JXFindBarS(jxList.getSearchable());

                jxPanel.add(jxList, BorderLayout.CENTER);
                jxPanel.add(findbar, BorderLayout.NORTH);


                jxDialog.setSize(1000,800);
                jxDialog.setVisible(true);
            }
        });
         */

        // @update: 将原来的 jTabbedPaneLeft放在左边, 在上方添加一个查找框
        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        JXFindBarS jxFindBarS = new JXFindBarS(jxListOfConceptList.getSearchable());
        jPanel1.add(jxFindBarS, BorderLayout.NORTH);
        jPanel1.add(jTabbedPaneLeft, BorderLayout.CENTER);
        jPanel1.setPreferredSize(new Dimension(440, 800));
        jTabbedPaneLeft.addChangeListener(new ChangeListener() { // 切换tab, 必须切换查找框的 查找对象
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = jTabbedPaneLeft.getSelectedIndex();
                if (selectedIndex == 0) {
                    jxFindBarS.setSearchable(jxListOfConceptList.getSearchable());
                } else if (selectedIndex == 1) {
                    jxFindBarS.setSearchable(jxListOfIndustryListLevel2.getSearchable());
                } else if (selectedIndex == 2) {
                    jxFindBarS.setSearchable(jxListOfIndustryListLevel3.getSearchable());
                }
            }
        });

        // 4.添加3大部分
        jPanel.add(jPanel1, BorderLayout.WEST);
        jPanel.add(buttonsPanel, BorderLayout.CENTER);
        jPanel.add(jScrollPaneRight, BorderLayout.EAST);


        // 5.各按钮功能实现
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1.当前左侧list
                int selectedIndex = jTabbedPaneLeft.getSelectedIndex();
                JXList currentJXList;
                if (selectedIndex == 0) {
                    currentJXList = jxListOfConceptList;
                } else if (selectedIndex == 1) {
                    currentJXList = jxListOfIndustryListLevel2;
                } else if (selectedIndex == 2) {
                    currentJXList = jxListOfIndustryListLevel3;
                } else {
                    return;
                }

                // 2.得到当前选择bean
                Object[] selectedValues0 = currentJXList.getSelectedValues();

                IndustryConceptSimple[] selectedValues = new IndustryConceptSimple[selectedValues0.length];
                for (int i = 0; i < selectedValues0.length; i++) {
                    selectedValues[i] = (IndustryConceptSimple) (selectedValues0[i]);
                }


                // 判定当前选择对象, 是否已存在今日已存在bean集合, 或者已存在右边列表
                // 3.是否已存在今日bean
                // 4.是否在右侧已选择
                DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                HashSet<IndustryConceptSimple> beansFromRightList = new HashSet<>();
                for (int i = 0; i < model.getSize(); i++) {
                    beansFromRightList.add((IndustryConceptSimple) model.getElementAt(i));
                }
                // 5.添加逻辑
                for (IndustryConceptSimple selectedValue : selectedValues) {
                    if (existsSimpleBeans.contains(selectedValue)) {
                        ManiLog.put(StrUtil.format("所选bean已生成,不可再次生成: {}", selectedValue.toString()));
                        continue;
                    }
                    if (beansFromRightList.contains(selectedValue)) {
                        ManiLog.put(StrUtil.format("所选bean已被选择,不可重复添加: {}", selectedValue.toString()));
                        continue;
                    }
                    model.addElement(selectedValue);
                }

            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = newBeanList.getSelectedIndex();
                if (selectedIndex < 0) { // -1
                    return;
                }
                DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                int rawSize = model.getSize();
                model.remove(selectedIndex);

                int shouldIndex = selectedIndex;
                if (selectedIndex == rawSize - 1) {
                    shouldIndex--;
                }
                try {
                    newBeanList.setSelectedIndex(shouldIndex); // 选择索引不变
                } catch (Exception e1) {
                }
            }
        });

        addYesterdayAll.addActionListener(new ActionListener() {
            // 添加所有昨日的bean; 将从数据库, 读取上一交易日,已存在所有bean, 读取name和type, 实例化简单bean, 添加到右侧列表
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                String datePlan = IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                        PlanReviewDateTimeDecider.getUniqueDatetime()
                );
                EastMoneyDbApi.getPreNTradeDateStrict(datePlan, 1);
                List<IndustryConceptThsOfPlan> beansByDate = IndustryConceptThsOfPlanDao
                        .getBeansByDate(EastMoneyDbApi.getPreNTradeDateStrict(datePlan, 1)); // 上一计划交易日
                List<IndustryConceptSimple> collect = beansByDate.stream()
                        .map(value -> new IndustryConceptSimple(value.getName(), value.getType())).collect(
                                Collectors.toList());

                DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                HashSet<IndustryConceptSimple> beansFromRightList = new HashSet<>();
                for (int i = 0; i < model.getSize(); i++) {
                    beansFromRightList.add((IndustryConceptSimple) model.getElementAt(i));
                }

                // 添加全部, 但对每一个依然需要检测是否已经存在
                for (IndustryConceptSimple industryConceptSimple : collect) {
                    if (existsSimpleBeans.contains(industryConceptSimple) || beansFromRightList
                            .contains(industryConceptSimple)) {
                        continue; // 不提示
                    }
                    model.addElement(industryConceptSimple);
                }
            }
        });
        IndustryConceptPanelForPlan panelForPlan = this;
        saveButton.addActionListener(new ActionListener() {
            // 保存右侧列表,
            // 将自动按照最后曾存在bean,初始化某些字段,见下; 因耗时所以显示进度条. 完成后关闭对话框.

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog(parentDialog, "生成中", true);
                JPanel jPanel1 = new JPanel();
                jPanel1.setLayout(new BorderLayout());
                JProgressBar jProgressBar = new JProgressBar();
                jProgressBar.setMaximum(100);
                jProgressBar.setMinimum(0);
                jProgressBar.setValue(0);
                jProgressBar.setStringPainted(true);

                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                        int total = model.getSize(); // 进度和遍历

                        Date uniqueDatetime = PlanReviewDateTimeDecider.getUniqueDatetime();
                        String stdDateStr = IndustryConceptThsOfPlanDao.decideDateStrForPlan(uniqueDatetime);
                        // 前10个交易日内的同名bean, 设置某些属性; 若获取全部则随着时间, 太多了太慢
                        for (int i = 0; i < total; i++) {
                            IndustryConceptSimple element = (IndustryConceptSimple) model.getElementAt(i);
                            IndustryConceptThsOfPlan.Type type;
                            if ("行业".equals(element.getType())) {
                                type = IndustryConceptThsOfPlan.Type.INDUSTRY;
                            } else { // "概念".equals(element.getType())
                                type = IndustryConceptThsOfPlan.Type.CONCEPT;
                            }
                            IndustryConceptThsOfPlan newBean = IndustryConceptThsOfPlanDao
                                    .getOrInitBeanForPlan(element.getName(), uniqueDatetime, type); // 已初步保存新bean
                            try {
                                IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(newBean, stdDateStr, 10);
                            } catch (SQLException ex) {
                                ManiLog.put(StrUtil.format("同步可编辑属性失败: {}", newBean.getName()));
                            }
                            int n = (int) CommonUtil.roundHalfUP((i + 1) * 1.0 / total * 100, 0);
                            jProgressBar.setValue(n); // 进度显示
                            // 读取历史上倒数1
                        }
                        panelForPlan.update();
                        panelForPlan.editorPanel.update();
                        ManiLog.put("批量添加完成!");
                        dialog.dispose();
                        parentDialog.dispose();
                    }
                }, true);


                jPanel1.add(jProgressBar, BorderLayout.CENTER);
                dialog.setContentPane(jPanel1);
                dialog.setSize(500, 70);
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);// 进度条对话框显示


            }
        });


        return jPanel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IndustryConceptSimple {
        String name;
        String type; // 行业概念

        @Override
        public String toString() {
            return name + " -- " + type;
        }
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
            jScrollPane.setViewportView(jTable); // 默认显式"数据获取中", 第一次刷新
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
