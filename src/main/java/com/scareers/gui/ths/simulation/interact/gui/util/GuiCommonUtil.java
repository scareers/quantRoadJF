package com.scareers.gui.ths.simulation.interact.gui.util;

import cn.hutool.core.util.StrUtil;
import com.scareers.gui.ths.simulation.order.Order;

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
     * 给定JTree, 和 String的节点路径, 选择某个节点.
     * nodePath 全路径字符串
     *
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
     * 树查找节点以选择, 全路径字符串
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
            if (new TreePath(node.getPath()).toString().equals(nodeStr)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 将 json字符串转换为 html形式, 方便 JLabel等 显示
     *
     * @param jsonPrettyStr
     * @return
     */
    public static String jsonStrToHtmlFormat(String jsonPrettyStr) {
        jsonPrettyStr = "<html>" + StrUtil.replace(jsonPrettyStr, "\n", "<br/>") + "</html>";
        // @noti: hutool 的pretty json以空格为格式, fastjson以 \t, 大约相当于4个空格
        jsonPrettyStr = StrUtil.replace(jsonPrettyStr, "\t", "&ensp;&ensp;&ensp;&ensp;");
        //        jsonPrettyStr = StrUtil.replace(jsonPrettyStr, " ", "&ensp;");
        return jsonPrettyStr;
    }

    private static void setLabelColorIfChange(Component label, Color newColor) {
        if (!label.getForeground().equals(newColor)) {
            label.setForeground(newColor);
        }
    }

    public static void setLabelForeColorByOrderLifePoint(Order order, Component label) {
        Order.LifePointStatus status = order.getLastLifePoint().getStatus();
        if (status == Order.LifePointStatus.EXECUTING) {
            setLabelColorIfChange(label, Color.yellow);
        } else if (status == Order.LifePointStatus.FINISH_EXECUTE) {
            setLabelColorIfChange(label, Color.gray);
        } else if (status == Order.LifePointStatus.CHECKING) {
            setLabelColorIfChange(label, Color.pink);
        } else if (status == Order.LifePointStatus.CHECKED) {
            setLabelColorIfChange(label, Color.blue);
        } else if (status == Order.LifePointStatus.RESENDED) {
            setLabelColorIfChange(label, Color.black);
        } else if (status == Order.LifePointStatus.FINISH) {
            setLabelColorIfChange(label, Color.cyan);
        } else if (status == Order.LifePointStatus.FAIL_FINALLY) {
            setLabelColorIfChange(label, Color.red);
        } else {
            setLabelColorIfChange(label, Color.gray);
        }
    }
//
//    public static void setLabelForeColorByOrderLifePoint(Order order, Component label) {
//        Order.LifePointStatus status = order.getLastLifePoint().getStatus();
//        if (status == Order.LifePointStatus.EXECUTING) {
//            setLabelColorIfChange(label, Color.yellow);
//        } else if (status == Order.LifePointStatus.FINISH_EXECUTE) {
//            setLabelColorIfChange(label, Color.gray);
//        } else if (status == Order.LifePointStatus.CHECKING) {
//            setLabelColorIfChange(label, Color.pink);
//        } else if (status == Order.LifePointStatus.CHECKED) {
//            label.setForeground(Color.blue);
//        } else if (status == Order.LifePointStatus.RESENDED) {
//            label.setForeground(Color.black);
//        } else if (status == Order.LifePointStatus.FINISH) {
//            label.setForeground(Color.CYAN);
//        } else if (status == Order.LifePointStatus.FAIL_FINALLY) {
//            label.setForeground(Color.red);
//        } else {
//            label.setForeground(Color.gray);
//        }
//    }

    /**
     * 常规对话框, 背景色为主色调, 文字默认黑色不清晰;
     * 本方法将 常规内容, 转换为 html,
     * 分两部分内容和对应地 两个颜色.
     * 第一部分为提示标题, 第二部分为核心信息
     *
     * @param infoContent
     * @param coreContent
     * @param color1
     * @param color2
     * @return
     * @noti : 内容可能太长, 这里分行 80个字符
     */
    public static String buildDialogShowStr(String infoContent, String coreContent, String color1, String color2) {
        coreContent = splitLineWithBr(coreContent, 80);
        String res = StrUtil.format("<html><h2><font color='{}'>{}</font></h2>" +
                "<br>" +
                "<p><font color='{}'>{}</font></p>" +
                "</html>", color1, infoContent, color2, coreContent);
        return res;
    }

    private static String splitLineWithBr(String content, int amount) {
        double lineCount = Math.ceil(content.length() * 1.0 / amount); // 行数
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            stringBuilder.append(content.substring(i * amount, Math.min(content.length(), (i + 1) * amount)));
            stringBuilder.append("<br>");
        }
        return stringBuilder.toString();
    }

    /**
     * 默认黄色+红色. 注意, 内容字符串别包含 {}
     *
     * @param infoContent
     * @param coreContent
     * @return
     */
    public static String buildDialogShowStr(String infoContent, String coreContent) {
        return buildDialogShowStr(infoContent, coreContent, "yellow", "red");
    }

}
