package com.scareers.tools.stockplan.stock.bean.selector;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.util.*;

/**
 * description: 多 (主线)概念/行业 选股; 问财遍历太慢, 直接读取数据库
 *
 * @author: admin
 * @date: 2022/3/26/026-19:03:44
 */
public class MultiConceptSelector extends StockSelector {


    public static void main(String[] args) throws Exception {
        HashMap<String, String> industryOrConceptWithLineTypeMap = new HashMap<>();
        industryOrConceptWithLineTypeMap.put("种植业与林业", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);
        industryOrConceptWithLineTypeMap.put("农业种植", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);
        industryOrConceptWithLineTypeMap.put("医药电商", IndustryConceptThsOfPlan.LineType.MAIN_LINE_3);
        industryOrConceptWithLineTypeMap.put("猪肉", IndustryConceptThsOfPlan.LineType.BRANCH_LINE_1);
        industryOrConceptWithLineTypeMap.put("房地产开发", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);
        industryOrConceptWithLineTypeMap.put("物流", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);
        industryOrConceptWithLineTypeMap.put("冷链物流", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);
        industryOrConceptWithLineTypeMap.put("中药", IndustryConceptThsOfPlan.LineType.MAIN_LINE_1);


        MultiConceptSelector multiConceptSelector = new MultiConceptSelector(industryOrConceptWithLineTypeMap);
        multiConceptSelector.stockSelect();
//        multiConceptSelector.showAllSelectRes();
        Console.log(multiConceptSelector.resToJson());


    }

    public static HashMap<String, Double> lineScoreMap;

    static {
        initLineScoreMap();
    }

    /**
     * 定义 主线1-3, 支线1-3, 的分数; 个股的 选股分数, 等于 它们所属主线 分数相加; 算法简单
     */
    private static void initLineScoreMap() {
        lineScoreMap = new HashMap<>();
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.MAIN_LINE_1, 30.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.MAIN_LINE_2, 20.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.MAIN_LINE_3, 15.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.BRANCH_LINE_1, 10.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.BRANCH_LINE_2, 8.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.BRANCH_LINE_3, 5.0);
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.SPECIAL_LINE, 12.0); // 特殊线, 分数 12, 介于主线支线之间
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.CARE_LINE, 25.0); // 关心线, 介于主线1和2
        lineScoreMap.put(IndustryConceptThsOfPlan.LineType.OTHER_LINE, 1.0); // 其他线; 1
    }


    HashMap<String, String> industryOrConceptWithLineTypeMap;
    HashMap<String, JSONObject> res;

    public MultiConceptSelector(HashMap<String, String> industryOrConceptWithLineTypeMap) {
        this.name = "多线加身选股";
        this.description = "给定多个 主线支线 行业/概念, 计算3元素组合, 2元素组合, 交集个股";
        this.scoreRule = "构造器需要给定概念行业列表和对应主线支线Map; 不同主线支线, 对应不同分数; 将这些分数简单相加得到score;[0,100]";
        this.industryOrConceptWithLineTypeMap = industryOrConceptWithLineTypeMap;
    }

    @Override
    public void showAllSelectRes() {
        if (res != null) {
            for (String s : res.keySet()) {
                Console.log("{}: {}", s, res.get(s).getDouble("score"));
            }
        }
    }

    @Override
    public void stockSelect() {
        this.res = parseMultiLine();
    }


    @Override
    public JSONObject getSelectResultOf(String code) {
        if (res != null) {
            log.warn("尚未选股, 请先调用 stockSelect()");
            return res.get(code);
        }
        return null;
    }

    public String resToJson() {
        if (res == null) {
            return "";
        }
        // 按照分数排序!
        ArrayList<ArrayList<Object>> arrayList = new ArrayList<>();
        for (String s : res.keySet()) {
            ArrayList<Object> li = new ArrayList<>();
            li.add(s);
            li.add(res.get(s));
            arrayList.add(li);
        }
        arrayList.sort(new Comparator<ArrayList<Object>>() {
            @Override
            public int compare(ArrayList<Object> o1, ArrayList<Object> o2) {
                JSONObject jsonObject1 = (JSONObject) o1.get(1);
                JSONObject jsonObject2 = (JSONObject) o2.get(1);

                Double score1 = jsonObject1.getDouble("score");
                Double score2 = jsonObject2.getDouble("score");
                if (score1 == null || score2 == null) {
                    return 0;
                }

                if (score1 < score2) {
                    return 1;
                } else if (score1 > score2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });


        return JSONUtilS.toJsonPrettyStr(arrayList);
    }

    /**
     * 计算多线加身! 遍历所有概念的所有个股! 计算器所属的概念数量, 和主线类型
     * // 不使用 组合 方法
     *
     * @return
     * @throws Exception
     */
    public HashMap<String, JSONObject> parseMultiLine() {
        ArrayList<String> concepts = new ArrayList<>(industryOrConceptWithLineTypeMap.keySet());
        // 1. 构建所有概念行业的 成分股map, 方便访问
        HashMap<String, List<ThsDbApi.ThsSimpleStock>> includeStockMap = new HashMap<>();
        for (String concept : concepts) {

            try {
                includeStockMap.put(concept, ThsDbApi.getConceptOrIndustryIncludeStocks(concept, DateUtil.today()));
            } catch (Exception e) {
                Console.log(concept);
                e.printStackTrace();
            }
        }
        // 1.2. 集合化, 方便contain 计算
        HashMap<String, HashSet<ThsDbApi.ThsSimpleStock>> includeStockSetMap = new HashMap<>();
        includeStockMap.keySet().forEach(s -> includeStockSetMap.put(s, new HashSet<>(includeStockMap.get(s))));

        // 2. 求得所有成分股的集合, 可能数量极多
        HashSet<ThsDbApi.ThsSimpleStock> allStockSet = new HashSet<>();
        for (String s : includeStockMap.keySet()) {
            allStockSet.addAll(includeStockMap.get(s));
        }

        // 3. 双层循环, 遍历所有个股,遍历所有概念成分股, 判定是否为成分股, 记录 概念加身+1
        HashMap<ThsDbApi.ThsSimpleStock, List<String>> belongToConceptMap = new HashMap<>();
        for (ThsDbApi.ThsSimpleStock stock : allStockSet) {
            belongToConceptMap.putIfAbsent(stock, new ArrayList<>());
            for (String concept : includeStockMap.keySet()) {
                if (includeStockSetMap.get(concept).contains(stock)) {
                    belongToConceptMap.get(stock).add(concept);
                }
            }
        }

        // 4.0. 用问财筛选一下近期曾涨停过的股票, 最终结果仅保留涨停池中的
        HashSet<String> priceLimitCodePool = null;
        try {
            DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("近60日的涨停次数>=1次;非st;非科创板;非创业板");
            priceLimitCodePool = new HashSet<>(DataFrameS.getColAsStringList(dataFrame, "code"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4.结果
        HashMap<String, JSONObject> res = new HashMap<>();
        for (ThsDbApi.ThsSimpleStock thsSimpleStock : belongToConceptMap.keySet()) {
            List<String> conceptList = belongToConceptMap.get(thsSimpleStock);
            if (conceptList.size() <= 1) { // 只关心多主线的
                continue;
            }

            if (priceLimitCodePool != null && !priceLimitCodePool.contains(thsSimpleStock.getCode())) {
                continue;
            }

            // 对单只股票 结果 json进行构造

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("industryConceptList", conceptList);
            Double score = 0.0;
            for (String s : conceptList) {
                Double singleDouble = lineScoreMap.get(industryOrConceptWithLineTypeMap.get(s));
                if (singleDouble != null) {
                    score += singleDouble;
                }
            }
            jsonObject.put("name", thsSimpleStock.getName());
            jsonObject.put("score", Math.max(0.0, Math.min(score, 100.0))); // 取值0-100
            jsonObject.put("industryConceptAmount", conceptList.size());
            res.put(thsSimpleStock.getCode(), jsonObject);
        }
        return res;
    }


}
