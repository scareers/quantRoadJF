package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.Crawler;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;

/**
 * description: 行业指数 -- 行业列表 -- 包含所有二级和三级行业. 一级行业不考虑
 * // [指数@涨跌幅:前复权[20220318], 指数@收盘价:不复权[20220318], code, 指数@同花顺行业指数, 指数@所属同花顺行业级别, 指数简称, market_code, 指数代码]
 *
 * @noti 某些三级行业, 没有涨跌幅和价格. 是null.
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class BkListThs extends CrawlerThs {
    public static void main(String[] args) {
        new BkListThs().run();

    }

    public BkListThs() {
        super("industry_list");
    }

    @Override
    protected void runCore() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("同花顺行业指数;");
        if (dataFrame == null) {
            logApiError("wenCaiQuery(\"同花顺行业指数;\")");
            success = false;
            return;
        }

        //
        dataFrame = dataFrame.rename(getRenameMap(dataFrame.columns()));
        String dateStr = DateUtil.today(); // 记录日期列
        List<Object> dateStrList = new ArrayList<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            dateStrList.add(dateStr);
        }
        dataFrame = dataFrame.add("dateStr", dateStrList);

        // todo: 删除逻辑;

        try {
            DataFrameS.toSql(dataFrame, tableName, this.conn, "append", sqlCreateTable);
        } catch (SQLException e) {
            e.printStackTrace();
            logSaveError();
            success = false;
            return;
        }
        success = true;
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
                        + "industryIndex varchar(32)  null,"
                        + "industryType varchar(32)  null,"
                        + "name varchar(32)  null,"
                        + "marketCode int  null,"
                        + "indexCode varchar(32)  null,"

                        + "dateStr varchar(32)  null,"


                        + "INDEX name_index (name ASC),\n"
                        + "INDEX industryType_index (industryType ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [指数@涨跌幅:前复权[20220318], 指数@收盘价:不复权[20220318], code, 指数@同花顺行业指数, 指数@所属同花顺行业级别, 指数简称, market_code, 指数代码]
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (column.toString().startsWith("指数@涨跌幅:前复权")) {
                renameMap.put(column, "chgP");
            } else if (column.toString().startsWith("指数@收盘价:不复权")) {
                renameMap.put(column, "close");
            }
        }
        renameMap.put("code", "code");
        renameMap.put("指数@同花顺行业指数", "industryIndex");
        renameMap.put("指数@所属同花顺行业级别", "industryType");
        renameMap.put("指数简称", "name");
        renameMap.put("market_code", "marketCode");
        renameMap.put("指数代码", "indexCode");
        return renameMap;
    }
}
