package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/5  0005-15:09
 */
public class SettingsOfSingleKlineBasePercent {
    public static int windowUsePeriodsCoreArg = 9;
    //    public static final int processAmount = Runtime.getRuntime().availableProcessors() / 2 + 1;
    public static final int processAmountParse = 8; // 实测8-16. 更多无济于事
    public static final int processAmountSave = 16; // 实测32附近差不多ssd61%,增大效果不佳
    public static final int gcControlEpochParse = 300; // 新增:控制Parse 时gc频率.越大则手动gc频率越低,小幅减少时间.过大则要求线程数量不要过大
    public static final int gcControlEpochSave = 1000; // 新增:控制Save 时gc频率.越大则手动gc频率越低. 通常比上大
    public static final int stockAmountsBeCalc = 1000000;
    public static final int perEpochTaskAmounts = 64;// 至少2.  实测 61%ssd负载; 很难再高.
    public static final boolean excludeSomeBoards = true;
    public static final List<String> excludeBoards = Arrays.asList(null, "CDR", "科创板", "创业板");
    public static final String saveTablename = getSaveTablename("single_kline_forms_analyze_results_next{}");
    public static final boolean showMemoryUsage = false;

    public static String saveTablenameFiltered = getSaveTablename("filtered_single_kline_from_next{}");
    public static final Connection ConnOfSaveTable = tryGetConnForSavetable();
    public static final boolean calcCdfAndFrequencyWithTick = false;
    // 读取股票数据时的列, 注意 Class 列表需要与列一一对应
    public static final List<String> fieldsOfDfRaw = Arrays.asList("trade_date", "open", "close", "high", "low", "vol");
    public static final Class[] fieldsOfDfRawClass = {String.class, Double.class, Double.class,
            Double.class, Double.class, Double.class};
    public static final List<String> conditionNames = Arrays.asList("OP", "CGO", "US", "LS", "PL", "P5DP", "VTP5D");
    public static final List<Double> upperShadowRangeList = Arrays.<Double>asList(0.0, 0.002, 0.01, 0.025, 0.04, 0.06,
            0.075, 0.09, 0.41);
    public static final List<Double> lowerShadowRangeList = Arrays.<Double>asList(0.0, 0.002, 0.01, 0.025, 0.04, 0.06,
            0.075, 0.09, 0.41);
    public static final List<Double> entityRangeList = Arrays.<Double>asList(-0.41, -0.09, -0.075, -0.06, -0.04, -0.025,
            -0.01, -0.002, 0.002, 0.01, 0.025, 0.04, 0.06, 0.075, 0.09, 0.41);
    public static final List<Double> todayOpenRangeList = Arrays.<Double>asList(-0.205, -0.09, -0.075, -0.06, -0.04,
            -0.025, -0.01, -0.002, 0.002, 0.01, 0.025, 0.04, 0.06, 0.075, 0.09, 0.205);
    public static final List<Double> pre5dayPercentRangeList = Arrays.<Double>asList(-1.0, -0.5, -0.4, -0.3, -0.2,
            -0.1, -0.05, 0.0, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1.0);
    public static final List<Double> volToPre5dayAvgRangeList = Arrays.<Double>asList(0.0, 0.2, 0.5, 0.8, 1.0, 2.0, 4.0,
            8.0, 16.0, 32.0, 64.0, 1000.);

    // @noti: 这里也用于了 LowBuyNextHighSellDistributionAnalyze 脚本. 分析分布的脚本.
    // 分别对应window=7,8,9,10 用,
    // 千分之5一个tick
    public static final List<Integer> binsList = Arrays.asList(44, 88, 136, 188);
    public static final List<List<Double>> effectiveValusRanges = Arrays.asList(
            // 分别对应window=7,8,9,10 用
            Arrays.asList(-0.11, 0.11),
            Arrays.asList(-0.22, 0.22),
            Arrays.asList(-0.34, 0.34),
            Arrays.asList(-0.47, 0.47)
    );
    public static List<Double> bigChangeThreshold = Arrays.asList(-0.05, 0.05);


    public static void refreshWindowUsePeriodRelativeSettings(int windowUsePeriodsCoreArg) {
        // 在使用批量脚本时, 虽然 windowUsePeriodsCoreArg 通过了参数传递,
        // 但是,
        // 其他与之相关的设定, 例如保存表名等, 并不会跟随刷新, 导致bug出现.
        // 本方法, 刷新相关参数, 使得可以跟随 windowUsePeriodsCoreArg 而改变
        // 目前仅有保存表需要随动, 其他均固定

        // 设置传递来的参数
        SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg = windowUsePeriodsCoreArg;
        // 保存表与之相关
        saveTablenameFiltered = getSaveTablename("filtered_single_kline_from_next{}");


    }

    private static String getSaveTablename(String s) {
        String res = StrUtil.format(s,
                windowUsePeriodsCoreArg - 7);
        if (excludeSomeBoards) {
            res = res + "__excybkcb";
        }
        return res;
    }

    public static final List<List<String>> dateRanges = Arrays.asList(
            Arrays.asList("20020129", "20031113"),
            Arrays.asList("20031113", "20040407"),
            Arrays.asList("20040407", "20050603"),

            Arrays.asList("20050603", "20060807"),
            Arrays.asList("20060807", "20071017"),
            Arrays.asList("20071017", "20081028"),

            Arrays.asList("20081028", "20090804"),
            Arrays.asList("20090804", "20111011"),
            Arrays.asList("20111011", "20140721"),

            Arrays.asList("20140721", "20150615"),
            Arrays.asList("20150615", "20160128"),

            Arrays.asList("20160128", "20170116"),
            Arrays.asList("20170116", "20180129"),
            Arrays.asList("20180129", "20190104"),

            Arrays.asList("20190104", "20200203"),
            Arrays.asList("20200203", "20210218"),
            Arrays.asList("20210218", "21000101")


            //            Arrays.asList('20020129', '20050603'), // 中组合区间1
            //            Arrays.asList('20050603', '20081028'),
            //            Arrays.asList('20081028', '20140721'),
            //            Arrays.asList('20140721', '20160128'),
            //            Arrays.asList('20160128', '20190104'),
            //            Arrays.asList('20190104', '21000101'),
            //
            //            Arrays.asList('20020129', '20140721'), // 大组合区间2
            //            Arrays.asList('20140721', '20200203'),
            //            Arrays.asList('20200203', '21000101')
    );

    public static final String sqlCreateSaveTableRaw = getSqlCreateSaveTableTemplate();
    public static final String sqlCreateSaveTable = StrUtil.format(sqlCreateSaveTableRaw, saveTablename);
    public static final String sqlDeleteExistDateRangeRaw = "delete from {} where stat_date_range=\'{}\'";
    public static final String sqlDeleteExistDateRange = StrUtil.format(sqlDeleteExistDateRangeRaw, saveTablename);
    // 仍有{}需要填充date range

    public static void main(String[] args) {
        Console.log(processAmountParse);
        Console.log(processAmountSave);

        Console.log(excludeBoards.get(1));
        Console.log(sqlDeleteExistDateRange);
    }

    private static Connection tryGetConnForSavetable() {
        try {
            Connection conn = ConnectionFactory.getConnLocalKlineForms();
            return conn;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getSqlCreateSaveTableTemplate() {
        String s = "create table if not exists `{}`\n" +
                "(\n" +
                "    id                                 int auto_increment comment 'uuid 作为id'\n" +
                "        primary key,\n" +
                "    form_name                          varchar(512)  null comment '形态名称.形如 条件[参数区间]__条件2[参数区间]',\n" +
                "    stat_date_range                    varchar(1024) null comment '该条记录的 统计日期区间',\n" +
                "    stat_result_algorithm              varchar(1024) null comment '统计使用的结果算法, 例如计算明日收盘,则为 Next0Close',\n" +
                "    stat_stock_counts                  int           null comment '统计时股票数量, 常规为全部股票. ',\n" +
                "    form_description                   varchar(1024) null comment '形态描述, 自行添加',\n" +
                "    conditions_set                     varchar(1024) null comment '条件集合,描述,自行添加',\n" +
                "    condition1                         varchar(1024) null comment '条件1 的取值, 形如  条件名称[下限,上限], 一般包含下限,不包含上限',\n" +
                "    condition2                         varchar(1024) null,\n" +
                "    condition3                         varchar(1024) null,\n" +
                "    condition4                         varchar(1024) null,\n" +
                "    condition5                         varchar(1024) null,\n" +
                "    condition6                         varchar(1024) null,\n" +
                "    condition7                         varchar(1024) null,\n" +
                "    condition8                         varchar(1024) null,\n" +
                "    condition9                         varchar(1024) null,\n" +
                "    condition10                        varchar(1024) null,\n" +
                "    big_change_threshold               mediumtext    null comment '简单统计结果出现大幅变化的状态, (-0.05,+0.05), 表示对-0.05以下和0.05以上,视为大改变,进行统计',\n" +
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
                "    cdf_with_tick                      mediumtext    null comment 'cdf 和对应 tick 的二元组列表',\n" +
                "    frequency_list                     mediumtext    null comment 'frequency 频率列表',\n" +
                "    frequency_with_tick                mediumtext    null comment 'frequency 和对应tick 的二元组列表',\n" +
                "    \n" +
                "    occurrences_list                   mediumtext    null comment '频数 列表',\n" +
                "    tick_list                          mediumtext    null comment 'tick 即区间分配列表',\n" +
                "    bigchange_compare_counts_0         int           null comment '大值改变,对应 big_change_threshold, 0,1,2分别对应 小值,中间值,大值出现次数',\n" +
                "    bigchange_compare_counts_1         int           null,\n" +
                "    bigchange_compare_counts_2         int           null,\n" +
                "    bigchange_compare_counts_percnet_0 float         null comment '大值改变,频数的百分比, 0,1,2分别对应小,间,大',\n" +
                "    bigchange_compare_counts_percnet_1 float         null,\n" +
                "    bigchange_compare_counts_percnet_2 float         null,\n" +
                "    effective_counts                   int           null comment '有效的数量, 排除掉了无效数据后',\n" +
                "    effective_count_percent            float         null comment '有效统计 百分比',\n" +
                "    outliers_counts                    int           null comment '异常值数量',\n" +
                "    outliers_count_percent             int           null comment '异常值百分比',\n" +
                "    total_counts                       int           null comment '总计数量, 含异常值',\n" +
                "    zero_compare_counts_0              int           null comment '以0作为分界统计, 统计 <0,==0,>0 的数量',\n" +
                "    zero_compare_counts_1              int           null,\n" +
                "    zero_compare_counts_2              int           null,\n" +
                "    zero_compare_counts_percent_0      float         null comment '以0作为分界统计, 统计 <0,==0,>0 的数量 的百分比',\n" +
                "    zero_compare_counts_percent_1      float         null,\n" +
                "    zero_compare_counts_percent_2      float         null,\n" +
                "    self_notes                         varchar(2048) null comment '其他备注',\n" +
                "\n" +
                "    INDEX condition1_index (condition1 ASC),\n" +
                "     INDEX condition2_index (condition2 ASC),\n" +
                "     INDEX condition3_index (condition3 ASC),\n" +
                "     INDEX condition4_index (condition4 ASC),\n" +
                "     INDEX condition5_index (condition5 ASC),\n" +
                "     INDEX condition6_index (condition6 ASC),\n" +
                "     INDEX condition7_index (condition7 ASC),\n" +
                "     INDEX condition8_index (condition8 ASC),\n" +
                "     INDEX condition9_index (condition9 ASC),\n" +
                "     INDEX condition10_index (condition10 ASC),\n" +
                "     \n" +
                "     INDEX zero_compare_counts_percent_0_index (zero_compare_counts_percent_0 ASC),\n" +
                "     INDEX zero_compare_counts_percent_1_index (zero_compare_counts_percent_1 ASC),\n" +
                "     INDEX zero_compare_counts_percent_2_index (zero_compare_counts_percent_2 ASC),\n" +
                "     \n" +
                "     INDEX bigchange_compare_counts_percnet_0_index (bigchange_compare_counts_percnet_0 ASC),\n" +
                "     INDEX bigchange_compare_counts_percnet_1_index (bigchange_compare_counts_percnet_1 ASC),\n" +
                "     INDEX bigchange_compare_counts_percnet_2_index (bigchange_compare_counts_percnet_2 ASC),\n" +
                "     \n" +
                "     INDEX max_index (max ASC),\n" +
                "     INDEX min_index (min ASC),\n" +
                "     INDEX std_index (std ASC),\n" +
                "     INDEX mean_index (mean ASC),\n" +
                "     INDEX skew_index (skew ASC),\n" +
                "     INDEX kurt_index (kurt ASC),\n" +
                "\n" +
                "     INDEX effective_counts_index (effective_counts ASC),\n" +
                "     INDEX form_name_index (form_name ASC),\n" +
                "     INDEX stat_date_range_index (stat_date_range ASC),\n" +
                "     INDEX stat_result_algorithm_index (stat_result_algorithm ASC),\n" +
                "    \n" +
                "     INDEX virtual_geometry_mean_index (virtual_geometry_mean ASC)\n" +
                ")\n" +
                "    comment '单条k线形态, 以及相关延伸的分析结果保存;';\n";
        return s;
    }
}
