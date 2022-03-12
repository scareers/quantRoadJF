package com.scareers.gui.ths.simulation.interact.gui.component.core;

import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * description: 左功能栏, 右功能栏, 下功能栏.  含多 Button 的 Panel.
 * --> "功能栏 == 按钮组"  , 均分为两个内部 panel, 管理着向两侧浮动的 按钮们.
 *
 * @key1 该Panel 不再管理自身2大按钮列表, 改为由 CorePane 管理, 本身只做 panel1和2管理
 * @key2 3侧功能栏的按钮列表对象由CorePane维护, 子功能框的按钮列表由 FuncFrameS0 维护
 * @author: admin
 * @date: 2022/1/13/013-02:51:06
 * @see ButtonFactory 注意, 传递的button, 应当与本对象 横向纵向相匹配 , ButtonFactory.getButton("功能树", true) 第二参数
 */
@Getter
@Setter
public class ToolsPanel extends JPanel {
    public static int placeholderHightOrWidth = 100; // 占位的默认高或宽, 对应属性的 宽或高, 组成 Dimension

    // 需要传递的属性:
    int widthOrHeight;
    ToolsPanelType type;

    // 内部两Panel的 纵横向 gap
    int panel1HGap;
    int panel1VGap;
    int panel2HGap;
    int panel2VGap;

    // 可null的前后占位符Label控件
    JLabel startPlaceholder;
    JLabel endPlaceholder;

    // 自动计算的属性
    JPanel panel1; // 前或上 panel容器
    JPanel panel2; // 后或下 panel容器, 方便使用左右, 上下浮动布局


    public ToolsPanel(int widthOrHeight, ToolsPanelType type,
                      int panel1HGap, int panel1VGap,
                      int panel2HGap, int panel2VGap) {
        this(widthOrHeight, type, panel1HGap, panel1VGap,
                panel2HGap, panel2VGap, null, null);
    }

    public ToolsPanel(int widthOrHeight, ToolsPanelType type,
                      int panel1HGap, int panel1VGap,
                      int panel2HGap, int panel2VGap, JLabel startPlaceholder, JLabel endPlaceholder) {
        super();
        this.widthOrHeight = widthOrHeight;
        this.type = type;

        this.panel1HGap = panel1HGap;
        this.panel1VGap = panel1VGap;
        this.panel2HGap = panel2HGap;
        this.panel2VGap = panel2VGap;
        this.startPlaceholder = startPlaceholder;
        this.endPlaceholder = endPlaceholder;
        init();
    }

    private void init() {
        if (type.equals(ToolsPanelType.VERTICAL)) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // 上下
            this.setPreferredSize(new Dimension(widthOrHeight, placeholderHightOrWidth)); // 定宽
            panel1 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, panel1HGap, panel1VGap));  // 上, 上浮动
            panel2 = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.BOTTOM, panel2HGap, panel2HGap)); // 下, 下浮动
        } else {
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS)); // 左右
            this.setPreferredSize(new Dimension(placeholderHightOrWidth, widthOrHeight)); // 定高
            panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, panel1HGap, panel1VGap));  // 左浮动
            panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, panel2VGap, panel2VGap)); // 右浮动
        }
        if (startPlaceholder != null) {
            panel1.add(startPlaceholder); // 添加前占位符
        }
        if (endPlaceholder != null) {
            panel2.add(endPlaceholder); // 添加后占位符控件
        }

        this.add(panel1);
        this.add(panel2);
    }


    public enum ToolsPanelType {
        VERTICAL,
        HORIZONTAL
    }
}
