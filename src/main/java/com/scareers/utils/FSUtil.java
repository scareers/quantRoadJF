package com.scareers.utils;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 分时图 tick 和时间转换. 为了方便, tick使用 Double的 1.0,2.0表示  0.0-240.0
 *
 * @author: admin
 * @date: 2021/11/18/018-9:13
 */
public class FSUtil {
    static {
        fsTicks = getAllFSTickTimes();
    }

    public static void main(String[] args) {
        System.out.println(fsTicks.subList(0, fsTicks.size()));
        System.out.println(fsTicks.size());
        Console.log(fsTickDoubleParseToTimeStr(100.0));
        Console.log(fsTimeStrParseToTickDouble("14:29"));
    }

    public static List<String> fsTicks;

    public static List<String> generateTickTimes(int start, int end, String hour) {
        List<String> res = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (i <= 9) {
                res.add(StrUtil.format("{}:0{}", hour, i));
            } else {
                res.add(StrUtil.format("{}:{}", hour, i));
            }
        }
        return res;
    }

    public static List<String> getAllFSTickTimes() {
        List<String> res = new ArrayList<>();
        res.addAll(generateTickTimes(30, 60, "09"));
        res.addAll(generateTickTimes(0, 60, "10"));
        res.addAll(generateTickTimes(0, 31, "11"));
        res.addAll(generateTickTimes(1, 60, "13"));
        res.addAll(generateTickTimes(0, 60, "14"));
        res.add("15:00");
        return res;
    }


    public static String fsTickDoubleParseToTimeStr(Double tickDouble) {
        return fsTicks.get(tickDouble.intValue());
    }

    public static Double fsTimeStrParseToTickDouble(String tickTimeStr) {
        Integer index = fsTicks.indexOf(tickTimeStr);
        return index.doubleValue();
    }
}
