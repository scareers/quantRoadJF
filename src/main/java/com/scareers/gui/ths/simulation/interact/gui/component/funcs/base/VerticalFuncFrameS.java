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
 * description: 垂直窗口. 常用于右侧工具栏
 *
 * @author: admin
 * @date: 2022/1/13/013-09:02:09
 */
@Setter
@Getter
public abstract class VerticalFuncFrameS extends FuncFrameS {
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
    protected VerticalFuncFrameS(TraderGui mainWindow, String title,
                                 boolean resizable, boolean closable, // JInternalFrame
                                 boolean maximizable, boolean iconifiable,
                                 int funcToolsHeight, double preferWidthScale, // 自身
                                 int autoMinWidth, int autoMaxWidth,
                                 Integer layer
    ) {
        this(mainWindow, title,
                resizable, closable, // JInternalFrame
                maximizable, iconifiable,
                funcToolsHeight, preferWidthScale, // 自身
                autoMinWidth, autoMaxWidth,
                layer, true); // 默认注册到mainPane
    }

    protected VerticalFuncFrameS(TraderGui mainWindow, String title,
                                 boolean resizable, boolean closable, // JInternalFrame
                                 boolean maximizable, boolean iconifiable,
                                 int funcToolsHeight, double preferWidthScale, // 自身
                                 int autoMinWidth, int autoMaxWidth,
                                 Integer layer, boolean addToMainPane
    ) {
        super(mainWindow, OrientationType.VERTICAL, title, resizable, closable, maximizable, iconifiable);
        initAttrs(funcToolsHeight, preferWidthScale, autoMinWidth, autoMaxWidth);
        initCenterComponent(); // abstract
        initOtherChildren();

        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏
        if (addToMainPane) { // 可暂时不注册
            this.mainPane.add(this, layer, 0);  //  JDesktopPane mainPane 放置
        }
        this.flushBounds(); // abstract
    }


    /**
     * 抽象方法, 创建核心中央组件, 以做子类区分
     */
    protected abstract void initCenterComponent();

    protected void initAttrs(int funcToolsHeight, double preferWidthScale, int autoMinWidth, int autoMaxWidth) {
        this.funcToolsHeight = funcToolsHeight;
        this.preferWidthScale = preferWidthScale;
        this.preferWidth = (int) (this.mainWindow.getHeight() * preferWidthScale); // flushBounds()中重复调用.
        this.autoMinWidth = autoMinWidth;
        this.autoMaxWidth = autoMaxWidth;
    }


    protected void initOtherChildren() {
        ToolsPanel.ToolsPanelType toolsPanelType = ToolsPanel.ToolsPanelType.HORIZONTAL;
        funcTools = new ToolsPanel(funcToolsHeight, toolsPanelType,
                getToolsButtons1(), getToolsButtons2(),
                0, 0, 0, 0);
        this.add(funcTools, BorderLayout.NORTH);
    }

    /**
     * 默认实现仅此3按钮, 子类可调用
     *
     * @return
     */
    protected List<JButton> getToolsButtons1() {
        VerticalFuncFrameS frame = this;
        JButton resetBounds = ButtonFactory.getButton("置");
        resetBounds.setToolTipText("重置位置");

        resetBounds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.flushBounds();
            }
        });

        JButton higherButton = ButtonFactory.getButton("左");
        higherButton.setToolTipText("增大宽度");

        higherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = frame.getBounds();
                int newWidth = Math.min(frame.getWidth() * 2, autoMaxWidth);
                int difference = newWidth - frame.getWidth();
                if (difference <= 0) {
                    return;
                }
                frame.setBounds(frame.getX() - difference, frame.getY(), newWidth, frame.getHeight());
            }
        });

        JButton shorterButton = ButtonFactory.getButton("右");
        shorterButton.setToolTipText("减小宽度");

        shorterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = frame.getBounds();
                int newWidth = Math.max(frame.getWidth() / 2, autoMinWidth);
                int difference = newWidth - frame.getWidth();
                if (difference >= 0) {
                    return;
                }
                frame.setBounds(frame.getX() - difference, frame.getY(), newWidth, frame.getHeight());
            }
        });
        return Arrays.asList(resetBounds, higherButton, shorterButton);
    }

    protected List<JButton> getToolsButtons2() {
        return Arrays.asList();
    }

    /**
     * 刷新位置, 注意, 自身已经加入 主面板 JDesktopPane 的某一层-0;
     * 因此位置需要依据 mainPane 当前情况刷新
     */
    @Override
    public void flushBounds() {
        this.preferWidth = (int) (this.mainPane.getWidth() * preferWidthScale);
        this.setBounds(
                //  x = mainPane宽度 - 自身宽度
                mainPane.getWidth() - preferWidth,
                // y = 0
                0,
                preferWidth,
                // 高度 = mainPane 高度
                mainPane.getHeight());
    }
}
