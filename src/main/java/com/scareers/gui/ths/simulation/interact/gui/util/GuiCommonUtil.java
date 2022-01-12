package com.scareers.gui.ths.simulation.interact.gui.util;

import javax.swing.*;
import java.awt.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/13/013-02:29:20
 */
public class GuiCommonUtil {
    /**
     * 创建占位符Label, 空Label, 逻辑上占位作用
     *
     * @param width
     * @param height
     * @return
     */
    public static JLabel createPlaceholderLabel(int width, int height) {
        JLabel placeholderLabel = new JLabel();
        placeholderLabel.setSize(new Dimension(width, height));
        placeholderLabel.setPreferredSize(new Dimension(height, height));
        placeholderLabel.setBorder(null);
        return placeholderLabel;
    }
}
