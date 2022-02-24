package com.scareers.gui.ths.simulation.strategy.adapter.factor.base;

import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.gui.ths.simulation.strategy.adapter.LowBuyHighSellStrategyAdapter;
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
        state.setPre2ClosePrice(
                SettingsOfBaseDataFactor.getPreNDayClosePriceQfq(state.getStockCode(), state.getPre2TradeDate()));
        state.setPreClosePrice(
                SettingsOfBaseDataFactor.getPreNDayClosePriceQfq(state.getStockCode(), state.getPreTradeDate()));
        state.setFsData(FsFetcher.getFsData(state.getBean()));
        state.setFsTransData(FsTransactionFetcher.getFsTransData(state.getBean()));
        state.setNewPriceTrans(FsTransactionFetcher.getNewestPrice(state.getBean()));
        if (state.getPre2ClosePrice() != null && state.getNewPriceTrans() != null) { // 需要前2收盘和当前最新成交两个价格不为null
            state.setNewPricePercentToPre2Close(state.getNewPriceTrans() / state.getPre2ClosePrice() - 1);
        }
        state.setAmountsTotalYc(LowBuyHighSellStrategyAdapter.yesterdayStockHoldsBeSellMap.get(state.getStockCode()));
        state.setActualAmountHighSelled(
                LowBuyHighSellStrategyAdapter.actualAmountHighSelledMap.get(state.getStockCode()));
        state.setAvailableAmountForHs(LowBuyHighSellStrategyAdapter.availableAmountForHsMap.get(state.getStockCode()));

        return state;
    }


}
