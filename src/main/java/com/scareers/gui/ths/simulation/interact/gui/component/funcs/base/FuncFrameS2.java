package com.scareers.gui.ths.simulation.interact.gui.component.funcs.base;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import com.scareers.gui.ths.simulation.interact.gui.component.core.CorePanel;
import com.scareers.gui.ths.simulation.interact.gui.component.core.ToolsPanel;
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

/**
 * description: 自定义 FuncFrameS 抽象类, 逻辑意义是: 各种子功能使用的组件, 基组件是 JInternalFrame
 * 主要添加刷新 bounds 等方法, 该方法从 主窗口获取信息, 并更新自身 bounds.
 * 各个子功能的子窗口, 均需要继承此抽象类. 以便主窗口统一控制.
 * 当主窗口 reSize, 则遍历所有绑定的 FuncFrameS, 它们均调用 flushBounds(),  重绘.
 *
 * @author: admin
 * @date: 2022/1/13/013-07:27:42
 * @see flushBounds()
 */
@Setter
@Getter
public abstract class FuncFrameS2 extends JInternalFrame {
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
    Integer layer; // 核心, 自身处于 mainPane 的层次. 越大越上层!. 单层索引默认0

    // 自动初始化
    CorePanel corePanel; // corePanel对象, 从 mainWindow 获取
    JDesktopPane mainPane; // 主界面 mainPane, 层级控件, 从 mainWindow 获取
    int preferWidthOrHeight; // 该宽/高每次经由preferScale 计算得来, 可变.  mainPane 宽度*倍率
    ToolsPanel funcTools; // 内部工具按钮组, 底部功能在左侧, 左右在上侧
    int mainPaneWidth; // 保存mainPane的 高度和宽度, 在构造器中首次更新. 当mainPane大小改变将使用并刷新此值
    int mainPaneHeight; // 逻辑上含义为 上次mainPane的尺寸, 含义为 "上一次 mainPane 的宽高"

    // 抽象方法实现初始化功能框内容
    protected Component centerComponent; // 主内容, 若调用特殊方法, 应当强制转型后调用

    // 3抽象方法, 需要填充中央组件, 内部工具栏2类按钮
    protected abstract void initCenterComponent();

    /**
     * 需要实现此2方法, 决定功能框 内部工具栏
     *
     * @return
     */
    protected abstract List<FuncButton> getToolsButtons1();

    protected abstract List<FuncButton> getToolsButtons2();


    public void flushBounds() {
        flushBounds(false); // 无参调用默认为非首次!
    }

    public abstract void flushBounds(boolean first);


    protected FuncFrameS2(Type type, String title, TraderGui mainWindow, FuncButton belongBtn,  // 4基本
                          boolean resizable, boolean closable,
                          boolean maximizable, boolean iconifiable, // 4 父类全参

                          int autoMaxWidthOrHeight, // 5自身功能
                          int autoMinWidthOrHeight,
                          double preferScale,
                          int funcToolsWidthOrHeight,
                          boolean halfWidthOrHeight,

                          Integer layer
    ) {
        super(title, resizable, closable, maximizable, iconifiable);
        this.type = type;

        this.mainWindow = mainWindow;
        this.corePanel = this.mainWindow.getCorePanel();
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
        initInnerFuncTools(); // 需要实现2 abstract, 总之, 默认要求实现 内部工具栏, 以及主内容控件
        initCenterComponent(); // abstract

        this.setDefaultCloseOperation(HIDE_ON_CLOSE); // 关闭时隐藏
        this.setBorder(null); // 无边框

        this.mainPane.add(this, layer, 0);  // @key2: JDesktopPane mainPane 放置自身

        registerSelfToCorePane(); // 将自身 注册到 corePane 的 3大类状态维持队列!(2列表, 1map)

        this.flushBounds(true); // 首次刷新, 将尽量采用默认尺寸
    }

    protected void registerSelfToCorePane() {
        switch (this.type) {
            case BOTTOM_LEFT: // 底左,底右 --> 应该对应mainPane高
                this.mainPane.add(this, layer, 0);  //  JDesktopPane mainPane 放置
                this.getMainWindow().getCorePanel().getFuncFrames().add(this); // 注册自身, 主界面变化时将自动调用 flushBounds()


            case BOTTOM_RIGHT:
                this.preferWidthOrHeight = (int) (this.mainPane.getHeight() * preferScale);
                break;
            default: // 其余四种, 均为宽
                this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferScale);
        }
    }

    ;


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
        switch (this.type) {
            case BOTTOM_LEFT: // 底左,底右 , 工具栏垂直, 且放在西方
            case BOTTOM_RIGHT:
                funcTools = new ToolsPanel(funcToolsWidthOrHeight, ToolsPanel.ToolsPanelType.VERTICAL,
                        getToolsButtons1(), getToolsButtons2(),
                        0, 0, 0, 0);
                this.add(funcTools, BorderLayout.WEST);
                break;
            default: // 其余四种, 均 水平在上
                funcTools = new ToolsPanel(funcToolsWidthOrHeight, ToolsPanel.ToolsPanelType.HORIZONTAL,
                        getToolsButtons1(), getToolsButtons2(),
                        0, 0, 0, 0);
                this.add(funcTools, BorderLayout.WEST);
        }
    }


    /**
     * 刷新位置, 注意, 自身已经加入 主面板 JDesktopPane 的某一层-0;
     * 因此位置需要依据 mainPane 当前情况刷新
     */
    @Override
    public void flushBounds(boolean first) {
        if (first) { // 首次刷新, 将读取默认比例, 并计算最新高度! 并设置最新高度
            this.preferWidthOrHeight = (int) (this.mainPane.getWidth() * preferWidthScale); // 需要更新默认高度
            actualFlush(preferWidthOrHeight);
        } else {
            double oldScale = (double) this.getWidth() / this.mainPaneWidth; // 注意, 需要读取上次保存的 旧的mainPane尺寸
            int newWidth = (int) (oldScale * this.mainPane.getWidth()); // 新的尺寸计算, 等比缩放
            actualFlush(newWidth);
        }
        // 无论如何, 均需要刷新mainPane尺寸, 做下一次更新时的 "旧尺寸"
        this.mainPaneWidth = this.mainPane.getWidth();
        this.mainPaneHeight = this.mainPane.getHeight(); // 刷新manePane尺寸
    }

    protected void actualFlush(int newWidth) {
        this.setBounds(
                //  x = mainPane宽度 - 自身宽度
                mainPane.getWidth() - newWidth,
                // y = 0
                0,
                newWidth,
                // 高度 = mainPane 高度
                mainPane.getHeight());
    }

}
