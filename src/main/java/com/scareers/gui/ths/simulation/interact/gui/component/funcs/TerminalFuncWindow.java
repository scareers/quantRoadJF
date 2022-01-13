package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.TerminalCorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.HorizontalFuncDialogS;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */

public class TerminalFuncWindow extends HorizontalFuncDialogS {
    private static TerminalFuncWindow INSTANCE;

    // DisplayForLog jDisplayForLog; // 主内容, 强行访问需要强转  Component
    public static TerminalFuncWindow getInstance(TraderGui owner, String title,
                                                 int funcToolsWidth, double preferHeightScale, int autoMinHight,
                                                 int autoMaxHight) {
        if (INSTANCE == null) {
            INSTANCE = new TerminalFuncWindow(owner, title, funcToolsWidth, preferHeightScale, autoMinHight,
                    autoMaxHight);
        }
        INSTANCE.flushBounds();
        INSTANCE.setVisible(true);
        return INSTANCE;
    }

    private TerminalFuncWindow(TraderGui owner, String title, int funcToolsWidth,
                               double preferHeightScale, int autoMinHight, int autoMaxHight) {
        super(owner, title, funcToolsWidth,
                preferHeightScale, autoMinHight, autoMaxHight);
    }

    @Override
    public void initCenterComponent() { // 抽象方法
        TerminalCorePanel displayForLog = null;
        try {
            displayForLog = new TerminalCorePanel();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
