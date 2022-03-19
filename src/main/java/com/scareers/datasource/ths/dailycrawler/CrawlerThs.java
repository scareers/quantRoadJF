package com.scareers.datasource.ths.dailycrawler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.Log;
import com.scareers.datasource.Crawler;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 同花顺爬虫基类
 *
 * @noti : 问财多数api可能字段值可能null, 因此数据库表尽量字段可null
 * @author: admin
 * @date: 2022/3/5/005-09:57:59
 */
@Setter
@Getter
public abstract class CrawlerThs extends Crawler {
    protected static final Log log = LogUtil.getLogger();
    protected String tableName;
    protected Connection conn;
    protected String sqlCreateTable;
    protected boolean success = false; // 标志是否运行成功!, 一般run成功后需要设定 success=true

    public CrawlerThs(String tableName) {
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

    protected abstract Map<Object, Object> getRenameMap(Set<Object> rawColumns); // 十分常见, 将df原列名, 转换为新列名


    /*
    设置保存数据库 conn对象
     */

    /**
     * 同花顺主数据库 -- eastmoney
     */
    protected void setSaveToMainDb() {
        conn = ConnectionFactory.getConnLocalThs();
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
     * 不重命名时, 直接调用此默认实现, 实现getRenameMap, 返回空
     */
    protected Map<Object, Object> doNotRenameMap(Set<Object> rawColumns) {
        return new HashMap<>();
    }
}
