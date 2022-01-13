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
 * description:
 *
 * @author: admin
 * @date: 2022/1/13/013-09:02:09
 */
@Setter
@Getter
public abstract class HorizontalFuncFrameS extends FuncFrameS {
    // 需要提供
    int autoMaxHight; // 自动增加可达到最大高度
    int autoMinHight; // 自动减小可达到最小高度
    double preferHeightScale; // 默认高度占父亲高度百分比
    int funcToolsWidth; // 按钮栏宽度

    // 自动初始化
    int preferHeight; // 主界面高度*倍率
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
    protected HorizontalFuncFrameS(JFrame mainWindow, String title,
                                boolean resizable, boolean closable, // JInternalFrame
                                boolean maximizable, boolean iconifiable,

                                int funcToolsWidth, double preferHeightScale, // 自身
                                int autoMinHight, int autoMaxHight) {
        super(mainWindow, OrientationType.HORIZONTAL, title, resizable, closable, maximizable, iconifiable);
        initAttrs(mainWindow, funcToolsWidth, preferHeightScale, autoMinHight, autoMaxHight);
        initCenterComponent(); // abstract
        initOtherChildren();
    }


    /**
     * 抽象方法, 创建核心中央组件, 以做子类区分
     */
    protected abstract void initCenterComponent();

    private void initAttrs(JFrame mainWindow, int funcToolsWidth, double preferHeightScale, int autoMinHight,
                           int autoMaxHight) {
        this.mainWindow = mainWindow; // TraderGui
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
        this.flushBounds(); // abstract
        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏
    }

    /**
     * 默认实现仅此3按钮, 子类可调用
     *
     * @return
     */
    protected List<JButton> getToolsButtons1() {
        HorizontalFuncFrameS frame = this;
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

    @Override
    public void flushBounds() {
        if (mainWindow instanceof TraderGui) {
            TraderGui mainWindowS = (TraderGui) mainWindow;
            this.preferHeight = (int) (this.mainWindow.getHeight() * preferHeightScale);
            this.setBounds(
                    //  x = 左侧边栏X + 左侧边栏宽度
                    mainWindowS.getCorePanel().getLeftTools().getX() + mainWindowS.getCorePanel().getLeftTools()
                            .getWidth() + mainWindowS
                            .getX(),
                    // y = 主界面底部 - 状态栏高度 - 底部栏高度 + 2(像素修正)
                    mainWindowS.getY() + mainWindowS.getHeight() - mainWindowS.getStatusBar().getHeight() - mainWindowS
                            .getCorePanel()
                            .getBottomTools().getHeight() - preferHeight + 2,
                    // 宽度 = 主宽 - (两个侧边栏宽之和)
                    mainWindowS.getWidth() - mainWindowS.getCorePanel().getLeftTools().getWidth() - mainWindowS
                            .getCorePanel()
                            .getRightTools().getWidth(),
                    preferHeight);
        } else {
            log.error("flushBounds 失败: 主界面非 TraderGui");
        }
    }
}
