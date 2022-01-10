package com.scareers.datasource.eastmoney.stockpoolimpl;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.StockPoolFactory;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.SecurityBeanEm.getTwoGlobalMarketIndexList;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * 均已经去重, 且加入两大指数!
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolFromTushare implements StockPoolFactory {
    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) throws Exception {
        Console.log(new StockPoolFromTushare(0, 10, true).createStockPool());
    }

    private int startIndex;
    private int endIndex;
    private boolean addTwoMarketIndex; // 是否添加两大指数

    public StockPoolFromTushare(int startIndex, int endIndex, boolean addTwoMarketIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.addTwoMarketIndex = addTwoMarketIndex;
    }

    @Override
    public List<SecurityBeanEm> createStockPool() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<SecurityBeanEm> results = stockPoolFromTushare(startIndex, endIndex);
        if (addTwoMarketIndex) {
            results.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        }
        log.warn("finish init stockPool: 完成初始化股票池,股票池数量: {}", results.size());
        return results;
    }


    public static List<SecurityBeanEm> stockPoolFromTushare(int start, int end) throws Exception {
        HashMap<String, String> stocks = TushareApi.getStockWithBoardAsMapFromTushare();
        List<String> stockList =
                new ArrayList<>(stocks.keySet()).stream().distinct()
                        .filter(stock -> stock.endsWith("SZ") || stock.endsWith("SH")).sorted()
                        .collect(Collectors.toList());
        stockList = stockList.subList(Math.max(0, start), Math.min(end, stockList.size()));
        return SecurityBeanEm.createStockList(stockList);
    }
}

