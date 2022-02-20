package com.scareers.keyfuncs.positiondecision;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.StrUtil;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static com.scareers.keyfuncs.positiondecision.PositionOfLowBuyByDistribution.*;
import static com.scareers.utils.CommonUtil.*;

/**
 * description: Low, High等相关分布, 决定买卖点出现时, 的仓位.
 * // 使用模拟实验决定相关参数设置, 而非正面实现逻辑
 * ----------- 假设
 * 1.假定每种情况下, Low1/2/3 均出现一次, 限定最多每只股票 买入/卖出 3次?
 * <p>
 * ----------- 问题
 * 1.给定 Low1, Low2, Low3 分布,
 * <p>
 * // @noti:
 * 1. HighSell 没有 totalAsserts 概念, 总仓位, 依据传递而来的 LowBuy持仓结果, 计算总仓位. 因此LowBuy尽量满仓.
 * 2.高卖的例如 纯高卖收益, 低买高卖收益等指标, 均基于 给定原始仓位.  而非30这种总仓位. 因此低买应该尽量满仓,
 *
 * @author: admin
 * @date: 2021/11/25/025-9:51
 */
public class PositionOfHighSellByDistribution {
    public static List<List<Double>> valuePercentOfHighx; // @key: 列表需要对我们不利的在前
    public static List<List<Double>> weightsOfHighx;
    public static Double tickGap;

    public static Double positionCalcKeyArgsOfCdf = 1.5; // 控制单股cdf倍率, 卖出速度.  1-2之间变化明显.
    public static final Double execHighSellThreshold = -0.01; // 必须 >0.01阈值, 才可能执行高卖,
    public static Double discountRemaingRate = -0.0; // 未能高卖成功的剩余部分, 以 0.0折算
    private static boolean showStockWithPositionFinally = false;

    public static void main(String[] args) throws IOException, SQLException {
        mainOfHighSell();
    }

    public static void mainOfHighSell() throws IOException, SQLException {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        // lowBuy初始化. 然后调用. 保存持仓结果, 以此结果再尝试高卖
        PositionOfLowBuyByDistribution.initDistributions();
        Console.log(valuePercentOfLowx);
        Console.log(weightsOfLowx);

        int loops = perLoops;
        List<Integer> sizes = new ArrayList<>();
        List<Double> totolPositions = new ArrayList<>();
        List<Boolean> reachTotalLimitInLoops = new ArrayList<>();
        List<Integer> epochs = new ArrayList<>();
        List<Double> weightedGlobalPrices = new ArrayList<>();
        List<HashMap<Integer, Double>> stockWithPositionList = new ArrayList<>();
        List<HashMap<Integer, List<Double>>> stockWithActualValueAndPositionList = new ArrayList<>();
        List<Integer> loopList = range(loops);
        List<Double> stds = new ArrayList<>();
        for (Integer i : Tqdm.tqdm(loopList, String.format("LowBuy process: "))) {
            List<Object> res = mainOfLowBuyCore();
            LowBuyResultParser parser = new LowBuyResultParser(res);
            HashMap<Integer, Double> positions = parser.getStockWithPosition();
            Boolean reachTotalLimitInLoop = parser.getReachTotalLimitInLoop();
            sizes.add(countNonZeroValueOfMap(positions));
            totolPositions.add(sumOfListNumber(new ArrayList<>(positions.values())));
            reachTotalLimitInLoops.add(reachTotalLimitInLoop);
            epochs.add(parser.getEpochCount()); // 跳出时执行到的轮次.  2代表判定到了 Low3
            weightedGlobalPrices.add(parser.getWeightedGlobalPrice());

            stockWithPositionList.add(positions); // 仓位在 0          @noti: HighSell新增
            stockWithActualValueAndPositionList.add((HashMap<Integer, List<Double>>) res.get(3)); // 仓位+价格在 3

            List<Double> positionsValue = positions.values().stream().collect(Collectors.toList());
            DataFrame<Double> dfx = new DataFrame<>();
            dfx.add("temp", positionsValue); // 唯一一列
            Double std = dfx.stddev().get(0, 0);
            stds.add(std);
        }
        // @noti: 这些是低买状况分析
        Console.log();
        Console.log("总计股票数量/资产总量: {}", PositionOfLowBuyByDistribution.totalAssets);
        Console.log("平均有仓位股票数量: {}", sizes.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("平均总仓位: {}",
                totolPositions.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("未循环完成Low3,中途退出比例: {}", countTrueOfListBooleans(reachTotalLimitInLoops) / (double) loops);
        Console.log("平均循环轮次: {}", epochs.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("平均交易价位: {}",
                weightedGlobalPrices.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("各股票仓位标准差: {}", stds.stream().mapToDouble(value -> value).average().getAsDouble());
        Console.log("LowBuy 耗时: {}s , 循环次数: {}", timer.intervalRestart() / 1000, loops);
        // 高卖初始化
        initDistributions();
        Console.log(valuePercentOfHighx);
        Console.log(weightsOfHighx);
        List<Double> profits = new ArrayList<>();
        List<Double> highSellProfits = new ArrayList<>();
        List<Double> highSellSuccessPercent = new ArrayList<>();

        // @noti: 使用低买结果, 尝试高卖, 并获得结果
        List<Integer> loopListOfHighSell = range(stockWithPositionList.size());
        for (Integer i : Tqdm.tqdm(loopListOfHighSell, StrUtil.format("HighSell process: "))) {
            // @bugfix: 这里应该是 i,  之前固定成了0, 重大bug. 虽然结果相差不大
            List<Object> highResult = mainOfHighSellCore(stockWithPositionList.get(i),
                    stockWithActualValueAndPositionList.get(i));
            HighSellParser parser = new HighSellParser(highResult);
            Double equalityProfitTwoDay = parser.getAllProfitsDiscountedProfitWeighted();
            profits.add(equalityProfitTwoDay);
            highSellProfits.add(parser.getWeightedGlobalPriceHighSellFinally()); // 纯高卖收益
            highSellSuccessPercent.add(parser.getHighSellSuccessPositionPercent()); // 高卖成功仓位总比例
        }
        Console.log();
        Console.log("高卖成功仓位占原始仓位平均比例: {}",
                highSellSuccessPercent.stream().mapToDouble(value -> value.doubleValue()).sum() / profits.size());
        Console.log("未成功高卖部分 折算价格: {}", discountRemaingRate);
        Console.log("纯高卖平均收益: {}",
                highSellProfits.stream().mapToDouble(value -> value.doubleValue()).sum() / profits.size());
        Console.log("低买高卖总体平均收益: {}",
                profits.stream().mapToDouble(value -> value.doubleValue()).sum() / profits.size());
        Console.log("HighSell 耗时: {}s , 循环次数: {}", timer.intervalRestart() / 1000, loops);
        /*
            HighSellParser parser = new HighSellParser(highResult);
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getStockWithPosition()));
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getStockWithHighSellActualValueAndPosition()));
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getStockWithPositionRemaining()));
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getStockWithHighSellActualValueAndPositionDiscountAll()));
            //        Console.com.scareers.log(parser.getWeightedGlobalPriceHighSellSuccess()); // Double, 不能转换json字符串, 转换为空
            //        Console.com.scareers.log(parser.getWeightedGlobalPriceHighSellFinally());
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getStockWithActualValueAndPosition()));
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.successHighSellPartProfits()));
            //        Console.com.scareers.log(parser.getSuccessPartProfitWeighted());
            //        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(parser.getAllProfitsDiscounted()));
            Console.com.scareers.log(parser.getAllProfitsDiscountedProfitWeighted());
         */

    }


    /**
     * 需要给定已有持仓.  参数2包含已有持仓 + 折算买入价格 (负数)
     *
     * @param stockWithPosition               股票:持仓
     * @param stockWithActualValueAndPosition 股票:[持仓,折算价格]  // 相当于包含了参数1
     * @return
     * @throws IOException
     */
    public static List<Object> mainOfHighSellCore(HashMap<Integer, Double> stockWithPosition,
                                                  HashMap<Integer, List<Double>> stockWithActualValueAndPosition)
            throws IOException {
        // @noti: 高卖算法, 与低买有很大区别.
        // 给定已有持仓, 用一定算法, 算出高卖持仓(折算比例) (类似低买过程)
        // 用原始持仓, 减去 算法卖出持仓, 剩余仓位, 以0卖出
        // 得到整体的高卖能力


        // 1.获取三个分布 的随机数生成器. key为 low/high几?
        HashMap<Integer, WeightRandom<Double>> highWithRandom = new HashMap<>();
        highWithRandom.put(1, getDistributionsOfHigh1());
        highWithRandom.put(2, getDistributionsOfHigh2());
        highWithRandom.put(3, getDistributionsOfHigh3());
        totalAssets = (double) stockWithPosition.size(); // 这里可能某些资产仓位0
        // 2.简单int随机, 取得某日是 出现2个低点还是 3个低点. 当然, 2个低点, Low3生成器用不到

        List<Integer> stockIds = new ArrayList<>(stockWithPosition.keySet()); // 资产列表
        HashMap<Integer, List<Integer>> stockHighOccurrences = buildStockOccurrences2(stockIds, 3); // 构造单只股票,
        // 出现了哪些Low. 且顺序随机
        // Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(stockLowOccurrences)); // 每只股票, High1,2,3 出现顺序不确定. 且3可不出现
        // 股票和对应的position, 已有仓位, 初始0
        // stock: [折算position, 折算value]
        // 专门保存卖出的仓位.以及折算
        // @noti: 它将最终与初始状态进行减法, 最终未能卖出的, 0.0处理
        HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPosition = new HashMap<>();
        // 高卖几乎无法完美, 因此, 不需要保存轮次相关状态
        for (int epoch = 0; epoch < 3; epoch++) { // 最多三轮, 某些股票第三轮将没有 Highx出现, 注意判定
            // 每一轮可能有n对股票配对成功, 这里暂存, 最后再将这些移除股票池, 然后加入完成池
            // @noti: @key2: 使用配对策略, 因此单个股票总仓配置2,而非1. 单次仓位, 则为 2* cdf(对应分布of该值)..
            // 完全决定本轮后的总仓位, 后面统一配对, 再做修改
            for (Integer id : stockIds) {
                // 已经卖出的仓位.
                stockWithHighSellActualValueAndPosition.putIfAbsent(id, new ArrayList<>(Arrays.asList(0.0, 0.0)));
                // 第epoch轮, 出现的 Low 几?
                List<Integer> highs = stockHighOccurrences.get(id);
                if (epoch >= highs.size()) {
                    continue; // 有些股票没有Low3, 需要小心越界. 这里跳过
                }
                Integer highx = stockHighOccurrences.get(id).get(epoch); // 出现的high几?
                WeightRandom<Double> random = highWithRandom.get(highx); // 获取到随机器
                // @key: high实际值, cdf等
                Double actualValue = Double.parseDouble(random.next().toString());  // 具体的High出现时的 真实值
                actualValue = actualValue - Math.abs(Math.random() * tickGap); // 同lowBUy. 这里实际值,也应该是<的
                if (actualValue < execHighSellThreshold) {
                    continue; // 必须大于一个阈值, 才可能执行卖出操作
                }

                // 此值以及对应权重应当被保存
                List<Double> valuePercentOfHigh = valuePercentOfHighx.get(highx - 1); // 出现low几? 得到值列表
                List<Double> weightsOfHigh = weightsOfHighx.get(highx - 1);
                if (forceFirstDistributionDecidePosition) {
                    valuePercentOfHigh = valuePercentOfHighx.get(0);
                    weightsOfHigh = weightsOfHighx.get(0);
                }
                Double cdfOfPoint = virtualCdfAsPositionForHighSell(valuePercentOfHigh, weightsOfHigh,
                        actualValue); // 得到卖出cdf和仓位

                // @key: 这里比低买, 多了原始仓位的 乘法因子. 不能项lowbuy一样,默认1
                Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint * stockWithPosition
                        .get(id); // 因两两配对,
                if (epochTotalPosition > stockWithPosition.get(id)) {
                    epochTotalPosition = stockWithPosition.get(id); // 上限应该是所有已有持仓.
                }

                Double oldPositionTemp = stockWithHighSellActualValueAndPosition
                        .get(id).get(0); // 已经卖出过的仓位
                List<Double> oldStockWithPositionAndValue = stockWithHighSellActualValueAndPosition
                        .get(id); // 默认0,0, 已经折算

                if (oldPositionTemp < epochTotalPosition) { // 本次真实执行了卖出 刷新卖出状况
                    // 此时需要对 仓位和均成本进行折算. 新的一部分, 价格为 actualValue, 总仓位 epochTotalPosition.
                    // 旧的一部分, 价格 stockWithPositionAndValue.get(1), 旧总仓位 stockWithPositionAndValue.get(0)
                    // 单步折算.
                    Double weightedPrice =
                            (oldStockWithPositionAndValue.get(0) / epochTotalPosition) * oldStockWithPositionAndValue
                                    .get(1) + actualValue * (1 - oldStockWithPositionAndValue
                                    .get(0) / epochTotalPosition);
                    stockWithHighSellActualValueAndPosition.put(id, Arrays.asList(epochTotalPosition, weightedPrice));
                }
                // 几乎无法全部股票恰好全部卖出, 因此, 不执行相关判定.  循环完成后, 返回前判定剩余
            }
        }
        // 用原始仓位 - 高卖执行的总仓位
        // 做减法, 得到剩余未能卖出的持仓, 并且全部 以0 折算, 并更新掉 stockWithHighSellActualValueAndPosition
        HashMap<Integer, Double> stockWithPositionRemaining =
                subRawPositionsWithHighSellExecPositions(stockWithPosition,
                        stockWithHighSellActualValueAndPosition);
        // 此为将未能卖出仓位, 折算进高卖成功仓位, 后的状态. 需要计算
        HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPositionDiscountAll =
                discountSuccessHighSellAndRemaining(stockWithPositionRemaining, stockWithHighSellActualValueAndPosition,
                        discountRemaingRate);


        List<Object> res = new ArrayList<>();
        res.add(stockWithPosition); // 0: 原始仓位
        res.add(stockWithHighSellActualValueAndPosition);// 1.高卖成功的仓位 和 价格
        res.add(stockWithPositionRemaining); // 2.剩余仓位 未能成功卖出
        res.add(stockWithHighSellActualValueAndPositionDiscountAll); // 3.用 0.0折算剩余仓位, 最终卖出仓位+价格. 此时仓位与原始同,全部卖出
        if (showStockWithPositionFinally) {
            Console.log(JSONUtilS.toJsonPrettyStr(stockWithHighSellActualValueAndPositionDiscountAll));
        }
        Double weightedGlobalPriceHighSellSuccess = calcWeightedGlobalPrice2(
                stockWithHighSellActualValueAndPosition); // 高卖成功部分折算价格
        Double weightedGlobalPriceHighSellFinally = calcWeightedGlobalPrice2(
                stockWithHighSellActualValueAndPositionDiscountAll); // 折算剩余, 最终折算价格
        res.add(weightedGlobalPriceHighSellSuccess); // 4.高卖成功部分, 折算价格
        res.add(weightedGlobalPriceHighSellFinally); // 5.折算后, 总体折算高卖价格
        res.add(stockWithActualValueAndPosition); // 6.原始低买时仓位+价格
        HashMap<Integer, List<Double>> successPartProfits = profitOfHighSell(stockWithActualValueAndPosition,
                stockWithHighSellActualValueAndPosition);
        res.add(successPartProfits); // 7.只计算高卖成功部分, 仓位+盈利值
        Double successPartProfitWeighted = calcWeightedGlobalPrice2(successPartProfits);
        res.add(successPartProfitWeighted); // 8.高卖成功部分, 整体的 加权盈利值!!
        HashMap<Integer, List<Double>> allProfitsDiscounted = profitOfHighSell(stockWithActualValueAndPosition,
                stockWithHighSellActualValueAndPositionDiscountAll);
        res.add(allProfitsDiscounted); // 9.全部, 仓位+盈利值
        Double allProfitsDiscountedProfitWeighted = calcWeightedGlobalPrice2(allProfitsDiscounted);
        res.add(allProfitsDiscountedProfitWeighted); // 10.高卖成功部分, 整体的 加权盈利值!!
        // @add: 新增, 计量 高卖成功总仓位 / 原始总仓位 , 得到高卖成功比例
        res.add(stockWithHighSellActualValueAndPosition.values().stream()
                .mapToDouble(value -> value.get(0).doubleValue())
                .sum() / stockWithHighSellActualValueAndPositionDiscountAll.values().stream()
                .mapToDouble(value -> value.get(0).doubleValue()).sum()); // 11.高卖成功总仓位 / 原始传递总仓位(尽量满仓但是达不到)
        return res;
    }

    public static Double calcWeightedGlobalPrice2(HashMap<Integer, List<Double>> stockWithActualValueAndPosition) {
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

    public static class HighSellParser {
        List<Object> highSellRes;

        public HighSellParser(List<Object> highSellRes) {
            this.highSellRes = highSellRes;
        }

        /**
         * 低买传递来的参数1
         *
         * @return 返回原始低买后, 传递来高卖 的仓位.  股票:仓位
         */
        public HashMap<Integer, Double> getStockWithPosition() {
            return (HashMap<Integer, Double>) highSellRes.get(0);
        }

        /**
         * @return 高卖成功执行掉的 仓位和价格. 股票:[高卖成功仓位, 折算价格]
         */
        public HashMap<Integer, List<Double>> getStockWithHighSellActualValueAndPosition() {
            return (HashMap<Integer, List<Double>>) highSellRes.get(1);
        }

        /**
         * @return 最终未能成功高卖, 剩余仓位.  股票: 剩余仓位
         */
        public HashMap<Integer, Double> getStockWithPositionRemaining() {
            return (HashMap<Integer, Double>) highSellRes.get(2);
        }

        /**
         * @return 用 0.0(或者其他设置值)将剩余仓位强制卖出,
         * 然后与高卖成功部分折算.全部卖出时的状态. 股票:[折算总仓位(等于初始),折算价格]
         */
        public HashMap<Integer, List<Double>> getStockWithHighSellActualValueAndPositionDiscountAll() {
            return (HashMap<Integer, List<Double>>) highSellRes.get(3);
        }

        /**
         * @return 仅计算高卖成功部分, 加权总折算价格    Double
         */
        public Double getWeightedGlobalPriceHighSellSuccess() {
            return (Double) highSellRes.get(4);
        }

        /**
         * @return 折算全部卖出的情况下, 最终的加权折算价格 Double
         */
        public Double getWeightedGlobalPriceHighSellFinally() {
            return (Double) highSellRes.get(5);
        }

        /**
         * 低买传递来的参数2
         *
         * @return 低买传递来的持仓情况. 股票:[持仓, 价格]
         */
        public HashMap<Integer, List<Double>> getStockWithActualValueAndPosition() {
            return (HashMap<Integer, List<Double>>) highSellRes.get(6);
        }

        /**
         * @return 只计算高卖成功部分, 仓位+盈利值(即高卖-低买成本).  股票:[仓位, 盈利值]
         */
        public HashMap<Integer, List<Double>> successHighSellPartProfits() {
            return (HashMap<Integer, List<Double>>) highSellRes.get(7);
        }

        /**
         * @return 只计算高卖成功部分,  加权的盈利值(假设了低买也是买的那么多仓位) Double
         */
        public Double getSuccessPartProfitWeighted() {
            return (Double) highSellRes.get(8);
        }

        /**
         * @return 全部折算卖出后, 盈利值.    股票:[仓位, 盈利值]
         */
        public HashMap<Integer, List<Double>> getAllProfitsDiscounted() {
            return (HashMap<Integer, List<Double>>) highSellRes.get(9);
        }

        /**
         * @return 全部折算卖出后, 加权盈利值 Double
         */
        public Double getAllProfitsDiscountedProfitWeighted() {
            return (Double) highSellRes.get(10);
        }

        public Double getHighSellSuccessPositionPercent() {
            return (Double) highSellRes.get(11);
        }
    }


    public static Log log = LogFactory.get();

    public static void initDistributions() throws SQLException {
        DataFrame<Object> dataFrame = DataFrame.readSql(ConnectionFactory.getConnLocalKlineForms(),
                "select form_set_id, (max(virtual_geometry_mean) - min(virtual_geometry_mean)) as width, avg(effective_counts) as ec\n" +
                        "from fs_distribution_of_lowbuy_highsell_next0b1s fdolhn0b1s\n" +
                        "where effective_counts\n" +
                        "    > 4000\n" +
                        "  and concrete_algorithm like '%value_percent%'\n" +
                        "  and condition1 = 'strict'\n" +
                        "  and stat_result_algorithm like '%1%'\n" +
                        "group by form_set_id\n" +
                        "order by width desc");
        List<Integer> formSetIds = DataFrameS.getColAsIntegerList(dataFrame, "form_set_id");
        flushDistributions(formSetIds.get(formSetIdControll));
    }


    public static void flushDistributions(Integer formSetId) throws SQLException {
        Console.log(formSetId);
        String sql = StrUtil.format("select stat_result_algorithm, tick_list, frequency_list\n" +
                "from fs_distribution_of_lowbuy_highsell_next0b1s fdolhn0b1s\n" +
                "where form_set_id = {}\n" +
                "  and concrete_algorithm like '%value_percent%'\n" +
                "  and condition1 = 'strict'\n" +
                "order by stat_result_algorithm, concrete_algorithm, condition1", formSetId);
        DataFrame<Object> dataFrame = DataFrame.readSql(ConnectionFactory.getConnLocalKlineForms(), sql);
        if (dataFrame.length() < 6) {
            log.warn("记录不足6, 解析失败");
        }
        List<List<Double>> valuePercentOfHighxTemp = new ArrayList<>();
        List<List<Double>> weightsOfHighxTemp = new ArrayList<>();

        for (int i = 1; i < 4; i++) { //High
            int finalI = i;
            DataFrame<Object> dfTemp = dataFrame
                    .select(row -> row.get(0).toString().equals(StrUtil.format("High{}", finalI)));

            List<Object> tempValues0 = JSONUtilS.parseArray(dfTemp.get(0, 1).toString());
            List<Double> tempValues = new ArrayList<>();
            tempValues0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(tempValues::add);
            //Collections.reverse(tempValues); // @noti: HighSell 不应该reverse
            valuePercentOfHighxTemp.add(tempValues);
            List<Object> tempWeights0 = JSONUtilS.parseArray(dfTemp.get(0, 2).toString());
            List<Double> tempWeights = new ArrayList<>();
            tempWeights0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(tempWeights::add);
            //Collections.reverse(tempWeights);
            weightsOfHighxTemp.add(tempWeights);
        }
        valuePercentOfHighx = valuePercentOfHighxTemp;
        weightsOfHighx = weightsOfHighxTemp;
        tickGap = // @noti: tick之间间隔必须固定, 在产生随机数时需要用到, todo: 对应的cdf也需要修改.
                Math.abs(Double.valueOf(valuePercentOfHighx.get(1).get(1).toString()) - Double
                        .valueOf(weightsOfHighx.get(1).get(0).toString())); // 间隔也刷新
    }


    private static HashMap<Integer, List<Double>> profitOfHighSell(
            HashMap<Integer, List<Double>> stockWithActualValueAndPosition,
            HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPosition) {
        // 仓位仅仅以高卖成功的计算, 无视原始总仓位. 计算价差后, 同样以卖出仓位作为权重.
        HashMap<Integer, List<Double>> positionWithProfit = new HashMap<>();
        for (Integer key : stockWithHighSellActualValueAndPosition.keySet()) {
            Double newPosition = stockWithHighSellActualValueAndPosition.get(key).get(0);
            Double profit =
                    stockWithHighSellActualValueAndPosition.get(key).get(1) - stockWithActualValueAndPosition
                            .getOrDefault(key, Arrays.asList(0.0, 0.0)).get(1);
            positionWithProfit.put(key, Arrays.asList(newPosition, profit));
        }
        return positionWithProfit;
    }

    private static HashMap<Integer, List<Double>> discountSuccessHighSellAndRemaining(
            HashMap<Integer, Double> stockWithPositionRemaining,
            HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPosition,
            Double discountRemaingPrice) {
        HashMap<Integer, List<Double>> res = new HashMap<>();
        for (Integer key : stockWithPositionRemaining.keySet()) {
            Double remainPosition = stockWithPositionRemaining.get(key);
            Double successSellPosition = stockWithHighSellActualValueAndPosition.get(key).get(0);
            Double successSellPrice = stockWithHighSellActualValueAndPosition.get(key).get(1);
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

    private static HashMap<Integer, Double> subRawPositionsWithHighSellExecPositions(
            HashMap<Integer, Double> stockWithPosition,
            HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPosition) {
        HashMap<Integer, Double> res = new HashMap<>();
        // 做减法.得到剩余未能成功卖出仓位
        for (Integer key : stockWithPosition.keySet()) {
            //注意可能一点都没有高卖掉
            res.put(key,
                    stockWithPosition.get(key) - stockWithHighSellActualValueAndPosition.getOrDefault(key,
                            Arrays.asList(0.0, 0.0)).get(0));
        }
        return res;
    }

    /**
     * 注意, 与LowBUy cdf 的算法, 只在于,  >, < 符号的差别.  因为低买tick是从大到小(不利->有利), 高卖tick相反
     *
     * @param valuePercentOfLow 值列表 tick, 要求从小到大, 或者从小到大, 即有序.  一般更不利于我们的, 放在前面.
     * @param weightsOfLow      对应权重列表
     * @param value             求该点处cdf, 是一个百分比数值
     * @return
     */
    public static Double virtualCdfAsPositionForHighSell(List<Double> valuePercentOfLow, List<Double> weightsOfLow,
                                                         Double value) {
        return virtualCdfAsPositionForHighSell(valuePercentOfLow, weightsOfLow, value, tickGap);
    }

    public static Double virtualCdfAsPositionForHighSell(List<Double> valuePercentOfLow, List<Double> weightsOfLow,
                                                         Double value, Double tickGap) {
//        Console.com.scareers.log(valuePercentOfLow);
//        Console.com.scareers.log(weightsOfLow);
//        Console.com.scareers.log(value);
        double total = 0.0;
        //Assert.isTrue(valuePercentOfLow.size() == weightsOfLow.size());
        for (int i = 0; i < valuePercentOfLow.size(); i++) {
            Double tick = valuePercentOfLow.get(i);
            if (tick < value) { // 前面的全部加入. 知道 本value在的 区间tick内
                total += weightsOfLow.get(i); // 相等时也需要加入, 因此先+
                continue; // 继续往后
            }
            // 然后还要加入一部分..  直到>
            if (i == 0) {
                break;  // 会出现索引越界,注意
            }
            Double tickPre = valuePercentOfLow.get(i - 1);
            //假设单区间内, 概率也平均叠加, 因此, 应当加入的部分是: 0到终点处概率,  * tick距离开始的百分比
            total += weightsOfLow.get(i - 1) + (weightsOfLow.get(i) - weightsOfLow
                    .get(i - 1)) * (Math.abs((value - tickPre)) / tickGap);
            break; //一次即可跳出
        }
//        double sum = sumOfListNumberUseLoop(weightsOfLow);
//        double res = total / sum;
////        Console.com.scareers.log(res);
//        return res; // 求和可能了多次
        return Math.min(total, 1.0);
    }

    public static WeightRandom<Double> getDistributionsOfHigh1() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(0), weightsOfHighx.get(0));
    }


    public static WeightRandom<Double> getDistributionsOfHigh2() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(1), weightsOfHighx.get(1));
    }

    public static WeightRandom<Double> getDistributionsOfHigh3() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(2), weightsOfHighx.get(2));
    }
}

