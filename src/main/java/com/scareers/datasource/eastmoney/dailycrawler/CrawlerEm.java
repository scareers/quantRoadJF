package com.scareers.datasource.eastmoney.dailycrawler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.Log;
import com.scareers.datasource.Crawler;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 爬虫基类
 *
 * @author: admin
 * @date: 2022/3/5/005-09:57:59
 */
@Setter
@Getter
public abstract class CrawlerEm extends Crawler {
    protected static final Log log = LogUtil.getLogger();
    protected String tableName;
    protected Connection conn;
    protected String sqlCreateTable;
    protected boolean success = false; // 标志是否运行成功!, 一般run成功后需要设定 success=true

    public CrawlerEm(String tableName) {
        this.tableName = tableName;
        setDb();
        initSqlCreateTable();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void run() {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        logStart();
        try {
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

    /**
     * 可重写clear方法
     */
    protected void clear() {
    }

    protected abstract void runCore();

    protected abstract void setDb(); // 设置数据库, 调用 以下方法设置

    protected abstract void initSqlCreateTable(); // 初始化建表语句


    /*
    设置保存数据库 conn对象
     */

    /**
     * 东财主数据库 -- eastmoney
     */
    protected void setSaveToMainDb() {
        conn = ConnectionFactory.getConnLocalEastmoney();
    }

    /**
     * 东财分时成交
     */
    protected void setSaveToFsTransDb() {
        conn = ConnectionFactory.getConnLocalFSTransactionFromEastmoney();
    }

    /**
     * 东财分时1M
     */
    protected void setSaveToFs1MDb() {
        conn = ConnectionFactory.getConnLocalFS1MFromEastmoney();
    }


    /*
    常用log方法
     */
    protected void logApiError(String apiName) {
        log.error("{}: api调用失败 -- {} ", this.toString(), apiName);
    }

    protected void logSaveError() {
        log.error("{}: 保存结果到数据库失败! ", this.toString());
    }

    protected void logSuccess() {
        log.warn("{}: run success! ", this.toString());
    }

    protected void logStart() {
        log.warn("{}: start... ", this.toString());
    }

    protected void logTimeConsume(long time) {
        log.warn("{}: time consume: {} s ", this.toString(), time / 1000.0);
    }

    /*
     * 其他常用方法
     */

    /**
     * 大多数表 均添加  self_record_time 字段, 表示记录的时间
     *
     * @return
     */
    protected String getRecordTime() {
        return DateUtil.now();
    }

    /**
     * 获取所有股票bean; 可传递slice, 截取部分, 以debug. 常态应 Integer.max_value
     *
     * @param amount
     * @return
     */
    protected List<SecurityBeanEm> getBkBeanList(int amount) {
        DataFrame<Object> bkListDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("所有板块"));
        List<String> bkCodes = DataFrameS.getColAsStringList(bkListDf, "资产代码");
        List<SecurityBeanEm> bkListBeans;
        try {
            bkListBeans = SecurityBeanEm.createBKList(bkCodes.subList(0, Math.min(amount, bkCodes.size())));
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createBKList");
            success = false;
            return null;
        }
        return bkListBeans;
    }

    protected List<SecurityBeanEm> getAllBkList() {
        return getBkBeanList(Integer.MAX_VALUE);
    }

    protected List<SecurityBeanEm> getStockBeanList(int amount) {
        DataFrame<Object> stockListDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("沪深京A股"));
        List<String> stockCodes = DataFrameS.getColAsStringList(stockListDf, "资产代码");
        List<SecurityBeanEm> stockBeans;
        try {
            stockBeans = SecurityBeanEm.createStockList(stockCodes.subList(0, Math.min(amount, stockCodes.size())),
                    true);
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createStockList");
            success = false;
            return null;
        }
        return stockBeans;
    }

    protected List<SecurityBeanEm> getAllStockList() {
        return getStockBeanList(Integer.MAX_VALUE);
    }

    protected List<SecurityBeanEm> getIndexBeanList(int amount) {
        DataFrame<Object> indexListDF = EmQuoteApi.getRealtimeQuotes(Arrays.asList("沪深系列指数"));
        DataFrame<Object> indexListDF2 = EmQuoteApi.getRealtimeQuotes(Arrays.asList("中证系列指数"));
        indexListDF = indexListDF.concat(indexListDF2);
        List<String> indexCodes = DataFrameS.getColAsStringList(indexListDF, "资产代码");

        List<SecurityBeanEm> indexBeans;
        try {
            indexBeans = SecurityBeanEm.createIndexList(indexCodes.subList(0, Math.min(amount, indexCodes.size())));
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createBKList");
            success = false;
            return null;
        }
        return indexBeans;
    }

    protected List<SecurityBeanEm> getAllIndexList() {
        return getIndexBeanList(Integer.MAX_VALUE);
    }




}
