package com.scareers.gui.ths.simulation.interact.gui.component.core;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.createPlaceholderLabel;

/**
 * description: 核心Pane, BorderLayout布局, 左功能栏, 右功能栏, 下功能栏, 中间为 主功能(例如项目结构)+编辑器  JSplitPane 分割可调
 * 1. 3大功能栏 宽度(左右)  高度(下) 可调. 且下功能栏头尾使用占位 Label
 * 2. 左功能栏控制 JSplitPane 的左边控件 mainMenuPanel (主菜单Panel)
 * 3. 右下功能栏点击均显示 Dialog/JInternalFrame/其他自定义子功能控件, 实现相应功能.
 * 4. 详见6大控件属性
 *
 * @author: admin
 * @date: 2022/1/13/013-02:23:04
 */
@Getter
@Setter
public class CorePanel extends JDesktopPane {
    // 仅做占位符使用的 int, 设定为默认宽/高的值,无实际意义.应当保证控件最终渲染宽/高>此值.
    public static int placeholderWidthOrHeight = 100;
    public static int verticalToolsHGap1 = 0; // 本质是内部 2个 FlowLayout 两gap设定
    public static int verticalToolsVGap1 = 0;
    public static int verticalToolsHGap2 = 0;
    public static int verticalToolsVGap2 = 0;
    public static int horizontalToolsHGap1 = 0; // 本质是内部 2个 FlowLayout 两gap设定
    public static int horizontalToolsVGap1 = 0;
    public static int horizontalToolsHGap2 = 0;
    public static int horizontalToolsVGap2 = 0;

    // 需要传递的属性
    // @warning: 该属性不能名为 parent!, 否则于父类定义的属性冲突
    TraderGui mainWindow; // 主界面, 方便计算位置,
    int mainFuncPanelDefaultWidth; // 主功能实现区默认宽度
    int centerSplitPaneDividerSize; // 主分割面板分割线宽度
    int leftToolsWidth;
    int rightToolsWidth;
    int bottomToolsHeight;

    CopyOnWriteArrayList<JButton> leftToolsButtonsUp;
    CopyOnWriteArrayList<JButton> leftToolsButtonsDown; // 左侧工具栏下排按钮列表
    CopyOnWriteArrayList<JButton> rightToolsButtonsUp;
    CopyOnWriteArrayList<JButton> rightToolsButtonsDown;
    CopyOnWriteArrayList<JButton> bottomToolsButtonsPre;
    CopyOnWriteArrayList<JButton> bottomToolsButtonsAfter; // 底部工具栏后排按钮列表

    // 自动计算的属性
    JPanel leftTools; // 左功能区, 按钮列表
    JPanel rightTools; // 右功能区, 按钮列表
    JPanel bottomTools; // 下功能区, 按钮列表

    JPanel mainFuncPanel; // 左功能实现区, 常为树形菜单形式! 被 leftTools 按钮们控制
    JPanel mainDisplayPanel; // 主要展示区, 对应idea编辑器. Editor
    JSplitPane centerSplitPane; // 分开 mainMenuPanel + mainDisplayPanel, 宽度可调
    JDesktopPane mainPane; // 新增核心层级pane, 原 splitPane 置于其中, 约束值 100


    public CorePanel(int mainFuncPanelDefaultWidth, int centerSplitPaneDividerSize,
                     int leftToolsWidth, int rightToolsWidth, int bottomToolsHeight,

                     List<JButton> leftToolsButtonsUp,
                     List<JButton> leftToolsButtonsDown,

                     List<JButton> rightToolsButtonsUp,
                     List<JButton> rightToolsButtonsDown,

                     List<JButton> bottomToolsButtonsPre,
                     List<JButton> bottomToolsButtonsAfter,
                     TraderGui mainWindow) {
        super();
        this.mainWindow = mainWindow;
        this.mainFuncPanelDefaultWidth = mainFuncPanelDefaultWidth;
        this.centerSplitPaneDividerSize = centerSplitPaneDividerSize;
        this.leftToolsWidth = leftToolsWidth;
        this.rightToolsWidth = rightToolsWidth;
        this.bottomToolsHeight = bottomToolsHeight;

        this.leftToolsButtonsUp = new CopyOnWriteArrayList<>(leftToolsButtonsUp);
        this.leftToolsButtonsDown = new CopyOnWriteArrayList<>(leftToolsButtonsDown);
        this.rightToolsButtonsUp = new CopyOnWriteArrayList<>(rightToolsButtonsUp);
        this.rightToolsButtonsDown = new CopyOnWriteArrayList<>(rightToolsButtonsDown);
        this.bottomToolsButtonsPre = new CopyOnWriteArrayList<>(bottomToolsButtonsPre);
        this.bottomToolsButtonsAfter = new CopyOnWriteArrayList<>(bottomToolsButtonsAfter);

        initMainPane();
        initLeftTools();
        initRightTools();
        initBottomTools();

        this.setLayout(new BorderLayout());
        this.add(leftTools, BorderLayout.WEST);
        this.add(rightTools, BorderLayout.EAST);
        this.add(mainPane, BorderLayout.CENTER); // @2022/1/14已更新为层级 pane
        this.add(bottomTools, BorderLayout.SOUTH);
    }


    private void initLeftTools() {
        leftTools = new ToolsPanel(leftToolsWidth, ToolsPanel.ToolsPanelType.VERTICAL,
                leftToolsButtonsUp, leftToolsButtonsDown,
                verticalToolsHGap1, verticalToolsVGap1, verticalToolsHGap2, verticalToolsVGap2);

    }

    private void initRightTools() {
        rightTools = new ToolsPanel(rightToolsWidth, ToolsPanel.ToolsPanelType.VERTICAL,
                rightToolsButtonsUp, rightToolsButtonsDown,
                verticalToolsHGap1, verticalToolsVGap1, verticalToolsHGap2, verticalToolsVGap2);
    }

    private void initBottomTools() {
        bottomTools = new ToolsPanel(bottomToolsHeight, ToolsPanel.ToolsPanelType.HORIZONTAL,
                bottomToolsButtonsPre, bottomToolsButtonsAfter,
                horizontalToolsHGap1, horizontalToolsVGap1, horizontalToolsHGap2, horizontalToolsVGap2,
                createPlaceholderLabel(leftToolsWidth, bottomToolsHeight),
                createPlaceholderLabel(rightToolsWidth, bottomToolsHeight));
    }

    private void initMainPane() {
        mainFuncPanel = new JPanel();
        mainFuncPanel.setPreferredSize(new Dimension(mainFuncPanelDefaultWidth, placeholderWidthOrHeight)); // 定默认宽
        mainFuncPanel.setBackground(Color.yellow);

        mainDisplayPanel = new JPanel();
        mainDisplayPanel.setBackground(Color.green);


        centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // 设定为左右拆分布局
        centerSplitPane.setBorder(null);
        centerSplitPane.setOneTouchExpandable(true); // 让分割线显示出箭头
        centerSplitPane.setContinuousLayout(true); // 调整时实时重绘
        centerSplitPane.setDividerSize(centerSplitPaneDividerSize); //设置分割线的宽度
        centerSplitPane.setLeftComponent(mainFuncPanel);
        centerSplitPane.setRightComponent(mainDisplayPanel);
//        centerSplitPane

        mainPane = new JDesktopPane(); // 核心层级pane, 原 splitPane 放于其上, 层级为 100, 各窗口应当高于此.
//        mainPane.setBorder(null);

        mainPane.add(centerSplitPane, Integer.valueOf(100), 0);
    }


    /**
     * 刷新自身位置, 置于主界面主要位置. 当主界面窗口启动, 或者主界面大小改变时应当调用!
     * 注意调用时机
     *
     * @noti 主界面大小确定后, 控制核心Panel大小, 因使用 JDesktopPane, 其大小由 每层子组件大小确定.
     * 因此, 这里应当设定 mainPane的每层子组件的 大小! 首先刷新 centerSplitPane
     */
    public void flushMainPanelBounds() {
        // 因相对坐标, x,y=0; 只计算 主splitPane 宽高
        centerSplitPane.setLocation(0, 0);
        centerSplitPane.setSize(
                mainPane.getWidth(), // 因本方法后于它们渲染完成调用,有效
                mainPane.getHeight()); // 必须设定具体大小, 方可正常显示, 在主界面回调中修改
        System.out.println(mainPane.getBounds());
        System.out.println(centerSplitPane.getBounds());
        mainPane.repaint();
//        mainPane.setSize(
//                parent.getWidth() - leftTools.getWidth() - rightTools.getWidth(), // 因本方法后于它们渲染完成调用,有效
//                leftTools.getHeight()); // 必须设定具体大小, 方可正常显示, 在主界面回调中修改
    }


}
