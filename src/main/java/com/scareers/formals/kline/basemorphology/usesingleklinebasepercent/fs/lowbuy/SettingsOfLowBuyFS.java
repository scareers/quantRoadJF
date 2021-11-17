package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/11/14  0014-8:51
 */
public class SettingsOfLowBuyFS {
    public static final int keyInt = 0; // 即判定 next0(明日) 的 最低点的分布.
    public static final int stockAmountsBeCalcFS = 1000000;
    public static final Connection connSingleton = ConnectionFactory.getConnLocalKlineForms();

    public static final List<List<String>> dateRanges = Arrays.asList(
            // 本身同主程序. 这里对任意形态组,均可在全日期区间验证. 常设置验证最后1区间
            Arrays.asList("20020129", "20031113"), // 5年熊市前半 3次触底大震荡
            Arrays.asList("20031113", "20040407"), // 短暂快牛
            Arrays.asList("20040407", "20050603"), // 长熊市后半触底

            Arrays.asList("20050603", "20060807"), // 牛市前段小牛
            Arrays.asList("20060807", "20071017"), // 超级牛市主升  10/16到6124
            Arrays.asList("20071017", "20081028"), // 超级熊市

            Arrays.asList("20081028", "20090804"), // 触底大幅反弹
            Arrays.asList("20090804", "20111011"), // 5年大幅振荡长熊市, 含后期底部平稳 -- 大幅振荡含凹坑
            Arrays.asList("20111011", "20140721"), // 5年大幅振荡长熊市, 含后期底部平稳 -- 小幅长久下跌

            Arrays.asList("20140721", "20150615"), // 大牛市  6/12到 5178, 周1暴跌
            Arrays.asList("20150615", "20160128"), // 大熊市, 含救市的明显回升后, 暴跌到底

            Arrays.asList("20160128", "20170116"), // 2年小长牛 -- 前段, 结尾下跌
            Arrays.asList("20170116", "20180129"), // 2年小长牛 -- 后段
            Arrays.asList("20180129", "20190104"), // 1年快速熊市

            Arrays.asList("20190104", "20200203"), // 1年中低位大幅振荡, 先升.
            Arrays.asList("20200203", "20210218"), // 开年暴跌后, 明显牛市到顶
            Arrays.asList("20210218", "21000101") // 顶部下跌后平稳年, 尝试突破未果;;@current 2021/10/11, 到未来

    );

    public static String saveTablenameLowBuyFS = "fs_distribution_of_low_buy_next{}";
    public static String sqlCreateSaveTableFSDistributionRaw = getSaveTableTemplate();
    public static String sqlCreateSaveTableFSDistribution = StrUtil.format(sqlCreateSaveTableFSDistributionRaw,
            StrUtil.format(saveTablenameLowBuyFS, keyInt));
    public static final String sqlDeleteExistDateRangeRawFS = "delete from {} where stat_date_range=\'{}\'";
    // 删除曾经的记录,逻辑同主程序
    public static String sqlDeleteExistDateRangeFS = StrUtil.format(sqlDeleteExistDateRangeRawFS,
            saveTablenameLowBuyFS);

    public static String getSaveTableTemplate() {
        String s = "create table if not exists `{}`\n" +
                "(\n" +
                "    id int auto_increment comment 'id'\n" + " primary key,\n" +

                "    stat_date_range   varchar(1024) null comment '该条记录的 统计日期区间',\n" +
                "    stat_result_algorithm     varchar(1024) null comment '统计使用的结果算法, 例如计算明日收盘,则为 " + "Next0Close',\n" +
                "    stat_stock_counts  int  null comment '统计时股票数量, 常规为全部股票. ',\n" +

                "    form_sets_id  int  not null comment '形态集合id, 对应 " + "next0b1s_of_single_kline 的id列,不能为空'," +
                "    analyze_item_type  varchar(2048)  not null comment '统计的项目:Low/2/3*4 总计12项',\n" +
                "    detail_list  longtext  not null comment '原始的分布结果列表,将对此列表进行统计分析.左右支配和出现时间全保存.价格转换为区间保存',\n" +
                "    analyze_item_type  varchar(2048)  not null comment '统计的项目:Low/2/3*4 总计12项',\n" +
                "    analyze_item_type  varchar(2048)  not null comment '统计的项目:Low/2/3*4 总计12项',\n" +
                "    analyze_item_type  varchar(2048)  not null comment '统计的项目:Low/2/3*4 总计12项',\n" +

                "     INDEX condition1_index (condition1 ASC),\n" +
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
                "     INDEX form_sets_id_index (form_sets_id ASC),\n" +
                "     INDEX stat_date_range_index (stat_date_range ASC),\n" +
                "     INDEX stat_result_algorithm_index (stat_result_algorithm ASC),\n" +
                ")\n" +
                "    comment '分时 低买 次日最低点分布分析';\n";
        return s;
    }
}