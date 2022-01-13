package com.scareers.gui.ths.simulation.interact.gui.component.simple;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.io.IOException;
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
    Process process; // 常态null, terminal状态下

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

    public StreamInteractiveTextPaneS(boolean isTerminal) throws IOException {
        this();

        Process process;
        try {
            process = Runtime.getRuntime().exec("powershell.exe");
        } catch (IOException e) {
            process = Runtime.getRuntime().exec("cmd.exe"); // 再次抛出异常
        }
        this.process = process;
        this.input = process.getInputStream();
        this.output = process.getOutputStream();
    }
}
