package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
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
 * description: 同花顺所有股票列表, 以 marketCode区分不同市场
 * 涨跌幅;现价;流通市值;量比;市盈率;roe;所属概念;  + 涨停 + 跌停; 以code字段 join
 * <p>
 * 两问句字段:
 * // 所属概念, 收盘价:前复权[20220408], 总市值[20220408], code, 最低价:前复权[20220408], a股市值(不含限售股)[20220408], 收盘价:不复权[20220408], market_code, 开盘价:前复权[20220408], 市盈率(pe)[20220408], 股票代码, 最高价:前复权[20220408], 开盘价:不复权[20220408], 换手率[20220408], 最高价:不复权[20220408], 所属同花顺行业, 股票简称, 最低价:不复权[20220408], 涨跌幅:前复权[20220408], 所属概念数量, 振幅[20220408], 量比[20220408]
 * // [涨停封单量占成交量比[20220408], 涨停类型[20220408], 涨停封单量占流通a股比[20220408], 首次涨停时间[20220408], code, a股市值(不含限售股)[20220408], 最新价, 最新涨跌幅, market_code, 涨停封单额[20220408], 涨停[20220408], 股票代码, 涨停原因类别[20220408], 几天几板[20220408], 涨停明细数据[20220408], 涨停封单量[20220408], 股票简称, 涨停开板次数[20220408], 最终涨停时间[20220408], 连续涨停天数[20220408]]
 * // [跌停[20220408], code, 跌停封单量[20220408], 最新价, 跌停明细数据[20220408], 最新涨跌幅, market_code, 首次跌停时间[20220408], 股票代码, 跌停封单量占流通a股比[20220408], 跌停封单额[20220408], 跌停原因类型[20220408], 跌停开板次数[20220408], 股票简称, 跌停类型[20220408], 最终跌停时间[20220408], 跌停封单量占成交量比[20220408], 连续跌停天数[20220408]]
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

        DataFrame<Object> dataFrame1 = WenCaiApi.wenCaiQuery("涨跌幅;现价;流通市值;量比;市盈率;roe;所属概念;");
        DataFrame<Object> dataFrame2 = WenCaiApi.wenCaiQuery("涨停");
        DataFrame<Object> dataFrame3 = WenCaiApi.wenCaiQuery("跌停");
        if (dataFrame1 == null || dataFrame1.length() == 0) {
            logApiError("wenCaiQuery(\"涨跌幅;现价;流通市值;量比;市盈率;roe;所属概念;\")");
            success = false;
            return;
        }
        if (dataFrame2 == null || dataFrame2.length() == 0) {
            logApiError("wenCaiQuery(\"涨停\")");
            success = false;
            return;
        }
        if (dataFrame3 == null || dataFrame3.length() == 0) {
            logApiError("wenCaiQuery(\"跌停;\")");
            success = false;
            return;
        }
        //
        dataFrame1 = dataFrame1.rename(getRenameMap(dataFrame1.columns()));
        dataFrame2 = dataFrame2.rename(getRenameMap2(dataFrame2.columns()));
        dataFrame3 = dataFrame3.rename(getRenameMap3(dataFrame3.columns()));

        dataFrame2 = dataFrame2.drop("deleteCol1", "deleteCol2", "deleteCol3", "deleteCol4", "deleteCol5");
        dataFrame3 = dataFrame3.drop("deleteCol1", "deleteCol2", "deleteCol3", "deleteCol4");

        dataFrame1 = moveTheJoinColToForemost(dataFrame1, Arrays.asList("code", "marketCode"));
        dataFrame2 = moveTheJoinColToForemost(dataFrame2, Arrays.asList("code", "marketCode"));
        dataFrame3 = moveTheJoinColToForemost(dataFrame3, Arrays.asList("code", "marketCode"));

        Console.log(dataFrame1);
        Console.log(dataFrame2);
        Console.log(dataFrame3);
        Console.log(dataFrame1.columns());
        Console.log(dataFrame2.columns());
        Console.log(dataFrame3.columns());


        DataFrame<Object> dataFrame = dataFrame1
                .joinOn(dataFrame2, "code", "marketCode");
        // code_left, marketCode_left 和right, 这里drop掉, 并改名掉
        dataFrame = dataFrame.drop("code_right", "marketCode_right");
        dataFrame = dataFrame.rename("code_left", "code");
        dataFrame = dataFrame.rename("marketCode_left", "marketCode");
        dataFrame = dataFrame.joinOn(dataFrame3, "code", "marketCode");

        dataFrame = dataFrame.drop("code_right", "marketCode_right");
        dataFrame = dataFrame.rename("code_left", "code");
        dataFrame = dataFrame.rename("marketCode_left", "marketCode");

        Console.log(dataFrame.columns());
        /*
        [code, marketCode, concepts, close, marketValue, low, circulatingMarketValue, closeNofq, open, pe, stockCode, high, openNofq, turnover, highNofq, industries, name, lowNofq, chgP, conceptAmount, amplitude, volRate, highLimitBlockadeVolumeRate, highLimitType, highLimitBlockadeCMVRate, highLimitFirstTime, highLimitBlockadeAmount, highLimit, highLimitReason, highLimitAmountType, highLimitDetail, highLimitBlockadeVol, highLimitBrokeTimes, highLimitLastTime, highLimitContinuousDays, lowLimit, lowLimitBlockadeVol, lowLimitDetails, lowLimitFirstTime, lowLimitBlockadeVolCMVRate, lowLimitBlockadeAmount, lowLimitReason, lowLimitBrokeTimes, lowLimitType, lowLimitLastTime, lowLimitBlockadeVolumeRate, lowLimitContinuousDays]
         */


//        List<Object> dateStrList = new ArrayList<>();
//        for (int i = 0; i < dataFrame1.length(); i++) {
//            dateStrList.add(dateStr);
//        }
//        dataFrame1 = dataFrame1.add("dateStr", dateStrList);
//        try {
//            dataFrame1 = dataFrame1.drop("最新涨跌幅");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        try {
//            String sqlDelete = StrUtil.format("delete from {} where dateStr='{}'", tableName, dateStr);
//            execSql(sqlDelete, conn);
//            DataFrameS.toSql(dataFrame1, tableName, this.conn, "append", sqlCreateTable);
//        } catch (Exception e) {
//            e.printStackTrace();
//            logSaveError();
//            success = false;
//            return;
//        }
//
//        // @key: 新增: 将下一交易日的本数据, 也暂时保存为与此刻相同的df! 为了操盘计划gui而做的妥协;
//        // 待明日运行后, 也保存后日的; 后日的实际刷新将在后日!
//        String nextTradeDateStr = null;
//        try {
//            nextTradeDateStr = EastMoneyDbApi.getPreNTradeDateStrict(dateStr, -1);
//        } catch (SQLException e) {
//            log.warn("获取下一交易日失败,不尝试将结果复制保存到下一交易日");
//
//        }
//        if (nextTradeDateStr != null) {
//            saveNextTradeDateTheSameDf(dataFrame1, nextTradeDateStr);
//        }
//
//        success = true;
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

    /**
     * 三个df 合并字段后!
     * [code, marketCode, concepts, close, marketValue, low, circulatingMarketValue,
     * closeNofq, open, pe, stockCode, high, openNofq, turnover, highNofq, industries, name,
     * lowNofq, chgP, conceptAmount, amplitude, volRate, highLimitBlockadeVolumeRate, highLimitType, highLimitBlockadeCMVRate, highLimitFirstTime, highLimitBlockadeAmount, highLimit, highLimitReason, highLimitAmountType, highLimitDetail, highLimitBlockadeVol, highLimitBrokeTimes, highLimitLastTime, highLimitContinuousDays, lowLimit, lowLimitBlockadeVol, lowLimitDetails, lowLimitFirstTime, lowLimitBlockadeVolCMVRate, lowLimitBlockadeAmount, lowLimitReason, lowLimitBrokeTimes, lowLimitType, lowLimitLastTime, lowLimitBlockadeVolumeRate, lowLimitContinuousDays]
     */
    @Override
    protected void initSqlCreateTable() {
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "code varchar(32)  null,"
                        + "marketCode int  null,"
                        + "stockCode varchar(32)  null,"
                        + "concepts longtext  null,"
                        + "close double  null,"
                        + "marketValue double  null,"
                        + "low double  null,"
                        + "circulatingMarketValue double  null,"
                        + "closeNofq double  null,"
                        + "open double  null,"
                        + "pe double  null,"
                        + "high double  null,"
                        + "openNofq double  null,"
                        + "turnover double  null,"
                        + "highNofq double  null,"
                        + "industries longtext  null,"
                        + "name varchar(32)  null,"
                        + "lowNofq double  null,"
                        + "chgP double  null,"
                        + "conceptAmount int  null,"
                        + "conceptAmount int  null,"


                        + "chgP double  null,"
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
     * 所属概念, 收盘价:前复权[20220408], 总市值[20220408], code, 最低价:前复权[20220408], a股市值(不含限售股)[20220408],
     * 收盘价:不复权[20220408], market_code, 开盘价:前复权[20220408], 市盈率(pe)[20220408],
     * 股票代码, 最高价:前复权[20220408], 开盘价:不复权[20220408], 换手率[20220408], 最高价:不复权[20220408],
     * 所属同花顺行业, 股票简称, 最低价:不复权[20220408], 涨跌幅:前复权[20220408], 所属概念数量, 振幅[20220408], 量比[20220408]
     * <p>
     * [涨停封单量占成交量比[20220408], 涨停类型[20220408], 涨停封单量占流通a股比[20220408], 首次涨停时间[20220408],
     * code, a股市值(不含限售股)[20220408], 最新价, 最新涨跌幅, market_code, 涨停封单额[20220408], 涨停[20220408], 股票代码,
     * 涨停原因类别[20220408], 几天几板[20220408], 涨停明细数据[20220408], 涨停封单量[20220408], 股票简称,
     * 涨停开板次数[20220408], 最终涨停时间[20220408], 连续涨停天数[20220408]]
     *
     * @param rawColumns
     * @return
     * @noti : 行业: 建筑装饰-建筑装饰-工程咨询服务  概念: 智慧城市;注册制次新股;融资融券;大数据;地下管网;建筑节能;疫情监测;北部湾自贸区
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (WenCaiApi.fieldLike(column.toString(), "收盘价:前复权[20220408]")) {
                renameMap.put(column, "close");
            } else if (WenCaiApi.fieldLike(column.toString(), "总市值[20220408]")) {
                renameMap.put(column, "marketValue");
            } else if (WenCaiApi.fieldLike(column.toString(), "最低价:前复权[20220408]")) {
                renameMap.put(column, "low");
            } else if (WenCaiApi.fieldLike(column.toString(), "a股市值(不含限售股)[20220408]")) {
                renameMap.put(column, "circulatingMarketValue");
            } else if (WenCaiApi.fieldLike(column.toString(), "收盘价:不复权[20220408]")) {
                renameMap.put(column, "closeNofq");
            } else if (WenCaiApi.fieldLike(column.toString(), "开盘价:前复权[20220408]")) {
                renameMap.put(column, "open");
            } else if (WenCaiApi.fieldLike(column.toString(), "市盈率(pe)[20220408]")) {
                renameMap.put(column, "pe");
            } else if (WenCaiApi.fieldLike(column.toString(), "最高价:前复权[20220408]")) {
                renameMap.put(column, "high");
            } else if (WenCaiApi.fieldLike(column.toString(), "开盘价:不复权[20220408]")) {
                renameMap.put(column, "openNofq");
            } else if (WenCaiApi.fieldLike(column.toString(), "换手率[20220408]")) {
                renameMap.put(column, "turnover");
            } else if (WenCaiApi.fieldLike(column.toString(), "最高价:不复权[20220408]")) {
                renameMap.put(column, "highNofq");
            } else if (WenCaiApi.fieldLike(column.toString(), "最低价:不复权[20220408]")) {
                renameMap.put(column, "lowNofq");
            } else if (WenCaiApi.fieldLike(column.toString(), "涨跌幅:前复权[20220408]")) {
                renameMap.put(column, "chgP");
            } else if (WenCaiApi.fieldLike(column.toString(), "振幅[20220408]")) {
                renameMap.put(column, "amplitude");
            } else if (WenCaiApi.fieldLike(column.toString(), "量比[20220408]")) {
                renameMap.put(column, "volRate");
            }

        }
        renameMap.put("所属概念", "concepts"); // str简单保存
        renameMap.put("所属概念数量", "conceptAmount"); // str简单保存
        renameMap.put("所属同花顺行业", "industries"); // str简单保存

        renameMap.put("code", "code");
        renameMap.put("market_code", "marketCode");

        renameMap.put("股票代码", "stockCode");
        renameMap.put("股票简称", "name");


        return renameMap;
    }

    /**
     * 问句2的字段map
     * * [涨停封单量占成交量比[20220408], 涨停类型[20220408], 涨停封单量占流通a股比[20220408], 首次涨停时间[20220408],
     * * code, a股市值(不含限售股)[20220408], 最新价, 最新涨跌幅, market_code, 涨停封单额[20220408], 涨停[20220408], 股票代码,
     * * 涨停原因类别[20220408], 几天几板[20220408], 涨停明细数据[20220408], 涨停封单量[20220408], 股票简称,
     * * 涨停开板次数[20220408], 最终涨停时间[20220408], 连续涨停天数[20220408]]
     *
     * @noti : 两字段与上重合, 删除
     */
    protected Map<Object, Object> getRenameMap2(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (WenCaiApi.fieldLike(column.toString(), "涨停封单量占成交量比[20220408]")) {
                renameMap.put(column, "highLimitBlockadeVolumeRate"); // 封锁成交比率 --> 封成比
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停类型[20220408]")) {
                renameMap.put(column, "highLimitType"); // highLimit: 涨停
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停封单量占流通a股比[20220408]")) {
                renameMap.put(column, "highLimitBlockadeCMVRate"); // 封单额/ 流通市值
            } else if (WenCaiApi.fieldLike(column.toString(), "首次涨停时间[20220408]")) {
                renameMap.put(column, "highLimitFirstTime"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停封单额[20220408]")) {
                renameMap.put(column, "highLimitBlockadeAmount"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停[20220408]")) {
                renameMap.put(column, "highLimit"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停原因类别[20220408]")) {
                renameMap.put(column, "highLimitReason"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "几天几板[20220408]")) {
                renameMap.put(column, "highLimitAmountType"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停明细数据[20220408]")) {
                renameMap.put(column, "highLimitDetail"); //  json字符串
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停封单量[20220408]")) {
                renameMap.put(column, "highLimitBlockadeVol"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "涨停开板次数[20220408]")) {
                renameMap.put(column, "highLimitBrokeTimes"); // 涨停被破坏次数
            } else if (WenCaiApi.fieldLike(column.toString(), "最终涨停时间[20220408]")) {
                renameMap.put(column, "highLimitLastTime"); // 涨停被破坏次数
            } else if (WenCaiApi.fieldLike(column.toString(), "连续涨停天数[20220408]")) {
                renameMap.put(column, "highLimitContinuousDays"); // 涨停被破坏次数
            } else if (WenCaiApi.fieldLike(column.toString(), "a股市值(不含限售股)[20220408]")) {
                renameMap.put(column, "deleteCol1"); // 封单额/ 流通市值
            }

        }
        renameMap.put("最新涨跌幅", "deleteCol2");

        renameMap.put("code", "code");  // join字段保留
        renameMap.put("market_code", "marketCode");

        renameMap.put("股票代码", "deleteCol3");
        renameMap.put("股票简称", "deleteCol4");

        renameMap.put("最新价", "deleteCol5"); // 放弃, 用close替代


        return renameMap;
    }

    /**
     * 问句3的字段map: 跌停
     * [跌停[20220408], code, 跌停封单量[20220408], 最新价, 跌停明细数据[20220408],
     * 最新涨跌幅, market_code, 首次跌停时间[20220408], 股票代码, 跌停封单量占流通a股比[20220408],
     * 跌停封单额[20220408], 跌停原因类型[20220408], 跌停开板次数[20220408], 股票简称, 跌停类型[20220408],
     * 最终跌停时间[20220408], 跌停封单量占成交量比[20220408], 连续跌停天数[20220408]]
     *
     * @noti : 两字段与上重合, 删除
     */
    protected Map<Object, Object> getRenameMap3(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (WenCaiApi.fieldLike(column.toString(), "跌停[20220408]")) {
                renameMap.put(column, "lowLimit"); // 封锁成交比率 --> 封成比
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停封单量[20220408]")) {
                renameMap.put(column, "lowLimitBlockadeVol"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停明细数据[20220408]")) {
                renameMap.put(column, "lowLimitDetails"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "首次跌停时间[20220408]")) {
                renameMap.put(column, "lowLimitFirstTime"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停封单量占流通a股比[20220408]")) {
                renameMap.put(column, "lowLimitBlockadeVolCMVRate"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停封单额[20220408]")) {
                renameMap.put(column, "lowLimitBlockadeAmount"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停原因类型[20220408]")) {
                renameMap.put(column, "lowLimitReason"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停开板次数[20220408]")) {
                renameMap.put(column, "lowLimitBrokeTimes"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停类型[20220408]")) {
                renameMap.put(column, "lowLimitType"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "最终跌停时间[20220408]")) {
                renameMap.put(column, "lowLimitLastTime"); //
            } else if (WenCaiApi.fieldLike(column.toString(), "跌停封单量占成交量比[20220408]")) {
                renameMap.put(column, "lowLimitBlockadeVolumeRate"); //封单成交比
            } else if (WenCaiApi.fieldLike(column.toString(), "连续跌停天数[20220408]")) {
                renameMap.put(column, "lowLimitContinuousDays"); //封单成交比
            }

        }
        renameMap.put("最新价", "deleteCol1");
        renameMap.put("最新涨跌幅", "deleteCol2");

        renameMap.put("code", "code");
        renameMap.put("market_code", "marketCode"); // 重复删除

        renameMap.put("股票代码", "deleteCol3");
        renameMap.put("股票简称", "deleteCol4");


        return renameMap;
    }

    /**
     * 因该库的 joinOn, 要求两个df的 join列, 列号相同,
     * 本方法将待join df, join列移动到最前方
     *
     * @param dataFrame
     * @param colNames
     * @return
     */
    public static DataFrame<Object> moveTheJoinColToForemost(DataFrame<Object> dataFrame, List<Object> colNames) {
        Set<Object> columns = dataFrame.columns();
        List<Object> newColumns = new ArrayList<>(colNames);
        for (Object column : columns) {
            if (!newColumns.contains(column)) {
                newColumns.add(column);
            }
        }
        DataFrame<Object> res = new DataFrame<>();

        for (Object newColumn : newColumns) {
            res.add(newColumn, dataFrame.col(newColumn));
        }
        return res;
    }
}
