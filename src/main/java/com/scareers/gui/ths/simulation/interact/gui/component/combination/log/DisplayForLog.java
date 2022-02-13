package com.scareers.gui.ths.simulation.interact.gui.component.combination.log;

import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: log文本展示
 *
 * @author: admin
 * @date: 2022/1/12/012-00:14:04
 */
public class DisplayForLog extends JPanel {
    JScrollPane jScrollPane = new JScrollPane(); // 滚动包裹
    JTextPane logTextPane = new TextPaneDisplay(true); // 不换行不可编辑展示框

    public DisplayForLog() {
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
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_MAIN , COLOR_SCROLL_BAR_THUMB); // 替换自定义barUi
        this.add(jScrollPane, BorderLayout.CENTER);
        this.setBorder(null);
    }
}
