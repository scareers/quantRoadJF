package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.HorizontalFuncDialogS;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */

public class LogFuncWindow extends HorizontalFuncDialogS {
    private static LogFuncWindow INSTANCE;

    // DisplayForLog jDisplayForLog; // 主内容, 强行访问需要强转  Component
    public static LogFuncWindow getInstance(TraderGui owner, String title,
                                            int funcToolsWidth, double preferHeightScale, int autoMinHight,
                                            int autoMaxHight) {
        if (INSTANCE == null) {
            INSTANCE = new LogFuncWindow(owner, title, funcToolsWidth, preferHeightScale, autoMinHight,
                    autoMaxHight);
        }
        INSTANCE.flushBounds();
        INSTANCE.setVisible(true);
        return INSTANCE;
    }

    private LogFuncWindow(TraderGui owner, String title, int funcToolsWidth,
                          double preferHeightScale, int autoMinHight, int autoMaxHight) {
        super(owner, title, funcToolsWidth,
                preferHeightScale, autoMinHight, autoMaxHight);
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