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
        ManipulateLogWindow.put(rawContent, color, bold);
    }

    public static void put(String rawContent) {
        ManipulateLogWindow.put(rawContent, Color.white, false);
    }

    public static void put(String rawContent, Color color) {
        ManipulateLogWindow.put(rawContent, color, false);
    }

    public static void put(String rawContent, boolean bold) {
        ManipulateLogWindow.put(rawContent, Color.white, false);
    }
}
