package com.scareers.gui.ths.simulation.interact.gui.util;

import com.scareers.gui.ths.simulation.interact.gui.component.funcs.ManipulateLogWindow;

import java.awt.*;

/**
 * description: 手动操作的log记录; 调用 Manipulate4LogWindow 相关方法
 *
 * @author: admin
 * @date: 2022/3/13/013-18:19:13
 */
public class ManiLog {
    public static void put(String rawContent, Color color, boolean bold) {
        try {
            ManipulateLogWindow.put(rawContent, color, bold);
        } catch (Exception e) {

        }
    }

    public static void put(String rawContent) {
        try {
            ManipulateLogWindow.put(rawContent, Color.white, false);
        } catch (Exception e) {

        }
    }

    public static void put(String rawContent, Color color) {
        try {
            ManipulateLogWindow.put(rawContent, color, false);
        } catch (Exception e) {

        }
    }

    public static void put(String rawContent, boolean bold) {
        try {
            ManipulateLogWindow.put(rawContent, Color.white, false);
        } catch (Exception e) {

        }
    }
}
