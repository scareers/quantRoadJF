package com.scareers.datasource.eastmoney.quotecenter.bean;

import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;

/**
 * description: 可转债基本信息
 *
 * @author: admin
 * @date: 2022/3/2/002-01:34:39
 */
public class BondBaseInfo {
    JSONObject rawJson;

    /*
            {
              "SECURITY_CODE": "123132",  资产代码
              "SECUCODE": "123132.SZ",  代码2
              "TRADE_MARKET": "CNSESZ",   所属市场
              "SECURITY_NAME_ABBR": "回盛转债", 资产名称
              "DELIST_DATE": null,
              "LISTING_DATE": "2022-01-07 00:00:00", 上市日期
              "CONVERT_STOCK_CODE": "300871", 正股代码
              "BOND_EXPIRE": "6", 期限(年)
              "RATING": "AA-", 评级
              "VALUE_DATE": "2021-12-17 00:00:00", 生效日期
              "ISSUE_YEAR": "2021",
              "CEASE_DATE": "2027-12-16 00:00:00", 停止日期
              "EXPIRE_DATE": "2027-12-17 00:00:00", 到期日期
              "PAY_INTEREST_DAY": "12-17", 付息日
              "INTEREST_RATE_EXPLAIN": "第一年0.40%,第二年0.60%,第三年1.00%,第四年1.50%,第五年2.50%,第六年3.00%。",  利率说明
              "BOND_COMBINE_CODE": "21270600001YDB",
              "ACTUAL_ISSUE_SCALE": 7,  发行规模(亿)
              "ISSUE_PRICE": 100, 发行价
              "REMARK": "本次发行的回盛转债向发行人在股权登记日收市后登记在册的原A股股东实行优先配售,原A股股东优先配售后余额部分(含原A股股东放弃优先配售部分)通过深交所交易系统网上向社会公众投资者发行。",  发行备注
              "PAR_VALUE": 100,
              "ISSUE_OBJECT": "(1)公司原股东:发行公告公布的股权登记日(即2021年12月16日,T-1日)收市后中国结算深圳分公司登记在册的公司所有A股股东。(2)社会公众投资者:持有中国证券登记结算有限责任公司深圳分公司证券账户的自然人、法人、证券投资基金、符合法律规定的其他投资者等(国家法律、法规禁止者除外)。(3)保荐机构(主承销商)的自营账户不得参与本次申购。", 发行说明
              "REDEEM_TYPE": null,
              "EXECUTE_REASON_HS": null,
              "NOTICE_DATE_HS": null,
              "NOTICE_DATE_SH": null,
              "EXECUTE_PRICE_HS": null,
              "EXECUTE_PRICE_SH": null,
              "RECORD_DATE_SH": null,
              "EXECUTE_START_DATESH": null,
              "EXECUTE_START_DATEHS": null,
              "EXECUTE_END_DATE": null,
              "CORRECODE": "370871",
              "CORRECODE_NAME_ABBR": "回盛发债",
              "PUBLIC_START_DATE": "2021-12-17 00:00:00",  申购日期
              "CORRECODEO": "380871",
              "CORRECODE_NAME_ABBRO": "回盛配债",
              "BOND_START_DATE": "2021-12-21 00:00:00",
              "SECURITY_START_DATE": "2021-12-16 00:00:00",
              "SECURITY_SHORT_NAME": "回盛生物",  正股名称
              "FIRST_PER_PREPLACING": 4.2105, 每股配售额
              "ONLINE_GENERAL_AAU": 1000,
              "ONLINE_GENERAL_LWR": 0.0022054859,  网上发行中签率
              "INITIAL_TRANSFER_PRICE": 28.32, 初始转股价
              "TRANSFER_END_DATE": "2027-12-16 00:00:00", 转股结束日期
              "TRANSFER_START_DATE": "2022-06-23 00:00:00", 转股开始日期
              "RESALE_CLAUSE": "(1)有条件回售条款本次向不特定对象发行可转债最后两个计息年度,如果公司股票在任意连续三十个交易日的收盘价格低于当期转股价格的70%时,可转债持有人有权将其持有的可转债全部或部分按债券面值加上当期应计利息的价格回售给公司。若在上述交易日内发生过转股价格因发生派送股票股利、转增股本、增发新股(不包括因本次向不特定对象发行可转债转股而增加的股本)、配股以及派发现金股利等情况而调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,在调整后的交易日按调整后的转股价格和收盘价格计算。如果出现转股价格向下修正的情况,则上述连续三十个交易日须从转股价格调整之后的第一个交易日起重新计算。本次向不特定对象发行可转债最后两个计息年度,可转债持有人在每年回售条件首次满足后可按上述约定条件行使回售权一次,若在首次满足回售条件而可转债持有人未在公司届时公告的回售申报期内申报并实施回售的,该计息年度不应再行使回售权,可转债持有人不能多次行使部分回售权。(2)附加回售条款若公司本次向不特定对象发行可转债募集资金投资项目的实施情况与公司在募集说明书中的承诺情况相比出现重大变化,且被中国证监会认定为改变募集资金用途的,可转债持有人享有一次回售的权利。可转债持有人有权将其持有的可转债全部或部分按债券面值加当期应计利息的价格回售给公司。持有人在附加回售条件满足后,可以在公司公告后的附加回售申报期内进行回售,本次附加回售申报期内不实施回售的,不应再行使附加回售权。当期应计利息的计算公式为:IA\u003dB×i×t/365IA:指当期应计利息;B:指本次向不特定对象发行可转债持有人持有的将赎回的可转债票面总金额;i:指可转债当年票面利率;t:指计息天数,即从上一个付息日起至本计息年度赎回日止的实际日历天数(算头不算尾)。",  回售条款
              "REDEEM_CLAUSE": "(1)到期赎回条款在本次向不特定对象发行可转债期满后五个交易日内,公司将按债券面值的115.00%(含最后一期利息)的价格赎回全部未转股的可转换公司债券。(2)有条件赎回条款在转股期内,当下述情形的任意一种出现时,公司有权决定按照以债券面值加当期应计利息的价格赎回全部或部分未转股的可转债:1)在转股期内,如果公司股票在任意连续三十个交易日中至少十五个交易日的收盘价格不低于当期转股价格的130%(含130%);2)当本次向不特定对象发行可转债未转股余额不足3,000万元时。当期应计利息的计算公式为:IA\u003dB×i×t/365IA:指当期应计利息;B:指本次发行的可转债持有人持有的将赎回的可转债票面总金额;i:指可转债当年票面利率;t:指计息天数,即从上一个付息日起至本计息年度赎回日止的实际日历天数(算头不算尾)。若在前述三十个交易日内发生过转股价格调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,调整后的交易日按调整后的转股价格和收盘价格计算。",  强赎条款
              "PARTY_NAME": "中证鹏元资信评估股份有限公司",  资信公司?
              "CONVERT_STOCK_PRICE": 25.87, 正股价
              "TRANSFER_PRICE": 28.32, 转股价
              "TRANSFER_VALUE": 91.3489, 转股价值
              "CURRENT_BOND_PRICE": 127.053, 债现价
              "TRANSFER_PREMIUM_RATIO": 39.09, 溢价率
              "CONVERT_STOCK_PRICEHQ": null,
              "MARKET": null,
              "RESALE_TRIG_PRICE": 19.82, 回售触发价
              "REDEEM_TRIG_PRICE": 36.82, 强赎触发价
              "PBV_RATIO": 2.96,
              "IB_START_DATE": "2021-12-17 00:00:00",
              "IB_END_DATE": "2022-12-16 00:00:00",
              "CASHFLOW_DATE": "2022-12-17 00:00:00",
              "COUPON_IR": 0.4, 票面利率(当期)
              "PARAM_NAME": "交易所系统网上向原A股无限售股东优先配售,交易所系统网上向社会公众投资者发行",
              "ISSUE_TYPE": "4,1",
              "EXECUTE_REASON_SH": null,
              "PAYDAYNEW": "-17",
              "CURRENT_BOND_PRICENEW": null
            }
     */


    /*

                .set("SECUCODE", "资产代码2")
                .set("TRADE_MARKET", "所属市场") // CNSESZ
                .set("SECURITY_NAME_ABBR", "资产名称")
                .set("LISTING_DATE", "上市日期")
                .set("CONVERT_STOCK_CODE", "正股代码")
                .set("BOND_EXPIRE", "期限(年)")
                .set("RATING", "评级")
                .set("VALUE_DATE", "生效日期")
                .set("CEASE_DATE", "停止日期")
                .set("EXPIRE_DATE", "到期日期")
                .set("PAY_INTEREST_DAY", "付息日")
                .set("INTEREST_RATE_EXPLAIN", "利率说明")
                .set("ACTUAL_ISSUE_SCALE", "发行规模(亿)")
                .set("ISSUE_PRICE", "发行价")
                .set("REMARK", "发行备注")
                .set("ISSUE_OBJECT", "发行对象")
                .set("SECURITY_SHORT_NAME", "正股名称")
                .set("FIRST_PER_PREPLACING", "每股配售额(元)")
                .set("ONLINE_GENERAL_LWR", "网上发行中签率(%)")
                .set("INITIAL_TRANSFER_PRICE", "初始转股价")
                .set("TRANSFER_END_DATE", "转股结束日期")
                .set("TRANSFER_START_DATE", "转股开始日期")
                .set("RESALE_CLAUSE", "回售条款")
                .set("PARTY_NAME", "资信公司")
                .set("CONVERT_STOCK_PRICE", "正股价")
                .set("TRANSFER_PRICE", "转股价")
                .set("TRANSFER_VALUE", "转股价值")
                .set("CURRENT_BOND_PRICE", "债现价")
                .set("TRANSFER_PREMIUM_RATIO", "转股溢价率")
                .set("RESALE_TRIG_PRICE", "回售触发价")
                .set("REDEEM_TRIG_PRICE", "强赎触发价")
                .set("COUPON_IR", "票面利率(当期)")
     */
    String secCode; // .set("SECURITY_CODE", "资产代码")
    String secCode2; //   .set("SECUCODE", "资产代码2")
    String tradeMarket; // .set("TRADE_MARKET", "所属市场") // CNSESZ
    String secName; // .set("SECURITY_NAME_ABBR", "资产名称")
    String listingDate; // .set("LISTING_DATE", "上市日期")
    String convertStockCode; // .set("CONVERT_STOCK_CODE", "正股代码")
    String BOND_EXPIRE; // .set("BOND_EXPIRE", "期限(年)")
    String RATING; // .set("RATING", "评级")
    String VALUE_DATE; // .set("VALUE_DATE", "生效日期")
    String CEASE_DATE; // .set("CEASE_DATE", "停止日期")
    String EXPIRE_DATE; // .set("EXPIRE_DATE", "到期日期")
    String PAY_INTEREST_DAY; // .set("PAY_INTEREST_DAY", "付息日")
    String INTEREST_RATE_EXPLAIN; // .set("INTEREST_RATE_EXPLAIN", "利率说明")
    String ACTUAL_ISSUE_SCALE; // .set("ACTUAL_ISSUE_SCALE", "发行规模(亿)")
    String ISSUE_PRICE; // .set("ISSUE_PRICE", "发行价")
    String REMARK; // .set("REMARK", "发行备注")
    String ISSUE_OBJECT; //  .set("ISSUE_OBJECT", "发行对象")
    String SECURITY_SHORT_NAME; // .set("SECURITY_SHORT_NAME", "正股名称")
    String FIRST_PER_PREPLACING; // .set("FIRST_PER_PREPLACING", "每股配售额(元)")
    String ONLINE_GENERAL_LWR; // .set("ONLINE_GENERAL_LWR", "网上发行中签率(%)")
    String INITIAL_TRANSFER_PRICE; // .set("INITIAL_TRANSFER_PRICE", "初始转股价")
    String TRANSFER_END_DATE; // .set("TRANSFER_END_DATE", "转股结束日期")
    String RESALE_CLAUSE;
    String PARTY_NAME;
    String CONVERT_STOCK_PRICE;
    String TRANSFER_PRICE;
    String TRANSFER_VALUE;
    String CURRENT_BOND_PRICE;
    String TRANSFER_PREMIUM_RATIO;
    String RESALE_TRIG_PRICE;
    String REDEEM_TRIG_PRICE;
    String COUPON_IR;


    public BondBaseInfo(JSONObject rawJson) {
        this.rawJson = rawJson;
    }


    private Double tryGetDoubleFromRawJson(String field) {
        return JSONUtilS.tryParseDoubleOrNull(rawJson, field);
    }


}
