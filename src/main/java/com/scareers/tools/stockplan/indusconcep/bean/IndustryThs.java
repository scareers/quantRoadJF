package com.scareers.tools.stockplan.indusconcep.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * description: 操盘计划时, 使用的同花顺行业对象 bean
 * 源数据参考 industry_list 数据表
 *
 * @author: admin
 * @date: 2022/3/28/028-21:38:16
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "new_aspect_summary",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "type_Index", columnList = "type")})
public class IndustryThs {


    /*
    基本字段: 都来自与 ths. industry_list 数据表
     */
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id")
    Long id;
    @Column(name = "name", columnDefinition = "varchar(32)")
    String name; // 行业名称
    @Column(name = "type", columnDefinition = "varchar(32)")
    String type; // "二级行业" 或者 "三级行业"
    @Column(name = "code", columnDefinition = "varchar(32)")
    String code; // 简单代码
    @Column(name = "indexCode", columnDefinition = "varchar(32)")
    String indexCode; // 完整代码, 一般有 .TI 后缀
    @Column(name = "dateStr", columnDefinition = "varchar(32)")
    String dateStr; // 该行业原始数据抓取时的日期
    @Column(name = "chgP", length = 64)
    Double chgP; // 最新涨跌幅

    /*
    核心自定义字段: 未指明长短期, 默认短期
     */
    @Column(name = "dateStr", columnDefinition = "varchar(32)")
    String lineType = LineType.OTHER_LINE; // 主线还是支线? 默认其他线
    @Column(name = "pricePositionShortTerm", columnDefinition = "varchar(32)")
    String pricePositionShortTerm = PricePosition.UNKNOWN_POSITION; // 当前价格大概位置描述; 短期位置; 默认未知
    @Column(name = "pricePositionLongTerm", columnDefinition = "varchar(32)")
    String pricePositionLongTerm = PricePosition.UNKNOWN_POSITION; // 当前价格大概位置描述; 长期位置;  两个位置字段可取值相同.
    @Column(name = "priceTrend", columnDefinition = "varchar(32)")
    String priceTrend = PriceTrend.UNKNOWN; // 价格状态: 横盘/快升/快降/慢升/慢降
    @Column(name = "oscillationAmplitude", columnDefinition = "varchar(32)")
    String oscillationAmplitude = OscillationAmplitude.UNKNOWN; // 振荡幅度: 小/中/大


    /**
     * 行业主线支线类型, 这里不使用枚举, 直接使用字符串
     */
    public static class LineType {
        public static String MAIN_LINE_1 = "主线1";
        public static String MAIN_LINE_2 = "主线2";
        public static String MAIN_LINE_3 = "主线3";

        public static String BRANCH_LINE_1 = "支线1";
        public static String BRANCH_LINE_2 = "支线2";
        public static String BRANCH_LINE_3 = "支线3";

        public static String SPECIAL_LINE = "特殊线";
        public static String CARE_LINE = "关注线";
        public static String OTHER_LINE = "其他线";
    }

    /**
     * 行业价格大概位置描述
     */
    public static class PricePosition {
        public static String HIGH_POSITION = "高位";
        public static String MEDIUM_HIGH_POSITION = "中高位";
        public static String LOW_POSITION = "低位";
        public static String MEDIUM_LOW_POSITION = "中低位";
        public static String MEDIUM_POSITION = "中位";

        public static String UNKNOWN_POSITION = "未知";
    }

    /**
     * 趋势描述
     */
    public static class PriceTrend {
        public static String HORIZONTAL_LINE = "横盘"; // 水平线
        public static String FAST_RAISE = "快升"; // 水平线
        public static String SLOW_RAISE = "慢升"; // 水平线
        public static String FAST_FALL = "快降"; // 水平线
        public static String SLOW_FALL = "慢降"; // 水平线
        public static String UNKNOWN = "未知"; // 水平线
    }

    /**
     * 震荡程度描述; 该幅度 通常基于 价格涨跌幅, 而非绝对数值
     */
    public static class OscillationAmplitude {
        public static String BIG_AMPLITUDE = "大"; // 水平线
        public static String SMALL_AMPLITUDE = "小"; // 水平线
        public static String MEDIUM_AMPLITUDE = "中"; // 水平线
        public static String UNKNOWN = "未知"; // 水平线
    }
}
