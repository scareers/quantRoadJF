package com.scareers.datasource.eastmoney.dailycrawler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.Log;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;

/**
 * description: 爬虫基类
 *
 * @author: admin
 * @date: 2022/3/5/005-09:57:59
 */
@Setter
@Getter
public abstract class Crawler {
    protected static final Log log = LogUtil.getLogger();
    protected String tableName;
    protected Connection conn;
    protected String sqlCreateTable;
    protected boolean success = false; // 标志是否运行成功!, 一般run成功后需要设定 success=true

    public Crawler(String tableName) {
        this.tableName = tableName;
        setDb();
        initSqlCreateTable();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public void run() {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        logStart();
        runCore();
        if (success) {
            logSuccess();
        }
        logTimeConsume(timer.interval());
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

}
