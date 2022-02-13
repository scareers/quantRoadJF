package com.scareers.gui.ths.simulation.interact.gui.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_SCROLL_BAR_THUMB;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;

/**
 * description: 滚动条ui.
 * track 的颜色需要手动设置为被包裹控件的主背景颜色. 主要看起来浑然一体, 如pycharm
 * thumb的4个颜色完全相同, 均为默认设置.
 * 上下按钮, 可选择是否paint. 构造器
 *
 * @author: admin
 * @date: 2022/2/13/013-17:26:22
 */
public class BasicScrollBarUIS extends BasicScrollBarUI {
    Color trackColorTemp;
    Color thumbColorTemp;

    public BasicScrollBarUIS(Color trackColor, Color thumbColor) {
        this.trackColorTemp = trackColor;
        this.thumbColorTemp = thumbColor;
    }

    @Override
    protected void paintDecreaseHighlight(Graphics g) {
        trackHighlightColor = Color.red; // 暂时无效
        super.paintDecreaseHighlight(g);
    }

    @Override
    protected void paintIncreaseHighlight(Graphics g) {
        trackHighlightColor = Color.red; //
        super.paintIncreaseHighlight(g);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        trackColor = trackColorTemp;
        super.paintTrack(g, c, trackBounds);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        thumbDarkShadowColor = thumbColorTemp;
        thumbColor = thumbColorTemp;
        thumbHighlightColor = thumbColorTemp;
        thumbLightShadowColor = thumbColorTemp;
        super.paintThumb(g, c, thumbBounds);
    }

    public static void replaceScrollBarUI(JScrollPane jScrollPane, Color trackColor, Color thumbColor) {
        BasicScrollBarUI barUI = new BasicScrollBarUIS(trackColor, thumbColor);
        JScrollBar barH = jScrollPane.getHorizontalScrollBar();
        barH.setBorder(null);
        barH.setUI(barUI);
        BasicScrollBarUI barUI2 = new BasicScrollBarUIS(trackColor, thumbColor);
        JScrollBar barV = jScrollPane.getVerticalScrollBar();
        barV.setBorder(null);
        barV.setUI(barUI2);

    }
}
