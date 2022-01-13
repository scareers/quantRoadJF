package com.scareers.gui.ths.simulation.interact.gui;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/14/014-02:31:47
 */

import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class Test {

    public static void main(String[] args) {
        JFrame jf = new JFrame("测试窗口");
        jf.setSize(2000, 1200);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        JButton logsFunc = ButtonFactory.getButton("日志输出");
        JButton terminalFunc = ButtonFactory.getButton("终端命令行");
        CorePanel corePanel = new CorePanel(100, 10, 30, 30, 30,
                Arrays.asList(ButtonFactory.getButton("对象查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据库", true)),
                Arrays.asList(ButtonFactory.getButton("书签", true)),
                Arrays.asList(logsFunc),
                Arrays.asList(terminalFunc),
                null
        );

        jf.setContentPane(corePanel);
        jf.setVisible(true);
    }

    public static void main0(String[] args) {
        JFrame jf = new JFrame("测试窗口");
        jf.setSize(2000, 1200);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JDesktopPane layeredPane = new JDesktopPane();

        // 层数: 100
        JPanel panel_100_1 = createPanel(Color.RED, "L=100, P=1", 0, 0, 200, 200);
        panel_100_1.add(ButtonFactory.getButton("按钮1"));
        layeredPane.add(panel_100_1, new Integer(100));

        // 层数: 200, 层内位置: 0（层内顶部）
        JPanel panel_200_0 = createPanel(Color.GREEN, "L=200, P=0", 100, 100, 200, 200);
        panel_200_0.add(ButtonFactory.getButton("按钮2"));
        layeredPane.add(panel_200_0, new Integer(200), 0);

        // 层数: 200, 层内位置: 1
        JPanel panel_200_1 = createPanel(Color.CYAN, "L=200, P=1", 200, 400, 200, 200);
        panel_200_1.add(ButtonFactory.getButton("按钮3"));
        layeredPane.add(panel_200_1, new Integer(200), 1);

        // 层数: 300
        JPanel panel_300 = createPanel(Color.YELLOW, "L=300", null, 600, 200, 200);
        panel_300.add(ButtonFactory.getButton("按钮4"));
        layeredPane.add(panel_300, new Integer(300));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(ButtonFactory.getButton("按钮5"));
        JButton six = ButtonFactory.getButton("按钮6");
        splitPane.setRightComponent(six);
        six.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (panel_300.isVisible()) {
                    System.out.println(layeredPane.getBounds());
                    panel_300.setSize(panel_300.getWidth() * 2 + 50, panel_300.getHeight() * 2 + 50);
                    panel_300.setVisible(false);
                } else {
                    panel_300.setVisible(true);
                }
            }
        });
        splitPane.setBounds(600, 600, 300, 300);
        splitPane.setContinuousLayout(true);
        layeredPane.add(splitPane, new Integer(400));

        jf.setContentPane(layeredPane);
        jf.setVisible(true);
    }

    /**
     * 创建一个面板容器（容器内包含一个水平方向居中, 垂直方向顶部对其的标签）
     *
     * @param bg     容器背景
     * @param text   容器内标签显示的文本
     * @param x      容器的横轴坐标
     * @param y      容器的纵坐标
     * @param width  容器的宽度
     * @param height 容器的高度
     * @return
     */
    private static JPanel createPanel(Color bg, String text, Integer x, Integer y, Integer width, Integer height) {
        // 创建一个 JPanel, 使用 1 行 1 列的网格布局
        JPanel panel = new JPanel(new GridLayout(1, 1));

        // 设置容器的位置和宽高
        if (x != null) {

            panel.setBounds(x, y, width, height);
        }

        // 设置 panel 的背景
        panel.setOpaque(true);
        panel.setBackground(bg);

        // 创建标签并设置相应属性
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.TOP);

        // 添加标签到容器
        panel.add(label);

        return panel;
    }

}

