package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import com.scareers.gui.ths.simulation.order.Order;

import javax.swing.*;
import java.awt.*;

/**
 * description: 用于展示单个 Order
 *
 * @author: admin
 * @date: 2022/1/18/018-09:01:15
 */
public class OrderDisplay extends JPanel {
    Order order;
    JLabel label;

    public OrderDisplay(Order order) {
        super(new BorderLayout());
        this.order = order;

        this.label = new JLabel();
        label.setText(order.toString());
        this.add(label, BorderLayout.CENTER);
    }


    public void flushOrder(Order order) {
        this.order = order;
        label.setText(order.toString());
    }
}
