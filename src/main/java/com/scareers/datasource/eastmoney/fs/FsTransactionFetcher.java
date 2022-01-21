package com.scareers.datasource.eastmoney.fs;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.datasource.eastmoney.stockpoolimpl.StockPoolFromTushare;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.StrUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.scareers.datasource.selfdb.ConnectionFactory.getConnLocalFSTransactionFromEastmoney;
import static com.scareers.utils.CommonUtil.*;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 获取dfcf: tick数据3s.
 *
 * @author admin
 * @impl1: 该api来自于 dfcf 行情页面的 分时成交(个股) 或者 分笔(指数) 页面.
 * https://push2.eastmoney.com/api/qt/stock/details/get?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&fltt=2&cb=jQuery35107646502669044317_1640077292479&pos=-5900&secid=0.600000&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1640077292670
 * 该url使用安全的股票代码作为参数, secid=0.600000   0/1 分别代表 深市/ 上市
 * 科创板属于上市, 创业板属于深市. 目前 北交所前缀也为 0, 约等于深市.
 * @impl2: 该api为分页api. 实测可以更改单页数量到 5000. 同样需指定 market和code . 本质上两个api 返回值相同
 * https://push2ex.eastmoney.com/getStockFenShi?pagesize=5000&ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wzfscj&cb=jQuery112405998333817754311_1640090463418&pageindex=0&id=3990011&sort=1&ft=1&code=399001&market=0&_=1640090463419
 * 目前使用实现2. 两个url基本相同.  均使用单次访问的方式, url1直接设定 -x, url2设定单页内容 5000条
 * @date 2021/12/21/021-15:26:04
 * @see StockApi.getFSTransaction()
 */
@Data
public class FsTransactionFetcher {
    public static void main(String[] args) throws Exception {
        FsTransactionFetcher fsTransactionFetcher = getInstance
                (new StockPoolFromTushare(0, 10, true).createStockPool(),
                        10, "15:10:00", 500,
                        10, 32);

        fsTransactionFetcher.startFetch(); // 测试股票池
        waitEnter();
    }

    private static FsTransactionFetcher INSTANCE;

    public static FsTransactionFetcher getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    public static FsTransactionFetcher getInstance(List<SecurityBeanEm> stockPool, long redundancyRecords,
                                                   String limitTick, int timeout, int logFreq,
                                                   int threadPoolCorePoolSize)
            throws SQLException, InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new FsTransactionFetcher(stockPool, redundancyRecords, limitTick, timeout, logFreq,
                    threadPoolCorePoolSize);
        }
        return INSTANCE;
    }


    // 静态属性 设置项
    // 7:00之前记为昨日,抓取数据存入昨日数据表. 09:00以后抓取今日, 期间程序sleep,等待到 09:00. 需要 0<1
    public static final List<String> newDayTimeThreshold = Arrays.asList("07:00", "08:00");
    public static final Connection connSave = getConnLocalFSTransactionFromEastmoney();
    public static ThreadPoolExecutor threadPoolOfFetch;
    public static ThreadPoolExecutor threadPoolOfSave;
    private static final Log log = LogUtil.getLogger();

    // 实例属性
    // 保存每只股票进度. key:value --> 股票id: 已被抓取的最新的时间 tick
    private volatile ConcurrentHashMap<SecurityBeanEm, String> processes;
    // 保存每只股票今日分时成交所有数据. 首次将可能从数据库加载!
    private volatile ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> fsTransactionDatas;
    private volatile AtomicBoolean firstTimeFinish; // 标志第一次抓取已经完成
    private volatile boolean stopFetch; // 可非强制停止抓取, 但并不释放资源
    private long redundancyRecords; // 冗余的请求记录数量. 例如完美情况只需要情况最新 x条数据, 此设定请求更多 +法
    // tick获取时间上限, 本身只用于计算 当前应该抓取的tick数量
    private DateTime limitTick;
    private int timeout; // 单个http访问超时毫秒
    private final List<SecurityBeanEm> stockPool;
    private String saveTableName; // 保存数据表名称
    private final int logFreq; // 分时图抓取多少次,log一次时间
    public int threadPoolCorePoolSize; // 线程池数量

    private FsTransactionFetcher(List<SecurityBeanEm> stockPool, long redundancyRecords,
                                 String limitTick, int timeout, int logFreq, int threadPoolCorePoolSize)
            throws SQLException, InterruptedException {
        // 4项全默认值
        this.processes = new ConcurrentHashMap<>();
        this.fsTransactionDatas = new ConcurrentHashMap<>();
        this.firstTimeFinish = new AtomicBoolean(false); //
        this.stopFetch = false;

        // yyyy-MM-dd HH:mm:ss.SSS // 决定保存数据表
        this.saveTableName = DateUtil.format(DateUtil.date(), "yyyyMMdd"); // 今日, tushare 通用格式
        if (newDayDecide()) {
            this.saveTableName = TushareApi.getPreTradeDate(saveTableName); // 查找上一个交易日 作为数据表名称
        }

        // 4项可设定
        this.stockPool = stockPool;
        this.redundancyRecords = redundancyRecords; // 10
        this.limitTick = DateUtil.parse(DateUtil.today() + " " + limitTick); // "15:10:00"
        this.timeout = timeout; // 1000
        this.logFreq = logFreq;
        this.threadPoolCorePoolSize = threadPoolCorePoolSize;
    }


    public void startFetch() {
        Thread fsFetchTask = new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                startFetch0();
            }
        });
        fsFetchTask.setDaemon(true);
        fsFetchTask.setPriority(Thread.MAX_PRIORITY);
        fsFetchTask.setName("FSTransFetcher");
        fsFetchTask.start();
        log.warn("FSTransFetcher start: 开始持续获取实时成交数据");
    }

    private void startFetch0() throws Exception {
        initThreadPool(threadPoolCorePoolSize); // 懒加载一次
        createSaveTable(saveTableName);
        initProcessAndRawDatas(saveTableName);

        log.warn("start: 开始抓取数据,股票池数量: {}", stockPool.size());
        TimeInterval timer = DateUtil.timer();
        timer.start();

        int epoch = 0; // 抓取轮次, 控制 log 频率
        while (!stopFetch) {
            epoch++;
            List<Future<Void>> futures = new ArrayList<>();
            for (SecurityBeanEm stock : stockPool) {
                Future<Void> f = threadPoolOfFetch
                        .submit(new FetchOneStockTask(stock, this));
                futures.add(f);
            }
            for (Future<Void> future : futures) {
                future.get();
            }
            if (!firstTimeFinish.get()) {
                log.warn("finish first: 首次抓取完成...");
//                Console.log(this.getFsTransactionDatas());
                firstTimeFinish.compareAndSet(false, true); // 第一次设置true, 此后设置失败不报错
            }
            if (epoch % logFreq == 0) {
                epoch = 0;
                log.info("finish timing: 共{}轮抓取结束,耗时: {} s", logFreq, ((double) timer.intervalRestart()) / 1000);
            }
        }
        threadPoolOfFetch.shutdown();
        threadPoolOfSave.shutdown();
    }

    /**
     * 从数据库读取今日已被抓取数据,可能空. 并填充 进度map和初始数据map
     */
    private void initProcessAndRawDatas(String saveTableName) throws SQLException {
        String sqlSelectAll = StrUtilS.format("select * from `{}`", saveTableName);
        DataFrame<Object> dfAll = DataFrame.readSql(connSave, sqlSelectAll);
        for (SecurityBeanEm stock : stockPool) {
            DataFrame<Object> datasOfOneStock =
                    dfAll.select(value -> value.get(0).toString().equals(stock.getStockCodeSimple()) && value.get(1)
                            .toString()
                            .equals(stock.getMarket().toString()));
            datasOfOneStock = datasOfOneStock.sortBy("time_tick"); // 保证有序
            fsTransactionDatas.put(stock, datasOfOneStock); // 可空
            processes.putIfAbsent(stock, "09:00:00"); // 默认值

            List<String> timeTicks = DataFrameS.getColAsStringList(datasOfOneStock, "time_tick");
            Optional<String> maxTick = timeTicks.stream().max(Comparator.naturalOrder());
            maxTick.ifPresent(s -> processes.put(stock, s)); // 修改.
        }
        log.warn("init process And datas: 初始化完成");
    }


    private boolean newDayDecide() throws InterruptedException, SQLException {
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
            log.error("wait: 当前时间介于昨日今日判定阈值设定之间, 需等待到: {}, 等待时间: {} 秒 / {}小时",
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
        String sql = StrUtilS.format("create table if not exists `{}`\n" +
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


    public static void initThreadPool(int threadPoolCorePoolSize) {
        if (threadPoolOfFetch == null) {
            threadPoolOfFetch = new ThreadPoolExecutor(threadPoolCorePoolSize,
                    threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    ThreadUtil.newNamedThreadFactory("FSFetcherPool-", null, true)
            );
            log.debug("init threadPoolOfFetch: 初始化Fetch线程池完成,核心线程数量: {}", threadPoolCorePoolSize);
        }
        if (threadPoolOfSave == null) { // 保存线程相同数量, 否则将成为瓶颈
            threadPoolOfSave = new ThreadPoolExecutor(threadPoolCorePoolSize,
                    threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    ThreadUtil.newNamedThreadFactory("FSTSavePool-", null, true)
            );
            log.debug("init threadPoolOfSave: 初始化Save线程池完成,核心线程数量: {}", threadPoolCorePoolSize);
        }
    }

    public void stopFetch() {
        // 关闭线程池即可
        stopFetch = true;
    }

    public static class FetchOneStockTask implements Callable<Void> {
        SecurityBeanEm stock;
        FsTransactionFetcher fetcher;

        public FetchOneStockTask(SecurityBeanEm stock, FsTransactionFetcher fetcher) {
            this.stock = stock;
            this.fetcher = fetcher;
        }

        /**
         * 单只股票抓取逻辑. 整体为 增量更新.
         *
         * @return
         * @throws Exception
         */
        @Override
        public Void call() throws Exception {
            String process = fetcher.getProcesses().get(stock); // 进度, time_tick表示
            DataFrame<Object> dataOriginal = fetcher.getFsTransactionDatas().get(stock); // 原全数据.
            // 计算一个合适的数量, 用当前时间 - 进度 的 秒数 / 3 == 数据数量,  外加 n 条冗余!
            int suitableCounts = (int) (calcCountsBetweenNowAndProcess(process) + fetcher.getRedundancyRecords());
            DataFrame<Object> dfNew = StockApi.getFSTransaction(suitableCounts, stock.getStockCodeSimple(),
                    stock.getMarket(), fetcher.timeout);
            // 将新df 中, 在 旧df中的 time_tick全部删除, 然后拼接更新的df
            HashSet<String> timeTicksOrginal = new HashSet<>(DataFrameS.getColAsStringList(dataOriginal,
                    "time_tick"));
            // @noti: 分时有序: 取决于初始化时排序, 且纯新增数据有序后连接
            DataFrame<Object> dfTemp = dfNew.select(value -> !timeTicksOrginal.contains(value.get(2).toString()))
                    .sortBy("time_tick");
            DataFrame<Object> dfCurrentAll = dataOriginal.concat(dfTemp);
            if (dfTemp.length() > 0) { // 若存在纯新数据
                threadPoolOfSave.submit(() -> {
                    try { // 保存使用另外线程池, 不阻塞主线程池,因此若从数据库获取数据, 显然有明显延迟.应从静态属性读取内存中数据
                        DataFrameS.toSql(dfTemp, fetcher.saveTableName, connSave, "append", null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }
            // 有序判定
            // Console.log(dfCurrentAll.col("time_tick").equals(dfCurrentAll.sortBy("time_tick").col("time_tick")));
            fetcher.getFsTransactionDatas().put(stock, dfCurrentAll);
            Optional<String> processNew =
                    (DataFrameS.getColAsStringList(dfCurrentAll, "time_tick")).stream()
                            .max(Comparator.naturalOrder());
            if (processNew.isPresent()) {
                fetcher.getProcesses().put(stock, processNew.get());
            } else {
                // 例如停牌则没有数据
                fetcher.getProcesses().put(stock, "09:00:00");
            }
            log.debug("updated: 已更新数据及进度: {} --> {} --> {} ", stock, dfCurrentAll.length(), processNew);
            return null;
        }

        public long calcCountsBetweenNowAndProcess(String process) {
            DateTime now = DateUtil.date();
            String today = DateUtil.today();
            DateTime processTick = DateUtil.parse(today + " " + process);

            long between;
            if (DateUtil.between(now, fetcher.getLimitTick(), DateUnit.SECOND, false) < 0) { // now超过4点
                between = DateUtil.between(processTick, fetcher.getLimitTick(), DateUnit.SECOND, false);
            } else {
                between = DateUtil.between(processTick, now, DateUnit.SECOND, false);
            }
            return Math.min(between / 3 + 1, 5000); // 上限5000
        }
    }
}
