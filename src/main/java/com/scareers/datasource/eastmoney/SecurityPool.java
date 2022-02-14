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
 * @noti 因股票池可能动态改变, 导致其他线程遍历时出现错误, 定义 xxxCopy 方法, 将返回一个新的浅复制的集合,以供遍历.
 * @author: admin
 * @date: 2022/2/14/014-17:05:04
 */
public class SecurityPool {
    /**
     * 所有 SecurityBeanEm, 包括所有股票,指数,板块. 自身不直接all元素, 其他分类股票池添加时, 均会添加到此集合
     */
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> allSecuritySet = new CopyOnWriteArraySet<>();

    /**
     * 昨日持仓股票 + 今日选中股票(可以逻辑选,可以手动选) + 其他关心的热点股票, 买卖池可有交集, 一般其余关心池互斥二者
     */
    // 今日选股, key
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> todaySelectedStocks = new CopyOnWriteArraySet<>();
    // 昨日持仓,卖出
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> yesterdayHoldStocks = new CopyOnWriteArraySet<>();
    // 其他热点股票
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareStocks = new CopyOnWriteArraySet<>();

    /**
     * 核心板块 及 其他关心的板块, 一般互斥
     */
    // 核心板块, 往往是选股和昨日持仓股票的板块
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> keyBKs = new CopyOnWriteArraySet<>();
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareBKs = new CopyOnWriteArraySet<>(); // 其他热点板块

    /**
     * 核心指数 及 其他关心的指数, 一般互斥
     */
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> keyIndexes = new CopyOnWriteArraySet<>();
    private static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareIndexes = new CopyOnWriteArraySet<>();

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

    /**
     * 4个板块添加方法
     *
     * @param beanEm
     */
    public static void addToKeyBKs(SecurityBeanEm beanEm) {
        addSingleBeanToSet(keyBKs, beanEm, "bk");
    }

    public static void addToKeyBKs(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(keyBKs, beans, "bk");
    }

    public static void addToOtherCareBKs(SecurityBeanEm beanEm) {
        addSingleBeanToSet(otherCareBKs, beanEm, "bk");
    }

    public static void addToOtherCareBKs(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(otherCareBKs, beans, "bk");
    }

    /**
     * 4个指数添加方法
     *
     * @param beanEm
     */
    public static void addToKeyIndexes(SecurityBeanEm beanEm) {
        addSingleBeanToSet(keyIndexes, beanEm, "index");
    }

    public static void addToKeyIndexes(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(keyIndexes, beans, "index");
    }

    public static void addToOtherCareIndexes(SecurityBeanEm beanEm) {
        addSingleBeanToSet(otherCareIndexes, beanEm, "index");
    }

    public static void addToOtherCareIndexes(Collection<SecurityBeanEm> beans) {
        addMultiBeanToSet(otherCareIndexes, beans, "index");
    }

    /*
     * 构造浅复制股票池以遍历! iterXxx
     */

    private static CopyOnWriteArraySet<SecurityBeanEm> iterSets(CopyOnWriteArraySet<SecurityBeanEm>... sets) {
        CopyOnWriteArraySet<SecurityBeanEm> res = new CopyOnWriteArraySet<>();
        for (CopyOnWriteArraySet<SecurityBeanEm> set : sets) {
            res.addAll(set);
        }
        return res;
    }

    /**
     * 8个单池浅复制.
     * @return
     */
    public static CopyOnWriteArraySet<SecurityBeanEm> todaySelectedStocksCopy() {
        return iterSets(todaySelectedStocks);
    }

    public static CopyOnWriteArraySet<SecurityBeanEm> yesterdayHoldStocksCopy() {
        return iterSets(yesterdayHoldStocks);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> otherCareStocksCopy() {
        return iterSets(otherCareStocks);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> keyBKsCopy() {
        return iterSets(keyBKs);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> otherCareBKsCopy() {
        return iterSets(otherCareBKs);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> keyIndexesCopy() {
        return iterSets(keyIndexes);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> otherCareIndexesCopy() {
        return iterSets(otherCareIndexes);
    }
    public static CopyOnWriteArraySet<SecurityBeanEm> allSecuritySetCopy() {
        return iterSets(allSecuritySet);
    }

    /**
     * 3个组合池复制
     */

}
