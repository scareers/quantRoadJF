package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import javax.swing.*;
import java.awt.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/13/013-09:55:01
 */
public class TerminalCorePanel extends JPanel {
    JScrollPane jScrollPane = new JScrollPane(); //
    JTextPane interactivePane = new JTextPane(); // 交互编辑框

    public TerminalCorePanel() {
        super();
        init();
    }

    private void init() {
        this.setLayout(new BorderLayout());
        interactivePane.setBorder(null);

        jScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPane.setViewportView(interactivePane);
        jScrollPane.setBorder(null);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.setBorder(null);

    }
}
