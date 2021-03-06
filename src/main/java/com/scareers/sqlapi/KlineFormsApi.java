package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.annotations.Cached;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.StrUtilS;
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
//        KlineFormsApi.getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
//        Console.log(timer.intervalRestart());
//        getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
//        Console.log(timer.intervalRestart());
//        getStockSelectResultOfTradeDate("20220810", Arrays.asList(0, 1));
//        Console.log(timer.intervalRestart());
//        getStockSelectResultOfTradeDate("20220810", 172L, Arrays.asList(0, 1));
//        Console.log(timer.intervalRestart());
//
//        Console.log(getEffectiveDatesBetweenDateRangeHasStockSelectResult(Arrays.asList("20200101", "20210101"),
//                Arrays.asList(0, 1)).size());
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
     * ????????????, ????????????keyInts,  ?????? ?????? ???????????? ???????????????  HashMap,
     *
     * @param trade_date
     * @param keyInts
     * @return ????????????????????????, (??????????????????), ?????????map,????????????null
     * @throws SQLException
     */
    @Cached(description = "????????????????????????????????????, lru256")
    public static HashMap<Long, List<String>> getStockSelectResultOfTradeDate(String trade_date, List<Integer> keyInts,
                                                                              String tableName)
            throws SQLException {
        if (tableName == null) {
            tableName = StrUtilS
                    .format(getSaveTablenameStockSelectResultRaw(), keyInts.get(0), keyInts.get(1));
        }

        String cacheKey = StrUtilS.format("{}_{}_{}", trade_date, keyInts.get(0), keyInts.get(1), tableName);
        HashMap<Long, List<String>> res = stockSelectPerDayCache.get(cacheKey);
        if (res != null) {
            return res;
        }
        String sql = StrUtilS
                .format("select ts_code,form_set_ids from {} where trade_date='{}'", tableName, trade_date);
        DataFrame<Object> dfTemp = DataFrame.readSql(conn, sql);

        res = new HashMap<>();
        for (int i = 0; i < dfTemp.length(); i++) {
            List<Object> row = dfTemp.row(i);
            String stock = row.get(0).toString();
            List<Object> formSetIdsRaw = JSONUtilS.parseArray(row.get(1).toString());
            List<Long> formSetIds = new ArrayList<>();
            formSetIdsRaw.stream().mapToLong(value -> Long.valueOf(value.toString())).forEach(formSetIds::add);

            for (Long formSetId : formSetIds) {
                res.putIfAbsent(formSetId, new ArrayList<>());
                res.get(formSetId).add(stock);
            }
        }
        stockSelectPerDayCache.put(cacheKey, res); // ????????????
        return res;
    }

    @Cached(description = "????????????????????????????????????, lru256")
    public static HashMap<Long, List<String>> getStockSelectResultOfTradeDate(String trade_date, List<Integer> keyInts)
            throws SQLException {
        return getStockSelectResultOfTradeDate(trade_date, keyInts, null);
    }

    /**
     * ?????????????????????????????????, ?????? ????????????????????? ?????????????????????. ????????????????????????null,???????????????. ????????????null
     *
     * @param trade_date
     * @param formSetId
     * @param keyInts
     * @return
     * @throws SQLException .
     */
    public static List<String> getStockSelectResultOfTradeDate(String trade_date,
                                                               Long formSetId, List<Integer> keyInts)
            throws SQLException {
        return getStockSelectResultOfTradeDate(trade_date, keyInts).get(formSetId);
    }

    public static List<String> getStockSelectResultOfTradeDate(String trade_date,
                                                               Long formSetId, List<Integer> keyInts, String tableName)
            throws SQLException {
        return getStockSelectResultOfTradeDate(trade_date, keyInts, tableName).get(formSetId);
    }

    /**
     * ????????????????????????, ??????????????????, ???????????????????????????, ???????????????????????????????????????(??????????????????,???????????????).
     * ??????, ???????????? keyInts, [0,1], ????????? next0b1s
     * ?????????????????????: ???????????????  getSaveTablenameStockSelectResultRaw() ?????????, ??????keyIntes
     *
     * @return
     */
    public static List<String> getEffectiveDatesBetweenDateRangeHasStockSelectResult(List<String> statDateRange,
                                                                                     List<Integer> keyInts)
            throws SQLException {
        String tablenameTemplate = getSaveTablenameStockSelectResultRaw();
        String saveTablenameStockSelectResult = StrUtilS.format
                (getSaveTablenameStockSelectResultRaw(), keyInts.get(0),
                        keyInts.get(1));

        DataFrame<Object> dates = DataFrame
                .readSql(conn, StrUtilS.format("select trade_date from {} where trade_date>='{}' and " +
                        "trade_date<'{}'", saveTablenameStockSelectResult, statDateRange.get(0), statDateRange.get(1)));
        List<String> dateList = DataFrameS.getColAsStringList(dates, "trade_date");
        HashSet<String> dateSet = new HashSet<>(dateList);
        List<String> res = new ArrayList<>(dateSet);
        res.sort(Comparator.naturalOrder());
        return res;
    }

    public static Log log = LogFactory.get();


    /**
     * ??????keyInt??????fs????????????, ?????? ????????????id, ?????? ???  Low1, ???High1 ?????????. (???tick?????????, ??????????????????????????? ??????1??????)
     *
     * @param formSetId
     * @param keyInts
     * @return ?????? ???????????????, ???????????? List<Double>,?????????????????????????????? Low1 tick/??????, High1 tick/??????
     * @throws Exception ..
     * @noti: ????????????????????????1?????????
     */
    @Cached
    public static List<List<Double>> getLowBuyAndHighSellDistributionByFomsetid(Long formSetId, List<Integer> keyInts)
            throws Exception {
        String cacheKey = StrUtilS.format("{}__{}", formSetId, keyInts);
        List<List<Double>> res = formSetIdDistributionsCacheByKeyInts.get(cacheKey);
        if (res != null) {
            return res;
        }

        String saveTablenameLowBuyFS = StrUtilS.format(SettingsOfLowBuyFS.saveTablenameLowBuyFSRaw, keyInts.get(0),
                keyInts.get(1)); // ?????????????????????
        String sql = StrUtilS
                .format("select stat_result_algorithm, tick_list, frequency_list\n" +
                                "from {}\n" +
                                "where form_set_id = {}\n" +
                                "  and concrete_algorithm like '%value_percent%'\n" +
                                "  and condition1 = 'strict'\n" +
                                "order by stat_result_algorithm, concrete_algorithm, condition1", saveTablenameLowBuyFS,
                        formSetId);
        DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
        if (dataFrame.length() < 6) {
            log.warn("????????????6, ????????????");
            throw new Exception("???????????? ????????????????????????. ");
        }
        // Low1??????
        DataFrame<Object> dfTemp = dataFrame
                .select(row -> row.get(0).toString().equals("Low1"));
        List<Object> tempTicks = JSONUtilS.parseArray(dfTemp.get(0, 1).toString());
        List<Double> ticksOfLow1 = new ArrayList<>(); // 1.??????tick, ????????????
        tempTicks.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(ticksOfLow1::add);
        Collections.reverse(ticksOfLow1); // Low ???tick??????????????????, ?????????????????????. ????????? ?????????????????????

        List<Object> weightsOfLow1Temp = JSONUtilS.parseArray(dfTemp.get(0, 2).toString());
        List<Double> weightsOfLow1 = new ArrayList<>(); // 2. ?????????????????????, ????????????
        weightsOfLow1Temp.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(weightsOfLow1::add);
        Collections.reverse(weightsOfLow1);

        // High1??????
        DataFrame<Object> dfHigh = dataFrame.select(row -> row.get(0).toString().equals("High1"));
        List<Object> tempValues0 = JSONUtilS.parseArray(dfHigh.get(0, 1).toString());
        List<Double> ticksOfHigh1 = new ArrayList<>();
        tempValues0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(ticksOfHigh1::add);
        //Collections.reverse(tempValues); // @noti: HighSell ?????????reverse
        List<Object> tempWeights0 = JSONUtilS.parseArray(dfHigh.get(0, 2).toString());
        List<Double> weightsOfHigh1 = new ArrayList<>();
        tempWeights0.stream().mapToDouble(value -> Double.valueOf(value.toString())).forEach(weightsOfHigh1::add);
        //Collections.reverse(tempWeights);
        res = Arrays.asList(ticksOfLow1, weightsOfLow1, ticksOfHigh1, weightsOfHigh1);
        formSetIdDistributionsCacheByKeyInts.put(cacheKey, res);
        return res;
    }
}
