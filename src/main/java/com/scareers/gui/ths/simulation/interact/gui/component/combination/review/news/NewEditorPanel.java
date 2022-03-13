package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import com.scareers.tools.stockplan.bean.dao.SimpleNewEmDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import org.apache.maven.model.Parent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 展示东财新闻bean 并可编辑保存!
 *
 * @author: admin
 * @date: 2022/3/13/013-10:37:30
 */
public class NewEditorPanel extends DisplayPanel {
    private static NewEditorPanel INSTANCE;

    public static NewEditorPanel getInstance(SimpleNewListPanel parent) {
        if (INSTANCE == null) {
            INSTANCE = new NewEditorPanel(parent);
        }
        return INSTANCE;
    }

    SimpleNewEm bean;

    // 子控件, 对应bean 各种属性, 以及部分操作按钮
    JLabel idLabel = getCommonLabel("id");  // id
    JLabel idValueLabel = getCommonLabel();

    JLabel dateTimeLabel = getCommonLabel("dateTime"); // 三大时间
    JLabel dateTimeValueLabel = getCommonLabel();
    JLabel saveTimeLabel = getCommonLabel("saveTime");
    JLabel saveTimeValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified"); // 编辑后自动设定
    JLabel lastModifiedValueLabel = getCommonLabel();

    // 固定
    JLabel titleLabel = getCommonLabel("title");
    JLabel titleValueLabel = getCommonLabel();
    JLabel urlLabel = getCommonLabel("url");
    JLabel urlValueLabel = getCommonLabel();
    JLabel detailTitleLabel = getCommonLabel("detailTitle");
    JLabel detailTitleValueLabel = getCommonLabel();
    JLabel typeLabel = getCommonLabel("type");
    JLabel typeValueLabel = getCommonLabel();
    JLabel urlRawHtmlLabel = getCommonLabel("urlRawHtml"); // 常null
    JLabel urlRawHtmlValueLabel = getCommonLabel();

    // 编辑
    JLabel brieflyLabel = getCommonLabel("briefly", Color.pink);
    JTextField brieflyValueLabel = getCommonEditor();
    JLabel relatedObjectLabel = getCommonLabel("relatedObject", Color.pink);
    JTextField relatedObjectValueLabel = getCommonEditor();
    JLabel trendLabel = getCommonLabel("trend", Color.pink);
    JTextField trendValueLabel = getCommonEditor();
    JLabel markedLabel = getCommonLabel("marked", Color.pink);
    JCheckBox markedValueLabel = getCommonCheckBox();
    JLabel remarkLabel = getCommonLabel("remark", Color.pink);
    JTextField remarkValueLabel = getCommonEditor();

    SimpleNewListPanel parent; // 以便调用持有者方法

    private NewEditorPanel(SimpleNewListPanel parent) {
        this.parent = parent;
        this.setLayout(new GridLayout(14, 2, 1, 1)); // 简易网格布局
        this.setPreferredSize(new Dimension(350, 500));

        this.add(idLabel);
        this.add(idValueLabel);

        this.add(dateTimeLabel);
        this.add(dateTimeValueLabel);

        this.add(saveTimeLabel);
        this.add(saveTimeValueLabel);

        this.add(lastModifiedLabel);
        this.add(lastModifiedValueLabel);

        this.add(titleLabel);
        this.add(titleValueLabel);

        this.add(urlLabel);
        this.add(urlValueLabel);

        this.add(detailTitleLabel);
        this.add(detailTitleValueLabel);

        this.add(typeLabel);
        this.add(typeValueLabel);

        this.add(urlRawHtmlLabel);
        this.add(urlRawHtmlValueLabel);

        this.add(brieflyLabel);
        this.add(brieflyValueLabel);

        this.add(relatedObjectLabel);
        this.add(relatedObjectValueLabel);

        this.add(trendLabel);
        this.add(trendValueLabel);


        this.add(remarkLabel);
        this.add(remarkValueLabel);

        this.add(markedLabel);
        this.add(markedValueLabel);
    }

    /**
     * 主刷新方法, 将被调用显示bean
     *
     * @param bean
     */
    public void update(SimpleNewEm bean) {
        this.bean = bean;
        this.update();
    }

    public static SimpleNewEm getEditedBean() {
        if (INSTANCE == null) {
            return null;
        }
        SimpleNewEm bean = INSTANCE.bean;
        if (bean == null) {
            return null;
        }
        bean.setLastModified(Timestamp.valueOf(DateUtil.date().toLocalDateTime()));
        bean.setBriefly(INSTANCE.brieflyValueLabel.getText());
        bean.setRelatedObject(INSTANCE.relatedObjectValueLabel.getText());
        try {
            bean.setTrend(Double.parseDouble(INSTANCE.trendValueLabel.getText()));
        } catch (NumberFormatException e) {
            // e.printStackTrace();
            log.warn("SimpleNewEm.trend: 解析为double失败, 请正确设置");
        }
        bean.setMarked(INSTANCE.markedValueLabel.isSelected());
        bean.setRemark(INSTANCE.remarkValueLabel.getText());
        return bean;
    }

    private static final Log log = LogUtil.getLogger();

    private static KeyAdapter buildKeyAdapterForEdit() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SimpleNewEm editedBean = getEditedBean();
                    SimpleNewEmDao.updateBean(editedBean);
                    INSTANCE.update(editedBean); // 将更新显示自动设置字段
//                    INSTANCE.parent.update();  // 该方式无法更新,
                    ((SimpleNewListPanel) NewsTabPanel.INSTANCE.getTabbedPane().getSelectedComponent()).update();
                    // 需要使用此方式进行更新
                }
            }
        };
    }

    @Override
    protected void update() {
        if (this.bean == null) {
            return;
        }
        idValueLabel.setText(String.valueOf(bean.getId()));
        setDateTimeOrNull(bean.getDateTime(), dateTimeValueLabel);
        setDateTimeOrNull(bean.getSaveTime(), saveTimeValueLabel);
        setDateTimeOrNull(bean.getLastModified(), lastModifiedValueLabel);

        titleValueLabel.setText(String.valueOf(bean.getTitle()));
        urlValueLabel.setText(String.valueOf(bean.getUrl()));
        detailTitleValueLabel.setText(String.valueOf(bean.getDetailTitle()));
        typeValueLabel.setText(String.valueOf(bean.getType()));
        urlRawHtmlValueLabel.setText(String.valueOf(bean.getUrlRawHtml()));


        brieflyValueLabel.setText(CommonUtil.toStringCheckNull(bean.getBriefly(), ""));
        relatedObjectValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRelatedObject(), ""));
        trendValueLabel.setText(CommonUtil.toStringCheckNull(bean.getTrend(), ""));
        markedValueLabel.setSelected(bean.getMarked());
        remarkValueLabel.setText(CommonUtil.toStringCheckNull(bean.getRemark(), ""));
    }

    public void setDateTimeOrNull(Timestamp timestamp, JLabel label) {
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

    public static JTextField getCommonEditor() {
        JTextField jTextField = new JTextField();
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit());
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
