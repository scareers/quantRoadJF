package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.gui.ths.simulation.interact.gui.util.ManiLog;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.log.LogUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * description: 东财的新闻快速发现及播报. 使用东财 PC 相同api, 而非爬虫使用的web api; 更加快速
 * 主要目标是盘中, 最快速度播报最新新闻, 面向 财经导读和要闻精华两栏.
 *
 * @author: admin
 * @date: 2022/5/30/030-00:48:28
 */
public class EmNewsNotify {
    public static String EM_PC_YWJH_URL = "https://eminfo.eastmoney.com/pc_news/News/GetNewsList"; // 东财pc要闻精华
    public static String EM_PC_CJDU_URL = "https://eminfo.eastmoney.com/pc_news/News/GetNewsList"; // 东财pc资讯导读
    public static int commonPageSize = 5; // 死循环时, 访问http时, 单页数量
    public static int sleepPerLoop = 500; // 循环sleep间隔, 毫秒
    public static CopyOnWriteArraySet<String> knownTitles = new CopyOnWriteArraySet<>(); // 保存已知的新闻标题
    public static CopyOnWriteArrayList<EmPcNewItem> foundNews = new CopyOnWriteArrayList<>();
    // 本程序已发现的最新新闻列表, 本质按发现时间顺序

    private static final Log log = LogUtil.getLogger();


    public static void main(String[] args) {
//        ThreadUtil.sleep(5000);
//
//        List<EmPcNewItem> news = getNewestPcCJDU(20);
//        for (EmPcNewItem item : news) {
//
//            guiNotify("资讯精华", item);
//        }

        main0();


    }

    /**
     * 东财新闻监控主函数
     */
    public static void main0() {
        infoLog("开始监控东财 要闻精华和财经导读 资讯!");
        getNewestPcCJDU(20).forEach(emPcNewItem -> knownTitles.add(emPcNewItem.getTitle()));
        getNewestPcYWJH(20).forEach(emPcNewItem -> knownTitles.add(emPcNewItem.getTitle()));
        infoLog(StrUtil.format("首次首页资讯加载完成,数量:{}! 开始死循环访问最新资讯!", knownTitles.size()));

        int epoch = 1;
        while (true) {
            try {
                List<EmPcNewItem> NewestPcYWJH = getNewestPcYWJH(commonPageSize);
                for (EmPcNewItem item : NewestPcYWJH) {
                    if (!knownTitles.contains(item.getTitle())) { // 新资讯发现
                        Tts.playSound("精", false);
                        knownTitles.add(item.getTitle()); // 已发现
                        foundNews.add(item);
                        infoLog("资讯精华新闻:\n" + item.toString());

                        guiNotify("资讯精华", item);
                        ThreadUtil.sleep(3000);
                    }
                }

                List<EmPcNewItem> newestPcCJDU = getNewestPcCJDU(commonPageSize);
                for (EmPcNewItem item : newestPcCJDU) {
                    if (!knownTitles.contains(item.getTitle())) { // 新资讯发现
                        Tts.playSound("导", false);
                        knownTitles.add(item.getTitle()); // 已发现
                        foundNews.add(item);

                        infoLog("财经导读新闻:\n" + item.toString());
                        guiNotify("财经导读", item);
                        ThreadUtil.sleep(3000);
                    }
                }

                if (epoch % 20 == 0) {
                    infoLog("东财新闻监控中...");
                    epoch = 1;
                }
                epoch++;
                ThreadUtil.sleep(sleepPerLoop);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * log 提示
     *
     * @param info
     */
    public static void infoLog(String info) {
        log.info(info);
        ManiLog.put(info);
    }
    // JOptionPane.showMessageDialog(null, "You input is"+str+"\n"+"ASCII is"+b, str, JOptionPane.PLAIN_MESSAGE);

    /**
     * 新的新闻, gui提示
     *
     * @param type 是财经导读或资讯精华
     * @param item
     */
    public static void guiNotify(String type, EmPcNewItem item) {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<html>");
                stringBuilder.append("<h3 color=\"red\">");
                stringBuilder.append(item.getTitle());
                stringBuilder.append("</h3>");
                stringBuilder.append("<br>");
                stringBuilder.append("<p color=\"black\">");

                // 分行加入
                String content = item.getContent();
                int line = (int) Math.ceil(content.length() / 30.0); // 行数
                for (int i = 0; i < line; i++) {
                    stringBuilder.append(content, i * 30, Math.min((i + 1) * 30, content.length()));
                    stringBuilder.append("<br>");
                }

                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<br>");

                stringBuilder.append("<p color=\"red\">");
                stringBuilder
                        .append("--- " + item.getSource() + " " + DateUtil.format(DateUtil.date(item.getShowDateTime()),
                                DatePattern.NORM_TIME_PATTERN));
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("</html>");


                JOptionPane
                        .showMessageDialog(null, stringBuilder.toString(), type + "-- 新闻发现", JOptionPane.PLAIN_MESSAGE);
            }
        }, true);
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
     * 东财pc版, 单条新闻对象, json解析结果
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
    }

}
