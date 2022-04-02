package com.scareers.gui.ths.simulation.interact.gui.component.simple;

import org.jdesktop.swingx.JXDialog;
import org.jdesktop.swingx.JXFindPanel;
import org.jdesktop.swingx.search.Searchable;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_MINOR;

/**
 * description: 源代码全部照抄 JXFindBar! 修改源代码, 重写少数方法, 设置文字背景色等
 *
 * @author: admin
 * @date: 2022/4/2/002-01:11:19
 */
public class JXFindBarS extends JXFindPanel {

    protected Color previousBackgroundColor = Color.black;

    protected Color previousForegroundColor = Color.red;

    // PENDING: need to read from UIManager
    protected Color notFoundBackgroundColor = Color.gray;

    protected Color notFoundForegroundColor = Color.green;

    protected JButton findNextS;

    protected JButton findPreviousS;

    public JXFindBarS() {
        this(Color.red);
    }

    Color findLabelColor;

    public JXFindBarS(Color findLabelColor) {
        super(null);
        getPatternModel().setIncremental(true);
        getPatternModel().setWrapping(true);
        this.setBackground(COLOR_THEME_MINOR);
        this.findLabelColor = findLabelColor;
    }


    public JXFindBarS(Searchable searchable) {
        super(searchable);
        getPatternModel().setIncremental(true);
        getPatternModel().setWrapping(true);
        this.setBackground(COLOR_THEME_MINOR);
        this.findLabelColor = Color.red;
    }

    @Override
    public void setSearchable(Searchable searchable) {
        super.setSearchable(searchable);
        match();
    }

    /**
     * here: set textfield colors to not-found colors.
     */
    @Override
    protected void showNotFoundMessage() {
        //JW: quick hack around #487-swingx - NPE in setSearchable
        if (searchField == null) return;
        searchField.setForeground(notFoundForegroundColor);
        searchField.setBackground(notFoundBackgroundColor);
    }

    /**
     * here: set textfield colors to normal.
     */
    @Override
    protected void showFoundMessage() {
        //JW: quick hack around #487-swingx - NPE in setSearchable
        if (searchField == null) return;
        searchField.setBackground(previousBackgroundColor);
        searchField.setForeground(previousForegroundColor);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (previousBackgroundColor == null) {
            previousBackgroundColor = searchField.getBackground();
            previousForegroundColor = searchField.getForeground();
        } else {
            searchField.setBackground(previousBackgroundColor);
            searchField.setForeground(previousForegroundColor);
        }
    }

    // --------------------------- action call back

    /**
     * Action callback method for bound action JXDialog.CLOSE_ACTION_COMMAND.
     * <p>
     * Here: does nothing. Subclasses can override to define custom "closing"
     * behaviour. Alternatively, any client can register a custom action with
     * the actionMap.
     */
    public void cancel() {
    }

    // -------------------- init

    @Override
    protected void initExecutables() {
        getActionMap().put(JXDialog.CLOSE_ACTION_COMMAND,
                createBoundAction(JXDialog.CLOSE_ACTION_COMMAND, "cancel"));
        super.initExecutables();
    }

    @Override
    protected void bind() {
        super.bind();
        searchField
                .addActionListener(getAction(JXDialog.EXECUTE_ACTION_COMMAND));
        findNextS.setAction(getAction(FIND_NEXT_ACTION_COMMAND));

        findPreviousS.setAction(getAction(FIND_PREVIOUS_ACTION_COMMAND));
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke,
                JXDialog.CLOSE_ACTION_COMMAND);
    }

    @Override
    protected void build() {
        setLayout(new FlowLayout(SwingConstants.LEADING));
        add(searchLabel);
//        add(new JLabel(":"));
        add(new JLabel("  "));
        searchField.setCaretColor(Color.red);
        searchField.setPreferredSize(new Dimension(searchField.getWidth(), 25));
        searchField.setBorder(null);
        add(searchField);
        add(findNextS);
        add(findPreviousS);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        findNextS = new JButton() {
            @Override
            public void setText(String text) {
                super.setText("下一个"); // 强制文字, 无视参数
            }
        };
        findPreviousS = new JButton() {
            @Override
            public void setText(String text) {
                super.setText("上一个"); // 强制文字, 无视参数
            }
        };

        findNextS.setBackground(Color.black);
        findNextS.setBorderPainted(false); // 无边框
        findNextS.setForeground(Color.red);
        findNextS.setPreferredSize(new Dimension(50, 25));

        findPreviousS.setBackground(Color.black);
        findPreviousS.setBorderPainted(false); // 无边框
        findPreviousS.setForeground(Color.red);
        findPreviousS.setPreferredSize(new Dimension(50, 25));

    }


    /**
     * @param locale
     */
    @Override
    protected void bindSearchLabel(Locale locale) {
        searchLabel.setText("查找 :");
        searchLabel.setForeground(this.findLabelColor);

        String mnemonic = getUIString(SEARCH_FIELD_MNEMONIC, locale);
        if (mnemonic != SEARCH_FIELD_MNEMONIC) {
            searchLabel.setDisplayedMnemonic(mnemonic.charAt(0));
        }
        searchLabel.setLabelFor(searchField);
    }
}
