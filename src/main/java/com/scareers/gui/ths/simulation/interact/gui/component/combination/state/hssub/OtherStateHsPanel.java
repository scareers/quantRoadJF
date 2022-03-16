package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.other.OtherStateHs;

import javax.swing.*;

/**
 * description: 高卖其他状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class OtherStateHsPanel extends DisplayPanel {
    OtherStateHs otherStateHs;
    OtherStateHs preOtherStateHs;

    public OtherStateHsPanel(OtherStateHs otherStateHs,
                             OtherStateHs preOtherStateHs) {
        this.otherStateHs = otherStateHs;
        this.preOtherStateHs = preOtherStateHs;
        this.add(new JLabel("其他状态"));
    }

    @Override
    public void update() {

    }

    public void update(OtherStateHs otherStateHs) {
        this.otherStateHs = otherStateHs;
        this.update();
    }
}
