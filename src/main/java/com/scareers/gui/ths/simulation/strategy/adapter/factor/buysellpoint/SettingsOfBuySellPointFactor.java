package com.scareers.gui.ths.simulation.strategy.adapter.factor.buysellpoint;

import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtil;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/2/21/021-19:25:16
 */
public class SettingsOfBuySellPointFactor {
    private static final Log log = LogUtil.getLogger();
    public static final String factorNameHs = "SellPointDecideFactor";
    public static final String nameCnHs = "卖点判定因子";
    public static final String descriptionHs = "综合state当前数据, 判定是否为卖点? 设置state的sellPointCurrent属性";

}
