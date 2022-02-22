package com.scareers.gui.ths.simulation.interact.gui.component.combination.state;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 给定资产bean, 展示其当前 HsState列表
 *
 * @author: admin
 * @date: 2022/2/22/022-17:21:15
 */
public class HsStateListPanel extends SecurityDisplayPanel {
    List<HsStatePanel> hsStatePanelList = new ArrayList<>(); // 复用控件

    public HsStateListPanel() {
        this.setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP)); // 自定义上下浮动的布局
    }


    @Override
    protected void update() {
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
            if (i == 0) {
                hsStatePanelList.get(i).update(newStates.get(i), null); // 第一个状态对象, preState为null
            } else {
                hsStatePanelList.get(i).update(newStates.get(i), newStates.get(i - 1));
            }
        }

        // 然后对新的更多数据, new HsStatePanel控件加入并显式
        if (newStates.size() <= hsStatePanelList.size()) {
            return; // 此时没有更多状态, 直接返回
        }

        // 此时有更多的状态. (包括第一次情形)
        for (int i = hsStatePanelList.size(); i < newStates.size(); i++) {
            HsStatePanel panel;
            if (i == 0) {
                panel = new HsStatePanel(newStates.get(i), null);
            } else {
                panel = new HsStatePanel(newStates.get(i), newStates.get(i - 1));
            }
            panel.update(); // 首次
            this.add(panel);
            hsStatePanelList.add(panel);
        }


    }
}
