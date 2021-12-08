package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/**
 * description:
 * // @noti: 除LowBuy外, 本主程序还 对 HighSell 进行了平行分析.
 *
 * @author: admin
 * @date: 2021/11/14  0014-8:51
 */
public class SettingsOfFSBacktest {
    public static final List<Integer> keyInts = Arrays.asList(0, 1);
    // 2021-最后一个周期数据, 140天出手次数在此范围才被选中
    public static final List<Integer> formSetIdsFilterArgs = Arrays.asList(1000, 10000);
    public static final int processAmountOfBacktest = 8;
    public static final int exit = 1000000; // 方便debug, 控制进度, 越大则无限制.回测多少天?
    public static final boolean forceSecrity = false; // 强制使得回测不能运行

    public static String saveTablenameFSBacktestRaw = "fs_backtest_lowbuy_highsell_next{}b{}s";
    public static String saveTablenameFSBacktest = StrUtil.format(saveTablenameFSBacktestRaw, keyInts.get(0),
            keyInts.get(1));
    public static String sqlCreateSaveTableFSBacktestRaw = getSaveTableTemplate(); // todo: 保存表需要重写
    public static String sqlCreateSaveTableFSBacktest = StrUtil.format(sqlCreateSaveTableFSBacktestRaw,
            saveTablenameFSBacktest);

    public static final String sqlDeleteExistDateRangeFSRaw = "delete from {} where stat_date_range=\'{}\'";
    public static String sqlDeleteExistDateRangeFSBacktest = StrUtil.format(sqlDeleteExistDateRangeFSRaw,
            saveTablenameFSBacktest);

    // 低买设定
    public static Double tickGap = 0.005; // 分时分布的tick, 间隔是 0.005, 千分之五 . 主要是cdf用. 虽然可以实时计算, 没必要
    public static Double positionUpperLimit = 1.3; // 控制上限, 一般不大于 倍率, 当然, 这些倍率都是对于 1只股票1块钱而言
    // @noti: 这些限制设定, 应当 / totalAsserts, 才能等价
    public static Double positionCalcKeyArgsOfCdf = 1.5; // 控制单股cdf倍率, 一般不小于上限
    public static final Double execLowBuyThreshold = -0.005; // 必须某个值 <= -0.1阈值, 才可能执行低买, 否则跳过不考虑
    public static int continuousFallTickCountThreshold = 2; // 低买时, 连续下跌数量的阈值, 应当不小于这个数量, 才考虑卖. 1最宽容,可考虑2

    // lb1: 大盘当时tick的涨跌幅加成算法.  两参数为0, 则相当于无此加成. 正数则符合现实意义, 也可尝试负数; 低买高卖参数可不同
    public static Double indexBelongThatTimePriceEnhanceArgLowBuy = 0.5;   // 对大盘当时涨跌幅, 计入仓位算法时的倍率. 越大则大盘当时涨跌幅影响越大
    public static Double indexBelongThatTimePriceEnhanceArgHighSell = 0.5;   // 对大盘当时涨跌幅, 计入仓位算法时的倍率. 越大则大盘当时涨跌幅影响越大

    // 高卖设定
    // 1.强制开盘卖出弱势股设定. 弱势股定义: 开盘价真实涨跌幅<阈值 且 小于相对于today涨跌幅阈值
    public static boolean forceSellOpenWeakStock = false; // 是否开盘强制卖出弱势股
    public static Double weakStockOpenPercentThatDayThreshold = -0.02; // 当日真实开盘价涨跌幅小于此阈值,  且:
    public static Double weakStockOpenPercentTodayThreshold = -0.07; // 开盘价(实际是9:31,而非9:30) 低于或等于此值,相对于today 视为弱势股,可开盘卖出

    public static Double positionCalcKeyArgsOfCdfHighSell = 1.5; // 控制单股cdf倍率, 卖出速度.  1-2之间变化明显.
    public static final Double execHighSellThreshold = -0.0; // 必须 >0.01阈值, 才可能执行高卖,
    public static int continuousRaiseTickCountThreshold = 1; // 高卖时, 连续上升数量的阈值, 应当不小于这个数量, 才考虑卖. 1最宽容,可考虑2,包含相等


    // 连接对象
    public static Connection connOfFS = ConnectionFactory.getConnLocalTushare1M();
    public static Connection connOfKlineForms = ConnectionFactory.getConnLocalKlineForms();
    public static Connection connLocalTushare = ConnectionFactory.getConnLocalTushare();

    public static final List<List<String>> dateRanges = Arrays.asList(
            // 本身同主程序. 这里对任意形态组,均可在全日期区间验证. 常设置验证最后1区间
//            Arrays.asList("20020129", "20031113"), // 5年熊市前半 3次触底大震荡
//            Arrays.asList("20031113", "20040407"), // 短暂快牛
//            Arrays.asList("20040407", "20050603"), // 长熊市后半触底
//
//            Arrays.asList("20050603", "20060807"), // 牛市前段小牛
//            Arrays.asList("20060807", "20071017"), // 超级牛市主升  10/16到6124
//            Arrays.asList("20071017", "20081028"), // 超级熊市
//
//            Arrays.asList("20081028", "20090804"), // 触底大幅反弹
//            Arrays.asList("20090804", "20111011"), // 5年大幅振荡长熊市, 含后期底部平稳 -- 大幅振荡含凹坑
//            Arrays.asList("20111011", "20140721"), // 5年大幅振荡长熊市, 含后期底部平稳 -- 小幅长久下跌

//            Arrays.asList("20140721", "20150615"), // 大牛市  6/12到 5178, 周1暴跌
//            Arrays.asList("20150615", "20160128"), // 大熊市, 含救市的明显回升后, 暴跌到底
//
//            Arrays.asList("20160128", "20170116"), // 2年小长牛 -- 前段, 结尾下跌
//            Arrays.asList("20170116", "20180129"), // 2年小长牛 -- 后段
//            Arrays.asList("20180129", "20190104"), // 1年快速熊市
//
//            Arrays.asList("20190104", "20200203") // 1年中低位大幅振荡, 先升.
            Arrays.asList("20210218", "21000101"), // 顶部下跌后平稳年, 尝试突破未果;;@current 2021/10/11, 到未来
            Arrays.asList("20200203", "20210218") // 开年暴跌后, 明显牛市到顶 todo: 顺序返回来

    );

    // 分时数据时, 仅访问close, 不访问多余字段,加速
    public static final List<String> fsSpecialUseFields = Arrays.asList("trade_time", "close"); // 简单买卖回测无视掉amount


    /**
     * [暂时的字段列表
     */
    public static String getSaveTableTemplate() {
        String s = "create table if not exists `{}`\n" +
                "(\n" +
                "    id int auto_increment comment 'id'\n" + " primary key,\n" +
                "    form_set_id  int  not null comment '形态集合id, 对应next0b1s_of_single_kline 的id列,不能为空'," +
                "    trade_date  varchar(1024) null comment '交易日期, 对应 回测时today含义',\n" +
                "    stocks_selected   longtext null comment '被选中股票列表',\n" +
                "    stat_date_range   varchar(1024) null comment '回测日期区间',\n" +
                "    stock_selected_count   double null comment '初始被选中股票数量',\n" +

                "    lb_position_price_map   longtext null comment '股票的 仓位,折算价格  字典保存',\n" +
                "    lb_full_position_time_tick   double null comment '低买达到满仓的时间',\n" +
                "    lb_buypoints   longtext null comment '买点',\n" +
                "    lb_weighted_buy_price   double null comment '低买全局折算价格',\n" +
                "    lb_simple_avg_buy_price   double null comment '低买简单平均价格, 无视仓位,不加权仓位',\n" +
                "    lb_global_position_sum   double null comment '低买总仓位, 目标是尽量靠近1',\n" +
                "    lb_has_position_stock_count   int null comment '低买有仓位的股票数量',\n" +

                "    lb_positions   longtext null comment '低买后持仓状况',\n" +
                "    hs_success_position_price   longtext null comment '高卖成功部分  仓位+价格',\n" +
                "    hs_open_close   longtext null comment '高卖日 开盘价和收盘价格',\n" +
                "    hs_sellpoints   longtext null comment '高卖点',\n" +
                "    hs_open_weak_stocks   longtext null comment '高卖当日,开盘弱势股票.开盘价低于等于某阈值',\n" +
                "    hs_remain_positions   longtext null comment '高卖未能成功剩余部分仓位',\n" +
                "    hs_discount_all_position_price   longtext null comment '高卖全部收盘折算后, 仓位和价格',\n" +
                "    hs_success_global_price   double null comment '高卖成功部分, 折算价格',\n" +
                "    hs_discount_all_global_price   double null comment '高卖全部折算, 折算价格',\n" +
                "    hs_success_position_profit   longtext null comment '高卖只计算成功部分,仓位+低买高卖操作收益',\n" +
                "    hs_success_profit   double null comment '高卖只计算成功部分, 加权盈利',\n" +
                "    hs_discount_all_position_profit   longtext null comment '高卖全部折算,仓位+低买高卖操作收益',\n" +
                "    hs_discount_all_profit   double null comment '高卖全部折算, 加权盈利',\n" +
                "    hs_success_global_percent   double null comment '@key:高卖成功总仓位 占比 低买成功总仓位.',\n" +
                "    lbhs_weighted_profit_conservative   double null comment '@key:低买高卖一次操作保守折算收益率.',\n" +
                "    self_notes                         varchar(2048) null comment '其他备注',\n" +

                "     INDEX form_set_id_index (form_set_id ASC),\n" +
                "     INDEX trade_date_index (trade_date ASC),\n" +
                "     INDEX stat_date_range_index (stat_date_range ASC),\n" +
                "     INDEX stock_selected_count_index (stock_selected_count ASC),\n" +
                "     INDEX lb_full_position_time_tick_index (lb_full_position_time_tick ASC),\n" +
                "     INDEX lb_weighted_buy_price_index (lb_weighted_buy_price ASC),\n" +
                "     INDEX lb_global_position_sum_index (lb_global_position_sum ASC),\n" +
                "     INDEX lb_has_position_stock_count_index (lb_has_position_stock_count ASC),\n" +
                "     INDEX hs_success_global_price_index (hs_success_global_price ASC),\n" +
                "     INDEX hs_discount_all_global_price_index (hs_discount_all_global_price ASC),\n" +
                "     INDEX hs_success_profit_index (hs_success_profit ASC),\n" +
                "     INDEX hs_discount_all_profit_index (hs_discount_all_profit ASC),\n" +
                "     INDEX hs_success_global_percent_index (hs_success_global_percent ASC),\n" +
                "     INDEX lbhs_weighted_profit_conservative_index (lbhs_weighted_profit_conservative ASC)\n" +
                ")\n" +
                "    comment '分时 低买高卖 回测结果保存表';\n";
        return s;
    }
}
