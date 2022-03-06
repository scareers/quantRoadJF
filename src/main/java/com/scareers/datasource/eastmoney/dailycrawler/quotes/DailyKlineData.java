package com.scareers.datasource.eastmoney.dailycrawler.quotes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.Crawler;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.Tqdm;
import com.sun.jna.platform.win32.WinNT;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static com.scareers.datasource.eastmoney.dailycrawler.CrawlerChain.waitPoolFinish;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 日 k线行情, fullMode == true为全量更新, 否则增量更新, 抓取单日
 * 采用全量更新
 *
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class DailyKlineData extends Crawler {
    public static void main(String[] args) {
        new DailyKlineData("qfq", true).run();
    }

    String fq;
    boolean fullMode;
    ThreadPoolExecutor poolExecutor;
    Map<Object, Object> fieldsMap = new HashMap<>();

    /**
     * 可直接指定是否增量更新
     *
     * @param fq
     * @param fullMode
     */
    public DailyKlineData(String fq, boolean fullMode) {
        super(StrUtil.format("stock_kline_daily_{}", fq));

        this.fq = fq;
        this.fullMode = fullMode;
        poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("EM.EmQuoteApi-", null, true));

        fieldsMap.putAll(Dict.create()
                // // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
                .set("日期", "date")
                .set("开盘", "open")
                .set("收盘", "close")
                .set("最高", "high")
                .set("最低", "low")
                .set("成交量", "vol")
                .set("成交额", "amount")
                .set("振幅", "amplitude")
                .set("涨跌幅", "chgPct")
                .set("涨跌额", "chaVal")
                .set("换手率", "turnover")
                .set("资产代码", "secCode")
                .set("资产名称", "secName")
        );
    }

    public DailyKlineData(String fq) {
        this(fq, true);
        if (DateUtil.date().isWeekend()) {
            this.fullMode = true;
        } else {
            this.fullMode = false;
        }
    }


    @Override
    public void setDb() {
        this.setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        // @noti: 查询结果的 id 字段, 替换为 idRaw 列
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "date varchar(32) not null,"
                        + "open double  null,"
                        + "close double  null,"
                        + "high double  null,"
                        + "low double  null,"
                        + "vol double  null,"
                        + "amount double  null,"
                        + "amplitude double  null," // 振幅
                        + "chgPct double  null,"
                        + "chaVal double  null,"
                        + "turnover double  null,"
                        + "secCode varchar(32)  null,"
                        + "secName varchar(32)  null,"

                        + "self_record_time varchar(32) not null"
                        + "\n)"
                , tableName);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + fq + "]";
    }

    @Override
    protected void runCore() {
        /**
         * 全量更新模式: 将删除原表
         */
        String begDate = null; // 全量更新模式, 两个均为null
        String endDate = null;
        if (fullMode) {
            log.warn("日k线数据: 全量更新");
            try { // 全量更新模式, 将删除原表
                execSql(StrUtil.format("drop table if exists `{}`", tableName), conn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // 增量更新模式, 将保留原表, 但日期为今日
            log.warn("日k线数据: 增量更新");
            begDate = DateUtil.today();
            endDate = DateUtil.today();
        }

        try {
            execSql(sqlCreateTable, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }


        DataFrame<Object> stockListDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("沪深京A股"));
        List<String> stockCodes = DataFrameS.getColAsStringList(stockListDf, "资产代码");
        List<SecurityBeanEm> stockBeans;
        try {
            stockBeans = SecurityBeanEm.createStockList(stockCodes.subList(0, 20), true);
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createStockList");
            success = true;
            return;
        }

        success = true;
        for (SecurityBeanEm beanEm : stockBeans) {
            String finalBegDate = begDate;
            String finalEndDate = endDate;

            poolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
                    DataFrame<Object> dfTemp = EmQuoteApi
                            .getQuoteHistorySingle(false, beanEm, finalBegDate, finalEndDate, "101", fq, 3, 4000);
                    if (dfTemp == null) {
                        log.error("获取日k线数据失败: {}", beanEm.getSecCode());
                        success = false;
                        return;
                    }

                    if (dfTemp.length() == 0) {

                        log.warn("当日无日k线数据", beanEm.getSecCode());
                    }

                    List<Object> recordTimes = new ArrayList<>();
                    for (int i = 0; i < dfTemp.length(); i++) {
                        recordTimes.add(getRecordTime());
                    }
                    dfTemp.add("self_record_time", recordTimes);
                    dfTemp = dfTemp.rename(fieldsMap);
                    try {
                        DataFrameS.toSql(dfTemp, tableName, conn, "append", sqlCreateTable);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logSaveError();
                        success = false;
                        return;
                    }
                    log.info("success: {}", beanEm.getSecCode());
                }
            });
        }

        waitPoolFinish(poolExecutor);
        logSuccess();
    }


//    public static String sqlTableCreateTemplate = "create table if not exists {}";


}

