package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.scareers.datasource.eastmoney.dailycrawler.CrawlerChainEm.waitPoolFinish;

/**
 * //@key3 : 两大url
 * http://stockpage.10jqka.com.cn/601788/event/
 * http://basic.10jqka.com.cn/000001/event.html
 * // @key4: 使用4个selenium 十分消耗cpu
 * description: 同花顺新概念发现超级版本, 使用selenium 访问 "近期重要事件"!
 * 因web api 太难处理;
 * ------------->
 * * description: 东财和同花顺某些api, 因加密极难破解, 因此使用 selenium获取数据.
 * * 使用谷歌浏览器, 自行保证 chromedriver.exe 位于path中, 且版本匹配.
 * * WebDriver driver 对象, 将构建driver池!
 * *
 * * 日志说明:
 * * 全项目默认使用hutool搭配log4j; 而selenium使用到 netty相关库, 默认使用 slf4j搭配logback.
 * * 为了控制日志, 因此需要创建 logback.xml 配置文件. 控制大量 netty库的日志输出. 默认debug级别日志太多了.
 * * selenium 自身的log级别则由: chromeOptions.setLogLevel(ChromeDriverLogLevel.OFF); 控制
 * ----------->
 *
 * @author: admin
 * @date: 2022/6/14/014-23:11:51
 */
public class ThsNewConceptDiscoverSuper {
    /**
     * 保存今日重要事件! 映射; value是 具体描述
     */
    public static ConcurrentHashMap<StockBondBean, List<ThsKeyIssue>> todayKeyIssuesMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {

        // 强制关闭遗留! --> 注意也会关闭正常的浏览器

        CommonUtil.closeChromeRelatedProcess();
        return;


//        main0();


    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThsKeyIssue {
        String type; // 类型, 例如: 新增概念, 异动提醒, 发布公告 等等描述性
        String content; // 具体内容
    }

    public static ArrayList<SpecialDriver> drivers;
    public static volatile boolean headless = true;
    public static final int driverPoolSize = 4;
    public static final int maxSoundNotiTimes = 2; // 新概念发现, 最多播放n次

    /**
     * 程序退出, 应当关闭所有浏览器
     */
    public static void quitAllDriver() {
        if (drivers != null) {
            for (SpecialDriver driver : drivers) {
                driver.quitDriver();
            }
            CommonUtil.notifyInfo("同花顺新概念F10发现: --> 已关闭所有selenium浏览器");
        }
    }

    public static void main0() {
        initOldConceptNameSet(); // 所有老概念列表

        // 1.所有股票代码列表
        List<StockBondBean> allStockWithBond = null;
        while (allStockWithBond == null) {
            CommonUtil.notifyInfo("获取全部转债股票bean,以监控同花顺股票近期重要事件");
            allStockWithBond = BondUtil.getAllStockWithBond();
        }

        // 2.浏览器池!
        drivers = new ArrayList<>();
        for (int i = 0; i < driverPoolSize; i++) {
            drivers.add(new SpecialDriver(headless));
        }
        CommonUtil.notifyInfo("初始化 chrome池 成功");

        while (true) {
            TimeInterval timer = DateUtil.timer();
            timer.start();
            // 3.每轮新线程池, 数量可与浏览器相等
            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(driverPoolSize, driverPoolSize, 200,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());

            for (int i = 0; i < allStockWithBond.size(); i++) {
//            for (int i = 150; i < 200; i++) {
//        for (int i = 0; i < 100; i++) {
                // 1.随机取
                SpecialDriver freeDriver = RandomUtil.randomEle(drivers);

                // 2.判定空闲
                if (freeDriver.isRunning()) { // 都忙, 则返回
                    ThreadUtil.sleep(1); // 稍等, 返回, 注意索引
                    i--; //
                    continue;
                }

                // 3.有空的.
                StockBondBean stockBondBean = allStockWithBond.get(i);

                SpecialDriver finalFreeDriver = freeDriver;
                // 异步访问.
                poolExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        List<ThsKeyIssue> issueList = finalFreeDriver.accessStock(stockBondBean);
                        todayKeyIssuesMap.putIfAbsent(stockBondBean, new ArrayList<>());
                        List<ThsKeyIssue> oldIssueList = todayKeyIssuesMap.get(stockBondBean);

                        if (issueList != null) {
                            for (ThsKeyIssue thsKeyIssue : issueList) {
                                notifyNewIssue(thsKeyIssue, stockBondBean);
//                                if (!oldIssueList.contains(thsKeyIssue)) { // 新的发现, 进行提示!
//                                }
                            }
                            todayKeyIssuesMap.put(stockBondBean, issueList);
                        }
                    }
                });


            }
            waitPoolFinish(poolExecutor);
            for (SpecialDriver driver : drivers) {
                driver.setRunning(false);
            }

//            CommonUtil.notifyInfo(StrUtil.format("耗时: {}", timer.intervalRestart()));
        }
    }

    /**
     * 当新概念发现时, 记录其总计播报次数; 发现新概念, 最多会播报n次
     */
    public static ConcurrentHashMap<String, Integer> notiTimesOfConceptMap = new ConcurrentHashMap<>();

    /**
     * 新事件的提示方法, 建议异步, 不要太耗时
     * -------> content=增加同花顺概念“粤港澳大湾区”概念解析 详细内容 ▼)
     *
     * @param thsKeyIssue
     */
    public static void notifyNewIssue(ThsKeyIssue thsKeyIssue, StockBondBean stockBondBean) {
        String content = thsKeyIssue.getContent();
//        Console.log(content);
        if (content.contains("增加同花顺概念")) {
            int i = content.indexOf("“");
            int j = content.indexOf("”");
            String conceptName = content.substring(i + 1, j);

//            CommonUtil.notifyInfo(
//                    StrUtil.format("{}/{} -- {}", stockBondBean.getBondName(), stockBondBean.getStockName(),
//                            thsKeyIssue)); // 增加概念

            if (!oldAllConceptNameSet.contains(conceptName)) {
                notiTimesOfConceptMap.putIfAbsent(conceptName, 0);
                Integer times = notiTimesOfConceptMap.get(conceptName);

                if (times < maxSoundNotiTimes) {
                    // 新概念发现了!
                    Tts.playSound(StrUtil.format("同花顺F10新概念发现: {}", conceptName), true, false);
                    notiTimesOfConceptMap.put(conceptName, times + 1);
                }
            }
        }

    }


    public static CopyOnWriteArraySet<String> oldAllConceptNameSet = new CopyOnWriteArraySet();

    public static void initOldConceptNameSet() {
        String today = DateUtil.today();
        String pre5Date = EastMoneyDbApi.getPreNTradeDateStrict(today, 2);
        oldAllConceptNameSet.addAll(ThsDbApi.getAllConceptNameByDate(pre5Date));
    }

    /**
     * 封装 selenium 的 WebDriver, 专门执行 任务!
     */
    @Data
    public static class SpecialDriver {

        private volatile boolean running; // 是否正在访问某只股票?
        private volatile boolean headless; // 是否隐藏浏览器窗口?

        private WebDriver driver;


        public SpecialDriver(boolean headless) {
            this.headless = headless;
            this.running = false;
            initDriver();
        }

        /**
         * 初始化浏览器属性
         */
        public void initDriver() {
            ChromeOptions chromeOptions = new ChromeOptions();
            if (headless) {
                chromeOptions.addArguments("--headless");
            }
            // 摆脱日志繁琐,  logback.xml 控制netty等库日志
            chromeOptions.setLogLevel(ChromeDriverLogLevel.OFF);
            this.driver = new ChromeDriver(chromeOptions);
            // 5秒默认超时, 差不多了
            // 查找元素 500ms内没出现就超时, 也可以禁用!
            driver.manage().timeouts().implicitlyWait(Duration.of(100, ChronoUnit.MILLIS));
        }

        /**
         * 核心方法, 访问个股页面! 方法同步!
         *
         * @param stockCode
         */
        public synchronized List<ThsKeyIssue> accessStock(StockBondBean stockBondBean) {
            this.running = true;

            driver.get(StrUtil.format("http://basic.10jqka.com.cn/{}/", stockBondBean.getStockCode()));
            WebElement tableToday = null;
            try {
                tableToday = driver.findElement(By.id("tableToday"));
            } catch (org.openqa.selenium.NoSuchElementException e) {
                this.running = false;
//                e.printStackTrace();
                return null;
            }
            WebElement tbody = tableToday.findElement(By.tagName("tbody"));
            List<WebElement> trs = tbody.findElements(By.tagName("tr"));
            List<ThsKeyIssue> res = new ArrayList<>();
            for (WebElement tr : trs) {
                List<WebElement> tds = tr.findElements(By.tagName("td"));
                for (WebElement td : tds) {
                    if (td.getText().contains("今天") || td.getAttribute("class").contains("today")) {
                        continue;
                    }

//                 第一个 strong 和第一个 span.
                    res.add(new ThsKeyIssue(
                            td.findElements(By.tagName("strong")).get(0).getText(),
                            td.findElements(By.tagName("span")).get(0).getText()
                    ));
                }
            }

            this.running = false;
            return res;
        }

        /**
         * 注意, close是关闭当前web窗口, quit才是退出浏览器
         */
        public void quitDriver() {
            if (driver != null) {
                driver.quit();
            }
        }

        /**
         * 重启浏览器, 即完全关闭, 新打开浏览器
         */
        public void restartDriver() {
            quitDriver();
            initDriver();
        }
    }


}
