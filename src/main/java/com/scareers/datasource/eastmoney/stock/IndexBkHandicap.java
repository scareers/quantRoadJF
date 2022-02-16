package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import lombok.Getter;
import lombok.ToString;

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
@ToString
public class IndexBkHandicap {
    public static HashMap<String, String> fieldsMap; // 当前使用的字段 及 对应描述.
    public static String fieldsStr; // 字段字符串. , 分割

    static {
        initFieldsMap();
        initFieldsStr();
    }

    /*
    IndexBkHandicap(
        dateTime=2022-02-16 18:53:11,
        secCode=BK1030,
        stockName=电机,

        newPrice=140238.0,
        highPrice=140574.0,
        lowPrice=138200.0,
        todayOpen=138677.0,
        totalVol=1862596.0,
        totalAmount=3.200639856E9,
        outerVol=954799.0,
        innerVol=907797.0,
        volRatio=81.0,
        preClose=137843.0,

        raisingStockCount=19.0,
        fallStockCount=1.0,
        flatStockCount=0.0,

        totalMarketValue=1.65554563E11,
        flowMarketValue=1.28519345E11,

        changePercentOf5Day=-150.0,
        changePercentOf20Day=-870.0,
        changePercentOf60Day=-993.0,
        changePercentOfThisYear=-1218.0,

        changePercent=174.0,
        changeValue=2395.0,
        turnoverRate=239.0,
        amplitude=172.0,

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
    // 股票代码,名称
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

    private void parseAttrs() {
        this.secCode = rawJson.getStr("f57");
        this.stockName = rawJson.getStr("f58");

        newPrice = rawJson.getDouble("f43");
        highPrice = rawJson.getDouble("f44");
        lowPrice = rawJson.getDouble("f45");
        todayOpen = rawJson.getDouble("f46");
        totalVol = rawJson.getDouble("f47");
        totalAmount = rawJson.getDouble("f48");
        outerVol = rawJson.getDouble("f49");
        innerVol = rawJson.getDouble("f161");
        volRatio = rawJson.getDouble("f50");
        preClose = rawJson.getDouble("f60");

        raisingStockCount = rawJson.getDouble("f113");
        fallStockCount = rawJson.getDouble("f114");
        flatStockCount = rawJson.getDouble("f115");
        totalMarketValue = rawJson.getDouble("f116");
        flowMarketValue = rawJson.getDouble("f117");

        changePercentOf5Day = rawJson.getDouble("f119");
        changePercentOf20Day = rawJson.getDouble("f120");
        changePercentOf60Day = rawJson.getDouble("f121");
        changePercentOfThisYear = rawJson.getDouble("f122");

        changePercent = rawJson.getDouble("f170");
        changeValue = rawJson.getDouble("f169");
        turnoverRate = rawJson.getDouble("f168");
        amplitude = rawJson.getDouble("f171");
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