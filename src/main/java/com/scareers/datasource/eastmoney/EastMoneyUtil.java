package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.Header;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.scareers.annotations.Cached;
import com.scareers.utils.log.LogUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.*;


/**
 * description:
 * // noti: 不同的api, 因底层设计不同, 可能需要使用不同实现.
 *
 * @author: admin
 * @date: 2021/12/21/021-22:31:30
 */
public class EastMoneyUtil {
    // dc 代码查询结果全缓存, 且线程安全, 因常线程池批量调用  getAsStrUseHutool/getAsStrUseKevin 使用不同库. 具体api自己试
    public static ConcurrentHashMap<String, JSONArray> quoteCache = new ConcurrentHashMap<>();
    public static ThreadPoolExecutor poolExecutor;

    public static void checkPoolExecutor() {
        if (poolExecutor == null) {
            poolExecutor = new ThreadPoolExecutor(16, 32, 10000, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), ThreadUtil.newNamedThreadFactory("klineGet-", null, true));

        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Console.log(querySecurityId("000001"));


    }

    /**
     * 添加默认设定请求头
     *
     * @param request
     * @param timeout
     * @return
     */
    public static HttpRequest addDefaultSettings(HttpRequest request, int timeout) {
        return request.header("User-Agent", HEADER_VALUE_OF_USER_AGENT)
                .header("Accept", HEADER_VALUE_OF_ACCEPT)
                .header("Accept-Language", HEADER_VALUE_OF_ACCEPT_LANGUAGE)
                .header("Referer", HEADER_VALUE_OF_REFERER)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                ;
    }

    public static HttpRequest addDefaultSettings(HttpRequest request) {
        return addDefaultSettings(request, DEFAULT_TIMEOUT);
    }

    /**
     * get 快捷;  将最多重试三次抛出异常
     *
     * @param url
     * @param params
     * @param timeout
     * @return
     */
    public static String getAsStrUseHutool(String url, Map<String, Object> params, int timeout, int retry) {
        String res = null;
        int i = 0;
        while (true) {
            i++;
            try {
                cn.hutool.http.HttpRequest request =
                        new cn.hutool.http.HttpRequest(url)
                                .method(Method.GET)
                                .form(params)
                                .timeout(timeout)
                                .header(Header.ACCEPT, HEADER_VALUE_OF_ACCEPT)
                                .header(Header.USER_AGENT, HEADER_VALUE_OF_USER_AGENT)
                                .header(Header.ACCEPT_LANGUAGE, HEADER_VALUE_OF_ACCEPT_LANGUAGE)
                                .header(Header.REFERER, HEADER_VALUE_OF_REFERER);
                res = request.execute().body();
            } catch (Exception e) {
                if (i >= retry) {
                    throw e;
                }
            }
            if (res != null) {
                break;
            }
        }
        return res;
    }

    public static String getAsStrUseHutool(String url, Map<String, Object> params, int timeout) {
        return getAsStrUseHutool(url, params, timeout, 3);
    }

    /**
     * get 快捷
     *
     * @param url
     * @param params
     * @param timeout
     * @return
     */
    public static String getAsStrUseKevin(String url, Map<String, Object> params, int timeout, int retry) {
        String res = null;
        int i = 0;
        while (true) {
            i++;
            try {
                res = addDefaultSettings(HttpRequest.get(url), timeout).form(params).body();
            } catch (Exception e) {
                if (i >= retry) {
                    throw e;
                }
            }
            if (res != null) {
                break;
            }
        }
        return res;
    }

    public static String getAsStrUseKevin(String url, Map<String, Object> params, int timeout) {
        return getAsStrUseKevin(url, params, timeout, 3);
    }


    /**
     * 查询 em 通用的资产代码.
     * 已重试3次, 失败返回 null
     * // @noti: 异常调用方处理;  不直接调用. 使用线程池执行
     * // @noti: 将会重试三次
     *
     * @param simpleCode 给定代码, 查询前3资产结果
     * @return
     */
    @Cached
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
                response = getAsStrUseHutool(url, params, 2000);
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
     * 批量查询简单股票代码, 获取 em 查询结果.
     * 因多为http请求, 使用线程池
     * // 自动取前6位代码
     * <p>
     * 并未对结果进行解析, 使用需要调用
     *
     * @param simpleCodes
     * @return
     * @noti 仅构建列表, 并未转换
     * @see SecurityBeanEm.toStockList
     * @see SecurityBeanEm.toIndexList
     */
    public static List<SecurityBeanEm> querySecurityIdsToBeanList(List<String> simpleCodes)
            throws Exception {
        simpleCodes = simpleCodes.stream().map(value -> value.substring(0, 6)).collect(Collectors.toList());
        List<SecurityBeanEm> beans = new CopyOnWriteArrayList<>();
        checkPoolExecutor();
        ConcurrentHashMap<String, Future<JSONArray>> futures = new ConcurrentHashMap<>();
        for (String simpleCode : simpleCodes) {
            Future<JSONArray> future = poolExecutor.submit(() -> {
                JSONArray res = null;
                try {
                    res = querySecurityId(simpleCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return res;
            });
            futures.put(simpleCode, future);
        }
        for (String stockCodeSimple : futures.keySet()) {
            // 可能null, 但是具体构建时, 会再次对 查询结果进行null check, 直到获取成功
            JSONArray temp = futures.get(stockCodeSimple).get();
            if (temp != null) {
                beans.add(new SecurityBeanEm(temp));
            } else {
                log.error("skip: em stockId query: 东方财富股票id查询失败[将跳过此股票]: {}!", stockCodeSimple);
            }
        }
        return beans;
    }

    private static final Log log = LogUtil.getLogger();
}
