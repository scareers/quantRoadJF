package com.scareers.gui.ths.simulation.interact.gui;

import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityBeanEm.SecurityEmPoForSmartFind;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS2;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.utils.CommonUtil;
import lombok.Data;
import org.jdesktop.swingx.JXList;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;

/**
 * description: 智能查找功能实现, 对话框!
 * 对话框单例; 设置在静态属性
 * // @key3: 进入智能查找模式后, focus一直在查找框里面!!! 结果选择也是查找框监听 上下箭头 切换结果列表的选择;
 * 且enter键, 将当前列表选择 作为 选择结果 !!!
 * -------------------------> @key3::
 * 添加查找类整个流程:
 * 1.确定要加入 查找map的 PO 对象, 实现合适的 toString方法(被列表展示); // 例如资产,功能等
 * 2.将对象加入 findingMap, 注意, key 需要保证唯一, 否则不同的对象将被覆盖!!! 功能可以 "功能_功能名称" 加前缀作为key; 资产用quoteId
 * 3.搜索本类todo, 需要 对 PO 类, 实现其查找逻辑, 判定类型...
 * 4.在initFindResultCallbackMap() 或者 动态调用 findResultCallbackMap.put() , 添加对于不同的类, 的查找结果的回调
 * 5.FindResultCallback 为查找结果回调接口; 对同类型对象, 因gui处于不同功能界面, 可能需要不同的响应逻辑, 使用 TraderGui.FunctionGuiCurrent 区分
 * 因此, 切换功能时, 至少应当将 TraderGui.FunctionGuiCurrent==null, 随后设置为新的值!  // 目前仅转债复盘设置了, 其他懒得加代码
 * ------------------------->
 *
 * @author: admin
 * @date: 2022/6/6/006-06:58:36
 */
@Data
public class SmartFindDialog extends JDialog {
    // 1.标志智能查找模式; 在该模式开始时, 一下按键监听生效, 否则不生效
    public static volatile boolean smartFinderMode = true; // 可通过修改此值, 关闭只能查找概念
    // 2.查找map; 切换功能应当清空它, 并填充它;
    public static volatile Hashtable<String, Object> findingMap = new Hashtable<>();
    public static volatile Hashtable<Class, FindResultCallback> findResultCallbackMap = new Hashtable<>(); // 回调map

    // value实际类型不固定, 例如 SecurityEmPoForSmartFind
    public static final int findResMaxAmount = 20; // 单次查找结果上限
    // 3.标志进入了单次只能查找模式, 该flag在监听到第一个字母后设置true, 在一次查找退出后, 设置false!
    public static volatile boolean smartFindingEntered = false; // 单例单锁逻辑

    private static SmartFindDialog INSTANCE;

    public static SmartFindDialog getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SmartFindDialog(TraderGui.INSTANCE, "智能查找", false);
        }

        return INSTANCE;
    }

    /*
    静态数据
     */
    public static int widthDefault0 = 300;
    public static int heightDefault0 = 450;
    public static HashSet<Integer> smartFinderStartKeySet; // A-Z, 0-9; 监听到这些键, 才开启 一次只能查找! 初始化后一般不变

    static {
        initSmartFinderStartKeySet(); //
        initFindResultCallbackMap(); // 查找结果回调 处理对象 填充
    }

    TraderGui parentS; // 自定义属性, 不使用父类 owner属性
    JPanel contentPanel;
    FinderInput findInput; // 查找内容输入框!
    JXList findResList; // 显示瞬间的查找结果, 并默认选中第一项, 并且监听 enter键, 确定选择

    public SmartFindDialog(TraderGui parent, String title, boolean modal) {
        super(parent, title, modal);
        this.parentS = parent;
        this.setSize(widthDefault0, heightDefault0); // 随后只需要设置 location 即可!
        this.setResizable(true);

        initContentPanel();
        this.setContentPane(contentPanel);
    }

    public static class FinderInput extends JTextField implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            textChange(e); // 插入和删除, 都调用相同回调
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChange(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {

        }

        // 文本大写
        @Override
        public void setText(String text) {
            super.setText(text.toUpperCase());
        }

        public void textChange(DocumentEvent e) {
            Document document = e.getDocument();
            try {
                String text = document.getText(0, document.getLength());
                if (INSTANCE != null) {
                    INSTANCE.doFind(text);
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 执行查找, 更新结果列表
     *
     * @param text
     */
    public void doFind(String text) {
        // 1. 查找结果列表
        ArrayList<Object> findRes = new ArrayList<>();

        // todo: @key3: 每当一种新类型put到map, 自行实现对应查找逻辑!
        // 2. 查找逻辑, 每当有一种类型的 东西, 被put到 map里面, 都自行实现 对应的查找逻辑
        Collection<Object> values = findingMap.values();
        for (Object value : values) {
            if (findRes.size() > findResMaxAmount) {
                break; // 查找结果上限, 以免循环次数太多
            }

            if (value instanceof SecurityEmPoForSmartFind) {
                SecurityEmPoForSmartFind po = (SecurityEmPoForSmartFind) value;
                SecurityBeanEm bean = po.getBean();
                // 1.检测拼音
                if (bean.getPinYin().toUpperCase().contains(text.toUpperCase())) {
                    findRes.add(po);
                } else if (bean.getSecCode().contains(text)) {
                    findRes.add(po);
                }
            }
        }

        // 查找结果
        // Console.log(findRes);
        // 2.把新的查找结果, 显示到 结果显示列表!
        model.flush(findRes);
        findResList.setSelectedIndex(0);
    }

    /**
     * 查找结果的回调函数, 处理查找确定下来的结果;
     * 对于同一种类型, 常态处理过程相近;  但gui可能处于不同界面功能, 则需要判定, 给出不同的逻辑; 由方法自行实现!
     * 例如: 当gui切换功能界面时, 修改一个 状态变量; --> TraderGui.FunctionGuiCurrent
     */
    public static abstract class FindResultCallback {
        public abstract void handleFindResult(Object findResult);
    }

    /**
     * --> 确认查找结果
     * 对查找结果, 查找框按下enter, 表示选中当前的 选择结果!
     */
    public void confirmFindResult(Object findResult) {
        // todo: 针对不同的对象, 均需要不同的回调处理
        if (findResult instanceof SecurityEmPoForSmartFind) {
            FindResultCallback callback = findResultCallbackMap.get(SecurityEmPoForSmartFind.class);
            if (callback != null) {
                callback.handleFindResult(findResult);
            }
        }

        // CommonUtil.notifyError("查找结果: " + findResult);
        // x. 最终都要退出
        this.setVisible(false); // 自动退出
    }

    public void initContentPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());

        // 1.查找框, 设置文档变化监听
        findInput = new FinderInput();
        findInput.getDocument().addDocumentListener(findInput);
        findInput.setText("测试内容");
        findInput.setPreferredSize(new Dimension(widthDefault0, 40));
        findInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (INSTANCE != null) {
                        INSTANCE.setVisible(false); // 将自动退出查找模式
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (INSTANCE != null) {
                        int selectedIndex = findResList.getSelectedIndex(); // 当前选择index
                        int elementCount = findResList.getElementCount(); // 总数
                        if (selectedIndex == 0 || selectedIndex == -1) {
                            findResList.setSelectedIndex(elementCount - 1); // 未选中时或第一个时, 按下上, 跳到最后
                            return;
                        }
                        findResList.setSelectedIndex(selectedIndex - 1);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (INSTANCE != null) {
                        int selectedIndex = findResList.getSelectedIndex(); // 当前选择index
                        int elementCount = findResList.getElementCount(); // 总数
                        if (selectedIndex == elementCount - 1 || selectedIndex == -1) {
                            findResList.setSelectedIndex(0); // 未选中时或第一个时, 按下上, 跳到最后
                            return;
                        }
                        findResList.setSelectedIndex(selectedIndex + 1);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) { // 确定选择!
                    if (INSTANCE != null) {
                        Object selectedValue = findResList.getSelectedValue();
                        if (selectedValue != null) {
                            confirmFindResult(selectedValue); // 执行确认查找结果
                        } else {
                            INSTANCE.setVisible(false); // 没有查找结果, 按下enter, 类似于esc
                        }
                    }
                }
            }
        });


        findResList = buildResList();
        initJListWrappedJScrollPane();
        findResList.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        contentPanel.add(findInput, BorderLayout.NORTH);
        contentPanel.add(findResList, BorderLayout.CENTER);
    }

    JScrollPane jScrollPaneForList;

    private void initJListWrappedJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(findResList); // 滚动包裹转债列表
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
    }

    DefaultListModelS2<Object> model;

    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private JXList buildResList() {
        // securityEmPos --> 自行实现逻辑, 改变自身该属性; 则 列表将自动刷新
        model = new DefaultListModelS2<>();
        model.flush(Collections.emptyList());
        JXList jList = new JXList(model);
        jList.setCellRenderer(new ResCellRendererS()); // 设置render
        jList.setForeground(COLOR_GRAY_COMMON);

        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        return jList;
    }

    /**
     * description: 结果列表显示 单元素显示渲染器
     *
     * @author: admin
     * @date: 2022/1/18/018-11:19:31
     */
    public class ResCellRendererS extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return label;
        }
    }


    /**
     * 重置位置, 将自身放在父亲右下角!
     */
    public void resetLocation() {
        if (this.parentS == null) {
            return;
        }

        this.setLocation(parentS.getX() + parentS.getWidth() - this.getWidth(),
                parentS.getY() + parentS.getHeight() - this.getHeight());
    }

    /**
     * 可作为只能查找开始一次查找的键 集合;  A-Z + 0-9; 其他键不可作为智能查找初始! 且不可组合键
     */
    public static void initSmartFinderStartKeySet() {
        java.util.List<Integer> keys = new ArrayList<>();
        for (int i = 0x41; i <= 0x5A; i++) { // A-Z, 无小写!
            keys.add(i);
        }
        for (int i = 0x30; i <= 0x39; i++) { // 0-9
            keys.add(i);
        }
        smartFinderStartKeySet = new HashSet<>(keys);
    }

    /**
     * 重写隐藏出现方法, 将合理设置 智能查找模式flag; 更加健壮
     *
     * @param b
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            smartFindingEntered = true;
        } else {
            smartFindingEntered = false;
        }
    }

    /**
     * @key3 : 全局唯一智能查找器, 在不同的功能区情况下, 请自行刷新 查找 Map! 一律返回查找结果 Object 类型;
     * 不同功能gui下, 显然返回结果可能不同 !
     * 全局只能查找器, 类似于同花顺 智能查找!
     */
    public static void addGlobalSmartFinder() {
        // 1.控件初始化, 使用 对话框, + 内部 Panel
        SmartFindDialog.getInstance(); // 初始化单例! 自行保证此前 TraderGui的单例已经创建!
        INSTANCE.resetLocation(); // 设置位置
        INSTANCE.setVisible(true);
        INSTANCE.setVisible(false);
        // 添加全局查找逻辑
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventPostProcessor(new KeyEventPostProcessor() {
            @Override
            public synchronized boolean postProcessKeyEvent(KeyEvent e) {
                INSTANCE.resetLocation(); // 设置位置
                // 1.需要开启智能查找模式设置下生效!
                if (!smartFinderMode) {
                    return true;
                }
                // 2.只监控按下事件, 无视释放事件
                if (e.getID() != KeyEvent.KEY_PRESSED) { // 需要按下事件, 否则一次按放回触发两次;
                    return true;
                }
                // 3. 3大组合键  任意按下状态, 也不会进入智能查找模式
                if (
                        (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0
                                || (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0
                                || (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0

                ) {
                    return true;
                }
                // 4. 如果非 26 + 10 字符, 无视掉!
                if (!smartFinderStartKeySet.contains(e.getKeyCode())) {
                    return true;
                }

                // 5. 进入或者维持智能查找模式! -- 因为各种输入框, 也会除非本回调, 因此进入只能查找模式后, 不继续监听!
                if (!smartFindingEntered) { // 此前非智能搜索模式, 则进入, 否则无视掉, 再按键则输入内容到查找框!!
                    if (INSTANCE.isVisible()) { // 对话框可见时, 意外进入, 此时无视;
                        return true;
                    }

                    // 查找框确定了搜索结果后, 应当退出本模式!
                    // 1.进入智能查找模式 flag
                    smartFindingEntered = true;
                    // 2. 此前的按键, 转换为单字符串
                    String s = Character.toString(e.getKeyCode()); // 按键字符串, 0-9, 大写 A-Z
                    INSTANCE.findInput.setText(s);

                    // 3. 设置对话框可见
                    INSTANCE.setVisible(true); // 可见
                    // 等待对话框显示
                    try {
                        CommonUtil.waitUtil(() -> INSTANCE.isVisible(), 100, 1, null, false);
                    } catch (TimeoutException | InterruptedException ex) {
//                        ex.printStackTrace();
                        INSTANCE.setVisible(true);
                    }

                    // 4. 设置输入框, 刚刚按下的按键, 且获取focus! 等待获取成功!
                    INSTANCE.findInput.requestFocus();
                    try {
                        CommonUtil.waitUtil(() -> INSTANCE.findInput.hasFocus(), 100, 1, null, false);
                    } catch (TimeoutException | InterruptedException ex) {
//                        ex.printStackTrace();
                        INSTANCE.findInput.requestFocus();
                    }
                }
                return true;
            }
        });
    }


    /**
     * 初始化填充 一些常用默认的 类型, 的查找结果, 的回调函数!
     * 也可以动态 put 添加
     */
    private static void initFindResultCallbackMap() {
        findResultCallbackMap.put(SecurityEmPoForSmartFind.class, new FindResultCallback() {
            @Override
            public void handleFindResult(Object findResult) {
                if (!(findResult instanceof SecurityEmPoForSmartFind)) {
                    return;
                }
                if (TraderGui.INSTANCE == null) {
                    return;
                }
                // 1.拿到对应类型的结果
                SecurityEmPoForSmartFind findRes = (SecurityEmPoForSmartFind) findResult;
                if (TraderGui.INSTANCE.functionGuiCurrent.equals(TraderGui.FunctionGuiCurrent.BOND_REVISE)) {
                    // 当前功能状态是 转债复盘
                    BondGlobalSimulationPanel instance = BondGlobalSimulationPanel.getInstance();
                    if (instance != null) {
                        instance.setSelectedBean(findRes.getBean());
                    }
                }
            }
        });
    }

}
