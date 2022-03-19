package com.scareers.datasource.ths.wencai;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import eu.verdelhan.ta4j.indicators.helpers.AmountIndicator;
import joinery.DataFrame;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * description: 问财破解尝试. 相关常用查询api.
 *
 * @noti 日期全部使用字符串表示, 必须保证能被hutool解析(后再标准化)
 * @noti : 问句均应当使用问财解析过的 标准问法!!!
 * @author: admin
 * @date: 2022/3/19/019-01:03:16
 */
public class WenCaiApi {
    public static void main(String[] args) throws Exception {
        TimeInterval timer = DateUtil.timer();
        timer.start();
//        Console.log(getPopularityRaisingRanking(DateUtil.date(), 100, 200));
        Console.log(timer.intervalRestart());

        Console.log(getReachDailyHighLimitOf("20220316",  true));

        Console.log(timer.intervalRestart());


        Console.log(timer.intervalRestart());

        Console.log(timer.intervalRestart());
    }

    public static String vCode; // 只需要调用一次初始化,

    /**
     * 执行js代码, 初始化vCode; 常态仅需要调用一次;
     *
     * @return
     * @throws Exception
     */
    private static String getVCode() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("js"); // 得到脚本引擎

        String str = ResourceUtil.readUtf8Str("ths/wencai/ths.js"); // 将会自动查找类路径下; 别绝对路径
        engine.eval(str);
        Invocable inv = (Invocable) engine;
        Object test2 = inv.invokeFunction("v");
        vCode = test2.toString();
        return test2.toString();
    }

    /**
     * 死循环初始化
     */
    private static void checkVCode() {
        while (vCode == null) {
            try {
                vCode = getVCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * @return 访问http失败, 或者结果数据项为空, 或者json解析失败, json解析null指针异常, 均返回null
     * @key3 核心问财api, 各实用api均调用此api, 各自对表头进行部分解析!
     * 一般而言数据量都较大, 人工智能计算也较慢, 因此问财api速度不快. 100ms - 1s 级别, 达不到毫秒级
     * data_json["data"]["answer"][0]["txt"][0]["content"]["components"][0]["data"][
     * "datas"
     * ]  为列表, 单项为 {}, key为表头, value为值
     */
    public static DataFrame<Object> wenCaiQuery(String question, int perPage, int page) {
        checkVCode();
        String url = "http://www.iwencai.com/unifiedwap/unified-wap/v2/result/get-robot-data";
        HttpRequest request = new HttpRequest(url);
        request.setMethod(Method.POST);
        request.header("host", "www.iwencai.com");
        request.header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36");
        request.header("Accept-Encoding", "gzip");
        request.header("Accept", "*/*");
        request.header("Connection", "keep-alive");
        request.header("hexin-v", vCode);
        request.header("Content-Type", "application/x-www-form-urlencoded");

        HashMap<String, Object> params = new HashMap<>();
        params.put("question", question);
        params.put("perpage", perPage);
        params.put("page", page);
        params.put("secondary_intent", "");
        params.put("log_info", "{\"input_type\":\"click\"}");
        params.put("source", "Ths_iwencai_Xuangu");
        params.put("version", "2.0");
        params.put("query_area", "");
        params.put("block_list", "");
        params.put("add_info", "{\"urp\":{\"scene\":1,\"company\":1,\"business\":1},\"contentType\":\"json\"}");
        request.form(params);

        String body = null;
        try {
            body = request.execute().body();
        } catch (HttpException e) {
            e.printStackTrace();
            return null;
        }
        JSONObject jsonObject = JSONUtilS.parseObj(body);
        JSONArray datas = null;
        try {
            datas = jsonObject.getJSONObject("data").getJSONArray("answer").getJSONObject(0)
                    .getJSONArray("txt").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("components").getJSONObject(0).getJSONObject("data")
                    .getJSONArray("datas");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (datas.size() == 0) {
            return null;
        }

        List<String> headers = new ArrayList<>(datas.getJSONObject(0).keySet());
        DataFrame<Object> res = new DataFrame<>(headers);

        for (int i = 0; i < datas.size(); i++) {
            JSONObject jsonObject1 = datas.getJSONObject(i);
            List<Object> row = new ArrayList<>();
            for (String header : headers) {
                row.add(jsonObject1.get(header));
            }
            res.append(row);
        }
        return res;
    }

    /**
     * 默认单页
     *
     * @param question
     * @return
     */
    public static DataFrame<Object> wenCaiQuery(String question) {
        return wenCaiQuery(question, 100000, 1);
    }

    /*
    热度排名 api
     */

    /**
     * 1.标准问句: 2022年03月17日个股热度排名从小到大排名前100
     * 2.当不指定日期, 将自动为今天
     * 个股热度排名从小到大: 即最热的在前!
     * 可指定日期 Date, 将转换为 yyyy年MM月dd日
     *
     * @return
     * @cols 个股热度排名排名[20220319]      code	股票简称	    最新价	  最新涨跌幅	market_code	个股热度排名[20220319]	     股票代码
     * market_code: 市场代码, 默认 沪17,深33
     */
    public static DataFrame<Object> getPopularityRankingBetween(String date, int amount) {
        String question = StrUtil.format("{}个股热度排名从小到大排名前{}",
                DateUtil.format(DateUtil.parse(date),
                        DatePattern.CHINESE_DATE_PATTERN),
                amount);
        return wenCaiQuery(question);
    }

    /**
     * 当前最新热度排名前n
     *
     * @param amount
     * @return
     * @cols 个股热度排名排名[20220319]      code	股票简称	    最新价	  最新涨跌幅	market_code	个股热度排名[20220319]	     股票代码
     */
    public static DataFrame<Object> getNewestPopularityRanking(int amount) {
        String question = StrUtil.format("个股热度排名从小到大排名前{}", amount);
        return wenCaiQuery(question);
    }

    /**
     * 1.标准问句: 2022年3月17日的个股热度排名>100(4676个) 2022年3月17日的个股热度排名<=200(200个)
     * 2. 注意加上 "且" 表达且关系
     * 热度排名介于! 为了方便使用, 使用 [low,high]; 两者均包括, 因此应当 101-200;  1-100; 201-300等
     * 两段包含
     *
     * @cols code    股票简称	   最新价	 最新涨跌幅	market_code	个股热度排名[20220319]	     股票代码
     */
    public static DataFrame<Object> getPopularityRankingBetween(String date, int lowRank, int highRank) {
        String question = StrUtil
                .format("{}个股热度排名>={}且{}个股热度排名<={}",
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN),
                        lowRank,
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN),
                        highRank
                );
        return wenCaiQuery(question);
    }

    /**
     * @cols code    股票简称	   最新价	 最新涨跌幅	market_code	个股热度排名[20220319]	     股票代码
     */
    public static DataFrame<Object> getNewestPopularityRankingBetween(int lowRank, int highRank) {
        String question = StrUtil
                .format("个股热度排名>={}且个股热度排名<={}",
                        lowRank, highRank
                );
        return wenCaiQuery(question);
    }

    /**
     * 个股热度提升量: 某一个靠后的日期, 比起某一个靠前的日期, 热度上升量 排行榜 前 n! 且 另一个未来日期 个股热度排名 <=某值.
     * 1.标准: (2022年3月16日个股热度排名-2022年3月18日个股热度排名)>=1000且2022年3月18日个股热度排名<=1000;
     * 2.选股结果数量显然不确定
     *
     * @param preDate               某一个前面的日期
     * @param afterDate             某一个后面的日期
     * @param popularityRaisingGe   要求热度上升值>= 本阈值
     * @param popularityRequireDate 指定的某一日
     * @param popularityLe          要求指定日期热度排名靠前 n
     * @param excludeSt             是否排除st
     * @return
     * @cols code    股票简称	   最新价	  最新涨跌幅	{(}个股热度排名[20220316]{-}个股热度排名[20220318]{)}	个股热度排名[20220318]	market_code	个股热度排名[20220316]	     股票代码
     */
    public static DataFrame<Object> getPopularityRaisingWithLimit(String preDate, String afterDate,
                                                                  int popularityRaisingGe,
                                                                  String popularityRequireDate, int popularityLe,
                                                                  boolean excludeSt) {
        String question = StrUtil
                .format("({}个股热度排名-{}个股热度排名)>{}且{}个股热度排名<={}",
                        DateUtil.format(DateUtil.parse(preDate), DatePattern.CHINESE_DATE_PATTERN),
                        DateUtil.format(DateUtil.parse(afterDate), DatePattern.CHINESE_DATE_PATTERN),
                        popularityRaisingGe,
                        DateUtil.format(DateUtil.parse(popularityRequireDate), DatePattern.CHINESE_DATE_PATTERN),
                        popularityLe
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /**
     * 简化版: 今日相比昨日热度提升量排名前n, 且可限制今日热度<=阈值.
     * 0.语句: (昨日个股热度排名-今日个股热度排名)从大到小排名前100且今日个股热度<=10000
     * 1.固定返回 n个结果
     * 2.将优先计算 今日热度<=阈值., 在此基础上, 再对热度提升量 排名, 因此要热度提升量最大, 建议 今日热度限制阈值足够高
     *
     * @param preDate
     * @param afterDate
     * @param popularityRaisingGe
     * @param popularityRequireDate
     * @param popularityLe
     * @param excludeSt
     * @return
     * @cols code    {(}个股热度排名[20220318]{-}个股热度排名排名[20220319]{)}	个股热度[20220319]	股票简称	{(}个股热度排名[20220318]{-}个股热度排名[20220319]{)}	个股热度排名[20220318]	market_code	个股热度排名[20220319]	     股票代码
     */
    public static DataFrame<Object> getPopularityRaisingTodayToYesterday(int amount,
                                                                         int popularityLe,
                                                                         boolean excludeSt) {
        String question = StrUtil
                .format("(昨日个股热度排名-今日个股热度排名)从大到小排名前{}且今日个股热度<={}",
                        amount, popularityLe
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /*
    涨幅和涨停板
     */

    /**
     * 近期多少天, 涨幅排名 前x名
     * 1.标准语句: 近20日的区间涨跌幅排名前100
     *
     * @param amount
     * @param nDay
     * @return
     * @cols 区间涨跌幅:前复权排名[20220228-20220318]	  code	股票简称	区间涨跌幅:前复权[20220228-20220318]	   最新价	  最新涨跌幅	market_code	     股票代码
     */
    public static DataFrame<Object> getChgPRankOfNDay(int amount, int nDay, boolean excludeSt) {
        String question = StrUtil
                .format("近{}日的区间涨跌幅排名前{}",
                        nDay, amount
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /**
     * 近期 涨停板数量 从多到少排名 前xx;
     * 1.标准语句: 近30天涨停次数从大到小前100
     *
     * @param amount
     * @param nDay
     * @param excludeSt
     * @return
     * @cols 区间振幅[20220228-20220318]	  code	涨停次数排名[20220228-20220318]	涨停次数[20220228-20220318]	股票简称	   最新价	  最新涨跌幅	区间成交额[20220228-20220318]	market_code	     股票代码
     */
    public static DataFrame<Object> getReachDailyHighLimitAmountOfNDay(int amount, int nDay, boolean excludeSt) {
        String question = StrUtil
                .format("近{}天涨停次数从大到小前{}",
                        nDay, amount
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /**
     * 获取某一日 的 n连板,  n>=1 , n==1即为首板
     * 1. 2022年3月16日的连续涨停天数=2日(11个)
     *
     * @param date
     * @param dailyHighLimitAmount
     * @param excludeSt
     * @return
     * @cols a股市值(不含限售股)[20220315]      code	股票简称	  最新价	 最新涨跌幅	连续涨停天数[20220315]	market_code	     股票代码
     */
    public static DataFrame<Object> getNContinuousReachDailyHighLimitOf(String date, int dailyHighLimitAmount,
                                                                        boolean excludeSt) {
        String question = StrUtil
                .format("{}的连续涨停天数={}日",
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN), dailyHighLimitAmount
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /**
     * 某一日连板数量 >= n, 方便查看高连板
     * 1. 2022年3月16日的连续涨停天数>=2日
     *
     * @param date
     * @param dailyHighLimitAmount
     * @param excludeSt
     * @return
     * @cols a股市值(不含限售股)[20220318]    a股市值(不含限售股)[20220316]	  code	{(}连续涨停天数[20220316]{-}连续涨停天数[20220318]{)}	 股票简称	连续涨停天数[20220318]	    最新价	连续涨停天数[20220317]	连续涨停天数[20220316]	  最新涨跌幅	market_code	     股票代码
     */
    public static DataFrame<Object> getContinuousReachDailyHighLimitGtN(String date, int dailyHighLimitAmountGe,
                                                                        boolean excludeSt) {
        String question = StrUtil
                .format("{}的连续涨停天数>={}日",
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN), dailyHighLimitAmountGe
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }

    /**
     * 某日全部涨停, 默认以 涨停封单额 排序
     * 1.2022年03月18日涨停封单额排名;2022年03月18日涨停;非st
     * 2.@noti: 字段极多
     * @param date
     * @param dailyHighLimitAmountGe
     * @param excludeSt
     * @return
     * @cols a股市值(不含限售股)[20220316]      code	涨停封单量占流通a股比[20220316]	涨停封单量占成交量比[20220316]	   最新价	涨停封单额排名[20220316]	    涨停原因类别[20220316]	  最新涨跌幅	涨停封单量[20220316]	market_code	首次涨停时间[20220316]	     股票代码	    涨停封单额[20220316]	涨停[20220316]	    涨停明细数据[20220316]	几天几板[20220316]	股票简称	最终涨停时间[20220316]	连续涨停天数[20220316]	   涨停类型[20220316]	涨停开板次数[20220316]
     */
    public static DataFrame<Object> getReachDailyHighLimitOf(String date,
                                                                        boolean excludeSt) {
        String question = StrUtil
                .format("{}涨停封单额排名;{}涨停",
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN),
                        DateUtil.format(DateUtil.parse(date), DatePattern.CHINESE_DATE_PATTERN)
                );
        if (excludeSt) {
            question += ";非st";
        }
        return wenCaiQuery(question);
    }


}
