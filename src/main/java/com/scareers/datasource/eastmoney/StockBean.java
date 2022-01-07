package com.scareers.datasource.eastmoney;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.querySecurityId;

/**
 * description: 代表一特定证券/指数等资产. 仅简单两字段. 基类. 常使用 EmSecurityBean
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StockBean {
    private static final long serialVersionUID = 156415111L;

    public static List<StockBean> toStockList(List<StockBean> beans) throws Exception {
        for (StockBean bean : beans) {
            bean.convertToStock();
        }
        return beans; // 列表不变
    }

    public static List<StockBean> toIndexList(List<StockBean> beans) throws Exception {
        for (StockBean bean : beans) {
            bean.convertToIndex();
        }
        return beans;
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
        STOCK, // 已转换为股票
        INDEX // 已转换为指数
    }


    public StockBean(JSONArray queryResults) {
        this.queryResults = queryResults;
        checkQueryResults(); // 若null将强制查询
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
            if (!convert(Arrays.asList("AStock", "23"))) {
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
                throw new Exception("转换StockBean为指数Bean异常");
            }
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

    public StockBean(String SecId) { // 1.000001
        Objects.requireNonNull(SecId);
        List<String> fragments = StrUtil.split(SecId, ".");
        this.stockCodeSimple = fragments.get(1);
        this.market = Integer.valueOf(fragments.get(0));
    }
}
