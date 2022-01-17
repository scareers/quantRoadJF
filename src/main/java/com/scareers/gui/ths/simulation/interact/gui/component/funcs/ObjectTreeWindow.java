package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import org.fife.ui.rtextarea.RTextAreaEditorKit;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-07:16:15
 */
public class ObjectTreeWindow extends FuncFrameS {
    private static ObjectTreeWindow INSTANCE;

    public static ObjectTreeWindow getInstance(Type type, String title, TraderGui mainWindow,
                                               FuncButton belongBtn, boolean resizable, boolean closable,
                                               boolean maximizable,
                                               boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                                               double preferScale,
                                               int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        if (INSTANCE == null) {
            INSTANCE = new ObjectTreeWindow(type, title, mainWindow, belongBtn, resizable, closable, maximizable,
                    iconifiable, autoMaxWidthOrHeight, autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight,
                    halfWidthOrHeight, layer);
            INSTANCE.setIconifiable(false);
            INSTANCE.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    INSTANCE.getCorePanel().getMainDisplayWindow().flushBounds(true); // 编辑界面就像第一次刷新
                }
            });
            INSTANCE.setBorder(new LineBorder(COLOR_MAIN_DISPLAY_BORDER, 1));
        }
        INSTANCE.getFuncTools().setVisible(false);
        return INSTANCE;
    }

    public static ObjectTreeWindow getInstance() {
        return INSTANCE; // 可null
    }

    private ObjectTreeWindow(Type type, String title, TraderGui mainWindow,
                             FuncButton belongBtn, boolean resizable, boolean closable, boolean maximizable,
                             boolean iconifiable, int autoMaxWidthOrHeight, int autoMinWidthOrHeight,
                             double preferScale,
                             int funcToolsWidthOrHeight, boolean halfWidthOrHeight, Integer layer) {
        super(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer);
    }

    @Override
    public void initCenterComponent() { // 抽象方法
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        JTree tree = buildTree();
        tree.setBackground(COLOR_THEME_MINOR);
        jPanel.add(tree, BorderLayout.WEST);
        tree.setLocation(0, 0);
        JScrollPane jScrollPane = new JScrollPane(jPanel);
        jScrollPane.setBorder(null);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.centerComponent = jScrollPane;
        this.add(this.centerComponent, BorderLayout.CENTER);
    }

    @Override
    protected List<FuncButton> getToolButtons1() { // 工具栏可重写(两组按钮)
        return super.defaultToolsButtonList1();
    }

    @Override
    protected List<FuncButton> getToolButtons2() {
        return super.defaultToolsButtonList2();
    }

    public static class User {
        private String name;

        public User(String n) {
            name = n;
        }

        // 重点在toString，节点的显示文本就是toString
        @Override
        public String toString() {
            return name;
        }
    }

    private JTree buildTree() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("软件部");
        node1.add(new DefaultMutableTreeNode(new User("小花")));
        node1.add(new DefaultMutableTreeNode(new User("小虎")));
        node1.add(new DefaultMutableTreeNode(new User("小龙")));

        DefaultMutableTreeNode node2 = new DefaultMutableTreeNode("销售部");
        node2.add(new DefaultMutableTreeNode(new User("小叶")));
        node2.add(new DefaultMutableTreeNode(new User("小雯")));
        node2.add(new DefaultMutableTreeNode(new User("小夏")));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("职员管理");

        top.add(new DefaultMutableTreeNode(new User("总经理")));
        top.add(node1);
        top.add(node2);
        final JTree tree = new JTree(top);
        // 添加选择事件
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
                        .getLastSelectedPathComponent();

                if (node == null) {
                    return;
                }

                Object object = node.getUserObject();
                if (node.isLeaf()) {
                    User user = (User) object;
                    System.out.println("你选择了：" + user.toString());
                }

            }
        });
        return tree;
    }
}
