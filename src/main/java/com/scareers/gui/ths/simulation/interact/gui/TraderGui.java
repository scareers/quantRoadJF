package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew.*;
import com.scareers.datasource.selfdb.HibernateSessionFactory;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondReviseUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.*;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncFrameS;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.interact.gui.notify.EmPcNewsNotify;
import com.scareers.gui.ths.simulation.interact.gui.notify.NewConceptDiscover;
import com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil;
import com.scareers.gui.ths.simulation.trader.ConvertibleBondArbitrage;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static com.scareers.gui.ths.simulation.interact.gui.SmartFindDialog.*;
import static com.scareers.utils.CommonUtil.waitForever;


/**
 * description: 模拟交易后台gui, 主要用于查看模拟交易进行各种状况.
 *
 * @key1 gui风格模仿idea;
 * @key2 主界面子组件:
 * 1.菜单栏 --> 常规实现
 * 2.工具栏 --> 常规实现
 * 3.路径栏(状态栏1) --> 多级label
 * 4.状态栏, 右侧含按钮 --> 常规实现
 * 5.核心组件: CorePanel --> 包含 左/右/下 功能按钮区. 以及 主功能区(idea项目文件树) + 主显示区(idea editor)
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 * @see CorePanel
 */
@Setter
public class TraderGui extends JFrame {
    public static TraderGui INSTANCE;

    private static final Log log = LogUtil.getLogger();
    public static int screenW; // 除去任务栏, 可用的全屏宽度/高度, 暂时未使用
    public static int screenH;

    static {
        initScreenBounds();
        try {
            initGlobalStyle();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        main0(args);
    }

    public static void main0(String[] agrs) throws Exception {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                HibernateSessionFactory.getSessionFactoryOfEastMoney();
            }
        }, true);
        TraderGui gui = new TraderGui();
        INSTANCE = gui;
        gui.setVisible(true);
        gui.showSystemTray();
        waitForever();
    }


    JLabel pathLabel; // 路径栏, 待完善
    JLabel statusBar; // 状态栏, 待完善
    CorePanel corePanel; // 核心组件
    JDesktopPane mainPane;

    private ImageIcon imageIcon; // 图标
    private TrayIcon trayIcon; // 系统托盘

    public TraderGui() throws Exception {
        super();
        init(); // 组装子控件
        initTrayIcon();
    }

    /**
     * 语义上, 表示gui当前处于 什么功能的 界面gui之下;;
     * 标志了gui状态
     * 当前仅仅用于 只能搜索时, 对同类型查找结果, 在 gui处于不同界面时, 可能应该有不同的 执行逻辑
     */
    public static enum FunctionGuiCurrent {
        BOND_REVISE // 转债复盘界面
    }

    public FunctionGuiCurrent functionGuiCurrent = null; // getter不好意思, 直接public了

    public void init() {
        this.setLayout(new BorderLayout());
        this.setUndecorated(false); // 标题栏显示,true 则类似专注模式
        imageIcon = new ImageIcon(ResourceUtil.getResource(ICON_TITLE_PATH));
        this.setIconImage(imageIcon.getImage()); // 图标

        if (MAXIMIZE_DEFAULT) {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH); // 直接最大化
        } else {
            centerSelf();
        }

        // 退出回调, 建议 HIDE_ON_CLOSE / EXIT_ON_CLOSE/ DO_NOTHING_ON_CLOSE(此可捕获事件),
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addListeners(); // 添加监听器


        statusBar = new JLabel("Running");
        statusBar.setFont(new Font("宋体", Font.BOLD, 15));
        statusBar.setForeground(Color.RED);
        statusBar.setPreferredSize(new Dimension(100, 20));

        corePanel = buildCorePanel();
        this.mainPane = corePanel.getMainPane();
//
        this.add(corePanel, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);

        // initPathLabel(); // paths 显示栏,
        initMenuBar(); // 菜单栏

        this.pack();
    }

    public void initPathLabel() {
        pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("宋体", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);
        pathLabel.setPreferredSize(new Dimension(100, 20));
        pathLabel.setBorder(BorderFactory.createLineBorder(Color.black, 1, false));
        this.add(pathLabel, BorderLayout.NORTH);
    }


    /*
    菜单栏相关
     */
    JMenuBar menuBar;
    JMenu startMenu;

    public void initMenuBar() {
        // 菜单栏
        menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_THEME_MINOR);
        menuBar.setBorder(BorderFactory.createEmptyBorder());
        // 菜单
        startMenu = new JMenu("开始");
        startMenu.setForeground(COLOR_GRAY_COMMON);
        // 菜单项
        JMenuItem startTraderItem = new JMenuItem("启动Trader");
        startMenu.add(startTraderItem);
        startTraderItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Trader.getAndStartInstance();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
        );

        // 分隔符
        startMenu.addSeparator();
        JMenuItem bondTtsItem = new JMenuItem("转债套利语音提示");
        bondTtsItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                ConvertibleBondArbitrage.main0();
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem);

        JMenuItem bondTtsItem2 = new JMenuItem("转债播报程序实盘启动");
        bondTtsItem2.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondBuyNotify.changeEnvironmentToActualTrading(); // 实盘环境
                                    BondBuyNotify.main1();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem2);
        JMenuItem bondTtsItem3 = new JMenuItem("转债播报程序停止!");
        bondTtsItem3.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondBuyNotify.stopBroadcast();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem3);

        JMenuItem bondTtsItem4 = new JMenuItem("核按钮快捷方式切换同花顺");
        bondTtsItem4.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ThreadUtil.execAsync(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    BondReviseUtil.recoverNuclearKeyBoardSettingToThs();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, true);
                    }
                }
        );
        startMenu.add(bondTtsItem4);


        startMenu.add(new JMenuItem("备用"));


        // 把菜单加入到菜单栏
        menuBar.add(startMenu);
        // 把菜单栏加入到frame，这里用的是set而非add
        this.setJMenuBar(menuBar);
    }

    /**
     * 主窗口打开回调, 非子控件相关的其他部分
     */
    private void whenWindowOpened() {
        SmartFindDialog.addGlobalSmartFinder(); // 窗口打开后, 首先添加只能查找框, 不可见
        BondReviseUtil.initNuclearKeyBoardSettingForRevise(); // 更改核按钮配置到复盘

        ThreadUtil.execAsync(() -> {
            try {
                this.setExtendedState(JFrame.MAXIMIZED_BOTH); // 最大化
                this.getCorePanel().flushAllFuncFrameBounds(); // 实测必须,否则主内容左侧无法正确初始化
                ThreadUtil.sleep(200);
                if (autoOpenLogsWindow) {
                    this.getCorePanel().getBottomLeftButtonList().get(0).doClick(); // 日志框显示
                }
                if (autoOpenManiLogsWindow) {
                    this.getCorePanel().getBottomLeftButtonList().get(1).doClick(); // 操作日志框显示
                }
                if (autoOpenFuncTree) {
                    this.getCorePanel().getLeftTopButtonList().get(0).doClick();
                }

                if (autoStartTrader) {
                    Trader.getAndStartInstance();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, true);

        if (autoStartEmNewFetcher) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    CronUtil.schedule("*/50 * * * * *", new Task() {
                        @Override
                        public void execute() {
                            ThreadUtil.sleep(2000);
                            new CaiJingDaoDuCrawlerEm().run(); // 财经导读抓取
                            new ZiXunJingHuaCrawlerEm().run(); // 资讯精华抓取
                            new CompanyMajorIssuesCrawlerEm().run(); // 重大事件抓取
                            new CompanyGoodNewsCrawlerEm().run(); // 利好抓取
                            new NewsFeedsCrawlerEm().run(); // 新闻联播集锦
                            new FourPaperNewsCrawlerEm().run(); // 四大报媒精华
                        }
                    });
                    CronUtil.setMatchSecond(true); // 第一位为秒, 否则为分
                    CronUtil.start();
                }
            });
        }

        if (autoNewConceptDiscover) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    NewConceptDiscover.newConceptDiscoverStarter(5, 5);
                }
            }, true);
        }

        if (autoEmPc724NewsNotify) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EmPcNewsNotify.notifyFast724New(); // 7*24
                }
            }, true);
        }

        if (autoEmPcHotNewsNotify) {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    EmPcNewsNotify.notifyNewestHotNew(); //
                }
            }, true);
        }

    }


    FuncTreeWindow funcTreeWindow;
    public FuncButton objectsBtn;
    public FuncButton maniLogFunc;
    public FuncButton logsFunc;
    // AnalyzeRealtimeWindow analyzeRealtimeWindow;
    static volatile Trader trader;

    private void addListeners() {
        TraderGui mainWindow = this;
        // 打开后启动交易程序
        this.addWindowListener(new WindowAdapter() {
            @SneakyThrows
            @Override
            public void windowOpened(WindowEvent e) {
                MainDisplayWindow mainDisplayWindow = MainDisplayWindow.getInstance(
                        "编辑器", mainWindow, false, false, true,
                        4096, 100, 1.0, 0, layerOfMainDisplay
                );
                mainWindow.getCorePanel().setMainDisplayWindow(mainDisplayWindow); // 必须手动设定

                corePanel.getFuncPool().put(ButtonFactory.getButton("mainDisplay"), mainDisplayWindow); // 仅加入池,
                // 无对应button
                mainDisplayWindow.flushBounds(true);
                mainDisplayWindow.setAutoMaxWidthOrHeight(corePanel.getWidth());
                mainDisplayWindow.show();


                logsFunc = ButtonFactory.getButton("日志输出");
                logsFunc.registerKeyboardAction(e1 -> logsFunc.doClick(), LOGS_BTN,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(logsFunc, FuncFrameS.Type.BOTTOM_LEFT);
                logsFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        LogFuncWindow logFuncWindow =  // 窗口启动时已经初始化, 单例模式可调用无参方法
                                LogFuncWindow
                                        .getInstance(FuncFrameS.Type.BOTTOM_LEFT, "logs", mainWindow, logsFunc, true,
                                                true,
                                                false,
                                                true,
                                                1200, 100, 0.3, 30, false, layerOfLogFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(logsFunc, logFuncWindow);
                        if (logFuncWindow.isVisible()) {
                            logFuncWindow.flushBounds();
                            logFuncWindow.hide();
                        } else {
                            logFuncWindow.flushBounds();
                            logFuncWindow.show();
                        }
                    }
                });

                // ManipulateLogWindow

                maniLogFunc = ButtonFactory.getButton("操作日志");
                maniLogFunc.registerKeyboardAction(e1 -> maniLogFunc.doClick(), MANI_LOG_BTN,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(maniLogFunc, FuncFrameS.Type.BOTTOM_LEFT);
                maniLogFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ManipulateLogWindow manipulateLogWindow =  // 窗口启动时已经初始化, 单例模式可调用无参方法
                                ManipulateLogWindow
                                        .getInstance(FuncFrameS.Type.BOTTOM_LEFT, "manis", mainWindow, maniLogFunc,
                                                true,
                                                true,
                                                false,
                                                true,
                                                1200, 100, 0.35, 30, true, layerOfManiLogFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(maniLogFunc, manipulateLogWindow);
                        if (manipulateLogWindow.isVisible()) {
                            manipulateLogWindow.flushBounds();
                            manipulateLogWindow.hide();
                        } else {
                            manipulateLogWindow.flushBounds();
                            manipulateLogWindow.show();
                        }
                    }
                });


                FuncButton databaseFunc = ButtonFactory.getButton("数据库", true);
                corePanel.registerFuncBtnWithoutFuncFrame(databaseFunc, FuncFrameS.Type.RIGHT_TOP);
                databaseFunc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DatabaseFuncWindow databaseFuncWindow = DatabaseFuncWindow
                                .getInstance(FuncFrameS.Type.RIGHT_TOP,
                                        "database", mainWindow, databaseFunc, true,
                                        true, false, true,
                                        1500, 100, 0.2, 30, false, layerOfDatabaseFuncWindow);
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(databaseFunc, databaseFuncWindow);
                        if (databaseFuncWindow.isVisible()) {
                            databaseFuncWindow.flushBounds();
                            databaseFuncWindow.hide();
                        } else {
                            databaseFuncWindow.flushBounds();
                            databaseFuncWindow.show();
                        }
                    }
                });

                objectsBtn = ButtonFactory.getButton("功能树", true);
                // objectsBtn.setMnemonic(KeyEvent.VK_O);
                objectsBtn.registerKeyboardAction(e1 -> objectsBtn.doClick(), OBJECT_TREE_KS,
                        JComponent.WHEN_IN_FOCUSED_WINDOW);
                corePanel.registerFuncBtnWithoutFuncFrame(objectsBtn, FuncFrameS.Type.LEFT_TOP);
                objectsBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        funcTreeWindow = FuncTreeWindow
                                .getInstance(FuncFrameS.Type.LEFT_TOP, "功能树",
                                        mainWindow, objectsBtn, true, false, false, true, 1000, 100, 0.12, 30,
                                        false,
                                        layerOfObjectsTree + 1); // 一定不为null, 单例
                        corePanel.registerFuncBtnAndCorrespondFuncFrame(objectsBtn, funcTreeWindow);
                        if (funcTreeWindow.isVisible()) {
                            funcTreeWindow.flushBounds();
                            funcTreeWindow.hide();
                        } else {
                            funcTreeWindow.flushBounds(true);
                            funcTreeWindow.show();
                        }
                    }
                });

                mainWindow.whenWindowOpened();


            }

            //捕获窗口关闭事件
            @SneakyThrows
            @Override
            public void windowClosing(WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(mainWindow, GuiCommonUtil.buildDialogShowStr("确认关闭", "将关闭程序",
                        "yellow", "red"),
                        "是否关闭程序",
                        JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    BondReviseUtil.recoverNuclearKeyBoardSettingToThs(); // 恢复核按钮配置
                    SystemTray.getSystemTray().remove(trayIcon); // 图标消失
                    if (Trader.getInstance() != null) {
                        Trader.getInstance().stopTrade();
                    }
                    System.exit(0);
                }
            }

            //捕获窗口最小化事件
            @Override
            public void windowIconified(WindowEvent e) {
//                if (SystemTray.isSupported()) {
//                    setVisible(false);
//
//                } else {
//                     默认行为, 最小化到任务栏
//                }
            }
        });

        // 尺寸改变
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 应当刷新bounds, 将自动重绘

            }
        });

    }


    /**
     * 居中自身, 且宽高为屏幕可用 3/4
     */
    private void centerSelf() {
        this.setLocation(screenW / 8, screenH / 8);
        this.setPreferredSize(new Dimension((int) (screenW * 0.75), (int) (screenH * 0.75)));
    }


    /**
     * 核心内容 Panel. 含 左右下 3个按钮列, 以及 JSplitPanel 的项目+编辑器 主界面
     *
     * @return
     */
    public CorePanel buildCorePanel() {
        return new CorePanel(30, 30, 30, this);
    }


    private void initTrayIcon() {
        PopupMenu popup = new PopupMenu();
        MenuItem exitItem = new MenuItem("还原");
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(true);
                setExtendedState(Frame.NORMAL);
            }
        };
        exitItem.addActionListener(listener);
        MenuItem exitItem2 = new MenuItem("关闭程序");
        exitItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SystemTray.getSystemTray().remove(trayIcon); // 图标消失
                System.exit(0);
            }
        });
        popup.add(exitItem);
        popup.add(exitItem2);

        //根据image、提示、菜单创建TrayIcon
        this.trayIcon = new TrayIcon(
                imageIcon.getImage(),
                "Scareers", popup);
        trayIcon.setImageAutoSize(true); // 自动缩放, 避免无法显示
        this.trayIcon.addActionListener(listener);
    }

    public void showSystemTray() {
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(this.trayIcon);
        } catch (AWTException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * 初始化各项默认 LookAndFeel设置, 可依需要修改
     *
     * @throws UnsupportedLookAndFeelException
     */
    public static void initGlobalStyle() throws UnsupportedLookAndFeelException {
        ToolTipManager.sharedInstance().setDismissDelay(50000000); // tooptip持续时间
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // 重写ui类, 继承 Metal相关. 此为默认lookandfeel, 显式设置一下
        UIDefaults defs = UIManager.getDefaults();

        defs.put("TextPane.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("TextPane.inactiveBackground", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("SplitPane.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("SplitPane.inactiveBackground", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("TabbedPane.background", new ColorUIResource(COLOR_THEME_MINOR));

        defs.put("Button.shadow", COLOR_THEME_MAIN);
        defs.put("Button.select", COLOR_THEME_MAIN);
        defs.put("Button.focus", COLOR_THEME_MAIN);
        defs.put("Button.background", new ColorUIResource(COLOR_THEME_MAIN));
        defs.put("Button.foreground", new ColorUIResource(COLOR_THEME_MAIN));//
        defs.put("Button.margin", new InsetsUIResource(2, 2, 2, 3));// 因为有竖直button,这里margin初始化
        defs.put("Button.gradient", null);// 将渐变去除

        defs.put("Panel.background", new ColorUIResource(COLOR_THEME_MINOR));
        defs.put("Panel.inactiveBackground", new ColorUIResource(COLOR_THEME_MINOR));

        defs.put("activeCaption", new javax.swing.plaf.ColorUIResource(Color.orange));
        defs.put("activeCaptionText", new javax.swing.plaf.ColorUIResource(Color.red));
        // System.out.println(JSONUtilS.toJsonPrettyStr(JSONUtilS.parse(defs)));

        UIManager.put("InternalFrame.activeTitleBackground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.activeTitleForeground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.inactiveTitleBackground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));
        UIManager.put("InternalFrame.inactiveTitleForeground", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));

        // 分割面板
        UIManager.put("SplitPaneDivider.draggingColor", new javax.swing.plaf.ColorUIResource(Color.red));
        UIManager.put("SplitPaneDivider.border", null);

        // 滚动条
        UIManager.put("ScrollBar.width", 12); // 滚动条宽度
        UIManager.put("ScrollBar.thumb", new javax.swing.plaf.ColorUIResource(Color.black)); // 滚动条上下按钮背景色

        // 对话框
        // 使得背景色同信息显示的背景色, 不会白色一块
        UIManager.put("OptionPane.background", new javax.swing.plaf.ColorUIResource(COLOR_THEME_MINOR));

    }

    /**
     * 假设状态栏在下方. 初始化可用的全屏幕的 宽,高
     */
    public static void initScreenBounds() {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        screenW = screenSize.width - insets.left - insets.right;
        screenH = screenSize.height - insets.top - insets.bottom;
    }


    public JLabel getPathLabel() {
        return pathLabel;
    }

    public JLabel getStatusBar() {
        return statusBar;
    }

    public CorePanel getCorePanel() {
        return corePanel;
    }

    public JDesktopPane getMainPane() {
        return mainPane;
    }

    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    public JMenu getStartMenu() {
        return startMenu;
    }

    public FuncTreeWindow getFuncTreeWindow() {
        return funcTreeWindow;
    }
}
