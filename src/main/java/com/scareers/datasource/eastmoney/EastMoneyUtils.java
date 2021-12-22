package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.*;
import static com.scareers.utils.JsonUtil.jsonStrToDf;


/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:31:30
 */
public class EastMoneyUtils {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Console.log(querySecurityId("300719"));
        GlobalThreadPool.shutdown(false);
    }

    /**
     * 添加默认设定请求头
     *
     * @param request
     * @param timeout
     * @return
     */
    public static HttpRequest addDefaultSettings(HttpRequest request, int timeout) {
        return request.header(Header.USER_AGENT, HEADER_VALUE_OF_USER_AGENT)
                .header(Header.ACCEPT, HEADER_VALUE_OF_ACCEPT)
                .header(Header.ACCEPT_LANGUAGE, HEADER_VALUE_OF_ACCEPT_LANGUAGE)
                .header(Header.REFERER, HEADER_VALUE_OF_REFERER)
                .timeout(timeout)
                ;
    }

    public static HttpRequest addDefaultSettings(HttpRequest request) {
        return request.header(Header.USER_AGENT, HEADER_VALUE_OF_USER_AGENT)
                .header(Header.ACCEPT, HEADER_VALUE_OF_ACCEPT)
                .header(Header.ACCEPT_LANGUAGE, HEADER_VALUE_OF_ACCEPT_LANGUAGE)
                .header(Header.REFERER, HEADER_VALUE_OF_REFERER)
                .timeout(DEFAULT_TIMEOUT)
                ;
    }

    /**
     * get 快捷
     *
     * @param url
     * @param params
     * @param timeout
     * @return
     */
    public static String getAsStr(String url, Map<String, Object> params, int timeout) {
        HttpRequest request = new HttpRequest(url);
        request = addDefaultSettings(request, timeout); // 默认请求头
        request.form(params); // 参数
        return request.execute().body();
    }

    /**
     * 查询dc 通用的资产代码.
     * // @noti: 异常调用方处理;
     *
     * @param simpleCode 给定代码, 查询前3资产id结果
     * @return
     */
    public static JSONArray querySecurityId(String simpleCode) throws ExecutionException, InterruptedException {
        Future<JSONArray> res = ThreadUtil.execAsync(() -> querySecurityIdCore(simpleCode));
        //        GlobalThreadPool.shutdown(false); // 关闭全局线程池
        return res.get();
    }


    /**
     * 查询dc 通用的资产代码.
     * // @noti: 异常调用方处理;  不直接调用. 使用线程池执行
     *
     * @param simpleCode 给定代码, 查询前3资产结果
     * @return
     */
    public static JSONArray querySecurityIdCore(String simpleCode) {
        // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
        // https://searchapi.eastmoney.com/api/suggest/get?input=000001&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=1
        String url = "https://searchapi.eastmoney.com/api/suggest/get";
        Map<String, Object> params = new HashMap<>();
        params.put("input", simpleCode);
        params.put("type", 14);
        params.put("token", "D43BF722C8E33BDC906FB84D85E326E8");
        params.put("count", 10);

        String response = getAsStr(url, params, 2000);
        JSONObject temp = JSONUtil.parseObj(response);
        return (JSONArray) temp.getByPath("QuotationCodeTable.Data");
    }


}
