package com.scareers.gui.ths.simulation.strategy.adapter.factor.base;

import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.utils.log.LogUtil;

import static com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi.getQuoteHistorySingle;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/21/021-19:11:25
 */
public class SettingsOfBaseDataFactor {
    private static final Log log = LogUtil.getLogger();
    public static final String factorName = "BaseDataFactorHs";
    public static final String nameCn = "基本数据项设置因子";
    public static final String description = "读取个股相关基本数据项,一般只做初始化设置;本因子一般在因子链头部";

    /**
     * 获取前2天收盘价 前复权, 作为基准
     *
     * @param stock
     * @param pre2TradeDate
     * @return
     */
    public static Double getPreNDayClosePriceQfq(String stock, String pre2TradeDate) {
        Double pre2ClosePrice = null;
        try {
            // 已经缓存
            //日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	   振幅	   涨跌幅	   涨跌额	  换手率	  资产代码	资产名称
            pre2ClosePrice = Double.valueOf(getQuoteHistorySingle(true, SecurityBeanEm.createStock(stock),
                    pre2TradeDate,
                    pre2TradeDate, "101", "qfq", 3, 2000).row(0).get(2).toString());
        } catch (Exception e) {
            log.error("skip: data get fail: 获取股票前日收盘价失败 {}, 将返回null", stock);
            e.printStackTrace();
        }
        return pre2ClosePrice;
    }
}
