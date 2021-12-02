package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.StrUtil;
import com.scareers.utils.Tqdm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.SettingsOfFSBacktest.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.parseFromsSetsFromDb;
import static com.scareers.sqlapi.KlineFormsApi.*;
import static com.scareers.utils.HardwareUtils.reportCpuMemoryDiskSubThread;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 回测框架
 * 1.同样:日期区间分批回测
 * 2.对单个日期, 可得 所有 形态集合 id, 对应的选股结果!
 * 3.单个日期, 遍历所有形态集合.
 * 4.对单个形态集合, 当日选股结果(股票列表), 判定 明日买/后日卖, 分时图低买高卖,
 * 5.保存单只股票, 低买各次, 高卖各次(成功), 强卖各次, 比较raw的数据
 * 5.保存单只股票, 低买平均仓位/价格, 高卖成功平均仓位/价格, 总体卖出平均价格和仓位
 * 5.保存单只股票, 当次操作的总体仓位,  盈利值.!!             // 所有仓位, 均为 该股票持仓 / 总股票数量, 即已经折算,否则不好比.
 * 6.决定仓位的 分布: 取 Low1 和 High1 对应的分布.  按照实际来讲, 并不能像模拟那样, Low2值为模拟值且分布也是模拟分布!!
 * 7.分布only best.
 * 8.如果单只股票保存为一条记录, 则数据表过大, 因此  日期-形态集合id 保存. 各字段均包含所有选中的股票,  相关操作
 * 9.边解析边保存, 不需要二次线程池.
 *
 * <p>
 * -Xmx512g -XX:MaxTenuringThreshold=0 -XX:GCTimeRatio=19 -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10
 *
 * @author: admin
 * @date: 2021/11/30
 */
public class FSBacktestOfLowBuyNextHighSell {
    public static void main(String[] args) throws Exception {
        // 股票列表也不需要, 因为直接读取了选股结果 股票列表
        // 未关闭连接,可复用
        reportCpuMemoryDiskSubThread(true); // 播报硬件信息
        execSql(sqlCreateSaveTableFSBacktest, // 建表分时回测
                connOfKlineForms, false);

        for (List<String> statDateRange : dateRanges) {
            Console.log("当前循环组: {}", statDateRange);
            // 不能关闭连接, 否则为 null, 引发空指针异常
            execSql(
                    StrUtil.format(sqlDeleteExistDateRangeFSBacktest,
                            StrUtil.format("[\"{}\",\"{}\"]", statDateRange.get(0), statDateRange.get(1))),
                    connOfKlineForms, false);
            // 主逻辑.
            fsLowBuyHighSellBacktestV1(statDateRange);
            log.info("current time");
        }
    }

    // 核心逻辑: 回测逻辑
    private static void fsLowBuyHighSellBacktestV1(List<String> backtestDateRange)
            throws Exception {
        // --------------------------------------------- 解析
        Console.log("开始回测区间: {}", backtestDateRange);
        List<String> dates = getEffectiveDatesBetweenDateRangeHasStockSelectResult(backtestDateRange, keyInts);
        ThreadPoolExecutor poolOfBacktest = new ThreadPoolExecutor(processAmountOfBacktest,
                processAmountOfBacktest * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()); // 唯一线程池, 一直不shutdown
        for (String tradeDate : dates) {
            HashMap<Long, List<String>> stockSelectResultPerDay = getStockSelectResultOfTradeDate(tradeDate, keyInts);
            if (stockSelectResultPerDay.size() <= 0) {
                log.warn("今日无选股结果(skip): {}", tradeDate);
                continue; // 无选股结果
            }
            // 单日的 2500+ 形态, 启用线程池执行解析保存回测结果
            ArrayList<Future<Void>> futuresOfBacktest = new ArrayList<>();

            List<Long> formSetIds = new ArrayList<>(stockSelectResultPerDay.keySet());
            for (Long formSetId : formSetIds) {
                Future<Void> f = poolOfBacktest
                        .submit(new BacktestTaskOfPerDay(formSetId, tradeDate, stockSelectResultPerDay.get(formSetId)));
                futuresOfBacktest.add(f);
            }
            List<Integer> indexesOfBacktest = CommonUtils.range(futuresOfBacktest.size());
            for (Integer i : Tqdm.tqdm(indexesOfBacktest, StrUtil.format("{} process: ", tradeDate))) {
                // 串行不再需要使用 CountDownLatch
                Future<Void> f = futuresOfBacktest.get(i);
                Void res = f.get();
                // todo: 可以处理返回值, 回测这里无返回值, 不需要组合成 大字典处理. 回测一天实时保存一天的结果即可
            }
            poolOfBacktest.shutdown(); // 关闭线程池
            System.out.println("finish");
        }
    }


    public static class BacktestTaskOfPerDay implements Callable<Void> {
        public void initDistributions() throws Exception { // 该api负责缓存, 本初始化不负责.
            List<List<Double>> res = getLowBuyAndHighSellDistributionByFomsetid(formSetId, keyInts);
            ticksOfLow1 = res.get(0);
            weightsOfLow1 = res.get(1);
            ticksOfHigh1 = res.get(2);
            weightsOfHigh1 = res.get(3);
        }


        Long formSetId;
        String tradeDate;
        List<String> stockSelected;
        // new对象时, 依据 formSetId 计算出来 低买分布(权重和tick) , 高卖分布(权重和tick)
        List<Double> ticksOfLow1;
        List<Double> weightsOfLow1;
        List<Double> ticksOfHigh1;
        List<Double> weightsOfHigh1;

        public BacktestTaskOfPerDay(Long formSetId, String tradeDate, List<String> stockSelected) throws Exception {
            this.formSetId = formSetId;
            this.tradeDate = tradeDate;
            this.stockSelected = stockSelected;
            initDistributions(); // 给定了formSetId, 就能初始化两大分布(四个列表) 了
        }

        @Override
        public Void call() {


            return null;
        }

        // @key: 从数据库获取的 2000+形态集合.的字典.  形态集合id: 已解析json的字符串列表.
        public static ConcurrentHashMap<Long, List<String>> formSetsMapFromDB;
        public static ConcurrentHashMap<Long, HashSet<String>> formSetsMapFromDBAsHashSet;

        static {
            try {
                formSetsMapFromDB = parseFromsSetsFromDb();
                formSetsMapFromDBAsHashSet = parseMapToSets(formSetsMapFromDB);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private static ConcurrentHashMap<Long, HashSet<String>> parseMapToSets(
                ConcurrentHashMap<Long, List<String>> formSetsMapFromDB) {
            ConcurrentHashMap<Long, HashSet<String>> res = new ConcurrentHashMap<>();
            for (Long key : formSetsMapFromDB.keySet()) {
                res.put(key, new HashSet<>(formSetsMapFromDB.get(key)));
            }
            return res;
        }
    }

    public static Log log = LogFactory.get();
}




