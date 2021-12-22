package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.StrUtilSelf;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.getSaveTablenameStockSelectResultRaw;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/30/030-10:55
 */
public class KlineFormsApi {
    public static Connection conn = ConnectionFactory.getConnLocalKlineForms();
    public static Cache<String, HashMap<Long, List<String>>> stockSelectPerDayCache = CacheUtil.newLRUCache(2048);
    public static Cache<String, List<List<Double>>> formSetIdDistributionsCacheByKeyInts = CacheUtil.newLRUCache(2048);

    public static void main(String[] args) throws Exception {
        TimeInterval timer = DateUtil.timer();
        timer.start();
        getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
        Console.log(timer.intervalRestart());
        getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
        Console.log(timer.intervalRestart());
        getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
        Console.log(timer.intervalRestart());
        getStockSelectResultOfTradeDate("20220810", 172L, Arrays.asList(0, 1));
        Console.log(timer.intervalRestart());

        Console.log(getEffectiveDatesBetweenDateRangeHasStockSelectResult(Arrays.asList("20200101", "20210101"),
                Arrays.asList(0, 1)).size());
        Console.log(timer.intervalRestart());
        Console.log(getLowBuyAndHighSellDistributionByFomsetid(167L, Arrays.asList(0, 1)).get(0));
        Console.log(timer.intervalRestart());
        Console.log(getLowBuyAndHighSellDistributionByFomsetid(167L, Arrays.asList(0, 1)).get(1));
        Console.log(timer.intervalRestart());
        Console.log(getLowBuyAndHighSellDistributionByFomsetid(167L, Arrays.asList(0, 1)).get(2));
        Console.log(timer.intervalRestart());
        Console.log(getLowBuyAndHighSellDistributionByFomsetid(167L, Arrays.asList(0, 1)).get(3));
        Console.log(timer.intervalRestart());
    }

    /**
     * 给定日期, 返回对应keyInts,  当日 所有 形态集合 的选股结果  HashMap,
     *
     * @param trade_date
     * @param keyInts
     * @return 当没有选股结果时, (交易日期不对), 返回空map,不会返回null
     * @throws SQLException
     */
    @Cached(description = "形态集合选股结果已经缓存, lru256")
    public static HashMap<Long, List<String>> getStockSelectResultOfTradeDate(String trade_date, List<Integer> keyInts)
            throws SQLException {
        String cacheKey = StrUtilSelf.format("{}_{}_{}", trade_date, keyInts.get(0), keyInts.get(1));
        HashMap<Long, List<String>> res = stockSelectPerDayCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String tableName = StrUtilSelf.format(getSaveTablenameStockSelectResultRaw(), keyInts.get(0), keyInts.get(1));
        String sql = StrUtilSelf
                .format("select ts_code,form_set_ids from {} where trade_date='{}'", tableName, trade_date);
        DataFrame<Object> dfTemp = DataFrame.readSql(conn, sql);

        res = new HashMap<>();
        for (int i = 0; i < dfTemp.length(); i++) {
            List<Object> row = dfTemp.row(i);
            String stock = row.get(0).toString();
            List<Object> formSetIdsRaw = JSONUtil.parseArray(row.get(1).toString());
            List<Long> formSetIds = new ArrayList<>();
            formSetIdsRaw.stream().mapToLong(value -> Long.valueOf(value.toString())).forEach(formSetIds::add);

            for (Long formSetId : formSetIds) {
                res.putIfAbsent(formSetId, new ArrayList<>());
                res.get(formSetId).add(stock);
            }
        }
        stockSelectPerDayCache.put(cacheKey, res); // 放入缓存
        return res;
    }

    /**
     * 给定日期和具体形态集合, 返回 当日该形态集合 的选股结果列表. 因原方法不会返回null,可放心调用. 可能返回null
     *
     * @param trade_date
     * @param formSetId
     * @param keyInts
     * @return
     * @throws SQLException
     */
    public static List<String> getStockSelectResultOfTradeDate(String trade_date,
                                                               Long formSetId, List<Integer> keyInts)
            throws SQLException {
        return getStockSelectResultOfTradeDate(trade_date, keyInts).get(formSetId);
    }

    /**
     * 分时回测框架调用, 给定日期区间, 在选股结果数据表中, 筛选出有选股结果的所有日期(介于日期区间,前包后不包).
     * 同样, 需要给定 keyInts, [0,1], 即代表 next0b1s
     * 当前数据表设定: 对应脚本的  getSaveTablenameStockSelectResultRaw() 为模板, 填充keyIntes
     *
     * @return
     */
    public static List<String> getEffectiveDatesBetweenDateRangeHasStockSelectResult(List<String> statDateRange,
                                                                                     List<Integer> keyInts)
            throws SQLException {
        String tablenameTemplate = getSaveTablenameStockSelectResultRaw();
        String saveTablenameStockSelectResult = StrUtilSelf.format
                (getSaveTablenameStockSelectResultRaw(), keyInts.get(0),
                        keyInts.get(1));

        DataFrame<Object> dates = DataFrame
                .readSql(conn, StrUtilSelf.format("select trade_date from {} where trade_date>='{}' and " +
                        "trade_date<'{}'", saveTablenameStockSelectResult, statDateRange.get(0), statDateRange.get(1)));
        List<String> dateList = DataFrameSelf.getColAsStringList(dates, "trade_date");
        HashSet<String> dateSet = new HashSet<>(dateList);
        List<String> res = new ArrayList<>(dateSet);
        res.sort(Comparator.naturalOrder());
        return res;
    }

    public static Log log = LogFactory.get();


    /**
     * 通过keyInt得到fs分布表后, 给定 形态集合id, 返回 其  Low1, 和High1 的分布. (即tick和权重, 权重列表已经处理为 和为1标准)
     *
     * @param formSetId
     * @param keyInts
     * @return 返回 四元素列表, 单元素为 List<Double>,分布对应已经标准化的 Low1 tick/权重, High1 tick/权重
     * @throws Exception
     */
    @Cached
    public static List<List<Double>> getLowBuyAndHighSellDistributionByFomsetid(Long formSetId, List<Integer> keyInts)
            throws Exception {
        String cacheKey = StrUtilSelf.format("{}__{}", formSetId, keyInts);
        List<List<Double>> res = formSetIdDistributionsCacheByKeyInts.get(cacheKey);
        if (res != null) {
            return res;
        }

        String saveTablenameLowBuyFS = StrUtilSelf.format(SettingsOfLowBuyFS.saveTablenameLowBuyFSRaw, keyInts.get(0),
                keyInts.get(1)); // 分时分析结果表
        String sql = StrUtilSelf
                .format("select stat_result_algorithm, tick_list, frequency_list\n" +
                                "from {}\n" +
                                "where form_set_id = {}\n" +
                                "  and concrete_algorithm like '%value_percent%'\n" +
                                "  and condition1 = 'strict'\n" +
                                "order by stat_result_algorithm, concrete_algorithm, condition1", saveTablenameLowBuyFS,
                        formSetId);
        DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
        if (dataFrame.length() < 6) {
            log.warn("记录不足6, 解析失败");
            throw new Exception("形态集合 分时分布解析错误. ");
        }
        // Low1分布
        DataFrame<Object> dfTemp = dataFrame
                .select(row -> row.get(0).toString().equals("Low1"));
        List<Object> tempTicks = JSONUtil.parseArray(dfTemp.get(0, 1).toString());
        List<Double> ticksOfLow1 = new ArrayList<>(); // 1.低买tick, 有利在后
        tempTicks.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(ticksOfLow1::add);
        Collections.reverse(ticksOfLow1); // Low 的tick需要反转过来, 越有利的在后面. 对应的 权重也应该反转

        List<Object> weightsOfLow1Temp = JSONUtil.parseArray(dfTemp.get(0, 2).toString());
        List<Double> weightsOfLow1 = new ArrayList<>(); // 2. 低买权重标准化, 有利在后
        weightsOfLow1Temp.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(weightsOfLow1::add);
        Collections.reverse(weightsOfLow1);

        // High1分布
        DataFrame<Object> dfHigh = dataFrame.select(row -> row.get(0).toString().equals("High1"));
        List<Object> tempValues0 = JSONUtil.parseArray(dfHigh.get(0, 1).toString());
        List<Double> ticksOfHigh1 = new ArrayList<>();
        tempValues0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(ticksOfHigh1::add);
        //Collections.reverse(tempValues); // @noti: HighSell 不应该reverse
        List<Object> tempWeights0 = JSONUtil.parseArray(dfHigh.get(0, 2).toString());
        List<Double> weightsOfHigh1 = new ArrayList<>();
        tempWeights0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(weightsOfHigh1::add);
        //Collections.reverse(tempWeights);
        res = Arrays.asList(ticksOfLow1, weightsOfLow1, ticksOfHigh1, weightsOfHigh1);
        formSetIdDistributionsCacheByKeyInts.put(cacheKey, res);
        return res;
    }
}
