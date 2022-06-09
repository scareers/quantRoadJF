package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

/**
 * description: 复盘时模拟账号!
 * 0.@update: 为了应对虚拟账号机制,对复盘开始时间的修改, 必须要求 running=false时才可进行!
 * <p>
 * 1.单次开始复盘, 重置账号!!!
 * 2.直到点击停止 ! 账号的状态保存!
 * 3.账号名称, 以 当前真实日期时间(开始时间) + 点击开始时, 的 复盘开始时间, 两者结合, 作为唯一ID;;
 * 4.只实现 买/卖功能, 视为下单后必定成交, 无撤单功能!
 *
 * @author: admin
 * @date: 2022/6/9/009-12:52:58
 */
public class ReviseAccount {
    String reviseDateStr; // 设置的复盘日期, 年月日
    String reviseStartTimeStr; // 设置的复盘开始时间! 时分秒
    String reviseStartDateTimeStr; // 设置的复盘开始  年月日 时分秒
    String reviseStopDateStr; // 点击停止复盘时, 结算单个账号, 当时的 复盘等价时间! 时分秒

    String startRealTime; // 开始的真实时间; 现实时间
    String stopRealTime; // 结束的真实时间; 现实时间


}



