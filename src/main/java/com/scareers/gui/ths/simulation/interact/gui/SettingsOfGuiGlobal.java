package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;

import javax.swing.*;
import java.awt.*;
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

    // 1.1.东财k线图
    public static Color COLOR_CHART_BG_EM = new Color(7, 7, 7); // 东方财富图标主背景色
    public static Color COLOR_CHART_AXIS_LINE_EM = new Color(38, 38, 38); // 东方财富图表两轴颜色
    public static Color COLOR_CHART_GRID_LINE_EM = new Color(38, 38, 38); // 东方财富图表网格颜色
    public static Color COLOR_CHART_CROSS_LINE_EM = new Color(120, 120, 120); // 东方财富图表十字线颜色
    public static Color COLOR_TEXT_INACTIVATE_EM = new Color(140, 140, 140); // 东方财富未激活的文字颜色, 偏灰,激活白
    public static Color COLOR_CURRENT_PRICE_MARKER = new Color(255, 198, 109); // 表示当前价格的marker,偏棕黄色
    // 1.2.东财列表
    public static Color COLOR_LIST_FLAT_EM = new Color(200, 200, 200); // 表示数据持平, 例如涨幅 0%时
    public static Color COLOR_LIST_RAISE_EM = new Color(255, 92, 92); // 表示上涨, 红色
    public static Color COLOR_LIST_FALL_EM = new Color(57, 207, 101); // 表示下跌, 绿色
    public static Color COLOR_LIST_STOCK_NAME_EM = new Color(245, 253, 166); // 名称默认金黄色
    public static Color COLOR_LIST_BK_EM = new Color(7, 7, 7); // 列表背景色
    public static Color COLOR_LIST_HEADER_FORE_EM = new Color(93, 93, 93); // 表头字颜色. 背景色同 BK


    public static Color COLOR_MAIN_DISPLAY_BORDER = new Color(50); // 编辑器边框

    public static Color colorTest = new Color(51); // 测试颜色.., 方便看..

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
    public static Integer layerOfManiLogFuncWindow = 201;
    public static Integer layerOfDatabaseFuncWindow = 150;
    public static Integer layerOfMainDisplay = 100; // 低
    public static Integer layerOfObjectsTree = 100; // 低

    // 4.全局快捷键设置  ks: key shortcut
    public static final KeyStroke OBJECT_TREE_KS = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);

    // 5.程序启动后, 自动启动小程序设置
    public static boolean autoStartTrader = false; // 设置是否自动启动Trader自动交易
    public static final boolean autoStartEmNewFetcher = false; // 自动启动 东财新闻抓取(web api)
    public static final boolean autoNewConceptDiscover = true; // 自动启动 新概念发现程序
    public static final boolean autoEmPc724NewsNotify = false; // 自动启动 东财新闻发现程序(pc api) 7*24
    public static final boolean autoEmPcHotNewsNotify = true; // 自动启动 东财热门资讯

    // 自动打开的窗口
    public static final boolean autoOpenLogsWindow = false; // 打开日志窗口
    public static final boolean autoOpenManiLogsWindow = true; // 打开操作日志窗口
    public static final boolean autoOpenFuncTree = true; // 打开左侧功能树

}
