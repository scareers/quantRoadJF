package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news.SimpleNewListPanel;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
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

    // todo: 具体展示
    JLabel relatedConceptListLabel = getCommonLabel("relatedConceptList"); // 编辑后自动设定
    JLabel relatedConceptListValueLabel = getCommonLabel();
    JLabel relatedIndustryListLabel = getCommonLabel("relatedIndustryList"); // 编辑后自动设定
    JLabel relatedIndustryListValueLabel = getCommonLabel();
    JLabel includeStockListLabel = getCommonLabel("includeStockList"); // 编辑后自动设定
    JLabel includeStockListValueLabel = getCommonLabel();

    JLabel leaderStockListLabel = getCommonLabel("leaderStockList", Color.pink); // 龙头股编辑
    JTextField leaderStockListValueLabel = getCommonEditor(this);

    // 编辑
    JLabel pricePositionShortTermLabel = getCommonLabel("pricePositionShortTerm", Color.pink);
    JTextField pricePositionShortTermValueLabel = getCommonEditor(this);
    JLabel pricePositionLongTermLabel = getCommonLabel("pricePositionLongTerm", Color.pink);
    JTextField pricePositionLongTermValueLabel = getCommonEditor(this);
    JLabel priceTrendLabel = getCommonLabel("priceTrend", Color.pink);
    JTextField priceTrendValueLabel = getCommonEditor(this);
    JLabel oscillationAmplitudeLabel = getCommonLabel("oscillationAmplitude", Color.pink);
    JTextField oscillationAmplitudeValueLabel = getCommonEditor(this);
    JLabel lineTypeLabel = getCommonLabel("lineType", Color.pink);
    JTextField lineTypeValueLabel = getCommonEditor(this);
    JLabel hypeReasonLabel = getCommonLabel("hypeReason", Color.pink);
    JTextField hypeReasonValueLabel = getCommonEditor(this);
    JLabel hypeStartDateLabel = getCommonLabel("hypeStartDate", Color.pink); // todo: 日期控件
    JTextField hypeStartDateValueLabel = getCommonEditor(this);
    JLabel hypePhaseCurrentLabel = getCommonLabel("hypePhaseCurrent", Color.pink);
    JTextField hypePhaseCurrentValueLabel = getCommonEditor(this);
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
        this.add(relatedConceptListValueLabel);

        this.add(relatedIndustryListLabel);
        this.add(relatedIndustryListValueLabel);

        this.add(includeStockListLabel);
        this.add(includeStockListValueLabel);

        this.add(pricePositionShortTermLabel);
        this.add(pricePositionShortTermValueLabel);

        this.add(pricePositionLongTermLabel);
        this.add(pricePositionLongTermValueLabel);

        this.add(priceTrendLabel);
        this.add(priceTrendValueLabel);

        this.add(oscillationAmplitudeLabel);
        this.add(oscillationAmplitudeValueLabel);

        this.add(lineTypeLabel);
        this.add(lineTypeValueLabel);

        this.add(hypeReasonLabel);
        this.add(hypeReasonValueLabel);

        this.add(hypeStartDateLabel);
        this.add(hypeStartDateValueLabel);

        this.add(hypePhaseCurrentLabel);
        this.add(hypePhaseCurrentValueLabel);

        this.add(specificDescriptionLabel);
        this.add(specificDescriptionValueLabel);

        this.add(leaderStockListLabel);
        this.add(leaderStockListValueLabel);

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


        bean.setLastModified(DateUtil.date());
//        bean.setBriefly(this.brieflyValueLabel.getText());
//        bean.setRelatedObject(this.relatedObjectValueLabel.getText());
        try {
            bean.setTrend(Double.parseDouble(this.trendValueLabel.getText()));
        } catch (NumberFormatException e) {
            // e.printStackTrace();
            log.warn("SimpleNewEm.trend: 解析为double失败, 请正确设置");
        }
//        bean.setMarked(this.markedValueLabel.isSelected());
        bean.setRemark(this.remarkValueLabel.getText());
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
        pricePositionShortTermValueLabel.setText(String.valueOf(bean.getPricePositionShortTerm()));
        pricePositionLongTermValueLabel.setText(String.valueOf(bean.getPricePositionLongTerm()));
        priceTrendValueLabel.setText(String.valueOf(bean.getPriceTrend()));
        oscillationAmplitudeValueLabel.setText(String.valueOf(bean.getOscillationAmplitude()));
        lineTypeValueLabel.setText(String.valueOf(bean.getLineType()));
        hypeReasonValueLabel.setText(String.valueOf(bean.getHypeReason()));
        setDateTimeOrNull(bean.getHypeStartDate(), hypeStartDateValueLabel); // todo
        hypePhaseCurrentValueLabel.setText(String.valueOf(bean.getHypePhaseCurrent()));
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
}
