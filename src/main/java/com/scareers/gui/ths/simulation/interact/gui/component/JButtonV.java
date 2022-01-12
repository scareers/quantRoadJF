package com.scareers.gui.ths.simulation.interact.gui.component;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * description: 文字默认竖直的 button, 继承 JButton, 仅对 text做处理.
 * 新增参数需要传递: textAlianType: FROM_TOP_TO_BUTTOM / FROM_BUTTOM_TO_TOP 控制文字从上到下还是从下到上读
 *
 * @author: admin
 * @date: 2022/1/12/012-19:15:22
 */
public class JButtonV extends JButton {
    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) {
        JFrame jf = new JFrame("测试窗口");
        jf.setSize(200, 200);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();

        // 创建一个按钮
        final JButton btn = new JButtonV("测试按钮");

        // 添加按钮的点击事件监听器
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("按钮被点击");
            }
        });

        panel.add(btn);

        jf.setContentPane(panel);
        jf.setVisible(true);

    }

    public JButtonV(String text) {

        StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        for (int i = 0; i < text.length(); i++) {
            builder.append(text.charAt(i));
            builder.append("<br>");
        }
        builder.append("</html>");
//        // super(text, icon) 实际调用!
        setModel(new DefaultButtonModel());
        init(builder.toString(), null);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // 全画一遍, 然后
        //
    }

}
