package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.core.util.ObjectUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.strategy.adapter.state.LbHsState;

import java.util.Objects;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/20/020-17:27:23
 */
public class GlobalIndexPricePercentRealtimeFactor extends LbHsFactor {
    public static final String factorName = "GlobalIndexPricePercentRealtimeFactor";
    public static final String nameCn = "大盘指数实时涨跌幅因子";
    public static final String description = "读取个股所属大盘指数实时涨跌幅, 该涨跌幅, 主要对分布的平均值进行左右平移操作";

    @Override
    public LbHsState influence() {
        Objects.requireNonNull(state, "初始状态不可为null, 设置后方可调用influence进行影响");
        /*
        影响过程
         */
        SecurityBeanEm bean = state.getBean();
        // 所属大盘指数,默认上证指数, 若非沪A,深A, 将log错误, 但默认使用上证指数
        SecurityBeanEm indexBelong = SecurityBeanEm.SHANG_ZHENG_ZHI_SHU;
        if (bean.isShenA()) {
            indexBelong = SecurityBeanEm.SHEN_ZHENG_CHENG_ZHI;
        } else if (!bean.isHuA()) {
            log.error("GlobalIndexPricePercentRealtimeFactor: 股票不属于沪深A股: {} - {}", bean.getSecCode(), bean.getName());
        }

        Double newPriceIndex = FsTransactionFetcher.getNewestPrice(indexBelong);
        Double preCloseOfIndex = EmQuoteApi.getPreCloseAndTodayOpenOfIndexOrBK(indexBelong, 2000, 3).get(0);
        if (newPriceIndex == null || preCloseOfIndex == -1.0) { // 数据缺失
            log.warn("GlobalIndexPricePercentRealtimeFactor: 指数昨收或最新价格获取失败: 最新价: {}, 昨收价: {} ", newPriceIndex,
                    preCloseOfIndex);
            log.error("GlobalIndexPricePercentRealtimeFactor: 指数涨跌幅获取失败, 无法计算影响, 返回原始状态");
            return state;
        }

        double changePercent = newPriceIndex / preCloseOfIndex - 1; // 变化百分比

        state.moveLowBuyPdf(changePercent);
        state.moveHighSellPdf(changePercent); // 实际影响逻辑

        this.state.addFactorWhofluenced(this); // 可不绝对, 保留灵活性
        return this.state;
    }

    /**
     * @noti 必须显式调用 setState, 设置原始状态
     */
    public GlobalIndexPricePercentRealtimeFactor() {
        super(null, factorName, nameCn, description);
    }
}
