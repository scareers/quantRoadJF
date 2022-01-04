package com.scareers.gui.rabbitmq.order;

/**
 * description: 无特别参数的 查询类订单类型
 * * 构造无特别参数查询函数, 因 目前两个查询函数均不需要参数, 因此 NoArgs
 * * 当前可使用查询api:
 * * //----> get_hold_stocks_info    账号股票持仓汇总数据
 * * //----> get_account_funds_info  9项账号资金数据
 * * //----> get_unsolds_not_yet  获取当前尚未完全成交的挂单
 *
 * @author: admin
 * @date: 2021/12/23/023-19:37:36
 */
public class QueryDefaultArgsOrder extends Order {
    public static long DEFAULT_PRIORITY = Order.PRIORITY_LOW;

    public QueryDefaultArgsOrder(String orderType) {
        super(orderType);
        this.setPriority(DEFAULT_PRIORITY);
    }
}
