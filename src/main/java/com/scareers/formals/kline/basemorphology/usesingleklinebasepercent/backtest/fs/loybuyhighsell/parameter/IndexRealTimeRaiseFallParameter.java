package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.parameter;

import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.StrUtil;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
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

        String sql = StrUtil.format("select avg(lb_weighted_buy_price)             as bp,\n" +
                "       avg(lb_simple_avg_buy_price)           as simplebp,\n" +
                "       avg(lb_global_position_sum)            as position,\n" +
                "       avg(lb_has_position_stock_count)       as stockcount,\n" +
                "       avg(hs_success_global_percent)         as hss,\n" +
                "       avg(hs_success_global_price)           as hsp,\n" +
                "       avg(lbhs_weighted_profit_conservative) as profit\n" +
                "from `fs_backtest_lowbuy_highsell_next0b1s_-5.0_-5.0`",);

    }

    public static void renameAllTable() throws Exception {
        if (true) {
            throw new Exception("本函数不再调用");
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
