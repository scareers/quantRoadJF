package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import lombok.Getter;
import lombok.Setter;

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
    boolean halfHeight;  // 请根据需要显示调用set方法设置该值. 若按钮位置变化,也应当实时读取位置确定是否全高!

    // 构造器相同
    protected LeftFuncFrameS(TraderGui mainWindow, String title, boolean resizable, boolean closable,
                             boolean maximizable,
                             boolean iconifiable, int funcToolsHeight, double preferWidthScale, int autoMinWidth,
                             int autoMaxWidth, Integer layer, boolean halfHeight) {
        this(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer, true, halfHeight);

    }

    protected LeftFuncFrameS(TraderGui mainWindow, String title, boolean resizable, boolean closable,
                             boolean maximizable,
                             boolean iconifiable, int funcToolsHeight, double preferWidthScale, int autoMinWidth,
                             int autoMaxWidth, Integer layer, boolean addToMainPane, boolean halfHeight) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer, addToMainPane); // 将首次调用flushBounds, 此时默认全高
        this.halfHeight = halfHeight;
        if (halfHeight) {
            this.flushBounds(true); // 若半高则 将第二次调用, 此时已经修改
        }
    }

    /**
     * 刷新位置, 注意, 自身已经加入 主面板 JDesktopPane 的某一层-0;
     * 因此位置需要依据 mainPane 当前情况刷新
     */
    @Override
    public void flushBounds(boolean first) {
        if (first) { // 首次刷新, 将读取mainDisplay是否被设定, 若未设定, 则采取默认宽度, 否则同 false.
            this.preferWidth = (int) (this.mainPane.getWidth() * preferWidthScale); // 需要更新默认高度
            actualFlush(preferWidth);
        } else {
            double oldScale = (double) this.getWidth() / this.mainPaneWidth; // 注意, 需要读取上次保存的 旧的mainPane尺寸
            int newWidth = (int) (oldScale * this.mainPane.getWidth()); // 新的尺寸计算, 等比缩放
            actualFlush(newWidth);
        }
        // 无论如何, 均需要刷新mainPane尺寸, 做下一次更新时的 "旧尺寸"
        this.mainPaneWidth = this.mainPane.getWidth();
        this.mainPaneHeight = this.mainPane.getHeight(); // 刷新manePane尺寸
    }


    /**
     * flush逻辑与父类同, 只是实际位置应当不同! 调用该方法前, 若要高度减半,(idea左下册功能), 请手动设置 halfHeight
     *
     * @param newWidth
     */
    @Override
    protected void actualFlush(int newWidth) {
        int y = 0;
        if (halfHeight) {
            y = mainPane.getHeight() / 2;
        }
        this.setBounds(
                0,
                // y = 0
                y,
                newWidth,
                // 高度 = mainPane 高度
                mainPane.getHeight() - y); // 注意逻辑
    }
}
