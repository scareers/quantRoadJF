package com.scareers.gui.ths.simulation.strategy.adapter.factor.index;

import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.utils.log.LogUtil;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/20/020-21:26:40
 */
public class SettingsOfIndexPercentFactor {
    private static final Log log = LogUtil.getLogger();
    public static final String factorName = "GlobalIndexPricePercentRealtimeFactor";
    public static final String nameCn = "大盘指数实时涨跌幅因子";
    public static final String description = "读取个股所属大盘指数实时涨跌幅, 该涨跌幅, 主要对分布的平均值进行左右平移操作";

    public static Double getIndexPercent(SecurityBeanEm bean) {
        SecurityBeanEm indexBelong = SecurityBeanEm.SHANG_ZHENG_ZHI_SHU;
        if (bean.isShenA()) {
            indexBelong = SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI;
        } else if (!bean.isHuA()) {
            log.error("GlobalIndexPricePercentRealtimeFactor: 股票不属于沪深A股: {} - {}", bean.getSecCode(), bean.getName());
        }
        Double newPriceIndex = FsTransactionFetcher.getNewestPrice(indexBelong);
        Double preCloseOfIndex = EmQuoteApi.getPreCloseAndTodayOpenOfIndexOrBK(indexBelong, 2000, 3).get(0);
        if (newPriceIndex == null || preCloseOfIndex == -1.0) {
            log.warn("GlobalIndexPricePercentRealtimeFactor: 指数昨收或最新价格获取失败: 最新价: {}, 昨收价: {} ", newPriceIndex,
                    preCloseOfIndex);
            return null;
        }
        return newPriceIndex / preCloseOfIndex - 1;
    }
}
