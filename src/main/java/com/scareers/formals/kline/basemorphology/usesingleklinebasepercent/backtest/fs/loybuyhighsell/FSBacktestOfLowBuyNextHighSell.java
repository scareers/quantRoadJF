package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.settings.SettingsCommon;
import com.scareers.sqlapi.MindgoFSApi;
import com.scareers.sqlapi.TushareApi;
import com.scareers.sqlapi.TushareIndexApi;
import com.scareers.utils.StrUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.SettingsOfFSBacktest.*;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.SettingsOfFSBacktest.connLocalTushare;
import static com.scareers.keyfuncs.positiondecision.PositionOfHighSellByDistribution.virtualCdfAsPositionForHighSell;
import static com.scareers.keyfuncs.positiondecision.PositionOfLowBuyByDistribution.virtualCdfAsPositionForLowBuy;
import static com.scareers.sqlapi.KlineFormsApi.*;
import static com.scareers.sqlapi.TushareApi.*;
import static com.scareers.sqlapi.TushareFSApi.getFs1mStockPriceOneDayAsDfFromTushare;
import static com.scareers.utils.CommonUtils.range;
import static com.scareers.utils.FSUtil.fsTimeStrParseToTickDouble;
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
        double[] argOfIndexLowBuys = {-5.0, -3.0, -1.0, 0.0, 1.0, 3.0, 5.0};
        double[] argOfIndexHighSells = {-5.0, -3.0, -1.0, 0.0, 1.0, 3.0, 5.0};

        for (int i = 0; i < argOfIndexLowBuys.length; i++) {
            Double lowbuyArg = argOfIndexLowBuys[i];
            for (int j = 0; j < argOfIndexHighSells.length; j++) {
                Double highSellArg = argOfIndexHighSells[i];

                Console.log("current settings: lowbuy: {}  highsell: {}", lowbuyArg, highSellArg);
                flushSettingsOfIndexBelongThatTimePriceEnhanceArg(lowbuyArg, highSellArg);// 刷新参数
                main0(); // 因同一进程, 因此相关sql查询结果已经被缓存
                // @warning: 注意内存使用.    缓存了太多东西
            }
        }
    }

    public static void main0() throws Exception {
        if (forceSecrity) {
            throw new Exception("强制不能运行本回测, 请修改设定");
        }


        // 股票列表也不需要, 因为直接读取了选股结果 股票列表
        // 未关闭连接,可复用
        //reportCpuMemoryDiskSubThread(false); // 播报硬件信息
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
        TimeInterval timer = DateUtil.timer();
        timer.start();
        // --------------------------------------------- 解析
        Console.log("开始回测区间: {}", backtestDateRange);
        List<String> dates = getEffectiveDatesBetweenDateRangeHasStockSelectResult(backtestDateRange, keyInts);
        ThreadPoolExecutor poolOfBacktest = new ThreadPoolExecutor(processAmountOfBacktest,
                processAmountOfBacktest * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()); // 唯一线程池, 一直不shutdown
        List<Integer> indexes = range(dates.size());
        for (Integer index : Tqdm.tqdm(indexes, StrUtil.format("{} total process ", backtestDateRange))) {
            Console.log("\ntotal process: {} / {}", index + 1, indexes.size()); // 换行以方便显示进度条
            if (index <= start) {
                continue; // 可设定起始值, 方便debug
            }
            if (index + 1 >= exit) {
                System.exit(1);
            }
            String tradeDate = dates.get(index);
            HashMap<Long, List<String>> stockSelectResultPerDay = getStockSelectResultOfTradeDate(tradeDate, keyInts);
            if (stockSelectResultPerDay.size() <= 0) {
                log.warn("今日无选股结果(skip): {}", tradeDate);
                continue; // 无选股结果
            }
            // 单日的 2500+ 形态, 启用线程池执行解析保存回测结果
            ArrayList<Future<Void>> futuresOfBacktest = new ArrayList<>();

            List<Long> formSetIds = new ArrayList<>(stockSelectResultPerDay.keySet());
            // 此处筛选掉极端的 formSetId.
            formSetIds = filterFormSetIds(formSetIds);
            for (Long formSetId : formSetIds) {
                Future<Void> f = poolOfBacktest
                        .submit(new BacktestTaskOfPerDay(formSetId, tradeDate, stockSelectResultPerDay.get(formSetId)
                                , backtestDateRange, backtestDateRange));
                futuresOfBacktest.add(f);
            }
            List<Integer> indexesOfBacktest = range(futuresOfBacktest.size());
            for (Integer i : Tqdm.tqdm(indexesOfBacktest, StrUtil.format("{} process ", tradeDate))) {
                // 串行不再需要使用 CountDownLatch
                Future<Void> f = futuresOfBacktest.get(i);
                Void res = f.get();
                // @noti: 可以处理返回值, 回测这里无返回值, 不需要组合成 大字典处理. 回测一天实时保存一天的结果即可
            }

        }
        MailUtil.send(SettingsCommon.receivers,
                StrUtil.format("部分回测完成: {} ", backtestDateRange),
                StrUtil.format("部分回测完成,耗时: {}h",
                        (double) timer.intervalRestart() / 3600000),
                false, null);
        poolOfBacktest.shutdown(); // 关闭线程池, 不能写在循环里面去, 否则报错 拒绝任务. 线程池大小0
    }

    private static List<Long> filterFormSetIds(List<Long> formSetIds) throws SQLException {
        DataFrame<Object> dataFrame = DataFrame.readSql(ConnectionFactory.getConnLocalKlineForms(),
                StrUtil.format(
                        "select form_set_id,\n" +
                                "       (max(virtual_geometry_mean) - min(virtual_geometry_mean)) as width,\n" +
                                "       max(virtual_geometry_mean)                                as\n" +
                                "                                                                    highsell,\n" +
                                "       min(virtual_geometry_mean)                                as lowbuy,\n" +
                                "       avg(effective_counts)                                     as ec,\n" +
                                "       min(stat_date_range)\n" +
                                "from fs_distribution_of_lowbuy_highsell_next0b1s fdolhn0b1s\n" +
                                "where effective_counts\n" +
                                "    >={} \n" +
                                "  and effective_counts < {} \n" +
                                "  and concrete_algorithm like '%value_percent%'\n" +
                                "  and stat_date_range = '[\"20210218\",\"21000101\"]'\n" +
                                "  and condition1 = 'strict'\n" +
                                "  and stat_result_algorithm like '%1%'\n" +
                                "group by form_set_id\n" +
                                "order by width desc", formSetIdsFilterArgs.get(0), formSetIdsFilterArgs.get(1)));
        List<Long> formSetIdsSelected = DataFrameSelf.getColAsLongList(dataFrame, "form_set_id");

        List<Long> res = new ArrayList<>();
        for (Long i : formSetIds) {
            if (formSetIdsSelected.contains(i)) {
                res.add(i);
            }
        }
        return res; // 只执行一遍, 所以不顾性能.
    }


    public static class BacktestTaskOfPerDay implements Callable<Void> {
        Long formSetId;
        String tradeDate; // 相当于today
        List<String> stockSelected;
        List<String> backtestDateRange;
        List<String> dateRange;

        Double totalAssets; // 表示总股票数量

        // new对象时, 依据 formSetId 计算出来 低买分布(权重和tick) , 高卖分布(权重和tick)
        List<Double> ticksOfLow1;
        List<Double> weightsOfLow1;
        List<Double> ticksOfHigh1;
        List<Double> weightsOfHigh1;

        public BacktestTaskOfPerDay(Long formSetId, String tradeDate, List<String> stockSelected,
                                    List<String> backtestDateRange, List<String> dateRange) throws Exception {
            this.formSetId = formSetId;
            this.tradeDate = tradeDate;
            this.stockSelected = stockSelected;
            this.backtestDateRange = backtestDateRange;
            this.dateRange = dateRange;

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
            Double lowPricePercent; // 低点价格, 实际低点价格百分比表示
            Double buyPricePercent; // 买点价格(低点+后一 /2)

            // @2021/12/08 当时, 此买点的股票, 所属两大指数(上证或深成), 当刻涨跌幅. 9:30视为当日open
            Double indexBelongPricePercentAtThatTime;


            public List<Double> toList() {
                return Arrays.asList(timeTick, lowPricePercent, buyPricePercent, indexBelongPricePercentAtThatTime);
            }
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class SellPoint { // 抽象卖点 对象
            Double timeTick;
            Double highPricePercent;
            Double sellPricePercent;

            // 对应low buy
            Double indexBelongPricePercentAtThatTime;

            public List<Double> toList() {
                return Arrays.asList(timeTick, highPricePercent, sellPricePercent, indexBelongPricePercentAtThatTime);
            }
        }


        @Override
        public Void call() throws Exception {
            // 单个形态集合, 某一天, 以及被选择的 股票列表,
            // 现在开始, 对 单只股票尝试 明日低买, 后日高卖,或平卖, 保存相关结果
            if (totalAssets == 0) {
                return null; // 无选中则pass
            }
            // 结果df. 将被保存到数据库. Map/List将转换为 json保存. 字符串等基本类型直接保存
            DataFrame<Object> dfLowBuyHighSell = new DataFrame<>();
            // 0.首先, 一些基本字段需要保存.
            dfLowBuyHighSell.add("form_set_id", Arrays.asList(formSetId));
            dfLowBuyHighSell.add("trade_date", Arrays.asList(tradeDate));
            dfLowBuyHighSell.add("stocks_selected", Arrays.asList(JSONUtil.toJsonStr(stockSelected)));
            dfLowBuyHighSell.add("stat_date_range", Arrays.asList(JSONUtil.toJsonStr(backtestDateRange)));
            dfLowBuyHighSell.add("stock_selected_count", Arrays.asList(totalAssets));

            // 低买开始 ********
            List<Object> lowBuyResults = lowBuyExecuteCore();
            if (lowBuyResults.size() < 3) {
                return null; // 没有买点结果
            }
            // 这两项是低买原始结论.
            HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPriceLowBuy = (HashMap<String, List<Double>>) lowBuyResults
                    .get(0); // 0.  lb_position_price_map   股票的 仓位,折算价格  字典保存
            //Console.log(stockWithTotalPositionAndAdaptedPriceLowBuy);


            try {
                dfLowBuyHighSell.add("lb_position_price_map",
                        Arrays.asList(JSONUtil.toJsonStr(stockWithTotalPositionAndAdaptedPriceLowBuy)));
            } catch (Exception e) {
                log.warn("发生异常");
                e.printStackTrace();
                Console.log(stockWithTotalPositionAndAdaptedPriceLowBuy);
/*              // 已经修复, 权重计算为NaN的bug.  此时因price为0.0, 因此权重设置为0, 不影响后续计算
                // 此时一般是 Double 有无限大的数, 无法被转换. NaN, 因此遍历且改变
                HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPriceLowBuyTemp = new HashMap<>();
                for (String key : stockWithTotalPositionAndAdaptedPriceLowBuy.keySet()) {
                    stockWithTotalPositionAndAdaptedPriceLowBuyTemp.put(key,
                            stockWithTotalPositionAndAdaptedPriceLowBuy.get(key).stream()
                                    .map(value -> value.equals(Double.NaN) ? null : value).collect(Collectors.toList())
                    );
                }// 将 NaN 转换为null 然后再, 但是将会发生 空指针异常
                dfLowBuyHighSell.add("lb_position_price_map",
                        Arrays.asList(JSONUtil.toJsonStr(stockWithTotalPositionAndAdaptedPriceLowBuyTemp)));
*/
                //return null;
            }
            Double reachTotalLimitTimeTick = (Double) lowBuyResults.get(1); // 1. lb_full_position_time_tick  满仓时间
            dfLowBuyHighSell.add("lb_full_position_time_tick", Arrays.asList(reachTotalLimitTimeTick));
            HashMap<String, List<BuyPoint>> stockLowBuyPointsMap = (HashMap<String, List<BuyPoint>>) lowBuyResults
                    .get(2); // 2.低买买点 lb_buypoints
            HashMap<String, List<List<Double>>> stockLowBuyPointsMapForSave =
                    convertBuyPointsToSaveMode(stockLowBuyPointsMap); // 转换一下.
            dfLowBuyHighSell.add("lb_buypoints", Arrays.asList(JSONUtil.toJsonStr(stockLowBuyPointsMapForSave)));
            Double weightedGlobalPrice = BacktestTaskOfPerDay  // 3.lb_weighted_buy_price  总加权平均成本百分比
                    .calcWeightedGlobalPrice(stockWithTotalPositionAndAdaptedPriceLowBuy);
            dfLowBuyHighSell.add("lb_weighted_buy_price", Arrays.asList(weightedGlobalPrice));
            Double totalPosition = stockWithTotalPositionAndAdaptedPriceLowBuy.values().stream()
                    .mapToDouble(value -> value.get(0)).sum(); // 4. 低买总仓位, 低买执行后即可获取  lb_global_position_sum
            dfLowBuyHighSell.add("lb_global_position_sum", Arrays.asList(totalPosition));

            // --------- 新增 低买可以统计的部分计量
            Long hasPositionStockCount =  // 1.低买完成后 , 持仓>0, 即有所持仓的被选中股票数量!!!!!!  lb_has_position_stock_count
                    stockWithTotalPositionAndAdaptedPriceLowBuy.values().stream().filter(value -> value.get(0) > 0.0)
                            .count();
            dfLowBuyHighSell.add("lb_has_position_stock_count", Arrays.asList(hasPositionStockCount));
            Double simpleAvgLowBuyPrice =
                    // 2.低买完成后, 对所有买入价格 简单平均数, 即无视仓位.  与之对比的是 lb_weighted_buy_price含有仓位,本值往往更低
                    stockWithTotalPositionAndAdaptedPriceLowBuy.values().stream().mapToDouble(value -> value.get(1))
                            .average().orElseGet(() -> {
                        return Double.NaN; // 后文替换为 null, 此处无法直接设置null
                    });
            dfLowBuyHighSell.add("lb_simple_avg_buy_price", Arrays.asList(simpleAvgLowBuyPrice));
            // --------- 结束

            // 开始高卖尝试 ************  同样有未处理仓位
            // stockWithTotalPositionAndAdaptedPrice 作为核心参数
            // 高卖仅4项基本数据
            List<Object> highSellResult = highSellExecuteCore(stockWithTotalPositionAndAdaptedPriceLowBuy); // 高卖
            HashMap<String, Double> stockWithPositionLowBuy =
                    (HashMap<String, Double>) highSellResult.get(0); // 5.低买结论衍生: 原始持仓map:  lb_positions
            dfLowBuyHighSell.add("lb_positions", Arrays.asList(JSONUtil.toJsonStr(stockWithPositionLowBuy)));
            HashMap<String, List<Double>> stockWithHighSellSuccessPositionAndAdaptedPrice =
                    (HashMap<String, List<Double>>) highSellResult.get(1); // 6.高卖成功部分仓位和价格 hs_success_position_price
            dfLowBuyHighSell.add("hs_success_position_price",
                    Arrays.asList(JSONUtil.toJsonStr(stockWithHighSellSuccessPositionAndAdaptedPrice)));
            HashMap<String, List<Double>> openAndCloseOfHighSell =
                    (HashMap<String, List<Double>>) highSellResult.get(2); // 7.高卖日开盘和收盘  hs_open_close
            dfLowBuyHighSell.add("hs_open_close", Arrays.asList(JSONUtil.toJsonStr(openAndCloseOfHighSell)));
            HashMap<String, List<SellPoint>> stockHighSellPointsMap =
                    (HashMap<String, List<SellPoint>>) highSellResult.get(3);
            // 8.高卖日所有高卖点  hs_sellpoints   各股票理论卖出点, 不一定有该高卖操作, 仅仅是理论上最多存在这些高卖点
            HashMap<String, List<List<Double>>> stockLowSellPointsMapForSave =
                    convertSellPointsToSaveMode(stockHighSellPointsMap); // 转换一下.
            dfLowBuyHighSell.add("hs_sellpoints", Arrays.asList(JSONUtil.toJsonStr(stockLowSellPointsMapForSave)));
            // @adding: 8.1: 开盘弱势股列表高卖当日
            List<String> weakStocks = (List<String>) highSellResult.get(4);
            dfLowBuyHighSell.add("hs_open_weak_stocks",
                    Arrays.asList(JSONUtil.toJsonStr(weakStocks)));
            // 项返回值解析完 毕

            // 用原始仓位 - 高卖执行的总仓位
            // 做减法, 得到剩余未能卖出的持仓, 并且全部 以close 折算
            HashMap<String, Double> stockWithPositionRemaining = // 9.剩余仓位 未能成功卖出  hs_remain_positions
                    subRawPositionsWithHighSellExecPositions(stockWithPositionLowBuy,
                            stockWithHighSellSuccessPositionAndAdaptedPrice);
            dfLowBuyHighSell.add("hs_remain_positions", Arrays.asList(JSONUtil.toJsonStr(stockWithPositionRemaining)));
            // 此为将未能卖出仓位, 折算进高卖成功仓位, 后的状态. 需要计算
            // 用 收盘价折算剩余仓位, 最终卖出仓位+价格. 此时仓位与原始同,全部卖出
            HashMap<String, List<Double>> stockWithHighSellPositionAndAdaptedPriceDiscountAll =
                    discountSuccessHighSellAndRemaining(stockWithPositionRemaining,
                            stockWithHighSellSuccessPositionAndAdaptedPrice,
                            openAndCloseOfHighSell); // 10.全折算卖出: hs_discount_all_position_price
            dfLowBuyHighSell.add("hs_discount_all_position_price",
                    Arrays.asList(JSONUtil.toJsonStr(stockWithHighSellPositionAndAdaptedPriceDiscountAll)));

            Double weightedGlobalPriceHighSellSuccess = calcWeightedGlobalPrice2(
                    stockWithHighSellSuccessPositionAndAdaptedPrice); // 11. 高卖成功部分折算价格 hs_success_global_price
            dfLowBuyHighSell.add("hs_success_global_price", Arrays.asList(weightedGlobalPriceHighSellSuccess));

            Double weightedGlobalPriceHighSellFinally = calcWeightedGlobalPrice2(
                    stockWithHighSellPositionAndAdaptedPriceDiscountAll); // 12.折算剩余, 最终折算价格 hs_discount_all_global_price
            dfLowBuyHighSell.add("hs_discount_all_global_price", Arrays.asList(weightedGlobalPriceHighSellFinally));

            HashMap<String, List<Double>> successPartProfits = profitOfHighSell(
                    stockWithTotalPositionAndAdaptedPriceLowBuy,
                    stockWithHighSellSuccessPositionAndAdaptedPrice);// 13.只计算高卖成功部分, 仓位+盈利值 hs_success_position_profit
            dfLowBuyHighSell.add("hs_success_position_profit", Arrays.asList(JSONUtil.toJsonStr(successPartProfits)));

            // 14.高卖成功部分, 整体的 加权盈利值!! hs_success_profit
            Double successPartProfitWeighted = calcWeightedGlobalPrice2(successPartProfits);
            dfLowBuyHighSell.add("hs_success_profit", Arrays.asList(successPartProfitWeighted));
            HashMap<String, List<Double>> allProfitsDiscounted = // 15.全部, 仓位+盈利值  hs_discount_all_position_profit
                    profitOfHighSell(stockWithTotalPositionAndAdaptedPriceLowBuy,
                            stockWithHighSellPositionAndAdaptedPriceDiscountAll);
            dfLowBuyHighSell
                    .add("hs_discount_all_position_profit", Arrays.asList(JSONUtil.toJsonStr(allProfitsDiscounted)));

            // 16.全折算后, 整体的 加权盈利值!!   hs_discount_all_profit
            Double allProfitsDiscountedProfitWeighted = calcWeightedGlobalPrice2(allProfitsDiscounted);
            dfLowBuyHighSell.add("hs_discount_all_profit", Arrays.asList(allProfitsDiscountedProfitWeighted));

            // @add: 新增, 计量 高卖成功总仓位 / 原始总仓位 , 得到高卖成功比例
            // 17.高卖成功总仓位 / 原始传递总仓位(尽量满仓但是达不到)  hs_success_global_percent
            Double highSellSuccessPositionPercent = stockWithHighSellSuccessPositionAndAdaptedPrice.values().stream()
                    .mapToDouble(value -> value.get(0).doubleValue())
                    .sum() / stockWithHighSellPositionAndAdaptedPriceDiscountAll.values().stream()
                    .mapToDouble(value -> value.get(0).doubleValue()).sum();
            dfLowBuyHighSell.add("hs_success_global_percent", Arrays.asList(highSellSuccessPositionPercent));
            Double profitDiscounted = allProfitsDiscountedProfitWeighted * totalPosition;
            // 18.单次操作盈利 总值, 已经折算仓位使用率, 低买部分不公平, 算是少算收益了 保守的.   lbhs_weighted_profit_conservative
            dfLowBuyHighSell.add("lbhs_weighted_profit_conservative", Arrays.asList(profitDiscounted));

            for (Object o : dfLowBuyHighSell.row(0)) {
                if (o instanceof Number) {
                    if (o.equals(Double.NaN)) {

                    }
                }
            }
            dfLowBuyHighSell = dfLowBuyHighSell.apply(value -> {
                if (value instanceof Number) {
                    if (value.equals(Double.NaN)) {
                        return null;
                    }
                }
                return value; // 如果值时 NaN, 则保存到mysql出错, 这里判定一下, 转换为 null
            });
            DataFrameSelf.toSql(dfLowBuyHighSell, saveTablenameFSBacktest,
                    connOfKlineForms, "append", null);
            // Console.log("success: {}", formSetId);
            return null;
        }

        private HashMap<String, List<List<Double>>> convertSellPointsToSaveMode(
                HashMap<String, List<SellPoint>> stockHighSellPointsMap) {
            HashMap<String, List<List<Double>>> res = new HashMap<>();
            for (String key : stockHighSellPointsMap.keySet()) {
                List<List<Double>> temp = new ArrayList<>();
                stockHighSellPointsMap.get(key).stream().map(value -> value.toList()).forEach(temp::add);
                res.put(key, temp); // 与下一样.使用流
            }
            return res;
        }

        private HashMap<String, List<List<Double>>> convertBuyPointsToSaveMode(
                HashMap<String, List<BuyPoint>> stockLowBuyPointsMap) {
            HashMap<String, List<List<Double>>> res = new HashMap<>();
            for (String key : stockLowBuyPointsMap.keySet()) {
                List<List<Double>> temp = new ArrayList<>();
                stockLowBuyPointsMap.get(key).stream().map(value -> value.toList()).forEach(temp::add);
                res.put(key, temp);
            }
            return res;
        }

        /**
         * 高卖核心逻辑.
         * 卖出策略分为几种: 1. 开盘无脑卖, 为了顺应现实, 使用 9:31而非9:30的价格  2.收盘无脑卖 3.尝试高卖,剩余的收盘卖出
         *
         * @param stockWithTotalPositionAndAdaptedPriceLowBuy 低买结果. map. 股票:[总仓位, 买入价格百分比]
         * @return
         * @throws Exception
         */
        public List<Object> highSellExecuteCore(
                HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPriceLowBuy)
                throws Exception {
            HashMap<String, Double> stockWithPositionLowBuy = new HashMap<>();
            for (String stock : stockWithTotalPositionAndAdaptedPriceLowBuy.keySet()) {
                List<Double> temp = stockWithTotalPositionAndAdaptedPriceLowBuy.get(stock);
                stockWithPositionLowBuy.put(stock, temp.get(0));
            }

            // 当尝试高卖时, 高卖逻辑与低买匹配.  剩余仓位收盘卖出
            // 1. 同理获取 卖点map, 比起低买, 高卖还需要获取 开盘价 和 收盘价map, 以便折算
            // 2. 高卖时, 计算剩余未能卖出的时, 将对所有股票进行计算(包含0.0仓位), 因此这里不进行筛选股票, 直接全部
            List<String> stockHasPosition = // 包括 仓位 0.0的, 这里不进行筛选
                    stockWithTotalPositionAndAdaptedPriceLowBuy.entrySet().stream()
                            .map(value -> value.getKey())
                            .collect(Collectors.toList()); // 注意流的使用!!@key 且无 filter

            List<Object> highSellPointsRes =
                    getStockHighSellPointsMap(stockWithTotalPositionAndAdaptedPriceLowBuy, stockHasPosition);
            HashMap<String, List<SellPoint>> stockHighSellPointsMap =
                    (HashMap<String, List<SellPoint>>) highSellPointsRes.get(0);
            HashMap<String, List<Double>> openAndCloseOfHighSell = (HashMap<String, List<Double>>) highSellPointsRes
                    .get(1);
            // 将卖点, 转换为 HashMap, key:value:  时间戳: 该tick 有卖点的股票 {股票: 卖点对象}
            // 卖点对象: SellPoint , 因为每个 tick, 每只股票最多只有单个 买点, 因此该数据结构符合逻辑
            HashMap<Double, HashMap<String, SellPoint>> buyPointsOfAllTick = convertToTickWithStockSells(
                    stockHighSellPointsMap);
            // @key: 开始模拟卖入,  tick 从  0.0 --> 240.0   ; 在同一分钟, 都有卖点的股票, 就无视先后顺序了, 随 Map 的缘
            ArrayList<Double> timeTicks = new ArrayList<>(buyPointsOfAllTick.keySet());
            timeTicks.sort(Comparator.naturalOrder()); // 已经排序. 买点可能很难分布与 240个tick都有, 所以
            // @key: 结果项
            // 股票和对应总仓位和折算价格,  这是高卖结果. 高卖需要不超过 低买已有持仓. 类似低买不超过总仓位 1.0
            HashMap<String, List<Double>> stockWithHighSellSuccessPositionAndAdaptedPrice = new HashMap<>();
            for (Double tick : timeTicks) { // 显然是要把所有卖点都执行到的. 不存在跳出2层循环
                HashMap<String, SellPoint> sellPointsMap = buyPointsOfAllTick.get(tick);
                if (sellPointsMap == null || sellPointsMap.size() == 0) {
                    continue;
                    // 需要有实际卖点
                }
                // 同一分钟不同股票的卖点, 无视先后顺序, 可以接受
                for (String stock : sellPointsMap.keySet()) {
                    stockWithHighSellSuccessPositionAndAdaptedPrice
                            .putIfAbsent(stock, new ArrayList<>(Arrays.asList(0.0, 0.0)));

                    SellPoint singleBuyPoint = sellPointsMap.get(stock);
                    // cdf 仓位卖出  . --> cdf使用highPrice低点,  其他均使用买入价格  sellPrice
                    Double highPrice = singleBuyPoint.getHighPricePercent(); // 仅计算cdf
                    Double sellPrice = singleBuyPoint.getSellPricePercent(); // 实际卖出价格
                    Double indexPriceThatTime = singleBuyPoint.getIndexBelongPricePercentAtThatTime();
                    // @update: 实际值应当小于此值, 而HighSell 的实际值, 也应当小于此值.  但是注意LowBuy是 正-->负, HighSell相反
                    if (highPrice <= execHighSellThreshold) { // @noti: 凡是阈值, 一般都包含等于, 虽然计算卖点时已经计算过.这里重复嫌疑
                        continue; // 必须大于等于阈值
                    }
                    Double cdfCalcPrice = calcEquivalenceCdfUsePriceOfHighSell(highPrice, indexPriceThatTime);
                    // cdf使用 high 计算.  价格使用 sellPrice计算
                    Double cdfOfPoint = virtualCdfAsPositionForHighSell(ticksOfHigh1, weightsOfHigh1, cdfCalcPrice,
                            tickGap);
                    // @key3: 高卖仓位折算!!!
                    Double lowBuyPositionTotal = stockWithTotalPositionAndAdaptedPriceLowBuy.get(stock).get(0);
                    Double epochTotalPosition =
                            positionCalcKeyArgsOfCdfHighSell * cdfOfPoint * lowBuyPositionTotal;  // 这里应该以 低买总持仓作为基数
                    if (epochTotalPosition > lowBuyPositionTotal) { // 设置高卖上限, 为低买总持仓,
                        epochTotalPosition = lowBuyPositionTotal; // 上限
                    }
                    // Console.log(highPrice, cdfOfPoint, lowBuyPositionTotal, epochTotalPosition);

                    List<Double> oldStockWithPositionAndPrice = stockWithHighSellSuccessPositionAndAdaptedPrice
                            .get(stock); // 默认0,0, 已经折算, 老卖出 [仓位,价格]
                    Double oldPositionTemp = oldStockWithPositionAndPrice.get(0); // 老总仓位.
                    if (oldPositionTemp < epochTotalPosition) { // 新的买点机制, 此 boolean 基本永恒 true
                        // 此时需要对 仓位和均成本进行折算. 新的一部分, 价格为 actualValue, 总仓位 epochTotalPosition.
                        // 旧的一部分, 价格 stockWithPositionAndValue.get(1), 旧总仓位 stockWithPositionAndValue.get(0)
                        // 单步折算.
                        Double weightedPrice =
                                (oldStockWithPositionAndPrice
                                        .get(0) / epochTotalPosition) * oldStockWithPositionAndPrice
                                        .get(1) + sellPrice * (1 - oldStockWithPositionAndPrice
                                        .get(0) / epochTotalPosition);
                        stockWithHighSellSuccessPositionAndAdaptedPrice.put(stock, Arrays.asList(epochTotalPosition,
                                weightedPrice));
                    }
                    // 几乎无法全部股票恰好全部卖出, 因此, 不执行相关判定.  循环完成后, 返回前判定剩余
                }
            }
            List<String> weakStocks = new ArrayList<>(); // 弱势股列表, 简单判定是否开盘价格<= 某阈值
            if (forceSellOpenWeakStock) { // 开盘强制全仓卖出弱势股票
                for (String stock : openAndCloseOfHighSell.keySet()) {
                    // @update: 弱势股使用当日开盘涨跌幅, 而非对于 前日(today) 的收盘价的涨跌幅.! 更加符合常规 弱势判定
                    DataFrame<Object> dfTemp = TushareApi.getStockPriceByTscodeAndTradeDateAsDfFromTushare(stock,
                            "nofq",
                            Arrays.asList("open", "pre_close"), tradeDate, connLocalTushare);
                    Double openPrice = Double.valueOf(dfTemp.row(0).get(0).toString());
                    Double preClosePrice = Double.valueOf(dfTemp.row(0).get(1).toString());
                    Double openPercentThatDay = openPrice / preClosePrice - 1; // 卖出当日纯涨跌幅(相对前一天)
                    Double openPercentRelativeToday = openAndCloseOfHighSell.get(stock).get(0); // 相对今天的收盘价 的开盘
                    if (openPercentThatDay <= weakStockOpenPercentThatDayThreshold
                            // 要求当日开盘涨跌幅<-2% 且 相对今日收盘涨跌幅 <-7%(部分 止 损意味)
                            && openPercentRelativeToday <= weakStockOpenPercentTodayThreshold) {
                        // 弱势股阈值
                        // 强制修改高卖 map 的结果!!
                        Double rawPositionOfThisStock = stockWithPositionLowBuy.get(stock); // 原始低买后该股总仓位
                        stockWithHighSellSuccessPositionAndAdaptedPrice.put(stock,
                                Arrays.asList(rawPositionOfThisStock,
                                        openPercentRelativeToday)); // 强制修改为 高卖成功了: [所有仓位,开盘价]
                        weakStocks.add(stock);
                    }
                }
            }

            List<Object> res = new ArrayList<>();
            // 因此仅仅需要 此4项作为原始数据返回, 其余的皆以此(结合低买) 计算而来
            res.add(stockWithPositionLowBuy); // 0. 依据lowbuy传递来的参数的衍生, 原始持仓map
            // @key: 高卖本质仅仅返回此核心map. 以此计算其余所有保存项. 因此, 加入开盘卖出弱势股票机制, 只需要修改此map.
            res.add(stockWithHighSellSuccessPositionAndAdaptedPrice); // 成功高卖map.
            res.add(openAndCloseOfHighSell); // 卖出当日的 开盘和收盘价
            res.add(stockHighSellPointsMap); // 各股票理论卖出点, 不一定有该操作
            //res.add(stockWithTotalPositionAndAdaptedPriceLowBuy); // 低买那里有此数据项,即参数传递而来的
            res.add(weakStocks); // @adding: 新增弱势股列表, 即使不开盘卖出弱势股, 本结论依旧有意义
            return res;
        }

        /**
         * 高卖盈利, 高卖 - 低买
         *
         * @param stockWithActualValueAndPosition
         * @param stockWithHighSellActualValueAndPosition
         * @return
         */
        private static HashMap<String, List<Double>> profitOfHighSell(
                HashMap<String, List<Double>> stockWithActualValueAndPosition,
                HashMap<String, List<Double>> stockWithHighSellActualValueAndPosition) {
            // 仓位仅仅以高卖成功的计算, 无视原始总仓位. 计算价差后, 同样以卖出仓位作为权重.
            HashMap<String, List<Double>> positionWithProfit = new HashMap<>();
            for (String key : stockWithHighSellActualValueAndPosition.keySet()) {
                Double newPosition = stockWithHighSellActualValueAndPosition.get(key).get(0);
                Double profit =
                        stockWithHighSellActualValueAndPosition.get(key).get(1) - stockWithActualValueAndPosition
                                .getOrDefault(key, Arrays.asList(0.0, 0.0)).get(1);
                positionWithProfit.put(key, Arrays.asList(newPosition, profit));
            }
            return positionWithProfit;
        }

        public static Double calcWeightedGlobalPrice2(HashMap<String, List<Double>> stockWithActualValueAndPosition) {
            Double res = 0.0;
            // 这里总仓位, 应当使用传递来的参数的, 的position之和. LowBuy那里可以直接 30, 这里不行
            // 且计算纯高时, 仓位并非全仓位, 只是 成功那一部分仓位!
            Double totalAssets =
                    stockWithActualValueAndPosition.values().stream().mapToDouble(value -> value.get(0)).sum();
            // 临时仓位之和. !!!!!

            for (List<Double> positionAndPrice : stockWithActualValueAndPosition.values()) {
                res += positionAndPrice.get(0) / totalAssets * positionAndPrice.get(1);
            }
            return res;
        }

        /**
         * 收盘价折算剩余部分, 计算最终等价的 仓位 和 价格. 仓位全部卖出, 等同于低买传递来的持仓
         *
         * @param stockWithPositionRemaining
         * @param stockWithHighSellSuccessPositionAndAdaptedPrice
         * @param openAndCloseOfHighSell
         * @return
         */
        private HashMap<String, List<Double>> discountSuccessHighSellAndRemaining(
                HashMap<String, Double> stockWithPositionRemaining,
                HashMap<String, List<Double>> stockWithHighSellSuccessPositionAndAdaptedPrice,
                HashMap<String, List<Double>> openAndCloseOfHighSell) {
            HashMap<String, List<Double>> res = new HashMap<>();
//            Console.log(openAndCloseOfHighSell.size());
//            Console.log(stockWithPositionRemaining.size());

            for (String key : stockWithPositionRemaining.keySet()) {
                Double discountRemaingPrice = openAndCloseOfHighSell.get(key).get(1); // 以收盘价作为折算, 当然也是百分比
                Double remainPosition = stockWithPositionRemaining.get(key);
                Double successSellPosition =
                        stockWithHighSellSuccessPositionAndAdaptedPrice.getOrDefault(key, Arrays.asList(0.0, 0.0))
                                .get(0);
                Double successSellPrice = stockWithHighSellSuccessPositionAndAdaptedPrice
                        .getOrDefault(key, Arrays.asList(0.0, 0.0)).get(1);
                Double totalPosition = remainPosition + successSellPosition;
                Double discountedPrice =
                        remainPosition / totalPosition * discountRemaingPrice +
                                successSellPosition / totalPosition * successSellPrice; // 简单加权
                if (discountedPrice.equals(Double.NaN)) {
                    discountedPrice = 0.0;
                }
                res.put(key, Arrays.asList(totalPosition, discountedPrice));
            }
            return res;
        }

        /**
         * 低买总持仓 - 高卖成功那部分仓位 == 剩余未能卖出仓位
         *
         * @param stockWithPosition
         * @param stockWithHighSellActualValueAndPosition
         * @return
         */
        private static HashMap<String, Double> subRawPositionsWithHighSellExecPositions(
                HashMap<String, Double> stockWithPosition,
                HashMap<String, List<Double>> stockWithHighSellActualValueAndPosition) {
            HashMap<String, Double> res = new HashMap<>();
            // 做减法.得到剩余未能成功卖出仓位
            for (String key : stockWithPosition.keySet()) {
                //注意可能一点都没有高卖掉
                res.put(key,
                        stockWithPosition.get(key) - stockWithHighSellActualValueAndPosition.getOrDefault(key,
                                Arrays.asList(0.0, 0.0)).get(0));
            }
            return res;
        }


        /**
         * 获取高卖 卖点列表. map;   同理需要传递 低买后 持仓map
         *
         * @param stockWithTotalPositionAndAdaptedPrice
         * @return
         * @throws Exception
         */
        List<Object> getStockHighSellPointsMap(
                HashMap<String, List<Double>> stockWithTotalPositionAndAdaptedPrice, List<String> stockHasPosition)
                throws Exception {
            HashMap<String, List<SellPoint>> res = new HashMap<>();
            // 把开盘和收盘也保存一下,多出来的卖出逻辑. stock: [open, close] 卖出当天的
            HashMap<String, List<Double>> resOfSellOpenAndClose = new HashMap<>();
            // 需要计算哪些股票有持仓, 没有持仓的股票, 则不需要计算卖点

            for (String stock : stockHasPosition) {
                resOfSellOpenAndClose.putIfAbsent(stock, Arrays.asList(0.0, 0.0)); // 赋值时, open和close分布设置 0/1 即可
                String highSellDate = getKeyIntsDateByStockAndToday(stock, tradeDate, keyInts).get(1); // get1 是卖出日期
                DataFrame<Object> dfFSHighSellDay = getFs1mStockPriceOneDayAsDfFromTushare(connOfFS, stock,
                        highSellDate,
                        fsSpecialUseFields); // Arrays.asList("trade_time", "close")
                if (dfFSHighSellDay == null || dfFSHighSellDay.length() == 0) {
                    continue;
                }
                // 1.将 trade_time, 转换为 tickDouble.
                List<Object> tradeTimeCol = dfFSHighSellDay.col(0);
                List<Double> tickDoubleCol = new ArrayList<>(); // key1列
                tradeTimeCol.stream().forEach(value -> {
                    tickDoubleCol.add(fsTimeStrParseToTickDouble(value.toString().substring(11, 16)));
                }); // 构建 tick Double 列.
                List<Double> closeCol = DataFrameSelf.getColAsDoubleList(dfFSHighSellDay, "close"); //key2列

                // 获取 今日收盘价, 作为 买入价_百分比 计算的 标准价格.       @Cached
                Double stdCloseOfHighSell = closePriceOfQfqStockSpecialDay(stock, tradeDate, highSellDate,
                        connLocalTushare);
                // 阈值是负数百分比. 计算阈值价格, 这里 需要 不小于次阈值实际价格, 才可能高卖
                // 算法同.只是下面<换成>
                Double highSellActualPriceThreshold = stdCloseOfHighSell * (1 + execHighSellThreshold);
                // 开始遍历close, 得到卖出点,
                List<SellPoint> sellPoints = new ArrayList<>();
                // @key: @noti: 很明显, 按照cdf 的买入逻辑, 如果后面的 价格, 比之前价格更低, cdf卖出仓位应该更低才对, 因此不是卖点!
                // @key: 因此, 如果一只股票有多个卖点, 则 价格是越来越高的, 不会变更低!, 也不会相等
                int lenth = closeCol.size();
                for (int i = 0; i < lenth; i++) {
                    if (i == 1) {
                        resOfSellOpenAndClose.get(stock).set(0, closeCol.get(i) / stdCloseOfHighSell - 1);
                    }// 保存开盘价
                    if (i == lenth - 1) {
                        resOfSellOpenAndClose.get(stock).set(1, closeCol.get(i) / stdCloseOfHighSell - 1);
                    }// 保存收盘价.


                    // 高卖点开始
                    // 首先, 必须 下一个tick 是 下降的, 或者 i==最后. 否则continue
                    boolean nextTickFall = false; // flag: 下一tick为下降
                    if (i == lenth - 1 || closeCol.get(i) > closeCol.get(i + 1)) {
                        nextTickFall = true; // 最后一tick视为 符合条件, 防止越界
                        // 或者后一>前1.
                    }
                    if (!nextTickFall) {
                        continue; // 如果下一tick 不是下降, 则 跳过, 不是卖出点
                    }
                    int continuousRaiseTickCount = 0; // 连续上升tick数量, 包括相等 !!!
                    // 从某个点, 往前, 连续上升的 tick 数量, 其中 0tick的连续上升视为 241,  // 正常上限 240.
                    // 即开盘视为满足 连续上升的条件, 仅仅需要对阈值进行符合判定, 即可买入
                    if (i == 0) {
                        continuousRaiseTickCount = 241; // 假定为最大, 开盘不限定
                    } else { // 其他tick都可以得到一个 连续下跌(包括相等) tick数量
                        for (int j = i; j > 0; j--) { // i开始, 计算到0, 不包括0.  即 -1分钟到0分钟 不计
                            if (closeCol.get(j) >= closeCol.get(j - 1)) {
                                continuousRaiseTickCount++;
                            } else {
                                break; // 一旦不符合, 应当跳出
                            }
                        }
                    }
                    // 多条件限定买点, 这里不用 and, 更加清晰
                    if (continuousRaiseTickCount >= continuousRaiseTickCountThreshold) { // 连续下跌数量必须不小于阈值设定
                        if (closeCol.get(i) >= highSellActualPriceThreshold) { // 必须价格不小于 设定阈值计算出来的价格
                            // 买入点, 必须 越来越低, 才符合 cdf 仓位算法!. 因此 需判定此前是否已经有买入点?
                            Double highPrice = closeCol.get(i) / stdCloseOfHighSell - 1; // 高点价格
                            if (sellPoints.size() == 0) { // 我是第一个卖点, 直接加入
                                Double buyPrice = i != lenth - 1 ?  // 折算卖出价格. 高点和下一tick(低) 的平均值
                                        (closeCol.get(i) + closeCol
                                                .get(i + 1)) / (2 * stdCloseOfHighSell) - 1 : highPrice;

                                // @update: 加入当时对应指数的涨跌幅. 9:30, 则为当日 open 的涨跌幅
                                Double indexPricePercent = getCurrentBelongIndexPricePercent(i, stock,
                                        highSellDate);
                                sellPoints.add(new SellPoint(tickDoubleCol.get(i), highPrice, buyPrice,
                                        indexPricePercent));
                            } else { // 此前有买入点, 当前价格, 应当 高于此前最后一个高点的 高点价格, 当然是百分比
                                Double lastLowPrice = sellPoints.get(sellPoints.size() - 1).getHighPricePercent();
                                if (highPrice > lastLowPrice) { // 必须更大, 才可能添加, 符合 cdf仓位算法
                                    Double buyPrice = i != lenth - 1 ?  // 折算买入价格. 低点和下一tick(高) 的平均值
                                            (closeCol.get(i) + closeCol
                                                    .get(i + 1)) / (2 * stdCloseOfHighSell) - 1 : highPrice;
                                    Double indexPricePercent = getCurrentBelongIndexPricePercent(i, stock,
                                            highSellDate);
                                    sellPoints.add(new SellPoint(tickDoubleCol.get(i), highPrice, buyPrice,
                                            indexPricePercent));
                                }
                            }
                        }
                    }
                }
                // @key: 已经得到 单只股票 所有买点列表 [时间, high价格百分比, 卖出价格百分比]: sellPoints 计算完毕, 可以是空列表
                res.put(stock, sellPoints);
            }
            List<Object> sellRes = new ArrayList<>();
            sellRes.add(res); // 卖点列表  HashMap<String, List<SellPoint>>
            sellRes.add(resOfSellOpenAndClose); // 开盘价和收盘价  HashMap<String, List<Double>>
            return sellRes;
        }

        public List<Object> lowBuyExecuteCore() throws Exception {
            List<Object> lowBuyResults = new ArrayList<>();
            // 1.首先, 应该获取到, 每只股票, 的(有效折合)买点(时间和价格percent),  这一步将大量访问数据库fs数据. 然后依据时间遍历.
            // 股票: [买点1, 买点2]   买点: --> 买点: [时间Double分时, 当时最低点(以便计算cdf), 折算购买价格]
            // @key: 买点使用简单逻辑: 首先需要 低于某个值, 且 连续下降后, 下一tick上升. 买入价格 这里设定为 最低点和下一tick平均
            // @noti: 得到所有被选中股票的 买点列表, 可能没有买点. 当时总资产需要分配 股票数量那么多
            HashMap<String, List<BuyPoint>> stockLowBuyPointsMap = getStockLowBuyPointsMap();
            if (stockLowBuyPointsMap.size() == 0) {
                return lowBuyResults; // 当没有买点, 返回空结果. 因此调用方需要判定
            }
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
                    Double indexPriceThatTime = singleBuyPoint.getIndexBelongPricePercentAtThatTime(); // 当时对应指数的涨跌
                    // @update: 实际值应当小于此值, 而HighSell 的实际值, 也应当小于此值.  但是注意LowBuy是 正-->负, HighSell相反
                    if (lowPrice > execLowBuyThreshold) { // @noti: 凡是阈值, 一般都包含等于
                        continue; // 必须小于等于阈值. 当然这是 低点的 绝对具体数值限制
                    }

                    // @key: cdfCalcPrice 就是以 BuyPoint各项属性(买点时各项数据), 折算一个仓位计算等价的price, 计算仓位
                    Double cdfCalcPrice = calcEquivalenceCdfUsePriceOfLowBuy(lowPrice, indexPriceThatTime);

                    // cdf使用low 计算.  价格使用buyPrice计算
                    Double cdfOfPoint = virtualCdfAsPositionForLowBuy(ticksOfLow1, weightsOfLow1, cdfCalcPrice,
                            tickGap);
                    // @key2: 本轮后总仓位;  @noti: 已经将总仓位标准化为 1!!, 因此后面计算总仓位, 不需要 /资产数量
                    Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint / totalAssets; // 加大标准仓位,倍率设定1
                    if (epochTotalPosition > positionUpperLimit / totalAssets) { // 设置上限控制标准差.
                        epochTotalPosition = positionUpperLimit / totalAssets; // 上限
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
                        weightedPrice = weightedPrice.equals(Double.NaN) ? 0.0 : weightedPrice;
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
                        weightedPrice = weightedPrice.equals(Double.NaN) ? 0.0 : weightedPrice;
                        stockWithTotalPositionAndAdaptedPrice.put(stock, Arrays.asList(newPosition,
                                weightedPrice));
                        break outerLoop; // 此时所有资金已经用掉, 我们可以提前结束双层循环. 完成低买整个过程
                    }
                }
            }

            lowBuyResults.add(stockWithTotalPositionAndAdaptedPrice); // 0. {股票: [总仓位, 折算买入价格]} 仓位已经标准化.
            lowBuyResults.add(reachTotalLimitTimeTick); // 1.达到满仓时的时间, 当然, 也可能240, 且不满仓
            lowBuyResults.add(stockLowBuyPointsMap); // 2. 各股票买入点
            // 当前仅仅返回3项原始数据. 其他数据需要调用方计算保存
            return lowBuyResults;
        }

        /**
         * 参数为 BuyPoint 蕴含的相关数据, 计算一个等价的 用于计算cdf(本质为仓位) 的价格.
         * <p>
         * // @v1: 直接等价于 lowPrice , 即用低点实际价格决定仓位
         * // @v2: 对 indexPriceThatTime 当时大盘涨跌幅 进行加成算法
         *
         * @param lowPrice
         * @param indexPriceThatTime
         * @return
         */
        private Double calcEquivalenceCdfUsePriceOfLowBuy(Double lowPrice, Double indexPriceThatTime) {
            // return lowPrice; // @v1
            /*
             * @noti: 大盘当tick涨跌幅 加成算法:
             * 1.低买的tick是 前大后小, 且计算cdf是 按序的, 因此, 想要更大仓位, 需要 让"price" 更小.
             * 因此, 在大盘此时形势很好, 正数, 在当前低点, 我应该用更高的仓位,激进一些, 因此 让 price更小, 则需要 - (大盘正涨跌幅)
             * 同理, 大盘形势差, 需要更小的仓位, 需要更大的等价"price", 因此需要 - (大盘负数涨跌幅)
             *
             */
            return lowPrice - indexBelongThatTimePriceEnhanceArgLowBuy * indexPriceThatTime; // @v2
        }

        /**
         * 参数为 SellPoint 蕴含的相关数据, 计算一个等价的 用于计算cdf(本质为仓位) 的价格. 决定卖出仓位
         * <p>
         * // @v1: 直接等价于 lowPrice , 即用低点实际价格决定仓位
         * // @v2: 对 indexPriceThatTime 当时大盘涨跌幅 进行加成算法
         *
         * @param highPrice
         * @param indexPriceThatTime
         * @return
         */
        private Double calcEquivalenceCdfUsePriceOfHighSell(Double highPrice, Double indexPriceThatTime) {
            // return lowPrice; // @v1
            /*
             * @noti: 大盘当tick涨跌幅 加成算法: 卖出
             * 1.高卖的tick是 前小后大, 且计算cdf是 按序的, 因此, 想要更大仓位, 需要 让"price" 更大, 与低买相反
             * 因此, 在大盘此时形势很好, 正数, 在当前高点, 我应该用更小的仓位, 博更高, 因此让 price更小, 则需要 - (大盘正涨跌幅)
             * 同理, 大盘形势差, 需要更大的仓位, 需要更大的等价"price", 因此需要 - (大盘负数涨跌幅)
             *
             */
            return highPrice - indexBelongThatTimePriceEnhanceArgHighSell * indexPriceThatTime; // @v2
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

        private HashMap<Double, HashMap<String, SellPoint>> convertToTickWithStockSells(
                HashMap<String, List<SellPoint>> stockHighSellPointsMap) {
            HashMap<Double, HashMap<String, SellPoint>> res = new HashMap<>();
            for (String stock : stockHighSellPointsMap.keySet()) {
                List<SellPoint> sellPointsOfStock = stockHighSellPointsMap.get(stock);
                for (SellPoint sellPoint : sellPointsOfStock) {
                    Double timeTick = sellPoint.getTimeTick();
                    res.putIfAbsent(timeTick, new HashMap<>());
                    res.get(timeTick).put(stock, sellPoint);
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
                DataFrame<Object> dfFSLowBuyDay = getFs1mStockPriceOneDayAsDfFromTushare(connOfFS, stock,
                        lowBuyDate,
                        fsSpecialUseFields); // Arrays.asList("trade_time", "close")
                if (dfFSLowBuyDay == null || dfFSLowBuyDay.length() == 0) {
                    continue; // 无分时数据, 则没有计算结果
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
                        for (int j = i; j > 0; j--) { // i开始, 计算到0, 不包括0.  即 -1分钟到0分钟 不计
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
                                        (closeCol.get(i) + closeCol
                                                .get(i + 1)) / (2 * stdCloseOfLowBuy) - 1 : lowPrice;
                                // @update: 加入当时对应指数的涨跌幅. 9:30, 则为当日 open 的涨跌幅
                                Double indexPricePercent = getCurrentBelongIndexPricePercent(i, stock,
                                        lowBuyDate);
                                buyPoints
                                        .add(new BuyPoint(tickDoubleCol.get(i), lowPrice, buyPrice,
                                                indexPricePercent));
                            } else { // 此前有买入点, 当前价格, 应当 低于此前最后一个低点的 低点价格, 当然是百分比
                                Double lastLowPrice = buyPoints.get(buyPoints.size() - 1).getLowPricePercent();
                                if (lowPrice < lastLowPrice) { // 必须更小, 才可能添加, 符合 cdf仓位算法
                                    Double buyPrice = (i != lenth - 1) ?  // 折算买入价格. 低点和下一tick(高) 的平均值
                                            (closeCol.get(i) + closeCol
                                                    .get(i + 1)) / (2 * stdCloseOfLowBuy) - 1 : lowPrice;
                                    Double indexPricePercent = getCurrentBelongIndexPricePercent(i, stock,
                                            lowBuyDate); // 对应
                                    buyPoints.add(new BuyPoint(tickDoubleCol.get(i), lowPrice, buyPrice,
                                            indexPricePercent));
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

        private Double getCurrentBelongIndexPricePercent(int tick, String stock, String lowBuyOrHighSellDate)
                throws Exception {
            // 上证6开头, 深证 0或者3.当然这里全主板全0
            String belongIndex = stock.startsWith("6") ? "000001.SH" : "399001.SZ";
            Double price; // 时刻的价格,
            if (tick == 0) { // 读取当日open
                // lowbuy开盘作为值, map已经缓存.  因为没有9:30的大盘fs
                HashMap<String, Double> indexOpens = TushareIndexApi
                        .getIndexSingleColumnAsMapByDateRange(belongIndex,
                                dateRange, "open");
                price = indexOpens.get(lowBuyOrHighSellDate);
            } else { // 否则当读取当日大盘的fs  tick.
                String belongIndexTemp = belongIndex.replace(".", "_").toLowerCase();
                DataFrame<Object> dfTemp = MindgoFSApi.getIndexFSByTradeDate(belongIndexTemp, lowBuyOrHighSellDate,
                        Arrays.asList("close")); // 指数本质作为数据表名称, 分时同样取 close字段
                // 已转化为 000001_sh
                // 注意, tick 需要减1, 才能匹配到对应分钟close
                //Console.log(tick, belongIndexTemp, lowBuyOrHighSellDate);
                //Console.log(dfTemp);
                price = Double.valueOf(dfTemp.row(tick - 1).get(0).toString());
            }
            // 然后需要获取昨日 close, 当日就是 today,  --> tradeDate
            double preClose = TushareIndexApi.getIndexDailyCloseByTradeDate(belongIndex, tradeDate);
            //Console.log(price / preClose - 1);
            if (price == null) {
                Console.log(tick, belongIndex, lowBuyOrHighSellDate, stock);
            }
            return price / preClose - 1;
        }

    }

    public static Log log = LogFactory.get();
}




