package com.scareers.gui.ths.simulation.strategy.adapter.state;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.LbFactor;
import com.scareers.gui.ths.simulation.strategy.stockselector.LbHsSelector;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 表示个股欲低买高卖时, 所有影响仓位的 参数集;
 *
 * @key3 注意当所有属性传递而来, 应当均 使用 "深复制" 语义. 否则将修改原始默认分布, 严重bug;
 * @key3 只可以使用工厂方法 创建默认状态.
 * @author: admin
 * @date: 2022/2/20/020-16:57:17
 */
@Getter
@Setter
public class LbState {
    protected SecurityBeanEm bean; // 哪只股票的状态?
    protected LbFactor factorInfluenceMe; //  被哪个因子影响而刷新?

    /**
     * 低买分布tick, 与pdf,cdf
     */
    protected List<Double> ticksOfLowBuy;
    protected List<Double> weightsOfLowBuy; // 44数据
    protected List<Double> cdfOfLowBuy;

    LbState() {
    }

    /*
     * 一些常用的影响方法, 例如 tick左右移(对应pdf右左移), pdf 变形, cdf重新计算 等
     */

    /**
     * 左右移动pdf, 相当于右左移动 ticks // + 变为 -
     * 当 distance为 正数, 表示右移, 为负数, 则左移
     *
     * @param distance
     */
    public void movePdf(Double distance) {
        ticksOfLowBuy = ticksOfLowBuy.stream().map(value -> value - distance).collect(Collectors.toList());
    }

    /**
     * todo: 可考虑单股票仅维护单个状态对象, 创建对象池. 但是无法保证多线程之下 逻辑安全. 因此目前使用无脑深复制实现
     *
     * @param bean
     * @param selector
     * @return
     */
    public static LbState createDefaultLbState(SecurityBeanEm bean, LbHsSelector selector) {
        Assert.isTrue(bean.isStock());
        LbState state = new LbState();
        state.setBean(bean); // 唯一, 无需深复制

        state.setTicksOfLowBuy(ObjectUtil.cloneByStream(selector.getTicksOfLowBuy()));
        state.setWeightsOfLowBuy(ObjectUtil.cloneByStream(selector.getWeightsOfLowBuy()));
        state.setCdfOfLowBuy(ObjectUtil.cloneByStream(selector.getCdfOfLowBuy()));

        return state;
    }

    public static LbState copyFrom(LbState oldState) {
        LbState state = new LbState();
        state.setBean(oldState.getBean()); // bean 不变, 不需要深复制
        state.setTicksOfLowBuy(ObjectUtil.cloneByStream(oldState.getTicksOfLowBuy()));
        state.setWeightsOfLowBuy(ObjectUtil.cloneByStream(oldState.getWeightsOfLowBuy()));
        state.setCdfOfLowBuy(ObjectUtil.cloneByStream(oldState.getCdfOfLowBuy()));
        // 完全复制过来
        // 当被因子影响后, 设置因子字段, 并以复制来的状态为基础更新.
        return state;
    }

}
