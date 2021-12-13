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

        String

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
