package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.base.FuncDialogS;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.LogFuncWindow;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.trader.Trader;
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
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
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
@Getter
public class TraderGui extends JFrame {
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
        TraderGui gui = new TraderGui();
        gui.setVisible(true);
        waitForever();
    }


    JLabel pathLabel; // 路径栏, 待完善
    JLabel statusBar; // 状态栏, 待完善
    CorePanel corePanel; // 核心组件

    CopyOnWriteArraySet<FuncDialogS> funcDialogs = new CopyOnWriteArraySet<>(); // 各个用对话框实现的子功能组件, 注册到队列. 当主界面size变化, 应当重置位置

    public TraderGui() throws Exception {
        super();
        init(); // 组装子控件

    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.setUndecorated(false); // 标题栏显示,true 则类似专注模式
        ImageIcon imageIcon = new ImageIcon(ResourceUtil.getResource(ICON_PATH));
        this.setIconImage(imageIcon.getImage()); // 图标

        if (MAXIMIZE_DEFAULT) {
            this.setExtendedState(JFrame.MAXIMIZED_BOTH); // 直接最大化
        } else {
            centerSelf();
        }

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 退出回调, 建议 HIDE_ON_CLOSE / EXIT_ON_CLOSE
        addListeners(); // 添加监听器

        pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("宋体", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);
        pathLabel.setPreferredSize(new Dimension(100, 20));

        statusBar = new JLabel("Running");
        statusBar.setFont(new Font("宋体", Font.BOLD, 15));
        statusBar.setForeground(Color.RED);
        statusBar.setPreferredSize(new Dimension(100, 20));

        corePanel = buildCorePanel();
//        this.add(pathLabel, BorderLayout.NORTH);
//        this.add(corePanel, BorderLayout.CENTER);
        this.setContentPane(corePanel);
//        this.add(statusBar, BorderLayout.SOUTH);
        this.pack();
    }

    private void addListeners() {
        TraderGui mainWindow = this;
        // 打开后启动交易程序
        this.addWindowListener(new WindowAdapter() {
            @SneakyThrows
            @Override
            public void windowOpened(WindowEvent e) {
                ThreadUtil.execAsync(() -> {
                    try {
//                        mainWindow.getCorePanel().flushMainPanelBounds(); // 刷新位置
//                        mainWindow.getCorePanel().getBottomToolsButtonsPre().get(0).doClick();
                        Trader.main0();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, true);
            }
        });

        // 尺寸改变
        this.addComponentListener(new ComponentAdapter() {
            @SneakyThrows
            @Override
            public void componentResized(ComponentEvent e) {
                // 应当刷新bounds, 将自动重绘
                Thread.sleep(20); // 对话框无法渐变size, 因此先等待
                for (FuncDialogS dialog : funcDialogs) {
                    dialog.flushBounds();
                    mainWindow.getCorePanel().flushMainPanelBounds(); // 刷新位置
                }
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
        JButton logsFunc = ButtonFactory.getButton("日志输出");
        TraderGui parent = this;
        logsFunc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                LogFuncWindow logFuncWindow = LogFuncWindow.getInstance(parent, "logs",
//                        30, 0.33, 100, 1080);
//                funcDialogs.add(logFuncWindow);

//                JDesktopPane desktopPane = new JDesktopPane();
//                JInternalFrame internalFrame = createInternalFrame("窗口1", 0, 0, 200, 200);
//                desktopPane.add(internalFrame, Integer.valueOf(100), 0);
//
//                JInternalFrame internalFrame2 = createInternalFrame("窗口2", 100, 100, 300, 300);
//                desktopPane.add(internalFrame2, Integer.valueOf(200), 0);
//                parent.getCorePanel().getCenterSplitPane().setRightComponent(desktopPane);
//
//                desktopPane.setBounds(0,0,1000,500);
//                desktopPane.setVisible(true);

//                try {
//                    // 设置 内部窗口 被选中
//                    internalFrame.setSelected(true);
//                } catch (Exception e2) {
//                    e2.printStackTrace();
//                }

            }
        });

        JButton terminalFunc = ButtonFactory.getButton("终端命令行");
        terminalFunc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("not implement: 点击了终端命令行");
            }
        });

        return new CorePanel(100, 10, 30, 30, 30,
                Arrays.asList(ButtonFactory.getButton("对象查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据查看", true)),
                Arrays.asList(ButtonFactory.getButton("数据库", true)),
                Arrays.asList(ButtonFactory.getButton("书签", true)),
                Arrays.asList(logsFunc),
                Arrays.asList(terminalFunc),
                this
        );
    }

    public static JInternalFrame createInternalFrame(String title, int x, int y, int width
            , int height) {
        // 创建一个内部窗口
        JInternalFrame internalFrame = new JInternalFrame(
                title,  // title
                true,       // resizable
                true,       // closable
                true,       // maximizable
                true        // iconifiable
        );

        // 设置窗口的宽高
        internalFrame.setSize(width, height);
        // 设置窗口的显示位置
        internalFrame.setLocation(x, y);
        // 内部窗口的关闭按钮动作默认就是销毁窗口，所有不用设置
        // internalFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // 创建内容面板
        JPanel panel = new JPanel();

        // 添加组件到面板
        panel.add(new JLabel("Label001"));
        panel.add(new JButton("JButton001"));

        // 设置内部窗口的内容面板
        internalFrame.setContentPane(panel);

        /*
         * 对于内部窗口，还可以不需要手动设置内容面板，直接把窗口当做普通面板使用，
         * 即直接设置布局，然后通过 add 添加组件，如下代码:
         *     internalFrame.setLayout(new FlowLayout());
         *     internalFrame.add(new JLabel("Label001"));
         *     internalFrame.add(new JButton("JButton001"));
         */

        // 显示内部窗口
        internalFrame.setVisible(true);

        return internalFrame;
    }


    /**
     * 初始化各项默认 LookAndFeel设置, 可依需要修改
     *
     * @throws UnsupportedLookAndFeelException
     */
    public static void initGlobalStyle() throws UnsupportedLookAndFeelException {
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
        // System.out.println(JSONUtil.toJsonPrettyStr(JSONUtil.parse(defs)));

        UIManager.put("InternalFrame.activeTitleBackground", new ColorUIResource(Color.black));
        UIManager.put("InternalFrame.activeTitleForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("InternalFrame.titleFont", new Font("Dialog", Font.PLAIN, 11));
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // 重写ui类, 继承 Metal相关. 此为默认lookandfeel, 显式设置一下
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
}
