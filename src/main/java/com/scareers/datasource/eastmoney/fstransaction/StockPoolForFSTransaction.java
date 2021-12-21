package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.lang.Console;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * // todo: debug仅测试.
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolForFSTransaction implements StockPoolFactory {


    @Override
    public List<StockBean> createStockPool() {
        List<StockBean> stockPool = new ArrayList<>();
        stockPool.add(new StockBean("000001", 1));// 上证指数
        stockPool.add(new StockBean("000001", 0));// 平安银行
        stockPool.add(new StockBean("000002", 1));// A股指数
        stockPool.add(new StockBean("000002", 0));// 万科A
        stockPool.add(new StockBean("399001", 0));// 深证成指
        stockPool.add(new StockBean("000153", 0));// 丰原药业
        stockPool.add(new StockBean("600037", 1));// 歌华有线
        // 去重 + 线程安全
        return new CopyOnWriteArrayList<>(stockPool.stream().distinct().collect(Collectors.toList()));
    }
}
