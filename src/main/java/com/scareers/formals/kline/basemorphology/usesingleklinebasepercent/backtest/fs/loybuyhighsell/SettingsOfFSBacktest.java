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

    public static String saveTablenameFSBacktestRaw = "fs_backtest_lowbuy_highsell_next{}b{}s";
    public static String saveTablenameFSBacktest = StrUtil.format(saveTablenameFSBacktestRaw, keyInts.get(0),
            keyInts.get(1));
    public static String sqlCreateSaveTableFSBacktestRaw = getSaveTableTemplate(); // todo: 保存表需要重写
    public static String sqlCreateSaveTableFSBacktest = StrUtil.format(sqlCreateSaveTableFSBacktestRaw,
            saveTablenameFSBacktest);

    public static final String sqlDeleteExistDateRangeFSRaw = "delete from {} where stat_date_range=\'{}\'";
    public static String sqlDeleteExistDateRangeFSBacktest = StrUtil.format(sqlDeleteExistDateRangeFSRaw,
            saveTablenameFSBacktest);
    public static final int processAmountOfBacktest = 16;

    // 低买设定
    public static Double tickGap = 0.005; // 分时分布的tick, 间隔是 0.005, 千分之五 . 主要是cdf用. 虽然可以实时计算, 没必要
    public static Double positionUpperLimit = 1.2; // 控制上限, 一般不大于 倍率, 当然, 这些倍率都是对于 1只股票1块钱而言
    public static Double positionCalcKeyArgsOfCdf = 1.5; // 控制单股cdf倍率, 一般不小于上限
    public static final Double execLowBuyThreshold = -0.0; // 必须某个值 <= -0.1阈值, 才可能执行低买, 否则跳过不考虑
    public static int continuousFallTickCountThreshold = 1; // 低买时, 连续下跌数量的阈值, 应当不小于这个数量, 才考虑卖. 1最宽容,可考虑2
    // 高卖设定
    public static Double positionCalcKeyArgsOfCdfHighSell = 1.5; // 控制单股cdf倍率, 卖出速度.  1-2之间变化明显.
    public static final Double execHighSellThreshold = 0.01; // 必须 >0.01阈值, 才可能执行高卖,
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
//            Arrays.asList("20200203", "20210218"), // 开年暴跌后, 明显牛市到顶
            Arrays.asList("20210218", "21000101") // 顶部下跌后平稳年, 尝试突破未果;;@current 2021/10/11, 到未来

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
                "    form_set_id  int  not null comment '形态集合id, 对应 " + "next0b1s_of_single_kline 的id列,不能为空'," +

                "    stat_date_range   varchar(1024) null comment '该条记录的 统计日期区间',\n" +
                "    stat_result_algorithm     varchar(1024) null comment '统计使用的结果算法, 例如计算明日收盘,则为Next0Close',\n" +
                "    concrete_algorithm     varchar(1024) null comment '具体的5种计量之一.',\n" +
                "    stat_stock_counts  int  null comment '统计时股票数量, 常规为全部股票. ',\n" +
                "    reference_value                    float null comment '参考值,主程序默认0.0,可设定',\n" +
                "    conditions_set                     varchar(1024) null comment '条件集合,描述,自行添加',\n" +
                "    condition1                         varchar(1024) null comment '条件1 的取值, 形如  条件名称[下限,上限], 一般包含下限,不包含上限',\n" +
                "    condition2                         varchar(1024) null,\n" +
                "    condition3                         varchar(1024) null,\n" +
                "    condition4                         varchar(1024) null,\n" +
                "    condition5                         varchar(1024) null,\n" +
                "    small_large_threshold               mediumtext    null comment '简单统计结果出现大幅变化的状态, (-0.05,+0.05), 表示对-0.05以下和0.05以上,视为大改变,进行统计',\n" +
                "    bins                               int           null comment '本身是matplotlib画柱状图时分区间数量参数, 这里引申对所有数据划分为多少个等价区间',\n" +
                "    effective_value_range              mediumtext    null comment '对结果数据, 设定有效区间, (-0.20.5,+0.205) 统计涨跌停以内情况',\n" +
                "    max                                float         null comment '基础统计量 -- 最大值',\n" +
                "    mean                               float         null comment '基础统计量 -- 算术平均',\n" +
                "    min                                float         null comment '基础统计量 -- 最小值',\n" +
                "    std                                float         null comment '基础统计量 -- 标准差, 不统计方差',\n" +
                "    virtual_geometry_mean              float         null comment '基础统计量 -- 折算的几何平均值, 即复利收益率,可能有少许误差',\n" +
                "    skew                                float   null comment '基础统计量 -- 斜度, 反映 聚集于 左边尾部 或右边尾部状态, 正太分布为0',\n" +
                "    kurt                                float   null comment '基础统计量 -- 峰度, 反映聚集于平均值附近的状态, 正太分布为3, 已减去3',\n" +
                "    cdf_list                           mediumtext    null comment 'cdf 即累计密度,列表;; 对应bins',\n" +
                "    frequency_list                     mediumtext    null comment 'frequency 频率列表',\n" +
                "    \n" +
                "    counts_list                   mediumtext    null comment '频数 列表',\n" +
                "    tick_list                          mediumtext    null comment 'tick 即区间分配列表',\n" +
                "    samlllarge_compare_counts_0         int           null comment '大值改变,对应 big_change_threshold, 0,1,2分别对应 小值,中间值,大值出现次数',\n" +
                "    samlllarge_compare_counts_1         int           null,\n" +
                "    samlllarge_compare_counts_2         int           null,\n" +
                "    samlllarge_compare_counts_percent_0 float         null comment '大值改变,频数的百分比, 0,1,2分别对应小,间,大',\n" +
                "    samlllarge_compare_counts_percent_1 float         null,\n" +
                "    samlllarge_compare_counts_percent_2 float         null,\n" +
                "    effective_counts                   int           null comment '有效的数量, 排除掉了无效数据后',\n" +
                "    effective_count_percent            float         null comment '有效统计 百分比',\n" +
                "    outliers_counts                    int           null comment '异常值数量',\n" +
                "    outliers_count_percent             int           null comment '异常值百分比',\n" +
                "    total_counts                       int           null comment '总计数量, 含异常值',\n" +
                "    reference_compare_counts_0              int           null comment '以0作为分界统计, 统计 <0,==0,>0 的数量'," +
                "    reference_compare_counts_1              int           null,\n" +
                "    reference_compare_counts_2              int           null,\n" +
                "    reference_compare_counts_percent_0      float         null comment '以0作为分界统计, 统计 <0,==0,>0 的数量 的百分比',\n" +
                "    reference_compare_counts_percent_1      float         null,\n" +
                "    reference_compare_counts_percent_2      float         null,\n" +
                "    self_notes                         varchar(2048) null comment '其他备注',\n" +

                "     INDEX condition1_index (condition1 ASC),\n" +
                "     INDEX condition2_index (condition2 ASC),\n" +
                "     INDEX condition3_index (condition3 ASC),\n" +
                "     INDEX condition4_index (condition4 ASC),\n" +
                "     INDEX condition5_index (condition5 ASC),\n" +
                "     \n" +
                "     INDEX form_set_id_index (form_set_id ASC),\n" +
                "     INDEX stat_date_range_index (stat_date_range ASC),\n" +
                "     INDEX stat_result_algorithm_index (stat_result_algorithm ASC),\n" +
                "     INDEX concrete_algorithm_index (concrete_algorithm ASC),\n" +
                "     INDEX reference_compare_counts_percent_0_index (reference_compare_counts_percent_0 ASC),\n" +
                "     INDEX reference_compare_counts_percent_1_index (reference_compare_counts_percent_1 ASC),\n" +
                "     INDEX reference_compare_counts_percent_2_index (reference_compare_counts_percent_2 ASC),\n" +
                "     \n" +
                "     INDEX samlllarge_compare_counts_percent_0_index (samlllarge_compare_counts_percent_0 ASC),\n" +
                "     INDEX samlllarge_compare_counts_percent_1_index (samlllarge_compare_counts_percent_1 ASC),\n" +
                "     INDEX samlllarge_compare_counts_percent_2_index (samlllarge_compare_counts_percent_2 ASC),\n" +
                "     \n" +
                "     INDEX max_index (max ASC),\n" +
                "     INDEX min_index (min ASC),\n" +
                "     INDEX std_index (std ASC),\n" +
                "     INDEX mean_index (mean ASC),\n" +
                "     INDEX skew_index (skew ASC),\n" +
                "     INDEX kurt_index (kurt ASC),\n" +
                "     INDEX virtual_geometry_mean_index (virtual_geometry_mean ASC),\n" +
                "\n" +
                "     INDEX effective_counts_index (effective_counts ASC)\n" +
                ")\n" +
                "    comment '分时 低买高卖 最低点最高点分布分析';\n";
        return null;
    }
}
