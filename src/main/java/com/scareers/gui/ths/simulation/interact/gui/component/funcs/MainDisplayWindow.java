package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.util.List;

/**
 * description: 主编辑区/主展示区, 应当继承RightFuncFrameS, 注意其宽度! 且该控件, 在 CorePane实例化(mainPane实例化)后再显式设置!
 * 其 preferWidthScale 往往较大, 其layer 应当 < 右侧其他.功能栏   > 左侧功能栏
 * 其应当设定监听器, 当尺寸改变时, 应当调用 所有左侧栏的 flushBounds(false) 方法, 非首次刷新 .
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
@Setter
@Getter
public class MainDisplayWindow extends FuncFrameS {
    private static MainDisplayWindow INSTANCE;

    private MainDisplayWindow(String title, TraderGui mainWindow,
                              boolean resizable, boolean maximizable, boolean iconifiable,
                              int autoMaxWidthOrHeight, int autoMinWidthOrHeight, double preferScale,
                              int funcToolsWidthOrHeight, Integer layer) {
        super(Type.RIGHT_TOP, title, mainWindow, null, resizable, false, maximizable, iconifiable,
                autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, false, layer, false, false);
        // 两参数使用固定的默认值
        ((BasicInternalFrameUI) this.getUI()).setNorthPane(null);
    }

    // 模拟数据库控件
    public static MainDisplayWindow getInstance(String title, TraderGui mainWindow,
                                                boolean resizable, boolean maximizable,
                                                boolean iconifiable,
                                                int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                                double preferScale,
                                                int funcToolsWidthOrHeight,
                                                Integer layer
    ) {
        if (INSTANCE == null) {
            INSTANCE = new MainDisplayWindow(title, mainWindow,
                    resizable, maximizable, iconifiable,
                    autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale,
                    funcToolsWidthOrHeight, layer);
        }
        return INSTANCE;
    }

    public static MainDisplayWindow getInstance() { // 快捷方法, 需要创建后
        return INSTANCE;
    }

    @Override
    public void initCenterPanel() { // 抽象方法
        JPanel panel = new JPanel(new BorderLayout()); // 默认空panel
        panel.add(new JLabel("xxxxx"), BorderLayout.CENTER);
        panel.setBackground(SettingsOfGuiGlobal.COLOR_THEME_MINOR);
        this.setBorder(null);
        setCenterPanel(panel);
    }

    /**
     * 设置新 展示panel, 展示区更改!
     *
     * @param centerPanel
     */
    @Override
    public void setCenterPanel(JPanel centerPanel) {
        if (this.centerPanel != null) {
            this.centerPanel.setVisible(false);
        }
        this.centerPanel = centerPanel;
        this.centerPanel.setVisible(true);
        this.setContentPane(centerPanel);
//        this.add(centerPanel, BorderLayout.CENTER);
    }

    @Override
    protected List<FuncButton> getToolButtons1() {
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }

    @Override
    public void flushBounds() {
        super.flushBounds();
    }

    @Override
    public void flushBounds(boolean first) {
        super.flushBounds(first);
    }
}
