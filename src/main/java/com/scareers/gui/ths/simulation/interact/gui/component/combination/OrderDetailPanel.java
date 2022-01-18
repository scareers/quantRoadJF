package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;

/**
 * description: Order对象 详情展示. 简单通过操作 label 显示
 *
 * @author: admin
 * @date: 2022/1/18/018-12:36:07
 */
@Getter
public class OrderDetailPanel extends Panel {
    JLabel label;

    public OrderDetailPanel() {
        this.setLayout(new BorderLayout());
        label = new JLabel("尚未选中订单");
//        label.setForeground(SettingsOfGuiGlobal.COLOR_GRAY_COMMON);
        label.setForeground(Color.green);
        label.setVerticalAlignment(JLabel.TOP);
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setBorder(BorderFactory.createLineBorder(Color.green));
//        label.setVerticalTextPosition(0);
        this.add(label, BorderLayout.WEST);
    }

    public void setText(String text) {
        this.label.setText(jsonStrToHtmlFormat(text));
    }
}
