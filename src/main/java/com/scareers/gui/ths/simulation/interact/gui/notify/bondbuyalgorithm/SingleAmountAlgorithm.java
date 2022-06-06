package com.scareers.gui.ths.simulation.interact.gui.notify.bondbuyalgorithm;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * description: 单笔tick, 的成交额, 比前n秒内的成交额均值, 大幅放大! 代表了 拉升初始 或者 下杀   第一个tick的可能性!
 *
 * @author: admin
 * @date: 2022/5/30/030-17:56:10
 */
public class SingleAmountAlgorithm extends BondBuyNotify.BondStateAlgorithm {
    public int periodSeconds = 33; // 时间窗口大小, 单位秒
    public double rateBig = 4.0; // 单个tick成交额, 需要>前n秒内, 所有tick的平均值得 这么多倍, 才算大单出现
    public double rateSky = 7.0; // 天量出现倍率阈值
    public double minNewestTickAmount = 100 * 10000; // 最新tick最少成交额限制

    // 衡量 买卖方其中 一方力量明显大 的比率阈值; 即时间窗口内买方成交量和卖方成交量, 差距很大; 正数, 买卖方自行判定
    public double windowBuySellDiffRate = 0.3;

    public long expireMillsDefault = 1000;  // 1秒过期
    public long priorityDefault = 500;  // 优先级

    public SingleAmountAlgorithm() {
    }

    @Override
    public BondBuyNotify.NotifyMessage describe(SecurityBeanEm bondBean, SecurityBeanEm stockBean,
                                                StockBondBean stockBondBean) {
        DataFrame<Object> fsTransData = this.getFsTransDfOfBond(bondBean);
        if (fsTransData == null || fsTransData.length() == 0) {
            return null; // 暂无无分时成交数据
        }

        // sec_code	market	time_tick	price	 vol	bs, 使用顺序 --> 复盘的数据库api, 也需要保证前n列, 是这些!!
        // 1.找到 分钟 tick点;
        String timeTickLast = fsTransData.get(fsTransData.length() - 1, "time_tick").toString();
        DateTime lastTime = DateUtil.parse(timeTickLast);
        // 时间窗口开始时间,包含
        DateTime windowStartTime = DateUtil.offset(lastTime, DateField.SECOND, -1 * (periodSeconds + 3)); // 已经+3,排除自身
        String startTime = DateUtil.format(windowStartTime, DatePattern.NORM_TIME_PATTERN);

        String notiedTimeTick = lastNotifyTimeTickMap.get(bondBean.getQuoteId()); // 单tick播放一次
        if (timeTickLast.equals(notiedTimeTick)) {
            return null; // 没有新数据, 当前最后一条数据, 提示过了
        }

        Long lastNotiTime = notifyTimeMillsMap.get(bondBean.getQuoteId());
        if (lastNotiTime != null) {
            if (System.currentTimeMillis() - lastNotiTime <= forceNotNotifyPeriod) {
                return null;
            }
        }

        // 2.筛选得到有效df 数据段
        DataFrame<Object> effectDf = fsTransData.select(new DataFrame.Predicate<Object>() {
            @Override
            public Boolean apply(List<Object> value) {
                Object o = value.get(2);
                if (o == null) {
                    return false;
                }
                String s = o.toString();
                return s.compareTo(startTime) >= 0; // 包含了最后一个tick, 需要注意
            }
        });
        // Console.log(effectDf);

        if (effectDf.length() - 1 < (periodSeconds / 6)) { // 起码有一半数据, 6s一个
            return null;
        }

        // 3.用到 price, vol, bs --> 首先计算成交额
        // 3.1. 成交额列
        List<Double> amountCol = new ArrayList<>();
        for (int i = 0; i < effectDf.length(); i++) {
            try {
                Double amount = Double.parseDouble(effectDf.get(i, 3).toString()) * Double
                        .parseDouble(effectDf.get(i, 4).toString());
                amountCol.add(amount);
            } catch (NumberFormatException e) {
                amountCol.add(null);
            }
        }

        // 3.2. 此时最后一行是最新一个tick, 前面所有, 均为时间窗口内的;
        Double newestAmount = amountCol.get(amountCol.size() - 1); // 最新tick的成交额
        // @update: 东财tick数据, 成交量为手; 实际计算时需按手数 , *10
        if (newestAmount * 10 < minNewestTickAmount) {
            return null; // 至少100万, 要求不过分
        }
        Double maxAmountPre = CommonUtil.maxOfListDouble(amountCol); // 此前时间窗口内, 最大成交额
        if (newestAmount < maxAmountPre * 1) { // 要求至少2倍
            return null; // 至少要求最新成交额, >时间窗口中最大的成交额, 再计算 与平均值的倍率
        }
        // 3.3. 最新tick成交额倍率计算
        Double avgAmountPre = CommonUtil.avgOfListNumberUseLoop(amountCol); // 此前时间窗口内, 平均成交额
        double rate = newestAmount / avgAmountPre;
        if (rate < rateBig) {
            return null; // 倍率太小, 没有超过播报第一阈值
        }

        // 3.4. 时间窗口买卖方向成交额比率计算
        List<Double> buyVols = new ArrayList<>();
        List<Double> sellVols = new ArrayList<>();
        for (int i = 0; i < effectDf.length(); i++) {
            List<Object> row = effectDf.row(i);
            int bs = Integer.parseInt(row.get(5).toString());
            if (bs == 2) {
                buyVols.add(Double.parseDouble(row.get(4).toString()));
            } else if (bs == 1) {
                sellVols.add(Double.parseDouble(row.get(4).toString()));
            }
        }
        if (buyVols.size() == 0 && sellVols.size() == 0) {
            return null;
        }
        Double buyVolAll = CommonUtil.sumOfListNumber(buyVols);
        Double sellVolAll = CommonUtil.sumOfListNumber(sellVols);
        double windowBsVolRate = (buyVolAll - sellVolAll) / (buyVolAll + sellVolAll); // 时间窗口内的买卖方向净买方比

        // 3.5. 最新的tick买卖方向:
        int bs = Integer.parseInt(effectDf.get(effectDf.length() - 1, 5).toString());  // 2买方, 1卖方

        // 3.6. 使用 rate, windowBsVolRate, bs, 判定播报
        String description = null;
        if (rate < rateSky) { // 普通大倍率
            if (bs == 2) { // 大买
                if (windowBsVolRate > windowBuySellDiffRate) { // 时间窗口内, 已经有了小买方明显优势, 大单拉升尝试
                    description = "瞬买拉";
                } else if (windowBsVolRate < -windowBuySellDiffRate) { // 时间窗口内, 卖方明显优势, 则本tick为抄底大单
                    description = "瞬买抄";
                } else { // 窗口买卖方偏向于中性
                    description = "瞬买";
                }
            } else if (bs == 1) { //大卖
                if (windowBsVolRate > windowBuySellDiffRate) { // 时间窗口内, 已经有了小买方明显优势; 则本tick为出货大单
                    description = "瞬卖出";
                } else if (windowBsVolRate < -windowBuySellDiffRate) { // 时间窗口内, 卖方明显优势, 大单杀跌
                    description = "瞬卖杀";
                } else { // 窗口买卖方偏向于中性
                    description = "瞬卖";
                }
            }
        } else { // 天大倍率
            if (bs == 2) { // 天买
                if (windowBsVolRate > windowBuySellDiffRate) { // 时间窗口内, 已经有了小买方明显优势, 大单拉升尝试
                    description = "天买拉";
                } else if (windowBsVolRate < -windowBuySellDiffRate) { // 时间窗口内, 卖方明显优势, 则本tick为抄底大单
                    description = "天买抄";
                } else { // 窗口买卖方偏向于中性
                    description = "天买";
                }
            } else if (bs == 1) { //天卖
                if (windowBsVolRate > windowBuySellDiffRate) { // 时间窗口内, 已经有了小买方明显优势; 则本tick为出货大单
                    description = "天卖出";
                } else if (windowBsVolRate < -windowBuySellDiffRate) { // 时间窗口内, 卖方明显优势, 大单杀跌
                    description = "天卖杀";
                } else { // 窗口买卖方偏向于中性
                    description = "天卖";
                }
            }
        }

        if (description == null) {
            return null;
        }

        // 长信息
        String infoLong = StrUtil
                .format("{} {} : 最新tick成交额:{} ;买卖方向:{} ; 窗口买卖盘比率:{}", bondBean.getName(), description,
                        newestAmount, bs, windowBsVolRate
                );
        // 短信息
        String infoShort = StrUtil.format("{}{}", bondBean.getName().replace("转债", ""), description);

        // 保留记录到map, 以被限制
        lastNotifyTimeTickMap.put(bondBean.getQuoteId(), timeTickLast);
        notifyTimeMillsMap.put(bondBean.getQuoteId(), System.currentTimeMillis());

        BondBuyNotify.NotifyMessage res = new BondBuyNotify.NotifyMessage();
        res.setInfoShort(infoShort);
        res.setInfoLong(infoLong);
        res.setPriority(priorityDefault);
        res.setExpireMills(expireMillsDefault);
        res.setGenerateMills(BondBuyNotify.getCurrentMills());

        return res;
    }
}
