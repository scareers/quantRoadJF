package com.scareers.datasource.eastmoney.fetcher;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
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
import static com.scareers.utils.CommonUtil.waitUtil;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 获取东方财富, 分时成交数据: 即tick数据最快3s/条, 或5s/条;
 *
 * @author admin
 * @impl1: 该api来自于 dfcf 行情页面的 分时成交(个股) 或者 分笔(指数) 页面.
 * https://push2.eastmoney.com/api/qt/stock/details/get?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&fltt=2&cb=jQuery35107646502669044317_1640077292479&pos=-5900&secid=0.600000&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1640077292670
 * 该url使用资产代码作为参数, secid=0.600000   0/1 分别代表 深市/ 上市
 * 科创板属于上市, 创业板属于深市. 目前 北交所前缀也为 0, 约等于深市.
 * @impl2: 该api为分页api. 实测可以更改单页数量到 5000. 同样需指定 market和code . 本质上两个api 返回值相同
 * https://push2ex.eastmoney.com/getStockFenShi?pagesize=5000&ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wzfscj&cb=jQuery112405998333817754311_1640090463418&pageindex=0&id=3990011&sort=1&ft=1&code=399001&market=0&_=1640090463419
 * 目前使用实现1. 两个url基本相同.  均使用单次访问的方式, url1直接设定 -x, url2设定单页内容 5000条
 * @warning 因保存到数据库, 所以需要判定当前api获取得到的分时成交是 "昨日" 还是 "今日的", 具体哪一天的?
 * 假设api数据于 每日 01:00:00 刷新为空, 将于9:25:02 刷新第一条今日数据! 则 01:00:00 前启动将保存到昨天数据库.
 * 因此在为 00:00 - 01:00:00 (刷新时刻) 之间, 不可运行抓取程序. 因此 -->
 * @noti 本类实现, 将在每一轮, 自动判定数据应当保存在 "昨日"还是"今日"数据库, 且切换时, 首先将所有数据设置为空df. 以免重复.
 * @noti 列参考:       stock_code	market	time_tick	price	 vol	bs
 * @noti 列数据实例:     000002    	0     	09:15:00 	20.82	 6   	4
 * @warning 见静态属性 sleepNoFetchDateTimeRange(将在此期间暂停). 时间后建立"今日"数据表, 时间前保存到昨日数据表, 时间中sleep(1000)
 * @warning sleepNoFetchDateTimeRange 的两个时间, 均代表 "今天的某个时间区间", 不代表昨天的
 * @date 2021/12/21/021-15:26:04
 * @see EmQuoteApi.getFSTransaction()
 * <p>
 * todo : 2个
 */
@Data
public class FsTransactionFetcher {
    public static void main(String[] args) throws Exception {
        SecurityPool.addToOtherCareBKs(SecurityPool.createBKPool(10, true, Arrays.asList("概念板块")));
        SecurityPool.addToOtherCareStocks(SecurityPool.createStockPool(10, true));
        SecurityPool.addToOtherCareIndexes(SecurityPool.createIndexPool(10, true, Arrays.asList("上证系列指数")));
        SecurityPool.addToOtherCareIndexes(SecurityBeanEm.getTwoGlobalMarketIndexList());

        FsTransactionFetcher fsTransactionFetcher = getInstance
                (
                        10, "15:10:00", 500,
                        10, 32);

        fsTransactionFetcher.startFetch(); // 测试股票池
        fsTransactionFetcher.waitFirstEpochFinish();
        fsTransactionFetcher.stopFetch(); // 软停止,且等待完成
        fsTransactionFetcher.reStartFetch(); // 再次开始
        fsTransactionFetcher.waitFirstEpochFinish();

        Console.log(fsTransactionDatas.size());
        Console.log(getStockPool().size());
        SecurityBeanEm stock = RandomUtil.randomEle(getStockPool());

        Console.log(FsTransactionFetcher.getFsTransData(stock));
        Console.log(FsTransactionFetcher.getColAsObjectList(stock, "price"));
        Console.log(FsTransactionFetcher.getColByColNameOrIndexAsDouble(stock, "price"));
        Console.log(FsTransactionFetcher.getNewestPrice(stock));

        fsTransactionFetcher.stopFetch();
    }


    private static FsTransactionFetcher INSTANCE;

    public static FsTransactionFetcher getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    public static FsTransactionFetcher getInstance(long redundancyRecords,
                                                   String limitTick, int timeout, int logFreq,
                                                   int threadPoolCorePoolSize) {
        if (INSTANCE == null) {
            INSTANCE = new FsTransactionFetcher(redundancyRecords, limitTick, timeout, logFreq,
                    threadPoolCorePoolSize);
        }
        return INSTANCE;
    }

    public static ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> getFsTransactionDatas() {
        return fsTransactionDatas;
    }

    public static List<SecurityBeanEm> getStockPool() {
        return SecurityPool.poolForFsTransactionFetcherCopy();
    }

    // 静态属性 设置项
    // todo: 需要正确更新这个 隔天交替时间!
    public static final List<String> sleepNoFetchDateTimeRange = Arrays.asList("08:00:00", "09:00:00");
    public static final Connection connSave = getConnLocalFSTransactionFromEastmoney();
    public static ThreadPoolExecutor threadPoolOfFetch;
    public static ThreadPoolExecutor threadPoolOfSave;
    private static final Log log = LogUtil.getLogger();

    // 数据, gui
    public static volatile ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> fsTransactionDatas;

    // 实例属性
    // 保存每只股票进度. key:value --> 股票id: 已被抓取的最新的时间 tick
    private volatile ConcurrentHashMap<SecurityBeanEm, String> processes;
    // 保存每只股票今日分时成交所有数据. 首次将可能从数据库加载!
    private volatile AtomicBoolean firstTimeFinish; // 标志第一次抓取已经完成
    private volatile boolean stopFetch; // 可非强制停止抓取, 但并不释放资源
    private long redundancyRecords; // 冗余的请求记录数量. 例如完美情况只需要情况最新 x条数据, 此设定请求更多 +法
    // tick获取时间上限, 本身只用于计算 当前应该抓取的tick数量
    private DateTime limitTick;
    private int timeout; // 单个http访问超时毫秒
    private final int logFreq; // 分时图抓取多少次,log一次时间
    private int threadPoolCorePoolSize; // 线程池数量

    private volatile String saveTableName; // 保存数据表名称, 每一轮初始化, 使用 日期作为表名
    private volatile String preSaveTableName; // 保存上一次数据表名称, 初始 "", 将被判定隔日切换时, 从新空表获取已抓取数据
    private volatile boolean running = false;

    private FsTransactionFetcher(long redundancyRecords,
                                 String limitTick, int timeout, int logFreq, int threadPoolCorePoolSize) {
        // 4项全默认值
        this.processes = new ConcurrentHashMap<>(); // 自动设置 00:00:00 作为初始
        fsTransactionDatas = new ConcurrentHashMap<>(); // 将自动设置空df
        this.firstTimeFinish = new AtomicBoolean(false); //
        this.stopFetch = false;

        // 4项可设定
        this.redundancyRecords = redundancyRecords; // 10
        this.limitTick = DateUtil.parse(DateUtil.today() + " " + limitTick); // "15:10:00"
        this.timeout = timeout; // 1000
        this.logFreq = logFreq;
        this.threadPoolCorePoolSize = threadPoolCorePoolSize;

        this.saveTableName = null; // 将每轮自动设定
        this.preSaveTableName = ""; // 将在隔日切换时
    }


    /**
     * 可停止后重启
     */
    public void reStartFetch() {
        if (this.running) {
            log.error("FSTransFetcher 正在运行中, 无法再次开始, 调用 stopFetch() 后可停止, 方可再次开始");
            return;
        }
        startFetch();
    }

    public void startFetch() {
        this.running = true;
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
        log.warn("start: 开始抓取数据,股票池数量: {}", getStockPool().size());
        this.running = true;
        TimeInterval timer = DateUtil.timer();
        timer.start();
        int epoch = 0; // 抓取轮次, 控制 log 频率
        while (!stopFetch) {
            initThreadPool(threadPoolCorePoolSize); // 懒加载一次
            newDayDecide(); // 决定是否是新的一天, 将可能设定或者更新 saveTableName 字段, 该字段为null时表示隔天切换, sleep
            if (saveTableName == null) {
                String today = DateUtil.today();
                DateTime now = DateUtil.date(); // now, 即使这一刻过 00:00:00, 影响不大, 因sleep 使用 abs值, 极小
                DateTime thresholdAfter = DateUtil.parse(today + " " + sleepNoFetchDateTimeRange.get(1));
                long sleepMs = DateUtil.between(now, thresholdAfter, DateUnit.MS, true); // 需要abs
                log.error("show: wait: 当前时间介于昨日今日判定阈值{}之间, 需要 sleep: {} s",
                        sleepNoFetchDateTimeRange, ((double) sleepMs) / 1000);
                if (sleepMs < 10000) { // 将会10s一次
                    Thread.sleep(sleepMs);
                } else {
                    Thread.sleep(10000);
                }
                continue;
            }

            createSaveTable(saveTableName); // 此时若在前一天则saveTableName不变, 否则新的一天. 建表将查询已建表缓存.

            if (!preSaveTableName.equals(saveTableName)) { // 隔日切换 以及 第一次初始化时
                initProcessAndRawDatas(saveTableName);
                preSaveTableName = saveTableName;
                epoch = 0;
                timer.restart();
            }

            epoch++;
            List<Future<Boolean>> futures = new ArrayList<>();
            for (SecurityBeanEm stock : getStockPool()) {
                Future<Boolean> f = threadPoolOfFetch
                        .submit(new FetchOneStockTask(stock, this));
                futures.add(f);
            }

            boolean allSuccess = true;
            for (Future<Boolean> future : futures) {
                boolean success = future.get();
                if (!success) {
                    allSuccess = false;
                }
            }
            if (!firstTimeFinish.get() && allSuccess) {
                log.warn("finish first: 首次抓取完成...");
                firstTimeFinish.compareAndSet(false, true); // 第一次设置true, 此后设置失败不报错
            }
            if (epoch % logFreq == 0) {
                epoch = 0;
                log.info("finish timing: 共{}轮抓取结束,耗时: {} s", logFreq, ((double) timer.intervalRestart()) / 1000);
            }
        }
        this.stopFetch = false;
        this.firstTimeFinish = new AtomicBoolean(false); // 首次完成flag也重置
        threadPoolOfFetch.shutdown();
        threadPoolOfSave.shutdown();
        this.running = false;
    }

    /**
     * 从数据库读取今日已被抓取数据,可能空. 并填充 进度map和初始数据map
     */
    private void initProcessAndRawDatas(String saveTableName) throws SQLException {
        String sqlSelectAll = StrUtilS.format("select * from `{}`", saveTableName);
        DataFrame<Object> dfAll = DataFrame.readSql(connSave, sqlSelectAll);
        for (SecurityBeanEm stock : getStockPool()) {
            DataFrame<Object> datasOfOneStock =
                    dfAll.select(value -> value.get(0).toString().equals(stock.getSecCode()) && value.get(1)
                            .toString()
                            .equals(stock.getMarket().toString()));
            datasOfOneStock = datasOfOneStock.sortBy("time_tick"); // 保证有序
            fsTransactionDatas.put(stock, datasOfOneStock); // 可空
            processes.putIfAbsent(stock, "00:00:00"); // 默认值
            List<String> timeTicks = DataFrameS.getColAsStringList(datasOfOneStock, "time_tick");
            Optional<String> maxTick = timeTicks.stream().max(Comparator.naturalOrder());
            maxTick.ifPresent(s -> processes.put(stock, s)); // 修改.
        }

        log.warn("init process And datas: 初始化完成");
    }

    private boolean logged = false;

    @SneakyThrows
    private void newDayDecide() {
        DateTime now = DateUtil.date();
        String today = DateUtil.today();
        DateTime thresholdBefore = DateUtil.parse(today + " " + sleepNoFetchDateTimeRange.get(0));
        DateTime thresholdAfter = DateUtil.parse(today + " " + sleepNoFetchDateTimeRange.get(1));

        boolean beforeLowLimit = DateUtil.between(now, thresholdBefore, DateUnit.SECOND, false) >= 0; // 比下限小
        boolean afterHighLimit = DateUtil.between(now, thresholdAfter, DateUnit.SECOND, false) <= 0;  // 比上限大

        //todo: 合理应当判定今日是否交易日, 依赖tushare. 等有其他更好方式再替换.
        if (!TushareApi.isTradeDate(DateUtil.format(now, "yyyyMMdd"))) {
            if (!logged) {
                log.warn("date decide: 今日非交易日,应当抓取上一交易日数据");
                logged = true;
            }
            this.saveTableName = EmQuoteApi.getPreTradeDateStrict(today).replace("-", "");
        }

        if (beforeLowLimit) {
            this.saveTableName = EmQuoteApi.getPreTradeDateStrict(today).replace("-", "");
        } else if (afterHighLimit) {
            this.saveTableName = today.replace("-", "");
        } else {
            // 此时介于两者之间, 应当等待到今日开始的时间阈值
            this.saveTableName = null; // 设置空表示应当sleep
        }
    }


    private static CopyOnWriteArraySet<String> alreadyCreatedTableName = new CopyOnWriteArraySet<>();

    /**
     * 手动建表, 以防多线程  df.to_sql 错误;  全字段索引
     *
     * @param saveTableName
     * @throws Exception
     */
    private static void createSaveTable(String saveTableName) throws Exception {
        if (alreadyCreatedTableName.contains(saveTableName)) {
            return; // 建过的表无视
        }
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
        alreadyCreatedTableName.add(saveTableName);
        log.info("create table: 创建数据表成功: {}", saveTableName);
    }


    private static void initThreadPool(int threadPoolCorePoolSize) {
        if (threadPoolOfFetch == null || threadPoolOfFetch.isShutdown()) {
            threadPoolOfFetch = new ThreadPoolExecutor(threadPoolCorePoolSize,
                    threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    ThreadUtil.newNamedThreadFactory("FSTransFtPool-", null, true)
            );
            log.debug("init threadPoolOfFetch: 初始化Fetch线程池完成,核心线程数量: {}", threadPoolCorePoolSize);
        }
        if (threadPoolOfSave == null || threadPoolOfSave.isShutdown()) { // 保存线程相同数量, 否则将成为瓶颈
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
        waitStopFetchAndSaveSuccess();
    }


    /**
     * 等待关闭完成, 等待关闭时那一轮执行完成
     */
    private void waitStopFetchAndSaveSuccess() {
        try {
            waitUtil(() -> threadPoolOfFetch.isTerminated(), Integer.MAX_VALUE, 10, null, false);
            waitSaveOk();
            waitUtil(() -> !this.running, Integer.MAX_VALUE, 10, null, false);
            log.warn("FsTransactionFetcher: stop fetch and save success");
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void waitFirstEpochFinish() throws TimeoutException, InterruptedException {
        waitUtil(() -> this.getFirstTimeFinish().get(), Integer.MAX_VALUE, 10, "第一次tick数据抓取完成");
    }


    /**
     * 等待保存线程池保存任务彻底完成, 注意, 需要先调用 stopFetch(), 将使得线程池 shutDown, 才能调用 isTerminated()
     * 这是线程池 isTerminated() 所要求
     *
     * @throws TimeoutException
     * @throws InterruptedException
     */
    private void waitSaveOk() throws TimeoutException, InterruptedException {
        waitUtil(() -> threadPoolOfSave.isTerminated(), 200000, 10, null, false);
    }


    private static class FetchOneStockTask implements Callable<Boolean> {
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
        public Boolean call() {
            String process = fetcher.getProcesses().get(stock); // 进度, time_tick表示
            DataFrame<Object> dataOriginal = fsTransactionDatas.get(stock); // 原全数据.
            // 计算一个合适的数量, 用当前时间 - 进度 的 秒数 / 3 == 数据数量,  外加 n 条冗余!
            int suitableCounts = (int) (calcCountsBetweenNowAndProcess(process) + fetcher.getRedundancyRecords());

            DataFrame<Object> dfNew = EmQuoteApi.getFSTransaction(suitableCounts, stock, 1, fetcher.timeout);
            if (dfNew == null) {
                return false;
            }

            // 将新df 中, 在 旧df中的 time_tick全部删除, 然后拼接更新的df
            HashSet<String> timeTicksOrginal = new HashSet<>(DataFrameS.getColAsStringList(dataOriginal,
                    "time_tick"));
            // @noti: 分时有序: 取决于初始化时排序, 且纯新增数据有序后连接
            DataFrame<Object> dfTemp = dfNew.select(value -> !timeTicksOrginal.contains(value.get(2).toString()))
                    .sortBy("time_tick");
            // 实测使用concat不比遍历添加行慢
            DataFrame<Object> dfCurrentAll = dataOriginal.concat(dfTemp);
            // 有序判定
            // Console.log(dfCurrentAll.col("time_tick").equals(dfCurrentAll.sortBy("time_tick").col("time_tick")));

            fsTransactionDatas.put(stock, dfCurrentAll); // 真实更新

            if (dfTemp.length() > 0) { // 若存在纯新数据, 保存到数据库
                threadPoolOfSave.submit(() -> {
                    try { // 保存使用另外线程池, 不阻塞主线程池,因此若从数据库获取数据, 显然有明显延迟.应从静态属性读取内存中数据
                        DataFrameS.toSql(dfTemp, fetcher.saveTableName, connSave, "append", null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }

            Optional<String> processNew = // 更新进度
                    (DataFrameS.getColAsStringList(dfCurrentAll, "time_tick")).stream()
                            .max(Comparator.naturalOrder());
            if (processNew.isPresent()) {
                fetcher.getProcesses().put(stock, processNew.get());
            } else {
                // 例如停牌则没有数据
                fetcher.getProcesses().put(stock, "00:00:00");
            }
            log.debug("updated: 已更新数据及进度: {} --> {} --> {} ", stock, dfCurrentAll.length(), processNew);
            return true;
        }

        private long calcCountsBetweenNowAndProcess(String process) {
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

    /*
    数据获取api, 均需要INSTANCE实例化后
     */

    /**
     * 给定股票/指数,获取其全部数据df
     *
     * @param stockOrIndex
     * @return 股票数据df; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static DataFrame<Object> getFsTransData(SecurityBeanEm bean) {
        return fsTransactionDatas.get(bean);
    }

    /**
     * 列参考: stock_code	 market	time_tick	price	 vol	bs
     *
     * @return 上证指数df;
     */
    public static DataFrame<Object> getShangZhengZhiShuFs() {
        return getFsTransData(SecurityBeanEm.SHANG_ZHENG_ZHI_SHU);
    }

    /**
     * 列参考: stock_code	 market	time_tick	price	 vol	bs
     *
     * @return 深证成指df;
     */
    public static DataFrame<Object> getShenZhengChengZhiFs() {
        return getFsTransData(SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI);
    }


    /**
     * 给定股票/指数,获取其df中 某列数据
     *
     * @param stockOrIndex
     * @return 股票数据df某列; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static List<Object> getColAsObjectList(SecurityBeanEm bean, Object colNameOrIndex) {
        DataFrame<Object> fsTransData = getFsTransData(bean);
        if (fsTransData == null) {
            return null;
        }
        return DataFrameS.getColAsObjectList(fsTransData, colNameOrIndex);
    }

    /**
     * 给定股票/指数,获取其df中 某列数据, 且元素转换为 Double 类型
     *
     * @param stockOrIndex
     * @return 某列List<Double>; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static List<Double> getColByColNameOrIndexAsDouble(SecurityBeanEm bean,
                                                              Object colNameOrIndex) {
        DataFrame<Object> fsTransData = getFsTransData(bean);
        if (fsTransData == null) {
            return null;
        }
        return DataFrameS.getColAsDoubleList(fsTransData, colNameOrIndex);
    }

    /**
     * 给定股票/指数,获取其df中 某列数据, 且元素转换为 String 类型
     *
     * @param stockOrIndex
     * @return 某列List<String>; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static List<String> getColByColNameOrIndexAsString(SecurityBeanEm bean,
                                                              Object colNameOrIndex) {
        DataFrame<Object> fsTransData = getFsTransData(bean);
        if (fsTransData == null) {
            return null;
        }
        return DataFrameS.getColAsStringList(fsTransData, colNameOrIndex);
    }

    /**
     * 给定股票/指数,获取其df中 某列数据, 且元素转换为 Long 类型
     *
     * @param stockOrIndex
     * @return 某列List<Long>; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static List<Long> getColByColNameOrIndexAsLong(SecurityBeanEm bean,
                                                          Object colNameOrIndex) {
        DataFrame<Object> fsTransData = getFsTransData(bean);
        if (fsTransData == null) {
            return null;
        }
        return DataFrameS.getColAsLongList(fsTransData, colNameOrIndex);
    }

    /**
     * 给定股票/指数,获取其df中 某列数据, 且元素转换为 Integer 类型
     *
     * @param stockOrIndex
     * @return 某列List<Integer>; 列参考: stock_code	 market	time_tick	price	 vol	bs
     */
    public static List<Integer> getColByColNameOrIndexAsInteger(SecurityBeanEm bean,
                                                                Object colNameOrIndex) {
        DataFrame<Object> fsTransData = getFsTransData(bean);
        if (fsTransData == null) {
            return null;
        }
        return DataFrameS.getColAsIntegerList(fsTransData, colNameOrIndex);
    }

    /**
     * 给定股票/指数,获取最新成交价格. 即最后一条成交记录的新价格.
     * 指数则是最新指数点数
     *
     * @param stock
     * @return 最后一条成交记录的新价格;; 列参考: stock_code	market	time_tick	price	 vol	bs
     */
    public static Double getNewestPrice(SecurityBeanEm bean) {
        List<Object> price = getColAsObjectList(bean, "price");
        if (price == null || price.size() == 0) {
            return null;
        }
        return Double.valueOf(price.get(price.size() - 1).toString());
    }

}
