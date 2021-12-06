package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;

import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/7/007-5:07
 */
public class MindgoFSApi {
    public static Connection conn = ConnectionFactory.getConnLocalMindgo1M();
    public static Cache<String, DataFrame<Object>> indexFSBYSingleDateCache = CacheUtil.newLRUCache(2048);

    public static void main(String[] args) throws SQLException {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        Console.log(getIndexFSByTradeDate("000001_sh", "2021-01-04", null));
        Console.log(timer.intervalRestart());
        Console.log(getIndexFSByTradeDate("000001_sh", "2021-01-04", null));
        Console.log(timer.intervalRestart());
        Console.log(getIndexFSByTradeDate("000001_sh", "2021-01-04", null));
        Console.log(timer.intervalRestart());


    }

    public static DataFrame<Object> getIndexFSByTradeDate(String indexTablename, String tradeDate,
                                                          List<String> fields) throws SQLException {
        String fieldsStr;
        if (fields == null) {
            fieldsStr = "*";
        } else {
            fieldsStr = StrUtil.join(",", fields);
        }
        // 缓存key使用 fieldsStr

        String cacheKey = StrUtil.format("{}__{}__{}", indexTablename, tradeDate, fieldsStr);
        DataFrame<Object> res = indexFSBYSingleDateCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String sql = StrUtil.format("select {} from {} where trade_date='{}'", fieldsStr, indexTablename, tradeDate);
        res = DataFrame.readSql(conn, sql);
        indexFSBYSingleDateCache.put(cacheKey, res);
        return res;
    }

}
