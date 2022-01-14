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
 * description: 左侧纵向子功能栏界面, 其宽度均取决于编辑区拖动.
 * 对比常规的右侧功能栏,
 * 1.其又分为两种子类型: 上半区/下半区  -- boolean halfHeight 决定 // 默认false将属于上半区,全高
 * 位于上半部分的子类 -- 高度==编辑区.  位于下半部分的子类 --  高度为一半! 完全模拟idea;
 * 上下参数应当由构造器传递而来! 因此对比 RightFuncFrameS 将多此属性 -- boolean halfHeight
 * 2.其flushBounds方法, 宽度, 应该读取 编辑区当前宽度做决定, 实时刷新.
 * 但第一次初始化, 应该给定一个合适宽度, 且强行降低编辑区宽度!
 *
 * @author: admin
 * @date: 2022/1/13/013-09:02:09
 */
@Setter
@Getter
public abstract class LeftFuncFrameS extends RightFuncFrameS {
    // 对比常规的 RightFuncFrameS 新增参数: 可控高度一半
    // 默认全高. 当按钮位于左侧工具栏下方时, 应当 true, 以显示一半高度, 该属性请根据需要调用set设置,不在构造器中
    boolean halfHeight = false;  // 请根据需要显示调用set方法设置该值. 若按钮位置变化,也应当实时读取位置确定是否全高!

    // 构造器相同
    protected LeftFuncFrameS(TraderGui mainWindow, String title, boolean resizable, boolean closable,
                             boolean maximizable,
                             boolean iconifiable, int funcToolsHeight, double preferWidthScale, int autoMinWidth,
                             int autoMaxWidth, Integer layer) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer);
    }

    protected LeftFuncFrameS(TraderGui mainWindow, String title, boolean resizable, boolean closable,
                             boolean maximizable,
                             boolean iconifiable, int funcToolsHeight, double preferWidthScale, int autoMinWidth,
                             int autoMaxWidth, Integer layer, boolean addToMainPane) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer, addToMainPane);
    }


    /**
     * 刷新位置, 注意, 自身已经加入 主面板 JDesktopPane 的某一层-0;
     * 因此位置需要依据 mainPane 当前情况刷新
     *
     * @noti 应当读取 mainPane宽度-编辑器宽度, 决定宽度,  读取 halfHeight 决定是否半高. 半高时, 其layer应当>全高的组件
     * @noti preferWidth 本身已经沦为了首次初始化显示时的默认宽度, 且 mainPane应当对应减小宽度.
     */
    @Override
    public void flushBounds() { // 默认为非首次刷新的逻辑. 因此实例化时, 应当调用首次刷新的逻辑
        flushBounds(false);
    }

    public void flushBounds(boolean first) {
        if (first) {
            this.preferWidth = (int) (this.mainPane.getWidth() * preferWidthScale);
            this.setBounds(
                    0,
                    // x,y = 0
                    0,
                    preferWidth,
                    // 高度 = mainPane 高度
                    mainPane.getHeight()); // 位于左边.

        }
    }
}
