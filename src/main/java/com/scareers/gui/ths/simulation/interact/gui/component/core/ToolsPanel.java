package com.scareers.gui.ths.simulation.interact.gui.component.fivecore;

import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * description: 左功能栏, 右功能栏, 下功能栏.  含多 Button 的 Panel.
 * 可纵向, 可横向
 *
 * @author: admin
 * @date: 2022/1/13/013-02:51:06
 * @see ButtonFactory 注意, 传递的button, 应当与本对象 横向纵向相匹配 , ButtonFactory.getButton("对象查看", true) 第二参数
 */
@Getter
@Setter
public class ToolsPanel extends JPanel {
    public static int placeholderHightOrWidth = 100; // 占位的默认高或宽, 对应属性的 宽或高, 组成 Dimension

    // 需要传递的属性:
    int widthOrHeight;
    ToolsPanelType type;
    CopyOnWriteArrayList<JButton> buttonsOfPanel1;
    CopyOnWriteArrayList<JButton> buttonsOfPanel2;
    // 内部两Panel的 纵横向 gap
    int panel1HGap;
    int panel1VGap;
    int panel2HGap;
    int panel2VGap;
    // 可null的前后占位符Label控件
    JLabel startPlaceholder;
    JLabel endPlaceholder;

    // 自动计算的属性
    JPanel panel1;
    JPanel panel2;
    CopyOnWriteArrayList<String> buttonTextsOfPanel1 = new CopyOnWriteArrayList<>(); // 列表均线程安全
    CopyOnWriteArrayList<String> buttonTextsOfPanel2 = new CopyOnWriteArrayList<>();

    public ToolsPanel(int widthOrHeight, ToolsPanelType type,
                      CopyOnWriteArrayList<JButton> buttonsOfPanel1, CopyOnWriteArrayList<JButton> buttonsOfPanel2,
                      int panel1HGap, int panel1VGap,
                      int panel2HGap, int panel2VGap) {
        this(widthOrHeight, type,
                buttonsOfPanel1, buttonsOfPanel2, panel1HGap, panel1VGap,
                panel2HGap, panel2VGap, null, null);
    }

    public ToolsPanel(int widthOrHeight, ToolsPanelType type,
                      CopyOnWriteArrayList<JButton> buttonsOfPanel1, CopyOnWriteArrayList<JButton> buttonsOfPanel2,
                      int panel1HGap, int panel1VGap,
                      int panel2HGap, int panel2VGap, JLabel startPlaceholder, JLabel endPlaceholder) {
        super();
        this.widthOrHeight = widthOrHeight;
        this.type = type;
        this.buttonsOfPanel1 = buttonsOfPanel1;
        this.buttonsOfPanel2 = buttonsOfPanel2;
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
        for (JButton button : buttonsOfPanel1) {
            panel1.add(button);
            buttonTextsOfPanel1.add(button.getText());
        }
        for (JButton button : buttonsOfPanel2) {
            panel2.add(button);
            buttonTextsOfPanel2.add(button.getText());
        }
        if (endPlaceholder != null) {
            panel2.add(endPlaceholder); // 添加后占位符控件
        }

        this.add(panel1);
        this.add(panel2);
    }

    public static enum ToolsPanelType {
        VERTICAL,
        HORIZONTAL
    }
}
