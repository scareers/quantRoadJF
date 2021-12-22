package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EmSecurityIdBean;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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

    private static final Log log = LogUtils.getLogger();

    @Override
    public List<StockBean> createStockPool() throws Exception {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<StockBean> res = stockPoolFromTushare(0, 100);
        log.warn("finish init stockPool: 完成初始化股票池...");
        GlobalThreadPool.shutdown(false); // 关闭hutool全局线程池, 但是不能再使用了
        return res;
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
        // https://searchapi.eastmoney.com/api/suggest/get?input=000001&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=3
        HashMap<String, String> stocks = TushareApi.getStockWithBoardAsMapFromTushare();
        List<StockBean> res = new ArrayList<>();
        List<String> stockList = new ArrayList<>(stocks.keySet());
        for (int i = Math.max(0, start); i < Math.min(stockList.size(), end); i++) {
            String stock = stockList.get(i);
            if (!stock.endsWith("SZ") && !stock.endsWith("SH")) {
                continue;
            }
            String secId = new EmSecurityIdBean(stock.substring(0, 6), false).getAStockSecId();
            if (secId == null) { // 无对应的指数 code
                continue;
            }
            res.add(new StockBean(secId));
        }
        return res;
    }
}

