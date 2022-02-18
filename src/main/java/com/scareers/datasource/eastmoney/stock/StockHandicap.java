package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;

/**
 * description: 表示个股实时盘口各项数据的对象. rawJson保存原始 json 对象.
 * 全字段参考下文, 但只保存部分字段, 且顺序类似 东财行情页面
 * 1.5档盘口的金额由计算得到
 * 2.包含5档盘口和16项常用盘口数据+委差委比; 不包含对应可转债 以及 基本面数据;
 * 3.字段若解析失败, 调用get将得到null, 而非使用 -1.0填充
 *
 * @author: admin
 * @date: 2022/2/14/014-17:54:01
 * @see StockApi.getStockHandicap() 访问实时盘口
 */
@Getter // 仅可获取属性
@ToString
public class StockHandicap extends Handicap{
    public static HashMap<String, String> fieldsMap; // 当前使用的字段 及 对应描述.
    public static String fieldsStr; // 字段字符串. , 分割

    static {
        initFieldsMap();
        initFieldsStr();
    }

    /* toString()
    StockHandicap(
        dateTime=2022-02-14 19:19:36

        stockCodeSimple=000001,
        stockName=平安银行,
        consignRatio=11.02,
        consignDifference=1426.0,

        sell5Price=16.63,
        sell5Vol=839.0,
        sell5Amount=1395257.0,

        sell4Price=16.62,
        sell4Vol=1158.0,
        sell4Amount=1924596.0000000002,

        sell3Price=16.61,
        sell3Vol=947.0,
        sell3Amount=1572967.0,

        sell2Price=16.6,
        sell2Vol=1996.0,
        sell2Amount=3313360.0000000005,

        sell1Price=16.59,
        sell1Vol=821.0,
        sell1Amount=1362039.0,

        buy1Price=16.58,
        buy1Vol=390.0,
        buy1Amount=646619.9999999999,

        buy2Price=16.57,
        buy2Vol=2162.0,
        buy2Amount=3582434.0000000005,

        buy3Price=16.56,
        buy3Vol=729.0,
        buy3Amount=1207224.0,

        buy4Price=16.55,
        buy4Vol=2905.0,
        buy4Amount=4807775.0,

        buy5Price=16.54,
        buy5Vol=1001.0,
        buy5Amount=1655654.0,

        newPrice=16.58,
        avgPrice=16.67,
        changePercent=-3.04,
        changeValue=-0.52,
        totalVol=1150659.0,
        totalAmount=1.917613424E9,
        turnoverRate=0.59,
        volRatio=0.93,
        highPrice=17.15,
        lowPrice=16.51,
        todayOpen=17.1,
        preClose=17.1,
        highLimitPrice=18.81,
        lowLimitPrice=15.39,
        outerVol=447054.0,
        innerVol=703604.0,

        rawJson={"f50":0.93,"f60":17.1,"f71":16.67,"f169":-0.52,"f52":15.39,"f168":0.59,"f51":18.81,"f43":16.58,"f45":16.51,"f44":17.15,"f47":1150659,"f58":"平安银行","f46":17.1,"f57":"000001","f161":703604,"f49":447054,"f48":1917613424,"f170":-3.04,"f192":1426,"f191":11.02}
        )

        + 时间,普通秒形式
     */

    private static void initFieldsMap() {
        fieldsMap = new HashMap<>();
        fieldsMap.put("f57", "股票代码");
        fieldsMap.put("f58", "股票名称");
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
    // 股票代码,名称
    String stockCodeSimple; // "f57": "600000", 股票代码
    String stockName; // "f58": "浦发银行", 名称

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

    public StockHandicap(JSONObject rawJson) {
        this.rawJson = rawJson;
        this.dateTime = DateUtil.now();

        parseAttrs();
    }

    private void parseAttrs() {
        this.stockCodeSimple = rawJson.getStr("f57");
        this.stockName = rawJson.getStr("f58");

        this.consignRatio = rawJson.getDouble("f191");
        this.consignDifference = rawJson.getDouble("f192");

        initSells();
        initBuys();
        initCommonSixteen();
    }

    /**
     * 16项常用盘口
     */
    private void initCommonSixteen() {
        newPrice = rawJson.getDouble("f43");
        avgPrice = rawJson.getDouble("f71");
        changePercent = rawJson.getDouble("f170");
        changeValue = rawJson.getDouble("f169");
        totalVol = rawJson.getDouble("f47");
        totalAmount = rawJson.getDouble("f48");
        turnoverRate = rawJson.getDouble("f168");
        volRatio = rawJson.getDouble("f50");
        highPrice = rawJson.getDouble("f44");
        lowPrice = rawJson.getDouble("f45");
        todayOpen = rawJson.getDouble("f46");
        preClose = rawJson.getDouble("f60");
        highLimitPrice = rawJson.getDouble("f51");
        lowLimitPrice = rawJson.getDouble("f52");
        outerVol = rawJson.getDouble("f49");
        innerVol = rawJson.getDouble("f161");
    }

    private void initSells() {
        this.sell5Price = rawJson.getDouble("f31");
        this.sell5Vol = rawJson.getDouble("f32");
        if (this.sell5Price != null && this.sell5Vol != null) {
            this.sell5Amount = sell5Price * sell5Vol * 100;
        }

        this.sell4Price = rawJson.getDouble("f33");
        this.sell4Vol = rawJson.getDouble("f34");
        if (this.sell4Price != null && this.sell4Vol != null) {
            this.sell4Amount = sell4Price * sell4Vol * 100;
        }

        this.sell3Price = rawJson.getDouble("f35");
        this.sell3Vol = rawJson.getDouble("f36");
        if (this.sell3Price != null && this.sell3Vol != null) {
            this.sell3Amount = sell3Price * sell3Vol * 100;
        }


        this.sell2Price = rawJson.getDouble("f37");
        this.sell2Vol = rawJson.getDouble("f38");
        if (this.sell2Price != null && this.sell2Vol != null) {
            this.sell2Amount = sell2Price * sell2Vol * 100;
        }

        this.sell1Price = rawJson.getDouble("f39");
        this.sell1Vol = rawJson.getDouble("f40");
        if (this.sell1Price != null && this.sell1Vol != null) {
            this.sell1Amount = sell1Price * sell1Vol * 100;
        }
    }

    private void initBuys() {
        this.buy5Price = rawJson.getDouble("f11");
        this.buy5Vol = rawJson.getDouble("f12");
        if (this.buy5Price != null && this.buy5Vol != null) {
            this.buy5Amount = buy5Price * buy5Vol * 100;
        }

        this.buy4Price = rawJson.getDouble("f13");
        this.buy4Vol = rawJson.getDouble("f14");
        if (this.buy4Price != null && this.buy4Vol != null) {
            this.buy4Amount = buy4Price * buy4Vol * 100;
        }

        this.buy3Price = rawJson.getDouble("f15");
        this.buy3Vol = rawJson.getDouble("f16");
        if (this.buy3Price != null && this.buy3Vol != null) {
            this.buy3Amount = buy3Price * buy3Vol * 100;
        }


        this.buy2Price = rawJson.getDouble("f17");
        this.buy2Vol = rawJson.getDouble("f18");
        if (this.buy2Price != null && this.buy2Vol != null) {
            this.buy2Amount = buy2Price * buy2Vol * 100;
        }

        this.buy1Price = rawJson.getDouble("f19");
        this.buy1Vol = rawJson.getDouble("f20");
        if (this.buy1Price != null && this.buy1Vol != null) {
            this.buy1Amount = buy1Price * buy1Vol * 100;
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
 * "f57": "600000", 股票代码
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