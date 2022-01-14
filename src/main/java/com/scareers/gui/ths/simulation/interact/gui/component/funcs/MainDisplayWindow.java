package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.RightFuncFrameS;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.layerOfDatabaseFuncWindow;

/**
 * description: 主编辑区/主展示区, 应当继承RightFuncFrameS, 注意其宽度! 且该控件, 在 CorePane实例化(mainPane实例化)后再显式设置!
 * 其 preferWidthScale 往往较大, 其layer 应当 < 右侧其他.功能栏   > 左侧功能栏
 * 其应当设定监听器, 当尺寸改变时, 应当调用 所有左侧栏的 flushBounds(false) 方法, 非首次刷新.
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class MainDisplayWindow extends RightFuncFrameS {
    private static MainDisplayWindow INSTANCE;

    private MainDisplayWindow(TraderGui mainWindow, String title,
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
    public static MainDisplayWindow getInstance(TraderGui mainWindow, String title,
                                                boolean resizable, boolean closable, // JInternalFrame
                                                boolean maximizable, boolean iconifiable,
                                                int funcToolsHeight, double preferWidthScale, // 自身
                                                int autoMinWidth, int autoMaxWidth
    ) {
        if (INSTANCE == null) {
            INSTANCE = new MainDisplayWindow(mainWindow, title,
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
        JLabel label = new JLabel("我是编辑区");
        label.setForeground(Color.WHITE);
        JPanel jPanel = new JPanel();
        jPanel.add(label);
        this.centerComponent = jPanel;
        this.add(this.centerComponent, BorderLayout.CENTER);
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
