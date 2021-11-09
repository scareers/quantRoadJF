package com.scareers.formals.kline.basemorphology.usesingleklinebasepercent;

import java.util.Arrays;
import java.util.List;

import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.SettingsOfSingleKlineBasePercent.refreshWindowUsePeriodRelativeSettings;

/**
 * description: 简单的多种周期启动器.  注意周期列表的设置
 *
 * @author: admin
 * @date: 2021/11/10  0010-1:32
 */
public class MultiStartUp {
    public static void main(String[] args) throws Exception {
        List<Integer> windowUsePeriodsCoreArgList = Arrays.asList(
//                7,
//                8,
                9,
                10,
                11
        );

        for (Integer i : windowUsePeriodsCoreArgList) {
            // 注意刷新设定里面与该参数相关的设定
            refreshWindowUsePeriodRelativeSettings(i);
            // 匹配执行主脚本执行
            SingleKlineFormsBase.main0(i);
        }


    }
}
