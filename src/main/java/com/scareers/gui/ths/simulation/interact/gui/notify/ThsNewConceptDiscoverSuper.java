package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.utils.CommonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/**
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

    public static void main(String[] args) throws UnsupportedEncodingException, TimeoutException, InterruptedException {
        main1();
//        SpecialDriver specialDriver = new SpecialDriver(true);
//        specialDriver.accessStock("600007");

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThsKeyIssue {
        String type; // 类型, 例如: 新增概念, 异动提醒, 发布公告 等等描述性
        String content; // 具体内容
    }


    public static volatile boolean oneEpochFinish = false;

    public static void main1() throws TimeoutException, InterruptedException {
        // 1.所有股票代码列表
        List<StockBondBean> allStockWithBond = BondUtil.getAllStockWithBond();

        // 2.浏览器池!
        ArrayList<SpecialDriver> drivers = new ArrayList<>();
        int driverPoolSize = 2;
        for (int i = 0; i < driverPoolSize; i++) {
            drivers.add(new SpecialDriver(true));
        }

        // 3.线程池, 数量与浏览器相等
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(driverPoolSize, driverPoolSize, 200, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        TimeInterval timer = DateUtil.timer();
        timer.start();
//        for (int i = 0; i < stockCodes.size(); i++) {
        for (int i = 0; i < allStockWithBond.size(); i++) {
            // 1.尝试等待有空闲浏览器, 没有则会阻塞
            CommonUtil.waitUtil(new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                    for (SpecialDriver specialDriver : SpecialDriver.driverPool) {
                        if (!specialDriver.isRunning()) {
                            return true;
                        }
                    }
                    return false;
                }
            }, Integer.MAX_VALUE, 1, null, false);
            // 尝试异步执行任务, 等待防止异步任务堆积

            int finalI = i;
            poolExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    StockBondBean stockBondBean = allStockWithBond.get(finalI);
                    List<ThsKeyIssue> s = SpecialDriver.accessStockWithPool(stockBondBean);
                    todayKeyIssuesMap.put(stockBondBean, s);
                }
            });
        }
        CommonUtil.waitUtil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return amount >= allStockWithBond.size() - 1;
            }
        }, Integer.MAX_VALUE, 1, null, false);

        CommonUtil.notifyInfo(StrUtil.format("耗时: {}", timer.intervalRestart()));
    }

    public static volatile int amount = 0;

    /**
     * 封装 selenium 的 WebDriver, 专门执行 任务!
     */
    @Data
    public static class SpecialDriver {
        public static CopyOnWriteArraySet<SpecialDriver> driverPool = new CopyOnWriteArraySet<>();
        public static CopyOnWriteArraySet<String> oldAllConceptNameSet = new CopyOnWriteArraySet();

        private volatile boolean running; // 是否正在访问某只股票?
        private volatile boolean headless; // 是否隐藏浏览器窗口?

        private WebDriver driver;

        public static void init() {
            String today = DateUtil.today();
            String pre5Date = EastMoneyDbApi.getPreNTradeDateStrict(today, 5);
            oldAllConceptNameSet.addAll(ThsDbApi.getAllConceptNameByDate(pre5Date));
        }

        public static List<ThsKeyIssue> accessStockWithPool(StockBondBean stockBondBean) {
            while (true) {
                for (SpecialDriver specialDriver : driverPool) {
                    if (!specialDriver.isRunning()) {
                        return specialDriver.accessStock(stockBondBean);
                    }
                }
                ThreadUtil.sleep(1);
            }


        }


        public SpecialDriver(boolean headless) {
            this.headless = headless;
            this.running = false;
            initDriver();
            driverPool.add(this);
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
//            driver.manage().timeouts().implicitlyWait(Duration.of(500, ChronoUnit.MILLIS));
        }

        /**
         * 核心方法, 访问个股页面!
         *
         * @param stockCode
         */
        public List<ThsKeyIssue> accessStock(StockBondBean stockBondBean) {
            amount++;
            driverPool.remove(this);
            this.running = true;

            driver.get(StrUtil.format("http://basic.10jqka.com.cn/{}/", stockBondBean.getStockCode()));
            WebElement tableToday = null;
            try {
                tableToday = driver.findElement(By.id("tableToday"));
            } catch (org.openqa.selenium.NoSuchElementException e) {
                this.running = false;
                driverPool.add(this);
//                e.printStackTrace();
                return null;
            }

            List<String> contentRes = new ArrayList<>();
            // 具体内容的标签, 都是span标签
            List<WebElement> contents = tableToday.findElements(By.tagName("span"));
            for (WebElement content : contents) {
                contentRes.add(content.getText());
            }

            List<String> typeRes = new ArrayList<>();
            // 具体内容的标签, 都是span标签
            List<WebElement> contents2 = tableToday.findElements(By.tagName("strong"));
            for (WebElement content2 : contents2) {
                typeRes.add(content2.getText());
            }

            List<ThsKeyIssue> res = new ArrayList<>();
            for (int i = 0; i < typeRes.size(); i++) {
                res.add(new ThsKeyIssue(typeRes.get(i), contentRes.get(i)));
            }
            Console.log(res);
            this.running = false;
            driverPool.add(this);
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
