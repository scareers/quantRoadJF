package com.scareers.gui.ths.simulation.interact.gui.component.core;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.VerticalFuncFrameS;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfCorePane;
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

    //    JPanel mainFuncPanel; // 左功能实现区, 常为树形菜单形式! 被 leftTools 按钮们控制
//    JPanel mainDisplayPanel; // 主要展示区, 对应idea编辑器. Editor
//    JSplitPane centerSplitPane; // 分开 mainMenuPanel + mainDisplayPanel, 宽度可调
    JDesktopPane mainPane; // 新增核心层级pane, 原 splitPane 置于其中, 约束值 100

    // 各个用对话框实现的子功能组件, 注册到队列. 当主界面size变化, 应当重置位置, 逻辑上 与 JDesktopPane mainPane  绑定
    CopyOnWriteArraySet<FuncFrameS> funcFrames = new CopyOnWriteArraySet<>();

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

        // 尺寸改变
        this.addComponentListener(new ComponentAdapter() {
            @SneakyThrows
            @Override
            public void componentResized(ComponentEvent e) {
                flushMainPanelBounds(); // 容器大小改变, 应当自动改变主内容, 实测直接最大化无法自动完成,因此
            }
        });
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
        bottomTools.setBorder(BorderFactory.createLineBorder(Color.black, 1, false));
    }

    private void initMainPane() {
        // 这里更新. 首先, 原编辑器转换为 无标题栏的对话框. 靠左. 设置一个较大的默认宽度, 最大 1.0  layer== 100
        // 左侧的功能, 例如项目文件树, 固定为 垂直的窗口, 宽度固定倍率为 1.0, 且置于其下. layer == 50
        // 因此, 左侧功能栏本身永恒占满底层mainPane, 而 编辑器功能栏 占据从右向左的较大部分, 初始占据 1.
        // 需要设置, 当左侧功能栏非 数量非null时, editor窗口起点右移. 表面是新的项目窗口撑开了, 实际是编辑器本身右移了

        VerticalFuncFrameS mainFunc = new VerticalFuncFrameS() {
            @Override
            protected void initCenterComponent() {
                JLabel label = new JLabel("我是项目文件树");
                label.setForeground(Color.WHITE);
                label.setBounds(0, 0, 200, 200);
                JPanel jPanel = new JPanel();
                jPanel.add(label);
                this.centerComponent = jPanel;
                this.add(this.centerComponent, BorderLayout.CENTER);
            }
        };
        VerticalFuncFrameS mainDisplay = new VerticalFuncFrameS() {
            @Override
            protected void initCenterComponent() {
                JLabel label = new JLabel("我是编辑器");
                label.setBounds(0, 0, 200, 200);
                label.setForeground(Color.WHITE);
                JPanel jPanel = new JPanel();
                jPanel.add(label);
                this.centerComponent = jPanel;
                this.add(this.centerComponent, BorderLayout.CENTER);
            }
        };


        mainPane = new JDesktopPane(); // 核心层级pane, 原 splitPane 放于其上, 层级为 100, 各窗口应当高于此.

        mainPane.add(mainFunc, layerOfCorePane, 50);
        mainPane.add(mainDisplay, layerOfCorePane, 100);
    }


    /**
     * 刷新mainPane及所有关联子组件bounds, 当主界面窗口启动, 或者主界面大小改变时应当调用! (mainPane大小改变时)
     * 注意调用时机
     *
     * @noti 皆因 JDesktopPane 层次面板size改变时, 其内子组件并不会相应自动改变bounds, 因此实现所有相关子组件位置刷新逻辑
     */
    public void flushMainPanelBounds() {
        // ??? 两大组件刷新


        for (FuncFrameS dialog : funcFrames) { // 其他关联的功能窗口, 也刷新
            dialog.flushBounds();
        }
    }


}
