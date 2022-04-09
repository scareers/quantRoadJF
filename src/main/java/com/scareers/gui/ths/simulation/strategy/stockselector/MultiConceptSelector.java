package com.scareers.gui.ths.simulation.strategy.stockselector;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;

import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 多 (主线)概念/行业 选股; 问财太慢
 *
 * @author: admin
 * @date: 2022/3/26/026-19:03:44
 */
public class MultiConceptSelector {
    public static void main(String[] args) throws Exception {
//        double factor = typicalVolBsRateFactor("2022-03-25", SecurityBeanEm.createStock("浩洋股份"), 0.5);
//        Console.log(factor);
        ArrayList<String> names = new ArrayList<>(
                Arrays.asList(
                        "乡村振兴", "数字货币", "华为概念", "鸿蒙概念", "养鸡", "房地产开发", "房地产服务", "银行", "中药"
                )
        );

        Console.log(threeAndTwoMainLine(names));


    }

    /**
     * 计算三线和双线加身; key为 2,3; 返回map
     * 双线加身已排除三线加身的股票
     *
     * @param names
     * @return
     * @throws Exception
     */
    public static HashMap<Integer, HashMap<String, ArrayList<String>>> threeAndTwoMainLine(List<String> names) {
        String[] nameArray = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            nameArray[i] = names.get(i);
        }

        HashSet<String> threeLineSet = new HashSet<>();
        HashMap<String, ArrayList<String>> threeLineRes = threeLine(nameArray);
        threeLineRes.values().forEach(threeLineSet::addAll);
        HashMap<String, ArrayList<String>> twoLineRes = twoLine(nameArray);
        twoLineRes.values().forEach(value -> value.removeAll(threeLineSet));

        HashMap<Integer, HashMap<String, ArrayList<String>>> res = new HashMap<>();
        res.put(2, twoLineRes);
        res.put(3, threeLineRes);
        return res;
    }

    private static HashMap<String, ArrayList<String>> twoLine(String[] nameArray) {

        HashMap<String, ArrayList<String>> res = new HashMap<>();

        for (String[] strings : MathUtil.combinationSelect(nameArray, 2)) {
            String nameA = strings[0];
            String nameB = strings[1];

            try {
                Set<String> collect1 = ThsDbApi
                        .getConceptOrIndustryIncludeStocks(nameA, DateUtil.today()).stream()
                        .map(ThsDbApi.ThsSimpleStock::getName).collect(
                                Collectors.toSet());
                Set<String> collect2 = ThsDbApi
                        .getConceptOrIndustryIncludeStocks(nameB, DateUtil.today()).stream()
                        .map(ThsDbApi.ThsSimpleStock::getName).collect(
                                Collectors.toSet());

                Set<String> stocks = CommonUtil.intersectionOfSet(collect1, collect2);
                ArrayList<String> stockList = new ArrayList<>();
                for (String stock : stocks) {
                    if (!stock.contains("ST")) {

                        stockList.add(stock);
                    }
                }

                if (stockList.size() > 0) {

                    res.put(StrUtil.format("{}__{}",nameA,nameB), stockList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;

    }

    private static HashMap<String, ArrayList<String>> threeLine(String[] nameArray) {
        HashMap<String, ArrayList<String>> res = new HashMap<>();
        for (String[] strings : MathUtil.combinationSelect(nameArray, 3)) {
            try {
                String nameA = strings[0];
                String nameB = strings[1];
                String nameC = strings[2];

                Set<String> collect1 = ThsDbApi
                        .getConceptOrIndustryIncludeStocks(nameA, DateUtil.today()).stream()
                        .map(value -> value.getName()).collect(
                                Collectors.toSet());
                Set<String> collect2 = ThsDbApi
                        .getConceptOrIndustryIncludeStocks(nameB, DateUtil.today()).stream()
                        .map(value -> value.getName()).collect(
                                Collectors.toSet());
                Set<String> collect3 = ThsDbApi
                        .getConceptOrIndustryIncludeStocks(nameC, DateUtil.today()).stream()
                        .map(value -> value.getName()).collect(
                                Collectors.toSet());

                Set<String> stocks = CommonUtil.intersectionOfSet(collect1, collect2);
                stocks = CommonUtil.intersectionOfSet(stocks, collect3);
                ArrayList<String> stockList = new ArrayList<>();
                for (String stock : stocks) {
                    if (!stock.contains("ST")) {
                        stockList.add(stock);
                    }
                }
                if (stockList.size() > 0) {

                    res.put(StrUtil.format("{}__{}__{}",nameA,nameB,nameC), stockList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;

    }


}
