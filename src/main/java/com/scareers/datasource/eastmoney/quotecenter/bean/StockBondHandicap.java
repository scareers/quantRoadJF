package com.scareers.datasource.eastmoney.quotecenter.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.utils.JSONUtilS;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;

/**
 * description: 表示 个股/转债 实时盘口各项数据的对象. rawJson保存原始 json 对象.
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
public class StockBondHandicap extends Handicap {
    public static HashMap<String, String> fieldsMap; // 当前使用的字段 及 对应描述.
    public static String fieldsStr; // 字段字符串. , 分割

    static {
        initFieldsMap();
        initFieldsStr();
    }

    /* toString()
        StockBondHandicap(
            dateTime=2022-03-01 20:26:47,
            secCode=127007,
            secName=湖广转债,

            consignRatio=40.28,
            consignDifference=1790.0,

            sell5Price=146.98,
            sell5Vol=23.0,
            sell5Amount=338054.0,

            sell4Price=146.9,
            sell4Vol=1.0,
            sell4Amount=14690.0,

            sell3Price=146.855,
            sell3Vol=14.0,
            sell3Amount=205596.99999999997,

            sell2Price=146.479,
            sell2Vol=678.0,
            sell2Amount=9931276.200000001,

            sell1Price=146.451,
            sell1Vol=611.0,
            sell1Amount=8948156.1,

            buy1Price=146.4,
            buy1Vol=2609.0,
            buy1Amount=3.819576E7,

            buy2Price=146.37,
            buy2Vol=213.0,
            buy2Amount=3117681.0,

            buy3Price=146.365,
            buy3Vol=9.0,
            buy3Amount=131728.5,

            buy4Price=146.36,
            buy4Vol=246.0,
            buy4Amount=3600456.0000000005,

            buy5Price=146.35,
            buy5Vol=40.0,
            buy5Amount=585400.0,

            newPrice=146.4,
            avgPrice=149.011,
            changePercent=1.67,
            changeValue=2.4,
            totalVol=939722.0,
            totalAmount=1.400290656E9,
            turnoverRate=226.7,
            volRatio=1.37,
            highPrice=154.8,
            lowPrice=142.98,
            todayOpen=144.3,
            preClose=144.0,

            highLimitPrice=null, // 可转债无涨跌停
            lowLimitPrice=null,

            outerVol=472978.0,
            innerVol=466744.0,

            rawJson={"f137":11672290.0,"f258":"-","f136":302190868.0,"f257":0,"f135":313863158.0,"f256":"-","f255":0,"f254":"-","f253":"-","f252":"-","f12":40,"f251":"-","f11":146.35,"f250":"-","f14":246,"f13":146.36,"f16":9,"f15":146.365,"f18":213,"f17":146.37,"f19":146.4,"f139":37766485.0,"f138":41540966.0,"f20":2609,"f128":"-","f127":"-","f279":255,"f32":23,"f275":"-","f31":146.98,"f274":"-","f34":1,"f273":"-","f33":146.9,"f36":14,"f271":"-","f35":146.855,"f270":0,"f38":678,"f37":146.479,"f39":146.451,"f148":580814640.0,"f269":"-","f147":569333568.0,"f268":2.2,"f146":-191216.0,"f267":7.42,"f145":506911248.0,"f266":7.26,"f40":611,"f43":146.4,"f144":506720032.0,"f143":7897809.0,"f264":"湖北广电","f45":142.98,"f142":264424383.0,"f263":0,"f44":154.8,"f141":272322192.0,"f262":"000665","f47":939722,"f140":3774481.0,"f261":"-","f46":144.3,"f260":"-","f49":472978,"f48":1400290656.0,"f149":-11481072.0,"f50":1.37,"f52":"-","f51":"-","f177":1,"f55":"-","f295":"-","f58":"湖广转债","f173":"-","f294":"-","f57":"127007","f293":"-","f292":5,"f170":1.67,"f60":144.0,"f169":2.4,"f168":226.7,"f62":0,"f167":"-","f288":-1,"f287":"-","f286":0,"f164":"-","f285":"-","f163":"-","f284":0,"f162":"-","f161":466744,"f282":"-","f281":"-","f280":"-","f71":149.011,"f111":8,"f199":"-","f110":0,"f78":0,"f197":-0.82,"f196":-0.01,"f195":0.56,"f194":0.27,"f193":0.83,"f192":1790,"f191":40.28,"f190":"-","f117":606858597.6,"f80":"[{\"b\":202203010930,\"e\":202203011130},{\"b\":202203011300,\"e\":202203011500}]","f116":606858597.6,"f104":"-","f85":4145209.0,"f84":4145209.0,"f189":20180801,"f188":"-","f86":1646120043,"f187":"-","f186":"-","f185":"-","f184":"-","f183":"-","f181":0,"f107":0,"f92":"-","f105":"-"})


        + 时间,普通秒形式
     */

    private static void initFieldsMap() {
        fieldsMap = new HashMap<>();
        fieldsMap.put("f57", "资产代码");
        fieldsMap.put("f58", "资产名称");
        fieldsMap.put("f191", "委比");
        fieldsMap.put("f192", "委差");

        fieldsMap.put("f31", "卖五");
        fieldsMap.put("f32", "卖五量");
        fieldsMap.put("f33", "卖四");
        fieldsMap.put("f34", "卖四量");
        fieldsMap.put("f35", "卖三");
        fieldsMap.put("f36", "卖三量");
        fieldsMap.put("f37", "卖二");
        fieldsMap.put("f38", "卖二量");
        fieldsMap.put("f39", "卖一");
        fieldsMap.put("f40", "卖一量");

        fieldsMap.put("f19", "买一");
        fieldsMap.put("f20", "买一量");
        fieldsMap.put("f17", "买二");
        fieldsMap.put("f18", "买二量");
        fieldsMap.put("f15", "买三");
        fieldsMap.put("f16", "买三量");
        fieldsMap.put("f13", "买四");
        fieldsMap.put("f14", "买四量");
        fieldsMap.put("f11", "买五");
        fieldsMap.put("f12", "买五量");

        fieldsMap.put("f43", "最新");
        fieldsMap.put("f71", "均价");
        fieldsMap.put("f170", "涨幅");
        fieldsMap.put("f169", "涨跌");
        fieldsMap.put("f47", "总手");
        fieldsMap.put("f48", "金额");
        fieldsMap.put("f168", "换手率");
        fieldsMap.put("f50", "量比");
        fieldsMap.put("f44", "最高");
        fieldsMap.put("f45", "最低");
        fieldsMap.put("f46", "今开");
        fieldsMap.put("f60", "昨收");
        fieldsMap.put("f51", "涨停");
        fieldsMap.put("f52", "跌停");
        fieldsMap.put("f49", "外盘");
        fieldsMap.put("f161", "内盘");
    }

    /**
     * 实测需要传递全部字段, 该url才可以正常获取5档盘口数据, 若只传递5档字段, 将获取失败
     * getStockHandicapCore()
     * http://push2.eastmoney.com/api/qt/stock/get?ut=fa5fd1943c7b386f172d6893dbfba10b&invt=2&fltt=2&fields=f43,f57,f58,f169,f170,f46,f44,f51,f168,f47,f164,f163,f116,f60,f45,f52,f50,f48,f167,f117,f71,f161,f49,f530,f135,f136,f137,f138,f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104,f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78,f110,f260,f261,f262,f263,f264,f267,f268,f250,f251,f252,f253,f254,f255,f256,f257,f258,f266,f269,f270,f271,f273,f274,f275,f127,f199,f128,f193,f196,f194,f195,f197,f80,f280,f281,f282,f284,f285,f286,f287,f292,f293,f181,f294,f295,f279,f288&secid=0.300059&cb=jQuery112409228148447288975_1643015501069&_=1643015501237
     */
    private static void initFieldsStr() {
        //fieldsStr = StrUtil.join(",", fieldsMap.keySet());
        fieldsStr = "f43,f57,f58,f169,f170,f46,f44,f51,f168,f47,f164,f163,f116,f60,f45,f52,f50,f48,f167,f117,f71," +
                "f161,f49,f530,f135,f136,f137,f138,f139,f141,f142,f144,f145,f147,f148,f140,f143,f146,f149,f55,f62,f162,f92,f173,f104,f105,f84,f85,f183,f184,f185,f186,f187,f188,f189,f190,f191,f192,f107,f111,f86,f177,f78,f110,f260,f261,f262,f263,f264,f267,f268,f250,f251,f252,f253,f254,f255,f256,f257,f258,f266,f269,f270,f271,f273,f274,f275,f127,f199,f128,f193,f196,f194,f195,f197,f80,f280,f281,f282,f284,f285,f286,f287,f292,f293,f181,f294,f295,f279,f288";
    }

    String dateTime; // 由于该api没有时间字段. 自行在new时生成 yyyy-MM-dd HH:mm:ss 形式 DateUtil.now();
    // 资产代码,名称
    String secCode; // "f57": "600000", 资产代码
    String secName; // "f58": "浦发银行", 名称

    // 委比,委差
    Double consignRatio; // "f191": -41.62, 委比
    Double consignDifference; // "f192": -25487, 委差

    // 卖盘5档
    Double sell5Price; // "f31": 8.78, // 卖5
    Double sell5Vol; // "f32": 7546, // 卖5量
    Double sell5Amount; // 自行计算的 价格 * 手数 * 100 == 金额. 类似tushare, amount表示金额

    Double sell4Price; // "f33": 8.78, // 卖5
    Double sell4Vol; // "f34": 7546, // 卖5量
    Double sell4Amount;

    Double sell3Price; // "f35": 8.78, // 卖5
    Double sell3Vol; // "f36": 7546, // 卖5量
    Double sell3Amount;

    Double sell2Price; // "f37": 8.78, // 卖5
    Double sell2Vol; // "f38": 7546, // 卖5量
    Double sell2Amount;

    Double sell1Price; // "f39": 8.78, // 卖5
    Double sell1Vol; // "f40": 7546, // 卖5量
    Double sell1Amount;

    // 买盘5档, 注意字段顺序类似卖盘.
    Double buy1Price; // "f19": 8.78, // 买1
    Double buy1Vol; // "f20": 7546, // 买1量
    Double buy1Amount; // 自行计算的 价格 * 手数 * 100 == 金额. 类似tushare, amount表示金额

    Double buy2Price; // "f17": 8.78, // 买2
    Double buy2Vol; // "f18": 7546, // 买2量
    Double buy2Amount;

    Double buy3Price; // "f15": 8.78, // 买3
    Double buy3Vol; // "f16": 7546, // 买3量
    Double buy3Amount;

    Double buy4Price; // "f13": 8.78, // 买4
    Double buy4Vol; // "f14": 7546, // 买4量
    Double buy4Amount;

    Double buy5Price; // "f11": 8.78, // 买5
    Double buy5Vol; // "f12": 7546, // 买5量
    Double buy5Amount;

    // 16项常用盘口数据, dc布局为8*2. 这里按照从左到右, 从上到下顺序对应
    Double newPrice; // "f43": 8.74, 最新
    Double avgPrice; // "f71": 8.72, 均价
    Double changePercent; // "f170": 0.58, // 涨幅%
    Double changeValue; // "f169": 0.05,  涨跌价格值
    Double totalVol; // "f47": 376829, 总手
    Double totalAmount; // "f48": 328719168.0,  金额, 已发生成交额?
    Double turnoverRate; // "f168": 0.13, 换手率 0.13%
    Double volRatio; // "f50": 0.96,  量比
    Double highPrice; // "f44": 8.76, 最高
    Double lowPrice; // "f45": 8.67, 最低
    Double todayOpen; // "f46": 8.7,  今开
    Double preClose; // "f60": 8.69, 昨收
    Double highLimitPrice; // "f51": 9.56,  涨停
    Double lowLimitPrice; // "f52": 7.82,  跌停
    Double outerVol; // "f49": 221532, 外盘 量
    Double innerVol; // "f161": 155297,  内盘

    // {"f50":0.93,"f60":17.1,"f71":16.67,"f169":-0.52,"f52":15.39,"f168":0.59,"f51":18.81,"f43":16.58,"f45":16.51,"f44":17.15,"f47":1150659,"f58":"平安银行","f46":17.1,"f57":"000001","f161":703604,"f49":447054,"f48":1917613424,"f170":-3.04,"f192":1426,"f191":11.02}
    JSONObject rawJson;
    Integer oneHandAmount = 100; // 每手数量, 股票100, 转债10. 构造器传递是否为转债. 在自动计算 10档盘口的 金额时, 乘法因子不同

    public StockBondHandicap(JSONObject rawJson, boolean isBond) {
        this.rawJson = rawJson;
        this.dateTime = DateUtil.now();
        if (isBond) {
            this.oneHandAmount = 10; // 转债10, 将影响10档金额计算
        }
        parseAttrs();
    }

    public StockBondHandicap(JSONObject rawJson) {
        this(rawJson, true);
    }

    private void parseAttrs() {
        this.secCode = rawJson.getString("f57");
        this.secName = rawJson.getString("f58");
        this.consignRatio = tryGetDoubleFromRawJson("f191");
        this.consignDifference = tryGetDoubleFromRawJson("f192");

        initSells();
        initBuys();
        initCommonSixteen();
    }

    /**
     * 16项常用盘口
     */
    private void initCommonSixteen() {
        newPrice = tryGetDoubleFromRawJson("f43");
        avgPrice = tryGetDoubleFromRawJson("f71");
        changePercent = tryGetDoubleFromRawJson("f170");
        changeValue = tryGetDoubleFromRawJson("f169");
        totalVol = tryGetDoubleFromRawJson("f47");
        totalAmount = tryGetDoubleFromRawJson("f48");
        turnoverRate = tryGetDoubleFromRawJson("f168");
        volRatio = tryGetDoubleFromRawJson("f50");
        highPrice = tryGetDoubleFromRawJson("f44");
        lowPrice = tryGetDoubleFromRawJson("f45");
        todayOpen = tryGetDoubleFromRawJson("f46");
        preClose = tryGetDoubleFromRawJson("f60");
        highLimitPrice = tryGetDoubleFromRawJson("f51");
        lowLimitPrice = tryGetDoubleFromRawJson("f52");
        outerVol = tryGetDoubleFromRawJson("f49");
        innerVol = tryGetDoubleFromRawJson("f161");
    }

    private Double tryGetDoubleFromRawJson(String field) {
        return JSONUtilS.tryParseDoubleOrNull(rawJson, field);
    }

    private void initSells() {
        this.sell5Price = tryGetDoubleFromRawJson("f31");
        this.sell5Vol = tryGetDoubleFromRawJson("f32");
        if (this.sell5Price != null && this.sell5Vol != null) {
            this.sell5Amount = sell5Price * sell5Vol * oneHandAmount;
        }

        this.sell4Price = tryGetDoubleFromRawJson("f33");
        this.sell4Vol = tryGetDoubleFromRawJson("f34");
        if (this.sell4Price != null && this.sell4Vol != null) {
            this.sell4Amount = sell4Price * sell4Vol * oneHandAmount;
        }

        this.sell3Price = tryGetDoubleFromRawJson("f35");
        this.sell3Vol = tryGetDoubleFromRawJson("f36");
        if (this.sell3Price != null && this.sell3Vol != null) {
            this.sell3Amount = sell3Price * sell3Vol * oneHandAmount;
        }


        this.sell2Price = tryGetDoubleFromRawJson("f37");
        this.sell2Vol = tryGetDoubleFromRawJson("f38");
        if (this.sell2Price != null && this.sell2Vol != null) {
            this.sell2Amount = sell2Price * sell2Vol * oneHandAmount;
        }

        this.sell1Price = tryGetDoubleFromRawJson("f39");
        this.sell1Vol = tryGetDoubleFromRawJson("f40");
        if (this.sell1Price != null && this.sell1Vol != null) {
            this.sell1Amount = sell1Price * sell1Vol * oneHandAmount;
        }
    }

    private void initBuys() {
        this.buy5Price = tryGetDoubleFromRawJson("f11");
        this.buy5Vol = tryGetDoubleFromRawJson("f12");
        if (this.buy5Price != null && this.buy5Vol != null) {
            this.buy5Amount = buy5Price * buy5Vol * oneHandAmount;
        }

        this.buy4Price = tryGetDoubleFromRawJson("f13");
        this.buy4Vol = tryGetDoubleFromRawJson("f14");
        if (this.buy4Price != null && this.buy4Vol != null) {
            this.buy4Amount = buy4Price * buy4Vol * oneHandAmount;
        }

        this.buy3Price = tryGetDoubleFromRawJson("f15");
        this.buy3Vol = tryGetDoubleFromRawJson("f16");
        if (this.buy3Price != null && this.buy3Vol != null) {
            this.buy3Amount = buy3Price * buy3Vol * oneHandAmount;
        }


        this.buy2Price = tryGetDoubleFromRawJson("f17");
        this.buy2Vol = tryGetDoubleFromRawJson("f18");
        if (this.buy2Price != null && this.buy2Vol != null) {
            this.buy2Amount = buy2Price * buy2Vol * oneHandAmount;
        }

        this.buy1Price = tryGetDoubleFromRawJson("f19");
        this.buy1Vol = tryGetDoubleFromRawJson("f20");
        if (this.buy1Price != null && this.buy1Vol != null) {
            this.buy1Amount = buy1Price * buy1Vol * oneHandAmount;
        }
    }
}

/*
 * {
 * "rc": 0,
 * "rt": 4,
 * "svr": 181669437,
 * "lt": 1,
 * "full": 1,
 * "data": {
 * "f43": 8.74, 最新
 * "f44": 8.76, 最高
 * "f45": 8.67, 最低
 * "f46": 8.7,  今开
 * "f47": 376829, 总手
 * "f48": 328719168.0,  金额
 * "f49": 221532, 外盘
 * "f50": 0.96,  量比
 * "f51": 9.56,  涨停
 * "f52": 7.82,  跌停
 * "f55": 1.415091382, 公司核心数据-收益(三)
 * "f57": "600000", 资产代码
 * "f58": "浦发银行", 名称
 * "f60": 8.69, 昨收
 * "f62": 3,
 * "f71": 8.72, 均价
 * "f78": 0,
 * "f80": "[{\"b\":202202110930,\"e\":202202111130},{\"b\":202202111300,\"e\":202202111500}]", 交易时间
 * "f84": 29352168006.0, 总股本
 * "f85": 29352168006.0, 流通股本
 * "f86": 1644566391,
 * "f92": 18.7324153, 每股净资产
 * "f104": 191137000000.0,
 * "f105": 41536000000.0, 净利润
 * "f107": 1,
 * "f110": 1,
 * "f111": 2,
 * "f116": 256537948372.44, 总市值
 * "f117": 256537948372.44, 流通市值
 * "f127": "银行",
 * "f128": "上海板块",
 * "f135": 129358320.0,
 * "f136": 129068671.0,
 * "f137": 289649.0,
 * "f138": 38749279.0,
 * "f139": 51406071.0,
 * "f140": -12656792.0,
 * "f141": 90609041.0,
 * "f142": 77662600.0,
 * "f143": 12946441.0,
 * "f144": 99927841.0,
 * "f145": 109458394.0,
 * "f146": -9530553.0,
 * "f147": 88260797.0,
 * "f148": 79019893.0,
 * "f149": 9240904.0,
 * "f161": 155297,  内盘
 * "f162": 4.63,  市盈率[动]
 * "f163": 4.4,
 * "f164": 4.65,
 * "f167": 0.47, 市净率
 * "f168": 0.13, 换手 0.13%
 * "f169": 0.05,  涨跌价格值
 * "f170": 0.58, // 涨幅%
 * "f173": 7.25,  ROE%
 * "f177": 577,
 * "f183": 143484000000.0,  总营收
 * "f184": -3.5278455736, 总营收同比
 * "f185": -7.165526798087, 净利润同比
 * "f186": 0.0,  毛利率%
 * "f187": 29.3614619052, 净利率
 * "f188": 91.6841375217, 负债率
 * "f189": 19991110,
 * "f190": 6.300658948334, 每股未分配利润(元)
 * "f191": -41.62, 委比
 * "f192": -25487, 委差
 * "f193": 0.09,
 * "f194": -3.85,
 * "f195": 3.94,
 * "f196": -2.9,
 * "f197": 2.81,
 * "f199": 90,
 * "f250": "-",
 * "f251": "-",
 * "f252": "-",
 * "f253": "-",
 * "f254": "-",
 * "f255": 0,
 * "f256": "-",
 * "f257": 0,
 * "f258": "-",
 * "f262": "110059",
 * "f263": 1,
 * "f264": "浦发转债",
 * "f266": 107.32,
 * "f267": 106.98,
 * "f268": -0.32,
 * "f269": "-",
 * "f270": 0,
 * "f271": "-",
 * "f273": "-",
 * "f274": "-",
 * "f275": "-",
 * "f280": "-",
 * "f281": "-",
 * "f282": "-",
 * "f284": 0,
 * "f285": "-",
 * "f286": 0,
 * "f287": "-",
 * "f292": 5,
 * "f31": 8.78, // 卖5
 * "f32": 7546, // 卖5量
 * "f33": 8.77, // 卖4
 * "f34": 5681,
 * "f35": 8.76, // 卖3
 * "f36": 11189,
 * "f37": 8.75, // 卖2
 * "f38": 10413,
 * "f39": 8.74, // 卖1
 * "f40": 8531, // 卖1量
 * "f19": 8.73, // 买1
 * "f20": 3553, // 买1量
 * "f17": 8.72,
 * "f18": 4841,
 * "f15": 8.71,
 * "f16": 1406,
 * "f13": 8.7,
 * "f14": 6429,
 * "f11": 8.69, // 买5
 * "f12": 1644 // 买5量
 * }
 */