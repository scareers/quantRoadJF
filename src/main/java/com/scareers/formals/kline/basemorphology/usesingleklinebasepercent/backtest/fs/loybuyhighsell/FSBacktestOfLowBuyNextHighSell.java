package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell;

import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.StrUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.SettingsOfFSBacktest.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell.LowBuyParseTask.parseFromsSetsFromDb;
import static com.scareers.keyfuncs.positiondecision.PositionOfLowBuyByDistribution.virtualCdfAsPositionForLowBuy;
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

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class BuyPoint { // 抽象买点 对象
            Double timeTick;
            Double lowPricePercent;
            Double buyPricePercent;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class SellPoint { // 抽象卖点 对象
            Double timeTick;
            Double highPricePercent;
            Double sellPricePercent;
        }


        @Override
        public Void call() throws Exception {
            // 单个形态集合, 某一天, 以及被选择的 股票列表,
            // 现在开始, 对 单只股票尝试 明日低买, 后日高卖,或平卖, 保存相关结果
            if (totalAssets == 0) {
                return null; // 无选中则pass
            }

            // 低买开始 ********
            List<Object> lowBuyResults = lowBuyExecuteCore();
            // 这两项是低买原始结论.
            HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPrice = (HashMap<String, List<Double>>) lowBuyResults
                    .get(0); // 1.  lb_position_price_map   股票的 仓位,折算价格  字典保存
            Double reachTotalLimitTimeTick = (Double) lowBuyResults.get(1); // 2. lb_full_position_time_tick  满仓时间
            // @noti: 以下为低买引申结论
//            HashMap<String, Double> stockWithPosition = new HashMap<>(); // 最后从 仓位+价格字段, 获取即可,加速
//            HashMap<String, Double> stockWithBuyPrice = new HashMap<>(); // 最后从 仓位+价格字段, 获取即可,加速
//            for (String stock : stockWithTotalPositionAndAdaptedPrice.keySet()) {
//                List<Double> temp = stockWithTotalPositionAndAdaptedPrice.get(stock);
//                stockWithPosition.put(stock, temp.get(0));
//                stockWithBuyPrice.put(stock, temp.get(1));
//            }
//            lowBuyResults.add(stockWithPosition); // 1.{股票: 总仓位}
//            lowBuyResults.add(stockWithBuyPrice); // 2.{股票: 折算买入成本价} // @noti: 为了减小数据列, 此2不再保存
            Double weightedGlobalPrice = BacktestTaskOfPerDay  // 3.lb_weighted_buy_price  总加权平均成本百分比
                    .calcWeightedGlobalPrice(stockWithTotalPositionAndAdaptedPrice);
            // todo: 其他保存项, 可以通过以上2项直接计算, 成为新的简单列, 方便mysql筛选查询, 这里就简单返回这两项即可

            // 开始高卖尝试 ************  同样有未处理仓位


            return null;
        }

        public List<Object> lowBuyExecuteCore() throws Exception {
            List<Object> lowBuyResults = new ArrayList<>();
            // 1.首先, 应该获取到, 每只股票, 的(有效折合)买点(时间和价格percent),  这一步将大量访问数据库fs数据. 然后依据时间遍历.
            // 股票: [买点1, 买点2]   买点: --> 买点: [时间Double分时, 当时最低点(以便计算cdf), 折算购买价格]
            // @key: 买点使用简单逻辑: 首先需要 低于某个值, 且 连续下降后, 下一tick上升. 买入价格 这里设定为 最低点和下一tick平均
            // @noti: 得到所有被选中股票的 买点列表, 可能没有买点. 当时总资产需要分配 股票数量那么多
            HashMap<String, List<BuyPoint>> stockLowBuyPointsMap = getStockLowBuyPointsMap();
            // 将买点, 转换为 HashMap, key:value:  时间戳: 该tick 有买点的股票 {股票: 买点对象}
            // 买点对象: List<List<Double>> , 因为每个 tick, 每只股票最多只有单个 买点, 因此该数据结构符合逻辑
            HashMap<Double, HashMap<String, BuyPoint>> buyPointsOfAllTick = convertToTickWithStockBuys(
                    stockLowBuyPointsMap);
            // @key: 开始模拟买入,  tick 从  0.0 --> 240.0   ; 在同一分钟, 都有买点的股票, 就无视先后顺序了, 随 Map 的缘
            ArrayList<Double> timeTicks = new ArrayList<>(buyPointsOfAllTick.keySet());
            timeTicks.sort(Comparator.naturalOrder()); // 已经排序. 买点可能很难分布与 240个tick都有, 所以

            // @key: 结果项
            // 股票和对应总仓位和折算价格
            HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPrice = new HashMap<>();
            Double reachTotalLimitTimeTick = 0.0; // 达到满仓的 tick值!可记录大约什么时间能够满仓. 如果是最大值,则可能未能满仓

            outerLoop:
            for (Double tick : timeTicks) { // 提前达到了满仓, 则跳出2层循环到这里
                reachTotalLimitTimeTick = tick; // 保存一下tick
                HashMap<String, BuyPoint> buyPointsMap = buyPointsOfAllTick.get(tick);
                if (buyPointsMap == null || buyPointsMap.size() == 0) {
                    continue;
                    // 需要有实际买点
                }
                // 同一分钟不同股票的买点, 无视先后顺序, 可以接受
                for (String stock : buyPointsMap.keySet()) {
                    stockWithTotalPositionAndAdaptedPrice.putIfAbsent(stock, new ArrayList<>(Arrays.asList(0.0, 0.0)));

                    BuyPoint singleBuyPoint = buyPointsMap.get(stock);
                    // cdf 仓位买入  . --> cdf使用lowPrice低点,  其他均使用买入价格  buyPrice
                    Double lowPrice = singleBuyPoint.getLowPricePercent(); // 仅计算cdf
                    Double buyPrice = singleBuyPoint.getBuyPricePercent(); // 实际买入价格
                    // @update: 实际值应当小于此值, 而HighSell 的实际值, 也应当小于此值.  但是注意LowBuy是 正-->负, HighSell相反
                    if (buyPrice > execLowBuyThreshold) { // @noti: 凡是阈值, 一般都包含等于
                        continue; // 必须小于等于阈值
                    }

                    // cdf使用low 计算.  价格使用buyPrice计算
                    Double cdfOfPoint = virtualCdfAsPositionForLowBuy(ticksOfLow1, weightsOfLow1, lowPrice);
                    // @key2: 本轮后总仓位;  @noti: 已经将总仓位标准化为 1!!, 因此后面计算总仓位, 不需要 /资产数量
                    Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint / totalAssets; // 加大标准仓位,倍率设定1
                    if (epochTotalPosition > positionUpperLimit) { // 设置上限控制标准差.
                        epochTotalPosition = positionUpperLimit; // 上限
                    }

                    Double oldPositionTemp = stockWithTotalPositionAndAdaptedPrice.get(stock).get(0); // 老总仓位.
                    List<Double> oldStockWithPositionAndPrice = stockWithTotalPositionAndAdaptedPrice
                            .get(stock); // 默认0,0, 已经折算
                    if (oldPositionTemp < epochTotalPosition) { // 新的买点机制, 此 boolean 基本永恒 true
                        // 此时需要对 仓位和均成本进行折算. 新的一部分, 价格为 actualValue, 总仓位 epochTotalPosition.
                        // 旧的一部分, 价格 stockWithPositionAndValue.get(1), 旧总仓位 stockWithPositionAndValue.get(0)
                        // 单步折算.
                        Double weightedPrice =
                                (oldStockWithPositionAndPrice
                                        .get(0) / epochTotalPosition) * oldStockWithPositionAndPrice
                                        .get(1) + buyPrice * (1 - oldStockWithPositionAndPrice
                                        .get(0) / epochTotalPosition);
                        stockWithTotalPositionAndAdaptedPrice.put(stock, Arrays.asList(epochTotalPosition,
                                weightedPrice));
                    }
                    // 对仓位之和进行验证, 一旦第一次 超过上限, 则立即退出循环.
                    Double sum =
                            stockWithTotalPositionAndAdaptedPrice.values().stream().mapToDouble(value -> value.get(0))
                                    .sum(); // 直接使用流计算总和,
//                    Double sum = sumOfListNumberUseLoop(
//                            new ArrayList<>(stockWithTotalPositionAndAdaptedPrice.values()));
                    if (sum > 1.0) { // 如果超上限, 则将本股票 epochTotalPosition 减小, 是的总仓位 刚好1, 并立即返回
                        // 低买使用总资产.
                        Double newPosition = epochTotalPosition - (sum - 1.0);
                        // 折算权重也需要修正.
                        Double weightedPrice = // 这个老旧 仓位和价格, 是保存着的曾经. 直接用 . 新的总仓位直接用即可
                                (oldStockWithPositionAndPrice.get(0) / newPosition) * oldStockWithPositionAndPrice
                                        .get(1) + buyPrice * (1 - oldStockWithPositionAndPrice.get(0) / newPosition);
                        stockWithTotalPositionAndAdaptedPrice.put(stock, Arrays.asList(newPosition,
                                weightedPrice));
                        break outerLoop; // 此时所有资金已经用掉, 我们可以提前结束双层循环. 完成低买整个过程
                    }
                }
            }

            lowBuyResults.add(stockWithTotalPositionAndAdaptedPrice); // 0. {股票: [总仓位, 折算买入价格]} 仓位已经标准化.
            lowBuyResults.add(reachTotalLimitTimeTick); // 2.达到满仓时的时间, 当然, 也可能240, 且不满仓

            return lowBuyResults;
        }

        public static Double calcWeightedGlobalPrice(HashMap<String, List<Double>> stockWithActualValueAndPosition) {
            Double res = 0.0; // 加权求和
            for (List<Double> positionAndPrice : stockWithActualValueAndPosition.values()) {
                res += positionAndPrice.get(0) * positionAndPrice.get(1);
            }
            return res;
        }

        private HashMap<Double, HashMap<String, BuyPoint>> convertToTickWithStockBuys(
                HashMap<String, List<BuyPoint>> stockLowBuyPointsMap) {
            HashMap<Double, HashMap<String, BuyPoint>> res = new HashMap<>();
            for (String stock : stockLowBuyPointsMap.keySet()) {
                List<BuyPoint> buyPointsOfStock = stockLowBuyPointsMap.get(stock);
                for (BuyPoint buyPoint : buyPointsOfStock) {
                    Double timeTick = buyPoint.getTimeTick();
                    res.putIfAbsent(timeTick, new HashMap<>());
                    res.get(timeTick).put(stock, buyPoint);
                    // @noti: 这里能够直接修改值
                }
            }
            return res;
        }

        /**
         * 获取所有股票 买点列表.  key:value --> 股票:买点列表           买点: [时间Double分时, 当时最低点(以便计算cdf), 折算购买价格]
         * // @key: 买入点逻辑: 必须<某个临界值, 且 连续下跌, 下一tick上涨. 买入价格为 最低点和下一tick的平均值
         * // @key: 但是, cdf 应该按照最低点 计算, 因此买点也保存了 那个局部最低点价格
         *
         * @return
         */
        HashMap<String, List<BuyPoint>> getStockLowBuyPointsMap() throws Exception {
            HashMap<String, List<BuyPoint>> res = new HashMap<>();
            for (String stock : stockSelected) {
                // 因每只股票可能有所不同, 因此实时计算.!!! 该api有大缓存池 2048
                String lowBuyDate = getKeyIntsDateByStockAndToday(stock, tradeDate, keyInts).get(0);
                DataFrame<Object> dfFSLowBuyDay = getFs1mStockPriceOneDayAsDfFromTushare(connOfFS, stock, lowBuyDate,
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
                List<BuyPoint> buyPoints = new ArrayList<>();
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
                                buyPoints.add(new BuyPoint(tickDoubleCol.get(i), lowPrice, buyPrice));
                            } else { // 此前有买入点, 当前价格, 应当 低于此前最后一个低点的 低点价格, 当然是百分比
                                Double lastLowPrice = buyPoints.get(buyPoints.size() - 1).getLowPricePercent();
                                if (lowPrice < lastLowPrice) { // 必须更小, 才可能添加, 符合 cdf仓位算法
                                    Double buyPrice = i != lenth - 1 ?  // 折算买入价格. 低点和下一tick(高) 的平均值
                                            closeCol.get(i) + closeCol
                                                    .get(i + 1) / (2 * stdCloseOfLowBuy) - 1 : lowPrice;
                                    buyPoints.add(new BuyPoint(tickDoubleCol.get(i), lowPrice, buyPrice));
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




