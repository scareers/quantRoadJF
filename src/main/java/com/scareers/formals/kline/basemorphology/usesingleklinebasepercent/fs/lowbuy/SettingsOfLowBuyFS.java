package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/**
 * description:
 * // @todo: LowBuy仅仅实现了 low和low1, low2未实现.
 *
 * @author: admin
 * @date: 2021/11/14  0014-8:51
 */
public class SettingsOfLowBuyFS {
    // 即判定 next0(明日) 的 最低点的分布. 本设定对应了 LowBuyNextHighSellDistributionAnalyze. correspondingFilterAlgos
    // 均表示 从上一级哪个结论表而分析.  比单独用一个 keyInt 更加合适

    // todo: 解决bug: 为什么next0b1s中, 出手次数 2000+的, 在这里只有 29? 这么少. 是哪边出了bug
    public static final List<Integer> keyInts = Arrays.asList(0, 1);
    public static final int stockAmountsBeCalcFS = 20;
    // 左右支配参数. 例如对于low, 左支配阈值, 为 abs(low)*0.2 + low; 对于 High, 则== high - abs(High)*0.2
    public static final Double dominateRateKeyArg = 0.2;
    public static final int calcLayer = 3; // 即判定3层. Low, Low2, Low3  @key: 核心设定
    public static final int processAmountParse = 16;
    public static final int processAmountSave = 32;
    public static final int perEpochTaskAmounts = 32;
    public static final int gcControlEpochParse = 100;
    public static final int gcControlEpochSave = 200;
    public static final boolean showMemoryUsage = true;
    public static final Class[] fieldsOfDfRawClass = {String.class, Double.class, Double.class,
            Double.class, Double.class, Double.class};
    public static Connection connOfFS = ConnectionFactory.getConnLocalTushare1M();
    public static Connection connOfKlineForms = ConnectionFactory.getConnLocalKlineForms();

    // 在 分析函数已经手动设定. 对这些参数不在显式设定, 见 analyzeStatsResults()
    //    public static List<Double> smallLargeThresholdOfValuePercent = Arrays.asList(-0.03, 0.03); // 涨跌幅的3个参数. low/high同
    //    public static List<Double> effectiveValueRangeOfValuePercent = Arrays.asList(-0.5, 0.5);
    //    public static int binsOfValuePercent = 200;
    //    public static List<Double> smallLargeThresholdOfAmountPercent = Arrays.asList(0.05, 0.15); // 连续成交额的3个参数.
    //    public static List<Double> effectiveValueRangeOfAmountPercent = Arrays.asList(0.0, 1.0); // 成交量 200tick, 每个 0.5%
    //    public static int binsOfAmountPercent = 200;

    public static final List<List<Double>> effectiveValueRanges = Arrays.asList(
            Arrays.asList(-0.11, 0.11), // 本设定暂时同主程序.
            Arrays.asList(-0.22, 0.22),
            Arrays.asList(-0.28, 0.34), // window lenth == 9, 即next2开始, 不再强行对称. 而改用 0.9**n/ 1.1**n折算
            Arrays.asList(-0.36, 0.47), //
            Arrays.asList(-0.42, 0.62), //
            Arrays.asList(-0.46, 0.78), // 12
            Arrays.asList(-0.54, 0.95) // 13
    );
    public static final List<Integer> correspondingBins = Arrays.asList(
            44, 88, 124, 188, // next0,1,2,3
            208, 248, 298
    );
    // 已经计算出实际严格使用的涨跌幅限制
    public static final List<Double> effectiveValueRangeForLow = effectiveValueRanges.get(keyInts.get(0));
    public static final int binForLow = correspondingBins.get(keyInts.get(0));
    public static final List<Double> effectiveValueRangeForHigh = effectiveValueRanges.get(keyInts.get(1));
    public static final int binForHigh = correspondingBins.get(keyInts.get(1));

    // 分时数据时, 仅访问close, 不访问多余字段,加速
    public static final List<String> fsSpecialUseFields = Arrays.asList("trade_time", "close", "amount");
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
//
//            Arrays.asList("20140721", "20150615"), // 大牛市  6/12到 5178, 周1暴跌
//            Arrays.asList("20150615", "20160128"), // 大熊市, 含救市的明显回升后, 暴跌到底
//
//            Arrays.asList("20160128", "20170116"), // 2年小长牛 -- 前段, 结尾下跌
//            Arrays.asList("20170116", "20180129"), // 2年小长牛 -- 后段
//            Arrays.asList("20180129", "20190104"), // 1年快速熊市
//
//            Arrays.asList("20190104", "20200203"), // 1年中低位大幅振荡, 先升.
//            Arrays.asList("20200203", "20210218"), // 开年暴跌后, 明显牛市到顶
            Arrays.asList("20210218", "21000101") // 顶部下跌后平稳年, 尝试突破未果;;@current 2021/10/11, 到未来

    );

    public static String saveTablenameLowBuyFSRow = "fs_distribution_of_lowbuy_highsell_next{}b{}s";
    public static String saveTablenameLowBuyFS = StrUtil.format(saveTablenameLowBuyFSRow, keyInts.get(0),
            keyInts.get(1));
    public static String sqlCreateSaveTableFSDistributionRaw = getSaveTableTemplate();
    public static String sqlCreateSaveTableFSDistribution = StrUtil.format(sqlCreateSaveTableFSDistributionRaw,
            saveTablenameLowBuyFS);
    // 删除曾经的记录,逻辑同主程序
    public static final String sqlDeleteExistDateRangeRawFS = "delete from {} where stat_date_range=\'{}\'";
    public static String sqlDeleteExistDateRangeFS = StrUtil.format(sqlDeleteExistDateRangeRawFS,
            saveTablenameLowBuyFS);

    /**
     * [暂时的字段列表
     * "small_large_threshold",
     * "samlllarge_compare_counts_percent_0",
     * "samlllarge_compare_counts_percent_1",
     * "samlllarge_compare_counts_percent_2",
     * "std",
     * "bins",
     * "frequency_list",
     * "outliers_counts",
     * "max",
     * "effective_value_range",
     * "cdf_list",
     * "tick_list",
     * "reference_compare_counts_percent_0",
     * "reference_compare_counts_percent_1",
     * "reference_compare_counts_percent_2",
     * "virtual_geometry_mean",
     * "effective_counts",
     * "total_counts",
     * "reference_value",
     * "min",
     * "samlllarge_compare_counts_0",
     * "samlllarge_compare_counts_1",
     * "samlllarge_compare_counts_2",
     * "reference_compare_counts_0",
     * "reference_compare_counts_1",
     * "reference_compare_counts_2",
     * "mean",
     * "effective_count_percent",
     * "counts_list",
     * "outliers_count_percent",
     * "kurt",
     * "skew"
     * ]
     * <p>
     * analyzeResultDf.add("form_set_id", formSetId.intValue());
     * analyzeResultDf.add("stat_result_algorithm", statResultAlgorithm);
     * analyzeResultDf.add("concrete_algorithm", statResultAlgorithm);
     * analyzeResultDf.add("stat_date_range", statDateRange);
     * analyzeResultDf.add("stat_stock_counts", stockCount);
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
        return s;
    }
}
