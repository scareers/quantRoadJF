package com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDfDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.securitylist.display.SecurityDisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.funcs.MainDisplayWindow;
import com.scareers.gui.ths.simulation.interact.gui.model.DefaultListModelS;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.ui.renderer.SecurityEmListCellRendererS;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.*;
import static java.awt.event.KeyEvent.VK_ENTER;

/**
 * description: 展示控件
 * 语义为: panel, 展示资产列表, 以及它对应的 某些数据
 * 该类实现了 资产列表展示, 由实现类实现展示内容
 *
 * @author: admin
 * @date: 2022/2/12/012-12:56:23
 */
@Getter
public abstract class SecurityListAndDisplayPanel extends JPanel {
    protected volatile Vector<SecurityBeanEm.SecurityEmPo> securityEmPos = new Vector<>(); // 开始空, 随后不变
    protected MainDisplayWindow mainDisplayWindow; // 主显示区
    protected volatile JList<SecurityBeanEm.SecurityEmPo> jList; //  展示股票的 list
    protected int jListWidth; // 列表宽度, 例如300

    /**
     * 实例初始化之前需要进行的一些等待,
     * 以及 资产列表 刷新的逻辑(可一次, 或者持续).
     * 已异步执行.
     */
    protected abstract void waitAndFlushSecurityListAsync();

    /**
     * 资产数据展示 Panel
     *
     * @return
     */
    protected abstract SecurityDisplayPanel buildDisplayPanel();
    // new SecurityDfDisplayPanel(this, this.jListWidth);

    protected SecurityListAndDisplayPanel(MainDisplayWindow mainDisplayWindow, int jListWidth) {
        // 异步开始等待某些状态, 并一次或者持续刷新股票列表
        ThreadUtil.execAsync(this::waitAndFlushSecurityListAsync, true);
        this.jListWidth = jListWidth;
        this.mainDisplayWindow = mainDisplayWindow;

        // 1.布局
        this.setLayout(new BorderLayout()); // border布局, 列表在左, 表格在右

        // 2.JList显示列表
        jList = getSecurityEmJList();
        this.add(jListWrappedWithJScrollPane(), BorderLayout.WEST); // 添加列表

        // 3. 1分钟fs详情控件, 表格.
        SecurityDisplayPanel securityDisplayPanel = buildDisplayPanel();
        this.add(securityDisplayPanel, BorderLayout.CENTER);

        // 6.主 展示窗口 添加尺寸改变监听. 改变 jList 和 orderContent尺寸.
        this.mainDisplayWindow.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                jList.setBounds(0, 0, jListWidth, getHeight()); // 固定宽默认 300
                securityDisplayPanel.setBounds(jScrollPaneForList.getVerticalScrollBar().getWidth() + jListWidth, 0,
                        getWidth() - jListWidth - jScrollPaneForList.getVerticalScrollBar().getWidth()
                        , getHeight()); //
                // 其余占满
                securityDisplayPanel.repaint();
            }
        });

        // 7.更新选择的股票以显示 对应的内容. 为了实时刷新的效果, 这里 持续刷新
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    SecurityBeanEm currentBean;
                    if (jList.getSelectedIndex() == -1) {
                        try {
                            if (securityEmPos.size() > 0) {
                                jList.setSelectedIndex(0); // 尝试选择第一个
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(10);
                        continue;
                    }
                    try {
                        currentBean = securityEmPos.get(jList.getSelectedIndex()).getBean();
                    } catch (Exception e) {
                        Thread.sleep(10);
                        continue;
                    }
                    if (currentBean != null) {
                        securityDisplayPanel.update(currentBean);
                    }
                    Thread.sleep(100);
                }
            }
        }, true);

    }

    JScrollPane jScrollPaneForList;

    private JScrollPane jListWrappedWithJScrollPane() {
        jScrollPaneForList = new JScrollPane();
        jScrollPaneForList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jScrollPaneForList.setViewportView(jList);
        jScrollPaneForList.getViewport().setBackground(COLOR_THEME_MINOR);
        BasicScrollBarUIS
                .replaceScrollBarUI(jScrollPaneForList, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        return jScrollPaneForList;
    }

    /**
     * 资产列表控件. 可重写
     *
     * @return
     */
    private JList<SecurityBeanEm.SecurityEmPo> getSecurityEmJList() {
        DefaultListModelS<SecurityBeanEm.SecurityEmPo> model = new DefaultListModelS<>();
        model.flush(securityEmPos); // 刷新一次数据, 首次为空

        JList<SecurityBeanEm.SecurityEmPo> jList = new JList<>(model);
        jList.setCellRenderer(new SecurityEmListCellRendererS()); // 设置render
        jList.setForeground(COLOR_GRAY_COMMON);

        // 持续刷新列表, 100 ms一次. securityEmPos 应该为持续变化
        ThreadUtil.execAsync(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) { // 每 100ms 刷新model
                    model.flush(securityEmPos);
                    Thread.sleep(100);
                }
            }
        }, true);

        // 双击事件监听, 跳转到东方财富资产行情页面
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) { // 非双击
                    return;
                }
                int index = jList.getSelectedIndex();
                SecurityBeanEm.SecurityEmPo po = jList.getModel().getElementAt(index);
                openSecurityQuoteUrl(po);
            }
        });
        jList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == VK_ENTER) {
                    int index = jList.getSelectedIndex();
                    SecurityBeanEm.SecurityEmPo po = jList.getModel().getElementAt(index);
                    openSecurityQuoteUrl(po);
                }
            }
        });

        jList.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
        jList.setPreferredSize(new Dimension(jListWidth, 10000));
        jList.setBackground(COLOR_THEME_MAIN);
        jList.setBorder(null);
        return jList;
    }

    public static void openSecurityQuoteUrl(SecurityBeanEm.SecurityEmPo po) {
        String url = null;
        SecurityBeanEm bean = po.getBean();
        if (bean.isBK()) {
            url = StrUtil.format("http://quote.eastmoney.com/bk/{}.html", bean.getQuoteId());
        } else if (bean.isIndex()) {
            url = StrUtil.format("http://quote.eastmoney.com/zs{}.html", bean.getSecCode());
        } else if (bean.isStock()) {
            if (bean.isHuA() || bean.isHuB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sh", po.getSecCode());
            } else if (bean.isShenA() || bean.isShenB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sz", po.getSecCode());
            } else if (bean.isJingA()) {
                url = StrUtil.format("http://quote.eastmoney.com/bj/{}.html", po.getSecCode());
            } else if (bean.isXSB()) {
                url = StrUtil.format("http://xinsanban.eastmoney.com/QuoteCenter/{}.html", po.getSecCode());
            } else if (bean.isKCB()) {
                url = StrUtil.format("http://quote.eastmoney.com/kcb/{}.html", po.getSecCode());
            }
        } else if (bean.isBond()) {
            if (bean.getMarket() == 0) {
                url = StrUtil.format("http://quote.eastmoney.com/sz{}.html", po.getSecCode());
            }else if(bean.getMarket()==1){
                url = StrUtil.format("http://quote.eastmoney.com/sh{}.html", po.getSecCode());
            }
        }

        if (url == null) {
            log.warn("未知资产类别, 无法打开行情页面: {}", bean.getName(), bean.getSecurityTypeName());
            return;
        }
        CommonUtil.openUrlWithDefaultBrowser(url);
    }

    public void showInMainDisplayWindow() {
        // 9.更改主界面显示自身
        mainDisplayWindow.setCenterPanel(this);
    }

    private static final Log log = LogUtil.getLogger();
}
