package com.scareers.datasource.eastmoney.realtimetransaction;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;

import java.util.Arrays;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-15:26:04
 */
public class FSTransactionFetcher {
    // 7:00之前记为昨日,抓取数据存入昨日数据表. 09:00以后抓取今日, 期间程序sleep,等待到 09:00
    public static final List<String> newDayTimeThreshold = Arrays.asList("15:00", "16:30");

    public static void main(String[] args) throws InterruptedException {
        FSTransactionFetcher fetcher = new FSTransactionFetcher();
        boolean shouldFetchToday = fetcher.newDayDecide();
        String saveTableName = DateUtil.today();
        if (!shouldFetchToday) {
            // 查找上一个交易日
        }


    }

    private static final Log log = LogUtils.getLogger();

    public boolean newDayDecide() throws InterruptedException {
        DateTime now = DateUtil.date();
        String today = DateUtil.today();
        DateTime thresholdBefore = DateUtil.parse(today + " " + newDayTimeThreshold.get(0));
        DateTime thresholdAfter = DateUtil.parse(today + " " + newDayTimeThreshold.get(1));

        long gtBefore = DateUtil.between(thresholdBefore, now, DateUnit.SECOND, false); // 比下限大
        long ltAfter = DateUtil.between(now, thresholdAfter, DateUnit.SECOND, false); // 比上限小

        if (gtBefore <= 0) {
            return false;
        }
        if (ltAfter <= 0) {
            return true;
        }
        // 此时介于两者之间, 应当等待到 今日开始的时间阈值
        log.warn("wait: 当前时间介于昨日今日判定阈值设定之间, 需等待到: {}, 等待时间: {} 秒",
                newDayTimeThreshold.get(1), ltAfter);
        log.warn("waiting ...");
        Thread.sleep(ltAfter * 1000);
        return true;
    }
}
