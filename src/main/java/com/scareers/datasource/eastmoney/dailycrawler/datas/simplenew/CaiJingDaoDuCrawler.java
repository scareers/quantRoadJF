package com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.dailycrawler.datas.SimpleNewCrawler;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.bean.SimpleNewEm;

import java.util.HashSet;
import java.util.List;

/**
 * description: 财经导读 25页 500条抓取: type=1
 *
 * @author: admin
 * @date: 2022/3/12/012-20:07:26
 */
public class CaiJingDaoDuCrawler extends SimpleNewCrawler {
    public static void main(String[] args) {
        CaiJingDaoDuCrawler simpleNewCrawler = new CaiJingDaoDuCrawler();
        Console.log(simpleNewCrawler.isSuccess());
        simpleNewCrawler.run();
        Console.log(simpleNewCrawler.isSuccess());
    }

    @Override
    protected HashSet<SimpleNewEm> initLastTimeFetchSaveBeansExpect500() throws Exception {
        return new HashSet<>(EastMoneyDbApi.getLatestSaveBeanByType(SimpleNewEm.CAI_JING_DAO_DU_TYPE, 520));
    }

    @Override
    protected List<SimpleNewEm> getPage(int page) {
        return EmDataApi.getCaiJingDaoDuNewsPerPage(page);
    }
}
