package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.*;
import org.jdesktop.swingx.JXList;
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
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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


        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.setBorder(null);
        buttonContainer.add(buttonFlushAll);
        buttonContainer.add(saveEditingBeanButton);
        buttonContainer.add(addBeansBatchButton);
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
        jTabbedPaneLeft.setPreferredSize(new Dimension(440, 800));

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

        // 4.添加3大部分
        jPanel.add(jTabbedPaneLeft, BorderLayout.WEST);
        jPanel.add(buttonsPanel, BorderLayout.CENTER);
        jPanel.add(jScrollPaneRight, BorderLayout.EAST);


        // 5.各按钮功能实现
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1.当前左侧list
                Component tabComponentAt = jTabbedPaneLeft.getSelectedComponent();
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
                    @SneakyThrows
                    public void run() {
                        DefaultListModel model = (DefaultListModel) newBeanList.getModel();
                        int total = model.getSize(); // 进度和遍历

                        Date uniqueDatetime = PlanReviewDateTimeDecider.getUniqueDatetime();
                        String stdDateStr = IndustryConceptThsOfPlanDao.decideDateStrForPlan(uniqueDatetime);
                        // 前10个交易日内的同名bean, 设置某些属性; 若获取全部则随着时间, 太多了太慢
                        String pre10DateStr = EastMoneyDbApi.getPreNTradeDateStrict(stdDateStr, 10);

                        for (int i = 0; i < total; i++) {
                            IndustryConceptSimple element = (IndustryConceptSimple) model.getElementAt(i);
                            IndustryConceptThsOfPlan.Type type;
                            if ("行业".equals(element.getType())) {
                                type = IndustryConceptThsOfPlan.Type.INDUSTRY;
                            } else { // "概念".equals(element.getType())
                                type = IndustryConceptThsOfPlan.Type.CONCEPT;
                            }
                            List<IndustryConceptThsOfPlan> oldBeans = IndustryConceptThsOfPlanDao
                                    .getBeanListByNameAndType("三胎概念", "概念", pre10DateStr, stdDateStr); // 曾经bean列表
                            IndustryConceptThsOfPlan newBean = IndustryConceptThsOfPlanDao
                                    .getOrInitBeanForPlan(element.getName(), uniqueDatetime, type); // 已初步保存新bean

                            // @key: 对简单可编辑属性 赋值
                            if (oldBeans.size() != 0) { // 存在历史
                                // @key: 主要是trend 必须自行设定, 以及预判4字段

                                IndustryConceptThsOfPlan lastBean = oldBeans.get(0); // 倒序, 第一个即最新

                                newBean.setPricePositionLongTerm(lastBean.getPricePositionLongTerm());
                                newBean.setPricePositionShortTerm(lastBean.getPricePositionShortTerm());
                                newBean.setPriceTrend(lastBean.getPriceTrend());
                                newBean.setOscillationAmplitude(lastBean.getOscillationAmplitude());
                                newBean.setOscillationAmplitude(lastBean.getOscillationAmplitude());
                                newBean.setLineType(lastBean.getLineType());
                                newBean.setHypeReason(lastBean.getHypeReason());
                                newBean.setHypeStartDate(lastBean.getHypeStartDate());
                                newBean.setHypePhaseCurrent(lastBean.getHypePhaseCurrent());
                                newBean.setSpecificDescription(lastBean.getSpecificDescription());

                                newBean.setLeaderStockList(lastBean.getLeaderStockList());
                                newBean.setLeaderStockListJsonStr(lastBean.getLeaderStockListJsonStr());

                                newBean.setGoodAspects(lastBean.getGoodAspects());
                                newBean.setBadAspects(lastBean.getBadAspects());
                                newBean.setWarnings(lastBean.getWarnings());

                                newBean.setRemark(lastBean.getRemark());
                            }

                            IndustryConceptThsOfPlanDao.saveOrUpdateBean(newBean); // 保存更新
                            int n = (int) CommonUtil.roundHalfUP((i + 1) * 1.0 / total * 100, 0);
                            Console.log(n);
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
