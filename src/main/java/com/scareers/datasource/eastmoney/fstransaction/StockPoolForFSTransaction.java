package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * 均已经去重, 且加入两大指数!
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolForFSTransaction implements StockPoolFactory {
    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws Exception {
        Console.log(new StockPoolForFSTransaction().createStockPool());
    }


    @Override
    public List<SecurityBeanEm> createStockPool() throws Exception {
        return createStockPool(30);
    }

    public List<SecurityBeanEm> createStockPool(int amounts) throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<SecurityBeanEm> res = stockPoolFromTushare(0, amounts);
        res.addAll(stockListOfTwoMarketIndex());
        log.warn("finish init stockPool: 完成初始化股票池...");
        return res;
    }

    /**
     * @return 返回两大指数的 SecurityBeanEm
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static List<SecurityBeanEm> stockListOfTwoMarketIndex()
            throws Exception {
        return SecurityBeanEm.createIndexList(Arrays.asList("000001", "399001"));

    }

    public static List<SecurityBeanEm> stockPoolFromTushare(int start, int end) throws Exception {
        HashMap<String, String> stocks = TushareApi.getStockWithBoardAsMapFromTushare();
        List<SecurityBeanEm> res = new ArrayList<>();
        List<String> stockList =
                new ArrayList<>(stocks.keySet()).stream().distinct()
                        .filter(stock -> stock.endsWith("SZ") || stock.endsWith("SH"))
                        .collect(Collectors.toList());
        Collections.sort(stockList);
        stockList = stockList.subList(Math.max(0, start), Math.min(end, stockList.size()));
        return SecurityBeanEm.createStockList(stockList);
    }
}

