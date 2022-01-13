package com.scareers.gui.ths.simulation.interact.gui.component.simple;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * description: 给定 输入输出流, 达成交互! 类似 cmd shell交互. 一般用于开启其他进程后, 文本编辑框交互!
 *
 * @author: admin
 * @date: 2022/1/13/013-10:06:32
 */
public class StreamInteractiveTextPaneS extends JTextPane {
    InputStream input;
    OutputStream output;

    Object lock = new Object();  // 读写共用一把锁

    public StreamInteractiveTextPaneS() {
        super();
        this.setEditable(true);
    }

    public StreamInteractiveTextPaneS(InputStream input, OutputStream output) {
        this();
        this.input = input;
        this.output = output;
    }
}
