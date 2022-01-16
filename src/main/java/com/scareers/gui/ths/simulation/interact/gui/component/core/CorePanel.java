package com.scareers.gui.ths.simulation.interact.gui.component.core;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS2;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.createPlaceholderLabel;

/**
 * description: 核心 Pane, BorderLayout布局, 左功能栏, 右功能栏, 下功能栏, + 编辑区.
 * 1. 实例化时, 仅实例化 3大功能栏(且不含按钮). + JDesktopPane(层级布局,位于CENTER),
 * 2. 主编辑区+各大功能按钮于 主界面windowOpened 后回调添加. 3方功能栏均于 按钮回调形式添加;  最初状态, 整块CENTER区空白.
 * 3. 3大功能栏 宽度(左右)  高度(下) 可调. 且下功能栏头尾使用占位 Label
 * 4. 功能栏按钮, 以及编辑区MainDisplayWindow , 均使用 FuncFrameS extends JInternalFrame, 配合 JDesktopPane mainPane
 * 5. 功能按钮+实现框 永不真正意义的删除, 最多仅不可见
 *
 * @author: admin
 * @date: 2022/1/13/013-02:23:04
 */
@Getter
@Setter
public class CorePanel extends JDesktopPane {
    private static final Log log = LogUtil.getLogger();
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

    // @noti: 所有功能栏, 均由 以下6列表 以及 ConcurrentHashMap 功能池控制访问,修改等
    // @key2: 按钮与对应功能栏成对初始化,均单例!, 并注册,  实时管理   "功能按钮+功能对话框 --> 成对"
    // 6大功能栏按钮列表, 均可变. 达成按钮可在侧边栏拖动的效果. 且可以动态添加按钮等.
    private Object funcChangeLock = new Object(); // 功能注册,删除,修改 锁!
    CopyOnWriteArrayList<FuncButton> leftTopButtonList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<FuncButton> leftBottomButtonList = new CopyOnWriteArrayList<>(); // 左侧工具栏下排按钮列表,
    CopyOnWriteArrayList<FuncButton> rightTopButtonList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<FuncButton> rightBottomButtonList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<FuncButton> bottomLeftButtonList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<FuncButton> bottomRightButtonList = new CopyOnWriteArrayList<>(); // 底部工具栏后排按钮列表
    // key:value -> 各大按钮对象:其对应功能栏, 均使用单例模式! 自行完成状态变化. 注意 FuncButton的equals和hashCode未曾重写过
    ConcurrentHashMap<FuncButton, FuncFrameS2> funcPool = new ConcurrentHashMap<>();

    // 自动计算的属性
    ToolsPanel leftTools; // 左功能区, 按钮列表
    ToolsPanel rightTools; // 右功能区, 按钮列表
    ToolsPanel bottomTools; // 下功能区, 按钮列表

    JDesktopPane mainPane; // 新增核心层级pane, 原 splitPane 置于其中, 约束值 100
    MainDisplayWindow mainDisplayWindow; // 该属性应当实例化后, 在主界面 WindowOpened 回调时实例化添加

    public CorePanel(int leftToolsWidth, int rightToolsWidth, int bottomToolsHeight,
                     TraderGui mainWindow) {
        super();
        this.mainWindow = mainWindow;
        this.leftToolsWidth = leftToolsWidth;
        this.rightToolsWidth = rightToolsWidth;
        this.bottomToolsHeight = bottomToolsHeight;

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


    /**
     * 注意传递了占位控件, 注意, 3大工具栏初始按钮列表 0; 需 windowOpened后实时添加按钮+功能框.
     */
    private void initBottomTools() {
        bottomTools = new ToolsPanel(bottomToolsHeight, ToolsPanel.ToolsPanelType.HORIZONTAL,

                horizontalToolsHGap1, horizontalToolsVGap1, horizontalToolsHGap2, horizontalToolsVGap2,
                createPlaceholderLabel(leftToolsWidth, bottomToolsHeight),
                createPlaceholderLabel(rightToolsWidth, bottomToolsHeight));
        bottomTools.setBorder(BorderFactory.createLineBorder(Color.black, 1, false));
    }

    private void initLeftTools() {
        leftTools = new ToolsPanel(leftToolsWidth, ToolsPanel.ToolsPanelType.VERTICAL,
                verticalToolsHGap1, verticalToolsVGap1, verticalToolsHGap2, verticalToolsVGap2);
    }

    private void initRightTools() {
        rightTools = new ToolsPanel(rightToolsWidth, ToolsPanel.ToolsPanelType.VERTICAL,
                verticalToolsHGap1, verticalToolsVGap1, verticalToolsHGap2, verticalToolsVGap2);
    }


    /**
     * 刷新 mainPane 及所有关联子组件bounds, 当主界面窗口启动, 或者主界面大小改变时应当调用! (mainPane大小改变时)
     * 注意调用时机
     *
     * @noti 皆因 JDesktopPane 层次面板size改变时, 其内子组件并不会相应自动改变bounds, 因此实现所有相关子组件位置刷新逻辑
     */
    public void flushAllFuncFrameBounds() {
        // 所有功能框刷新 位置.
        for (FuncFrameS2 dialog : funcPool.values()) { // 其他关联的功能窗口, 也刷新
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

    /**
     * 注册 功能按钮 + 对应功能实现框, 分发函数
     *
     * @param funcButton
     * @param funcFrameS
     */
    public void registerFuncBtnAndCorrespondFuncFrame(FuncButton funcButton, FuncFrameS2 funcFrameS) {
        synchronized (funcChangeLock) {
            removeFuncFromSixList(funcButton);
            funcPool.put(funcButton, funcFrameS); // 添加/更新到功能池
            switch (funcFrameS.getType()) {
                case LEFT_TOP:
                    leftTopButtonList.add(funcButton); // 添加到正确的列表
                    leftTools.getPanel1().add(funcButton); // 添加组件到侧边栏并显示
                    break;
                case LEFT_BOTTOM:
                    leftBottomButtonList.add(funcButton);
                    leftTools.getPanel2().add(funcButton);
                    break;
                case RIGHT_TOP:
                    rightTopButtonList.add(funcButton);
                    rightTools.getPanel1().add(funcButton);
                    break;
                case RIGHT_BOTTOM:
                    rightBottomButtonList.add(funcButton);
                    rightTools.getPanel2().add(funcButton);
                    break;
                case BOTTOM_LEFT:
                    bottomLeftButtonList.add(funcButton);
                    bottomTools.getPanel1().add(funcButton);
                    break;
                case BOTTOM_RIGHT:
                    bottomRightButtonList.add(funcButton);
                    bottomTools.getPanel2().add(funcButton);
                    break;
                default:
                    log.error("致命错误: 未知 FuncFrameS Type");
            }
        }
    }

    /**
     * 给定 funcButton, 从各大列表删除, 初次注册,或修改时均可调用, 通常然后再添加到正确的 列表
     * CopyOnWriteArrayList.remove(Object) 参数可 null,
     *
     * @param funcButton
     */
    private void removeFuncFromSixList(FuncButton funcButton) {
        leftTopButtonList.remove(funcButton);
        leftBottomButtonList.remove(funcButton);
        rightTopButtonList.remove(funcButton);
        rightBottomButtonList.remove(funcButton);
        bottomLeftButtonList.remove(funcButton);
        bottomRightButtonList.remove(funcButton);
    }


}
