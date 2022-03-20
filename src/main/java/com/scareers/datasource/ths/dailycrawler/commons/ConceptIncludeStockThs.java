package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.Crawler;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerChain;
import com.scareers.datasource.ths.ThsWebApi;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 概念(主体) --> 所有成分股股票 : 因问财限流太严重, 使用 webapi
 * [序号, 代码, 名称, 现价, 涨跌幅(%), 涨跌, 涨速(%), 换手(%), 量比, 振幅(%), 成交额, 流通股, 流通市值, 市盈率, 加自选]
 *
 * @noti : 该表支持 概念 查 所有股票成分
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class ConceptIncludeStockThs extends CrawlerThs {
    public static int poolCoreSize = 16;

    public static void main(String[] args) {
        new ConceptIncludeStockThs(true).run();


    }

    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存;

    public ConceptIncludeStockThs(boolean forceUpdate) {
        super("concept_include_stock");
        this.forceUpdate = forceUpdate;
    }

    protected AtomicInteger failAmount = new AtomicInteger(0);

    protected void runTask(String conceptcode, String dateStr) {
        if (!forceUpdate) {
            String sql = StrUtil.format("select count(*) from {} where dateStr='{}' and conceptName='{}'", tableName,
                    dateStr, conceptcode);
            try {
                DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
                if (Integer.parseInt(dataFrame.get(0, 0).toString()) > 0) {
                    return; // 当不强制更新, 判定是否已运行过, 运行过则直接返回, 连带不更新 关系数据表
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        DataFrame<Object> dataFrame = null;
        while (dataFrame == null) { // 死循环一定要成功!
            dataFrame = ThsWebApi.getIncludeStocksByIndustryOrConceptCode(conceptcode, 3);
        }

        // 1.重命名列
        dataFrame = dataFrame.rename(getRenameMap(dataFrame.columns()));
        // 2. dateStr字段添加
        List<Object> dateStrList = new ArrayList<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            dateStrList.add(dateStr);
        }
        dataFrame = dataFrame.add("dateStr", dateStrList);
        // 将 conceptName 字段 绝对改为 concept字符串
        for (int i = 0; i < dataFrame.length(); i++) {
            dataFrame.set(i, "conceptName", conceptcode);
        }

        // 3.@noti: 不再保存 conceptNews 列
        try {
            dataFrame = dataFrame.drop("conceptNews");
        } catch (Exception e) {

        }

        try {
            String sqlDelete = StrUtil.format("delete from {} where dateStr='{}' and conceptName='{}'", tableName,
                    dateStr, conceptcode);
            execSql(sqlDelete, conn);
            DataFrameS.toSql(dataFrame, tableName, this.conn, "append", sqlCreateTable);
        } catch (Exception e) {
            e.printStackTrace();
            logSaveError();
            failAmount.incrementAndGet();
            return;
        }
    }

    @Override
    protected void runCore() {
        log.warn("同花顺爬虫依赖: {} --> {}", "ConceptIncludeStockThs", "ConceptListThs");

        String dateStr = DateUtil.today(); // 要求今日已经爬取过
        List<String> conceptList = ThsDbApi.getConceptSimpleCodeList(dateStr);
//        ExecutorService executorService = ThreadUtil.newExecutor(poolCoreSize, poolCoreSize * 2, Integer.MAX_VALUE);

        for (String concept : Tqdm.tqdm(conceptList, "process: ")) {
            runTask(concept, dateStr);
        }
//        CrawlerChain.waitPoolFinish(executorService);

        if (failAmount.get() == 0) {
            success = true;
        } else {
            success = false;
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
                        + "marketValue double  null,"
                        + "conceptName varchar(32)  null,"
                        + "code varchar(32)  null,"
                        + "stockName varchar(32)  null,"
                        + "codeFull varchar(32)  null,"
                        + "newPrice double  null,"
                        + "chgP double  null,"
                        + "marketCode int  null,"
                        + "belongToConceptAmount int  null,"
                        + "conceptParse longtext  null,"
//                        + "conceptNews longtext  null,"


                        + "dateStr varchar(32)  null,"

                        + "INDEX stockName_index (stockName ASC),\n"
                        + "INDEX code_index (code ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC),\n"
                        + "INDEX conceptName_index (conceptName ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [序号, 代码, 名称, 现价, 涨跌幅(%), 涨跌, 涨速(%), 换手(%), 量比, 振幅(%), 成交额, 流通股, 流通市值, 市盈率, 加自选]
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        HashMap<Object, Object> renameMap = new HashMap<>();
        for (Object column : rawColumns) {
            if (column.toString().startsWith("a股市值(不含限售股)")) {
                renameMap.put(column, "marketValue");
            }
        }
        renameMap.put("所属概念", "conceptName");
        renameMap.put("概念资讯", "conceptNews"); // 一般为 "-"
        renameMap.put("code", "code");
        renameMap.put("股票简称", "stockName");
        renameMap.put("最新价", "newPrice");
        renameMap.put("最新涨跌幅", "chgP");
        renameMap.put("market_code", "marketCode");
        renameMap.put("所属概念数量", "belongToConceptAmount");
        renameMap.put("股票代码", "codeFull");
        renameMap.put("概念解析", "conceptParse");

        return renameMap;
    }
}
