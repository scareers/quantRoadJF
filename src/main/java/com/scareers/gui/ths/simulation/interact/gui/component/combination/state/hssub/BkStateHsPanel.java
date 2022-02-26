package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.bk.BkStateHs;

/**
 * description: 高卖板块状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class BkStateHsPanel extends DisplayPanel {
    BkStateHs bkStateHs;

    @Override
    protected void update() {

    }

    public void update(BkStateHs bkStateHs) {
        this.bkStateHs = bkStateHs;
        this.update();
    }

}
