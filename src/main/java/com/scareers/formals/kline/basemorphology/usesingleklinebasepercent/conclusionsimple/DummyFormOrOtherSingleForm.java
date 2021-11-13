package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.conclusionsimple;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/14  0014-5:04
 * <p>
 * Dummy 形态基本结论.
 * <p>
 * 1.Dummy 即无任何附加限制条件, 统计所有情况.
 * 2.17个日期区间分开
 * 3.计算时, 因对 第三天的价格变化幅度也做了统计限制, 因此会出现一定误差, 基本可接受
 * 4.首先简单对 第二天四个价格 和第三天四个价格做简单对比
 * 5.主要逻辑为, 读取 next0, next1两个表Dummy统计结果; 结果df中, _x表示明日, _y表示后日, 方便对比
 * 6.结论总结: // 17个日期区间
 * <p>
 * 1)Next0/1Open
 * 1. mean_x 几乎全部日期区间< 0; mean_y 则几乎只在大牛市期间才可能>0;
 */
public class DummyFormOrOtherSingleForm {
    //    public static String targetForm = "PL[0,1]";
    public static String targetForm = "PL[0,1]";
    // 对比next0,1,3 三个表, 某种形态的数据.结果已经join
    public static List<Integer> keyIntegers = ListUtil.of(0, 1, 2);
    public static List<String> simpleAlgorithms = ListUtil.of("Open", "Close", "High", "Low");

    public static Connection connection = ConnectionFactory.getConnLocalKlineForms();

    public static void main(String[] args) throws SQLException {
        for (String algorithm : simpleAlgorithms) {

            List<DataFrame<Object>> dfs = new ArrayList<>();
            for (Integer keyInt : keyIntegers) {
                DataFrame<Object> dfTemp = DataFrame.readSql(connection, StrUtil.format(
                        "select stat_date_range as Adates,mean as mean_{},zero_compare_counts_percent_2 as zp2_{},\n" +
                                "            bigchange_compare_counts_percnet_2 as bp2_{},\n" +
                                "            virtual_geometry_mean as vm_{},effective_counts as ec_{}\n" +
                                "            from single_kline_forms_analyze_results_next{}__excybkcb\n" +
                                "            where form_name = '{}'\n" +
                                "              and stat_result_algorithm = 'Next{}{}'\n" +
                                "            order by Adates", keyInt, keyInt, keyInt, keyInt, keyInt, keyInt,
                        targetForm,
                        keyInt, algorithm));
//                Console.log(dfTemp);
//                Console.log(dfTemp.columns());
                dfs.add(dfTemp);
            }

            DataFrame<Object> dfTotal = dfs.get(0);
            for (int i = 0; i < dfs.size() - 1; i++) {
                dfTotal = dfTotal.join(dfs.get(i + 1), DataFrame.JoinType.OUTER, value -> value.get(0));

            }
            Console.log("{} -- {}", targetForm, algorithm);
            dfTotal = DataFrameSelf.sortByColumnName(dfTotal);
            System.out.println(dfTotal.toString(1000));
            Console.log("********************");

        }
    }
}
