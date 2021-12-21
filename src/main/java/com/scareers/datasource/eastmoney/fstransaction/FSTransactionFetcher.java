package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.StrUtil;
import com.scareers.utils.log.LogUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.scareers.datasource.selfdb.ConnectionFactory.getConnLocalFSTransactionFromEastmoney;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 获取dfcf: tick数据3s.
 * 该api来自于 dfcf 行情页面的 分时成交(个股) 或者 分笔(指数) 页面.
 * https://push2.eastmoney.com/api/qt/stock/details/get?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&fltt=2&cb=jQuery35107646502669044317_1640077292479&pos=-5900&secid=0.600000&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1640077292670
 * <p>
 * // @noti: 该url使用安全的股票代码作为参数, secid=0.600000   0/1 分别代表 深市/ 上市
 * // @noti: 科创板属于上市, 创业板属于深市. 目前 北交所前缀也为 0, 约等于深市.
 *
 * @author: admin
 * @date: 2021/12/21/021-15:26:04
 */
public class FSTransactionFetcher {
    // 实例属性
    public StockPoolFactory stockPoolFactory;

    // 静态属性
    // 7:00之前记为昨日,抓取数据存入昨日数据表. 09:00以后抓取今日, 期间程序sleep,等待到 09:00. 需要 0<1
    public static final List<String> newDayTimeThreshold = Arrays.asList("17:00", "18:00");
    public static final Connection connSave = getConnLocalFSTransactionFromEastmoney();
    public static String keyUrlTemplate = "https://push2.eastmoney.com/api/qt/stock/details/get?fields1=f1,f2,f3,f4" +
            "&fields2=f51,f52,f53,f54,f55&fltt=2" +
            "&cb=jQuery35107646502669044317_{}" +  // 尾部为毫秒时间戳
            "&pos=-{}" + // 为倒数多少条数据? 标准四小时上限为 4800, 数据时间升序
            "&secid={}" + // 安全id, 见类文档  0.600000
            "&ut=fa5fd1943c7b386f172d6893dbfba10b" +
            "&_={}"; // 时间戳2. 比上一多一点时间, 建议 random 毫秒
    public static int threadPoolCorePoolSize = 16;
    private static final Log log = LogUtils.getLogger();
    public static ThreadPoolExecutor threadPool;


    public static void main(String[] args) throws Exception {
        initThreadPool();
        FSTransactionFetcher fetcher = new FSTransactionFetcher(new StockPoolForFSTransaction());
        boolean shouldFetchToday = fetcher.newDayDecide();
        // yyyy-MM-dd HH:mm:ss.SSS
        String saveTableName = DateUtil.format(DateUtil.date(), "yyyyMMdd"); // 今日, tushare 通用格式
        if (!shouldFetchToday) {
            saveTableName = TushareApi.getPreTradeDate(saveTableName); // 查找上一个交易日 作为数据表名称
        }
        fetcher.createSaveTable(saveTableName);
        List<String> stockPool = fetcher.stockPoolFactory.createStockPool(); // 该实现线程安全


    }

    public FSTransactionFetcher(StockPoolFactory stockPoolFactory) {
        this.stockPoolFactory = stockPoolFactory;
    }

    public FSTransactionFetcher() {
    }


    public boolean newDayDecide() throws InterruptedException, SQLException {
        DateTime now = DateUtil.date();
        String today = DateUtil.today();
        DateTime thresholdBefore = DateUtil.parse(today + " " + newDayTimeThreshold.get(0));
        DateTime thresholdAfter = DateUtil.parse(today + " " + newDayTimeThreshold.get(1));

        long gtBefore = DateUtil.between(thresholdBefore, now, DateUnit.SECOND, false); // 比下限大
        long ltAfter = DateUtil.between(now, thresholdAfter, DateUnit.SECOND, false); // 比上限小

        boolean res;
        if (!TushareApi.isTradeDate(DateUtil.format(DateUtil.date(), "yyyyMMdd"))) {
            log.warn("date decide: 今日非交易日,应当抓取上一交易日数据");
            return false;
        }
        if (gtBefore <= 0) {
            res = false;
        } else if (ltAfter <= 0) {
            res = true;
        } else {
            // 此时介于两者之间, 应当等待到 今日开始的时间阈值
            log.warn("wait: 当前时间介于昨日今日判定阈值设定之间, 需等待到: {}, 等待时间: {} 秒 / {}小时",
                    newDayTimeThreshold.get(1), ltAfter, ((double) ltAfter) / 3600);
            log.warn("waiting ...");
            Thread.sleep(ltAfter * 1000);
            res = true;
        }
        if (res) {
            log.warn("date decide: 依据设定,应当抓取今日数据");
        } else {
            log.warn("date decide: 依据设定,应当抓取上一交易日数据");
        }
        return res;
    }

    /**
     * 手动建表, 以防多线程  df.to_sql 错误;  全字段索引
     *
     * @param saveTableName
     * @throws Exception
     */
    public void createSaveTable(String saveTableName) throws Exception {
        String sql = StrUtil.format("create table if not exists `{}`\n" +
                "        (\n" +
                "            stock_code varchar(128)   null,\n" +
                "            market int null,\n" +
                "            time_tick  varchar(128)   null,\n" +
                "            price      double null,\n" +
                "            vol        bigint null,\n" +
                "            bs         int null,\n" +
                "            index stock_code_index (stock_code ASC),\n" +
                "            index market_index (market ASC),\n" +
                "            index time_tick_index (time_tick ASC),\n" +
                "            index bs_index (bs ASC),\n" +
                "            index price_index (price ASC),\n" +
                "            index vol_index (vol ASC)\n" +
                "        );", saveTableName);
        execSql(sql, connSave);
        log.info("create table: 创建数据表成功: {}", saveTableName);
    }

    public StockPoolFactory getStockPoolFactory() {
        return stockPoolFactory;
    }

    public void setStockPoolFactory(StockPoolFactory stockPoolFactory) {
        this.stockPoolFactory = stockPoolFactory;
    }


    public static void initThreadPool() {
        threadPool = new ThreadPoolExecutor(threadPoolCorePoolSize,
                threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()); // 唯一线程池, 一直不shutdown
        log.debug("init threadpool: 初始化唯一线程池,核心线程数量: {}", threadPoolCorePoolSize);
    }
}
