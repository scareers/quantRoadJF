package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.bk.BkStateHs;

import javax.swing.*;

/**
 * description: 高卖板块状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class BkStateHsPanel extends DisplayPanel {
    BkStateHs bkStateHs;
    BkStateHs preBkStateHs;

    @Override
    protected void update() {

    }

    public BkStateHsPanel(BkStateHs bkStateHs,
                          BkStateHs preBkStateHs) {
        this.bkStateHs = bkStateHs;
        this.preBkStateHs = preBkStateHs;

        this.add(new JLabel("板块信息"));
    }

    public void update(BkStateHs bkStateHs) {
        this.bkStateHs = bkStateHs;
        this.update();
    }

}
