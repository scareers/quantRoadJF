package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.date.DateUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.editor.FourPaperNewEditorPanel;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import org.jdesktop.swingx.JXList;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorTest;
import static javax.swing.SwingConstants.LEADING;
import static javax.swing.border.TitledBorder.DEFAULT_POSITION;

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

    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id", unique = true)
    Long id;
    @Column(name = "type", length = 64)
    String type; // 操盘 plan? 复盘 review? 见类型列表
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间;
    @Column(name = "dateStr", length = 32)
    String dateStr; // 简单日期字符串, 特指对某一天的观点.  一般将以此字段读取

    /*
总体评价字段
 */
    @Column(name = "trend")
    Double trend; // -1.0 - 1.0 总体利空利好偏向自定义
    @Column(name = "remark", columnDefinition = "longtext")
    String remark; // 总体备注

    @Column(name = "scoreSchemaOfPreJudgment", columnDefinition = "longtext")
    Double scoreSchemaOfPreJudgment; // 未来对总体预判的总评分

    /*
    4类因素
     */

    // 维护四大列表, 为了数据库便于维护, 不使用外键方式, 简单保存 jsonStr;
    // 这些字段均不保存到数据库
    @Transient
    List<String> goodPoints = new ArrayList<>(); // 利好因素
    @Transient
    List<String> badPoints = new ArrayList<>(); // 利空因素
    @Transient
    List<String> neutralPoints = new ArrayList<>(); // 中性因素
    @Transient
    List<String> otherPoints = new ArrayList<>(); // 其他因素

    // 与之对应的4大字符串. 这些字符串不手动设定, 当每次修改列表时, 将自动转换json, 自动设置!
    @Column(name = "goodPoints", columnDefinition = "longtext")
    String goodPointsJsonStr = "[]";
    @Column(name = "badPoints", columnDefinition = "longtext")
    String badPointsJsonStr = "[]";
    @Column(name = "neutralPoints", columnDefinition = "longtext")
    String neutralPointsJsonStr = "[]";
    @Column(name = "otherPoints", columnDefinition = "longtext")
    String otherPointsJsonStr = "[]";


    /*
    预判相关 + 未来对预判打分
     */
    // 核心字段: 预判看法列表 + 未来实际情景列表 + 对预判评分列表 + 评分解析列表; 它们将一一对应
    // 增加预判项目时, 自动添加(初始化)对应的 情景/评分/评分解析 项目. 始终维持 4个列表项目相同
    @Transient
    List<String> preJudgmentViews = new ArrayList<>(); // 预判观点, 核心字段
    @Column(name = "preJudgmentViews", columnDefinition = "longtext")
    String preJudgmentViewsJsonStr = "[]";
    @Transient
    List<String> futures = new ArrayList<>(); // 未来情况列表
    @Column(name = "futures", columnDefinition = "longtext")
    String futuresJsonStr = "[]";
    @Transient
    List<Double> scoresOfPreJudgment = new ArrayList<>(); // 未来对预判进行评分, 范围 -100.0 - 100.0; 默认0
    @Column(name = "scoresOfPreJudgment", columnDefinition = "longtext")
    String scoresOfPreJudgmentJsonStr = "[]"; // 未来情景描述, json字符串
    @Transient
    List<String> scoreReasons = new ArrayList<>(); // 如此评分的原因
    @Column(name = "scoreReasons", columnDefinition = "longtext")
    String scoreReasonsJsonStr = "[]"; // 未来情景描述, json字符串

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
    JTextField scoreSchemaOfPreJudgmentValueLabel = getCommonEditor(this); // scoreSchemaOfPreJudgment


    // 固定
    JLabel nameLabel = getCommonLabel("name"); // 三大时间
    JLabel nameValueLabel = getCommonLabel();
    JLabel quoteUrlLabel = getCommonLabel("quoteUrl");
    JLabel quoteUrlValueLabel = getCommonLabel();
    JLabel titleLabel = getCommonLabel("title");
    JLabel titleValueLabel = getCommonLabel();
    JLabel contentLabel = getCommonLabel("content");
    JLabel contentValueLabel = getCommonLabel();


    // 编辑
    JLabel markedLabel = getCommonLabel("marked", Color.pink);
    JCheckBox markedValueLabel = getCommonCheckBox();
    JLabel brieflyLabel = getCommonLabel("briefly", Color.pink);
    JTextField brieflyValueLabel = getCommonEditor(this);


    public NewAspectSummaryPanel() {
        initThreeContainerPanel();
        fillBaseInfoPanel(); // 将8字段相关控件, 放置于 baseInfoPanel
        fillPointsPanel(); // 资讯总结4类,
    }

    private void fillPointsPanel() {
        pointsPanel.setLayout(new GridLayout(2, 2, 1, 1)); // 3
        TitledBorder border = new TitledBorder("资讯汇总");
        border.setTitleColor(Color.red);
        pointsPanel.setBorder(border);

        pointsPanel.add(new JLabel("利好"));
        pointsPanel.add(new JLabel("利空"));
        pointsPanel.add(new JLabel("中性"));
        pointsPanel.add(new JLabel("其他"));
    }

    private void fillBaseInfoPanel() {
        baseInfoPanel.setLayout(new GridLayout(8, 2, 1, 1)); // 3
        baseInfoPanel.setPreferredSize(new Dimension(350, 400));
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
        baseInfoPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        pointsPanel = new JPanel();
        pointsPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        preJudgementPanel = new JPanel();
        preJudgementPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        this.add(baseInfoPanel, BorderLayout.WEST);

        JPanel jPanelCenter = new JPanel();
        jPanelCenter.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        jPanelCenter.add(pointsPanel);
        jPanelCenter.add(preJudgementPanel);
        this.add(jPanelCenter, BorderLayout.CENTER);
    }

    @Override
    public void update() {

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
