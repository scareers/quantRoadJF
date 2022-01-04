package com.scareers.gui.ths.simulation.interact.gui;

import com.alee.laf.WebLookAndFeel;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.xvolks.jnative.util.constants.COM;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;

import org.jb2011.lnf.beautyeye.BeautyEyeLookAndFeelCross;
import org.jb2011.lnf.beautyeye.BeautyEyeLookAndFeelWin;

import com.pagosoft.plaf.PgsLookAndFeel;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 */
public class JFrameDemo {


    public static void main(String[] agrs) throws Exception {
        // https://www.cnblogs.com/starcrm/p/5025658.html
        //        UIManager.setLookAndFeel(new MetalLookAndFeel());
        //        UIManager.setLookAndFeel(new NimbusLookAndFeel());

        WebLookAndFeel.install();
//                UIManager.setLookAndFeel(new PgsLookAndFeel());
        //        BeautyEyeLNFHelper.launchBeautyEyeLNF(); // 不可用

        JFrame jf = new JFrame("Java第二个GUI程序");    //创建一个JFrame对象
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setBounds(300, 100, 400, 200);    //设置窗口大小和位置
        JPanel jp = new JPanel();    //创建一个JPanel对象
        JLabel jl = new JLabel("这是放在JPanel上的标签");    //创建一个标签
        jp.setBackground(Color.white);    //设置背景色
        jp.add(jl);    //将标签添加到面板
        jf.add(jp);    //将面板添加到窗口
        jf.setVisible(true);    //设置窗口可见
    }
}
