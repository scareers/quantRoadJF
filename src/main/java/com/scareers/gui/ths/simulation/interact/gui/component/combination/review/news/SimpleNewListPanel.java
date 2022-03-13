package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;

import javax.swing.*;
import java.awt.*;

/**
 * description: 简单的新闻列表显示 Panel; 简单继承 update()方法
 * 左边为编辑区, 右边表格显示 !
 *
 * @author: admin
 * @date: 2022/3/13/013-08:50:46
 */
public abstract class SimpleNewListPanel extends DisplayPanel {

    JPanel editorPanel;
    JPanel tablePanel;

    public SimpleNewListPanel() {
        this.setLayout(new BorderLayout());


    }

    public void updateNewList() {
        this.update();
    }
}
