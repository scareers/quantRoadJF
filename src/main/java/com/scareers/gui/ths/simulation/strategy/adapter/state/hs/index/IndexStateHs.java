package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.index;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.annotation.ManualModify;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.CustomizeStateArgsPoolHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.DefaultStateArgsPoolHs;
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

    // 因子设置
    protected Double parallelMoveValue; // 实际平移量

    // 默认值
    @ManualModify
    protected Boolean affectedByIndex; // 个股是否被index影响?

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

        affectedByIndex = CustomizeStateArgsPoolHs.affectedByIndexHsMap.getOrDefault(beanEm.getSecCode(),
                DefaultStateArgsPoolHs.affectedByIndexDefault); // 决定个股是否被指数影响 ??
    }

    private void initIndexChgPt() {
        if (indexNewPrice == null || indexPreClosePrice == null) {
            if (DateUtil.between(DateUtil.parse(DateUtil.today() + " 09:25:30"), DateUtil.date(), DateUnit.MS,
                    false) > 0) {
                // 大盘涨跌幅 9:25:x 刷新第一次. 那之前不打印log
                log.warn("指数昨收或最新价格获取失败,无法计算涨跌幅: 最新价: {}, 昨收价: {} ", indexNewPrice, indexPreClosePrice);
            }
        } else {
            indexPriceChgPtCurrent = indexNewPrice / indexPreClosePrice - 1;
        }
    }

    private void initIndexBean(SecurityBeanEm beanEm) {
        indexBean = SecurityBeanEm.getShangZhengZhiShu();
        if (beanEm.isShenA()) {
            indexBean = SecurityBeanEm.getShenZhengChengZhi();
        } else if (!beanEm.isHuA()) {
            log.error("股票不属于沪深A股,默认使用上证指数: {} - {}", beanEm.getSecCode(), beanEm.getName());
        }
    }


}
