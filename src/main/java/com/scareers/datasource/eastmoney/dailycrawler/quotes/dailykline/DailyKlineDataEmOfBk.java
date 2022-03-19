package com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.sqlapi.MysqlApi;

import java.sql.SQLException;
import java.util.List;

/**
 * description: 个股日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineDataEmOfBk extends DailyKlineDataEm {
    private DailyKlineDataEmOfBk(String tablePrefix, String fq, boolean fullMode) {
        super(tablePrefix, fq, fullMode);
    }

    public DailyKlineDataEmOfBk(boolean fullMode) {
        super("bk_kline_daily", "", fullMode); // 会使用 nofq 默认
    }

    public static void main(String[] args) throws SQLException {
//        new DailyKlineDataEmOfBk(true).run();
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

