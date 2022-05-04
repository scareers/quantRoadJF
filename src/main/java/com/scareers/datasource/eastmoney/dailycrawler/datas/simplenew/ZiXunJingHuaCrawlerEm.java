package com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.dailycrawler.datas.SimpleNewCrawlerEm;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;

import java.util.HashSet;
import java.util.List;

/**
 * description: 资讯精选25页 500条抓取; type=0
 *
 * @author: admin
 * @date: 2022/3/12/012-20:07:26
 */
public class ZiXunJingHuaCrawlerEm extends SimpleNewCrawlerEm {

    public static void main(String[] args) {
        SimpleNewCrawlerEm simpleNewCrawler = new ZiXunJingHuaCrawlerEm();
        Console.log(simpleNewCrawler.isSuccess());
        simpleNewCrawler.run();
        Console.log(simpleNewCrawler.isSuccess());
    }

    @Override
    protected HashSet<SimpleNewEm> initLastTimeFetchSaveBeansExpect500() throws Exception {
        return new HashSet<>(EastMoneyDbApi.getLatestSaveBeanByType(SimpleNewEm.ZI_XUN_JING_HUA_TYPE, 520));
    }

    @Override
    protected List<SimpleNewEm> getPage(int page) {
        return EmDataApi.getZiXunJingHuaPerPage(page);
    }
}
