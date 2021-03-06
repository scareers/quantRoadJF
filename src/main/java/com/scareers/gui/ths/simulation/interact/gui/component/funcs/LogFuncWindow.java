package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;

import java.awt.*;
import java.util.List;

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
//            ((BasicInternalFrameUI) INSTANCE.getUI()).setNorthPane(null);
//            BasicInternalFrameUI ui = ((BasicInternalFrameUI) INSTANCE.getUI());
//            ui.setNorthPane(new BasicInternalFrameTitlePaneS(INSTANCE));
//            BasicInternalFrameTitlePane northPane = (BasicInternalFrameTitlePane) ui.getNorthPane();
//            northPane.setBackground(Color.red);
//
//            ui.setNorthPane(northPane);
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
    public void initCenterPanel() { // 抽象方法
        DisplayForLog displayForLog = new DisplayForLog();
        setCenterPanel(displayForLog);
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
