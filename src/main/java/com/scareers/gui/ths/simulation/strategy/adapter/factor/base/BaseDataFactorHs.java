package com.scareers.gui.ths.simulation.strategy.adapter.factor.base;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getQuoteHistorySingle;

/**
 * description: 基本数据项因子, 该因子逻辑上初始设置 HsState 的基本数据项. 并且基本不对其他属性进行修改, 例如分布状况
 *
 * @author: admin
 * @date: 2022/2/21/021-19:09:55
 */
public class BaseDataFactorHs extends HsFactor {
    public BaseDataFactorHs() {
        super(SettingsOfBaseDataFactor.factorName, SettingsOfBaseDataFactor.nameCn,
                SettingsOfBaseDataFactor.description);
    }

    @Override
    public HsState influence(HsState state) {
        state.setStockCode(state.getBean().getSecCode());
        state.setPre2ClosePrice(
                SettingsOfBaseDataFactor.getPre2DayClosePriceQfq(state.getStockCode(), state.getPre2TradeDate()));

        return state;
    }


}
