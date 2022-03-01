package com.scareers.datasource.eastmoney.quotecenter;

/**
 * description: 应对sse推送技术, 当读取到1行时, 对改行数据进行的处理回调
 *
 * @author: admin
 * @date: 2022/2/25/025-11:49:48
 */
public interface SseCallback<T> {
    /**
     * 东财sse 类型的api, 回调. 需要提供泛型
     *
     * @param perData
     */
    public abstract void process(T perData);
}
