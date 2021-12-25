package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.querySecurityId;

/**
 * description: 东方财富通用 股票/指数 等资产, id
 * <p>
 * //@using: 查询该url, 可给定简单6位代码, 返回所有相关资产. 例如 个股/指数/基金
 * https://searchapi.eastmoney.com/api/suggest/get?input=000001&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=1
 * 响应: {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
 *
 * @author: admin
 * @date: 2021/12/22/022-17:40:47
 */

public class EmSecurityIdBean {
    private static final Log log = LogUtils.getLogger();
    // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
    String stockCodeSimple;
    JSONArray queryResults;


    public EmSecurityIdBean(String stockCodeSimple, JSONArray queryResults) {
        this.stockCodeSimple = stockCodeSimple;
        this.queryResults = queryResults;
    }

    private void checkQueryResults() throws ExecutionException, InterruptedException { // 死循环查询
        int retry = 0;
        while (queryResults == null) {
            try {
                this.queryResults = querySecurityId(stockCodeSimple);
            } catch (Exception e) {
                e.printStackTrace();
                log.warn("new EmSecurityIdBean fail: 构造器执行异常");
                if (retry >= 3) {
                    throw e;
                }
            }
            retry++;
        }
    }

    // "QuoteID" --> 0.000001 23科创板
    public String getAStockSecId() throws ExecutionException, InterruptedException {
        checkQueryResults();
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if ("AStock".equals(ele.get("Classify").toString()) || "23".equals(ele.get("Classify").toString())) {
                return ele.get("QuoteID").toString();
            }
        }
        return null;
    }

    public String getIndexSecId() throws ExecutionException, InterruptedException {
        checkQueryResults();
        for (int i = 0; i < queryResults.size(); i++) {
            JSONObject ele = queryResults.getJSONObject(i);
            if ("Index".equals(ele.get("Classify").toString())) {
                return ele.get("QuoteID").toString();
            }
        }
        return null;
    }

    public String getStockCodeSimple() {
        return stockCodeSimple;
    }

    public void setStockCodeSimple(String stockCodeSimple) {
        this.stockCodeSimple = stockCodeSimple;
    }

    public JSONArray getQueryResults() {
        return queryResults;
    }

    public void setQueryResults(JSONArray queryResults) {
        this.queryResults = queryResults;
    }

    public EmSecurityIdBean() {
    }
}
