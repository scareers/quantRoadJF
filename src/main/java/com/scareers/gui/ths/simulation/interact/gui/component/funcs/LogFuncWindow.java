package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import javax.swing.*;
import java.awt.*;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */
public class LogFuncWindow extends JDialog {
    OrientationType type;

    public LogFuncWindow(Window owner, String title, OrientationType type) {
        super(owner, title, ModalityType.MODELESS); // 永不阻塞顶级窗口
        this.type = type;
        init();
    }

    private void init() {


    }

    public static enum OrientationType {
        VERTICAL,
        HORIZONTAL
    }
}
