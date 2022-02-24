package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 高卖因子链.  该链维护因子列表, 以及每一步状态变化的 状态列表! 可直接作为 bean, 以访问数据
 *
 * @author: admin
 * @date: 2022/2/20/020-18:19:40
 */
@Getter
public class HsFactorChain {
    private List<HsFactor> factorList = new ArrayList<>(); // 单线程语义
    private List<HsState> hsStates = new ArrayList<>(); // 初始状态 + 每个因子按序影响后的状态  // 末尾即为最终状态

    /**
     * 添加因子
     *
     * @param factor
     */
    public void addFactor(HsFactor factor) {
        factorList.add(factor);
    }

    /**
     * 需要给定初始默认状态
     *
     * @param initialState
     */
    public HsFactorChain(HsState initialState) {
        this.hsStates.add(initialState);
    }

    /**
     * 获取当前最新的状态
     *
     * @return
     */
    public HsState getNewestState() {
        return hsStates.get(hsStates.size() - 1);
    }

    /**
     * 核心方法, 对初始状态应用所有因子, 影响后, 刷新新状态列表
     *
     * @return
     */
    public void applyFactorInfluence() {
        if (factorList.size() == 0) {
            log.warn("HsFactorChain: 因子列表为空, 状态列表不变, 仅含初始状态");
        } else {
            for (HsFactor hsFactor : factorList) {
                // 对旧状态, 调用因子影响, 获取新状态对象, 加入 状态列表. 注意各状态有深复制语义
                HsState oldState = getNewestState(); // 获取最新的状态
                HsState newState = HsState.copyFrom(oldState); // 新状态, 尚未影响, 但是新深复制对象
                newState.setPreState(oldState);
                newState = hsFactor.influence(newState); // 执行影响, 真正刷新状态对象
                newState.setFactorInfluenceMe(hsFactor); // 单纯属性设置
                hsStates.add(newState); // 添加结果
            }
        }
    }

    private static final Log log = LogUtil.getLogger();
}
