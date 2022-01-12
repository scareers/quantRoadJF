package com.scareers.gui.ths.simulation.interact.gui.component.fivecore;

import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMinor;

/**
 * description: 左功能栏, 右功能栏, 下功能栏.  含多 Button 的 Panel.
 * 可纵向, 可横向
 *
 * @author: admin
 * @date: 2022/1/13/013-02:51:06
 */
public class ToolsPanel extends JPanel {
    public ToolsPanel() {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // 上下
        this.setPreferredSize(new Dimension(20, 100)); // 定宽
        JPanel panel1 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0));  // 上, 上浮动
        JPanel panel2 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, 0, 0)); // 下, 下浮动
        JButton projectButton = ButtonFactory.getButton("对象查看", true);
        panel1.add(projectButton);
        projectButton.setBackground(colorThemeMinor);
        JButton favoritesButton = ButtonFactory.getButton("数据查看", true);
        panel2.add(favoritesButton);
        this.add(panel1);
        this.add(panel2);
    }
}
