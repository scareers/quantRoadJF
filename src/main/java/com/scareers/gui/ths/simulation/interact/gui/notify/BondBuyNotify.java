package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.date.*;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.SecurityPool;
import com.scareers.datasource.eastmoney.fetcher.FsFetcher;
import com.scareers.datasource.eastmoney.fetcher.FsTransactionFetcher;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.eastmoney.quotecenter.bond.EmConvertibleBondApi;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond.BondGlobalSimulationPanel;
import com.scareers.gui.ths.simulation.interact.gui.notify.bondbuyalgorithm.SingleAmountAlgorithm;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * description: 可转债实盘提示
 * 因复盘也会用到播报程序! 本质上, 只有 "数据源" 会不同,
 * 因此, 设置 "环境"变量, 实盘或者复盘
 * 仅仅需要 "数据api"性质的逻辑, 根据环境而实现不同即可 !!! 以及启动程序稍有不同(加载数据)
 *
 * @author: admin
 * @date: 2022/3/22/022-17:10:35
 */
public class BondBuyNotify {
    public static void main(String[] args) throws Exception {
//        main1();

        List<StockBondBean> volTopNStockWithBond = getVolTopNStockWithBond(20);
        for (StockBondBean stockBondBean : volTopNStockWithBond) {
            Console.log(stockBondBean.getBondName());
        }
    }

    /*
     * 常规静态属性
     */
    // ---------> 偏双环境
    public static HashSet<String> excludeBonds = new HashSet<>(); // 保存绝对排除的转债代码; 例如创建东财bean失败的
    // ---------> 偏实盘环境
    // ---------> 偏复盘环境

    public static boolean loadFsToCacheAfterBondPoolInitReviseEnvironment = true; // 重要设置: 复盘环境下, 强制将转债池全部分时成交数据载入缓存
    public static volatile boolean broadcastRunning = false; // 可控制播报程序停止(跳出主循环) 和 运行主循环(需要调用方法


    public static void stopBroadcast() {
        BondBuyNotify.broadcastRunning = false;
        CommonUtil.notifyKey("将软停止播报程序 ----->");
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    CommonUtil.waitUtil(new BooleanSupplier() {
                        @Override
                        public boolean getAsBoolean() {
                            return !broadcastRunning;
                        }
                    }, 1000, 1, null, false);
                    CommonUtil.notifyKey("播报程序停止成功");
                } catch (TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
    }

    public static void main1() {
        if (broadcastRunning) {
            notifyInfoError("播报程序运行中, 不可重复启动! 需要停止后才可启动");
            return;
        }


        if (isActualTradingEnvironment()) { // 实盘环境!
            CommonUtil.notifyKey("播报程序启动, 环境: 实盘环境");
            broadcastRunning = true; // 启动flag

            // 1.准备步骤
            // 1.1.开始更新监控转债列表任务 -- 子线程死循环, 不能再次调用
            startUpdateBondListTask(false);
            try {
                SecurityBeanEm.createBondList(
                        allStockWithBond.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()), false);
            } catch (Exception e) {
                // 单次初始化所有转债对象; 目的上是加入缓存, 以便未来
            }
            // 1.2.开始消费消息队列 -- 均调用一次开启消费; 注意, 过期时间判定, 当前时间调用静态方法 getCurrentMills, 不再调用System的
            startNotifyMessages();
            // 1.3.静态数据更新
            StaticData.startFlushAllStaticData(); // 复盘的 可强制调用刷新一下, 尤其是更新了日期以后!

            // 2.开启爬虫, 待首次转债列表更新完, 会自动获取数据
            FsTransactionFetcher fsTransactionFetcher =
                    FsTransactionFetcher.getInstance(4,
                            "15:10:00", 600, 100, 32);
            fsTransactionFetcher.startFetch();

            List<BondStateAlgorithm> algorithmChain = new ArrayList<>();
            algorithmChain.add(new SingleAmountAlgorithm());
//        algorithmChain.add(new ChgPctAlgorithm());
            CommonUtil.notifyKey("播报程序进入主循环, 环境: 实盘环境");

            while (true) {
                if (!broadcastRunning) { // 被停止
                    break;
                }
                ThreadUtil.sleep(50);
                // Console.log(bondPoolSet.size()); // 检测死循环是否进行中
                for (StockBondBean stockBondBean : new HashSet<>(bondPoolSet)) {
                    if (!broadcastRunning) { // 跳出内层循环后, 下次外层循环检测到, 真正停止
                        break;
                    }


                    if (excludeBonds.contains(stockBondBean.getBondCode())) {
                        continue; // 不能在排除列表中; 可手动设置排除转债, 以及一些创建东财bean失败的; 因为问财结果不保证转债当前可交易
                    }
                    SecurityBeanEm bondBean;
                    try {
                        bondBean = SecurityBeanEm.createBond(stockBondBean.getBondCode());
                    } catch (Exception e) {
                        excludeBonds.add(stockBondBean.getBondCode());
                        continue;
                    }

                    // 算法链遍历
                    NotifyMessage message = null;
                    for (BondStateAlgorithm algorithm : algorithmChain) {
                        message = algorithm.describe(bondBean, null, null);
                        if (message != null) { // 取最前方算法的非null结果
                            break;
                        }
                    }
                    if (message != null) { // 可能算法链遍历完成, 也没有结果
                        messageQueue.put(message);
                    }
                }
            }
            broadcastRunning = false; // 停止flag

        } else if (isReviseEnvironment()) { // 多数同实盘, 不同之处将会标记!
            CommonUtil.notifyKey("播报程序启动, 环境: 复盘环境");
            broadcastRunning = true; // 启动flag

            startUpdateBondListTask(false);
            try {
                SecurityBeanEm.createBondList(
                        allStockWithBond.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()), false);
            } catch (Exception e) {
            }
            startNotifyMessages();
            StaticData.startFlushAllStaticData();

            // 2.开启爬虫, 待首次转债列表更新完, 会自动获取数据
            List<BondStateAlgorithm> algorithmChain = new ArrayList<>();
            algorithmChain.add(new SingleAmountAlgorithm());
//        algorithmChain.add(new ChgPctAlgorithm());
            String preDateStrSetting = getReviseDateStr(); // 首次的设置日期, 可能会改变, 每轮都需要检测

            CommonUtil.notifyKey("播报程序进入主循环, 环境: 复盘环境");
            // Console.log(bondPoolSet);

            String reviseDateStr = getReviseDateStr();

            while (true) {
                if (!broadcastRunning) { // 被停止
                    break;
                }

                ThreadUtil.sleep(100); // 延迟多一点
                // @diff: 1.需要 复盘程序对象, 是 running 状态, 才进行本次循环; 暂停以及停止时, 都暂停!!
                if (!reviseProcessActualRunning()) { // 停止或者暂停状态, 都不继续
                    continue;
                }

                // @diff: 2.需要刷新静态数据池!
                if (reviseDateStr == null || !reviseDateStr.equals(preDateStrSetting)) { // 复盘日期改变, 应当刷新静态数据, 已经fs数据池!
                    CommonUtil.notifyKey("复盘日期更改, 需要刷新静态数据池");
                    // 刷新静态数据池!
                    StaticData.forceFlushAllStaticData();
                    // @noti: 动态数据因为动态读取 复盘日期设定, 访问数据库(带缓存), 因此无需在此刷新
                    preDateStrSetting = reviseDateStr; // 保留改变
                }


                // Console.log(bondPoolSet.size()); // 检测死循环是否进行中
                for (StockBondBean stockBondBean : new HashSet<>(bondPoolSet)) {
                    if (!broadcastRunning) { // 被停止
                        break;
                    }


                    if (excludeBonds.contains(stockBondBean.getBondCode())) {
                        continue; // 不能在排除列表中; 可手动设置排除转债, 以及一些创建东财bean失败的; 因为问财结果不保证转债当前可交易
                    }
                    SecurityBeanEm bondBean;
                    try {
                        bondBean = SecurityBeanEm.createBond(stockBondBean.getBondCode());
                    } catch (Exception e) {
                        excludeBonds.add(stockBondBean.getBondCode());
                        continue;
                    }

                    // 算法链遍历
                    NotifyMessage message = null;
                    for (BondStateAlgorithm algorithm : algorithmChain) {
                        message = algorithm.describe(bondBean, null, null);
                        if (message != null) { // 取最前方算法的非null结果
                            break;
                        }
                    }
                    if (message != null) { // 可能算法链遍历完成, 也没有结果
                        messageQueue.put(message);
                    }
                }
            }
            broadcastRunning = false; // 停止flag
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
     * 会检测消息是否过期 !
     */
    public static volatile boolean startedNotifyMessages = false; // 标志是否已经开启消息消费队列

    public static void startNotifyMessages() {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                if (startedNotifyMessages) {
                    return;
                }
                startedNotifyMessages = true;
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
                        if (!message.isExpired(getCurrentMills())) { // 消息未过期;
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


        // 分时成交数据, 数据库列,重命名为与 实时爬虫列名称相同!
        public static Map<Object, Object> fsTransDbColAdaptationMap = new HashMap<>();
        // @key: 因为实时爬虫获取到 数据, 会比真实时间, 一般都有 0.x秒延迟, 因此我们复盘程序, 筛选时间应该设置一定延迟, 达到等价实盘
        // @key: 建议设置1 或者0, (因为复盘程序不含毫秒, 因此只能精确到秒)
        public static int simulationDataFromDbDelaySecond = 1;

        static {
            intiFsTransDbColAdaptationMap(); // 分时成交数据库与实时爬虫 列名相同, 不再需要适配!
        }

        private static void intiFsTransDbColAdaptationMap() {
            fsTransDbColAdaptationMap.putAll(Dict.create()
                    .set("test", "test")
            );
        }

        // 1.正股和转债的分时和分时成交数据获取默认方法, 从爬虫获取
        // @noti: 未来复盘实现, 需要重写方法, 才数据库读取数据;  默认实现未使用到 current 参数, 因为获取最新全部数据
        // @key3: 实盘和复盘, fs成交数据, 列索引也要保持一直;  数据库api  的sql语句, 已经保证了!!!
        public DataFrame<Object> getFsTransDfOfBond(SecurityBeanEm bondBean) {
            if (isActualTradingEnvironment()) {
                return FsTransactionFetcher.getFsTransData(bondBean);
            } else if (isReviseEnvironment()) {
                DataFrame<Object> fsTransDf = EastMoneyDbApi
                        .getFsTransByDateAndQuoteIdS(getReviseDateStr(), bondBean.getQuoteId()); // 已经缓存
                // 实时数据字段列: 需要重命名匹配: sec_code	 market	time_tick	price	 vol	bs
                // 数据库均包含这些列!!!!!!! 且名称相同, 无需适配
                // fsTransDf = fsTransDf.rename(fsTransDbColAdaptationMap);
                // 需要筛选!
                DateTime current = getReviseSimulationCurrentTime();
                // 在某个时间只能获取到 前一秒以前的数据, 模拟实盘爬虫的数据延迟! 可设定
                DateTime filterTime = DateUtil.offset(current, DateField.SECOND, -simulationDataFromDbDelaySecond);
                String filterTick = DateUtil.format(filterTime, DatePattern.NORM_TIME_PATTERN);
                // 使用遍历查找, 更加快速, 这里不用 filter了
                int shouldIndex = -1;
                for (int i = 0; i < fsTransDf.length(); i++) {
                    String tick = fsTransDf.get(i, "time_tick").toString();
                    if (tick.compareTo(filterTick) <= 0) {
                        shouldIndex = i;
                    } else {
                        break;
                    }
                }

                if (shouldIndex == -1) { // 时间没到, 没数据
                    return new DataFrame<>(fsTransDf.columns());
                } else {
                    return fsTransDf.slice(0, shouldIndex + 1);
                }
            }
            return null;
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
     * 4.消息过期机制: 使用 生成时时间戳毫秒, 以及 维持时间毫秒; 参数为(等价)当前时间戳; // 复盘程序将使用模拟时间戳
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
        public long expireMills; // 消息过期(维持)最大时间,  毫秒

        /**
         * 过期判定!
         *
         * @return
         */
        public boolean isExpired(long currentTimeMillis) {
            return currentTimeMillis > generateMills + expireMills;
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
        // 转债昨日收盘价map, key为quoteId
        public static ConcurrentHashMap<String, Double> bondPreCloseMap = new ConcurrentHashMap<>();
        public static HashMap<String, Double> allPreCloseByDate = null; // 复盘时读取所有昨收需要. 仅载入一次 ; key为quoteid

        /**
         * 刷新转债昨收map
         */
        public static void flushBondPreCloseMap() {
            if (isActualTradingEnvironment()) { // 实盘将读取东财实时转债列表
                DataFrame<Object> realtimeQuotesOfBond = EmConvertibleBondApi.getRealtimeQuotes();
                // 资产代码 和 昨收 两列
                if (realtimeQuotesOfBond == null || realtimeQuotesOfBond.length() == 0) {
                    log.error("获取东财全部转债实时截面数据错误, 无法更新昨收价map");
                    return;
                }
                bondPreCloseMap.clear();
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
            } else if (isReviseEnvironment()) { // 复盘环境, 需要最新的 日期设置!
                // 已经缓存, 可以多次调用 , 例如复盘日期改变后!
                allPreCloseByDate =
                        EastMoneyDbApi
                                .getAllPreCloseByDate(EastMoneyDbApi.getPreNTradeDateStrict(getReviseDateStr(), 1));

                if (allStockWithBond == null) {
                    allStockWithBond = getAllStockWithBond();
                }
                bondPreCloseMap.clear();
                for (StockBondBean stockBondBean : allStockWithBond) {
                    if (excludeBonds.contains(stockBondBean.getBondCode())) {
                        continue; // 不能在排除列表中; 可手动设置排除转债, 以及一些创建东财bean失败的; 因为问财结果不保证转债当前可交易
                    }
                    try {
                        SecurityBeanEm bond = SecurityBeanEm.createBond(stockBondBean.getBondCode());
                        bondPreCloseMap.put(stockBondBean.getBondCode(), allPreCloseByDate.get(bond.getQuoteId()));
                    } catch (Exception e) {

                    }
                }
            }
        }

        /*
        入口
         */

        /**
         * 总入口, 刷新所有静态数据项 -- 子线程 -- 1分钟更新 -- 各方法自行区分环境!
         */
        public static boolean loopStarted = false; // 保证静态数据更新死循环, 仅仅启动一次!

        public static void startFlushAllStaticData() {
            if (loopStarted) {
                return;
            }
            loopStarted = true;
            ThreadUtil.execAsync(new Runnable() {
                @Override
                public void run() {
                    while (true) { //
                        if (broadcastRunning) {
                            forceFlushAllStaticData();
                        }
                        ThreadUtil.sleep(60 * 1000);
                    }
                }
            }, true);
        }

        public static void forceFlushAllStaticData() {
            flushBondPreCloseMap();
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

    public static volatile CopyOnWriteArraySet<StockBondBean> bondPoolSet = new CopyOnWriteArraySet<>(); // 维护更新后的全关注转债对象,
    // 遍历时遍历
    // 维护更新后转债列表, 东财资产bean; 本质上更多作用是查询api构建缓存
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> bondSet = new CopyOnWriteArraySet<>();
    public static volatile CopyOnWriteArraySet<SecurityBeanEm> stockSet = new CopyOnWriteArraySet<>(); // 维护更新后正股列表,
    // 东财资产bean


    public static long sleepOfUpdateBondListPerLoop = 10 * 1000; // 转债列表更新sleep


    /**
     * 转债列表更新任务, 单线程执行; 注意主线程别结束
     * 实盘: 调用 updateBondListAndPushToCrawlerPool() 更新相关转债池
     * 复盘: 转债池相同, 但一次载入, 随后不变!
     * --> 实盘环境仅仅需要启动时调用一次; 复盘环境, 可随时调用, 都会最新设置初始化转债池,且除了第一次一般完成极快
     */
    public static void startUpdateBondListTask(boolean addStockToPool) {
        if (isActualTradingEnvironment()) {
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
                        ThreadUtil.sleep(60 * 1000);
                    }
                }
            }, true);
        } else if (isReviseEnvironment()) {
            // 复盘环境, 仅需要填充 转债池即可! 需要 StockBondBean 对象列表, 因此,
            // 先从问财访问所有转债, 转换为 StockBondBean 对象
            // 再取问财实时成交额前 150 名, 最终放入转债池!
            // --> 只需要执行一次, 不需要子线程死循环更新
            // --> 其他列表均为空, 未初始化!

            if (allStockWithBond == null || allStockWithBond.size() < 100) {
                allStockWithBond = getAllStockWithBond(); // 问财实时全部,}
            }




            /* 数据库的东财列表获取前50

            List<String> allBondCodeFromDb = EastMoneyDbApi.getAllBondCodeByDateStr(getReviseDateStr()); // 东财成交额排序
            if (allBondCodeFromDb == null || allBondCodeFromDb.size() < 100) {
                allBondCodeFromDb = EastMoneyDbApi.getAllBondCodeByDateStr(DateUtil.today()); // 失败则今天,需要今天爬虫已经执行
            }
            if (allBondCodeFromDb == null) { // 实在不行, 再访问东财实时数据列表
                DataFrame<Object> bondDf = EmQuoteApi.getRealtimeQuotes(Arrays.asList("可转债"));
                allBondCodeFromDb = DataFrameS.getColAsStringList(bondDf, "资产代码");
            }
            HashSet<String> selectBondCodes = new HashSet<>(allBondCodeFromDb.subList(0, reviseBondPoolSize));
            bondPoolSet.clear();
            for (StockBondBean stockBondBean : allStockWithBond) {
                if (selectBondCodes.contains(stockBondBean.getBondCode())) {
                    bondPoolSet.add(stockBondBean);
                }
            }
             */

            // 问财实时成交额
            try {
                List<StockBondBean> volTopNStockWithBond = getVolTopNStockWithBond(150);
                bondPoolSet.clear();
                bondPoolSet.addAll(volTopNStockWithBond);


                CommonUtil.notifyKey(StrUtil.format("当前转债池数量: {} [{}]...", bondPoolSet.size(),
                        volTopNStockWithBond.subList(0, 10).stream().map(StockBondBean::getBondName)
                                .collect(Collectors.toList())));

                if (loadFsToCacheAfterBondPoolInitReviseEnvironment) {
                    CommonUtil.notifyKey("模拟环境: 初始一次性载入转债池全部 分时成交数据");
                    ThreadUtil.execAsync(new Runnable() {
                        @Override
                        public void run() {
                            List<String> collect = bondPoolSet.stream().map(StockBondBean::getBondCode)
                                    .collect(Collectors.toList());
                            try {
                                List<SecurityBeanEm> bondList = SecurityBeanEm.createBondList(collect, false);
                                for (SecurityBeanEm beanEm : bondList) {
                                    EastMoneyDbApi.getFsTransByDateAndQuoteId(getReviseDateStr(), beanEm.getQuoteId());
                                    EastMoneyDbApi.getFsTransByDateAndQuoteIdS(getReviseDateStr(), beanEm.getQuoteId());
                                }
                            } catch (Exception e) {
                            }

                        }
                    }, true);
                }
                // 不刷新到
//                BondGlobalSimulationPanel.getInstance().flushBondListAs(SecurityBeanEm.createBondListOrdered(
//                        volTopNStockWithBond.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()),
//                        false
//                ));
            } catch (Exception e) {

            }

        }
    }

    // 共用, 问财实时全部转债 列表!  复盘时仅初始化一次即可
    public static List<StockBondBean> allStockWithBond = null;

    // 复盘相关
    public static final int reviseBondPoolSize = 150; // 东财爬虫成交额排名前150, 构造复牌时转债池

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
            allStockWithBond = getAllStockWithBond(); //
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
                0, WenCaiApi.TypeStr.BOND);
        ThreadUtil.sleep(100);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前涨幅后30
     *
     * @return
     */
    public static List<StockBondBean> getChgPctLast30StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债涨跌幅从小到大排名前30",
                0, WenCaiApi.TypeStr.BOND);
        ThreadUtil.sleep(100);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前涨幅前30
     *
     * @return
     */
    public static List<StockBondBean> getChgPctTop30StockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("可转债涨跌幅从大到小排名前30",
                0, WenCaiApi.TypeStr.BOND);
        ThreadUtil.sleep(100);
        return parseStockBondBeanList(dataFrame);
    }

    /**
     * 获取当前成交额前60
     *
     * @return
     */
    public static List<StockBondBean> getVolTop60StockWithBond() {
        return getVolTopNStockWithBond(60);
    }

    /**
     * 获取当前成交额前n ; 问财实时数据
     *
     * @param preN
     * @return
     */
    public static List<StockBondBean> getVolTopNStockWithBond(int preN) {
        DataFrame<Object> dataFrame = BondUtil.getVolTopNBondDf(preN);
        ThreadUtil.sleep(100);
        return parseStockBondBeanList(dataFrame);
    }


    /**
     * 获取所有转债, 并转换为 StockBondBean 列表
     *
     * @return
     */
    public static List<StockBondBean> getAllStockWithBond() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("正股代码;正股简称",
                0, WenCaiApi.TypeStr.BOND); // 全部转债
        ThreadUtil.sleep(100);
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
//                e.printStackTrace();
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

    /**
     * 两种类型环境!!  严格意义上, 程序启动后, 环境初始化后就是固定的, 不可改变 !!
     */
    public enum Environment {
        ACTUAL_TRADING, // 实盘
        REVISE // 复盘环境
    }

    /**
     * 两大设置方法, 和判定方法!!
     */
    private static volatile Environment environment = Environment.ACTUAL_TRADING; // 默认即实盘,复盘一般需要主动调用一下

    /**
     * @key3 : 因为停止flag, 启动停止机制的问题, 菜单启动为实盘环境, 复盘界面按钮启动为复盘环境(停止按钮也可停止实盘播报)!
     * 且两者不会冲突 !! 当一个启动了, 另一个不允许成功启动
     */
    public static void changeEnvironmentToRevise() {
        environment = Environment.REVISE; // 默认即实盘
    }

    public static void changeEnvironmentToActualTrading() {
        environment = Environment.ACTUAL_TRADING; // 默认即实盘
    }

    public static boolean isReviseEnvironment() {
        return environment.equals(Environment.REVISE);
    }

    public static boolean isActualTradingEnvironment() {
        return environment.equals(Environment.ACTUAL_TRADING);
    }

    /*
    当前时间
     */

    /**
     * 获取当前时间戳! 实盘环境 与 复盘环境不同!
     *
     * @return
     */
    public static long getCurrentMills() {
        if (isReviseEnvironment()) {
            return getReviseSimulationCurrentTime().getTime(); // Date.getTime 就是时间戳
        } else if (isActualTradingEnvironment()) {
            return System.currentTimeMillis();
        } else {
            notifyInfoError("环境设置错误, 无法判定当前时间戳");
            return System.currentTimeMillis();
        }
    }

    /*
     * 复盘相关, 简单方法
     */

    /**
     * 复盘日期, 读取复盘gui实例的设置控件!
     *
     * @return
     */
    public static String getReviseDateStr() {
        return BondGlobalSimulationPanel.getInstance().getReviseDateStrSettingYMD();
    }

    /**
     * 复盘虚拟当前时间!
     *
     * @return
     */
    public static DateTime getReviseSimulationCurrentTime() { // 实时获取复盘 虚拟的 当前时间!
        return BondGlobalSimulationPanel.getInstance().getReviseSimulationCurrentTime();
    }

    /**
     * 判定复盘程序是否 running 它需要 running为true, 且 pause 不为 true; 即暂停也不行
     *
     * @return
     */
    public static boolean reviseProcessActualRunning() {
        return BondGlobalSimulationPanel.getInstance().isReviseRunning() && !BondGlobalSimulationPanel.getInstance()
                .isRevisePausing();
    }
}
