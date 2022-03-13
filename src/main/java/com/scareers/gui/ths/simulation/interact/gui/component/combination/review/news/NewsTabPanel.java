package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-08:42:25
 */
@Getter
public class NewsTabPanel extends DisplayPanel {
    public static NewsTabPanel INSTANCE;

    public static NewsTabPanel getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new NewsTabPanel(mainDisplayWindow);
        }
        return INSTANCE;
    }

    JTabbedPane tabbedPane;
    MainDisplayWindow mainDisplayWindow;

    private NewsTabPanel(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("资讯精华", ZiXunJingHuaPanel.getInstance());
        tabbedPane.addTab("财经导读", CaiJingDaoDuPanel.getInstance());
        tabbedPane.setSelectedIndex(1);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        TabbedPaneUIS ui = new TabbedPaneUIS();
        tabbedPane.setUI(ui);
        tabbedPane.setForeground(Color.LIGHT_GRAY);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                ((SimpleNewListPanel) tabbedPane.getSelectedComponent()).update();
            }
        });

        this.add(tabbedPane, BorderLayout.CENTER);
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ((SimpleNewListPanel) tabbedPane.getSelectedComponent()).update();
                    ThreadUtil.sleep(60000);
                }
            }
        }, true);
    }

    @Override
    protected void update() {

    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
