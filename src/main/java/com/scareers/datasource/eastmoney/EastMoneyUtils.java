package com.scareers.datasource.eastmoney;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.*;


/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:31:30
 */
public class EastMoneyUtils {
    public static HttpRequest addDefaultSettings(HttpRequest request, int timeout) {
        return request.header(Header.USER_AGENT, HEADER_VALUE_OF_USER_AGENT)
                .header(Header.ACCEPT, HEADER_VALUE_OF_ACCEPT)
                .header(Header.ACCEPT_LANGUAGE, HEADER_VALUE_OF_ACCEPT_LANGUAGE)
                .header(Header.REFERER, HEADER_VALUE_OF_REFERER)
                .timeout(timeout)
                ;
    }

    public static HttpRequest addDefaultSettings(HttpRequest request) {
        return addDefaultSettings(request, DEFAULT_TIMEOUT);
    }

    public static HttpResponse get(String url) {
        return addDefaultSettings(HttpRequest.get(url)).execute();
    }



    public static String getAsStr(String url) {
        return get(url).body();
    }
}
