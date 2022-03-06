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
public class DailyKlineDataOfIndex extends DailyKlineData {
    private DailyKlineDataOfIndex(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
    }

    public DailyKlineDataOfIndex(boolean fullMode) {
        super("index_kline_daily", "", fullMode); // 会使用 nofq 默认
    }

    public static void main(String[] args) {
        new DailyKlineDataOfIndex(true).run();
    }


    @Override
    protected List<SecurityBeanEm> getBeanList() {
        return getAllIndexList();
//        return getIndexBeanList(20);
    }

}

