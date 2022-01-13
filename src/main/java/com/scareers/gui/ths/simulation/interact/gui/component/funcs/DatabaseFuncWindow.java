package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.VerticalFuncFrameS;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfDatabaseFuncWindow;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfLogFuncWindow;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class DatabaseFuncWindow extends VerticalFuncFrameS {
    private static DatabaseFuncWindow INSTANCE;

    private DatabaseFuncWindow(TraderGui mainWindow, String title,
                               boolean resizable, boolean closable, // JInternalFrame
                               boolean maximizable, boolean iconifiable,
                               int funcToolsHeight, double preferWidthScale, // 自身
                               int autoMinWidth, int autoMaxWidth,
                               Integer layer) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer);
        this.getMainWindow().getCorePanel().getFuncFrames().add(this); // 注册自身, 主界面变化时将自动调用 flushBounds()
    }

    // 模拟数据库控件
    public static DatabaseFuncWindow getInstance(TraderGui mainWindow, String title,
                                                 boolean resizable, boolean closable, // JInternalFrame
                                                 boolean maximizable, boolean iconifiable,
                                                 int funcToolsHeight, double preferWidthScale, // 自身
                                                 int autoMinWidth, int autoMaxWidth
    ) {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseFuncWindow(mainWindow, title,
                    resizable, closable, // JInternalFrame
                    maximizable, iconifiable,

                    funcToolsHeight, preferWidthScale, // 自身
                    autoMinWidth, autoMaxWidth, layerOfDatabaseFuncWindow);
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
    protected java.util.List<JButton> getToolsButtons1() { // 工具栏可重写(两组按钮)
        java.util.List<JButton> res = new ArrayList<JButton>(super.getToolsButtons1());
        // 可加入其他 button
        return res;
    }

    @Override
    protected List<JButton> getToolsButtons2() {
        return super.getToolsButtons2();
    }
}
