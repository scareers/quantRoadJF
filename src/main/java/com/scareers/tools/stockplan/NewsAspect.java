package com.scareers.tools.stockplan;

import cn.hutool.core.lang.Console;
import com.scareers.tools.stockplan.bean.FourKeyNew;

/**
 * description: 消息面抓取; 需要给定 当前交易日, 以及前一交易日日期;
 * 将从多个 资讯 来源, 获取数据. 并生成 markdown 文本模板. -- String
 *
 * @author: admin
 * @date: 2022/3/10/010-21:27:18
 */
public class NewsAspect {
    public static void main(String[] args) {
        String dateStr = "3月10日";

        Console.log(FourKeyNew.newInstance(dateStr));

    }


}
