package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alee.managers.animation.easing.Back;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.DisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.log.ManipulateLogPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import org.apache.commons.collections.functors.FalsePredicate;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

/**
 * description: 操作日志输出, 专门用于各种人工操作引起的 log;
 * 单例模式,
 * 提供各种util方法, 单次添加行, 自动生成 [时间] 前缀, 可设置 单行颜色, 加粗! // 功能由 CenterPanel 实现
 *
 * @author: admin
 * @date: 2022/1/13/013-04:41:26
 */

public class ManipulateLogWindow extends FuncFrameS {
    private static ManipulateLogWindow INSTANCE;

    public static ManipulateLogWindow getInstance() {
        return INSTANCE;
    }

    public static ManipulateLogWindow getInstance(Type type, String title, TraderGui mainWindow,
                                                  FuncButton belongBtn, boolean resizable, boolean closable,
                                                  boolean maximizable,
                                                  boolean iconifiable, int autoMaxWidthOrHeight,
                                                  int autoMinWidthOrHeight,
                                                  double preferScale,
                                                  int funcToolsWidthOrHeight, boolean halfWidthOrHeight,
                                                  Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new ManipulateLogWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
        }
        return INSTANCE;
    }

    private ManipulateLogWindow(Type type, String title, TraderGui mainWindow,
                                FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                                boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                double preferScale,
                                int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    JTextPane logTextPane;

    @Override
    public void initCenterPanel() { // 抽象方法
        ManipulateLogPanel displayForLog = new ManipulateLogPanel();
        logTextPane = displayForLog.getLogTextPane(); // 操作.
        setCenterPanel(displayForLog);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // 工具栏可重写(两组按钮)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }


    public static void put(String rawContent, Color color, boolean bold) {
        JTextPane out = INSTANCE.logTextPane;
        Font font = new Font("Consolas", Font.PLAIN, 14);
        if (bold) {
            font = new Font("Consolas", Font.BOLD, 14);
        }
        Document document = out.getDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
        aset = sc.addAttribute(aset, StyleConstants.Family, font.getFamily());
        aset = sc.addAttribute(aset, StyleConstants.FontSize, font.getSize());
        aset = sc.addAttribute(aset, StyleConstants.Bold, font.isBold());
        aset = sc.addAttribute(aset, StyleConstants.Italic, font.isItalic());

        String content = StrUtil.format("[{}] {}{}", DateUtil.format(DateUtil.date(), DatePattern.NORM_TIME_PATTERN),
                rawContent, "\n"); // 加换行符, 加时间
        try {
            document.insertString(document.getLength(), content, aset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        out.setCaretPosition(out.getDocument().getLength());
    }

    public static void put(String rawContent) {
        put(rawContent, Color.white, false);
    }

    public static void put(String rawContent,Color color) {
        put(rawContent, color, false);
    }

    public static void put(String rawContent,boolean bold) {
        put(rawContent, Color.white, false);
    }

}
