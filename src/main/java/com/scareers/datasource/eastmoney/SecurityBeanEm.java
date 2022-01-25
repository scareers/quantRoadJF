package com.scareers.datasource.eastmoney;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;
import lombok.Data;
import org.jfree.chart.util.HMSNumberFormat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityId;
import static com.scareers.datasource.eastmoney.EastMoneyUtil.querySecurityIdsToBeanList;

/**
 * description: 代表一特定证券/指数等资产. Em 东方财富.
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@Data
public class SecurityBeanEm {
    @Override
    public int hashCode() {
        return this.getStockCodeSimple().hashCode() | this.getMarket().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SecurityBeanEm) {
            SecurityBeanEm other = (SecurityBeanEm) obj;
            return other.getStockCodeSimple().equals(this.getStockCodeSimple()) &&
                    other.getMarket().equals(this.getMarket());
        }
        return false;
    }

    private static final long serialVersionUID = 156415111L;
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
            beanPool.put(bean.getStockCodeSimple() + "__stock", bean); // 放入缓存池
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
            beanPool.put(bean.getStockCodeSimple() + "__index", bean); // 放入缓存池
        }
        return beans;
    }

    /**
     * @param stockListSimple
     * @return
     * @throws Exception
     * @noti 仅构建列表, 并未转换,  转换需要调用 toStockList / toIndexList 方法
     */
    public static List<SecurityBeanEm> queryBatchStockWithoutConvert(List<String> stockListSimple) throws Exception {
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


    String stockCodeSimple;
    Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.
    // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
    private JSONArray queryResults; // 全部查询结果, 以下为结果字段
    // Code --> stockCodeSimple, MktNum--> market , QuoteID --> secId
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

    private ConvertState convertState = ConvertState.NULL;

    /**
     * 表示当前已转换类型! 保证仅转换一次!
     */
    public enum ConvertState {
        NULL, // 尚未转换
        FAIL, // 转换失败
        STOCK, // 已转换为股票
        INDEX // 已转换为指数
    }

    /**
     * 给定查询结果构造
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
     * @param stockCodeSimple
     */
    private SecurityBeanEm(String stockCodeSimple) {
        this.stockCodeSimple = stockCodeSimple; // 将被查询
        checkQueryResults();
    }

    private void checkQueryResults() { // 死循环查询 3次
        int retry_ = 0;
        while (queryResults == null) {
            try {
                this.queryResults = querySecurityId(stockCodeSimple);
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
     * 调用后均需要判定转换成功与否!
     *
     * @return
     * @noti: 不新建对象
     */
    public SecurityBeanEm convertToStock() throws Exception {
        if (convertState != ConvertState.STOCK) {
            if (convert(Arrays.asList("AStock", "23"))) {
                convertState = ConvertState.STOCK;
            } else {
                convertState = ConvertState.FAIL;
                throw new Exception("转换StockBean为股票Bean异常");
            }
        }
        return this;
    }

    /**
     * 尝试从查询结果中, 读取到指数结果, 填充各个字段, 然后返回this
     *
     * @return
     */
    public SecurityBeanEm convertToIndex() throws Exception {
        if (convertState != ConvertState.INDEX) {
            if (!convert(Arrays.asList("Index"))) {
                convertState = ConvertState.FAIL;
                throw new Exception("转换StockBean为指数Bean异常");
            }
            convertState = ConvertState.INDEX;
        }
        return this;
    }

    private boolean convert(List<String> typeConditions) {
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if (typeConditions.contains(ele.get("Classify").toString())) {
                // 三项基本
                try {
                    secId = ele.get("QuoteID").toString();
                    stockCodeSimple = ele.get("Code").toString();
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
        return this.convertState == ConvertState.INDEX;
    }

    public boolean isStock() {
        return this.convertState == ConvertState.STOCK;
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

}
