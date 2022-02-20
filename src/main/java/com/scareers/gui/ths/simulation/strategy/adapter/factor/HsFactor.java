package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * description: 低买高卖类策略, 影响因子 抽象类.
 * 例如: 大盘指数状态, 自身行业状态, 自身概念状态, 自身价格状态, 自身历史价格状态, 自身基本面状态等.
 *
 * @author: admin
 * @date: 2022/2/20/020-16:52:31
 */
@Getter
@Setter
public abstract class HsFactor {
    protected static final Log log = LogUtil.getLogger();
    protected String name;
    protected String nameCn;
    protected String description;


    protected HsState state;

    public HsFactor(String name, String nameCn, String description,
                    HsState state) {
        this.name = name;
        this.nameCn = nameCn;
        this.description = description;
        this.state = state;
    }

    public abstract HsState influence();
}
