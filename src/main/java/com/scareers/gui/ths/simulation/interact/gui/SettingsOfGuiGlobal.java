package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;

import java.awt.*;

/**
 * description: gui 全局设定
 *
 * @noti 路径设定, 均为资源路径. classpath下相对路径
 * @author: admin
 * @date: 2022/1/12/012-02:29:10
 * @see ResourceUtil.getResource(iconPath)
 * @see ClassLoader.getSystemResource(iconPath)
 */
public class SettingsOfGuiGlobal {
    // 颜色相关设定
    public static Color COLOR_THEME_MAIN = new Color(43, 43, 43); // 主色,常见内容框背景色
    public static Color COLOR_THEME_MINOR = new Color(60, 63, 65); // 主色2,次要颜色菜单栏,树形菜单等内容主色,主界面标题栏
    public static Color COLOR_THEME_TITLE = new Color(59, 71, 84); // 主色3, 子组件标题栏颜色
    public static Color COLOR_GRAY_COMMON = new Color(187, 187, 187); // 常规灰,按钮字颜色

    public static Color colorTest = new Color(51, 51, 51); // 测试颜色.., 方便看..

    // 主界面相关设定
    public static final String ICON_PATH = "gui/img/titleIcon0.png"; // 图标
    public static final boolean MAXIMIZE_DEFAULT = false; // 默认启动时最大化?
    public static final Dimension trayIconSize = new Dimension(16, 16); // 系统托盘必须尺寸

}
