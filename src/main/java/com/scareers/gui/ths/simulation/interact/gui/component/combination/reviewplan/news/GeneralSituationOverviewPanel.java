package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONArray;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.gui.ths.simulation.interact.gui.component.simple.FuncButton;
import com.scareers.gui.ths.simulation.interact.gui.factory.ButtonFactory;
import com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout;
import joinery.DataFrame;
import org.jdesktop.swingx.JXImageView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import static com.scareers.gui.ths.simulation.interact.gui.layout.VerticalFlowLayout.TOP;

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
public class GeneralSituationOverviewPanel extends DisplayPanel {
    public static void main(String[] args) throws Exception {
//        SecurityBeanEm shangZhengZhiShu = SecurityBeanEm.getShangZhengZhiShu();
//        DataFrame<Object> quoteHistorySingle = EmQuoteApi
//                .getQuoteHistorySingle(false, shangZhengZhiShu, "20220401", "20220401", "101", "0", 3, 3000);
//        Console.log(quoteHistorySingle);

//        SecurityBeanEm yinn = SecurityBeanEm.createMgByCode("YINN"); // 富时中国3倍做多ETF -- 三倍做多FTSE中国ETF-Direxion
//        Console.log(yinn);
//        Console.log(yinn.getName());
////
//        SecurityBeanEm cweb = SecurityBeanEm.createMgByCode("CWEB"); // 中国互联网两倍做多 -- Direxion Daily CSI China Intern
//        Console.log(cweb);
//        Console.log(cweb.getName());
//
//        SecurityBeanEm fxi = SecurityBeanEm.createMgByCode("FXI"); // 富时中国50指数ETF -- 中国大型股ETF-iShares
//        Console.log(fxi);
//        Console.log(fxi.getName());

//        SecurityBeanEm PGJ = SecurityBeanEm.createMgByCode("PGJ"); // 金龙中国指数ETF -- 中概股ETF-PowerShares
//        Console.log(PGJ);
//        Console.log(PGJ.getName());


        JSONArray cn0Y = EastMoneyUtil.querySecurityId("HSAHGAHZS");
        Console.log(cn0Y);

//        SecurityBeanEm cn00Y = SecurityBeanEm.createFutureByCode("CN00Y"); // 富时A50期指连续 - A50期指当月连续
//        Console.log(cn00Y.getName()); // 注意, 同花顺代码时 CN0Y, 东财多个0

        // A指数: 上证指数,深证成指,沪深300,中证500,创业板指,科创50

//        SecurityBeanEm FXP = SecurityBeanEm
//                        .createMgByCode("FXP"); // 富时中国50两倍做空 -- 二倍做空FTSE中国50指数ETF
//        Console.log(FXP);
//        Console.log(FXP.getName());


//        SecurityBeanEm YANG = SecurityBeanEm
//                .createMgByCode("YANG"); // 富时中国50三倍做空 -- 三倍做空FTSE中国ETF-Direxion
//        Console.log(YANG);
//        Console.log(YANG.getName());

//        SecurityBeanEm hszs = SecurityBeanEm.createIndex("恒生指数"); // 香港恒生指数 - 恒生指数
//        Console.log(hszs.getName());

//        SecurityBeanEm hszs = SecurityBeanEm.createIndex("恒生科技指数"); // 恒生科技指数 - 恒生科技指数
//        Console.log(hszs.getName());

//        SecurityBeanEm hszs = SecurityBeanEm.createIndex("国企指数"); // 恒生科技指数 - 恒生科技指数
//        Console.log(hszs.getName());

        SecurityBeanEm hszs = SecurityBeanEm.createIndex("HSAHGAHZS"); // 恒生AH股AH指数 - 恒生AH股A+H指数
        Console.log(hszs.getName());

    }

    private static GeneralSituationOverviewPanel INSTANCE;

    public static GeneralSituationOverviewPanel getInstance(NewsTabPanel parentS) {
        if (INSTANCE == null) {
            INSTANCE = new GeneralSituationOverviewPanel(parentS);
        }
        return INSTANCE;
    }

    NewsTabPanel parentS;

    private GeneralSituationOverviewPanel(NewsTabPanel parentS) {
        this.parentS = parentS;


        this.setLayout(new VerticalFlowLayout(TOP));
//        this.setPreferredSize(new Dimension(800, 1200));

        KlineFsSimpleDisplayPanel klineFsSimpleDisplayPanel = new KlineFsSimpleDisplayPanel(300);

        FuncButton x = ButtonFactory.getButton("上证指数");
        x.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        SecurityBeanEm bean = null;
                        try {
                            bean = SecurityBeanEm.createStock("五粮液");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        klineFsSimpleDisplayPanel.update(bean);

                    }
                }, true);
            }
        });

        this.add(x);
        this.add(klineFsSimpleDisplayPanel);
    }

    @Override
    public void update() {

    }


}


