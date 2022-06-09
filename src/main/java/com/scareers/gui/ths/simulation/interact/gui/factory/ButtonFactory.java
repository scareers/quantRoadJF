package com.scareers.gui.ths.simulation.interact.gui.factory;

import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_GRAY_COMMON;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 功能栏按钮 工厂
 * // @key: 按钮默认有在focus时, space 按键将触发按钮点击和释放
 * JButton btn = new JButton("Test");
 * InputMap im = btn.getInputMap();
 * for (KeyStroke ik : im.allKeys()) {
 * System.out.println(ik + " = " + im.get(ik));
 * }
 * <p>
 * --> 打印
 * pressed SPACE = pressed
 * released SPACE = released
 * 取消默认的方法
 * <p>
 * Action blankAction = new AbstractAction() {
 *
 * @Override public void actionPerformed(ActionEvent e) {
 * }
 * };
 * <p>
 * ActionMap am = btn.getActionMap();
 * am.put("pressed", blankAction);
 * am.put("released", blankAction);
 * @author: admin
 * @date: 2022/1/12/012-21:42:52
 */
public class ButtonFactory {
    public static Action blankAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
        }
    };

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

        ActionMap am = button.getActionMap();
        am.put("pressed", blankAction);
        am.put("released", blankAction);
        return button;
    }

    public static FuncButton getButton(String text) {
        return getButton(text, false);
    }

}
