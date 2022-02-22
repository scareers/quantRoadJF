package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;

/**
 * description: 展示 资产 某类数据的 Panel.
 * 主要被各具体展示数据继承
 * 例如展示df, 展示行情文字等
 *
 * @author: admin
 * @date: 2022/2/18/018-14:32:20
 */
public abstract class SecurityDisplayPanel extends DisplayPanel {
    protected SecurityBeanEm newBean; // 最新bean

    public void update(SecurityBeanEm currentBean) {
        this.newBean = currentBean;
        update();
    }
}
