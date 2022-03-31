package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jfree.chart.util.HMSNumberFormat;

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
//        Console.log(getIndustryByNameAndDate("电力", "2022-03-28"));
//        Console.log(getIndustryAllRecordByName("电力"));
        Console.log(getConceptByNameAndDate("三胎概念", "2022-03-28"));
        Console.log(getConceptAllRecordByName("三胎概念"));

//        Console.log(getIndustryNameLevel23WithFullCodeMap(DateUtil.today()));
//        Console.log(getStockBelongToIndustry23WithName(DateUtil.today()));
//        List<ThsSimpleStock> includeList = getConceptOrIndustryIncludeStocks("新冠检测", DateUtil.today());
//        Console.log(includeList);
//        Console.log(includeList.size());


//        List<ThsConceptIndustryRelation> relations = getAllRelationsByName("融资融券", DateUtil.today());
//        Console.log(relations);
//        Console.log(relations.size());

        //        for (String s : conceptNameList) {
//            Console.log(s);
//        }

//        List<ThsSimpleStock> conceptIncludeStocks = getConceptIncludeStocks(RandomUtil.randomEle(conceptNameList),
//                DateUtil.today());
//        Console.log(conceptIncludeStocks.get(0));
//        Console.log(conceptIncludeStocks);


        List<ThsConceptIndustryRelation> maxConceptRelationshipOf = getMaxRelationshipOfConcept(
//                RandomUtil.randomEle(conceptNameList),
                "俄乌冲突概念",
                DateUtil.today(), 10, 0.6, 0.4, true);
        for (ThsConceptIndustryRelation thsConceptRelation : maxConceptRelationshipOf) {
            Console.log(thsConceptRelation);
        }
    }

    /**
     * 医疗器械概念
     *
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
     * 所有行业列表, 映射代码
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getIndustryNameLevel23WithFullCodeMap(String dateStr) {
        String sql = StrUtil.format("select indexCode,name from industry_list where dateStr='{}'",
                dateStr);
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
     * 二级行业列表, 映射代码
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getIndustryNameLevel2WithFullCodeMap(String dateStr) {
        String sql = StrUtil.format("select indexCode,name from industry_list where dateStr='{}' and " +
                        "industryType='二级行业'",
                dateStr);
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
     * 二级行业列表, 映射代码
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getIndustryNameLevel3WithFullCodeMap(String dateStr) {
        String sql = StrUtil.format("select indexCode,name from industry_list where dateStr='{}' and " +
                        "industryType='三级行业'",
                dateStr);
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
     * 获取行业单条记录. 需要提供行业名称 和 日期字符串
     *
     * @param dateStr
     * @return
     * @cols [id, chgP, close, code, industryIndex, industryType, name, marketCode, indexCode, dateStr]
     */
    public static DataFrame<Object> getIndustryByNameAndDate(String industryName, String dateStr) {
        String sql = StrUtil.format("select * from industry_list where dateStr='{}' and " +
                        "name='{}'",
                dateStr, industryName);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }

    /**
     * 获取行业所有日期的记录. 需要提供行业名称, 返回所有日期的 industry_list 中对应行业的记录
     *
     * @param dateStr
     * @return
     * @cols [id, chgP, close, code, industryIndex, industryType, name, marketCode, indexCode, dateStr]
     */
    public static DataFrame<Object> getIndustryAllRecordByName(String industryName) {
        String sql = StrUtil.format("select * from industry_list where " +
                        "name='{}'",
                industryName);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }

    /**
     * 获取概念单条记录. 需要提供 概念名称 和 日期字符串
     *
     * @param dateStr
     * @return
     * @cols [id, chgP, close, code, name, marketCode, indexCode, conceptIndex, dateStr]
     */
    public static DataFrame<Object> getConceptByNameAndDate(String conceptName, String dateStr) {
        String sql = StrUtil.format("select * from concept_list where dateStr='{}' and " +
                        "name='{}'",
                dateStr, conceptName);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }

    /**
     * 获取行业所有日期的记录. 需要提供行业名称, 返回所有日期的 industry_list 中对应行业的记录
     *
     * @param dateStr
     * @return
     * @cols [id, chgP, close, code, name, marketCode, indexCode, conceptIndex, dateStr]
     */
    public static DataFrame<Object> getConceptAllRecordByName(String conceptName) {
        String sql = StrUtil.format("select * from concept_list where " +
                        "name='{}'",
                conceptName);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }

    /**
     * 给定日期, 返回所有 股票名称,代码, 二级行业和三级行业
     *
     * @param dateStr
     * @return
     * @cols code      name	  belongToConceptAll
     */
    public static DataFrame<Object> getStockBelongToIndustry23WithName(String dateStr) {
        String sql = StrUtil.format("select code,name,industryLevel2,industryLevel3 from " +
                "stock_belong_to_industry_and_concept " +
                "where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }


    /**
     * 给定日期, 返回所有 股票名称,代码, 所属 二级行业,三级行业
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
    行业概念成分个股及关系对象
     */

    /**
     * 获取指定日期, 行业或者概念成分股:  json 字符串 --> 行业包含二级和3级行业
     * 实测 二级行业/三级行业/概念 名称均正常等价于问财结果
     *
     * @param conceptOrIndustryName
     * @param dateStr
     * @return
     */
    public static List<ThsSimpleStock> getConceptOrIndustryIncludeStocks(String conceptOrIndustryName, String dateStr) {
        String sql = StrUtil
                .format("select includeStocks from concept_industry_relation_parse where cptOrIndusName='{}' and " +
                        "dateStr='{}'", conceptOrIndustryName, dateStr);
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
     * 获取 行业或概念 所有与其它 概念或行业 的关系列表, 解析json后, 构建 ThsConceptIndustryRelation 列表对象
     * 注意: 关系列表, 包含了所有的 行业 和 概念; 想要单独查找 与之相关的 行业 或 概念 , 需要自行区分
     *
     * @param conceptOrIndustryName
     * @param dateStr
     * @return
     */
    public static List<ThsConceptIndustryRelation> getAllRelationsByName(String conceptOrIndustryName,
                                                                         String dateStr) {
        String sql = StrUtil
                .format("select relationMap from concept_industry_relation_parse where cptOrIndusName='{}' and " +
                        "dateStr='{}'", conceptOrIndustryName, dateStr);
        DataFrame<Object> dataFrame = null;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        if (dataFrame.length() == 0) {
            return null;
        }

        List<ThsConceptIndustryRelation> res = new ArrayList<>();
        JSONObject relationMap = JSONUtilS.parseObj(dataFrame.get(0, 0).toString());
        for (Object nameBRaw : relationMap.keySet()) {
            String nameB = nameBRaw.toString();
            JSONArray jsonArray = relationMap.getJSONArray(nameB);
            res.add(new ThsConceptIndustryRelation(
                    conceptOrIndustryName,
                    nameB,
                    jsonArray.getInteger(0),
                    jsonArray.getDouble(1),
                    jsonArray.getDouble(2)
            ));
        }
        return res;
    }

    /**
     * 给定概念, 返回与之最相关的 前 N个概念 或者行业 , 关系对象列表.
     * 可指定是否排除掉非典型概念!!
     */
    public static List<ThsConceptIndustryRelation> getMaxRelationshipOf(String conceptOrIndustryName, String dateStr,
                                                                        int maxN,
                                                                        double weightA, double weightB,
                                                                        boolean dropAtypicalConcepts) {
        List<ThsConceptIndustryRelation> conceptAllRelations = getAllRelationsByName(conceptOrIndustryName, dateStr);

        if (conceptAllRelations == null) {
            return null;
        }


        // 正常逻辑

        // 排除掉非典型概念
        if (dropAtypicalConcepts) {
            conceptAllRelations =
                    conceptAllRelations.stream()
                            .filter(value -> !atypicalConceptNameSet.contains(value.getNameB())).collect(
                            Collectors.toList());
        }
        // 数量不会太多, 能接受, 全部排序, 取前N
        Collections.sort(conceptAllRelations, new Comparator<ThsConceptIndustryRelation>() {
            @Override
            public int compare(ThsConceptIndustryRelation o1, ThsConceptIndustryRelation o2) {
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

    /**
     * 只考虑与 概念 的关系
     *
     * @param conceptOrIndustryName
     * @param dateStr
     * @param maxN
     * @param weightA
     * @param weightB
     * @param dropAtypicalConcepts
     * @return
     */
    public static List<ThsConceptIndustryRelation> getMaxRelationshipOfConcept(String conceptOrIndustryName,
                                                                               String dateStr,
                                                                               int maxN,
                                                                               double weightA, double weightB,
                                                                               boolean dropAtypicalConcepts) {
        List<ThsConceptIndustryRelation> maxRelationshipOfAll = getMaxRelationshipOf(conceptOrIndustryName, dateStr,
                Integer.MAX_VALUE, weightA, weightB,
                dropAtypicalConcepts); // 获取所有关系
        // 筛选出 B 为概念的关系对象; 因为已经排序, 不必再次排序
        List<ThsConceptIndustryRelation> collect = maxRelationshipOfAll.stream()
                .filter(ThsConceptIndustryRelation::bIsConcept)
                .collect(Collectors.toList());

        return collect.subList(0, Math.min(maxN, collect.size()));
    }

    /**
     * 只考虑与 二级行业的关系
     *
     * @param conceptOrIndustryName
     * @param dateStr
     * @param maxN
     * @param weightA
     * @param weightB
     * @param dropAtypicalConcepts
     * @return
     */
    public static List<ThsConceptIndustryRelation> getMaxRelationshipOfIndustryLevel2(String conceptOrIndustryName,
                                                                                      String dateStr,
                                                                                      int maxN,
                                                                                      double weightA, double weightB,
                                                                                      boolean dropAtypicalConcepts) {
        List<ThsConceptIndustryRelation> maxRelationshipOfAll = getMaxRelationshipOf(conceptOrIndustryName, dateStr,
                Integer.MAX_VALUE, weightA, weightB,
                dropAtypicalConcepts); // 获取所有关系
        // 筛选出 B 为概念的关系对象; 因为已经排序, 不必再次排序
        List<ThsConceptIndustryRelation> collect = maxRelationshipOfAll.stream()
                .filter(ThsConceptIndustryRelation::bIsIndustryLevel2)
                .collect(Collectors.toList());

        return collect.subList(0, Math.min(maxN, collect.size()));
    }

    /**
     * 只考虑与 3级行业的关系
     *
     * @param conceptOrIndustryName
     * @param dateStr
     * @param maxN
     * @param weightA
     * @param weightB
     * @param dropAtypicalConcepts
     * @return
     */
    public static List<ThsConceptIndustryRelation> getMaxRelationshipOfIndustryLevel3(String conceptOrIndustryName,
                                                                                      String dateStr,
                                                                                      int maxN,
                                                                                      double weightA, double weightB,
                                                                                      boolean dropAtypicalConcepts) {
        List<ThsConceptIndustryRelation> maxRelationshipOfAll = getMaxRelationshipOf(conceptOrIndustryName, dateStr,
                Integer.MAX_VALUE, weightA, weightB,
                dropAtypicalConcepts); // 获取所有关系
        // 筛选出 B 为概念的关系对象; 因为已经排序, 不必再次排序
        List<ThsConceptIndustryRelation> collect = maxRelationshipOfAll.stream()
                .filter(ThsConceptIndustryRelation::bIsIndustryLevel3)
                .collect(Collectors.toList());

        return collect.subList(0, Math.min(maxN, collect.size()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThsSimpleStock {
        String code;
        String name;

        public JSONObject toJsonObject() {
            JSONObject res = new JSONObject();
            res.put("code", code);
            res.put("name", name);
            return res;
        }

        /**
         * 从JSONObject加载数据构建对象
         */
        public static ThsSimpleStock createFromJsonObject(JSONObject rawJson) {
            ThsSimpleStock res = new ThsSimpleStock();
            res.setCode(rawJson.getString("code"));
            res.setName(rawJson.getString("name"));
            return res;
        }
    }


    /**
     * 同花顺 概念之间的 关系
     */
    @Data
    @NoArgsConstructor
    public static class ThsConceptIndustryRelation {
        String nameA;
        String nameB;
        Integer sameIncludeStockAmount; // 相同成分股个数
        Double percentA; // 相同成分股个数 / 概念A总数
        Double percentB; // 相同成分股个数 / 概念B总数

        Integer includeStockAmountA;
        Integer includeStockAmountB; // 靠推断计算

        // 类型靠推断! 读取静态属性的行业名称集合 和 概念名称集合
        Integer aType = 0; // a是行业还是概念?  1概念,2 二级行业, 3 三级行业  0 未知
        Integer bType = 0; // b是行业还是概念? 1概念, 2 二级行业,3 三级行业 0 未知


        public static HashSet<String> allIndustryLevel2NameSet;
        public static HashSet<String> allIndustryLevel3NameSet;
        public static HashSet<String> allConceptNameSet;


        public ThsConceptIndustryRelation(String nameA, String nameB, Integer sameIncludeStockAmount,
                                          Double percentA, Double percentB) {
            this.nameA = nameA;
            this.nameB = nameB;
            this.sameIncludeStockAmount = sameIncludeStockAmount;
            this.percentA = percentA;
            this.percentB = percentB;

            this.includeStockAmountA = (int) CommonUtil.roundHalfUP(sameIncludeStockAmount / percentA, 0);
            this.includeStockAmountB = (int) CommonUtil.roundHalfUP(sameIncludeStockAmount / percentB, 0);
            checkConceptNameSet();
            checkIndustryLevel2NameSet();
            checkIndustryLevel3NameSet();


            // 推断两指数各自是行业还是概念
            if (allIndustryLevel2NameSet.contains(this.nameA)) {
                this.aType = 2;
            } else if (allIndustryLevel3NameSet.contains(this.nameA)) {
                this.aType = 3;
            } else if (allConceptNameSet.contains(this.nameA)) {
                this.aType = 1;
            }

            if (allIndustryLevel2NameSet.contains(this.nameB)) {
                this.bType = 2;
            } else if (allIndustryLevel3NameSet.contains(this.nameB)) {
                this.bType = 3;
            } else if (allConceptNameSet.contains(this.nameB)) {
                this.bType = 1;
            }
        }

        @SneakyThrows
        public static void checkConceptNameSet() {
            if (allConceptNameSet == null) {
                HashMap<String, String> conceptNameWithFullCodeMap = getConceptNameWithFullCodeMap(DateUtil.today());
                if (conceptNameWithFullCodeMap == null) { // 若失败, 则获取最后一次爬虫记录的 最大日期
                    String sql = "select max(dateStr) from concept_list";
                    DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
                    conceptNameWithFullCodeMap = getConceptNameWithFullCodeMap(dataFrame.get(0, 0).toString());
                }
                allConceptNameSet = new HashSet<>(conceptNameWithFullCodeMap.keySet());
            }
        }

        @SneakyThrows
        public static void checkIndustryLevel2NameSet() {
            if (allIndustryLevel2NameSet == null) {
                HashMap<String, String> industryNameWithFullCodeMap = getIndustryNameLevel2WithFullCodeMap(
                        DateUtil.today());
                if (industryNameWithFullCodeMap == null) { // 若失败, 则获取最后一次爬虫记录的 最大日期
                    String sql = "select max(dateStr) from industry_list";
                    DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
                    industryNameWithFullCodeMap = getIndustryNameLevel2WithFullCodeMap(dataFrame.get(0, 0).toString());
                }
                allIndustryLevel2NameSet = new HashSet<>(industryNameWithFullCodeMap.keySet());
            }
        }

        @SneakyThrows
        public static void checkIndustryLevel3NameSet() {
            if (allIndustryLevel3NameSet == null) {
                HashMap<String, String> industryNameWithFullCodeMap = getIndustryNameLevel3WithFullCodeMap(
                        DateUtil.today());
                if (industryNameWithFullCodeMap == null) { // 若失败, 则获取最后一次爬虫记录的 最大日期
                    String sql = "select max(dateStr) from industry_list";
                    DataFrame<Object> dataFrame = DataFrame.readSql(connection, sql);
                    industryNameWithFullCodeMap = getIndustryNameLevel3WithFullCodeMap(dataFrame.get(0, 0).toString());
                }
                allIndustryLevel3NameSet = new HashSet<>(industryNameWithFullCodeMap.keySet());
            }
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

        public boolean aIsIndustryLevel2() { // a是二级行业/三级行业/概念/行业/未知; b类似
            return this.aType == 2;
        }

        public boolean aIsIndustryLevel3() {
            return this.aType == 3;
        }

        public boolean aIsConcept() {
            return this.aType == 1;
        }

        public boolean aTypeUnknown() {
            return this.aType == 0;
        }

        public boolean aIsIndustry() {
            return aIsIndustryLevel2() || aIsIndustryLevel3();
        }

        public boolean bIsIndustryLevel2() {
            return this.bType == 2;
        }

        public boolean bIsIndustryLevel3() {
            return this.bType == 3;
        }

        public boolean bIsConcept() {
            return this.bType == 1;
        }

        public boolean bIsIndustry() {
            return bIsIndustryLevel2() || bIsIndustryLevel3();
        }

        public boolean bTypeUnknown() {
            return this.bType == 0;
        }

        /**
         * 转换为JSONObject; 即 属性名:属性值, 方便json序列化
         *
         * @return
         */
        public JSONObject toJsonObject() { // 类型匹配 JSONObject
            JSONObject res = new JSONObject();
            res.put("nameA", nameA);
            res.put("nameB", nameB);
            res.put("sameIncludeStockAmount", sameIncludeStockAmount);
            res.put("percentA", percentA);
            res.put("percentB", percentB);
            res.put("includeStockAmountA", includeStockAmountA);
            res.put("includeStockAmountB", includeStockAmountB);
            res.put("aType", aType);
            res.put("bType", bType);
            return res;
        }

        /**
         * 从JSONObject加载数据构建对象
         */
        public static ThsConceptIndustryRelation createFromJsonObject(JSONObject rawJson) {
            ThsConceptIndustryRelation res = new ThsConceptIndustryRelation();
            res.setNameA(rawJson.getString("nameA"));
            res.setNameB(rawJson.getString("nameB"));
            res.setSameIncludeStockAmount(rawJson.getInteger("sameIncludeStockAmount"));
            res.setPercentA(rawJson.getDouble("percentA"));
            res.setPercentB(rawJson.getDouble("percentB"));
            res.setIncludeStockAmountA(rawJson.getInteger("includeStockAmountA"));
            res.setIncludeStockAmountB(rawJson.getInteger("includeStockAmountB"));
            res.setAType(rawJson.getInteger("aType"));
            res.setBType(rawJson.getInteger("bType"));
            return res;
        }

    }


}
