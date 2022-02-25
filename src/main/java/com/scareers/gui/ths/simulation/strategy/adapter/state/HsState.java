package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.BkStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.FundamentalStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.IndexStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.StockStateHs;
import lombok.Data;

import java.io.Serializable;

/**
 * description: 表示个股欲低买高卖时, 所有影响仓位的 参数集;
 *
 * @noti 当添加任意属性, 应当同时完成 首次初始化 和 copyFrom 两大方法对应字段的 设置/复制
 * @key3 注意当所有属性传递而来, 应当均 使用 "深复制" 语义. 否则将修改原始默认分布, 严重bug;
 * @key3 只可以使用工厂方法 创建默认状态.
 * @author: admin
 * @date: 2022/2/20/020-16:57:17
 */
@Data
public class HsState implements Serializable {
    private static final long serialVersionUID = 105102100L;

    public static void main(String[] args) throws Exception {
        DefaultStatesPool.initManualSelector();

        StockStateHs stockStateHs = new StockStateHs(SecurityBeanEm.createStock("000001"));
        HsState state = new HsState(null, new BkStateHs(), stockStateHs, new IndexStateHs(),
                new FundamentalStateHs());
        Console.log(state);
        HsState state2 = copyFrom(state);
        Console.log(state2);


    }

    //
    protected transient HsFactor factorInfluenceMe; //  被哪个因子影响而刷新?
    protected HsState preState; // 维护前一状态对象.

    protected BkStateHs bkStateHs;
    protected StockStateHs stockStateHs;
    protected IndexStateHs indexStateHs;
    protected FundamentalStateHs fundamentalStateHs;

    public HsState(HsState preState, BkStateHs bkStateHs,
                   StockStateHs stockStateHs,
                   IndexStateHs indexStateHs,
                   FundamentalStateHs fundamentalStateHs) {
        this.preState = preState;
        this.bkStateHs = bkStateHs;
        this.stockStateHs = stockStateHs;
        this.indexStateHs = indexStateHs;
        this.fundamentalStateHs = fundamentalStateHs;
    }

    /**
     * copy 逻辑, 对于某些字段, 因为数据量大, 且类单例模式, transient 后, 深复制时将重置为null;
     * 它将提高复制速度.
     * 这些字段需要手动设置, 例如  SecurityBeanEm
     * 当然, 深复制的属性, 仅仅 equals, 对于这些手动设置的属性, 则还会 ==. 同一对象的多个指针
     *
     * @param oldState
     * @return
     */
    public static HsState copyFrom(HsState oldState) {
        HsState hsState = ObjectUtil.cloneByStream(oldState);
        hsState.getStockStateHs().setBean(oldState.getStockStateHs().getBean());
        hsState.getStockStateHs().setStockCode(oldState.getStockStateHs().getStockCode());
        hsState.getStockStateHs().setFsData(oldState.getStockStateHs().getFsData());
        hsState.getStockStateHs().setFsTransData(oldState.getStockStateHs().getFsTransData());
        return hsState;
    }
}
