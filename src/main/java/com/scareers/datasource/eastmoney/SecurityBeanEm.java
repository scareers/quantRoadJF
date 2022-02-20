package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@Data
public class SecurityBeanEm {
    private static final long serialVersionUID = 156415111L;
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
    public static final SecurityBeanEm SHANG_ZHENG_ZHI_SHU = initShIndex(); // 上证指数, 死循环获取直到成功
    public static final SecurityBeanEm SHEN_ZHENG_CHENG_ZHI = initSzIndex(); // 深证成指


    public static void main(String[] args) throws Exception {
        SecurityBeanEm stock = SecurityBeanEm.createIndex("H30597");
        Console.log(stock);

        SecurityBeanEm stock2 = SecurityBeanEm.createIndex("000001");
        Console.log(stock2);

        Console.log(SecurityBeanEm.createStockList(Arrays.asList("000001", "000007")));
        Console.log(SecurityBeanEm.createBKList(Arrays.asList("bk1030", "bk1020")));
        Console.log(SecurityBeanEm.createIndexList(Arrays.asList("000001", "399001")));
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


    /**
     * 给定股票简单代码列表, 获取 已转换为 股票 的 SecurityBeanEm
     * // 本身已经是 CopyOnWriteArrayList
     *
     * @param beans
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createStockList(List<String> queryConditionList) throws Exception {
        List<String> withoutCache = queryConditionList.stream()
                .filter(value -> !beanPool.containsKey(value + "__stock"))
                .collect(Collectors.toList());
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(withoutCache); // 新增到cache
        for (SecurityBeanEm bean : beans) {
            bean.convertToStock();
            beanPool.put(bean.getSecCode() + "__stock", bean); // 放入缓存池
        }
        return queryConditionList.stream().map(value -> { // 从缓存池查询
            try {
                return SecurityBeanEm.createStock(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
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
        return queryConditionList.stream().map(value -> { // 从缓存池查询
            try {
                return SecurityBeanEm.createIndex(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
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
        return queryConditionList.stream().map(value -> { // 从缓存池查询
            try {
                return SecurityBeanEm.createBK(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
    }

    /**
     * @param queryConditionList
     * @return
     * @throws Exception
     * @noti 仅构建列表, 并未转换,  转换需要调用 toStockList / toIndexList 方法
     */
    private static List<SecurityBeanEm> queryBatchStockWithoutConvert(List<String> queryConditionList)
            throws Exception {
        return querySecurityIdsToBeanList(queryConditionList); // 使用线程池
    }


    /**
     * @return 返回两大指数的 SecurityBeanEm
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static List<SecurityBeanEm> getTwoGlobalMarketIndexList() {
        return new CopyOnWriteArrayList<>(Arrays.asList(SHANG_ZHENG_ZHI_SHU, SHEN_ZHENG_CHENG_ZHI));
    }

    private static final Log log = LogUtil.getLogger();
    private static final int retry = 3; // 查询时3次

    String secCode;
    Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.    板块为 90?
    // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
    private JSONArray queryResults; // 全部查询结果, 以下为结果字段
    // Code --> stockCodeSimple, MktNum--> market , QuoteID --> secId ,形如1.000001
    private String secId;

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
        OTHER, // 其他类型, 尚未实现的转换类型冗余
    }

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

    private void checkQueryResults() { // 死循环查询 3 次
        int retry_ = 0;
        while (queryResults == null) {
            try {
                this.queryResults = querySecurityId(queryCondition);
            } catch (Exception e) {
                if (retry_ >= retry) {
                    log.error("new EmSecurityBean fail: new时查询失败超过重试次数, 视为失败");
                    throw e;
                }
                log.warn("new EmSecurityBean warning: new时查询失败, 将重试");
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
    private SecurityBeanEm convertToStock() throws Exception {
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
        return this;
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
     * 转换中具体的那一条json结果
     */
    JSONObject convertRawJsonObject;

    private boolean convert(Set<String> typeConditions) {
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (typeConditions.contains(ele.get("SecurityTypeName").toString())) {
                // 三项基本
                try {
                    secId = ele.get("QuoteID").toString();
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

    public boolean isStock() {
        return this.secType == SecType.STOCK;
    }

    public boolean isBK() {
        return this.secType == SecType.BK;
    }

    public boolean isShenA() {
        return this.getSecurityTypeName().equals("深A");
    }

    public boolean isHuA() {
        return this.getSecurityTypeName().equals("沪A");
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

    public boolean isXSB() {
        return this.getSecurityTypeName().equals("三板");
    }

    /**
     * 单个实例工厂, 使用缓存. SecurityBeanEm 一旦被转换为股票或者指数后, 不可变
     *
     * @param queryCondition
     * @return
     * @throws Exception
     */
    public static SecurityBeanEm createStock(String queryCondition) throws Exception {
        String cacheKey = queryCondition + "__stock";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(queryCondition).convertToStock();
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

    public static SecurityBeanEm createBeanWithType(String queryCondition, SecType type) throws Exception {
        if (type == SecType.INDEX) {
            return createIndex(queryCondition);
        } else if (type == SecType.STOCK) {
            return createStock(queryCondition);
        } else if (type == SecType.BK) {
            return createBK(queryCondition);
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
        return this.getSecCode().hashCode() | this.getMarket().hashCode() | this.getSecId().hashCode();
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
            return other.getSecCode().equals(this.getSecCode()) &&
                    other.getMarket().equals(this.getMarket()) && this.getSecId().equals(other.getSecId());
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

        @SneakyThrows
        @Override
        public int compareTo(Object o) {
            if (o instanceof SecurityEmPo) {
                if (this.type.equals(((SecurityEmPo) o).type)) {// 1.类型优先
                    return this.secCode.compareTo(((SecurityEmPo) o).secCode); // 2.代码优先
                } else {
                    return this.type.compareTo(((SecurityEmPo) o).type);
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
        Integer type; // 类型: 0代表指数, 1代表今日选股(可买), 2代表昨日持仓(可卖), 3代表昨日有持仓且今日被选中, 4.未知

        public SecurityEmPo(SecurityBeanEm securityBeanEm) {
            this.secCode = securityBeanEm.getSecCode();
            this.market = securityBeanEm.getMarket();
            this.name = securityBeanEm.getName();
            this.bean = securityBeanEm;

            if (bean.isIndex()) {
                this.type = 0;
            } else {
                if (SecurityPool.yesterdayHoldStocks.contains(bean)) {
                    if (SecurityPool.todaySelectedStocks.contains(bean)) {
                        this.type = 3;
                    } else {
                        this.type = 2;
                    }
                } else {
                    if (SecurityPool.todaySelectedStocks.contains(bean)) {
                        this.type = 1;
                    } else {
                        this.type = 4;
                    }
                }
            }
        }

        @Override
        public String toString() { // 显示 代码.市场[中文名称]
            StringBuilder builder = new StringBuilder();
            builder.append("<html>");
            builder.append(secCode);
            builder.append(".");
            builder.append(market.toString());
            builder.append(" ["); // 简单形式
            builder.append(name); // 简单形式
            builder.append("]"); // 简单形式

            builder.append(" "); // 类型分类
            if (this.type == 0) {
                builder.append(" <font color=\"red\">["); // 红色表示指数
                builder.append("指数");
                builder.append("]</font>"); // 简单形式
            } else if (this.type == 1) {
                builder.append(" <font color=\"yellow\">["); // 蓝色表示选股
                builder.append("今日选股");
                builder.append("]</font>"); // 简单形式
            } else if (this.type == 2) {
                builder.append(" <font color=\"green\">["); // 绿色表示昨日持仓
                builder.append("昨日持仓");
                builder.append("]</font>"); // 简单形式
            } else if (this.type == 3) {
                builder.append(" <font color=\"purple\">["); // 紫色表示今日选股且昨日持仓
                builder.append("昨日持仓");
                builder.append("]</font>"); // 简单形式
            } else {
                builder.append(" <font color=\"red\">[");
                builder.append("未知类型");
                builder.append("]</font>"); // 简单形式
            }

            builder.append("</html>");
            return builder.toString();
        }

        public String toToolTip() { // 提示文字, 显示
            return JSONUtilS.toJsonPrettyStr(bean.getConvertRawJsonObject());
        }
    }
}
