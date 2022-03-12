package com.scareers.gui.ths.simulation.strategy.bond;

import com.scareers.gui.ths.simulation.strategy.Strategy;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/3/003-07:34:51
 */
public class BondStrategy extends Strategy {

    @Override
    protected void initSecurityPool() throws Exception {

    }

    @Override
    protected List<String> getSecurityTodaySelect() throws Exception {
        return null;
    }

    @Override
    protected List<String> initYesterdayHolds() throws Exception {
        return null;
    }

    @Override
    public void saveAllConfig() {

    }
}
