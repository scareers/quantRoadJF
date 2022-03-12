package com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.MysqlApi;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * description: 个股日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineDataOfBk extends DailyKlineData {
    private DailyKlineDataOfBk(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
    }

    public DailyKlineDataOfBk(boolean fullMode) {
        super("bk_kline_daily", "", fullMode); // 会使用 nofq 默认
    }

    public static void main(String[] args) throws SQLException {
//        new DailyKlineDataOfBk(true).run();
        Console.log(MysqlApi.getDiskUsageOfDB("eastmoney_fs_transaction",
                ConnectionFactory.getConnLocalFSTransactionFromEastmoney()));
        Console.log(MysqlApi.getDiskUsageOfDB("eastmoney_fs1m",
                ConnectionFactory.getConnLocalFS1MFromEastmoney()));
        Console.log(MysqlApi.getDiskUsageOfDB("eastmoney",
                ConnectionFactory.getConnLocalEastmoney()));
    }


    @Override
    protected List<SecurityBeanEm> getBeanList() {
//        return getBkBeanList(20);
        return getAllBkList();
    }

}

