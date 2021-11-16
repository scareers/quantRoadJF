package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;
/**
 * -Xmx512g -XX:+PrintGC -XX:MaxTenuringThreshold=3
 */

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.settings.SettingsCommon;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.SqlUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent.bigChangeThreshold;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.keysfunc.KeyFuncOfSingleKlineBasePercent.*;
import static com.scareers.sqlapi.TushareApi.getAdjdatesByTscodeFromTushare;
import static com.scareers.sqlapi.TushareApi.getStockPriceByTscodeAndDaterangeAsDfFromTushare;
import static com.scareers.utils.CommonUtils.showMemoryUsageMB;
import static com.scareers.utils.HardwareUtils.reportCpuMemoryDisk;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/5  0005-15:13
 * <p>
 * 1.分析计算结果保存脚本 -- 全部数据
 */
public class SingleKlineFormsBase {
    public static Log log = LogFactory.get();

    public static void main(String[] args) throws Exception {
        MainCanExclude(args);
    }

    public static void MainCanExclude(String[] args) throws Exception {
//        List<Integer> windowUsePeriodsCoreArgList = ListUtil.of(9, 10);
        List<Integer> windowUsePeriodsCoreArgList = ListUtil.of(9);
        for (Integer windowUsePeriodsCoreArg : windowUsePeriodsCoreArgList) {
            // 不需要刷新. 批量执行需要刷新
            TimeInterval timer = DateUtil.timer();
            timer.start();

            log.info("current time");
            log.warn(StrUtil.format("start windowUsePeriodsCoreArg: {}", windowUsePeriodsCoreArg));
            // 刷新相关设定:
            SettingsOfSingleKlineBasePercent.refreshWindowUsePeriodRelativeSettings(windowUsePeriodsCoreArg);
            main0(SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg);
            log.info("current time");
            // 直接执行时, 读取设定的 周期数量
            // 在批量调用时, 调用main0, 周期数量通过 参数  windowUsePeriodsCoreArg 传递.
            // 设定的所有都不需要变, 只有 周期数需要改变
            MailUtil.send(SettingsCommon.receivers,
                    StrUtil.format("全部解析完成,windowUsePeriodsCoreArg: {}", windowUsePeriodsCoreArg),
                    StrUtil.format("全部解析完成,耗时: {}h",
                            (double) timer.intervalRestart() / 3600000),
                    false, null);
            Console.log("耗时: windowUsePeriodsCoreArg: {} , {} ", windowUsePeriodsCoreArg,
                    (double) timer.intervalRestart() / 3600000);
        }

    }

    /**
     * main0 主逻辑开始. 前部分代码可复用用于分时分析
     *
     * @param windowUsePeriodsCoreArg
     * @throws Exception
     */
    public static void main0(int windowUsePeriodsCoreArg) throws Exception {
        List<String> stocks = TushareApi.getStockListFromTushareExNotMain();
        stocks = stocks.subList(0, Math.min(SettingsOfSingleKlineBasePercent.stockAmountsBeCalc, stocks.size()));
        DataFrame<String> stockWithBoard = TushareApi.getStockListWithBoardFromTushare();

        List<List<String>> dateRanges = SettingsOfSingleKlineBasePercent.dateRanges;
        HashMap<String, List<List<String>>> stockWithStDateRanges = TushareApi.getStockWithStDateRanges();

        // 未关闭连接,可复用
        SqlUtil.execSql(SettingsOfSingleKlineBasePercent.sqlCreateSaveTable,
                SettingsOfSingleKlineBasePercent.ConnOfSaveTable, false);
        int bins = SettingsOfSingleKlineBasePercent.binsList.get(windowUsePeriodsCoreArg - 7);
        List<Double> effectiveValueRange =
                SettingsOfSingleKlineBasePercent.effectiveValusRanges.get(windowUsePeriodsCoreArg - 7);
        for (List<String> statDateRange : dateRanges) {
            Console.log("当前循环组: {}", statDateRange);
            // 不能关闭连接, 否则为 null, 引发空指针异常
            SqlUtil.execSql(
                    StrUtil.format(SettingsOfSingleKlineBasePercent.sqlDeleteExistDateRange,
                            StrUtil.format("[\"{}\",\"{}\"]", statDateRange.get(0), statDateRange.get(1))),
                    SettingsOfSingleKlineBasePercent.ConnOfSaveTable, false);

            // 7, 8用.

            statsConclusionOfBatchFormsCommons(stocks, stockWithStDateRanges, stockWithBoard, statDateRange,
                    bigChangeThreshold, bins, effectiveValueRange,
                    SettingsOfSingleKlineBasePercent.saveTablename, windowUsePeriodsCoreArg);
            String hardwareInfo = reportCpuMemoryDisk(true);
            MailUtil.send(SettingsCommon.receivers, StrUtil.format("部分解析完成: {}", statDateRange),
                    StrUtil.format("部分解析完成, 硬件信息:\n", hardwareInfo), false,
                    null);
            log.info("current time");
        }
    }


    /**
     * main: 主逻辑函数, 单date_range 的分析.
     * 1.比起python的同方法, 少了 show_cdf_figure 参数显示图片
     * 2.线程池等待结束, 最优方案是使用 countDownLatch .await()
     * 3.原则: 定义时使用List, 实例化时使用ArrayList
     *
     * @param stocks
     * @param stockWithStDateRanges
     * @param statDateRange
     * @param bigChangeThreshold
     * @param bins
     * @param effectiveValueRange
     * @param saveTablename
     */
    public static void statsConclusionOfBatchFormsCommons(List<String> stocks,
                                                          HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                          DataFrame<String> stockWithBoard,
                                                          List<String> statDateRange, List<Double> bigChangeThreshold,
                                                          int bins, List<Double> effectiveValueRange,
                                                          String saveTablename, int windowUsePeriodsCoreArg)
            throws Exception {
        Console.log("构建结果字典");
        ConcurrentHashMap<String, List<Double>> results = new ConcurrentHashMap<>(8);
        ThreadPoolExecutor poolOfParse = new ThreadPoolExecutor(SettingsOfSingleKlineBasePercent.processAmountParse,
                SettingsOfSingleKlineBasePercent.processAmountParse * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        CountDownLatch latchOfParse = new CountDownLatch(stocks.size());
        Connection connOfParse = ConnectionFactory.getConnLocalTushare();
        AtomicInteger parseProcess = new AtomicInteger(0);
        ArrayList<Future<ConcurrentHashMap<String, List<Double>>>> futuresOfParse = new ArrayList<>();
        for (String stock : stocks) {
            // 全线程使用1个conn
            Future<ConcurrentHashMap<String, List<Double>>> f = poolOfParse
                    .submit(new StockSingleParseTask(latchOfParse, stock, stockWithBoard, statDateRange,
                            stockWithStDateRanges, connOfParse, windowUsePeriodsCoreArg));
            futuresOfParse.add(f);
        }
        List<Integer> indexesOfParse = CommonUtils.range(futuresOfParse.size());
        for (Integer i : Tqdm.tqdm(indexesOfParse, StrUtil.format("{} process: ", statDateRange))) {
            Future<ConcurrentHashMap<String, List<Double>>> f = futuresOfParse.get(i);
            ConcurrentHashMap<String, List<Double>> resultTemp = f.get();
            //            synchronized (results) {
            for (String key : resultTemp.keySet()) {
                // @bugfix: value的列表应该线程安全! 而非简单的AL;
                // @bigfix2: CopyOnWriteArrayList 由于使用锁, 对象过大, 内存不足; 因此使用同步关键字
                // @noti: 按照逻辑来讲, 此处本身就是串行, 不需要同步.
                results.putIfAbsent(key, new ArrayList<>());
                results.get(key).addAll(resultTemp.get(key));
            }
            //            }
            resultTemp.clear();
            if (parseProcess.incrementAndGet() % SettingsOfSingleKlineBasePercent.gcControlEpochParse == 0) {
                System.gc();
                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
                    showMemoryUsageMB();
                }
            }
        }
        //        latchOfParse.await(); // 不需要,
        //        connOfParse.close(); // 关闭连接
        poolOfParse.shutdown(); // 关闭线程池
        System.out.println();
        Console.log("results size: {}", results.size());
        System.gc();

        Console.log("构建结果字典完成");
        Console.log("开始计算并保存");
        ArrayList<String> forNameRaws = new ArrayList<>(results.keySet());
        //        forNameRaws.sort((o1, o2) -> o1.compareTo(o2)); // 排序
        forNameRaws.sort(Comparator.naturalOrder()); // 排序, 自然顺序
        ThreadPoolExecutor poolOfCalc = new ThreadPoolExecutor(SettingsOfSingleKlineBasePercent.processAmountSave,
                SettingsOfSingleKlineBasePercent.processAmountSave * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        /**
         * 因java执行速度快, 因此考虑, 单个线程, 执行一个epoch; 而非 全进程执行一个epoch.
         * 且每次写入mysql为一个epoch的数据, 而非单条数据.
         */
        // 仅用于显示进度
        int totalEpochAmounts = (int) Math
                .ceil((double) results.size() / SettingsOfSingleKlineBasePercent.perEpochTaskAmounts);
        List<Integer> epochs = CommonUtils.range(totalEpochAmounts);
        Connection connOfSave = SettingsOfSingleKlineBasePercent.ConnOfSaveTable;
        CountDownLatch latchOfCalcForEpoch = new CountDownLatch(totalEpochAmounts);
        ArrayList<Future<List<String>>> futuresOfSave = new ArrayList<>();
        // 批量插入不伤ssd. 单条插入很伤ssd
        for (Integer currentEpoch : Tqdm
                .tqdm(epochs, StrUtil.format("{} process: ", statDateRange))) {
            int startIndex = currentEpoch * SettingsOfSingleKlineBasePercent.perEpochTaskAmounts;
            int endIndex = (currentEpoch + 1) * SettingsOfSingleKlineBasePercent.perEpochTaskAmounts;
            List<String> formNamesCurrentEpoch = forNameRaws
                    .subList(startIndex, Math.min(endIndex, forNameRaws.size()));

            Future<List<String>> f = poolOfCalc.submit(new CalcStatResultAndSaveTask(latchOfCalcForEpoch,
                    connOfSave, formNamesCurrentEpoch,
                    stocks.size(), statDateRange, results, bigChangeThreshold, bins, effectiveValueRange,
                    saveTablename));
            futuresOfSave.add(f);

        }
        AtomicInteger saveProcess = new AtomicInteger(0);
        for (Integer i : Tqdm
                .tqdm(epochs, StrUtil.format("{} process: ", statDateRange))) {
            Future<List<String>> f = futuresOfSave.get(i);
            List<String> finishedFormNames = f.get();
            for (String formName0 : finishedFormNames) {
                // 删除key, 节省空间
                results.remove(formName0);
            }
            //            if (parseProcess.incrementAndGet() % SettingsOfSingleKlineBasePercent.gcControlEpoch == 0) {
            //                System.gc();
            //                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
            //                    showMemoryUsageMB();
            //                }
            //            }
        }

        Console.log("计算并保存完成!");
        latchOfCalcForEpoch.await();
        //        connOfSave.close(); // 不可关闭, 因底层默认会重新获取到null连接
        poolOfCalc.shutdown();
        // 本轮执行完毕
    }

    public static class CalcStatResultAndSaveTask implements Callable<List<String>> {
        private CountDownLatch latchOfCalcForEpoch;
        // 每轮计数, 多出来.  而少了图片显示参数
        private Connection connOfSingleThread;

        // 注意是单个批量的forms(单个线程执行一个批量)

        private List<String> formNameRaws;
        private int statStockCounts;
        private List<String> statDateRange;
        // 单形态的统计列表

        private ConcurrentHashMap<String, List<Double>> results;
        private List<Double> bigChangeThreshold;
        private int bins;
        private List<Double> effectiveValueRange;
        private String saveTablename;

        public CalcStatResultAndSaveTask(CountDownLatch latchOfCalcForEpoch, Connection connOfSingleThread,
                                         List<String> formNameRaws, int statStockCounts,
                                         List<String> statDateRange,
                                         ConcurrentHashMap<String, List<Double>> singleResult,
                                         List<Double> bigChangeThreshold, int bins,
                                         List<Double> effectiveValueRange, String saveTablename) {
            this.latchOfCalcForEpoch = latchOfCalcForEpoch;
            this.connOfSingleThread = connOfSingleThread;

            this.formNameRaws = formNameRaws;
            this.statStockCounts = statStockCounts;
            this.statDateRange = statDateRange;
            this.results = singleResult;
            this.bigChangeThreshold = bigChangeThreshold;
            this.bins = bins;
            this.effectiveValueRange = effectiveValueRange;
            this.saveTablename = saveTablename;
        }

        @Override
        public List<String> call() throws Exception {
            try {
                DataFrame<Object> dfTotalSave = null;
                HashMap<String, HashMap<String, Object>> analyzeResultMapTotal =
                        analyzeStatsResults(SettingsOfSingleKlineBasePercent.calcCdfAndFrequencyWithTick);
                for (String formName : formNameRaws) {
                    HashMap<String, Object> analyzeResultMap = analyzeResultMapTotal.get(formName);
                    if (analyzeResultMap == null) { // 单条分析是可能为null的
                        continue;
                    }
                    // 精细分析也不需要保存 cdfwithtick. 过于冗余
                    // 已经得到 分析结果, 需要注意 Map的Value 实际类别各不相同. 保存时需要一一对应
                    int splitIndex = formName.lastIndexOf("__");
                    String formNamePure = formName.substring(0, splitIndex);
                    String statResultAlgorithm = formName.substring(splitIndex + 2);
                    List<String> conditions = StrUtil.split(formNamePure, "__");
                    String condition1 = null;
                    String condition2 = null;
                    String condition3 = null;
                    String condition4 = null;
                    String condition5 = null;
                    String condition6 = null;
                    String condition7 = null;

                    for (String condition : conditions) {
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(0))) {
                            condition1 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(1))) {
                            condition2 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(2))) {
                            condition3 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(3))) {
                            condition4 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(4))) {
                            condition5 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(5))) {
                            condition6 = condition;
                        }
                        if (condition.startsWith(SettingsOfSingleKlineBasePercent.conditionNames.get(6))) {
                            condition7 = condition;
                        }
                    }

                    DataFrameSelf<Object> dfSingleSaved = prepareSaveDfForAnalyzeResult(analyzeResultMap, formNamePure,
                            statDateRange,
                            statResultAlgorithm, "",
                            condition1, condition2,
                            condition3, condition4, condition5, condition6, condition7);

                    //                synchronized (this) {
                    // 同步this, 单个线程的任务, 拼接总df.
                    if (dfTotalSave == null) { // 单个线程中, 是串行的, 不需要同步
                        dfTotalSave = dfSingleSaved;
                    } else {
                        dfTotalSave = dfTotalSave.concat(dfSingleSaved);
                    }
                    //                }

                    //                synchronized (this) {
                    //                    // 同步this, 单个线程的任务, 拼接总df.
                    //                    if (dfTotalSave == null) {
                    //                        dfTotalSave = dfSingleSaved;
                    //                    } else {
                    //                        dfTotalSave = dfTotalSave.concat(dfSingleSaved);
                    //                    }
                    //                }
                    //results.remove(formName);
                    // 这里直接删除了, 则 主线程不需要读取返回值的 列表, 进行删除. 因此可使用latch完成等待, 而非 f.get()
                    //或者: 返回后统一删除key.
                }
                // dfTotalSave 应当转换为 self
                DataFrameSelf.toSql(dfTotalSave, saveTablename, connOfSingleThread, "append", null);
                return formNameRaws;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latchOfCalcForEpoch.countDown();
                if (latchOfCalcForEpoch.getCount() % SettingsOfSingleKlineBasePercent.gcControlEpochSave == 0) {
                    System.gc();
                    if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
                        showMemoryUsageMB();
                    }
                }
                return formNameRaws;
            }
        }

        public HashMap<String, HashMap<String, Object>> analyzeStatsResults(boolean calcCdfOrFrequencyWithTick) {
            return analyzeStatsResultsStatic(formNameRaws, results, statStockCounts, bigChangeThreshold, bins,
                    effectiveValueRange, calcCdfOrFrequencyWithTick);
        }

    }


    /**
     * -- 面向对象与面向过程
     * 对应了python中的, 对单只股票进行解析的函数. java需要实现成类. 函数的参数, 则经过实例化对象时, 通过属性进行传递.
     * run() 方法才能直接访问这些属性, 相当于读取了函数参数
     * 该函数使用了 4个参数.
     * -- 与python对称
     * 原python代码, 单个线程返回值是 dict, 将多个dict, 汇总extend到 大的result;
     * 调用端, 大的 result使用 ConcurrentHashMap, 而 各个线程, 则将自身结果extend到Map;
     * https://blog.csdn.net/huyaowei789/article/details/102811729 参考.
     * 这里, 我们同样, 将 CountDownLatch计数器, 和 汇总的 result 的大Map, 作为参数传递!. 因此作为类的属性.
     * -- 实现接口与线程池调用
     * 如果使用单个线程返回值, 主线程拼接的方式, 则实现Callable<Object>, 泛型可以指定为准确的返回值, 方便future.get()
     * 如果单线程自行添加部分结果到 总Result, 则实现Runnable
     * 且汇总方式调用pool.execute即可, 而 返回值方式, 使用 pool.submit(), 它返回Future
     * -- run()实现注意
     * 需要 try{主要过程}, finally {latch.countDown()} 即保证绝对调用计数器-1, 使得即使某个线程异常, 也能阻塞结束,主线程能够继续运行
     * -- tqdm 显示进度
     * 则需要 单个线程返回值的方式. Future f = pool.submit(Callable); f.get() 转型
     */
    public static class StockSingleParseTask implements Callable<ConcurrentHashMap<String, List<Double>>> {
        //  注意间接==settings
        public static List<String> fieldsOfDfRaw = KeyFuncOfSingleKlineBasePercent.fieldsOfDfRaw;
        public static Class[] fieldsOfDfRawClass = SettingsOfSingleKlineBasePercent.fieldsOfDfRawClass;

        private Connection conn;


        // 读取静态属性省一下代码长度.
        // 以下2属性, 为java于python主要不同. 这里并没有使用 Future 对单线程返回值做操作. python也可以这样写. 但cdl是java独有

        private CountDownLatch countDownLatch;
        private String stock;
        private List<String> statDateRange;
        private DataFrame<String> stockWithBoard;
        private HashMap<String, List<List<String>>> stockWithStDateRanges;
        private int windowUsePeriodsCoreArg;


        public StockSingleParseTask() {
        }

        public StockSingleParseTask(CountDownLatch countDownLatch,
                                    String stock,
                                    DataFrame<String> stockWithBoard, List<String> statDateRange,
                                    HashMap<String, List<List<String>>> stockWithStDateRanges, Connection conn,
                                    int windowUsePeriodsCoreArg) {
            this.countDownLatch = countDownLatch;
            this.stock = stock;
            this.stockWithBoard = stockWithBoard;
            this.statDateRange = statDateRange;
            this.stockWithStDateRanges = stockWithStDateRanges;
            this.conn = conn;
            this.windowUsePeriodsCoreArg = windowUsePeriodsCoreArg;
        }

        @Override
        public ConcurrentHashMap<String, List<Double>> call() {
            // 实际逻辑显然对应了python 的parse_single_stock() 函数
            // 使得不会返回null. 至少会返回空的字典
            ConcurrentHashMap<String, List<Double>> resultSingle = new ConcurrentHashMap<>(2 ^ 5);
            try {
                // 开始主要逻辑
                // 添加结果到 线程安全的 总结果集
                List<String> statDateRangeFull = CommonUtils.changeStatRangeForFull(statDateRange);
                // 单个线程用一个 conn 对象, 用完close(), 否则线程池容量不够
                // 连接未关闭, 传递了 conn. 若不传递, 则临时从池子获取.
                DataFrame<Object> dfRaw = getStockPriceByTscodeAndDaterangeAsDfFromTushare(stock, "nofq",
                        SettingsOfSingleKlineBasePercent.fieldsOfDfRaw,
                        statDateRangeFull, conn);
                dfRaw = dfRaw.dropna();
                // 新知识: java不定参数等价于 数组.而非List
                dfRaw.convert(fieldsOfDfRawClass);
                HashSet<String> adjDates = getAdjdatesByTscodeFromTushare(stock, conn);
                resultSingle = baseFormAndOpenConditionAnalyzer(dfRaw, adjDates, stock, stockWithBoard,
                        stockWithStDateRanges, statDateRange,
                        conn, windowUsePeriodsCoreArg); // 注意: dfRaw依据fulldates获取, 而这里要传递统计区间日期
                return resultSingle;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 必须计数减1, 使得主线程能够结束阻塞
                countDownLatch.countDown();
                return resultSingle;
            }
        }

        /**
         * 对应python 的 BaseFormAndOpenConditionAnalyzer 函数
         * 因为conn一直没有关闭, 这里传递conn对象. 直到单只股票分析完, 再关闭
         *
         * @param dfRaw
         * @param adjDates
         * @param stock
         * @param stockWithBoard
         * @param stockWithStDateRanges
         * @param statDateRange:        已经是 statDateRangeFull.
         * @return
         */
        public static ConcurrentHashMap<String, List<Double>> baseFormAndOpenConditionAnalyzer(
                DataFrame<Object> dfRaw, HashSet<String> adjDates,
                String stock,
                DataFrame<String> stockWithBoard,
                HashMap<String, List<List<String>>> stockWithStDateRanges,
                List<String> statDateRange,
                Connection conn, int windowUsePeriodsCoreArg) {

            ConcurrentHashMap<String, List<Double>> resultTemp = new ConcurrentHashMap<>(2);
            if (dfRaw.length() < windowUsePeriodsCoreArg) {
                return resultTemp;
            }
            for (int i = 4; i < dfRaw.length() - windowUsePeriodsCoreArg; i++) {
                try {

                    DataFrame<Object> dfWindowRaw = dfRaw.slice(i - 4,
                            i - 4 + windowUsePeriodsCoreArg);
                    String todayTemp = (String) dfWindowRaw
                            .get(5, fieldsOfDfRaw.indexOf("trade_date"));
                    if (todayTemp.compareTo(statDateRange.get(0)) < 0 || todayTemp
                            .compareTo(statDateRange.get(1)) >= 0) {
                        // 前包后不包
                        continue;
                    }
                    DataFrame<Object> dfWindow;
                    if (hasIntersectionBetweenAdjDatesAndDfWindow(adjDates, dfWindowRaw)) {
                        List<Object> colTradeDates = dfWindowRaw.col(fieldsOfDfRaw.indexOf("trade_date"));
                        List<String> dateRangeEqual = Arrays.asList((String) colTradeDates.get(0),
                                (String) colTradeDates.get(colTradeDates.size() - 1));
                        // @bugfix: 当获取等价后复权时,日期应当包含尾; 而原sqlApi实现包头不包尾.
                        // 不读取后复权sql时,不会影响,因此出现这种情况;  而python的sql是现写的, 没出现bug. :  < --> <=
                        // @bugfix: 已重构, 参数 excludeEndDate, 可以选择 设定. 此处设置 false, 则包尾
                        dfWindow = TushareApi
                                .getStockPriceByTscodeAndDaterangeAsDfFromTushare0(stock, "hfq", fieldsOfDfRaw,
                                        dateRangeEqual, conn, false);
                        dfWindow.convert(fieldsOfDfRawClass);
                    } else {
                        dfWindow = dfWindowRaw;
                    }
                    dfWindow = dfWindow.dropna();
                    //@bugfix: 注意是 length, 而非size()
                    if (dfWindow.length() != windowUsePeriodsCoreArg) {
                        continue; // 后复权数据,有所缺失
                    }

                    List<Object> pre5dayKlineRow = dfWindow.row(0);
                    List<Object> yesterdayKlineRow = dfWindow.row(4);
                    List<Object> todayKlineRow = dfWindow.row(5);
                    List<Object> resultAccordingKlineRow =
                            dfWindow.row(windowUsePeriodsCoreArg - 1);
                    List<String> concreteTodayFormStrs = parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                            yesterdayKlineRow, todayKlineRow, stockWithStDateRanges, stockWithBoard);

                    // 四个结果值
                    Double todayClose = getPriceOfSingleKline(todayKlineRow, "close");
                    Double singleResultAccordingNextOpen =
                            getPriceOfSingleKline(resultAccordingKlineRow, "open") / todayClose - 1;
                    Double singleResultAccordingNextClose =
                            getPriceOfSingleKline(resultAccordingKlineRow, "close") / todayClose - 1;
                    Double singleResultAccordingNextHigh =
                            getPriceOfSingleKline(resultAccordingKlineRow, "high") / todayClose - 1;
                    Double singleResultAccordingNextLow =
                            getPriceOfSingleKline(resultAccordingKlineRow, "low") / todayClose - 1;

                    // 7条件判定完成 *******
                    if (concreteTodayFormStrs.contains("-")) {
                        continue;
                    }
                    List<String> allForms = getAllFormNamesByConcreteFormStrs(concreteTodayFormStrs);
                    String prefix = "Next" + (windowUsePeriodsCoreArg - 7);
                    for (String keyTemp : allForms) {
                        resultTemp.putIfAbsent(keyTemp + prefix + "Open", new ArrayList<>());
                        resultTemp.get(keyTemp + prefix + "Open").add(singleResultAccordingNextOpen);
                        resultTemp.putIfAbsent(keyTemp + prefix + "Close", new ArrayList<>());
                        resultTemp.get(keyTemp + prefix + "Close").add(singleResultAccordingNextClose);
                        resultTemp.putIfAbsent(keyTemp + prefix + "High", new ArrayList<>());
                        resultTemp.get(keyTemp + prefix + "High").add(singleResultAccordingNextHigh);
                        resultTemp.putIfAbsent(keyTemp + prefix + "Low", new ArrayList<>());
                        resultTemp.get(keyTemp + prefix + "Low").add(singleResultAccordingNextLow);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 打印此时的 dfwindow 前3行
                    Console.log("发生了异常, dfWindow: ");
                    Console.log(dfRaw.slice(i, i + 3));
                }
            }
            return resultTemp;
        }


        public static List<String> parseConditionsAsStrs(String stock, DataFrame<Object> dfWindow,
                                                         List<Object> pre5dayKlineRow,
                                                         List<Object> yesterdayKlineRow,
                                                         List<Object> todayKlineRow,
                                                         HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                         DataFrame<String> stockWithBoard) throws SQLException {
            return KeyFuncOfSingleKlineBasePercent.parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                    yesterdayKlineRow, todayKlineRow,
                    stockWithStDateRanges, stockWithBoard, false, false);
        }

    }


}

