package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.settings.SettingsCommon;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.SqlUtil;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.*;
import static com.scareers.utils.HardwareUtils.reportCpuMemoryDisk;

/**
 * description: 针对 next0b1s/1b2s等, 对 nextLow 在买入当日的 最低点出现的时间, 0-240; 分布分析.  -- 出现时间分布
 * -- 时间
 * 0 代表 09:30;  240代表 15:00 , 对应tushare分时数据, 每日 241个数据.
 * -- 分区间
 * 例如将241分钟, 中间 240间隔, 按刻分, 则分为8个区间, 规定 9:30 算作第一个区间内 30-45 算作第一个区间; 46-00 第二个
 *
 * @author: admin
 * @date: 2021/11/14  0014-4:48
 */
public class FSAnalyzeLowDistributionOfLowBuyNextHighSell {
    public static void main(String[] args) throws Exception {
        List<String> stocks = TushareApi.getStockListFromTushareExNotMain();
        stocks = stocks.subList(0, Math.min(stockAmountsBeCalcFS, stocks.size()));
        DataFrame<String> stockWithBoard = TushareApi.getStockListWithBoardFromTushare();
        List<List<String>> dateRanges = SettingsOfLowBuyFS.dateRanges;
        HashMap<String, List<List<String>>> stockWithStDateRanges = TushareApi.getStockWithStDateRanges();

        // 连接对象并不放在设置类
        Connection connSingleton = ConnectionFactory.getConnLocalKlineForms();
        // 未关闭连接,可复用
        SqlUtil.execSql(SettingsOfLowBuyFS.sqlCreateSaveTableFSDistribution,
                connSingleton, false);
        for (List<String> statDateRange : dateRanges) {
            // 测试时用最新一个日期区间即可
            Console.log("当前循环组: {}", statDateRange);
            // 不能关闭连接, 否则为 null, 引发空指针异常
            SqlUtil.execSql(
                    StrUtil.format(SettingsOfLowBuyFS.sqlDeleteExistDateRangeFS,
                            StrUtil.format("[\"{}\",\"{}\"]", statDateRange.get(0), statDateRange.get(1))),
                    connSingleton, false);
            // 主逻辑.
            // 主程序分析计算的几个参数用不到, 删除即可
            // 主程序使用 windowUsePeriodsCoreArg=7/8/9/10,
            // FS分析为了更加直观, 修改为 keyInt设定. 0代表next0, 即明日, 对应了主程序中的 7
            fsLowBuyDistributionDetailAnalyze(stocks, stockWithStDateRanges, stockWithBoard, statDateRange,
                    saveTablenameLowBuyFS, keyInt);

            String hardwareInfo = reportCpuMemoryDisk(true);
            MailUtil.send(SettingsCommon.receivers, StrUtil.format("LowBuy部分完成: {}", statDateRange),
                    StrUtil.format("LowBuy部分完成, 硬件信息:\n", hardwareInfo), false,
                    null);
            log.info("current time");
        }
    }

    // 核心逻辑: next0 low 分时分布详细分析

    /**
     * next0/1/2/3等,        low 在分时图中 出现的分析, 分析项目为以下几个.
     * --- 分析项目
     * 1.Low最低价出现时间(0-240表示).
     * 2.Low的左领域值 , 即low往左边计算, 左边相邻有多少个区域, 其价格比low高, 但是很相近, 例如low=-0.05, 相邻阈值可以 -0.045
     * 3.Low的右领域值 , 右边相近的时间数量
     * 4.
     *
     * @param stocks
     * @param stockWithStDateRanges
     * @param stockWithBoard
     * @param statDateRange
     * @param saveTablenameLowBuyFS
     * @param keyInt
     */
    private static void fsLowBuyDistributionDetailAnalyze(List<String> stocks,
                                                          HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                          DataFrame<String> stockWithBoard, List<String> statDateRange,
                                                          String saveTablenameLowBuyFS, int keyInt) {
    }

    public static Log log = LogFactory.get();
}
