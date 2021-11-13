package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs;

/**
 * description: 针对 next0b1s/1b2s等, 对 nextLow 在买入当日的 最低点出现的时间, 0-240; 分布分析.  -- 出现时间分布
 * -- 时间
 * 0 代表 09:30;  240代表 15:00 , 对应tushare分时数据, 每日 241个数据.
 * -- 分区间
 * 例如将241分钟, 中间 240间隔, 按刻分, 则分为8个区间, 规定 9:30 算作第一个区间内 30-45 算作第一个区间; 46-00 第二个
 *
 * @author: admin
 * @date: 2021/11/14  0014-4:48
 */
public class FSAnalyzeLowDistributionOfLowBuyNextHighSell {
}
