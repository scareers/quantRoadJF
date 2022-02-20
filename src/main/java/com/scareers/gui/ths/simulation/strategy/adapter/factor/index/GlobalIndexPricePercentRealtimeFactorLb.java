package com.scareers.gui.ths.simulation.strategy.adapter.factor.index;

import com.scareers.gui.ths.simulation.strategy.adapter.factor.LbFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.LbState;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

import static com.scareers.gui.ths.simulation.strategy.adapter.factor.index.SettingsOfIndexPercentFactor.enhanceRateForLbDefault;

/**
 * description: 大盘指数影响
 *
 * @author: admin
 * @date: 2022/2/20/020-17:27:23
 */
@Setter
@Getter
public class GlobalIndexPricePercentRealtimeFactorLb extends LbFactor {
    private Double enhanceRate = enhanceRateForLbDefault;

    public GlobalIndexPricePercentRealtimeFactorLb() {
        super(SettingsOfIndexPercentFactor.factorName, SettingsOfIndexPercentFactor.nameCn,
                SettingsOfIndexPercentFactor.description);
    }

    @Override
    public LbState influence(LbState state) {
        Objects.requireNonNull(state, "初始状态不可为null, 设置后方可调用influence进行影响");
        Double changePercent = getIndexPercent(state); // 变化百分比
        if (changePercent == null) { // 数据缺失
            log.error("GlobalIndexPricePercentRealtimeFactor: 指数涨跌幅获取失败, 无法计算影响, 返回原始状态");
            return state;
        }
        state.movePdf(changePercent); // 高卖低卖, 均是平移pdf
        return state;
    }

    private Double getIndexPercent(LbState state) {
        return SettingsOfIndexPercentFactor.getIndexPercent(state.getBean());
    }
}
