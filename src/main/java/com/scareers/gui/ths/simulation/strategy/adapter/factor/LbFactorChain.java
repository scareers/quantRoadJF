package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.strategy.adapter.state.LbState;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 低买因子链.  该链维护因子列表, 以及每一步状态变化的 状态列表! 可直接作为 bean, 以访问数据
 *
 * @author: admin
 * @date: 2022/2/20/020-18:19:40
 */
public class LbFactorChain {
    private List<LbFactor> factorList = new ArrayList<>(); // 单线程语义
    private List<LbState> lbStates = new ArrayList<>(); // 初始状态 + 每个因子按序影响后的状态  // 末尾即为最终状态

    /**
     * 添加因子
     *
     * @param factor
     */
    public void addFactor(LbFactor factor) {
        factorList.add(factor);
    }

    /**
     * 需要给定初始默认状态
     *
     * @param initialState
     */
    public LbFactorChain(LbState initialState) {
        this.lbStates.add(initialState);
    }

    /**
     * 获取当前最新的状态
     *
     * @return
     */
    public LbState getNewestState() {
        return lbStates.get(lbStates.size() - 1);
    }

    /**
     * 核心方法, 对初始状态应用所有因子, 影响后, 得到新状态.
     *
     * @return
     */
    public void applyFactorInfluence() {
        if (factorList.size() == 0) {
            log.warn("HsFactorChain: 因子列表为空, 状态列表不变, 仅含初始状态");
        } else {
            for (LbFactor lbFactor : factorList) {
                // 对旧状态, 调用因子影响, 获取新状态对象, 加入 状态列表. 注意各状态有深复制语义
                LbState oldState = getNewestState(); // 获取最新的状态
                LbState newState = LbState.copyFrom(oldState); // 新状态, 尚未影响, 但是新深复制对象
                newState = lbFactor.influence(newState); // 执行影响, 真正刷新状态对象
                newState.setFactorInfluenceMe(lbFactor); // 单纯属性设置
                lbStates.add(newState); // 添加结果
            }
        }
    }

    private static final Log log = LogUtil.getLogger();
}
