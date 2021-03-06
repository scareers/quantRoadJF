package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.ths.wencai.WenCaiDataApi;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.ThsFsDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.ThsKLineDisplayPanel;
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
 * description: ??????????????? ????????????: ??????????????????????????????
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
    protected IndustryConceptThsOfPlanEditorPanel editorPanel; // ?????????
    protected IndustryConceptThsOfPlan currentBean;

    protected JXTable jTable;
    protected JScrollPane jScrollPane;

    protected JPanel buttonContainer; // ??????????????????
    protected JButton buttonFlushAll; // ??????????????????

    protected JXCollapsiblePane klineCollapsiblePane;
    protected JPanel klineDisplayContainerPanel;

    public IndustryConceptPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        JLabel label = new JLabel("???????????????"); // ??????????????????
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // ??????
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS); // ???????????????
        jScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS); // ???????????????


        // ????????????, ???????????????????????????
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());

        // @update: ??????????????????????????????, ?????? ???????????? ????????????/k??????! --> ths 4???
        klineCollapsiblePane = new JXCollapsiblePane();
        klineCollapsiblePane.setLayout(new BorderLayout());
        initKlineDisplayPanel();
        klineCollapsiblePane.add("Center", klineDisplayContainerPanel);
        klineCollapsiblePane.setAnimated(true);
        klineCollapsiblePane.setCollapsed(false); // ????????????

        panelTemp.add(klineCollapsiblePane, BorderLayout.SOUTH); // ???????????????k???, ????????????
        initButtons();
        panelTemp.add(buttonContainer, BorderLayout.NORTH);
        panelTemp.add(jScrollPane, BorderLayout.CENTER);
        this.add(panelTemp, BorderLayout.CENTER);

        editorPanel = new IndustryConceptThsOfPlanEditorPanel(this);
        JPanel panel = new JPanel();
        panel.add(editorPanel.getEditorContainerScrollPane()); // ??????????????????, ?????????????????????
        this.add(panel, BorderLayout.WEST); // ??????????????????, ?????? editorPanel????????????
    }

    ThsFsDisplayPanel fsDisplayPanel; // ?????????
    ThsKLineDisplayPanel dailyKLineDisplayPanel; // ???k???
    ThsKLineDisplayPanel weeklyKLineDisplayPanel; // ???k???
    ThsKLineDisplayPanel monthlyKLineDisplayPanel; // ???k???

    private void initKlineDisplayPanel() {
        klineDisplayContainerPanel = new JPanel();
        klineDisplayContainerPanel.setLayout(new GridLayout(1, 4, -1, -1)); // 4??? k???
        // 4???k???
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
     * ???????????????, ??????????????????????????????k???????????????. ??? jTable?????????
     */
    private void updateKLineAndFsDisplay() {
        if (this.currentBean == null) {
            return;
        }
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                poolOfFsAndKlineUpdate.submit(() -> updateFsDisplay());
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(0, dailyKLineDisplayPanel, "-???K", "?????????k?????????: {}"));
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(1, weeklyKLineDisplayPanel, "-???K", "?????????k?????????: {}"));
                poolOfFsAndKlineUpdate.submit(() -> updateKLine(2, monthlyKLineDisplayPanel, "-???K", "?????????k?????????: {}"));
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
                preClose = Double.valueOf(last5DailyKLine.get(last5DailyKLine.length() - 2, "??????").toString());
            } catch (NumberFormatException e) {
                preClose = Double.valueOf(fs1M.get(0, "??????").toString());
                ManiLog.put(StrUtil.format("???????????????????????????,????????????????????????close??????: {}", currentBean.getName()));
            }
            fsDisplayPanel.update(fs1M, currentBean.getName() + "-??????", preClose);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            ManiLog.put(StrUtil.format("?????????????????????: {}", currentBean.getName()));
        }
    }

    public static final int preferKLinePeriods = 60; // k??? ????????????

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
        // 1.??????????????????
        buttonFlushAll = ButtonFactory.getButton("????????????");
        buttonFlushAll.setMaximumSize(new Dimension(60, 16));
        IndustryConceptPanelForPlan panelForPlan = this;
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelForPlan.update(); // ????????? ?????????????????? df??????,
            }
        });

        // 2.?????????????????????bean??????
        JButton saveEditingBeanButton = ButtonFactory.getButton("????????????");
        saveEditingBeanButton.setMaximumSize(new Dimension(60, 16));
        saveEditingBeanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndustryConceptThsOfPlanEditorPanel.tryAutoSaveEditedBean(panelForPlan.editorPanel, "????????????");
            }
        });

        // 3.????????????: ??????????????????, ????????????bean; ??????: ????????????, ?????? ???????????????, ???????????????
        // @noti: ??????????????????, ???????????? "????????????????????????", ??????????????? ?????????????????????????????????????????????/??????bean ???????????????
        // ??????????????????????????????????????????; ????????????????????????
        // ???????????????????????????????????????: ????????????4??????
        // @noti: ?????????????????????trend
        JButton addBeansBatchButton = ButtonFactory.getButton("????????????");
        addBeansBatchButton.setMaximumSize(new Dimension(60, 16));
        addBeansBatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ???????????????, ????????????, ???????????? bean ??????????????????; ???????????? update() ????????????


                // 0.??????????????????bean, ?????????????????????????????????????????????bean, ?????????
                List<IndustryConceptThsOfPlan> existsBeanList = IndustryConceptThsOfPlanDao
                        .getBeanListForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
                Set<IndustryConceptSimple> existsSimpleBeans =
                        existsBeanList.stream()
                                .map(value -> new IndustryConceptSimple(value.getName(), value.getType()))
                                .collect(Collectors.toSet()); // ?????????????????????????????????

                // 1.??????????????????, ??????/??????????????????, ???????????????????????????,???????????????. ??????????????????????????????,?????????
                List<String> conceptNameList = ThsDbApi
                        .getConceptNameList(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> concepts = conceptNameList.stream()
                        .map(value -> new IndustryConceptSimple(value, "??????")).collect(Collectors.toList());
                List<String> industryNameListLevel2 = ThsDbApi
                        .getIndustryNameListLevel2(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> industryLevel2s = industryNameListLevel2.stream()
                        .map(value -> new IndustryConceptSimple(value, "??????")).collect(Collectors.toList());
                List<String> industryNameListLevel3 = ThsDbApi
                        .getIndustryNameListLevel3(IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                                PlanReviewDateTimeDecider.getUniqueDatetime()
                        ));
                List<IndustryConceptSimple> industryLevel3s = industryNameListLevel3.stream()
                        .map(value -> new IndustryConceptSimple(value, "??????")).collect(Collectors.toList());

                // 3.???????????????, tab??????, 3tab, ??????,23?????????; ????????????????????????????????????????????????bean???????????????
                // ??????"??????????????????" ?????????????????????bean, ????????????
                JDialog dialog = new JDialog(TraderGui.INSTANCE, "???????????????/??????");
                // ??????esc???????????????, ????????????modal??????, ??????????????????
                KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
                dialog.getRootPane().registerKeyboardAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

                //??????JDialog
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                dialog.setContentPane(panel);

                JPanel addBeansPanel = initAddBeansPanel(concepts, industryLevel2s, industryLevel3s,
                        existsSimpleBeans, dialog);

                //????????????????????????
                JScrollPane jScrollPane = new JScrollPane();
                jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
                BasicScrollBarUIS
                        .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
                jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // ????????????
                jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                jScrollPane.setViewportView(addBeansPanel);
                panel.add(addBeansPanel, BorderLayout.CENTER);

                //??????????????????setVisible()??????????????????????????????????????????
                dialog.setSize(1000, 800);
//                    dialog.setLocation();
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });

        // 4.????????????table??????bean, ??????????????????
        JButton deleteTableSelectRowsButton = ButtonFactory.getButton("????????????");
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
                    // @key3: ????????????????????????index, ????????? model??????; ??????model???????????????!
                    selectedRow = rowSorter.convertRowIndexToModel(selectedRow);
                    Object valueAt = model.getValueAt(selectedRow, 0);// id????????????
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
                        GuiCommonUtil.buildDialogShowStr("?????????????????????/??????,?????????????", beanNameList.toString()),
                        "????????????",
                        JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    //??????????????????
                    IndustryConceptThsOfPlanDao.deleteBeanBatch(beansBeDelete);
                    panelForPlan.editorPanel.update();
                    panelForPlan.update();
                    ManiLog.put(StrUtil.format("?????????: {}",
                            beanNameList));
                }


            }
        });

        // 5.?????? ???????????? ??????trend Map, ?????????trend
        JButton flushRelatedTrendMapButton = ButtonFactory.getButton("????????????trend");
        flushRelatedTrendMapButton.setMaximumSize(new Dimension(60, 16));
        flushRelatedTrendMapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndustryConceptThsOfPlanEditorPanel.flushWithRelatedTrends(panelForPlan.editorPanel);
                panelForPlan.editorPanel.update(); // ?????????????????????????????????
                panelForPlan.update(); // ????????????
            }
        });

        // 6.??????????????? ??????, ?????????????????????.
        JButton multiConceptSelectorButton = ButtonFactory.getButton("???????????????");
        multiConceptSelectorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (beanMap == null) {
                    return;
                }

                JDialog dialog = new JDialog(TraderGui.INSTANCE, "??????????????????", false);
                // ??????esc???????????????, ????????????modal??????, ??????????????????
//                KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
//                dialog.getRootPane().registerKeyboardAction(new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        dialog.dispose();
//                    }
//                }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

                GuiCommonUtil.addEscDisposeCallbackToJDialog(dialog);

                //??????JDialog
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
                //????????????????????????
                JScrollPane jScrollPane = new JScrollPane();
                jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
                BasicScrollBarUIS
                        .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
                jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // ????????????
                jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                jScrollPane.setViewportView(jLabel);
                panel.add(jScrollPane, BorderLayout.CENTER);

                //??????????????????setVisible()??????????????????????????????????????????
                dialog.setSize(500, 800);
//                    dialog.setLocation();
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);

            }
        });


        // 7.??????/??????k???, ?????????????????????.
        JButton buttonOfKLineDisplay = ButtonFactory.getButton("??????k???");
        buttonOfKLineDisplay.setAction(klineCollapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        buttonOfKLineDisplay.setText("??????k???");
        buttonOfKLineDisplay.setForeground(Color.red);


        // 100.????????????
        JButton testButton = ButtonFactory.getButton("??????");
        testButton.setMaximumSize(new Dimension(60, 16));
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JXPanel jxPanel = new JXPanel();
                jxPanel.setLayout(new BorderLayout());

                JXDialog jxDialog = new JXDialog(jxPanel);

                DefaultListModel model = new DefaultListModel();
                model.addAll(Arrays.asList("????????????", "??????", "??????2"));
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
     * ??????bean???, ????????? ????????????????????????????????????bean, ??????????????????, ????????????bean?????????!
     *
     * @param concepts
     * @param industryLevel2s
     * @param industryLevel3s
     * @return
     */
    private JPanel initAddBeansPanel(List<IndustryConceptSimple> concepts,
                                     List<IndustryConceptSimple> industryLevel2s,
                                     List<IndustryConceptSimple> industryLevel3s,
                                     Set<IndustryConceptSimple> existsSimpleBeans, // ??????????????????bean, ???????????????
                                     JDialog parentDialog
    ) {

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());

        // 1.??????tabpane
        JTabbedPane jTabbedPaneLeft = new JTabbedPane();
        jTabbedPaneLeft.setPreferredSize(new Dimension(440, 775));

        // 1.1. ????????????
        DefaultListModel model1 = new DefaultListModel();
        model1.addAll(concepts);
        JXList jxListOfConceptList = new JXList(model1);
        jxListOfConceptList.setForeground(Color.orange);
        jxListOfConceptList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane1, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(25); // ????????????
        jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane1.setViewportView(jxListOfConceptList);
//        jScrollPane1.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfConceptList.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.2. ??????????????????: ??????????????????
        DefaultListModel model2 = new DefaultListModel();
        model2.addAll(industryLevel2s);
        JXList jxListOfIndustryListLevel2 = new JXList(model2);
        jxListOfIndustryListLevel2.setForeground(Color.orange);
        jxListOfIndustryListLevel2.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane2 = new JScrollPane();
        jScrollPane2.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane2, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPane2.getVerticalScrollBar().setUnitIncrement(25); // ????????????
        jScrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane2.setViewportView(jxListOfIndustryListLevel2);
//        jScrollPane2.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfIndustryListLevel2.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.3. ????????????
        // 1.2. ??????????????????: ??????????????????
        DefaultListModel model3 = new DefaultListModel();
        model3.addAll(industryLevel3s);
        JXList jxListOfIndustryListLevel3 = new JXList(model3);
        jxListOfIndustryListLevel3.setForeground(Color.orange);
        jxListOfIndustryListLevel3.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPane3 = new JScrollPane();
        jScrollPane3.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane3, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPane3.getVerticalScrollBar().setUnitIncrement(25); // ????????????
        jScrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane3.setViewportView(jxListOfIndustryListLevel3);
//        jScrollPane3.setPreferredSize(new Dimension(440, 800));
        try {
            jxListOfIndustryListLevel3.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 1.4. ??????3???tab
        jTabbedPaneLeft.addTab("????????????", jScrollPane1);
        jTabbedPaneLeft.addTab("????????????", jScrollPane2);
        jTabbedPaneLeft.addTab("????????????", jScrollPane3);


        // 2.????????????, ?????? ??? ????????????
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new VerticalFlowLayout()); // ????????????
        JButton addButton = ButtonFactory.getButton("??????->");
        JButton deleteButton = ButtonFactory.getButton("<-??????");
        JButton addYesterdayAll = ButtonFactory.getButton("??????????????????");
        JButton saveButton = ButtonFactory.getButton("????????????");
        saveButton.setBackground(Color.red);
        saveButton.setForeground(Color.black);
        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(addYesterdayAll);
        buttonsPanel.add(saveButton);
        buttonsPanel.setSize(new Dimension(60, 800));

        // 3.?????????: ???????????? ????????????bean??????, IndustryConceptSimple ??????
        DefaultListModel model4 = new DefaultListModel(); // ?????????
        JXList newBeanList = new JXList(model4);
        newBeanList.setForeground(Color.orange);
        newBeanList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPaneRight = new JScrollPane();
        jScrollPaneRight.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneRight, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // ??????????????? barUi
        jScrollPaneRight.getVerticalScrollBar().setUnitIncrement(25); // ????????????
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
        JButton testButton = ButtonFactory.getButton("??????");
        testButton.setMaximumSize(new Dimension(60, 16));
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JXPanel jxPanel = new JXPanel();
                jxPanel.setLayout(new BorderLayout());

                JXDialog jxDialog = new JXDialog(jxPanel);

                DefaultListModel model = new DefaultListModel();
                model.addAll(Arrays.asList("????????????", "??????", "??????2"));
                JXList jxList = new JXList(model);
                JXFindBarS findbar = new JXFindBarS(jxList.getSearchable());

                jxPanel.add(jxList, BorderLayout.CENTER);
                jxPanel.add(findbar, BorderLayout.NORTH);


                jxDialog.setSize(1000,800);
                jxDialog.setVisible(true);
            }
        });
         */

        // @update: ???????????? jTabbedPaneLeft????????????, ??????????????????????????????
        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        JXFindBarS jxFindBarS = new JXFindBarS(jxListOfConceptList.getSearchable());
        jPanel1.add(jxFindBarS, BorderLayout.NORTH);
        jPanel1.add(jTabbedPaneLeft, BorderLayout.CENTER);
        jPanel1.setPreferredSize(new Dimension(440, 800));
        jTabbedPaneLeft.addChangeListener(new ChangeListener() { // ??????tab, ???????????????????????? ????????????
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

        // 4.??????3?????????
        jPanel.add(jPanel1, BorderLayout.WEST);
        jPanel.add(buttonsPanel, BorderLayout.CENTER);
        jPanel.add(jScrollPaneRight, BorderLayout.EAST);


        // 5.?????????????????????
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1.????????????list
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

                // 2.??????????????????bean
                Object[] selectedValues0 = currentJXList.getSelectedValues();

                IndustryConceptSimple[] selectedValues = new IndustryConceptSimple[selectedValues0.length];
                for (int i = 0; i < selectedValues0.length; i++) {
                    selectedValues[i] = (IndustryConceptSimple) (selectedValues0[i]);
                }


                // ????????????????????????, ??????????????????????????????bean??????, ???????????????????????????
                // 3.?????????????????????bean
                // 4.????????????????????????
                DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                HashSet<IndustryConceptSimple> beansFromRightList = new HashSet<>();
                for (int i = 0; i < model.getSize(); i++) {
                    beansFromRightList.add((IndustryConceptSimple) model.getElementAt(i));
                }
                // 5.????????????
                for (IndustryConceptSimple selectedValue : selectedValues) {
                    if (existsSimpleBeans.contains(selectedValue)) {
                        ManiLog.put(StrUtil.format("??????bean?????????,??????????????????: {}", selectedValue.toString()));
                        continue;
                    }
                    if (beansFromRightList.contains(selectedValue)) {
                        ManiLog.put(StrUtil.format("??????bean????????????,??????????????????: {}", selectedValue.toString()));
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
                    newBeanList.setSelectedIndex(shouldIndex); // ??????????????????
                } catch (Exception e1) {
                }
            }
        });

        addYesterdayAll.addActionListener(new ActionListener() {
            // ?????????????????????bean; ???????????????, ?????????????????????,???????????????bean, ??????name???type, ???????????????bean, ?????????????????????
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                String datePlan = IndustryConceptThsOfPlanDao.decideDateStrForPlan(
                        PlanReviewDateTimeDecider.getUniqueDatetime()
                );
                EastMoneyDbApi.getPreNTradeDateStrict(datePlan, 1);
                List<IndustryConceptThsOfPlan> beansByDate = IndustryConceptThsOfPlanDao
                        .getBeansByDate(EastMoneyDbApi.getPreNTradeDateStrict(datePlan, 1)); // ?????????????????????
                List<IndustryConceptSimple> collect = beansByDate.stream()
                        .map(value -> new IndustryConceptSimple(value.getName(), value.getType())).collect(
                                Collectors.toList());

                DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                HashSet<IndustryConceptSimple> beansFromRightList = new HashSet<>();
                for (int i = 0; i < model.getSize(); i++) {
                    beansFromRightList.add((IndustryConceptSimple) model.getElementAt(i));
                }

                // ????????????, ???????????????????????????????????????????????????
                for (IndustryConceptSimple industryConceptSimple : collect) {
                    if (existsSimpleBeans.contains(industryConceptSimple) || beansFromRightList
                            .contains(industryConceptSimple)) {
                        continue; // ?????????
                    }
                    model.addElement(industryConceptSimple);
                }
            }
        });
        IndustryConceptPanelForPlan panelForPlan = this;
        saveButton.addActionListener(new ActionListener() {
            // ??????????????????,
            // ??????????????????????????????bean,?????????????????????,??????; ??????????????????????????????. ????????????????????????.

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new JDialog(parentDialog, "?????????", true);
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
                        int total = model.getSize(); // ???????????????

                        Date uniqueDatetime = PlanReviewDateTimeDecider.getUniqueDatetime();
                        String stdDateStr = IndustryConceptThsOfPlanDao.decideDateStrForPlan(uniqueDatetime);
                        // ???10????????????????????????bean, ??????????????????; ??????????????????????????????, ???????????????
                        for (int i = 0; i < total; i++) {
                            IndustryConceptSimple element = (IndustryConceptSimple) model.getElementAt(i);
                            IndustryConceptThsOfPlan.Type type;
                            if ("??????".equals(element.getType())) {
                                type = IndustryConceptThsOfPlan.Type.INDUSTRY;
                            } else { // "??????".equals(element.getType())
                                type = IndustryConceptThsOfPlan.Type.CONCEPT;
                            }
                            IndustryConceptThsOfPlan newBean = IndustryConceptThsOfPlanDao
                                    .getOrInitBeanForPlan(element.getName(), uniqueDatetime, type); // ??????????????????bean
                            try {
                                IndustryConceptThsOfPlanDao.syncEditableAttrByLatestNSameBean(newBean, stdDateStr, 10);
                            } catch (SQLException ex) {
                                ManiLog.put(StrUtil.format("???????????????????????????: {}", newBean.getName()));
                            }
                            int n = (int) CommonUtil.roundHalfUP((i + 1) * 1.0 / total * 100, 0);
                            jProgressBar.setValue(n); // ????????????
                            // ?????????????????????1
                        }
                        panelForPlan.update();
                        panelForPlan.editorPanel.update();
                        ManiLog.put("??????????????????!");
                        dialog.dispose();
                        parentDialog.dispose();
                    }
                }, true);


                jPanel1.add(jProgressBar, BorderLayout.CENTER);
                dialog.setContentPane(jPanel1);
                dialog.setSize(500, 70);
                dialog.setLocationRelativeTo(dialog.getParent());
                dialog.setVisible(true);// ????????????????????????


            }
        });


        return jPanel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IndustryConceptSimple {
        String name;
        String type; // ????????????

        @Override
        public String toString() {
            return name + " -- " + type;
        }
    }

    /**
     * ?????? ?????????????????? bean?????? ????????? ????????????
     * ?????????????????? beanMap, ?????? newDf ????????????
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
    protected DataFrame<Object> newDf; // ????????????df??????

    @Override
    public void update() {
        flushBeanMapAndShowDf(); // ?????? beanMap???newDf
        if (newDf == null) {
            return;
        }
        if (jTable == null) { // ????????????
            Vector<Vector<Object>> datas = new Vector<>();
            for (int i = 0; i < newDf.length(); i++) {
                datas.add(new Vector<>(newDf.row(i)));
            }
            Vector<Object> cols = new Vector<>(newDf.columns());
            DefaultTableModel model = new DefaultTableModel(datas, cols) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // ????????????!
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
                        return; // ??????????????????, ?????????????????????
                    }
                    Object valueAt;
                    try {
                        valueAt = model.getValueAt(row, 0);
                    } catch (Exception ex) {
                        return;
                    }
                    currentBean = beanMap.get(Long.parseLong(valueAt.toString()));
                    editorPanel.update(currentBean);
                    updateKLineAndFsDisplay(); // k??????????????????
                }
            });
            initJTableStyle();
            jScrollPane.setViewportView(jTable); // ????????????"???????????????", ???????????????
            jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            fitTableColumns(jTable);
        } else { // ???????????????
            fullFlush();
        }
    }

    /**
     * ???????????????
     */
    private void initJTableStyle() {
        // ???????????????????????????
        jTable.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(COLOR_LIST_HEADER_FORE_EM);
        for (int i = 0; i < newDf.size(); i++) {
            //i???????????????
            TableColumn column = jTable.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //??????????????????
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        jTable.setForeground(COLOR_LIST_FLAT_EM);
        jTable.setBackground(COLOR_LIST_BK_EM);

        DefaultTableCellRenderer cellRendererOfTitle = new DefaultTableCellRenderer();
        cellRendererOfTitle.setForeground(COLOR_LIST_RAISE_EM);
        jTable.getColumn("??????").setCellRenderer(cellRendererOfTitle);

        jTable.setRowHeight(30);
        jTable.setFont(new Font("????????????", Font.PLAIN, 18));
    }

    private void removeEnterKeyDefaultAction() {
        ActionMap am = jTable.getActionMap();
        am.getParent().remove("selectNextRowCell"); // ???????????????: ????????????????????????????????????
        jTable.setActionMap(am);
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * ??????????????????
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
        // ??????????????????, ??????????????????, ????????????????????????, ???????????????
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
     * ?????????????????????
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
            header.setResizingColumn(column); // ???????????????

            int actualWidth = width + myTable.getIntercellSpacing().width + 2;
            actualWidth = Math.min(700, actualWidth); // ??????????????????
            if (dummyIndex <= 20 && dummyIndex > 8) {
                column.setWidth(Math.min(actualWidth, 80)); // 12????????????????????????
            } else {
                column.setWidth(Math.max(actualWidth, 80)); // ???5
            }
//            break; // ??????????????????. ???????????????

            if (dummyIndex == 5) {
                column.setWidth(5); // ???5
            }
            if (dummyIndex == 8) {
                column.setWidth(5); // ???5
            }
            if (dummyIndex == 9) {
                column.setWidth(5); // ???5
            }


            dummyIndex++;
        }
    }

    public void showInMainDisplayWindow() {
        // 9.???????????????????????????
        mainDisplayWindow.setCenterPanel(this);
    }
}
