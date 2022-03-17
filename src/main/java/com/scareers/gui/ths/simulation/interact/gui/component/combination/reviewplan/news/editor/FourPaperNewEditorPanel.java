package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.editor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.FourPaperNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.MajorIssueListPanel;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.bean.FourPaperNew;
import com.scareers.tools.stockplan.bean.MajorIssue;
import com.scareers.tools.stockplan.bean.dao.FourPaperNewDao;
import com.scareers.tools.stockplan.bean.dao.MajorIssueDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.Date;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 展示 重大事项新闻bean 并可编辑保存!
 *
 * @author: admin
 * @date: 2022/3/13/013-10:37:30
 */
@Getter
public class FourPaperNewEditorPanel extends DisplayPanel {
    FourPaperNew bean;

    JLabel totalAmountLabel = getCommonLabel("重大事件总数量", Color.red);  // id
    JLabel totalAmountValueLabel = getCommonLabel("", Color.red);

    // 子控件, 对应bean 各种属性, 以及部分操作按钮
    JLabel idLabel = getCommonLabel("id");  // id
    JLabel idValueLabel = getCommonLabel();

    JLabel dateStrLabel = getCommonLabel("dateStr"); // 虽字符串但重要
    JLabel dateStrValueLabel = getCommonLabel();
    JLabel saveTimeLabel = getCommonLabel("saveTime");
    JLabel saveTimeValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified"); // 编辑后自动设定
    JLabel lastModifiedValueLabel = getCommonLabel();

    // 固定
    JLabel quoteUrlLabel = getCommonLabel("url");
    JLabel quoteUrlValueLabel = getCommonLabel();
    JLabel titleLabel = getCommonLabel("title");
    JLabel titleValueLabel = getCommonLabel();
    JLabel contentLabel = getCommonLabel("content");
    JLabel contentValueLabel = getCommonLabel();
    JLabel typeLabel = getCommonLabel("type");
    JLabel typeValueLabel = getCommonLabel();

    // 编辑
    JLabel markedLabel = getCommonLabel("marked", Color.pink);
    JCheckBox markedValueLabel = getCommonCheckBox();
    JLabel brieflyLabel = getCommonLabel("briefly", Color.pink);
    JTextField brieflyValueLabel = getCommonEditor(this);
    JLabel relatedObjectLabel = getCommonLabel("relatedObject", Color.pink);
    JTextField relatedObjectValueLabel = getCommonEditor(this);
    JLabel trendLabel = getCommonLabel("trend", Color.pink);
    JTextField trendValueLabel = getCommonEditor(this);
    JLabel remarkLabel = getCommonLabel("remark", Color.pink);
    JTextField remarkValueLabel = getCommonEditor(this);

    FourPaperNewListPanel parent; // 以便调用持有者方法

    public FourPaperNewEditorPanel(FourPaperNewListPanel parent) {
        this.parent = parent;
        this.setLayout(new GridLayout(15, 2, 1, 1)); // 简易网格布局
        this.setPreferredSize(new Dimension(350, 500));

        this.add(totalAmountLabel);
        this.add(totalAmountValueLabel);

        this.add(idLabel);
        this.add(idValueLabel);

        this.add(dateStrLabel);
        this.add(dateStrValueLabel);

        this.add(saveTimeLabel);
        this.add(saveTimeValueLabel);

        this.add(lastModifiedLabel);
        this.add(lastModifiedValueLabel);

        this.add(titleLabel);
        this.add(titleValueLabel);

        this.add(quoteUrlLabel);
        this.add(quoteUrlValueLabel);

        this.add(contentLabel);
        this.add(contentValueLabel);

        this.add(typeLabel);
        this.add(typeValueLabel);


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

    }

    /**
     * 主刷新方法, 将被调用显示bean
     *
     * @param bean
     */
    public void update(FourPaperNew bean) {
        this.bean = bean;
        totalAmountValueLabel.setText(String.valueOf(this.parent.getBeanMap().size())); // 显示总数量
        this.update();
    }

    public FourPaperNew getEditedBean() {
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
            log.warn("MajorIssue.trend: 解析为double失败, 请正确设置");
        }
        bean.setMarked(this.markedValueLabel.isSelected());
        bean.setRemark(this.remarkValueLabel.getText());
        return bean;
    }

    private static final Log log = LogUtil.getLogger();

    private static KeyAdapter buildKeyAdapterForEdit(FourPaperNewEditorPanel panel) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, "四大报媒");
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
    private static FocusListener buildJTextFieldBlurForEdit(FourPaperNewEditorPanel panel) {
        return new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                tryAutoSaveEditedBean(panel, "四大报媒");
            }
        };
    }

    private static void tryAutoSaveEditedBean(FourPaperNewEditorPanel panel, String logPrefix) {
        FourPaperNew editedBean = panel.getEditedBean();
        if (editedBean == null) {
            return;
        }
        try {
            FourPaperNewDao.updateBean(editedBean);
            panel.update(editedBean); // 将更新显示自动设置字段
//                    INSTANCE.parent.update();  // 该方式无法更新,
            ((FourPaperNewListPanel) panel.parent.getParentS().getTabbedPane().getSelectedComponent())
                    .update();
            // 需要使用此方式进行更新
            ManiLog.put(StrUtil.format("{}: 更新新闻bean成功: {}.{} --> {} / {}", logPrefix, editedBean.getType(),
                    editedBean.getId(),
                    editedBean.getTitle(), editedBean.getTitle()));
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
        setDateTimeOrNull(bean.getSaveTime(), saveTimeValueLabel);
        setDateTimeOrNull(bean.getLastModified(), lastModifiedValueLabel);

        dateStrValueLabel.setText(bean.getDateStr());

        titleValueLabel.setText(String.valueOf(bean.getTitle()));
        quoteUrlValueLabel.setText(String.valueOf(bean.getUrl()));
        contentValueLabel.setText(String.valueOf(bean.getContent()));
        typeValueLabel.setText(String.valueOf(bean.getType()));

        brieflyValueLabel.setText(CommonUtil.toStringCheckNull(bean.getBriefly(), ""));
        relatedObjectValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRelatedObject(), ""));
        trendValueLabel.setText(CommonUtil.toStringCheckNull(bean.getTrend(), ""));
        markedValueLabel.setSelected(bean.getMarked());
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
            FourPaperNewEditorPanel panel) {
        JTextField jTextField = new JTextField();
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit(panel));
        jTextField.addFocusListener(buildJTextFieldBlurForEdit(panel));
        return jTextField;
    }

    public static JCheckBox getCommonCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        checkBox.setBackground(COLOR_THEME_MINOR);
        checkBox.setForeground(Color.pink);
        return checkBox;
    }

}
