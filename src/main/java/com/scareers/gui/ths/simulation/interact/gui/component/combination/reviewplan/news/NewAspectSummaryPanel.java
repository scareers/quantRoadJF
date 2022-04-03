package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.PlanReviewDateTimeDecider;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.news.bean.CompanyGoodNew;
import com.scareers.tools.stockplan.news.bean.NewAspectSummary;
import com.scareers.tools.stockplan.news.bean.dao.NewAspectSummaryDao;
import com.scareers.utils.CommonUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 新闻资讯面 个人总结!
 * 分为3部分: 3个Panel 左上下
 * 1.显示基本字段, 包含2总体评价字段
 * 2.显示 利好/利空/中性/其他 新闻总结
 * 3.显示 自己预判观点 以及未来打分
 *
 * @author: admin
 * @date: 2022/3/17/017-19:09:17
 */
public class NewAspectSummaryPanel extends DisplayPanel {


    NewAspectSummary bean;

    JPanel baseInfoPanel; // 基本字段
    JPanel pointsPanel; // 利好利空中性其他 消息总结
    JPanel preJudgementPanel; // 预判相关

    // 1.基本字段展示: 8个简单字段; 放于 baseInfoPanel
    JLabel idLabel = getCommonLabel("id");  // id
    JLabel idValueLabel = getCommonLabel();
    JLabel typeLabel = getCommonLabel("type");  // type
    JLabel typeValueLabel = getCommonLabel();

    JLabel dateStrLabel = getCommonLabel("dateStr"); // 虽字符串但重要
    JLabel dateStrValueLabel = getCommonLabel();
    JLabel saveTimeLabel = getCommonLabel("saveTime");
    JLabel saveTimeValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified"); // 编辑后自动设定
    JLabel lastModifiedValueLabel = getCommonLabel();

    JLabel trendLabel = getCommonLabel("trend", Color.pink);
    JTextField trendValueLabel = getCommonEditor(this);
    JLabel remarkLabel = getCommonLabel("remark", Color.pink);
    JTextField remarkValueLabel = getCommonEditor(this);
    JLabel scoreSchemaOfPreJudgmentLabel = getCommonLabel("预判评分[未来设置]", Color.red); // 未来设置
    JLabel scoreSchemaOfPreJudgmentValueLabel = getCommonLabel(); // scoreSchemaOfPreJudgment


    public NewAspectSummaryPanel() {
        this.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        initBean(); // todo: 将被抽象方法,
        initThreeContainerPanel();
        fillBaseInfoPanel(); // 将8字段相关控件, 放置于 baseInfoPanel
        fillPointsPanel(); // 资讯总结4类,

        fillPreJudgementPanel();

        this.update();
    }

    private void fillPreJudgementPanel() {
        preJudgementPanel.setLayout(new BorderLayout()); // 3
        TitledBorder border = new TitledBorder("资讯预判");
        border.setTitleColor(Color.red);
        preJudgementPanel.setBorder(border);

        preJudgementPanel.add(new PreJudgementListTOfNewPanel(this, this.bean), BorderLayout.CENTER);
    }

    public void initBean() {
        try {
            this.bean = NewAspectSummaryDao.getOrInitBeanForPlan(PlanReviewDateTimeDecider.getUniqueDatetime());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fillPointsPanel() {
        pointsPanel.setLayout(new GridLayout(2, 2, 1, 1)); // 3
        TitledBorder border = new TitledBorder("资讯汇总");
        border.setTitleColor(Color.red);
        pointsPanel.setBorder(border);

        pointsPanel.add(new NewPointsPanel(this, NewAspectSummary.POINT_TYPE_GOOD, bean));
        pointsPanel.add(new NewPointsPanel(this, NewAspectSummary.POINT_TYPE_BAD, bean));
        pointsPanel.add(new NewPointsPanel(this, NewAspectSummary.POINT_TYPE_NEUTRAL, bean));
        pointsPanel.add(new NewPointsPanel(this, NewAspectSummary.POINT_TYPE_OTHER, bean));
    }

    private void fillBaseInfoPanel() {
        baseInfoPanel.setLayout(new GridLayout(8, 2, 1, 1)); // 3
        baseInfoPanel.setPreferredSize(new Dimension(350, 320));
        TitledBorder border = new TitledBorder("基本信息");
        border.setTitleColor(Color.red);
        baseInfoPanel.setBorder(border);

        baseInfoPanel.add(idLabel);
        baseInfoPanel.add(idValueLabel);

        baseInfoPanel.add(typeLabel);
        baseInfoPanel.add(typeValueLabel);

        baseInfoPanel.add(dateStrLabel);
        baseInfoPanel.add(dateStrValueLabel);

        baseInfoPanel.add(saveTimeLabel);
        baseInfoPanel.add(saveTimeValueLabel);

        baseInfoPanel.add(lastModifiedLabel);
        baseInfoPanel.add(lastModifiedValueLabel);

        baseInfoPanel.add(trendLabel);
        baseInfoPanel.add(trendValueLabel);

        baseInfoPanel.add(remarkLabel);
        baseInfoPanel.add(remarkValueLabel);

        baseInfoPanel.add(scoreSchemaOfPreJudgmentLabel);
        baseInfoPanel.add(scoreSchemaOfPreJudgmentValueLabel);

    }

    private void initThreeContainerPanel() {
        this.setLayout(new BorderLayout());
        baseInfoPanel = new JPanel();
        pointsPanel = new JPanel();
        preJudgementPanel = new JPanel();

        JPanel panelTemp = new JPanel();
        panelTemp.setLayout(new BorderLayout());
        panelTemp.add(baseInfoPanel, BorderLayout.NORTH); // 包装一下, 形状不变
        panelTemp.add(new JPanel(), BorderLayout.CENTER); // 必须占位
        this.add(panelTemp, BorderLayout.WEST);

        JPanel jPanelCenter = new JPanel();
        jPanelCenter.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        jPanelCenter.add(pointsPanel);
        jPanelCenter.add(preJudgementPanel);
        this.add(jPanelCenter, BorderLayout.CENTER);
    }

    public void update(NewAspectSummary bean) {
        this.bean = bean;
        this.update();
    }

    @Override
    public void update() {
        if (this.bean == null) {
            return;
        }
        idValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getId()));
        typeValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getType()));
        dateStrValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getDateStr()));
        setDateTimeOrNull(this.bean.getGeneratedTime(), saveTimeValueLabel);
        setDateTimeOrNull(this.bean.getLastModified(), lastModifiedValueLabel);

        trendValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getTrend()));
        remarkValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getRemark()));
        scoreSchemaOfPreJudgmentValueLabel.setText(CommonUtil.toStringCheckNull(this.bean.getScoresOfPreJudgment()));

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
            NewAspectSummaryPanel panel) {
        JTextField jTextField = new JTextField();
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);
        jTextField.setBackground(Color.black);
        jTextField.setCaretColor(Color.red);
        jTextField.addKeyListener(buildKeyAdapterForEdit(panel, "大势总结"));
        return jTextField;
    }

    private static KeyAdapter buildKeyAdapterForEdit(NewAspectSummaryPanel panel, String pointType) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 按下回车, 自动保存当前bean. null时忽略
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    tryAutoSaveEditedBean(panel, pointType);
                }
            }
        };
    }

    public NewAspectSummary getEditedBean() {
        if (this.bean == null) {
            return null;
        }
        bean.setLastModified(Timestamp.valueOf(DateUtil.date().toLocalDateTime()));
        try {
            bean.setTrend(Double.parseDouble(this.trendValueLabel.getText()));
        } catch (Exception e) {
        }
        bean.setRemark(this.remarkValueLabel.getText());
        return bean;
    }

    public static void tryAutoSaveEditedBean(NewAspectSummaryPanel panel, String logPrefix) {
        try {
            NewAspectSummary editedBean = panel.getEditedBean();
            NewAspectSummaryDao.saveOrUpdateBean(editedBean);
            panel.update(editedBean);
            ManiLog.put(StrUtil.format("{}: 更新成功: {} --> {}", logPrefix,
                    panel.bean.getId(), panel.bean.getDateStr()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static JCheckBox getCommonCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        checkBox.setBackground(COLOR_THEME_MINOR);
        checkBox.setForeground(Color.pink);
        return checkBox;
    }
}
