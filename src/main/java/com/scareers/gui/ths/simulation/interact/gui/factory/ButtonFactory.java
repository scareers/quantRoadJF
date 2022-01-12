package com.scareers.gui.ths.simulation.interact.gui.factory;

import com.scareers.gui.ths.simulation.interact.gui.component.simple.JButtonV;

import javax.swing.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: 默认button 工厂
 *
 * @author: admin
 * @date: 2022/1/12/012-21:42:52
 */
public class ButtonFactory {
    /**
     * 默认设置的button
     *
     * @return
     */
    public static JButton getButton(String text, boolean vertical) {
        JButton button;
        if (vertical) {
            button = new JButtonV(text);
        } else {
            button = new JButton(text);
        }
        button.setBackground(COLOR_THEME_MINOR); // 次色
        button.setBorderPainted(false); // 无边框
        button.setForeground(COLOR_GRAY_COMMON); // 常态字灰
        button.setFocusPainted(false); // 去掉focus时文字框
        return button;
    }

    public static JButton getButton(String text) {
        return getButton(text, false);
    }

}
