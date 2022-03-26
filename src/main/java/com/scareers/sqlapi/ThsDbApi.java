package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.hssf.record.InterfaceEndRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 同花顺数据库sql api
 *
 * @author: admin
 * @date: 2022/3/20/020-01:32:09
 */
public class ThsDbApi {
    /**
     * 核心变量: 非典型概念(名称集合)
     * 非典型概念: 给定概念A, 当计算A 与其他所有常规概念的 "关系"时, 将直接排除掉这些 "非典型概念"
     * 也就是 例如 计算 "风电" 概念的 关系列表时, 一般不计算 与 "ST板块" 的相关性
     * 这些非典型概念, 一般是成分股很多, 但是并非 "概念性的"
     */
    public static HashSet<String> atypicalConceptNameSet = new HashSet<>();

    public static Connection connection = ConnectionFactory.getConnLocalThs();
    private static Cache<String, Boolean> isTradeDateCache = CacheUtil.newLRUCache(2048);


    static {
        initStypicalConceptNameSet();
    }

    private static void initStypicalConceptNameSet() {
        atypicalConceptNameSet.addAll(Arrays.asList(
                "MSCI概念", "标普道琼斯A股", "融资融券", "沪股通", "参股新三板", "中字头股票", "送转填权", "深股通"
        ));
    }

    public static void main(String[] args) {
        List<String> conceptNameList = getConceptNameList(DateUtil.today());
//        for (String s : conceptNameList) {
//            Console.log(s);
//        }

//        List<ThsSimpleStock> conceptIncludeStocks = getConceptIncludeStocks(RandomUtil.randomEle(conceptNameList),
//                DateUtil.today());
//        Console.log(conceptIncludeStocks.get(0));
//        Console.log(conceptIncludeStocks);

//        List<ThsConceptRelation> conceptAllRelations = getConceptAllRelations();
//        Console.log(conceptAllRelations.get(0));
//        Console.log(conceptAllRelations.size());
//        Console.log(conceptAllRelations.stream().mapToDouble(value -> value.getSameIncludeStockAmount()).max());

        List<ThsConceptRelation> maxConceptRelationshipOf = getMaxConceptRelationshipOf(
//                RandomUtil.randomEle(conceptNameList),
               "风电",
                DateUtil.today(), 10, 0.6, 0.4, true);
        for (ThsConceptRelation thsConceptRelation : maxConceptRelationshipOf) {
            Console.log(thsConceptRelation);
        }
    }

    /**医疗器械概念
     * @param dateStr 给定日期, 可以不给定, 一般可以给定今天; 标准日期格式
     * @return 读取同花顺概念列表, 返回概念名称列表; 失败返回null ; 已经去重和去null
     */
    public static List<String> getConceptNameList(String dateStr) {
        String sql = StrUtil.format("select name from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }

        List<String> names = DataFrameS.getColAsStringList(dataFrame, "name");
        HashSet<String> nameSet = new HashSet<>();
        for (String name : names) {
            if (name != null) {
                nameSet.add(name);
            }
        }
        return new ArrayList<>(nameSet);
    }

    /**
     * 同上, 返回所有概念 6位简单代码列表
     *
     * @param dateStr
     * @return
     */
    public static List<String> getConceptSimpleCodeList(String dateStr) {
        String sql = StrUtil.format("select code from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }

        List<String> names = DataFrameS.getColAsStringList(dataFrame, "code");
        HashSet<String> nameSet = new HashSet<>();
        for (String name : names) {
            if (name != null) {
                nameSet.add(name);
            }
        }
        return new ArrayList<>(nameSet);
    }

    /**
     * 同上, 返回所有概念map.  key:value --> 概念代码简单: 概念名称
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getConceptCodeWithNameMap(String dateStr) {
        String sql = StrUtil.format("select code,name from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        HashMap<String, String> res = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            res.put(dataFrame.get(i, 0).toString(), dataFrame.get(i, 1).toString());
        }
        return res;
    }

    /**
     * 同上反向, name作为key, code为value
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getConceptNameWithCodeMap(String dateStr) {
        HashMap<String, String> conceptCodeWithNameMap = getConceptCodeWithNameMap(dateStr);
        HashMap<String, String> res = new HashMap<>();
        for (String s : conceptCodeWithNameMap.keySet()) {
            res.put(conceptCodeWithNameMap.get(s), s);
        }
        return res;
    }


    /**
     * 同上 代码为完整代码, 例如 880111.TI
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getConceptNameWithFullCodeMap(String dateStr) {
        String sql = StrUtil.format("select indexCode,name from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        HashMap<String, String> res = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            res.put(dataFrame.get(i, 1).toString(), dataFrame.get(i, 0).toString());
        }
        return res;
    }

    /**
     * 给定日期, 返回所有 股票名称,代码, 所属概念列表
     * 实测概念包含非常规概念
     *
     * @param dateStr
     * @return
     * @cols code      name	  belongToConceptAll
     */
    public static DataFrame<Object> getStockBelongToConceptsWithName(String dateStr) {
        String sql = StrUtil.format("select code,name,belongToConceptAll from stock_belong_to_industry_and_concept " +
                "where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }

    /*
    概念成分个股及关系对象
     */

    /**
     * 获取指定日期, 概念成分股列表:  json 字符串
     *
     * @param conceptName
     * @param dateStr
     * @return
     */
    public static List<ThsSimpleStock> getConceptIncludeStocks(String conceptName, String dateStr) {
        String sql = StrUtil
                .format("select includeStocks from concept_include_stock_and_relation_parse where conceptName='{}' and " +
                        "dateStr='{}'", conceptName, dateStr);
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        if (dataFrame.size() == 0) {
            return null;
        }

        List<ThsSimpleStock> res = new ArrayList<>();
        JSONArray stockRaws = JSONUtilS.parseArray(dataFrame.get(0, 0).toString());
        for (Object stockRaw : stockRaws) {
            JSONArray stockInfo = (JSONArray) stockRaw;
            res.add(new ThsSimpleStock(stockInfo.getString(0), stockInfo.getString(1))); // 先代码后名称
        }
        return res;
    }

    /**
     * 获取概念 所有与其他概念关系列表
     *
     * @param conceptName
     * @param dateStr
     * @return
     */
    public static List<ThsConceptRelation> getConceptAllRelations(String conceptName, String dateStr) {
        String sql = StrUtil
                .format("select relationMap from concept_include_stock_and_relation_parse where conceptName='{}' and " +
                        "dateStr='{}'", conceptName, dateStr);
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        if (dataFrame.size() == 0) {
            return null;
        }

        List<ThsConceptRelation> res = new ArrayList<>();
        JSONObject relationMap = JSONUtilS.parseObj(dataFrame.get(0, 0).toString());
        for (Object conceptNameBRaw : relationMap.keySet()) {
            String conceptNameB = conceptNameBRaw.toString();
            JSONArray jsonArray = relationMap.getJSONArray(conceptNameB);
            res.add(new ThsConceptRelation(
                    conceptName,
                    conceptNameB,
                    jsonArray.getInteger(0),
                    jsonArray.getDouble(1),
                    jsonArray.getDouble(2)
            ));

        }
        return res;
    }

    /**
     * 给定概念, 返回与之最相关的 前N个概念, 关系对象列表.
     * 可指定是否排除掉非典型概念!!
     */
    public static List<ThsConceptRelation> getMaxConceptRelationshipOf(String conceptName, String dateStr, int maxN,
                                                                       double weightA, double weightB,
                                                                       boolean dropAtypicalConcepts) {
        List<ThsConceptRelation> conceptAllRelations = getConceptAllRelations(conceptName, dateStr);
        if (conceptAllRelations == null) {
            return null;
        }


        // 正常逻辑

        // 排除掉非典型概念
        if (dropAtypicalConcepts) {
            conceptAllRelations =
                    conceptAllRelations.stream()
                            .filter(value -> !atypicalConceptNameSet.contains(value.getConceptNameB())).collect(
                            Collectors.toList());
        }
        // 数量不会太多, 能接受, 全部排序, 取前N
        Collections.sort(conceptAllRelations, new Comparator<ThsConceptRelation>() {
            @Override
            public int compare(ThsConceptRelation o1, ThsConceptRelation o2) {
                double v1 = o1.calcRelationValue(weightA, weightB);
                double v2 = o2.calcRelationValue(weightA, weightB);
                if (v2 < v1) { // @noti: 已经逆序!
                    return -1;
                } else if (v2 > v1) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        if (conceptAllRelations.size() <= maxN) {
            return conceptAllRelations;
        }
        return conceptAllRelations.subList(0, Math.min(maxN, conceptAllRelations.size()));
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThsSimpleStock {
        String code;
        String name;


    }

    /**
     * 同花顺 概念之间的 关系
     */
    @Data
    @NoArgsConstructor
    public static class ThsConceptRelation {
        String conceptNameA;
        String conceptNameB;
        Integer sameIncludeStockAmount; // 相同成分股个数
        Double percentA; // 相同成分股个数 / 概念A总数
        Double percentB; // 相同成分股个数 / 概念B总数

        Integer includeStockAmountA;
        Integer includeStockAmountB; // 靠推断计算

        public ThsConceptRelation(String conceptNameA, String conceptNameB, Integer sameIncludeStockAmount,
                                  Double percentA, Double percentB) {
            this.conceptNameA = conceptNameA;
            this.conceptNameB = conceptNameB;
            this.sameIncludeStockAmount = sameIncludeStockAmount;
            this.percentA = percentA;
            this.percentB = percentB;

            this.includeStockAmountA = (int)CommonUtil.roundHalfUP(sameIncludeStockAmount/percentA, 0);
            this.includeStockAmountB = (int)CommonUtil.roundHalfUP(sameIncludeStockAmount/percentB, 0);
        }
        // 同花顺概念指数涨幅排名;主力金额;涨速;指数市值;指数流通市值;指数涨跌家数;指数领涨股;指数阶段涨幅
        // 同花顺概念指数5日阶段涨幅,10日的区间涨跌幅,20日区间涨跌幅

        /**
         * 给定两个权重, 它们将 * percentA 和B,  计算得到 在此2权重之下, 两个概念的 相关性值!
         * 两个权重可以任意值, 只要和不为0; 权重更高的 比低的大即可, 不要求和为1
         *
         * @param weightA
         * @param weightB
         * @return 结果理论上是 0.0 - 1.0
         */
        public double calcRelationValue(double weightA, double weightB) {
            return percentA * weightA + percentB * weightB / (weightA + weightB);
        }

        public double calcRelationValueBiasA() {
            return calcRelationValue(0.6, 0.4); // 更加偏向于 percentA. 即若占比A更多, 则相关性更高
        }

        public double calcRelationValueBiasB() {
            return calcRelationValue(0.4, 0.6); // 更加偏向于 percentB. 即若占比B更多, 则相关性更高
        }

        /**
         * 默认等权重
         *
         * @return
         */
        public double calcRelationValue() {
            return calcRelationValue(0.5, 0.5); // 更加偏向于 percentB. 即若占比B更多, 则相关性更高
        }
    }


}
