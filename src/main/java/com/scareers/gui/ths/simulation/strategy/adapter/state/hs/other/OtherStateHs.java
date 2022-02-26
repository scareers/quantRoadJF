package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.other;

import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import lombok.Data;

import java.io.Serializable;

/**
 * description: 其他, 例如外围
 *
 * @author: admin
 * @date: 2022/2/25/025-18:44:10
 */
@Data
public class OtherStateHs implements Serializable {
    private transient HsState parent;
    private static final long serialVersionUID = 56812125L;
    String attr = "test";
}
