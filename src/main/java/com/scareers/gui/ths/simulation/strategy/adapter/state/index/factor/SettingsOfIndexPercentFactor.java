package com.scareers.gui.ths.simulation.strategy.adapter.state.index.factor;

import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.utils.log.LogUtil;

/**
 * description: lb/hs 共同使用属性 , 方法等
 *
 * @author: admin
 * @date: 2022/2/20/020-21:26:40
 */
public class SettingsOfIndexPercentFactor {
    private static final Log log = LogUtil.getLogger();
    public static final String factorNameHs = "IndexPricePercentFactorHs";
    public static final String nameCnHs = "大盘指数实时涨跌幅因子";
    public static final String descriptionHs = "读取个股所属大盘指数实时涨跌幅, 该涨跌幅, 主要对分布的平均值进行左右平移操作; \n" +
            "当大盘向好, 应当适当博取更高的价格再卖出; 当大盘极差, 应当更加快速卖出";



}
