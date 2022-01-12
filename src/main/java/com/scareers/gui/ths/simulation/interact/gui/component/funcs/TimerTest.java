/**
 * 该包均为 左右下功能栏按钮, 回调, 创建的各个子功能交互窗口(或其他组件)
 */
package com.scareers.gui.ths.simulation.interact.gui.component.funcs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/13/013-04:28:43
 */
public class TimerTest extends JFrame {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static JButton button;
    private static TimerTest TimerTest;

    public static void main(String[] args) {
        TimerTest = new TimerTest();
        button = new JButton("按我");
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TimeDialog d = new TimeDialog();
                int result = d.showDialog(TimerTest, "对方想要和你语音是否接受?", 10);// TimerTest是程序主窗口类，弹出的对话框10秒后消失
                System.out.println("===result: " + result);
            }
        });

        button.setBounds(2, 5, 80, 20);
        TimerTest.getContentPane().setLayout(null);
        TimerTest.getContentPane().add(button);
        TimerTest.setSize(new Dimension(400, 200));
        TimerTest.setLocation(500, 200);
        TimerTest.setVisible(true);
        TimerTest.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

}


class TimeDialog {
    private String message = null;
    private int secends = 0;
    private JLabel label = new JLabel();
    private JButton confirm, cancel;
    private JDialog dialog = null;
    int result = -5;

    public int showDialog(JFrame father, String message, int sec) {
        this.message = message;
        secends = sec;
        label.setText(message);
        label.setBounds(80, 6, 200, 20);
        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        confirm = new JButton("接受");
        confirm.setBounds(100, 40, 60, 20);
        confirm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = 0;
                TimeDialog.this.dialog.dispose();
            }
        });
        cancel = new JButton("拒绝");
        cancel.setBounds(190, 40, 60, 20);
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                result = 1;
                TimeDialog.this.dialog.dispose();
            }
        });

        dialog = new JDialog(father, true);
        dialog.setTitle("提示: 本窗口将在" + secends + "秒后自动关闭");
        dialog.setLayout(null);
        dialog.add(label);
        dialog.add(confirm);
        dialog.add(cancel);
        s.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                TimeDialog.this.secends--;
                if (TimeDialog.this.secends == 0) {
                    TimeDialog.this.dialog.dispose();
                } else {
                    dialog.setTitle("提示: 本窗口将在" + secends + "秒后自动关闭");
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
        dialog.pack();
        dialog.setSize(new Dimension(350, 100));
        dialog.setLocationRelativeTo(father);
        dialog.setVisible(true);
        return result;

    }

}
