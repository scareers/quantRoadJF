package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.parameter;

import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.utils.StrUtilSelf;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
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
        singleTableGroupByFormsetidAvg();
    }

    public static void singleTableGroupByFormsetidAvg() throws Exception {
        List<String> tables = getResultTables();
        // //单表 分form_set_id,  全日期 avg
        String sql = "select form_set_id,\n" +
                "       avg(lb_weighted_buy_price)             as bp,\n" +
                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
                "       avg(lb_global_position_sum)            as position,\n" +
                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
                "       avg(hs_success_global_percent)         as hss,\n" +
                "       avg(hs_success_global_price)           as hsp,\n" +
                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
                "from `{}` `fblhn0b1sip-5.0-3.0`\n" +
                "group by form_set_id\n" +
                "order by profit desc";
        //"form_set_id"
        List<String> columns = Arrays
                .asList("bp", "simplebp", "position", "stockcount", "hss", "hsp", "profit");
        HashMap<String, HashMap<Integer, HashMap<String, Double>>> res = new HashMap<>();
        for (String table : tables) {
            Console.log("parsing: {}", table);
            String fullSql = StrUtilSelf.format(sql, table);
            DataFrame<Object> dfTemp = DataFrame.readSql(klineForms, fullSql);

            HashMap<Integer, HashMap<String, Double>> resOfPerFormsetid = new HashMap<>();

            for (int i = 0; i < dfTemp.length(); i++) {
                List<Object> row = dfTemp.row(i);
                HashMap<String, Double> singleRecord = new HashMap<>();
                for (int j = 0; j < columns.size(); j++) {
                    String key = columns.get(j);
                    Double value = Double.valueOf(row.get(1 + j).toString());
                    singleRecord.put(key, value);
                }
                Integer formSetId = Integer.valueOf(row.get(0).toString());
                resOfPerFormsetid.put(formSetId, singleRecord);
            }
            res.put(table, resOfPerFormsetid);

        }

        Console.log(JSONUtil.toJsonPrettyStr(res));
        Console.log("开始写入json文件");
        FileWriter writer = new FileWriter("singleTableGroupByFormsetidAvg.json");
        writer.write(JSONUtil.toJsonPrettyStr(res));
        Console.log("finish");
    }

    public static void singleTableAllAvg() throws Exception {
        List<String> tables = getResultTables();
        // //单表全avg
        String sql = "select avg(lb_weighted_buy_price)             as bp,\n" +
                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
                "       avg(lb_global_position_sum)            as position,\n" +
                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
                "       avg(hs_success_global_percent)         as hss,\n" +
                "       avg(hs_success_global_price)           as hsp,\n" +
                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
                "from `{}`";

        List<String> columns = Arrays.asList("bp", "simplebp", "position", "stockcount", "hss", "hsp", "profit");
        HashMap<String, HashMap<String, Double>> res = new HashMap<>();
        for (String table : tables) {
            Console.log("parseing: {}", table);
            HashMap<String, Double> singleRes = new HashMap<>();
            String fullSql = StrUtilSelf.format(sql, table);
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
        Console.log("开始写入json文件");
        FileWriter writer = new FileWriter("singleTableAllAvg.json");
        writer.write(JSONUtil.toJsonPrettyStr(res));
        Console.log("finish");
    }

    public static List<String> getResultTables() throws SQLException {
        List<String> tables = getAllTables(klineForms);
        tables = tables.stream().filter(value -> value.startsWith("fs_backtest_lowbuy_highsell_next0b1s"))
                .collect(Collectors.toList());
        return tables;
    }

    public static void renameAllTable() throws Exception {
        if (true) {
            throw new Exception("本函数不再调用;已经改过名了");
        }
        List<String> tables = getResultTables();
        int prefixLenth = "fs_backtest_lowbuy_highsell_next0b1s".length();
        for (String tablename : tables) {
            String newTablename = StrUtilSelf.format("{}_{}{}", tablename.substring(0, prefixLenth), "index_percent",
                    tablename.substring(prefixLenth, tablename.length()));
            renameTable(klineForms, tablename, newTablename);
        }
    }


}
