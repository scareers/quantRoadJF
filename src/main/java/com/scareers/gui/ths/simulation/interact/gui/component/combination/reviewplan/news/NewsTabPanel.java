package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.thread.ThreadUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import lombok.Getter;
import org.jdesktop.swingx.JXCollapsiblePane;

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
    protected JXCollapsiblePane collapsiblePane;
    protected JButton buttonNewAspect; // 资讯面总结
    protected JPanel buttonContainer;
    protected NewAspectSummaryPanel newAspectSummaryPanel; // 资讯面总结内容面板, 放于可折叠面板中

    protected NewsTabPanel(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout());

        initCollapsiblePane(); // 折叠面板, 放 资讯面总结
        initButtonContainer(); // 按钮组, 控制折叠, 以及其他功能按钮
        initTabbedPane(); // 多tab面板, 展示不同类型资讯

        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.add(buttonContainer, BorderLayout.NORTH);
        jPanel.add(collapsiblePane, BorderLayout.CENTER);
        this.add(jPanel, BorderLayout.NORTH); // 放置按钮组合可折叠面板. jPanel将一直显示

        this.add(tabbedPane, BorderLayout.CENTER); // 主tab放于中央
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        ((DisplayPanel) tabbedPane.getSelectedComponent()).update();
                    } catch (Exception e) {

                    }
                    ThreadUtil.sleep(60000);
                }
            }
        }, true);
    }

    private void initTabbedPane() {
        tabbedPane = new JTabbedPane();
        this.addTabs(); // 抽象方法, 添加 tabs
        tabbedPane.setSelectedIndex(-1); // 默认不选择, 否则将卡死
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
    }


    private void initCollapsiblePane() {
        collapsiblePane = new JXCollapsiblePane();
        collapsiblePane.setLayout(new BorderLayout());
        initNewAspectSummaryPanel(); // 初始化资讯总结面板
        collapsiblePane.add("Center", newAspectSummaryPanel);
        collapsiblePane.setAnimated(true);
        collapsiblePane.setCollapsed(true); // 默认不展开
    }

    private void initNewAspectSummaryPanel() {
        newAspectSummaryPanel = new NewAspectSummaryPanel();
    }

    protected void initButtonContainer() {
        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonNewAspect = ButtonFactory.getButton("");
        buttonNewAspect.setAction(collapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        buttonNewAspect.setText("资讯面总结");
        buttonNewAspect.setForeground(Color.red);
        buttonContainer.add(buttonNewAspect);
    }

    @Override
    public void update() {

    }

    protected abstract void addTabs();

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
        if (tabbedPane.getSelectedIndex() != -1) {

            tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex());
        } else {
            tabbedPane.setSelectedIndex(0);
        }
    }
}
