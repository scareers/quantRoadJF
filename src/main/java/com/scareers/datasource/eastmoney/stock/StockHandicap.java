package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONObject;
import lombok.Getter;

import java.util.HashMap;

/**
 * description: 表示个股实时盘口各项数据的对象. rawJson保存了原始 json 对象.
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
public class StockHandicap {
    public static HashMap<String, String> fieldsMap; // 当前使用的字段 及 对应描述.

    static {
        initFieldsMap();
    }

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

    JSONObject rawJson;

    public StockHandicap(JSONObject rawJson) {
        this.rawJson = rawJson;
        parseAttrs();
    }

    private void parseAttrs() {
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