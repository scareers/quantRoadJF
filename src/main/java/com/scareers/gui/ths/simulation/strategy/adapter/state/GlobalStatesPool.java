package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.date.DateUtil;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;

import java.util.List;

/**
 * description: 全局唯一状态池. 一般初始化一些数据量不大的全局唯一变量. 各种属性一般不可变
 *
 * @author: admin
 * @date: 2022/2/25/025-19:33:07
 */
public class GlobalStatesPool {
    private GlobalStatesPool() {
    }

    public static List<String> todaySuspendStocks;

    static {
        initTodaySuspendStocks();
    }

    private static void initTodaySuspendStocks() {
        todaySuspendStocks = EmDataApi.getSuspensionStockCodes(DateUtil.today(), 3000, 10);
    }
}
