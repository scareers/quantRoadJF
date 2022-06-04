package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityBeanEm.SecurityEmPo;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.SecurityEmListCellRendererS;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static java.awt.event.KeyEvent.VK_ENTER;

/**
 * description: 转债全市场(全局)复盘panel;
 * 主展示页面, 左转债列表, 带搜索栏; 右为动态分时图 -- 使用东财数据库数据实现的仿真
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
@Getter
public class BondGlobalSimulationPanel extends JPanel {
    private static BondGlobalSimulationPanel INSTANCE;

    public static BondGlobalSimulationPanel getInstance(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        if (INSTANCE == null) {
            INSTANCE = new BondGlobalSimulationPanel(mainDisplayWindow, jListWidth);
        }
        return INSTANCE;
    }

    /**
     * JFrame frame = new JFrame("temp");
     * <p>
     * ChartPanel chartPanel = new ChartPanel(chart);
     * <p>
     * <p>
     * // 大小
     * chartPanel.setPreferredSize(new Dimension(1200, 800));
     * chartPanel.setMouseZoomable(false);
     * chartPanel.setRangeZoomable(false);
     * chartPanel.setDomainZoomable(false);
     * <p>
     * chartPanel.addChartMouseListener(getCrossLineListenerForFsXYPlot(allFsTimeTicks));
     * <p>
     * frame.setLayout(new BorderLayout());
     * //            frame.setContentPane(chartPanel);
     * <p>
     * JPanel panelRight = new JPanel();
     * panelRight.setPreferredSize(new Dimension(323, 1024));
     * <p>
     * ManipulateLogPanel displayForLog = new ManipulateLogPanel();
     * logTextPane = displayForLog.getLogTextPane(); // 操作.
     * logTextPane.setBackground(new Color(0, 0, 0));
     * <p>
     * panelRight.setLayout(new BorderLayout());
     * jScrollPane = new JScrollPane(logTextPane);
     * jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
     * BasicScrollBarUIS
     * .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
     * //            jScrollPane.getViewport().setBackground(Color.red);
     * panelRight.add(jScrollPane, BorderLayout.CENTER);
     * <p>
     * <p>
     * frame.add(panelRight, BorderLayout.EAST);
     * frame.add(chartPanel, BorderLayout.CENTER);
     * <p>
     * <p>
     * frame.pack(); // 显示.
     * // @noti: 这里由例子中的 org.jfree.ui.RefineryUtilities;变为了 org.jfree.chart.ui.UIUtils;
     * frame.setVisible(true);
     */

    protected volatile Vector<SecurityBeanEm.SecurityEmPo> securityEmPos = new Vector<>(); // 转债列表对象
    protected volatile JXList jListForBonds; //  转债展示列表控件
    protected int jListWidth; // 列表宽度, 例如300
    protected MainDisplayWindow mainDisplayWindow; // 主显示区

    JPanel panelLeft; // 左panel, 显示列表和搜索等. 列表在下, 各种功能按钮组在上!
    JPanel panelMainForRevise; // 主要的复盘区域panel, 在右

    protected BondGlobalSimulationPanel(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        // 异步开始等待某些状态, 并一次或者持续刷新股票列表
        this.jListWidth = jListWidth;
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout()); // border布局, 列表在左, 其余在右; 总宽度为展示区; 列表固定宽

        // 1.左panel 初始化和组装
        buildLeftPanel();
        this.add(panelLeft, BorderLayout.WEST); // 左

        // 2. 右panel
        buildMainPanel();
        this.add(panelMainForRevise, BorderLayout.CENTER); // 中

        // 3.主 展示窗口 添加尺寸改变监听. 改变 jList 和 orderContent尺寸.
        this.mainDisplayWindow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelLeft.setBounds(0, 0, jListWidth, getHeight()); // 固定宽默认 300
                panelMainForRevise.setBounds(panelLeft.getWidth(), 0,
                        getWidth() - panelLeft.getWidth()
                        , getHeight());
                panelMainForRevise.repaint();
            }
        });
    }

    /**
     * 主panel
     */
    private void buildMainPanel() {
        panelMainForRevise = new JPanel();
        JLabel x = new JLabel("测试label");
        panelMainForRevise.add(x);
    }

    JPanel functionPanel; // 功能按钮区 在左上
    JXFindBarS jxFindBarS; // 常规的查找转债列表 -- 查找控件

    /**
     * 组装左panel
     * 上功能区, 下列表区!
     *
     * @return
     */
    private void buildLeftPanel() {
        panelLeft = new JPanel();
        panelLeft.setLayout(new BorderLayout());
        panelLeft.setPreferredSize(new Dimension(jListWidth, 2048));

        // 1.上功能区
        initFunctionPanel();

        // 2.转债列表
        jListForBonds = getSecurityEmJList(); // 已经实现自动读取并刷新 securityEmPos 属性
        initJListWrappedJScrollPane(); // 列表被包裹

        // 3.新panel包裹转债列表, 以及附带的查找框
        JPanel panelListContainer = new JPanel();
        panelListContainer.setLayout(new BorderLayout());
        jxFindBarS = new JXFindBarS(Color.red);
        panelListContainer.add(jxFindBarS, BorderLayout.NORTH);
        panelListContainer.add(jScrollPaneForList, BorderLayout.CENTER);

        // 4.最后组装
        panelLeft.add(functionPanel, BorderLayout.NORTH);
        panelLeft.add(panelListContainer, BorderLayout.CENTER);

    }

    /**
     * 功能区初始化
     */
    private void initFunctionPanel() {
        functionPanel = new JPanel();
        functionPanel.setPreferredSize(new Dimension(jListWidth, 300));

        functionPanel.setLayout(new GridLayout(2, 2, -1, -1)); // 网格布局按钮

        // @key: 各种功能按钮!
        FuncButton loadBondListButton = ButtonFactory.getButton("刷新列表");
        loadBondListButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) { // 点击加载或刷新转债列表;
                List<SecurityBeanEm> bondList = SecurityBeanEm.createBondList(Arrays.asList("小康转债", "卡倍转债"), false);
                securityEmPos = SecurityEmPo.fromBeanList(bondList); // 更新
            }
        });


        functionPanel.add(loadBondListButton);


    }

    JScrollPane jScrollPaneForList;

    private void initJListWrappedJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jListForBonds); // 滚动包裹转债列表
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }

    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private JXList getSecurityEmJList() {
        // securityEmPos --> 自行实现逻辑, 改变自身该属性; 则 列表将自动刷新

        DefaultListModelS<SecurityBeanEm.SecurityEmPo> model = new DefaultListModelS<>();
        model.flush(securityEmPos); // 刷新一次数据, 首次为空

        JXList jList = new JXList(model);
        jList.setCellRenderer(new SecurityEmListCellRendererS()); // 设置render
        jList.setForeground(COLOR_GRAY_COMMON);

        // 持续刷新列表, 100 ms一次. securityEmPos 应该为持续变化
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(securityEmPos);
                    if (jListForBonds != null) {
                        jxFindBarS.setSearchable(jListForBonds.getSearchable());
                    }
                    Thread.sleep(100);
                }
            }
        }, true);

        // 双击事件监听, 跳转到东方财富资产行情页面
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // 非双击
                    return;
                }
                int index = jList.getSelectedIndex();
                SecurityBeanEm.SecurityEmPo po = (SecurityEmPo) jList.getModel().getElementAt(index);
                openSecurityQuoteUrl(po);
            }
        });
        jList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == VK_ENTER) {
                    int index = jList.getSelectedIndex();
                    SecurityBeanEm.SecurityEmPo po = (SecurityEmPo) jList.getModel().getElementAt(index);
                    openSecurityQuoteUrl(po);
                }
            }
        });

        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(jListWidth, 10000));
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        return jList;
    }

    public static void openSecurityQuoteUrl(SecurityBeanEm.SecurityEmPo po) {
        String url = null;
        SecurityBeanEm bean = po.getBean();
        if (bean.isBK()) {
            url = StrUtil.format("http://quote.eastmoney.com/bk/{}.html", bean.getQuoteId());
        } else if (bean.isIndex()) {
            url = StrUtil.format("http://quote.eastmoney.com/zs{}.html", bean.getSecCode());
        } else if (bean.isStock()) {
            if (bean.isHuA() || bean.isHuB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sh", po.getSecCode());
            } else if (bean.isShenA() || bean.isShenB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sz", po.getSecCode());
            } else if (bean.isJingA()) {
                url = StrUtil.format("http://quote.eastmoney.com/bj/{}.html", po.getSecCode());
            } else if (bean.isXSB()) {
                url = StrUtil.format("http://xinsanban.eastmoney.com/QuoteCenter/{}.html", po.getSecCode());
            } else if (bean.isKCB()) {
                url = StrUtil.format("http://quote.eastmoney.com/kcb/{}.html", po.getSecCode());
            }
        } else if (bean.isBond()) {
            if (bean.getMarket() == 0) {
                url = StrUtil.format("http://quote.eastmoney.com/sz{}.html", po.getSecCode());
            } else if (bean.getMarket() == 1) {
                url = StrUtil.format("http://quote.eastmoney.com/sh{}.html", po.getSecCode());
            }
        }

        if (url == null) {
            log.warn("未知资产类别, 无法打开行情页面: {}", bean.getName(), bean.getSecurityTypeName());
            return;
        }
        CommonUtil.openUrlWithDefaultBrowser(url);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    private static final Log log = LogUtil.getLogger();
}
