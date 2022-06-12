package com.scareers.utils;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import java.io.OutputStream;

/**
 * description: 多为hutool http 接口的一些改写优化
 *
 * @author: admin
 * @date: 2022/6/12/012-11:40:51
 */
public class HttpUtilS {
    /**
     * 该api来自hutool, 不过timeout默认-1, 改写为可设置 timeout
     *
     * @param url
     * @param out
     * @param isCloseOut
     * @param streamProgress
     * @return
     */
    public static long download(String url, OutputStream out, boolean isCloseOut, StreamProgress streamProgress,
                                int timeout) {
        Assert.notNull(out, "[out] is null !");

        return requestDownload(url, timeout).writeBody(out, isCloseOut, streamProgress);
    }

    private static HttpResponse requestDownload(String url, int timeout) {
        Assert.notBlank(url, "[url] is blank !");

        final HttpResponse response = HttpUtil.createGet(url, true)
                .timeout(timeout)
                .executeAsync();

        if (response.isOk()) {
            return response;
        }

        throw new HttpException("Server response error with status code: [{}]", response.getStatus());
    }
}
