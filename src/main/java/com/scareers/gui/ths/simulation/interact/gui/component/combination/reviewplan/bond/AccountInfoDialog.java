package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import com.scareers.gui.ths.simulation.interact.gui.TraderGui;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

/**
 * description: 账户信息对话框.
 *
 * @author: admin
 * @date: 2022/6/11/011-10:24:40
 */
@Getter
@Setter
public class AccountInfoDialog extends JDialog {
    ReviseAccountWithOrder account;

    public AccountInfoDialog(Frame owner, String title, boolean modal, ReviseAccountWithOrder account) {
        super(owner, title, modal);
        this.setSize(1200, 800);
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout());
        jPanel.add(new JLabel(account.toString()), BorderLayout.CENTER);
        this.setContentPane(jPanel);
        this.setLocationRelativeTo(TraderGui.INSTANCE);
    }
}
