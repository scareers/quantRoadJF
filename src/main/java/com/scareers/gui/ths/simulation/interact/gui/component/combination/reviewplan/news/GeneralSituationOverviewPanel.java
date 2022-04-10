package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import com.scareers.gui.ths.simulation.interact.gui.ui.BasicScrollBarUIS;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;
import lombok.Data;
import lombok.Getter;
import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_SCROLL_BAR_THUMB;
import static com.scareers.gui.ths.simulation.interact.gui.SettingsOfGuiGlobal.COLOR_THEME_TITLE;
import static com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout.TOP;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * description: 大势概览面板, 主要显示 各大指数(包括外围) 涨跌幅; 上证深证市场状态; 类似同花顺(和东财)的 0 和 .1(大势查看);
 * 本面板, 将可折叠, 放置于 NewAspectSummaryPanel 的右侧的最上方! 可以看着他写一些预判/利好利空等
 * // 折叠由 NewAspectSummaryPanel实现控制, 自身不实现
 * 当着眼于新闻时, 可以折叠本面板, 专心于新闻查看!
 * 在数据方面, 它相对比较独立, 基本不与其他控件交互;
 * update(), 主要访问url, 得到最新行情等! 不保存数据库!
 *
 * @author: admin
 * @date: 2022/4/3/003-08:52:43
 */
@Getter
public class GeneralSituationOverviewPanel extends DisplayPanel {
    public static void main(String[] args) throws Exception {

        JSONArray cn0Y = EastMoneyUtil.querySecurityId("美元指数");
        for (Object o : cn0Y) {
            JSONObject o1 = (JSONObject) o;
            Console.log("{} - {} - {}", o1.getString("Code"), o1.getString("Name"), o1.getString("SecurityTypeName"));
        }
    }

    private static GeneralSituationOverviewPanel INSTANCE;

    public static GeneralSituationOverviewPanel getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new GeneralSituationOverviewPanel(parentS);
        }
        return INSTANCE;
    }

    NewsTabPanel parentS;
    JScrollPane containerJScrollPane = new JScrollPane();

    JXPanel quoteButtonContainer = new JXPanel(); // fs k线查看,按钮容器; 可点击查看常用指数
    KlineFsSimpleDisplayPanel klineFsSimpleDisplayPanel; // 分时k线对象, 它放在 mainContentPanel 的west
    JXPanel mainContentPanel = new JXPanel();

    private GeneralSituationOverviewPanel(NewsTabPanel parentS) {
        this.parentS = parentS;
        this.setLayout(new VerticalFlowLayout(TOP));
        initContainerJScrollPane();

        klineFsSimpleDisplayPanel = new KlineFsSimpleDisplayPanel();
        klineFsSimpleDisplayPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        quoteButtonContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        initQuoteButtons();

        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.add(klineFsSimpleDisplayPanel, BorderLayout.WEST);
        mainContentPanel.setBorder(BorderFactory.createLineBorder(Color.red, 1));
        initMainContentPanel();

        this.add(quoteButtonContainer);
        this.add(mainContentPanel);


        this.update();


    }

    SecurityListChgPPanel aHIndexes;
    SecurityListChgPPanel mgIndexes;
    SecurityListChgPPanel youAndPanIndexes;

    /**
     * 主内容面板的 CENTER, 需要是显示主要内容的面板
     */
    private void initMainContentPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new GridLayout(1, 3, 1, 1));

        aHIndexes = new SecurityListChgPPanel(buildGeneralOverviewOfAHIndexes());
        mgIndexes = new SecurityListChgPPanel(buildGeneralOverviewOfMgIndexes());
        youAndPanIndexes = new SecurityListChgPPanel(
                buildGeneralOverviewOfYouAndPanIndexes()); // 游和盘

        jPanel.add(aHIndexes);
        jPanel.add(mgIndexes);
        jPanel.add(youAndPanIndexes);
        mainContentPanel.add(jPanel, BorderLayout.CENTER);
    }

    /**
     * 大势A港指数
     *
     * @return
     */
    private List<SecurityButton> buildGeneralOverviewOfAHIndexes() {
        List<SecurityButton> buttons = new ArrayList<>();

        /*
        1.做多,
         */

        // 三倍做多FTSE中国ETF-Direxion
        buttons.add(new SecurityButton("富时中国3倍做多ETF", "YINN", SecurityBeanEm.SecType.MG_BY_CODE,
                klineFsSimpleDisplayPanel));
        // 中国大型股ETF-iShares
        buttons.add(
                new SecurityButton("富时中国50指数ETF", "FXI", SecurityBeanEm.SecType.MG_BY_CODE, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("富时A50期指连续", "CN00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));

        // 2. 6大指数
        buttons.add(new SecurityButton("上证指数", "上证指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("深证成指", "深证成指", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("创业板指", "创业板指", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("科创50", "科创50", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("沪深300", "沪深300", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("中证500", "中证500", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));

        // 3. 2个做空etf, 2倍和3倍
        // 二倍做空FTSE中国50指数ETF
        buttons.add(
                new SecurityButton("富时中国50两倍做空", "FXP", SecurityBeanEm.SecType.MG_BY_CODE, klineFsSimpleDisplayPanel));
        // 三倍做空FTSE中国ETF-Direxion
        buttons.add(
                new SecurityButton("富时中国50三倍做空", "YANG", SecurityBeanEm.SecType.MG_BY_CODE, klineFsSimpleDisplayPanel));

        // 4. 4个港股指数
        buttons.add(new SecurityButton("恒生指数", "恒生指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("恒生科技指数", "恒生科技指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("国企指数", "国企指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(
                new SecurityButton("恒生AH股AH指数", "HSAHGAHZS", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        // 中概股ETF-PowerShares
        buttons.add(
                new SecurityButton("金龙中国指数ETF", "PGJ", SecurityBeanEm.SecType.MG_BY_CODE, klineFsSimpleDisplayPanel));
        // Direxion Daily CSI China Intern
        buttons.add(
                new SecurityButton("中国互联网两倍做多", "CWEB", SecurityBeanEm.SecType.MG_BY_CODE, klineFsSimpleDisplayPanel));
        return buttons;
    }

    /**
     * 大势A港指数
     *
     * @return
     */
    private List<SecurityButton> buildGeneralOverviewOfMgIndexes() {
        List<SecurityButton> buttons = new ArrayList<>();
        buttons.add(new SecurityButton("道琼斯指数", "道琼斯", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("纳斯达克指数", "纳斯达克", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("标准普尔500指数", "标普500", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));

        buttons.add(new SecurityButton("俄罗斯RTS指数", "俄罗斯RTS", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("日经225指数", "日经225", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("韩国综合指数", "韩国KOSPI", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("台湾加权指数", "台湾加权", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));

        buttons.add(new SecurityButton("美原油连续", "CL00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("布伦特原油当月连续", "B00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("纽约金主连", "GC00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("纽约银主连", "SI00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("纽约铜主连", "HG00Y", SecurityBeanEm.SecType.FUTURE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("美元离岸人民币", "USDCNH", SecurityBeanEm.SecType.FOREIGN_EXCHANGE_BY_CODE,
                klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("美元指数", "美元指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        return buttons;
    }


    /**
     * 上中下游 和 大中小盘 指数
     *
     * @return
     */
    private List<SecurityButton> buildGeneralOverviewOfYouAndPanIndexes() {
        List<SecurityButton> buttons = new ArrayList<>();
        buttons.add(new SecurityButton("中证上游", "中证上游", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("中证中游", "中证中游", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("中证下游", "中证下游", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("上超大盘", "超大盘", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("上证中盘", "上证中盘", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("上证中小", "上证中小", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        buttons.add(new SecurityButton("上证小盘", "上证小盘", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        return buttons;
    }

    private void initContainerJScrollPane() {
        containerJScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        containerJScrollPane.setViewportView(this);
        containerJScrollPane.setBorder(null);
        BasicScrollBarUIS
                .replaceScrollBarUI(containerJScrollPane, COLOR_THEME_TITLE, COLOR_SCROLL_BAR_THUMB); // 替换自定义 barUi
        containerJScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED); // 一般都需要
        containerJScrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED); // 一般都需要
        containerJScrollPane.getVerticalScrollBar().setUnitIncrement(30); // 增加滚动速度
    }

    private void initQuoteButtons() {
        quoteButtonContainer
                .add(new SecurityButton("上证指数", "上证指数", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("深证成指", "深证成指", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("创业板指", "创业板指", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("科创50", "科创50", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("沪深300", "沪深300", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("中证500", "中证500", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("道琼斯指数", "道琼斯", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("标普500", "标普500", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
        quoteButtonContainer
                .add(new SecurityButton("纳斯达克指数", "纳斯达克", SecurityBeanEm.SecType.INDEX, klineFsSimpleDisplayPanel));
    }


    @Override
    /**
     * 只调用一次
     */
    public void update() {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        aHIndexes.update();
                        mgIndexes.update();
                        youAndPanIndexes.update();
                        ThreadUtil.sleep(30000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, true);
    }

    /**
     * 附带了 SecurityBeanEm 的按钮; bean懒加载, 需要提供查询条件和类型
     */
    @Data
    public static class SecurityButton extends JButton {
        SecurityBeanEm beanEm;
        String queryCondition;
        SecurityBeanEm.SecType type;
        KlineFsSimpleDisplayPanel klineFsSimpleDisplayPanel;

        public SecurityButton(String text, String queryCondition, SecurityBeanEm.SecType type,
                              KlineFsSimpleDisplayPanel klineFsSimpleDisplayPanel) {
            super(text);
            this.queryCondition = queryCondition;
            this.klineFsSimpleDisplayPanel = klineFsSimpleDisplayPanel;
            this.type = type;
            init();
        }

        private void init() {
            this.setBorderPainted(false); // 无边框
            this.setBackground(Color.black);
            this.setForeground(Color.red);
            this.setBorder(new LineBorder(Color.red, 1, true));

            this.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            klineFsSimpleDisplayPanel.update(getBean());
                        }
                    }, true);
                }
            });
        }


        /**
         * 懒加载, 可能为null; 在查询失败时
         *
         * @return
         */
        public SecurityBeanEm getBean() {
            if (beanEm == null) {
                try {
                    this.beanEm = SecurityBeanEm.createBeanWithType(queryCondition, type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return beanEm;
        }
    }

    /**
     * 一批指数的涨跌幅显示
     */
    public static class SecurityListChgPPanel extends DisplayPanel {
        java.util.List<SecurityButton> buttons; // 按钮, 必须指定
        java.util.List<JLabel> chaPLabels = new ArrayList<>(); // 按钮, 必须指定

        public SecurityListChgPPanel(
                List<SecurityButton> buttons) {
            this.buttons = buttons;
            for (int i = 0; i < buttons.size(); i++) {
                chaPLabels.add(buildLabel());
            }

            this.setLayout(new VerticalFlowLayout(TOP, -1, -1, true, true));
//            this.setLayout(new GridLayout(buttons.size(), 2, -1, -1));


            for (int i = 0; i < buttons.size(); i++) {
                SecurityButton comp = buttons.get(i);
                comp.setBorder(BorderFactory.createLineBorder(Color.black, 1));
                JLabel comp1 = chaPLabels.get(i);
                comp1.setBorder(BorderFactory.createLineBorder(Color.black, 1));

                JPanel jPanel = new JPanel();
                jPanel.setLayout(new GridLayout(1, 2, -1, -1));
                jPanel.add(comp);
                jPanel.add(comp1);
                jPanel.setPreferredSize(new Dimension(200, 30));
                this.add(jPanel);
            }
            this.add(new JPanel());
        }

        public JLabel buildLabel() {
            JLabel jLabel = new JLabel();
            jLabel.setPreferredSize(new Dimension(80, 30));
            jLabel.setBackground(Color.black);
            jLabel.setForeground(Color.red);
            jLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
            return jLabel;
        }

        /**
         * 遍历buttons, 得到所有bean, 得到 最新日k线 涨跌幅; 时间对不对自行把握, 跟用同花顺一样, 没刷新时显示上一日涨跌幅
         */
        @Override
        public void update() {
            ArrayList<SecurityBeanEm> beans = new ArrayList<>();
            for (SecurityButton button : buttons) {
                if (button.getBean() != null) {
                    beans.add(button.getBean());
                } else {
                    Console.log(button.getQueryCondition());
                }
            }

            String startDate = DateUtil.today();
            try {
                EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 3); // 取3天所有数据, 等下用到最新的一条数据
            } catch (Exception e) {
                e.printStackTrace();
            }
            DataFrame<Object> dfTemp = EmQuoteApi.getLatestQuoteOfBeanList(beans);
            if (dfTemp == null || dfTemp.length() == 0) {
                ManiLog.put(StrUtil.format("SecurityListChgPPanel: 暂无数据"));
                return;
            }

            HashMap<String, Object> quoteMap = new HashMap<>(); // 转换为map查询,更快
            for (int i = 0; i < dfTemp.length(); i++) {
                Object code = dfTemp.get(i, "资产代码");
                Object name = dfTemp.get(i, "资产名称");
                if (code == null || name == null) {
                    continue; // 不完整
                }
                quoteMap.put(StrUtil.format("{}__{}", code, name), dfTemp.get(i, "涨跌幅"));
            }

            for (int i = 0; i < chaPLabels.size(); i++) {
                JLabel jLabel = chaPLabels.get(i);
                SecurityButton securityButton = buttons.get(i);

                SecurityBeanEm bean = securityButton.getBean();
                if (bean == null) {
                    ManiLog.put(StrUtil.format("行情按钮: bean==null: {}", securityButton.getQueryCondition()));
                    continue;
                }

                Object chgP = quoteMap.get(StrUtil.format("{}__{}", bean.getSecCode(), bean.getName()));
                if (chgP == null) {
                    ManiLog.put(StrUtil.format("暂无涨跌幅数据: {} - {}", bean.getName(), bean.getQuoteId()));
                    continue;
                }
                jLabel.setText(chgP.toString() + " %");
                Double chgPNum = null;
                try {
                    chgPNum = Double.valueOf(chgP.toString());
                } catch (NumberFormatException e) {

                }
                if (chgPNum != null) {
                    if (chgPNum > 0) {
                        jLabel.setForeground(Color.red);
                    } else if (chgPNum < 0) {
                        jLabel.setForeground(Color.green);
                    } else {
                        jLabel.setForeground(Color.white);
                    }
                }

            }
        }
    }


}


