package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.industryconcept;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import org.hibernate.engine.transaction.jta.platform.internal.BorlandEnterpriseServerJtaPlatform;

import javax.swing.*;
import java.awt.*;

/**
 * description: 行业和概念 操盘计划: 使用同花顺行业和概念
 *
 * @author: admin
 * @date: 2022/3/28/028-21:20:34
 */
public class IndustryConceptPanelForPlan extends DisplayPanel {
    private static IndustryConceptPanelForPlan INSTANCE;

    public static IndustryConceptPanelForPlan getInstance(MainDisplayWindow mainDisplayWindow) {
        if (INSTANCE == null) {
            INSTANCE = new IndustryConceptPanelForPlan(mainDisplayWindow);
        }
        return INSTANCE;
    }

    MainDisplayWindow mainDisplayWindow;


    public IndustryConceptPanelForPlan(MainDisplayWindow mainDisplayWindow) {
        this.mainDisplayWindow = mainDisplayWindow;
        JLabel jLabel = new JLabel("测试label");
        this.setLayout(new BorderLayout());

        this.add(jLabel, BorderLayout.CENTER);
    }

    @Override
    public void update() {

    }


    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }
}
