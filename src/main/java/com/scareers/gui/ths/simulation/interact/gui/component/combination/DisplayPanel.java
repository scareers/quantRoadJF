package com.scareers.gui.ths.simulation.interact.gui.component.combination;

import javax.swing.*;

/**
 * description: 展示Panel. 例如展示df, 展示文本等. update常为持续循环调用不断更新内容
 *
 * @author: admin
 * @date: 2022/2/18/018-16:48:28
 */
public abstract class DisplayPanel extends JPanel {
    protected abstract void update();
}
