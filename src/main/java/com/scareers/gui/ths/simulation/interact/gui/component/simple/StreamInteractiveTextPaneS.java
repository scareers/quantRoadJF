package com.scareers.gui.ths.simulation.interact.gui.component.simple;

import cn.hutool.core.lang.Tuple;
import cn.hutool.core.thread.ThreadUtil;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;

/**
 * description: 给定 输入输出流, 达成交互! 类似 cmd shell交互. 一般用于开启其他进程后, 文本编辑框交互!
 *
 * @author: admin
 * @date: 2022/1/13/013-10:06:32
 */
public class StreamInteractiveTextPaneS extends JTextPane {
    BufferedReader input;
    BufferedWriter output;
    Process process; // 常态null, terminal状态下

    Object lock = new Object();  // 读写共用一把锁

    private StreamInteractiveTextPaneS() {
        super();
        this.setEditable(true);
    }

    private StreamInteractiveTextPaneS(BufferedReader input, BufferedWriter output) {
        this();
        this.input = input;
        this.output = output;
    }

    public StreamInteractiveTextPaneS(boolean isTerminal) throws IOException {
        this();

        try {
            this.readForever();
        } catch (BadLocationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                System.out.println("write start");
                while (true) {
                    if(output==null){
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    output.write("ipconfig\n");
                    Thread.sleep(1000);
                    System.out.println("write");
                }
            }
        }, true);


        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Process process;
        try {
            process = Runtime.getRuntime().exec("dir\n");
//            process = Runtime.getRuntime().exec("powershell.exe");
        } catch (IOException e) {
            process = Runtime.getRuntime().exec("cmd.exe"); // 再次抛出异常
//            process = Runtime.getRuntime().exec("cmd.exe"); // 再次抛出异常
        }
        System.out.println(process.toString());
        this.process = process;
        this.input = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.output = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        try {
            this.getDocument().insertString(this.getDocument().getLength(), "hello\n", getAset());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }


    }

    public void readForever() throws IOException, BadLocationException, InterruptedException {
        StreamInteractiveTextPaneS component = this;
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {

                    System.out.println(process);
                    if (output == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
//                    System.out.println("xx");
//

                    String line;


                    if ((line = input.readLine()) != null) {
                        System.out.println(line);
                        component.getDocument().insertString(component.getDocument().getLength(), line, getAset());
                        component.setCaretPosition(component.getDocument().getLength());
                    } else { // 100ms 尝试读一次, 没有的情况下
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, true);


    }

    public static AttributeSet getAset() {
        Color color = Color.WHITE;
        Font font = new Font("宋体", Font.BOLD, 16);

        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

        aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
        aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
        aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
        aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());
        return aset;
    }


    /*
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream());
    while((s=bufferedReader.readLine()) != null)
    System.out.println(s);
    int exitcode = process.waitfor(); // 阻塞等结束
    process.destroy();  // 强杀
     */


}
