package com.scareers.datasource.eastmoney.quotecenter.bean;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;

/**
 * description: 表示指数或板块实时盘口各项数据的对象. rawJson保存了原始 json 对象.
 * 全字段参考下文, 但只保存部分字段, 且顺序类似 东财行情页面
 * 1.5档盘口的金额由计算得到
 * 2.包含5档盘口和16项常用盘口数据+委差委比; 不包含对应可转债 以及 基本面数据;
 * 3.字段若解析失败, 调用get将得到null, 而非使用 -1.0填充
 *
 * @author: admin
 * @date: 2022/2/14/014-17:54:01
 * @see EmQuoteApi.getStockHandicap() 访问实时盘口
 */
@Getter // 仅可获取属性
@ToString
public class IndexBkHandicap extends Handicap {
    public static HashMap<String, String> fieldsMap; // 当前使用的字段 及 对应描述.
    public static String fieldsStr; // 字段字符串. , 分割

    static {
        initFieldsMap();
        initFieldsStr();
    }

    /*
        IndexBkHandicap(
        dateTime=2022-02-16 19:07:37,
        secCode=BK1030,
        stockName=电机,

        newPrice=1402.38,
        highPrice=1405.74,
        lowPrice=1382.0,
        todayOpen=1386.77,
        totalVol=1862596.0,
        totalAmount=3.200639856E9,

        outerVol=954799.0,
        innerVol=907797.0,
        volRatio=0.81,
        preClose=1378.43,

        raisingStockCount=19.0,
        fallStockCount=1.0,
        flatStockCount=0.0,

        totalMarketValue=1.65554563E11,
        flowMarketValue=1.28519345E11,

        changePercentOf5Day=-1.5,
        changePercentOf20Day=-8.7,
        changePercentOf60Day=-9.93,
        changePercentOfThisYear=-12.18,

        changePercent=1.74,
        changeValue=23.95,
        turnoverRate=2.39,
        amplitude=1.72,

        rawJson={"f50":81,"f115":0,"f71":"-","f114":1,"f113":19,"f154":4,"f152":2,"f58":"电机","f57":"BK1030","f171":172,"f292":5,"f59":2,"f170":174,"f39":"-","f19":"-","f119":-150,"f117":128519345000,"f116":165554563000,"f60":137843,"f169":2395,"f85":7808567296,"f168":239,"f84":9346113792,"f167":"-","f40":"-","f43":140238,"f122":-1218,"f86":1644997202,"f121":-993,"f20":"-","f45":138200,"f120":-870,"f164":"-","f44":140574,"f47":1862596,"f46":138677,"f161":907797,"f49":954799,"f48":3200639856,"f108":"-","f107":90,"f92":"-","f601":"-","f600":"-"})



     */

    private static void initFieldsMap() {
        fieldsMap = new HashMap<>();
        fieldsMap.put("f57", "代码");
        fieldsMap.put("f58", "名称");

        fieldsMap.put("f43", "最新");
        fieldsMap.put("f44", "最高");
        fieldsMap.put("f45", "最低");
        fieldsMap.put("f46", "今开");
        fieldsMap.put("f47", "总手"); // 成交量
        fieldsMap.put("f48", "金额"); // 成交额
        fieldsMap.put("f49", "外盘");
        fieldsMap.put("f161", "内盘");
        fieldsMap.put("f50", "量比"); // ??
        fieldsMap.put("f60", "昨收");

        fieldsMap.put("f113", "涨家数");
        fieldsMap.put("f114", "跌家数");
        fieldsMap.put("f115", "平家数");

        fieldsMap.put("f116", "总市值");
        fieldsMap.put("f117", "流通市值");

        fieldsMap.put("f119", "5日涨跌幅");
        fieldsMap.put("f120", "20日涨跌幅");
        fieldsMap.put("f121", "60日涨跌幅");
        fieldsMap.put("f122", "今年涨跌幅");

        fieldsMap.put("f170", "涨幅");
        fieldsMap.put("f169", "涨跌");
        fieldsMap.put("f168", "换手率");
        fieldsMap.put("f171", "振幅");
    }

    /**
     * http://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&cb=jQuery351037846734899553613_1644751180897&fields=f58%2Cf107%2Cf57%2Cf43%2Cf59%2Cf169%2Cf170%2Cf152%2Cf46%2Cf60%2Cf44%2Cf45%2Cf47%2Cf48%2Cf19%2Cf532%2Cf39%2Cf161%2Cf49%2Cf171%2Cf50%2Cf86%2Cf600%2Cf601%2Cf154%2Cf84%2Cf85%2Cf168%2Cf108%2Cf116%2Cf167%2Cf164%2Cf92%2Cf71%2Cf117%2Cf292%2Cf113%2Cf114%2Cf115%2Cf119%2Cf120%2Cf121%2Cf122&secid=1.000001&ut=fa5fd1943c7b386f172d6893dbfba10b&_=1644751180898
     * 全部字段. 指数全字段, 板块少4, 但+4能正确返回
     */
    private static void initFieldsStr() {
        //fieldsStr = StrUtil.join(",", fieldsMap.keySet());
        fieldsStr = "f58,f107,f57,f43,f59,f169,f170,f152,f46,f60,f44,f45,f47,f48,f19,f532,f39,f161,f49,f171,f50,f86,f600,f601,f154,f84,f85,f168,f108,f116,f167,f164,f92,f71,f117,f292,f113,f114,f115,f119,f120,f121,f122";

    }

    String dateTime; // 由于该api没有时间字段. 自行在new时生成 yyyy-MM-dd HH:mm:ss 形式 DateUtil.now();
    // 资产代码,名称
    String secCode; // "f57": "000001", 指数/板块代码 "BK1030"
    String stockName; // "f58": "浦发银行", 名称

    // 16项常用盘口数据, dc布局为8*2. 这里按照从左到右, 从上到下顺序对应
    Double newPrice; // "f43": 8.74, 最新
    Double highPrice; // "f44": 8.76, 最高
    Double lowPrice; // "f45": 8.67, 最低
    Double todayOpen; // "f46": 8.7,  今开
    Double totalVol; // "f47": 376829, 总手
    Double totalAmount; // "f48": 328719168.0,  金额,
    Double outerVol; // "f49": 221532, 外盘 量
    Double innerVol; // "f161": 155297,  内盘
    Double volRatio; // "f50": 0.96,  量比
    Double preClose; // "f60": 8.69, 昨收

    Double raisingStockCount; // "f113", "涨家数"
    Double fallStockCount; // "f114", "跌家数"
    Double flatStockCount; // "f115", "平家数"

    Double totalMarketValue; // "f116", "总市值"
    Double flowMarketValue; // "f117", "流通市值"

    Double changePercentOf5Day; // "f119", "5日涨跌幅"
    Double changePercentOf20Day; // "f120", "20日涨跌幅"
    Double changePercentOf60Day; // "f121", "60日涨跌幅"
    Double changePercentOfThisYear; // "f122", "今年涨跌幅"


    Double changePercent; // "f170": 0.58, // 涨幅%
    Double changeValue; // "f169": 0.05,  涨跌价格值
    Double turnoverRate; // "f168": 0.13, 换手率 0.13%
    Double amplitude; // f171 振幅

    //{"f50":81,"f115":0,"f71":"-","f114":1,"f113":19,"f154":4,"f152":2,"f58":"电机","f57":"BK1030",
    // "f171":172,"f292":5,"f59":2,"f170":174,"f39":"-","f19":"-","f119":-150,"f117":128519345000,"f116":165554563000,"f60":137843,"f169":2395,"f85":7808567296,"f168":239,"f84":9346113792,"f167":"-","f40":"-","f43":140238,"f122":-1218,"f86":1644997202,"f121":-993,"f20":"-","f45":138200,"f120":-870,"f164":"-","f44":140574,"f47":1862596,"f46":138677,"f161":907797,"f49":954799,"f48":3200639856,"f108":"-","f107":90,"f92":"-","f601":"-","f600":"-"}
    JSONObject rawJson;

    public IndexBkHandicap(JSONObject rawJson) {
        this.rawJson = rawJson;
        this.dateTime = DateUtil.now();

        parseAttrs();
    }

    /**
     * 指数和板块的数据 , 比起个股, 多数字段需要 / 100, 但是需要检查一下null.
     *
     * @return
     */
    private static final Log log = LogUtil.getLogger();

    private Double tryParseDoubleAndDivide100(String field) {
        Double res = tryGetDoubleFromRawJson(field);
        if (res != null) {
            return res / 100.0;
        }
        return null;
    }

    private void parseAttrs() {
        this.secCode = rawJson.getString("f57");
        this.stockName = rawJson.getString("f58");

        newPrice = tryParseDoubleAndDivide100("f43");
        highPrice = tryParseDoubleAndDivide100("f44");
        lowPrice = tryParseDoubleAndDivide100("f45");
        todayOpen = tryParseDoubleAndDivide100("f46");
        totalVol = tryGetDoubleFromRawJson("f47");
        totalAmount = tryGetDoubleFromRawJson("f48");
        outerVol = tryGetDoubleFromRawJson("f49");
        innerVol = tryGetDoubleFromRawJson("f161");
        volRatio = tryParseDoubleAndDivide100("f50");
        preClose = tryParseDoubleAndDivide100("f60");

        raisingStockCount = tryGetDoubleFromRawJson("f113");
        fallStockCount = tryGetDoubleFromRawJson("f114");
        flatStockCount = tryGetDoubleFromRawJson("f115");

        totalMarketValue = tryGetDoubleFromRawJson("f116");
        flowMarketValue = tryGetDoubleFromRawJson("f117");

        changePercentOf5Day = tryParseDoubleAndDivide100("f119");
        changePercentOf20Day = tryParseDoubleAndDivide100("f120");
        changePercentOf60Day = tryParseDoubleAndDivide100("f121");
        changePercentOfThisYear = tryParseDoubleAndDivide100("f122");

        changePercent = tryParseDoubleAndDivide100("f170");
        changeValue = tryParseDoubleAndDivide100("f169");
        turnoverRate = tryParseDoubleAndDivide100("f168");
        amplitude = tryParseDoubleAndDivide100("f171");
    }


    private Double tryGetDoubleFromRawJson(String field) {
        return JSONUtilS.tryParseDoubleOrNull(rawJson, field);
    }


}

/*
 * {
 * "rc": 0,
 * "rt": 4,
 * "svr": 182995791,
 * "lt": 1,
 * "full": 1,
 * "data": {
 * "f43": 346295, 最新
 * "f44": 350015, 最高
 * "f45": 345933, 最低
 * "f46": 347228, 今开
 * "f47": 361356091, 成交量
 * "f48": 426297925632.0, 成交额
 * "f49": 168609531, 外盘
 * "f50": 107,
 * "f57": "000001",
 * "f58": "上证指数",
 * "f59": 2,
 * "f60": 348591, 昨收
 * "f71": "-",
 * "f84": 4629436239233.0,
 * "f85": 4061217883902.0,
 * "f86": 1644566379,
 * "f92": "-",
 * "f107": 1,
 * "f108": "-",
 * "f113": 285, 涨家数
 * "f114": 1765, 跌家数
 * "f115": 44, 平家数
 * "f116": 49300990877782.0,
 * "f117": 41749793711638.0, 流通市值
 * "f119": 302, 5日涨跌幅, /100百分比
 * "f120": -326, 20日涨跌幅
 * "f121": -198, 60日
 * "f122": -486, 今年
 * "f152": 2,
 * "f154": 4,
 * "f161": 192746560, 内盘
 * "f164": "-",
 * "f167": "-",
 * "f168": 89, 换手率, /100百分比, 0.89%
 * "f169": -2296, 涨跌点数, 注意 /100, 实际 22.96点
 * "f170": -66, 涨跌幅度, 同样 /100且百分比,  -0.66%
 * "f171": 117, 振幅, /100百分比  1.17%
 * "f292": 5,
 * "f39": "-",
 * "f40": "-",
 * "f19": "-",
 * "f20": "-",
 * "f600": "-",
 * "f601": "-"
 * }
 * }
 */