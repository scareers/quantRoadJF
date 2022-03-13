package com.scareers.datasource.eastmoney.dailycrawler;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.dailycrawler.commons.BkList;
import com.scareers.datasource.eastmoney.dailycrawler.commons.IndexList;
import com.scareers.datasource.eastmoney.dailycrawler.commons.StockList;
import com.scareers.datasource.eastmoney.dailycrawler.commons.TradeDates;
import com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew.CaiJingDaoDuCrawler;
import com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew.ZiXunJingHuaCrawler;
import com.scareers.datasource.eastmoney.dailycrawler.quotes.Fs1MData;
import com.scareers.datasource.eastmoney.dailycrawler.quotes.FsTransData;
import com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline.DailyKlineDataOfBk;
import com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline.DailyKlineDataOfIndex;
import com.scareers.datasource.eastmoney.dailycrawler.quotes.dailykline.DailyKlineDataOfStock;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * description: 爬取逻辑主流程类
 *
 * @author: admin
 * @date: 2022/3/5/005-09:21:58
 */
public class CrawlerChain {
    public static void main(String[] args) {
        boolean fullMode = true;

        CrawlerChain crawlerChain = new CrawlerChain(1, 1);
//        crawlerChain.addSynCrawler(new StockList());
//        crawlerChain.addSynCrawler(new IndexList());
//        crawlerChain.addSynCrawler(new BkList());
//        crawlerChain.addSynCrawler(new TradeDates());

        crawlerChain.addFrontCrawlers(new DailyKlineDataOfStock("nofq", fullMode));
        crawlerChain.addFrontCrawlers(new DailyKlineDataOfStock("hfq", fullMode));
        crawlerChain.addFrontCrawlers(new DailyKlineDataOfStock("qfq", fullMode));
        crawlerChain.addFrontCrawlers(new DailyKlineDataOfBk(fullMode));
        crawlerChain.addFrontCrawlers(new DailyKlineDataOfIndex(fullMode));

        crawlerChain.addFrontCrawlers(new Fs1MData());
        crawlerChain.addFrontCrawlers(new FsTransData());

        // 资讯
        crawlerChain.addRearCrawlers(new CaiJingDaoDuCrawler());
        crawlerChain.addFrontCrawlers(new ZiXunJingHuaCrawler());


        crawlerChain.run();

    }

    List<Crawler> synCrawlers = new ArrayList<>(); // 同步爬虫, 按序run, 一般为 最基本数据. 后续爬虫会使用它们
    List<Crawler> frontCrawlers = new ArrayList<>(); // 前置爬虫, 多线程执行, 一些优先级偏前的爬虫, 可能被依赖
    List<Crawler> rearCrawlers = new ArrayList<>(); // 后置爬虫, 常规数据项
    ExecutorService esFront;
    ExecutorService esRear;


    public CrawlerChain(
            int coreSizeOfFrontCrawlersThreadPool,
            int coreSizeOfRearCrawlersThreadPool) {
        this.esFront = ThreadUtil
                .newExecutor(coreSizeOfFrontCrawlersThreadPool, 2 * coreSizeOfFrontCrawlersThreadPool,
                        Integer.MAX_VALUE);
        this.esRear = ThreadUtil
                .newExecutor(coreSizeOfRearCrawlersThreadPool, 2 * coreSizeOfRearCrawlersThreadPool,
                        Integer.MAX_VALUE);
    }

    public void addSynCrawler(Crawler crawler) {
        synCrawlers.add(crawler);
    }

    public void addFrontCrawlers(Crawler crawler) {
        frontCrawlers.add(crawler);
    }

    public void addRearCrawlers(Crawler crawler) {
        rearCrawlers.add(crawler);
    }

    private static final Log log = LogUtil.getLogger();

    CopyOnWriteArrayList<Crawler> successCrawlers = new CopyOnWriteArrayList<>(); // 执行成功
    CopyOnWriteArrayList<Crawler> failCrawlers = new CopyOnWriteArrayList<>(); // 第一次执行失败的爬虫
    CopyOnWriteArrayList<Crawler> failFinallyCrawlers = new CopyOnWriteArrayList<>(); // 重试后依然失败的爬虫

    public void run() {
        log.error("show: 开始执行同步爬虫");
        for (Crawler synCrawler : synCrawlers) {
            synCrawler.run();
        }

        log.error("show: 开始执行前置爬虫");
        for (Crawler frontCrawler : frontCrawlers) {
            esFront.submit(() -> {
                frontCrawler.run();
                if (frontCrawler.isSuccess()) {
                    successCrawlers.add(frontCrawler);
                } else {
                    failCrawlers.add(frontCrawler);
                }
            });
        }
        waitPoolFinish(esFront);
        log.error("失败的前置爬虫数量: {}", failCrawlers.size());
        log.error("show: 开始执行后置爬虫");
        for (Crawler rearCrawler : rearCrawlers) {
            esRear.submit(() -> {
                rearCrawler.run();
                if (rearCrawler.isSuccess()) {
                    successCrawlers.add(rearCrawler);
                } else {
                    failCrawlers.add(rearCrawler);
                }
            });
        }
        waitPoolFinish(esRear);
        log.error("失败总爬虫数量: {}", failCrawlers.size());
        log.error("show: 尝试再次执行失败爬虫");

        for (Crawler failCrawler : failCrawlers) {
            esRear.submit(() -> {
                failCrawler.run();
                if (failCrawler.isSuccess()) {
                    successCrawlers.add(failCrawler);
                } else {
                    failFinallyCrawlers.add(failCrawler);
                }
            });
        }
        waitPoolFinish(esRear);

        log.error("show: 执行状况:\n 成功的爬虫:数量: {}\n{}" +
                        "\n首次失败的爬虫数量: {}\n{}\n重试后最终失败的爬虫数量: {}\n{}", successCrawlers.size(), successCrawlers,
                failCrawlers.size(), failCrawlers, failFinallyCrawlers.size(), failFinallyCrawlers);

    }

    public static void waitPoolFinish(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
