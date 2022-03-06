package com.scareers.datasource.eastmoney.dailycrawler.commons;

import cn.hutool.core.date.*;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.Crawler;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 交易日期数据爬取
 * 逻辑:
 * 1.过去的时间, 用 上证指数有行情的日期,
 * 2.将来的时间, 用 数据中心--财经日历--休市安排 API:
 * https://datacenter-web.eastmoney.com/api/data/get?type=RPTA_WEB_ZGXSRL&sty=ALL&ps=200&st=sdate&sr=-1&callback=jQuery11230209712528561385_1646443457043&_=1646443457044
 * 该api可显示未来的节日安排: EmDataApi.getFutureMarketCloseDates
 *
 * @author: admin
 * @date: 2022/3/5/005-09:22:50
 */
public class TradeDates extends Crawler {
    public TradeDates() {
        super("trade_dates");
    }

    public static void main(String[] args) {
        new TradeDates().run();
    }

    @Override
    public void setDb() {
        this.setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        // Arrays.asList("date", "is_open", "is_weekend", "holiday", "sdate", "market", "edate");
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "date varchar(32) not null,"
                        + "is_open int not null,"
                        + "is_weekend int not null,"
                        + "holiday varchar(256) null,"
                        + "sdate varchar(32) null,"
                        + "market varchar(256) null,"
                        + "edate varchar(32) null,"
                        + "self_record_time varchar(32) null"
                        + "\n)"
                , tableName);
    }

    @Override
    protected void runCore() {
        DataFrame<Object> dfTemp = EmQuoteApi.getQuoteHistorySingle(false,
                SecurityBeanEm.getShangZhengZhiShu(),
                null,
                null, "101", "1", 5,
                3000); // 使用缓存
        if (dfTemp == null) {
            success = false;
            logApiError("上证指数历史日K线");
            return;
        }

        // 1.过去的日期列表
        List<String> pastDates = DataFrameS.getColAsStringList(dfTemp, "日期");
        Set<DateTime> historyOpens = pastDates.stream().map(DateUtil::parse).collect(Collectors.toSet());

        // 2.获取未来的日期
        // [结束日期, 节日描述, 休市市场, 开始日期, 未知]
        DataFrame<Object> futureDatesDf = EmDataApi.getFutureMarketCloseDates(3000, 3);
        if (futureDatesDf == null) {
            success = false;
            logApiError("东财休市安排数据");
            return;
        }

        // 最小的有记录的日期, >=时, 使用第二套逻辑, 此前的使用简单逻辑
        DateTime minDateOfHoliday = DateUtil.parse(futureDatesDf.get(futureDatesDf.length() - 1, "开始日期").toString());
        // 确认A股不开市的日期集合
        Set<DateTime> notOpens = new HashSet<>();
        for (int i = 0; i < futureDatesDf.length(); i++) {
            if (futureDatesDf.get(i, "休市市场").toString().contains("A股")) {
                DateTime start = DateUtil.parse(futureDatesDf.get(i, "开始日期").toString());
                DateTime end = DateUtil.parse(futureDatesDf.get(i, "结束日期").toString());
                DateRange range = DateUtil.range(start, end, DateField.DAY_OF_YEAR);
                for (DateTime dateTime : range) {
                    notOpens.add(dateTime);
                }
            }
        }

        // 3.结果df构造
        List<String> colsActual = Arrays
                .asList("date", "is_open", "is_weekend", "holiday", "sdate", "market", "edate", "self_record_time");
        DataFrame<Object> result = new DataFrame<>(colsActual);

        // 4.全部日期, 遍历填充 结果df
        DateTime startDate = DateUtil.parse(pastDates.get(0)); // 上证指数首次交易
        DateTime endDate = DateUtil.parse(futureDatesDf.get(0, "开始日期").toString()); // 休市api最新日期的开始日期
        DateRange dates = DateUtil.range(startDate, endDate, DateField.DAY_OF_YEAR);

        for (DateTime date : dates) {
            List<Object> row = new ArrayList<>();
            row.add(DateUtil.format(date, DatePattern.NORM_DATE_PATTERN)); // 日期均添加

            // 休市api获取的数据, 的逻辑
            if (date.compareTo(minDateOfHoliday) >= 0) {
                // 1.是否开市判定, 1开市,0不开
                if (notOpens.contains(date)) {
                    row.add(0); // 节日不开市
                } else if (date.isWeekend()) {
                    row.add(0); // 周末不开市
                } else {
                    row.add(1);  // 其余开市
                }

                // 2.周末判定
                if (date.isWeekend()) {
                    row.add(1);
                } else {
                    row.add(0);
                }

                // 3.节日, 只在 sdate 那一条进行记录, 这里筛选原df, 开始日期列, == 当前遍历date的那一行
                // 4.sdate  , 节日才有
                DataFrame<Object> select = futureDatesDf
                        .select(row0 -> DateUtil.parse(row0.get(3).toString()).equals(date));
                if (select.length() > 0) {
                    row.add(select.col("节日描述").get(0).toString());
                    row.add(select.col("开始日期").get(0).toString());
                    row.add(select.col("休市市场").get(0).toString());
                    row.add(select.col("结束日期").get(0).toString());
                } else {
                    row.add(null);
                    row.add(null);
                    row.add(null);
                    row.add(null);
                }


            } else { // 常规的逻辑,历史日期
                // 1.是否开市判定, 1开市,0不开
                if (historyOpens.contains(date)) {
                    row.add(1);
                } else {
                    row.add(0);
                }

                // 2.周末判定
                if (date.isWeekend()) {
                    row.add(1);
                } else {
                    row.add(0);
                }
                // 3.节日判定: 节日描述,开始日期,休市市场,结束日期
                row.add(null);
                row.add(null);
                row.add(null);
                row.add(null);
            }

            row.add(getRecordTime());
            result.append(row);
        }

        try {
            DataFrameS.toSql(result, tableName, this.conn, "replace", sqlCreateTable);
        } catch (SQLException e) {
            e.printStackTrace();
            logSaveError();
            success = false;
            return;
        }
        success = true;
    }


//    public static String sqlTableCreateTemplate = "create table if not exists {}";


}
