package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

    private static ArrayList<SecurityBeanEm> iterSets(CopyOnWriteArraySet<SecurityBeanEm>... sets) {
        ArrayList<SecurityBeanEm> res = new ArrayList<>();
        for (CopyOnWriteArraySet<SecurityBeanEm> set : sets) {
            res.addAll(set);
        }
        return res;
    }

    /**
     * 8个单池浅复制. 以遍历
     *
     * @return
     */
    public static ArrayList<SecurityBeanEm> todaySelectedStocksCopy() {
        return iterSets(todaySelectedStocks);
    }

    public static ArrayList<SecurityBeanEm> yesterdayHoldStocksCopy() {
        return iterSets(yesterdayHoldStocks);
    }

    public static ArrayList<SecurityBeanEm> otherCareStocksCopy() {
        return iterSets(otherCareStocks);
    }

    public static ArrayList<SecurityBeanEm> keyBKsCopy() {
        return iterSets(keyBKs);
    }

    public static ArrayList<SecurityBeanEm> otherCareBKsCopy() {
        return iterSets(otherCareBKs);
    }

    public static ArrayList<SecurityBeanEm> keyIndexesCopy() {
        return iterSets(keyIndexes);
    }

    public static ArrayList<SecurityBeanEm> otherCareIndexesCopy() {
        return iterSets(otherCareIndexes);
    }

    public static ArrayList<SecurityBeanEm> allSecuritySetCopy() {
        return iterSets(allSecuritySet);
    }

    /**
     * 3个组合池复制, 以遍历
     */
    public static ArrayList<SecurityBeanEm> allStocksCopy() {
        ArrayList<SecurityBeanEm> beanEms = new ArrayList<>();
        beanEms.addAll(todaySelectedStocks);
        beanEms.addAll(yesterdayHoldStocks);
        beanEms.addAll(otherCareStocks);
        return beanEms;
    }

    public static ArrayList<SecurityBeanEm> allIndexesCopy() {
        ArrayList<SecurityBeanEm> beanEms = new ArrayList<>();
        beanEms.addAll(keyIndexes);
        beanEms.addAll(otherCareIndexes);
        return beanEms;
    }

    public static ArrayList<SecurityBeanEm> allBKsCopy() {
        ArrayList<SecurityBeanEm> beanEms = new ArrayList<>();
        beanEms.addAll(keyBKs);
        beanEms.addAll(otherCareBKs);
        return beanEms;
    }

    /*
    资产池构造静态方法.
     */

    /**
     * 将读取东财某些市场全部标的列表, 随机选择n个, 或者前n个. debug用
     * type准确性自行保证
     *
     * @param stockPoolSimple
     * @param addTwoMarketIndex
     * @return
     * @throws Exception
     */
    private static List<SecurityBeanEm> createSecurityPoolRandom(int amount, boolean random, List<String> markets,
                                                                 SecurityBeanEm.SecType type)
            throws Exception {
        DataFrame<Object> tick = StockApi.getRealtimeQuotes(markets);
        List<String> stockCode = DataFrameS.getColAsStringList(tick, "股票代码"); // 可能是指数/板块代码
        List<String> stocks;
        if (random) {
            stocks = RandomUtil.randomEleList(stockCode, amount);
        } else {
            stocks = stockCode.subList(0, Math.min(amount, stockCode.size()));
        }
        if (type == SecurityBeanEm.SecType.INDEX) {
            return SecurityBeanEm.createStockList(stocks);
        } else if (type == SecurityBeanEm.SecType.BK) {
            return SecurityBeanEm.createBKList(stocks);
        } else {
            return SecurityBeanEm.createStockList(stocks);
        }
    }

    public static List<SecurityBeanEm> createStockPool(int amount, boolean random, boolean addTwoMarketIndex)
            throws Exception {
        List<SecurityBeanEm> results = createSecurityPoolRandom(amount, random, Arrays.asList("stock"),
                SecurityBeanEm.SecType.STOCK);
        if (addTwoMarketIndex) {
            results.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        }
        return results;
    }

    /**
     * @param amount
     * @param random
     * @param markets Arrays.asList("概念板块","行业板块","地域板块")
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createBKPool(int amount, boolean random, List<String> markets)
            throws Exception {
        List<SecurityBeanEm> results = createSecurityPoolRandom(amount, random, markets,
                SecurityBeanEm.SecType.BK);
        return results;
    }

    /**
     * @param amount
     * @param random
     * @param markets Arrays.asList("上证系列指数", "深证系列指数", "沪深系列指数")
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createIndexPool(int amount, boolean random, List<String> markets)
            throws Exception {
        List<SecurityBeanEm> results = createSecurityPoolRandom(amount, random, markets,
                SecurityBeanEm.SecType.INDEX);
        return results;
    }

    /**
     * 个股池构造.
     *
     * @param stockCodes
     * @param addTwoMarketIndex
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createStockPool(List<String> stockCodes, boolean addTwoMarketIndex)
            throws Exception {
        List<SecurityBeanEm> results = SecurityBeanEm.createStockList(stockCodes);
        if (addTwoMarketIndex) {
            results.addAll(SecurityBeanEm.getTwoGlobalMarketIndexList());
        }
        return results;
    }

    public static List<SecurityBeanEm> createIndexPool(List<String> indexCodes)
            throws Exception {
        return SecurityBeanEm.createIndexList(indexCodes);
    }

    public static List<SecurityBeanEm> createBKPool(List<String> bkCodes)
            throws Exception {
        return SecurityBeanEm.createBKList(bkCodes);
    }


}
