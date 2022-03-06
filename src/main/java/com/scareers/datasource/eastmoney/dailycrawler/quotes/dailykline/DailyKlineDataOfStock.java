package com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.List;

/**
 * description: 个股日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineDataOfStock extends DailyKlineData {
    private DailyKlineDataOfStock(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
    }

    public DailyKlineDataOfStock(String fq, boolean fullMode) {
        super("stock_kline_daily__", fq, fullMode);
    }

    public static void main(String[] args) {
        new DailyKlineDataOfStock("nofq", true).run();
    }


    @Override
    protected List<SecurityBeanEm> getBeanList() {
//        return getStockBeanList(20);
        return getAllStockList();
    }

}

