package com.scareers.datasource.eastmoney;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityId;
import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityIdsToBeanList;

/**
 * description: 代表一特定证券/指数/板块等资产. Em 东方财富.
 * 内部查询api 可使用资产代码或者名称进行查询. 本类统一使用代码进行查询
 * <p>
 * 1.单个bean, 仅可转换一次, 转换为特定类型后不可变化
 * 2.且本类仅包含一种资产的各种属性, 原则上不包含 "数据(特指k线,分时图等)", 应当将本类实例组合进代表数据的类中
 * 3.东财所有相关api, 建议使用 SecurityBeanEm 作为参数, 而非简单使用股票代码等
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@Data
public class SecurityBeanEm {
    private static final long serialVersionUID = 156415111L;
    // 缓存. key为 代码+类型
    public static ConcurrentHashMap<String, SecurityBeanEm> beanPool = new ConcurrentHashMap<>();
    public static final SecurityBeanEm SHANG_ZHENG_ZHI_SHU = initShIndex(); // 上证指数, 死循环获取直到成功
    public static final SecurityBeanEm SHEN_ZHENG_CHENG_ZHI = initSzIndex(); // 深证成指


    private static SecurityBeanEm initShIndex() {
        SecurityBeanEm res;
        while (true) {
            try {
                res = createIndex("000001");
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
                res = createIndex("399001");
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
    public static List<SecurityBeanEm> createStockList(List<String> stockListSimple) throws Exception {
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(stockListSimple);
        for (SecurityBeanEm bean : beans) {
            bean.convertToStock();
            beanPool.put(bean.getSecCode() + "__stock", bean); // 放入缓存池
        }
        return beans; // 列表不变
    }

    /**
     * 给定股票简单代码列表, 获取 已转换为 指数 的 SecurityBeanEm
     *
     * @param stockListSimple
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createIndexList(List<String> stockListSimple) throws Exception {
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(stockListSimple);
        for (SecurityBeanEm bean : beans) {
            bean.convertToIndex();
            beanPool.put(bean.getSecCode() + "__index", bean); // 放入缓存池
        }
        return beans;
    }

    /**
     * 给定股票简单代码列表, 获取 已转换为 板块 的 SecurityBeanEm
     *
     * @param stockListSimple
     * @return
     * @throws Exception
     */
    public static List<SecurityBeanEm> createBKList(List<String> stockListSimple) throws Exception {
        List<SecurityBeanEm> beans = queryBatchStockWithoutConvert(stockListSimple);
        for (SecurityBeanEm bean : beans) {
            bean.convertToBK();
            beanPool.put(bean.getSecCode() + "__bk", bean); // 放入缓存池
        }
        return beans;
    }

    /**
     * @param stockListSimple
     * @return
     * @throws Exception
     * @noti 仅构建列表, 并未转换,  转换需要调用 toStockList / toIndexList 方法
     */
    private static List<SecurityBeanEm> queryBatchStockWithoutConvert(List<String> stockListSimple) throws Exception {
        return querySecurityIdsToBeanList(stockListSimple); // 使用线程池
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
    public SecurityBeanEm(JSONArray queryResults) {
        this.queryResults = queryResults;
        checkQueryResults(); // 若null将强制查询
    }

    /**
     * 给定股票简单代码构造, 将一定执行查询.  不建议过多单独调用, 应使用线程池版本创建股票池
     *
     * @param secCode
     */
    private SecurityBeanEm(String secCode) {
        this.secCode = secCode; // 将被查询
        checkQueryResults();
    }

    private void checkQueryResults() { // 死循环查询 3次
        int retry_ = 0;
        while (queryResults == null) {
            try {
                this.queryResults = querySecurityId(secCode);
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
     *
     * @return
     * @noti: 不新建对象
     */
    private SecurityBeanEm convertToStock() throws Exception {
        if (secType != SecType.NULL) {
            throw new Exception("SecurityBeanEm 已被转化,不可再次转换");
        }
        if (convert(Arrays.asList("AStock", "23"))) {
            secType = SecType.STOCK;
        } else {
            secType = SecType.FAIL;
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
        if (convert(Arrays.asList("BK"))) {
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
        if (!convert(Arrays.asList("Index"))) {
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

    private boolean convert(List<String> typeConditions) {
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (typeConditions.contains(ele.get("Classify").toString())) {
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


    /**
     * 单个实例工厂, 使用缓存. SecurityBeanEm 一旦被转换为股票或者指数后, 不可变
     *
     * @param stockCodeSimple
     * @return
     * @throws Exception
     */
    public static SecurityBeanEm createStock(String stockCodeSimple) throws Exception {
        String cacheKey = stockCodeSimple + "__stock";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(stockCodeSimple).convertToStock();
        beanPool.put(cacheKey, res);
        return res;
    }


    public static SecurityBeanEm createIndex(String stockCodeSimple) throws Exception {
        String cacheKey = stockCodeSimple + "__index";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(stockCodeSimple).convertToIndex();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createBK(String stockCodeSimple) throws Exception {
        String cacheKey = stockCodeSimple + "__bk";
        SecurityBeanEm res = beanPool.get(cacheKey);
        if (res != null) {
            return res;
        }
        res = new SecurityBeanEm(stockCodeSimple).convertToBK();
        beanPool.put(cacheKey, res);
        return res;
    }

    public static SecurityBeanEm createBeanWithType(String stockCodeSimple, SecType type) throws Exception {
        if (type == SecType.INDEX) {
            return createIndex(stockCodeSimple);
        } else if (type == SecType.STOCK) {
            return createStock(stockCodeSimple);
        } else if (type == SecType.BK) {
            return createBK(stockCodeSimple);
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
        // 两大集合将在选股完成后立即填充.
        public static CopyOnWriteArraySet<SecurityBeanEm> yesterdayHolds = new CopyOnWriteArraySet<>(); // 昨日持仓
        public static CopyOnWriteArraySet<SecurityBeanEm> todaySelected = new CopyOnWriteArraySet<>(); // 今日选中.

        private static final long serialVersionUID = 78921546L;

        @SneakyThrows
        @Override
        public int compareTo(Object o) {
            if (o instanceof SecurityEmPo) {
                if (this.type.equals(((SecurityEmPo) o).type)) {
                    return this.stockCodeSimple.compareTo(((SecurityEmPo) o).stockCodeSimple); // 代码优先
                } else { // 类型优先
                    return this.type.compareTo(((SecurityEmPo) o).type);
                }
            } else {
                throw new Exception("OrderPo cant not compareTo other types");
            }
        }

        String stockCodeSimple;
        Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.
        // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
        String name;
        SecurityBeanEm bean;
        Integer type; // 类型: 0代表指数, 1代表今日选股(可买), 2代表昨日持仓(可卖), 3代表昨日有持仓且今日被选中, 4.未知

        public SecurityEmPo(SecurityBeanEm securityBeanEm) {
            this.stockCodeSimple = securityBeanEm.getSecCode();
            this.market = securityBeanEm.getMarket();
            this.name = securityBeanEm.getName();
            this.bean = securityBeanEm;
            if (bean.isIndex()) {
                this.type = 0;
            } else {
                if (yesterdayHolds.contains(bean)) {
                    if (todaySelected.contains(bean)) {
                        this.type = 3;
                    } else {
                        this.type = 2;
                    }
                } else {
                    if (todaySelected.contains(bean)) {
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
            builder.append(stockCodeSimple);
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
            return JSONUtil.toJsonPrettyStr(bean.getConvertRawJsonObject());
        }
    }
}
