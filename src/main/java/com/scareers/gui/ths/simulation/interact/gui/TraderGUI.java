package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.json.JSONUtil;
import com.scareers.gui.ths.simulation.interact.gui.component.JButtonV;
import com.scareers.gui.ths.simulation.interact.gui.component.forlog.JDisplayForLog;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.TabbedPaneUIS;
import com.scareers.gui.ths.simulation.trader.Trader;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMain;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMinor;


/**
 * description: 模拟交易后台gui, 主要用于查看模拟交易进行各种状况.
 *
 * @key1 gui风格模仿idea;
 * @key2 主界面子组件:
 * 1.菜单栏, --> 常规实现
 * 2.工具栏 --> 常规实现
 * 3.路径栏(状态栏1) --> 多级label+> , 容器FlowLayout
 * 4.右侧边功能栏,可调宽度 JTabbedPane, 各panel为子功能
 * --> 内容控件或许是 JInternalFrame 或者 自定义(例如按钮容器+内容容器) , 主要含功能最小化.
 * 5.下边功能栏,可调高度 JTabbedPane , 各panel为子功能.
 * --> 内容控件或许是 JInternalFrame 或者 自定义(例如按钮容器+内容容器) , 主要含功能最小化.
 * 6.状态栏, 右侧含按钮 --> 简单容器放在下即可, 添加按钮
 * 7.左功能栏 + 主编辑器,  占据主要界面. 左功能栏可关闭
 * --> 标准 JSplitPane, 左功能栏可关闭. 编辑框不可最小化. 编辑框为 JTabbedPane,动态增加tab
 * @key3 主界面主布局采用  JDesktopPane / JLayeredPane
 * 使得 右/下 功能栏可以叠加到 主界面(左功能+编辑器)之上.
 * @author: admin
 * @date: 2022/1/4/004-17:03:03
 */
public class TraderGUI {
    JFrame mainWindow;
    int screenW; // 除去任务栏, 可用的全屏宽度/高度
    int screenH;

    public static void main(String[] args) throws Exception {
        main0(args);
    }

    public static void main0(String[] agrs) throws Exception {
        TraderGUI gui = new TraderGUI();
        gui.initScreenBounds();
        gui.initGlobalStyle();
        gui.initMainWindow();
        gui.showAndStartTrader();
    }

    private void showAndStartTrader() throws Exception {
        JLabel pathLabel = new JLabel("paths: ");
        pathLabel.setFont(new Font("宋体", Font.BOLD, 15));
        pathLabel.setForeground(Color.RED);
        pathLabel.setPreferredSize(new Dimension(100, 20));

        JLabel label = new JLabel("Running");
        label.setFont(new Font("宋体", Font.BOLD, 15));
        label.setForeground(Color.RED);
        label.setPreferredSize(new Dimension(100, 20));

        JPanel corePanel = buildCorePanel(mainWindow);
        mainWindow.add(pathLabel, BorderLayout.NORTH);
        mainWindow.add(corePanel, BorderLayout.CENTER);
        mainWindow.add(label, BorderLayout.SOUTH);


        mainWindow.pack();
        mainWindow.setVisible(true);
        Trader.main0(null);
    }

    /**
     * 核心内容 Panel. 含 左右下 3个按钮列, 以及 JSplitPanel 的项目+编辑器 主界面
     *
     * @return
     */
    public JPanel buildCorePanel(JFrame mainWindow) {
        JPanel corePane = new JPanel();


        // 1. 左主要树形菜单 + 编辑器  JSplitPane,不定宽高,自动BorderLayout适应
        JPanel leftPane = new JPanel(); // mainTree
        leftPane.setPreferredSize(new Dimension(100, 200)); // 定宽
        leftPane.setBackground(Color.yellow);
        leftPane.setOpaque(true);

        JPanel editor = new JPanel();
        editor.setBackground(Color.green);
        editor.setOpaque(true);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); //设定为左右拆分布局
        centerSplitPane.setBorder(null);
        centerSplitPane.setOneTouchExpandable(true); // 让分割线显示出箭头
        centerSplitPane.setContinuousLayout(true); // 操作箭头，重绘图形
        centerSplitPane.setDividerSize(20); //设置分割线的宽度
        centerSplitPane.setLeftComponent(leftPane);
        centerSplitPane.setRightComponent(editor);
        centerSplitPane.setOpaque(true);

        // 2. 左工具栏 JPanel,     box + 2Panel(一上Flow, 一下Flow) // 按钮列表
        JPanel leftTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        leftTools.setLayout(new BoxLayout(leftTools, BoxLayout.Y_AXIS)); // 上下
        leftTools.setPreferredSize(new Dimension(20, 100)); // 定宽
        JPanel panel1 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0));  // 上, 上浮动
        JPanel panel2 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, 0, 0)); // 下, 下浮动
        JButton projectButton = ButtonFactory.getButton("对象查看", true);
        panel1.add(projectButton);
        projectButton.setBackground(colorThemeMinor);
        JButton favoritesButton = ButtonFactory.getButton("数据查看", true);
        panel2.add(favoritesButton);
        leftTools.add(panel1);
        leftTools.add(panel2);


        // 3. 右工具栏 类似2
        JPanel rightTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        rightTools.setLayout(new BoxLayout(rightTools, BoxLayout.Y_AXIS)); // 上下
        rightTools.setPreferredSize(new Dimension(20, 100)); // 定宽
        JPanel panel3 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0));  // 上, 上浮动
        JPanel panel4 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, 0, 0)); // 下, 下浮动
        JButton databaseButton = ButtonFactory.getButton("数据库", true);
        panel3.add(databaseButton);
        JButton mavenButton = ButtonFactory.getButton("书签", true);
        panel4.add(mavenButton);
        rightTools.add(panel3);
        rightTools.add(panel4);


        // 4. 下工具栏, 横向排布,逻辑类似2,3
        JPanel bottomTools = new JPanel(); // 工具栏包含2个Panel, 一个左浮动, 一个右浮动
        bottomTools.setLayout(new BoxLayout(bottomTools, BoxLayout.X_AXIS)); // 左右
        bottomTools.setPreferredSize(new Dimension(100, 20)); // 定高
        JPanel panel5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));  // 上, 上浮动
        JPanel panel6 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); // 下, 下浮动

        JButton terminalButton = ButtonFactory.getButton("终端命令行");
        panel5.add(createPlaceholderLabel()); // 前面需要添加占位符label, 宽度等于左工具栏宽度20
        panel5.add(terminalButton);
        JButton runButton = ButtonFactory.getButton("终端2");
        panel6.add(runButton);
        panel6.add(createPlaceholderLabel()); // @noti: 占位label依然需要最后添加, 右Flow应当先加入所有控件,再右对齐
        bottomTools.add(panel5);
        bottomTools.add(panel6);

        corePane.setLayout(new BorderLayout());
        corePane.add(leftTools, BorderLayout.WEST);
        corePane.add(rightTools, BorderLayout.EAST);
        corePane.add(centerSplitPane, BorderLayout.CENTER);
        corePane.add(bottomTools, BorderLayout.SOUTH);
        return corePane;
    }

    public JLabel createPlaceholderLabel() {
        JLabel placeholderLabel = new JLabel();
        placeholderLabel.setSize(new Dimension(20, 20));
        placeholderLabel.setPreferredSize(new Dimension(20, 20));
        return placeholderLabel;
    }

    public void initMainWindow() {
        mainWindow = new JFrame("Trader");    //创建一个JFrame对象
        mainWindow.setLayout(new BorderLayout());
        mainWindow.setBounds(0, 0, screenW, screenH);
        mainWindow.setUndecorated(false); // 标题栏显示
        ImageIcon imageIcon = new ImageIcon(ClassLoader.getSystemResource("gui/img/titleIcon0.png"));
        mainWindow.setIconImage(imageIcon.getImage());
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public void initGlobalStyle() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new MetalLookAndFeel()); // 重写ui类, 继承 Metal相关. 此为默认lookandfeel, 显式设置一下
        UIDefaults defs = UIManager.getDefaults();

        defs.put("TextPane.background", new ColorUIResource(colorThemeMain));
        defs.put("TextPane.inactiveBackground", new ColorUIResource(colorThemeMain));
        defs.put("SplitPane.background", new ColorUIResource(colorThemeMain));
        defs.put("SplitPane.inactiveBackground", new ColorUIResource(colorThemeMain));
        defs.put("TabbedPane.background", new ColorUIResource(colorThemeMinor));

        defs.put("Button.shadow", colorThemeMain);
        defs.put("Button.select", colorThemeMain);
        defs.put("Button.focus", colorThemeMain);
        defs.put("Button.background", new ColorUIResource(colorThemeMain));
        defs.put("Button.foreground", new ColorUIResource(colorThemeMain));//
        defs.put("Button.margin", new InsetsUIResource(2, 2, 2, 3));// 因为有竖直button,这里margin初始化
        defs.put("Button.gradient", null);// 将渐变去除

        defs.put("Panel.background", new ColorUIResource(colorThemeMinor));
        defs.put("Panel.inactiveBackground", new ColorUIResource(colorThemeMinor));
//                System.out.println(JSONUtil.toJsonPrettyStr(JSONUtil.parse(defs)));
    }

    public void initScreenBounds() {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        screenW = screenSize.width - insets.left - insets.right;
        screenH = screenSize.height - insets.top - insets.bottom;
    }
}
