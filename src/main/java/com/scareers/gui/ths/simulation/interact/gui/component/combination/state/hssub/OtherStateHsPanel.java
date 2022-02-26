package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.other.OtherStateHs;

/**
 * description: 高卖其他状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class OtherStateHsPanel extends DisplayPanel {
    OtherStateHs otherStateHs;

    @Override
    protected void update() {

    }

    public void update(OtherStateHs otherStateHs) {
        this.otherStateHs = otherStateHs;
        this.update();
    }
}
