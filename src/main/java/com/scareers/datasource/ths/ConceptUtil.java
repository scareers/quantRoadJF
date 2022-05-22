package com.scareers.datasource.ths;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.*;

/**
 * description: 同花顺概念/行业相关util; 包含少量东财板块
 *
 * @author: admin
 * @date: 2022/5/21/021-15:34:13
 */
public class ConceptUtil {
    public static void main(String[] args) {
//        HashMap<String, List<String>> recentNewConceptMap = calcThsNewConceptRecent(20);
//        Console.log(recentNewConceptMap);

//        Console.log(calcEmNewConceptRecent(5));

        newConceptDiscover(5, 5);

    }


    /**
     * todo: 同花顺新概念发现, 使用个股详情页面的 新概念添加信息. 信息抓取比较复杂
     */
    public static void xx() {
        //        String url = "http://basic.10jqka.com.cn/000001/";
//        cn.hutool.http.HttpRequest request =
//                new HttpRequest(url)
//                        .method(Method.GET)
//                        .timeout(3000)
//                        .header(Header.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng," +
//                                "*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
//                        .header("Accept-Encoding", "gzip, deflate")
//                        .header("Accept-Language", "zh-CN,zh;q=0.9")
//                        .header("Cache-Control", "max-age=0")
//                        .header("Connection", "keep-alive")
//                        .header("Cookie", "__utmz=156575163.1646917474.1.1.utmcsr=yuanchuang.10jqka.com.cn|utmccn=" +
//                                "(referral)|utmcmd=referral|utmcct=/; spversion=20130314; searchGuide=sg; log=; Hm_lvt_78c58f01938e4d85eaf619eae71b4ed1=1651881106,1652788217,1653114788,1653120585; __utma=156575163.717940102.1646917474.1652788220.1653120590.5; __utmc=156575163; cmsad_170_0=0; cmsad_171_0=0; cmsad_172_0=0; Hm_lvt_22a3c65fd214b0d5fd3a923be29458c7=1652788226,1653120600; Hm_lvt_f79b64788a4e377c608617fba4c736e2=1652788277,1653120608; __utmt=1; __utmb=156575163.3.10.1653120590; historystock=000779%7C*%7C600031%7C*%7C000001%7C*%7C603998; Hm_lpvt_78c58f01938e4d85eaf619eae71b4ed1=1653121436; Hm_lpvt_f79b64788a4e377c608617fba4c736e2=1653121436; Hm_lpvt_22a3c65fd214b0d5fd3a923be29458c7=1653121436; " +
//                                "v="+ThsConstants.getNewVCode())
//                        .header("Host", "basic.10jqka.com.cn")
//                        .header("Referer", "http://basic.10jqka.com.cn/000001/operate.html")
//                        .header("Upgrade-Insecure-Requests", "1")
//                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
//                ;
//
//        String body = request.execute().body();
//        Console.log(body);

    }

    public enum ConceptSource { // 新概念来源
        EAST_MONEY, THS
    }

    @Data
    @NoArgsConstructor
    public static class NewConcept {
        public ConceptSource source; // 概念来源,同花顺或者东财
        public String name; // 概念名称, 当新概念出现时, 谨以此作为"id"
        // 成分股代码列表, 懒加载, 需要时请调用方法, 访问 http获取; 同花顺访问问财获取, 可能会比较慢
        public List<String> includeStockCodes;

        public NewConcept(ConceptSource source, String name) {
            this.source = source;
            this.name = name;
        }

        /**
         * @noti 访问网络, 获取成分股代码列表; 会比较慢, 谨慎调用!
         * @noti 存在失败可能, 调用方自行判定成分股代码列表是否初始化成功!
         */
        public void fetchIncludeStockCodesFromWeb() {
            if (source.equals(ConceptSource.EAST_MONEY)) {
                SecurityBeanEm bk = null;
                try {
                    bk = SecurityBeanEm.createBK(name);
                } catch (Exception e) {
                }
                if (bk == null) {
                    log.error("创建东财板块对象失败,无法进一步获取成分股: {}", name);
                    return;
                }
                DataFrame<Object> dataFrame = EmQuoteApi.getBkMembersQuote(bk, 3000, 2);
                includeStockCodes = DataFrameS.getColAsStringList(dataFrame, "资产代码");
            } else if (source.equals(ConceptSource.THS)) {
                DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("所属概念包含白酒概念", WenCaiApi.TypeStr.ASTOCK);
                if (dataFrame != null) {
                    try {
                        includeStockCodes = DataFrameS.getColAsStringList(dataFrame, "code"); // code列是股票简易代码
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } // 可增加
        }

        /*
        批量工厂方法
         */

        /**
         * 批量创建同花顺新概念对象; 成分股均懒加载
         *
         * @param names
         * @return
         */
        public static List<NewConcept> createBatchThs(Collection<String> names) {
            List<NewConcept> res = new ArrayList<>();
            for (String name : names) {
                res.add(new NewConcept(ConceptSource.THS, name));
            }
            return res;
        }

        public static List<NewConcept> createBatchEm(Collection<String> names) {
            List<NewConcept> res = new ArrayList<>();
            for (String name : names) {
                res.add(new NewConcept(ConceptSource.EAST_MONEY, name));
            }
            return res;
        }
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 功能函数: 发现新概念.
     * 将调用同花顺和东财的新概念发现函数
     * 播报或者记录自行发现
     * 两参数分别是同花顺和东财 查询新概念的天数限制, 注意底层有上限20
     */
    public static void newConceptDiscover(int preDayAmountThs, int preDayAmountEm) {
        HashMap<String, List<String>> thsNewConceptMap = calcThsNewConceptRecent(preDayAmountThs);
        // 1.同花顺近期新概念
        HashSet<String> thsNewConceptSet = new HashSet<>();
        thsNewConceptMap.values().forEach(thsNewConceptSet::addAll);
        // 2.东财近期新概念
        HashSet<String> emNewConceptSet = calcEmNewConceptRecent(preDayAmountEm);

        List<NewConcept> batchThs = NewConcept.createBatchThs(thsNewConceptSet);
        List<NewConcept> batchEm = NewConcept.createBatchEm(emNewConceptSet);

        // 成分股抓取
        batchThs.forEach(NewConcept::fetchIncludeStockCodesFromWeb);
        batchEm.forEach(NewConcept::fetchIncludeStockCodesFromWeb);

        Console.log("同花顺近期新概念");
        batchThs.forEach(Console::log);
        Console.log("东财近期新概念");
        batchEm.forEach(Console::log);

    }

    /**
     * 给定日期数量n, 计算 前 n+1个交易日开始, 每天新增的同花顺概念, 包括了小概念,非正式概念等(凡是问财能查到的)
     * 参数为1时, 计算上一交易日, 到今日(假设今日是交易日的话)
     * 返回值key为日期字符串, value为当日新概念列表
     *
     * @param preDayAmount 建议不要太大, 上限15, 否则多次读取数据库比较慢
     * @return
     * @key3 本方法, 历史数据使用爬虫的数据库数据, 而 今日比昨日则使用 问财最新数据的概念列表;
     * 另有爬取"近期重要事件"的方法, 则可保证绝对最新: 爬取网页版的 "公司大事-近期重要事件"
     */
    @SneakyThrows
    public static HashMap<String, List<String>> calcThsNewConceptRecent(int preDayAmount) {
        int days = Math.min(20, preDayAmount);
        String today = DateUtil.today();
        List<String> dateStrList = new ArrayList<>(); // 应当判定的日期列表, 才早到晚, 已经按序
        for (int i = days; i >= 0; i--) { // 已经包含了today
            dateStrList.add(EastMoneyDbApi.getPreNTradeDateStrict(today, i));
        }

        HashMap<String, List<String>> res = new HashMap<>();
        if (dateStrList.size() < 2) {
            return res;
        }

        for (int i = 0; i < dateStrList.size() - 2; i++) {      // -2 是因为不计算 上一交易日和此刻最新, 逻辑后面实现
            HashSet<String> set1 = ThsDbApi.getAllConceptNameByDate(dateStrList.get(i));
            HashSet<String> set2 = ThsDbApi.getAllConceptNameByDate(dateStrList.get(i + 1));
            HashSet<String> newConcepts = CommonUtil.subtractionOfSet(set2, set1);
            if (newConcepts.size() > 0) {
                res.put(dateStrList.get(i + 1), new ArrayList<>(newConcepts));
            }
        }

        // 访问问财最新数据计算, 今日和上一交易日最新概念
        HashSet<String> set1 = ThsDbApi.getAllConceptNameByDate(dateStrList.get(dateStrList.size() - 2)); // 上一
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("所属概念");
        List<String> conceptCol = DataFrameS.getColAsStringList(dataFrame, "所属概念");// 分号分割
        HashSet<String> set2 = new HashSet<>(); // 此刻最新
        for (String s : conceptCol) {
            set2.addAll(StrUtil.split(s, ";"));
        }
        HashSet<String> newConcepts = CommonUtil.subtractionOfSet(set2, set1);
        if (newConcepts.size() > 0) {
            res.put(today, new ArrayList<>(newConcepts));
        }

        return res;
    }

    /**
     * 类似的, 求东财近期新概念, 但具体日期无法精确确定;
     * 给定参数后, 将访问 前n个交易日, 以及 前n+3个交易日,
     * 访问这个日期区间的概念集合, 与当前最新概念集合, 求差集; 上限20
     *
     * @param preDayAmount
     * @return
     */
    @SneakyThrows
    public static HashSet<String> calcEmNewConceptRecent(int preDayAmount) {
        int days = Math.min(20, preDayAmount);
        String today = DateUtil.today();

        String date1 = EastMoneyDbApi.getPreNTradeDateStrict(today, days + 3);
        String date2 = EastMoneyDbApi.getPreNTradeDateStrict(today, days);
        HashSet<String> allBkNameByDate = EastMoneyDbApi.getAllBkNameByDateRange(date1, date2);

        DataFrame<Object> bkQuotes = EmQuoteApi.getRealtimeQuotes(Collections.singletonList("所有板块"));
        return CommonUtil.subtractionOfSet(new HashSet<>(DataFrameS.getColAsStringList(bkQuotes, "资产名称")),
                allBkNameByDate);

    }
}
