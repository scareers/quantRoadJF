package com.scareers.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import com.scareers.settings.SettingsCommon;
import com.scareers.utils.log.LogUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/6  0006-23:38
 */
public class CommonUtil {
    public static void main(String[] args) throws TimeoutException, InterruptedException {
//        Console.log(countTrueOfListBooleans(Arrays.asList(true, false, true)));
//        Console.log(Collections.frequency(Arrays.asList(true, false, true), true));
//        waitEnter();
//        waitForever();

//        openUrlWithDefaultBrowser("https://www.baidu.com");

        Console.log(stdOfListNumberUseLoop(Arrays.asList(1, 2, 3)));
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

    public static Set<String> intersectionOfSet(Set<String> set1, Set<String> set2) {
        // 交集
        HashSet<String> res = new HashSet<>();
        res.addAll(set1);
        res.retainAll(set2);
        return res;
    }

    /**
     * 要求 set1 长度短一点, 更快
     *
     * @param set1
     * @param set2
     * @return
     */
    public static boolean isIntersectOfSetUseStream(List<String> set1, HashSet<String> set2) {
        // 流更慢
        return set1.stream().anyMatch(s -> set2.contains(s));
    }

    public static boolean isIntersectOfSet(List<String> set1, HashSet<String> set2) {
        // 是否交集,
        for (String key : set1) {
            if (set2.contains(key)) {
                return true;
            }
        }
        return false;
    }


    public static HashSet<String> intersectionOfList(List<String> list1, List<String> list2) {
        // 交集
        HashSet<String> set1 = new HashSet<>(list1);
        HashSet<String> set2 = new HashSet<>(list2);
        return intersectionOfSet(set1, set2);
    }

    public static HashSet<String> subtractionOfSet(HashSet<String> set1, HashSet<String> set2) {
        // 差集
        HashSet<String> res = new HashSet<>(set1);
        res.removeAll(set2);
        return res;
    }

    public static HashSet<String> subtractionOfList(List<String> list1, List<String> list2) {
        // 差集
        HashSet<String> set1 = new HashSet<>(list1);
        HashSet<String> set2 = new HashSet<>(list2);
        return subtractionOfSet(set1, set2);
    }

    public static HashSet<Object> aggregateOfSet(HashSet<Object> set1, HashSet<Object> set2) {
        // 并集
        HashSet<Object> res = new HashSet<>();
        res.addAll(set1);
        res.addAll(set2);
        return res;
    }

    public static Double minOfListDouble(List<Double> doubles) {
        return doubles.stream().filter(Objects::nonNull).mapToDouble(x -> x).min().getAsDouble();
    }


    public static Double sumOfListNumber(Collection<? extends Number> numbers) {
        return numbers.stream().
                mapToDouble(Number::doubleValue).sum();
    }

    public static double sumOfListNumberUseLoop(Collection<? extends Number> numbers) {
        double sum = 0.0;
        for (Number number : numbers) {
            sum += number.doubleValue();
        }
        return sum;
    }

    public static double avgOfListNumberUseLoop(Collection<? extends Number> numbers) {
        return sumOfListNumberUseLoop(numbers) / numbers.size();
    }

    public static double varOfListNumberUseLoop(Collection<? extends Number> numbers) {
        double avg = avgOfListNumberUseLoop(numbers);
        return varOfListNumberUseLoop(numbers, avg);
    }

    /**
     * 可给定平均值计算方差
     *
     * @param numbers
     * @param avg
     * @return
     */
    public static double varOfListNumberUseLoop(Collection<? extends Number> numbers, double avg) {
        double expSum = 0;
        for (Number number : numbers) {
            expSum += Math.pow(number.doubleValue() - avg, 2);
        }
        return expSum / numbers.size();
    }

    public static double stdOfListNumberUseLoop(Collection<? extends Number> numbers) {
        return Math.sqrt(varOfListNumberUseLoop(numbers));
    }

    public static double stdOfListNumberUseLoop(Collection<? extends Number> numbers, double avg) {
        return Math.sqrt(varOfListNumberUseLoop(numbers, avg));
    }

    public static Double maxOfListDouble(List<Double> doubles) {
        return doubles.stream().filter(Objects::nonNull).mapToDouble(x -> x).max().getAsDouble();
    }

    public static int countTrueOfListBooleans(List<Boolean> bools) {
        return Collections.frequency(bools, true);
    }

    // 字典的值非0.0的
    public static int countNonZeroValueOfMap(Map<? extends Object, ? extends Number> map) {
        int res = 0;
        for (Object o : map.keySet()) {
            if (map.get(o).doubleValue() != 0.0) {
                res += 1;
            }
        }
        return res;
    }

    public static List<Double> cumSumOfListDouble(List<Double> doubles) {
        ArrayList<Double> objects = new ArrayList<>();
        double sum = 0.0;
        for (Double aDouble : doubles) {
            sum += aDouble;
            objects.add(sum);
        }
        return objects;
    }

    /**
     * 等待 某函数返回true
     *
     * @param booleanSupplier 等待某方法返回 true
     * @param timeout         超时ms
     * @param interval        等待死循环间隔
     * @param description     可log提示信息
     * @param showWaitTime    log总计等待时间
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public static void waitUtil(BooleanSupplier booleanSupplier, int timeout, int interval, String description,
                                boolean showWaitTime)
            throws TimeoutException, InterruptedException {
        if (description != null) {
            LogUtil.log.warn("wait util: {}", description);
        }
        TimeInterval timer = DateUtil.timer();
        timer.start();
        long start = System.currentTimeMillis();
        while (!booleanSupplier.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeout) {
                throw new TimeoutException("等待超时");
            }
            Thread.sleep(interval);
        }
        if (showWaitTime) {
            LogUtil.log.warn("wait util time consume: {}s", timer.interval() / 1000.0);
        }
        if (description != null) {
            LogUtil.log.warn("wait finish: {} finish!", description);
        }
    }

    public static void waitUtil(BooleanSupplier booleanSupplier, int timeout, int interval, String description)
            throws TimeoutException, InterruptedException {
        waitUtil(booleanSupplier, timeout, interval, description, true);
    }

    public static void waitForever() {
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void waitEnter() {
        // 按下确定退出
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }


    /**
     * 判定 double 列表, 之和 约等于 某个值
     *
     * @param doubles
     * @param equalTo   约等于多少
     * @param deviation 允许误差, abs
     * @return
     */
    public static boolean sumEqualApproximately(Collection<Double> doubles, double equalTo, double deviation) {
        return Math.abs(
                doubles.stream().mapToDouble(value -> value).sum() - equalTo) < deviation;
    }

    public static void sendEmailSimple(String subject, String content, boolean async) {
        if (!async) {
            MailUtil.send(SettingsCommon.receivers,
                    subject,
                    content,
                    false, null);
        } else {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    MailUtil.send(SettingsCommon.receivers,
                            subject,
                            content,
                            false, null);
                }
            });
        }
    }

    /**
     * 默认浏览器打开某url
     *
     * @param args
     */
    public static void openUrlWithDefaultBrowser(String url) {
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                java.awt.Desktop dp = java.awt.Desktop.getDesktop();
                if (dp.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    dp.browse(uri);
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 返回某对象 字符串形式, 若为null , 则返回 "null".    常用于gui显示空对象
     *
     * @param o
     * @return
     */
    public static String toStringCheckNull(Object o) {
        return toStringCheckNull(o, "");
    }

    public static String toStringCheckNull(Object o, String defaultStr) {
        if (o == null) {
            return defaultStr;
        }
        return o.toString();
    }

    public static String toStringOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    /**
     * 将数字, 转换为 "1.23亿" 文字
     *
     * @param value
     * @return
     */
    public static String formatNumberWithYi(Number value) {
        if (value == null) {
            return "null";
        }
        return roundHalfUP(value.doubleValue() / 100000000, 2) + "亿";
    }

    /**
     * 将数字, 转换为 "1.23万" 文字
     *
     * @param value
     * @return
     */
    public static String formatNumberWithWan(Number value) {
        if (value == null) {
            return "null";
        }
        return roundHalfUP(value.doubleValue() / 10000, 2) + "万";
    }

    /**
     * 可以对原值 / 一个数,
     *
     * @param value
     * @param divide
     * @return
     */
    public static String formatNumberWithSuitable(Number value, double divide) {
        if (value == null) {
            return "null";
        }
        value = value.doubleValue() / divide;
        if (Math.abs(value.doubleValue()) < 10000) {
            return Double.valueOf(roundHalfUP(value.doubleValue(), 2)).toString();
        } else if (Math.abs(value.doubleValue()) < 100000000) {
            return formatNumberWithWan(value);
        } else {
            return formatNumberWithYi(value);
        }

    }

    public static String formatNumberWithSuitable(Number value) {
        return formatNumberWithSuitable(value, 1.0);
    }


}
