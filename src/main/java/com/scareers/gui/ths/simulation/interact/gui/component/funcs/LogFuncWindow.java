package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.core.ToolsPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.log.DisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */
@Setter
@Getter
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
        super(owner, title, ModalityType.MODELESS); // 永不阻塞顶级窗口, 且已经设置水平方向
        initAttrs(owner, funcToolsWidth, preferHeightScale, autoMinHight, autoMaxHight);
        DisplayForLog displayForLog = new DisplayForLog(); // 仅这里相比父构造器, 添加了固定组件
        initChildren(displayForLog);
    }


    @Override
    protected List<JButton> getToolsButtons1() {
        List<JButton> res = new ArrayList<JButton>(super.getToolsButtons1());
        // 可加入其他 button
        return res;
    }

    @Override
    protected List<JButton> getToolsButtons2() {
        return super.getToolsButtons2();
    }
}
