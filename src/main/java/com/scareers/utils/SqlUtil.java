package com.scareers.utils;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * description:
 * author: admin
 * date: 2021/11/4  0004-23:44
 * <p>
 * noti:
 * 1. execSql  多用于建表等等,修改表结构等, 不需要读取返回的boolean, 因此 可选择传递参数, 控制是否关闭掉连接.
 * 在未来代码需要用到同一个conn对象时, 不需要关闭连接. 传递 closeConn=true 即可.
 * 默认函数是 自动关闭连接.  execSqlUpdate 同理
 * <p>
 * DataFrame 相关sql操作, 默认均不会关闭连接, 如果不再复用conn, 则应当关闭掉连接对象
 */
public class SqlUtil {
    public static void main(String[] args) throws Exception {
//        createIndexes(ConnectionFactory.getConnLocalKlineForms(), "test", Arrays.asList("a"));
    }

    // 默认不关闭连接.
    public static boolean execSql(String sql, Connection conn) throws Exception {
        return execSql(sql, conn, false);
    }

    public static boolean execSql(String sql, Connection conn, boolean closeConn) throws Exception {
        Statement s = conn.createStatement();
        boolean rs = s.execute(sql);
        if (closeConn) {
            conn.close();
        }
        return rs;
    }


    public static ResultSet execSqlQuery(String sql, Connection conn) throws Exception {
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery(sql);
        // 查询返回 ResultSet
            /*
            ResultSet symbols = execSqlQuery(sqlGetStockList);
            ArrayList<String> stockListTemp = new ArrayList<>();
            while (symbols.next()) { // 遍历读取
                stockListTemp.add(symbols.getString("symbol")); // 另有getDouble() 等等
            }
             */
        return rs;
    }
    public static int execSqlUpdate(String sql, Connection conn) throws Exception {
        return execSqlUpdate(sql, conn, true);
    }

    public static int execSqlUpdate(String sql, Connection conn, boolean closeConn) throws Exception {
        Statement s = conn.createStatement();
        int rs = s.executeUpdate(sql);
        // 更新返回影响记录数量
        if (closeConn) {
            conn.close();
        }
        return rs;
    }

    /**
     * 给定数据表 和 它包含的 多个列, 执行sql语句, 创建 简单索引
     *
     * @param conn
     * @param tablename
     * @param columns
     */
    public static void createIndexes(Connection conn, String tablename, List<String> columns) throws Exception {
        for (String column : columns) {
            String sql = StrUtil.format("alter table {} add index {}({})",
                    tablename, column + "__index", column);
            execSql(sql, conn);
            Console.log("创建索引成功: {}", column + "__index");
        }
    }


}
