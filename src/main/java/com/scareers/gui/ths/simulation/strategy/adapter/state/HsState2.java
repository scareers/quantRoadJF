package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.BkStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.FundamentalStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.IndexStateHs;
import com.scareers.gui.ths.simulation.strategy.adapter.state.sub.StockStateHs;
import lombok.Data;

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
public class HsState2 {

    public static void main(String[] args) throws Exception {
        DefaultStatesPool.initManualSelector();

        StockStateHs stockStateHs = new StockStateHs(SecurityBeanEm.createStock("000001"));
        HsState2 state = new HsState2();
        state.setStockStateHs(stockStateHs);
        Console.log(state.getStockStateHs().getCdfRateForPosition());

    }

    //    protected HsFactor factorInfluenceMe; //  被哪个因子影响而刷新?
    protected HsState2 preState; // 维护前一状态对象.

    protected BkStateHs bkStateHs;
    protected StockStateHs stockStateHs;
    protected IndexStateHs indexStateHs;
    protected FundamentalStateHs fundamentalStateHs;

}
