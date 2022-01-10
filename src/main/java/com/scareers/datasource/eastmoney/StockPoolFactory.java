package com.scareers.datasource.eastmoney;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-18:06:54
 */
public interface StockPoolFactory {
    /**
     * @return 生成股票池, 股票代码列表
     */
    default List<SecurityBeanEm> createStockPool() throws Exception {
        return null;
    }
}
