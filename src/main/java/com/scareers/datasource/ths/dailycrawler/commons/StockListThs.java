package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 同花顺所有股票列表, 核心以 marketCode区分不同市场
 * [code, 振幅[20220401], 最新价, 开盘价:前复权[20220401], 收盘价:前复权[20220401], market_code, 股票代码, 涨跌幅:前复权[20220401],
 * 成交额[20220401], 最高价:前复权[20220401], 股票简称, 成交量[20220401], 最低价:前复权[20220401]]
 *
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class StockListThs extends CrawlerThs {
    public static void main(String[] args) {
        new StockListThs(true).run();

    }

    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存;

    public StockListThs(boolean forceUpdate) {
        super("stock_list");
        this.forceUpdate = forceUpdate;
    }

    @Override
    protected void runCore() {
        try {
            execSql(sqlCreateTable, conn);
        } catch (Exception e) {

        }

        String dateStr = DateUtil.today(); // 记录日期列
        if (!forceUpdate) {
            String sql = StrUtil.format("select count(*) from {} where dateStr='{}'", tableName, dateStr);
            try {
                DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
                if (Integer.parseInt(dataFrame.get(0, 0).toString()) > 0) {
                    success = true;
                    return; // 当不强制更新, 判定是否已运行过, 运行过则直接返回, 连带不更新 关系数据表
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("涨跌幅;成交额");
        if (dataFrame == null || dataFrame.length() == 0) {
            logApiError("wenCaiQuery(\"涨跌幅;成交额\")");
            success = false;
            return;
        }

        //
        dataFrame = dataFrame.rename(getRenameMap(dataFrame.columns()));
        List<Object> dateStrList = new ArrayList<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            dateStrList.add(dateStr);
        }
        dataFrame = dataFrame.add("dateStr", dateStrList);

        try {
            String sqlDelete = StrUtil.format("delete from {} where dateStr='{}'", tableName, dateStr);
            execSql(sqlDelete, conn);
            DataFrameS.toSql(dataFrame, tableName, this.conn, "append", sqlCreateTable);
        } catch (Exception e) {
            e.printStackTrace();
            logSaveError();
            success = false;
            return;
        }

        // @key: 新增: 将下一交易日的本数据, 也暂时保存为与此刻相同的df! 为了操盘计划gui而做的妥协;
        // 待明日运行后, 也保存后日的; 后日的实际刷新将在后日!
        String nextTradeDateStr = null;
        try {
            nextTradeDateStr = EastMoneyDbApi.getPreNTradeDateStrict(dateStr, -1);
        } catch (SQLException e) {
            log.warn("获取下一交易日失败,不尝试将结果复制保存到下一交易日");

        }
        if (nextTradeDateStr != null) {
            saveNextTradeDateTheSameDf(dataFrame, nextTradeDateStr);
        }

        success = true;
    }

    /**
     * 它将修改 df 的 dateStr 列
     *
     * @param dataFrame
     */
    private void saveNextTradeDateTheSameDf(DataFrame<Object> dataFrame, String nextDateStr) {
        for (int i = 0; i < dataFrame.length(); i++) {
            dataFrame.set(i, "dateStr", nextDateStr);
        }

        try {
            String sqlDelete = StrUtil.format("delete from {} where dateStr='{}'", tableName, nextDateStr);
            execSql(sqlDelete, conn);
            DataFrameS.toSql(dataFrame, tableName, this.conn, "append", sqlCreateTable);
        } catch (Exception e) {
            log.error("保存相同数据到下一交易日失败,暂不视为错误");
            return;
        }
    }


    @Override
    protected void setDb() {
        setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "code varchar(32)  null,"
                        + "stockCode varchar(32)  null,"
                        + "name varchar(32)  null,"
                        + "marketCode int  null,"

                        + "chgP double  null,"
                        + "newPrice double  null,"
                        + "open double  null,"
                        + "high double  null,"
                        + "low double  null,"
                        + "close double  null,"
                        + "vol double  null,"
                        + "amount double  null,"

                        + "amplitude double  null,"

                        + "dateStr varchar(32)  null,"


                        + "INDEX name_index (name ASC),\n"
                        + "INDEX code_index (code ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [code, 振幅[20220401], 最新价, 开盘价:前复权[20220401], 收盘价:前复权[20220401], market_code,
     * 股票代码, 涨跌幅:前复权[20220401], 成交额[20220401], 最高价:前复权[20220401], 股票简称,
     * 成交量[20220401], 最低价:前复权[20220401]]
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (column.toString().startsWith("涨跌幅:前复权")) {
                renameMap.put(column, "chgP");
            } else if (column.toString().startsWith("振幅")) {
                renameMap.put(column, "amplitude");
            } else if (column.toString().startsWith("最高价:前复权")) {
                renameMap.put(column, "high");
            } else if (column.toString().startsWith("成交量")) {
                renameMap.put(column, "vol");
            } else if (column.toString().startsWith("开盘价:前复权")) {
                renameMap.put(column, "open");
            } else if (column.toString().startsWith("收盘价:前复权")) {
                renameMap.put(column, "close");
            } else if (column.toString().startsWith("最低价:前复权")) {
                renameMap.put(column, "low");
            } else if (column.toString().startsWith("成交额")) {
                renameMap.put(column, "amount");
            }

        }
        renameMap.put("code", "code");
        renameMap.put("股票简称", "name");
        renameMap.put("最新价", "newPrice");
        renameMap.put("market_code", "marketCode");
        renameMap.put("股票代码", "stockCode");

        return renameMap;
    }
}
