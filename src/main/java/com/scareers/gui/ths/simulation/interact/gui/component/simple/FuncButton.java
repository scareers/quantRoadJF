package com.scareers.gui.ths.simulation.interact.gui.component.simple;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * description: 特指 左/右/下 功能栏特殊使用的按钮, 可指定横竖项. 新增 rawText 保留构造参数文本. 并以此文本作为 "id"
 * 简单继承JButton, 该类 equals 未曾重写过, ==
 *
 * @author: admin
 * @date: 2022/1/12/012-19:15:22
 */
@Getter
@Setter
public class FuncButton extends JButton {
    public enum BtnType {
        HORIZONTAL,
        VERTICAL
    }

    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) {
        JFrame jf = new JFrame("测试窗口");
        jf.setSize(200, 200);
        jf.setLocationRelativeTo(null);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();

        // 创建一个按钮
        final JButton btn = new FuncButton("测试按钮", BtnType.HORIZONTAL);
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

    private String rawText; // 保留最原始字符串, 垂直button依然为正常字符串, 未html化
    private BtnType btnType; // 类型

    public FuncButton(String text, BtnType btnType) {
        this.rawText = text;
        this.btnType = btnType;
        if (btnType == BtnType.VERTICAL) {
            StringBuilder builder = new StringBuilder();
            builder.append("<html>");
            for (int i = 0; i < text.length(); i++) {
                builder.append(text.charAt(i));
                builder.append("<br>");
            }
            builder.append("</html>");
            this.setText(builder.toString());
        } else {
            this.setText(text);
        }
    }

    public FuncButton(String text) { // 默认横向, 普通按钮
        this(text, BtnType.HORIZONTAL);
    }
}
