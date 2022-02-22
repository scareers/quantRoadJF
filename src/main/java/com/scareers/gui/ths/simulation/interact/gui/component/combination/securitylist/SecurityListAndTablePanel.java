package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDfDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import lombok.Getter;

import javax.swing.*;
import java.util.Vector;

/**
 * description: 展示控件
 * 语义为: panel, 展示资产列表, 以及它对应的 某项 df形式的数据(用Table展示df)
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
@Getter
public abstract class SecurityListAndTablePanel extends SecurityListAndDisplayPanel {


    protected SecurityListAndTablePanel(
            MainDisplayWindow mainDisplayWindow, int jListWidth) {
        super(mainDisplayWindow, jListWidth);
    }

    /**
     * 资产数据展示 df Panel ;  改变了展示内容的返回类型
     *
     * @return
     */
    @Override
    protected abstract SecurityDfDisplayPanel buildDisplayPanel();
    // new SecurityDfDisplayPanel(this, this.jListWidth);

}
