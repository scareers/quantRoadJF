package com.scareers.gui.ths.simulation.strategy.adapter.state.index;

import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.log.LogUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * description: 大盘指数状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:15
 */
@Data
public class IndexStateHs implements Serializable {
    private static final Log log = LogUtil.getLogger();
    private static final long serialVersionUID = 7822512015L;
    private transient HsState parent;

    /*
    自动初始化
     */
    protected transient SecurityBeanEm indexBean; // 指数对象
    protected Double indexNewPrice; // 指数最新价格
    protected Double indexPreClosePrice; // 指数昨收
    protected Double indexPriceChgPtCurrent; // 指数当前涨跌幅.  change percent --> ChgPt

    /**
     * 构造器传递bean,为初始化指数相关数据; 但不持有bean指针. 仅 StockStateHs 持有
     *
     * @param beanEm
     */
    public IndexStateHs(SecurityBeanEm beanEm) {
        initIndexBean(beanEm);
        indexNewPrice = FsTransactionFetcher.getNewestPrice(indexBean); // 获取指数价格
        indexPreClosePrice = EmQuoteApi.getPreCloseOfIndexOrBK(indexBean, 3000, 3, true);
        initIndexChgPt();
    }

    private void initIndexChgPt() {
        if (indexNewPrice == null || indexPreClosePrice == null) {
            log.warn("指数昨收或最新价格获取失败,无法计算涨跌幅: 最新价: {}, 昨收价: {} ", indexNewPrice, indexPreClosePrice);
        } else {
            indexPriceChgPtCurrent = indexNewPrice / indexPreClosePrice - 1;
        }
    }

    private void initIndexBean(SecurityBeanEm beanEm) {
        indexBean = SecurityBeanEm.SHANG_ZHENG_ZHI_SHU;
        if (beanEm.isShenA()) {
            indexBean = SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI;
        } else if (!indexBean.isHuA()) {
            log.error("股票不属于沪深A股,默认使用上证指数: {} - {}", indexBean.getSecCode(), indexBean.getName());
        }
    }


}
