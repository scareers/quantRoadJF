package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: 展示给定股票盘口信息.
 *
 * @author: admin
 * @date: 2022/2/22/022-17:21:15
 */
public class HandicapPanel extends SecurityDisplayPanel {
    List<HsStatePanel> hsStatePanelList = new ArrayList<>(); // 复用控件
    //    JPanel kernelPanel = new JPanel();
    JXPanel kernelPanel = new JXPanel();
    JScrollPane jScrollPane;

    public HandicapPanel() {
        this.setLayout(new BorderLayout());
        kernelPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP, 1, 1)); // 自定义上下浮动的布局
        kernelPanel.setScrollableTracksViewportHeight(false);
        kernelPanel.setScrollableTracksViewportWidth(true);

        jScrollPane = new JScrollPane();

        jScrollPane.setBorder(null);
        JLabel label = new JLabel("数据获取中"); // 默认显示内容
        label.setForeground(Color.red);
        jScrollPane.setViewportView(label); // 占位
        jScrollPane.getViewport().setBackground(COLOR_THEME_MINOR);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        jScrollPane.getVerticalScrollBar().setUnitIncrement(30); // 增加滚动速度
        jScrollPane.setViewportView(kernelPanel);

        this.add(jScrollPane, BorderLayout.CENTER);

    }

    boolean firstUpdate = true; // 控制第一次替换占位label显式列表

    @Override
    public void update() {

        List<HsState> newStates = LowBuyHighSellStrategyAdapter.stockWithStatesInfByFactorsHs
                .get(this.newBean.getSecCode());
        if (newStates == null) {
            return;
        }
        if (hsStatePanelList.size() > newStates.size()) { // 应当删除多余的HsStatePanel控件, 这里采用隐藏,而不删除
            for (int i = newStates.size() - 1; i < hsStatePanelList.size(); i++) {
                hsStatePanelList.get(i).setVisible(false); // 隐藏多余的.
            }
        }
        // 首先对相同数量进行修改, 此时 应当遍历原list, 依次调用update
        // 首次因为没有已存在控件, 因此循环次数为0
        for (int i = 0; i < Math.min(hsStatePanelList.size(), newStates.size()); i++) {
            hsStatePanelList.get(i).update(newStates.get(i));
        }

        // 然后对新的更多数据, new HsStatePanel控件加入并显式
        if (newStates.size() <= hsStatePanelList.size()) {
            return; // 此时没有更多状态, 直接返回
        }

        // 此时有更多的状态. (包括第一次情形)
        for (int i = hsStatePanelList.size(); i < newStates.size(); i++) {
            HsStatePanel panel;
            panel = new HsStatePanel(newStates.get(i));
            panel.update(); // 首次
            kernelPanel.add(panel);
            hsStatePanelList.add(panel);
        }

        if (firstUpdate) {
            jScrollPane.setViewportView(kernelPanel); // 占位
            firstUpdate = false;
        }
    }
}
