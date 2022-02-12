package com.scareers.gui.ths.simulation.interact.gui.component.combination.order;

import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.order.Order;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;
import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.setLabelForeColorByOrderLifePoint;

/**
 * description: Order对象 详情展示. 简单通过操作 label 显示
 *
 * @author: admin
 * @date: 2022/1/18/018-12:36:07
 */
@Getter
public class OrderDetailPanel extends JPanel {
    JLabel label;
    String preText = "";

    public OrderDetailPanel() {
        this.setLayout(new BorderLayout());
        label = new JLabel("尚未选中订单");
        label.setForeground(Color.green);
        label.setVerticalAlignment(JLabel.TOP);
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setBorder(BorderFactory.createLineBorder(Color.green));

        JScrollPane jScrollPane = new JScrollPane();
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setViewportView(label);
        jScrollPane.getViewport().setBackground(SettingsOfGuiGlobal.COLOR_THEME_MINOR);


        this.add(jScrollPane, BorderLayout.CENTER);


    }

    public void updateText(Order order) throws Exception {
        String newText = jsonStrToHtmlFormat(order.toStringPretty());
        if (!newText.equals(preText)) {
            this.label.setText(newText);
            setLabelForeColorByOrderLifePoint(order, label);
            preText = newText;
        }
    }
}
