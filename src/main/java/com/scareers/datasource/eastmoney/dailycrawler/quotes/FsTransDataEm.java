package com.scareers.datasource.eastmoney.dailycrawler.quotes;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.MysqlApi;
import joinery.DataFrame;
import lombok.SneakyThrows;
import org.jsoup.select.Collector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
 * --> 抓取时间:
 * 1.非交易日, 抓取上一交易日数据,放入上一交易日命名的数据表
 * 2.交易日:6点钟以前, 抓取上一交易日数据,放入上一交易日数据表; 6-15点, 禁止抓取,success=false; 15点后, 抓取今日放入今日数据表
 * --> 抓取机制
 * 根据决定好的数据表名, 访问已存在的 quoteId 列表, 存在的quoteId列表视为已经成功抓取, 不再抓取; -- 自动检测增量更新
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
        String sqlGetAlreadyExistQuoteIds = StrUtil.format("select quoteId from `{}` group by quoteId", tableName);
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
        log.info("FsTransDataEm: 应抓取资产总数量:{} ; 已经获取总数量: {}", stockBeans.size(), existsQuoteIdSet.size());

        stockBeans = stockBeans.stream().filter(value -> !existsQuoteIdSet.contains(value.getQuoteId())).collect(
                Collectors.toList());
        log.info("FsTransDataEm: 实际应抓取资产总数量:{}", stockBeans.size());

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
                log.error("分时成交失败队列重试超过5次, 视为失败!");
                log.error("最终失败的股票队列: \n");
                log.error(failBeans.stream().map(SecurityBeanEm::getQuoteId).collect(Collectors.toList()).toString());
                success = true;
                poolExecutor.shutdownNow();
                return;
            }

            log.error("分时成交获取失败队列不为空, 重新获取中; 重试轮次: {}", failRetryEpoch);
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
        poolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                DataFrame<Object> dfTemp = EmQuoteApi.getFSTransaction(10000, beanEm, 3, 5000);
                if (dfTemp == null) {
                    log.error("放入失败队列: 获取 fstrans 数据失败: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    failBeans.add(beanEm);
                    return;
                }

                if (dfTemp.length() == 0) {
                    log.warn("当日无fstrans数据: {} -- {}", beanEm.getSecCode(), beanEm.getName());
                    return;
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
    }

}

