package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SingleKlineFormsBase;
import com.scareers.settings.SettingsCommon;
import com.scareers.sqlapi.TushareApi;
import com.scareers.utils.CommonUtils;
import com.scareers.utils.SqlUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.*;
import static com.scareers.utils.CommonUtils.showMemoryUsageMB;
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
     * 3.Low的右领域值 , 右边相近的时间数量    dominateRight
     * 4.Low2  次低价, 定义为排除掉 Low 以及其左右支配后, 剩余下面选择.
     * 5.Low2 的左右支配
     * 6.Low3 第三低价 及 左右支配
     * 7.Low /Low2/Low3 的具体数值.  // 总计12项目
     * --- 说明
     * 1.本脚本是给定形态集合,id, 分析之下 next Low 的相关分布.  本身行数不会多大. 列数多.
     * --- 保存的数据
     * 2.对单个形态集合,  基本分析以上 12项目:
     * 1.Low/2/3出现时间: 结果为 列表json字符串,   241个元素, 代表了在当分钟出现的次数!!!
     * 后续如果分区间分析, 也方便转换
     * 2.3个价格(百分比), 依旧采用 [] 区间分析方法.
     * 3.左支配时间:  因为最大可能 241个支配, 统一性, 同 出现时间, 用 241个数组
     * 4.右支配时间: 同理
     * --- 保存数据 总之
     *
     * @param stocks
     * @param stockWithStDateRanges
     * @param stockWithBoard
     * @param statDateRange
     * @param saveTablenameLowBuyFS
     * @param keyInt
     * @key: 保存 12项基本数据, 每种数据都是一个列表的 json字符串.
     * @key: 要对 12项基本 数组进行 分布分析,  因此考虑,  用 字段 "analyze_item_type" 列, 保存单条记录是什么"类型", 总计12种
     * @key: 数据表改造后, 单行数据, 表示了 某一项数据(一个列表)的分析结果.
     * analyze_item_type 表示分析的什么数据
     * detail_list  保存 那个json列表!   其余列, 表示对 detail_list 的统计分析
     */
    private static void fsLowBuyDistributionDetailAnalyze(List<String> stocks,
                                                          HashMap<String, List<List<String>>> stockWithStDateRanges,
                                                          DataFrame<String> stockWithBoard, List<String> statDateRange,
                                                          String saveTablenameLowBuyFS, int keyInt) {

        Console.log("构建结果字典");
        ConcurrentHashMap<String, List<Double>> results = new ConcurrentHashMap<>(8);
        ThreadPoolExecutor poolOfParse = new ThreadPoolExecutor(SettingsOfSingleKlineBasePercent.processAmountParse,
                SettingsOfSingleKlineBasePercent.processAmountParse * 2, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        CountDownLatch latchOfParse = new CountDownLatch(stocks.size());
        Connection connOfParse = ConnectionFactory.getConnLocalTushare();
        AtomicInteger parseProcess = new AtomicInteger(0);
        ArrayList<Future<ConcurrentHashMap<String, List<Double>>>> futuresOfParse = new ArrayList<>();
        for (String stock : stocks) {
            // 全线程使用1个conn
            Future<ConcurrentHashMap<String, List<Double>>> f = poolOfParse
                    .submit(new SingleKlineFormsBase.StockSingleParseTask(latchOfParse, stock, stockWithBoard,
                            statDateRange,
                            stockWithStDateRanges, connOfParse, windowUsePeriodsCoreArg));
            futuresOfParse.add(f);
        }
        List<Integer> indexesOfParse = CommonUtils.range(futuresOfParse.size());
        for (Integer i : Tqdm.tqdm(indexesOfParse, StrUtil.format("{} process: ", statDateRange))) {
            Future<ConcurrentHashMap<String, List<Double>>> f = futuresOfParse.get(i);
            ConcurrentHashMap<String, List<Double>> resultTemp = f.get();
            //            synchronized (results) {
            for (String key : resultTemp.keySet()) {
                // @bugfix: value的列表应该线程安全! 而非简单的AL;
                // @bigfix2: CopyOnWriteArrayList 由于使用锁, 对象过大, 内存不足; 因此使用同步关键字
                // @noti: 按照逻辑来讲, 此处本身就是串行, 不需要同步.
                results.putIfAbsent(key, new ArrayList<>());
                results.get(key).addAll(resultTemp.get(key));
            }
            //            }
            resultTemp.clear();
            if (parseProcess.incrementAndGet() % SettingsOfSingleKlineBasePercent.gcControlEpochParse == 0) {
                System.gc();
                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
                    showMemoryUsageMB();
                }
            }
        }
        //        latchOfParse.await(); // 不需要,
        //        connOfParse.close(); // 关闭连接
        poolOfParse.shutdown(); // 关闭线程池
        System.out.println();
        Console.log("results size: {}", results.size());
        System.gc();
    }

    public static Log log = LogFactory.get();
}
