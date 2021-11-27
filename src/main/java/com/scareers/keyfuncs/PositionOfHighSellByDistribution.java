package com.scareers.keyfuncs;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import joinery.DataFrame;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static com.scareers.keyfuncs.PositionOfLowBuyByDistribution.buildStockOccurrences;
import static com.scareers.keyfuncs.PositionOfLowBuyByDistribution.getActualDistributionRandom;
import static com.scareers.utils.CommonUtils.*;

/**
 * description: Low, High等相关分布, 决定买卖点出现时, 的仓位.
 * // 使用模拟实验决定相关参数设置, 而非正面实现逻辑
 * ----------- 假设
 * 1.假定每种情况下, Low1/2/3 均出现一次, 限定最多每只股票 买入/卖出 3次?
 * <p>
 * ----------- 问题
 * 1.给定 Low1, Low2, Low3 分布,
 *
 * @author: admin
 * @date: 2021/11/25/025-9:51
 */
public class PositionOfHighSellByDistribution {
    public static final boolean showDistribution = false;
    public static List<List<Object>> valuePercentOfHighx; // @key: 列表需要对我们不利的在前
    //            = Arrays.asList( // Low1/2/3 的具体值刻度
//            Arrays.asList(-0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1, -0.11),
//            Arrays.asList(-0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1),
//            Arrays.asList(0.0, -0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09)
//    );
    public static List<List<Object>> weightsOfHighx;
    //            = Arrays.asList( // Low1/2/3 的权重列表
//            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.),
//            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.),
//            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.)
//    );
    public static Double tickGap;

    public static Double positionUpperLimit = 1.3; // 控制上限, 一般不大于 倍率
    public static Double positionCalcKeyArgsOfCdf = 1.5; // 控制单股cdf倍率, 一般不小于上限
    public static final Double execHighSellThreshold = 0.005; // 必须某个值 <= -0.1阈值, 才可能执行低买, 否则跳过不考虑
    public static Double totalAssets; // 总计30块钱资产. 为了方便理解. 最终结果 /30即可
    public static int perLoops = 100;
    public static Double discountRemaingRate = 0.0; // 未能高卖的剩余部分, 以 0.0折算
    private static boolean showStockWithPositionFinally = false;

    public static void main(String[] args) throws IOException, SQLException {
        mainOfHighSell();
    }

    public static void mainOfHighSell() throws IOException, SQLException {
        initDistributions();
        Console.log(valuePercentOfHighx);
        Console.log(weightsOfHighx);
        int loops = perLoops;
        List<Integer> sizes = new ArrayList<>();
        List<Double> totolPositions = new ArrayList<>();
        List<Boolean> reachTotalLimitInLoops = new ArrayList<>();
        List<Integer> epochs = new ArrayList<>();
        List<Double> weightedGlobalPrices = new ArrayList<>();

        for (int i = 0; i < loops; i++) {
            List<Object> res = mainOfHighSellCore(null);
            HashMap<Integer, Double> positions = (HashMap<Integer, Double>) res.get(0);
            Boolean reachTotalLimitInLoop = (Boolean) res.get(1);
            //            Console.log(JSONUtil.toJsonPrettyStr(positions));
            //            Console.log(stockWithActualValueAndPosition);
            sizes.add(countNonZeroValueOfMap(positions));
            totolPositions.add(sumOfListNumber(new ArrayList<>(positions.values())));
            reachTotalLimitInLoops.add(reachTotalLimitInLoop);
            epochs.add((Integer) res.get(2)); // 跳出时执行到的轮次.  2代表判定到了 Low3
            weightedGlobalPrices.add((Double) res.get(4));
        }
        Console.log("总计股票数量/资产总量: {}", totalAssets);
        Console.log("平均有仓位股票数量: {}", sizes.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("平均总仓位: {}",
                totolPositions.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("未循环完成Low3,中途退出比例: {}", countTrueOfListBooleans(reachTotalLimitInLoops) / (double) loops);
        Console.log("平均循环轮次: {}", epochs.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
        Console.log("平均交易价位: {}",
                weightedGlobalPrices.stream().mapToDouble(value -> value.doubleValue()).average().getAsDouble());
    }


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
        List<Integer> formSetIds = DataFrameSelf.getColAsIntegerList(dataFrame, "form_set_id");
        flushDistributions(formSetIds.get(0));
    }

    public static Log log = LogFactory.get();

    public static void flushDistributions(Integer formSetId) throws SQLException {
        Console.log(formSetId);
        String sql = StrUtil.format("select stat_result_algorithm, tick_list, counts_list\n" +
                "from fs_distribution_of_lowbuy_highsell_next0b1s fdolhn0b1s\n" +
                "where form_set_id = {}\n" +
                "  and concrete_algorithm like '%value_percent%'\n" +
                "  and condition1 = 'strict'\n" +
                "order by stat_result_algorithm, concrete_algorithm, condition1", formSetId);
        DataFrame<Object> dataFrame = DataFrame.readSql(ConnectionFactory.getConnLocalKlineForms(), sql);
        if (dataFrame.length() < 6) {
            log.warn("记录不足6, 解析失败");
        }
        List<List<Object>> valuePercentOfHighxTemp = new ArrayList<>();
        List<List<Object>> weightsOfHighxTemp = new ArrayList<>();

        for (int i = 1; i < 4; i++) { //High
            int finalI = i;
            DataFrame<Object> dfTemp = dataFrame
                    .select(row -> row.get(0).toString().equals(StrUtil.format("High{}", finalI)));

            List<Object> tempValues = JSONUtil.parseArray(dfTemp.get(0, 1).toString());
            Collections.reverse(tempValues);
            valuePercentOfHighxTemp.add(tempValues);
            List<Object> tempWeights = JSONUtil.parseArray(dfTemp.get(0, 2).toString());
            Collections.reverse(tempWeights);
            weightsOfHighxTemp.add(tempWeights);
        }
        valuePercentOfHighx = valuePercentOfHighxTemp;
        weightsOfHighx = weightsOfHighxTemp;
        tickGap = // @noti: tick之间间隔必须固定, 在产生随机数时需要用到, todo: 对应的cdf也需要修改.
                Math.abs(Double.valueOf(valuePercentOfHighx.get(1).get(1).toString()) - Double
                        .valueOf(weightsOfHighx.get(1).get(0).toString())); // 间隔也刷新
    }

    /**
     * 需要给定已有持仓, 以便卖出.
     *
     * @param stockWithPosition
     * @return
     * @throws IOException
     */
    public static List<Object> mainOfHighSellCore(HashMap<Integer, Double> stockWithPosition) throws IOException {
        // @noti: 高卖算法, 与低买有很大区别.
        // 给定已有持仓, 用一定算法, 算出高卖持仓(折算比例) (类似低买过程)
        // 用原始持仓, 减去 算法卖出持仓, 剩余仓位, 以0卖出
        // 得到整体的高卖能力


        // 1.获取三个分布 的随机数生成器. key为 low/high几?
        HashMap<Integer, WeightRandom<Object>> highWithRandom = new HashMap<>();
        highWithRandom.put(1, getDistributionsOfLow1());
        highWithRandom.put(2, getDistributionsOfLow2());
        highWithRandom.put(3, getDistributionsOfLow3());
        totalAssets = (double) stockWithPosition.size(); // 这里可能某些资产仓位0
        // 2.简单int随机, 取得某日是 出现2个低点还是 3个低点. 当然, 2个低点, Low3生成器用不到

        List<Integer> stockIds = new ArrayList<>(stockWithPosition.keySet()); // 资产列表
        HashMap<Integer, List<Integer>> stockHighOccurrences = buildStockOccurrences(stockIds, 3); // 构造单只股票,
        // 出现了哪些Low. 且顺序随机
        // Console.log(JSONUtil.toJsonPrettyStr(stockLowOccurrences)); // 每只股票, High1,2,3 出现顺序不确定. 且3可不出现
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
                WeightRandom<Object> random = highWithRandom.get(highx); // 获取到随机器
                // @key: high实际值, cdf等
                Double actualValue = Double.parseDouble(random.next().toString());  // 具体的High出现时的 真实值
                if (actualValue < execHighSellThreshold) {
                    continue; // 必须大于一个阈值, 才可能执行卖出操作
                }

                // 此值以及对应权重应当被保存
                List<Object> valuePercentOfHigh = valuePercentOfHighx.get(highx - 1); // 出现low几? 得到值列表
                List<Object> weightsOfHigh = weightsOfHighx.get(highx - 1);
                Double cdfOfPoint = virtualCdfAsPosition(valuePercentOfHigh, weightsOfHigh, actualValue); // 得到卖出cdf和仓位

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
        res.add(stockWithPosition); // 0元素: 原始仓位
        res.add(stockWithHighSellActualValueAndPosition);// 高卖成功的仓位 和 价格
        res.add(stockWithPositionRemaining); // 剩余仓位 未能成功卖出
        res.add(stockWithHighSellActualValueAndPositionDiscountAll); // 用 0.0折算剩余仓位, 最终卖出仓位+价格. 此时仓位与原始同,全部卖出
        if (showStockWithPositionFinally) {
            Console.log(JSONUtil.toJsonPrettyStr(stockWithHighSellActualValueAndPositionDiscountAll));
        }

        res.add(stockWithHighSellActualValueAndPosition);
        Double weightedGlobalPrice = calcWeightedGlobalPrice(stockWithHighSellActualValueAndPosition);
        res.add(weightedGlobalPrice);
        return res;
    }

    private static HashMap<Integer, List<Double>> discountSuccessHighSellAndRemaining(
            HashMap<Integer, Double> stockWithPositionRemaining,
            HashMap<Integer, List<Double>> stockWithHighSellActualValueAndPosition,
            Double discountRemaingRate) {
        HashMap<Integer, List<Double>> res = new HashMap<>();
        for (Integer key : stockWithPositionRemaining.keySet()) {
            Double remainPosition = stockWithPositionRemaining.get(key);
            Double successSellPosition = stockWithHighSellActualValueAndPosition.get(key).get(0);
            Double successSellPrice = stockWithHighSellActualValueAndPosition.get(key).get(1);
            Double totalPosition = remainPosition + successSellPosition;
            Double discountedPrice =
                    remainPosition / totalPosition * discountRemaingRate +
                            successSellPosition / totalPosition * successSellPrice; // 简单加权
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

    private static Double calcWeightedGlobalPrice(HashMap<Integer, List<Double>> stockWithActualValueAndPosition) {
        Double res = 0.0;
        for (List<Double> positionAndPrice : stockWithActualValueAndPosition.values()) {
            res += positionAndPrice.get(0) / totalAssets * positionAndPrice.get(1);
        }
        return res;
    }

    /**
     * 将临时的 股票:总仓位 map, 转换为 2元素列表的列表, 且依据仓位排序, 以方便配对; 2元素分别为 Integer,Double
     *
     * @param stockWithPosition
     * @return
     */
    public static List<List<Object>> mapTo2eleListOrderByPosition(HashMap<Integer, Double> stockWithPosition) {
        List<List<Object>> listOfOrderedStockWithPosition = new ArrayList<>();
        for (Integer key : stockWithPosition.keySet()) {
            ArrayList<Object> per = new ArrayList<>();
            per.add(key);
            per.add(stockWithPosition.get(key));
            listOfOrderedStockWithPosition.add(per);
        }
        listOfOrderedStockWithPosition.sort(Comparator.comparing(o -> ((Double) o.get(1))));// .........@noti: java也有简写
        return listOfOrderedStockWithPosition;
    }


    /**
     * 给定 可能值, 及其权重 列表, 给定某个值, 求一个模拟的 该点 cdf !!
     *
     * @param valuePercentOfLow 值列表 , 要求从小到大, 或者从小到大, 即有序.  一般更不利于我们的, 放在前面.
     * @param weightsOfLow      权重列表
     * @param value             求该点处cdf
     * @return 返回虚拟近似cdf ,
     */
    public static Double virtualCdfAsPosition(List<Object> valuePercentOfLow, List<Object> weightsOfLow, Double value) {
//        Console.log(valuePercentOfLow);
//        Console.log(weightsOfLow);
//        Console.log(value);
        double total = 0.0;
        Assert.isTrue(valuePercentOfLow.size() == weightsOfLow.size());
        for (int i = 0; i < valuePercentOfLow.size(); i++) {
            total += Double.parseDouble(weightsOfLow.get(i).toString()); // 相等时也需要加入, 因此先+
            if (value.equals(Double.valueOf(valuePercentOfLow.get(i).toString()))) { // 相等时,已被加入, 然后跳出
                break;
            }
        }
        double sum = weightsOfLow.stream().mapToDouble(value1 -> Double.parseDouble(value1.toString())).sum();
        return total / sum; // 求和可能了多次
    }

    public static WeightRandom<Object> getDistributionsOfLow1() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(0), weightsOfHighx.get(0));
    }


    public static WeightRandom<Object> getDistributionsOfLow2() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(1), weightsOfHighx.get(1));
    }

    public static WeightRandom<Object> getDistributionsOfLow3() throws IOException {
        return getActualDistributionRandom(valuePercentOfHighx.get(2), weightsOfHighx.get(2));
    }
}

