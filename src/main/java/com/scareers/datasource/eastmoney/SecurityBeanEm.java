package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityId;
import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityIdsToBeanList;

/**
 * description: 代表一特定证券/指数/板块等资产. Em 东方财富.
 * 内部查询api 可使用资产代码或者名称进行查询. 本类统一使用代码进行查询
 * <p>
 * 1.单个bean, 仅可转换一次, 转换为特定类型后不可变化
 * 2.且本类仅包含一种资产的各种属性, 原则上不包含 "数据(特指k线,分时图等)", 应当将本类实例组合进代表数据的类中
 * 3.东财所有相关api, 建议使用 SecurityBeanEm 作为参数, 而非简单使用资产代码等
 * // @noti: web api, 概念板块和风格板块,无法区分,  "TypeUS":"3" 都是3;
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@Data
public class SecurityBeanEm implements Serializable {
    private static final long serialVersionUID = 156415111L;
    private static final Log log = LogUtil.getLogger();
    // 缓存. key为 代码+类型
    public static ConcurrentHashMap<String, SecurityBeanEm> beanPool = new ConcurrentHashMap<>();

    /**
     * 个股类型/指数/板块 的SecurityTypeName, 以此来转换bean类型;
     * 个股的创业板显示深A, 科创板单独显示,非沪A
     * 每种类型有相关的 类型判定方法. isXxx()
     */
    private static HashSet<String> stockSecurityTypeNames = new HashSet<>(
            Arrays.asList("深A", "沪A", "京A", "科创板", "三板", "深B", "沪B"));
    private static HashSet<String> bkSecurityTypeNames = new HashSet<>(Collections.singletonList("板块"));
    private static HashSet<String> indexSecurityTypeNames = new HashSet<>(Collections.singletonList("指数"));
    private static HashSet<String> bondSecurityTypeNames = new HashSet<>(Collections.singletonList("债券"));
    private static SecurityBeanEm SHANG_ZHENG_ZHI_SHU; // 上证指数, 死循环获取直到成功
    private static SecurityBeanEm SHEN_ZHENG_CHENG_ZHI; // 上证指数, 死循环获取直到成功
    private static SecurityBeanEm ShangZhengZhuanZhaiIndex; // 上证转债指数
    private static SecurityBeanEm ShenZhengZhuanZhaiIndex; // 深证转债指数


    public static void main(String[] args) throws Exception {
        SecurityBeanEm bk = SecurityBeanEm.createBK("茅指数");
        Console.log(bk.getConvertRawJsonObject());
        bk = SecurityBeanEm.createBK("屏下摄像");
        Console.log(bk.getConvertRawJsonObject());
        bk = SecurityBeanEm.createBK("证券");
        Console.log(bk.getConvertRawJsonObject());
        bk = SecurityBeanEm.createBK("北京板块");
        Console.log(bk.getConvertRawJsonObject());

//        Console.log(SecurityBeanEm.createStock("002070",true).getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createStock("000001",true).getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createStock("600798",true).getConvertRawJsonObject());

//        Console.log(SecurityBeanEm.createIndex("000978").getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createIndex("399990").getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createIndex("930707").getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createIndex("H11030").getConvertRawJsonObject());

//        Console.log(SecurityBeanEm.getShangZhengZhuanZhaiIndex());
//        Console.log(SecurityBeanEm.getShenZhengZhuanZhaiIndex());


//        Console.log(SecurityBeanEm.createBond("124005"));
//        Console.log(SecurityBeanEm.createBond("江山转债").getConvertRawJsonObject());
//        Console.log(SecurityBeanEm.createBond("中金转债").getConvertRawJsonObject());
//
//        SecurityBeanEm.getFourGlobalIndex().forEach(Console::log);

//        Console.log(SecurityBeanEm.createBK("充电桩").isConceptBK());
//        Console.log(SecurityBeanEm.createBK("北京板块").isAreaBK());
//        Console.log(SecurityBeanEm.createBK("重庆板块").isAreaBK());
//        Console.log(SecurityBeanEm.createBK("家电行业").isIndustryBK());

//        Console.log(querySecurityId("广电转债"));
//        Console.log(querySecurityId("金农转债"));
//
//        Console.log(querySecurityId("22国债01"));
//        Console.log(querySecurityId("国债1619"));
//
//        Console.log(querySecurityId("20融创01"));
//        Console.log(querySecurityId("17常德01"));

//        Console.log(SecurityBeanEm.createBond("广电转债").isBond());
//        Console.log(SecurityBeanEm.createBond("金农转债").isConvertibleBond());
//        Console.log(SecurityBeanEm.createBond("17常德01").isBond());
//        Console.log(SecurityBeanEm.createBond("22国债01").isConvertibleBond());
//        Console.log(SecurityBeanEm.createBondList(Arrays.asList("广电转债", "金农转债")));

        /*
        [{"SecurityType":"21","Classify":"Bond","JYS":"4","QuoteID":"1.110044","TypeUS":"4","Code":"110044","MktNum":"1","Name":"广电转债","UnifiedCode":"110044","InnerCode":"14636945584533","SecurityTypeName":"债券","PinYin":"GDZZ","ID":"1100441","MarketType":"1"}]
[{"SecurityType":"21","Classify":"Bond","JYS":"8","QuoteID":"0.128036","TypeUS":"8","Code":"128036","MktNum":"0","Name":"金农转债","UnifiedCode":"128036","InnerCode":"41449824414410","SecurityTypeName":"债券","PinYin":"JNZZ","ID":"1280362","MarketType":"2"}]
[{"SecurityType":"16","Classify":"Bond","JYS":"4","QuoteID":"1.019666","TypeUS":"4","Code":"019666","MktNum":"1","Name":"22国债01","UnifiedCode":"019666","InnerCode":"41566785323637","SecurityTypeName":"债券","PinYin":"22GZ01","ID":"0196661","MarketType":"1"}]
[{"SecurityType":"16","Classify":"Bond","JYS":"8","QuoteID":"0.101619","TypeUS":"8","Code":"101619","MktNum":"0","Name":"国债1619","UnifiedCode":"101619","InnerCode":"24832434032234","SecurityTypeName":"债券","PinYin":"GZ1619","ID":"1016192","MarketType":"2"}]
[{"SecurityType":"16","Classify":"Bond","JYS":"4","QuoteID":"1.163376","TypeUS":"4","Code":"163376","MktNum":"1","Name":"20融创01","UnifiedCode":"163376","InnerCode":"18817858935595","SecurityTypeName":"债券","PinYin":"20RC01","ID":"1633761","MarketType":"1"}]
[{"SecurityType":"16","Classify":"Bond","JYS":"8","QuoteID":"0.114173","TypeUS":"8","Code":"114173","MktNum":"0","Name":"17常德01","UnifiedCode":"114173","InnerCode":"49307705056198","SecurityTypeName":"债券","PinYin":"17CD01","ID":"1141732","MarketType":"2"}]
         */


//        SecurityBeanEm stock = SecurityBeanEm.createIndex("H30597");
//        Console.log(stock);
//
//        SecurityBeanEm stock2 = SecurityBeanEm.createIndex("000001");
//        Console.log(stock2);
//
//        Console.log(SecurityBeanEm.createStockList(Arrays.asList("000001", "000007")));
//        Console.log(SecurityBeanEm.createBKList(Arrays.asList("bk1030", "bk1020")));
//        Console.log(SecurityBeanEm.createIndexList(Arrays.asList("000001", "399001")));
//
//        Console.log(SecurityBeanEm.createStock("688513").isHuA());
//        Console.log(SecurityBeanEm.createStock("688513").isKCB());
    }


    private static SecurityBeanEm initShIndex() {
        SecurityBeanEm res;
        while (true) {
            try {
                res = createIndex("上证指数");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("SecurityBeanEm init: 初始化[上证指数]失败");
                continue;
            }
            break;
        }
        return res;
    }

    private static SecurityBeanEm initShBondIndex() {
        SecurityBeanEm res;
        while (true) {
            try {
                res = createIndex("上证转债");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("SecurityBeanEm init: 初始化[上证转债指数]失败");
                continue;
            }
            break;
        }
        return res;
    }


    private static SecurityBeanEm initSzIndex() {
        SecurityBeanEm res;
        while (true) {
            try {
                res = createIndex("深证成指");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("SecurityBeanEm init: 初始化[深证成指]失败");
                continue;
            }
            break;
        }
        return res;
    }

    private static SecurityBeanEm initSzBondIndex() {
        SecurityBeanEm res;
        while (true) {
            try {
                res = createIndex("深证转债");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("SecurityBeanEm init: 初始化[深证转债指数]失败");
                continue;
            }
            break;
        }
        return res;
    }


    /**
     * 给定股票简单代码列表, 获取 已转换为 股票 的 SecurityBeanEm
     * // 本身已经是 CopyOnWriteArrayList
     *
     * @param beans
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createStockList(List<String> queryConditionList, boolean lazyInitBkInfo)
            throws Exception {
        return createStockList(queryConditionList, lazyInitBkInfo, true);
    }

    public static List<SecurityBeanEm> createStockList(List<String> queryConditionList, boolean lazyInitBkInfo,
                                                       boolean logError)
            throws Exception {
        Set<String> withoutCache = queryConditionList.stream()
                .filter(value -> !beanPool.containsKey(value + "__stock"))
                .collect(Collectors.toSet());

        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(new ArrayList<>(withoutCache), logError); // 新增到cache
        for (SecurityBeanEm bean : beans) {
            bean.convertToStock(lazyInitBkInfo);
            beanPool.put(bean.getSecCode() + "__stock", bean); // 放入缓存池
        }

        List<SecurityBeanEm> res = new ArrayList<>(beans);
        for (String s : queryConditionList) {
            if (!withoutCache.contains(s)) {
                res.add(beanPool.get(s + "__stock"));
            }
        }
        return res;
    }

    public static List<SecurityBeanEm> createStockList(List<String> queryConditionList) throws Exception {
        return createStockList(queryConditionList, false);
    }


    /**
     * 给定股票简单代码列表, 获取 已转换为 指数 的 SecurityBeanEm
     *
     * @param queryConditionList
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createIndexList(List<String> queryConditionList) throws Exception {
        List<String> withoutCache = queryConditionList.stream()
                .filter(value -> !beanPool.containsKey(value + "__index"))
                .collect(Collectors.toList());
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(withoutCache); // 新增到cache
        for (SecurityBeanEm bean : beans) {
            bean.convertToIndex();
            beanPool.put(bean.getSecCode() + "__index", bean); // 放入缓存池
        }
        List<SecurityBeanEm> res = new ArrayList<>(beans);
        for (String s : queryConditionList) {
            if (!withoutCache.contains(s)) {
                res.add(beanPool.get(s + "__index"));
            }
        }
        return res;
    }

    /**
     * 给定股票简单代码列表, 获取 已转换为 板块 的 SecurityBeanEm
     *
     * @param queryConditionList
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createBKList(List<String> queryConditionList) throws Exception {
        List<String> withoutCache = queryConditionList.stream()
                .filter(value -> !beanPool.containsKey(value + "__bk"))
                .collect(Collectors.toList());
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(withoutCache); // 新增到cache
        for (SecurityBeanEm bean : beans) {
            bean.convertToBK();
            beanPool.put(bean.getSecCode() + "__bk", bean); // 放入缓存池
        }
        List<SecurityBeanEm> res = new ArrayList<>(beans);
        for (String s : queryConditionList) {
            if (!withoutCache.contains(s)) {
                res.add(beanPool.get(s + "__bk"));
            }
        }
        return res;
    }

    /**
     * 给定股票简单代码列表, 获取 已转换为 债券 的 SecurityBeanEm
     *
     * @param queryConditionList
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createBondList(List<String> queryConditionList, boolean logError)
            throws Exception {
        List<String> withoutCache = queryConditionList.stream()
                .filter(value -> !beanPool.containsKey(value + "__bond"))
                .collect(Collectors.toList());
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(withoutCache, logError); // 新增到cache
        for (SecurityBeanEm bean : beans) {
            bean.convertToBond();
            beanPool.put(bean.getSecCode() + "__bond", bean); // 放入缓存池
        }
        List<SecurityBeanEm> res = new ArrayList<>(beans);
        for (String s : queryConditionList) {
            if (!withoutCache.contains(s)) {
                res.add(beanPool.get(s + "__bond"));
            }
        }
        return res;
    }

    /**
     * 按序创建转债对象列表; 先调用常规方法, 加入缓存后, 再遍历创建, 加入最终结果保证了有序
     *
     * @param queryConditionList
     * @param logError
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createBondListOrdered(List<String> queryConditionList, boolean logError)
            throws Exception {
        createBondList(queryConditionList, logError); // 普通方法将加入缓存
        List<SecurityBeanEm> res = new ArrayList<>();
        for (String s : queryConditionList) {
            try {
                res.add(SecurityBeanEm.createBond(s,logError)); // 按序创建, 读取缓存; 失败无视
            } catch (Exception e) {

            }
        }
        return res;
    }

    /**
     * @param queryConditionList
     * @return
     * @throws Exception
     * @noti 仅构建列表, 并未转换,  转换需要调用 toStockList / toIndexList 方法
     */
    private static List<SecurityBeanEm> queryBatchStockWithoutConvert(List<String> queryConditionList, boolean logError)
            throws Exception {
        return querySecurityIdsToBeanList(queryConditionList, logError); // 使用线程池
    }

    private static List<SecurityBeanEm> queryBatchStockWithoutConvert(List<String> queryConditionList)
            throws Exception {
        return querySecurityIdsToBeanList(queryConditionList); // 使用线程池
    }


    /**
     * @return 返回两大指数的 SecurityBeanEm
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private static List<SecurityBeanEm> twoGlobalIndex = Arrays.asList(SHANG_ZHENG_ZHI_SHU, SHEN_ZHENG_CHENG_ZHI);

    public static List<SecurityBeanEm> getTwoGlobalMarketIndexList() {
        return twoGlobalIndex;
    }

    /**
     * 两大转债指数
     *
     * @return
     */
    private static List<SecurityBeanEm> twoGlobalBondIndex = Arrays
            .asList(ShangZhengZhuanZhaiIndex, ShenZhengZhuanZhaiIndex);

    public static List<SecurityBeanEm> getTwoGlobalBondIndexList() {
        return twoGlobalBondIndex;
    }

    private static List<SecurityBeanEm> fourGlobalIndex = Arrays
            .asList(SHANG_ZHENG_ZHI_SHU, SHEN_ZHENG_CHENG_ZHI, ShangZhengZhuanZhaiIndex, ShenZhengZhuanZhaiIndex);

    public static List<SecurityBeanEm> getFourGlobalIndex() {
        return fourGlobalIndex;
    }


    private static final int retry = 1; // 查询时3次

    String secCode;
    Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.    板块为 90?
    // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
    private JSONArray queryResults; // 全部查询结果, 以下为结果字段
    // Code --> stockCodeSimple, MktNum--> market , QuoteID --> secId ,形如1.000001
    private String quoteId;

    private String Name;
    private String PinYin;
    private String ID;
    private String JYS;
    private String Classify;
    private String MarketType;
    private String SecurityTypeName;
    private String SecurityType;
    private String TypeUS;
    private String UnifiedCode;
    private String InnerCode;

    private SecType secType = SecType.NULL;
    private List<SecurityBeanEm> bkListBelongTo; // 个股该字段将被填充, 所属板块列表

    /**
     * 表示当前已转换类型! 保证仅转换一次!
     * 代表了资产类型
     */
    public enum SecType {
        NULL, // 尚未转换的类型
        FAIL, // 转换失败
        STOCK, // 已转换为股票
        INDEX, // 已转换为指数
        BK, // 已转换为板块
        BOND, // 已转换为 债券
        MG_BY_CODE, // 强制用 "Code" 属性, 转换的美股!
        FUTURE_BY_CODE, //  强制用 "Code" 属性, 转换的期货, 例如富时A50期指连续
        FOREIGN_EXCHANGE_BY_CODE, //  强制用 "Code" 属性, 转换的期货, 例如富时A50期指连续
        OTHER, // 其他类型, 尚未实现的转换类型冗余
    }
    /**
     * [{"SecurityType":"7","Classify":"UsStock","JYS":"AMEX","QuoteID":"107.YINN","TypeUS":"5","Code":"YINN","MktNum":"107","Name":"三倍做多FTSE中国ETF-Direxion","UnifiedCode":"YINN","InnerCode":"17236082930963","SecurityTypeName":"美股","PinYin":"SBZDFTSEZGETFDIREXION","ID":"YINN7","MarketType":"7"},{"SecurityType":"7","Classify":"UsStock","JYS":"AMEX","QuoteID":"107.BITQ","TypeUS":"5","Code":"BITQ","MktNum":"107","Name":"Bitwise Crypto Industry Innovat","UnifiedCode":"BITQ","InnerCode":"27066737335219","SecurityTypeName":"美股","PinYin":"BITWISECRYPTOINDUSTRYINNOVAT","ID":"BITQ7","MarketType":"7"}]
     * [{"SecurityType":"13","Classify":"UniversalFutures","JYS":"SGX","QuoteID":"104.CN00Y","TypeUS":"1","Code":"CN00Y","MktNum":"104","Name":"A50期指当月连续","UnifiedCode":"CN00Y","InnerCode":"46183740881780","SecurityTypeName":"期货","PinYin":"A50QZDYLX","ID":"CN00Y0","MarketType":"0"}]
     */

    /**
     * 给定查询结果构造.
     *
     * @param queryResults
     */
    private String queryCondition;

    public SecurityBeanEm(JSONArray queryResults, String queryCondition) {
        Objects.requireNonNull(queryResults);
        this.queryCondition = queryCondition;
        this.queryResults = queryResults;
    }

    /**
     * 给定股票简单代码构造, 将一定执行查询.  不建议过多单独调用, 应使用线程池版本创建股票池
     *
     * @param queryCondition
     */
    private SecurityBeanEm(String queryCondition) {
        this.queryCondition = queryCondition;
        checkQueryResults();
    }

    private SecurityBeanEm(String queryCondition, boolean logError) {
        this.queryCondition = queryCondition;
        checkQueryResults(logError);
    }

    private void checkQueryResults() { // 死循环查询 3 次
        checkQueryResults(true);
    }

    private void checkQueryResults(boolean logError) { // 死循环查询 3 次
        int retry_ = 0;
        while (queryResults == null) {
            try {
                this.queryResults = querySecurityId(queryCondition);
            } catch (Exception e) {
                if (logError) {
                    log.warn("new EmSecurityBean warning: new时查询失败, 将重试, 条件: " + queryResults);
                }
            }
            if (retry_ >= retry) {
                if (logError) {
                    log.error("new EmSecurityBean fail: new时查询失败超过重试次数, 视为失败, 条件: " + queryResults);
                }
                break;
            }
            retry_++;
        }
    }

    /**
     * 尝试从查询结果中, 读取到股票结果, 填充各个字段, 然后返回this
     * NEEQ 似乎为新三板股票+北交所  23 为科创板
     *
     * @return Arrays.asList(" AStock ", " 23 ", " NEEQ ")
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToStock(boolean lazyInitBkInfo) throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        if (convert(stockSecurityTypeNames)) {
            secType = SecType.STOCK;
        } else {
            secType = SecType.FAIL;
            Console.log(this.queryResults);
            throw new Exception("转换StockBean为股票Bean异常");
        }

        if (!lazyInitBkInfo) {
            try { // 获取板块可能失败, 例如退市股票
                DataFrame<Object> bkDf = EmQuoteApi.getBksTheStockBelongTo(this, 3000, 4);
                if (bkDf == null) {
                    secType = SecType.FAIL;
                    throw new Exception("转换StockBean为股票Bean异常: 获取所属板块列表失败");
                } else {
                    this.bkListBelongTo = createBKList(DataFrameS.getColAsStringList(bkDf, "板块代码"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    /**
     * 因个股初始化时, 可不加载所属板块数据. 本方法为懒加载情况下, 的主动再加载bk数据
     *
     * @param beanEm
     */
    public static void initBksTheStockBelongTo(SecurityBeanEm beanEm) {
        if (beanEm.bkListBelongTo == null) {
            try { // 获取板块可能失败, 例如退市股票
                DataFrame<Object> bkDf = EmQuoteApi.getBksTheStockBelongTo(beanEm, 3000, 4);
                beanEm.bkListBelongTo = createBKList(DataFrameS.getColAsStringList(bkDf, "板块代码"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private SecurityBeanEm convertToStock() throws Exception {
        return convertToStock(false);
    }

    /**
     * 尝试从查询结果中, 读取到板块结果, 填充各个字段, 然后返回this
     *
     * @return
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToBK() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        if (convert(bkSecurityTypeNames)) {
            secType = SecType.BK;
        } else {
            secType = SecType.FAIL;
            throw new Exception("转换StockBean为板块Bean异常");
        }
        return this;
    }

    /**
     * 尝试从查询结果中, 读取到指数结果, 填充各个字段, 然后返回this
     *
     * @return
     */
    private SecurityBeanEm convertToIndex() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        if (!convert(indexSecurityTypeNames)) {
            secType = SecType.FAIL;
            throw new Exception("转换StockBean为指数Bean异常");
        }
        secType = SecType.INDEX;
        return this;
    }


    /**
     * 尝试从查询结果中, 读取到指数结果, 填充各个字段, 然后返回this
     *
     * @return
     */
    private SecurityBeanEm convertToBond() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        if (!convert(bondSecurityTypeNames)) {
            secType = SecType.FAIL;
            throw new Exception("转换StockBean为债券Bean异常");
        }
        secType = SecType.BOND;
        return this;
    }

    /**
     * 尝试从查询结果中, 解析美股, 且其 Code属性, 需要等于 查询条件!
     *
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToMGByCode() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (ele.getString("Code").equals(queryCondition) && "美股".equals(ele.getString("SecurityTypeName"))) {
                // 三项基本
                try {
                    quoteId = ele.get("QuoteID").toString();
                    secCode = ele.get("Code").toString();
                    market = Integer.valueOf(ele.get("MktNum").toString());
                    Name = ele.get("Name").toString();
                    PinYin = ele.get("PinYin").toString();
                    ID = ele.get("ID").toString();
                    JYS = ele.get("JYS").toString();
                    Classify = ele.get("Classify").toString();
                    MarketType = ele.get("MarketType").toString();
                    SecurityTypeName = ele.get("SecurityTypeName").toString();
                    SecurityType = ele.get("SecurityType").toString();
                    TypeUS = ele.get("TypeUS").toString();
                    UnifiedCode = ele.get("UnifiedCode").toString();
                    InnerCode = ele.get("InnerCode").toString();

                    convertRawJsonObject = ele;
                    secType = SecType.MG_BY_CODE;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return this;
            }
        }
        secType = SecType.FAIL;
        throw new Exception("转换StockBean为 美股ByCode 失败");
    }

    /**
     * 尝试从查询结果中, 解析期货, 且其 Code属性, 需要等于 查询条件!
     *
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToFutureByCode() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (ele.getString("Code").equals(queryCondition) && "期货".equals(ele.getString("SecurityTypeName"))) {
                // 三项基本
                try {
                    quoteId = ele.get("QuoteID").toString();
                    secCode = ele.get("Code").toString();
                    market = Integer.valueOf(ele.get("MktNum").toString());
                    Name = ele.get("Name").toString();
                    PinYin = ele.get("PinYin").toString();
                    ID = ele.get("ID").toString();
                    JYS = ele.get("JYS").toString();
                    Classify = ele.get("Classify").toString();
                    MarketType = ele.get("MarketType").toString();
                    SecurityTypeName = ele.get("SecurityTypeName").toString();
                    SecurityType = ele.get("SecurityType").toString();
                    TypeUS = ele.get("TypeUS").toString();
                    UnifiedCode = ele.get("UnifiedCode").toString();
                    InnerCode = ele.get("InnerCode").toString();

                    convertRawJsonObject = ele;
                    secType = SecType.FUTURE_BY_CODE;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return this;
            }
        }
        secType = SecType.FAIL;
        throw new Exception("转换StockBean为 期货ByCode 失败");
    }

    /**
     * 尝试从查询结果中, 解析期货, 且其 Code属性, 需要等于 查询条件!
     *
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToForeignExchangeByCode() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (ele.getString("Code").equals(queryCondition) && "外汇".equals(ele.getString("SecurityTypeName"))) {
                // 三项基本
                try {
                    quoteId = ele.get("QuoteID").toString();
                    secCode = ele.get("Code").toString();
                    market = Integer.valueOf(ele.get("MktNum").toString());
                    Name = ele.get("Name").toString();
                    PinYin = ele.get("PinYin").toString();
                    ID = ele.get("ID").toString();
                    JYS = ele.get("JYS").toString();
                    Classify = ele.get("Classify").toString();
                    MarketType = ele.get("MarketType").toString();
                    SecurityTypeName = ele.get("SecurityTypeName").toString();
                    SecurityType = ele.get("SecurityType").toString();
                    TypeUS = ele.get("TypeUS").toString();
                    UnifiedCode = ele.get("UnifiedCode").toString();
                    InnerCode = ele.get("InnerCode").toString();

                    convertRawJsonObject = ele;
                    secType = SecType.FOREIGN_EXCHANGE_BY_CODE;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return this;
            }
        }
        secType = SecType.FAIL;
        throw new Exception("转换StockBean为 外汇ByCode 失败");
    }

    /**
     * 转换中具体的那一条json结果
     */
    JSONObject convertRawJsonObject;

    private boolean convert(Set<String> typeConditions) {
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (typeConditions.contains(ele.get("SecurityTypeName").toString())) {
                // 三项基本
                try {
                    quoteId = ele.get("QuoteID").toString();
                    secCode = ele.get("Code").toString();
                    market = Integer.valueOf(ele.get("MktNum").toString());
                    Name = ele.get("Name").toString();
                    PinYin = ele.get("PinYin").toString();
                    ID = ele.get("ID").toString();
                    JYS = ele.get("JYS").toString();
                    Classify = ele.get("Classify").toString();
                    MarketType = ele.get("MarketType").toString();
                    SecurityTypeName = ele.get("SecurityTypeName").toString();
                    SecurityType = ele.get("SecurityType").toString();
                    TypeUS = ele.get("TypeUS").toString();
                    UnifiedCode = ele.get("UnifiedCode").toString();
                    InnerCode = ele.get("InnerCode").toString();

                    convertRawJsonObject = ele;
                    return true;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isIndex() {
        return this.secType == SecType.INDEX;
    }

    public boolean isHuIndex() {
        return this.isIndex() && this.getMarket() == 1;
    }

    public boolean isShenIndex() {
        return this.isIndex() && this.getMarket() == 0;
    }

    public boolean isZhongIndex() { // 中证系列指数
        return this.isIndex() && this.getMarket() == 2;
    }


    public boolean isStock() {
        return this.secType == SecType.STOCK;
    }

    public boolean isBK() {
        return this.secType == SecType.BK;
    }

    public boolean isBond() {
        return this.secType == SecType.BOND;
    }

    public boolean isShenBond() {
        return this.isBond() && this.getSecCode().startsWith("12");
    }


    public boolean isHuBond() {
        return this.isBond() && this.getSecCode().startsWith("11");
    }


    public boolean isConvertibleBond() {
        return this.isBond() && "21".equals(this.SecurityType);
        // 可转债的securityType为"21", 国债和企业债,逆回购债券都是"16"
    }


    public boolean isAreaBK() { // 地域板块
        return isBK() && this.getTypeUS().equals("1");
    }

    public boolean isIndustryBK() { // 行业板块
        return isBK() && this.getTypeUS().equals("2");
    }

    public boolean isConceptBK() { // 概念板块
        return isBK() && this.getTypeUS().equals("3");
    }

    public boolean isShenA() { // 包含主板和创业板
        return this.getSecurityTypeName().equals("深A");
    }

    public boolean isHuA() { // 需要添加科创板的逻辑
        return this.getSecurityTypeName().equals("沪A") || isKCB();
    }

    public boolean isShenB() {
        return this.getSecurityTypeName().equals("深B");
    }

    public boolean isHuB() {
        return this.getSecurityTypeName().equals("沪B");
    }

    public boolean isJingA() {
        return this.getSecurityTypeName().equals("京A");
    }

    public boolean isKCB() {
        return this.getSecurityTypeName().equals("科创板");
    }

    public boolean isCYB() { // 创业板为80, 普通主板为6
        return this.isShenA() && "80".equals(this.getJYS());
    }

    public boolean isXSB() {
        return this.getSecurityTypeName().equals("三板");
    }

    public boolean isMg() {
        return "美股".equals(this.getSecurityTypeName());
    }

    public boolean isFuture() {
        return "期货".equals(this.getSecurityTypeName());
    }

    /**
     * 单个实例工厂, 使用缓存. SecurityBeanEm 一旦被转换为股票或者指数后, 不可变
     *
     * @param queryCondition
     * @return
     * @throws Exception
     */
    public static SecurityBeanEm createStock(String queryCondition) throws Exception {
        return createStock(queryCondition, false);
    }

    public static SecurityBeanEm createStock(String queryCondition, boolean lazyInitBkInfo) throws Exception {
        String cacheKey = queryCondition + "__stock";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(queryCondition).convertToStock(lazyInitBkInfo);
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createMgByCode(String code) throws Exception {
        String cacheKey = code + "__mg_by_code";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(code).convertToMGByCode();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createFutureByCode(String code) throws Exception {
        String cacheKey = code + "__future_by_code";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(code).convertToFutureByCode();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createForeignExchangeByCode(String code) throws Exception {
        String cacheKey = code + "__foreign_exchange_by_code";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(code).convertToForeignExchangeByCode();
        beanPool.put(cacheKey, res);
        return res;
    }


    public static SecurityBeanEm createIndex(String queryCondition) throws Exception {
        String cacheKey = queryCondition + "__index";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(queryCondition).convertToIndex();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createBK(String queryCondition) throws Exception {
        String cacheKey = queryCondition + "__bk";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(queryCondition).convertToBK();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createBond(String queryCondition) throws Exception {
        return createBond(queryCondition, false);
    }

    public static SecurityBeanEm createBond(String queryCondition, boolean logError) throws Exception {
        String cacheKey = queryCondition + "__bond";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(queryCondition, logError).convertToBond();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm getShenZhengZhuanZhaiIndex() {
        if (ShenZhengZhuanZhaiIndex == null) {
            ShenZhengZhuanZhaiIndex = initSzBondIndex();
        }
        return ShenZhengZhuanZhaiIndex;
    }

    public static SecurityBeanEm getShangZhengZhuanZhaiIndex() {
        if (ShangZhengZhuanZhaiIndex == null) {
            ShangZhengZhuanZhaiIndex = initShBondIndex();
        }
        return ShangZhengZhuanZhaiIndex;
    }

    public static SecurityBeanEm getShangZhengZhiShu() {
        if (SHANG_ZHENG_ZHI_SHU == null) {
            SHANG_ZHENG_ZHI_SHU = initShIndex();
        }
        return SHANG_ZHENG_ZHI_SHU;
    }

    public static SecurityBeanEm getShenZhengChengZhi() {
        if (SHEN_ZHENG_CHENG_ZHI == null) {
            SHEN_ZHENG_CHENG_ZHI = initSzIndex();
        }
        return SHEN_ZHENG_CHENG_ZHI;
    }

    public static SecurityBeanEm createBeanWithType(String queryCondition, SecType type) throws Exception {
        if (type == SecType.INDEX) {
            return createIndex(queryCondition);
        } else if (type == SecType.STOCK) {
            return createStock(queryCondition);
        } else if (type == SecType.BK) {
            return createBK(queryCondition);
        } else if (type == SecType.BOND) {
            return createBond(queryCondition);
        } else if (type == SecType.MG_BY_CODE) {
            return createMgByCode(queryCondition); // code必须
        } else if (type == SecType.FUTURE_BY_CODE) {
            return createFutureByCode(queryCondition); // code必须
        } else if (type == SecType.FOREIGN_EXCHANGE_BY_CODE) {
            return createForeignExchangeByCode(queryCondition); // code必须
        } else {
            throw new Exception("未知资产类型, 无法创建");
        }
    }


    /**
     * 核心字段: 代码, 市场, 以及唯一的 SecId. (虽然前两周构成SecId)
     *
     * @return
     */
    @Override
    public int hashCode() {
        return this.getSecCode().hashCode() | this.getMarket().hashCode() | this.getQuoteId().hashCode() | this
                .getSecurityType().hashCode();
    }

    /**
     * 同样要求3字段equal
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityBeanEm) {
            SecurityBeanEm other = (SecurityBeanEm) obj;
            if (this == obj) {
                return true; // 单例经常
            }

            return other.getSecCode().equals(this.getSecCode()) &&
                    other.getMarket().equals(this.getMarket()) && this.getQuoteId().equals(other.getQuoteId()) && this
                    .getSecurityType().equals(other.getSecurityType());
        }
        return false;
    }

    /**
     * PO 展示对象.
     * 方便GUI展示, 持有简单字段
     * 实现 toString 以便gui展示.
     * toToolTip 展示提示
     */
    @Setter
    @Getter
    public static class SecurityEmPo implements Comparable {
        private static final long serialVersionUID = 78921546L;

        /**
         * 排序比较.  优先比较 beanType, 其次 focusType, 再次 subBeanType
         *
         * @param o
         * @return
         */
        @SneakyThrows
        @Override
        public int compareTo(Object o) {
            if (o instanceof SecurityEmPo) {
                SecurityEmPo other = (SecurityEmPo) o;
                if (this.beanType == other.beanType) {
                    if (this.focusType == other.focusType) {
                        if (this.subBeanType == other.subBeanType) {
                            return 0;
                        }
                        return this.subBeanType > other.subBeanType ? 1 : -1;
                    } else {
                        return this.focusType > other.focusType ? 1 : -1;
                    }
                } else {
                    return this.beanType > other.beanType ? 1 : -1;
                }
            } else {
                throw new Exception("OrderPo cant not compareTo other types");
            }
        }

        String secCode;
        Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.
        // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
        String name;
        SecurityBeanEm bean;

        int beanType = 100; // 资产所属种类 类型: 股票0, 债券1, 指数2, 板块3, , 其他4
        // 沪A主板0,深A主板1,科创板2,创业板3,沪可转债4,深可转债5, 概念板块6,行业板块7,地域板块8, 大指数9(上深,上深债), 普通指数10, 其他11
        int subBeanType = 100; // 资产所属子种类 类型: 例如股票分为 科创板,创业板,上证,深证等,板块分为 概念/行业/地域板块等
        int focusType = 100; // 关注度类型: 昨持今选0, 今选1, 昨持2, 关注3, 其他4

        public SecurityEmPo(SecurityBeanEm securityBeanEm) {
            this.secCode = securityBeanEm.getSecCode();
            this.market = securityBeanEm.getMarket();
            this.name = securityBeanEm.getName();
            this.bean = securityBeanEm;

            initThreeTypes(); // 设置3种类型
        }

        public static Vector<SecurityEmPo> fromBeanList(List<SecurityBeanEm> beanEmList) {
            if (beanEmList == null) {
                return new Vector<>();
            }
            return beanEmList.stream().map(SecurityEmPo::new).collect(Collectors.toCollection(Vector::new));
        }

        private void initThreeTypes() {
            subBeanType = 11;
            if (bean.isStock()) {
                beanType = 0;

                if (bean.isHuA()) {
                    if (bean.isKCB()) {
                        subBeanType = 2;
                    } else {
                        subBeanType = 0;
                    }
                } else if (bean.isShenA()) {
                    if (bean.isCYB()) {
                        subBeanType = 3;
                    } else {
                        subBeanType = 1;
                    }
                }
            } else if (bean.isBond()) {
                beanType = 1;
                if (bean.getMarket() == 0) {
                    subBeanType = 5;
                } else if (bean.getMarket() == 1) {
                    subBeanType = 4;
                }
            } else if (bean.isIndex()) {
                beanType = 2;

                if (fourGlobalIndex.contains(bean)) {
                    subBeanType = 9;
                } else {
                    subBeanType = 10;
                }
            } else if (bean.isBK()) {
                beanType = 3;
                if (bean.isAreaBK()) {
                    subBeanType = 8;
                } else if (bean.isIndustryBK()) {
                    subBeanType = 7;
                } else if (bean.isConceptBK()) {
                    subBeanType = 6;
                }
            } else {
                beanType = 4;
            }

            if (SecurityPool.isYhTs(bean)) {
                focusType = 0;
            } else if (SecurityPool.isYesterdayHold(bean)) {
                focusType = 2;
            } else if (SecurityPool.isTodaySelected(bean)) {
                focusType = 1;
            } else if (SecurityPool.isOtherCare(bean)) {
                focusType = 3;
            } else {
                focusType = 4;
            }

        }

        @Override
        public String toString() { // 显示 代码.市场[中文名称]
            StringBuilder builder = new StringBuilder();
            builder.append("<html>");
            builder.append(secCode);
            builder.append(".");
            builder.append(market.toString());
            if (bean.isBond()) { // 转债带颜色
                addCommentCore(builder, "red", name);
            } else { // 其他显示put名字
                builder.append(" ["); // 简单形式
                builder.append(name); // 简单形式
                builder.append("]"); // 简单形式
            }

            addCommentAccordingBeanType(builder); // 添加类型附加说明

            builder.append("</html>");
            return builder.toString();
        }

        private void addCommentAccordingBeanType(StringBuilder builder) {
            if (this.beanType == 0) {
                addCommentCore(builder, "green", "股票");
            } else if (this.beanType == 1) {
                addCommentCore(builder, "yellow", "债券");
            } else if (this.beanType == 3) {
                if (this.subBeanType == 6) {
                    addCommentCore(builder, "orange", "概念板块");
                } else if (this.subBeanType == 7) {
                    addCommentCore(builder, "yellow", "行业板块");
                } else if (this.subBeanType == 8) {
                    addCommentCore(builder, "orange", "地域板块");
                }
            } else if (this.beanType == 2) {
                if (this.subBeanType == 9) {
                    addCommentCore(builder, "red", "全局指数");
                } else {
                    addCommentCore(builder, "white", "指数");
                }
            }


            if (this.focusType == 0) {
                addCommentCore(builder, "red", "持选");
            } else if (this.focusType == 1) {
                addCommentCore(builder, "orange", "今选");
            } else if (this.focusType == 2) {
                addCommentCore(builder, "green", "昨持");
            } else if (this.focusType == 3) {
                addCommentCore(builder, "yellow", "关注");
            }


            //// 沪A主板0,深A主板1,科创板2,创业板3,沪可转债4,深可转债5, 概念板块6,行业板块7,地域板块8, 大指数9(上深,上深债), 普通指数10, 其他11
            if (this.subBeanType == 0) {
                addCommentCore(builder, "white", "沪A主板");
            } else if (this.subBeanType == 1) {
                addCommentCore(builder, "gray", "深A主板");
            } else if (this.subBeanType == 2) {
                addCommentCore(builder, "yellow", "科创板");
            } else if (this.subBeanType == 3) {
                addCommentCore(builder, "yellow", "创业板");
            } else if (this.subBeanType == 4) {
                addCommentCore(builder, "white", "沪债");
            } else if (this.subBeanType == 5) {
                addCommentCore(builder, "gray", "深债");
            }
        }

        private void addCommentCore(StringBuilder builder, String colorStr, String comment) {
            builder.append(StrUtil.format(" <font color=\"{}\">[", colorStr)); // 紫色表示今日选股且昨日持仓
            builder.append(comment);
            builder.append("]</font>"); // 简单形式
        }

        public String toToolTip() { // 提示文字, 显示
            return JSONUtilS.toJsonPrettyStr(bean.getConvertRawJsonObject());
        }
    }

    /**
     * PO 展示对象. -- 智能查找时
     * 方便GUI展示, 持有简单字段 -- 东财,同花顺都一样的, 展示 : 代码 -- 名称 -- 类型
     * 实现 toString 以便gui展示.
     * toToolTip 展示提示
     */
    @Setter
    @Getter
    public static class SecurityEmPoForSmartFind {
        private static final long serialVersionUID = 7832100546L;
        public static final int nameShowLength = 10; // 等宽字体, 字符数量, 后pad 空格
        public static final int codeShowLength = 10; // 等宽字体, 字符数量, 后pad 空格
        public static final int typeShowLength = 8; // 等宽字体, 字符数量, 后pad 空格, hutool的pad实现, 会从后截取, 自行修改为截取前面的

        String secCode;
        // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
        String name;
        String securityTypeName;
        SecurityBeanEm bean;

        public SecurityEmPoForSmartFind(SecurityBeanEm securityBeanEm) {
            this.secCode = securityBeanEm.getSecCode();
            this.name = securityBeanEm.getName();
            this.bean = securityBeanEm;
            this.securityTypeName = bean.getSecurityTypeName();
        }

        public static Vector<SecurityEmPoForSmartFind> fromBeanList(List<SecurityBeanEm> beanEmList) {
            if (beanEmList == null) {
                return new Vector<>();
            }
            return beanEmList.stream().map(SecurityEmPoForSmartFind::new).collect(Collectors.toCollection(Vector::new));
        }


        /**
         * 因为给查找列表结果 显示; 该列表注意设置字体, 为等宽字体;
         * 设置  代码/名称/类型, 三者固定 后pad为 等宽 !!!! 不使用 html
         *
         * @return
         */
        @Override
        public String toString() { // 显示 代码.市场[中文名称]
            StringBuilder builder = new StringBuilder();

            addAttr(builder, secCode, codeShowLength);
            addAttr(builder, name, nameShowLength);
            addAttr(builder, securityTypeName, typeShowLength);

            return builder.toString();
        }

        public void addAttr(StringBuilder builder, String attr, int shouldLength) {
            String resStr = attr;
            if (attr == null) {
                resStr = "null";
            }
            if (resStr.length() > shouldLength) {
                resStr = resStr.substring(0, shouldLength);
            }
            builder.append(StrUtil.padAfter(resStr, shouldLength, " "));
        }

        public String toToolTip() { // 提示文字, 显示
            return JSONUtilS.toJsonPrettyStr(bean.getConvertRawJsonObject());
        }
    }
}
