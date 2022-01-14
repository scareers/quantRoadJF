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
 * description: 底部横向子功能栏界面, 继承自 JInternalFrame.
 *
 * @author: admin
 * @date: 2022/1/13/013-09:02:09
 */
@Setter
@Getter
public abstract class BottomFuncFrameS extends FuncFrameS {
    // 需要提供
    int autoMaxHight; // 自动增加可达到最大高度
    int autoMinHight; // 自动减小可达到最小高度
    double preferHeightScale; // 默认高度占主内容高度百分比
    int funcToolsWidth; // 按钮栏宽度

    // 自动初始化
    int preferHeight; // mainPane高度*倍率
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
    protected BottomFuncFrameS(TraderGui mainWindow, String title,
                               boolean resizable, boolean closable, // JInternalFrame
                               boolean maximizable, boolean iconifiable,
                               int funcToolsWidth, double preferHeightScale, // 自身
                               int autoMinHeight, int autoMaxHeight,
                               Integer layer
    ) {
        super(mainWindow, OrientationType.HORIZONTAL_BOTTOM, title, resizable, closable, maximizable, iconifiable);
        initAttrs(funcToolsWidth, preferHeightScale, autoMinHeight, autoMaxHeight);
        initCenterComponent(); // abstract
        initOtherChildren();

        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏
        this.mainPane.add(this, layer, 0);  //  JDesktopPane mainPane 放置
    }


    /**
     * 抽象方法, 创建核心中央组件, 以做子类区分
     */
    protected abstract void initCenterComponent();

    private void initAttrs(int funcToolsWidth, double preferHeightScale, int autoMinHight, int autoMaxHight) {
        this.funcToolsWidth = funcToolsWidth;
        this.preferHeightScale = preferHeightScale;
        this.preferHeight = (int) (this.mainWindow.getHeight() * preferHeightScale); // flushBounds()中重复调用.
        this.autoMinHight = autoMinHight;
        this.autoMaxHight = autoMaxHight;
    }


    protected void initOtherChildren() {
        ToolsPanel.ToolsPanelType toolsPanelType = ToolsPanel.ToolsPanelType.VERTICAL; // 横向功能对话框, 使用纵向工具栏
        funcTools = new ToolsPanel(funcToolsWidth, toolsPanelType,
                getToolsButtons1(), getToolsButtons2(),
                0, 0, 0, 0);
        this.add(funcTools, BorderLayout.WEST);
    }

    /**
     * 默认实现仅此3按钮, 子类可调用
     *
     * @return
     */
    protected List<JButton> getToolsButtons1() {
        BottomFuncFrameS frame = this;
        JButton resetBounds = ButtonFactory.getButton("置");
        resetBounds.setToolTipText("重置位置");

        resetBounds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.flushBounds();
            }
        });

        JButton higherButton = ButtonFactory.getButton("上");
        higherButton.setToolTipText("增大高度");

        higherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = frame.getBounds();
                int newHeight = Math.min(frame.getHeight() * 2, autoMaxHight);
                int difference = newHeight - frame.getHeight();
                if (difference <= 0) {
                    return;
                }
                frame.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference,
                        (int) rawbounds.getWidth(), newHeight);
            }
        });

        JButton shorterButton = ButtonFactory.getButton("下");
        shorterButton.setToolTipText("减小高度");

        shorterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = frame.getBounds();
                int newHeight = Math.max(frame.getHeight() / 2, autoMinHight);
                int difference = newHeight - frame.getHeight();
                if (difference >= 0) {
                    return;
                }
                frame.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference, // y-负数, 变大
                        (int) rawbounds.getWidth(), newHeight);
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
    public void flushBounds(boolean first) {
        if (first) { // 首次刷新, 将读取默认比例, 并计算最新高度! 并设置最新高度
            this.preferHeight = (int) (this.mainPane.getHeight() * preferHeightScale); // 需要更新默认高度
            actualFlush(preferHeight);
        } else {
            double oldScale = (double) this.getHeight() / this.mainPaneHeight; // 注意, 需要读取上次保存的 旧的mainPane尺寸
            int newHeight = (int) (oldScale * this.mainPane.getHeight()); // 新的尺寸计算, 等比缩放
            actualFlush(newHeight);
        }
        // 无论如何, 均需要刷新mainPane尺寸, 做下一次更新时的 "旧尺寸"
        this.mainPaneWidth = this.mainPane.getWidth();
        this.mainPaneHeight = this.mainPane.getHeight(); // 刷新manePane尺寸
    }

    private void actualFlush(int newHeight) {
        this.setBounds(
                //  x = 0, 已被 mainPane 管理
                0,
                // y = mainPane高度 - 自身高度
                mainPane.getHeight() - newHeight,
                // 宽度 = mainPane 同宽
                mainPane.getWidth(),
                newHeight);
    }

}
