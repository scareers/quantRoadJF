package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.core.ToolsPanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * description: 左工具栏 垂直窗口. 继承垂直窗口, idea的该类窗口, width应当占满整个pane, 处于最底层! layer默认为 50.
 *
 * @author: admin
 * @date: 2022/1/13/013-09:02:09
 */
@Setter
@Getter
public abstract class LeftFuncFrameS extends VerticalFuncFrameS {
    // 需要提供
    int autoMaxWidth;
    int autoMinWidth;
    double preferWidthScale;
    int funcToolsHeight;

    // 自动初始化
    int preferWidth; // mainPane 宽度*倍率
    ToolsPanel funcTools; // 工具按钮组


    // 抽象方法实现初始化
    protected Component centerComponent; // 主内容, 若调用特殊方法, 应当强制转型后调用


    /**
     * 全参构造器, 方向确定为水平
     *
     * @param mainWindow
     * @param title
     * @param resizable
     * @param closable
     * @param maximizable
     * @param iconifiable
     */
    protected LeftFuncFrameS(TraderGui mainWindow, String title,
                             boolean resizable, boolean closable, // JInternalFrame
                             boolean maximizable, boolean iconifiable,
                             int funcToolsWidth, double preferHeightScale, // 自身
                             int autoMinHight, int autoMaxHight,
                             Integer layer
    ) {
        super(mainWindow, title,
                resizable, closable, // JInternalFrame
                maximizable, iconifiable,
                funcToolsWidth, 1, // 自身
                autoMinHight, autoMaxHight,
                50);
    }


}
