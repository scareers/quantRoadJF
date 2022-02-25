package com.scareers.gui.ths.simulation.strategy.adapter.state.sub;

import java.io.Serializable;

/**
 * description: 大盘指数状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:15
 */
public class IndexStateHs implements Serializable {
    private static final long serialVersionUID = 7822512015L;

    protected Double indexPricePercentThatTime=0.0; // 对应大盘指数涨跌幅当前
}
