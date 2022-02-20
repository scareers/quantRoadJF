package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

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
    // 1.颜色相关设定
    public static Color COLOR_THEME_MAIN = new Color(43, 43, 43); // 主色,常见内容框背景色
    public static Color COLOR_THEME_MINOR = new Color(60, 63, 65); // 主色2,次要颜色菜单栏,树形菜单等内容主色,主界面标题栏
    public static Color COLOR_THEME_TITLE = new Color(59, 71, 84); // 主色3, 子组件标题栏颜色
    public static Color COLOR_GRAY_COMMON = new Color(187, 187, 187); // 常规灰,按钮字颜色
    public static Color COLOR_TREE_ITEM_SELECTED = new Color(75, 110, 175); // 树形菜单被选中
    public static Color COLOR_SCROLL_BAR_THUMB = new Color(94, 97, 97); // 滚动条背景
    public static Color COLOR_SPLIT_PANE_DIVIDER_BACK = new Color(49, 51, 53); // 分割面板分隔条主颜色

    public static Color COLOR_MAIN_DISPLAY_BORDER = new Color(50); // 编辑器边框

    public static Color colorTest = new Color(200, 221, 242); // 测试颜色.., 方便看..

    // 2.主界面相关设定
    public static final String ICON_TITLE_PATH = "gui/img/titleIcon0.png"; // 图标
    public static final String ICON_FOLDER_CLOSE_PATH = "gui/img/folder_close.png"; // 图标
    public static final String ICON_FOLDER_OPEN_PATH = "gui/img/folder_open0.png"; // 图标
    public static final String ICON_FILE0_PATH = "gui/img/file0.png"; // 图标
    public static final boolean MAXIMIZE_DEFAULT = false; // 默认启动时最大化?

    // 3.各主/子功能界面默认层级layer, 被各功能窗口工厂方法使用
    // 主内容在 JDesktopPane 的默认层级. 其余子功能对话框 应 > 此值, 才可显示在上
    // 另外横向功能栏默认层级应当 > 纵向, 同idea效果. 但都 > 此值.
    public static Integer layerOfLogFuncWindow = 200;
    public static Integer layerOfDatabaseFuncWindow = 150;
    public static Integer layerOfMainDisplay = 100; // 低
    public static Integer layerOfObjectsTree = 100; // 低

    // 4.全局快捷键设置
    public static final KeyStroke OBJECT_TREE_KS = KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK);

    /*
                    objectsBtn.setMnemonic(KeyEvent.VK_O); // 默认ALT+XX
                objectsBtn.registerKeyboardAction(new ActionListener() {
                                                      @Override
                                                      public void actionPerformed(ActionEvent e) {
                                                          objectsBtn.doClick();
                                                      }
                                                  },
                        KeyStroke.getKeyStroke(KeyEvent.VK_O,
                        KeyEvent.CTRL_DOWN_MASK), // 可 CTRL+
                        JComponent.WHEN_IN_FOCUSED_WINDOW); // 主界面focus

                openJMenuItem.setAccelerator() 各种菜单项
     */

}
