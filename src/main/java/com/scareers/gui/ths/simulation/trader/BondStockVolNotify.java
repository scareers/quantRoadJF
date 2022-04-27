package com.scareers.gui.ths.simulation.trader;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 可转债和正股, 当正股分时成交, 出现 "巨量"时, 立即提示
 *
 * @author: admin
 * @date: 2022/3/22/022-17:10:35
 */
public class BondStockVolNotify {

    // 记录保存每只股票, 最后一次提示时的涨跌幅差距;
    // 当新的涨跌幅 变得更大, 或者变得更小, 会提示 扩大到/缩小到 xxx
    // 变化阈值 百分之0.5
    public static HashMap<String, Double> lastNotifiMap = new HashMap<>();
    public static double changeThreshold = 0.5;
    public static double notiThreshold = 1; // 百分比差距大于此值, 才进行播报

    public static void main(String[] args) throws Exception {
        main0();



    }


    public static HashMap<String, Double> pre5DayAvgFsTransAmount = new HashMap<>(); // 股票前5日平均单tick分时成交额

    public static void fillPre5DayAvgFsTransAmountMap(List<SecurityBeanEm> stockList) {
        log.warn("计算前5日分时成交量平均值");
        for (SecurityBeanEm beanEm : stockList) {

            List<Double> amounts = new ArrayList<>(); // 所有分时成交量

            for (int i = 1; i < 6; i++) {
                String tradeDate = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
                DataFrame<Object> fsTransByDateAndQuoteId = EastMoneyDbApi
                        .getFsTransByDateAndQuoteId(tradeDate, beanEm.getQuoteId(), true);
                List<Double> vol = DataFrameS.getColAsDoubleList(fsTransByDateAndQuoteId, "vol");

                for (int j = 0; j < vol.size(); j++) {
                    Double vol0 = vol.get(j);
                    if (vol0 > 0) {
                        amounts.add(vol0);
                    }
                }

            }

            double avg = CommonUtil.avgOfListNumberUseLoop(amounts);
            pre5DayAvgFsTransAmount.put(beanEm.getQuoteId(), avg);
        }

    }

    // 相对前5日平均成交量
    public static final double bigBuyVolRate = 8; // 大买倍率
    public static final double skyBuyVolRate = 15; // 天买倍率
    public static final double bigSellVolRate = 7; // 大卖倍率
    public static final double skySellVolRate = 14; // 天卖倍率

    // 相对今日平均成交量
    public static final double bigBuyVolRateToday = 6; // 大买倍率
    public static final double skyBuyVolRateToday = 12; // 天买倍率
    public static final double bigSellVolRateToday = 5; // 大卖倍率
    public static final double skySellVolRateToday = 10; // 天卖倍率

    // 每只转债正股大买单出现, 将有一个 时间tick, 保留某转债最后一次提示时的时间tick; 若下次循环依然是此tick, 将不再提示
    // key为 股票bean的 quoteId, value是最后一次提示时的时间, 例如09:40:03
    public static HashMap<String, String> lastNotiTimeTickMap = new HashMap<>();

    public static void main0() throws Exception {
        log.info("解析昨日前100成交量可转债及股票");
        List<StockBondBean> hotStockWithBondList = getHotStockWithBond();
        List<SecurityBeanEm> stockList = null;
        List<SecurityBeanEm> bondList = null;
        try {
            log.info("解析股票/转债bean");
            stockList = SecurityBeanEm.createStockList(
                    hotStockWithBondList.stream().map(StockBondBean::getStockCode).collect(Collectors.toList()));
            bondList = SecurityBeanEm.createBondList(
                    hotStockWithBondList.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()),
                    true);
        } catch (Exception e) {
            e.printStackTrace();
            ManiLog.put("问财获取股票转债列表失败");
            return;
        }
        // 4.加入分时成交爬虫池
        SecurityPool.addToTodaySelectedBonds(bondList);
        SecurityPool.addToTodaySelectedStocks(stockList); // 加入后, 爬虫自动获取
        FsTransactionFetcher fsTransactionFetcher =
                FsTransactionFetcher.getInstance(10,
                        "15:10:00", 1000, 100, 32); // 跟Trader相同参数
        fsTransactionFetcher.startFetch();

        // 5.对股票池, 读取其前5日的分时成交数据, 然后求出一个平均的 分时成交量! 然后设定一个倍数, 作为 "大量" 的标准
        // 将这个 基准平均分时成交量, 保存为集合. key为股票 quoteId, 值为 平均分时成交额     -- 使用成交额而非成交量
        fillPre5DayAvgFsTransAmountMap(stockList);
        Console.log(pre5DayAvgFsTransAmount);

        while (true) {
            ThreadUtil.sleep(100);


            for (StockBondBean stockBondBean : hotStockWithBondList) {

                SecurityBeanEm stockBean = SecurityBeanEm.createStock(stockBondBean.getStockCode());
                Double avgVolPre5 = pre5DayAvgFsTransAmount.get(stockBean.getQuoteId());
                if (avgVolPre5 == null) {
                    log.warn("近5日平均分时成交量为null,跳过: {}", stockBean.getQuoteId());
                    continue;
                }

                DataFrame<Object> fsTransData = FsTransactionFetcher.getFsTransData(stockBean);
                if (fsTransData == null || fsTransData.length() == 0) {
                    continue;
                }
                // sec_code	market	time_tick	price	 vol	bs, 使用顺序
                List<Double> volList = DataFrameS.getColAsDoubleList(fsTransData, "vol");
                double vol = volList.get(volList.size() - 1);
                int bs = Integer.parseInt(fsTransData.get(fsTransData.length() - 1, 5).toString());
                String timeTickLast = fsTransData.get(fsTransData.length() - 1, 2).toString();
                if (timeTickLast.compareTo("09:30:00") < 0) {
                    continue; // 开盘后才播报
                }
                String notiedTimeTick = lastNotiTimeTickMap.get(stockBean.getQuoteId());
                if (timeTickLast.equals(notiedTimeTick)) {
                    continue; // 没有新数据, 当前最后一条数据, 提示过了
                }
                SecurityBeanEm bondBean = SecurityBeanEm.createBond(stockBondBean.getBondCode());

                String description = null;

                // 0.1: 当时间过去一段时间后, 今日数据已经有部分了, 则计算 今日平均值
                // 当时间>10:00:00后, 计算今日成交量倍率
                if (timeTickLast.compareTo("10:00:00") >= 0) { // 10点后计算
                    double rate2 = vol / CommonUtil.avgOfListNumberUseLoop(volList);
                    if (bs == 2) {
                        if (rate2 >= skyBuyVolRateToday) {
                            description = "天买";
                        } else if (rate2 >= bigBuyVolRateToday) {
                            description = "大买";
                        }
                    } else if (bs == 1) {
//                        if (rate2 >= skySellVolRateToday) {
//                            description = "天卖";
//                        } else if (rate2 >= bigSellVolRateToday) {
//                            description = "大卖";
//                        }
                    }
                }

                // 0.2: 当前成交量 / 5日平均分时成交量 倍率
                if (description == null) {
                    double rate = vol / avgVolPre5;

                    if (bs == 2) {
                        if (rate < bigBuyVolRate) {
                            continue;
                        } else if (rate < skyBuyVolRate) {
                            description = "大买";
                        } else {
                            description = "天买";
                        }
                    } else if (bs == 1) {
                        if (rate < bigSellVolRate) {
                            continue;
                        }
//                        else if (rate < skySellVolRate) {
//                            description = "大卖";
//                        } else {
//                            description = "天卖";
//                        }
                    } else {
                        continue;
                    }
                }

                if (description == null) {
                    continue;
                }

                // 长信息提示
                String infoLong = StrUtil
                        .format("{} 正股{}倍率: {}  {} -- {}", bondBean.getName(), description, vol / avgVolPre5,
                                fsTransData.row(fsTransData.length() - 1), avgVolPre5);
                Console.log(infoLong);

                // 短信息
                String infoShort = StrUtil.format("{}{}", bondBean.getName().replace("转债", ""), description);

                try {
                    ManiLog.put(infoLong);
                } catch (Exception e) {
                }

                Tts.playSound(infoShort, true);


                lastNotiTimeTickMap.put(stockBean.getQuoteId(), timeTickLast);

            }


        }


    }

    private static final Log log = LogUtil.getLogger();


    public static List<StockBondBean> getHotStockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("剩余规模<100亿;上一交易日成交额排名从大到小前500;正股代码",
                WenCaiApi.TypeStr.BOND);
        List<StockBondBean> stockBondBeanList = new ArrayList<>();

        /*
万孚转债
福能转债
湖广转债
设研转债
长久转债
北港转债

         */
        HashSet<String> careBonds = new HashSet<>(
                StrUtil.split("交建转债,万孚转债,福能转债,湖广转债,设研转债,长久转债,北港转债", ",")
        );

        for (int i = 0; i < dataFrame.length(); i++) {
            if (!careBonds.contains(dataFrame.get(i, "可转债@可转债简称").toString())) {
                continue;
            }

            stockBondBeanList.add(new StockBondBean(
                    dataFrame.get(i, "可转债@正股简称").toString(),
                    dataFrame.get(i, "可转债@正股代码").toString().substring(0, 6),
                    dataFrame.get(i, "可转债@可转债简称").toString(),
                    dataFrame.get(i, "code").toString(),
                    -1
            ));

        }
        Console.log(stockBondBeanList.get(0));
        Console.log(stockBondBeanList.size());
        return stockBondBeanList;
    }


}
