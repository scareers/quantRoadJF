package com.scareers.datasource.eastmoney.dailycrawler.quotes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.datasource.eastmoney.dailycrawler.CrawlerChainEm.waitPoolFinish;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 个股,指数,板块   全部单日的 1分钟分时数据.
 *
 * @noti : 对于单日数据, 均全量更新!
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class FsTransDataEm extends CrawlerEm {
    public static void main(String[] args) {
        new FsTransDataEm().run();
    }

    ThreadPoolExecutor poolExecutor;

    /**
     * 可直接指定是否增量更新
     *
     * @param fq       "qfq","hfq","nofq", 默认nofq
     * @param fullMode
     */
    public FsTransDataEm() {
        super(DateUtil.today());
        poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("FsTransDataEm-", null, true));
    }

    @Override
    public void setDb() {
        this.setSaveToFsTransDb();
    }

    @Override
    protected void initSqlCreateTable() {
        // "sec_code", "market", "time_tick", "price", "vol", "bs"
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "sec_code varchar(32) not null,"
                        + "market int  null,"
                        + "time_tick varchar(32) not null,"
                        + "price double  null,"
                        + "vol double  null,"
                        + "bs varchar(8)  null,"

                        + "secName varchar(32)  null,"
                        + "quoteId varchar(32)  null," // 比日k线增加! 将作为唯一资产标志
                        + "self_record_time varchar(32) not null,"


                        + "INDEX time_tick_index (time_tick ASC),\n"
                        + "INDEX price_index (price ASC),\n"
                        + "INDEX vol_index (vol ASC),\n"
                        + "INDEX bs_index (bs ASC),\n"
                        + "INDEX secCode_index (sec_code ASC),\n"
                        + "INDEX secName_index (secName ASC),\n"
                        + "INDEX quoteId_index (quoteId ASC)\n"
                        + "\n)"
                , tableName);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void runCore() {
        try { // 全量更新模式, 将删除原表
            execSql(StrUtil.format("drop table if exists `{}`", tableName), conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try { // 创建新表
            execSql(sqlCreateTable, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<SecurityBeanEm> stockBeans = getBeanList();
        if (stockBeans == null) {
            return;
        }

        success = true;
        AtomicInteger process = new AtomicInteger(1);
        for (SecurityBeanEm beanEm : stockBeans) {
            poolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    DataFrame<Object> dfTemp = EmQuoteApi.getFSTransaction(10000, beanEm, 3, 5000);
                    if (dfTemp == null) {
                        log.error("获取 fstrans 数据失败: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                        success = false;
                        return;
                    }
                    if (dfTemp.length() == 0) {
                        log.warn("当日无fstrans数据: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    }

                    List<Object> names = new ArrayList<>();
                    for (int i = 0; i < dfTemp.length(); i++) {
                        names.add(beanEm.getName());
                    }
                    dfTemp.add("secName", names);
                    List<Object> quoteIds = new ArrayList<>();
                    for (int i = 0; i < dfTemp.length(); i++) {
                        quoteIds.add(beanEm.getQuoteId());
                    }
                    dfTemp.add("quoteId", quoteIds);
                    List<Object> recordTimes = new ArrayList<>();
                    for (int i = 0; i < dfTemp.length(); i++) {
                        recordTimes.add(getRecordTime());
                    }
                    dfTemp.add("self_record_time", recordTimes);

                    try {
                        DataFrameS.toSql(dfTemp, tableName, conn, "append", sqlCreateTable);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logSaveError();
                        success = false;
                        return;
                    }
                    log.info("success: {} -- {} -- {}/{}", beanEm.getSecCode(), beanEm.getName(), process,
                            stockBeans.size());
                    process.incrementAndGet();
                }
            });
        }

        waitPoolFinish(poolExecutor);
        logSuccess();
        poolExecutor.shutdownNow();
    }

    protected List<SecurityBeanEm> getBeanList() {
        List<SecurityBeanEm> allBkList = getAllBkList();
        List<SecurityBeanEm> allIndexList = getAllIndexList();
        List<SecurityBeanEm> allStockList = getAllStockList();

        if (allBkList == null || allIndexList == null || allStockList == null) {
            return null;
        }

        allStockList.addAll(allIndexList);
        allStockList.addAll(allBkList);

        return allStockList;
//        return getStockBeanList(10);
    }

}

