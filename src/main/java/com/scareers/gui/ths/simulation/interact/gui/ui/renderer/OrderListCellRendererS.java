package com.scareers.gui.ths.simulation.interact.gui.ui.renderer;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.trader.Trader;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;
import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.setLabelForeColorByOrderLifePoint;

/**
 * description: Order列表JList专用, 将显示 toolTip
 *
 * @author: admin
 * @date: 2022/1/18/018-11:19:31
 */
public class OrderListCellRendererS extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Order.OrderSimple orderSimple = (Order.OrderSimple) value;
        String toolTip = orderSimple.toToolTip();
        label.setToolTipText(jsonStrToHtmlFormat(toolTip)); // pretty json 可换行

        setLabelForeColorByOrderLifePoint(orderSimple.getOrder(), label);

        return label;
    }
}
