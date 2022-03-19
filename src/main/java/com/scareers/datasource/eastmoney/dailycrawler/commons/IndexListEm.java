package com.scareers.datasource.eastmoney.dailycrawler.commons;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description: 股票列表
 * 逻辑: 沪深京A股 截面tick, 得到列表. 再调用 sb.createStock(代码,true)
 * 保留大多数 bean的字段;
 *
 * @noti: 比tushare主要少了 "上市退市日期", 是否退市 等字段. 其他字段均可由另外api补充
 * @author: admin
 * @date: 2022/3/5/005-09:22:50
 */
public class IndexListEm extends CrawlerEm {
    public IndexListEm() {
        super("index_list");
    }

    public static void main(String[] args) {
        new IndexListEm().run();
    }

    @Override
    public void setDb() {
        this.setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        // @noti: 查询结果的 id 字段, 替换为 idRaw 列
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "secCode varchar(32) not null,"
                        + "name varchar(64) not null,"
                        + "pinYin varchar(32) not null,"
                        + "quoteId varchar(32) not null,"
                        + "jys varchar(32) not null,"
                        + "idRaw varchar(64) not null,"
                        + "innerCode varchar(32) not null,"
                        + "unifiedCode varchar(64) not null,"
                        + "classify varchar(32) not null,"
                        + "market varchar(32) not null,"
                        + "marketType varchar(32) not null,"
                        + "typeUs varchar(32) not null,"
                        + "securityType varchar(32) not null,"
                        + "securityTypeName varchar(64) not null,"
                        + "convertRawJsonObject text not null,"

                        + "isHuIndex boolean not null,"
                        + "isShenIndex boolean not null,"
                        + "isZhongIndex boolean not null,"
                        + "self_record_time varchar(32) null,"


                        + "INDEX secCode_index (secCode ASC),\n"
                        + "INDEX name_index (name ASC)\n"
                        + "\n)"
                , tableName);
    }

    @Override
    protected void runCore() {
        DataFrame<Object> indexListDF = EmQuoteApi.getRealtimeQuotes(Arrays.asList("沪深系列指数"));
        DataFrame<Object> indexListDF2 = EmQuoteApi.getRealtimeQuotes(Arrays.asList("中证系列指数"));
        indexListDF = indexListDF.concat(indexListDF2);


        List<String> bkCodes = DataFrameS.getColAsStringList(indexListDF, "资产代码");

        List<SecurityBeanEm> bkListBeans;
        try {
            bkListBeans = SecurityBeanEm.createIndexList(bkCodes);
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createBKList");
            success = false;
            return;
        }

        List<String> cols = Arrays.asList(
                "secCode", "name", "pinYin", "quoteId", "jys", "idRaw",
                "innerCode", "unifiedCode",
                "classify", "market", "marketType", "typeUs",
                "securityType", "securityTypeName",
                "convertRawJsonObject",
                "isHuIndex", "isShenIndex", "isZhongIndex",

                "self_record_time"
        );
        DataFrame<Object> res = new DataFrame<>(cols);

        for (SecurityBeanEm bean : bkListBeans) {
            List<Object> row = new ArrayList<>();

            // 股票基本信息类
            row.add(bean.getSecCode());
            row.add(bean.getName());
            row.add(bean.getPinYin());
            row.add(bean.getQuoteId());
            row.add(bean.getJYS());
            row.add(bean.getID());

            // 代码
            row.add(bean.getInnerCode());
            row.add(bean.getUnifiedCode());

            // 分类市场
            row.add(bean.getClassify());
            row.add(bean.getMarket());
            row.add(bean.getMarketType());
            row.add(bean.getTypeUS());

            // 资产类别
            row.add(bean.getSecurityType());
            row.add(bean.getSecurityTypeName());

            // 原始转换json
            row.add(JSONUtilS.toJsonStr(bean.getConvertRawJsonObject()));

            // 所属板块标志
            row.add(bean.isHuIndex());
            row.add(bean.isShenIndex());
            row.add(bean.isZhongIndex());


            row.add(getRecordTime());
            res.append(row);
        }

        try {
            DataFrameS.toSql(res, tableName, this.conn, "append", sqlCreateTable);
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
