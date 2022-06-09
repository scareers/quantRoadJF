package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.scareers.sqlapi.ThsDbApi;
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
    public static void main(String[] args) {
        ReviseAccountWithOrder x = new ReviseAccountWithOrder();
        ReviseAccountWithOrderDao.saveOrUpdateBean(x);

    }

    /**
     * 当正式开始一次复盘时, 会实例化新的 账户对象!! 设置为新的当前账户!
     *
     * @return
     */
    public static ReviseAccountWithOrder initAccountWithOrderWhenRiveStart(String reviseDateStr,
                                                                           String reviseStartTimeStr) {
        ReviseAccountWithOrder res = new ReviseAccountWithOrder();
        res.setReviseDateStr(reviseDateStr); // 2022-06-06
        res.setReviseStartTimeStr(reviseStartTimeStr); // 09:30:00
        res.setReviseStartDateTimeStr(reviseDateStr + " " + reviseStartTimeStr); // 标准的日期时间字符串
        // 复盘停止时间为null.

        // 设置当前时间, 它将不会再改变, 理论上能标志唯一账号; 唯一账号对应一次未停止复盘, 使用的唯一虚拟账号
        String currentWitMills = DateUtil.format(DateUtil.date(), DatePattern.NORM_DATETIME_MS_PATTERN);
        res.setStartRealTime(currentWitMills);
        // 停止的真实时间也null
        res.setGenerateTime(currentWitMills); // 订单生成时间, 也默认设置, 当订单生成时, 将会刷新它!

        return null;
    }

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

    // 1.佣金配置
    @Column(name = "commissionRate", columnDefinition = "double")
    Double commissionRateShen = 0.00006; // 佣金率, 深市 十万分之6
    Double commissionRateHu = 0.000002; // 佣金率, 沪市 百万分之2
    Double commissionMinShen = 0.1; // 深市起收 1毛钱
    Double commissionMinHu = 0.1; // 沪市起收 1毛钱

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
    String bondCostPriceJsonStr = "{}";

    /*
    订单信息相关!
     */
    @Column(name = "orderGenerateTime", columnDefinition = "varchar(64)")
    String orderGenerateTime; // 订单生成时间, 常用于但账户的订单排序, 形式为 HH:mm:ss, 是复盘的虚拟时间
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
    @Column(name = "amount", columnDefinition = "int")
    Integer amount; // 转债数量, 整数!, 自行计算合适的数量, 然后设置!
    @Column(name = "price", columnDefinition = "double")
    Double price; // 交易价格, 复盘时, 访问"未来数据" 以确定价格! 模拟订单成交了!

}



