package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.core.ToolsPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * description: 自定义 FuncFrameS0 抽象类, 逻辑意义是: 各种子功能使用的组件, 基组件是 JInternalFrame
 * 主要添加刷新 bounds 等方法, 该方法从 主窗口获取信息, 并更新自身 bounds.
 * 各个子功能的子窗口, 均需要继承此抽象类. 以便主窗口统一控制.
 * 当主窗口 reSize, 则遍历所有绑定的 FuncFrameS0, 它们均调用 flushBounds(),  重绘.
 *
 * @author: admin
 * @date: 2022/1/13/013-07:27:42
 * @see flushBounds()
 */
@Setter
@Getter
public abstract class FuncFrameS extends JInternalFrame {
    /**
     * 各功能栏对话框, 共计6种类型. (注意:主编辑区 属于 RIGHT_TOP/Bottom 类型, 宽度较大)
     * 每种类型, 主要在于 flushBounds位置刷新不同
     */
    public enum Type {
        /**
         * 左册工具栏, 上半部分功能区, 依次为 左下, 右上,右下, 底左, 底右
         */
        LEFT_TOP,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_BOTTOM,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    protected static final Log log = LogUtil.getLogger();


    // 需要提供
    Type type; // 功能窗口6类, 核心属性
    TraderGui mainWindow; // 主界面, 以便将自身添加到主内容面板 -- 层级面板中, 自行强转 TraderUI
    FuncButton belongBtn; // 本功能由哪个button控制, 在btn添加监听器时传递
    int autoMaxWidthOrHeight; // 点击增大/缩小按钮时, 最大宽度/高度
    int autoMinWidthOrHeight; // 点击增大/缩小按钮时, 最小宽度/高度
    double preferScale; // 该值final, 为默认的宽/高占 mainPane 大小! 每次初始化size用到
    int funcToolsWidthOrHeight; // 子功能内部工具栏宽/高
    boolean halfWidthOrHeight; // 底栏是否占用一般宽? 左右栏是否占用一般高? idea默认 左下按钮组对话框一半高度
    // Integer layer; // 核心, 自身处于 mainPane 的层次. 越大越上层!. 单层索引默认0

    // 自动初始化
    CorePanel corePanel; // corePanel对象, 从 mainWindow 获取
    MainDisplayWindow mainDisplayWindow; // 编辑区
    JDesktopPane mainPane; // 主界面 mainPane, 层级控件, 从 mainWindow 获取
    int preferWidthOrHeight; // 该宽/高每次经由preferScale 计算得来, 可变.  mainPane 宽度*倍率
    ToolsPanel funcTools; // 内部工具按钮组, 底部功能在左侧, 左右在上侧
    int mainPaneWidth; // 保存mainPane的 高度和宽度, 在构造器中首次更新. 当mainPane大小改变将使用并刷新此值
    int mainPaneHeight; // 逻辑上含义为 上次mainPane的尺寸, 含义为 "上一次 mainPane 的宽高"

    // 维护内部工具栏按钮双列表
    CopyOnWriteArrayList<FuncButton> toolButtonList1 = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<FuncButton> toolButtonList2 = new CopyOnWriteArrayList<>();

    // 抽象方法实现初始化功能框内容
    protected JPanel centerPanel; // 主内容, 若调用特殊方法, 应当强制转型后调用

    // 3抽象方法, 需要填充中央组件, 内部工具栏2类按钮
    protected abstract void initCenterPanel();

    public void setCenterPanel(JPanel centerPanel) {
        this.centerPanel = centerPanel;
        this.add(this.centerPanel, BorderLayout.CENTER);
    }

    /**
     * 需要实现此2方法, 决定功能框 内部工具栏, 常调用 defaultToolsButtonList1/2, 默认实现3按钮在1
     *
     * @return
     */
    protected abstract List<FuncButton> getToolButtons1();

    protected abstract List<FuncButton> getToolButtons2();

    protected List<FuncButton> defaultToolsButtonList1() {
        boolean btnVertical = false; // 按钮是横向类型, 只有底部两种类型变为true,纵向
        if (this.type == Type.BOTTOM_LEFT || this.type == Type.BOTTOM_RIGHT) {
            btnVertical = true; // 按钮类型确定
        }

        // 1.重置按钮功能相同
        FuncFrameS frame = this;
        FuncButton resetBounds = ButtonFactory.getButton("置", btnVertical);
        resetBounds.setToolTipText("重置位置");
        resetBounds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.flushBounds(true);
            }
        });

        // 2.0. 左侧栏功能没有 左右,因此直接返回, 右侧, 下侧有
        if (this.type == Type.LEFT_TOP || this.type == Type.LEFT_BOTTOM) {
            return Arrays.asList(resetBounds);
        }

        // 2. 确定4个文本
        String text1;
        String text2;
        String toolTip1;
        String toolTip2;
        FuncButton btn1; // 增大
        FuncButton btn2; // 减小
        switch (this.type) {
            case RIGHT_TOP:
            case RIGHT_BOTTOM:
                text1 = "左";
                text2 = "右";
                toolTip1 = "增大宽度";
                toolTip2 = "减小宽度";

                btn1 = ButtonFactory.getButton(text1, btnVertical);
                btn1.setToolTipText(toolTip1);
                btn1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Rectangle rawbounds = frame.getBounds();
                        int newWidth = Math.min(frame.getWidth() * 2, autoMaxWidthOrHeight);
                        int difference = newWidth - frame.getWidth();
                        if (difference <= 0) {
                            return;
                        }
                        frame.setBounds(frame.getX() - difference, frame.getY(), newWidth, frame.getHeight());
                    }
                });

                btn2 = ButtonFactory.getButton(text2, btnVertical);
                btn2.setToolTipText(toolTip2);
                btn2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Rectangle rawbounds = frame.getBounds();
                        int newWidth = Math.max(frame.getWidth() / 2, autoMinWidthOrHeight);
                        int difference = newWidth - frame.getWidth();
                        if (difference >= 0) {
                            return;
                        }
                        frame.setBounds(frame.getX() - difference, frame.getY(), newWidth, frame.getHeight());
                    }
                });
                break;
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                text1 = "上";
                text2 = "下";
                toolTip1 = "增大高度";
                toolTip2 = "减小高度";

                btn1 = ButtonFactory.getButton(text1, btnVertical);
                btn1.setToolTipText(toolTip1);

                btn1.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Rectangle rawbounds = frame.getBounds();
                        int newHeight = Math.min(frame.getHeight() * 2, autoMaxWidthOrHeight);
                        int difference = newHeight - frame.getHeight();
                        if (difference <= 0) {
                            return;
                        }
                        frame.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference,
                                (int) rawbounds.getWidth(), newHeight);
                    }
                });

                btn2 = ButtonFactory.getButton(text2, btnVertical);
                btn2.setToolTipText(toolTip2);

                btn2.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Rectangle rawbounds = frame.getBounds();
                        int newHeight = Math.max(frame.getHeight() / 2, autoMinWidthOrHeight);
                        int difference = newHeight - frame.getHeight();
                        if (difference >= 0) {
                            return;
                        }
                        frame.setBounds((int) rawbounds.getX(), (int) rawbounds.getY() - difference, // y-负数, 变大
                                (int) rawbounds.getWidth(), newHeight);
                    }
                });
                break;
            default:
                log.error("致命错误: 未知 FuncFrameS0.Type");
                throw new IllegalStateException("Unexpected value: " + this.type);
        }
        return Arrays.asList(resetBounds, btn1, btn2);
    }

    protected List<FuncButton> defaultToolsButtonList2() {
        return Arrays.asList();
    }

    protected FuncFrameS(Type type, String title, TraderGui mainWindow, FuncButton belongBtn,  // 4基本
                         boolean resizable, boolean closable,
                         boolean maximizable, boolean iconifiable, // 4 父类全参

                         int autoMaxWidthOrHeight, // 5自身功能
                         int autoMinWidthOrHeight,
                         double preferScale,
                         int funcToolsWidthOrHeight,
                         boolean halfWidthOrHeight,
                         Integer layer
    ) {
        this(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer, true);
        // 默认是功能栏, 绑定到 CorePane管理.  仅 主编辑器调用 false 构造器, 且 belongBtn可传递 null.
    }

    protected FuncFrameS(Type type, String title, TraderGui mainWindow, FuncButton belongBtn,  // 4基本
                         boolean resizable, boolean closable,
                         boolean maximizable, boolean iconifiable, // 4 父类全参

                         int autoMaxWidthOrHeight, // 5自身功能
                         int autoMinWidthOrHeight,
                         double preferScale,
                         int funcToolsWidthOrHeight,
                         boolean halfWidthOrHeight,

                         Integer layer,
                         boolean bindToCorePane
    ) {
        this(type, title, mainWindow, belongBtn, resizable, closable, maximizable, iconifiable, autoMaxWidthOrHeight,
                autoMinWidthOrHeight, preferScale, funcToolsWidthOrHeight, halfWidthOrHeight, layer, bindToCorePane,
                true);
    }

    protected FuncFrameS(Type type, String title, TraderGui mainWindow, FuncButton belongBtn,  // 4基本
                         boolean resizable, boolean closable,
                         boolean maximizable, boolean iconifiable, // 4 父类全参

                         int autoMaxWidthOrHeight, // 5自身功能
                         int autoMinWidthOrHeight,
                         double preferScale,
                         int funcToolsWidthOrHeight,
                         boolean halfWidthOrHeight,

                         Integer layer,
                         boolean bindToCorePane,
                         boolean hasInternalTool
    ) {
        super(title, resizable, closable, maximizable, iconifiable);
        this.type = type;

        this.mainWindow = mainWindow;
        this.corePanel = this.mainWindow.getCorePanel();
        this.mainDisplayWindow = corePanel.getMainDisplayWindow();
        this.mainPane = this.corePanel.getMainPane();
        this.mainPaneWidth = this.mainPane.getWidth();
        this.mainPaneHeight = this.mainPane.getHeight();
        this.belongBtn = belongBtn;

        this.preferScale = preferScale;
        this.funcToolsWidthOrHeight = funcToolsWidthOrHeight;
        this.autoMaxWidthOrHeight = autoMaxWidthOrHeight;
        this.autoMinWidthOrHeight = autoMinWidthOrHeight;
        this.halfWidthOrHeight = halfWidthOrHeight;

        flushPreferWidthOrHeight(); // 视type, 决定是默认宽/高
        this.setLayout(new BorderLayout());
        if (hasInternalTool) {
            initInnerFuncTools(); // 需要实现2 abstract, 总之, 默认要求实现 内部工具栏, 以及主内容控件
        }
        initCenterPanel(); // abstract

        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏
        this.setBorder(null); // 无边框

        this.setLayer(layer);
        this.mainPane.add(this, layer, 0);  // @key2: JDesktopPane mainPane 放置自身
        if (bindToCorePane) {
            registerSelfToCorePane(); // 将自身 注册到 corePane,将显示.
        }

        this.flushBounds(true); // 首次刷新, 将尽量采用默认尺寸
    }

    protected void registerSelfToCorePane() {
        this.corePanel.registerFuncBtnAndCorrespondFuncFrame(belongBtn, this);
    }


    /**
     * 抽象方法, 创建核心中央组件, 以做子类区分
     */


    protected void flushPreferWidthOrHeight() {
        switch (this.type) {
            case BOTTOM_LEFT: // 底左,底右 --> 应该对应mainPane高
            case BOTTOM_RIGHT:
                this.preferWidthOrHeight = (int) (this.mainPane.getHeight() * preferScale);
                break;
            default: // 其余四种, 均为宽
                this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale);
        }
    }


    /**
     * 初始化内部小工具栏, 请实现抽象方法 getToolsButtons1(), getToolsButtons2()
     */
    protected void initInnerFuncTools() {
        this.toolButtonList1.addAll(getToolButtons1()); // 初始化内部工具栏
        this.toolButtonList2.addAll(getToolButtons2());
        switch (this.type) {
            case BOTTOM_LEFT: // 底左,底右 , 工具栏垂直, 且放在西方
            case BOTTOM_RIGHT:
                funcTools = new ToolsPanel(funcToolsWidthOrHeight, ToolsPanel.ToolsPanelType.VERTICAL,
                        0, 0, 0, 0);

                this.add(funcTools, BorderLayout.WEST);
                break;
            default: // 其余四种, 均 水平在上
                funcTools = new ToolsPanel(funcToolsWidthOrHeight, ToolsPanel.ToolsPanelType.HORIZONTAL,

                        0, 0, 0, 0);
                this.add(funcTools, BorderLayout.NORTH);
        }
        for (FuncButton btn : this.toolButtonList1) {
            funcTools.getPanel1().add(btn); // 按钮添加显示
        }
        for (FuncButton btn : this.toolButtonList2) {
            funcTools.getPanel2().add(btn);
        }
    }


    public void flushBounds() {
        flushBounds(false);
    }

    /**
     * 刷新位置, 自身已经加入 主面板 JDesktopPane 的某一层-0; 需要提供于 mainPane 的相对bounds
     * 位置需要依据 mainPane 当前情况刷新
     * 由首次刷新, 与非首次刷新的区别!
     */
    public void flushBounds(boolean first) {
        switch (this.type) {
            case RIGHT_TOP:
                if (first) { // 首次刷新, 将读取默认比例
                    this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale); // 需要更新默认宽度
                    actualFlushForRightTop(preferWidthOrHeight);
                } else {
                    double oldScale = (double) this.getWidth() / this.mainPaneWidth; // 注意, 需要读取上次保存的 旧的mainPane尺寸
                    int newWidth = (int) (oldScale * this.mainPane.getWidth()); // 新的尺寸计算, 等比缩放
                    actualFlushForRightTop(newWidth);
                }
                break;
            case RIGHT_BOTTOM:
                if (first) { // 首次刷新, 将读取默认比例
                    this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale); // 需要更新默认宽度
                    actualFlushForRightBottom(preferWidthOrHeight);
                } else {
                    double oldScale = (double) this.getWidth() / this.mainPaneWidth; // 注意, 需要读取上次保存的 旧的mainPane尺寸
                    int newWidth = (int) (oldScale * this.mainPane.getWidth()); // 新的尺寸计算, 等比缩放
                    actualFlushForRightBottom(newWidth);
                }
                break;
            case BOTTOM_LEFT:
                if (first) { // 首次刷新, 将读取默认比例, 并计算最新高度! 并设置最新高度
                    this.preferWidthOrHeight = (int) (this.mainPane.getHeight() * preferScale); // 需要更新默认高度
                    actualFlushForBottomLeft(preferWidthOrHeight);
                } else {
                    double oldScale = (double) this.getHeight() / this.mainPaneHeight; // 注意, 需要读取上次保存的 旧的mainPane尺寸
                    int newHeight = (int) (oldScale * this.mainPane.getHeight()); // 新的尺寸计算, 等比缩放
                    actualFlushForBottomLeft(newHeight);
                }
                break;
            case BOTTOM_RIGHT:
                if (first) { // 首次刷新, 将读取默认比例, 并计算最新高度! 并设置最新高度
                    this.preferWidthOrHeight = (int) (this.mainPane.getHeight() * preferScale); // 需要更新默认高度
                    actualFlushForBottomRight(preferWidthOrHeight);
                } else {
                    double oldScale = (double) this.getHeight() / this.mainPaneHeight; // 注意, 需要读取上次保存的 旧的mainPane尺寸
                    int newHeight = (int) (oldScale * this.mainPane.getHeight()); // 新的尺寸计算, 等比缩放
                    actualFlushForBottomRight(newHeight);
                }
                break;
            case LEFT_TOP:
                if (first) {
                    // 首次刷新, 自身使用默认宽度, 且将读取mainDisplay, 将其宽度修改为 "剩余宽度".
                    // 因mainDisplay也设置了 size改变回调, 也将再次修改 this的宽度, 但已经非第一次. 浪费一点时间
                    this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale); // 需要更新默认高度
                    actualFlushForLeftTop(preferWidthOrHeight); // 刷新
                    this.corePanel.getMainDisplayWindow().setLocation(preferWidthOrHeight, 0);
                    this.corePanel.getMainDisplayWindow().setSize(this.mainPane.getWidth() - preferWidthOrHeight,
                            this.mainPane.getHeight()); // 主编辑器修改宽高.
                } else {
                    int newWidth = mainPane.getWidth() - this.mainWindow.getCorePanel().getMainDisplayWindow()
                            .getWidth();
                    actualFlushForLeftTop(newWidth);
                }
                break;
            case LEFT_BOTTOM:
                if (first) {
                    // 首次刷新, 自身使用默认宽度, 且将读取mainDisplay, 将其宽度修改为 "剩余宽度".
                    // 因mainDisplay也设置了 size改变回调, 也将再次修改 this的宽度, 但已经非第一次. 浪费一点时间
                    this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale); // 需要更新默认高度
                    actualFlushForLeftBottom(preferWidthOrHeight); // 刷新
                    this.corePanel.getMainDisplayWindow().setSize(this.mainPane.getWidth() - preferWidthOrHeight,
                            this.mainPane.getHeight()); // 主编辑器修改宽高.
                } else {
                    int newWidth = mainPane.getWidth() - this.mainWindow.getCorePanel().getMainDisplayWindow()
                            .getWidth();
                    actualFlushForLeftBottom(newWidth);
                }
                break;
            default:
                log.error("致命错误: 未知 FuncFrameS0.Type");
                throw new IllegalStateException("Unexpected value: " + this.type);
        }
        // 无论如何, 均需要刷新mainPane尺寸, 做下一次更新时的 "旧尺寸"
        this.mainPaneWidth = this.mainPane.getWidth();
        this.mainPaneHeight = this.mainPane.getHeight(); // 刷新manePane尺寸
    }

    protected void actualFlushForRightTop(int newWidth) {
        int height = mainPane.getHeight();
        if (halfWidthOrHeight) {
            height /= 2;
        }
        this.setBounds(
                //  x = mainPane宽度 - 自身宽度
                mainPane.getWidth() - newWidth,
                // y = 0
                0,
                newWidth,
                // 高度 = mainPane 高度 或者一半
                height);
    }

    protected void actualFlushForRightBottom(int newWidth) {
        int y = 0;
        if (halfWidthOrHeight) {
            y = mainPane.getHeight() / 2;
        }
        this.setBounds(
                //  x = mainPane宽度 - 自身宽度
                mainPane.getWidth() - newWidth,
                // y = 0 或者中间高度
                y,
                newWidth,
                // 高度 = mainPane 高度 或者一半
                mainPane.getHeight() - y);
    }

    private void actualFlushForBottomLeft(int newHeight) {
        int width = mainPane.getWidth();
        if (halfWidthOrHeight) {
            width /= 2;
        }
        this.setBounds(
                //  x = 0,
                0,
                // y = mainPane高度 - 自身高度
                mainPane.getHeight() - newHeight,
                // 宽度 = mainPane 同宽
                width,
                newHeight);
    }

    private void actualFlushForBottomRight(int newHeight) {
        int x = 0;
        if (halfWidthOrHeight) {
            x = mainPane.getWidth() / 2;
        }
        this.setBounds(
                //  x = 0, 或者宽度中间
                x,
                // y = mainPane高度 - 自身高度
                mainPane.getHeight() - newHeight,
                // 宽度 = mainPane 同宽
                mainPane.getWidth() - x,
                newHeight);
    }

    /**
     * flush逻辑与父类同, 只是实际位置应当不同! 调用该方法前, 若要高度减半,(idea左下册功能), 请手动设置 halfHeight
     *
     * @param newWidth
     */
    protected void actualFlushForLeftTop(int newWidth) {
        int height = mainPane.getHeight();
        if (halfWidthOrHeight) {
            height /= 2;
        }
        this.setBounds(
                0,
                // y = 0
                0,
                newWidth,
                // 高度 = mainPane 高度
                height); // 注意逻辑
    }

    protected void actualFlushForLeftBottom(int newWidth) {
        int y = 0;
        if (halfWidthOrHeight) {
            y = mainPane.getHeight() / 2;
        }
        this.setBounds(
                0,
                // y = 0
                y,
                newWidth,
                // 高度 = mainPane 高度
                mainPane.getHeight() - y); // 注意逻辑
    }
}
