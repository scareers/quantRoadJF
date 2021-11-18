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
        System.out.println(fsTicks.subList(230, fsTicks.size()));
        System.out.println(fsTicks.size());
        Console.log(fsTickDoubleParseToTimeStr(5.0));
        Console.log(fsTimeStrParseToTickDouble("14:29"));
    }

    public static List<String> fsTicks;

    public static List<String> getAllFSTickTimes() {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            res.add(StrUtil.format("09:{}", 30 + i));
        }
        for (int i = 0; i < 60; i++) {
            if (i <= 9) {
                res.add(StrUtil.format("10:0{}", i));
            } else {
                res.add(StrUtil.format("10:{}", i));
            }
        }
        for (int i = 0; i < 31; i++) {
            if (i <= 9) {
                res.add(StrUtil.format("11:0{}", i));
            } else {
                res.add(StrUtil.format("11:{}", i));
            }
        }
        for (int i = 1; i < 60; i++) {
            if (i <= 9) {
                res.add(StrUtil.format("13:0{}", i));
            } else {
                res.add(StrUtil.format("13:{}", i));
            }
        }
        for (int i = 0; i < 60; i++) {
            if (i <= 9) {
                res.add(StrUtil.format("14:0{}", i));
            } else {
                res.add(StrUtil.format("14:{}", i));
            }
        }
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
