package com.scareers.utils.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * description: 将log 打印到 JTextArea, 注意需要调用 addJTextArea(文本域) 绑定.可绑定多个文本框
 *
 * @author: admin
 * @date: 2022/1/11/011-19:12:33
 */
public class JTextPaneAppender extends AppenderSkeleton {
    //在log4j.xml中配置需要的属性，此处可以注入
    //private String developer;


    private static int truncateLenth = -1; // 内容截取长度(max(10,x))更加方便好看. -1 显示全部内容. 否则截取 lenth, 建议 50/100等
    private static int prefixLenth = 135; // 前缀长度约 135, 将不被截取
    private static CopyOnWriteArrayList<JTextPane> outputs = new CopyOnWriteArrayList<>(); // 可设置多个核心控件,同时显示
    private static String fontName = "Consolas";
    private static int fontSize = 16;
    private static int fontStyle = Font.PLAIN;
    private static Map<String, Color> colorMap;


    static {
        colorMap = new ConcurrentHashMap<>();
        colorMap.put("WARN", new Color(214, 191, 85));
        colorMap.put("ERROR", new Color(222, 106, 103));
        colorMap.put("DEBUG", new Color(163, 186, 186));
        colorMap.put("INFO", new Color(174, 138, 190));
        colorMap.put("DEFAULT", Color.gray);
    }

    public static void addJTextPane(JTextPane jTextArea) {
        outputs.add(jTextArea);
    }

    /**
     * 激活各组件的方法
     * Appender初始的时候被调用
     */
    @Override
    public void activateOptions() {
        init();
        super.activateOptions();
    }

    /**
     * 自定义初始化方法
     */
    private void init() {
        if (this.layout == null) {
            // LogLog.debug("The layout is not loaded... we set it.");
            String pattern = "[%p]  %d{yyyy-MM-dd HH:mm:ss,SSS}  [%75.75l]  [%16.16t] --> %m%n";
            this.setLayout(new org.apache.log4j.PatternLayout(pattern));
        }
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        String str = this.getLayout().format(loggingEvent); // 实际字符串
        writeLogAsync(str);
    }

    /**
     * 异步输出log
     *
     * @param value
     */
    private void writeLogAsync(String value) {
//        CompletableFuture.runAsync(() -> {
//            try {
//                writeLog(value);
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
//        });

        try {
            writeLog(value);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将遍历 静态属性 outputs, 将内容append到所有关联的子控件
     *
     * @param value
     */
    private void writeLog(String value) throws BadLocationException {
        //可以记录到数据库、本地、缓存等
        for (JTextPane out : outputs) {
            if (out != null) {
                //添加一个可以设置样式的类
                Color color;
                Font font = new Font(fontName, fontStyle, fontSize);
                if (value.startsWith("[WARN]")) {
                    color = colorMap.get("WARN");
                    font = new Font(fontName, Font.BOLD, fontSize);
                } else if (value.startsWith("[INFO]")) {
                    color = colorMap.get("INFO");
                } else if (value.startsWith("[ERROR]")) {
                    color = colorMap.get("ERROR");
                } else if (value.startsWith("[DEBUG]")) {
                    color = colorMap.get("DEBUG");
                } else {
                    color = colorMap.get("DEFAULT"); // TRACE 等级
                }
                Document document = out.getDocument();
                StyleContext sc = StyleContext.getDefaultStyleContext();
                AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

                aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
                aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
                aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
                aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());

                if (truncateLenth != -1) {
                    value = value.substring(0, Math.min(truncateLenth + prefixLenth, value.length())).stripTrailing() + " ...\n";
                }
                document.insertString(document.getLength(), value, aset);
                out.setCaretPosition(out.getDocument().getLength());
            }
        }
    }

    @Override
    public boolean requiresLayout() {
        //是否需要布局，如果返回false，即使log4j.xml配置了layout也不生效。
        return true;
    }

    @Override
    public void close() {
        //释放资源
        this.closed = true;
    }
}
