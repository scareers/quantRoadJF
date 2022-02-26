package com.scareers.gui.ths.simulation.strategy.adapter.state.index;

import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * description: 大盘指数状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:15
 */
@Data
public class IndexStateHs implements Serializable {
    private static final long serialVersionUID = 7822512015L;
    private transient HsState parent;
    protected Double indexPricePercentThatTime=0.0; // 对应大盘指数涨跌幅当前
}
