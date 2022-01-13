package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * description: 自定义 FuncDialogS 抽象类, 逻辑意义是: 各种子功能使用的组件, 基组件是对话框
 * 主要添加刷新bounds 等方法, 该方法从 主窗口获取信息, 并更新自身 bounds.
 * 各个子功能的子窗口, 均需要继承此抽象类. 以便主窗口统一控制.
 * 例如当主窗口 reSize, 则遍历所有绑定的功能对话框, 它们均调用 flushBounds(), 且重绘.
 *
 * @author: admin
 * @date: 2022/1/13/013-07:27:42
 * @see flushBounds()
 */
@Setter
@Getter
public abstract class FuncDialogS extends JDialog {
    public enum OrientationType {
        VERTICAL,
        HORIZONTAL
    }

    OrientationType typeS; // 功能对话框2类, 横, 竖

    public abstract void flushBounds();

    public FuncDialogS(Window owner, String title, ModalityType type) {
        super(owner, title, type);
    }
}
