package com.scareers.datasource.eastmoney.dailycrawler;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew.*;
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
//        crawlerChain.addSynCrawler(new StockListEm());
//        crawlerChain.addSynCrawler(new IndexListEm());
//        crawlerChain.addSynCrawler(new BkListEm());
//        crawlerChain.addSynCrawler(new TradeDatesEm());

//        crawlerChain.addFrontCrawlers(new DailyKlineDataEmOfStock("nofq", fullMode));
//        crawlerChain.addFrontCrawlers(new DailyKlineDataEmOfStock("hfq", fullMode));
//        crawlerChain.addFrontCrawlers(new DailyKlineDataEmOfStock("qfq", fullMode));
//        crawlerChain.addFrontCrawlers(new DailyKlineDataEmOfBk(fullMode));
//        crawlerChain.addFrontCrawlers(new DailyKlineDataEmOfIndex(fullMode));
//
//        crawlerChain.addFrontCrawlers(new Fs1MDataEm());
//        crawlerChain.addFrontCrawlers(new FsTransDataEm());

        // 资讯
        crawlerChain.addRearCrawlers(new CaiJingDaoDuCrawlerEm());
        crawlerChain.addFrontCrawlers(new ZiXunJingHuaCrawlerEm());
        crawlerChain.addFrontCrawlers(new CompanyMajorIssuesCrawlerEm()); // 最近今天公司重大事件
        crawlerChain.addFrontCrawlers(new CompanyMajorIssuesCrawlerEm()); // 最近今天公司利好消息
        crawlerChain.addFrontCrawlers(new NewsFeedsCrawlerEm()); // 新闻联播集锦
        crawlerChain.addFrontCrawlers(new FourPaperNewsCrawlerEm()); // 四大报媒精华


        crawlerChain.run();

    }

    List<CrawlerEm> synCrawlerEms = new ArrayList<>(); // 同步爬虫, 按序run, 一般为 最基本数据. 后续爬虫会使用它们
    List<CrawlerEm> frontCrawlerEms = new ArrayList<>(); // 前置爬虫, 多线程执行, 一些优先级偏前的爬虫, 可能被依赖
    List<CrawlerEm> rearCrawlerEms = new ArrayList<>(); // 后置爬虫, 常规数据项
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

    public void addSynCrawler(CrawlerEm crawlerEm) {
        synCrawlerEms.add(crawlerEm);
    }

    public void addFrontCrawlers(CrawlerEm crawlerEm) {
        frontCrawlerEms.add(crawlerEm);
    }

    public void addRearCrawlers(CrawlerEm crawlerEm) {
        rearCrawlerEms.add(crawlerEm);
    }

    private static final Log log = LogUtil.getLogger();

    CopyOnWriteArrayList<CrawlerEm> successCrawlerEms = new CopyOnWriteArrayList<>(); // 执行成功
    CopyOnWriteArrayList<CrawlerEm> failCrawlerEms = new CopyOnWriteArrayList<>(); // 第一次执行失败的爬虫
    CopyOnWriteArrayList<CrawlerEm> failFinallyCrawlerEms = new CopyOnWriteArrayList<>(); // 重试后依然失败的爬虫

    public void run() {
        log.error("show: 开始执行同步爬虫");
        for (CrawlerEm synCrawlerEm : synCrawlerEms) {
            synCrawlerEm.run();
        }

        log.error("show: 开始执行前置爬虫");
        for (CrawlerEm frontCrawlerEm : frontCrawlerEms) {
            esFront.submit(() -> {
                frontCrawlerEm.run();
                if (frontCrawlerEm.isSuccess()) {
                    successCrawlerEms.add(frontCrawlerEm);
                } else {
                    failCrawlerEms.add(frontCrawlerEm);
                }
            });
        }
        waitPoolFinish(esFront);
        log.error("失败的前置爬虫数量: {}", failCrawlerEms.size());
        log.error("show: 开始执行后置爬虫");
        for (CrawlerEm rearCrawlerEm : rearCrawlerEms) {
            esRear.submit(() -> {
                rearCrawlerEm.run();
                if (rearCrawlerEm.isSuccess()) {
                    successCrawlerEms.add(rearCrawlerEm);
                } else {
                    failCrawlerEms.add(rearCrawlerEm);
                }
            });
        }
        waitPoolFinish(esRear);
        log.error("失败总爬虫数量: {}", failCrawlerEms.size());
        log.error("show: 尝试再次执行失败爬虫");

        for (CrawlerEm failCrawlerEm : failCrawlerEms) {
            esRear.submit(() -> {
                failCrawlerEm.run();
                if (failCrawlerEm.isSuccess()) {
                    successCrawlerEms.add(failCrawlerEm);
                } else {
                    failFinallyCrawlerEms.add(failCrawlerEm);
                }
            });
        }
        waitPoolFinish(esRear);

        log.error("show: 执行状况:\n 成功的爬虫:数量: {}\n{}" +
                        "\n首次失败的爬虫数量: {}\n{}\n重试后最终失败的爬虫数量: {}\n{}", successCrawlerEms.size(), successCrawlerEms,
                failCrawlerEms.size(), failCrawlerEms, failFinallyCrawlerEms.size(), failFinallyCrawlerEms);

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
