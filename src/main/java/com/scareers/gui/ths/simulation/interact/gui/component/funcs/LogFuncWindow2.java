//package com.scareers.gui.ths.simulation.interact.gui.component.funcs;
//
//import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
//import com.scareers.gui.ths.simulation.interact.gui.component.core.ToolsPanel;
//import com.scareers.gui.ths.simulation.interact.gui.component.log.DisplayForLog;
//import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
//import lombok.Getter;
//import lombok.Setter;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * description: 查看日志输出, 约等于idea的 Run 显示信息. 无法显示 print
// *
// * @author: admin
// * @date: 2022/1/13/013-04:41:26
// */
//@Setter
//@Getter
//public class LogFuncWindow2 extends HorizontalFuncDialogS {
//    private static LogFuncWindow2 INSTANCE;
//    public static final int autoMaxHight = 1080; // 自动增加可达到最大高度
//    public static final int autoMinHight = 50; // 自动减小可达到最小高度
//    public static final double preferHeightScale = 0.3; // 默认高度占父亲高度百分比
//
//
//    int preferHeight; //
//
//    TraderGui parent;
//    ToolsPanel funcTools; // 工具按钮组
//    int funcToolsWidth; // 按钮宽度
//
//    DisplayForLog jDisplayForLog; // 主内容
//
//    public static LogFuncWindow2 getInstance(TraderGui owner, String title, OrientationType type, int funcToolsWidth) {
//        if (INSTANCE == null) {
//            INSTANCE = new LogFuncWindow2(owner, title, type, funcToolsWidth);
//        }
//        INSTANCE.flushBounds();
//        INSTANCE.setVisible(true);
//        return INSTANCE;
//    }
//
//    private LogFuncWindow2(TraderGui owner, String title, OrientationType type, int funcToolsWidth) {
//        super(owner, title, ModalityType.MODELESS); // 永不阻塞顶级窗口
//        this.parent = owner; // 达成对 TraderGui 而非父类 owner Window 的访问
//        this.typeS = type;
//        this.funcToolsWidth = funcToolsWidth;
//        this.preferHeight = (int) (this.parent.getHeight() * preferHeightScale); // flushBounds()中重复调用.
//        init();
//    }
//
//    private void init() {
//        this.setLayout(new BorderLayout());
//        jDisplayForLog = new DisplayForLog();
//        this.add(jDisplayForLog, BorderLayout.CENTER); // 默认borderlayout
//
//        ToolsPanel.ToolsPanelType toolsPanelType = ToolsPanel.ToolsPanelType.VERTICAL;
//        // 若本对话框为竖直类型, 则工具栏为水平类型, 且放在 north
//        if (typeS.equals(OrientationType.VERTICAL)) {
//            toolsPanelType = ToolsPanel.ToolsPanelType.HORIZONTAL; // 若本对话框为水平类型, 则工具栏为竖直类型, 且放在west
//        }
//
//        funcTools = new ToolsPanel(funcToolsWidth, toolsPanelType,
//                getToolsButtons1(), getToolsButtons2(),
//                0, 0, 0, 0);
//        if (typeS.equals(OrientationType.VERTICAL)) {
//            this.add(funcTools, BorderLayout.NORTH);
//        } else {
//            this.add(funcTools, BorderLayout.WEST);
//        }
//
//        // 注意, 起点(x,y) 应当+主窗口x,y, 因为setBounds本身是绝对定位
//        this.flushBounds();
//        this.setResizable(true);
//        this.setFocusable(true);
//        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏而非真正关闭,配合单例模式
//    }
//
//
//    @Override
//    protected List<JButton> getToolsButtons1() {
//        LogFuncWindow2 dialog = this;
//        JButton resetBounds = ButtonFactory.getButton("置");
//        resetBounds.setToolTipText("重置位置");
//
//        resetBounds.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                dialog.flushBounds();
//            }
//        });
//
//        JButton higherButton = ButtonFactory.getButton("上");
//        higherButton.setToolTipText("增大高度");
//
//        higherButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                Rectangle rawbounds = dialog.getBounds();
//                int newHeight = Math.min(dialog.getHeight() * 2, autoMaxHight);
//                int difference = newHeight - dialog.getHeight();
//                if (difference <= 0) {
//                    return;
//                }
//                dialog.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference,
//                        (int) rawbounds.getWidth(), newHeight);
//            }
//        });
//
//        JButton shorterButton = ButtonFactory.getButton("下");
//        shorterButton.setToolTipText("减小高度");
//
//        shorterButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                Rectangle rawbounds = dialog.getBounds();
//                int newHeight = Math.max(dialog.getHeight() / 2, autoMinHight);
//                int difference = newHeight - dialog.getHeight();
//                if (difference >= 0) {
//                    return;
//                }
//                dialog.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference, // y-负数, 变大
//                        (int) rawbounds.getWidth(), newHeight);
//            }
//        });
//
//
//        return Arrays.asList(resetBounds, higherButton, shorterButton);
//    }
//
//    private List<JButton> getToolsButtons2() {
//        return Arrays.asList();
//    }
//
//    @Override
//    public void flushBounds() {
//        this.preferHeight = (int) (this.parent.getHeight() * preferHeightScale);
//        this.setBounds(
//                //  x = 左侧边栏X + 左侧边栏宽度
//                parent.getCorePanel().getLeftTools().getX() + parent.getCorePanel().getLeftTools().getWidth() + parent
//                        .getX(),
//                // y = 主界面底部 - 状态栏高度 - 底部栏高度 + 2(像素修正)
//                parent.getY() + parent.getHeight() - parent.getStatusBar().getHeight() - parent.getCorePanel()
//                        .getBottomTools().getHeight() - preferHeight + 3,
//                // 宽度 = 主宽 - (两个侧边栏宽之和)
//                parent.getWidth() - parent.getCorePanel().getLeftTools().getWidth() - parent.getCorePanel()
//                        .getRightTools().getWidth(),
//                preferHeight);
//    }
//
//
//}
