package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.settings.SettingsCommon;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg;
import static com.scareers.utils.HardwareUtil.reportCpuMemoryDisk;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 以今日收盘后, 决定明日低买, 后日高卖 为目标, 对2700w+原始数据表的的简单筛选脚本.
 * 倒数两个日期
 * Arrays.asList("20200203", "20210218"),
 * Arrays.asList("20210218", "21000101")
 * 这里我们 对 倒2进行主要筛选, 更早日期为辅助, 对 倒1日期, 则不引入未来数据
 * <p>
 * noti:
 * 同python, 主要设置是 设置类的:  windowUsePeriodsCoreArg = 7;
 *
 * @author: admin
 * @date: 2021/11/10  0010-8:12
 * <p>
 * 2.对全数据进行 初步条件筛选的脚本
 */
public class FilterSimpleFor0B1S {
    // 主要设置是 设置类的:  windowUsePeriodsCoreArg = 7; 更改即可筛选不同基表

    public static String saveTablenameFiltered = getSaveTablenameFiltered();
    // 读取当前配置中的数据表
    public static String sourceTablenameBeFilter = SettingsOfSingleKlineBasePercent.saveTablename;
    public static Connection connection = ConnectionFactory.getConnLocalKlineForms();

    // open,close,high,low
    public static List<String> algorithms = getAlgorithms();
    // 注意参数与上面顺序匹配
    public static List<Double> minVirtualGeometryMeans = Arrays.asList(-0.001, 0.001, 0.02, -0.05);
    public static String sqlCreateFiteredSaveTable =
            StrUtil.format(SettingsOfSingleKlineBasePercent.sqlCreateSaveTableRaw, saveTablenameFiltered);
    //17个日期周期, 至少有8个才可能被选中

    public static Integer haveMinStatRanges = 8;
    //17个日期周期, 至少有8个才可能被选中; 且对最后 30%(不含最后一期), 进行>min的判定; 数值越小, 判定越加严格
    public static double gtMinVGMeanPercent = 0.7;
    public static Log log = LogFactory.get();


    public static void main(String[] c) throws Exception {
//        setBufferPoolSizeGB(40, connection);

        TimeInterval timer = DateUtil.timer();
        timer.start();
        log.info("current time");

        // 不能关闭
        execSql(sqlCreateFiteredSaveTable, connection, false);
        // 四种算法
        List<Integer> indexes = CommonUtil.range(algorithms.size());

        CountDownLatch latch = new CountDownLatch(algorithms.size());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(4,
                8, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        List<Future<String>> futures = new ArrayList<>();
        for (Integer index : Tqdm.tqdm(indexes, StrUtil.format("process: "))) {
            String algorithm = algorithms.get(index);
            Double minVGMean = minVirtualGeometryMeans.get(index);
            // 全线程使用1个conn
            Future<String> f = pool
                    .submit(new FilterWithAlgorithmAndMinVGMean(algorithm, minVGMean, sourceTablenameBeFilter,
                            connection, saveTablenameFiltered, latch));
            futures.add(f);
        }

        for (Future<String> future : futures) {
            future.get();
        }
        MailUtil.send(SettingsCommon.receivers,
                StrUtil.format("简易筛选完成,windowUsePeriodsCoreArg:{}", windowUsePeriodsCoreArg),
                StrUtil.format("简易筛选完成,耗时: {}h \n硬件信息:{}",
                        (double) timer.intervalRestart() / 3600000, reportCpuMemoryDisk(true)),
                false, null);
        pool.shutdown();
        log.info("current time");
    }

    public static String getSaveTablenameFiltered() {
//        filtered_single_kline_from_next{total_use_periods - 7}
        String res = StrUtil.format("filtered_single_kline_from_next{}",
                windowUsePeriodsCoreArg - 7);
        if (SettingsOfSingleKlineBasePercent.excludeSomeBoards) {
            res = res + "__excybkcb";
        }
        return res;
    }

    public static List<String> getAlgorithms() {
        return Arrays
                .asList(
                        StrUtil.format("Next{}Open", windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}Close", windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}High", windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}Low", windowUsePeriodsCoreArg - 7)
                );
    }

    /**
     * 依据算法, 和对应不同的 最小 VGMean , 进行简单的筛选
     */
    public static class FilterWithAlgorithmAndMinVGMean implements Callable<String> {

        String algorithm;
        Double minVGMean;
        String sourceTablenameBeFilter;
        Connection connection;
        String saveTablenameFiltered;
        CountDownLatch latch;

        public FilterWithAlgorithmAndMinVGMean(String algorithm, Double minVGMean, String sourceTablenameBeFilter,
                                               Connection connection, String saveTablenameFiltered,
                                               CountDownLatch latch) {
            this.algorithm = algorithm;
            this.minVGMean = minVGMean;
            this.sourceTablenameBeFilter = sourceTablenameBeFilter;
            this.connection = connection;
            this.saveTablenameFiltered = saveTablenameFiltered;
            this.latch = latch;
        }

        @Override
        public String call() throws Exception {
            try {
                Console.log("start: {} -- {}", algorithm, minVGMean);
                String sqlSelectGoodForm = StrUtil.format("select form_name,stat_result_algorithm\n" +
                        "        from {}\n" +
                        "        where virtual_geometry_mean > {}\n" +
                        "            and effective_counts > 1\n" +
                        "            and stat_result_algorithm = '{}'\n" +
                        "            and stat_date_range = '[\"20200203\",\"20210218\"]'\n" +
                        "        order by virtual_geometry_mean desc\n", sourceTablenameBeFilter, minVGMean, algorithm);
                // 初步选择, 倒数第二个 日期区间, 符合 算法, 且>VGMean 的.

                String sqlValidateGoodFormRaw = StrUtil.format("select stat_date_range,virtual_geometry_mean\n" +
                        // 仅仅读取2个字段, 只是做验证. 验证成功再读取全部字段
                        "    from {}\n" +
                        "    where stat_result_algorithm='{}'\n" +
                        "      and form_name = '{}'\n" +
                        "    order by stat_date_range\n" +
                        "    limit 17", sourceTablenameBeFilter, algorithm);
                // 没有附带 formname

                String sqlSaveGoodFormRaw = StrUtil.format("select *\n" +
                        // 同上, 只不过读取全部字段
                        "    from {}\n" +
                        "    where stat_result_algorithm='{}'\n" +
                        "      and form_name = '{}'\n" +
                        "    order by stat_date_range\n" +
                        "    limit 17", sourceTablenameBeFilter, algorithm); // 没有附带 formname

                DataFrame<Object> dfSelectedForms = DataFrame.readSql(connection, sqlSelectGoodForm);
                Console.log("{} - {} : {}", algorithm, minVGMean, dfSelectedForms.length());
                dfSelectedForms.cast(String.class);
                List<Integer> indexes = CommonUtil.range(dfSelectedForms.length());
                for (Integer i : Tqdm.tqdm(indexes, StrUtil.format("{} process", algorithm))) {
                    String formName = (String) dfSelectedForms.get(i, 0);

                    DataFrame<Object> dfTemp = DataFrame
                            .readSql(connection, StrUtil.format(sqlValidateGoodFormRaw, formName));

                    if (dfTemp.length() < FilterSimpleFor0B1S.haveMinStatRanges) {
                        continue;
                    }
                    dfTemp.convert(Double.class);
                    boolean allGtMinVGMean = true;
                    for (int j = (int) (dfTemp.size() * gtMinVGMeanPercent); j < dfTemp.size() - 1; j++) {
                        // 排除掉了最后一期!   且仅仅对 倒数多期(不包含最后一期) 进行 >MinVGMean 的强制判定
                        Double vgMean = (Double) dfTemp.get(j, 1);
                        if (vgMean < minVGMean) {
                            allGtMinVGMean = false;
                            break;
                        }
                    }
                    if (allGtMinVGMean) {
                        // 读取全部字段
                        DataFrame<Object> dfSave = DataFrame
                                .readSql(connection, StrUtil.format(sqlSaveGoodFormRaw, formName));
                        DataFrameS.toSql(dfSave, saveTablenameFiltered, connection, "append", null);
                    }
                }
                return "success";
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
                return "success";
            }
        }
    }
}



/*

数量记录
-0.001, 0.001, 0.02, -0.05
Open, Close, High, Low

Next0Open   158052 // todo: 待修改
Next0Close  158388
Next0High   248706
Next0Low    326498


Next1Open   153535
Next1Close  149224
Next1High   192686
Next1Low    272175

Next2Open   157074
Next2Close  152660
Next2High   183938
Next2Low    264138

Next3Open   159178
Next3Close  150845
Next3High   179207
Next3Low    256958

Next4Open   157918
Next4Close  151536
Next4High   174520
Next4Low    253360

 */