package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.parameter;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.StrUtil;
import joinery.DataFrame;
import org.apache.poi.hssf.usermodel.HSSFName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.scareers.sqlapi.CommonSqlApi.getAllTables;
import static com.scareers.sqlapi.CommonSqlApi.renameTable;

/**
 * description: 大盘指数, 在买卖点时间, 实时的涨跌幅, 对仓位进行影响加成. 结果分析
 *
 * @author: admin
 * @date: 2021/12/13/013-14:17
 */
public class IndexRealTimeRaiseFallParameter {
    public static Connection klineForms = ConnectionFactory.getConnLocalKlineForms();

    public static void main(String[] args) throws Exception {
        List<String> tables = getAllTables(klineForms);
        tables = tables.stream().filter(value -> value.startsWith("fs_backtest_lowbuy_highsell_next0b1s"))
                .collect(Collectors.toList());
//        String sql = "select avg(lb_weighted_buy_price)             as bp,\n" +
//                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
//                "       avg(lb_global_position_sum)            as position,\n" +
//                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
//                "       avg(hs_success_global_percent)         as hss,\n" +
//                "       avg(hs_success_global_price)           as hsp,\n" +
//                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
//                "from `{}`";
// avg

        String sql = "select avg(lb_weighted_buy_price)             as bp,\n" +
                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
                "       avg(lb_global_position_sum)            as position,\n" +
                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
                "       avg(hs_success_global_percent)         as hss,\n" +
                "       avg(hs_success_global_price)           as hsp,\n" +
                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
                "from `{}`";
// max, 需要 groupby

        List<String> columns = Arrays.asList("bp", "simplebp", "position", "stockcount", "hss", "hsp", "profit");
        HashMap<String, HashMap<String, Double>> res = new HashMap<>();
        for (String table : tables) {
            Console.log("parseing: {}", table);
            HashMap<String, Double> singleRes = new HashMap<>();
            String fullSql = StrUtil.format(sql, table);
            DataFrame<Object> dfTemp = DataFrame.readSql(klineForms, fullSql);
            int colCount = dfTemp.size();
            for (int i = 0; i < colCount; i++) {
                Double value = Double.valueOf(dfTemp.col(i).get(0).toString());
                String key = columns.get(i);
                singleRes.put(key, value);
            }
            res.put(table, singleRes);
        }

        Console.log(JSONUtil.toJsonPrettyStr(res));
    }

    public static void renameAllTable() throws Exception {
        if (true) {
            throw new Exception("本函数不再调用;已经改过名了");
        }
        List<String> tables = getAllTables(klineForms);
        tables = tables.stream().filter(value -> value.startsWith("fs_backtest_lowbuy_highsell_next0b1s"))
                .collect(Collectors.toList());
        int prefixLenth = "fs_backtest_lowbuy_highsell_next0b1s".length();
        for (String tablename : tables) {
            String newTablename = StrUtil.format("{}_{}{}", tablename.substring(0, prefixLenth), "index_percent",
                    tablename.substring(prefixLenth, tablename.length()));
            renameTable(klineForms, tablename, newTablename);
        }
    }


}
