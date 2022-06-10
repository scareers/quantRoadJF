package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.JSONUtilS;
import com.thoughtworks.qdox.parser.expression.PlusSignDef;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 复盘时模拟账号 + 订单;  账号和订单放入一个表之内, 账号相关列A开头, 订单相关列 O开头
 * // @key3: 单条记录, 包含了账号全基本信息, 以及账号瞬间的资产状况, 以及单个订单对象!
 * // @key3: 订单保存时, 账号状况是此前状况, 订单保存后, 才成交, 并刷新账户状态
 * 0.@update: 为了应对虚拟账号机制,对复盘开始时间的修改, 必须要求 running=false时才可进行!
 * // @key2: 因新订单生成并保存, 应当保存新的对象, 因此实现 from(ReviseAccountWithOrder oldStare) , 复制初始状态!
 * // @key3: 成交机制,买卖单给出价格, 读取未来tick价格, 若不合适则无法成交! 视为"自动立即撤单",设置canClinch=false; 而不会修改账户状态!!
 * // @key1: 因hibernate机制, 本质上复盘程序中, 账户状态对象, 会不断是新对象, 以便能够保存新记录到数据库, 而非修改对象属性, 那样只会修改数据库记录,而非增加!
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
    Integer clinchDelaySecond = 1; // @key3: 建议 1 或者 2 秒; 太长不合适, 0也不合适; 本设置很可能影响滑点大小!


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
        res.flushThreeAccountMapJsonStr(); // 初始化为 "{}" // 相关map为空map

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


    public static final String NOT_CLINCH_REASON_BUY__PRICE_FAIL = "买单价格过低"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String NOT_CLINCH_REASON_SELL_PRICE_FAIL = "卖单价格过高"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段
    public static final String NOT_CLINCH_REASON_AMOUNT_ZERO_FAIL = "买卖单仓位对应数量为0"; // 实际执行订单, 但因价格问题, 订单未成交! 将设置未成交原因字段


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

    // 当前持仓,初始空, key为转债 转债代码, value 为数量!
    @Transient
    ConcurrentHashMap<String, Integer> holdBondsMap = new ConcurrentHashMap<>();
    @Column(name = "holdBondsMap", columnDefinition = "longtext")
    String holdBondsMapJsonStr = "{}"; // 成分股列表json字符串
    // 单只资产, 今日收益, 元; key为转债代码, value 为今日最终盈利 数值! 如果还有持仓, 以最新价格计算
    @Transient
    ConcurrentHashMap<String, Double> bondProfitMap = new ConcurrentHashMap<>();
    @Column(name = "bondProfitMap", columnDefinition = "longtext")
    String bondProfitMapJsonStr = "{}";
    // 单只资产, 当前的剩余仓位的持仓成本价格(已折算); key为转债代码, value 为当前剩余仓位的持仓成本
    @Transient
    ConcurrentHashMap<String, Double> bondCostPriceMap = new ConcurrentHashMap<>();
    @Column(name = "bondCostPriceMap", columnDefinition = "longtext")
    String bondCostPriceMapJsonStr = "{}";

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
    Double oderPrice; // 交易价格, 复盘时, 访问"未来数据" 以确定价格! 模拟订单成交了!
    // @key: 买卖单, 均使用核按钮, 并且以 仓位形式给出! 常态有 1/1,1/2,1/3,1/4 --> 订单需要提供此值, 实际数量由此计算
    @Column(name = "oderPositionPercent", columnDefinition = "double")
    Double oderPositionPercent; // 订单仓位
    @Column(name = "amount", columnDefinition = "int")
    Integer amount; // @key: 订单数量, 张数, 由给定的仓位参数, 而自动计算!!!!!!!!!!! 且是 10的倍数(不区分沪深)

    // @key: 读取分时成交未来数据, 给出下1/2 tick的实时价格!!!, 将判定其与订单价格的大小, 判定是否能够成交
    // @key: 当订单生成时, 需要提供 orderGenerateTick ,时分秒, 将以此tick, 自动读取df, 计算 未来的可能成交价格
    @Column(name = "clinchPriceFuture", columnDefinition = "double")
    Double clinchPriceFuture;
    // 当自动计算 未来可能的成交价格后, 将对比订单给的价格, 自动判定 是否成交!!
    // 另外, 如果 amount 计算出来, 为 0 (张), 那么也设置为 无法成交! --> 设置对应的 未成交原因字段!
    @Column(name = "canClinch")
    Boolean canClinch;
    @Column(name = "notClinchReason", columnDefinition = "longtext")
    String notClinchReason; // 当执行真实成交, 会对 价格和数量执行实际判定, 是否能够成交, 给出原因描述

    /**
     * 刷新3个账户资产map的jsonStr 属性, 即设置3大jsonstr属性, 用对应属性的 hm
     */
    public void flushThreeAccountMapJsonStr() {
        holdBondsMapJsonStr = JSONUtilS.toJsonStr(holdBondsMap);
        bondProfitMapJsonStr = JSONUtilS.toJsonStr(bondProfitMap);
        bondCostPriceMapJsonStr = JSONUtilS.toJsonStr(bondCostPriceMap);
    }

}



