package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.news.bean.NewAspectSummary;
import com.scareers.tools.stockplan.news.bean.dao.NewAspectSummaryDao;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * description:  NewAspectSummary bean的 利好利空中性其他  List<String> 信息展示 panel
 * 使用内部类: PointPanel 代表单条信息; 其包含序号 和 编辑框
 * 自身使用 垂直flow向上布局. add时, 实例化 PointPanel, 并添加
 *
 * @author: admin
 * @date: 2022/3/18/018-20:00:06
 */
@Getter
@Setter
public class NewPointsPanel extends DisplayPanel {
    NewAspectSummary bean;

    String pointType; // 使用带标题边框

    JPanel pointsPanel;
    NewAspectSummaryPanel parentPanel;

    public NewPointsPanel(NewAspectSummaryPanel parentPanel, String pointType, NewAspectSummary bean) {
        // 数据
        this.parentPanel = parentPanel;
        this.pointType = pointType;
        this.bean = bean;

        JPanel titleContainer = new JPanel();
        titleContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        JLabel titleLabel = getCommonLabel(pointType, getMainColorByTitle());
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleContainer.add(titleLabel);

        JButton addButton = ButtonFactory.getButton("添加");
        NewPointsPanel temp = this;
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 将依据类型, 进行添加操作, 添加时, 默认添加空 消息
                if (pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
                    bean.addGoodPoint(""); // bean执行添加
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
                    bean.addBadPoint(""); // bean执行添加
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
                    bean.addNeutralPoint(""); // bean执行添加
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
                    bean.addOtherPoint(""); // bean执行添加
                }
                NewAspectSummaryDao.saveOrUpdateBean(bean);
                temp.setVisible(false);
                temp.update();
                temp.setVisible(true);
            }
        });

        addButton.setFont(new Font("微软雅黑", Font.ITALIC, 16));
        addButton.setForeground(getMainColorByTitle());
        titleContainer.add(addButton);

        pointsPanel = new JPanel();
        pointsPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 1, 1));
        // 垂直布局

        this.setLayout(new BorderLayout());
        this.add(titleContainer, BorderLayout.NORTH);
        this.add(pointsPanel, BorderLayout.CENTER);

        this.update();
    }

    public Color getMainColorByTitle() {
        /*
                if (pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
                } else if (pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
                }
         */
        if (pointType == null) {
            return Color.white;
        }
        if (pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
            return Color.red;
        }
        if (pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
//            return Color.green;
            return new Color(0, 230, 0);
        }
        if (pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
            return Color.white;
        }
        if (pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
            return Color.yellow;
        }
        return Color.gray;
    }

    @Override
    public void update() {
        List<String> points = new ArrayList<>();
        if (pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
            points = bean.getGoodPoints();
        } else if (pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
            points = bean.getBadPoints();
        } else if (pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
            points = bean.getNeutralPoints();
        } else if (pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
            points = bean.getOtherPoints();
        }
        pointsPanel.removeAll();

        // 构建子控件列表
        for (int i = 0; i < points.size(); i++) {
            PointPanel pointPanel = new PointPanel(i, points.get(i), this, getMainColorByTitle(), bean);
            pointsPanel.add(pointPanel);
        }
    }

    @Setter
    @Getter
    public static class PointPanel extends DisplayPanel {
        NewAspectSummary bean;

        int index;  // 基于0的索引, 但是, update 显示时, +1 显示文字
        String content;

        JLabel indexLabel;
        JTextField contentTextField;
        NewPointsPanel containerPanel;
        JButton deleteButton;

        public PointPanel(int index, String content, NewPointsPanel containerPanel, Color textColor,
                          NewAspectSummary bean) {
            this.bean = bean;
            this.containerPanel = containerPanel;
            this.index = index;
            this.content = content;
            indexLabel = getCommonLabel();
            indexLabel.setPreferredSize(new Dimension(25, 16));
            contentTextField = getCommonEditor(this, this.containerPanel.getPointType());
            contentTextField.setForeground(textColor);
            contentTextField.setCaretColor(textColor);

            deleteButton = ButtonFactory.getButton("删除");
            deleteButton.setForeground(Color.pink);
            deleteButton.setFont(new Font("微软雅黑", Font.ITALIC, 12));
            PointPanel temp = this;
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
                        bean.removeGoodPoint(index); // bean执行删除
                    } else if (containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
                        bean.removeBadPoint(index); // bean执行删除
                    } else if (containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
                        bean.removeNeutralPoint(index); // bean执行删除
                    } else if (containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
                        bean.removeOtherPoint(index); // bean执行删除
                    }
                    NewAspectSummaryDao.saveOrUpdateBean(bean);
                    containerPanel.setVisible(false);
                    containerPanel.update();
                    containerPanel.setVisible(true);
                    NewAspectSummaryPanel.tryAutoSaveEditedBean(temp.containerPanel.parentPanel, "大势总结");
                }
            });

            this.setLayout(new BorderLayout());
            this.add(indexLabel, BorderLayout.WEST);
            this.add(contentTextField, BorderLayout.CENTER);
            this.add(deleteButton, BorderLayout.EAST);

            this.update();
        }


        @Override
        public void update() {
            indexLabel.setText(String.valueOf(index + 1) + ".");
            contentTextField.setText(this.content);
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
            PointPanel panel, String pointType) {
        JTextField jTextField = new JTextField();
        jTextField.setBackground(SettingsOfGuiGlobal.COLOR_CHART_BG_EM);
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit(panel, pointType));
//        jTextField.addFocusListener(buildJTextFieldBlurForEdit(panel, pointType));
        return jTextField;
    }

    private static KeyAdapter buildKeyAdapterForEdit(PointPanel panel, String pointType) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, StrUtil.format("NewAspectSummary [{}]", pointType));
                    NewAspectSummaryPanel.tryAutoSaveEditedBean(panel.containerPanel.parentPanel, "大势总结");
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
    private static FocusListener buildJTextFieldBlurForEdit(PointPanel panel, String pointType) {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                tryAutoSaveEditedBean(panel, StrUtil.format("NewAspectSummary [{}]", pointType));
            }
        };
    }

    private static void tryAutoSaveEditedBean(PointPanel panel, String logPrefix) {
        String text = panel.contentTextField.getText();

        if (panel.containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_GOOD)) {
            panel.bean.updateGoodPoint(panel.index, text);
        } else if (panel.containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_BAD)) {
            panel.bean.updateBadPoint(panel.index, text);
        } else if (panel.containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_NEUTRAL)) {
            panel.bean.updateNeutralPoint(panel.index, text);
        } else if (panel.containerPanel.pointType.equals(NewAspectSummary.POINT_TYPE_OTHER)) {
            panel.bean.updateOtherPoint(panel.index, text);
        }
        try {
            NewAspectSummaryDao.saveOrUpdateBean(panel.bean);
            ManiLog.put(StrUtil.format("{}: 更新成功: {} --> {} / {}", logPrefix,
                    panel.bean.getId(), panel.index, text));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
