package com.scareers.gui.ths.simulation.interact.gui;

import javax.swing.*;

/**
 * description: 智能查找功能实现, 对话框!
 *
 * @author: admin
 * @date: 2022/6/6/006-06:58:36
 */
public class SmartFindDialog extends JDialog {
    public static int width = 300;
    public static int height = 400;

    TraderGui parentS; // 自定义属性, 不使用父类 owner属性
    JPanel contentPanel;

    public SmartFindDialog(TraderGui parent, String title, boolean modal) {
        super(parent, title, modal);
        this.parentS = parent;
        this.setSize(width, height); // 随后只需要设置 location 即可!
        this.setResizable(true);

//        dialog.setContentPane(chartPanel);
//        dialog.setVisible(true);
    }

    public void initContentPanel() {

    }

    /**
     * 重置位置, 将自身放在父亲右下角!
     */
    public void resetLocation() {
        if (this.parentS == null) {
            return;
        }

        this.setLocation(parentS.getX() + parentS.getWidth() - this.getWidth(),
                parentS.getY() + parentS.getHeight() - this.getHeight());
    }

}
