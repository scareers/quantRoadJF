package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static com.scareers.utils.CommonUtils.showMemoryUsageMB;
import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 以今日收盘后, 决定明日低买, 后日高卖 为目标, 对2700w+原始数据表的的简单筛选脚本.
 * 倒数两个日期
 * Arrays.asList("20200203", "20210218"),
 * Arrays.asList("20210218", "21000101")
 * 这里我们 对 倒2进行主要筛选, 更早日期为辅助, 对 倒1日期, 则不引入未来数据
 *
 * @author: admin
 * @date: 2021/11/10  0010-8:12
 */
public class FilterSimpleFor0B1S {
    public static String saveTablenameFiltered = getSaveTablenameFiltered();
    // 读取当前配置中的数据表
    public static String sourceTablenameBeFilter = SettingsOfSingleKlineBasePercent.saveTablename;
    public static Connection connection = ConnectionFactory.getConnLocalKlineForms();

    public static List<String> algorithms = getAlgorithms(); // open,close,high,low
    public static List<Double> minVirtualGeometryMeans = Arrays.asList(-0.001, 0.001, 0.02, -0.05); // 注意参数与上面顺序匹配
    public static String sqlCreateFiteredSaveTable =
            StrUtil.format(SettingsOfSingleKlineBasePercent.sqlCreateSaveTableRaw, saveTablenameFiltered);
    //17个日期周期, 至少有8个才可能被选中
    public static Integer haveMinStatRanges = 8;
    //17个日期周期, 至少有8个才可能被选中; 且对最后 30%(不含最后一期), 进行>min的判定
    public static double gtMinVGMeanPercent = 0.7;


    public static void main(String[] args) throws Exception {
        execSql(sqlCreateFiteredSaveTable, connection);
        CountDownLatch latchOfParse = new CountDownLatch(algorithms.size());

        // 四种算法
        List<Integer> indexes = Arrays.asList(0, 1, 2, 3);
        for (Integer index : Tqdm.tqdm(indexes, StrUtil.format("process: "))) {
            String algorithm = algorithms.get(index);
            Double minVGMean = minVirtualGeometryMeans.get(index);


            // 全线程使用1个conn
            Future<ConcurrentHashMap<String, List<Double>>> f = poolOfParse
                    .submit(new StockSingleParseTask(latchOfParse, stock, stockWithBoard, statDateRange,
                            stockWithStDateRanges, connOfParse, windowUsePeriodsCoreArg));
            ConcurrentHashMap<String, List<Double>> resultTemp = f.get();
            for (String key : resultTemp.keySet()) {
                results.putIfAbsent(key, new ArrayList<>());
                results.get(key).addAll(resultTemp.get(key));
            }
            resultTemp.clear();
            if (parseProcess.incrementAndGet() % SettingsOfSingleKlineBasePercent.gcControlEpoch == 0) {
                System.gc();
                if (SettingsOfSingleKlineBasePercent.showMemoryUsage) {
                    showMemoryUsageMB();
                }
            }
        }
    }

    public static String getSaveTablenameFiltered() {
//        filtered_single_kline_from_next{total_use_periods - 7}
        String res = StrUtil.format("filtered_single_kline_from_next{}",
                SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7);
        if (SettingsOfSingleKlineBasePercent.excludeSomeBoards) {
            res = res + "__excybkcb";
        }
        return res;
    }

    public static List<String> getAlgorithms() {
        return Arrays
                .asList(
                        StrUtil.format("Next{}Open", SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}Close", SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}High", SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7),
                        StrUtil.format("Next{}Low", SettingsOfSingleKlineBasePercent.windowUsePeriodsCoreArg - 7)
                );
    }

}

/**
 * 依据算法, 和对应不同的 最小 VGMean , 进行简单的筛选
 */
class FilterWithAlgorithmAndMinVGMean implements Callable<String> {

    String algorithm;
    Double minVGMean;
    String sourceTablenameBeFilter;
    Connection connection;

    @Override
    public String call() throws Exception {
        Console.log("start: {} -- {}", algorithm, minVGMean);
        String sqlSelectGoodForm = StrUtil.format("select form_name,stat_result_algorithm\n" +
                "        from {}\n" +
                "        where virtual_geometry_mean > {}\n" +
                "            and effective_counts > 1\n" +
                "            and stat_result_algorithm = '{}'\n" +
                "            and stat_date_range = '[\"20200203\",\"20210218\"]'\n" +
                "        order by virtual_geometry_mean desc\n", sourceTablenameBeFilter, minVGMean, algorithm);
        // 初步选择, 倒数第二个 日期区间, 符合 算法, 且>VGMean 的.

        String sqlValidateGoodFormRaw = StrUtil.format("select virtual_geometry_mean\n" +
                "    from {}\n" +
                "    where stat_result_algorithm={}\n" +
                "      and form_name = {}\n" +
                "    order by stat_date_range\n" +
                "    limit 17", sourceTablenameBeFilter, algorithm); // 没有附带 formname

        DataFrame<Object> dfSelectedForms = DataFrame.readSql(connection, sqlSelectGoodForm);
        Console.log("{} - {} : {}", algorithm, minVGMean, dfSelectedForms.length());
        dfSelectedForms.cast(String.class);

        for (int i = 0; i < dfSelectedForms.length(); i++) {
            String formName = (String) dfSelectedForms.get(i, 0);
            String statResultAlgorithm = (String) dfSelectedForms.get(i, 1);

            DataFrame<Object> dfTemp = DataFrame.readSql(connection, StrUtil.format(sqlValidateGoodFormRaw, formName));

            if (dfTemp.length() < FilterSimpleFor0B1S.haveMinStatRanges) {
                continue;
            }
            dfTemp.convert(Double.class);
            boolean allGtMinVGMean = true;
            for (int j = (int) (dfTemp.size() * 0.7); j < dfTemp.size() - 1; j++) {
                // 排除掉了最后一期!   且仅仅对 倒数多期(不包含最后一期) 进行 >MinVGMean 的强制判定
                Double vgMean = (Double) dfTemp.get(j, 0);
                if (vgMean < minVGMean) {
                    allGtMinVGMean = false;
                    break;
                }
            }
            if(allGtMinVGMean){
                //
            }


        }

    }
}

