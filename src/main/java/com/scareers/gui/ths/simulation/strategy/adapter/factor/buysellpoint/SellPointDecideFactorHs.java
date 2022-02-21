package com.scareers.gui.ths.simulation.strategy.adapter.factor.buysellpoint;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;

/**
 * description: 高卖时, 卖点判定的因子算法. 将读取各种状态, 判定是否卖点, 设置state的 sellPointCurrent 属性
 *
 * @author: admin
 * @date: 2022/2/21/021-19:24:10
 */
public class SellPointDecideFactorHs extends HsFactor {
    public SellPointDecideFactorHs() {
        super(SettingsOfBuySellPointFactor.factorNameHs, SettingsOfBuySellPointFactor.nameCnHs,
                SettingsOfBuySellPointFactor.descriptionHs);
    }

    @Override
    public HsState influence(HsState state) {
        return null;
    }
}
