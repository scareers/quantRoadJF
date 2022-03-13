package com.scareers.gui.ths.simulation.interact.gui.component.combination.log;

import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_SCROLL_BAR_THUMB;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MAIN;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-17:59:31
 */
@Getter
public class ManipulateLogPanel extends JPanel {
    JScrollPane jScrollPane = new JScrollPane(); // 滚动包裹
    JTextPane logTextPane = new TextPaneDisplay(false); // 不换行不可编辑展示框

    public ManipulateLogPanel() {
        super();
        this.setLayout(new BorderLayout());
        logTextPane.setBorder(null);
        jScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setViewportView(logTextPane);
        jScrollPane.setBorder(null);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_MAIN, COLOR_SCROLL_BAR_THUMB); // 替换自定义barUi
        this.add(jScrollPane, BorderLayout.CENTER);
        this.setBorder(null);
    }
}
