package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.quotecenter.StockApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * description: 维护全局唯一股票(资产)池! 以下功能均访问此唯一股票(指数/概念)池
 * <p>
 * 1.FsFetcher: 将获取 股票/指数/概念 1分钟分时图
 * 2.FsTransactionFetcher: 获取个股/指数/概念 tick成交数据. 通常3s/5s每秒
 * 3.Strategy: 主策略相关子类, 含选股策略等, 将可能动态填充本类股票池!
 * 4.Trader/TraderGui: 可在交易过程中, 手动添加股票池
 * <p>
 * -----------> 特殊
 * 1.todo: 将调用 EmSeleniumApi 获取东方财富个股人气榜和人气飙升榜.
 *
 * @noti 为保证gui多线程可见性, 全部使用静态属性维护
 * @noti 因股票池可能动态改变, 导致其他线程遍历时出现错误, 定义 xxxCopy 方法, 将返回一个新的浅复制的集合,以供遍历.
 * @noti 各股票池public, 当不需要遍历时可直接使用某些方法, 例如 contains?
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
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareBKs = new CopyOnWriteArraySet<>(); // 其他热点板块

    /**
     * 核心指数 及 其他关心的指数, 一般互斥
     */
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> keyIndexes = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> otherCareIndexes = new CopyOnWriteArraySet<>();

    private static final Log log = LogUtil.getLogger();


    /*
    @key: 全局股票池. 例如供 FsFetcher 等爬虫遍历的股票池; 仅提高copy方法, 自行实现逻辑. 一般组合以上7种基本的股票池, 作为爬取对象
    @key: 可添加其他动态股票池
    @key: 因此 FsFetcher 等爬虫运行之前, 一般需要先往对应股票池添加 bean
     */

    /**
     * FsFetcher 使用的股票池, 自行实现逻辑.
     *
     * @return
     */
    public static ArrayList<SecurityBeanEm> poolForFsFetcherCopy() {
        return iterSets(allSecuritySet);
    }

    /**
     * FsTransactionFetcher 使用的股票池, 自行实现逻辑.
     *
     * @return
     */
    public static ArrayList<SecurityBeanEm> poolForFsTransactionFetcherCopy() {
        return iterSets(allSecuritySet);
    }


    /*
     * 各项集合的添加方法.
     *
     * @param singleBeanOrCollection
     */

    /**
     * 3个辅助方法.
     *
     */
    /**
     * @param whichSet
     * @param beanEm
     * @param checkType
     * @param excludeBad 是否强制忽略st和退市股票, 默认忽略. 可通过设置 excludeBad 属性 修改
     */
    public static void addSingleBeanToSet(CopyOnWriteArraySet<SecurityBeanEm> whichSet, SecurityBeanEm beanEm,
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
        for (SecurityBeanEm beanEm : beans) {
            whichSet.add(beanEm);
        }
        allSecuritySet.addAll(beans);
    }

    private static void checkType(SecurityBeanEm beanEm, String checkType) {
        if ("bk".equals(checkType)) {
            Assert.isTrue(beanEm.isBK());
        } else if ("index".equals(checkType)) {
            Assert.isTrue(beanEm.isIndex());
        } else if ("stock".equals(checkType)) {
            Assert.isTrue(beanEm.isStock());
        } // 可不传递 checkType, 则 无视类型, 均添加到某池
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
        return iterSets(todaySelectedStocks, yesterdayHoldStocks, otherCareStocks);
    }

    public static ArrayList<SecurityBeanEm> allIndexesCopy() {
        return iterSets(keyIndexes, otherCareIndexes);
    }

    public static ArrayList<SecurityBeanEm> allBKsCopy() {
        return iterSets(keyBKs, otherCareBKs);
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
            return SecurityBeanEm.createIndexList(stocks);
        } else if (type == SecurityBeanEm.SecType.BK) {
            return SecurityBeanEm.createBKList(stocks);
        } else {
            return SecurityBeanEm.createStockList(stocks);
        }
    }

    /**
     * @param amount
     * @param random
     * @param addTwoMarketIndex
     * @return
     * @throws Exception
     * @noti 若市场设置为 "stock" 则会包含新三板,代号 NEED,  北交所.
     */
    public static List<SecurityBeanEm> createStockPool(int amount, boolean random)
            throws Exception {
        List<SecurityBeanEm> results = createSecurityPoolRandom(amount, random, Arrays.asList("沪深A股"),
                SecurityBeanEm.SecType.STOCK);
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
    public static List<SecurityBeanEm> createStockPool(List<String> stockCodes)
            throws Exception {
        List<SecurityBeanEm> results = SecurityBeanEm.createStockList(stockCodes);
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
