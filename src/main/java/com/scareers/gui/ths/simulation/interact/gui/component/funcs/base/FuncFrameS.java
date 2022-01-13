package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * description: 自定义 FuncFrameS 抽象类, 逻辑意义是: 各种子功能使用的组件, 基组件是 JInternalFrame
 * 主要添加刷新 bounds 等方法, 该方法从 主窗口获取信息, 并更新自身 bounds.
 * 各个子功能的子窗口, 均需要继承此抽象类. 以便主窗口统一控制.
 * 当主窗口 reSize, 则遍历所有绑定的 FuncFrameS, 它们均调用 flushBounds(), 重绘.
 *
 * @author: admin
 * @date: 2022/1/13/013-07:27:42
 * @see flushBounds()
 */
@Setter
@Getter
public abstract class FuncFrameS extends JInternalFrame {
    public enum OrientationType {
        VERTICAL,
        HORIZONTAL
    }

    protected static final Log log = LogUtil.getLogger();
    OrientationType typeS; // 功能窗口2类, 横, 竖
    JFrame mainWindow; // 主界面, 以便将自身添加到主内容面板 -- 层级面板中, 自行强转 TraderUI

    public abstract void flushBounds();

    /**
     * JInternalFrame 的全参构造器 + 新增2属性
     *
     * @param mainWindow
     * @param typeS
     * @param title
     * @param resizable
     * @param closable
     * @param maximizable
     * @param iconifiable
     */
    protected FuncFrameS(JFrame mainWindow, OrientationType typeS, String title, boolean resizable, boolean closable,
                         boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);
        this.typeS = typeS;
        this.mainWindow = mainWindow;
        this.setLayout(new BorderLayout());
    }

}
