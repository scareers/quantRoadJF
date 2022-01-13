package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.HorizontalFuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.VerticalFuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfLogFuncWindow;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */

public class LogFuncWindow extends HorizontalFuncFrameS {
    private static LogFuncWindow INSTANCE;

    private LogFuncWindow(TraderGui mainWindow, String title, boolean resizable, boolean closable, boolean maximizable,
                          boolean iconifiable, int funcToolsWidth, double preferHeightScale, int autoMinHight,
                          int autoMaxHight, Integer layer) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsWidth, preferHeightScale,
                autoMinHight, autoMaxHight, layer);
        this.getMainWindow().getCorePanel().getFuncFrames().add(this); // 注册自身, 主界面变化时将自动调用 flushBounds()
    }

    // DisplayForLog jDisplayForLog;
    public static LogFuncWindow getInstance(TraderGui mainWindow, String title,
                                            boolean resizable, boolean closable, // JInternalFrame
                                            boolean maximizable, boolean iconifiable,

                                            int funcToolsWidth, double preferHeightScale, // 自身
                                            int autoMinHight, int autoMaxHight) {
        if (INSTANCE == null) {
            INSTANCE = new LogFuncWindow(mainWindow, title,
                    resizable, closable, // JInternalFrame
                    maximizable, iconifiable,

                    funcToolsWidth, preferHeightScale, // 自身
                    autoMinHight, autoMaxHight, layerOfLogFuncWindow);
        }
        INSTANCE.flushBounds(); // 均刷新
        INSTANCE.show(); // 均显示
        return INSTANCE;
    }

    @Override
    public void initCenterComponent() { // 抽象方法
        DisplayForLog displayForLog = new DisplayForLog();
        this.centerComponent = displayForLog;
        this.add(displayForLog, BorderLayout.CENTER);
    }

    @Override
    protected List<JButton> getToolsButtons1() { // 工具栏可重写(两组按钮)
        List<JButton> res = new ArrayList<JButton>(super.getToolsButtons1());
        // 可加入其他 button
        return res;
    }

    @Override
    protected List<JButton> getToolsButtons2() {
        return super.getToolsButtons2();
    }
}
