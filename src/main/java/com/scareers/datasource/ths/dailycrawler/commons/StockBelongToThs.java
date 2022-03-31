package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 个股(主体) --> 所属行业列表(主内容) + 所属概念列表
 * // 当然, 一只股只属于"一个行业" , 这里列表指代 1/2/3级行业. 一般1级行业无视; 三级行业分为3个字段
 * // 概念列表仅 保存于单个字段, 解析为json保存!
 * [所属概念, code, 所属同花顺行业, 股票简称, 最新价, 最新dde大单净额, 最新涨跌幅, 市盈率(pe)[20220318], market_code, 总股本[20220318], 股票代码]
 *
 * @noti : 该表支持 股票查(任意级别)行业, (任意级别)行业查所属成分股!!
 * @noti : 该表支持 股票查 所属概念列表(json列表), 但不支持反查. 反查请查看 ConceptIncludeStock
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class StockBelongToThs extends CrawlerThs {
    public static void main(String[] args) {
        new StockBelongToThs(true).run();

    }

    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存;

    public StockBelongToThs(boolean forceUpdate) {
        super("stock_belong_to_industry_and_concept");
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
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("所属同花顺行业;"); // 也将返回概念列表.
        if (dataFrame == null) {
            logApiError("wenCaiQuery(\"所属同花顺行业;\")");
            success = false;
            return;
        }

        // 1.重命名列
        dataFrame = dataFrame.rename(getRenameMap(dataFrame.columns()));
        // 2. dateStr字段添加
        List<Object> dateStrList = new ArrayList<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            dateStrList.add(dateStr);
        }
        dataFrame = dataFrame.add("dateStr", dateStrList);

        // 3.分割行业字段
        // 将 行业字段  belongToIndustryAll 分拆为3个字段, 代表123级行业. 然后删除汇总 belongToIndustryAll
        // 电子-光学光电子-面板
        List<String> belongToIndustryAll = DataFrameS.getColAsStringList(dataFrame, "belongToIndustryAll");
        List<Object> industryLevel1List = new ArrayList<>(); // 1/2/3 级行业拆分
        List<Object> industryLevel2List = new ArrayList<>();
        List<Object> industryLevel3List = new ArrayList<>();
        for (String s : belongToIndustryAll) {
            if (s == null) {
                industryLevel1List.add(null);
                industryLevel2List.add(null);
                industryLevel3List.add(null); // 占位必须
                continue;
            }
            List<String> industryList = StrUtil.split(s, "-");
            if (industryList.size() == 0) {
                industryLevel1List.add(null);
                industryLevel2List.add(null);
                industryLevel3List.add(null); // 占位必须
            } else if (industryList.size() == 1) {
                industryLevel1List.add(industryList.get(0));
                industryLevel2List.add(null);
                industryLevel3List.add(null); // 占位必须
            } else if (industryList.size() == 2) {
                industryLevel1List.add(industryList.get(0));
                industryLevel2List.add(industryList.get(1));
                industryLevel3List.add(null); // 占位必须
            } else {
                industryLevel1List.add(industryList.get(0));
                industryLevel2List.add(industryList.get(1));
                industryLevel3List.add(industryList.get(2)); // 占位必须
            }
        }
        dataFrame = dataFrame.drop("belongToIndustryAll"); // 删除列
        dataFrame = dataFrame.add("industryLevel1", industryLevel1List); // 新增3列
        dataFrame = dataFrame.add("industryLevel2", industryLevel2List); // 新增3列
        dataFrame = dataFrame.add("industryLevel3", industryLevel3List); // 新增3列


        // 4.将 belongToConceptAll 概念列表, 解析转换为 json列表字符串保存
        // 消费电子概念;小米概念;专精特新;
        for (int i = 0; i < dataFrame.length(); i++) {
            Object belongToConceptAll = dataFrame.get(i, "belongToConceptAll");
            if (belongToConceptAll != null) { // 为null时不修改
                List<String> stringList = StrUtil.split(belongToConceptAll.toString(), ";");
                dataFrame.set(i, "belongToConceptAll", JSONUtilS.toJsonStr(stringList)); // 改为json列表形式字符串
            }
        }

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
        // 注意: belongToIndustryAll字段改为 --> industryLevel1/2/3 3个字段
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "pe double  null,"
                        + "totalShareCapital double  null,"
                        + "belongToConceptAll text  null,"
                        + "code varchar(32)  null,"
                        + "codeFull varchar(32)  null,"
                        + "name varchar(32)  null,"
                        + "industryLevel1 varchar(32)  null,"
                        + "industryLevel2 varchar(32)  null,"
                        + "industryLevel3 varchar(32)  null,"
                        + "newPrice double  null,"
                        + "ddeNet double  null,"
                        + "chgP double  null,"
                        + "marketCode int  null,"


                        + "dateStr varchar(32)  null,"

                        + "INDEX name_index (name ASC),\n"
                        + "INDEX code_index (code ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC),\n"
                        + "INDEX industryLevel1_index (industryLevel1 ASC),\n"
                        + "INDEX industryLevel2_index (industryLevel2 ASC),\n"
                        + "INDEX industryLevel3_index (industryLevel3 ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [所属概念, code, 所属同花顺行业, 股票简称, 最新价, 最新dde大单净额, 最新涨跌幅, 市盈率(pe)[20220318],
     * market_code, 总股本[20220318], 股票代码]
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (column.toString().startsWith("市盈率(pe)")) {
                renameMap.put(column, "pe");
            } else if (column.toString().startsWith("总股本")) {
                renameMap.put(column, "totalShareCapital");
            }
        }
        renameMap.put("所属概念", "belongToConceptAll");
        renameMap.put("code", "code");
        renameMap.put("所属同花顺行业", "belongToIndustryAll");
        renameMap.put("股票简称", "name");
        renameMap.put("最新价", "newPrice");
        renameMap.put("最新dde大单净额", "ddeNet");
        renameMap.put("最新涨跌幅", "chgP");
        renameMap.put("market_code", "marketCode");
        renameMap.put("股票代码", "codeFull");
        return renameMap;
    }
}
