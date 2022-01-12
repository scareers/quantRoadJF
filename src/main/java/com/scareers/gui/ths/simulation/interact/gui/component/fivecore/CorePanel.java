package com.scareers.gui.ths.simulation.interact.gui.component.fivecore;

import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.colorThemeMinor;
import static com.scareers.gui.ths.simulation.interact.gui.util.GuiCommonUtil.createPlaceholderLabel;

/**
 * description: 核心Pane, BorderLayout布局, 左功能栏, 右功能栏, 下功能栏, 中间为 主树形(类似项目结构)+编辑器  JSplitPane 分割可调
 * 1. 3大功能栏 宽度(左右)  高度(下) 可调. 且下功能栏头尾使用占位Label
 * 2. 左功能栏控制 JSplitPane 的左边控件 mainMenuPanel (主菜单Panel)
 * 3. 右下功能栏点击均显示 Dialog/JInternalFrame/其他自定义子功能控件, 实现相应功能.
 * 4. 详见6大属性
 *
 * @author: admin
 * @date: 2022/1/13/013-02:23:04
 */
@Getter
@Setter
public class CorePanel extends JPanel {
    // 仅做占位符使用的 int, 设定为默认宽/高的值,无实际意义.应当保证控件最终渲染宽/高>此值.
    public static int placeholderWidthOrHeight = 10;

    JPanel leftTools; // 左功能区, 按钮列表
    JPanel rightTools; // 右功能区, 按钮列表
    JPanel bottomTools; // 下功能区, 按钮列表

    JPanel mainFuncPanel; // 左功能实现区, 常为树形菜单形式! 被 leftTools 按钮们控制
    JPanel mainDisplayPanel; // 主要展示区, 对应idea编辑器. Editor
    JSplitPane centerSplitPane; // 分开 mainMenuPanel + mainDisplayPanel, 宽度可调

    int mainFuncPanelDefaultWidth = 100; // 主功能实现区默认宽度
    int centerSplitPaneDividerSize = 20;


    public CorePanel() {
        super();
        initTwoMainPane();
        initLeftTools();


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
        panel5.add(createPlaceholderLabel(20, 20)); // 前面需要添加占位符label, 宽度等于左工具栏宽度20
        panel5.add(terminalButton);
        JButton runButton = ButtonFactory.getButton("终端2");
        panel6.add(runButton);
        panel6.add(createPlaceholderLabel(20, 20)); // @noti: 占位label依然需要最后添加, 右Flow应当先加入所有控件,再右对齐
        bottomTools.add(panel5);
        bottomTools.add(panel6);

        this.setLayout(new BorderLayout());
        this.add(leftTools, BorderLayout.WEST);
        this.add(rightTools, BorderLayout.EAST);
        this.add(centerSplitPane, BorderLayout.CENTER);
        this.add(bottomTools, BorderLayout.SOUTH);
    }

    private void initLeftTools() {
        // 2. 左工具栏 JPanel,     box + 2Panel(一上Flow, 一下Flow) // 按钮列表
        leftTools = new JPanel();
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
    }

    private void initTwoMainPane() {
        // 1. 左主要树形菜单 + 编辑器  JSplitPane,不定宽高,自动BorderLayout适应
        mainFuncPanel = new JPanel(); // mainTree
        mainFuncPanel.setPreferredSize(new Dimension(mainFuncPanelDefaultWidth, placeholderWidthOrHeight)); // 定默认宽

        mainDisplayPanel = new JPanel();

        centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // 设定为左右拆分布局
        centerSplitPane.setBorder(null);
        centerSplitPane.setOneTouchExpandable(true); // 让分割线显示出箭头
        centerSplitPane.setContinuousLayout(true); // 调整时实时重绘
        centerSplitPane.setDividerSize(centerSplitPaneDividerSize); //设置分割线的宽度
        centerSplitPane.setLeftComponent(mainFuncPanel);
        centerSplitPane.setRightComponent(mainDisplayPanel);
    }
}
