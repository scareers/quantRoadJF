package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfLogFuncWindow;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */

public class LogFuncWindow extends FuncFrameS {
    private static LogFuncWindow INSTANCE;

    public static LogFuncWindow getInstance() {
        return INSTANCE;
    }

    public static LogFuncWindow getInstance(Type type, String title, TraderGui mainWindow,
                                            FuncButton belongBtn, boolean resizable, boolean closable,
                                            boolean maximizable,
                                            boolean iconifiable, int autoMaxWidthOrHeight,
                                            int autoMinWidthOrHeight,
                                            double preferScale,
                                            int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new LogFuncWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
            ((BasicInternalFrameUI) INSTANCE.getUI()).setNorthPane(null);
        }

        return INSTANCE;
    }

    private LogFuncWindow(Type type, String title, TraderGui mainWindow,
                         FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                         boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight, double preferScale,
                         int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    @Override
    public void initCenterComponent() { // 抽象方法
        DisplayForLog displayForLog = new DisplayForLog();
        this.centerComponent = displayForLog;
        this.add(displayForLog, BorderLayout.CENTER);
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
