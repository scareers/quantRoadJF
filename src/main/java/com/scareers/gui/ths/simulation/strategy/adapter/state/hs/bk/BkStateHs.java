package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.bk;

import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import lombok.Data;

import java.io.Serializable;

/**
 * description: 板块状态
 *
 * @author: admin
 * @date: 2022/2/25/025-18:43:25
 */
@Data
public class BkStateHs implements Serializable {
    private static final long serialVersionUID = 15619125420L;

    private transient HsState parent;
    String attr = "test";
}
