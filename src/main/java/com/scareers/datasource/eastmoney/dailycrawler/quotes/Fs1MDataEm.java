package com.scareers.datasource.eastmoney.dailycrawler.quotes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.dailycrawler.CrawlerChainEm.waitPoolFinish;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 个股,指数,板块   全部单日的 1分钟分时数据.
 * success标记一律视为成功
 *
 * @noti : 对于单日数据, 均全量更新!
 * @author: admin
 * @date: 2022/3/6/006-15:21:25
 */
public class Fs1MDataEm extends CrawlerEm {


    public static void main(String[] args) {
        new Fs1MDataEm().run();
    }

    ThreadPoolExecutor poolExecutor;
    Map<Object, Object> fieldsMap = new HashMap<>();

    /**
     * 可直接指定是否增量更新
     *
     * @param fq       "qfq","hfq","nofq", 默认nofq
     * @param fullMode
     */
    public Fs1MDataEm() {
        super(DateUtil.today());
        poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("Fs1MDataEm-", null, true));

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

    @Override
    public void setDb() {
        this.setSaveToFs1MDb();
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
                        + "quoteId varchar(32)  null," // 比日k线增加! 将作为唯一资产标志

                        + "self_record_time varchar(32) not null,"

                        + "INDEX date_index (date ASC),\n"
                        + "INDEX open_index (open ASC),\n"
                        + "INDEX close_index (close ASC),\n"
                        + "INDEX high_index (high ASC),\n"
                        + "INDEX low_index (low ASC),\n"
                        + "INDEX amplitude_index (amplitude ASC),\n"
                        + "INDEX chgPct_index (chgPct ASC),\n"
                        + "INDEX turnover_index (turnover ASC),\n"
                        + "INDEX secCode_index (secCode ASC),\n"
                        + "INDEX secName_index (secName ASC),\n"
                        + "INDEX quoteId_index (quoteId ASC)\n"
                        + "\n)"
                , tableName);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @SneakyThrows
    @Override
    public void run() {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        logStart();
        if (!EastMoneyDbApi.isTradeDate(tableName)) {
            log.warn("今日非交易日,应当尝试抓取上一交易日数据");

            tableName = EastMoneyDbApi.getPreNTradeDateStrict(tableName, 1); // 转换为上一交易日
            initSqlCreateTable(); // 并重置建表语句
        }

        try { // 同样尝试建表
            execSql(sqlCreateTable, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        runCore();
        if (success) {
            logSuccess();
        }
        logTimeConsume(timer.interval());

        clear();
    }

    List<SecurityBeanEm> failBeans = new CopyOnWriteArrayList<>(); // 暂存本轮失败的股票, 将重试它们
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
            addTaskForOneBean(stockBeans, process, beanEm);
        }

        waitPoolFinish(poolExecutor);


        int failRetryEpoch = 1;
        // 处理失败队列:
        while (failBeans.size() > 0) {
            if (failRetryEpoch > 5) {
                log.error("分时1m失败队列重试超过5次, 视为失败!");
                log.error("最终失败的股票队列: \n");
                log.error(failBeans.stream().map(SecurityBeanEm::getQuoteId).collect(Collectors.toList()).toString());
                success=true;
                poolExecutor.shutdownNow();
                return;
            }

            log.error("分时1m获取失败队列不为空, 重新获取中; 重试轮次: {}", failRetryEpoch);
            failRetryEpoch++;

            ArrayList<SecurityBeanEm> beanEms = new ArrayList<>(failBeans);
            failBeans.clear(); // 清空

            for (SecurityBeanEm beanEm : beanEms) {
                addTaskForOneBean(stockBeans, process, beanEm);
            }
            waitPoolFinish(poolExecutor);
        }
        success=true;
        logSuccess();
        poolExecutor.shutdownNow();
    }

    private void addTaskForOneBean(List<SecurityBeanEm> stockBeans, AtomicInteger process, SecurityBeanEm beanEm) {
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
                DataFrame<Object> dfTemp = EmQuoteApi
                        .getFs1MToday(beanEm, 3, 4000);
                if (dfTemp == null) {
                    log.error("获取 fs1m 数据失败: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    failBeans.add(beanEm);
                    return;
                }
                if (dfTemp.length() == 0) {
                    log.warn("当日无fs1m数据: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    return;
                }

                //Assert.isTrue(dfTemp.length() == 240);

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

                dfTemp = dfTemp.rename(fieldsMap);
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
//        return allBkList;
    }

}

