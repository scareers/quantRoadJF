package com.scareers.datasource.eastmoney.fstransaction;

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
    public List<String> createStockPool() {
        List<String> stockPool = new ArrayList<>();
        stockPool.add("1.000001"); // 上证指数
        stockPool.add("0.000001");  // 平安银行

        stockPool.add("1.000002");  // A股指数
        stockPool.add("0.000002");  // 万科A

        stockPool.add("0.399001");  // 深证成指
        stockPool.add("1.000153");  // 丰原药业
        // 去重 + 线程安全
        return new CopyOnWriteArrayList<>(stockPool.stream().distinct().collect(Collectors.toList()));
    }
}
