package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Assert;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;

import java.util.Collection;
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
     * 所有 SecurityBeanEm, 包括所有股票,指数,板块. 自身不直接all元素, 其他分类股票池添加时, 均会添加到此集合
     */
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> allSecuritySet = new CopyOnWriteArraySet<>();

    /**
     * 昨日持仓股票 + 今日选中股票(可以逻辑选,可以手动选) + 其他关心的热点股票, 买卖池可有交集, 一般其余关心池互斥二者
     */
    // 今日选股, key
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> todaySelectedStocks = new CopyOnWriteArraySet<>();
    // 昨日持仓,卖出
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> yesterdayHoldStocks = new CopyOnWriteArraySet<>();
    // 其他热点股票
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareStocks = new CopyOnWriteArraySet<>();

    /**
     * 核心板块 及 其他关心的板块, 一般互斥
     */
    // 核心板块, 往往是选股和昨日持仓股票的板块
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> keyBKs = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> careBKs = new CopyOnWriteArraySet<>(); // 其他热点板块

    /**
     * 核心指数 及 其他关心的指数, 一般互斥
     */
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> keyIndexs = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> careIndexs = new CopyOnWriteArraySet<>();

    private static final Log log = LogUtil.getLogger();

    /*
     * 各项集合的添加方法.
     *
     * @param singleBeanOrCollection
     */

    /**
     * 3个辅助方法.
     *
     * @param whichSet
     * @param beanEm
     * @param checkType
     */
    private static void addSingleBeanToSet(CopyOnWriteArraySet<SecurityBeanEm> whichSet, SecurityBeanEm beanEm,
                                           String checkType) {
        checkType(beanEm, checkType);
        whichSet.add(beanEm);
        allSecuritySet.add(beanEm);
    }

    private static void addMultiBeanToSet(CopyOnWriteArraySet<SecurityBeanEm> whichSet,
                                          Collection<SecurityBeanEm> beans,
                                          String checkType) {
        for (SecurityBeanEm bean : beans) {
            checkType(bean, checkType);
        }
        whichSet.addAll(beans);
        allSecuritySet.addAll(beans);
    }

    private static void checkType(SecurityBeanEm beanEm, String checkType) {
        if ("bk".equals(checkType)) {
            Assert.isTrue(beanEm.isBK());
        } else if ("index".equals(checkType)) {
            Assert.isTrue(beanEm.isIndex());
        } else {
            Assert.isTrue(beanEm.isStock());
        }
    }

    /**
     * 6个股票添加方法
     *
     * @param beanEm
     */
    public static void addToTodaySelectedStocks(SecurityBeanEm beanEm) {
        addSingleBeanToSet(todaySelectedStocks, beanEm, "stock");
    }

    public static void addToTodaySelectedStocks(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(todaySelectedStocks, beans, "stock");
    }

    public static void addToYesterdayHoldStocks(SecurityBeanEm beanEm) {
        addSingleBeanToSet(yesterdayHoldStocks, beanEm, "stock");
    }

    public static void addToYesterdayHoldStocks(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(yesterdayHoldStocks, beans, "stock");
    }

    public static void addToOtherCareStocks(SecurityBeanEm beanEm) {
        addSingleBeanToSet(otherCareStocks, beanEm, "stock");
    }

    public static void addToOtherCareStocks(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(otherCareStocks, beans, "stock");
    }


}
