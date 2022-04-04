package com.scareers.datasource.ths.wencai;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.ths.ThsConstants;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * description: 问财web, 相关的数据api, 非问财问句查询的数据api
 *
 * @author: admin
 * @date: 2022/4/2/002-03:13:18
 */
public class WenCaiDataApi {
    public static void main(String[] args) {
        DataFrame<Object> lastNDailyKline = getLastNKline(48, "885311", 2, 0, 1000);
        Console.log(lastNDailyKline);
        Console.log(lastNDailyKline.length());
        Console.log(lastNDailyKline.size());
//
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3; j++) {
//
//                HashMap<String, Object> lastDayBaseDatas = getLastDayBaseDatas(17, "600018", i, j);
//                Console.log(lastDayBaseDatas);
//            }
//        }
//        for (int i = 0; i < 100; i++) {
//
//            DataFrame<Object> fs1M = getFS1M(32, "399001");
//            Console.log(fs1M.length());
//        }

//        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("指数类型排序;同花顺概念指数取反;同花顺行业指数取反;");
//        Console.log(dataFrame.columns());
//        Console.log(dataFrame.length());
//        // [指数@指数类型, 指数@涨跌幅:前复权[20220401], code, 指数@收盘价:不复权[20220401], 指数简称, market_code, 指数代码]
//
//        List<String> colAsStringList = DataFrameS.getColAsStringList(dataFrame, "指数@指数类型");
//        HashSet<String> types = new HashSet<>(colAsStringList);
//        Console.log(types);
//
//        List<String> names = DataFrameS.getColAsStringList(dataFrame, "指数简称");
//        Console.log(names.contains("细分化工"));
//        Console.log(dataFrame.toString(10000));
    }

    /**
     * 获取最后n条 k线, 可指定 日/周/月 线, 以及 前/后/不/复权
     * 需要提供 市场代码, 6位资产代码, lastN
     * k线类别: 0/1/2 日/周/月
     * 复权类别: 0/1/2 不/前/后
     * http://d.10jqka.com.cn/v6/line/48_885311/01/last3600.js?hexin-v=A4ynRRxHApirARbk2T_l9L24XeG7xSnuMmdFO-ZpmyjbYyLfDtUA_4J5FIs1
     * http://d.10jqka.com.cn/v6/line/33_300086/11/last3600.js?hexin-v=A_DbMYDbZsVvBDqww3_BaPlMwbVHOcyF9gEotuo_vhUfOZ7rkkmkE0Yt-Jc5
     * http://d.10jqka.com.cn/v6/line/33_300086/21/last3600.js?hexin-v=A_DbMYDbZsVvBDqww3_BaPlMwbVHOcyF9gEotuo_vhUfOZ7rkkmkE0Yt-Jc5
     * http://d.10jqka.com.cn/v6/line/33_300575/22/last3600.js?hexin-v=A2hD2Vhznj2X7LLIi1XJALG0OV1_kdVzruugLiLnt4_XoQZDyqGcK_4
     * http://d.10jqka.com.cn/v6/line/33_002314/20/last3600.js?hexin-v=A9T_nYTvSrEjSN6MR1K9HKWQpRlDLeOXOnGMam7LY7TjrnoHlj3Ip4phXHq9
     * new DataFrame<>(Arrays.asList("日期", "开盘", "最高", "最低", "收盘", "成交量", "成交额"));
     *
     *     	      日期	      开盘	      最高	      最低	      收盘	        成交量	              成交额
     *    0	20070831	976.728 	1032.163	940.157 	1017.768	1787555400 	32589345000.000
     *    1	20070928	1028.107	1123.050	1004.033	1113.713	2044824800 	39127837000.000
     *    2	20071031	1138.843	1138.984	913.064 	1000.847	1374504900 	36482250000.000
     *    3	20071130	1000.338	1000.338	858.067 	912.622 	1035683990 	20753044000.000
     *
     * @param marketCode 资产都有 marketCode 属性, 必须指定
     * @param simpleCode 简单6位代码
     * @param klineType  k线类别: 0/1/2 日/周/月
     * @param fqType     复权类别: 0/1/2 不/前/后
     * @param lastN      最后多少条记录?
     * @return
     */
    public static DataFrame<Object> getLastNKline(int marketCode, String simpleCode, int klineType, int fqType,
                                                  int lastN) {
        String newVCode = ThsConstants.getNewVCode();
        String url = StrUtil.format("http://d.10jqka.com.cn/v6/line/{}_{}/{}{}/last{}" +
                ".js?hexin-v={}", marketCode, simpleCode, klineType, fqType, lastN, newVCode);


        HttpRequest request = new HttpRequest(url);
        request.setMethod(Method.GET);
        request.header("Accept", "*/*");
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Connection", "keep-alive");
        request.header("host", "d.10jqka.com.cn");
        request.header("Referer", "http://www.iwencai.com/");
        request.header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");

        String body = null;
        try {
            body = request.execute().body();
        } catch (HttpException e) {
            e.printStackTrace();
            return null;
        }

        DataFrame<Object> res = new DataFrame<>(Arrays.asList("日期", "开盘", "最高", "最低", "收盘", "成交量", "成交额"));
        int start = 0, end = body.length();
        start = body.indexOf("(") + "(".length();
        end = body.lastIndexOf(")");
        body = body.substring(start, end);
        JSONObject json = JSONUtilS.parseObj(body);
        String data = json.getString("data");
        // 该数据为字符串, 以";" 分割每条数据
        // 单条: 20180222,1573.842,1593.511,1573.592,1590.686,477520710,5544183400.000,,,,0;
        // 日期,开盘,最高,最低,收盘 成交量,成交额, 后面的字段无视

        List<String> rows = StrUtil.split(data, ";");
        for (String row : rows) {
            List<String> colValues = StrUtil.split(row, ",");
            res.append(colValues.subList(0, 7));
        }
        return res;

    }

    /**
     * 获取资产最后一交易日 最新几项数据.
     * 同样可提供 周期和复权
     * 结果: {成交量=1.7364341E9, 最低=2011.439, 开盘=2026.069, 成交额=1.9293401E10, 市场类型描述=, 最高=2029.666, 日期=20220401, 名称=智能电网,
     * 收盘=2021.187}
     * http://d.10jqka.com.cn/v6/line/33_000632/20/today.js?hexin-v=AxswMAfS_ZTQPQERpphKGRbFqnSA8DLgKSXzlw0n9HgxZDVqlcC_QjnUg24e
     *
     * @param marketCode 资产都有 marketCode 属性, 必须指定
     * @param simpleCode 简单6位代码
     * @param klineType  k线类别: 0/1/2 日/周/月
     * @param fqType     复权类别: 0/1/2 不/前/后
     * @return
     */
    public static HashMap<String, Object> getLastDayBaseDatas(int marketCode, String simpleCode, int klineType,
                                                              int fqType) {
        String newVCode = ThsConstants.getNewVCode();
        String url = StrUtil.format("http://d.10jqka.com.cn/v6/line/{}_{}/{}{}/today" +
                ".js?hexin-v={}", marketCode, simpleCode, klineType, fqType, newVCode);


        HttpRequest request = new HttpRequest(url);
        request.setMethod(Method.GET);
        request.header("Accept", "*/*");
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Connection", "keep-alive");
        request.header("host", "d.10jqka.com.cn");
        request.header("Referer", "http://www.iwencai.com/");
        request.header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");

        String body = null;
        try {
            body = request.execute().body();
        } catch (HttpException e) {
            e.printStackTrace();
            return null;
        }
        HashMap<String, Object> res = new HashMap<>();
        int start, end;
        start = body.indexOf("(") + "(".length();
        end = body.lastIndexOf(")");
        body = body.substring(start, end);
        JSONObject json = JSONUtilS.parseObj(body);
        JSONObject data = json.getJSONObject(StrUtil.format("{}_{}", marketCode, simpleCode));
        /*
        {
        "1": "20220401",
        "7": "7.90",
        "8": "8.43",
        "9": "7.25",
        "11": "8.43",
        "13": 208807560,
        "19": "1649174600.00",
        "74": "",
        "1968584": "7.322",
        "66": null,
        "open": 0,
        "dt": "0408",
        "name": "\u4fe1\u8fbe\u5730\u4ea7",
        "marketType": ""
         */
        res.put("日期", data.getString("1"));
        res.put("开盘", data.getDouble("7"));
        res.put("最高", data.getDouble("8"));
        res.put("最低", data.getDouble("9"));
        res.put("收盘", data.getDouble("11"));
        res.put("成交量", data.getDouble("13"));
        res.put("成交额", data.getDouble("19"));
        res.put("名称", data.getString("name"));
        res.put("市场类型描述", data.getString("marketType"));
        return res;

    }

    /**
     * 1分钟分时图; 多用于 概念/指数, 个股用东财
     * 注意: 这里的均价, 概念/行业指数时, 是简单的 成交额/成交量; 个股更是
     * http://d.10jqka.com.cn/v6/time/151_831305/last.js?hexin-v=A6iDGRgzXgzCxXKITQsJwPF0eZ2_0Q327jfgX2LS9lIXy0aDCuHcaz5FsPex
     */
    public static DataFrame<Object> getFS1M(int marketCode, String simpleCode) {
        String newVCode = ThsConstants.getNewVCode();
        String url = StrUtil
                .format("http://d.10jqka.com.cn/v6/time/{}_{}/last.js?hexin-v={}",
                        marketCode, simpleCode, newVCode);
        HttpRequest request = new HttpRequest(url);
        request.setMethod(Method.GET);
        request.header("Accept", "*/*");
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Connection", "keep-alive");
        request.header("host", "d.10jqka.com.cn");
        request.header("Referer", "http://www.iwencai.com/");
        request.header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36");

        String body = null;
        try {
            body = request.execute().body();
        } catch (HttpException e) {
            e.printStackTrace();
            return null;
        }
        DataFrame<Object> res = new DataFrame<>(Arrays.asList("时间", "价格", "成交额", "均价", "成交量"));
        int start, end;
        start = body.indexOf("(") + "(".length();
        end = body.lastIndexOf(")");
        body = body.substring(start, end);
        JSONObject json = JSONUtilS.parseObj(body);
        JSONObject data = json.getJSONObject(StrUtil.format("{}_{}", marketCode, simpleCode));
        String datas = data.getString("data"); // 数据
        // 又是;+, 的方式
        List<String> rows = StrUtil.split(datas, ";");
        for (String row : rows) {
            // 0930,95.14,15669558,95.140,164700;
            List<String> stringList = StrUtil.split(row, ",");
            String timeStr = stringList.get(0);
            timeStr = timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4);
            stringList.set(0, timeStr);
            res.append(stringList);
        }

        return res;

    }
}
