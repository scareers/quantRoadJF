package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EmSecurityIdBean;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.querySecurityIdsToBeans;

/**
 * description: 构建fs成交需要抓取的股票池, Map形式. 以构建 secid.
 * 均已经去重, 且加入两大指数!
 *
 * @author: admin
 * @date: 2021/12/21/021-18:16:14
 */
public class StockPoolForFSTransaction implements StockPoolFactory {
    public static void main(String[] args) throws Exception {
        Console.log(new StockPoolForFSTransaction().createStockPool());
    }

    private static final Log log = LogUtils.getLogger();

    @Override
    public List<StockBean> createStockPool() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<StockBean> res = stockPoolFromTushare(0, 100);
//        List<StockBean> res = stockPoolTest();
        log.warn("finish init stockPool: 完成初始化股票池...");
        return res;
    }

    /**
     * @param stockList
     * @param addTwoIndex 添加两大指数
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static List<StockBean> stockListFromSimpleStockList(List<String> stockList, boolean addTwoIndex)
            throws ExecutionException, InterruptedException {
        List<StockBean> res = new ArrayList<>();
        List<EmSecurityIdBean> rawBeans = querySecurityIdsToBeans(stockList);
        for (EmSecurityIdBean bean : rawBeans) {
            String secId = bean.getAStockSecId(); // 股票结果,  而非指数结果
            if (secId == null) { // 无对应的指数 code
                continue;
            }
            res.add(new StockBean(secId));
        }
        if (addTwoIndex) {
            res.add(new StockBean("1.000001"));
            res.add(new StockBean("0.399001")); // 两大指数
        }
        return new CopyOnWriteArrayList<>(res.stream().distinct().collect(Collectors.toList()));
    }


    public static List<StockBean> stockListFromSimpleStockList(List<String> stockList)
            throws ExecutionException, InterruptedException {
        return stockListFromSimpleStockList(stockList, true);
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
        List<String> stockList = new ArrayList<>(stocks.keySet());
        stockList =
                stockList.stream().distinct().filter(stock -> stock.endsWith("SZ") || stock.endsWith("SH"))
                        .collect(Collectors.toList());
        List<EmSecurityIdBean> rawBeans = querySecurityIdsToBeans(stockList.subList(Math.max(0, start),
                Math.min(stockList.size(), end)));
        for (EmSecurityIdBean bean : rawBeans) {
            String secId = bean.getAStockSecId(); // 股票结果,  而非指数结果
            if (secId == null) { // 无对应的指数 code
                continue;
            }
            res.add(new StockBean(secId));
        }
        res.add(new StockBean("1.000001"));
        res.add(new StockBean("0.399001")); // 两大指数
        return new CopyOnWriteArrayList<>(res.stream().distinct().collect(Collectors.toList()));
    }
}

