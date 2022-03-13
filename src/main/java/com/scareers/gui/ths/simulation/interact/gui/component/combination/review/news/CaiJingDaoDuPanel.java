package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import cn.hutool.core.lang.Console;
import org.jdesktop.swingx.JXHyperlink;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class CaiJingDaoDuPanel extends SimpleNewListPanel {
    public static CaiJingDaoDuPanel INSTANCE;

    public static CaiJingDaoDuPanel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CaiJingDaoDuPanel();
        }
        return INSTANCE;
    }
    JXHyperlink hyperlink;

    public CaiJingDaoDuPanel() {



    }

    @Override
    protected void update() {
        Console.log("财经导读被电击");
    }
}
