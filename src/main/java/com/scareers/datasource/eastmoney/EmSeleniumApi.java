package com.scareers.datasource.eastmoney;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.backtest.fs.loybuyhighsell.parameter.IndexRealTimeRaiseFallParameter;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * description: 东方财富某些api, 因加密极难破解, 因此使用 selenium技术获取数据.
 * 使用谷歌浏览器, 自行保证 chromedriver.exe 位于path中, 且版本匹配.
 * WebDriver driver 对象, 将懒加载为静态属性, 每个api均检测其是否初始化.
 * <p>
 * 日志说明:
 * 全项目默认使用hutool搭配log4j; 而selenium使用到 netty相关库, 默认使用 slf4j搭配logback.
 * 为了控制日志, 因此需要创建 logback.xml 配置文件. 控制大量 netty库的日志输出. 默认debug级别日志太多了.
 * selenium 自身的log级别则由: chromeOptions.setLogLevel(ChromeDriverLogLevel.OFF); 控制
 *
 * @author: admin
 * @date: 2022/2/15/015-18:02:28
 */
public class EmSeleniumApi {
    private static final Log log = LogUtil.getLogger();

    // 全局唯一
    //    public static CopyOnWriteArrayList<WebDriver> driverPool;
    public static WebDriver driver;
    public static final Object driverLock = new Object();
    public static final List<String> stockPopularityListColNames = Arrays.asList("当前排名", "排名较昨日变动", "历史趋势", "代码",
            "股票名称", "相关",
            "最新价", "涨跌额", "涨跌幅",
            "新晋粉丝", "铁杆粉丝"); // 个股人气榜表头.
    public static final List<String> stockPopularityRaisingListColNames = Arrays.asList("排名较昨日变动", "当前排名", "历史趋势",
            "代码",
            "股票名称", "相关",
            "最新价", "涨跌额", "涨跌幅",
            "新晋粉丝", "铁杆粉丝"); // 个股人气飙升榜 表头.

    public static void checkDriver() {
        if (driver == null) {
            ChromeOptions chromeOptions = new ChromeOptions();
//            chromeOptions.addArguments("--headless");
            // 摆脱日志繁琐,  logback.xml 控制netty等库日志
            chromeOptions.setLogLevel(ChromeDriverLogLevel.WARNING);
            driver = new ChromeDriver(chromeOptions);
            driver.manage().timeouts().implicitlyWait(Duration.of(20, ChronoUnit.SECONDS));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }

        }
    }

    public static void closeDriver() {
        if (driver != null) {
            // driver.close(); // 关闭当前窗口, 若唯一则退出
            driver.quit(); // 关闭所有窗口退出, 且将终止 chromedriver.exe进程
        }
    }

    public static void main(String[] args) {
        DataFrame<Object> dataFrame = stockPopularityRaisingList();
        Console.log(dataFrame.col("当前排名"));
        Console.log(dataFrame.col("排名较昨日变动"));
        Console.log(dataFrame.col("历史趋势"));
        Console.log(dataFrame.col("股票名称"));
        Console.log(dataFrame.col("涨跌额"));
        Console.log(dataFrame.col("新晋粉丝"));
        Console.log(dataFrame.col("铁杆粉丝"));
        Console.log(dataFrame.col("相关"));
        closeDriver();
    }

    /**
     * 原因: 实测股票排名api, 返回的数据股票列表需要 AES 解密. 且js源代码因压缩而难以阅读.
     * <p>
     * 个股人气榜单前 100
     * url: https://guba.eastmoney.com/rank/
     * api: 获取前100股票的行情数据. 但需要提供股票 secIds 参数.
     * https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&np=3&ut=a79f54e3d4c8d44e494efb8f748db291&invt=2&secids=0.300059,0.002349,0.002400,0.002432,0.002104,1.603123,0.002657,0.000796,0.300261,0.300750,0.002761,0.002621,0.002348,0.002721,0.301201,0.002542,0.002530,1.603259,0.002537,0.300363&fields=f1,f2,f3,f4,f12,f13,f14,f152,f15,f16&cb=qa_wap_jsonpCB1644896036131
     *
     * @return
     * @noti 排名的前123, 文本为空, 已填充
     * @noti 历史趋势列, 为图片. 文字为 "", 点击可以到热度趋势页面, 该字段已更改为该url
     * @noti 新晋粉丝:铁杆粉丝列 已分割开
     * @noti "相关" 列, 更改为股吧 url.
     */
    public static DataFrame<Object> stockPopularityOnTheList() {
        checkDriver();

        DataFrame<Object> res = new DataFrame<>(stockPopularityListColNames);

        synchronized (driverLock) {
            driver.get("https://guba.eastmoney.com/rank/");

            for (int i = 0; i < 5; i++) {
                getOnePage(res, i);
            }
        }

        res.set(0, "当前排名", "1");
        res.set(1, "当前排名", "2");
        res.set(2, "当前排名", "3");

        repairPopularityList(res);
//        https://guba.eastmoney.com/rank/stock?code=002395

        return res;
    }

    /**
     * raising: 飙升榜前100
     *
     * @return
     */
    public static DataFrame<Object> stockPopularityRaisingList() {
        checkDriver();
        DataFrame<Object> res = new DataFrame<>(stockPopularityRaisingListColNames);
        String raisingListXPath = "//*[@id=\"rankCont\"]/div[1]/div[1]/div[1]/span[2]";
        synchronized (driverLock) {
            driver.get("https://guba.eastmoney.com/rank/");
            ExpectedConditions.presenceOfElementLocated(By.xpath(raisingListXPath));
            driver.findElement(By.xpath(raisingListXPath)).click();// 飙升榜按钮
            // //*[@id="rankCont"]/div[1]/div[1]/div[1]/span[2]

            int i = 0;
            while (i < 5) {
                try {
                    getOnePage(res, i);
                } catch (Exception e) {
                    driver.navigate().refresh(); // 刷新一下
                    ExpectedConditions.presenceOfElementLocated(By.xpath(raisingListXPath));
                    driver.findElement(By.xpath(raisingListXPath)).click();// 飙升榜按钮
                    res = new DataFrame<>(stockPopularityRaisingListColNames);
                    i = 0;
                }

                // 判定是否从人气榜, 成功转换到飙升榜?, 若未能, 则重置所有
                if (i == 0) {
                    List<Integer> ranks = DataFrameS.getColAsIntegerList(res, "当前排名");
                    boolean notChangeFlag = true;
                    for (int j = 3; j < 20; j++) {
                        if (ranks.get(j) != j + 1) {
                            notChangeFlag = false;
                            break;
                        }
                    }
                    if (notChangeFlag) {
                        res = new DataFrame<>(stockPopularityRaisingListColNames);
                        driver.get("https://guba.eastmoney.com/rank/");
                        ExpectedConditions.presenceOfElementLocated(By.xpath(raisingListXPath));
                        driver.findElement(By.xpath(raisingListXPath)).click();// 飙升榜按钮
                        continue;
                    }
                }
                i++;
            }
        }
        repairPopularityList(res);
        return res;
    }

    private static void repairPopularityList(DataFrame<Object> res) {
        for (int i = 0; i < res.length(); i++) {
            String[] fans = res.get(i, "新晋粉丝").toString().split("\n");
            res.set(i, "新晋粉丝", fans[0]);
            res.set(i, "铁杆粉丝", fans[1]);

            res.set(i, "历史趋势",
                    StrUtil.format("https://guba.eastmoney.com/rank/stock?code={}", res.get(i, "代码").toString()));
            res.set(i, "相关",
                    StrUtil.format("https://guba.eastmoney.com/list,{}.html", res.get(i, "代码").toString()));
        }
    }

    private static void getOnePage(DataFrame<Object> res, int i) throws Exception {
        if (i != 0) {
            driver.findElement(By.linkText("下一页")).click(); // 点击4次下一页
        } // 刷新以后, 所有元素更新, 需要重新获取. 否则报错: 元素过期

        ExpectedConditions.presenceOfElementLocated(By.className("rank_table"));
        WebElement rankTable = driver.findElement(By.className("rank_table"));
        WebElement stockBody = rankTable.findElement(By.className("stock_tbody"));
        ExpectedConditions.textToBe(By.xpath("//*[@id=\"rankCont\"]/div[1]/div[2]/table/tbody/tr[20]/td[1" +
                "]"), Integer.valueOf(20 * (i + 1)).toString()); // 等待最后一行的 排名数值正确刷新!!!

        WebElement firstLine = stockBody.findElement(By.tagName("tr"));
        List<String> firstLineContent =
                firstLine.findElements(By.tagName("td")).stream().map(WebElement::getText).collect(Collectors.toList());
        if (firstLineContent.contains("--") || firstLineContent.contains("")) {
            // 此时表示第一行显示有缺失, 这是常见的bug, 由调用方解决
            throw new Exception("第一行数据渲染缺失");
        }


        List<WebElement> stockList = stockBody.findElements(By.tagName("tr"));
        for (WebElement webElement : stockList) {
            List<WebElement> elements = webElement.findElements(By.tagName("td"));
            res.append(elements.stream().map(WebElement::getText).collect(Collectors.toList()));
        }
    }
}
