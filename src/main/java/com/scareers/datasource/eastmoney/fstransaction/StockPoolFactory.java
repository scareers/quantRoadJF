package com.scareers.datasource.eastmoney.fstransaction;

import java.util.List;
import java.util.Set;

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
    default List<StockBean> createStockPool() throws Exception {
        return null;
    }
}
