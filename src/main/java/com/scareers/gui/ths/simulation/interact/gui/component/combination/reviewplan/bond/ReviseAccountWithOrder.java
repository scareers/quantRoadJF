package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 复盘时模拟账号 + 订单;  账号和订单放入一个表之内, 账号相关列A开头, 订单相关列 O开头
 * // @key3: 单条记录, 包含了账号全基本信息, 以及账号瞬间的资产状况, 以及单个订单对象!
 * // @key3: 订单保存时, 账号状况是此前状况, 订单保存后, 才成交, 并刷新账户状态
 * 0.@update: 为了应对虚拟账号机制,对复盘开始时间的修改, 必须要求 running=false时才可进行!
 * // @key2: 因新订单生成并保存, 应当保存新的对象, 因此实现 from(ReviseAccountWithOrder oldStare) , 复制初始状态!
 * // @key3: 成交机制,买卖单给出价格, 读取未来tick价格, 若不合适则无法成交! 视为"自动立即撤单",设置canClinch=false; 而不会修改账户状态!!
 * // @key1: 因hibernate机制, 本质上复盘程序中, 账户状态对象, 会不断是新对象, 以便能够保存新记录到数据库, 而非修改对象属性, 那样只会修改数据库记录,而非增加!
 * // @key: 订单一旦成交, 视为全部成交;
 * // @key: 仓位, 均使用 "总资产" 的 仓位; 并需要检测 cash 够不够
 * <p>
 * 1.单次开始复盘, 重置账号!!!
 * 2.直到点击停止 ! 账号的状态保存!
 * 3.账号名称, 以 当前真实日期时间(开始时间) + 点击开始时, 的 复盘开始时间, 两者结合, 作为唯一ID;;
 * 4.只实现 买/卖功能, 视为下单后必定成交, 无撤单功能!
 * 5.买卖订单, 均以 仓位 生成订单, 而非数量, 本类实现计算方法!
 * 6.复盘停止时, 默认以收盘价格, 卖出所有转债 !! 并生成最后一批订单;
 *
 * @author: admin
 * @date: 2022/6/9/009-12:52:58
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "revise_account_with_order",
        indexes = {@Index(name = "AStartRealTime_Index", columnList = "AStartRealTime"), // 复盘真实开始时间带毫秒, 唯一确定单个账号!
                @Index(name = "AReviseStartDateTimeStr_Index", columnList = "AReviseStartDateTimeStr"), // 复盘模拟的开始日期时间
                @Index(name = "targetCode_Index", columnList = "targetCode"), // 订单相关的 转债代码名称和行情id
                @Index(name = "targetQuoteId_Index", columnList = "targetQuoteId"),
                @Index(name = "targetName_Index", columnList = "targetName"),
        }
)
public class ReviseAccountWithOrder {
    // 固定属性设置
    // 1.佣金配置
    @Column(name = "commissionRateShen", columnDefinition = "double")
    Double commissionRateShen = 0.00006; // 佣金率, 深市 十万分之6
    @Column(name = "commissionRateHu", columnDefinition = "double")
    Double commissionRateHu = 0.000002; // 佣金率, 沪市 百万分之2
    @Column(name = "commissionMinShen", columnDefinition = "double")
    Double commissionMinShen = 0.1; // 深市起收 1毛钱
    @Column(name = "commissionMinHu", columnDefinition = "double")
    Double commissionMinHu = 0.1; // 沪市起收 1毛钱
    // 2.成交延迟秒数设置
    // @key3: 成交机制: 生成订单时, 需要给定当前的复盘模拟时间时分秒 tick; 将此tick+本设置秒数, 得到新的tick;
    // @key3: 在分时成交数据中, 读取 >= 延迟后tick 的 第一个tick 的价格, 作为 未来可能成交价!!
    // @key2: 在停止复盘时, 采用的模拟全部卖出机制, 如果时间tick恰好为15:00:00, 将可能没有符合条件的 未来成交tick, 此时则以 当日收盘价, 即强制最后一个tick的价格
    @Column(name = "clinchDelaySecond", columnDefinition = "int")
    Integer clinchDelaySecond = 1; // @key3: 建议 1 或者 2 秒; 太长不合适, 0也不合适; 本设置很可能影响滑点大小! ; 负数会向前移动时间, 严禁!!!!!!


    public static void main(String[] args) {
        ReviseAccountWithOrder account = initAccountWithOrderWhenRiveStart("2022-06-06", "09:30:00", 100000);
        ReviseAccountWithOrderDao.saveOrUpdateBean(account);

    }

    /**
     * 当正式开始一次复盘时, 会实例化新的 账户对象!! 设置为新的当前账户!!!
     *
     * @return
     */
    public static ReviseAccountWithOrder initAccountWithOrderWhenRiveStart(String reviseDateStr,
                                                                           String reviseStartTimeStr,
                                                                           double initMoney
    ) {
        ReviseAccountWithOrder res = new ReviseAccountWithOrder();
        res.setInnerObjectType(INNER_TYPE_INIT);

        res.setReviseDateStr(reviseDateStr); // 2022-06-06
        res.setReviseStartTimeStr(reviseStartTimeStr); // 09:30:00
        res.setReviseStartDateTimeStr(reviseDateStr + " " + reviseStartTimeStr); // 标准的日期时间字符串
        // 复盘停止时间为null.

        // 设置当前时间, 它将不会再改变, 理论上能标志唯一账号; 唯一账号对应一次未停止复盘, 使用的唯一虚拟账号
        String currentWitMills = DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_PATTERN);
        res.setStartRealTime(currentWitMills);
        // 停止的真实时间也null

        res.setInitMoney(initMoney); // 初始资金, 不会改变
        res.setCash(initMoney); // 初始现金
        res.setTotalAssets(initMoney); // 初始总资产, 实时变动
        res.flushSixAccountMapJsonStr(); // 初始化为 "{}" // 相关map为空map

        // @key: 没有订单, 订单相关所有字段均不需要初始化, 全部null
        return res;
    }

    /**
     * 当正式停止一次复盘时, 会实例化新的 账户对象!!
     * 1.该对象, 设置内部对象类型为停止,
     * 2.复制此前的 账户对象, 的资金 资产 等属性;  无视订单相关属性
     * 3.自动将 "当前所有持仓" 以 当前 复盘tick 所对应的价格, 卖出, 执行全部卖出逻辑, 更新账户资产状态!!
     * --> "模拟全部卖出" 的卖出价格, 为停止时复盘 tick 的下一个tick的价格(同成交机制)
     *
     * @return
     */
    public static ReviseAccountWithOrder initAccountWithOrderWhenRiveStop(ReviseAccountWithOrder preAccount,
                                                                          String reviseStopTimeStr
    ) {
        ReviseAccountWithOrder res = new ReviseAccountWithOrder();
        res.setInnerObjectType(INNER_TYPE_STOP);

        res.setReviseDateStr(preAccount.getReviseDateStr()); // 2022-06-06
        res.setReviseStartTimeStr(preAccount.getReviseStartTimeStr()); // 09:30:00
        res.setReviseStartDateTimeStr(preAccount.getReviseStartDateTimeStr()); // 标准的日期时间字符串
        res.setReviseStopTimeStr(reviseStopTimeStr); // @key: 给定停止tick参数, 并设置

        // 设置当前时间, 它将不会再改变, 理论上能标志唯一账号; 唯一账号对应一次未停止复盘, 使用的唯一虚拟账号
        res.setStartRealTime(preAccount.getStartRealTime());
        String currentWitMills = DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_PATTERN);
        res.setStopRealTime(currentWitMills); // @key:实际停止时间!  它与开始, 本质上能确定唯一一次复盘, 但此前保存记录此字段无值!

        /*
        账户资产资金状态 设置: 将会执行 "全部卖出" 逻辑!
         */
        // todo!

        // @key: 没有订单, 订单相关所有字段均不需要初始化, 全部null
        return res;
    }

    /**
     * 提交新的 订单时, 需要新建 ReviseAccountWithOrder 对象;
     * 返回新的 账户订单对象, 账户相关字段, copy过来, 订单相关字段,设置为 给定的参数!
     * 返回值是 新的 账户订单 状态对象; 且填充了 订单相关字段, 且自动填充了未来tick作为参考成交价, 且判定是否能成交!
     * // @key3: 只填充未来tick作为成交参考, 且设置能否成交的flag, 但 不实际执行 "成交动作", 即暂不 更新账户状态
     * // @key3: 此时, 返回值对象, 的账户状态, 还是 订单提交前的状态 !!! 对象类型标志位 为 1!
     *
     * @param oldAccountState
     * @return
     */


    // 4大内部状态
    public static final String INNER_TYPE_INIT = "复盘开始"; // 复盘开始时初始化, 一次复盘的 源对象!
    public static final String INNER_TYPE_ORDER_SUBMIT = "订单提交"; // 订单提交而来, 会读取分时成交数据, 确定未来可能成交价和是否能够成交
    public static final String INNER_TYPE_ORDER_CLINCHED = "订单成交"; // 实际执行订单, 此时订单已经成交, 且刷新账户资产相关状态
    public static final String INNER_TYPE_ORDER_NOT_CLINCH = "订单未成交"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String INNER_TYPE_STOP = "复盘停止"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段


    public static final String NOT_CLINCH_REASON_BUY_PRICE_FAIL = "买单价格过低"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String NOT_CLINCH_REASON_SELL_PRICE_FAIL = "卖单价格过高"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String NOT_CLINCH_REASON_AMOUNT_ZERO_FAIL = "买卖单仓位对应数量为0"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String NOT_CLINCH_REASON_FSTRANS_NOT_EXISTS = "买卖单仓位对应数量为0"; // 实际执行订单, 但分时成交df数据没有!


    // @key3: 内部对象类型: 即表示 当前账户订单对象, 是 刚新建, 还是 提交订单时, 还是 执行订单后!
    @Column(name = "innerObjectType", columnDefinition = "varchar(32)")
    String innerObjectType; // 设置的复盘日期, 年月日

    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id")
    Long id;

    /*
    账号信息相关-- 复盘时间类, 其中, reviseStopTimeStr 和 stopRealTime 两个停止时间, 同一账号只可能有一条订单记录 同时带有! 且时间最后!
     */
    @Column(name = "AReviseDateStr", columnDefinition = "varchar(32)")
    String reviseDateStr; // 设置的复盘日期, 年月日
    @Column(name = "AReviseStartTimeStr", columnDefinition = "varchar(32)")
    String reviseStartTimeStr; // 设置的复盘开始时间! 时分秒
    @Column(name = "AReviseStartDateTimeStr", columnDefinition = "varchar(64)")
    String reviseStartDateTimeStr; // 设置的复盘开始  年月日+时分秒
    @Column(name = "AReviseStopTimeStr", columnDefinition = "varchar(32)")
    String reviseStopTimeStr; // 点击停止复盘时, 结算单个账号, 当时的 复盘等价时间! 时分秒

    @Column(name = "AStartRealTime", columnDefinition = "varchar(64)")
    String startRealTime; // 开始的真实时间; 现实时间 , 标准日期带毫秒
    @Column(name = "AStopRealTime", columnDefinition = "varchar(64)")
    String stopRealTime; // 结束的真实时间; 现实时间, 标准日期带毫秒


    /*
    账号持仓实时信息相关 -- 资金,持仓
     */

    // 2.账号资金资产数据
    @Column(name = "initMoney", columnDefinition = "double")
    Double initMoney = 10.0 * 10000; // 初始10万资金默认, 可修改
    @Column(name = "cash", columnDefinition = "double")
    Double cash = 10.0 * 10000;  // 当前现金; 初始需要设置为 initMoney, 随后随着下单, 将自动增减! 持仓也会增减!
    // 自动计算的账户属性!, flushAccount()
    @Column(name = "totalAssets", columnDefinition = "double")
    Double totalAssets;  // 当前总资产 == 现金 + 各个资产数量*价格求和

    /*
    单债统计map: 当前持仓数量,成本价,实时价格; 已发生盈利(卖出), 剩余持仓部分盈利百分比, 单债总浮盈!
     */
    // 当前持仓,初始空, key为转债 转债代码, value 为数量!
    @Transient
    ConcurrentHashMap<String, Integer> holdBondsAmountMap = new ConcurrentHashMap<>();
    @Column(name = "holdBondsAmountMap", columnDefinition = "longtext")
    String holdBondsMapJsonStr = "{}"; // 成分股列表json字符串

    // 单只资产, 当前的剩余仓位的持仓成本价格(已折算); key为转债代码, value 为当前剩余仓位的持仓成本
    @Transient
    ConcurrentHashMap<String, Double> bondCostPriceMap = new ConcurrentHashMap<>();
    @Column(name = "bondCostPriceMap", columnDefinition = "longtext")
    String bondCostPriceMapJsonStr = "{}";

    // 持有转债,当前实时价格map; 应当实时刷新
    @Transient
    ConcurrentHashMap<String, Double> holdBondsCurrentPriceMap = new ConcurrentHashMap<>();
    @Column(name = "holdBondsCurrentPriceMap", columnDefinition = "longtext")
    String holdBondsCurrentPriceMapJsonStr = "{}"; // 成分股列表json字符串

    // 单只资产, 今日已发生的收益, 元(即不包括当前持仓的浮盈, 实际转换为了钱的收益);
    // key为转债代码, value 为今日最终盈利 数值! 如果还有持仓, 以最新价格计算
    @Transient
    ConcurrentHashMap<String, Double> bondAlreadyProfitMap = new ConcurrentHashMap<>(); // @key如果要真正盈利综合, 需要加上剩余持仓浮盈
    @Column(name = "bondAlreadyProfitMap", columnDefinition = "longtext")
    String bondAlreadyProfitMapJsonStr = "{}";
    // 持有转债, 剩余的数量, 成本价 与 当前价格相比, 赚的比例, 即 当前价格map - 成本价格map! (成本价是折算价)
    @Transient
    ConcurrentHashMap<String, Double> holdBondsGainPercentMap = new ConcurrentHashMap<>();
    @Column(name = "holdBondsGainPercentMap", columnDefinition = "longtext")
    String holdBondsGainPercentMapJsonStr = "{}";
    // 持有转债, 已发生(卖出) 的盈利 + 当前剩余仓位浮盈, 即今日单债总盈利, 浮动!
    @Transient
    ConcurrentHashMap<String, Double> holdBondsTotalProfitMap = new ConcurrentHashMap<>();
    @Column(name = "holdBondsTotalProfitMap", columnDefinition = "longtext")
    String holdBondsTotalProfitMapJsonStr = "{}";

    /*
    订单信息相关!
     */
    @Column(name = "orderGenerateTick", columnDefinition = "varchar(64)")
    String orderGenerateTick; // 订单生成时间, 常用于但账户的订单排序, 形式为 HH:mm:ss, 是复盘的虚拟时间
    @Column(name = "orderGenerateTimeReal", columnDefinition = "varchar(64)")
    String orderGenerateTimeReal; // 订单生成时间, 真实时间, 带毫秒!
    @Column(name = "orderType", columnDefinition = "varchar(8)")
    String orderType; // 需要设定为 buy 或者 sell
    @Column(name = "targetCode", columnDefinition = "varchar(16)")
    String targetCode; // 目标转债代码
    @Column(name = "targetName", columnDefinition = "varchar(16)")
    String targetName; // 目标转债名称
    @Column(name = "targetQuoteId", columnDefinition = "varchar(16)")
    String targetQuoteId; // 目标转债 东财行情id, 方便访问数据库数据

    // 订单 给出的买入卖出价格, 有可能不成交
    @Column(name = "orderPrice", columnDefinition = "double")
    Double orderPrice; // 交易价格, 复盘时, 访问"未来数据" 以确定价格! 模拟订单成交了!
    // @key: 买卖单, 均使用核按钮, 并且以 仓位形式给出! 常态有 1/1,1/2,1/3,1/4 --> 订单需要提供此值, 实际数量由此计算
    @Column(name = "oderPositionPercent", columnDefinition = "double")
    Double orderPositionPercent; // 订单仓位
    @Column(name = "amount", columnDefinition = "int")
    Integer amount; // @key: 订单数量, 张数, 由给定的仓位参数, 而自动计算!!!!!!!!!!! 且是 10的倍数(不区分沪深)

    // @key: 读取分时成交未来数据, 给出下1/2 tick的实时价格!!!, 将判定其与订单价格的大小, 判定是否能够成交
    // @key: 当订单生成时, 需要提供 orderGenerateTick ,时分秒, 将以此tick, 自动读取df, 计算 未来的可能成交价格
    @Column(name = "clinchPriceFuture", columnDefinition = "double")
    Double clinchPriceFuture;
    @Column(name = "clinchTimeTickFuture", columnDefinition = "varchar(16)")
    String clinchTimeTickFuture; // 同理, 自动计算的, 未来可能的成交时间tick,
    // 当自动计算 未来可能的成交价格后, 将对比订单给的价格, 自动判定 是否成交!!
    // 另外, 如果 amount 计算出来, 为 0 (张), 那么也设置为 无法成交! --> 设置对应的 未成交原因字段!
    @Column(name = "canClinch")
    Boolean canClinch;
    @Column(name = "notClinchReason", columnDefinition = "longtext")
    String notClinchReason; // 当执行真实成交, 会对 价格和数量执行实际判定, 是否能够成交, 给出原因描述

    @Column(name = "stopAutoOrderFlag")
    Boolean stopAutoOrderFlag = false; // 默认不是收盘自动卖出订单; 在stop复盘时, 自动执行停止卖出订单, 此属性将设置为 true!!!

    /**
     * 刷新3个账户资产map的jsonStr 属性, 即设置3大jsonstr属性, 用对应属性的 hm
     */
    public void flushSixAccountMapJsonStr() {
//        holdBondsAmountMap
//                bondAlreadyProfitMap
//        bondCostPriceMap
//                holdBondsCurrentPriceMap
//        holdBondsGainPercentMap
//                holdBondsTotalProfitMap
        holdBondsMapJsonStr = JSONUtilS.toJsonStr(holdBondsAmountMap);
        bondAlreadyProfitMapJsonStr = JSONUtilS.toJsonStr(bondAlreadyProfitMap);
        bondCostPriceMapJsonStr = JSONUtilS.toJsonStr(bondCostPriceMap);
        holdBondsCurrentPriceMapJsonStr = JSONUtilS.toJsonStr(holdBondsCurrentPriceMap);
        holdBondsGainPercentMapJsonStr = JSONUtilS.toJsonStr(holdBondsGainPercentMap);
        holdBondsTotalProfitMapJsonStr = JSONUtilS.toJsonStr(holdBondsTotalProfitMap);
    }

    /**
     * @return 提交订单, 填充相关订单字段后的 账户订单 对象
     * @key3 以this当前账户的状态, 新建对象, 复制账户状态后(不复制订单相关属性),
     * 使用参数, 执行提交订单逻辑, 但不 执行成交判定和 更新账户状态 !!!
     */
    public ReviseAccountWithOrder submitNewOrder(
            String orderGenerateTick, // 下单的 复盘虚拟tick, 时分秒
            String orderType, // 类型, buy 或者 sell
            SecurityBeanEm orderBean, // 转债东财bean, 获取转债基本信息!
            Double price, // 价格
            Double positionPercent, // 仓位!
            boolean stopAutoOrderFlag // 是否为stop时自动生成的卖出订单???
    ) {
        ReviseAccountWithOrder res = new ReviseAccountWithOrder();
        /*
         * 1. 内部类型, 自设
         */
        res.setInnerObjectType(INNER_TYPE_ORDER_SUBMIT);
        if (stopAutoOrderFlag) {
            res.setStopAutoOrderFlag(true); // 标志了是 停止阶段的 自动卖出订单! 否则就是期间的正常 买卖单
        }

        copyTimeAndMoneyAndMapAttrs(res); // 复制设置 时间/金钱/6map 等基本字段

        /*
        5.订单相关字段:
         */
        res.orderGenerateTick = orderGenerateTick; // 参数必须给定
        res.orderGenerateTimeReal = DateUtil
                .format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_PATTERN); // 真实的此刻, 带毫秒
        res.orderType = orderType; // buy 还是 sell??? 参数给定
        res.targetCode = orderBean.getSecCode(); // 给定东财bean--> 3属性 参数给定
        res.targetName = orderBean.getName();
        res.targetQuoteId = orderBean.getQuoteId();

        res.orderPrice = price;
        res.orderPositionPercent = positionPercent;

        /*
        6.订单提交, 就计算出与成交相关的属性, 但不执行 成交动作, 即不会修改账户状态!
        即 INNER_TYPE_ORDER_SUBMIT 状态 而非 INNER_TYPE_ORDER_CLINCHED /NOT_CLINCH 状态
         */

        // todo: res.clinchPriceFuture = xx
        // todo: res.clinchTimeTickFuture = xx
        // todo: 自动计算: res.amount = xx
        // todo: res.canClinch = true? false
        // todo: notClinchReason= "未成交原因?"

        // 6.1. 访问分时成交df, 并依据延迟成交算法, 得到成交 tick 行;
        DataFrame<Object> fsTransDf = EastMoneyDbApi // 缓存的分时成交df
                .getFsTransByDateAndQuoteIdS(reviseDateStr, targetQuoteId, false);
        DateTime submitTime = DateUtil.parseTime(orderGenerateTick); // 年月日默认 19700101; 只有时分秒有效
        DateTime offset = DateUtil.offset(submitTime, DateField.SECOND, clinchDelaySecond);
        // 找到第一个>=本tick的  有数据的tick, 作为 成交tick!!
        String virtualClinchMinTick = DateUtil.format(offset, DatePattern.NORM_TIME_PATTERN);
        if (fsTransDf != null) {
            int clinchIndex = calcShouldClinchDfRowIndex(fsTransDf, virtualClinchMinTick);
            res.clinchPriceFuture = Double.valueOf(fsTransDf.get(clinchIndex, "price").toString());
            res.clinchTimeTickFuture = fsTransDf.get(clinchIndex, "time_tick").toString();

            // 依据仓位计算 数量(张数), 精确到 10的倍数, 即一手! 与同花顺相同, 向下取整!!
            if ("buy".equals(orderType)) {
                double shouldPositionMoney = totalAssets * orderPositionPercent; // 总资产 * 仓位 == 想要买入的现金;
                Double shouldHands = shouldPositionMoney / res.orderPrice / 10; // 浮点数的最大手数!
                int actualHands = shouldHands.intValue(); // 实际订单手数
                res.amount = actualHands * 10; // 最终订单数量!
                res.canClinch = clinchPriceFuture <= res.orderPrice; // 需要订单给的价格更大, 才成交, 否则不成交!

                if (res.amount <= 0) {
                    res.notClinchReason = NOT_CLINCH_REASON_AMOUNT_ZERO_FAIL; // 为0, 失败原因; 其实是下单失败
                }
                if (!res.canClinch) { // 不能成交
                    res.notClinchReason = NOT_CLINCH_REASON_BUY_PRICE_FAIL;
                }
                // 否则 notClinchReason == null; 即未初始化!
            } else { // 卖单!
                Double shouldSellHand = res.amount * positionPercent / 10; // 应当卖出的手数
                int shouldSellAmount = shouldSellHand.intValue();
                res.canClinch = clinchPriceFuture >= res.orderPrice; // 成功卖出
                if (res.amount <= 0) {
                    res.notClinchReason = NOT_CLINCH_REASON_AMOUNT_ZERO_FAIL; // 为0, 失败原因; 其实是下单失败
                }
                if (!res.canClinch) { // 不能成交
                    res.notClinchReason = NOT_CLINCH_REASON_SELL_PRICE_FAIL;
                }
            }
        } else {
            res.notClinchReason = NOT_CLINCH_REASON_FSTRANS_NOT_EXISTS; // 分时成交数据木有, 无法成交.
        }

        /*
        7.不会执行成交 动作, 即不修改账号信息
         */
        return res;
    }

    /**
     * 实际的成交执行, 主要是 刷新账户相关信息, 主要是修改新的转债数量, 折算成本价(加仓时主要),
     *
     * @param orderGenerateTick
     * @param orderType
     * @param orderBean
     * @param price
     * @param positionPercent
     * @param stopAutoOrderFlag
     * @return
     */
    public ReviseAccountWithOrder clinchOrderDetermine(
    ) {
        ReviseAccountWithOrder res = new ReviseAccountWithOrder();
        /*
         * 1. 内部类型, 自设
         */
//       todo: 最终成交状态 res.setInnerObjectType(INNER_TYPE_ORDER_CLINCHED);
        res.setStopAutoOrderFlag(this.stopAutoOrderFlag); // 是否停止时自动的卖单flag, 复制而来
        /*
        2.复制基本字段
         */
        copyTimeAndMoneyAndMapAttrs(res); // 复制设置 时间/金钱/6map 等基本字段

        /*
        3.复制订单相关字段
         */
        copyOrderBaseAttrAndFiveAutoOrderAttrs(res); // 复制订单基本字段, 以及5大已经自动计算了的成交相关属性!

        /*
        4.执行成交 动作, @key: 核心步骤
         */


        // todo: res.clinchPriceFuture = xx
        // todo: res.clinchTimeTickFuture = xx
        // todo: 自动计算: res.amount = xx
        // todo: res.canClinch = true? false
        // todo: notClinchReason= "未成交原因?"

        if (notClinchReason == null) { // 不存在未成交原因时, 执行成交, 账户状态变化! // 修改res的属性, 而非this
            // 成交条件: 1. amount>0,且为10整数倍;  2.价格合适能与未来tick匹配成交!
            if ("buy".equals(res.orderType)) {
                // 买单顺利成交!
                // 1.转债 数量增加, 成本变化!
                // cash 将会变化; totalAssets 假设瞬间情况下价格变化为0, 本质上成交时刻是不会变化的!!!!!!
                /*
                res.holdBondsAmountMap.putAll(holdBondsAmountMap);
                res.bondAlreadyProfitMap.putAll(bondAlreadyProfitMap);
                res.bondCostPriceMap.putAll(bondCostPriceMap);
                res.holdBondsCurrentPriceMap.putAll(holdBondsCurrentPriceMap);
                res.holdBondsGainPercentMap.putAll(holdBondsGainPercentMap);
                res.holdBondsTotalProfitMap.putAll(holdBondsTotalProfitMap);
                 */
                // 1.现金减少!
                res.cash = res.cash - res.clinchPriceFuture * res.amount; // 成交数量*价格, 现金减去
                // 2.转债数量增加!
                Integer rawAmount = res.holdBondsAmountMap.getOrDefault(res.targetCode, 0);
                res.holdBondsAmountMap.put(res.targetCode, rawAmount + res.amount);
                // 3.转债已实现盈利不变, 视为瞬间价格并没有变化! 该值变化需要 卖单, 才能确定

                // 4.转债 折算成本, 可能变化!
                Double rawCostPrice = res.bondCostPriceMap.get(res.targetCode);
                if (rawCostPrice == null) { // 此前尚未有过持仓
                    res.bondCostPriceMap.put(res.targetCode, res.clinchPriceFuture); // 以本次买单成交价格作为成本价
                } else { // 有持仓, 则 用总成本 / 总数量
                    double newCost =
                            (rawAmount * rawCostPrice + res.clinchPriceFuture * res.amount) / (res.amount + rawAmount);
                    res.bondCostPriceMap.put(res.targetCode, newCost);
                }
                // 5.转债当前价格: 自动刷新! 暂时瞬间使用成交价格,
                res.holdBondsCurrentPriceMap.put(res.targetCode, res.clinchPriceFuture);
                // 6.当前持仓, 的盈利百分比, 用当前价格 / 持仓成本 -1
                res.holdBondsGainPercentMap
                        .put(res.targetCode, res.clinchPriceFuture / res.bondCostPriceMap.get(res.targetCode));
                // 7.单转债总盈利 元 -- 参考值! 用已经实现, + 当前持仓盈利 即可


            }


        }
        // 未成交, 则不会更新账户状态

        return res;
    }

    public void copyOrderBaseAttrAndFiveAutoOrderAttrs(ReviseAccountWithOrder res) {
        res.orderGenerateTick = this.orderGenerateTick; // 复制
        res.orderGenerateTimeReal = this.orderGenerateTimeReal; // 复制
        res.orderType = this.orderType; // buy 还是 sell??? 复制
        res.targetCode = this.targetCode;
        res.targetName = this.targetName;
        res.targetQuoteId = this.targetQuoteId;
        res.orderPrice = this.orderPrice;
        res.orderPositionPercent = this.orderPositionPercent;


        res.notClinchReason = this.notClinchReason;
        res.clinchPriceFuture = this.clinchPriceFuture;
        res.clinchTimeTickFuture = this.clinchTimeTickFuture;
        res.amount = this.amount;
        res.canClinch = this.canClinch;
    }


    private void copyTimeAndMoneyAndMapAttrs(ReviseAccountWithOrder res) {
    /*
    2.复盘日期时间 和 真实 日期时间: 复制
     */
        res.setReviseDateStr(this.getReviseDateStr()); // 2022-06-06
        res.setReviseStartTimeStr(this.getReviseStartTimeStr()); // 09:30:00
        res.setReviseStartDateTimeStr(this.getReviseStartDateTimeStr()); // 标准的日期时间字符串
        res.setReviseStopTimeStr(this.getReviseStopTimeStr()); // @key: 通常是 null;

        res.setStartRealTime(this.getStartRealTime());
        res.setStopRealTime(this.getStopRealTime()); // 常常null

        /*
        3. 账户 3大资金资金数据: 复制
         */
        res.setInitMoney(initMoney);
        res.setCash(cash);
        res.setTotalAssets(totalAssets);

        /*
        4.账户 6大 转债列表映射: 复制, 并初始化 对应JsonStr 属性
         */
        res.holdBondsAmountMap.putAll(holdBondsAmountMap);
        res.bondAlreadyProfitMap.putAll(bondAlreadyProfitMap);
        res.bondCostPriceMap.putAll(bondCostPriceMap);
        res.holdBondsCurrentPriceMap.putAll(holdBondsCurrentPriceMap);
        res.holdBondsGainPercentMap.putAll(holdBondsGainPercentMap);
        res.holdBondsTotalProfitMap.putAll(holdBondsTotalProfitMap);

        res.flushSixAccountMapJsonStr();
    }

    /**
     * 给定完整的分时成交df, 给定一个 最早的成交tick,  找到第一个 >= 该 tick的 行, 作为 备用成交行!
     * 将保留该行 tick 和 price , 作为成交价格 !
     *
     * @param fsTransDf
     * @param minTickForClinch
     */
    public static int calcShouldClinchDfRowIndex(DataFrame<Object> fsTransDf, String minTickForClinch) {
        // 3. 筛选有效分时成交! time_tick 列
        int shouldIndex = 0; // 如果时间过早, 用第一个竞价tick也勉强符合, 但不够严谨
        for (int i = 0; i < fsTransDf.length(); i++) {
            String timeTick1 = fsTransDf.get(i, "time_tick").toString();
            shouldIndex = i; // 直接赋值
            if (timeTick1.compareTo(minTickForClinch) >= 0) { // 当当前tick>= 参数, 跳出; 符合要求
                break;
            }
        }
        // 可能为0, 即给的参数时间太早了, 还在竞价之前;
        // 可能为最后一个tick, 因为 已经 > 15:00:00; 也是很可能的;
        // 总之符合逻辑
        return shouldIndex;
    }
}



