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
import com.scareers.datasource.eastmoney.quotecenter.EmNewsApi;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.scareers.datasource.eastmoney.quotecenter.EmNewsApi.*;

/**
 * description: 东财的新闻快速发现及播报. 使用东财 PC 相同api, 而非爬虫使用的web api; 更加快速
 * 主要目标是盘中, 最快速度播报最新新闻, 面向 财经导读和要闻精华两栏.
 *
 * @author: admin
 * @date: 2022/5/30/030-00:48:28
 */
public class EmPcNewsNotify {
    /*
    财经导读和资讯精华
     */

    public static CopyOnWriteArraySet<String> knownTitles = new CopyOnWriteArraySet<>(); // 保存已知的新闻标题
    public static CopyOnWriteArrayList<EmNewsApi.EmPcNewItem> foundNews = new CopyOnWriteArrayList<>(); // 本程序发现的新闻列表
    // 本程序已发现的最新新闻列表, 本质按发现时间顺序

    public static int commonPageSize = 10; // 死循环时, 访问http时, 单页数量
    public static int sleepPerLoop = 200; // 循环sleep间隔, 毫秒
    /*
    7*24小时
     */
    public static CopyOnWriteArraySet<String> knownFastNewTitles = new CopyOnWriteArraySet<>(); // 保存已知的新闻标题 -- 快讯
    public static CopyOnWriteArrayList<EmNewsApi.EmPcFastNew> foundFastNews = new CopyOnWriteArrayList<>(); // 本程序发现的新闻列表 -- 快讯

    /*
    最新热门资讯 -- 弹窗提示
     */
    public static CopyOnWriteArraySet<String> knownHotTitles = new CopyOnWriteArraySet<>(); // 保存已知的新闻标题 -- 快讯
    public static CopyOnWriteArrayList<EmNewsApi.EmPcNewestHotNew> foundHotNews = new CopyOnWriteArrayList<>(); // 本程序发现的新闻列表 -- 快讯


    public static void main(String[] args) {

//        notifyCommonNew(); // 监控财经导读和要闻精华
//        notifyFast724New(); // 监控7*24中重要的
//        notifyNewestHotNew(); // 最新热门资讯

        List<EmNewsApi.EmPcNewestHotNew> newestHotEmPc = getNewestHotEmPc(40, 1);
        for (EmNewsApi.EmPcNewestHotNew x : newestHotEmPc) {
            Console.log(x.source);
        }
    }

    /**
     * 东财 最新热门资讯 新闻监控主函数
     */
    public static void notifyNewestHotNew() {
        infoLog("开始监控东财 最新热门资讯!");
        // 1.载入最新100条打底
        for (int i = 1; i <= 2; i++) {
            getNewestHotEmPc(40, i).forEach(item -> knownHotTitles.add(item.getTitle()));
        }
        infoLog(StrUtil.format("首次热门资讯加载完成,数量:{}! 开始死循环访问最新热门资讯!", knownHotTitles.size()));

        int epoch = 1;
        while (true) {
            try {
                List<EmPcNewestHotNew> NewestHotEmPc = getNewestHotEmPc(commonPageSize, 1);
                for (EmPcNewestHotNew item : NewestHotEmPc) {
                    if (!knownHotTitles.contains(item.getTitle())) { // 新资讯发现
                        Tts.playSound("热", true);
                        knownHotTitles.add(item.getTitle()); // 已发现
                        foundHotNews.add(item);
                        infoLog("最新热门资讯:\n" + item.toString()); //
//                        guiNotify("最新热门资讯", item);
                        ThreadUtil.sleep(3000);
                    }
                }
                if (epoch % 500 == 0) {
                    infoLog("东财新闻监控中...");
                    epoch = 1;
                }
                epoch++;
                ThreadUtil.sleep(sleepPerLoop / 4);
            } catch (Exception e) {
                e.printStackTrace();
                ThreadUtil.sleep(sleepPerLoop / 4);
            }
        }
    }


    /**
     * 东财 7*24 新闻监控主函数 -- 已筛选提示要闻,全部则太多了
     */
    public static void notifyFast724New() {
        infoLog("开始监控东财 7*24小时快讯 -- 重要新闻!");
        // 1.载入最新3页 7*24快讯
        for (int i = 1; i <= 3; i++) {
            getFastNew724EmPc(20, i).forEach(item -> knownFastNewTitles.add(item.getTitle()));
        }
        infoLog(StrUtil.format("首次快讯加载完成,数量:{}! 开始死循环访问最新7*24快讯!", knownFastNewTitles.size()));

        int epoch = 1;
        while (true) {
            try {
                List<EmPcFastNew> FastNew724EmPc = getFastNew724EmPc(commonPageSize, 1);
                for (EmPcFastNew item : FastNew724EmPc) {
                    if (item.isKeyInfo()) { // 多加逻辑, 需要是keyInfo
                        if (!knownFastNewTitles.contains(item.getTitle())) { // 新资讯发现
                            Tts.playSound("快", true);
                            knownFastNewTitles.add(item.getTitle()); // 已发现
                            foundFastNews.add(item);
                            infoLog("7*24快讯重要资讯:\n" + item.toString()); //
                            guiNotify("7*24快讯重要资讯", item);
                            ThreadUtil.sleep(3000); //
                        }
                    }
                }
                if (epoch % 100 == 0) {
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
     * 东财 要闻精华和财经导读 新闻监控主函数
     */
    public static void notifyCommonNew() {
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
                        Tts.playSound("精", true);
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
                        Tts.playSound("导", true);
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
     * 最新热门资讯, gui提示
     *
     * @param type 是财经导读或资讯精华
     * @param item
     */
    public static void guiNotify(String type, EmPcNewestHotNew item) {
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
                String content = item.getDigest();
                int line = (int) Math.ceil(content.length() / 30.0); // 行数
                for (int i = 0; i < line; i++) {
                    stringBuilder.append(content, i * 30, Math.min((i + 1) * 30, content.length()));
                    stringBuilder.append("<br>");
                }

                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<br>");

                stringBuilder.append("<p color=\"red\">");
                stringBuilder.append("--- ").append(item.getDateTimeStr());
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<p color=\"red\">");
//                <a href="url">链接文本</a>
                stringBuilder.append(StrUtil.format("<a href=\"{}\">", item.getUrl()));
                stringBuilder.append(item.getUrl());
                stringBuilder.append("</a>");
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("</html>");
                JOptionPane
                        .showMessageDialog(null, stringBuilder.toString(), type + "-- 新闻发现", JOptionPane.PLAIN_MESSAGE);
            }
        }, true);
    }

    /**
     * 新的新闻(7*24小时), gui提示
     *
     * @param type 是财经导读或资讯精华
     * @param item
     */
    public static void guiNotify(String type, EmPcFastNew item) {
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
                String content = item.getDigest();
                int line = (int) Math.ceil(content.length() / 30.0); // 行数
                for (int i = 0; i < line; i++) {
                    stringBuilder.append(content, i * 30, Math.min((i + 1) * 30, content.length()));
                    stringBuilder.append("<br>");
                }

                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<br>");

                stringBuilder.append("<p color=\"red\">");
                stringBuilder.append("--- ").append(item.getDateTimeStr());
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("</html>");


                JOptionPane
                        .showMessageDialog(null, stringBuilder.toString(), type + "-- 新闻发现", JOptionPane.PLAIN_MESSAGE);
            }
        }, true);
    }


    /**
     * 新的新闻(财经导读和资讯精华), gui提示
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
                        .append("--- " + item.getSource() + " " + item.getDateTimeStr());
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("</html>");


                JOptionPane
                        .showMessageDialog(null, stringBuilder.toString(), type + "-- 新闻发现", JOptionPane.PLAIN_MESSAGE);
            }
        }, true);
    }


    private static final Log log = LogUtil.getLogger();
}
