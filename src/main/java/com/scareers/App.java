package com.scareers;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Month;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.Tqdm;
import joinery.DataFrame;


import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.CalcStatResultAndSaveTask
// .calcVirtualGeometryMeanRecursion;


/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws Exception {

        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        System.gc();
        Map<Integer, String> passedMap = new HashMap<>();
        for (int i = 0; i < 1000000; i++) {
            passedMap.put(i, "000000");
        }
        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        for (int i = 0; i < 1000000; i++) {
            passedMap.remove(i);
        }
        System.gc();
        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//



//        for (int i : Tqdm.tqdm(Arrays.asList(1, 2, 3, 4, 5, 6), "iterating")) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        Object x = "abc";
//        Console.log(x instanceof String);
//        Double nan = Double.NaN;
//        Console.log(Double.compare(1, Double.NaN));
//
//        Console.log(JSONUtil.toJsonStr((Object) Arrays.asList(0.25)));
//        Console.log(JSONUtil.toJsonStr(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4))));
//        Console.log(Math.pow(2.1867241478865562, 5));
//
//        DataFrame<Double> df = new DataFrame<>();
//        df.add(Arrays.asList(1.0));
//        df = df.apply(value -> {
//            return value + 1;
//        });
//        Console.log(df);
//        Console.log(df.kurt());


//        DataFrameSelf<Object> df = new DataFrameSelf<>("a", "b");
//        Connection conn = ConnectionFactory.getConnLocalKlineForms();

//        df.append(Arrays.asList(1, 2));
//        df.append(Arrays.asList(3, 4));
//        Console.log(df.mean());
//        Console.log(df.mean().getClass().getName());
//        Console.log(df.mean().get(0, 0));
//        Console.log(df.skew().get(0, 0).getClass().getName());
//        Console.log(df.skew().get(0, 0).getClass().getName());


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
//        Console.log(new Date());
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
//        Console.log("Properties: {}", properties);
//
//        String str = "abcde" +
//                "fgh" + (1 ^ 2);
//        String strSub1 = StrUtil.sub(str, 2, 3); //strSub1 -> c
//        String strSub2 = StrUtil.sub(str, 2, -3); //strSub2 -> cde
//        String strSub3 = StrUtil.sub(str, 3, 2); //strSub2 -> c
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






















