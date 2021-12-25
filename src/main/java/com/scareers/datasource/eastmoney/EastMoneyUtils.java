package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.*;
import static com.scareers.utils.JsonUtil.jsonStrToDf;


/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:31:30
 */
public class EastMoneyUtils {
    // dc 代码查询结果全缓存, 且线程安全, 因常线程池批量调用
    public static ConcurrentHashMap<String, JSONArray> quoteCache = new ConcurrentHashMap<>();

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
     * 查询 em 通用的资产代码.
     * // @noti: 异常调用方处理;  不直接调用. 使用线程池执行
     * // @noti: 将会重试三次
     *
     * @param simpleCode 给定代码, 查询前3资产结果
     * @return
     */
    public static JSONArray querySecurityId(String simpleCode) {
        // {"QuotationCodeTable":{"Data":[{"Code":"000001","Name":"平安银行","PinYin":"PAYH","ID":"0000012","JYS":"6","Classify":"AStock","MarketType":"2","SecurityTypeName":"深A","SecurityType":"2","MktNum":"0","TypeUS":"6","QuoteID":"0.000001","UnifiedCode":"000001","InnerCode":"15855238340410"}],"Status":0,"Message":"成功","TotalPage":7,"TotalCount":7,"PageIndex":1,"PageSize":1,"Keyword":"000001","RelatedWord":"","SourceName":"QuotationCodeTable","SourceId":14,"ScrollId":""}}
        // https://searchapi.eastmoney.com/api/suggest/get?input=000001&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&count=1
        log.debug("em querySecurityId: {}", simpleCode);
        JSONArray res = quoteCache.get(simpleCode);
        if (res != null) {
            return res;
        }
        String url = "https://searchapi.eastmoney.com/api/suggest/get";
        Map<String, Object> params = new HashMap<>();
        params.put("input", simpleCode);
        params.put("type", 14);
        params.put("token", "D43BF722C8E33BDC906FB84D85E326E8");
        params.put("count", 10);

        String response = null;
        for (int i = 0; i < 3; i++) {
            try {
                response = getAsStr(url, params, 2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (response != null) {
                break;
            }
        }
        if (response == null) {
            return null;
        }

        JSONObject temp = JSONUtil.parseObj(response);
        Object resTemp = temp.getByPath("QuotationCodeTable.Data");
        if (JSONNull.NULL.equals(resTemp)) {
            return null;
        }
        res = (JSONArray) resTemp;
        quoteCache.put(simpleCode, res);
        return res;
    }

    /**
     * 批量查询简单股票代码, 获取 em 查询结果.  返回  EmSecurityIdBean 对象!!
     * 因多为http请求, 使用线程池
     * // 自动取前6位代码
     *
     * @param simpleCodes
     * @return
     */
    public static List<EmSecurityIdBean> querySecurityIdsToBeans(List<String> simpleCodes)
            throws ExecutionException, InterruptedException {
        simpleCodes = simpleCodes.stream().map(value -> value.substring(0, 6)).collect(Collectors.toList());
        List<EmSecurityIdBean> beans = new CopyOnWriteArrayList<>();
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(8,
                16 * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                ThreadUtil.newNamedThreadFactory("EmIdQueryPool-", null, true)
        );
        ConcurrentHashMap<String, Future<JSONArray>> futures = new ConcurrentHashMap<>();
        for (String simpleCode : simpleCodes) {
            Future<JSONArray> future = poolExecutor.submit(new Callable<JSONArray>() {
                @Override
                public JSONArray call() throws Exception {
                    JSONArray res = null;
                    try {
                        res = querySecurityId(simpleCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return res;
                }
            });
            futures.put(simpleCode, future);
        }
        for (String stockCodeSimple : futures.keySet()) {
            // 可能null, 但是具体构建时, 会再次对 查询结果进行null check, 直到获取成功
            JSONArray temp = futures.get(stockCodeSimple).get();
            if (temp != null) {
                beans.add(new EmSecurityIdBean(stockCodeSimple, temp));
            } else {
                log.warn("fail em stockId query: 东方财富股票id查询失败: {}!", stockCodeSimple);
            }
        }
        poolExecutor.shutdown();
        return beans;
    }

    private static final Log log = LogUtils.getLogger();
}
