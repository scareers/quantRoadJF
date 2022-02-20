package com.scareers.gui.ths.simulation.strategy.adapter.factor;

import cn.hutool.log.Log;
import com.scareers.gui.ths.simulation.strategy.adapter.state.LbState;
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
public abstract class LbFactor {
    protected static final Log log = LogUtil.getLogger();
    protected String name;
    protected String nameCn;
    protected String description;


    protected LbState state;

    public LbFactor(String name, String nameCn, String description,
                    LbState state) {
        this.name = name;
        this.nameCn = nameCn;
        this.description = description;
        this.state = state;
    }

    public abstract LbState influence();
}
