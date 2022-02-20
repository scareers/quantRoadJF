package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.strategy.adapter.state.LbHsState;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/20/020-18:19:40
 */
public class LbHsFactorChain {
    private List<LbHsFactor> factorList = new ArrayList<>(); // 单线程语义
    private LbHsState initialState; // 初始状态

    /**
     * 添加因子
     *
     * @param factor
     */
    public void addFactor(LbHsFactor factor) {
        factorList.add(factor);
    }

    /**
     * 需要给定初始默认状态
     *
     * @param initialState
     */
    public LbHsFactorChain(LbHsState initialState) {
        this.initialState = initialState;
    }

    /**
     * 核心方法, 对初始状态应用所有因子, 影响后, 得到新状态.
     * 对状态的修该
     *
     * @return
     */
    public LbHsState applyFactorInfluence() {
        if (factorList.size() == 0) {
            log.warn("LbHsFactorChain: 因子列表为空, 返回初始状态");
            return initialState;
        }

        for (LbHsFactor lbHsFactor : factorList) {
            lbHsFactor.setState(initialState); // 设置状态
            initialState = lbHsFactor.influence(); // 影响状态并保存, 将作为下一个因子影响的初始状态
        }
        return initialState;
    }

    private static final Log log = LogUtil.getLogger();
}
