package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.bond.EmConvertibleBondApi;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.interact.gui.notify.bondbuyalgorithm.ChgPctAlgorithm;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.gui.ths.simulation.order.Order;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections.functors.FalsePredicate;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import static com.scareers.utils.CommonUtil.waitForever;

/**
 * description: 可转债和正股, 当正股分时成交, 出现 "巨量"时, 立即提示
 *
 * @author: admin
 * @date: 2022/3/22/022-17:10:35
 */
public class BondBuyNotify {

    // 记录保存每只股票, 最后一次提示时的涨跌幅差距;
    // 当新的涨跌幅 变得更大, 或者变得更小, 会提示 扩大到/缩小到 xxx
    // 变化阈值 百分之0.5
    public static HashMap<String, Double> lastNotifiMap = new HashMap<>();
    public static double changeThreshold = 0.5;
    public static double notiThreshold = 1; // 百分比差距大于此值, 才进行播报

    public static void main(String[] args) throws Exception {
//        startUpdateBondListTask(false);
//        ThreadUtil.sleep(20000);
//        Console.log(SecurityPool.allSecuritySet.size());
//        waitForever();

//        StaticData.flushBondPreCloseMap();
//        Console.log(StaticData.bondPreCloseMap.size());

        main1();

    }

    //
//    public static HashMap<String, Double> pre5DayAvgFsTransAmount = new HashMap<>(); // 股票前5日平均单tick分时成交额
//
//    public static void fillPre5DayAvgFsTransAmountMap(List<SecurityBeanEm> stockList) {
//        log.warn("计算前5日分时成交量平均值");
//        for (SecurityBeanEm beanEm : stockList) {
//
//            List<Double> amounts = new ArrayList<>(); // 所有分时成交量
//
//            for (int i = 1; i < 6; i++) {
//                String tradeDate = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
//                DataFrame<Object> fsTransByDateAndQuoteId = EastMoneyDbApi
//                        .getFsTransByDateAndQuoteId(tradeDate, beanEm.getQuoteId(), true);
//                List<Double> vol = DataFrameS.getColAsDoubleList(fsTransByDateAndQuoteId, "vol");
//
//                for (int j = 0; j < vol.size(); j++) {
//                    Double vol0 = vol.get(j);
//                    if (vol0 > 0) {
//                        amounts.add(vol0);
//                    }
//                }
//
//            }
//
//            double avg = CommonUtil.avgOfListNumberUseLoop(amounts);
//            pre5DayAvgFsTransAmount.put(beanEm.getQuoteId(), avg);
//        }
//
//    }
//
//    // 相对前5日平均成交量
//    public static final double bigBuyVolRate = 8; // 大买倍率
//    public static final double skyBuyVolRate = 15; // 天买倍率
//    public static final double bigSellVolRate = 7; // 大卖倍率
//    public static final double skySellVolRate = 14; // 天卖倍率
//
//    // 相对今日平均成交量
//    public static final double bigBuyVolRateToday = 6; // 大买倍率
//    public static final double skyBuyVolRateToday = 12; // 天买倍率
//    public static final double bigSellVolRateToday = 5; // 大卖倍率
//    public static final double skySellVolRateToday = 10; // 天卖倍率
//
//    // 每只转债正股大买单出现, 将有一个 时间tick, 保留某转债最后一次提示时的时间tick; 若下次循环依然是此tick, 将不再提示
//    // key为 股票bean的 quoteId, value是最后一次提示时的时间, 例如09:40:03
//    public static HashMap<String, String> lastNotiTimeTickMap = new HashMap<>();
//
////    public static void main0() throws Exception {
////        log.info("解析昨日前100成交量可转债及股票");
////        List<StockBondBean> hotStockWithBondList = getHotStockWithBond();
////        List<SecurityBeanEm> stockList = null;
////        List<SecurityBeanEm> bondList = null;
////        try {
////            log.info("解析股票/转债bean");
////            stockList = SecurityBeanEm.createStockList(
////                    hotStockWithBondList.stream().map(StockBondBean::getStockCode).collect(Collectors.toList()));
////            bondList = SecurityBeanEm.createBondList(
////                    hotStockWithBondList.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()),
////                    true);
////        } catch (Exception e) {
////            e.printStackTrace();
////            ManiLog.put("问财获取股票转债列表失败");
////            return;
////        }
////        // 4.加入分时成交爬虫池
////        SecurityPool.addToTodaySelectedBonds(bondList);
////        SecurityPool.addToTodaySelectedStocks(stockList); // 加入后, 爬虫自动获取
////        FsTransactionFetcher fsTransactionFetcher =
////                FsTransactionFetcher.getInstance(10,
////                        "15:10:00", 1000, 100, 32); // 跟Trader相同参数
////        fsTransactionFetcher.startFetch();
////
////        // 5.对股票池, 读取其前5日的分时成交数据, 然后求出一个平均的 分时成交量! 然后设定一个倍数, 作为 "大量" 的标准
////        // 将这个 基准平均分时成交量, 保存为集合. key为股票 quoteId, 值为 平均分时成交额     -- 使用成交额而非成交量
////        fillPre5DayAvgFsTransAmountMap(stockList);
////        Console.log(pre5DayAvgFsTransAmount);
////
////        while (true) {
////            ThreadUtil.sleep(100);
////
////
////            for (StockBondBean stockBondBean : hotStockWithBondList) {
////
////                SecurityBeanEm stockBean = SecurityBeanEm.createStock(stockBondBean.getStockCode());
////                Double avgVolPre5 = pre5DayAvgFsTransAmount.get(stockBean.getQuoteId());
////                if (avgVolPre5 == null) {
////                    log.warn("近5日平均分时成交量为null,跳过: {}", stockBean.getQuoteId());
////                    continue;
////                }
////
////                DataFrame<Object> fsTransData = FsTransactionFetcher.getFsTransData(stockBean);
////                if (fsTransData == null || fsTransData.length() == 0) {
////                    continue;
////                }
////                // sec_code	market	time_tick	price	 vol	bs, 使用顺序
////                List<Double> volList = DataFrameS.getColAsDoubleList(fsTransData, "vol");
////                double vol = volList.get(volList.size() - 1);
////                int bs = Integer.parseInt(fsTransData.get(fsTransData.length() - 1, 5).toString());
////                String timeTickLast = fsTransData.get(fsTransData.length() - 1, 2).toString();
////                if (timeTickLast.compareTo("09:30:00") < 0) {
////                    continue; // 开盘后才播报
////                }
////                String notiedTimeTick = lastNotiTimeTickMap.get(stockBean.getQuoteId());
////                if (timeTickLast.equals(notiedTimeTick)) {
////                    continue; // 没有新数据, 当前最后一条数据, 提示过了
////                }
////                SecurityBeanEm bondBean = SecurityBeanEm.createBond(stockBondBean.getBondCode());
////
////                String description = null;
////
////                // 0.1: 当时间过去一段时间后, 今日数据已经有部分了, 则计算 今日平均值
////                // 当时间>10:00:00后, 计算今日成交量倍率
////                if (timeTickLast.compareTo("10:00:00") >= 0) { // 10点后计算
////                    double rate2 = vol / CommonUtil.avgOfListNumberUseLoop(volList);
////                    if (bs == 2) {
////                        if (rate2 >= skyBuyVolRateToday) {
////                            description = "天买";
////                        } else if (rate2 >= bigBuyVolRateToday) {
////                            description = "大买";
////                        }
////                    } else if (bs == 1) {
//////                        if (rate2 >= skySellVolRateToday) {
//////                            description = "天卖";
//////                        } else if (rate2 >= bigSellVolRateToday) {
//////                            description = "大卖";
//////                        }
////                    }
////                }
////
////                // 0.2: 当前成交量 / 5日平均分时成交量 倍率
////                if (description == null) {
////                    double rate = vol / avgVolPre5;
////
////                    if (bs == 2) {
////                        if (rate < bigBuyVolRate) {
////                            continue;
////                        } else if (rate < skyBuyVolRate) {
////                            description = "大买";
////                        } else {
////                            description = "天买";
////                        }
////                    } else if (bs == 1) {
////                        if (rate < bigSellVolRate) {
////                            continue;
////                        }
//////                        else if (rate < skySellVolRate) {
//////                            description = "大卖";
//////                        } else {
//////                            description = "天卖";
//////                        }
////                    } else {
////                        continue;
////                    }
////                }
////
////                if (description == null) {
////                    continue;
////                }
////
////                // 长信息提示
////                String infoLong = StrUtil
////                        .format("{} 正股{}倍率: {}  {} -- {}", bondBean.getName(), description, vol / avgVolPre5,
////                                fsTransData.row(fsTransData.length() - 1), avgVolPre5);
////                Console.log(infoLong);
////
////                // 短信息
////                String infoShort = StrUtil.format("{}{}", bondBean.getName().replace("转债", ""), description);
////
////                try {
////                    ManiLog.put(infoLong);
////                } catch (Exception e) {
////                }
////
////                Tts.playSound(infoShort, true);
////
////
////                lastNotiTimeTickMap.put(stockBean.getQuoteId(), timeTickLast);
////
////            }
////
////
////        }
////
////
////    }
//
//
    public static void main1() throws Exception {
        mainX();
    }


    public static void mainX() {
        // 1.准备步骤
        // 1.1.开始更新监控转债列表任务
        startUpdateBondListTask(false);
        // 1.2.开始消费消息队列
        startNotifyMessages();

        // 2.开启爬虫, 待首次转债列表更新完, 会自动获取数据
        FsTransactionFetcher fsTransactionFetcher =
                FsTransactionFetcher.getInstance(5,
                        "15:10:00", 300, 100, 32);
        fsTransactionFetcher.startFetch();

        ChgPctAlgorithm algorithm = new ChgPctAlgorithm(); // 单例
        while (true) {
            ThreadUtil.sleep(100);
            for (StockBondBean stockBondBean : bondPoolSet) {
                SecurityBeanEm bondBean;
                try {
                    bondBean = SecurityBeanEm.createBond(stockBondBean.getBondCode());
                } catch (Exception e) {
                    continue;
                }
                NotifyMessage message = algorithm.describe(bondBean, null, null);
                if (message != null) {
                    messageQueue.put(message);
                }
            }
        }


    }

    /*
    消息队列和播报
     */

    /**
     * 消息优先级队列, 消息对象使用 priority属性作为优先级比较排序;
     * 无限队列, 使用 put 和 take (可能阻塞)  放入和拿出单个元素
     */
    public static volatile PriorityBlockingQueue<NotifyMessage> messageQueue = new PriorityBlockingQueue<>();

    /**
     * 子线程死循环, 遍历消息队列, 取优先级最高消息, 播报消息!
     * 一般都会检测消息是否过期 !
     */
    public static void startNotifyMessages() {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                log.info("开始访问消息队列, 播报消息!");
                while (true) {
                    NotifyMessage message = null;
                    try {
                        message = messageQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 获取最高优先级消息
                    if (message != null) {
                        if (!message.isExpired()) { // 消息未过期;
                            notifyInfoCommon(message.getInfoLong());
                            Tts.playSound(message.getInfoShort(), true);
                        } else { // 已过期消息, 将仅仅 醒目log一下
                            notifyInfoError("过期消息: " + message.getInfoLong());
                        }
                    }
                }
            }
        }, true);
    }



    /*
    描述算法接口
     */

    /**
     * 转债实时状态判定算法接口 -- 返回播报描述内容 -- 仅限播报程序使用
     * 实现类见 bondbuyalgorithm 包
     */
    public abstract static class BondStateAlgorithm {
        /*
        针对所有算法的全局限制 相关静态属性! 若算法需要更加灵活机制, 请不使用这些静态属性, 自行实现即可
         */
        /**
         * 保留某转债最后一次提示时的时间tick; 若下次循环依然是此tick, 将不再提示
         * key为 股票(或转债)bean的 quoteId, value是最后一次提示时的时间, 例如09:40:03
         */
        public static ConcurrentHashMap<String, String> lastNotifyTimeTickMap = new ConcurrentHashMap<>();
        /**
         * 对每只资产(转债), 记录一下播报的时间, 在 n 秒内, 强制不再进行播报! 配合 forceNotNotifyPeriod 属性; 默认10秒;
         * 往往 forceNotNotifyPeriod 需要自定义设置
         */
        public static ConcurrentHashMap<String, Long> notifyTimeMillsMap = new ConcurrentHashMap<>(); // value是时间戳
        public static long forceNotNotifyPeriod = 1000 * 10; // 十秒内, 强制不会生成2次播报, 以免堆积


        /**
         * 该方法返回 "描述字符串"列表, 以供播报;  一般元素1是 简短信息(一般是播报内容), 元素2是长信息提示(一般用于log)
         * // @key3
         * 播报程序 将使用算法链, 算法链中, 靠前的算法, 一旦不返回null(即有了具体描述), 则立即终止执行算法链, 播报提示描述内容!
         * 因此, 算法链中算法的先后顺序, 极大程度上影响播报内容!
         *
         * @param bondBean      东财转债资产bean
         * @param stockBean     对应正股, 若算法实现本身不需要用到正股数据, 则通常可null
         * @param StockBondBean 同理可null; 持有转债代码名称,正股代码名称的简单对象
         * @return
         */
        public abstract NotifyMessage describe(SecurityBeanEm bondBean, SecurityBeanEm stockBean,
                                               StockBondBean stockBondBean);

        /*
        默认实现的一些方法
         */

        // 1.正股和转债的分时和分时成交数据获取默认方法, 从爬虫获取
        // @noti: 未来复盘实现, 需要重写方法, 才数据库读取数据;  默认实现未使用到 current 参数, 因为获取最新全部数据
        public DataFrame<Object> getFsTransDfOfBond(Date current, SecurityBeanEm bondBean) {
            return FsTransactionFetcher.getFsTransData(bondBean);
        }

        // 日期 开盘	收盘 最高	最低	成交量	成交额	    振幅 涨跌幅	涨跌额  换手率	资产代码	资产名称
        public DataFrame<Object> getFsDfOfBond(Date current, SecurityBeanEm bondBean) {
            return FsFetcher.getFsData(bondBean);
        }

        // sec_code	market	time_tick	price	 vol	bs, 使用顺序
        public DataFrame<Object> getFsTransDfOfStock(Date current, SecurityBeanEm stockBean) {
            return FsTransactionFetcher.getFsTransData(stockBean);
        }

        public DataFrame<Object> getFsDfOfStock(Date current, SecurityBeanEm stockBean) {
            return FsFetcher.getFsData(stockBean);
        }
    }

    /**
     * 播报消息对象!!!
     * 1.简短信息一般用于播报
     * 2.长信息用于log
     * 3.priority 表示优先级, 小在前;
     * 4.消息过期机制: 使用 生成时时间戳毫秒, 以及 维持时间毫秒;;
     * 当播报队列拿到此消息, 若 current > 生成时间+维持时间, 表示过期, 将丢弃!
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotifyMessage implements Comparable {
        public String infoShort;
        public String infoLong;
        public long priority; // 使用 0 - 10000;  0最大优先级
        public long generateMills; // 消息生成时 时间戳, 已重写setter
        public String generateDateTimeStr; // 消息生成时 时间字符串, 一般等于 generateMills 转换的日期字符串
        public long expireMills; // 消息过期(维持)最大时间,  都是毫秒

        /**
         * 过期判定!
         *
         * @return
         */
        public boolean isExpired() {
            return System.currentTimeMillis() > generateMills + expireMills;
        }

        public void setGenerateMills(long generateMills) {
            this.generateMills = generateMills;
            this.generateDateTimeStr = DateUtil
                    .format(DateUtil.date(generateMills), DatePattern.NORM_DATETIME_MS_PATTERN);
        }

        @SneakyThrows
        @Override
        public int compareTo(Object o) { // 优先级比较!
            if (o instanceof NotifyMessage) {
                NotifyMessage o1 = (NotifyMessage) o;
                long res = this.priority - o1.priority;
                if (res > 0) {
                    return 1;
                } else if (res == 0) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                throw new Exception("NotifyMessage 对象只能与NotifyMessage对象进行比较");
            }
        }
    }

    /**
     * 维护静态数据!! 例如 昨日收盘价
     */
    public static class StaticData {
        // 转债相关
        // 转债昨日收盘价map, key为转债简单6位代码!!!
        public static ConcurrentHashMap<String, Double> bondPreCloseMap = new ConcurrentHashMap<>();

        /**
         * 刷新转债昨收map
         */
        public static void flushBondPreCloseMap() {
            DataFrame<Object> realtimeQuotesOfBond = EmConvertibleBondApi.getRealtimeQuotes();
            // 资产代码 和 昨收 两列
            if (realtimeQuotesOfBond == null || realtimeQuotesOfBond.length() == 0) {
                log.error("获取东财全部转债实时截面数据错误, 无法更新昨收价map");
                return;
            }
            for (int i = 0; i < realtimeQuotesOfBond.length(); i++) {
                try {
                    bondPreCloseMap.put(
                            realtimeQuotesOfBond.get(i, "资产代码").toString(),
                            Double.valueOf(realtimeQuotesOfBond.get(i, "昨收").toString())
                    );
                } catch (Exception e) {
                    log.warn("获取昨收失败: {} -- {}", realtimeQuotesOfBond.get(i, "资产代码"), realtimeQuotesOfBond.get(i,
                            "资产名称"));
                }
            }
        }

        /*
        入口
         */

        /**
         * 总入口, 刷新所有静态数据项 -- 子线程
         */
        public static void startFlushAllStaticData() {
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        flushBondPreCloseMap();
                    }
                }
            }, true);
        }


    }


    /*
    转债池构建相关
     */

    /**
     * 静态和动态转债列表, 单线程负责维护更新; 并且更新到 爬虫资产池!
     */
    public static List<StockBondBean> allBonds = new ArrayList<>(); // 全部转债, 较长时间才更新一次; 均从这里筛选
    public static Date allBondsUpdateTime = null; // 全部转债更新时间, 较长时间才更新一次
    public static List<StockBondBean> careBonds = null; // 关注转债, 读取文件而来, 原则上一次即可
    public static List<StockBondBean> scaleLast30Bonds = new ArrayList<>(); // 规模最小30
    public static List<StockBondBean> chgPctLast30Bonds = new ArrayList<>(); // 涨跌幅最小30, 负数
    public static List<StockBondBean> chgPctTop30Bonds = new ArrayList<>(); // 涨跌幅最大30,
    public static List<StockBondBean> volTop60Bonds = new ArrayList<>(); // 成交额最大60

    public static HashSet<StockBondBean> bondPoolSet = new HashSet<>(); // 维护更新后的全关注转债对象,遍历时遍历
    // 维护更新后转债列表, 东财资产bean; 本质上更多作用是查询api构建缓存
    public static HashSet<SecurityBeanEm> bondSet = new HashSet<>();
    public static HashSet<SecurityBeanEm> stockSet = new HashSet<>(); // 维护更新后正股列表, 东财资产bean


    public static long sleepOfUpdateBondListPerLoop = 10 * 1000; // 转债列表更新sleep

    /**
     * 转债列表更新任务, 单线程执行; 注意主线程别结束
     */
    public static void startUpdateBondListTask(boolean addStockToPool) {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    updateBondListAndPushToCrawlerPool(true, addStockToPool);
                    ThreadUtil.sleep(sleepOfUpdateBondListPerLoop);
                } catch (Exception e) {
                    e.printStackTrace();
                    notifyInfoError("首次更新转债列表失败!");
                }

                while (true) {
                    try {
                        updateBondListAndPushToCrawlerPool(false, addStockToPool);
                        ThreadUtil.sleep(sleepOfUpdateBondListPerLoop);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, true);
    }


    /**
     * 更新转债列表, 并且将资产添加到 爬虫将要爬取的资产池; -- 核心方法
     * 本方法将使用单线程 持续死循环运行
     *
     * @param first          标志是否第一次更新, 将打印日志
     * @param addStockToPool 是否将正股也加入池, 这样数据获取量为2倍
     */
    public static void updateBondListAndPushToCrawlerPool(boolean first, boolean addStockToPool) {
        if (first) {
            notifyInfoCommon("首次更新转债列表");
        }

        // 1.更新全部转债, 每10分钟更新一次. 且要求新数据行数 > 老数据行数*0.8; 有效更新
        if (allBondsUpdateTime == null || DateUtil
                .between(allBondsUpdateTime, DateUtil.date(), DateUnit.MINUTE) >= 10) {
            List<StockBondBean> allStockWithBond = getAllStockWithBond();
            if (allStockWithBond.size() >= allBonds.size() * 0.8) {
                allBonds = allStockWithBond; // 更新
            } else {
                notifyInfoError("更新全部转债列表失败, 使用旧数据");
            }
        }
        if (first) {
            notifyInfoCommon(StrUtil.format("全部转债数量: {}", allBonds.size()));
        }

        // 2.读取文件更新关注转债, 一次运行; 它将从所有转债筛选; 注意文件需要存在
        if (careBonds == null) {
            String s = ResourceUtil.readUtf8Str("bonds.txt");
            List<String> bonds = StrUtil.split(s, "\r\n");
            bonds.remove("");
            HashSet<String> careBondSet = new HashSet<>(bonds);
            careBonds = allBonds.stream().filter(stockBondBean -> careBondSet.contains(stockBondBean.getBondName()))
                    .collect(Collectors.toList());
        }
        if (first) {
            notifyInfoCommon(StrUtil.format("关注转债数量: {}", careBonds.size()));
        }

        // 3.分别动态获取 4种筛选条件的转债列表
        List<StockBondBean> scaleLast30StockWithBond = getScaleLast30StockWithBond();
        if (scaleLast30StockWithBond.size() >= scaleLast30Bonds.size() * 0.8) {
            scaleLast30Bonds = scaleLast30StockWithBond;
        }

        List<StockBondBean> chgPctLast30StockWithBond = getChgPctLast30StockWithBond();
        if (chgPctLast30StockWithBond.size() >= chgPctLast30Bonds.size() * 0.8) {
            chgPctLast30Bonds = chgPctLast30StockWithBond;
        }

        List<StockBondBean> chgPctTop30StockWithBond = getChgPctTop30StockWithBond();
        if (chgPctTop30StockWithBond.size() >= chgPctTop30Bonds.size() * 0.8) {
            chgPctTop30Bonds = chgPctTop30StockWithBond;
        }

        List<StockBondBean> volTop60StockWithBond = getVolTop60StockWithBond();
        if (volTop60StockWithBond.size() >= volTop60Bonds.size() * 0.8) {
            volTop60Bonds = volTop60StockWithBond;
        }

        if (first) {
            notifyInfoCommon("动态转债列表获取完成");
        }

        // 4.将转债列表添加到资产池子 -- @noti:
        // 考虑到其他程序可能添加转债, 因此, 只采用增量添加的方式, 这保证了 转债一旦进入池子, 不会被删除;
        // 只增不减, 不会错过曾经的热点转债, 符合需求; 但对硬件和网络要求更高

        // 4.1. 首先构建 StockBondBean 集合, 再一次性添加
        HashSet<StockBondBean> finalSet = new HashSet<>(careBonds);
        finalSet.addAll(scaleLast30Bonds);
        finalSet.addAll(chgPctLast30Bonds);
        finalSet.addAll(chgPctTop30Bonds);
        finalSet.addAll(volTop60Bonds);

        // 4.2. 实例化东财bean对象
        List<SecurityBeanEm> stockList0 = null;
        List<SecurityBeanEm> bondList0 = null;
        if (first) {
            notifyInfoCommon("开始对转债列表构建东财bean对象");
        }
        try {
            if (addStockToPool) {
                stockList0 = SecurityBeanEm.createStockList(
                        finalSet.stream().map(StockBondBean::getStockCode).collect(Collectors.toList()), false);
            }
            bondList0 = SecurityBeanEm.createBondList(
                    finalSet.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()),
                    false);
        } catch (Exception e) {
            e.printStackTrace();
            notifyInfoError("转债列表构建东财bean对象失败");
            return;
        }

        // 4.3. 加入资产池, 将被爬虫获取数据
        SecurityPool.addToTodaySelectedBonds(bondList0);
        if (addStockToPool) {
            SecurityPool.addToTodaySelectedStocks(stockList0);
        }
        // 5.彻底更新静态属性; 监控主程序将真实遍历这些集合!!
        bondPoolSet.addAll(finalSet);
        if (stockList0 != null) {
            stockSet.addAll(stockList0);
        }
        bondSet.addAll(bondList0);

        if (first) {
            notifyInfoCommon(StrUtil.format("转债列表首次构建并加入资产池成功, 数量: {}", bondList0.size()));
        }
    }


    /**
     * 获取当前 规模最小30
     *
     * @return
     */
    public static List<StockBondBean> getScaleLast30StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债债券余额从小到大排名前30",
                WenCaiApi.TypeStr.BOND);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前涨幅后30
     *
     * @return
     */
    public static List<StockBondBean> getChgPctLast30StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债涨跌幅从小到大排名前30",
                WenCaiApi.TypeStr.BOND);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前涨幅前30
     *
     * @return
     */
    public static List<StockBondBean> getChgPctTop30StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债涨跌幅从大到小排名前30",
                WenCaiApi.TypeStr.BOND);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前成交额前60
     *
     * @return
     */
    public static List<StockBondBean> getVolTop60StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债成交额从大到小排名前60",
                WenCaiApi.TypeStr.BOND);
        return parseStockBondBeanList(dataFrame);
    }


    /**
     * 获取所有转债, 并转换为 StockBondBean 列表
     *
     * @return
     */
    public static List<StockBondBean> getAllStockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("正股代码;正股简称",
                WenCaiApi.TypeStr.BOND); // 全部转债
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 解析问财结果
     *
     * @param dataFrame
     * @return
     */
    private static List<StockBondBean> parseStockBondBeanList(DataFrame<Object> dataFrame) {
        List<StockBondBean> stockBondBeanList = new ArrayList<>();
        if (dataFrame == null) {
            notifyInfoError("注意, 问财api访问转债列表, 解析结果为空列表");
            return stockBondBeanList;
        }
        for (int i = 0; i < dataFrame.length(); i++) {
            try {
                stockBondBeanList.add(new StockBondBean(
                        dataFrame.get(i, "可转债@正股简称").toString(),
                        dataFrame.get(i, "可转债@正股代码").toString().substring(0, 6),
                        dataFrame.get(i, "可转债@可转债简称").toString(),
                        dataFrame.get(i, "code").toString(),
                        -1
                ));
            } catch (Exception e) {
            }
        }
        return stockBondBeanList;
    }


    /*
    常规log
     */
    private static final Log log = LogUtil.getLogger();

    /**
     * 常规log
     */
    public static void notifyInfoCommon(String content) {
        log.info(content);
        ManiLog.put(content);
    }

    /**
     * 警示log
     *
     * @param content
     */
    public static void notifyInfoError(String content) {
        log.error(content);
        ManiLog.put(content, Color.red);
    }


}
