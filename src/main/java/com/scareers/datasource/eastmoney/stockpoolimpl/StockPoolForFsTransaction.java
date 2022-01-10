package com.scareers.datasource.eastmoney.stockpoolimpl;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.StockPoolFactory;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtil;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.SecurityBeanEm.getTwoGlobalMarketIndexList;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * 均已经去重, 且加入两大指数!
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolForFsTransaction implements StockPoolFactory {
    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) throws Exception {
        StockPoolForFsTransaction stockPoolForFsTransaction = new StockPoolForFsTransaction(
                Arrays.asList("000001", "600001"), false);
        Console.log(stockPoolForFsTransaction.createStockPool());
    }

    private List<String> stockPoolSimple; // 纯股票代码列表
    private boolean addTwoMarketIndex; // 是否添加两大指数

    public StockPoolForFsTransaction(List<String> stockPoolSimple, boolean addTwoMarketIndex) {
        this.stockPoolSimple = stockPoolSimple;
        this.addTwoMarketIndex = addTwoMarketIndex;
    }

    @Override
    public List<SecurityBeanEm> createStockPool() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<SecurityBeanEm> results = SecurityBeanEm.createStockList(stockPoolSimple);
        if (addTwoMarketIndex) {
            results.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        }
        log.warn("finish init stockPool: 完成初始化股票池,股票池数量: {}", results.size());
        return results;
    }
}

