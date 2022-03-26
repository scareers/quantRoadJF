package com.scareers.gui.ths.simulation.trader;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 可转债套利, tts提示
 * 调用同花顺问财api, 读取具有一定热度 且含有可转债的股票, (才可能具有关注度,流通性)
 * 1.读取过去几天正股与对应可转债涨跌幅 差值
 * 2.实时获取 正股涨跌幅, 可转债涨跌幅, 当两者差距过大, TTS播报提示!
 *
 * @author: admin
 * @date: 2022/3/22/022-17:10:35
 */
public class ConvertibleBondArbitrage {

    // 记录保存每只股票, 最后一次提示时的涨跌幅差距;
    // 当新的涨跌幅 变得更大, 或者变得更小, 会提示 扩大到/缩小到 xxx
    // 变化阈值 百分之0.5
    public static HashMap<String, Double> lastNotifiMap = new HashMap<>();
    public static double changeThreshold = 0.5;
    public static double notiThreshold = 1; // 百分比差距大于此值, 才进行播报

    public static void main(String[] args) {
        main0();
    }


    public static void main0() {
        boolean first = true;
        while (true) {
            // 1. 获取正股-转债 列表
            if (first) {
                log.info("解析热点含债股票");
            }
            List<StockBondBean> hotStockWithBondList = getHotStockWithBond();
            // 2.查询加入缓存, 以便单独访问
            if (first) {
                log.info("解析股票/转债bean");
            }
            List<SecurityBeanEm> stockList = null;
            List<SecurityBeanEm> bondList = null;
            try {
                stockList = SecurityBeanEm.createStockList(
                        hotStockWithBondList.stream().map(StockBondBean::getStockCode).collect(Collectors.toList()));
                bondList = SecurityBeanEm.createBondList(
                        hotStockWithBondList.stream().map(StockBondBean::getBondCode).collect(Collectors.toList()),
                        true);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // 3.批量访问 转债列表行情 / 股票列表行情, 得到 两个 df;
            if (first) {
                log.info("批量获取股票/转债行情");
            }
            DataFrame<Object> stockQuoteDf = EmQuoteApi.getLatestQuoteOfBeanList(stockList);
            DataFrame<Object> bondQuoteDf = EmQuoteApi.getLatestQuoteOfBeanList(bondList);

            // 4.将Df 转换为 Map, 以便方便访问
            // map: 资产名称:涨跌幅
            HashMap<String, Double> stockQuotes = buildChgPMap(stockQuoteDf);
            HashMap<String, Double> bondQuotes = buildChgPMap(bondQuoteDf);

            // 5.遍历原 股债对, 检测涨跌幅差距
            if (first) {
                log.info("检测涨跌幅差距,获取套利机会");
            }
            // 测试缩小放大
            // hotStockWithBondList.forEach(value -> lastNotifiMap.put(value.getStockCode(), 10.0));

            for (StockBondBean stockBondBean : hotStockWithBondList) {
                Double chgPOfStock = stockQuotes.get(stockBondBean.getStockCode());
                Double chgPOfBond = bondQuotes.get(stockBondBean.getBondCode());


                if (chgPOfBond == null || chgPOfStock == null) {
                    log.error("正股-可转债对 涨跌幅获取失败:{} 正股涨跌幅:{} ; 可转债涨跌幅:{}", stockBondBean.getStockName(), chgPOfStock,
                            chgPOfBond);
                    continue;
                }

                if (chgPOfStock - chgPOfBond > notiThreshold) { // 行情就是百分比, 不是 0.01
                    double newDiff = chgPOfStock - chgPOfBond;
                    stockBondBean.setCurrentDiff(CommonUtil.roundHalfUP(newDiff, 2));
                    //log.info(stockBondBean.toString());
                    Double oldDiff = lastNotifiMap.get(stockBondBean.getStockCode());

                    if (oldDiff == null) {
                        ManiLog.put(stockBondBean.toString());
                        Tts.playSound(
                                StrUtil.format("{}:百分之{}", stockBondBean.getStockName(),
                                        CommonUtil.roundHalfUP(chgPOfStock - chgPOfBond, 2)),
                                false);
                        lastNotifiMap.put(stockBondBean.getStockCode(), newDiff);
                        StockBondBean.saveBean(stockBondBean);
                    } else {
                        if (newDiff - oldDiff > changeThreshold) {
                            ManiLog.put(stockBondBean.toString());
                            Tts.playSound(
                                    StrUtil.format("{}扩大到:百分之{}", stockBondBean.getStockName(),
                                            CommonUtil.roundHalfUP(chgPOfStock - chgPOfBond, 2)),
                                    false);
                            lastNotifiMap.put(stockBondBean.getStockCode(), newDiff);
                            StockBondBean.saveBean(stockBondBean);
                        } else if (oldDiff - newDiff > changeThreshold) {
                            ManiLog.put(stockBondBean.toString());
                            Tts.playSound(
                                    StrUtil.format("{}缩小到:百分之{}", stockBondBean.getStockName(),
                                            CommonUtil.roundHalfUP(chgPOfStock - chgPOfBond, 2)),
                                    false);
                            lastNotifiMap.put(stockBondBean.getStockCode(), newDiff);
                            StockBondBean.saveBean(stockBondBean);
                        }
                        // 差距<0.5, 不提示

                    }
                }
            }
            first = false;
            // Console.log(lastNotifiMap);
            ThreadUtil.sleep(1000);
        }


    }

    private static final Log log = LogUtil.getLogger();

    /*
     * 资产代码	 资产名称	涨跌幅	最新价	最高 最低	 今开	 涨跌额	         换手率	          量比	          动态市盈率
     * 成交量	                 成交额	          昨日收盘	           总市值	          流通市值	市场编号	    行情id	市场类型
     */
    public static HashMap<String, Double> buildChgPMap(DataFrame<Object> quoteDf) {
        HashMap<String, Double> res = new HashMap<>();
        for (int i = 0; i < quoteDf.length(); i++) {
            String key = quoteDf.get(i, "资产代码").toString();
            try {
                res.put(key, Double.valueOf(quoteDf.get(i, "涨跌幅").toString()));
            } catch (Exception e) {
                // pass
            }
        }
        return res;
    }


    public static LRUCache<String, List<StockBondBean>> hotStockWithBondCache = CacheUtil.newLRUCache(1, 60 * 1000);

    /**
     * [未清偿可转债简称[20220321], code, 未清偿可转债转股价格[20220321], 个股热度排名[20220322], 股票简称, 个股热度排名排名[20220322], 最新价, 最新涨跌幅, 未清偿可转债代码[20220321], market_code, 股票代码]
     *
     * @param sourceCols
     * @return
     */
    public static Map<Object, Object> buildRenameMap(Set<Object> sourceCols) {
        HashMap<Object, Object> res = new HashMap<>();
        for (Object sourceCol : sourceCols) {
            if (sourceCol.toString().startsWith("未清偿可转债简称")) {
                res.put(sourceCol, "可转债简称");
            } else if (sourceCol.toString().startsWith("个股热度排名[")) {
                res.put(sourceCol, "个股热度排名");
            } else if (sourceCol.toString().startsWith("未清偿可转债代码")) {
                res.put(sourceCol, "可转债代码");
            }
        }

        return res;
    }


    public static List<StockBondBean> getHotStockWithBond() {
        if (hotStockWithBondCache.get("cache") != null) {
            return hotStockWithBondCache.get("cache");
        }
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("个股热度排名从小到大排名;个股热度排名<500;含有可转债");
        dataFrame = dataFrame.rename(buildRenameMap(dataFrame.columns()));
        List<StockBondBean> stockBondBeanList = new ArrayList<>();

        for (int i = 0; i < dataFrame.length(); i++) {
            Integer hot = 0;
            try {
                hot = Integer.valueOf(dataFrame.get(i, "个股热度排名").toString());
            } catch (Exception e) {

            }
            stockBondBeanList.add(new StockBondBean(
                    dataFrame.get(i, "股票简称").toString(),
                    dataFrame.get(i, "code").toString(),
                    dataFrame.get(i, "可转债简称").toString(),
                    dataFrame.get(i, "可转债代码").toString(),
                    hot

            ));
        }
        hotStockWithBondCache.put("cache", stockBondBeanList);
        return stockBondBeanList;
    }


}
