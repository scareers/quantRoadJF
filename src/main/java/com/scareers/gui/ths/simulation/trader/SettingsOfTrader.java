package com.scareers.gui.ths.simulation.trader;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;

import java.text.DateFormat;

/**
 * description: Trader 各项设置. 因python客户端不同, 可能设置会有所不同
 *
 * @author: admin
 * @date: 2022/2/19/019-14:56:32
 */
public class SettingsOfTrader {
    /**
     * 核心两项配置. 需要python端开启 M个客户端, 且id需要对应设置为 1-M; 其中 1-N个, 将作为账号状态刷新客户端.
     * M >= N+1
     */
    public static int totalClientAmount = 4; // 总计python模拟操作客户端. id从i开始到  M
    public static int accountStateFlushClientAmount = 2; // 前 N 个客户端用于账户状态刷新! N

    public enum ClientType { // python使用的券商客户端类型
        THS_SIMULATE_STOCK_TRADING, // 同花顺模拟交易
        DONG_FANG_ZHENG_QUAN, // 东方证券
        DONG_GUAN_ZHENG_QUAN // 东莞证券
    }

    // 核心设定, 当前使用的客户端类型,
    // @key: 必须设定
    public static ClientType clientType = ClientType.THS_SIMULATE_STOCK_TRADING;

    static {
        initSettingsByClientType();
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 初始化所有会 随着客户端类型不同而可能变化的 属性配置
     */
    public static void initSettingsByClientType() {
        if (clientType == ClientType.THS_SIMULATE_STOCK_TRADING) {
            initThsSstSettings();
        } else if (clientType == ClientType.DONG_FANG_ZHENG_QUAN) {
            initDongFangZqSettings();
        } else if (clientType == ClientType.DONG_GUAN_ZHENG_QUAN) {
            initDongGuanZqSettings();
        } else {
            log.error("fatal: 未知的模拟操作客户端环境! 请准确设置");
            System.exit(1);
        }
    }


    private static void initThsSstSettings() {
        STR_SEC_CODE = "证券代码"; // 持仓表头
        STR_SEC_BALANCE = "股票余额"; // 持仓表头
        STR_AVAILABLE_AMOUNT = "可用余额"; // 持仓表头
    }


    private static void initDongFangZqSettings() {
    }


    private static void initDongGuanZqSettings() {
    }


    /*
    因客户端不同而可能发生变化的字段!
    @key: 属性前缀说明:
        STR : 表示某些常量字符串. 例如各种表头名称(因客户端不同而不同)
     */
    public static String STR_SEC_CODE; // "证券代码" 表头, 常见于
    public static String STR_SEC_BALANCE; // "股票余额" 表头, 常见于
    public static String STR_AVAILABLE_AMOUNT; // "可用余额" 表头, 常见于



    /*
    常规常用设定.
     */

    /**
     * 日期格式常用! 同 hutool 设定: DatePattern.NORM_TIME_PATTERN
     */
    public static String DATE_FT_NORM = "yyyy-MM-dd"; // 常规日期格式 // NORM_DATE_PATTERN
    public static String DATE_FT_SIMPLEST = "yyyyMMdd"; // 最简单的日期格式 // PURE_DATE_PATTERN

    public static String TIME_FT_NORM = "HH:mm:ss"; // 常规时间格式 // NORM_TIME_PATTERN
    public static String TIME_FT_WITHOUT_SECONDS = "HH:mm"; // 时间格式不带秒 // NORM_TIME_PATTERN
    public static String TIME_FT_WITH_MS = "HH:mm:ss.SSS"; // 时间格式带毫秒 // 无

    public static String DT_FT_NORM = "yyyy-MM-dd HH:mm:ss"; // 日期时间格式标准 // NORM_DATETIME_PATTERN
    public static String DT_FT_WITH_MS = "yyyy-MM-dd HH:mm:ss.SSS"; // 日期时间格式带毫秒 // NORM_DATETIME_PATTERN
    public static String DT_FT_WITHOUT_SECONDS = "yyyy-MM-dd HH:mm"; // 日期时间格式不带秒 // NORM_DATETIME_MINUTE_PATTERN // 分时图

    /**
     * 非客户端相关的df数据 表头
     */
}
