package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

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
 * description: 多新闻 tab 显示页; 每个tab子空间应当为 DisplayPanel 子类, 实现 update() 方法
 * 所有新闻tab类:
 * SimpleNewListPanel : 显示常规东财新闻列表并可编辑
 *
 * @noti : 本类有2个实例, 复盘一个, 操盘计划一个. 几乎一一对应; addTabs()抽象方法 可能添加不同的tab
 * @author: admin
 * @date: 2022/3/13/013-08:42:25
 */
@Getter
public abstract class NewsTabPanel extends DisplayPanel {

    protected JTabbedPane tabbedPane;
    protected MainDisplayWindow mainDisplayWindow;

    protected NewsTabPanel(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        this.addTabs();

        tabbedPane.setSelectedIndex(0);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        TabbedPaneUIS ui = new TabbedPaneUIS();
        tabbedPane.setUI(ui);
        tabbedPane.setForeground(Color.LIGHT_GRAY);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ((DisplayPanel) tabbedPane.getSelectedComponent()).update();
            }
        });

        this.add(tabbedPane, BorderLayout.CENTER);
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ((DisplayPanel) tabbedPane.getSelectedComponent()).update();
                    ThreadUtil.sleep(60000);
                }
            }
        }, true);
    }

    @Override
    public void update() {

    }

    protected abstract void addTabs();

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
