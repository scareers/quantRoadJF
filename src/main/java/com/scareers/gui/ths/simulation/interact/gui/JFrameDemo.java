//package com.scareers.gui.ths.simulation.interact.gui;
//
//import cn.hutool.core.thread.ThreadUtil;
//import cn.hutool.json.JSONUtil;
//import com.alee.laf.WebLookAndFeel;
//import com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher;
//import com.scareers.datasource.eastmoney.StockBean;
//import eu.verdelhan.ta4j.indicators.MACDIndicator;
//import joinery.DataFrame;
//import lombok.SneakyThrows;
//import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
//import org.xvolks.jnative.util.constants.COM;
//
//import javax.swing.*;
//import javax.swing.plaf.metal.MetalLookAndFeel;
//import javax.swing.plaf.nimbus.NimbusLookAndFeel;
//import java.awt.*;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.util.Arrays;
//import java.util.Vector;
//import java.util.concurrent.ConcurrentHashMap;
//
//import org.jb2011.lnf.beautyeye.BeautyEyeLookAndFeelCross;
//import org.jb2011.lnf.beautyeye.BeautyEyeLookAndFeelWin;
//
//import com.pagosoft.plaf.PgsLookAndFeel;
//
///**
// * description:
// *
// * @author: admin
// * @date: 2022/1/4/004-17:03:03
// */
//public class JFrameDemo {
//    public static void main(String[] args) throws Exception {
//        main0(args);
//    }
//
//    public static void main0(String[] agrs) throws Exception {
//
//        // https://www.cnblogs.com/starcrm/p/5025658.html
//        //        UIManager.setLookAndFeel(new MetalLookAndFeel());
//        //        UIManager.setLookAndFeel(new NimbusLookAndFeel());
//
//        WebLookAndFeel.install();
////                UIManager.setLookAndFeel(new PgsLookAndFeel());
//        //        BeautyEyeLNFHelper.launchBeautyEyeLNF(); // 不可用
//
//        JFrame jf = new JFrame("Java第二个GUI程序");    //创建一个JFrame对象
////        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        jf.setBounds(300, 100, 800, 500);    //设置窗口大小和位置
//        JPanel jp = new JPanel();    //创建一个JPanel对象
//
////        Image image = Toolkit.getDefaultToolkit().createImage()
////        ConcurrentHashMap<StockBean, DataFrame<Object>> fsTransactionDatas = new ConcurrentHashMap<>();
////        ConcurrentHashMap<StockBean, DataFrame<Object>> fsTransactionDatas = FSTransactionFetcher.fsTransactionDatas;
////        fsTransactionDatas.put(new StockBean("1.000001"), new DataFrame(Arrays.asList(Arrays.asList(1, 2),
////                Arrays.asList(3, 4))));
////        fsTransactionDatas.put(new StockBean("0.000001"), new DataFrame(Arrays.asList(Arrays.asList(10, 20),
////                Arrays.asList(30, 40))));
////        Vector<StockBean> stocks = new Vector<>(fsTransactionDatas.keySet());
////        JComboBox comboBox = new JComboBox(stocks);
//
//        JTextArea textArea = new JTextArea();
//        comboBox.addItemListener(new ItemListener() {
//            @SneakyThrows
//            @Override
//            public void itemStateChanged(ItemEvent e) {
//                if (e.getStateChange() == ItemEvent.SELECTED) {
//                    ThreadUtil.execAsync(new Runnable() {
//                        @SneakyThrows
//                        @Override
//                        public void run() {
////                            textArea.setText(fsTransactionDatas.get(comboBox.getSelectedItem()).toString(100));
//                        }
//                    }, false);
//
//                }
//            }
//        });
//
//
//        jp.setBackground(Color.white);    //设置背景色
//        jp.add(comboBox);
//        jp.add(textArea);
//        jf.add(jp);    //将面板添加到窗口
//        jf.setVisible(true);    //设置窗口可见
//        comboBox.setSelectedIndex(0);
//
//
//    }
//}
