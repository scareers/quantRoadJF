package com.scareers.gui.ths.simulation.interact.gui.notify;

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;
import lombok.Data;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public static CopyOnWriteArraySet<String> res = new CopyOnWriteArraySet<>();

    public static void main(String[] args) {
        String url = StrUtil.format("http://basic.10jqka.com.cn/{}/", "000001");

        String responseText = StrUtil.format("var xmlhttp=new XMLHttpRequest();\n" +
                "            xmlhttp.open(\\\"POST\\\",\\\"%s\\\",false);\n" +
                "            xmlhttp.setRequestHeader(\\\"Content-type\\\",\\\"application/x-www-form-urlencoded\\\");\n" +
                "            xmlhttp.send();\n" +
                "            return xmlhttp.responseText;", url);

        SpecialDriver driver1 = new SpecialDriver(true);
        driver1.accessStock("600001");

        WebDriver driver = driver1.getDriver();
// todo 未完成

//        // 1.所有股票代码列表
//        DataFrame<Object> stocks = EmQuoteApi.getRealtimeQuotes(Arrays.asList("沪深A股"));
//        List<String> stockCodes = DataFrameS.getColAsStringList(stocks, "资产代码");
//        Console.log(stockCodes.size());
//
//        // 2.浏览器池!
//        ArrayList<SpecialDriver> drivers = new ArrayList<>();
//        int driverPoolSize = 5;
//        for (int i = 0; i < driverPoolSize; i++) {
//            drivers.add(new SpecialDriver(true));
//        }
//
//        // 3.线程池, 数量与浏览器相等
//        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(driverPoolSize, driverPoolSize, 200, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>());
//
//        // 3.遍历股票, 执行访问
//        while (true) {
//            for (int i = 0; i < 100; i++) {
//                String s = stockCodes.get(i);
//
//                // 1.找到一个空闲的driver
//                SpecialDriver tempDriver = null;
//                for (SpecialDriver driver : drivers) {
//                    if (!driver.running) {
//                        tempDriver = driver;
//                        break;
//                    }
//                }
//                // 2.全部遍历完成, 都在运行有可能
//                if (tempDriver == null) {
//                    ThreadUtil.sleep(100);
//                    continue;
//                }
//
//                // 3.有浏览器空闲, 执行任务
//                SpecialDriver finalTempDriver = tempDriver;
//                poolExecutor.submit(new Runnable() {
//                    @Override
//                    public void run() {
//                        String s1 = finalTempDriver.accessStock(s);
//                        res.add(s);
//                        Console.log(res.size());
//                    }
//                });
//            }
//        }
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
            driver.manage().timeouts().implicitlyWait(Duration.of(500, ChronoUnit.MILLIS));
        }

        /**
         * 核心方法, 访问个股页面!
         *
         * @param stockCode
         */
        public String accessStock(String stockCode) {
            this.running = true;
            Console.log(stockCode);
            driver.get(StrUtil.format("http://basic.10jqka.com.cn/{}/", stockCode));
            WebElement tablelist = driver.findElement(By.id("tableList"));
            this.running = false;
            return tablelist.getText();
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
