package com.scareers.datasource.eastmoney;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * description: 维护全局唯一股票池! 以下功能均访问此唯一股票(指数/概念)池
 * 1.FsFetcher: 将获取 股票/指数/概念 1分钟分时图
 * 2.FsTransactionFetcher: 获取个股/指数/概念 tick成交数据. 通常3s/5s每秒
 * 3.Strategy: 主策略相关子类, 含选股策略等, 将可能动态填充本类股票池!
 * 4.Trader/TraderGui: 可在交易过程中, 手动添加股票池
 *
 * @noti 为保证gui多线程可见性, 全部使用静态属性维护
 * @author: admin
 * @date: 2022/2/14/014-17:05:04
 */
public class SecurityPool {
    /**
     * 所有 SecurityBeanEm, 包括所有股票,指数,板块
     */
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> allSecuritySet = new CopyOnWriteArraySet<>();

    /**
     * 昨日持仓股票 + 今日选中股票(可以逻辑选,可以手动选) + 其他关心的热点股票
     */
    // 今日选股, key
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> todaySelectedStocks = new CopyOnWriteArraySet<>();//
    // 昨日持仓,卖出
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> yesterdayHoldStocks = new CopyOnWriteArraySet<>(); //
    // 其他热点股票
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareStocks = new CopyOnWriteArraySet<>();

    /**
     * 核心板块 及 其他关心的板块
     */
    // 核心板块, 往往是选股和昨日持仓股票的板块
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> keyBKs = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> careBKs = new CopyOnWriteArraySet<>(); // 其他热点板块

    /**
     * 核心指数 及 其他关心的指数
     */
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> keyIndexs = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> careIndexs = new CopyOnWriteArraySet<>();

}
