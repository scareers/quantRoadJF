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
public abstract class HorizontalFuncDialogS extends FuncDialogS {
    int autoMaxHight = 1080; // 自动增加可达到最大高度
    int autoMinHight = 50; // 自动减小可达到最小高度
    double preferHeightScale = 0.3; // 默认高度占父亲高度百分比
    int preferHeight;
    TraderGui parent;
    ToolsPanel funcTools; // 工具按钮组
    int funcToolsWidth; // 按钮宽度
    protected Component centerComponent; // 主内容, 若调用特殊方法, 应当强制转型后调用

    protected HorizontalFuncDialogS(Window owner, String title, ModalityType type) {
        super(owner, title, type);
        this.typeS = OrientationType.HORIZONTAL; // 固定
    }

    /**
     * 主要构造器
     *
     * @param owner
     * @param centerComponent
     * @param title
     * @param funcToolsWidth
     * @param preferHeightScale
     * @param autoMinHight
     * @param autoMaxHight
     */
    protected HorizontalFuncDialogS(TraderGui owner, String title, int funcToolsWidth,
                                    double preferHeightScale, int autoMinHight, int autoMaxHight) {
        this(owner, title, ModalityType.MODELESS); // 永不阻塞顶级窗口, 且已经设置水平方向
        initAttrs(owner, funcToolsWidth, preferHeightScale, autoMinHight, autoMaxHight);
        initCenterComponent();
        initChildren();
    }

    /**
     * 抽象方法, 创建核心中央组件, 以做子类区分
     */
    protected abstract void initCenterComponent();

    private void initAttrs(TraderGui owner, int funcToolsWidth, double preferHeightScale, int autoMinHight,
                           int autoMaxHight) {
        this.parent = owner; // 达成对 TraderGui 而非父类 owner Window 的访问
        this.funcToolsWidth = funcToolsWidth;
        this.preferHeightScale = preferHeightScale;
        this.preferHeight = (int) (this.parent.getHeight() * preferHeightScale); // flushBounds()中重复调用.
        this.autoMinHight = autoMinHight;
        this.autoMaxHight = autoMaxHight;
    }

    /**
     * 次要构造器
     *
     * @param owner
     * @param centerComponent
     * @param title
     * @param funcToolsWidth
     * @param preferHeightScale
     */
    protected HorizontalFuncDialogS(TraderGui owner, String title, int funcToolsWidth,
                                    double preferHeightScale) {
        this(owner, title, funcToolsWidth,
                preferHeightScale, 50, 1080);
    }

    protected void initChildren() {
        ToolsPanel.ToolsPanelType toolsPanelType = ToolsPanel.ToolsPanelType.VERTICAL; // 横向功能对话框, 使用纵向工具栏
        funcTools = new ToolsPanel(funcToolsWidth, toolsPanelType,
                getToolsButtons1(), getToolsButtons2(),
                0, 0, 0, 0);

        this.add(funcTools, BorderLayout.WEST);

        // 注意, 起点(x,y) 应当+主窗口x,y, 因为setBounds本身是绝对定位
        this.flushBounds();
        this.setResizable(true);
        this.setFocusable(true);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏而非真正关闭,配合单例模式
    }

    /**
     * 默认实现仅此3按钮, 子类可调用
     *
     * @return
     */
    protected List<JButton> getToolsButtons1() {
        HorizontalFuncDialogS dialog = this;
        JButton resetBounds = ButtonFactory.getButton("置");
        resetBounds.setToolTipText("重置位置");

        resetBounds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.flushBounds();
            }
        });

        JButton higherButton = ButtonFactory.getButton("上");
        higherButton.setToolTipText("增大高度");

        higherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = dialog.getBounds();
                int newHeight = Math.min(dialog.getHeight() * 2, autoMaxHight);
                int difference = newHeight - dialog.getHeight();
                if (difference <= 0) {
                    return;
                }
                dialog.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference,
                        (int) rawbounds.getWidth(), newHeight);
            }
        });

        JButton shorterButton = ButtonFactory.getButton("下");
        shorterButton.setToolTipText("减小高度");

        shorterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Rectangle rawbounds = dialog.getBounds();
                int newHeight = Math.max(dialog.getHeight() / 2, autoMinHight);
                int difference = newHeight - dialog.getHeight();
                if (difference >= 0) {
                    return;
                }
                dialog.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference, // y-负数, 变大
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
        this.preferHeight = (int) (this.parent.getHeight() * preferHeightScale);
        this.setBounds(
                //  x = 左侧边栏X + 左侧边栏宽度
                parent.getCorePanel().getLeftTools().getX() + parent.getCorePanel().getLeftTools().getWidth() + parent
                        .getX(),
                // y = 主界面底部 - 状态栏高度 - 底部栏高度 + 2(像素修正)
                parent.getY() + parent.getHeight() - parent.getStatusBar().getHeight() - parent.getCorePanel()
                        .getBottomTools().getHeight() - preferHeight + 3,
                // 宽度 = 主宽 - (两个侧边栏宽之和)
                parent.getWidth() - parent.getCorePanel().getLeftTools().getWidth() - parent.getCorePanel()
                        .getRightTools().getWidth(),
                preferHeight);
    }
}
