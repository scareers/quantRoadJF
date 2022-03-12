package com.scareers.gui.ths.simulation.interact.gui.ui.renderer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * description: 功能树器 树形控件渲染
 *
 * @author: admin
 * @date: 2022/1/18/018-11:18:40
 */
public class TreeCellRendererS extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object nodeData = node.getUserObject();
        this.setText(nodeData.toString());
//            this.setIcon();
        return this;
    }
}
