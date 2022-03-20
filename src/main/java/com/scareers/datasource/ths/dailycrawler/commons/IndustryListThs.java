package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 行业指数 -- 行业列表 -- 包含所有二级和三级行业. 一级行业不考虑
 * // [指数@涨跌幅:前复权[20220318], 指数@收盘价:不复权[20220318], code, 指数@同花顺行业指数, 指数@所属同花顺行业级别, 指数简称, market_code, 指数代码]
 *
 * @noti 某些三级行业, 没有涨跌幅和价格. 是null.
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class IndustryListThs extends CrawlerThs {
    public static void main(String[] args) {
        new IndustryListThs(false).run();

    }

    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存

    public IndustryListThs(boolean forceUpdate) {
        super("industry_list");
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
                    return; // 当不强制更新, 判定是否已运行过, 运行过则直接返回
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("同花顺行业指数;");
        if (dataFrame == null) {
            logApiError("wenCaiQuery(\"同花顺行业指数;\")");
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
