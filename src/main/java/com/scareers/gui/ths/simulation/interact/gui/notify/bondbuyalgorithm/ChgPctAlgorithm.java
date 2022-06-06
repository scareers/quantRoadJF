package com.scareers.gui.ths.simulation.interact.gui.notify.bondbuyalgorithm;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.notify.BondBuyNotify;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 转债 近n秒快速拉升或者下跌
 *
 * @author: admin
 * @date: 2022/5/30/030-17:56:10
 */
public class ChgPctAlgorithm extends BondBuyNotify.BondStateAlgorithm {
    public int periodSeconds = 30; // 监控转债走势时, 监控的时间窗口大小, 单位时秒
    public double chgPercent = 0.0065; // 走势变化>= 该数值时, 播报
    public double buySellRate = 0.3; // 衡量 买卖方其中 一方力量 特大/大 的比率阈值

    public long expireMillsDefault = 1000;  // 1秒过期
    public long priorityDefault = 1000;  // 优先级

    public ChgPctAlgorithm() {
    }

    @Override
    public BondBuyNotify.NotifyMessage describe(SecurityBeanEm bondBean, SecurityBeanEm stockBean,
                                                StockBondBean stockBondBean) {
        DataFrame<Object> fsTransData = this.getFsTransDfOfBond(null, bondBean);
        if (fsTransData == null || fsTransData.length() == 0) {
            return null; // 暂无无分时成交数据
        }

        // sec_code	market	time_tick	price	 vol	bs, 使用顺序
        // 1.找到 分钟 tick点;
        String timeTickLast = fsTransData.get(fsTransData.length() - 1, 2).toString();
        DateTime lastTime = DateUtil.parse(timeTickLast);
        // 时间窗口开始时间,包含
        DateTime windowStartTime = DateUtil.offset(lastTime, DateField.SECOND, -1 * periodSeconds);
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
                return s.compareTo(startTime) >= 0;
            }
        });


        // 3.计算涨跌幅播报, 且当 买卖方向成交量差距明显时, 播报买
        if (effectDf.length() < (periodSeconds / 6)) { // 起码有一半数据, 6s一个
            return null;
        }

        double priceStart;
        double priceEnd;
        try {
            priceStart = Double.parseDouble(effectDf.row(0).get(3).toString());
            priceEnd = Double.parseDouble(effectDf.row(effectDf.length() - 1).get(3).toString());
        } catch (NumberFormatException e) {
            return null;
        }
        if (Math.abs((priceEnd - priceStart) / priceStart) < chgPercent) {
            return null; // 涨跌幅不够, 无视
        }

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

        String description = null;
        if (priceStart < priceEnd) {
            // 价格上涨
            if ((buyVolAll - sellVolAll) / (buyVolAll + sellVolAll) > buySellRate) {
                // 买力量明显大, 至少 6.5 / 3.5 的样子
                description = "量拉";
            } else {
                description = "拉升";
            }
        } else {
            // 价格下降
            if ((sellVolAll - buyVolAll) / (buyVolAll + sellVolAll) > buySellRate) {
                // 卖力量明显大, 至少 6.5 / 3.5 的样子
                description = "量跌";
            } else {
                description = "下跌";
            }
        }

        // 长信息
        String infoLong = StrUtil
                .format("{} {} : 幅度:{} 买卖盘比率:{}", bondBean.getName(), description,
                        (priceEnd - priceStart) / priceStart,
                        (buyVolAll - sellVolAll) / (buyVolAll + sellVolAll)
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
