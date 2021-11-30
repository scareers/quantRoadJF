package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.utils.StrUtil;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.getSaveTablenameStockSelectResultRaw;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/30/030-10:55
 */
public class KlineFormsApi {
    public static Connection conn = ConnectionFactory.getConnLocalKlineForms();
    public static Cache<String, HashMap<Long, List<String>>> stockSelectPerDayCache = CacheUtil.newLRUCache(64);

    public static void main(String[] args) throws SQLException {
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
    }

    /**
     * 给定日期, 返回对应keyInts,  当日 所有 形态集合 的选股结果
     *
     * @param trade_date
     * @param keyInts
     * @return 当没有选股结果时, (交易日期不对), 返回空map,不会返回null
     * @throws SQLException
     */
    public static HashMap<Long, List<String>> getStockSelectResultOfTradeDate(String trade_date, List<Integer> keyInts)
            throws SQLException {
        String cacheKey = StrUtil.format("{}_{}_{}", trade_date, keyInts.get(0), keyInts.get(1));
        HashMap<Long, List<String>> res = stockSelectPerDayCache.get(cacheKey);
        if (res != null) {
            return res;
        }

        String tableName = StrUtil.format(getSaveTablenameStockSelectResultRaw(), keyInts.get(0), keyInts.get(1));
        String sql = StrUtil.format("select ts_code,form_set_ids from {} where trade_date='{}'", tableName, trade_date);
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
}
