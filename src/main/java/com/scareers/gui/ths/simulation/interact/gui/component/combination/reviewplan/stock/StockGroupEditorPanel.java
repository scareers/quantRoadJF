package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.stock;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.tools.stockplan.stock.bean.StockGroupOfPlan;

import javax.swing.*;
import java.awt.*;

/**
 * description: 股票组编辑panel; 使用问财选股编辑成分股
 *
 * @author: admin
 * @date: 2022/4/10/010-11:56:12
 */
public class StockGroupEditorPanel extends DisplayPanel {
    StockGroupOfPlan bean;

    // 股票组属性显示
    // 1.不可手动修改的属性, bean null时空
    JLabel idLabel = getCommonLabel("id");
    JLabel idValueLabel = getCommonLabel();
    JLabel dateStrLabel = getCommonLabel("dateStr");
    JLabel dateStrValueLabel = getCommonLabel();
    JLabel generatedTimeLabel = getCommonLabel("generatedTime");
    JLabel generatedTimeLabelValueLabel = getCommonLabel();
    JLabel lastModifiedLabel = getCommonLabel("lastModified");
    JLabel lastModifiedValueLabel = getCommonLabel();

    // 2. 两可编辑属性
    JLabel nameLabel = getCommonLabel("name");
    JTextField nameValueJTextField = getCommonEditorOfStockGroup(this);
    JLabel descriptionLabel = getCommonLabel("description");
    JTextField descriptionValueJTextField = getCommonEditorOfStockGroup(this);

    public StockGroupEditorPanel(StockGroupOfPlan bean) {
        this.bean = bean;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        // 1.北边方grid 简单字段
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        JPanel simpleAttrPanel = initSimpleAttrPanel();
        northPanel.add(simpleAttrPanel, BorderLayout.WEST);
        northPanel.add(new JPanel(), BorderLayout.CENTER); // 占位

        // 2.中间, 成分股编辑器!


    }

    private JPanel initSimpleAttrPanel() {
        JPanel simpleAttrPanel = new JPanel();
        simpleAttrPanel.setLayout(new GridLayout(6, 2, -1, -1));
        simpleAttrPanel.setPreferredSize(new Dimension(400, 200));

        simpleAttrPanel.add(idLabel);
        simpleAttrPanel.add(idValueLabel);

        simpleAttrPanel.add(dateStrLabel);
        simpleAttrPanel.add(dateStrValueLabel);
        simpleAttrPanel.add(generatedTimeLabel);
        simpleAttrPanel.add(generatedTimeLabelValueLabel);
        simpleAttrPanel.add(lastModifiedLabel);
        simpleAttrPanel.add(lastModifiedValueLabel);
        simpleAttrPanel.add(nameLabel);
        simpleAttrPanel.add(nameValueJTextField);
        simpleAttrPanel.add(descriptionLabel);
        simpleAttrPanel.add(descriptionValueJTextField);

        return simpleAttrPanel;
    }

    @Override
    public void update() {

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

    /**
     * trend编辑器的 回调函数有所不同, 除了自动保存外,
     * 还应该 自动更新 所有bean 的  relatedTrendMap/relatedTrendsDiscount 三个字段; 随后进行全量保存
     *
     * @param isTrendEditor
     * @param panel
     * @return
     */
    public static JTextField getCommonEditorOfStockGroup(StockGroupEditorPanel panel) {
        JTextField jTextField = new JTextField();
        jTextField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        jTextField.setForeground(Color.red);

        jTextField.setBackground(Color.BLACK);
        jTextField.setForeground(Color.red);
        jTextField.setCaretColor(Color.red);
        return jTextField;
    }
}
