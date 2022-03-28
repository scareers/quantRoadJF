package com.scareers.tools.stockplan.news.bean;

import lombok.Data;

/**
 * description: 东财4大新闻消息:  上市公司利好一览, 上市公司重大事项公告; 晚间新闻联播, 四大报媒
 * 很明显, 需要设定每种新闻的 标题前缀的日期字符串!
 *
 * @author: admin
 * @date: 2022/3/12/012-10:56:26
 */
@Data
public class FourKeyNew {
    String companyGoodNewsDateStr;
    String companyMajorIssuesDateStr;
    String newsFeedsDateStr;
    String fourPaperNewsDateStr;


    SimpleNewEm companyGoodNews; // 晚间上市公司利好消息一览(附名单)
    SimpleNewEm companyMajorIssues; // 晚间沪深上市公司重大事项公告最新快递
    SimpleNewEm newsFeeds; // 晚间央视新闻联播财经内容集锦
    SimpleNewEm fourPaperNews; // 国内四大证券报纸、重要财经媒体头版头条内容精华摘要

    /**
     * 给定4种新闻标题的 前缀日期字符串. 形如 3月11日
     *
     * @param companyGoodNewsDateStr
     * @param companyMajorIssuesDateStr
     * @param newsFeedsDateStr
     * @param fourPaperNewsDateStr
     */
    private FourKeyNew(String companyGoodNewsDateStr, String companyMajorIssuesDateStr, String newsFeedsDateStr,
                       String fourPaperNewsDateStr) {
        this.companyGoodNewsDateStr = companyGoodNewsDateStr;
        this.companyMajorIssuesDateStr = companyMajorIssuesDateStr;
        this.newsFeedsDateStr = newsFeedsDateStr;
        this.fourPaperNewsDateStr = fourPaperNewsDateStr;

        // 自动构建; 因为底层http api带有2分钟过期缓存, 不会太过缓慢
//        this.companyGoodNews = SimpleNewEmDao.getCompanyGoodNewsOf(companyGoodNewsDateStr);
//        this.companyMajorIssues = SimpleNewEm.getCompanyMajorIssuesOf(companyMajorIssuesDateStr);
//        this.newsFeeds = SimpleNewEmDao.getNewsFeedsOf(newsFeedsDateStr);
//        this.fourPaperNews = SimpleNewEmDao.getFourPaperNewsOf(fourPaperNewsDateStr);
    }

    public static FourKeyNew newInstance(String companyGoodNewsDateStr,
                                         String companyMajorIssuesDateStr,
                                         String newsFeedsDateStr,
                                         String fourPaperNewsDateStr) {
        return new FourKeyNew(companyGoodNewsDateStr, companyMajorIssuesDateStr, newsFeedsDateStr,
                fourPaperNewsDateStr);
    }

    public static FourKeyNew newInstance(String oneDay) {
        return new FourKeyNew(oneDay, oneDay, oneDay, oneDay);
    }


}
