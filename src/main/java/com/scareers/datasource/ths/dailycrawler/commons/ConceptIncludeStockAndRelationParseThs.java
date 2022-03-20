package com.scareers.datasource.ths.dailycrawler.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerChainEm;
import com.scareers.datasource.ths.dailycrawler.CrawlerThs;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.scareers.datasource.ths.wencai.WenCaiApi.wenCaiQuery;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 概念(主体) --> 所有成分股股票 + 概念相关性分析
 * 读取 stockBelongTo 数据表, 以及 ConceptList 表; 自行分析
 * 1. 放弃实现: 放弃读取ths web api; 因为太容易被屏蔽
 * 2. 可选实现: 可以使用问财, 单概念一次访问, 但需要控制频率, 且 需要两类问句; "金属铅"等少数概念无结果!
 * 3. 具体实现: 读取两个数据表, 只对常规概念, 进行成分股解析, 以及关系解析, 保存结果. 因此不访问http; 速度比想象中快太多
 *
 * @noti : 该表支持 概念 查 所有股票成分 以及 与其他概念的相关性
 * @author: admin
 * @date: 2022/3/19/019-20:59:21
 */
public class ConceptIncludeStockAndRelationParseThs extends CrawlerThs {
    public static void main(String[] args) {
        new ConceptIncludeStockAndRelationParseThs(true).run();
    }


    boolean forceUpdate; // 是否强制更新, 将尝试删除 dateStr==今日, 再行保存;

    public ConceptIncludeStockAndRelationParseThs(boolean forceUpdate) {
        super("concept_include_stock_and_relation_parse");
        this.forceUpdate = forceUpdate;
    }


    HashMap<String, String> conceptNameMap;
    HashMap<String, String> conceptUrlMap;

    @Override
    protected void runCore() {
        logDependence("ConceptListThs");
        logDependence("StockBelongToThs");
        String dateStr = DateUtil.today();
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

        // 1.基本数据读取: 从两大依赖数据表
        log.info("1.读取概念列表,以及个股所属概念列表");
        DataFrame<Object> stockBelongToConceptsWithNameDf = ThsDbApi.getStockBelongToConceptsWithName(dateStr);
        HashMap<String, String> conceptNameWithCodeMap = ThsDbApi.getConceptNameWithCodeMap(dateStr);

        // 2.构建成分股map: 概念名称: 概念成分股
        log.info("2.开始构建成分股Map");
        HashMap<String, List<List<String>>> includeMap = new HashMap<>(); // 单只成分股为二元组, 元素1为代码,2为个股名称
        // 成分股列表, key必须是常规概念, 即在conceptNameWithCodeMap里面
        for (String s : conceptNameWithCodeMap.keySet()) {
            includeMap.put(s, new ArrayList<>()); // 空列表,代表没有成分股.
        }

        for (int i = 0; i < stockBelongToConceptsWithNameDf.length(); i++) {
            // code	  name	  belongToConceptAll
            String stockCode = stockBelongToConceptsWithNameDf.get(i, "code").toString();
            String stockName = stockBelongToConceptsWithNameDf.get(i, "name").toString();
            Object belongToConceptAll = stockBelongToConceptsWithNameDf.get(i, "belongToConceptAll");
            if (belongToConceptAll == null) {
                continue; // 例如某些退市个股没有所属概念
            }
            JSONArray conceptList = JSONUtilS
                    .parseArray(belongToConceptAll.toString());

            for (Object o : conceptList) {
                String conceptName = o.toString();
                if (conceptNameWithCodeMap.containsKey(conceptName)) {
                    includeMap.get(conceptName).add(Arrays.asList(stockCode, stockName)); // 不会null
                }
            }
        }

        // 3.0. 首先构建成分股代码
        log.info("3.开始构建关系Map");
        HashMap<String, Set<String>> includeStockCodeMap = new HashMap<>();
        for (String conceptName : includeMap.keySet()) {
            List<List<String>> stockList = includeMap.get(conceptName);
            Set<String> includeCodes = stockList.stream().map(value -> value.get(0)).collect(Collectors.toSet());
            includeStockCodeMap.put(conceptName, includeCodes);
        }


        // 3.构建关系图谱
        // 概念A 与 概念B 的关系对象, 分为3个字段:
        // 1.两者相同的成分股个数 x!
        // 2.x / 概念A成分股总数
        // 3.x / 概念B成分股总数
        // --> A与B 相关度有多少? 显然这个值 应当对 2和3的两个百分比, 取权重加成.
        HashMap<String, HashMap<String, List<Double>>> relationMap = new HashMap<>();
        // key为概念A名称, value是其关系map, 该Map key为所有其他概念, value是 两者相关对象,
        // 关系:List<Double>, 3个元素, 第一是个股相同数量, 第二个是 数量/概念A总数, 3是B

        for (String conceptNameA : includeMap.keySet()) {
            relationMap.put(conceptNameA, new HashMap<>()); // 无脑空Map

            List<List<String>> stockListA = includeMap.get(conceptNameA);
            if (stockListA.size() == 0) {
                continue; // 没有成分股, 则空Map, 没有关系
            }
            // 自身成分股代码集合
            Set<String> includeCodesA = includeStockCodeMap.get(conceptNameA);

            // 遍历概念B
            for (String conceptNameB : includeMap.keySet()) {
                if (conceptNameB.equals(conceptNameA)) {
                    continue; // 自己无视
                }
                List<List<String>> stockListB = includeMap.get(conceptNameB);
                if (stockListB.size() == 0) {
                    continue; // B没有成分股, 也无视
                }
                // B成分股代码
                Set<String> includeCodesB = includeStockCodeMap.get(conceptNameB);
                // 1.求成分股交集
                Set<String> intersectionStockList = CommonUtil.intersectionOfSet(includeCodesB, includeCodesA);
                double intersectionAmount = intersectionStockList.size(); // 转为double
                // 2.占A比
                double percentA = intersectionAmount / includeCodesA.size();
                double percentB = intersectionAmount / includeCodesB.size();
                relationMap.get(conceptNameA).put(conceptNameB, Arrays.asList(intersectionAmount, percentA, percentB));
            }
        }


        // 4.构建结果df:
        // 4.1. 列: id / conceptName, includeStocks, relationMap, dateStr
        log.info("4.开始构建解析结果Df");
        DataFrame<Object> res = new DataFrame<>(
                Arrays.asList("conceptName", "includeStocks", "relationMap", "dateStr"));
        for (String conceptName : relationMap.keySet()) {
            String includeStocks = JSONUtilS.toJsonStr(includeMap.get(conceptName));
            String relationMapStr = JSONUtilS.toJsonStr(relationMap.get(conceptName));
            res.append(Arrays.asList(conceptName, includeStocks, relationMapStr, dateStr));
        }

        // 5.保存结果.
        log.info("5.开始保存");
        try {
            String sqlDelete = StrUtil.format("delete from {} where dateStr='{}'", tableName, dateStr);
            execSql(sqlDelete, conn);
            DataFrameS.toSql(res, tableName, this.conn, "append", sqlCreateTable);
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
        // 注意: belongToIndustryAll字段改为 --> industryLevel1/2/3 3个字段
        sqlCreateTable = StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "conceptName varchar(32)  null,"

                        + "includeStocks longtext null,"
                        + "relationMap longtext  null,"

                        + "dateStr varchar(32)  null,"

                        + "INDEX conceptName_index (conceptName ASC),\n"
                        + "INDEX dateStr_index (dateStr ASC)\n"
                        + "\n)"
                , tableName);
    }


    /**
     * [序号, 代码, 名称, 现价, 涨跌幅(%), 涨跌, 涨速(%), 换手(%), 量比, 振幅(%), 成交额, 流通股, 流通市值, 市盈率, 加自选]
     * // @noti: 需要另行增加 conceptCode 和 conceptName 两个字段!
     *
     * @param rawColumns
     * @return
     */
    @Override
    protected Map<Object, Object> getRenameMap(Set<Object> rawColumns) {
        return null; // 不需要
    }


    /**
     * 两种问法, 问财访问但概念个股! 已放弃
     *
     * @param conceptNameList
     * @return
     * @deprecated 不再使用问财访问成分股, 而自行从数据表解析! 本方案可靠性尚可. 但关系解析需要另写逻辑.
     */
    public static List<String> temp(List<String> conceptNameList) {
        /*
            List<String> conceptNameList = ThsDbApi.getConceptNameList(DateUtil.today());
            while (conceptNameList.size() > 0) {
                Console.log("剩余错误数量: {}", conceptNameList.size());
                conceptNameList = temp(conceptNameList); // 失败的再次执行
            }
         */
        ExecutorService pool = ThreadUtil.newExecutor(1, 2, Integer.MAX_VALUE);
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < conceptNameList.size(); i++) {
            int finalI = i;
            pool.submit(new Runnable() {
                @Override
                public void run() {
//                    String concept = RandomUtil.randomEle(conceptNameList);
                    String concept = conceptNameList.get(finalI);
                    if (concept.endsWith("概念")) {
                        concept = concept.substring(0, concept.length() - 2);
                    }
                    DataFrame<Object> dataFrame = wenCaiQuery(StrUtil.format("所属概念包含{}", concept));
                    if (dataFrame == null || dataFrame.size() <= 1) {
                        dataFrame = wenCaiQuery(StrUtil.format("{}概念", concept));
                    }

                    if (dataFrame == null || dataFrame.size() <= 1) {
                        Console.log("error: {}", concept);
                        errors.add(concept);
                    } else {
                        Console.log("ok: {}, {}", concept, dataFrame.length());
                    }
                }
            });
        }
        CrawlerChainEm.waitPoolFinish(pool);
        return errors;
    }
}
