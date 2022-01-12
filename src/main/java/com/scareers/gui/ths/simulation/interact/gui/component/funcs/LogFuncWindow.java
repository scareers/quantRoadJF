package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.log.DisplayForLog;

import javax.swing.*;
import java.awt.*;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */
public class LogFuncWindow extends JDialog {
    public static int preferHeight = 200;
    OrientationType type;
    TraderGui parent;

    public LogFuncWindow(TraderGui owner, String title, OrientationType type) {
        super(owner, title, ModalityType.MODELESS); // 永不阻塞顶级窗口
        this.parent = owner; // 达成对 TraderGui 而非父类 owner Window 的访问
        this.type = type;
        init();
    }

    private void init() {
        DisplayForLog jDisplayForLog = new DisplayForLog();
        this.add(jDisplayForLog);
        // 注意, 起点(x,y) 应当+主窗口x,y, 因为setBounds本身是绝对定位
        this.setBounds(
                //  x = 左侧边栏X + 左侧边栏宽度 + 主窗口X
                parent.getCorePanel().getLeftTools().getX() + parent.getCorePanel().getLeftTools().getWidth(),
                // y = 从底部栏的 y - 默认高度 + 主窗口Y
                parent.getCorePanel().getBottomTools().getY() - preferHeight,
                // 宽度 = 主宽 - (两个侧边栏宽之和)
                parent.getWidth() - parent.getCorePanel().getLeftTools().getWidth() - parent.getCorePanel()
                        .getRightTools().getWidth(),
                preferHeight);
        this.setVisible(true);
    }

    public enum OrientationType {
        VERTICAL,
        HORIZONTAL
    }
}
