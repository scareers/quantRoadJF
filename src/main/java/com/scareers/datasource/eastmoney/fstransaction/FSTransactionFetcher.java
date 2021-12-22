package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.StrUtil;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.scareers.datasource.selfdb.ConnectionFactory.getConnLocalFSTransactionFromEastmoney;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 获取dfcf: tick数据3s.
 * /*
 * <p>
 * 实现1:该api来自于 dfcf 行情页面的 分时成交(个股) 或者 分笔(指数) 页面.
 * https://push2.eastmoney.com/api/qt/stock/details/get?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&fltt=2&cb=jQuery35107646502669044317_1640077292479&pos=-5900&secid=0.600000&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1640077292670
 * <p>
 * // @noti: 该url使用安全的股票代码作为参数, secid=0.600000   0/1 分别代表 深市/ 上市
 * // @noti: 科创板属于上市, 创业板属于深市. 目前 北交所前缀也为 0, 约等于深市.
 * <p>
 * 实现2: 该api为分页api. 实测可以更改单页数量到 5000. 同样需指定 market和code . 本质上两个api 返回值相同
 * https://push2ex.eastmoney.com/getStockFenShi?pagesize=5000&ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wzfscj&cb=jQuery112405998333817754311_1640090463418&pageindex=0&id=3990011&sort=1&ft=1&code=399001&market=0&_=1640090463419
 * <p>
 * // @noti: 目前使用实现2. 两个url基本相同.  均使用单次访问的方式, url1直接设定 -x, url2设定单页内容 5000条
 * <p>
 * // @see: StockApi.getFSTransaction()
 *
 * @author: admin
 * @date: 2021/12/21/021-15:26:04
 */
public class FSTransactionFetcher {
    // 实例属性
    private List<StockBean> stockPool;
    private StockPoolFactory stockPoolFactory;

    // 静态属性
    // 7:00之前记为昨日,抓取数据存入昨日数据表. 09:00以后抓取今日, 期间程序sleep,等待到 09:00. 需要 0<1
    public static final List<String> newDayTimeThreshold = Arrays.asList("08:00", "09:00");
    public static final Connection connSave = getConnLocalFSTransactionFromEastmoney();
    public static int threadPoolCorePoolSize = 8;
    private static final Log log = LogUtils.getLogger();
    public static ThreadPoolExecutor threadPoolOfFetch;
    public static ThreadPoolExecutor threadPoolOfSave;
    // 保存每只股票进度. key:value --> 股票id: 已被抓取的最新的时间 tick
    public static ConcurrentHashMap<StockBean, String> processes = new ConcurrentHashMap<>();
    // 保存每只股票今日分时成交所有数据. 首次将可能从数据库加载!
    public static ConcurrentHashMap<StockBean, DataFrame<Object>> fsTransactionDatas = new ConcurrentHashMap<>();
    public static long redundancyRecords = 20; // 冗余的请求记录数量. 例如完美情况只需要情况最新 x条数据, 此设定请求更多 +法
    public static AtomicBoolean firstTimeFinish = new AtomicBoolean(false);

    public static void startFetch() throws Exception {
        initThreadPool();
        FSTransactionFetcher fetcher = new FSTransactionFetcher(new StockPoolForFSTransaction());
        boolean shouldFetchToday = fetcher.newDayDecide();
        // yyyy-MM-dd HH:mm:ss.SSS
        String saveTableName = DateUtil.format(DateUtil.date(), "yyyyMMdd"); // 今日, tushare 通用格式
        if (!shouldFetchToday) {
            saveTableName = TushareApi.getPreTradeDate(saveTableName); // 查找上一个交易日 作为数据表名称
        }
        createSaveTable(saveTableName);
        List<StockBean> stockPool = fetcher.getStockPool();
        initProcessAndRawDatas(saveTableName, stockPool);
        log.warn("start: 开始抓取数据...");
        TimeInterval timer = DateUtil.timer();
        timer.start();
        while (true) {
            List<Future<Void>> futures = new ArrayList<>();
            for (StockBean stock : stockPool) {
                Future<Void> f = threadPoolOfFetch
                        .submit(new FetchOneStockTask(stock, saveTableName));
                futures.add(f);
            }
            //Tqdm.tqdm(futures, "process: ")
            for (Future<Void> future : futures) {
                future.get();
            }
            if (!firstTimeFinish.get()) {
                log.warn("finish first: 首次抓取完成...");
            }
            log.warn("finish timing: 本轮抓取结束,耗时: {} s", ((double) timer.intervalRestart()) / 1000);
            firstTimeFinish.compareAndSet(false, true); // 第一次设置true, 此后设置失败不报错
        }
        //        threadPool.shutdown();
    }

    /**
     * 从数据库读取今日已被抓取数据,可能空. 并填充 进度map和初始数据map
     */
    private static void initProcessAndRawDatas(String saveTableName, List<StockBean> stockPool) throws SQLException {
        String sqlSelectAll = StrUtil.format("select * from `{}`", saveTableName);
        DataFrame<Object> dfAll = DataFrame.readSql(connSave, sqlSelectAll);
        for (StockBean stock : stockPool) {
            DataFrame<Object> datasOfOneStock =
                    dfAll.select(value -> value.get(0).toString().equals(stock.getStockCodeSimple()) && value.get(1)
                            .toString()
                            .equals(stock.getMarket().toString()));
            fsTransactionDatas.put(stock, datasOfOneStock); // 可空
            processes.putIfAbsent(stock, "09:00:00"); // 默认值

            List<String> timeTicks = DataFrameSelf.getColAsStringList(datasOfOneStock, "time_tick");
            Optional<String> maxTick = timeTicks.stream().max(Comparator.naturalOrder());
            maxTick.ifPresent(s -> processes.put(stock, s)); // 修改.
        }
        log.warn("init process And datas: 初始化完成");
    }

    public FSTransactionFetcher(StockPoolFactory stockPoolFactory) throws Exception {
        this.stockPoolFactory = stockPoolFactory;
        this.stockPool = this.stockPoolFactory.createStockPool();
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
    public static void createSaveTable(String saveTableName) throws Exception {
        String sql = StrUtil.format("create table if not exists `{}`\n" +
                "        (\n" +
                "            stock_code varchar(128)   null,\n" +
                "            market int null,\n" +
                "            time_tick  varchar(128)   null,\n" +
                "            price      double null,\n" +
                "            vol        double null,\n" +
                "            bs         varchar(8) null,\n" +
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

    public List<StockBean> getStockPool() {
        return stockPool;
    }

    public void setStockPool(List<StockBean> stockPool) {
        this.stockPool = stockPool;
    }

    public static void initThreadPool() {
        threadPoolOfFetch = new ThreadPoolExecutor(threadPoolCorePoolSize,
                threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()); // 唯一线程池, 一直不shutdown
        threadPoolOfSave = new ThreadPoolExecutor(threadPoolCorePoolSize,
                threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()); // 唯一线程池, 一直不shutdown
        log.debug("init threadpool: 初始化唯一线程池,核心线程数量: {}", threadPoolCorePoolSize);
    }

    public static class FetchOneStockTask implements Callable<Void> {
        StockBean stock;
        String saveTableName;

        public FetchOneStockTask(StockBean stock, String saveTableName) {
            this.stock = stock;
            this.saveTableName = saveTableName;
        }

        /**
         * 单只股票抓取逻辑. 整体为 增量更新.
         *
         * @return
         * @throws Exception
         */
        @Override
        public Void call() throws Exception {
            String process = processes.get(stock); // 进度, time_tick表示
            DataFrame<Object> dataOriginal = fsTransactionDatas.get(stock); // 原全数据.
            // 计算一个合适的数量, 用当前时间 - 进度 的 秒数 / 3 == 数据数量,  外加 n 条冗余!
            int suitableCounts = (int) (calcCountsBetweenNowAndProcess(process) + redundancyRecords);
            DataFrame<Object> dfNew = StockApi.getFSTransaction(suitableCounts, stock.getStockCodeSimple(),
                    stock.getMarket());
            // 将新df 中, 在 旧df中的 time_tick全部删除, 然后拼接更新的df
            HashSet<String> timeTicksOrginal = new HashSet<>(DataFrameSelf.getColAsStringList(dataOriginal,
                    "time_tick"));
            DataFrame<Object> dfTemp = dfNew.select(value -> !timeTicksOrginal.contains(value.get(2).toString()));
            DataFrame<Object> dfCurrentAll = dataOriginal.concat(dfTemp);
            if (dfTemp.length() > 0) { // 若存在纯新数据
                threadPoolOfSave.submit(() -> {
                    try { // 保存使用另外线程池, 不阻塞主线程池,因此若从数据库获取数据, 显然有明显延迟.应从静态属性读取内存中数据
                        DataFrameSelf.toSql(dfTemp, saveTableName, connSave, "append", null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }
            fsTransactionDatas.put(stock, dfCurrentAll);
            Optional<String> processNew =
                    (DataFrameSelf.getColAsStringList(dfCurrentAll, "time_tick")).stream()
                            .max(Comparator.naturalOrder());
            if (processNew.isPresent()) {
                processes.put(stock, processNew.get());
            } else {
                // 例如停牌则没有数据
//                Console.log(dfCurrentAll);
//                Console.log(dataOriginal);
//                Console.log(dfTemp);
//                Console.log(stock);
                processes.put(stock, "09:00:00");
            }
            log.info("updated: 已更新数据及进度: {} --> {} --> {} ", stock, dfCurrentAll.length(), processNew);
            return null;
        }

        public long calcCountsBetweenNowAndProcess(String process) {
            DateTime now = DateUtil.date();
            String today = DateUtil.today();
            DateTime processTick = DateUtil.parse(today + " " + process);
            long between = DateUtil.between(processTick, now, DateUnit.SECOND, false);
            return Math.min(between / 3 + 1, 5000); // 上限5000
        }
    }
}
