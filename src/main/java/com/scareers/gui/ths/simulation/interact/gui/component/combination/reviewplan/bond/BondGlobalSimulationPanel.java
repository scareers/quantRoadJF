package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityBeanEm.SecurityEmPo;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.SecurityEmListCellRendererS;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.CrossLineListenerForFsXYPlot;
import com.scareers.utils.charts.EmChart;
import com.scareers.utils.charts.EmChart.DynamicEmFs1MV2ChartForRevise;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdesktop.swingx.JXList;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

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

    public static final int tick3sLogPanelWidth = 323; // 3stick数据显示组件宽度

    protected volatile Vector<SecurityBeanEm.SecurityEmPo> securityEmPos = new Vector<>(); // 转债列表对象
    protected volatile JXList jListForBonds; //  转债展示列表控件
    protected SecurityBeanEm selectedBean = null; // 被选中的转债 东财bean对象
    protected SecurityBeanEm preChangedSelectedBean = null; // 此前被选中,且更新过fs图对象, 当新的等于它时, 将不重新实例化动态图表对象
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

    DynamicEmFs1MV2ChartForRevise dynamicChart; // 随时更新对象
    ChartPanel chartPanel; // 更新时: 仅需要更新 内部chart对象;
    JPanel panelOfTick3sLog; // 更新时: 仅需将新 dynamicChart 的log组件, add到其center即可
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlot; // 监听器, 更新时, 需要更新其时间列表,否则可能出现问题

    /**
     * 更新分时图显示 主 区; 它读取自身属性, selectedBean, 以及设置区设置的 日期 ! 实例化 DynamicEmFs1MV2ChartForRevise 对象
     * 它要求 selectedBean 已设置不为 null;
     */
    public void updateFsDisplay() {
        if (selectedBean == null || this.selectedBean.equals(this.preChangedSelectedBean)) {
            Console.log("xx");
            return; // 为空或者未改变, 不会重新实例化 动态分时图表 对象
        }
        // 1.实例化动态图表
        dynamicChart = new DynamicEmFs1MV2ChartForRevise(selectedBean, getDateStr());
        preChangedSelectedBean = this.selectedBean; // 更新了图表对象时, 才更新

        // 3. 更新chart对象, 刷新!
        chartPanel.setChart(dynamicChart.getChart());
        crossLineListenerForFsXYPlot.setTimeTicks(dynamicChart.getAllFsTimeTicks()); // 保证十字线正常
        panelOfTick3sLog.removeAll(); // 需要删除才能保证只有一个
        JScrollPane jScrollPaneForTickLog = dynamicChart.getJScrollPaneForTickLog();
        panelOfTick3sLog.setPreferredSize(new Dimension(tick3sLogPanelWidth, panelMainForRevise.getHeight()));
        jScrollPaneForTickLog
                .setPreferredSize(new Dimension(tick3sLogPanelWidth, panelOfTick3sLog.getHeight())); // 容器同宽
        jScrollPaneForTickLog.setLocation(0, 0);
        jScrollPaneForTickLog.setBorder(null);
        panelOfTick3sLog.add(jScrollPaneForTickLog, BorderLayout.CENTER);
    }

    public String getDateStr() {
        return "2022-06-02"; // 读取设定的日期, 分时图显示
    }

    JPanel functionContainerMain;

    /**
     * 主panel -- 对控制复盘的按钮, 还是应当放在本panel 最上方, 以便控制
     */
    private void buildMainPanel() {
        panelMainForRevise = new JPanel();
        panelMainForRevise.setLayout(new BorderLayout());

        // 1.复盘,分时图,等相关功能区
        functionContainerMain = new JPanel();
        functionContainerMain.setLayout(new FlowLayout(FlowLayout.LEFT));
        functionContainerMain.setPreferredSize(new Dimension(2048, 50));
        functionContainerMain.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        addMainFunctions(); // 主功能区按钮

        // 2.分时图(或未来k线)显示区
        JPanel fsMainPanel = new JPanel(); // fs和tick容器
        fsMainPanel.setLayout(new BorderLayout());

        chartPanel = new ChartPanel(null); // 图表
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);
        crossLineListenerForFsXYPlot =
                EmChart.getCrossLineListenerForFsXYPlot(CommonUtil.generateMarketOpenTimeListHm(false));
        chartPanel // 注意, 必须要求 东财1分钟分时图, 241 行; 即使用 v2 版本的东财api; 同同花顺默认;但更新chart时应当刷新
                .addChartMouseListener(crossLineListenerForFsXYPlot);
        panelOfTick3sLog = new JPanel();  // tick显示
        panelOfTick3sLog.setLayout(new BorderLayout());
        JLabel tempLabel = new JLabel("暂无数据");
        tempLabel.setPreferredSize(new Dimension(tick3sLogPanelWidth, 1024));
        tempLabel.setBackground(Color.black);
        tempLabel.setForeground(Color.red);
        panelOfTick3sLog.add(tempLabel, BorderLayout.CENTER);

        // 2.1. 加入两大组件
        fsMainPanel.add(chartPanel, BorderLayout.CENTER);
        fsMainPanel.add(panelOfTick3sLog, BorderLayout.EAST);


        // 3.组装
        panelMainForRevise.add(fsMainPanel, BorderLayout.CENTER);
        panelMainForRevise.add(functionContainerMain, BorderLayout.NORTH);
    }

    /**
     * 主功能区组件添加, 添加到 functionContainerMain, 该panel为左浮动布局
     */
    private void addMainFunctions() {
        // 4.主功能区!
        FuncButton flushFs = ButtonFactory.getButton("刷新分时");

        flushFs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                double timeRate = 5;
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        List<DateTime> allFsTransTimeTicks = CommonUtil.generateMarketOpenTimeListHms(false);
                        for (int i = 0; i < allFsTransTimeTicks.size(); i++) {
                            DateTime tick = allFsTransTimeTicks.get(i);
                            ThreadUtil.sleep((long) (1000 / timeRate));
                            Console.log("即将刷新");
                            dynamicChart.updateChartFsTrans(tick); // 重绘图表
                        }
                    }
                }, true);
            }
        });


        functionContainerMain.add(flushFs);
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
     * 功能区上方, 显示 当前选中的bean 的基本信息; 例如概念, 行业,余额等;  --> 最新背诵信息
     * 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
     * 113537	文灿转债	278.03	1.4亿	20190705	40.6	603348	文灿股份	交运设备-汽车零部件-汽车零部件Ⅲ	蔚来汽车概念;新能源汽车;特斯拉	41.8	130.49亿
     */
    public static class SelectBeanDisplayPanel extends DisplayPanel {
        public static DataFrame<Object> allBondInfoDfForRevise = null; // 背诵字段df; 仅载入一次
        public static ConcurrentHashMap<String, List<Object>> allBondInfoForReviseMap = new ConcurrentHashMap<>(); //

        SecurityBeanEm bondBean;

        JLabel bondInfoLabel = getCommonLabel();
        JLabel stockInfoLabel = getCommonLabel();
        JLabel industryInfoLabel = getCommonLabel();
        JLabel conceptInfoLabel = getCommonLabel();

        public SelectBeanDisplayPanel() {
            this.setLayout(new GridLayout(4, 1, -1, -1)); // 4行1列;
            this.add(bondInfoLabel);
            this.add(stockInfoLabel);
            this.add(industryInfoLabel);
            this.add(conceptInfoLabel);

            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        allBondInfoDfForRevise = BondUtil.generateCSVForRecite1();
                        if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                            return;
                        }
                        // 载入到map里面, key为转债代码, value 为df单行!, 带有这种代码列, 因此注意索引!
                        for (int i = 0; i < allBondInfoDfForRevise.length(); i++) {
                            allBondInfoForReviseMap.put(allBondInfoDfForRevise.get(i, 0).toString(),
                                    allBondInfoDfForRevise.row(i));
                        }
                    }
                }, true);
            }
        }

        public static JLabel getCommonLabel() {
            JLabel jLabel = new JLabel();
            jLabel.setForeground(Color.red);
            jLabel.setBackground(Color.black);
            return jLabel;
        }

        /**
         * 给定df一行, 给定索引列表, 创建显示内容, 使用 / 间隔, 且null显示null
         *
         * @param objects
         * @param indexes
         * @return
         */
        public static String buildStrForLabelShow(List<Object> objects, List<Integer> indexes) {
            StringBuilder stringBuilder = new StringBuilder("");
            for (Integer index : indexes) {
                stringBuilder.append(" / "); // 最后去除
                Object o = objects.get(index);
                if (o == null) {
                    stringBuilder.append("null");
                } else {
                    stringBuilder.append(o.toString());
                }
            }
            return StrUtil.sub(stringBuilder.toString(), 3, stringBuilder.length());
        }


        @Override
        public void update() {
            if (this.bondBean == null) {
                return;
            }
            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                return; // 要有全数据
            }
            String bondCode = bondBean.getSecCode();
            // 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
            List<Object> infos = allBondInfoForReviseMap.get(bondCode);
            String s = buildStrForLabelShow(infos, Arrays.asList(0, 1, 2, 3));
            bondInfoLabel.setText(s);
            bondInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(6, 7, 10, 11));
            stockInfoLabel.setText(s);
            stockInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(8, 4, 5));
            industryInfoLabel.setText(s);
            industryInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(9));
            conceptInfoLabel.setText(s);
            conceptInfoLabel.setToolTipText(s);
            // conceptInfoLabel.setText(infos.get(9).toString());
        }

        public void update(SecurityBeanEm beanEm) {
            this.bondBean = beanEm;
            this.update();
        }
    }

    SelectBeanDisplayPanel bondInfoPanel;

    /**
     * 功能区初始化
     */
    private void initFunctionPanel() {
        functionPanel = new JPanel();
        functionPanel.setPreferredSize(new Dimension(jListWidth, 300));
        functionPanel.setLayout(new BorderLayout());

        // 1.转债信息显示
        bondInfoPanel = new SelectBeanDisplayPanel();
        bondInfoPanel.setPreferredSize(new Dimension(jListWidth, 100));
        functionPanel.add(bondInfoPanel, BorderLayout.NORTH);

        // 2.功能按钮列表
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new GridLayout(2, 2, -1, -1)); // 网格布局按钮


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


        buttonContainer.add(loadBondListButton);
        functionPanel.add(buttonContainer, BorderLayout.CENTER);
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
        jList.addMouseListener(new MouseAdapter() { // 双击打开东财url
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

        jList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = jList.getSelectedIndex(); // 选中切换
                SecurityBeanEm.SecurityEmPo po = (SecurityEmPo) jList.getModel().getElementAt(index);
                setSelectedBean(po.getBean());
                bondInfoPanel.update(selectedBean);
            }
        });

        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(jListWidth, 10000));
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        return jList;
    }

    /**
     * 更新选中bean , 请调用方法, 同步设置pre的方式, 因控件事件触发, 而不合适, preSelectedBean的语义已经修改
     *
     * @param selectedBean
     */
    public void setSelectedBean(SecurityBeanEm bean) {
        this.selectedBean = bean;
        updateFsDisplay(); // 自动改变分时图显示
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
