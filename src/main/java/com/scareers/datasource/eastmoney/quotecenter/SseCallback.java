package com.scareers.datasource.eastmoney.quotecenter;

/**
 * description: 应对sse推送技术, 当读取到1行时, 对改行数据进行的处理回调
 *
 * @author: admin
 * @date: 2022/2/25/025-11:49:48
 */
interface SseCallback {
    public abstract void processLine(Object line);
}
