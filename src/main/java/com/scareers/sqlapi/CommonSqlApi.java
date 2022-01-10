package com.scareers.sqlapi;

import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.StrUtilS;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 一些比较常规的sql操作
 *
 * @author: admin
 * @date: 2021/12/13/013-14:53
 */
public class CommonSqlApi {

    public static void renameTable(Connection connection, String tableName, String newTableName) throws Exception {
        String sql = StrUtilS.format("rename table `{}` to `{}`", tableName, newTableName);
        execSql(sql, connection);
    }

    public static List<String> getAllTables(Connection connection) throws SQLException {
        String sql = "show tables";
        DataFrame<Object> dfTemp = DataFrame.readSql(connection, sql);
        return DataFrameS.getColAsStringList(dfTemp, 0);
    }
}
