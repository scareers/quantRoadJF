package com.scareers.keyfuncs;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.lang.WeightRandom.WeightObj;
import cn.hutool.core.util.RandomUtil;

import java.io.IOException;
import java.util.*;

import static com.scareers.utils.CommonUtils.*;
import static com.scareers.utils.charts.ChartUtil.listOfDoubleAsLineChartSimple;

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
public class PositionByDistribution {
    public static final boolean showDistribution = false;
    public static List<List<Object>> valuePercentOfLowx = Arrays.asList( // Low1/2/3 的具体值刻度
            Arrays.asList(-0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1, -0.11),
            Arrays.asList(-0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09, -0.1),
            Arrays.asList(0.0, -0.01, -0.02, -0.03, -0.04, -0.05, -0.06, -0.07, -0.08, -0.09)
    );
    public static Double tickGap = // @noti: tick之间间隔必须固定, 在产生随机数时需要用到, todo: 对应的cdf也需要修改.
            Math.abs(Double.valueOf(valuePercentOfLowx.get(1).get(1).toString()) - Double
                    .valueOf(valuePercentOfLowx.get(1).get(0).toString()));
    public static List<List<Object>> weightsOfLowx = Arrays.asList( // Low1/2/3 的权重列表
            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.),
            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.),
            Arrays.asList(5., 10.0, 20., 30., 50., 50., 30., 20., 10., 5.)
    );
    public static Double positionUpperLimit = 1.2; // 控制上限, 一般不大于 倍率
    public static Double positionCalcKeyArgsOfCdf = 1.5; // 控制单股cdf倍率, 一般不小于上限
    public static Double totalAssets = 30.0; // 总计30块钱资产. 为了方便理解. 最终结果 /30即可

    public static void main(String[] args) throws IOException {
        mainOfLowBuy();
    }

    public static void mainOfLowBuy() throws IOException {

        int loops = 10000;
        List<Integer> sizes = new ArrayList<>();
        List<Double> totolPositions = new ArrayList<>();
        List<Boolean> reachTotalLimitInLoops = new ArrayList<>();
        List<Integer> epochs = new ArrayList<>();
        List<Double> weightedGlobalPrices = new ArrayList<>();

        for (int i = 0; i < loops; i++) {
            List<Object> res = mainOfLowBuyCore();
            HashMap<Integer, Double> positions = (HashMap<Integer, Double>) res.get(0);
            Boolean reachTotalLimitInLoop = (Boolean) res.get(1);
            //            Console.log(JSONUtil.toJsonPrettyStr(positions));
            //            Console.log(stockWithActualValueAndPosition);
            sizes.add(positions.size());
            totolPositions.add(sumOfListNumber(new ArrayList<>(positions.values())));
            reachTotalLimitInLoops.add(reachTotalLimitInLoop);
            epochs.add((Integer) res.get(2)); // 跳出时执行到的轮次.  2代表判定到了 Low3
            weightedGlobalPrices.add((Double) res.get(4));
        }

        Console.log(sizes.stream().mapToDouble(value -> value.doubleValue()).average());
        Console.log(totolPositions.stream().mapToDouble(value -> value.doubleValue()).average());
        Console.log(countTrueOfListBooleans(reachTotalLimitInLoops) / (double) loops);
        Console.log(epochs.stream().mapToDouble(value -> value.doubleValue()).average());
        Console.log(weightedGlobalPrices.stream().mapToDouble(value -> value.doubleValue()).average());
    }

    public static List<Object> mainOfLowBuyCore() throws IOException {
        // 1.获取三个分布 的随机数生成器. key为 low几?
        HashMap<Integer, WeightRandom<Object>> lowWithRandom = new HashMap<>();
        lowWithRandom.put(1, getDistributionsOfLow1());
        lowWithRandom.put(2, getDistributionsOfLow2());
        lowWithRandom.put(3, getDistributionsOfLow3());

        // 2.简单int随机, 取得某日是 出现2个低点还是 3个低点. 当然, 2个低点, Low3生成器用不到


        List<Integer> stockIds = range(totalAssets.intValue());
        HashMap<Integer, List<Integer>> stockLowOccurrences = buildStockLowOccurrences(stockIds, 3); // 构造单只股票,
        // 出现了哪些Low. 且顺序随机
//        Console.log(JSONUtil.toJsonPrettyStr(stockLowOccurrences)); // 每只股票, Low1,2,3 出现顺序不确定. 且3可不出现

        List<Integer> stockPool = range(totalAssets.intValue()); // 初始保存全部股票, 等待配对完成, 移除放入下列表
        HashMap<Integer, Double> stockWithPosition = new HashMap<>(); // 股票和对应的position, 已有仓位, 初始0
        // stock: [折算position, 折算value]
        HashMap<Integer, List<Double>> stockWithActualValueAndPosition = new HashMap<>();
        Boolean reachTotalLimitInLoop = false;
        int epochRaw = 0;

        // 尝试算法: 2个一组, 共分2块钱, 意味着某只股票最多得到 2块钱的分配, 且那时必须另一只股票0, 然后两只股票达成配对完成,清除股票池
        // @noti:  @Hypothesis: 假设 股票按顺序出现 第一次low(不论Low几), 然后第二次Low, 然后第三次Low

        for (int epoch = 0; epoch < 3; epoch++) { // 最多三轮, 某些股票第三轮将没有 Lowx出现, 注意判定
            epochRaw = epoch;
            // 每一轮可能有n对股票配对成功, 这里暂存, 最后再将这些移除股票池, 然后加入完成池
            // @noti: @key2: 使用配对策略, 因此单个股票总仓配置2,而非1. 单次仓位, 则为 2* cdf(对应分布of该值)..
            // 完全决定本轮后的总仓位, 后面统一配对, 再做修改
            for (Integer id : stockPool) {
                stockWithPosition.putIfAbsent(id, 0.0); // 默认0
                stockWithActualValueAndPosition.putIfAbsent(id, new ArrayList<>(Arrays.asList(0.0, 0.0)));
                // 第epoch轮, 出现的 Low 几?
                List<Integer> lows = stockLowOccurrences.get(id);
                if (epoch >= lows.size()) {
                    continue; // 有些股票没有Low3, 需要小心越界. 这里跳过
                }
                Integer lowx = stockLowOccurrences.get(id).get(epoch);

                WeightRandom<Object> random = lowWithRandom.get(lowx); // 获取到随机器
                // @key: low实际值, cdf等
                Double actualValue = Double.parseDouble(random.next().toString());  // 具体的LOw出现时的 真实值
                // 此值以及对应权重应当被保存

                List<Object> valuePercentOfLow = valuePercentOfLowx.get(lowx - 1); // 出现low几? 得到值列表
                List<Object> weightsOfLow = weightsOfLowx.get(lowx - 1);
                Double cdfOfPoint = virtualCdfAsPosition(valuePercentOfLow, weightsOfLow, actualValue);

                // @key2: 本轮后总仓位
                Double epochTotalPosition = positionCalcKeyArgsOfCdf * cdfOfPoint; // 因两两配对, 因此这里仓位使用 2作为基数. 且为该股票总仓位
                if (epochTotalPosition > positionUpperLimit) {
                    epochTotalPosition = positionUpperLimit; // 上限
                }

                Double oldPositionTemp = stockWithPosition.get(id);
                List<Double> oldStockWithPositionAndValue = stockWithActualValueAndPosition.get(id); // 默认0,0, 已经折算
                if (oldPositionTemp < epochTotalPosition) {
                    stockWithPosition.put(id, epochTotalPosition); // 必须新的总仓位, 大于此前轮次总仓位, 才需要修改!!
                    // 此时需要对 仓位和均成本进行折算. 新的一部分, 价格为 actualValue, 总仓位 epochTotalPosition.
                    // 旧的一部分, 价格 stockWithPositionAndValue.get(1), 旧总仓位 stockWithPositionAndValue.get(0)
                    // 单步折算.
                    Double weightedPrice =
                            (oldStockWithPositionAndValue.get(0) / epochTotalPosition) * oldStockWithPositionAndValue
                                    .get(1) + actualValue * (1 - oldStockWithPositionAndValue
                                    .get(0) / epochTotalPosition);
                    stockWithActualValueAndPosition.put(id, Arrays.asList(epochTotalPosition, weightedPrice));
                }
                // 对仓位之和进行验证, 一旦第一次 超过上限, 则立即退出循环.
                Double sum = stockWithPosition.values().stream().mapToDouble(value1 -> value1).sum();
                if (sum > totalAssets) { // 如果超上限, 则将本股票 epochTotalPosition 减小, 是的总仓位 刚好30, 并立即返回
                    Double newPosition = epochTotalPosition - (sum - totalAssets);
                    stockWithPosition.put(id, newPosition); // 修改仓位
                    // 折算权重也需要修正.
                    Double weightedPrice =
                            (oldStockWithPositionAndValue.get(0) / newPosition) * oldStockWithPositionAndValue
                                    .get(1) + actualValue * (1 - oldStockWithPositionAndValue.get(0) / newPosition);
                    stockWithActualValueAndPosition.put(id, Arrays.asList(epochTotalPosition, weightedPrice));

                    reachTotalLimitInLoop = true;
                    List<Object> res = new ArrayList<>();
                    res.add(stockWithPosition);
                    res.add(reachTotalLimitInLoop);
                    res.add(epoch);
                    res.add(stockWithActualValueAndPosition);
                    Double weightedGlobalPrice = calcWeightedGlobalPrice(stockWithActualValueAndPosition);
                    res.add(weightedGlobalPrice);
                    return res;
                }
            }

            // @key3: 尝试两两配对, 且修复仓位, 符合不超过2, 此时本轮 stockWithPosition 已经raw完成

        }
        List<Object> res = new ArrayList<>();
        res.add(stockWithPosition);
        res.add(reachTotalLimitInLoop);
        res.add(epochRaw);
        res.add(stockWithActualValueAndPosition);
        Double weightedGlobalPrice = calcWeightedGlobalPrice(stockWithActualValueAndPosition);
        res.add(weightedGlobalPrice);
        return res; // 循环完成仍旧没有达到过30上限, 也返回最终的仓位分布
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

    public static HashMap<Integer, List<Integer>> buildStockLowOccurrences(List<Integer> stockIds, int maxLow) {
        HashMap<Integer, List<Integer>> stockLowOccurrences = new HashMap<>();
        for (Integer stockId : stockIds) {
            ArrayList<Integer> occurrs = new ArrayList<>();
            int lenth = RandomUtil.randomInt(2, maxLow + 1); // 今天某只股票出现几个 Low?
            for (int i = 1; i < lenth + 1; i++) {
                occurrs.add(i);
            }
            Collections.shuffle(occurrs); // 底层也是al,打乱low123出现顺序
            stockLowOccurrences.put(stockId, occurrs);
        }
        return stockLowOccurrences; // 股票: 打乱的出现的 Low1,Low2,Low3, 自行对应, 得到对应的随机器
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
        double total = 0.0;
        Assert.isTrue(valuePercentOfLow.size() == weightsOfLow.size());
        for (int i = 0; i < valuePercentOfLow.size(); i++) {
            total += Double.parseDouble(weightsOfLow.get(i).toString()); // 相等时也需要加入, 因此先+
            if (value.equals(valuePercentOfLow.get(i))) { // 相等时,已被加入, 然后跳出
                break;
            }
        }
        double sum = weightsOfLow.stream().mapToDouble(value1 -> Double.parseDouble(value1.toString())).sum();
        return total / sum; // 求和可能了多次
    }

    public static WeightRandom<Object> getDistributionsOfLow1() throws IOException {
        return getActualDistributionRandom(valuePercentOfLowx.get(0), weightsOfLowx.get(0));
    }


    public static WeightRandom<Object> getDistributionsOfLow2() throws IOException {
        return getActualDistributionRandom(valuePercentOfLowx.get(1), weightsOfLowx.get(1));
    }

    public static WeightRandom<Object> getDistributionsOfLow3() throws IOException {
        return getActualDistributionRandom(valuePercentOfLowx.get(2), weightsOfLowx.get(2));
    }

    public static WeightRandom<Object> getActualDistributionRandom(List<Object> valuePercentOfLow1,
                                                                   List<Object> weightsOfLow1) throws IOException {
        Assert.isTrue(valuePercentOfLow1.size() == weightsOfLow1.size());
        // 构建 WeightObj<Double> 列表. 以构建随机器

        List<WeightObj<Object>> weightObjs = new ArrayList<>();
        for (int i = 0; i < valuePercentOfLow1.size(); i++) {
            weightObjs.add(new WeightObj<>(valuePercentOfLow1.get(i), Double.valueOf(weightsOfLow1.get(i).toString())));
        }
        if (showDistribution) {
            listOfDoubleAsLineChartSimple(weightsOfLow1, false, null, valuePercentOfLow1);
        }
        return RandomUtil.weightRandom(weightObjs);
    }
}

