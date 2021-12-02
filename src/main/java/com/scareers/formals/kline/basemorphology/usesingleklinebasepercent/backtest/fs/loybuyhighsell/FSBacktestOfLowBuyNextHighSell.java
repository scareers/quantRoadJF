package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.StrUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.SettingsOfFSBacktest.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.parseFromsSetsFromDb;
import static com.scareers.sqlapi.KlineFormsApi.*;
import static com.scareers.sqlapi.TushareApi.closePriceOfQfqStockSpecialDay;
import static com.scareers.sqlapi.TushareApi.getKeyIntsDateByStockAndToday;
import static com.scareers.sqlapi.TushareFSApi.getFs1mStockPriceOneDayAsDfFromTushare;
import static com.scareers.utils.CommonUtils.range;
import static com.scareers.utils.FSUtil.fsTimeStrParseToTickDouble;
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


        Long formSetId;
        String tradeDate;
        List<String> stockSelected;

        Double totalAssets; // 表示总股票数量

        // new对象时, 依据 formSetId 计算出来 低买分布(权重和tick) , 高卖分布(权重和tick)
        List<Double> ticksOfLow1;
        List<Double> weightsOfLow1;
        List<Double> ticksOfHigh1;
        List<Double> weightsOfHigh1;

        public BacktestTaskOfPerDay(Long formSetId, String tradeDate, List<String> stockSelected) throws Exception {
            this.formSetId = formSetId;
            this.tradeDate = tradeDate;
            this.stockSelected = stockSelected;

            totalAssets = (double) stockSelected.size();
            initDistributions(); // 给定了formSetId, 初始化两大分布(四个列表)
        }

        public void initDistributions() throws Exception {
            // 该api负责缓存, 本初始化方法不负责. 缓存已经针对 keyInts, 放心调用
            List<List<Double>> res = getLowBuyAndHighSellDistributionByFomsetid(formSetId, keyInts);
            ticksOfLow1 = res.get(0);
            weightsOfLow1 = res.get(1);
            ticksOfHigh1 = res.get(2);
            weightsOfHigh1 = res.get(3);
        }


        @Override
        public Void call() throws Exception {
            // 单个形态集合, 某一天, 以及被选择的 股票列表,
            // 现在开始, 对 单只股票尝试 明日低买, 后日高卖,或平卖, 保存相关结果
            if (totalAssets == 0) {
                return null; // 无选中则pass
            }

            // 低买开始 ********
            HashMap<Integer, Double> stockWithPositionLowBuy = new HashMap<>(); // 股票和对应的position, 已有仓位, 初始0
            HashMap<Integer, List<Double>> stockWithActualValueAndPositionLowBuy = new HashMap<>();


            // 1.首先, 应该获取到, 每只股票, 的(有效折合)买点(时间和价格percent),  这一步将大量访问数据库fs数据. 然后依据时间遍历.
            // 股票: [买点1, 买点2]   买点: --> 买点: [时间Double分时, 当时最低点(以便计算cdf), 折算购买价格]
            // @key: 买点使用简单逻辑: 首先需要 低于某个值, 且 连续下降后, 下一tick上升. 买入价格 这里设定为 最低点和下一tick平均
            HashMap<String, List<List<Double>>> stockLowBuyPointsMap = new HashMap<>();


            for (String stock : stockSelected) {
                // 首先需要找到 keyInts 对应的 低买 高卖, 两个日期,
                // 给定 stock, today, keyInts, 返回2个日期字符串. TushareApi
                List<String> keyIntsDate = getKeyIntsDateByStockAndToday(stock, tradeDate, keyInts);
                String loyBuyDate = keyIntsDate.get(0);
                String highSellDate = keyIntsDate.get(1); // 得到买卖日期

                // 开始低买, 单只股票, 参考 模拟程序. 低买逻辑基本一样, 这里简化, 分布仅使用Low1. 且 真实的买入点, 并非随机数产生买入点
                //List<Object> res = mainOfLowBuyCore();


            }


            return null;
        }

        /**
         * 获取所有股票 买点列表.  key:value --> 股票:买点列表           买点: [时间Double分时, 当时最低点(以便计算cdf), 折算购买价格]
         * // @key: 买入点逻辑: 必须<某个临界值, 且 连续下跌, 下一tick上涨. 买入价格为 最低点和下一tick的平均值
         * // @key: 但是, cdf 应该按照最低点 计算, 因此买点也保存了 那个局部最低点价格
         *
         * @return
         */
        HashMap<String, List<List<Double>>> getStockLowBuyPointsMap(String lowBuyDate) throws Exception {
            HashMap<String, List<List<Double>>> res = new HashMap<>();
            for (String stock : stockSelected) {
                // 因每只股票可能有所不同, 因此实时计算.!!! 该api有大缓存池 2048
                String loyBuyDate = getKeyIntsDateByStockAndToday(stock, tradeDate, keyInts).get(0);
                DataFrame<Object> dfFSLowBuyDay = getFs1mStockPriceOneDayAsDfFromTushare(connOfFS, stock, loyBuyDate,
                        fsSpecialUseFields); // Arrays.asList("trade_time", "close")
                if (dfFSLowBuyDay == null || dfFSLowBuyDay.length() == 0) {
                    return res; // 无分时数据, 则没有计算结果
                }
                // 1.将 trade_time, 转换为 tickDouble.
                List<Object> tradeTimeCol = dfFSLowBuyDay.col(0);
                List<Double> tickDoubleCol = new ArrayList<>(); // key1列
                tradeTimeCol.stream().forEach(value -> {
                    tickDoubleCol.add(fsTimeStrParseToTickDouble(value.toString().substring(11, 16)));
                }); // 构建 tick Double 列.
                List<Double> closeCol = DataFrameSelf.getColAsDoubleList(dfFSLowBuyDay, "close"); //key2列

                // 获取 今日收盘价, 作为 买入价_百分比 计算的 标准价格.       @Cached
                Double stdCloseOfLowBuy = closePriceOfQfqStockSpecialDay(stock, tradeDate, lowBuyDate,
                        connLocalTushare);
                // 阈值是负数百分比. 计算阈值价格, 这里 需要 不大于次阈值实际价格, 才可能低买
                Double lowBuyActualPriceThreshold = stdCloseOfLowBuy * (1 + execLowBuyThreshold);
                // 开始遍历close, 得到买入点,
                List<List<Double>> buyPoints = new ArrayList<>();
                // @key: @noti: 很明显, 按照cdf 的买入逻辑, 如果后面的 价格, 比之前价格更高, cdf仓位应该更低才对, 因此不是买点!
                // @key: 因此, 如果一只股票有多个买点, 则 价格是越来越低的, 不会变高!, 也不会相等
                int lenth = closeCol.size();
                for (int i = 0; i < lenth; i++) {
                    // 首先, 必须 下一个tick 是 上升的, 或者 i==最后. 否则continue
                    boolean nextTickRaise = false; // flag: 下一tick为上升
                    if (i == lenth - 1 || closeCol.get(i) < closeCol.get(i + 1)) {
                        nextTickRaise = true; // 最后一tick视为 符合条件, 防止越界
                        // 或者后一>前1.
                    }
                    if (!nextTickRaise) {
                        continue; // 如果下一tick 不是上升, 则 跳过, 不是买入点
                    }
                    int continuousFallTickCount = 0; // 连续下跌tick数量, 包括相等 !!!
                    // 从某个点, 往前, 连续下跌的 tick 数量, 其中 0tick的连续下降视为 250,
                    // 即开盘视为满足 连续下跌的条件, 仅仅需要对阈值进行符合判定, 即可买入
                    if (i == 0) {
                        continuousFallTickCount = 241; // 假定为最大, 开盘不限定
                    } else { // 其他tick都可以得到一个 连续下跌(包括相等) tick数量
                        for (int j = i; j > 0; j++) { // i开始, 计算到0, 不包括0.  即 -1分钟到0分钟 不计
                            if (closeCol.get(j) <= closeCol.get(j - 1)) {
                                continuousFallTickCount++;
                            } else {
                                break; // 一旦不符合, 应当跳出
                            }
                        }
                    }
                    // 多条件限定买点, 这里不用 and, 更加清晰
                    if (continuousFallTickCount >= continuousFallTickCountThreshold) { // 连续下跌数量必须不小于阈值设定
                        if (closeCol.get(i) <= lowBuyActualPriceThreshold) { // 必须价格不大于 设定阈值计算出来的价格
                            // 买入点, 必须 越来越低, 才符合 cdf 仓位算法!. 因此 需判定此前是否已经有买入点?
                            Double lowPrice = closeCol.get(i) / stdCloseOfLowBuy - 1; // 低点价格
                            if (buyPoints.size() == 0) { // 我是第一个买点, 直接加入
                                Double buyPrice = i != lenth - 1 ?  // 折算买入价格. 低点和下一tick(高) 的平均值
                                        closeCol.get(i) + closeCol.get(i + 1) / (2 * stdCloseOfLowBuy) - 1 : lowPrice;
                                buyPoints.add(Arrays  // 买入点: 时间tick, 当前最低点,  买入价格
                                        .asList(tickDoubleCol.get(i), lowPrice, buyPrice));
                            } else { // 此前有买入点, 当前价格, 应当 低于此前最后一个低点的 低点价格, 当然是百分比
                                Double lastLowPrice = buyPoints.get(buyPoints.size() - 1).get(1);
                                if (lowPrice < lastLowPrice) { // 必须更小, 才可能添加, 符合 cdf仓位算法
                                    Double buyPrice = i != lenth - 1 ?  // 折算买入价格. 低点和下一tick(高) 的平均值
                                            closeCol.get(i) + closeCol
                                                    .get(i + 1) / (2 * stdCloseOfLowBuy) - 1 : lowPrice;
                                    buyPoints.add(Arrays  // 买入点: 时间tick, 当前最低点,  买入价格
                                            .asList(tickDoubleCol.get(i), lowPrice, buyPrice));
                                }
                            }
                        }
                    }
                }
                // @key: 已经得到 单只股票 所有买点列表 [时间, low价格百分比, 买入价格百分比]: buyPoints 计算完毕, 可以是空列表
                res.put(stock, buyPoints);
            }
            return res;
        }

//        public List<Object> mainOfLowBuyCore() throws IOException {
//            HashMap<Integer, Double> stockWithPosition = new HashMap<>(); // 股票和对应的position, 已有仓位, 初始0
//            // stock: [折算position, 折算value]
//            HashMap<Integer, List<Double>> stockWithActualValueAndPosition = new HashMap<>();
//
//            for (int epoch = 0; epoch < 3; epoch++) { // 最多三轮, 某些股票第三轮将没有 Lowx出现, 注意判定
//                epochRaw = epoch;
//                // 每一轮可能有n对股票配对成功, 这里暂存, 最后再将这些移除股票池, 然后加入完成池
//                // @noti: @key2: 使用配对策略, 因此单个股票总仓配置2,而非1. 单次仓位, 则为 2* cdf(对应分布of该值)..
//                // 完全决定本轮后的总仓位, 后面统一配对, 再做修改
//                for (Integer id : stockPool) { // 应当改为, 计算出 每股票的买点(包括price和时间),
//                    // todo
//                    stockWithPosition.putIfAbsent(id, 0.0); // 默认0
//                    stockWithActualValueAndPosition.putIfAbsent(id, new ArrayList<>(Arrays.asList(0.0, 0.0)));
//                    // 第epoch轮, 出现的 Low 几?
//                    List<Integer> lows = stockLowOccurrences.get(id);
//                    if (epoch >= lows.size()) {
//                        continue; // 有些股票没有Low3, 需要小心越界. 这里跳过
//                    }
//                    Integer lowx = stockLowOccurrences.get(id).get(epoch);
//
//                    WeightRandom<Double> random = lowWithRandom.get(lowx); // 获取到随机器
//                    // @key: low实际值, cdf等
//                    Double actualValue = Double.parseDouble(random.next().toString());  // 具体的LOw出现时的 真实值
//                    // @update: 实际值应当小于此值, 而HighSell 的实际值, 也应当小于此值.  但是注意LowBuy是 正-->负, HighSell相反
//                    // 修改以更加符合实际. 当然,这里单个区间内的无数个值, 是平均概率的. 已经十分接近实际
//                    // @noti: cdf那里应当同时修改.
//                    actualValue = actualValue - Math.abs(Math.random() * tickGap);
//                    if (actualValue > execLowBuyThreshold) {
//                        continue; // 必须小于阈值
//                    }
//
//                    // 此值以及对应权重应当被保存
//
//                    List<Double> valuePercentOfLow = valuePercentOfLowx.get(lowx - 1); // 出现low几? 得到值列表
//                    List<Double> weightsOfLow = weightsOfLowx.get(lowx - 1);
//                    if (forceFirstDistributionDecidePosition) {
//                        valuePercentOfLow = valuePercentOfLowx.get(0);
//                        weightsOfLow = weightsOfLowx.get(0);
//                    }
//                    Double cdfOfPoint = virtualCdfAsPositionForLowBuy(valuePercentOfLow, weightsOfLow, actualValue);
//
//                    // @key2: 本轮后总仓位
//                    Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint; // 因两两配对, 因此这里仓位使用 2作为基数. 且为该股票总仓位
//                    if (epochTotalPosition > positionUpperLimit) {
//                        epochTotalPosition = positionUpperLimit; // 上限
//                    }
//
//                    Double oldPositionTemp = stockWithPosition.get(id);
//                    List<Double> oldStockWithPositionAndValue = stockWithActualValueAndPosition.get(id); // 默认0,0, 已经折算
//                    if (oldPositionTemp < epochTotalPosition) {
//                        stockWithPosition.put(id, epochTotalPosition); // 必须新的总仓位, 大于此前轮次总仓位, 才需要修改!!
//                        // 此时需要对 仓位和均成本进行折算. 新的一部分, 价格为 actualValue, 总仓位 epochTotalPosition.
//                        // 旧的一部分, 价格 stockWithPositionAndValue.get(1), 旧总仓位 stockWithPositionAndValue.get(0)
//                        // 单步折算.
//                        Double weightedPrice =
//                                (oldStockWithPositionAndValue
//                                        .get(0) / epochTotalPosition) * oldStockWithPositionAndValue
//                                        .get(1) + actualValue * (1 - oldStockWithPositionAndValue
//                                        .get(0) / epochTotalPosition);
//                        stockWithActualValueAndPosition.put(id, Arrays.asList(epochTotalPosition, weightedPrice));
//                    }
//                    // 对仓位之和进行验证, 一旦第一次 超过上限, 则立即退出循环.
//                    Double sum = sumOfListNumberUseLoop(new ArrayList<>(stockWithPosition.values()));
//                    if (sum > totalAssets) { // 如果超上限, 则将本股票 epochTotalPosition 减小, 是的总仓位 刚好30, 并立即返回
//                        Double newPosition = epochTotalPosition - (sum - totalAssets);
//                        stockWithPosition.put(id, newPosition); // 修改仓位
//                        // 折算权重也需要修正.
//                        Double weightedPrice =
//                                (oldStockWithPositionAndValue.get(0) / newPosition) * oldStockWithPositionAndValue
//                                        .get(1) + actualValue * (1 - oldStockWithPositionAndValue.get(0) / newPosition);
//                        stockWithActualValueAndPosition.put(id, Arrays.asList(epochTotalPosition, weightedPrice));
//
//                        reachTotalLimitInLoop = true;
//                        List<Object> res = new ArrayList<>();
//                        res.add(stockWithPosition);
//                        if (showStockWithPosition) {
//                            Console.log(JSONUtil.toJsonPrettyStr(stockWithPosition));
//                        }
//                        res.add(reachTotalLimitInLoop);
//                        res.add(epoch + 1);
//                        res.add(stockWithActualValueAndPosition);
//                        Double weightedGlobalPrice = calcWeightedGlobalPrice(stockWithActualValueAndPosition);
//                        res.add(weightedGlobalPrice);
//                        return res;
//                    }
//                }
//
//                // @key3: 尝试两两配对, 且修复仓位, 符合不超过2, 此时本轮 stockWithPosition 已经raw完成
//
//            }
//            List<Object> res = new ArrayList<>();
//            res.add(stockWithPosition);
//            if (showStockWithPosition) {
//                Console.log(JSONUtil.toJsonPrettyStr(stockWithPosition));
//            }
//            res.add(reachTotalLimitInLoop);
//            res.add(epochRaw + 1);
//            res.add(stockWithActualValueAndPosition);
//            Double weightedGlobalPrice = calcWeightedGlobalPrice(stockWithActualValueAndPosition);
//            res.add(weightedGlobalPrice);
//            return res; // 循环完成仍旧没有达到过30上限, 也返回最终的仓位分布
//        }


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




