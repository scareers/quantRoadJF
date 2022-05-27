
package com.scareers.datasource.selfdb;


import cn.hutool.core.lang.Console;
//import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class ConnectionFactory {
    // 各个db对应的连接池对象. 单例懒汉模式
//    private static ComboPooledDataSource localTushareConnPool;
//    private static ComboPooledDataSource localStocktestConnPool;
//    private static ComboPooledDataSource localKlineFormsConnPool;
//    private static ComboPooledDataSource localTushare1MConnPool;


    public static void main(String[] args) throws PropertyVetoException, SQLException {

//        getConnLocalTushareFromPool();
        Console.log("************");
//        getConnLocalTushareFromPool();
        Console.log("************");
//        getConnLocalTushare1MFromPool();
        Console.log("************");
//        getConnLocalStocktestFromPool();
        Console.log("************");
//        getConnLocalKlineFormsFromPool();
        getConnLocalFSTransactionFromEastmoney();

    }


    /**
     * 因连接池也有多个, 每个连接到各自不同的 db; 因此一个db对应一个 c3p0 连接池.
     * 因此: 连接池单例模式(静态属性懒汉模式). 而工厂方法, 则从 对应连接池获取 Connection 对象
     */


    /**
     * java.sql.Connection   连接工厂. 连接mysql; 提供 ip,port,dbname,user,password
     * getX 方法获取内置默认设置;   静态方法 connectToMysql() 可任意获取
     */
    public static Connection getConnLocalStocktest() {
        return connectToLocalMysqlMain("stocktest");
    }

//    public static Connection getConnLocalStocktestFromPool() throws SQLException {
//        if (localStocktestConnPool == null) {
//            localStocktestConnPool = new ComboPooledDataSource("localStocktest");
//        }
//        return localStocktestConnPool.getConnection();
//    }


    public static Connection getConnLocalTushare()  {
        return connectToLocalMysqlMain("tushare");
    }
//
//    public static Connection getConnLocalTushareFromPool() throws SQLException {
//        if (localTushareConnPool == null) {
//            localTushareConnPool = new ComboPooledDataSource("localTushare");
//        }
//        return localTushareConnPool.getConnection();
//        // todo: 完成连接池
//    }


    public static Connection getConnLocalKlineForms() {
        return connectToLocalMysqlMain("kline_forms");
    }

    public static Connection getConnLocalMindgo1M() {
        return connectToLocalMysqlMain("mindgo_1m");
    }

//    public static Connection getConnLocalKlineFormsFromPool() throws SQLException {
//        if (localKlineFormsConnPool == null) {
//            localKlineFormsConnPool = new ComboPooledDataSource("localKlineForms");
//        }
//        return localKlineFormsConnPool.getConnection();
//    }

    public static Connection getConnLocalTushare1M() {
        return connectToLocalMysqlMain("tushare_1m");
    }
    public static Connection getConnLocalFSTransactionFromEastmoney() {
        return connectToLocalMysqlMain("eastmoney_fs_transaction");
    }
    public static Connection getConnLocalEastmoney() {
        return connectToLocalMysqlMain("eastmoney");
    }
    public static Connection getConnLocalThs() {
        return connectToLocalMysqlMain("ths");
    }
    public static Connection getConnLocalFS1MFromEastmoney() {
        return connectToLocalMysqlMain("eastmoney_fs1m");
    }


//    public static Connection getConnLocalTushare1MFromPool() throws SQLException {
//        if (localTushare1MConnPool == null) {
//            localTushare1MConnPool = new ComboPooledDataSource("localTushare1M");
//        }
//        return localTushare1MConnPool.getConnection();
//    }

    public static Connection getConnLocalQuant() {
        return connectToLocalMysqlMain("quant");
    }

    public static Connection getConnLocalAkTicks() {
        return connectToLocalMysqlMain("ak_ticks");
    }

    public static Connection getConnLocalStrategy() {
        return connectToLocalMysqlMain("strategy");
    }

    public static Connection getConnLocalJqAnalysis() {
        return connectToLocalMysqlMain("jq_analysis");
    }

    public static Connection getConnLocalAkStock() {
        return connectToLocalMysqlMain("ak_stock");
    }


    public static Connection getConnLocalTushare1MSlave1() {
        return connectToLocalMysqlSlave1("tushare_1m");
    }

    public static Connection connectToMysql(String ip, int port, String dbname, String user, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // &allowMultiQueries=true 允许多条sql语句连续执行.  ; 分割即可
        String url = String
                .format("jdbc:mysql://%s:%s/%s?useSSL=false&generateSimpleParameterMetadate=true&allowMultiQueries=true",
                        ip, port, dbname);

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("获取连接失败, 返回null: " + url);
        }
        return null;
    }

    public static Connection connectToLocalMysql(int port, String dbname, String user, String password) {
        return connectToMysql("localhost", port, dbname, user, password);
    }

    public static Connection connectToLocalMysqlMain(String dbname) {
        return connectToMysql("localhost", 3306, dbname, "root", "123");
    }

    public static Connection connectToLocalMysqlSlave1(String dbname) {
        return connectToMysql("localhost", 13306, dbname, "root", "yyy");
    }

    public static Connection connectToLocalMysqlSlave2(String dbname) {
        return connectToMysql("localhost", 23306, dbname, "root", "xxx");
    }


}


