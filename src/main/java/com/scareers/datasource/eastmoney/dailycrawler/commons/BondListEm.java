package com.scareers.datasource.eastmoney.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.sqlapi.EastMoneyDbApi.getBondRecordAmountByDateStr;

/**
 * description: 每日转债列表
 * 逻辑: 可转债 截面tick, 得到列表. 再调用 sb.createStock(代码,true)
 * 保留大多数 bean的字段;
 *
 * @author: admin
 * @date: 2022/6/6/005-09:22:50
 */
public class BondListEm extends CrawlerEm {
    public BondListEm() {
        super("bond_list");
    }

    public static void main(String[] args) {
        new BondListEm().run();
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

                        + "isShenA boolean not null,"
                        + "isHuA boolean not null,"
                        + "isKCB boolean not null,"
                        + "isCYB boolean not null,"
                        + "isShenB boolean not null,"
                        + "isHuB boolean not null,"
                        + "isJingA int not null,"
                        + "isXSB boolean not null,"
                        + "self_record_time varchar(32) null,"
                        + "dateStr varchar(32) null,"

                        + "INDEX secCode_index (secCode ASC),\n"
                        + "INDEX name_index (name ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC)\n"
                        + "\n)"
                , tableName);
    }

    @Override
    protected void runCore() {
        String today = DateUtil.today(); // today 作为实际意义上的今天, 注意逻辑!
        try {
            if (EastMoneyDbApi.isTradeDate(today)) {
                if (DateUtil.hour(DateUtil.date(), true) >= 15) {
                    today = DateUtil.today();
                } else {
                    today = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
                }
            } else {
                today = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        Integer bondRecordAmountByDateStr = getBondRecordAmountByDateStr(today);
        if (bondRecordAmountByDateStr != null && bondRecordAmountByDateStr > 100) {
            log.info("今日 bond_list 已经有记录, 不再爬取");
            success = true;
            return;
        }


        DataFrame<Object> bondListDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("可转债"));
        List<String> bondCodes = DataFrameS.getColAsStringList(bondListDf, "资产代码");
        List<SecurityBeanEm> bondListBeans;
        try {
            bondListBeans = SecurityBeanEm.createBondList(bondCodes, true);
        } catch (Exception e) {
            e.printStackTrace();
            logApiError("SecurityBeanEm.createBondList");
            success = false;
            return;
        }

        List<String> cols = Arrays.asList(
                "secCode", "name", "pinYin", "quoteId", "jys", "idRaw",
                "innerCode", "unifiedCode",
                "classify", "market", "marketType", "typeUs",
                "securityType", "securityTypeName",
                "convertRawJsonObject",
                "isShenA", "isHuA", "isKCB", "isCYB", "isShenB", "isHuB", "isJingA", "isXSB",

                "self_record_time",
                "dateStr"
        );
        DataFrame<Object> res = new DataFrame<>(cols);


        for (SecurityBeanEm bean : bondListBeans) {
            List<Object> row = new ArrayList<>();

            // 转债基本信息类
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
            row.add(bean.isShenA());
            row.add(bean.isHuA());
            row.add(bean.isKCB());
            row.add(bean.isCYB());
            row.add(bean.isShenB());
            row.add(bean.isHuB());
            row.add(bean.isJingA());
            row.add(bean.isXSB());

            row.add(getRecordTime());
            row.add(today);
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
