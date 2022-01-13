package com.scareers.gui.ths.simulation.interact.gui.component.combination.log;

import com.scareers.utils.log.JTextPaneAppender;

import javax.swing.*;
import java.awt.*;

/**
 * description: 不会自动换行的文本框, 仅用作展示用
 *
 * @author: admin
 * @date: 2022/1/11/011-23:55:07
 */
public class TextPaneDisplay extends JTextPane {
    public TextPaneDisplay(boolean registerToLog) {
        super();
        setEditable(false); // 展示用因此不可编辑
        if (registerToLog) { //是否绑定到cmd log, 作为log显示控件?
            JTextPaneAppender.addJTextPane(this); // 注册log
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
//        return (getSize().width < getParent().getSize().width - 10000);
    }

    @Override
    public void setSize(Dimension d) {
        if (d.width < getParent().getSize().width) {
            d.width = getParent().getSize().width;
        }
        d.width += 10000; // 极大则不会.
        super.setSize(d);
    }
}
