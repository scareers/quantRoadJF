package com.scareers.gui.ths.simulation.interact.gui.ui.renderer;

import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.order.Order;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.jsonStrToHtmlFormat;

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
        String toolTip = ((Order.OrderSimple) value).toToolTip();
        label.setToolTipText(jsonStrToHtmlFormat(toolTip)); // pretty json 可换行
        return label;
    }
}
