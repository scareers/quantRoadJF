//package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;
//
//import cn.hutool.log.Log;
//import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
//import com.scareers.utils.log.LogUtil;
//import lombok.Getter;
//import lombok.Setter;
//
//import javax.swing.*;
//import java.awt.*;
//
///**
// * description: 自定义 FuncFrameS0 抽象类, 逻辑意义是: 各种子功能使用的组件, 基组件是 JInternalFrame
// * 主要添加刷新 bounds 等方法, 该方法从 主窗口获取信息, 并更新自身 bounds.
// * 各个子功能的子窗口, 均需要继承此抽象类. 以便主窗口统一控制.
// * 当主窗口 reSize, 则遍历所有绑定的 FuncFrameS0, 它们均调用 flushBounds(),  重绘.
// *
// * @author: admin
// * @date: 2022/1/13/013-07:27:42
// * @see flushBounds()
// */
//@Setter
//@Getter
//public abstract class FuncFrameS0 extends JInternalFrame {
//    public enum OrientationType {
//        VERTICAL_RIGHT, // 右侧功能栏, 编辑区也属于此, 常规尺寸较大且layer较低且无标题栏
//        VERTICAL_LEFT, // 左侧功能栏,  需要与编辑区尺寸互动
//        HORIZONTAL_BOTTOM // 底部功能栏
//    }
//
//    protected static final Log log = LogUtil.getLogger();
//    OrientationType typeS; // 功能窗口2类, 横, 竖
//    TraderGui mainWindow; // 主界面, 以便将自身添加到主内容面板 -- 层级面板中, 自行强转 TraderUI
//    JDesktopPane mainPane; // 主界面mainPane
//
//    // 自动计算
//    int mainPaneWidth; // 保存mainPane的 高度和宽度, 在构造器中首次更新. 当mainPane大小改变将使用并刷新此值
//    int mainPaneHeight; // 逻辑上含义为 上次mainPane的尺寸
//
//    public abstract void flushBounds(boolean first);
//
//    public void flushBounds() {
//        flushBounds(false); // 无参调用默认为非首次!
//    }
//
//    /**
//     * JInternalFrame 的全参构造器 + 新增2属性
//     *
//     * @param mainWindow
//     * @param typeS
//     * @param title
//     * @param resizable
//     * @param closable
//     * @param maximizable
//     * @param iconifiable
//     */
//    protected FuncFrameS0(TraderGui mainWindow, OrientationType typeS, String title, boolean resizable, boolean closable,
//                          boolean maximizable, boolean iconifiable) {
//        super(title, resizable, closable, maximizable, iconifiable);
//        this.typeS = typeS;
//        this.mainWindow = mainWindow;
//        this.mainPane = this.mainWindow.getCorePanel().getMainPane();
//        this.mainPaneWidth = this.mainPane.getWidth();
//        this.mainPaneHeight = this.mainPane.getHeight();
//        this.setBorder(null);
//        this.setLayout(new BorderLayout());
//    }
//
//}
