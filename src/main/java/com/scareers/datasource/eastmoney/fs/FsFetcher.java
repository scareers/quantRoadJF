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
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.scareers.utils.CommonUtil.waitEnter;

/**
 * description: 参考 FsTransactionFetcher, 逻辑更为简单. 不同的是, 并不保存到数据库! 因分时图访问过于简单
 * 注意: 当某分钟开始后, fs将更新到 当分钟.
 * 例如当前 13:21:10, 则将更新到 13:22
 */
@Data
public class FsFetcher {
    public static void main(String[] args) throws Exception {
        FsFetcher fsFetcher = getInstance
                (new StockPoolFromTushare(0, 10, true).createStockPool(),
                        1000,
                        10, 16);

        fsFetcher.startFetch(); // 测试股票池
        waitEnter();
    }

    private static FsFetcher INSTANCE;

    public static FsFetcher getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    public static FsFetcher getInstance(List<SecurityBeanEm> stockPool,
                                        int timeout, int logFreq,
                                        int threadPoolCorePoolSize)
            throws SQLException, InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new FsFetcher(stockPool, timeout, logFreq,
                    threadPoolCorePoolSize);
        }
        return INSTANCE;
    }


    // 静态属性 设置项
    // 7:00之前记为昨日,抓取数据存入昨日数据表. 09:00以后抓取今日, 期间程序sleep,等待到 09:00. 需要 0<1
    public static final List<String> newDayTimeThreshold = Arrays.asList("08:00", "09:00");
    public static ThreadPoolExecutor threadPoolOfFetch;
    private static final Log log = LogUtil.getLogger();

    // 实例属性
    // 保存每只股票今日分时数据. 每次运行将删除原数据库, 然后对单只股票, 进行append语义插入
    private volatile ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> fsDatas;
    private volatile AtomicBoolean firstTimeFinish; // 标志第一次抓取已经完成
    private volatile boolean stopFetch; // 可非强制停止抓取, 但并不释放资源

    // tick获取时间上限, 本身只用于计算 当前应该抓取的tick数量
    private int timeout; // 单个http访问超时毫秒
    private final List<SecurityBeanEm> stockPool;
    private String saveTableName; // 保存数据表名称
    private final int logFreq; // 分时图抓取多少次,log一次时间
    public int threadPoolCorePoolSize; // 线程池数量

    private FsFetcher(List<SecurityBeanEm> stockPool, int timeout, int logFreq, int threadPoolCorePoolSize)
            throws SQLException, InterruptedException {
        // 4项全默认值
        this.fsDatas = new ConcurrentHashMap<>();
        this.firstTimeFinish = new AtomicBoolean(false); //
        this.stopFetch = false;

        // yyyy-MM-dd HH:mm:ss.SSS // 决定保存数据表
        this.saveTableName = DateUtil.format(DateUtil.date(), "yyyyMMdd"); // 今日, tushare 通用格式
        if (newDayDecide()) {
            this.saveTableName = TushareApi.getPreTradeDate(saveTableName); // 查找上一个交易日 作为数据表名称
        }

        // 4项可设定
        this.stockPool = stockPool;
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
        fsFetchTask.setName("FSFetcher");
        fsFetchTask.start();
        log.warn("FSTransFetcher start: 开始持续获取实时成交数据");
    }

    private void startFetch0() throws Exception {
        initThreadPool(threadPoolCorePoolSize); // 懒加载一次

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
                log.warn("fs finish first: 首次抓取完成...");
                firstTimeFinish.compareAndSet(false, true); // 第一次设置true, 此后设置失败不报错
            }
            if (epoch % logFreq == 0) {
                epoch = 0;
                log.info("fs finish timing: 共{}轮抓取结束,耗时: {} s", logFreq, ((double) timer.intervalRestart()) / 1000);
            }
            /*
             */
        }
        threadPoolOfFetch.shutdown();
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


    public static void initThreadPool(int threadPoolCorePoolSize) {
        if (threadPoolOfFetch == null) {
            threadPoolOfFetch = new ThreadPoolExecutor(threadPoolCorePoolSize,
                    threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    ThreadUtil.newNamedThreadFactory("FSFetcherPool-", null, true)
            );
            log.debug("init threadPoolOfFetch: 初始化Fetch线程池完成,核心线程数量: {}", threadPoolCorePoolSize);
        }
    }

    public void stopFetch() {
        // 关闭线程池即可
        stopFetch = true;
    }

    public static class FetchOneStockTask implements Callable<Void> {
        SecurityBeanEm stock;
        FsFetcher fetcher;

        public FetchOneStockTask(SecurityBeanEm stock, FsFetcher fetcher) {
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
            boolean isIndex = false;
            if (stock.getConvertState() == SecurityBeanEm.ConvertState.INDEX) {
                isIndex = true;
            } else if (stock.getConvertState() == SecurityBeanEm.ConvertState.STOCK) {
                isIndex = false;
            } else {
                throw new Exception("SecurityBeanEm stock --> 尚未转换为指数或者个股!");
            }
            DataFrame<Object> dfNew = StockApi.getQuoteHistorySingle(stock.getStockCodeSimple(), null, null, "1",
                    "qfq", 3, isIndex, fetcher.getTimeout(), false);
            fetcher.fsDatas.put(stock, dfNew); // 直接替换数据.
            return null;
        }
    }
}
