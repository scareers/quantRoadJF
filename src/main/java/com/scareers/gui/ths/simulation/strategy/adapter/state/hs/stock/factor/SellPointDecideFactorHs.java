package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.factor;

import cn.hutool.core.date.DateUtil;
import com.scareers.gui.ths.simulation.strategy.adapter.factor.HsFactor;
import com.scareers.gui.ths.simulation.strategy.adapter.state.HsState;
import com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.StockStateHs;
import com.scareers.gui.ths.simulation.trader.SettingsOfTrader;
import com.scareers.gui.ths.simulation.trader.Trader;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.util.List;

import static com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.factor.SettingsOfSellPointFactor.highSellBeforehandThresholdMap;


/**
 * description: 高卖时, 卖点判定的因子算法. 将读取各种状态, 判定是否卖点, 设置state的 sellPointCurrent 属性
 *
 * @author: admin
 * @date: 2022/2/21/021-19:24:10
 */
public class SellPointDecideFactorHs extends HsFactor {
    public SellPointDecideFactorHs() {
        super(SettingsOfSellPointFactor.factorNameHs, SettingsOfSellPointFactor.nameCnHs,
                SettingsOfSellPointFactor.descriptionHs);
    }

    @Override
    public HsState influence(HsState state) {
        state.getStockStateHs().setSellPointCurrent(isSellPoint(state));
        return state;
    }


    /**
     * 卖点判定机制.
     * 1.集合竞价 9:25 后才可能出现卖点
     * 2.集合竞价专属卖点! 唯一. 将于 9:25:xx (可能)产生, 该类订单 将在 otherRawMessages 属性中, 添加 afterAuctionFirst=True
     * 注意对应的 sellCheck 逻辑也应当判定此类订单, 其等待成交的时长不应是固定1分钟, 而是 持续到例如 9:31为止!
     * 见Order新增 isAfterAuctionFirst() 方法
     * 3. 9:30-9:31之间, 连续上升条件视为达成, 且上个分时close, 取值为 9:31的open值, 其余逻辑同普通情况
     * 4.一般情况:
     * 判定前几分钟分时图 连续上升 n分钟, >=阈值
     * 判定本分钟价格必须 < 上一分钟分时close.
     * 取当前秒数, 若为0-9s, 返回false,不可能下单.
     * 10-20s, 需要 上升成交记录数 /(上升+下降) >= 1.0; 时间越长, 该百分比限制越不严格, 直到 50-59s, 直接返回true.
     * 见 SettingsOfSellPointFactor.highSellBeforehandThresholdMap 静态属性, 作为设置项, 逻辑上final
     *
     * @return
     * @see SettingsOfFSBacktest
     */
    public boolean isSellPoint(HsState state0) {
        StockStateHs state = state0.getStockStateHs();
        // 1.x: sell订单,单股票互斥: 在等待队列和正在执行查找所有sell订单, 判定其 stockCode参数是否为本stock,若存在则互斥跳过
        if (Trader.getInstance().getOrderExecutor().executingSellOrderOf(state.getStockCode()) || Trader.getInstance()
                .getOrderExecutor()
                .hasSellOrderWaitExecuteOf(state.getStockCode())) {
            // log.warn("Mutual Sell Order: 卖单互斥: {}", stock);
            return false;
        }

        // 获取今日分时图
        // 2022-01-20 11:30	17.24	17.22	17.24	17.21	10069	17340238.00 	0.17	-0.12	-0.02	0.01	000001	平安银行
        // 数据池获取分时图, 因 9:25:xx 后将有 9:31 单条记录. 因此lenth<0时, 直接返回false
        DataFrame<Object> fsCurrent = state.getFsData();
        if (fsCurrent.length() == 0) {
            return false; // 9:25:0x 之前  // 一般是 9:25:02左右
        }

        String nowTime = DateUtil.date().toString(SettingsOfTrader.TIME_FT_NORM);
        if (nowTime.compareTo("09:25:00") <= 0) {
            return false; // 集合竞价之前
        }
        //System.out.println(nowTime); // 09:28:10
        if (nowTime.compareTo("09:25:00") > 0 && nowTime.compareTo("09:30:00") < 0) {
            // 集合竞价结束后的五分钟, 应当 集合竞价处理, 绝对卖点
            return true; // 固定返回 true, 将整个5分钟均视为卖点, 但因相同股票卖单互斥, 因此不会重复下单.
        }

        // 此时已经 9:30:0x 开盘
        final String nowStr = DateUtil.date().toString(SettingsOfTrader.DT_FT_WITHOUT_SECONDS); // 2022-01-20 11:30

        DataFrame<Object> fsDf = dropAfter1Fs(fsCurrent, nowStr);
        // 对 fsDf进行筛选, 筛选 不包含本分钟的. 因底层api会生成最新那一分钟的. 即 13:34:31, 分时图已包含 13:35, 我们需要 13:34及以前
        // 将最后1行 fs记录去掉

        // 计算连续上涨数量
        int continuousRaise = 0;
        List<Double> closes = DataFrameS.getColAsDoubleList(fsDf, "收盘");
        if (nowTime.compareTo("09:31:00") <= 0) {
            continuousRaise = Integer.MAX_VALUE; // 9:30:xx 只会有 9:31 的fs数据, 第一条fs图,此前视为连续上升.
        } else {
            for (int i = closes.size() - 1; i >= 1; i--) {
                if (closes.get(i) >= closes.get(i - 1)) {
                    continuousRaise++;
                } else {
                    break;
                }
            }
        }
        if (continuousRaise < SettingsOfSellPointFactor.continuousRaiseTickCountThreshold) { // 连续上升必须>=阈值
            return false;
        }

        double lastFsClose;
        if (nowTime.compareTo("09:31:00") <= 0) {
            // 9:30:xx
            List<Double> temp = DataFrameS.getColAsDoubleList(fsDf, "开盘"); // 此时未筛选 9:31
            lastFsClose = temp.get(temp.size() - 1); // 使用 第一条分时图的开盘, 视为 9:30 那一刻的收盘!
        } else {
            lastFsClose = closes.get(closes.size() - 1); // 作为 计算最新一分钟 价格的基准, 计算涨跌
        }

        // 0	000010    	0          	09:15:09 	3.8  	177 	4
        /*
            sec_code,market,time_tick,price,vol,bs
         */
        DataFrame<Object> fsTransDf = state.getFsTransData();

        String tickWithSecond0 = nowTime.substring(0, 5) + ":00"; // 本分钟.开始时刻
        // 筛选fs图最近一分钟所有记录,
        DataFrame<Object> fsLastMinute = getCurrentMinuteAll(fsTransDf, tickWithSecond0);

        // 获取最新一分钟所有 成交记录. 价格列
        if (fsLastMinute.size() <= 0) { // 未能获取到最新一分钟数据,返回false
            return false;
        }
        List<Double> pricesLastMinute = DataFrameS.getColAsDoubleList(fsLastMinute, 3);
        if (pricesLastMinute.size() <= 0) {
            return false;
        }
        if (pricesLastMinute.get(pricesLastMinute.size() - 1) >= lastFsClose) {
            return false; // 最新价格必须 < 上一分时收盘, 否则无视.
        }


        if (state.getChgPToPre2Close() < SettingsOfSellPointFactor.execHighSellThreshold) {
            return false; // 价格必须足够高, 才可能卖出
        }


        // 计算对比  lastFsClose, 多少上升, 多少下降? // 此时已经确定最新价格更低了
        int countOfLower = 0; // 最新一分钟, 价格比上一分钟收盘更低 的数量
        int countOfHigher = 0; // 最新一分钟, 价格比上一分钟收盘更低 的数量
        for (Double price : pricesLastMinute) {
            if (price < lastFsClose) {
                countOfLower++;
            } else if (price > lastFsClose) {
                countOfHigher++;
            }
        }
        if (countOfLower + countOfHigher == 0) {// 本分钟价格一点没变
            return false;
        }
        int currentSecond = Integer.parseInt(nowTime.substring(6, 8)); // 秒数
        if (currentSecond < 10) {
            return false; // 0-9s, 不会出现卖点
        } else if (currentSecond < 20) { // 前10s
            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(20);
        } else if (currentSecond < 30) { // 前10s
            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(30);
        } else if (currentSecond < 40) { // 前10s
            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(40);
        } else if (currentSecond < 50) { // 前10s
            return ((double) countOfLower) / (countOfHigher + countOfLower) >= highSellBeforehandThresholdMap.get(50);
        } else {
            return true;
        }
    }

    /**
     * 因东方财富 securitylist 获取, xx:01s 就将获取到 xx+1 的分时图, 因此需要去掉最新一分钟的分时图.
     * 例如在  14:32:32, 去掉 14:33,保留到14:32, 这里不使用slice[0,-1], 而倒序遍历判定. 若使用 select 将性能瓶颈
     *
     * @param fsDf
     * @return
     */
    private DataFrame<Object> dropAfter1Fs(DataFrame<Object> fsDf, String nowStr) {
        if (fsDf.length() == 1) { // 9:30:0x 期间仅自身.返回值不被使用,会有其他处理方式
            return fsDf;
        }
        int endRow = fsDf.length();
        for (int i = fsDf.length() - 1; i >= 0; i--) {
            if (fsDf.row(i).get(0).toString().compareTo(nowStr) <= 0) {
                endRow = i; // 第一个<=的, 将>的全部排除. 比select快
                break;
            }
        }
        return fsDf.slice(0, endRow + 1);
    }

    private DataFrame<Object> getCurrentMinuteAll(DataFrame<Object> fsTransDf, String tickWithSecond0) {
        int rowStart = 0;
        for (int i = fsTransDf.length() - 1; i >= 0; i--) {
            if (fsTransDf.row(i).get(2).toString().compareTo(tickWithSecond0) < 0) {// 找到第一个小
                rowStart = i + 1;
                break;
            }
        }
        return fsTransDf.slice(rowStart, fsTransDf.length());
    }
}
