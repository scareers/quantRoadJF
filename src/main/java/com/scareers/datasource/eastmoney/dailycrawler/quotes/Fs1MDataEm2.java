package com.scareers.datasource.eastmoney.dailycrawler.quotes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
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
 * description: 单日1分钟分时图数据, 使用东财行情页分时图api;
 *
 * @key3 : 数据表存储同版本1, 表名带后缀:  _v2 ;  tableName属性依旧是日期字符串, 只在sql语句+ _v2
 * @noti : 该api与同花顺相同, 从9:30开始, 共计 241条数据, 且带有均价!
 * @key3 "2022-06-02" 及以后,才有数据
 * @author: admin
 */
public class Fs1MDataEm2 extends CrawlerEm {
    public static void main(String[] args) {
        new Fs1MDataEm2().run();
    }

    ThreadPoolExecutor poolExecutor;
    Map<Object, Object> fieldsMap = new HashMap<>();


    /**
     * 可直接指定是否增量更新
     *
     * @param fq       "qfq","hfq","nofq", 默认nofq
     * @param fullMode
     */
    public Fs1MDataEm2() {
        super(DateUtil.today());
        poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("Fs1MDataEm-", null, true));

        fieldsMap.putAll(Dict.create()
                // // 日期       开盘	   收盘	   最高	   最低	  成交量	        成交额	    均价	昨收	  资产代码	资产名称
                .set("日期", "date")
                .set("开盘", "open")
                .set("收盘", "close")
                .set("最高", "high")
                .set("最低", "low")
                .set("成交量", "vol")
                .set("成交额", "amount")
                .set("均价", "avgPrice")
                .set("昨收", "preClose")
                .set("资产代码", "secCode")
                .set("资产名称", "secName")
        );
    }

    @Override
    public void setDb() {
        this.setSaveToFs1MDb();
    }

    /**
     * 构建实际的数据表名
     *
     * @return
     */
    private String getActualTableName() {
        return tableName + "_v2";
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
                        + "avgPrice double  null,"
                        + "preClose double  null,"

                        + "secCode varchar(32)  null,"
                        + "secName varchar(32)  null,"
                        + "quoteId varchar(32)  null," // 比日k线增加! 将作为唯一资产标志

                        + "self_record_time varchar(32) not null,"

                        + "INDEX date_index (date ASC),\n"
                        + "INDEX open_index (open ASC),\n"
                        + "INDEX close_index (close ASC),\n"
                        + "INDEX high_index (high ASC),\n"
                        + "INDEX low_index (low ASC),\n"
                        + "INDEX avgPrice_index (low ASC),\n"
                        + "INDEX secCode_index (secCode ASC),\n"
                        + "INDEX secName_index (secName ASC),\n"
                        + "INDEX quoteId_index (quoteId ASC)\n"
                        + "\n)"
                , getActualTableName());

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
        } else { // 今天交易日
            if (DateUtil.hour(DateUtil.date(), true) <= 5) { // 凌晨6点钟以前, 均抓取上一交易日
                log.warn("今日交易日,但凌晨6点钟以前, 仍然抓取上一交易日");
                tableName = EastMoneyDbApi.getPreNTradeDateStrict(tableName, 1); // 转换为上一交易日
                initSqlCreateTable(); // 并重置建表语句
            } else if (DateUtil.hour(DateUtil.date(), true) < 15) {
                log.error("今日交易日,但尚未收盘,请收盘后运行");
                tableName = EastMoneyDbApi.getPreNTradeDateStrict(tableName, 1); // 转换为上一交易日
                initSqlCreateTable(); // 并重置建表语句
                success = true;
                return;
            }// 其他情况正常抓取
            // >=15点才抓取
        }


        runCore();
        if (success) {
            logSuccess();
        }
        logTimeConsume(timer.interval());

        clear();
    }

    List<SecurityBeanEm> failBeans = new CopyOnWriteArrayList<>(); // 暂存本轮失败的股票, 将重试它们
    HashSet<String> existsQuoteIdSet = new HashSet<>();

    @Override
    protected void runCore() {
        // 1.尝试创建新表
        try {
            execSql(sqlCreateTable, conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2.读取 quoteId 列, group by; 得到所有已经获取过的资产; 后面将会排除掉它们
        String sqlGetAlreadyExistQuoteIds = StrUtil
                .format("select quoteId from `{}` group by quoteId", getActualTableName());
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(conn, sqlGetAlreadyExistQuoteIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (dataFrame != null && dataFrame.length() != 0) {
            List<String> quoteIds = DataFrameS.getColAsStringList(dataFrame, "quoteId");
            existsQuoteIdSet.addAll(quoteIds);
        }

        List<SecurityBeanEm> stockBeans = getBeanList();
        if (stockBeans == null) {
            success = false;
            log.error("stockBeans 获取错误");
            return;
        }
        log.info("Fs1MDataEm2: 应抓取资产总数量:{} ; 已经获取总数量: {}", stockBeans.size(), existsQuoteIdSet.size());

        stockBeans = stockBeans.stream().filter(value -> !existsQuoteIdSet.contains(value.getQuoteId())).collect(
                Collectors.toList());
        log.info("Fs1MDataEm2: 实际应抓取资产总数量:{}", stockBeans.size());

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
                success = true;
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
        success = true;
        logSuccess();
        poolExecutor.shutdownNow();
    }

    private void addTaskForOneBean(List<SecurityBeanEm> stockBeans, AtomicInteger process, SecurityBeanEm beanEm) {
        poolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // 日期	   开盘	   收盘	   最高	   最低	    成交量	成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
                DataFrame<Object> dfTemp = EmQuoteApi
                        .getNewestFs1M(beanEm, 3000, 3);
//                Console.log(dfTemp);
                if (dfTemp == null) {
                    log.error("获取 NewestFs1M 数据失败: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    failBeans.add(beanEm);
                    return;
                }
                if (dfTemp.length() == 0) {
                    log.warn("当日无NewestFs1M数据: {} -- {}", beanEm.getSecCode(), beanEm.getName());
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

                    DataFrameS.toSql(dfTemp, getActualTableName(), conn, "append", sqlCreateTable);
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
//        return getAllBondList();

        List<SecurityBeanEm> allBkList = getAllBkList();
        List<SecurityBeanEm> allIndexList = getAllIndexList();
        List<SecurityBeanEm> allStockList = getAllStockList();
        List<SecurityBeanEm> allBondList = getAllBondList();

        if (allBkList == null || allIndexList == null || allStockList == null) {
            return null;
        }

        allStockList.addAll(allIndexList);
        allStockList.addAll(allBkList);
        allStockList.addAll(allBondList);
        return allStockList;

    }

}

