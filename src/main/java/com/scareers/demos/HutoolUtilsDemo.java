package com.scareers.demos;

import cn.hutool.core.lang.Console;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/13  0013-9:53
 */
public class HutoolUtilsDemo {
    public static void cronDemo() {
        CronUtil.schedule("0 30 15 * * ?", new Task() {
            // 动态定时任务
            @Override
            public void execute() {
                try {
//                    mainx(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

// 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
        Console.log("定时任务开始");

    }

    public static void logDemo() {
        Log log = LogFactory.get();
        log.info("x");
    }

    public static void combinationDemo() {
        List<String[]> strs = MathUtil.combinationSelect(new String[]{"a", "b", "c"}, 2);
        strs.stream().forEach(Console::log);
    }

    public static void roundDemo() {
        Console.log(NumberUtil.round(1.234, 2));
    }
}
