package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.sqlapi.TushareApi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.scareers.sqlapi.TushareApi.getStockListWithBoardFromTushare;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * // todo: debug仅测试.
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolForFSTransaction implements StockPoolFactory {

    public static void main(String[] args) throws Exception {
        Console.log(new StockPoolForFSTransaction().createStockPool());
    }

    @Override
    public List<StockBean> createStockPool() throws Exception {
//        return stockPoolTest();
        return stockPoolFromTushare(0, 100);
    }


    public static List<StockBean> stockPoolTest() {
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

    public static List<StockBean> stockPoolFromTushare(int start, int end) throws Exception {
        HashMap<String, String> stocks = TushareApi.getStockWithBoardAsMapFromTushare();
        List<StockBean> res = new ArrayList<>();
        for (String stock : stocks.keySet()) {
            if (!stock.endsWith("SZ") && !stock.endsWith("SH")) {
                continue;
            }
            String stockCodeSimple = StrUtil.sub(stock, 0, 6);
            int market = 0;
            if (stock.startsWith("6")) {
                market = 1; // 沪
            }
            res.add(new StockBean(stockCodeSimple, market));
        }
        return res.subList(start, end);
    }
}