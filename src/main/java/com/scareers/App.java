package com.scareers;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.EastMoneyUtil;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import org.jdesktop.swingx.JXButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;


/**
 * Hello world!
 */
public class App {

    public static long fibonacci(long number) {
        if ((number == 0) || (number == 1)) {
            return 1;
        } else {
            return fibonacci(number - 1) + fibonacci(number - 2);
        }
    }

    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) throws Exception {
        String xx = EastMoneyUtil.getAsStrUseHutool("http://stock.10jqka.com.cn/jiepan_list/20220614/", null, 3000, 2);
        Console.log(xx);
//
//        String convert = CharsetUtil.convert("鍙戝竷鍏", "GBK", "UTF-8");
//        Console.log(convert);


        String x = PinyinUtil.getPinyin("同花顺", ",");
        Console.log(x);


//        URL resource = ResourceUtil.getResource("ths/nuclear/nuclear/");
//        Console.log(resource.getPath().substring(1));

//        String x = CommonUtil.getFullPathOfClassPathFileOrDir("ths/nuclear/nuclear/revise");
////        String x = CommonUtil.getFullPathOfClassPathFileOrDir("ths/nuclear/nuclear/raw");
//        Console.log(x);
//
//        FileUtil.copyFilesFromDir(FileUtil.file(x), FileUtil.file("C:\\Users\\admin\\Desktop\\自设键盘"), true);


//        FileUtil.copyFilesFromDir()


//        String s = Character.toString(0x30);
//        Console.log(s);
//
//        JButton btn = new JButton("Test");
//        InputMap im = btn.getInputMap();
//        for (KeyStroke ik : im.allKeys()) {
//            System.out.println(ik + " = " + im.get(ik));
//        }
//
//        Action blankAction = new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//            }
//        };
//
//        ActionMap am = btn.getActionMap();
//        am.put("pressed", blankAction);
//        am.put("released", blankAction);


//        DateTime date = DateUtil.date(1646150400);
//        Console.log(date);

//        Console.log(JSONUtilS.parseObj(
//                "{\"MsgId\":\"Msg@@XGSG@@2022-03-07 17\",\"PushTime\":1646643751600,\"Type\":5,\"Data\":[{\"securitycode\":\"688282\",\"purchasedate\":\"2022-03-08 00:00:00\",\"subcode\":\"787282\",\"securityshortname\":\"理工导航\",\"issueprice\":\"65.21\",\"peissuea\":\"80.87\",\"wszqjkr\":\"2022-03-10 00:00:00\",\"Url\":\"http://data.eastmoney.com/xg/xg/default.html\"},{\"securitycode\":\"688150\",\"purchasedate\":\"2022-03-08 00:00:00\",\"subcode\":\"787150\",\"securityshortname\":\"莱特光电\",\"issueprice\":\"22.05\",\"peissuea\":\"133.71\",\"wszqjkr\":\"2022-03-10 00:00:00\",\"Url\":\"http://data.eastmoney.com/xg/xg/default.html\"},{\"securitycode\":\"301219\",\"purchasedate\":\"2022-03-08 00:00:00\",\"subcode\":\"301219\",\"securityshortname\":\"腾远钴业\",\"issueprice\":\"173.98\",\"peissuea\":\"63.05\",\"wszqjkr\":\"2022-03-10 00:00:00\",\"Url\":\"http://data.eastmoney.com/xg/xg/default.html\"},{\"securitycode\":\"001308\",\"purchasedate\":\"2022-03-08 00:00:00\",\"subcode\":\"001308\",\"securityshortname\":\"康冠科技\",\"issueprice\":\"48.84\",\"peissuea\":\"22.99\",\"wszqjkr\":\"2022-03-10 00:00:00\",\"Url\":\"http://data.eastmoney.com/xg/xg/default.html\"}],\"ShieldName\":\"新股申购\",\"ShieldKey\":\"5\",\"Stock\":\"\"}"
//                ));
//
//        String s = ResourceUtil.readUtf8Str("bonds.txt");
//        List<String> split = StrUtil.split(s, "\r\n");
//        split.remove("");
//        Console.log(split);


//        Console.log(ObjectUtil.cloneByStream(Double.valueOf(1.00)));
//        System.out.println(String.format("%.2f", 1.234));
//        System.out.println("abc".getClass());
////        String[] fontNames= GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
////        Console.log(fontNames);
//        while (true) {
//
//            long start = System.currentTimeMillis();
////            fibonacci(40);
//            for (long i = 0; i < 1000000; i++) {
//                JSONUtilS.parseObj(
//                        "{\n" +
//                                "            \"code\": 200,\n" +
//                                "            \"success\": true,\n" +
//                                "            \"payload\": {\n" +
//                                "                \"features\": [\n" +
//                                "                    \"awesome\",\n" +
//                                "                    \"easyAPI\",\n" +
//                                "                    \"lowLearningCurve\"\n" +
//                                "                ]\n" +
//                                "            }\n" +
//                                "        }"
//
//                );
//
//            }
//            System.out.println(System.currentTimeMillis() - start);
//            break;
//        }


//        System.out.println(fibonacci(40));

//        String x = "\u51fa\u73b0\u4e86\u5f02\u5e38, \u5f02\u5e38\u6808" ;
//        System.out.println(x);
//
//        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
//        long id = snowflake.nextId(); // 雪花算法, 但是不同次运行能够生成相同id
//        Console.log(id);
//
//        String id2 = IdUtil.objectId();
//        Console.log(id2); // 类似于uuid version1, mongodb使用的id生成策略, 选用此种方式

//        DataFrame<Object> df_ = new DataFrame<>();
//        df_.add("a", ListUtil.of("abc", "xyz"));
//        df_.add("b", ListUtil.of("2.0", "1.0"));
//
//        Console.com.scareers.log(df_);
//        Console.com.scareers.log(df_.types());
//        List<Object> col1 = df_.col(0);
//        Console.com.scareers.log(col1.get(0) instanceof String);
//        HashMap<String, Double> temp = calc10ItemValusOfLowBuyDeprecated(3346000000.0, 19.43, "20210831",
//                ConnectionFactory.getConnLocalTushare1M(), "000002.SZ");
//        Console.com.scareers.log(JSONUtilS.toJsonPrettyStr(temp));

//        HashMap<String, Double> temp2 = calc5ItemValusOfHighSell(3346000000.0, 19.43, "20210901",
//                ConnectionFactory.getConnLocalTushare1M(), "000002.SZ", 4);
//        ArrayList<String> keys = new ArrayList<>(temp2.keySet());
//        keys.sort(Comparator.naturalOrder());
//
//        for (String key : keys) {
//            if (key.endsWith("happen_tick")) {
//                Console.com.scareers.log(StrUtilS.format("{} --> {}", key, fsTickDoubleParseToTimeStr(temp2.get(key))));
//                continue;
//            }
//            Console.com.scareers.log(StrUtilS.format("{} --> {}", key, temp2.get(key)));
//        }
//        Console.com.scareers.log(RandomUtil.randomInt(10));

//        Robot robot = new Robot();
//        robot.setSourcePath(App.class);

        //reportCpuMemoryDiskSubThread(true);

        //        List<String> x = Arrays.asList("abc", "xyz");

//        HashMap<String, HashMap<String, Double>> x = new HashMap<>();
//        x.put("xx", new HashMap<>());
//        x.get("xx").put("yy", 0.1);
//        Console.com.scareers.log(x);

//
//        MailUtil.send(SettingsCommon.receivers,
//                "硬件信息: ",
//                reportCpuMemoryDisk(true),
//                false, null);
// SF1141606309289
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//        System.gc();
//        Map<Integer, String> passedMap = new HashMap<>();
//        for (int i = 0; i < 1000000; i++) {
//            passedMap.put(i, "000000");
//        }
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//        for (int i = 0; i < 1000000; i++) {
//            passedMap.remove(i);
//        }
//        System.gc();
//        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
////
//
//        DataFrame<Double> dfo = new DataFrame<>("a");
//        ArrayList<Double> row = new ArrayList<>();
//        for (int i = 0; i < 100; i++) {
//            row.add(RandomUtil.randomDouble());
//        }
//        dfo.append(row);
////        dfo.show();
//        dfo.plot(DataFrame.PlotType.LINE);

//        for (int i : Tqdm.tqdm(Arrays.asList(1, 2, 3, 4, 5, 6), "iterating")) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        Object x = "abc";
//        Console.com.scareers.log(x instanceof String);
//        Double nan = Double.NaN;
//        Console.com.scareers.log(Double.compare(1, Double.NaN));
//
//        Console.com.scareers.log(JSONUtilS.toJsonStr((Object) Arrays.asList(0.25)));
//        Console.com.scareers.log(JSONUtilS.toJsonStr(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4))));
//        Console.com.scareers.log(Math.pow(2.1867241478865562, 5));
//
//        DataFrame<Double> df = new DataFrame<>();
//        df.add(Arrays.asList(1.0));
//        df = df.apply(value -> {
//            return value + 1;
//        });
//        Console.com.scareers.log(df);
//        Console.com.scareers.log(df.kurt());


//        DataFrameS<Object> df = new DataFrameS<>("a", "b");
//        Connection conn = ConnectionFactory.getConnLocalKlineForms();

//        df.append(Arrays.asList(1, 2));
//        df.append(Arrays.asList(3, 4));
//        Console.com.scareers.log(df.mean());
//        Console.com.scareers.log(df.mean().getClass().getName());
//        Console.com.scareers.log(df.mean().get(0, 0));
//        Console.com.scareers.log(df.skew().get(0, 0).getClass().getName());
//        Console.com.scareers.log(df.skew().get(0, 0).getClass().getName());


//        System.out.println(df.col("a")); // 注意无法直接打印 df, 只能打印 行,列 List,元素Object,  等等
//        System.out.println(df.getColumnsAsString());
//        System.out.println(df.getIndexesAsString()); //
//        System.out.println(df.columns());
//        System.out.println(df.index());  // 本质是 Map 的key, 组成的 keySet属性:  Set<K> 对象
//
//        df.toSql("test", conn, "replace", "create table if not exists test(a int null,b int null)");
//        conn.close(); // 自行封装的都并没有close, 注意手动关闭; 跟python稍有不同
//
//        HttpRequest req = HttpUtil.createGet("https://www.baidu.com");
//        HttpResponse res = req.execute();
////        System.out.println(res.body());
//
//        String zodiac = DateUtil.getZodiac(Month.OCTOBER.getValue(), 22);
//        System.out.println(zodiac);
//        System.out.println(DateUtil.getChineseZodiac(1990));
//        Console.com.scareers.log(new Date());
//
//
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(System.getProperty("java.class.path"));
//
//        System.out.println(ResourceUtil.readUtf8Str("config/test.txt"));
//
//        ClassPathResource resource = new ClassPathResource("test.properties");
//        Properties properties = new Properties();
//        properties.load(resource.getStream());
//        Console.com.scareers.log("Properties: {}", properties);
//
//        String str = "abcde" +
//                "fgh" + (1 ^ 2);
//        String strSub1 = StrUtilS.sub(str, 2, 3); //strSub1 -> c
//        String strSub2 = StrUtilS.sub(str, 2, -3); //strSub2 -> cde
//        String strSub3 = StrUtilS.sub(str, 3, 2); //strSub2 -> c
//        System.out.println(strSub1);
//
////        System.out.println(RuntimeUtil.execForStr("systeminfo"));
//
//        System.out.println(Runtime.getRuntime().availableProcessors());
//        System.out.println(Runtime.getRuntime().maxMemory());
//        System.out.println(Runtime.getRuntime().freeMemory());
//        System.out.println(Runtime.getRuntime().totalMemory());


    }


}


