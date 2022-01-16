package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class DatabaseFuncWindow extends FuncFrameS {
    public DatabaseFuncWindow(Type type, String title, TraderGui mainWindow,
                              FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                              boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                              double preferScale,
                              int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    @Override
    public void initCenterComponent() { // 抽象方法
        JLabel label = new JLabel("我是数据库");
        label.setForeground(Color.WHITE);
        JPanel jPanel = new JPanel();
        jPanel.add(label);
        this.centerComponent = jPanel;
        this.add(this.centerComponent, BorderLayout.CENTER);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // 工具栏可重写(两组按钮)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }
}
