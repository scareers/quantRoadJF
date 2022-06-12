package com.scareers.datasource.eastmoney.quotecenter;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.gui.ths.simulation.interact.gui.notify.EmPcNewsNotify;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/6/12/012-19:26:55
 */
public class EmNewsApi {
    public static String EM_PC_YWJH_URL = "https://eminfo.eastmoney.com/pc_news/News/GetNewsList"; // 东财pc要闻精华
    public static String EM_PC_CJDU_URL = "https://eminfo.eastmoney.com/pc_news/News/GetNewsList"; // 东财pc资讯导读
    public static String EM_PC_724_URL = "https://eminfo.eastmoney.com/pc_news/FastNews/GetInfoList"; // 东财pc 7*24小时,// 似乎最快?
    public static String EM_PC_NEWEST_HOT_URL = "http://np-listapi.eastmoney.com/cms/pushlist"; //

    public static void main(String[] args) {
        List<EmPcNewestHotNew> newestHotEmPc = getNewestHotEmPc(500, 2);
        Console.log(newestHotEmPc);
    }

    /**
     * // @key3: 单页500, 大概就有十天半个月的 热点资讯了! 因此爬虫只需要一页就可以了!
     * 东财PC版本, 最新热门资讯, 即右上角资讯提示
     * wireshark找到
     * pageSize 可100, 不可1000
     * pageSize 似乎上限是500
     * pageNum 页数, 0和1都返回第一页!; 倒序
     * http://np-listapi.eastmoney.com/cms/pushlist?column=745&client=PC&sortStart=0&sortEnd=0&order=3&getInteract=0&req_trace=333D9AEBD0D64fa689008A742D4563C1&page_index=1&page_size=40
     */
    public static List<EmPcNewestHotNew> getNewestHotEmPc(int pageSize, int pageNum) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("column", "745");
        params.put("client", "PC");
        params.put("page_index", pageNum);
        params.put("page_size", pageSize);
        params.put("sortStart", "0");
        params.put("sortEnd", "0");
        params.put("order", "3");
        params.put("getInteract", "0");
        params.put("req_trace", "333D9AEBD0D64fa689008A742D4563C1");

        List<EmPcNewestHotNew> res = new ArrayList<>();

        try {
            String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(EM_PC_NEWEST_HOT_URL, params, 3000, 2);
            JSONObject jsonObject = JSONUtilS.parseObj(asStrUseHutool);
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                EmPcNewestHotNew item = new EmPcNewestHotNew();

                item.setCode(jsonObject1.getString("code"));
                item.setTitle(jsonObject1.getString("title"));
                item.setDigest(jsonObject1.getString("digest"));
                item.setSimtitle(jsonObject1.getString("simtitle"));
                item.setTitlecolor(jsonObject1.getString("titlecolor"));
                item.setShowtime(jsonObject1.getString("showtime"));
                item.setOrdertime(jsonObject1.getString("ordertime"));
                item.setPushtime(jsonObject1.getString("pushtime"));
                item.setUrl(jsonObject1.getString("url"));
                item.setImage(jsonObject1.getString("image"));
                item.setAuthor(jsonObject1.getString("author"));
                item.setSource(jsonObject1.getString("source"));
                item.setColumns(jsonObject1.getString("columns"));
                item.setChannels(jsonObject1.getString("channels"));
                item.setInteract(jsonObject1.getString("interact"));
                item.setSort(jsonObject1.getLong("sort"));
                item.setType(jsonObject1.getLong("type"));

                item.initDateTimeStr(); // 自行调用

                res.add(item);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 东财PC版本, 7*24小时
     * 数最大为 20 ; 才有效, 可更少
     * <p>
     * https://eminfo.eastmoney.com/pc_news/FastNews/GetInfoList?code=100&pageNumber=1&pagesize=20&condition=&r=1653918999873
     */
    public static List<EmPcFastNew> getFastNew724EmPc(int pageSize, int pageNum) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("code", "100");
        params.put("pageNumber", pageNum);
        params.put("pagesize", pageSize);
        params.put("condition", "");
        params.put("r", System.currentTimeMillis());

        List<EmPcFastNew> res = new ArrayList<>();

        try {
            String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(EM_PC_724_URL, params, 3000, 2);
            JSONObject jsonObject = JSONUtilS.parseObj(asStrUseHutool);
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                EmPcFastNew item = new EmPcFastNew();

                item.setInfoType(jsonObject1.getLong("infoType"));
                item.setInfoCode(jsonObject1.getString("infoCode"));
                item.setH24Type(jsonObject1.getLong("h24Type"));
                item.setCode(jsonObject1.getString("code"));

                item.setTitle(jsonObject1.getString("title"));
                item.setTitleStyle(jsonObject1.getString("titleStyle"));
                item.setCommentCount(jsonObject1.getLong("commentCount"));
                item.setUpdateTime(jsonObject1.getLong("updateTime"));
                item.setDigest(jsonObject1.getString("digest"));
                res.add(item);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 东财PC版本, 要闻精华, 同api; 首页!
     * 数最大为 20 ; 才有效, 可更少
     * https://eminfo.eastmoney.com/pc_news/News/GetNewsList?searchType=001&searchCondition=&startTime=1646150400000&endTime=&columnCode=345&p=1&pagesize=20&useOrderTime=true
     */
    public static List<EmPcNewItem> getNewestPcYWJH(int pageSize) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("searchType", "001");
        params.put("searchCondition", "");
        params.put("startTime", "1646150400000");
        params.put("endTime", "");
        params.put("columnCode", "345");
        params.put("p", "1");
        params.put("pagesize", pageSize);
        params.put("useOrderTime", "true");
        List<EmPcNewItem> res = new ArrayList<>();

        try {
            String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(EM_PC_YWJH_URL, params, 3000, 2);
            JSONObject jsonObject = JSONUtilS.parseObj(asStrUseHutool);
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                EmPcNewItem item = new EmPcNewItem();
                item.setInfoCode(jsonObject1.getString("infoCode"));
                item.setTitle(jsonObject1.getString("title"));
                item.setContent(jsonObject1.getString("content"));
                item.setSource(jsonObject1.getString("source"));

                item.setShowDateTime(jsonObject1.getLong("showDateTime"));

                res.add(item);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 东财PC版本, 要闻精华, 同api; 首页!
     * 参数最大为 20 ; 才有效, 可更少
     * pageNum 默认为1; 最小
     *
     * @noti 只有 columnCode参数不同, 且useOrderTime不同
     * https://eminfo.eastmoney.com/pc_news/News/GetNewsList?searchType=001&searchCondition=&startTime=1646150400000&endTime=&columnCode=344&p=1&pagesize=20&useOrderTime=false
     */
    public static List<EmPcNewItem> getNewestPcCJDU(int pageSize) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("searchType", "001");
        params.put("searchCondition", "");
        params.put("startTime", "1646150400000");
        params.put("endTime", "");
        params.put("columnCode", "344");
        params.put("p", "1");
        params.put("pagesize", pageSize);
        params.put("useOrderTime", "false");
        List<EmPcNewItem> res = new ArrayList<>();

        try {
            String asStrUseHutool = EastMoneyUtil.getAsStrUseHutool(EM_PC_CJDU_URL, params, 3000, 2);
            JSONObject jsonObject = JSONUtilS.parseObj(asStrUseHutool);
            JSONArray jsonArray = jsonObject.getJSONArray("items");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                EmPcNewItem item = new EmPcNewItem();
                item.setInfoCode(jsonObject1.getString("infoCode"));
                item.setTitle(jsonObject1.getString("title"));
                item.setContent(jsonObject1.getString("content"));
                item.setSource(jsonObject1.getString("source"));

                item.setShowDateTime(jsonObject1.getLong("showDateTime"));
                res.add(item);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 东财pc版, 单条新闻对象, json解析结果 -- 财经导读和资讯精华
     * <p>
     * "infoCode": "NW202205292395438077",
     * "showDateTime": 1653829208000,
     * "title": "什么信号？海外中国ETF重现强劲流入 机构发话：增持时机已到！",
     * "content": "最近一段时间随着疫情情况改善，海外机构杀回马枪，不少海外投资中国产品近期重获申购。与此同时，不少外资机构表示，目前投资者对中国市场情绪边际改善。相较于全球其它市场中国市场可作为分散风险的选项，与此同时，部分投资者认为中概股已经“跌不动”了，现在是底部进入的时机。我们一起来看看。中概互联投资风向标-K...",
     * "source": "中国基金报"
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmPcNewItem {
        public String infoCode;
        public long showDateTime; // 精确到秒数
        public String title;
        public String content;
        public String source;

        public String dateTimeStr;

        public void setShowDateTime(long showDateTime) {
            this.showDateTime = showDateTime;
            this.dateTimeStr = DateUtil.format(DateUtil.date(showDateTime), DatePattern.NORM_DATETIME_PATTERN);
        }
    }

    /**
     * 7*24小时快速资讯 -- json示例:
     * 普通资讯
     * {
     * "infoType": 1,
     * "infoCode": "NW202205302396671645",
     * "h24Type": 3,
     * "code": "202205302396671645",
     * "title": "加拿大年金计划委员会手握190只中国股票 市值超500亿元",
     * "titleStyle": "0",
     * "commentCount": 0,
     * "updateTime": 1653919229000,
     * "digest": "【加拿大年金计划委员会手握190只中国股票 市值超500亿元】近日，加拿大主权财富基金——加拿大年金计划委员会(CPPIB)披露最新财报，共190家中国企业现身其投资组合，市值超过500亿元人民币。其中，腾讯控股、美的集团、阿里巴巴、中免集团、宁德时代、贵州茅台、美团、中国平安、拼多多等均被持有。按市值计算，该机构持有最多的中国股票为腾讯控股，截至报告期末持仓市值达11.82亿加元，约合人民币62亿元。（中国证券报）"
     * },
     * <p>
     * <p>
     * 重点提示-->
     * {
     * "infoType": 1,
     * "infoCode": "NW202205302396671291",
     * "h24Type": 3,
     * "code": "202205302396671291",
     * "title": "央行研究报告提出促进消费投资增长 哪些领域将受到货币政策重点支持？",
     * "titleStyle": "3",
     * "commentCount": 13,
     * "updateTime": 1653919185000,
     * "digest": "【央行研究报告提出促进消费投资增长 哪些领域将受到货币政策重点支持？】周一央行研究局课题组发布政策研究报告指出，我国经济增长韧性强、潜力大，促进消费和投资增长大有可为，要充分发挥消费的基础作用和投资的关键作用，为绿色低碳发展提供系统化金融支持。"
     * },
     *
     * @key3 初步判定: 普通资讯与弹窗提示重要资讯, 应当以 "titleStyle": "3", 作为区分! 普通为 0
     * for (int i = 1; i <= 10; i++) {
     * List<EmPcFastNew> fastNew724EmPc = getFastNew724EmPc(20, i);
     * for (EmPcFastNew x : fastNew724EmPc) {
     * if (!x.getTitleStyle().equals("0")) {
     * Console.log(x.getTitle());
     * }
     * }
     * }  // 该机制 --> 比东财的资讯提示列表多 一点点资讯; 但东财提示列表有的, 几乎都有!
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmPcFastNew {
        public Long infoType;
        public String infoCode;
        public Long h24Type;
        public String code;

        public String title;
        public String titleStyle;
        public Long commentCount;
        public Long updateTime;
        public String digest;

        public String dateTimeStr;

        public boolean isKeyInfo() {
            if (titleStyle == null) {
                return false; // 初始化失败
            }
            return !"0".equals(titleStyle); // 常态0, 特殊3; 是否有其他未知
        }

        /**
         * 重写一下. 自增属性, 字符串表示的 日期
         *
         * @param updateTime
         */
        public void setUpdateTime(Long updateTime) {
            this.updateTime = updateTime;
            this.dateTimeStr = DateUtil.format(DateUtil.date(updateTime), DatePattern.NORM_DATETIME_PATTERN);
        }
    }

    /**
     * 东财最新热门资讯 json解析对象
     * {
     * "code": "202205312398034850",
     * "title": "消费电子股大爆发！旺季迎来需求复苏 业绩兑现成关键 关注电子板块两主线",
     * "digest": "【消费电子股大爆发！旺季迎来需求复苏 Q2业绩兑现成关键 关注电子板块两大主线】今日(5月31日)，三大指数集体走高，其中，创业板指大涨超2%。板块方面，消费电子、农业、科创板次新等板块涨幅居前，其中，消费电子概念股今日大爆发，个股掀起涨停潮，截至收盘，长盈精密、华兴源创、英集芯20cm涨停，国光电器、水晶光电、科瑞技术、鹏鼎控股、兴瑞科技、和胜股份、数源科技、歌尔股份、五方光电涨停，和林微纳、联合光电涨幅超10%。",
     * "simtitle": "",
     * "titlecolor": "3",
     * "showtime": "2022-05-31 18:03:41",
     * "ordertime": "2022-05-31 18:03:41",
     * "pushtime": "2022-05-31 18:07:34",
     * "url": "http://finance.eastmoney.com/a/202205312398034850.html",
     * "image": "https://dfscdn.dfcfw.com/download/D25467532527146238758_w210h154.jpg",
     * "author": "梓隆",
     * "source": "财联社",
     * "columns": "353,345,344,1379,1065,1060,745,746,748,750,796,797,1223,1224,1486,1525,405,1215,1216,406,408,804,1218,420",
     * "channels": "9,27,8,19,5,12",
     * "interact": null,
     * "sort": 1653991654000,
     * "type": 0
     * },
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmPcNewestHotNew {
        public String code;
        public String title;
        public String digest;
        public String simtitle;
        public String titlecolor;
        public String showtime;
        public String ordertime;
        public String pushtime;
        public String url;
        public String image;
        public String author;
        public String source;
        public String columns;
        public String channels;
        public String interact;
        public Long sort;
        public Long type;

        public String dateTimeStr; // == showtime / pushtime


        /**
         * 需要自行调用一下, 在设置了两个时间字符串以后
         *
         * @param updateTime
         */
        public void initDateTimeStr() {
            this.dateTimeStr = showtime + " / " + pushtime;
        }
    }

    private static final Log log = LogUtil.getLogger();
}
