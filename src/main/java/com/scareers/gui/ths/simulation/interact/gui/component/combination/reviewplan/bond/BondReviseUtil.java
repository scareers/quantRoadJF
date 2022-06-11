package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.NumberUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.charts.EmChartFs;
import joinery.DataFrame;

import java.util.*;

/**
 * description: 重载, 减少主类行数, 降低idea负担
 *
 * @author: admin
 * @date: 2022/6/11/011-14:39:17
 */
public class BondReviseUtil {

    public static final int tick3sLogPanelWidth = EmChartFs.DynamicEmFs1MV2ChartForRevise.tickLogPanelWidthDefault; // 3stick数据显示组件宽度
    public static final double timeRateDefault = 1.0; // 默认复盘时间倍率
    // 转债全列表, 是否使用问财实时列表; 若不, 则使用数据库对应日期列表; @noti: 目前问财的成交额排名, 似乎有bug, 无法排名正确
    public static final boolean bondListUseRealTimeWenCai = true;
    public static final boolean loadAllFsDataFromDbWhenFlushBondList = true; // @key: 更新转债列表显示时, 是否载入所有fs数据
    public static List<String> bondTableColNames = Arrays.asList("代码", "名称", "涨跌幅", "成交额");
    // 主循环因代码执行而损耗的时间, 修正每次循环的sleep值, 以符合理论! 将自动调整sleep值! <-- 2毫秒约等于循环的代码执行(排除sleep)时间,
    public static volatile long codeExecLossSleepFixSimulation = 2;
    public static final int kLineAmountHope = 170; // k线图希望的数量
    // 今天多少点钟后, 复盘默认日期设置为今天(一般这时爬虫运行过了,数据库有数据了) , 否则设置默认复盘日期为上一交易日
    public static final int afterTodayNHDefaultDateAsToday = 20;
    public static final double accountInitMoney = 10 * 10000;
    public static final double accountStateFlushSleep = 1500; // 子线程不断刷新账户状态, 时间间隔
    public static final double buySellPriceBias = 5.0; // 买卖时, 挂单价格, 在最新价格的偏移价格! 元
    public static final String nuclearKeyBoardSettingDstDir = "C:\\Users\\admin\\Desktop\\自设键盘"; // 核按钮键盘配置文件目标文件夹
    public static final String nuclearKeyBoardSettingOfThs = "ths/nuclear/nuclear/raw"; // 核按钮键盘配置文件 -- classpath中路径 --    // 同花顺原配置
    public static final String nuclearKeyBoardSettingOfRevise = "ths/nuclear/nuclear/revise"; // 核按钮键盘配置文件 -- classpath中路径 -- 复盘时使用配置
    public static final long dummyBuySellOperationSleep = 500; // 模拟交易的弹窗持续的时间
    public static final long dummyClinchOccurSleep = 1000; // 模拟买卖后, 到成交时间,大约sleep多久, 在上个sleep之后


    /**
     * 初始化复盘软件使用的 核按钮配置!
     */
    public static void initNuclearKeyBoardSettingForRevise() {
        try {
            String x = CommonUtil.getFullPathOfClassPathFileOrDir(nuclearKeyBoardSettingOfRevise);
            FileUtil.copyFilesFromDir(FileUtil.file(x), FileUtil.file(nuclearKeyBoardSettingDstDir), true);
            CommonUtil.notifyInfo("核按钮键盘已更改配置: 复盘配置");
        } catch (IORuntimeException e) {
            CommonUtil.notifyError("核按钮键盘更改配置失败 --> 复盘配置");
        }
    }

    /**
     * 恢复核按钮配置, 到同花顺配置
     */
    public static void recoverNuclearKeyBoardSettingToThs() {
        try {
            String x = CommonUtil.getFullPathOfClassPathFileOrDir(nuclearKeyBoardSettingOfThs);
            FileUtil.copyFilesFromDir(FileUtil.file(x), FileUtil.file(nuclearKeyBoardSettingDstDir), true);
            CommonUtil.notifyInfo("核按钮键盘已恢复配置: 同花顺配置");
        } catch (IORuntimeException e) {
            CommonUtil.notifyError("核按钮键盘恢复配置失败 --> 同花顺配置");
        }
    }

    /**
     * //@key: 重要方法, 给定完整的 fs成交Df, 给定两个常态时间tick字符串, 返回 介于两个tick之间的部分df,
     * 新建的df对象; 且两个tick都可能包括(如果恰好有数据的话)!
     *
     * @param fsTransDfFull
     * @param startTick
     * @param endTick
     * @return
     */
    public static DataFrame<Object> getEffectDfByTickRange(DataFrame<Object> fsTransDfFull, String startTick,
                                                           String endTick) {
        if (fsTransDfFull == null) {
            return null;
        }
        // @update: 需要>=9:30:00, 否则竞价的全部进去了, 不合事实
        int shouldStartIndex = -1;
        for (int i = 0; i < fsTransDfFull.length(); i++) {
            String timeTick1 = fsTransDfFull.get(i, "time_tick").toString();
            if (timeTick1.compareTo(startTick) > 0) {
                break; // 不要>=, 本索引包含, 将包含 09:30:00 这个tick;
            } else {
                shouldStartIndex = i; // 本质上, 会包含 <=9:30:00 的第一个tick, 符合事实!
            }
        }
        if (shouldStartIndex == -1) {
            return null;
        }

        // 3. 筛选有效分时成交! time_tick 列
        int shouldIndex = -1;
        for (int i = 0; i < fsTransDfFull.length(); i++) {
            String timeTick1 = fsTransDfFull.get(i, "time_tick").toString();
            if (timeTick1.compareTo(endTick) <= 0) {
                shouldIndex = i; // 找到截断索引
            } else {
                break;
            }
        }
        if (shouldIndex == -1) {
            return null; // 筛选不到
        }
        if (shouldStartIndex > shouldIndex) { // 例如时间< 9:30时, 将会
            return null;
        }
        return DataFrameS.copy(fsTransDfFull
                .slice(shouldStartIndex, Math.min(shouldIndex + 1, fsTransDfFull.length())));
    }

    /**
     * 复盘期间, 给定转债列表, 给定 日期和时间, 生成 转债"实时"数据截面列表df, 被table展示
     * 模拟实盘下, 点击全债券列表的展示页面, 对整个市场有个宏观展示
     * 当前仅展示 涨幅(很好计算) 和 当前总成交额(需要分时成交求和, 计算量稍大)
     *
     * @param bondList
     * @return
     */
    public static DataFrame<Object> getReviseTimeBondListOverviewDataDf(List<SecurityBeanEm> bondList, String date,
                                                                        String timeTick) {
        HashMap<SecurityBeanEm, Double> chgPctRealTime = new HashMap<>(); // 涨跌幅
        HashMap<SecurityBeanEm, Double> amountRealTime = new HashMap<>(); // 成交额
        for (SecurityBeanEm bondBean : bondList) {
            // 1.昨收, 计算涨跌幅
            Double preClose = EastMoneyDbApi.getPreCloseOf(date, bondBean.getQuoteId());
            if (preClose == null) {
                continue;
            }
            // 2.分时成交
            DataFrame<Object> fsTransDf = EastMoneyDbApi
                    .getFsTransByDateAndQuoteIdS(date, bondBean.getQuoteId(), false);
            if (fsTransDf == null) {
                continue;
            }

            DataFrame<Object> effectDf = getEffectDfByTickRange(fsTransDf, "09:29:59", timeTick);
            if (effectDf == null || effectDf.length() == 0) {
                continue;
            }
            // 4.涨跌幅很好计算
            Double newestPrice = Double.valueOf(effectDf.get(effectDf.length() - 1, "price").toString());
            chgPctRealTime.put(bondBean, newestPrice / preClose - 1);

            // 5.总计成交额, 需要强行计算! price 和 vol 列, vol手数 需要转换为 张数, *10
            amountRealTime.put(bondBean, getAmountOfEffectDf(bondBean, effectDf));
        }

        // 1.构建结果df! 列简单: 代码,名称, 涨跌幅, 当前总成交额!
        DataFrame<Object> res = new DataFrame<>(bondTableColNames);
        for (SecurityBeanEm beanEm : bondList) {
            List<Object> row = new ArrayList<>();
            row.add(beanEm.getSecCode());
            row.add(beanEm.getName());
            row.add(chgPctRealTime.get(beanEm));
            row.add(amountRealTime.get(beanEm)); // 涨跌幅成交额都可能是 null, 但保证需要所有转债;
            res.append(row);
        }

        // 2. 无需排序, 自行使用 JXTable 的排序功能! 但转换为数字排序, 是需要重新一下排序逻辑的, 默认按照字符串排序
        //Console.log(res.toString(10));
        return res;
    }


    /*
    对分时成交df, 的筛选, 然后求 最新价, 最高,最低, 总成交额 的简单方法
     */

    /**
     * 求筛选出的部分分时成交df, 的总计成交量
     *
     * @param bondBean
     * @param effectDf
     * @return
     */
    public static Double getAmountOfEffectDf(SecurityBeanEm bondBean, DataFrame<Object> effectDf) {
        int volRate = bondBean.isBond() ? 10 : 100;
        List<Double> tickAmountList = new ArrayList<>();
        for (int i = 0; i < effectDf.length(); i++) {
            Object price = effectDf.get(i, "price");
            Object vol = effectDf.get(i, "vol");
            tickAmountList.add(Double.parseDouble(price.toString()) * Double.parseDouble(vol.toString()) * volRate);
        }
        return CommonUtil.sumOfListNumberUseLoop(tickAmountList);
    }


    /**
     * 求筛选出的部分分时成交df, 的 阶段性最大价格
     *
     * @param effectDf
     * @return
     */
    public static Double getHighOfEffectDf(DataFrame<Object> effectDf) {
        return CommonUtil.maxOfListDouble(DataFrameS.getColAsDoubleList(effectDf, "price"));
    }

    public static Double getLowOfEffectDf(DataFrame<Object> effectDf) {
        return CommonUtil.minOfListDouble(DataFrameS.getColAsDoubleList(effectDf, "price"));
    }

    public static Double getCloseOfEffectDf(DataFrame<Object> effectDf) {
        return Double.valueOf(effectDf.get(effectDf.length() - 1, "price").toString());
    }

    /**
     * sleep衰减设置值队列, 求平均值, 去掉最大和最小
     * 仅一次循环求3值, 尽量最快速度! 不调用常规 求和/max/min  api
     *
     * @param deque
     * @return
     */
    public static long getTheAvgOfDequeExcludeMaxAndMin(ArrayDeque<Long> deque) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Long value : deque) {
            sum = sum + value;
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        // 默认四舍五入
        return NumberUtil.round((sum - min - max) * 1.0 / (deque.size() - 2), 0).longValue();
    }
}
