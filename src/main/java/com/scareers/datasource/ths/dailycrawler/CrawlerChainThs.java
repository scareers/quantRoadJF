package com.scareers.datasource.ths.dailycrawler;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.ths.dailycrawler.commons.*;
import com.scareers.utils.log.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * description: 同花顺爬虫链. 逻辑同东财, 爬虫类型不同而已
 *
 * @author: admin
 * @date: 2022/3/5/005-09:21:58
 */
public class CrawlerChainThs {
    public static void main(String[] args) {
        boolean forceUpdate = true;

        CrawlerChainThs crawlerChainThs = new CrawlerChainThs(4, 4);
        crawlerChainThs.addSynCrawler(new ConceptListThs(forceUpdate));
        crawlerChainThs.addSynCrawler(new IndustryListThs(forceUpdate));
        crawlerChainThs.addSynCrawler(new StockBelongToThs(forceUpdate));
        crawlerChainThs.addSynCrawler(new ConceptAndIndustryIncludeStockAndRelationParseThs(forceUpdate));
        crawlerChainThs.addSynCrawler(new StockListThs(forceUpdate));


        crawlerChainThs.run();

    }

    List<CrawlerThs> synCrawlerEms = new ArrayList<>(); // 同步爬虫, 按序run, 一般为 最基本数据. 后续爬虫会使用它们
    List<CrawlerThs> frontCrawlerEms = new ArrayList<>(); // 前置爬虫, 多线程执行, 一些优先级偏前的爬虫, 可能被依赖
    List<CrawlerThs> rearCrawlerEms = new ArrayList<>(); // 后置爬虫, 常规数据项
    ExecutorService esFront;
    ExecutorService esRear;


    public CrawlerChainThs(
            int coreSizeOfFrontCrawlersThreadPool,
            int coreSizeOfRearCrawlersThreadPool) {
        this.esFront = ThreadUtil
                .newExecutor(coreSizeOfFrontCrawlersThreadPool, 2 * coreSizeOfFrontCrawlersThreadPool,
                        Integer.MAX_VALUE);
        this.esRear = ThreadUtil
                .newExecutor(coreSizeOfRearCrawlersThreadPool, 2 * coreSizeOfRearCrawlersThreadPool,
                        Integer.MAX_VALUE);
    }

    public void addSynCrawler(CrawlerThs crawlerEm) {
        synCrawlerEms.add(crawlerEm);
    }

    public void addFrontCrawlers(CrawlerThs crawlerEm) {
        frontCrawlerEms.add(crawlerEm);
    }

    public void addRearCrawlers(CrawlerThs crawlerEm) {
        rearCrawlerEms.add(crawlerEm);
    }

    private static final Log log = LogUtil.getLogger();

    CopyOnWriteArrayList<CrawlerThs> successCrawlerEms = new CopyOnWriteArrayList<>(); // 执行成功
    CopyOnWriteArrayList<CrawlerThs> failCrawlerEms = new CopyOnWriteArrayList<>(); // 第一次执行失败的爬虫
    CopyOnWriteArrayList<CrawlerThs> failFinallyCrawlerEms = new CopyOnWriteArrayList<>(); // 重试后依然失败的爬虫

    public void run() {
        log.error("show: 开始执行同步爬虫");
        for (CrawlerThs synCrawlerEm : synCrawlerEms) {
            synCrawlerEm.run();
        }

        log.error("show: 开始执行前置爬虫");
        for (CrawlerThs frontCrawlerEm : frontCrawlerEms) {
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
        for (CrawlerThs rearCrawlerEm : rearCrawlerEms) {
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

        for (CrawlerThs failCrawlerEm : failCrawlerEms) {
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
