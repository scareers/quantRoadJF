package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.SecurityListAndTablePanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/22/022-19:41:30
 */
public class HsStateDisplayPanel extends SecurityDisplayPanel {

    private JTable jTable;
    private JScrollPane jScrollPane;

    private JPanel buttonContainer; // 功能按钮容器
    private JButton buttonFlushAll; // 全量刷新按钮
    private boolean fullFlushFlag = false; // 强制全量刷新flag. 刷新一次自动false. 点击刷新按钮将自动全量刷新一次.

    private SecurityBeanEm preBean;

    /**
     * @param parent 仅用于位置修复
     */
    SecurityListAndTablePanel parent;

    public HsStateDisplayPanel(
            SecurityListAndTablePanel parent,
            int listWidth) {
        this.parent = parent;
        this.setBorder(null);
        this.setLayout(new BorderLayout());

        jScrollPane = new JScrollPane();
        jScrollPane.setBorder(null);
        JLabel label = new JLabel("数据获取中"); // 默认显示内容
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // 占位
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi

        buttonFlushAll = ButtonFactory.getButton("全量刷新");
        buttonFlushAll.setMaximumSize(new Dimension(60, 20));
        buttonFlushAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fullFlushFlag = true; // 下一次将全量更新
            }
        });

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonContainer.add(buttonFlushAll);
        buttonContainer.setBorder(null);
        this.add(buttonContainer, BorderLayout.NORTH);
        this.add(jScrollPane, BorderLayout.CENTER);

        this.setBounds(listWidth, 0, parent.getWidth() - listWidth, parent.getHeight());
        parent.getMainDisplayWindow().flushBounds();
    }



    /**
     * SecurityBeanEm newBean 以更新设定
     */
    @Override
    protected void update() {

    }
}
