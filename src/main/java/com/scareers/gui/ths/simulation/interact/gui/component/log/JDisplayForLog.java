package com.scareers.gui.ths.simulation.interact.gui.component.log;

import javax.swing.*;
import java.awt.*;

/**
 * description: log文本展示
 *
 * @author: admin
 * @date: 2022/1/12/012-00:14:04
 */
public class JDisplayForLog extends JPanel {
    JScrollPane jScrollPane = new JScrollPane(); // 滚动包裹
    JTextPane logTextPane = new JTextPaneDisplay(true); // 不换行不可编辑展示框

    public JDisplayForLog() {
        super();
        this.setLayout(new BorderLayout());

        logTextPane.setBorder(null);
        jScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setViewportView(logTextPane);
        jScrollPane.setBorder(null);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.setBorder(null);
    }


}
