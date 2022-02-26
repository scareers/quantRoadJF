package com.scareers.gui.ths.simulation.strategy.adapter.state.hs.stock.factor;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;

import java.util.concurrent.ConcurrentHashMap;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/21/021-19:25:16
 */
public class SettingsOfSellPointFactor {
    private static final Log log = LogUtil.getLogger();
    public static final String factorNameHs = "SellPointDecideFactor";
    public static final String nameCnHs = "卖点判定因子";
    public static final String descriptionHs = "综合state当前数据, 判定是否为卖点? 设置state的sellPointCurrent属性";

    static {
        initHighSellBeforehandThresholdMap();
    }

    /*
    高卖相关参数
     */
    public static ConcurrentHashMap<Integer, Double> highSellBeforehandThresholdMap; // 判定本分钟为下降的阈值map
    public static int continuousRaiseTickCountThreshold = 1; // 连续上升n分钟,本分钟下降,才是卖点
    public static double execHighSellThreshold = -0.02; // 价格>=此值(百分比)才考虑卖出, 同样是相对于前2收盘价而言

    private static void initHighSellBeforehandThresholdMap() {
        highSellBeforehandThresholdMap = new ConcurrentHashMap<>();
        // 注意, key 控制 当前秒数<key, 不包含
        highSellBeforehandThresholdMap.put(10, 2.0); // 必须 价格降低次数 /(价格升高+降低) >= 此百分比, 显然2.0意味着不可能
        highSellBeforehandThresholdMap.put(20, 1.0);
        highSellBeforehandThresholdMap.put(30, 0.9);
        highSellBeforehandThresholdMap.put(40, 0.7);
        highSellBeforehandThresholdMap.put(50, 0.6);
        highSellBeforehandThresholdMap.put(60, 0.0); // 最后10s视为绝对符合条件. 10和60并未用到
    }
}
