package com.scareers.gui.ths.simulation.interact.gui.util;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/13/013-02:29:20
 */
public class GuiCommonUtil {
    /**
     * 创建占位符Label, 空Label, 逻辑上占位作用
     *
     * @param width
     * @param height
     * @return
     */
    public static JLabel createPlaceholderLabel(int width, int height) {
        JLabel placeholderLabel = new JLabel();
        placeholderLabel.setSize(new Dimension(width, height));
        placeholderLabel.setPreferredSize(new Dimension(height, height));
        placeholderLabel.setBorder(null);
        return placeholderLabel;
    }


    /**
     * 给定JTree, 和 String的节点路径, 选择某个节点
     * @param tree
     * @param nodePath
     */
    public static void selectTreeNode(JTree tree, String nodePath) {
        DefaultMutableTreeNode defaultSelect = searchNode(nodePath,
                tree); // 查找这个节点点击
        TreeNode[] nodes = ((DefaultTreeModel) tree.getModel()).getPathToRoot(defaultSelect);  //有节点到根路径数组
        tree.setSelectionPath(new TreePath(nodes));
    }

    /**
     * 树查找节点以选择
     *
     * @param nodeStr
     * @param first
     * @return
     */
    private static DefaultMutableTreeNode searchNode(String nodeStr, JTree tree) {
        DefaultMutableTreeNode node = null;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) ((DefaultTreeModel) tree.getModel()).getRoot();
        Enumeration e = root.breadthFirstEnumeration();  //获取root下所有节点
        while (e.hasMoreElements()) {
            node = (DefaultMutableTreeNode) e.nextElement();
            if ((node.getUserObject().toString()).contains(nodeStr)) {
                return node;
            }
        }
        return null;
    }
}
