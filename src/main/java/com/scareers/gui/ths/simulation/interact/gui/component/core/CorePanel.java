package com.scareers.gui.ths.simulation.interact.gui.component.core;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.createPlaceholderLabel;

/**
 * description: 核心 Pane, BorderLayout布局, 左功能栏, 右功能栏, 下功能栏, + 编辑区.
 * 1. 实例化时, 仅实例化 3大功能栏. + JDesktopPane(层级布局,位于CENTER),
 * 2. 主编辑区于 主界面windowOpened 后回调添加. 3方功能栏均于 按钮回调形式添加;  因此最初状态, 整块CENTER区空白.
 * 3. 3大功能栏 宽度(左右)  高度(下) 可调. 且下功能栏头尾使用占位 Label
 * 4. 功能栏按钮, 钙能按钮, 以及编辑区MainDisplayWindow , 均使用 FuncFrameS extends JInternalFrame, 配合 JDesktopPane
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
    int leftToolsWidth;
    int rightToolsWidth;
    int bottomToolsHeight;

    // @noti: 所有功能栏, 均由 以下12列表 以及 ConcurrentHashMap 功能池控制访问,修改等
    // 6大功能栏按钮列表, 均可变. 达成按钮可在侧边栏拖动的效果. 且可以动态添加按钮等.
    CopyOnWriteArrayList<FuncButton> leftToolsButtonsUp;
    CopyOnWriteArrayList<FuncButton> leftToolsButtonsDown; // 左侧工具栏下排按钮列表,
    CopyOnWriteArrayList<FuncButton> rightToolsButtonsUp;
    CopyOnWriteArrayList<FuncButton> rightToolsButtonsDown;
    CopyOnWriteArrayList<FuncButton> bottomToolsButtonsPre;
    CopyOnWriteArrayList<FuncButton> bottomToolsButtonsAfter; // 底部工具栏后排按钮列表

    // 与按钮对应的, 功能栏列表, 构造器仅传递buttons, 初始功能实现框为空列表
    CopyOnWriteArraySet<FuncFrameS> leftTopfuncFrames = new CopyOnWriteArraySet<>();
    CopyOnWriteArraySet<FuncFrameS> leftBottomFrames = new CopyOnWriteArraySet<>();
    CopyOnWriteArraySet<FuncFrameS> rightTopfuncFrames = new CopyOnWriteArraySet<>();
    CopyOnWriteArraySet<FuncFrameS> rightBottomFrames = new CopyOnWriteArraySet<>();
    CopyOnWriteArraySet<FuncFrameS> bottomLeftfuncFrames = new CopyOnWriteArraySet<>();
    CopyOnWriteArraySet<FuncFrameS> bottomRightFrames = new CopyOnWriteArraySet<>();

    // key:value -> 各大按钮对象:其对应功能栏, 均使用单例模式! 自行完成状态变化. 注意 FuncButton的equals和hashCode未曾重写过
    ConcurrentHashMap<FuncButton, FuncFrameS> existFuncFrames = new ConcurrentHashMap<>();

    // 自动计算的属性
    ToolsPanel leftTools; // 左功能区, 按钮列表
    ToolsPanel rightTools; // 右功能区, 按钮列表
    ToolsPanel bottomTools; // 下功能区, 按钮列表

    JDesktopPane mainPane; // 新增核心层级pane, 原 splitPane 置于其中, 约束值 100
    MainDisplayWindow mainDisplayWindow; // 该属性应当实例化后, 在主界面 WindowOpened 回调时实例化添加

    public CorePanel(int leftToolsWidth, int rightToolsWidth, int bottomToolsHeight,
                     List<FuncButton> leftToolsButtonsUp,
                     List<FuncButton> leftToolsButtonsDown,

                     List<FuncButton> rightToolsButtonsUp,
                     List<FuncButton> rightToolsButtonsDown,

                     List<FuncButton> bottomToolsButtonsPre,
                     List<FuncButton> bottomToolsButtonsAfter,
                     TraderGui mainWindow) {
        super();
        this.mainWindow = mainWindow;
        this.leftToolsWidth = leftToolsWidth;
        this.rightToolsWidth = rightToolsWidth;
        this.bottomToolsHeight = bottomToolsHeight;

        this.leftToolsButtonsUp = new CopyOnWriteArrayList<>(leftToolsButtonsUp);
        this.leftToolsButtonsDown = new CopyOnWriteArrayList<>(leftToolsButtonsDown);
        this.rightToolsButtonsUp = new CopyOnWriteArrayList<>(rightToolsButtonsUp);
        this.rightToolsButtonsDown = new CopyOnWriteArrayList<>(rightToolsButtonsDown);
        this.bottomToolsButtonsPre = new CopyOnWriteArrayList<>(bottomToolsButtonsPre);
        this.bottomToolsButtonsAfter = new CopyOnWriteArrayList<>(bottomToolsButtonsAfter);

        initMainPane(); // JDesktopPane, 层级控制
        initLeftTools();
        initRightTools();
        initBottomTools();

        this.setLayout(new BorderLayout());
        this.add(leftTools, BorderLayout.WEST);
        this.add(rightTools, BorderLayout.EAST);
        this.add(mainPane, BorderLayout.CENTER); // @2022/1/14已更新为层级 pane
        this.add(bottomTools, BorderLayout.SOUTH);

        addListeners();
    }


    private void initMainPane() {
        mainPane = new JDesktopPane(); // 核心层级pane
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

    /**
     * 注意传递了占位控件
     */
    private void initBottomTools() {
        bottomTools = new ToolsPanel(bottomToolsHeight, ToolsPanel.ToolsPanelType.HORIZONTAL,
                bottomToolsButtonsPre, bottomToolsButtonsAfter,
                horizontalToolsHGap1, horizontalToolsVGap1, horizontalToolsHGap2, horizontalToolsVGap2,
                createPlaceholderLabel(leftToolsWidth, bottomToolsHeight),
                createPlaceholderLabel(rightToolsWidth, bottomToolsHeight));
        bottomTools.setBorder(BorderFactory.createLineBorder(Color.black, 1, false));
    }


    /**
     * 刷新 mainPane 及所有关联子组件bounds, 当主界面窗口启动, 或者主界面大小改变时应当调用! (mainPane大小改变时)
     * 注意调用时机
     *
     * @noti 皆因 JDesktopPane 层次面板size改变时, 其内子组件并不会相应自动改变bounds, 因此实现所有相关子组件位置刷新逻辑
     */
    public void flushAllFuncFrameBounds() {
        // 所有功能框刷新 位置.
        for (FuncFrameS dialog : existFuncFrames.values()) { // 其他关联的功能窗口, 也刷新
            dialog.flushBounds();
        }
    }

    /**
     * 当尺寸改变时, 应当调用 所有相关的 功能实现对话框刷新位置
     */
    private void addListeners() {
        this.addComponentListener(new ComponentAdapter() {
            @SneakyThrows
            @Override
            public void componentResized(ComponentEvent e) {
                flushAllFuncFrameBounds(); // 容器大小改变, 应当自动改变主内容, 实测直接最大化无法自动完成,因此
            }
        });
    }


}
