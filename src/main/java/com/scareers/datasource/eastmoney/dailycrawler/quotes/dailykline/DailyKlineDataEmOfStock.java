package com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline;

import com.scareers.datasource.eastmoney.SecurityBeanEm;

import java.util.List;

/**
 * description: 个股日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineDataEmOfStock extends DailyKlineDataEm {
    private DailyKlineDataEmOfStock(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
        this.hasAlreadyIncrementalUpdatedThreshold = 4000;
    }

    public DailyKlineDataEmOfStock(String fq, boolean fullMode) {
        super("stock_kline_daily__", fq, fullMode);
    }

    public static void main(String[] args) {
        new DailyKlineDataEmOfStock("nofq", true).run();
    }


    @Override
    protected List<SecurityBeanEm> getBeanList() {
//        return getStockBeanList(20);
        return getAllStockList();
    }

}

