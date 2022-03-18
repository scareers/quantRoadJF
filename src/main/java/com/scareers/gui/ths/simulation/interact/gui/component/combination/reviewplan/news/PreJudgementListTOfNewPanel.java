package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.bean.NewAspectSummary;
import com.scareers.tools.stockplan.bean.dao.NewAspectSummaryDao;
import com.scareers.utils.CommonUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 对资讯总结, 大势预判 列表! 作为NewAspectSummaryPanel 第三个子组件
 * 类似 NewPointsPanel, 不使用table;
 * 使用内部类 PreJudgementPanel --> 4个字 段
 *
 * @author: admin
 * @date: 2022/3/18/018-23:19:45
 */
@Setter
@Getter
public class PreJudgementListTOfNewPanel extends DisplayPanel {
    NewAspectSummary bean;
    JPanel itemsPanel;

    public PreJudgementListTOfNewPanel(NewAspectSummary bean) {
        this.bean = bean;

        initHeaderPanel();

        JPanel titleContainer = new JPanel();
        titleContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));

        JButton addButton = ButtonFactory.getButton("添加");
        PreJudgementListTOfNewPanel temp = this;
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bean.addPreJudgmentView(""); // bean执行添加
                NewAspectSummaryDao.saveOrUpdateBean(bean);
                temp.setVisible(false);
                temp.update();
                temp.setVisible(true);
            }
        });

        addButton.setFont(new Font("微软雅黑", Font.ITALIC, 16));
        addButton.setForeground(Color.red);
        titleContainer.add(addButton);

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 1, 1));
        // 垂直布局

        this.setLayout(new BorderLayout());
        this.add(titleContainer, BorderLayout.NORTH);
        this.add(itemsPanel, BorderLayout.CENTER);

        this.update();
    }

    JPanel headerPanel = new JPanel();

    /**
     * 表头控件. 布局完全等价于 item
     */
    private void initHeaderPanel() {
        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new GridLayout(1, 3, 1, 1));
        panelTemp.add(getCommonLabel("预判", Color.red));
        panelTemp.add(getCommonLabel("未来实际", Color.red));
        panelTemp.add(getCommonLabel("得分分析", Color.red));

        JPanel panelTemp2 = new JPanel();
        panelTemp2.setLayout(new BorderLayout());
        panelTemp2.add(panelTemp, BorderLayout.CENTER);
        JLabel scoreHeader = getCommonLabel("得分", Color.red);
        scoreHeader.setPreferredSize(new Dimension(50, 18));
        panelTemp2.add(scoreHeader, BorderLayout.EAST);

        headerPanel.setLayout(new BorderLayout());
        JPanel panelIndexHolder = new JPanel();
        panelIndexHolder.setPreferredSize(new Dimension(25, 18)); // 模拟index占位符
        headerPanel.add(panelIndexHolder, BorderLayout.WEST);
        JPanel deleteButtonHolder = new JPanel();
        deleteButtonHolder.setPreferredSize(new Dimension(40, 18)); // 模拟删除按钮占位符
        headerPanel.add(deleteButtonHolder, BorderLayout.EAST);
        headerPanel.add(panelTemp2, BorderLayout.CENTER); // 4主字段
    }


    @Override
    public void update() {
        itemsPanel.removeAll();
        itemsPanel.add(headerPanel);

        // 构建子控件列表
        for (int i = 0; i < this.bean.getPreJudgmentViews().size(); i++) {
            PreJudgementPanel pointPanel = new PreJudgementPanel(i, this, Color.red, bean);
            itemsPanel.add(pointPanel);
        }
    }

    /**
     * 单条预判 相关 4字段
     */
    @Setter
    @Getter
    public static class PreJudgementPanel extends DisplayPanel {
        NewAspectSummary bean;

        int index;  // 基于0的索引, 但是, update 显示时, +1 显示文字
        PreJudgementListTOfNewPanel containerPanel; // 父亲

        JLabel indexLabel; // 左
        JButton deleteButton; // 右

        JPanel centerFourFieldPanel; // 包含以下4字段
        // 中4字段, 采用 3平分 + 1窄(打分)
        JTextField prejudgementTextField;
        JTextField futureStateTextField;
        JTextField scoreReasonTextField; // 3grid布局 中
        JTextField scoreTextField; // 东,要求数字

        public PreJudgementPanel(int index,
                                 PreJudgementListTOfNewPanel containerPanel, Color textColor,
                                 NewAspectSummary bean) {
            this.bean = bean;
            this.containerPanel = containerPanel;
            this.index = index;
            indexLabel = getCommonLabel();
            indexLabel.setPreferredSize(new Dimension(25, 18));
            initCenterPanel(textColor);

            deleteButton = ButtonFactory.getButton("删除");
            deleteButton.setForeground(Color.pink);
            deleteButton.setFont(new Font("微软雅黑", Font.ITALIC, 12));
            deleteButton.setPreferredSize(new Dimension(40, 18));
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    bean.removePreJudgmentView(index);
                    NewAspectSummaryDao.saveOrUpdateBean(bean);
                    containerPanel.setVisible(false);
                    containerPanel.update();
                    containerPanel.setVisible(true);
                }
            });

            this.setLayout(new BorderLayout());
            this.add(indexLabel, BorderLayout.WEST);
            this.add(centerFourFieldPanel, BorderLayout.CENTER);
            this.add(deleteButton, BorderLayout.EAST);
            this.update();
        }

        private void initCenterPanel(Color textColor) {
            centerFourFieldPanel = new JPanel();
            centerFourFieldPanel.setLayout(new BorderLayout());

            prejudgementTextField = getCommonEditor(this);
            prejudgementTextField.setForeground(textColor);
            prejudgementTextField.setCaretColor(Color.red);

            futureStateTextField = getCommonEditor(this);
            futureStateTextField.setForeground(textColor);
            futureStateTextField.setCaretColor(Color.red);

            scoreReasonTextField = getCommonEditor(this);
            scoreReasonTextField.setForeground(textColor);
            scoreReasonTextField.setCaretColor(Color.red);

            scoreTextField = getCommonEditor(this);
            scoreTextField.setForeground(textColor);
            scoreTextField.setCaretColor(Color.red);
            scoreTextField.setPreferredSize(new Dimension(50, 18));

            JPanel panelTemp = new JPanel();
            panelTemp.setLayout(new GridLayout(1, 3, 1, 1));
            panelTemp.add(prejudgementTextField);
            panelTemp.add(futureStateTextField);
            panelTemp.add(scoreReasonTextField);

            centerFourFieldPanel.add(panelTemp, BorderLayout.CENTER);
            centerFourFieldPanel.add(scoreTextField, BorderLayout.EAST);
        }


        @Override
        public void update() {
            indexLabel.setText((index + 1) + ".");
            prejudgementTextField.setText(this.bean.getPreJudgmentViews().get(index));
            futureStateTextField.setText(this.bean.getFutures().get(index));
            scoreReasonTextField.setText(this.bean.getScoreReasons().get(index));
            scoreTextField.setText(CommonUtil.toStringCheckNull(this.bean.getScoresOfPreJudgment().get(index), ""));
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
            PreJudgementPanel panel) {
        JTextField jTextField = new JTextField();
        jTextField.setBackground(SettingsOfGuiGlobal.COLOR_CHART_BG_EM);
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit(panel));
        jTextField.addFocusListener(buildJTextFieldBlurForEdit(panel));
        return jTextField;
    }

    private static KeyAdapter buildKeyAdapterForEdit(PreJudgementPanel panel) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, "NewAspectSummary [PreJudgement]");
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
    private static FocusListener buildJTextFieldBlurForEdit(PreJudgementPanel panel) {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                tryAutoSaveEditedBean(panel, "NewAspectSummary [PreJudgement]");
            }
        };
    }

    private static void tryAutoSaveEditedBean(PreJudgementPanel panel, String logPrefix) {
        String text = panel.prejudgementTextField.getText();
        Double score;
        try {
            score = Double.valueOf(panel.getScoreTextField().getText()); // 避免被错误编辑了
        } catch (Exception e) {
            score = 0.0; // 同默认值
        }
        panel.bean.updatePreJudgmentView(panel.index,
                panel.getPrejudgementTextField().getText(),
                panel.getFutureStateTextField().getText(),
                score, panel.getScoreReasonTextField().getText());
        try {
            NewAspectSummaryDao.saveOrUpdateBean(panel.bean);
            ManiLog.put(StrUtil.format("{}: 更新成功: {} --> {} / {}", logPrefix,
                    panel.bean.getId(), panel.index, text));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
