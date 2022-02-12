package com.scareers.gui.ths.simulation.interact.gui.component.combination.fs;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fs.FsFetcher;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.SecurityEmListCellRendererS;
import joinery.DataFrame;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: 1分钟分时图爬虫, 相关数据展示panel
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
public class FsFetcherListAndDataPanel extends JPanel {
    public static volatile FsFetcherListAndDataPanel INSTANCE;
    public static volatile Vector<SecurityBeanEm.SecurityEmPo> securityEmPos = new Vector<>(); // 开始空, 随后不变

    MainDisplayWindow mainDisplayWindow; // 主显示区
    volatile JList<SecurityBeanEm.SecurityEmPo> jList;

    public static FsFetcherListAndDataPanel getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            // 首次调用, 将开始更新数据
            ThreadUtil.execAsync(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    //等待 有控件被注册
                    try {
                        FsFetcher.waitInstanceNotNull();
                        FsFetcher.getInstance().waitFirstEpochFinish();
                    } catch (TimeoutException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    Console.log(securityEmPos);
                    securityEmPos = FsFetcher.getStockPool().stream()
                            .map(SecurityBeanEm.SecurityEmPo::new).collect(Collectors.toCollection(Vector::new));
                }
            }, true);

            INSTANCE = new FsFetcherListAndDataPanel(mainDisplayWindow); // 默认所有订单,自行调用changeType
        }
        Console.log(securityEmPos);
        return INSTANCE;
    }

    private FsFetcherListAndDataPanel(MainDisplayWindow mainDisplayWindow) {
        super();
        this.mainDisplayWindow = mainDisplayWindow;

        // 1.布局
        this.setLayout(new BorderLayout());

        // 2.JList显示列表
        jList = getSecurityEmJList();
        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(300, 10000));
        jList.setBackground(COLOR_THEME_TITLE);
        jList.setBorder(null);
        JScrollPane jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jList);
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        this.add(jScrollPaneForList, BorderLayout.WEST); // 添加列表

        // 3. 1分钟fs详情控件, 表格.
        Fs1MPanel fs1MPanel = new Fs1MPanel(this);

        this.add(fs1MPanel, BorderLayout.CENTER);

        // 6.主panel 添加尺寸改变监听. 改变 jList 和 orderContent尺寸
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setBounds(0, 0, 300, getHeight()); // 固定宽 300
                fs1MPanel.setBounds(300, 0, getWidth() - 300, getHeight()); // 其余占满
            }
        });

        // 7.更新选择的股票以显示分时图
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {

                while (true) {
                    SecurityBeanEm currentBean;
                    if (jList.getSelectedIndex() == -1) {
                        try {
                            if (securityEmPos.size() > 0) {
                                jList.setSelectedIndex(0); // 尝试选择第一个
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }
                    try {
                        currentBean = securityEmPos.get(jList.getSelectedIndex()).getBean();
                    } catch (Exception e) {
                        Thread.sleep(1); // 不断刷新
                        continue;
                    }

                    if (currentBean != null) {
                        fs1MPanel.updateText(currentBean);
                    }
                    Thread.sleep(100); // 不断刷新
                }
            }
        }, true);

    }

    private JList<SecurityBeanEm.SecurityEmPo> getSecurityEmJList() {
        DefaultListModelS<SecurityBeanEm.SecurityEmPo> model = new DefaultListModelS<>();
        model.flush(securityEmPos);

        JList<SecurityBeanEm.SecurityEmPo> jList = new JList<>(model);
        jList.setCellRenderer(new SecurityEmListCellRendererS());
        jList.setForeground(COLOR_GRAY_COMMON);

        // 单例单线程
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(securityEmPos);
                    Thread.sleep(100);
                }
            }
        }, true);

        return jList;
    }

    public static class Fs1MPanel extends JPanel {
        public static JTable jTable;
        public static JScrollPane jScrollPane = new JScrollPane();
        public static JButton buttonFlush; // 全量刷新按钮
        public static JButton buttonGraph; // 1分钟fs k线显示

        public static SecurityBeanEm preBean;
        //public static int anInt = 5; // 测试新数据增加
        public static boolean fullFlushFlag = false; // 强制全量刷新flag. 刷新一次自动false. 点击刷新按钮将自动全量刷新一次.

        /**
         * @param parent 仅用于位置修复
         */
        public Fs1MPanel(FsFetcherListAndDataPanel parent) {
            this.setBorder(null);
            this.setLayout(new BorderLayout());

            jScrollPane.setBorder(null);
            JLabel label = new JLabel("数据获取中");
            label.setForeground(Color.red);
            jScrollPane.setViewportView(label); // 占位
            jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);


            buttonFlush = ButtonFactory.getButton("全量刷新");
            buttonFlush.setMaximumSize(new Dimension(60, 20));
            buttonFlush.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fullFlushFlag = true; // 下一次将全量更新
                }
            });

            buttonGraph = ButtonFactory.getButton("简易分时图");
            buttonGraph.setMaximumSize(new Dimension(60, 20));
            buttonGraph.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fullFlushFlag = true; // 下一次将全量更新
                }
            });


            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            panel.add(buttonFlush);
            panel.setBorder(null);
            this.add(panel, BorderLayout.NORTH);
            this.add(jScrollPane, BorderLayout.CENTER);

            this.setBounds(300, 0, parent.getWidth() - 300, parent.getHeight());
            parent.mainDisplayWindow.flushBounds();
        }

        public void updateText(SecurityBeanEm currentBean) {
            if (jTable == null) {
                DataFrame<Object> fsDataOfStock = FsFetcher.getFsDatas().get(currentBean);
                Vector<Vector<Object>> datas = new Vector<>();
                for (int i = 0; i < fsDataOfStock.length(); i++) {
                    datas.add(new Vector<>(fsDataOfStock.row(i)));
                }
                Vector<Object> cols = new Vector<>(fsDataOfStock.columns());
                DefaultTableModel model = new DefaultTableModel(datas, cols);

                jTable = new JTable();
                jTable.setModel(model);
                jScrollPane.setViewportView(jTable);
            } else {
                DataFrame<Object> fsDataOfStock = FsFetcher.getFsDatas().get(currentBean);
                //fsDataOfStock = fsDataOfStock.slice(0, anInt);

                DefaultTableModel model = (DefaultTableModel) jTable.getModel();
                if (currentBean == preBean) { // 股票选中没变, 考虑增加行
                    if (fullFlushFlag) {
                        fullFlushFlag = false;
                        fullFlush(fsDataOfStock, model);
                        Console.log("已全量刷新");
                    } else { // 增量刷新
                        if (fsDataOfStock.length() > model.getRowCount()) { // 因保证必然有序, 所以只添加新的行.
                            int oldRowCount = model.getRowCount();
                            // 添加数据
                            for (int i = oldRowCount; i < fsDataOfStock.length(); i++) {
                                model.addRow(fsDataOfStock.row(i).toArray()); // 该方法将会自动增加行数,无需手动调用增加行数
                            }
                        }
                    }
                } else { // 股票选择变化, 则全量更新
                    fullFlush(fsDataOfStock, model);
                    // Console.log("已经切换股票");
                }
                preBean = currentBean;
                //anInt += 1;
            }
        }

        /**
         * 全量刷新逻辑
         *
         * @param fsDataOfStock
         * @param model
         */
        private void fullFlush(DataFrame<Object> fsDataOfStock, DefaultTableModel model) {
            Vector<Vector<Object>> datas = new Vector<>();
            for (int i = 0; i < fsDataOfStock.length(); i++) {
                datas.add(new Vector<>(fsDataOfStock.row(i)));
            }
            Vector<Object> cols = new Vector<>(fsDataOfStock.columns());
            model.setDataVector(datas, cols);
        }
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
