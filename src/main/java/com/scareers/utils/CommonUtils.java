package com.scareers.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/6  0006-23:38
 */
public class CommonUtils {
    public static void main(String[] args) {
        Console.log(changeStatRangeForFull(Arrays.asList("20120102", "20130102")));
    }

    /**
     * # -- 获取读取数据的区间, 并非真实统计区间. 他会更大
     * # 给定统计的日期区间, 但是往往访问数据库, 需要的实际数据会超过这个区间,
     * # 本函数返回 新的日期区间, 它包括了给定区间, 并且前后都有 冗余的日期.
     * # python 的实现, 是 读取设置中的 settings.date_ranges, 取前一个设置和后一个设置的start/end作为新区间.
     * # java 实现修改: 将start区间提前1年, end区间延后一年即可. 该方法更加方便.
     * # 两种实现, 都需要注意的点: 末尾区间同样会缺少最后的几次统计.
     *
     * @param statRange : 8位标准的日期形式.
     * @return 为了数据完整性, 前后更宽的statRange
     */
    public static List<String> changeStatRangeForFull(List<String> statRange) {
        //        return Arrays.asList("19000101", "21000101");
        String start = statRange.get(0);
        String end = statRange.get(1);
        int startYear = DateUtil.parse(start).year() - 2;
        int endYear = DateUtil.parse(start).year() + 2;

        String startNew = StrUtil.format("{}{}", startYear, StrUtil.sub(start, 4, 8));
        String endNew = StrUtil.format("{}{}", endYear, StrUtil.sub(end, 4, 8));
        return Arrays.asList(startNew, endNew);
    }

    public static void showMemoryUsageByte() {
        Console.log("{}b - {}b == {}b", Runtime.getRuntime().totalMemory(),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public static void showMemoryUsageMB() {
        Console.log("{}M - {}M == {}M", Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().freeMemory() / 1024 / 1024,
                Runtime.getRuntime().totalMemory() / 1024 / 1024 - Runtime.getRuntime().freeMemory() / 1024 / 1024);
    }

    // 模拟 python 的range. 主要遍历index索引
    public static List<Integer> range(int start, int end, int step) {
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = start; i < end; i += step) {
            res.add(i);
        }
        return res;
    }

    public static List<Integer> range(int start, int end) {
        return range(start, end, 1);
    }

    public static List<Integer> range(int end) {
        return range(0, end);
    }

    public static double roundHalfUP(double value, int scale) {// 正宗四舍五入
        return NumberUtil.round(value, scale).doubleValue();
        //        return new BigDecimal(value).setScale(scale,  // 底层实现同
        //                BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static HashSet<String> intersectionOfSet(HashSet<String> set1, HashSet<String> set2) {
        // 交集
        HashSet<String> res = new HashSet<>();
        res.addAll(set1);
        res.retainAll(set2);
        return res;
    }

    public static HashSet<String> intersectionOfList(List<String> list1, List<String> list2) {
        // 交集
        HashSet<String> set1 = new HashSet<>(list1);
        HashSet<String> set2 = new HashSet<>(list2);
        return intersectionOfSet(set1, set2);
    }

    public static HashSet<Object> subtractionOfSet(HashSet<Object> set1, HashSet<Object> set2) {
        // 差集
        HashSet<Object> res = new HashSet<>();
        res.addAll(set1);
        res.removeAll(set2);
        return res;
    }

    public static HashSet<Object> aggregateOfSet(HashSet<Object> set1, HashSet<Object> set2) {
        // 并集
        HashSet<Object> res = new HashSet<>();
        res.addAll(set1);
        res.addAll(set2);
        return res;
    }

    public static Double minOfListDouble(List<Double> doubles) {
        if (doubles.size() == 0) {
            return null;
        }
        Double res = Double.MAX_VALUE;
        for (int i = 0; i < doubles.size(); i++) {
            Double ele = doubles.get(i);
            if (ele == null) { // 有null时能够正确返回.
                continue;
            }
            if (ele < res) {
                res = ele;
            }
        }
        if (res.equals(Double.MAX_VALUE)) {
            return null; // 全部时null时, 可能发生.
        }
        return res;
    }

    /**
     * 可null, 如果用
     *
     * @param numbers
     * @return
     */
    public static Double minOfListNumber(List<Number> numbers) {
        if (numbers.size() == 0) {
            return null;
        }
        Double res = Double.MAX_VALUE;
        for (int i = 0; i < numbers.size(); i++) {
            Number ele = numbers.get(i);
            if (ele == null) { // 有null时能够正确返回.
                continue;
            }
            if (ele.doubleValue() < res) {
                res = ele.doubleValue();
            }
        }
        if (res.equals(Double.MAX_VALUE)) {
            return null; // 全部时null时, 可能发生.
        }
        return res;
    }

    public static Double sumOfListNumber(List<Double> numbers) {
        return numbers.stream().
                mapToDouble(Number::doubleValue).sum();
    }

    public static Double maxOfListDouble(List<Double> doubles) {
        if (doubles.size() == 0) {
            return null;
        }
        Double res = Double.MIN_VALUE;
        for (int i = 0; i < doubles.size(); i++) {
            Double ele = doubles.get(i);
            if (ele == null) { // 有null时能够正确返回.
                continue;
            }
            if (ele > res) {
                res = ele;
            }
        }
        if (res.equals(Double.MIN_VALUE)) {
            return null; // 全部时null时, 可能发生.
        }
        return res;
    }

}
