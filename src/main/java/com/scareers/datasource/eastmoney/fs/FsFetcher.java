package com.scareers.datasource.eastmoney.fs;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.datasource.eastmoney.stockpoolimpl.StockPoolFromTushare;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.getColAsObject;
import static com.scareers.utils.CommonUtil.waitForever;
import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: 给定股票池, 抓取 1分钟分时数据, 保存于 fsDatas 属性
 *
 * @author admin
 * @noti 字段列表: 日期	            开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
 * @noti 数据实例: 2022-01-25 09:31	17.08	17.02	17.08	17.02	11145	19006426.00	0.35	-1.05	-0.18	0.01	000001	平安银行
 * @noti 当某分钟开始后(即0秒以后, fs将更新到当分钟 + 1. 例如当前 13 : 21 : 10, 则将更新到 13 : 22
 * @noti 集合竞价结果将于 09:25:xx 更新, 作为第一条分时图字段. 且时间固定为 9:31, 当9:30:xx后, 价格将刷新, 但时间依旧9:31;
 * 直到时间超过 9:31:xx, 将出现第二条记录 9:32
 * 即: 集合竞价与 9:30:xx 均作为 9:31 这一条fs K线图(不断更新).
 * @noti 时间字段完整格式:  yyyy-MM-dd HH:mm       // DatePattern.NORM_DATETIME_MINUTE_PATTERN
 * @noti 单次访问http将 retry==0; 失败返回null,不进行重试
 * @noti 内部 stockPool 自动添加两大指数, 且去重, 线程安全, 保证可以动态添加, 减少股票
 * @warning 因1分分时图api十分方便, 且并不保存到数据库, 且全量更新, 因此不对启动时间做过多限制. 启动时间由调用方保证合理
 * @noti y
 * @see StockApi.getFs1MToday()
 */
@Data
public class FsFetcher {
    public static void main(String[] args) throws Exception {
        FsFetcher fsFetcher = getInstance
                (new StockPoolFromTushare(0, 10, true).createStockPool(),
                        1000,
                        10, 16, 100);
        Console.log(stockPool);
        fsFetcher.startFetch(); // 测试股票池
        fsFetcher.waitFirstEpochFinish();
        fsFetcher.stopFetch(); // 软停止,且等待完成
        fsFetcher.reStartFetch(); // 再次开始
        fsFetcher.waitFirstEpochFinish();

        Console.log(fsFetcher.getFsDatas().size());
        Console.log(fsFetcher.getStockPool().size());

        Console.log(FsFetcher.getShangZhengZhiShuDf());
        Console.log(FsFetcher.getShenZhengChengZhiDf());

        SecurityBeanEm stock = SecurityBeanEm.createStock("000002");
        DataFrame<Object> dataFrame = FsFetcher.getDf(stock).orElse(null);
        Console.log(dataFrame);
        Console.log(FsFetcher.getCertainLastClosePrice(stock));
        Console.log(FsFetcher.getValueByTimeTick(stock, "9:31:59", 2, false));
        Console.log(FsFetcher.getClosePriceByTimeTick(stock, "9:32"));
        Console.log(DataFrameS.getColAsDoubleList(dataFrame, "开盘"));

        Console.log(FsFetcher.getColumnByColNameOrIndex(stock, 0));
        Console.log(FsFetcher.getColumnByColNameOrIndex(stock, "开盘"));
        Console.log(FsFetcher.getColumnByColNameOrIndexAsDouble(stock, "开盘"));
        Console.log(FsFetcher.getColumnByColNameOrIndexAsString(stock, "日期"));

//        fsFetcher.stopFetch(); // 软停止,且等待完成

        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                String preStr = "";
                while (true) {
//                    getFsDatas().values().stream().forEach(value -> Console.log(value.row(value.length() - 1)));
                    DataFrame<Object> data = getData(SecurityBeanEm.createStock("000002"));
                    String x = data.get(data.length() - 1, 0).toString();
                    if (!x.equals(preStr)) {
                        log.warn(x);
                    }
                    preStr = x;
                }
            }
        }, true);
        waitForever();

    }

    public static ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> getFsDatas() {
        return fsDatas;
    }

    public static DataFrame<Object> getData(SecurityBeanEm bean) {
        return fsDatas.get(bean);
    }

    public static List<SecurityBeanEm> getStockPool() {
        return stockPool;
    }

    /**
     * 单例模式
     */
    private static FsFetcher INSTANCE;

    public static FsFetcher getInstance() {
        Objects.requireNonNull(INSTANCE);
        return INSTANCE;
    }

    public static void waitInstanceNotNull() {
        try {
            waitUtil(() -> INSTANCE != null, Integer.MAX_VALUE, 1, null, false);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static FsFetcher getInstance(List<SecurityBeanEm> stockPool,
                                        int timeout, int logFreq,
                                        int threadPoolCorePoolSize,
                                        int sleepPerEpoch) {
        if (INSTANCE == null) {
            INSTANCE = new FsFetcher(stockPool, timeout, logFreq,
                    threadPoolCorePoolSize, sleepPerEpoch);
        }
        return INSTANCE;
    }


    // 静态属性
    private static final Log log = LogUtil.getLogger();
    public static ThreadPoolExecutor threadPoolOfFetch;

    // 实例属性
    public static ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> fsDatas; // 数据Map
    private volatile AtomicBoolean firstTimeFinish; // 标志第一次抓取已经完成
    private volatile boolean stopFetch; // 可非强制停止抓取, 但并不释放资源. 将等待正在进行的一轮结束后停止.可再次调用 startFetch()启动

    private int timeout; // 单次http访问超时毫秒
    private static List<SecurityBeanEm> stockPool; // 股票池需要提供
    private final int logFreq; // 多少轮,log 一次时间
    public int threadPoolCorePoolSize; // 线程池核心数量
    private int sleepPerEpoch; // 每轮后强制 sleep;
    private volatile boolean running; //标志正在抓取中

    private FsFetcher(List<SecurityBeanEm> stockPool0, int timeout, int logFreq, int threadPoolCorePoolSize,
                      int sleepPerEpoch) {
        // 4项全默认值
        fsDatas = new ConcurrentHashMap<>();
        this.firstTimeFinish = new AtomicBoolean(false); //
        this.stopFetch = false;
        this.sleepPerEpoch = sleepPerEpoch;

        // 4项可设定
        HashSet<SecurityBeanEm> temp = new HashSet<>(stockPool0);
        temp.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        stockPool = new CopyOnWriteArrayList<>(temp);
        this.timeout = timeout; // 1000
        this.logFreq = logFreq;
        this.threadPoolCorePoolSize = threadPoolCorePoolSize;
        this.running = false;
        for (SecurityBeanEm stock : stockPool) {
            fsDatas.put(stock, new DataFrame<>()); // 数据初始化置空, 使得不会访问到null, 最多空df
        }
    }

    /**
     * 可停止后重启
     */
    public void reStartFetch() {
        if (this.running) {
            log.error("FSFetcher 正在运行中, 无法再次开始, 调用 stopFetch() 后可停止, 方可再次开始");
            return;
        }
        startFetch();
    }

    /**
     * 首次启动, 新建线程启动, 调用方自行sleep, 否则将结束
     */
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
        fsFetchTask.setName("FS1MFetcher");
        fsFetchTask.start();
    }

    /**
     * 核心方法
     *
     * @throws Exception
     */
    private void startFetch0() throws Exception {
        this.running = true;
        initThreadPool(threadPoolCorePoolSize); // 懒加载一次
        log.warn("FS1MFetcher start: 开始持续获取 [1分钟分时] 数据,股票池数量: {}", stockPool.size());
        TimeInterval timer = DateUtil.timer();
        timer.start();
        int epoch = 0; // 抓取轮次, 控制 log 频率
        while (!stopFetch) {
            epoch++;
            Boolean epochAllSuccess = true; // 本轮http全部成功

            List<Future<Boolean>> futures = new ArrayList<>();
            for (SecurityBeanEm stock : stockPool) {
                Future<Boolean> f = threadPoolOfFetch
                        .submit(new FetchOneStockTask(stock, this));
                futures.add(f);
            }

            for (Future<Boolean> future : futures) {
                Boolean reqSuccess = future.get();
                if (!reqSuccess) {
                    epochAllSuccess = false;
                }
            }
            if (!firstTimeFinish.get() && epochAllSuccess) { // 需要第一轮全部抓取成功,否则延后
                log.warn("fs_1m finish first: 首次抓取完成...");
                firstTimeFinish.compareAndSet(false, true); // 第一次成功设置true
            }
            if (epoch % logFreq == 0) {
                epoch = 0;
                log.info("fs_1m finish timing: 共{}轮抓取结束,耗时: {} s", logFreq, ((double) timer.intervalRestart()) / 1000);
            }
            Thread.sleep(sleepPerEpoch);
        }
        stopFetch = false; // 被停止后, 将stopFetch恢复为默认值false
        this.firstTimeFinish = new AtomicBoolean(false); // 首次完成flag也重置
        threadPoolOfFetch.shutdown();
        this.running = false; // 标志位
    }

    /**
     * 线程池初始化
     *
     * @param threadPoolCorePoolSize
     */
    private static void initThreadPool(int threadPoolCorePoolSize) {
        if (threadPoolOfFetch == null || threadPoolOfFetch.isTerminated()) {
            threadPoolOfFetch = new ThreadPoolExecutor(threadPoolCorePoolSize,
                    threadPoolCorePoolSize * 2, 10000, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    ThreadUtil.newNamedThreadFactory("FS1MFetcherPool-", null, true)
            );
            log.debug("init FS1MFetcherThreadPool: 核心线程数量: {}", threadPoolCorePoolSize);
        }
    }

    /**
     * 软关闭死循环http访问的抓取逻辑. 且关闭线程池. 下次start将重新初始化线程池
     */
    public void stopFetch() {
        stopFetch = true;
        waitStopFetchSuccess();
    }

    /**
     * 等待关闭完成, 等待关闭时那一轮执行完成
     */
    private void waitStopFetchSuccess() {
        try {
            waitUtil(() -> threadPoolOfFetch.isTerminated(), Integer.MAX_VALUE, 10, null, false);
            waitUtil(() -> !this.running, Integer.MAX_VALUE, 10, null, false);
            log.warn("FsFetcher: stop fetch success");
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 等待第一轮抓取完成(要求该轮所有http访问均成功)
     *
     * @param timeout 最多等待时间
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public void waitFirstEpochFinish() throws TimeoutException, InterruptedException {
        waitUtil(() -> this.getFirstTimeFinish().get(), Integer.MAX_VALUE, 10, "第一次tick数据抓取完成");
    }


    private static class FetchOneStockTask implements Callable<Boolean> {
        SecurityBeanEm stock;
        FsFetcher fetcher;

        public FetchOneStockTask(SecurityBeanEm stock, FsFetcher fetcher) {
            this.stock = stock;
            this.fetcher = fetcher;
        }

        /**
         * 单只股票抓取逻辑. 全量更新.
         * 返回值 true表示成功获取最新数据(即使没有新的行). false表示 http 访问失败
         *
         * @return
         * @throws Exception
         */
        @Override
        public Boolean call() throws Exception {
            boolean isIndex;
            if (stock.getSecType() == SecurityBeanEm.SecType.INDEX) {
                isIndex = true;
            } else if (stock.getSecType() == SecurityBeanEm.SecType.STOCK) {
                isIndex = false;
            } else {
                throw new Exception("SecurityBeanEm stock --> 尚未转换为指数或者个股!");
            }

            DataFrame<Object> dfNew = StockApi
                    .getFs1MToday(stock.getSecCode(), SecurityBeanEm.SecType.STOCK, 0, fetcher.getTimeout(),
                            false);
            if (dfNew != null) { // 访问失败将返回null.
//                ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> datasNew = new ConcurrentHashMap<>(
//                        fsDatas);
//                datasNew.put(stock, dfNew); // 直接替换数据.
                fsDatas.put(stock, dfNew);
//                fsDatas = datasNew;
                return true; // 成功更新为最新
            }
            return false; // http访问失败
        }
    }

    /*
    数据获取相关api, 一般需要给定 stock, 从数据池中获取相应数据
     */

    /**
     * 列索引参考: 日期 开盘	收盘 最高	最低	成交量	成交额	    振幅 涨跌幅	涨跌额  换手率	股票代码	股票名称
     *
     * @param stockOrIndex
     * @return 单股票/指数今日完整分时图 df;
     */
    public static Optional<DataFrame<Object>> getDf(SecurityBeanEm stockOrIndex) {
        DataFrame<Object> res = fsDatas.get(stockOrIndex);
        if (res == null || res.length() == 0) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

    /**
     * 列索引参考: 日期 开盘	收盘 最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     *
     * @param stockCodeSimple 6为简单股票代码,必须股票,不可指数
     * @return 获取单股票 完整分时图df;
     */
    public static Optional<DataFrame<Object>> getDf(String stockCodeSimple) {
        SecurityBeanEm stock;
        try {
            stock = SecurityBeanEm.createStock(stockCodeSimple);
        } catch (Exception e) {
            log.error("getDf: 从股票代码创建 SecurityBeanEm 对象失败, 返回空");
            return Optional.empty();
        }
        return getDf(stock);
    }

    /**
     * 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     *
     * @return 上证指数df;
     */
    public static Optional<DataFrame<Object>> getShangZhengZhiShuDf() {
        return getDf(SecurityBeanEm.SHANG_ZHENG_ZHI_SHU);
    }

    /**
     * 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     *
     * @return 深证成指df;
     */
    public static Optional<DataFrame<Object>> getShenZhengChengZhiDf() {
        return getDf(SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI);
    }

    /**
     * 本方法获取 已经确定的最后一个close, 本质是获取 9:40 这一行的close. 即倒数第二行.
     * 直接采用倒数 2行, 当然 9:31:00之前 返回 唯一一行的close;;
     * 因该api底层 9:40:xx 将显示 到 9:41 的分时行.
     * 不采用传递 nowStr的方式, 这样即使相差n微秒(极短时间), 也能获取真正的 确定了的 最后1分时k线的 close
     * 也不采用获取当前时间去秒数的方式
     * 直接采用倒数 2行, 更符合确定的含义 - CertainLastClose
     *
     * @param stockOrIndex
     * @return 返回已确定的最后一个close价格, 常常为倒数第二行
     * @deprecated 建议不直接调用此方法. 调用 getClosePriceByTimeTick 获取更加准确
     */
    public static Optional<Double> getCertainLastClosePrice(SecurityBeanEm stockOrIndex) {
        Optional<DataFrame<Object>> dfTemp = getDf(stockOrIndex);
        if (dfTemp.isPresent()) {
            List<Object> closes = dfTemp.get().col("收盘");
            return Optional.of(Double.valueOf(closes.get(Math.max(0, closes.size() - 1)).toString()));
        }
        return Optional.empty();
    }

    /**
     * @param stockOrIndex 股票
     * @param tickStr      NORM_DATETIME_MINUTE_PATTERN 或者 hutool.DateUtil 能够转换的其他形式,用到小时和分钟意义
     * @return 给定时间tick的close价格, 默认倒序查找, 正序查找请传递 reverseFind=false
     */
    public static Optional<Double> getClosePriceByTimeTick(SecurityBeanEm stockOrIndex, String tickStr) {
        return getClosePriceByTimeTick(stockOrIndex, tickStr, true);
    }

    /**
     * @param stockOrIndex 股票
     * @param tickStr      NORM_DATETIME_MINUTE_PATTERN 或者 hutool.DateUtil 能够转换的其他形式
     * @param reverseFind  可指定不使用倒序,而正序查找
     * @return 给定时间tick的close价格, 默认倒序查找, 正序查找请传递 reverseFind=false
     */
    public static Optional<Double> getClosePriceByTimeTick(SecurityBeanEm stockOrIndex, String tickStr,
                                                           boolean reverseFind) {
        Optional<Object> valueByTimeTick = getValueByTimeTick(stockOrIndex, tickStr, 2, reverseFind);
        if (valueByTimeTick.isEmpty()) {
            return Optional.empty();
        }
        Double res;
        try {
            res = Double.valueOf(valueByTimeTick.get().toString());
        } catch (Exception e) {
            log.error("getClosePriceByTimeTick: 或者值成功但转换Double失败, 返回 empty");
            return Optional.empty();
        }
        return Optional.of(res);
    }

    /**
     * @param stockOrIndex 股票/指数 SecurityBeanEm 对象
     * @param tickStr      时间tick, 要求格式: 2022-01-25 09:31, 或者 hutool.DateUtil 能够转换的其他形式, 可自动设定日期为今天
     * @param colIndex     列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @param reverseFind  正序或者反向遍历, 可根据情况提高性能
     * @return 给定股票/指数, 时间戳字符串, 列索引序号, 查找对应的值 Object 返回; 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     */
    public static Optional<Object> getValueByTimeTick(SecurityBeanEm stockOrIndex, String tickStr, int colIndex,
                                                      boolean reverseFind) {
        String tickStrSmart;
        try { // 智能转换
            tickStrSmart = DateUtil.parse(tickStr).toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("getValueByTimeTick fail: tick时间字符串参数错误, 建议形式: yyyy-MM-dd HH:mm");
            return Optional.empty();
        }
        Optional<DataFrame<Object>> dfTemp = getDf(stockOrIndex);

        if (dfTemp.isPresent()) {
            DataFrame<Object> dataFrame = dfTemp.get();
            if (reverseFind) {
                for (int i = dataFrame.length() - 1; i >= 0; i--) {
                    List<Object> line = dataFrame.row(i);
                    if (line.get(0).toString().equals(tickStrSmart)) {
                        return Optional.of(Double.valueOf(line.get(colIndex).toString()));
                    }
                }
            } else {
                for (int i = 0; i < dataFrame.length(); i++) {
                    List<Object> line = dataFrame.row(i);
                    if (line.get(0).toString().equals(tickStrSmart)) {
                        return Optional.of(Double.valueOf(line.get(colIndex).toString()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 给定股票bean和列名, 返回对应列.
     *
     * @param stockOrIndex   SecurityBeanEm
     * @param colNameOrIndex 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @return List<Object> 整列数据
     */
    public static Optional<List<Object>> getColumnByColNameOrIndex(SecurityBeanEm stockOrIndex,
                                                                   Object colNameOrIndex) {
        return getColAsObject(colNameOrIndex, getDf(stockOrIndex), log, stockOrIndex);
    }

    /**
     * 返回 List<String> 列. 即将列元素转换为String;;
     * 使用 Optional对象 map方法
     *
     * @param stockOrIndex
     * @param colNameOrIndex 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @return
     */
    public static Optional<List<String>> getColumnByColNameOrIndexAsString(SecurityBeanEm stockOrIndex,
                                                                           Object colNameOrIndex) {
        return getColumnByColNameOrIndex(stockOrIndex, colNameOrIndex)
                .map(objects -> objects.stream().map(Object::toString).collect(Collectors.toList()));
    }

    /**
     * 返回 List<Long> 列. 即将列元素转换为 Long;;
     * 使用 Optional对象 map方法
     *
     * @param stockOrIndex
     * @param colNameOrIndex 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @return
     */
    public static Optional<List<Long>> getColumnByColNameOrIndexAsLong(SecurityBeanEm stockOrIndex,
                                                                       Object colNameOrIndex) {
        return getColumnByColNameOrIndex(stockOrIndex, colNameOrIndex)
                .map(objects -> objects.stream().map(value -> Long.valueOf(value.toString()))
                        .collect(Collectors.toList()));
    }

    /**
     * 返回 List<Integer> 列. 即将列元素转换为 Integer;;
     * 使用 Optional对象 map方法
     *
     * @param stockOrIndex
     * @param colNameOrIndex 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @return
     */
    public static Optional<List<Integer>> getColumnByColNameOrIndexAsInteger(SecurityBeanEm stockOrIndex,
                                                                             Object colNameOrIndex) {
        return getColumnByColNameOrIndex(stockOrIndex, colNameOrIndex)
                .map(objects -> objects.stream().map(value -> Integer.valueOf(value.toString()))
                        .collect(Collectors.toList()));
    }

    /**
     * 返回 List<Double> 列. 即将列元素转换为 Double;;
     * 使用 Optional对象 map方法
     *
     * @param stockOrIndex
     * @param colNameOrIndex 列索引参考: 日期 开盘	收盘	最高	最低	成交量	成交额	    振幅	涨跌幅	涨跌额  换手率	股票代码	股票名称
     * @return
     */
    public static Optional<List<Double>> getColumnByColNameOrIndexAsDouble(SecurityBeanEm stockOrIndex,
                                                                           Object colNameOrIndex) {
        return getColumnByColNameOrIndex(stockOrIndex, colNameOrIndex)
                .map(objects -> objects.stream().map(value -> Double.valueOf(value.toString()))
                        .collect(Collectors.toList()));
    }


}
