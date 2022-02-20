//package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.securitylist.loybuyhighsell2;
//
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.file.FileWriter;
//import cn.hutool.core.lang.Console;
//import cn.hutool.core.util.StrUtil;
//import com.scareers.utils.JSONUtilS;
//import com.scareers.datasource.selfdb.ConnectionFactory;
//import com.scareers.pandasdummy.DataFrameS;
//import com.scareers.utils.StrUtilS;
//import com.scareers.utils.charts.ChartUtil;
//import joinery.DataFrame;
//
//import java.io.File;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static com.scareers.sqlapi.CommonSqlApi.getAllTables;
//import static com.scareers.sqlapi.CommonSqlApi.renameTable;
//
///**
// * description: 大盘指数, 在买卖点时间, 实时的涨跌幅, 对仓位进行影响加成. 相关分析
// *
// * @author: admin
// * @date: 2021/12/13/013-14:17
// */
//public class IndexRealTimeRaiseFallParameter {
//    public static Connection klineForms = ConnectionFactory.getConnLocalKlineForms();
//
//    public static void main(String[] args) throws Exception {
////        singleTableGroupByFormsetidAvg();
////
////        renameAllTable();
////        singleTableAllAvg();
//        test();
//    }
//
//
//    public static void test() throws SQLException, IOException {
////        String sql = "select lbhs_weighted_profit_conservative\n" +
////                "from `fs_backtest_lowbuy_highsell_next0b1s_v2_1.0_1.0` `fbln0b1sis1.41.61.20.00.0`\n" +
////                "where form_set_id = {} order by trade_date";
//
//
//
//        String sql = "select lbhs_weighted_profit_conservative\n" +
//                "from `fs_bk_lbhs_next0b1s_indexper_scale1.41.61.2_1.0_1.0` `fbln0b1sis1.41.61.20.00.0`\n" +
//                "where form_set_id = {} order by trade_date";
//
//
//        DataFrame<Object> dataFrame = DataFrameS.readSql(ConnectionFactory.getConnLocalKlineForms(),
//                StrUtil.format(sql, 618));
//
//
//        List<Double> doubles1 = DataFrameS.getColAsDoubleList(dataFrame, 0);
////        doubles1 = doubles1.subList(200, 400);
//
//
//        Console.log(doubles1.stream().mapToDouble(value -> value).summaryStatistics());
//
//        List<Double> doubleList = doubles1.stream().map(value -> 1 + value).collect(Collectors.toList());
//        double product = 1.0;
//
//        List<Double> products = new ArrayList<>();
//        for (Double aDouble : doubleList) {
//            product *= aDouble;
//            products.add(product);
//        }
//        Console.log(product);
//        Console.log(products);
//
//        double pow = Math.pow(product, (double) 1 / doubleList.size()) - 1;
//        Console.log(pow);
//
//        ChartUtil.listOfDoubleAsLineChartSimple(products,null,true);
//
//    }
//
//
//    public static void singleTableGroupByFormsetidAvg() throws Exception {
//        List<String> tables = getResultTables();
//        // //单表 分form_set_id,  全日期 avg
//        String sql = "select form_set_id,\n" +
//                "       avg(lb_weighted_buy_price)             as bp,\n" +
//                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
//                "       avg(lb_global_position_sum)            as position,\n" +
//                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
//                "       avg(hs_success_global_percent)         as hss,\n" +
//                "       avg(hs_success_global_price)           as hsp,\n" +
//                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
//                "from `{}` `fblhn0b1sip-5.0-3.0`\n" +
//                "group by form_set_id\n" +
//                "order by profit desc";
//        //"form_set_id"
//        List<String> columns = Arrays
//                .asList("bp", "simplebp", "position", "stockcount", "hss", "hsp", "profit");
//        HashMap<String, HashMap<Integer, HashMap<String, Double>>> res = new HashMap<>();
//        for (String table : tables) {
//            Console.log("parsing: {}", table);
//            String fullSql = StrUtilS.format(sql, table);
//            DataFrame<Object> dfTemp = DataFrame.readSql(klineForms, fullSql);
//
//            HashMap<Integer, HashMap<String, Double>> resOfPerFormsetid = new HashMap<>();
//
//            for (int i = 0; i < dfTemp.length(); i++) {
//                List<Object> row = dfTemp.row(i);
//                HashMap<String, Double> singleRecord = new HashMap<>();
//                for (int j = 0; j < columns.size(); j++) {
//                    String key = columns.get(j);
//                    Double value = Double.valueOf(row.get(1 + j).toString());
//                    singleRecord.put(key, value);
//                }
//                Integer formSetId = Integer.valueOf(row.get(0).toString());
//                resOfPerFormsetid.put(formSetId, singleRecord);
//            }
//            res.put(table, resOfPerFormsetid);
//
//        }
//
//        Console.log(JSONUtilS.toJsonPrettyStr(res));
//        Console.log("开始写入json文件");
//
//        File file = FileUtil.file("results/IndexRealTimeRaiseFallParameter/" +
//                "singleTableGroupByFormsetidAvg_scale1.41.61.2" +
//                ".json");
//        Console.log(file.getAbsolutePath());
//        FileWriter writer = new FileWriter(file);
//        writer.write(JSONUtilS.toJsonPrettyStr(res));
//        Console.log("finish");
//
//
//    }
//
//    public static void singleTableAllAvg() throws Exception {
//        List<String> tables = getResultTables();
//        // //单表全avg
//        String sql = "select avg(lb_weighted_buy_price)             as bp,\n" +
//                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
//                "       avg(lb_global_position_sum)            as position,\n" +
//                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
//                "       avg(hs_success_global_percent)         as hss,\n" +
//                "       avg(hs_success_global_price)           as hsp,\n" +
//                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
//                "from `{}`";
//
//        List<String> columns = Arrays.asList("bp", "simplebp", "position", "stockcount", "hss", "hsp", "profit");
//        HashMap<String, HashMap<String, Double>> res = new HashMap<>();
//        for (String table : tables) {
//            Console.log("parseing: {}", table);
//            HashMap<String, Double> singleRes = new HashMap<>();
//            String fullSql = StrUtilS.format(sql, table);
//            DataFrame<Object> dfTemp = DataFrame.readSql(klineForms, fullSql);
//            int colCount = dfTemp.size();
//            for (int i = 0; i < colCount; i++) {
//                Double value = Double.valueOf(dfTemp.col(i).get(0).toString());
//                String key = columns.get(i);
//                singleRes.put(key, value);
//            }
//            res.put(table, singleRes);
//        }
//
//        Console.log(JSONUtilS.toJsonPrettyStr(res));
//        Console.log("开始写入json文件");
//        FileWriter writer = new FileWriter("singleTableAllAvg.json");
//        writer.write(JSONUtilS.toJsonPrettyStr(res));
//        Console.log("finish");
//    }
//
//    public static List<String> getResultTables() throws SQLException {
//        List<String> tables = getAllTables(klineForms);
//        tables = tables.stream().filter(value -> value.startsWith("fs_bk_lbhs_next0b1s_indexper_scale1.41.61.2"))
//                .collect(Collectors.toList());
//        return tables;
//    }
//
//    public static void renameAllTable() throws Exception {
//        if (true) {
//            throw new Exception("本函数不再调用;已经改过名了");
//        }
//        List<String> tables = getResultTables();
//        int prefixLenth = "fs_bk_lbhs_next0b1s_indexper_scale1.21.3".length();
//        for (String tablename : tables) {
//            String newTablename = StrUtilS.format("{}{}{}",
//                    "fs_bk_lbhs_next0b1s_indexper_scale1.21.31.2",
//                    "",
//                    tablename.substring(prefixLenth, tablename.length()));
//            renameTable(klineForms, tablename, newTablename);
//        }
//    }
//
//
//}
