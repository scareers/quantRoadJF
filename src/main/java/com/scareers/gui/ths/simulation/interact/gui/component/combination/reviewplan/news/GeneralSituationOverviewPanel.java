package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.news;

import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;

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
    public GeneralSituationOverviewPanel() {


    }

    @Override
    public void update() {

    }
}
