package com.scareers.gui.ths.simulation.interact.gui.factory;

import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_GRAY_COMMON;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 功能栏按钮 工厂
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
    public static FuncButton getButton(String text, boolean vertical) {
        FuncButton button;
        if (vertical) {
            button = new FuncButton(text, FuncButton.BtnType.VERTICAL);
        } else {
            button = new FuncButton(text);
        }
        button.setBackground(COLOR_THEME_MINOR); // 次色
        button.setBorderPainted(false); // 无边框
        button.setForeground(COLOR_GRAY_COMMON); // 常态字灰
//        button.setFocusPainted(false); // 去掉focus时文字框
        return button;
    }

    public static FuncButton getButton(String text) {
        return getButton(text, false);
    }

}
