package com.scareers.gui.ths.simulation.interact.gui.component.combination.state.hssub;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.index.IndexStateHs;

import javax.swing.*;

import static com.scareers.gui.ths.simulation.interact.gui.component.combination.state.HsStatePanel.getDefaultJLabel;


/**
 * description: 高卖指数状态展示
 *
 * @author: admin
 * @date: 2022/2/26/026-20:53:36
 */
public class IndexStateHsPanel extends DisplayPanel {
    IndexStateHs indexStateHs;
    IndexStateHs preIndexStateHs;

    JLabel indexPercentLabel = getDefaultJLabel("对应大盘当前涨跌幅");
    JLabel indexPercentValueLabel = getDefaultJLabel();


    public IndexStateHsPanel(IndexStateHs indexStateHs,
                             IndexStateHs preIndexStateHs) {
        this.indexStateHs = indexStateHs;
        this.preIndexStateHs = preIndexStateHs;
        this.add(indexPercentLabel);
    }

    @Override
    protected void update() {

    }

    // 放入大盘指数状态

    public void update(IndexStateHs indexStateHs, IndexStateHs preIndexStateHs) {
        this.indexStateHs = indexStateHs;
        this.preIndexStateHs = preIndexStateHs;
        this.update();
    }


}
