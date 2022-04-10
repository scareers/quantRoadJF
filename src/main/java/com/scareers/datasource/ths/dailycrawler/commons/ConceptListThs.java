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
 * description: 同花顺概念指数列表!
 *
 * @question: 同花顺概念指数;涨幅;量比;成交量;总市值;流通市值;主力净量;主力净额;;上涨家数;下跌家数;上涨家数/成分股总数;涨停家数;一字涨停家数;跌停家数;一字跌停家数;跌停家数/成分股总数;
 * [code, market_code, {(}指数@上涨家数[20220401]{/}指数@成分股总数[20220401]{)},
 * {(}指数@跌停家数[20220401]{/}指数@成分股总数[20220401]{)},
 * 指数@dde大单净量[20220401], 指数@一字涨停家数[20220401], 指数@一字涨停家数占比[20220401],
 * 指数@一字跌停家数[20220401], 指数@上涨家数[20220401], 指数@下跌家数[20220401], 指数@主力资金流向[20220401],
 * 指数@同花顺概念指数, 指数@总市值[20220401], 指数@成交量[20220401], 指数@成分股总数[20220401],
 * 指数@收盘价:不复权[20220401], 指数@流通市值[20220401], 指数@涨停家数[20220401], 指数@涨停家数占比[20220401],
 * 指数@涨跌幅:前复权[20220401], 指数@跌停家数[20220401], 指数@量比[20220401], 指数代码, 指数简称]
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class ConceptListThs extends CrawlerThs {
    public static void main(String[] args) throws Exception {
        new ConceptListThs(true).run();


    }

    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存;

    public ConceptListThs(boolean forceUpdate) {
        super("concept_list");
        this.forceUpdate = forceUpdate;
    }

    @Override
    protected void runCore() {
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
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery(
                "同花顺概念指数;涨幅;量比;成交量;总市值;流通市值;主力净量;主力净额;;上涨家数;下跌家数;上涨家数/成分股总数;涨停家数;一字涨停家数;跌停家数;一字跌停家数;跌停家数/成分股总数;");
        if (dataFrame == null) {
            logApiError("wenCaiQuery(\"同花顺概念指数;\")");
            success = false;
            return;
        }


        //
        dataFrame = dataFrame.rename(getRenameMap(dataFrame.columns()));
        dataFrame = dataFrame.drop("deleteLine1", "deleteLine2"); // 删除不保存行
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
        } catch (Exception e) {
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
                        + "chgP double  null,"
                        + "close double  null,"
                        + "code varchar(32)  null,"
                        + "name varchar(32)  null,"
                        + "marketCode int  null,"
                        + "indexCode varchar(32)  null,"
                        + "conceptIndex varchar(32)  null,"

                        // 新增
                        + "vol double  null,"
                        + "volRate double  null,"
                        + "ddeNetVol double  null,"
                        + "ddeNetAmount double  null,"
                        + "totalMarketValue double  null,"
                        + "circulatingMarketValue double  null,"
                        + "includeStockAmount int  null,"
                        + "upAmount int  null,"
                        + "downAmount int  null,"
                        + "upPercent double  null,"
                        + "lowLimitAmount int  null,"
                        + "lowLimitPercent double  null,"
                        + "highLimitAmount int  null,"
                        + "highLimitPercent double  null,"
                        + "lineLowLimitAmount int  null,"
                        + "lineHighLimitAmount int  null,"
                        + "lineHighLimitPercent double  null,"


                        + "dateStr varchar(32)  null,"


                        + "INDEX name_index (name ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [code, market_code, {(}指数@上涨家数[20220401]{/}指数@成分股总数[20220401]{)},
     * {(}指数@跌停家数[20220401]{/}指数@成分股总数[20220401]{)},
     * 指数@dde大单净量[20220401], 指数@一字涨停家数[20220401], 指数@一字涨停家数占比[20220401],
     * 指数@一字跌停家数[20220401], 指数@上涨家数[20220401], 指数@下跌家数[20220401], 指数@主力资金流向[20220401],
     * 指数@同花顺概念指数, 指数@总市值[20220401], 指数@成交量[20220401], 指数@成分股总数[20220401],
     * 指数@收盘价:不复权[20220401], 指数@流通市值[20220401], 指数@涨停家数[20220401], 指数@涨停家数占比[20220401],
     * 指数@涨跌幅:前复权[20220401], 指数@跌停家数[20220401], 指数@量比[20220401],
     * <p>
     * 指数@非一字涨停家数[20220401], 指数@非一字涨停家数占比[20220401],        // 不保存. 重命名后将删除掉
     * <p>
     * 指数代码, 指数简称]
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (WenCaiApi.fieldLike(column.toString(), "{(}指数@上涨家数[20220401]{/}指数@成分股总数[20220401]{)}")) {
                renameMap.put(column, "upPercent");
            } else if (WenCaiApi.fieldLike(column.toString(), "{(}指数@跌停家数[20220401]{/}指数@成分股总数[20220401]{)}")) {
                renameMap.put(column, "lowLimitPercent");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@dde大单净量[20220401]")) {
                renameMap.put(column, "ddeNetVol");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@一字涨停家数[20220401]")) {
                renameMap.put(column, "lineHighLimitAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@一字涨停家数占比[20220401]")) {
                renameMap.put(column, "lineHighLimitPercent");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@一字跌停家数[20220401]")) {
                renameMap.put(column, "lineLowLimitAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@上涨家数[20220401]")) {
                renameMap.put(column, "upAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@下跌家数[20220401]")) {
                renameMap.put(column, "downAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@主力资金流向[20220401]")) {
                renameMap.put(column, "ddeNetAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@总市值[20220401]")) {
                renameMap.put(column, "totalMarketValue");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@成交量[20220401]")) {
                renameMap.put(column, "vol");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@成分股总数[20220401]")) {
                renameMap.put(column, "includeStockAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@收盘价:不复权[20220401]")) {
                renameMap.put(column, "close");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@流通市值[20220401]")) {
                renameMap.put(column, "circulatingMarketValue");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@涨停家数[20220401]")) {
                renameMap.put(column, "highLimitAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@涨停家数占比[20220401]")) {
                renameMap.put(column, "highLimitPercent");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@涨跌幅:前复权[20220401]")) {
                renameMap.put(column, "chgP");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@跌停家数[20220401]")) {
                renameMap.put(column, "lowLimitAmount");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@量比[20220401]")) {
                renameMap.put(column, "volRate");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@非一字涨停家数[20220401]")) {
                renameMap.put(column, "deleteLine1");
            } else if (WenCaiApi.fieldLike(column.toString(), "指数@非一字涨停家数占比[20220401]")) {
                renameMap.put(column, "deleteLine2");
            }


        }

        renameMap.put("code", "code");
        renameMap.put("market_code", "marketCode");
        renameMap.put("指数@同花顺概念指数", "conceptIndex");
        renameMap.put("指数代码", "indexCode");
        renameMap.put("指数简称", "name");

        return renameMap;
    }
}
