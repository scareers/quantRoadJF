package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.editor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.PcHotNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.news.bean.PcHotNewEm;
import com.scareers.tools.stockplan.news.bean.dao.PcHotNewEmDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Timestamp;
import java.util.Date;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 展示 常规东财新闻bean 并可编辑保存!
 *
 * @author: admin
 * @date: 2022/3/13/013-10:37:30
 */
public class PcHotNewEditorPanel extends DisplayPanel {
    PcHotNewEm bean;

    JLabel newAmountLabel = getCommonLabel("新闻总数量", Color.red);  // id
    JLabel newAmountValueLabel = getCommonLabel("", Color.red);

    // 子控件, 对应bean 各种属性, 以及部分操作按钮
    JLabel idLabel = getCommonLabel("id");  // id
    JLabel idValueLabel = getCommonLabel();

    JLabel showTimeLabel = getCommonLabel("showTime");
    JLabel showTimeValueLabel = getCommonLabel();
    JLabel orderTimeLabel = getCommonLabel("orderTime");
    JLabel orderTimeValueLabel = getCommonLabel();
    JLabel pushTimeLabel = getCommonLabel("pushTime");
    JLabel pushTimeValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified"); // 编辑后自动设定
    JLabel lastModifiedValueLabel = getCommonLabel();

    // 固定
    JLabel titleLabel = getCommonLabel("title");
    JLabel titleValueLabel = getCommonLabel();
    JLabel codeLabel = getCommonLabel("code");
    JLabel codeValueLabel = getCommonLabel();
    JLabel digestLabel = getCommonLabel("digest");
    JLabel digestValueLabel = getCommonLabel();
    JLabel simTitleLabel = getCommonLabel("simTitle");
    JLabel simTitleValueLabel = getCommonLabel();
    JLabel titleColorLabel = getCommonLabel("titleColor");
    JLabel titleColorValueLabel = getCommonLabel();

    JLabel urlLabel = getCommonLabel("url");
    JLabel urlValueLabel = getCommonLabel();

    JLabel imageLabel = getCommonLabel("image");
    JLabel imageValueLabel = getCommonLabel();
    JLabel authorLabel = getCommonLabel("author");
    JLabel authorValueLabel = getCommonLabel();
    JLabel sourceLabel = getCommonLabel("source");
    JLabel sourceValueLabel = getCommonLabel();
    JLabel columnsLabel = getCommonLabel("columns");
    JLabel columnsValueLabel = getCommonLabel();
    JLabel channelsLabel = getCommonLabel("channels");
    JLabel channelsValueLabel = getCommonLabel();
    JLabel interactLabel = getCommonLabel("interact");
    JLabel interactValueLabel = getCommonLabel();
    JLabel sortLabel = getCommonLabel("sort");
    JLabel sortValueLabel = getCommonLabel();
    JLabel typeLabel = getCommonLabel("type");
    JLabel typeValueLabel = getCommonLabel();


    // 编辑
    JLabel brieflyLabel = getCommonLabel("briefly", Color.pink);
    JTextField brieflyValueLabel = getCommonEditor(this);
    JLabel relatedObjectLabel = getCommonLabel("relatedObject", Color.pink);
    JTextField relatedObjectValueLabel = getCommonEditor(this);
    JLabel trendLabel = getCommonLabel("trend", Color.pink);
    JTextField trendValueLabel = getCommonEditor(this);
    JLabel markedLabel = getCommonLabel("marked", Color.pink);
    JCheckBox markedValueLabel = getCommonCheckBox(this);
    JLabel remarkLabel = getCommonLabel("remark", Color.pink);
    JTextField remarkValueLabel = getCommonEditor(this);

    PcHotNewListPanel parent; // 以便调用持有者方法

    public PcHotNewEditorPanel(PcHotNewListPanel parent) {
        this.parent = parent;
        this.setLayout(new GridLayout(25, 2, 1, 1)); // 简易网格布局
        this.setPreferredSize(new Dimension(350, 750));

        this.add(newAmountLabel);
        this.add(newAmountValueLabel);

        this.add(idLabel);
        this.add(idValueLabel);

        this.add(showTimeLabel);
        this.add(showTimeValueLabel);
        this.add(orderTimeLabel);
        this.add(orderTimeValueLabel);
        this.add(pushTimeLabel);
        this.add(pushTimeValueLabel);
        this.add(lastModifiedLabel);
        this.add(lastModifiedValueLabel);

        this.add(titleLabel);
        this.add(titleValueLabel);
        this.add(urlLabel);
        this.add(urlValueLabel);
        this.add(codeLabel);
        this.add(codeValueLabel);
        this.add(digestLabel);
        this.add(digestValueLabel);
        this.add(simTitleLabel);
        this.add(simTitleValueLabel);
        this.add(titleColorLabel);
        this.add(titleColorValueLabel);

        this.add(typeLabel);
        this.add(typeValueLabel);
        this.add(imageLabel);
        this.add(imageValueLabel);
        this.add(authorLabel);
        this.add(authorValueLabel);
        this.add(sourceLabel);
        this.add(sourceValueLabel);
        this.add(columnsLabel);
        this.add(columnsValueLabel);
        this.add(channelsLabel);
        this.add(channelsValueLabel);
        this.add(interactLabel);
        this.add(interactValueLabel);
        this.add(sortLabel);
        this.add(sortValueLabel);


        this.add(markedLabel);
        this.add(markedValueLabel);
        this.add(brieflyLabel);
        this.add(brieflyValueLabel);
        this.add(relatedObjectLabel);
        this.add(relatedObjectValueLabel);
        this.add(trendLabel);
        this.add(trendValueLabel);
        this.add(remarkLabel);
        this.add(remarkValueLabel);

        PcHotNewEditorPanel panelX = this;
        parent.getSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryAutoSaveEditedBean(panelX, "PC热门资讯");
            }
        });

    }

    /**
     * 主刷新方法, 将被调用显示bean
     *
     * @param bean
     */
    public void update(PcHotNewEm bean) {
        this.bean = bean;

        newAmountValueLabel.setText(String.valueOf(this.parent.getBeanMap().size())); // 显示总数量
        this.update();
    }

    public PcHotNewEm getEditedBean() {
        if (this.bean == null) {
            return null;
        }


        bean.setLastModified(Timestamp.valueOf(DateUtil.date().toLocalDateTime()));
        bean.setBriefly(this.brieflyValueLabel.getText());
        bean.setRelatedObject(this.relatedObjectValueLabel.getText());
        try {
            bean.setTrend(Double.parseDouble(this.trendValueLabel.getText()));
        } catch (NumberFormatException e) {
            // e.printStackTrace();
            log.warn("PcHotNewEm.trend: 解析为double失败, 请正确设置");
        }
        bean.setMarked(this.markedValueLabel.isSelected());
        bean.setRemark(this.remarkValueLabel.getText());
        return bean;
    }

    private static final Log log = LogUtil.getLogger();

    private static KeyAdapter buildKeyAdapterForEdit(PcHotNewEditorPanel panel) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, "PC热门资讯");
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
    private static FocusListener buildJTextFieldBlurForEdit(PcHotNewEditorPanel panel) {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                tryAutoSaveEditedBean(panel, "PC热门资讯");
            }
        };
    }

    private static void tryAutoSaveEditedBean(PcHotNewEditorPanel panel, String logPrefix) {
        PcHotNewEm editedBean = panel.getEditedBean();
        if (editedBean == null) {
            return;
        }
        try {
            PcHotNewEmDao.updateBean(editedBean);
            panel.update(editedBean); // 将更新显示自动设置字段
//                    INSTANCE.parent.update();  // 该方式无法更新,
            ((SimpleNewListPanel) panel.parent.getParentS().getTabbedPane().getSelectedComponent())
                    .update();
            // 需要使用此方式进行更新
            ManiLog.put(StrUtil.format("{}: 更新新闻bean成功: {} --> {}", logPrefix,
                    editedBean.getId(),
                    editedBean.getTitle()));
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
        setDateTimeOrNull(bean.getLastModified(), lastModifiedValueLabel);
        showTimeValueLabel.setText(String.valueOf(bean.getShowtime()));
        orderTimeValueLabel.setText(String.valueOf(bean.getOrdertime()));
        pushTimeValueLabel.setText(String.valueOf(bean.getPushtime()));
        titleValueLabel.setText(String.valueOf(bean.getTitle()));
        codeValueLabel.setText(String.valueOf(bean.getCode()));
        digestValueLabel.setText(String.valueOf(bean.getDigest()));
        simTitleValueLabel.setText(String.valueOf(bean.getSimtitle()));
        titleColorValueLabel.setText(String.valueOf(bean.getTitlecolor()));


        typeValueLabel.setText(String.valueOf(bean.getType()));
        imageValueLabel.setText(String.valueOf(bean.getImage()));
        authorValueLabel.setText(String.valueOf(bean.getAuthor()));
        sourceValueLabel.setText(String.valueOf(bean.getSource()));
        columnsValueLabel.setText(String.valueOf(bean.getColumns()));
        channelsValueLabel.setText(String.valueOf(bean.getChannels()));
        interactValueLabel.setText(String.valueOf(bean.getInteract()));
        sortValueLabel.setText(String.valueOf(bean.getSort()));

        brieflyValueLabel.setText(CommonUtil.toStringCheckNull(bean.getBriefly(), ""));
        relatedObjectValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRelatedObject(), ""));
        trendValueLabel.setText(CommonUtil.toStringCheckNull(bean.getTrend(), ""));
        Boolean marked = bean.getMarked();
        if (marked != null) {
            markedValueLabel.setSelected(bean.getMarked());
        } else {
            markedValueLabel.setSelected(false);
        }
        remarkValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRemark(), ""));
    }

    public void setDateTimeOrNull(Date timestamp, JLabel label) {
        if (timestamp == null) {
            label.setText("");
        } else {
            label.setText(DateUtil.date(timestamp).toStringDefaultTimeZone());
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
            PcHotNewEditorPanel panel) {
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

    public static JCheckBox getCommonCheckBox(PcHotNewEditorPanel panel) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        checkBox.setBackground(COLOR_THEME_MINOR);
        checkBox.setForeground(Color.pink);
        checkBox.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tryAutoSaveEditedBean(panel, "PC热门资讯");
                    }
                }
        );
        return checkBox;
    }

}
