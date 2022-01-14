package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.RightFuncFrameS;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfDatabaseFuncWindow;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class DatabaseFuncWindow extends RightFuncFrameS {
    private static DatabaseFuncWindow INSTANCE;

    private DatabaseFuncWindow(TraderGui mainWindow, String title,
                               boolean resizable, boolean closable, // JInternalFrame
                               boolean maximizable, boolean iconifiable,
                               int funcToolsHeight, double preferWidthScale, // 自身
                               int autoMinWidth, int autoMaxWidth,
                               Integer layer) {
        super(mainWindow, title, resizable, closable, maximizable, iconifiable, funcToolsHeight, preferWidthScale,
                autoMinWidth, autoMaxWidth, layer);
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
        return INSTANCE;
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
