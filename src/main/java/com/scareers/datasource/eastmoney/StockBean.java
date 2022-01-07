package com.scareers.datasource.eastmoney;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.querySecurityId;
import static com.scareers.datasource.eastmoney.EastMoneyUtils.querySecurityIdsToBeanList;

/**
 * description: 代表一特定证券/指数等资产. 仅简单两字段. 基类. 常使用 EmSecurityBean
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@Data
public class StockBean {
    private static final long serialVersionUID = 156415111L;

    /**
     * 给定股票简单代码列表, 获取 已转换为 股票 的 StockBean
     * // 本身已经是 CopyOnWriteArrayList
     *
     * @param beans
     * @return
     * @throws Exception
     */
    public static List<StockBean> createStockList(List<String> stockListSimple) throws Exception {
        List<StockBean> beans = queryBatchStockWithoutConvert(stockListSimple);
        for (StockBean bean : beans) {
            bean.convertToStock();
        }
        return beans; // 列表不变
    }

    /**
     * 给定股票简单代码列表, 获取 已转换为 指数 的 StockBean
     *
     * @param stockListSimple
     * @return
     * @throws Exception
     */
    public static List<StockBean> createIndexList(List<String> stockListSimple) throws Exception {
        List<StockBean> beans = queryBatchStockWithoutConvert(stockListSimple);
        for (StockBean bean : beans) {
            bean.convertToIndex();
        }
        return beans;
    }

    /**
     * @param stockListSimple
     * @return
     * @throws Exception
     * @noti 仅构建列表, 并未转换,  转换需要调用 toStockList / toIndexList 方法
     */
    public static List<StockBean> queryBatchStockWithoutConvert(List<String> stockListSimple) throws Exception {
        return querySecurityIdsToBeanList(stockListSimple); // 使用线程池
    }

    private static final Log log = LogUtils.getLogger();
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
    private enum ConvertState {
        NULL, // 尚未转换
        FAIL, // 转换失败
        STOCK, // 已转换为股票
        INDEX // 已转换为指数
    }


    public StockBean(JSONArray queryResults) {
        this.queryResults = queryResults;
        checkQueryResults(); // 若null将强制查询
    }

    public StockBean(String stockCodeSimple) {
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
    public StockBean convertToStock() throws Exception {
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
    public StockBean convertToIndex() throws Exception {
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

    @Override
    public boolean equals(Object o) {

    }

}
