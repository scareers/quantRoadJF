package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.thread.ThreadUtil;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * description: 同花顺实时解盘, 使用 web api;
 * 延迟未知!
 *
 * @author: admin
 * @date: 2022/6/15/015-11:16:00
 */
public class ThsSsjpWebApiNotify {
    public static CopyOnWriteArraySet<ThsSsjpNew> newPool = new CopyOnWriteArraySet<>();

    public static void main(String[] args) {
        main0();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ThsSsjpNew {
        String title;
        String timeStr;
        String content;
    }


    public static int perLoopSleep = 200;

    public static void main0() {
        CommonUtil.notifyInfo("开始 同花顺实时解盘 新闻监控...");
        // 1.首次载入
        List<ThsSsjpNew> newestSsjp = getNewestSsjp();
        newPool.addAll(newestSsjp);

        int epoch = -1;
        // 2.判定新增
        while (true) {
            ThreadUtil.sleep(perLoopSleep);
            List<ThsSsjpNew> newestSsjp0 = getNewestSsjp();
            for (ThsSsjpNew thsSsjpNew : newestSsjp0) {
                if (!newPool.contains(thsSsjpNew)) {
                    newPool.add(thsSsjpNew);
                    Tts.playSound("解", true, false);
                    CommonUtil.notifyInfo("同花顺最新热门资讯:\n" + thsSsjpNew.toString() + "  [" + thsSsjpNew.timeStr + " ]");
//                        guiNotify("最新热门资讯", item);
                    ThreadUtil.sleep(3000);
                }
            }

            if (epoch % 1000 == 0) {
                CommonUtil.notifyInfo("同花顺实时解盘 监控中...");
                epoch = 1;
                continue;
            }
            epoch++;
        }

    }

    /**
     * 获取今日实时解盘页面, 全新闻!
     *
     * @return
     */
    public static List<ThsSsjpNew> getNewestSsjp() {
        List<ThsSsjpNew> res = new ArrayList<>();
        try {
            String htmlStr = EastMoneyUtil
                    .getAsStrUseHutool("http://stock.10jqka.com.cn/jiepan_list/20220615/", null, 3000, 2);
            Document document = Jsoup.parse(htmlStr);

            Elements container = document.getElementsByClass("list-con"); // 唯一
            Element ul = container.get(0).getElementsByTag("ul").get(0);


            List<Element> elementList = ul.getElementsByTag("li").subList(0, 20); // 20

            for (Element element : elementList) {
                Element span1 = element.getElementsByClass("arc-title").get(0);
                Element titleA = span1.getElementsByTag("a").get(0);
                Element timeSpan = span1.getElementsByTag("span").get(0);
                Element contentA = element.getElementsByTag("a").get(1);
                ThsSsjpNew thsSsjpNew = new ThsSsjpNew(titleA.text(), timeSpan.text(), contentA.text());
                res.add(thsSsjpNew);
            }
        } catch (Exception e) {
            CommonUtil.notifyError("同花顺实时解盘监控异常: " + e.toString());
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 提示方式
     *
     * @param thsSsjpNew
     */
    public static void notifyNew(ThsSsjpNew thsSsjpNew) {

    }
}
