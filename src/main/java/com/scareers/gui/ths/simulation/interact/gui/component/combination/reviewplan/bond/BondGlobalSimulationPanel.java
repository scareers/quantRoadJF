package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.*;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.interact.gui.SmartFindDialog;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.DateTimePicker;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.JXFindBarS;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.CrossLineListenerForFsXYPlot;
import com.scareers.utils.charts.EmChartFs;
import com.scareers.utils.charts.EmChartFs.DynamicEmFs1MV2ChartForRevise;
import com.scareers.utils.charts.EmChartKLine;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate.ColumnHighlightPredicate;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil.*;
import static com.scareers.utils.CommonUtil.waitForever;

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

    public static BondGlobalSimulationPanel getInstance() {
        return INSTANCE;
    }


    protected volatile List<SecurityBeanEm> bondBeanList = new ArrayList<>();
    protected volatile JXTable jXTableForBonds; //  转债展示列表控件
    protected SecurityBeanEm selectedBean = null; // 被选中的转债 东财bean对象
    protected SecurityBeanEm preChangedSelectedBean = null; // 此前被选中,且更新过fs图对象, 当新的等于它时, 将不重新实例化动态图表对象
    protected int jListWidth; // 列表宽度, 例如300
    protected MainDisplayWindow mainDisplayWindow; // 主显示区


    JPanel panelLeft; // 左panel, 显示列表和搜索等. 列表在下, 各种功能按钮组在上!
    JPanel panelMainForRevise; // 主要的复盘区域panel, 在右

    @SneakyThrows
    public static void main(String[] args) {
//        DataFrame<Object> res = getReviseTimeBondListOverviewDataDf(
//                SecurityBeanEm.createBondList(Arrays.asList("小康转债", "盘龙转债"), true),
//                "2022-06-07",
//                "10:00:00");
//        Console.log(res);

//        playClinchSuccessSound();
        playClinchFailSound();
        waitForever();
    }

    protected BondGlobalSimulationPanel(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        // 异步开始等待某些状态, 并一次或者持续刷新股票列表
        this.jListWidth = jListWidth;
        this.mainDisplayWindow = mainDisplayWindow;
        this.setLayout(new BorderLayout()); // border布局, 列表在左, 其余在右; 总宽度为展示区; 列表固定宽

        // 1.左panel 初始化和组装
        buildLeftPanel();
        if (infoRightAndFsLeft) {
            this.add(panelLeft, BorderLayout.EAST); // 左
        } else {
            this.add(panelLeft, BorderLayout.WEST); // 左
        }

        // @update: 使用折叠面板, 放k线图, 放在分时的上方! 新增临时面板
        // 2. 右panel
        initKlineDisplayPanel();
        buildMainPanel();
        buttonCollapsibleKLinePanel // 按钮绑定折叠
                .setAction(klineCollapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        EmKLineDisplayPanel.InfoDialog infoDialog = dailyKLineDisplayPanel.getInfoDialog();
        infoDialog.setVisible(true);
        buttonCollapsibleKLinePanel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 切换折叠
//                klineCollapsiblePane.setCollapsed(!klineCollapsiblePane.isCollapsed());
                EmKLineDisplayPanel.InfoDialog infoDialog = dailyKLineDisplayPanel.getInfoDialog();
                //infoDialog.setVisible(!infoDialog.isVisible());
                infoDialog.setVisible(klineCollapsiblePane.isCollapsed());
            }
        });
        buttonCollapsibleKLinePanel.setText("折叠k线");

        JPanel panelTemp = new JPanel(); // 总容器, 上为新增k线折叠面板, 下为原分时图主面板
        panelTemp.setLayout(new BorderLayout());
        panelTemp.add(klineCollapsiblePane, BorderLayout.SOUTH); // 可折叠展示k线, 放在南边 *** 下
        panelTemp.add(panelMainForRevise, BorderLayout.CENTER);
        this.add(panelTemp, BorderLayout.CENTER); // 中

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

        // 4.需要异步调用 BondUtil, 初始化 转债-- 正股/指数 两大map, 方便查询, 以调用 自定义动态图表类 的构造器!!
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (!BondUtil.flushBondToStockAndIndexMap()) { // 自动刷新, 只要填充超过200, 就视为成功
                    ThreadUtil.sleep(5000); // 间隔5s
                }
            }
        }, true);
    }

    protected JXCollapsiblePane klineCollapsiblePane; // k线图折叠面板对象
    protected JPanel klineDisplayContainerPanel; // k线图容器
    EmKLineDisplayPanel dailyKLineDisplayPanel; // 日k线

    private void initKlineDisplayPanel() {
        //@key:面板折叠, 需要隐藏掉 k线信息对话框; 在按钮的回调设置
        klineCollapsiblePane = new JXCollapsiblePane();
        klineCollapsiblePane.setAnimated(false); // 不慢动作
        klineCollapsiblePane.setLayout(new BorderLayout());

        klineDisplayContainerPanel = new JPanel(); // 方便添加其他k线, 目前虽然只有日k线
        klineDisplayContainerPanel.setLayout(new GridLayout(1, 1, -1, -1)); // 4份 k线
        // 4大k线
        dailyKLineDisplayPanel = new EmKLineDisplayPanel();
        dailyKLineDisplayPanel.setPreferredSize(new Dimension(300, EmKLineDisplayPanel.preferHeight));
        klineDisplayContainerPanel.add(dailyKLineDisplayPanel);
        klineCollapsiblePane.add("Center", klineDisplayContainerPanel);
        klineCollapsiblePane.setCollapsed(false); // 默认展开
    }

    DynamicEmFs1MV2ChartForRevise dynamicChart; // 随时更新对象
    ChartPanel chartPanel; // 更新时: 仅需要更新 内部chart对象;
    JPanel panelOfTick3sLog; // 更新时: 仅需将新 dynamicChart 的log组件, add到其center即可
    CrossLineListenerForFsXYPlot crossLineListenerForFsXYPlot; // 监听器, 更新时, 需要更新其时间列表,否则可能出现问题
    private boolean firstAddLogPanel = true; // 首次添加log到右panel

    /**
     * 更新分时图显示 主 区; 它读取自身属性, selectedBean, 以及设置区设置的 日期 ! 实例化 DynamicEmFs1MV2ChartForRevise 对象
     * 它要求 selectedBean 已设置不为 null;
     */
    public void updateFsDisplay(boolean forceCreateDynamicChart) {
        if (selectedBean == null) {
            return; // 为空或者未改变, 不会重新实例化 动态分时图表 对象
        }
        // 可强制重新创建 DynamicEmFs1MV2ChartForRevise 对象, 将读取新的selectedBean和 日期设置
        if (!forceCreateDynamicChart) {
            // 不强制时, 才使用对比机制, 可能无需新建对象; 但同样 DateStr 不会修改; 在开始和重启时, 显然需要重新读取
            if (this.selectedBean.equals(this.preChangedSelectedBean)) {
                return;
            }
        }
        // 1.实例化动态图表 -- 实例化最消耗时间
        String reviseDateStrSettingYMD = getReviseDateStrSettingYMD();
        // @key: 当前优化到 1-2 ms 级别
        SecurityBeanEm stock = BondUtil.getStockBeanByBond(selectedBean);
        SecurityBeanEm index = BondUtil.getIndexBeanByBond(selectedBean);
        if (stock == null || index == null) {
            // CommonUtil.notifyError("转债对应的 正股/指数 bean为null, 尝试访问网络直接获取");
            try {
                stock = SecurityBeanEm.createStock(BondUtil.getStockCodeOfBond(selectedBean), true);
                index = selectedBean.getSecCode().startsWith("11") ? SecurityBeanEm
                        .getShangZhengZhiShu() : SecurityBeanEm.getShenZhengChengZhi();
            } catch (Exception e) {

            }
        }
        if (stock == null || index == null) {
            CommonUtil.notifyError("转债对应的 正股/指数 bean为null, 可尝试刷新 债股映射后稍等重试; 返回");
            return;
        }
        try {
            dynamicChart = new DynamicEmFs1MV2ChartForRevise(selectedBean, reviseDateStrSettingYMD, index, stock);
        } catch (Exception e) {
            e.printStackTrace();
            CommonUtil.notifyError("实例化动态图表对象失败, 请检查构造器参数");
            return;
        }
        preChangedSelectedBean = this.selectedBean; // 更新了图表对象时, 才更新

        // 3. 更新chart对象, 刷新! // 后面常态逻辑也优化到了 0-1毫秒级别
        crossLineListenerForFsXYPlot.setTimeTicks(dynamicChart.getAllFsTimeTicks()); // 保证十字线正常
        chartPanel.setChart(dynamicChart.getChart());

        // 尝试更新一下
        Double costPriceMaybe = null;
        if (account != null && selectedBean != null) {
            costPriceMaybe = account.getBondCostPriceMap().get(selectedBean.getSecCode());
        }
        List<ReviseAccountWithOrder.BuySellPointRecord> buySellPointRecords = null;
        if (selectedBean != null) {
            buySellPointRecords = ReviseAccountWithOrder.BSPointSavingMap
                    .get(selectedBean.getSecCode());
        }
        dynamicChart.updateChartFsTrans(DateUtil.parse(getReviseSimulationCurrentTimeStr()), costPriceMaybe,
                buySellPointRecords); // 重绘图表

        chartPanel.repaint();
        chartPanel.updateUI();
//        CommonUtil.notifyError("正常");
        if (firstAddLogPanel) {
            try {
                panelOfTick3sLog.removeAll(); // 需要删除才能保证只有一个
                JScrollPane jScrollPaneForTickLog = DynamicEmFs1MV2ChartForRevise.getJScrollPaneForTickLog();
                panelOfTick3sLog.setPreferredSize(new Dimension(tick3sLogPanelWidth, panelMainForRevise.getHeight()));
                jScrollPaneForTickLog
                        .setPreferredSize(new Dimension(tick3sLogPanelWidth, panelOfTick3sLog.getHeight())); // 容器同宽
                jScrollPaneForTickLog.setLocation(0, 0);
                jScrollPaneForTickLog.setBorder(null);
                panelOfTick3sLog.add(jScrollPaneForTickLog, BorderLayout.CENTER);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            firstAddLogPanel = false; // 首次添加panel
        }
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
        functionContainerMain.setPreferredSize(new Dimension(2048, 40));
        functionContainerMain.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        functionContainerMain.setBackground(Color.black);
        addMainFunctions(); // 主功能区按钮

        // 2.分时图(或未来k线)显示区
        JPanel fsMainPanel = new JPanel(); // fs和tick容器
        fsMainPanel.setLayout(new BorderLayout());

        chartPanel = new ChartPanel(null); // 图表
        chartPanel.setPreferredSize(new Dimension(1200, 800));
        chartPanel.setMouseZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(false);
        chartPanel.setMouseZoomable(false, false);
        chartPanel.setMouseWheelEnabled(false);
        crossLineListenerForFsXYPlot =
                EmChartFs.getCrossLineListenerForFsXYPlot(CommonUtil.generateMarketOpenTimeListHm(false));
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
        chartPanel.setBackground(Color.black);
        panelOfTick3sLog.setBackground(Color.black);
        fsMainPanel.setBackground(Color.black);
        fsMainPanel.add(chartPanel, BorderLayout.CENTER);
        fsMainPanel.add(panelOfTick3sLog, BorderLayout.EAST);


        // 3.组装
        panelMainForRevise.add(fsMainPanel, BorderLayout.CENTER);
        panelMainForRevise.add(functionContainerMain, BorderLayout.NORTH);
    }

    // 主功能区相关 -- 主要实现 虚拟时间, 以便复盘, 以及开始,暂停,重置,复盘起始时间设定等功能; 达成仿真
    JTextField jTextFieldOfReviseStartDatetime; // 设置显示复盘开始日期和时间! 时间选择器
    DateTimePicker dateTimePickerOfReviseStartDatetime; // 与jTextFieldOfReviseStartDatetime结合达成时间选择功能
    DateTime reviseStartDatetime; // 默认的复盘开始时间
    // 单秒全序列: 复盘过程中, 可能出现的所有虚拟 时刻.复盘开始后, 遍历此序列, 选择第一个不小于reviseStartDatetime的开始;仅仅时分秒有效
    List<String> allFsTransTimeTicks; // 仅仅包含时分秒的标准时间
    JLabel labelOfRealTimeSimulationTime; // 仿真的 "实时时间"; 只显示时分秒, 年月日从reviseStartDatetime去看
    JTextField jTextFieldOfTimeRate; // 复盘时, 时间流速倍率, 无视了程序执行时间, 仅仅控制 sleep 的时间! 将解析text为double

    private volatile boolean reviseRunning = false; // 标志复盘是否进行中, 将其手动设置为false, 可以停止进行中的循环
    // 该值为true时, 点击重启才有效;
    private volatile boolean revisePausing = false; // 标志复盘是否暂停中; 当暂停时, 理论上running应当为 true

    // 功能按钮
    FuncButton startReviseButton; // 开始按钮
    FuncButton stopReviseButton; // 停止按钮
    FuncButton pauseRebootReviseButton; // 暂停和重启按钮, 将自行变换状态; 检测 自身text 判定应当执行的功能!
    FuncButton buttonCollapsibleKLinePanel; // 关闭k线折叠面板
    FuncButton openAccountButton; // 打开账户按钮! -f12

    volatile ReviseAccountWithOrder account; // 账户对象, 点击开始按钮首次实例化! 见开始按钮的回调
    public static final Object accountLock = new Object(); // 任意时候, 访问account, 均需要同步获取锁

    ThreadPoolExecutor poolExecutorForKLineUpdate = new ThreadPoolExecutor(4, 8, 100, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()); // 专门用于k线更新的; 异步执行

    /**
     * 主功能区组件添加, 添加到 functionContainerMain, 该panel为左浮动布局
     */
    private void addMainFunctions() {
        // 0.1. @add: 账号对象, 子线程持续刷新
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (account != null) {
                        synchronized (accountLock) { // 加锁刷新! 需要保证该方法执行快速!
                            account.flushAccountStateByCurrentTick(getReviseDateStrSettingYMD(),
                                    getReviseSimulationCurrentTimeStr());
                        }
                        holdBondHighLighterPredicate.addHoldBondCodes(account.getHoldBondsAmountMap().keySet());
                    }
                    ThreadUtil.sleep(accountStateFlushSleep); // 1.5s
                }
            }
        }, true);

        // 1. 初始化复盘功能 相关属性
        // 1.0. 默认的复盘开始时间: >16点,今天是交易日,就今天, 否则都上一天! 给爬虫一个小时!
        try {
            if (DateUtil.hour(DateUtil.date(), true) >= afterTodayNHDefaultDateAsToday) {
                if (EastMoneyDbApi.isTradeDate(DateUtil.today())) {
                    reviseStartDatetime = DateUtil.parse(DateUtil.today() + " 09:30:00");
                } else {
                    reviseStartDatetime = DateUtil
                            .parse(EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1) + " " +
                                    "09:30:00");
                }
            } else {
                reviseStartDatetime = DateUtil
                        .parse(EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1) + " " +
                                "09:30:00");
            }
        } catch (SQLException e) {
            reviseStartDatetime = DateUtil.parse(DateUtil.today() + " 09:30:00");
        }
        // 1.1. 复盘开始时间 时间选择器!
        jTextFieldOfReviseStartDatetime = new JTextField("复盘开始时间");
        jTextFieldOfReviseStartDatetime.setBorder(null);
        jTextFieldOfReviseStartDatetime.setBackground(Color.black);
        jTextFieldOfReviseStartDatetime.setForeground(Color.yellow);
        jTextFieldOfReviseStartDatetime.setCaretColor(Color.red);
        jTextFieldOfReviseStartDatetime.setPreferredSize(new Dimension(130, 40));

        dateTimePickerOfReviseStartDatetime = new DateTimePicker("yyyy-MM-dd HH:mm:ss", 160, 200) {
            @Override
            public void commit() {
                if (reviseRunning) {
                    CommonUtil.notifyError("复盘进行中,修改复盘开始日期需要先停止上次复盘!");
                    dateTimePickerOfReviseStartDatetime.tryClosePopup();
                    return;
                }
                super.commit();
            }
        };
        dateTimePickerOfReviseStartDatetime.setEnable(true).setSelect(reviseStartDatetime) // 默认值
                .changeDateEvent(new Consumer<DateTimePicker>() {
                    @Override
                    public void accept(DateTimePicker o) { // 选择后回调, 它仅仅会自动修改注册组件的文字, 以及内部date
                        // 也修改静态属性!
                        reviseStartDatetime = DateUtil.parse(dateTimePickerOfReviseStartDatetime.getSelect());
                    }
                }).register(jTextFieldOfReviseStartDatetime);

        // 1.2. 所有可能的时间. 时分秒
        allFsTransTimeTicks = CommonUtil.generateMarketOpenTimeStrListHms(false);


        // 1.3. 仿真 实时时间显示label! 不可编辑,固定 HH:mm:ss 格式
        labelOfRealTimeSimulationTime = new JLabel();
        labelOfRealTimeSimulationTime.setForeground(Color.green);
        labelOfRealTimeSimulationTime.setPreferredSize(new Dimension(60, 40));
        labelOfRealTimeSimulationTime.setBackground(Color.black);
        labelOfRealTimeSimulationTime.setText("15:00:00"); // 初始! 未开始展示全部将

        // 1.4. 时间流速倍率, 默认 1.0
        jTextFieldOfTimeRate = new JTextField(String.valueOf(timeRateDefault));
        jTextFieldOfTimeRate.setBackground(Color.black);
        jTextFieldOfTimeRate.setBorder(null);
        jTextFieldOfTimeRate.setForeground(Color.yellow);
        jTextFieldOfTimeRate.setPreferredSize(new Dimension(35, 40));
        jTextFieldOfTimeRate.setCaretColor(Color.red);


        // 2.主功能区!
        // 2.1. 时间选择器, 操作可绝对开始时间 reviseStartDatetime;
        // 2.2. 静态仿真实时时间显示label
        // 2.3. 开始复盘按钮: 开始复盘,读取reviseStartDatetime设置; 若当前正在运行, 则先停止再直接运行!
        startReviseButton = ButtonFactory.getButton("开始"); //
        startReviseButton.setForeground(Color.yellow);
        startReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) { // 同步
                if (reviseRunning) { // 正在运行中, 则点击停止按钮, 并且等待 flag, 真正停止下来
                    CommonUtil.notifyError("复盘进行中, 停止后才可开始!");
                    return;
                }

                if (revisePausing) {
                    CommonUtil.notifyError("复盘暂停中, 请点击重启!");
                    return;
                }
                // 主要目的是 将当日最新热门资讯 载入缓存!
                BondReviseUtil.notifyNewestHotNewRevise("00:00:xx", getReviseDateStrSettingYMD());

                // 此时 reviseRunning 必然为 false, 正式执行 -- 开始复盘
                // @key3: 复盘逻辑:
                // 1.更新对象! 将读取 年月日 日期设定; 且强制更新,使用新日期设置
                updateFsDisplay(true);

                // 2.读取时间流速设定, -- @update: 已经改为实时读取
                // double timeRate = getReviseTimeRateSetting();

                // 3.读取开始的 时分秒 tick 设置!
                String startTickHms = getReviseDateStrSettingHMS();

                // 4.在 allFsTransTimeTicks 所有时间tick中, 筛选>= 开始tick的子列表, 以便遍历!
                int startIndex = allFsTransTimeTicks.indexOf(startTickHms);
                if (startIndex == -1) {
                    startIndex = 0; // 当没有找到, 则从0开始!
                }

                // 5.开始循环遍历 tick, 执行更新! 期间将 检测 pause flag, 有可能被暂停!
                // @noti: 暂停机制, 本质上也是break了循环 而停止; 只是保留 labelOfRealTimeSimulationTime 值, 以便重启!
                // @noti: 停止机制, 则 会将 labelOfRealTimeSimulationTime 设置为 00:00:00, 不可重启!
                int finalStartIndex = startIndex;
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        if (dynamicChart == null) {
                            return;
                        }

                        // 死循环开始执行!
                        // 0.真正逻辑上开始, 设置 flag
                        reviseRunning = true;
                        // @update: 账户对象初始实例化!
                        synchronized (accountLock) {
                            account =
                                    ReviseAccountWithOrder
                                            .initAccountWithOrderWhenRiveStart(getReviseDateStrSettingYMD(),
                                                    getReviseDateStrSettingHMS(), "", accountInitMoney);
                            CommonUtil.notifyKey("账户已经重新初始化");
                        }
                        ReviseAccountWithOrderDao.saveOrUpdateBean(account); // 首次保存!
                        ReviseAccountWithOrder.BSPointSavingMap.clear(); // @add: 晴空此前买卖点!
                        revisePausing = false;
                        CommonUtil.notifyKey("复盘开始");

                        startReviseMainLoop(finalStartIndex);
                        reviseRunning = false; // 非运行状态
                    }
                }, true);
            }
        });

        // 2.4. 停止按钮
        stopReviseButton = ButtonFactory.getButton("停止");
        stopReviseButton.setForeground(Color.RED);
        stopReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                labelOfRealTimeSimulationTime.setText("15:00:00"); // 停止时, 不会改变最终的tick
                reviseRunning = false; // 将停止 start后 的线程中的循环
                revisePausing = false; // 暂停flag也将恢复!
                pauseRebootReviseButton.setText("暂停"); // 强制暂停按钮恢复暂停状态

                if (account != null) {
                    synchronized (accountLock) {
                        ReviseAccountWithOrder accountFinal = ReviseAccountWithOrder
                                .handleAccountWhenReviseStop(BondGlobalSimulationPanel.this.account,
                                        getReviseDateStrSettingYMD(),
                                        getReviseSimulationCurrentTimeStr());
                        account = accountFinal;
                    }
                    ReviseAccountWithOrderDao.saveOrUpdateBean(account); // 停止时保存!
                }
            }
        });

        // 2.5. 暂停按钮
        pauseRebootReviseButton = ButtonFactory.getButton("暂停"); // 默认暂停!
//给刷新按钮绑定F5快捷键
        this.pauseRebootReviseButton.registerKeyboardAction(e -> pauseRebootReviseButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
//                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pauseRebootReviseButton.setForeground(Color.pink);
        pauseRebootReviseButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                String text = pauseRebootReviseButton.getText();
                if ("暂停".equals(text)) {// 执行暂停功能
                    if (!reviseRunning) {
                        CommonUtil.notifyError("复盘尚未进行, 不可暂停!");
                        return;
                    }
                    // 主要目的是 将当日最新热门资讯 载入缓存!
                    BondReviseUtil.notifyNewestHotNewRevise("00:00:xx", getReviseDateStrSettingYMD());
                    revisePausing = true; // 暂停, 正在执行的将停止, 但保留进度
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                CommonUtil.waitUtil(new BooleanSupplier() {
                                    @Override
                                    public boolean getAsBoolean() {
                                        return reviseRunning == false; // 等待确实停止了下来
                                    }
                                }, Integer.MAX_VALUE, 1, null, false);
                            } catch (TimeoutException | InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            pauseRebootReviseButton.setText("重启"); // 变换状态!
                        }
                    }, true);
                } else if ("重启".equals(text)) { // 执行重启功能! 它与开始功能的差别在于, 开始tick从 label读取, 而非设置读取
                    if (reviseRunning) { // 正在运行中, 不可重启
                        CommonUtil.notifyError("复盘进行中, 停止后才可重启!");
                        return;
                    }
                    if (!revisePausing) {
                        CommonUtil.notifyError("复盘未暂停, 不可重启!");
                        return;
                    }

                    // 此时 reviseRunning 必然为 false, 且revisePausing 为true *************
                    // @key3: 复盘逻辑:
                    // 1.更新对象! 将读取 年月日 日期设定; 且强制更新,使用新日期设置
                    updateFsDisplay(true);
                    // 2.读取时间流速设定, -- @update: 已经改为实时读取
                    // double timeRate = getReviseTimeRateSetting();
                    // 3.读取开始的 时分秒 tick 设置!
                    String startTickHms = getReviseRestartTickFromLabel();

                    // 4.在 allFsTransTimeTicks 所有时间tick中, 筛选>= 开始tick的子列表, 以便遍历!
                    int startIndex = allFsTransTimeTicks.indexOf(startTickHms);
                    if (startIndex == -1) {
                        startIndex = 0; // 当没有找到, 则从0开始!
                    }

                    // 5.开始循环遍历 tick, 执行更新! 期间将 检测 pause flag, 有可能被暂停!
                    // @noti: 暂停机制, 本质上也是break了循环 而停止; 只是保留 labelOfRealTimeSimulationTime 值, 以便重启!
                    // @noti: 停止机制, 则 会将 labelOfRealTimeSimulationTime 设置为 00:00:00, 不可重启!
                    int finalStartIndex = startIndex;
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            if (dynamicChart == null) {
                                return;
                            }

                            // 死循环开始执行!
                            // 0.真正逻辑上开始, 设置 flag
                            reviseRunning = true;
                            revisePausing = false; // 重设暂停flag
                            pauseRebootReviseButton.setText("暂停"); // 变换状态!
                            CommonUtil.notifyKey("复盘即将重启");
                            startReviseMainLoop(finalStartIndex);
                            reviseRunning = false; // 非运行状态
                        }
                    }, true);

                }// 其他不执行, 一般不可能
            }
        });


        // 3.全部组件添加
        functionContainerMain.add(jTextFieldOfReviseStartDatetime);
        functionContainerMain.add(labelOfRealTimeSimulationTime);
        functionContainerMain.add(jTextFieldOfTimeRate);
        addTimeTickSlider(); // 1.5: 滑块滑动,可改变设置时间!
        functionContainerMain.add(startReviseButton);
        functionContainerMain.add(stopReviseButton);
        functionContainerMain.add(pauseRebootReviseButton);


        // 4.@update: 折叠面板 按钮关闭
        buttonCollapsibleKLinePanel = ButtonFactory.getButton("折叠k线");
        // buttonCollapsibleKLinePanel.setText("资讯面总结");
        buttonCollapsibleKLinePanel.setForeground(Color.red);
        functionContainerMain.add(buttonCollapsibleKLinePanel);

        // 5.模拟账户打开!
        initOpenAccountButton();
        functionContainerMain.add(openAccountButton);


    }

    /**
     * 滑块, 可以秒改变复盘具体时间!
     */
    JSlider sliderOfReviseTimeTick;

    private void addTimeTickSlider() {
        // @noti: 滑块的所有取值, 对应 分时tick的 索引范围
        sliderOfReviseTimeTick = new JSlider(0, allFsTransTimeTicks.size() - 1, 0);
        sliderOfReviseTimeTick.setBackground(Color.black);
        sliderOfReviseTimeTick.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
            @Override
            public void paintThumb(Graphics g) {
                //绘制指示物
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.red);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.fillOval(thumbRect.x, thumbRect.y, thumbRect.width,
                        thumbRect.height);//修改为圆形
                //也可以帖图(利用鼠标事件转换image即可体现不同状态)
                //g2d.drawImage(image, thumbRect.x, thumbRect.y, thumbRect.width,thumbRect.height,null);

            }

            @Override
            public void paintTrack(Graphics g) {
                //绘制刻度的轨迹
                int cy, cw;
                Rectangle trackBounds = trackRect;
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setPaint(Color.black);//将背景设为黑色
                    cy = (trackBounds.height / 2) - 2;
                    cw = trackBounds.width;

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.translate(trackBounds.x, trackBounds.y + cy);
                    g2.fillRect(0, -cy + 5, cw, cy);

                    int trackLeft = 0;
                    int trackRight = 0;
                    trackRight = trackRect.width - 1;

                    int middleOfThumb = 0;
                    int fillLeft = 0;
                    int fillRight = 0;
                    //换算坐标
                    middleOfThumb = thumbRect.x + (thumbRect.width / 2);
                    middleOfThumb -= trackRect.x;

                    if (!drawInverted()) {
                        fillLeft = !slider.isEnabled() ? trackLeft : trackLeft + 1;
                        fillRight = middleOfThumb;
                    } else {
                        fillLeft = middleOfThumb;
                        fillRight = !slider.isEnabled() ? trackRight - 1
                                : trackRight - 2;
                    }
                    //设定渐变,在这里从红色变为红色,则没有渐变,滑块划过的地方自动变成红色
                    g2.setPaint(new GradientPaint(0, 0, Color.orange, cw, 0,
                            Color.red, true));
                    g2.fillRect(0, -cy + 5, fillRight - fillLeft, cy);

                    g2.setPaint(slider.getBackground());
                    g2.fillRect(10, 10, cw, 5);

                    g2.setPaint(Color.orange);
                    g2.drawLine(0, cy, cw - 1, cy);

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2.translate(-trackBounds.x, -(trackBounds.y + cy));
                } else {
                    super.paintTrack(g);
                }
            }

        });

        sliderOfReviseTimeTick.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isReviseRunning()) {
                    CommonUtil.notifyError("复盘进行中, 不可移动滑块,设置复盘开始时间(时分秒)");
                    return;
                }
                int indexS = sliderOfReviseTimeTick.getValue(); // 索引
                String tick = allFsTransTimeTicks.get(indexS); // 设置开始tick
                labelOfRealTimeSimulationTime.setText(tick);
                String rawDateTimeStr = getReviseDateStrSettingYMDHMS();
                String newDateTimeStr = rawDateTimeStr.substring(0, 11) + tick;
                jTextFieldOfReviseStartDatetime.setText(newDateTimeStr);
                DateTime changedDate = DateUtil.parse(newDateTimeStr);
                dateTimePickerOfReviseStartDatetime.setSelect(changedDate);
                if (dynamicChart != null) {
                    dynamicChart.updateChart(changedDate); // 尝试刷新到对应的时间!
                }
            }
        });

        functionContainerMain.add(sliderOfReviseTimeTick);
    }

    AccountInfoDialog accountInfoDialog;

    private void initOpenAccountButton() {
        openAccountButton = ButtonFactory.getButton("打开账户");
        openAccountButton.setForeground(Color.orange);
        BondGlobalSimulationPanel temp = this;
        openAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (accountInfoDialog == null) {
                    accountInfoDialog = AccountInfoDialog
                            .getInstance(TraderGui.INSTANCE, "账户 -- 尚未初始化 ", AccountInfoDialog.modalS,
                                    temp);
                }
                accountInfoDialog.setVisible(true);
            }
        });
    }


    ThreadPoolExecutor poolExecutorForHotNewNotify = new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()); // 专门用于k线更新的; 异步执行

    /**
     * 复盘主循环逻辑
     *
     * @param finalStartIndex
     */
    public void startReviseMainLoop(int finalStartIndex) {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        // 实现自动调整sleep时间衰减设置(主要解决非sleep代码执行耗时的问题);
        // 因为倍率可能变化, 这里保留设置值的队列! 去最大和最小求平均; 而不保留耗时队列
        ArrayDeque<Long> sleepReduceSettingValueDeque = new ArrayDeque<>(20);
        for (int i = 0; i < 3; i++) {
            sleepReduceSettingValueDeque.add(codeExecLossSleepFixSimulation);
        }

        for (int i = finalStartIndex; i < allFsTransTimeTicks.size(); i++) {
            if (!reviseRunning) { // 被停止
                labelOfRealTimeSimulationTime.setText("15:00:00");
                reviseRunning = false; // 保证有效
                CommonUtil.notifyCommon("复盘已停止");
                break; // 被停止, 则立即停止循环!
            }

            if (revisePausing) { // 被暂停
                labelOfRealTimeSimulationTime.setText("15:00:00"); // 实时时间得以保留!
                revisePausing = true; // 保证有效
                CommonUtil.notifyCommon("复盘已暂停");
                break; // 被停止也终止循环, 等待重启!
            }
            // @update: 使用timer, 自动适应调整sleep毫秒数量, 以便符合设置的逻辑耗时!
            double reviseTimeRateSetting = getReviseTimeRateSetting(); // 当前倍率
            long shouldConsume = (long) (1000.0 / reviseTimeRateSetting); // 应当总消耗
            long tempSettingValue = getTheAvgOfDequeExcludeMaxAndMin(sleepReduceSettingValueDeque);
            long actualSleep = shouldConsume - tempSettingValue; // 本次循环将实际sleep的

            String tick = allFsTransTimeTicks.get(i);
            labelOfRealTimeSimulationTime.setText(tick); // 更新tick显示label
            // 尝试从账户获取持仓成本价!(折算过的), 画持仓线
            Double costPriceMaybe = null;
            if (account != null && selectedBean != null) {
                costPriceMaybe = account.getBondCostPriceMap().get(selectedBean.getSecCode());
            }
            List<ReviseAccountWithOrder.BuySellPointRecord> buySellPointRecords = null;
            if (selectedBean != null) {
                buySellPointRecords = ReviseAccountWithOrder.BSPointSavingMap
                        .get(selectedBean.getSecCode());
            }
            dynamicChart.updateChartFsTrans(DateUtil.parse(tick), costPriceMaybe, buySellPointRecords); // 重绘图表
            flushKlineWhenBondNotChangeAsync(); // 异步刷新当前转债k线图 -- 今日那最后一根k线

            poolExecutorForHotNewNotify.submit(new Runnable() {
                @Override
                public void run() {
                    BondReviseUtil.notifyNewestHotNewRevise(getReviseSimulationCurrentTimeStr(),
                            getReviseDateStrSettingYMD());
                }
            }); // 尝试播报当时如果有最新资讯

            ThreadUtil.sleep(actualSleep); // 实际执行sleep

            long actualConsume = timer.intervalRestart(); // 这是实际消耗的! 我的目标是 shouldConsume
            // @key: 调整理论应当sleep的值, 无论应当增大还是减少,该等式成立 !!! 之后加入队列, 进入下一次计算权重
            tempSettingValue = tempSettingValue + (actualConsume - shouldConsume);
            if (tempSettingValue < -5) { // 强制修正值为 -5 到 30
                tempSettingValue = -5;
            }
            if (tempSettingValue > 30) {
                tempSettingValue = 30;
            }
            sleepReduceSettingValueDeque.add(tempSettingValue); // 添加新值, 下次将其加入权重计算新的设置值

            // Console.log(actualConsume);
        }
    }

    public void flushKlineWhenBondNotChangeAsync() {
        poolExecutorForKLineUpdate.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    DataFrame<Object> todayFs1MDfV2 = dynamicChart.getFsDfV2Df();
                    Double open = Double.valueOf(todayFs1MDfV2.get(0, "open").toString()); // 实时读取今日开盘价
                    // 今日目前最高, 最低, 收盘, 都需要筛选分时成交df, 以便获取!
                    DataFrame<Object> fsTransDf = dynamicChart.getFsTransDf();
                    String currentTick = "09:30:00";
                    if (labelOfRealTimeSimulationTime != null) {
                        currentTick = labelOfRealTimeSimulationTime.getText();
                    }
                    DataFrame<Object> effectDf = getEffectDfByTickRange(fsTransDf, "09:29:59", currentTick);
                    Double amount = getAmountOfEffectDf(selectedBean, effectDf);
                    Double close = getCloseOfEffectDf(effectDf);
                    Double high = getHighOfEffectDf(effectDf);
                    Double low = getLowOfEffectDf(effectDf);
                    dailyKLineDisplayPanel.getDynamicKLineChart().updateTodayKline(open, high, low, close, amount);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    CommonUtil.notifyError(
                            "刷新今日单条K线失败! 当前转债: " + (selectedBean != null ? selectedBean.getName() : "null"));
                }
            }
        });
    }


    public String getReviseDateStrSettingYMD() { // 复盘日期设定 -- 年月日
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_DATE_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘日期失败, 默认返回上一交易日");
            return EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
        }
    }

    public String getReviseDateStrSettingHMS() { // 复盘开始tick设定 -- 时分秒
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_TIME_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘开始tick失败, 默认返回 09:30:00");
            return "09:30:00";
        }
    }

    public String getReviseDateStrSettingYMDHMS() { // 复盘开始tick设定 -- 年月日 时分秒
        try {
            return DateUtil.format(DateUtil.parse(jTextFieldOfReviseStartDatetime.getText()),
                    DatePattern.NORM_DATETIME_PATTERN); // 读取最新设定的 年月日 日期
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘开始tick失败, 默认返回 今日 09:30:00");
            return DateUtil.today() + " 09:30:00";
        }
    }

    public String getReviseRestartTickFromLabel() { // 重启复盘时, 应当从label读取tick, 失败则 9:30:00
        try {
            return DateUtil.format(DateUtil.parse(labelOfRealTimeSimulationTime.getText()),
                    DatePattern.NORM_TIME_PATTERN); // 读取当前被暂停时label的时间
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 复盘暂停重启tick失败, 默认返回 09:30:00");
            return "09:30:00";
        }
    }

    public DateTime getReviseSimulationCurrentTime() { // 实时获取复盘 虚拟的 当前时间!
        try {
            return DateUtil.parse(labelOfRealTimeSimulationTime.getText()); // 读取当前label的时间
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 虚拟当前时间失败,返回此刻但无逻辑意义");
            return DateUtil.date();
        }
    }

    public String getReviseSimulationCurrentTimeStr() { // 实时获取复盘 虚拟的 当前时间! 字符串形式
        return labelOfRealTimeSimulationTime.getText();
    }

    public double getReviseTimeRateSetting() { // 复盘时间流速倍率, 错误将返回 1.0
        double v = timeRateDefault;
        try {
            v = Double.parseDouble(jTextFieldOfTimeRate.getText());
        } catch (Exception e) {
            CommonUtil.notifyError("复盘程序读取 时间流速倍率失败, 默认返回 " + timeRateDefault);
        }
        if (v <= 0.1) { // 倍率不能太小; 显然也不能为0
            return timeRateDefault;
        }
        return v;
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
        panelLeft.setBackground(Color.black);

        // 1.上功能区
        initFunctionPanel();

        // 2.转债列表
        initSecurityEmJXTable(); // 已经实现自动读取并刷新 securityEmPos 属性
        flushBondListCare(); // 刷新一次列表, 该方法已经异步

        // 3.新panel包裹转债列表, 以及附带的查找框
        JPanel panelListContainer = new JPanel();
        panelListContainer.setLayout(new BorderLayout());
        jxFindBarS = new JXFindBarS(Color.red);
        if (jXTableForBonds != null) {
            jxFindBarS.setSearchable(jXTableForBonds.getSearchable());
        }
        panelListContainer.add(jxFindBarS, BorderLayout.NORTH);
        panelListContainer.add(jScrollPaneForList, BorderLayout.CENTER);

        // 4.最后组装
        panelLeft.add(functionPanel, BorderLayout.NORTH);
        panelLeft.add(panelListContainer, BorderLayout.CENTER);

    }


    SelectBeanDisplayPanel bondInfoPanel;

    /*
    8个买卖按钮
     */
    FuncButton buy1Button;
    FuncButton buy2Button;
    FuncButton buy3Button;
    FuncButton buy4Button;
    FuncButton sell1Button;
    FuncButton sell2Button;
    FuncButton sell3Button;
    FuncButton sell4Button;

    /**
     * 功能区初始化
     */
    private void initFunctionPanel() {
        functionPanel = new JPanel();
        functionPanel.setPreferredSize(new Dimension(jListWidth, 300));
        functionPanel.setLayout(new BorderLayout());
        functionPanel.setBackground(Color.black);

        // 1.转债信息显示
        bondInfoPanel = new SelectBeanDisplayPanel();
        bondInfoPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        bondInfoPanel.setPreferredSize(new Dimension(jListWidth, 100));
        functionPanel.add(bondInfoPanel, BorderLayout.NORTH);
        bondInfoPanel.setBackground(Color.black);

        // 2.功能按钮列表
        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new GridLayout(11, 4, 0, 0)); // 网格布局按钮
        buttonContainer.setBackground(Color.black);
        buttonContainer.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        // 2.0. 4个点击改变复盘开始 年月日 的按钮
        List<Integer> changeAmounts = Arrays
                .asList(1, -1, 3, -3);
        for (Integer changeAmount : changeAmounts) {
            FuncButton changeReviseStartDateButton = getChangeReviseStartDateButton(changeAmount);
            buttonContainer.add(changeReviseStartDateButton);
        }

        // 2.1. 8个点击改变复盘开始时间的按钮; 仅改变时分秒
        List<String> changeReviseStartTimeButtonTexts = Arrays
                .asList("9:30:00", "9:45:00",
                        "10:00:00", "10:15:00",
                        "10:30:00", "10:45:00",
                        "11:00:00", "11:15:00",
                        "13:00:00", "13:15:00",
                        "13:30:00", "13:45:00",
                        "14:00:00", "14:15:00",
                        "14:30:00", "14:45:00");
        for (String text : changeReviseStartTimeButtonTexts) {
            FuncButton changeReviseStartTimeButton = getChangeReviseStartTimeButton(text);
            buttonContainer.add(changeReviseStartTimeButton);
        }
        // 2.1.2. 分钟调节4个按钮
        buttonContainer.add(getChangeReviseStartTimeMoveMinuteButton("前1分", -1));
        buttonContainer.add(getChangeReviseStartTimeMoveMinuteButton("前5分", -5));
        buttonContainer.add(getChangeReviseStartTimeMoveMinuteButton("后1分", 1));
        buttonContainer.add(getChangeReviseStartTimeMoveMinuteButton("后5分", 5));
        // 2.1.3. 秒级别调节
        buttonContainer.add(getChangeReviseStartTimeMoveSecondButton("前10秒", -10));
        buttonContainer.add(getChangeReviseStartTimeMoveSecondButton("前30秒", -30));
        buttonContainer.add(getChangeReviseStartTimeMoveSecondButton("后10秒", 10));
        buttonContainer.add(getChangeReviseStartTimeMoveSecondButton("后30秒", 30));

        // 2.2. 4个点击改变复盘时间倍率的按钮
        List<String> changeReviseTimeRateButtonTexts = Arrays
                .asList("1", "3", "5", "10");
        for (String text : changeReviseTimeRateButtonTexts) {
            FuncButton button = getChangeReviseTimeRateButton(text);
            buttonContainer.add(button);
        }

        // 2.2.0. 假装买卖模拟操作的弹窗, 会自动消失
        dummyDialogWhenBuySell = new JDialog(TraderGui.INSTANCE, "交易执行中", false);
        dummyDialogWhenBuySell.setSize(230, 270);
        dummyBuySellInfoLabel = new JLabel("正在买入...");

        dummyBuySellInfoLabel.setFont(new Font("宋体", Font.BOLD, 22));
        JPanel panelTemp0 = new JPanel();
        panelTemp0.setLayout(new BorderLayout());
        panelTemp0.add(dummyBuySellInfoLabel, BorderLayout.CENTER);
        dummyDialogWhenBuySell.setContentPane(panelTemp0);
        dummyDialogWhenBuySell.setLocationRelativeTo(TraderGui.INSTANCE);

        // 2.2.1. @add: 4个买入按钮和4个卖出按钮!
        buy1Button = getBuyButton(1);

        buy2Button = getBuyButton(2);
        buy3Button = getBuyButton(3);
        buy4Button = getBuyButton(4);
        sell1Button = getSellButton(1);
        sell2Button = getSellButton(2);
        sell3Button = getSellButton(3);
        sell4Button = getSellButton(4);
        buttonContainer.add(sell1Button);
        buttonContainer.add(sell2Button);
        buttonContainer.add(sell3Button);
        buttonContainer.add(sell4Button);
        buttonContainer.add(buy1Button);
        buttonContainer.add(buy2Button);
        buttonContainer.add(buy3Button);
        buttonContainer.add(buy4Button);


        // 2.3.@key: 各种功能按钮!
        // 2.3.1: 主动刷新转债列表 (已经有线程自动刷新)
        FuncButton loadBondListButton = ButtonFactory.getButton("刷新列表");
        loadBondListButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            flushBondListCare(); // 已经实现了新建线程执行
        });


        // ---> 播报开启按钮 以及 停止按钮;
        // 2.3.2. 播报开启按钮
        FuncButton broadcastProcessStartButton = ButtonFactory.getButton("开启播报");
        broadcastProcessStartButton.setForeground(Color.orange);
        broadcastProcessStartButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondBuyNotify.changeEnvironmentToRevise(); // 模拟环境!
                    BondBuyNotify.main1(); // 播报程序启动; 主死循环会检测停止 flag; 重复点击将打印错误log
                }
            }, true);
        });

        // 2.3.3. 播报停止按钮 , 设置flag, 将会软停止播报主循环
        FuncButton broadcastProcessStopButton = ButtonFactory.getButton("停止播报");
        broadcastProcessStopButton.setForeground(Color.red);
        broadcastProcessStopButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondBuyNotify.stopBroadcast();
                }
            }, true);
        });

        //  2.3.3. 可手动刷新 转债 -- 正股/指数 map; 方便查询
        FuncButton flushBondToStockAndIndexMapButton = ButtonFactory.getButton("刷新债股字典");
        flushBondToStockAndIndexMapButton.addActionListener(e -> { // 点击加载或刷新转债列表;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    BondUtil.flushBondToStockAndIndexMap();
                }
            }, true);
        });

        // 按钮添加
        buttonContainer.add(broadcastProcessStartButton);
        buttonContainer.add(broadcastProcessStopButton);
        buttonContainer.add(loadBondListButton);
        buttonContainer.add(flushBondToStockAndIndexMapButton);


        functionPanel.add(buttonContainer, BorderLayout.CENTER);
    }

    /**
     * 刷新转债列表方法; 列表组件实例化时将调用一次; 也是刷新列表按钮回调函数
     *
     * @throws Exception
     */
    public void flushBondListCare() {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> allBondCodes = null;
                    if (bondListUseRealTimeWenCai) {
                        DataFrame<Object> dataFrame = BondUtil.getVolTopNBondDf(200); // 限制200, 否则太多
                        try {
                            allBondCodes = DataFrameS.getColAsStringList(dataFrame, "code");
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    if (allBondCodes == null || allBondCodes.size() < 100) { // 失败或者设置就使用数据库
                        String dateStr = DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATE_PATTERN);
                        allBondCodes = EastMoneyDbApi.getAllBondCodeByDateStr(
                                dateStr);

                        if (allBondCodes == null || allBondCodes.size() < 50) {
                            // 运行爬虫也是今日的了
                            log.warn("数据库获取转债代码列表失败: {} [建议运行爬虫 BondListEm] ; 将访问最新实时转债列表", dateStr);
                            try {
                                DataFrame<Object> bondDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("可转债"));
                                allBondCodes = DataFrameS.getColAsStringList(bondDf, "资产代码");
                            } catch (Exception e) {
                                CommonUtil.notifyError("访问最新实时转债代码列表依然失败, 更新转债列表失败");
                            }
                        }
                    }
                    if (allBondCodes == null || allBondCodes.size() < 100) {
                        CommonUtil.notifyError("转债代码列表获取失败, 更新转债列表失败");
                        return;
                    }
                    allBondCodes = allBondCodes.stream().filter(Objects::nonNull)
                            .collect(Collectors.toList()); // 不可null
                    List<SecurityBeanEm> bondList = SecurityBeanEm.createBondListOrdered(allBondCodes, false);
                    // bondList 无序, 将其按照 原来的allBondCodes 排序
                    flushBondListAs(bondList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, true);
    }

    /**
     * 手动给定转债列表来刷新; 并且将查找信息 加入只能查找map中
     *
     * @param bondList
     */
    public void flushBondListAs(List<SecurityBeanEm> bondList) {
        for (SecurityBeanEm beanEm : bondList) { // 智能查找map加入数据
            SmartFindDialog.findingMap.put(beanEm.getQuoteId(),
                    new SecurityBeanEm.SecurityEmPoForSmartFind(beanEm));
        }
        if (bondList != null) {
            this.bondBeanList = bondList; // 更新列表
        }
        if (loadAllFsDataFromDbWhenFlushBondList) { // 设置项: 是否载入数据到缓存; 异步执行
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EastMoneyDbApi.loadFs1MAndFsTransAndKLineDataToCache(bondList, getReviseDateStrSettingYMD());
                }
            }, true);
        }
    }

    /**
     * 一类按钮: 点击改变复盘开始日期;  参数表示以当前设置为基点, 往前往后多少个交易日!
     *
     * @param tickHms
     * @return
     */
    public FuncButton getChangeReviseStartDateButton(int changeDate) {
        String text = changeDate < 0 ? "后" : "前";
        text = text + Math.abs(changeDate);

        FuncButton changeReviseStartTimeButton = ButtonFactory.getButton(text);
        changeReviseStartTimeButton.setForeground(Color.RED);
        changeReviseStartTimeButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (reviseRunning) {
                    CommonUtil.notifyError("复盘进行中,修改复盘开始日期需要先停止上次复盘!");
                    return;
                }


                String preSetDate = getReviseDateStrSettingYMD();
                String newSet = EastMoneyDbApi.getPreNTradeDateStrict(preSetDate, changeDate);


                // 重设复盘开始时间!
                reviseStartDatetime = DateUtil.parse(newSet + " " + DateUtil.format(reviseStartDatetime,
                        DatePattern.NORM_TIME_PATTERN)); // 变量更新
                jTextFieldOfReviseStartDatetime // 文本框更新
                        .setText(DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATETIME_PATTERN));
                dateTimePickerOfReviseStartDatetime.setSelect(reviseStartDatetime);// 改变选中
            }
        });
        return changeReviseStartTimeButton;
    }

    JDialog dummyDialogWhenBuySell; // 买卖时的 模拟弹窗, 展示1s左右; 假装 同花顺买入卖出弹窗
    JLabel dummyBuySellInfoLabel;

    /**
     * 买入按钮.
     *
     * @param denominator 分母, 即 x, 代表 按钮使用的仓位是  1/x
     * @return
     */
    public FuncButton getBuyButton(int denominator) {
        String text = "买1/" + denominator;
        FuncButton buyButton = ButtonFactory.getButton(text);
        buyButton.setForeground(Color.white);
        buyButton.setBackground(new Color(253, 68, 1)); // 类似核按钮,红背景,白色字
        buyButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (!reviseRunning) {
                    CommonUtil.notifyError("复盘尚未运行,不可买卖");
                    return;
                }
                Double fsTransNewestPrice = dynamicChart.getFsTransNewestPrice();
                if (fsTransNewestPrice != null) { // 最新价格
                    if (account != null) {
                        ReviseAccountWithOrder account0 = BondGlobalSimulationPanel.this.account.submitNewOrder(
                                getReviseSimulationCurrentTimeStr(), "buy", selectedBean,
                                fsTransNewestPrice + buySellPriceBias, denominator, false);
                        account0.flushAccountStateByCurrentTick(getReviseDateStrSettingYMD(),
                                getReviseSimulationCurrentTimeStr());
                        ReviseAccountWithOrderDao.saveOrUpdateBean(account0);
                        ReviseAccountWithOrder account1 = account0.clinchOrderDetermine();
                        String notClinchReason = account1.getNotClinchReason();
                        account1.flushAccountStateByCurrentTick(getReviseDateStrSettingYMD(),
                                getReviseSimulationCurrentTimeStr());
                        ReviseAccountWithOrderDao.saveOrUpdateBean(account1);
                        synchronized (accountLock) {
                            account = account1; // 设置.
                        }
                        // 都进行好了才假装出现买卖窗口
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                notifyOrderClinchResult(notClinchReason, Color.red, "买入进行中...");
                            }
                        });
                    }

                }
            }
        });
        return buyButton;
    }

    /**
     * sleep赋予了一定随机性, 模拟现实成交的时间随机性
     *
     * @param notClinchReason
     * @param red
     * @param s
     */
    public void notifyOrderClinchResult(String notClinchReason, Color red, String s) {
        dummyBuySellInfoLabel.setForeground(red);
        dummyBuySellInfoLabel.setText(s);
        dummyDialogWhenBuySell.setVisible(true);
//        ThreadUtil.sleep(RandomUtil.randomInt((int) (dummyBuySellOperationSleep * 0.6),
//                (int) (dummyBuySellOperationSleep * 1.2)));
        ThreadUtil.sleep(dummyBuySellOperationSleep);
        dummyDialogWhenBuySell.setVisible(false);
//        ThreadUtil.sleep(RandomUtil.randomInt((int) (dummyClinchOccurSleep * 0.5),
//                (int) (c * 1.4)));
        ThreadUtil.sleep(dummyClinchOccurSleep);
        if (notClinchReason == null) {
            playClinchSuccessSound();
        } else {
            playClinchFailSound();
        }
        CommonUtil.notifyInfo(account.getOrderFinalClinchDescription());
    }

    /**
     * 卖出按钮.
     *
     * @param denominator 分母, 即 x, 代表 按钮使用的仓位是  1/x
     * @return
     */
    public FuncButton getSellButton(int denominator) {
        String text = "卖1/" + denominator;
        FuncButton sellButton = ButtonFactory.getButton(text);
        sellButton.setForeground(Color.red);
        sellButton.setBackground(new Color(204, 172, 0)); // 类似核按钮,黄背景红字
        sellButton.addActionListener(new ActionListener() {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (!reviseRunning) {
                    CommonUtil.notifyError("复盘尚未运行,不可买卖");
                    return;
                }
                Double fsTransNewestPrice = dynamicChart.getFsTransNewestPrice();
                if (fsTransNewestPrice != null) { // 最新价格
                    if (account != null) {
                        ReviseAccountWithOrder account0 = BondGlobalSimulationPanel.this.account.submitNewOrder(
                                getReviseSimulationCurrentTimeStr(), "sell", selectedBean,
                                fsTransNewestPrice - buySellPriceBias, denominator, false);
                        account0.flushAccountStateByCurrentTick(getReviseDateStrSettingYMD(),
                                getReviseSimulationCurrentTimeStr());
                        ReviseAccountWithOrderDao.saveOrUpdateBean(account0);
                        ReviseAccountWithOrder account1 = account0.clinchOrderDetermine();
                        account1.flushAccountStateByCurrentTick(getReviseDateStrSettingYMD(),
                                getReviseSimulationCurrentTimeStr());
                        String notClinchReason = account1.getNotClinchReason();
                        ReviseAccountWithOrderDao.saveOrUpdateBean(account1);
                        synchronized (accountLock) {
                            account = account1; // 设置.
                        }
                        // 都进行好了才假装出现买卖窗口
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                notifyOrderClinchResult(notClinchReason, Color.green, "卖出进行中...");
                            }
                        });
                    }

                }
            }
        });
        return sellButton;
    }


    /**
     * 一类按钮: 点击改变复盘开始时间(仅仅时分秒) 到按钮文字那么多
     *
     * @param tickHms
     * @return
     */
    public FuncButton getChangeReviseStartTimeButton(String tickHms) {
        FuncButton changeReviseStartTimeButton = ButtonFactory.getButton(tickHms);
        changeReviseStartTimeButton.setForeground(Color.orange);
        changeReviseStartTimeButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (reviseRunning) {
                    CommonUtil.notifyError("复盘进行中,修改复盘开始日期需要先停止上次复盘!");
                    return;
                }
                // 重设复盘开始时间!
                reviseStartDatetime = DateUtil.parse(DateUtil.format(reviseStartDatetime,
                        DatePattern.NORM_DATE_PATTERN + " " + tickHms)); // 变量更新
                jTextFieldOfReviseStartDatetime // 文本框更新
                        .setText(DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATETIME_PATTERN));
                dateTimePickerOfReviseStartDatetime.setSelect(reviseStartDatetime);// 改变选中

                labelOfRealTimeSimulationTime.setText(tickHms);
                int i = allFsTransTimeTicks.indexOf(tickHms);
                if (i >= 0) {
                    sliderOfReviseTimeTick.setValue(i);
                }
                if (dynamicChart != null) {
                    dynamicChart.updateChart(reviseStartDatetime);
                }
            }
        });
        return changeReviseStartTimeButton;
    }

    /**
     * 一类按钮: 点击改变复盘开始时间(仅仅时分秒) ; 改变分钟!
     * 将调整时间! 前1分, 前5分, 后1分, 后5分
     * 参数如上4个
     *
     * @param text
     * @return
     */
    public FuncButton getChangeReviseStartTimeMoveMinuteButton(String text, int minuteChange) {
        FuncButton changeReviseStartTimeButton = ButtonFactory.getButton(text);
        changeReviseStartTimeButton.setForeground(Color.green);
        changeReviseStartTimeButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (reviseRunning) {
                    CommonUtil.notifyError("复盘进行中,修改复盘开始日期需要先停止上次复盘!");
                    return;
                }

                // 当前设定!
                DateTime parse = DateUtil.parse(getReviseDateStrSettingYMDHMS());
                DateTime newDateTime = DateUtil.offset(parse, DateField.MINUTE, minuteChange); // 新时间!
                String format = DateUtil.format(newDateTime, DatePattern.NORM_TIME_PATTERN);
                if (format.compareTo("09:30:00") < 0) {
                    format = "09:30:00";
                } else if (format.compareTo("15:00:00") > 0) {
                    format = "15:00:00";
                }
                newDateTime = DateUtil.parse(getReviseDateStrSettingYMD() + " " + format);

                // 重设复盘开始时间!
                reviseStartDatetime = newDateTime; // 变量更新
                jTextFieldOfReviseStartDatetime // 文本框更新
                        .setText(DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATETIME_PATTERN));
                dateTimePickerOfReviseStartDatetime.setSelect(reviseStartDatetime);// 改变选中

                labelOfRealTimeSimulationTime.setText(format);
                int i = allFsTransTimeTicks.indexOf(format);
                if (i >= 0) {
                    sliderOfReviseTimeTick.setValue(i);
                }
                if (dynamicChart != null) {
                    dynamicChart.updateChart(reviseStartDatetime);
                }
            }
        });
        return changeReviseStartTimeButton;
    }

    /**
     * 秒级改变复盘开始时间
     * @param text
     * @param secondChange
     * @return
     */
    public FuncButton getChangeReviseStartTimeMoveSecondButton(String text, int secondChange) {
        FuncButton changeReviseStartTimeButton = ButtonFactory.getButton(text);
        changeReviseStartTimeButton.setForeground(Color.green);
        changeReviseStartTimeButton.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                if (reviseRunning) {
                    CommonUtil.notifyError("复盘进行中,修改复盘开始日期需要先停止上次复盘!");
                    return;
                }

                // 当前设定!
                DateTime parse = DateUtil.parse(getReviseDateStrSettingYMDHMS());
                DateTime newDateTime = DateUtil.offset(parse, DateField.SECOND, secondChange); // 新时间!
                String format = DateUtil.format(newDateTime, DatePattern.NORM_TIME_PATTERN);
                if (format.compareTo("09:30:00") < 0) {
                    format = "09:30:00";
                } else if (format.compareTo("15:00:00") > 0) {
                    format = "15:00:00";
                }
                newDateTime = DateUtil.parse(getReviseDateStrSettingYMD() + " " + format);

                // 重设复盘开始时间!
                reviseStartDatetime = newDateTime; // 变量更新
                jTextFieldOfReviseStartDatetime // 文本框更新
                        .setText(DateUtil.format(reviseStartDatetime, DatePattern.NORM_DATETIME_PATTERN));
                dateTimePickerOfReviseStartDatetime.setSelect(reviseStartDatetime);// 改变选中

                labelOfRealTimeSimulationTime.setText(format);
                int i = allFsTransTimeTicks.indexOf(format);
                if (i >= 0) {
                    sliderOfReviseTimeTick.setValue(i);
                }
                if (dynamicChart != null) {
                    dynamicChart.updateChart(reviseStartDatetime);
                }
            }
        });
        return changeReviseStartTimeButton;
    }

    /**
     * 一类按钮: 点击改变 复盘时间倍率 到按钮文字那么多 -- 要求参数可解析为double
     *
     * @param tickHms
     * @return
     */
    public FuncButton getChangeReviseTimeRateButton(String timeRate) {
        FuncButton button = ButtonFactory.getButton(timeRate);
        button.setForeground(Color.yellow);
        button.addActionListener(new ActionListener() {
            @SneakyThrows
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                jTextFieldOfTimeRate.setText(timeRate);
            }
        });
        return button;
    }

    JScrollPane jScrollPaneForList;

    private void initJTableWrappedJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jXTableForBonds); // 滚动包裹转债列表
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }


    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private void initSecurityEmJXTable() {
        // 1.构造model
        Vector<Vector<Object>> datas = new Vector<>(); // 空数据, 仅提供列信息
        Vector<Object> cols = new Vector<>(bondTableColNames);
        DefaultTableModel model = new DefaultTableModel(datas, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 不可编辑!
            }

            @Override
            public Class getColumnClass(int column) { // 返回列值得类型, 使得能够按照数据排序, 否则默认按照字符串排序
                if (column == 0 || column == 1) { // 名称代码
                    return String.class;
                } else if (column == 2 || column == 3) { // 涨跌幅成交额
                    return Double.class;
                } else {
                    return Object.class;
                }
            }
        };

        // 2.实例化table, 设置model
        jXTableForBonds = new JXTable();
        jXTableForBonds.setSortOrder("涨跌幅", SortOrder.DESCENDING);
        jXTableForBonds.setModel(model);
        removeEnterKeyDefaultAction(jXTableForBonds); // 按下enter不下移, 默认行为

        addTableHighlighters(); // 高亮

        jXTableForBonds.setSortOrder(2, SortOrder.DESCENDING); // 默认涨跌幅降序
        jXTableForBonds.setAutoCreateRowSorter(true);

        // 3.切换选择的回调绑定
        jXTableForBonds.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            private volatile int preIndex = -2;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jXTableForBonds.getSelectedRow();
                if (row == -1) {
                    return;
                }

                try {
                    row = jXTableForBonds.convertRowIndexToModel(row);
                } catch (Exception exx) {
                    return;
                }

                if (row == preIndex) {
                    return;
                }

                preIndex = row;

                String bondCode = null;
                try {
                    bondCode = model.getValueAt(row, 0).toString();
                } catch (Exception ex) {
                    return; // 行数底层实现有bug
                }
                try {
                    SecurityBeanEm bond = SecurityBeanEm.createBond(bondCode);
                    setSelectedBean(bond);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    CommonUtil.notifyError("表格切换选中行,回调函数执行失败");
                }
            }
        });
        fitTableColumns(jXTableForBonds);
        if (jxFindBarS != null) { // 双边都有判定设置
            jxFindBarS.setSearchable(this.jXTableForBonds.getSearchable());
        }
        initJTableStyle();
        setTableColCellRenders(); // 设置显示render
        jXTableForBonds.setGridColor(Color.black); // 不显示网格
        // 持续刷新列表, 100 ms一次. securityEmPos 应该为持续变化
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 不断更新表格内容!
                    String currentTick = "09:30:00";
                    if (labelOfRealTimeSimulationTime != null) {
                        currentTick = labelOfRealTimeSimulationTime.getText();
                    }
                    DataFrame<Object> newDataDf = getReviseTimeBondListOverviewDataDf(
                            bondBeanList, getReviseDateStrSettingYMD(), currentTick
                    );
                    fullFlushBondListTable(newDataDf);
                    ThreadUtil.sleep(1000); // 每秒刷新!
                }
            }

        }, true);
    }

    /**
     * 设置表格各列的 cellRender
     */
    private void setTableColCellRenders() {
        jXTableForBonds.getColumn(0).setCellRenderer(new TableCellRendererForBondTable());
        jXTableForBonds.getColumn(1).setCellRenderer(new TableCellRendererForBondTable());
        jXTableForBonds.getColumn(2).setCellRenderer(new TableCellRendererForBondTableForPercent());
        jXTableForBonds.getColumn(3).setCellRenderer(new TableCellRendererForBondTableForBigNumber());
    }


    public static Color commonForeColor = Color.white; // 普通的字颜色, 转债代码和名称 使用白色
    public static Color amountForeColor = new Color(2, 226, 224); // 文字颜色 : 成交额
    public static Color upForeColor = new Color(255, 50, 50); // 向上的红色 : 涨跌幅
    public static Color downForeColor = new Color(0, 230, 0); // 向下的绿色: 涨跌幅
    public static Color selectedBackColor = new Color(64, 0, 128); // 选中时背景色
    public static Color holdBondForeColor = new Color(206, 14, 95); // 持仓的文字颜色: 代码和名称!

    HoldBondHighLighterPredicate holdBondHighLighterPredicate;
    ColorHighlighter holdBondHighlighter;

    /**
     * 默认设置是: 全背景黑色, 全字体 白色, 选中背景 深蓝!
     * 高亮对象均在此基础上更改
     * 转债列表高亮设置
     */
    private void addTableHighlighters() {
        // 1.成交额列蓝色
        ColumnHighlightPredicate columnHighlightPredicate = new ColumnHighlightPredicate(3);
        ColorHighlighter amountColForeHighlighter = new ColorHighlighter(columnHighlightPredicate, null,
                amountForeColor, null, amountForeColor);

        // 2.涨跌幅>0 则文字 偏红色 , <0, 则偏绿色
        ChgPctGt0HighLighterPredicate chgPctGt0HighLighterPredicate = new ChgPctGt0HighLighterPredicate();
        ColorHighlighter chgPctGt0Highlighter = new ColorHighlighter(chgPctGt0HighLighterPredicate, null,
                upForeColor, null, upForeColor);
        ChgPctLt0HighLighterPredicate chgPctLt0HighLighterPredicate = new ChgPctLt0HighLighterPredicate();
        ColorHighlighter chgPctLt0Highlighter = new ColorHighlighter(chgPctLt0HighLighterPredicate, null,
                downForeColor, null, downForeColor);

        // 3.持仓转债 代码和名称 变深红 , 两者均为属性, 为了实时修改!
        holdBondHighLighterPredicate = new HoldBondHighLighterPredicate(null);
        holdBondHighlighter = new ColorHighlighter(holdBondHighLighterPredicate, null,
                holdBondForeColor, null, holdBondForeColor);

        jXTableForBonds.setHighlighters(
                amountColForeHighlighter,
                chgPctGt0Highlighter,
                chgPctLt0Highlighter,
                holdBondHighlighter
        );

    }

    /**
     * 设置转债列表表格样式
     */
    private void initJTableStyle() {
        // 1.表头框颜色和背景色
        jXTableForBonds.getTableHeader().setBackground(Color.BLACK);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setBackground(COLOR_LIST_BK_EM);
        cellRenderer.setForeground(Color.white);
        TableColumnModel columnModel = jXTableForBonds.getTableHeader().getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            //i是表头的列
            TableColumn column = jXTableForBonds.getTableHeader().getColumnModel().getColumn(i);
            column.setHeaderRenderer(cellRenderer);
            //表头文字居中
            cellRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
        }

        // 2.表格自身文字颜色和背景色
        jXTableForBonds.setBackground(Color.black);
        jXTableForBonds.setForeground(Color.white);
        // 4. 单行高 和字体
        jXTableForBonds.setRowHeight(30);
        jXTableForBonds.setFont(new Font("微软雅黑", Font.PLAIN, 18));

        // 5.单选,大小,无边框, 加放入滚动
        jXTableForBonds.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jXTableForBonds.setPreferredSize(new Dimension(jListWidth, 10000));
        jXTableForBonds.setBorder(null);
        initJTableWrappedJScrollPane(); // 表格被包裹
    }


    /**
     * 全量刷新逻辑
     *
     * @param fullDf
     * @param model
     */
    protected void fullFlushBondListTable(DataFrame<Object> newestDf) {
        int sortedColumnIndex = jXTableForBonds.getSortedColumnIndex(); // 唯一主要的排序列!
        SortOrder sortOrder = null;
        if (sortedColumnIndex != -1) {
            sortOrder = jXTableForBonds.getSortOrder(sortedColumnIndex); // 什么顺序
        }

        DefaultTableModel model = (DefaultTableModel) jXTableForBonds.getModel();
        // 老数据
        Vector<Vector> oldDatas = model.getDataVector();

        // 新数据
        Vector<Vector> newDatas = new Vector<>();
        for (int i = 0; i < newestDf.length(); i++) {
            newDatas.add(new Vector<>(newestDf.row(i)));
        }
        // 行改变 :将自动增减行, 若新数据行少, 则多的行不会显示, 但本身存在
        model.setRowCount(newDatas.size());

//        model.setDataVector(newDatas, new Vector<>(bondTableColNames));

        for (int i = 0; i < newDatas.size(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                model.setValueAt(newDatas.get(i).get(j), i, j);
            }
        }

        // 排序恢复!
        if (sortedColumnIndex != -1) {
            jXTableForBonds.setSortOrder(sortedColumnIndex, sortOrder);
        }
    }

    /**
     * 更新选中bean , 请调用方法, 同步设置pre的方式, 因控件事件触发, 而不合适, preSelectedBean的语义已经修改
     *
     * @param selectedBean
     */
    public void setSelectedBean(SecurityBeanEm bean) {
        this.selectedBean = bean;
        // @speed: 已经优化到 2-4 ms 级别
        updateFsDisplay(false); // 自动改变分时图显示, 不强制 首次18ms, 后面3ms
        updateKlineDisplayAsync(); // 异步更新k线显示 --> 即更换转债
        bondInfoPanel.update(selectedBean); // 信息也要更改
    }

    public void updateKlineDisplayAsync() {
        poolExecutorForKLineUpdate.submit(new Runnable() { // 更新k线图全体
            @Override
            public void run() {
                EmChartKLine.DynamicEmKLineChartForRevise dynamicKlineChart =
                        new EmChartKLine.DynamicEmKLineChartForRevise(selectedBean, getReviseDateStrSettingYMD(),
                                kLineAmountHope);
                dailyKLineDisplayPanel.update(dynamicKlineChart);
            }
        });
    }


    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    private static final Log log = LogUtil.getLogger();
}
