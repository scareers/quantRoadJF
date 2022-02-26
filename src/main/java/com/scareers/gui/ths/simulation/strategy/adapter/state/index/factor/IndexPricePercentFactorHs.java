package com.scareers.gui.ths.simulation.strategy.adapter.state.index.factor;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.index.IndexStateHs;
import lombok.Getter;
import lombok.Setter;


/**
 * description: 大盘指数影响
 *
 * @author: admin
 * @date: 2022/2/20/020-17:27:23
 */
@Setter
@Getter
public class IndexPricePercentFactorHs extends HsFactor {
    public IndexPricePercentFactorHs() {
        super(SettingsOfIndexPercentFactor.factorNameHs, SettingsOfIndexPercentFactor.nameCnHs,
                SettingsOfIndexPercentFactor.descriptionHs);
    }

    @Override
    public HsState influence(HsState state0) {
        IndexStateHs state = state0.getIndexStateHs();
        Double indexPricePercentThatTime = state.getIndexPriceChgPtCurrent();
        if (indexPricePercentThatTime == null) { // 数据缺失
            log.error("IndexPricePercentFactorHs: 指数涨跌幅获取失败, 无法计算影响, 返回原始状态");
            return state0; // 相当于不移动
        }
        // 平移pdf
        state0.getStockStateHs().movePdf(decideMoveDistanceByIndexChangePercent(indexPricePercentThatTime));
        return state0;
    }

    /**
     * @param indexPricePercentThatTime
     * @return
     * @key3 移动量算法!
     */
    private Double decideMoveDistanceByIndexChangePercent(Double indexPricePercentThatTime) {
        if (indexPricePercentThatTime == null) {
            return 0.0;
        }
        double abs = Math.abs(indexPricePercentThatTime);
        if (abs > 0.05) {
            return -0.11; // 无条件全仓止损
        } else if (abs > 0.02) {
            return indexPricePercentThatTime * 3; // 3倍增幅
        } else if (abs > 0.01) {
            return indexPricePercentThatTime * 2; // 2倍增幅
        } else {
            return indexPricePercentThatTime;
        }
    }


}
