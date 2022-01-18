package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import cn.hutool.json.JSONUtil;
import com.scareers.gui.ths.simulation.Response;
import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;

/**
 * description: Order对象 详情展示. 简单通过操作 label 显示
 *
 * @author: admin
 * @date: 2022/1/18/018-12:36:07
 */
@Getter
public class OrderResponsePanel extends JPanel {
    JLabel label;
    String preText = ""; // 是否有必要更新?

    public OrderResponsePanel() {
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

    public void updateText(Order order) {
        List<Response> responses = Trader.getOrdersAllMap().get(order);
        String newText = jsonStrToHtmlFormat(JSONUtil.toJsonPrettyStr(responses));

        if (!newText.equals(preText)) {
            this.label.setText(newText);
            if (responses.size() > 0) {
                String state = responses.get(responses.size() - 1).getStr("state");
                if (state.equals("success")) {
                    label.setForeground(Color.green);
                } else if (state.equals("fail")) {
                    label.setForeground(Color.red);
                } else {
                    label.setForeground(Color.gray);
                }
            } else {
                label.setForeground(Color.gray);
            }

            this.preText = newText;
        }
    }
}