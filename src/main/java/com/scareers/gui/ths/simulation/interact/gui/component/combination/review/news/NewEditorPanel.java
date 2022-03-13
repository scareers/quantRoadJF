package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.tools.stockplan.bean.SimpleNewEm;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-10:37:30
 */
public class NewEditorPanel extends DisplayPanel {
    SimpleNewEm bean;

    public NewEditorPanel() {

    }

    public void update(SimpleNewEm bean) {
        this.bean = bean;
        this.update();
    }

    @Override
    protected void update() {

    }
}
