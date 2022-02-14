package com.scareers.datasource.eastmoney;

/**
 * description: 全局唯一股票池! 以下功能均访问此唯一股票(指数/概念)池
 * 1.FsFetcher: 将获取 股票/指数/概念 1分钟分时图
 * 2.FsTransactionFetcher: 获取个股/指数/概念 tick成交数据. 通常3s/5s每秒
 * 3.Strategy: 主策略相关子类, 含选股策略等, 将可能动态填充本类股票池!
 * 4.Trader/TraderGui: 可在交易过程中, 手动添加股票池
 *
 * @author: admin
 * @date: 2022/2/14/014-17:05:04
 */
public class SecurityPool {
}
