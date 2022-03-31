package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.DateTimePicker;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import org.jdesktop.swingx.JXComboBox;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/29/029-01:18:49
 */
@Getter
public class IndustryConceptThsOfPlanEditorPanel extends DisplayPanel {
    IndustryConceptPanelForPlan parentPanel;
    IndustryConceptThsOfPlan bean;


    JLabel totalAmountLabel = getCommonLabel("行业概念总数量", Color.red);  // id
    JLabel totalAmountLabelLabel = getCommonLabel("", Color.red);

    // 子控件, 对应bean 各种属性, 以及部分操作按钮
    JLabel idLabel = getCommonLabel("id");  // id
    JLabel idValueLabel = getCommonLabel();
    JLabel nameLabel = getCommonLabel("name");
    JLabel nameValueLabel = getCommonLabel();
    JLabel typeLabel = getCommonLabel("type");
    JLabel typeValueLabel = getCommonLabel();
    JLabel type2Label = getCommonLabel("type2");
    JLabel type2ValueLabel = getCommonLabel();
    JLabel codeLabel = getCommonLabel("code");
    JLabel codeValueLabel = getCommonLabel();
    JLabel indexCodeLabel = getCommonLabel("indexCode");
    JLabel indexCodeValueLabel = getCommonLabel();
    JLabel dateStrLabel = getCommonLabel("dateStr");
    JLabel dateStrValueLabel = getCommonLabel();
    JLabel chgPLabel = getCommonLabel("chgP");
    JLabel chgPValueLabel = getCommonLabel();

    JLabel generatedTimeLabel = getCommonLabel("generatedTime");
    JLabel generatedTimeValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified"); // 编辑后自动设定
    JLabel lastModifiedValueLabel = getCommonLabel();

    // 带有查看的不可变3字段: 关联概念/关联行业/成分股
    JLabel relatedConceptListLabel = getCommonLabel("relatedConceptList");
    JLabel relatedConceptListValueLabel = getCommonLabel();
    JPanel relatedConceptListPanel = initRelatedConceptListDisplayPanel();
    JLabel relatedIndustryListLabel = getCommonLabel("relatedIndustryList");
    JLabel relatedIndustryListValueLabel = getCommonLabel();
    JPanel relatedIndustryListPanel = initRelatedIndustryListDisplayPanel();
    JLabel includeStockListLabel = getCommonLabel("includeStockList"); // 编辑后自动设定
    JLabel includeStockListValueLabel = getCommonLabel();
    JPanel includeStockListPanel = initIncludeStockListPanel();
    // 龙头股可编辑
    JLabel leaderStockListLabel = getCommonLabel("leaderStockList", Color.pink); // 龙头股编辑
    JTextField leaderStockListValueLabel = getCommonEditor(this);
    JPanel leaderStockListPanel = initLeaderStockListPanel();


    // 编辑
    JLabel pricePositionShortTermLabel = getCommonLabel("pricePositionShortTerm", Color.pink);
    JXComboBox pricePositionShortTermValueComboBox = getCommonJXComboBox(this,
            IndustryConceptThsOfPlan.PricePosition.allPricePositions);
    JLabel pricePositionLongTermLabel = getCommonLabel("pricePositionLongTerm", Color.pink);
    JXComboBox pricePositionLongTermValueComboBox = getCommonJXComboBox(this,
            IndustryConceptThsOfPlan.PricePosition.allPricePositions);
    JLabel priceTrendLabel = getCommonLabel("priceTrend", Color.pink);
    JXComboBox priceTrendValueComboBox = getCommonJXComboBox(this, IndustryConceptThsOfPlan.PriceTrend.allPriceTrends);
    JLabel oscillationAmplitudeLabel = getCommonLabel("oscillationAmplitude", Color.pink);
    JXComboBox oscillationAmplitudeValueComboBox = getCommonJXComboBox(this,
            IndustryConceptThsOfPlan.OscillationAmplitude.allOscillationAmplitudes);
    JLabel lineTypeLabel = getCommonLabel("lineType", Color.pink);
    JXComboBox lineTypeValueComboBox = getCommonJXComboBox(this, IndustryConceptThsOfPlan.LineType.allLineTypes);
    JLabel hypeReasonLabel = getCommonLabel("hypeReason", Color.pink);
    JTextField hypeReasonValueLabel = getCommonEditor(this);
    JLabel hypeStartDateLabel = getCommonLabel("hypeStartDate", Color.pink); // todo: 日期控件
    JTextField hypeStartDateValueLabel = getCommonEditor(this);

    // 特殊: 炒作开始时间, 使用 JTextField, 配合 日期选择器, 将绑定 hypeStartDateValueLabel
    DateTimePicker hypeStartDatePicker;

    JLabel hypePhaseCurrentLabel = getCommonLabel("hypePhaseCurrent", Color.pink);
    JXComboBox hypePhaseCurrentValueComboBox = getCommonJXComboBox(this,
            IndustryConceptThsOfPlan.HypePhase.allHypePhases);
    JLabel specificDescriptionLabel = getCommonLabel("specificDescription", Color.pink);
    JTextField specificDescriptionValueLabel = getCommonEditor(this);
    JLabel goodAspectsLabel = getCommonLabel("goodAspects", Color.pink);
    JTextField goodAspectsValueLabel = getCommonEditor(this);
    JLabel badAspectsLabel = getCommonLabel("badAspects", Color.pink);
    JTextField badAspectsValueLabel = getCommonEditor(this);
    JLabel warningsLabel = getCommonLabel("warnings", Color.pink);
    JTextField warningsValueLabel = getCommonEditor(this);

    JLabel trendLabel = getCommonLabel("trend", Color.pink);
    JTextField trendValueLabel = getCommonEditor(this);
    JLabel remarkLabel = getCommonLabel("remark", Color.pink);
    JTextField remarkValueLabel = getCommonEditor(this);

    JLabel preJudgmentViewsLabel = getCommonLabel("preJudgmentViews", Color.pink);
    JTextField preJudgmentViewsValueLabel = getCommonEditor(this);
    JLabel futuresLabel = getCommonLabel("futures", Color.pink);
    JTextField futuresValueLabel = getCommonEditor(this);
    JLabel scoreOfPreJudgmentLabel = getCommonLabel("scoreOfPreJudgment", Color.pink);
    JTextField scoreOfPreJudgmentValueLabel = getCommonEditor(this);
    JLabel scoreReasonLabel = getCommonLabel("scoreReason", Color.pink);
    JTextField scoreReasonValueLabel = getCommonEditor(this);


    public IndustryConceptThsOfPlanEditorPanel(IndustryConceptPanelForPlan parentPanel) {
        this.parentPanel = parentPanel;
        this.setLayout(new GridLayout(33, 2, 1, 1)); // 简易网格布局
        this.setPreferredSize(new Dimension(350, 1200));

        this.add(totalAmountLabel);
        this.add(totalAmountLabelLabel);

        this.add(idLabel);
        this.add(idValueLabel);

        this.add(nameLabel);
        this.add(nameValueLabel);

        this.add(typeLabel);
        this.add(typeValueLabel);

        this.add(type2Label);
        this.add(type2ValueLabel);

        this.add(codeLabel);
        this.add(codeValueLabel);

        this.add(indexCodeLabel);
        this.add(indexCodeValueLabel);

        this.add(dateStrLabel);
        this.add(dateStrValueLabel);

        this.add(chgPLabel);
        this.add(chgPValueLabel);

        this.add(generatedTimeLabel);
        this.add(generatedTimeValueLabel);

        this.add(lastModifiedLabel);
        this.add(lastModifiedValueLabel);

        this.add(relatedConceptListLabel);
        //this.add(relatedConceptListValueLabel);
        this.add(relatedConceptListPanel); // 单纯的label展示,换成label和查看按钮

        this.add(relatedIndustryListLabel);
        //this.add(relatedIndustryListValueLabel);
        this.add(relatedIndustryListPanel);

        this.add(includeStockListLabel);
        //this.add(includeStockListValueLabel);
        this.add(includeStockListPanel);

        this.add(leaderStockListLabel); // @noti: 龙头股设置, 移动到成分股后面
        //this.add(leaderStockListValueLabel);
        this.add(leaderStockListPanel);

        this.add(pricePositionShortTermLabel);
        this.add(pricePositionShortTermValueComboBox);

        this.add(pricePositionLongTermLabel);
        this.add(pricePositionLongTermValueComboBox);

        this.add(priceTrendLabel);
        this.add(priceTrendValueComboBox);

        this.add(oscillationAmplitudeLabel);
        this.add(oscillationAmplitudeValueComboBox);

        this.add(lineTypeLabel);
        this.add(lineTypeValueComboBox);

        this.add(hypeReasonLabel);
        this.add(hypeReasonValueLabel);

        this.add(hypeStartDateLabel);
        this.add(hypeStartDateValueLabel);

        this.add(hypePhaseCurrentLabel);
        this.add(hypePhaseCurrentValueComboBox);

        this.add(specificDescriptionLabel);
        this.add(specificDescriptionValueLabel);


        this.add(goodAspectsLabel);
        this.add(goodAspectsValueLabel);

        this.add(badAspectsLabel);
        this.add(badAspectsValueLabel);

        this.add(warningsLabel);
        this.add(warningsValueLabel);

        this.add(trendLabel);
        this.add(trendValueLabel);

        this.add(remarkLabel);
        this.add(remarkValueLabel);

        this.add(preJudgmentViewsLabel);
        this.add(preJudgmentViewsValueLabel);

        this.add(futuresLabel);
        this.add(futuresValueLabel);

        this.add(scoreOfPreJudgmentLabel);
        this.add(scoreOfPreJudgmentValueLabel);

        this.add(scoreReasonLabel);
        this.add(scoreReasonValueLabel);

        initOther();

    }

    private void initOther() {
        // 1. 炒作时间选择器绑定
        hypeStartDatePicker = new DateTimePicker("yyyy-MM-dd HH:mm:ss", 160, 200);
        hypeStartDatePicker.setEnable(true).setSelect(DateUtil.date()).changeDateEvent(new Consumer<DateTimePicker>() {
            @Override
            public void accept(DateTimePicker o) {

            }
        }).register(hypeStartDateValueLabel); // 绑定到时间选择

        // 2.炒作原因背景色
        hypeReasonValueLabel.setBackground(Color.cyan);

        // 3.预判4字段背景色
        preJudgmentViewsValueLabel.setBackground(Color.cyan);
        futuresValueLabel.setBackground(Color.cyan);
        scoreOfPreJudgmentValueLabel.setBackground(Color.cyan);
        scoreReasonValueLabel.setBackground(Color.cyan);
        // 4.龙头股标签醒目
        leaderStockListLabel.setForeground(Color.red);
    }

    /**
     * 关联概念列表展示 panel
     */
    private JPanel initRelatedConceptListDisplayPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());

        JButton detailButton = ButtonFactory.getButton("查看");
        detailButton.setPreferredSize(new Dimension(60, 30));
        detailButton.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        jPanel.add(relatedConceptListValueLabel, BorderLayout.CENTER);
        jPanel.add(detailButton, BorderLayout.EAST);
        detailButton.addActionListener(new ActionListener() { // 点击按钮, 弹窗展示详细的 相关概念列表
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bean != null) {
                    JDialog dialog = new JDialog(TraderGui.INSTANCE, "关联概念列表");

                    //创建JDialog
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    dialog.setContentPane(panel);

                    JLabel jLabel = new JLabel(GuiCommonUtil.jsonStrToHtmlFormat(bean.getRelatedConceptListJsonStr()));
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
            }
        });
        return jPanel;
    }

    private JPanel initRelatedIndustryListDisplayPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JButton detailButton = ButtonFactory.getButton("查看");
        detailButton.setPreferredSize(new Dimension(60, 30));
        detailButton.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        jPanel.add(relatedIndustryListValueLabel, BorderLayout.CENTER);
        jPanel.add(detailButton, BorderLayout.EAST);
        detailButton.addActionListener(new ActionListener() { // 点击按钮, 弹窗展示详细的 相关概念列表
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bean != null) {
                    JDialog dialog = new JDialog(TraderGui.INSTANCE, "关联行业列表");

                    //创建JDialog
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    dialog.setContentPane(panel);

                    JLabel jLabel = new JLabel(GuiCommonUtil.jsonStrToHtmlFormat(bean.getRelatedIndustryListJsonStr()));
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
            }
        });
        return jPanel;
    }


    private JPanel initIncludeStockListPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JButton detailButton = ButtonFactory.getButton("查看");
        detailButton.setPreferredSize(new Dimension(60, 30));
        detailButton.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        jPanel.add(includeStockListValueLabel, BorderLayout.CENTER);
        jPanel.add(detailButton, BorderLayout.EAST);
        detailButton.addActionListener(new ActionListener() { // 点击按钮, 弹窗展示详细的 相关概念列表
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bean != null) {
                    JDialog dialog = new JDialog(TraderGui.INSTANCE, "关联行业列表");

                    //创建JDialog
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    dialog.setContentPane(panel);

                    JLabel jLabel = new JLabel(GuiCommonUtil.jsonStrToHtmlFormat(bean.getIncludeStockListJsonStr()));
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
            }
        });
        return jPanel;
    }

    /**
     * 龙头股列表 编辑 和 查看, 主要是点击按钮显示的对话框回调
     *
     * @return
     */
    private JPanel initLeaderStockListPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JButton detailButton = ButtonFactory.getButton("编辑查看");
        detailButton.setPreferredSize(new Dimension(60, 30));
        detailButton.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        jPanel.add(leaderStockListValueLabel, BorderLayout.CENTER);
        jPanel.add(detailButton, BorderLayout.EAST);
        detailButton.addActionListener(new ActionListener() { // 点击按钮, 弹窗展示详细的 相关概念列表
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bean != null) {
                    JDialog dialog = new JDialog(TraderGui.INSTANCE, "龙头股列表");

                    //创建JDialog
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    dialog.setContentPane(panel);

                    JPanel leaderStockListEditPanel = initLeaderStockListEditPanel();

                    //添加控件到对话框
                    JScrollPane jScrollPane = new JScrollPane();
                    jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
                    BasicScrollBarUIS
                            .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
                    jScrollPane.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
                    jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                    jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    jScrollPane.setViewportView(leaderStockListEditPanel);
                    panel.add(leaderStockListEditPanel, BorderLayout.CENTER);

                    //显示对话框（setVisible()方法会阻塞，直到对话框关闭）
                    dialog.setSize(1000, 800);
//                    dialog.setLocation();
                    dialog.setLocationRelativeTo(dialog.getParent());
                    dialog.setVisible(true);
                }
            }
        });
        return jPanel;
    }

    /**
     * 龙头股编辑对话框主内容: 左成分股列表, 中 添加 和 删除按钮, 右龙头股列表; 使用 BorderLayout
     * todo: 龙头股编辑
     *
     * @return
     */
    private JPanel initLeaderStockListEditPanel() {

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());

        // 1.左列表: 全部成分股
        Vector<ThsDbApi.ThsSimpleStock> allIncludeStockList = new Vector<>(bean.getIncludeStockList());
        DefaultListModel model1 = new DefaultListModel();
        model1.addAll(allIncludeStockList);
        JXList allStocksList = new JXList(model1);
        allStocksList.setForeground(Color.orange);
        allStocksList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPaneLeft = new JScrollPane();
        jScrollPaneLeft.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneLeft, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPaneLeft.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPaneLeft.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneLeft.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneLeft.setViewportView(allStocksList);
        jScrollPaneLeft.setPreferredSize(new Dimension(440, 800));
        try {
            allStocksList.setSelectedIndex(0);
        } catch (Exception e) {
        }

        // 2.两个按钮, 添加 和 删除按钮
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new VerticalFlowLayout()); // 居中对齐
        JButton addButton = ButtonFactory.getButton("添加->");
        JButton deleteButton = ButtonFactory.getButton("<-删除");
        JButton upButton = ButtonFactory.getButton("上移"); // 龙头股排序, 将选中的项目上移
        JButton downButton = ButtonFactory.getButton("下移");
        JButton saveButton = ButtonFactory.getButton("保存");
        saveButton.setBackground(Color.red);
        saveButton.setForeground(Color.black);
        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(upButton);
        buttonsPanel.add(downButton);
        buttonsPanel.add(saveButton);
        buttonsPanel.setSize(new Dimension(60, 800));

        // 3.右列表: 龙头股
        Vector<ThsDbApi.ThsSimpleStock> leaderStocks = new Vector<>(bean.getLeaderStockList());
        DefaultListModel model2 = new DefaultListModel();
        model2.addAll(leaderStocks);
        JXList leaderStocksList = new JXList(model2);
        leaderStocksList.setForeground(Color.orange);
        leaderStocksList.setBackground(COLOR_THEME_MINOR);

        JScrollPane jScrollPaneRight = new JScrollPane();
        jScrollPaneRight.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneRight, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPaneRight.getVerticalScrollBar().setUnitIncrement(25); // 滑动速度
        jScrollPaneRight.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneRight.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneRight.setViewportView(leaderStocksList);
        jScrollPaneRight.setPreferredSize(new Dimension(440, 800));
        try {
            leaderStocksList.setSelectedIndex(0);
        } catch (Exception e) {
        }

        jPanel.add(jScrollPaneLeft, BorderLayout.WEST);
        jPanel.add(buttonsPanel, BorderLayout.CENTER);
        jPanel.add(jScrollPaneRight, BorderLayout.EAST);

        addButton.addActionListener(new ActionListener() { // 读取左列表当前选择, 添加到右列表,
            // 仅控件数据更新,bean数据不变, 要保存更改, 需要点击保存按钮
            @Override
            public void actionPerformed(ActionEvent e) {
                ThsDbApi.ThsSimpleStock selectedValue = (ThsDbApi.ThsSimpleStock) allStocksList.getSelectedValue();
                if (selectedValue == null) {
                    return;
                }
                DefaultListModel model = (DefaultListModel) leaderStocksList.getModel();
                for (int i = 0; i < model.getSize(); i++) {
                    if (model.getElementAt(i).equals(selectedValue)) {
                        ManiLog.put("龙头股已存在,不可重复添加");
                        return;
                    }
                }
                model.addElement(selectedValue);
            }
        });

        deleteButton.addActionListener(new ActionListener() { // 读取右列表当前选择, 删除
            // 仅控件数据更新,bean数据不变, 要保存更改, 需要点击保存按钮
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = leaderStocksList.getSelectedIndex();
                if (selectedIndex < 0) { // -1
                    return;
                }
                DefaultListModel model = (DefaultListModel) leaderStocksList.getModel();
                int rawSize = model.getSize();
                model.remove(selectedIndex);

                int shouldIndex = selectedIndex;
                if (selectedIndex == rawSize - 1) {
                    shouldIndex--;
                }
                try {
                    leaderStocksList.setSelectedIndex(shouldIndex); // 选择索引不变
                } catch (Exception e1) {
                }
            }
        });
        IndustryConceptThsOfPlanEditorPanel panelTemp = this;
        saveButton.addActionListener(new ActionListener() { // 保存右侧列表, 作为bean的龙头股列表
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultListModel model = (DefaultListModel) leaderStocksList.getModel();
                List<ThsDbApi.ThsSimpleStock> stocks = new ArrayList<>();
                for (int i = 0; i < model.getSize(); i++) {
                    try {
                        ThsDbApi.ThsSimpleStock elementAt = (ThsDbApi.ThsSimpleStock) model.getElementAt(i);
                        stocks.add(elementAt);
                    } catch (Exception ex) {
                        // 一般不会
                    }
                }
                bean.updateLeaderStockList(stocks);
                tryAutoSaveEditedBean(panelTemp, "概念行业");
                //ManiLog.put("IndustryConceptThsOfPlan: 已更新龙头股列表");
                panelTemp.update(); // 将刷新显示
            }
        });

        upButton.addActionListener(new ActionListener() { // 读取右列表当前选择, 上移
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = leaderStocksList.getSelectedIndex();
                if (selectedIndex < 0) { // -1
                    return;
                }
                DefaultListModel model = (DefaultListModel) leaderStocksList.getModel();
                if (selectedIndex == 0) {
                    ManiLog.put("龙头股已到达最前,无法上移");
                    return;
                }

                Object raw = model.getElementAt(selectedIndex);
                Object rawUp = model.getElementAt(selectedIndex - 1);

                model.set(selectedIndex, rawUp);
                model.set(selectedIndex - 1, raw);
                leaderStocksList.setSelectedIndex(selectedIndex - 1); // 选中跟随
            }
        });

        downButton.addActionListener(new ActionListener() { // 读取右列表当前选择, 下移
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = leaderStocksList.getSelectedIndex();
                if (selectedIndex < 0) { // -1
                    return;
                }
                DefaultListModel model = (DefaultListModel) leaderStocksList.getModel();
                if (selectedIndex == model.size()-1) {
                    ManiLog.put("龙头股已到达最后,无法下移");
                    return;
                }

                Object raw = model.getElementAt(selectedIndex);
                Object rawDown = model.getElementAt(selectedIndex + 1);

                model.set(selectedIndex, rawDown);
                model.set(selectedIndex + 1, raw);
                leaderStocksList.setSelectedIndex(selectedIndex + 1); // 选中跟随
            }
        });


        return jPanel;
    }

    /**
     * 主刷新方法, 将被调用显示bean
     *
     * @param bean
     */
    public void update(IndustryConceptThsOfPlan bean) {
        this.bean = bean;

        totalAmountLabelLabel.setText(String.valueOf(this.parentPanel.getBeanMap().size())); // 显示总数量
        this.update();
    }

    public IndustryConceptThsOfPlan getEditedBean() {
        if (this.bean == null) {
            return null;
        }

//
//        bean.setLastModified(DateUtil.date());
////        bean.setBriefly(this.brieflyValueLabel.getText());
////        bean.setRelatedObject(this.relatedObjectValueLabel.getText());
//        try {
//            bean.setTrend(Double.parseDouble(this.trendValueLabel.getText()));
//        } catch (NumberFormatException e) {
//            // e.printStackTrace();
//            log.warn("SimpleNewEm.trend: 解析为double失败, 请正确设置");
//        }
////        bean.setMarked(this.markedValueLabel.isSelected());
//        bean.setRemark(this.remarkValueLabel.getText());

        log.info("假装修改了");
        return bean;
    }

    private static final Log log = LogUtil.getLogger();

    private static KeyAdapter buildKeyAdapterForEdit(IndustryConceptThsOfPlanEditorPanel panel) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, "概念行业");
                }
            }
        };
    }

    /**
     * 失去焦点执行相同保存逻辑
     *
     * @param panel
     * @return
     */
    private static FocusListener buildJTextFieldBlurForEdit(IndustryConceptThsOfPlanEditorPanel panel) {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                tryAutoSaveEditedBean(panel, "概念行业");
            }
        };
    }

    private static void tryAutoSaveEditedBean(IndustryConceptThsOfPlanEditorPanel panel, String logPrefix) {
        IndustryConceptThsOfPlan editedBean = panel.getEditedBean();
        if (editedBean == null) {
            return;
        }
        try {
            IndustryConceptThsOfPlanDao.saveOrUpdateBean(editedBean);
            panel.update(editedBean); // 将更新显示自动设置字段
            panel.parentPanel.update(); // 更新列表
            ManiLog.put(StrUtil.format("{}: 更新概念行业bean成功: {} --> {}", logPrefix, editedBean.getId(),
                    editedBean.getName()
                    )
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update() {
        if (this.bean == null) {
            return;
        }

        idValueLabel.setText(String.valueOf(bean.getId()));
        nameValueLabel.setText(String.valueOf(bean.getName()));
        typeValueLabel.setText(String.valueOf(bean.getType()));
        type2ValueLabel.setText(String.valueOf(bean.getType2()));
        codeValueLabel.setText(String.valueOf(bean.getCode()));
        indexCodeValueLabel.setText(String.valueOf(bean.getIndexCode()));
        dateStrValueLabel.setText(String.valueOf(bean.getDateStr()));
        chgPValueLabel.setText(String.valueOf(bean.getChgP()));

        setDateTimeOrNull(bean.getGeneratedTime(), generatedTimeValueLabel);
        setDateTimeOrNull(bean.getLastModified(), lastModifiedValueLabel);

        relatedConceptListValueLabel.setText(String.valueOf(bean.getRelatedConceptListJsonStr()));
        relatedIndustryListValueLabel.setText(String.valueOf(bean.getRelatedIndustryListJsonStr()));
        includeStockListValueLabel.setText(String.valueOf(bean.getIncludeStockListJsonStr()));

        leaderStockListValueLabel.setText(String.valueOf(bean.getLeaderStockListJsonStr()));
        pricePositionShortTermValueComboBox.setSelectedItem(String.valueOf(bean.getPricePositionShortTerm()));
        pricePositionLongTermValueComboBox.setSelectedItem(String.valueOf(bean.getPricePositionLongTerm()));
        priceTrendValueComboBox.setSelectedItem(String.valueOf(bean.getPriceTrend()));
        oscillationAmplitudeValueComboBox.setSelectedItem(String.valueOf(bean.getOscillationAmplitude()));
        lineTypeValueComboBox.setSelectedItem(String.valueOf(bean.getLineType()));
        hypeReasonValueLabel.setText(String.valueOf(bean.getHypeReason()));
        setDateTimeOrNull(bean.getHypeStartDate(), hypeStartDateValueLabel); // todo
        hypePhaseCurrentValueComboBox.setSelectedItem(String.valueOf(bean.getHypePhaseCurrent()));
        specificDescriptionValueLabel.setText(String.valueOf(bean.getSpecificDescription()));
        goodAspectsValueLabel.setText(String.valueOf(bean.getGoodAspects()));
        badAspectsValueLabel.setText(String.valueOf(bean.getBadAspects()));
        warningsValueLabel.setText(String.valueOf(bean.getWarnings()));
        warningsValueLabel.setText(String.valueOf(bean.getWarnings()));

        trendValueLabel.setText(CommonUtil.toStringCheckNull(bean.getTrend(), ""));
        remarkValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRemark(), ""));

        preJudgmentViewsValueLabel.setText(String.valueOf(bean.getPreJudgmentViews()));
        futuresValueLabel.setText(String.valueOf(bean.getFutures()));
        scoreOfPreJudgmentValueLabel.setText(String.valueOf(bean.getScoreOfPreJudgment()));
        scoreReasonValueLabel.setText(String.valueOf(bean.getScoreReason()));


    }

    public void setDateTimeOrNull(Date timestamp, JLabel label) {
        if (timestamp == null) {
            label.setText("");
        } else {
            label.setText(DateUtil.date(timestamp).toStringDefaultTimeZone());
        }
    }

    public void setDateTimeOrNull(Date timestamp, JTextField jTextField) {
        if (timestamp == null) {
            jTextField.setText("");
        } else {
            jTextField.setText(DateUtil.date(timestamp).toStringDefaultTimeZone());
        }
    }


    public static JLabel getCommonLabel() {
        return getCommonLabel("");
    }


    public static JLabel getCommonLabel(String text) {
        return getCommonLabel(text, Color.white);
    }

    public static JLabel getCommonLabel(String text, Color foreColor) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        label.setForeground(foreColor);
        return label;
    }

    public static JTextField getCommonEditor(
            IndustryConceptThsOfPlanEditorPanel panel) {
        JTextField jTextField = new JTextField();
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit(panel));
//        jTextField.addFocusListener(buildJTextFieldBlurForEdit(panel));
        jTextField.setBackground(Color.BLACK);
        jTextField.setForeground(Color.red);
        jTextField.setCaretColor(Color.red);
        return jTextField;
    }


    public static JCheckBox getCommonCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        checkBox.setBackground(COLOR_THEME_MINOR);
        checkBox.setForeground(Color.pink);
        return checkBox;
    }

    /**
     * 因bean多个字段设置为备选项列表, 因此使用 JXComboBox, 而非编辑框
     *
     * @param panel
     * @return
     */
    public static JXComboBox getCommonJXComboBox(
            IndustryConceptThsOfPlanEditorPanel panel, Vector<?> items) {
        JXComboBox comboBox = new JXComboBox(items);
        comboBox.setBackground(Color.black);
        comboBox.setForeground(Color.orange);
        return comboBox;
    }
}
