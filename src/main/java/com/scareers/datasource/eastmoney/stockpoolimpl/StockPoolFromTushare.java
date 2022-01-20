package com.scareers.datasource.eastmoney.stockpoolimpl;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.StockPoolFactory;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 从tushare读取股票列表, 设置slice, 做测试股票池所用
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
        List<SecurityBeanEm> results = stockPoolFromTushare(startIndex, endIndex);
        if (addTwoMarketIndex) {
            results.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        }
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

