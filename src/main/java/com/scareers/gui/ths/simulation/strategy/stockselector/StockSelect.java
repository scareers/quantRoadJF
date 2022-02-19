package com.scareers.gui.ths.simulation.strategy.stockselector;

import java.util.List;

/**
 * 股票选择器接口.
 * 各个 股票选择器 均需要实现此接口
 * 核心方法返回 股票代码列表; // 股票名称也可.
 * 这里不限制返回 东财资产bean, 使得更加灵活
 */
public interface StockSelect {
    void selectStock() throws Exception;

    List<String> getSelectResults() throws Exception;
}
