package com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.dailycrawler.datas.SimpleNewCrawlerEm;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;

import java.util.HashSet;
import java.util.List;

/**
 * description: 财经导读 25页 500条抓取: type=1
 *
 * @author: admin
 * @date: 2022/3/12/012-20:07:26
 */
public class CaiJingDaoDuCrawlerEm extends SimpleNewCrawlerEm {
    public static void main(String[] args) {
        CaiJingDaoDuCrawlerEm simpleNewCrawler = new CaiJingDaoDuCrawlerEm();
        Console.log(simpleNewCrawler.isSuccess());
        simpleNewCrawler.run();
        Console.log(simpleNewCrawler.isSuccess());
    }

    @Override
    protected HashSet<SimpleNewEm> initLastTimeFetchSaveBeansExpect500() throws Exception {
        return new HashSet<>(EastMoneyDbApi.getLatestSaveBeanByType(SimpleNewEm.CAI_JING_DAO_DU_TYPE, 1024));
    }

    @Override
    protected List<SimpleNewEm> getPage(int page) {
        return EmDataApi.getCaiJingDaoDuNewsPerPage(page);
    }
}
