package com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline;

import com.scareers.datasource.eastmoney.SecurityBeanEm;

import java.util.List;

/**
 * description: 指数日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineDataEmOfIndex extends DailyKlineDataEm {
    private DailyKlineDataEmOfIndex(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
        this.hasAlreadyIncrementalUpdatedThreshold=100000000;
    }

    public DailyKlineDataEmOfIndex(boolean fullMode) {
        super("index_kline_daily", "", fullMode); // 会使用 nofq 默认
        this.earlyDateStr = null; // null则全部
    }

    public static void main(String[] args) {
        new DailyKlineDataEmOfIndex(false).run();
    }


    @Override
    protected List<SecurityBeanEm> getBeanList() {
        return getAllIndexList();
//        return getIndexBeanList(20);
    }

}

