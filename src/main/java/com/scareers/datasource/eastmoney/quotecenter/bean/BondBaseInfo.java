package com.scareers.datasource.eastmoney.quotecenter.bean;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.annotations.TimeConsume;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.bond.EmConvertibleBondApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * description: 可转债基本信息
 *
 * @noti: 当使用rawJson构造时, http API, 有5大字段与ALL API不同. 将自动为null;
 * 当使用 ALL API 单行df数据进行构造时, 则可正确填充.  // HTTP API 特点
 * @author: admin
 * @date: 2022/3/2/002-01:34:39
 */
@Getter
@ToString
public class BondBaseInfo {
    private static final Log log = LogUtil.getLogger();

    public static void main(String[] args) throws Exception {

        initBondInfoPoolAndStartFlush();

        Console.log(bondInfoPool);

//        Console.log(bondInfoPool.get(SecurityBeanEm.createBond("127007")));

    }


    JSONObject rawJson;
    DataFrame<Object> rawLine; // 一般为仅包含单一可转债行的df

    String secCode; // .set("SECURITY_CODE", "资产代码")
    String secCode2; //   .set("SECUCODE", "资产代码2")
    String tradeMarket; // .set("TRADE_MARKET", "所属市场") // CNSESZ
    String secName; // .set("SECURITY_NAME_ABBR", "资产名称")
    DateTime listingDate; // .set("LISTING_DATE", "上市日期")
    String convertStockCode; // .set("CONVERT_STOCK_CODE", "正股代码")
    Double bondExpireYears; // .set("BOND_EXPIRE", "期限(年)")
    String rating; // .set("RATING", "评级")
    DateTime valueDate; // .set("VALUE_DATE", "生效日期")
    DateTime ceaseDate; // .set("CEASE_DATE", "停止日期")
    DateTime expireDate; // .set("EXPIRE_DATE", "到期日期")
    String payInterestDay; // .set("PAY_INTEREST_DAY", "付息日")
    String interestRateExplain; // .set("INTEREST_RATE_EXPLAIN", "利率说明")
    Double actualIssueScale; // .set("ACTUAL_ISSUE_SCALE", "发行规模(亿)")
    Double issuePrice; // .set("ISSUE_PRICE", "发行价")
    String remark; // .set("REMARK", "发行备注")
    String issueObject; //  .set("ISSUE_OBJECT", "发行对象")
    String securityShortName; // .set("SECURITY_SHORT_NAME", "正股名称")
    Double firstPerPreplacing; // .set("FIRST_PER_PREPLACING", "每股配售额(元)")
    Double onlineGeneralLwr; // .set("ONLINE_GENERAL_LWR", "网上发行中签率(%)")
    Double initialTransferPrice; // .set("INITIAL_TRANSFER_PRICE", "初始转股价")
    DateTime transferEndDate; // .set("TRANSFER_END_DATE", "转股结束日期")
    DateTime transferStartDate; // .set("TRANSFER_START_DATE", "转股开始日期")
    String resaleClause; // .set("RESALE_CLAUSE", "回售条款")
    String redeemClause; // .set("REDEEM_CLAUSE", "强赎条款")
    String partyName; // set("PARTY_NAME", "资信公司")
    Double convertStockPrice; // .set("CONVERT_STOCK_PRICE", "正股价")
    Double transferPrice; // .set("TRANSFER_PRICE", "转股价")
    Double transferValue; // .set("TRANSFER_VALUE", "转股价值")
    Double currentBondPrice; // .set("CURRENT_BOND_PRICE", "债现价")
    Double transferPremiumRatio; // .set("TRANSFER_PREMIUM_RATIO", "转股溢价率")
    Double resaleTrigPrice; // .set("RESALE_TRIG_PRICE", "回售触发价")
    Double redeemTrigPrice; // .set("REDEEM_TRIG_PRICE", "强赎触发价")
    Double couponIr; // .set("COUPON_IR", "票面利率(当期)")

    /*
    BondBaseInfo(rawJson=
        {"SECUCODE":"127007.SZ","TRADE_MARKET":"CNSESZ","TRANSFER_VALUE":10.16,"SECURITY_CODE":"127007","ISSUE_OBJECT":"本次可转换公司债券的发行对象为持有中国证券登记结算有限责任公司深圳分公司证券账户的自然人、法人、证券投资基金、符合法律规定的其他投资者等(国家法律、法规禁止者除外)。","SECURITY_START_DATE":"2018-06-27 00:00:00","ISSUE_PRICE":100,"IB_START_DATE":"2021-06-28 00:00:00","EXPIRE_DATE":"2024-06-28 00:00:00","TRANSFER_END_DATE":"2024-06-28 00:00:00","CORRECODE_NAME_ABBR":"湖广发债","PUBLIC_START_DATE":"2018-06-28 00:00:00","FIRST_PER_PREPLACING":2.7248,"TRANSFER_START_DATE":"2019-01-04 00:00:00","VALUE_DATE":"2018-06-28 00:00:00","INTEREST_RATE_EXPLAIN":"本次发行的可转债票面利率第一年0.5%、第二年0.8%、第三年1.0%、第四年1.5%、第五年1.8%、第六年2.0%。","PARAM_NAME":"交易所系统网上向社会公众投资者发行,交易所系统网上向原A股无限售股东优先配售,交易所系统网下向机构投资者协议发行","ISSUE_YEAR":"2018","RATING":"AA+","ISSUE_TYPE":"1,4,2","INITIAL_TRANSFER_PRICE":10.16,"COUPON_IR":1.5,"PAY_INTEREST_DAY":"06-28","LISTING_DATE":"2018-08-01 00:00:00","CORRECODEO":"080665","CONVERT_STOCK_CODE":"000665","CORRECODE_NAME_ABBRO":"湖广配债","SECURITY_NAME_ABBR":"湖广转债","REDEEM_CLAUSE":"(1)到期赎回条款在本次发行的可转换公司债券期满后5个交易日内,公司将以本次可转债票面面值上浮8%(含最后一期利息)的价格向投资者赎回全部未转股的可转债。(2)有条件赎回条款在本次发行的可转换公司债券转股期内,当下述两种情形的任意一种出现时,公司董事会有权决定按照债券面值加当期应计利息的价格赎回全部或部分未转股的可转换公司债券:A.在本次发行的可转换公司债券转股期内,如果公司A股股票连续30个交易日中至少有15个交易日的收盘价格不低于当期转股价格的130%(含130%);B.当本次发行的可转换公司债券未转股余额不足3,000万元时。若在前述30个交易日内发生过转股价格调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,调整后的交易日按调整后的转股价格和收盘价格计算。当期应计利息的计算公式为:IA=B2*i*t/365。IA:指当期应计利息;B2:指本次发行的可转换公司债券持有人持有的将赎回的可转换公司债券票面总金额;i:指可转换公司债券当年票面利率;t:指计息天数,即从上一个付息日起至本计息年度赎回日止的实际日历天数(算头不算尾)。若在前述三十个交易日内发生过转股价格调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,调整后的交易日按调整后的转股价格和收盘价格计算。","CASHFLOW_DATE":"2022-06-28 00:00:00","PAR_VALUE":100,"BOND_START_DATE":"2018-07-02 00:00:00","PARTY_NAME":"联合资信评估股份有限公司","ACTUAL_ISSUE_SCALE":17.33592,"REMARK":"本次可转债向本公司原A股股东优先配售,优先配售后余额部分(含原A股股东放弃优先配售部分)采用网下对机构投资者配售和网上向社会公众投资者通过深交所交易系统发售的方式进行,认购金额不足1,733,592,000元的部分由保荐机构(主承销商)包销。","IB_END_DATE":"2022-06-27 00:00:00","CEASE_DATE":"2024-06-27 00:00:00","PAYDAYNEW":"-28","ONLINE_GENERAL_AAU":1000,"RESALE_CLAUSE":"(1)有条件回售条款在本次发行的可转换公司债券最后两个计息年度,如果公司股票在任何连续30个交易日的收盘价格低于当期转股价的70%时,可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按面值加上当期应计利息的价格回售给公司。若在上述交易日内发生过转股价格因发生送红股、转增股本、增发新股(不包括因本次发行的可转换公司债券转股而增加的股本)、配股以及派发现金股利等情况而调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,在调整后的交易日按调整后的转股价格和收盘价格计算。如果出现转股价格向下修正的情况,则上述“连续30个交易日”须从转股价格调整之后的第一个交易日起重新计算。最后两个计息年度可转换公司债券持有人在每年回售条件首次满足后可按上述约定条件行使回售权一次,若在首次满足回售条件而可转换公司债券持有人未在公司届时公告的回售申报期内申报并实施回售的,该计息年度不能再行使回售权,可转换公司债券持有人不能多次行使部分回售权。(2)附加回售条款若公司本次发行的可转换公司债券募集资金投资项目的实施情况与公司在募集说明书中的承诺情况相比出现重大变化,根据中国证监会的相关规定被视作改变募集资金用途或被中国证监会认定为改变募集资金用途的,可转换公司债券持有人享有一次回售的权利。可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按债券面值加上当期应计利息价格回售给公司。持有人在附加回售条件满足后,可以在公司公告后的附加回售申报期内进行回售,该次附加回售申报期内不实施回售的,不能再行使附加回售权。","BOND_EXPIRE":"6","TRANSFER_PREMIUM_RATIO":100,"SECURITY_SHORT_NAME":"湖北广电","CORRECODE":"070665","BOND_COMBINE_CODE":"18270600001DJZ","ONLINE_GENERAL_LWR":1.0127940397}

        secCode=127007,
        secCode2=127007.SZ,
        tradeMarket=CNSESZ,
        secName=湖广转债,
        listingDate=2018-08-01 00:00:00,
        convertStockCode=000665,
        bondExpireYears=6.0, rating=AA+,
        valueDate=2018-06-28 00:00:00,
        ceaseDate=2024-06-27 00:00:00,
        expireDate=2024-06-28 00:00:00,
        payInterestDay=06-28,
        interestRateExplain=本次发行的可转债票面利率第一年0.5%、第二年0.8%、第三年1.0%、第四年1.5%、第五年1.8%、第六年2.0%。,
        actualIssueScale=17.33592,
        issuePrice=100.0,
        remark=本次可转债向本公司原A股股东优先配售,优先配售后余额部分(含原A股股东放弃优先配售部分)采用网下对机构投资者配售和网上向社会公众投资者通过深交所交易系统发售的方式进行,认购金额不足1,733,592,000元的部分由保荐机构(主承销商)包销。,
        issueObject=本次可转换公司债券的发行对象为持有中国证券登记结算有限责任公司深圳分公司证券账户的自然人、法人、证券投资基金、符合法律规定的其他投资者等(国家法律、法规禁止者除外)。,
        securityShortName=湖北广电,
        firstPerPreplacing=2.7248,
        onlineGeneralLwr=1.0127940397,
        initialTransferPrice=10.16,
        transferEndDate=2024-06-28 00:00:00,
        transferStartDate=2019-01-04 00:00:00,
        resaleClause=(1)有条件回售条款在本次发行的可转换公司债券最后两个计息年度,如果公司股票在任何连续30个交易日的收盘价格低于当期转股价的70%时,可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按面值加上当期应计利息的价格回售给公司。若在上述交易日内发生过转股价格因发生送红股、转增股本、增发新股(不包括因本次发行的可转换公司债券转股而增加的股本)、配股以及派发现金股利等情况而调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,在调整后的交易日按调整后的转股价格和收盘价格计算。如果出现转股价格向下修正的情况,则上述“连续30个交易日”须从转股价格调整之后的第一个交易日起重新计算。最后两个计息年度可转换公司债券持有人在每年回售条件首次满足后可按上述约定条件行使回售权一次,若在首次满足回售条件而可转换公司债券持有人未在公司届时公告的回售申报期内申报并实施回售的,该计息年度不能再行使回售权,可转换公司债券持有人不能多次行使部分回售权。(2)附加回售条款若公司本次发行的可转换公司债券募集资金投资项目的实施情况与公司在募集说明书中的承诺情况相比出现重大变化,根据中国证监会的相关规定被视作改变募集资金用途或被中国证监会认定为改变募集资金用途的,可转换公司债券持有人享有一次回售的权利。可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按债券面值加上当期应计利息价格回售给公司。持有人在附加回售条件满足后,可以在公司公告后的附加回售申报期内进行回售,该次附加回售申报期内不实施回售的,不能再行使附加回售权。,
        partyName=联合资信评估股份有限公司,

        convertStockPrice=null,
        transferPrice=null,
        transferValue=10.16,
        currentBondPrice=null,
        transferPremiumRatio=100.0,
        resaleTrigPrice=null,
        redeemTrigPrice=null,
        couponIr=1.5)




        secCode=127007, secCode2=127007.SZ, tradeMarket=CNSESZ, secName=湖广转债,
        listingDate=2018-08-01 00:00:00, convertStockCode=000665, bondExpireYears=6.0, rating=AA+,
        valueDate=2018-06-28 00:00:00, ceaseDate=2024-06-27 00:00:00, expireDate=2024-06-28 00:00:00, payInterestDay=06-28,
        interestRateExplain=本次发行的可转债票面利率第一年0.5%、第二年0.8%、第三年1.0%、第四年1.5%、第五年1.8%、第六年2.0%。, actualIssueScale=17.33592,
        issuePrice=100.0, remark=本次可转债向本公司原A股股东优先配售,优先配售后余额部分(含原A股股东放弃优先配售部分)采用网下对机构投资者配售和网上向社会公众投资者通过深交所交易系统发售的方式进行,认购金额不足1,733,592,000元的部分由保荐机构(主承销商)包销。, issueObject=本次可转换公司债券的发行对象为持有中国证券登记结算有限责任公司深圳分公司证券账户的自然人、法人、证券投资基金、符合法律规定的其他投资者等(国家法律、法规禁止者除外)。,
        securityShortName=湖北广电, firstPerPreplacing=2.7248, onlineGeneralLwr=1.0127940397,
        initialTransferPrice=10.16, transferEndDate=2024-06-28 00:00:00, transferStartDate=2019-01-04 00:00:00,
        resaleClause=(1)有条件回售条款在本次发行的可转换公司债券最后两个计息年度,如果公司股票在任何连续30个交易日的收盘价格低于当期转股价的70%时,可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按面值加上当期应计利息的价格回售给公司。若在上述交易日内发生过转股价格因发生送红股、转增股本、增发新股(不包括因本次发行的可转换公司债券转股而增加的股本)、配股以及派发现金股利等情况而调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,在调整后的交易日按调整后的转股价格和收盘价格计算。如果出现转股价格向下修正的情况,则上述“连续30个交易日”须从转股价格调整之后的第一个交易日起重新计算。最后两个计息年度可转换公司债券持有人在每年回售条件首次满足后可按上述约定条件行使回售权一次,若在首次满足回售条件而可转换公司债券持有人未在公司届时公告的回售申报期内申报并实施回售的,该计息年度不能再行使回售权,可转换公司债券持有人不能多次行使部分回售权。(2)附加回售条款若公司本次发行的可转换公司债券募集资金投资项目的实施情况与公司在募集说明书中的承诺情况相比出现重大变化,根据中国证监会的相关规定被视作改变募集资金用途或被中国证监会认定为改变募集资金用途的,可转换公司债券持有人享有一次回售的权利。可转换公司债券持有人有权将其持有的可转换公司债券全部或部分按债券面值加上当期应计利息价格回售给公司。持有人在附加回售条件满足后,可以在公司公告后的附加回售申报期内进行回售,该次附加回售申报期内不实施回售的,不能再行使附加回售权。, redeemClause=(1)到期赎回条款在本次发行的可转换公司债券期满后5个交易日内,公司将以本次可转债票面面值上浮8%(含最后一期利息)的价格向投资者赎回全部未转股的可转债。(2)有条件赎回条款在本次发行的可转换公司债券转股期内,当下述两种情形的任意一种出现时,公司董事会有权决定按照债券面值加当期应计利息的价格赎回全部或部分未转股的可转换公司债券:A.在本次发行的可转换公司债券转股期内,如果公司A股股票连续30个交易日中至少有15个交易日的收盘价格不低于当期转股价格的130%(含130%);B.当本次发行的可转换公司债券未转股余额不足3,000万元时。若在前述30个交易日内发生过转股价格调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,调整后的交易日按调整后的转股价格和收盘价格计算。当期应计利息的计算公式为:IA=B2*i*t/365。IA:指当期应计利息;B2:指本次发行的可转换公司债券持有人持有的将赎回的可转换公司债券票面总金额;i:指可转换公司债券当年票面利率;t:指计息天数,即从上一个付息日起至本计息年度赎回日止的实际日历天数(算头不算尾)。若在前述三十个交易日内发生过转股价格调整的情形,则在调整前的交易日按调整前的转股价格和收盘价格计算,调整后的交易日按调整后的转股价格和收盘价格计算。,
        partyName=联合资信评估股份有限公司, convertStockPrice=7.42, transferPrice=5.58,
        transferValue=132.9749, currentBondPrice=146.4, transferPremiumRatio=10.1, resaleTrigPrice=3.91, redeemTrigPrice=7.25, couponIr=1.5)


     */

    /**
     * 单可转债基本信息 api, 使用http json字符串填充, 有5个属性将无法获取, 为null
     * convertStockPrice, transferPrice, currentBondPrice, resaleTrigPrice, redeemTrigPrice
     *
     * @param rawJson
     */
    public BondBaseInfo(JSONObject rawJson) {
        this.rawJson = rawJson;
        this.rawLine = null;


        this.secCode = rawJson.getString("SECURITY_CODE");
        this.secCode2 = rawJson.getString("SECUCODE");
        this.tradeMarket = rawJson.getString("TRADE_MARKET");
        this.secName = rawJson.getString("SECURITY_NAME_ABBR");
        this.listingDate = DateUtil.parse(rawJson.getString("LISTING_DATE"));
        this.convertStockCode = rawJson.getString("CONVERT_STOCK_CODE");
        this.bondExpireYears = tryGetDoubleFromRawJson("BOND_EXPIRE");
        this.rating = rawJson.getString("RATING");
        this.valueDate = DateUtil.parse(rawJson.getString("VALUE_DATE"));
        this.ceaseDate = DateUtil.parse(rawJson.getString("CEASE_DATE"));
        this.expireDate = DateUtil.parse(rawJson.getString("EXPIRE_DATE"));
        this.payInterestDay = rawJson.getString("PAY_INTEREST_DAY");
        this.interestRateExplain = rawJson.getString("INTEREST_RATE_EXPLAIN");
        this.actualIssueScale = tryGetDoubleFromRawJson("ACTUAL_ISSUE_SCALE");
        this.issuePrice = tryGetDoubleFromRawJson("ISSUE_PRICE");

        this.remark = rawJson.getString("REMARK");
        this.issueObject = rawJson.getString("ISSUE_OBJECT");
        this.securityShortName = rawJson.getString("SECURITY_SHORT_NAME");

        this.firstPerPreplacing = tryGetDoubleFromRawJson("FIRST_PER_PREPLACING");
        this.onlineGeneralLwr = tryGetDoubleFromRawJson("ONLINE_GENERAL_LWR");
        this.initialTransferPrice = tryGetDoubleFromRawJson("INITIAL_TRANSFER_PRICE");

        this.transferEndDate = DateUtil.parse(rawJson.getString("TRANSFER_END_DATE"));
        this.transferStartDate = DateUtil.parse(rawJson.getString("TRANSFER_START_DATE"));

        this.resaleClause = rawJson.getString("RESALE_CLAUSE");
        this.redeemClause = rawJson.getString("REDEEM_CLAUSE");
        this.partyName = rawJson.getString("PARTY_NAME");

        this.convertStockPrice = tryGetDoubleFromRawJson("CONVERT_STOCK_PRICE");
        this.transferPrice = tryGetDoubleFromRawJson("TRANSFER_PRICE");
        this.transferValue = tryGetDoubleFromRawJson("TRANSFER_VALUE");
        this.currentBondPrice = tryGetDoubleFromRawJson("CURRENT_BOND_PRICE");
        this.transferPremiumRatio = tryGetDoubleFromRawJson("TRANSFER_PREMIUM_RATIO");
        this.resaleTrigPrice = tryGetDoubleFromRawJson("RESALE_TRIG_PRICE");
        this.redeemTrigPrice = tryGetDoubleFromRawJson("REDEEM_TRIG_PRICE");
        this.couponIr = tryGetDoubleFromRawJson("COUPON_IR");

    }

    /**
     * 从单行 单一可转债 df行构造. 一般调用静态方法, 构造dict.
     *
     * @param allBondBaseInfoDf
     * @see EmConvertibleBondApi.getAllBondBaseData
     */
    private BondBaseInfo(DataFrame<Object> bondBaseInfoDf) {
        Assert.isTrue(bondBaseInfoDf.length() > 0); // 使用第一行
        /*
                allBondBaseDataFields.putAll(Dict.create()
                .set("SECURITY_CODE", "资产代码")
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
        );
         */
        this.rawJson = null;
        this.rawLine = bondBaseInfoDf;

        this.secCode = tryGetStrFromDfLine("资产代码");
        this.secCode2 = tryGetStrFromDfLine("资产代码2");
        this.tradeMarket = tryGetStrFromDfLine("所属市场");
        this.secName = tryGetStrFromDfLine("资产名称");
        this.listingDate = DateUtil.parse(tryGetStrFromDfLine("上市日期"));
        this.convertStockCode = tryGetStrFromDfLine("正股代码");
        this.bondExpireYears = tryGetDoubleFromDfLine("期限(年)");
        this.rating = tryGetStrFromDfLine("评级");
        this.valueDate = DateUtil.parse(tryGetStrFromDfLine("生效日期"));
        this.ceaseDate = DateUtil.parse(tryGetStrFromDfLine("停止日期"));
        this.expireDate = DateUtil.parse(tryGetStrFromDfLine("到期日期"));
        this.payInterestDay = tryGetStrFromDfLine("付息日");
        this.interestRateExplain = tryGetStrFromDfLine("利率说明");
        this.actualIssueScale = tryGetDoubleFromDfLine("发行规模(亿)");
        this.issuePrice = tryGetDoubleFromDfLine("发行价");

        this.remark = tryGetStrFromDfLine("发行备注");
        this.issueObject = tryGetStrFromDfLine("发行对象");
        this.securityShortName = tryGetStrFromDfLine("正股名称");

        this.firstPerPreplacing = tryGetDoubleFromDfLine("每股配售额(元)");
        this.onlineGeneralLwr = tryGetDoubleFromDfLine("网上发行中签率(%)");
        this.initialTransferPrice = tryGetDoubleFromDfLine("初始转股价");

        this.transferEndDate = DateUtil.parse(tryGetStrFromDfLine("转股结束日期"));
        this.transferStartDate = DateUtil.parse(tryGetStrFromDfLine("转股开始日期"));

        this.resaleClause = tryGetStrFromDfLine("回售条款");
        this.redeemClause = tryGetStrFromDfLine("强赎条款");
        this.partyName = tryGetStrFromDfLine("资信公司");

        this.convertStockPrice = tryGetDoubleFromDfLine("正股价");
        this.transferPrice = tryGetDoubleFromDfLine("转股价");
        this.transferValue = tryGetDoubleFromDfLine("转股价值");
        this.currentBondPrice = tryGetDoubleFromDfLine("债现价");
        this.transferPremiumRatio = tryGetDoubleFromDfLine("转股溢价率");
        this.resaleTrigPrice = tryGetDoubleFromDfLine("回售触发价");
        this.redeemTrigPrice = tryGetDoubleFromDfLine("强赎触发价");
        this.couponIr = tryGetDoubleFromDfLine("票面利率(当期)");

    }

    private Double tryGetDoubleFromRawJson(String field) {
        return JSONUtilS.tryParseDoubleOrNull(rawJson, field);
    }

    private Double tryGetDoubleFromDfLine(String colName) {
        try {
            return Double.valueOf(this.rawLine.get(0, colName).toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String tryGetStrFromDfLine(String colName) {
        try {
            return this.rawLine.get(0, colName).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static ConcurrentHashMap<SecurityBeanEm, BondBaseInfo> bondInfoPool = new ConcurrentHashMap<>();

    @TimeConsume(description = "将查询所有可转债列表,并解析基本信息.最消耗时间的操作未查询构造bean")
    public static void initBondInfoPool() throws Exception {
        DataFrame<Object> allBondBaseData = EmConvertibleBondApi.getAllBondBaseData(5000, 4);
        List<String> secCodes = DataFrameS.getColAsStringList(allBondBaseData, "资产名称"); // 可转债代码列表

        List<SecurityBeanEm> bondList = SecurityBeanEm.createBondList(secCodes, false);// 目的是多线程查询构建后,放入缓存
        Set<String> names = bondList.stream().map(bean -> bean.getName()).collect(Collectors.toSet()); // 成功的转债名称列表

        for (int i = 0; i < allBondBaseData.length(); i++) {
//        for (int i = 0; i < 10; i++) {
            if (names.contains(allBondBaseData.get(i, "资产名称").toString())) { // 需要成功bean
                DataFrame<Object> dfTemp = new DataFrame<>(allBondBaseData.columns());
                dfTemp.append(allBondBaseData.row(i)); // 单行结果
                bondInfoPool.put(SecurityBeanEm.createBond(allBondBaseData.get(i, "资产名称").toString()),
                        new BondBaseInfo(dfTemp));
            }
        }
    }

    public static void flushPoolTaskStart() {
        CronUtil.schedule("0 0-59/5 * * * ?", new Task() {
            @Override
            public void execute() {
                try {
                    log.info("定时任务: BondBaseInfo.flushPoolTaskStart start");
                    initBondInfoPool();
                    log.info("定时任务: BondBaseInfo.flushPoolTaskStart finish");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        CronUtil.start();
    }

    public static void initBondInfoPoolAndStartFlush() throws Exception {
        initBondInfoPool();
        flushPoolTaskStart();
    }

    public static BondBaseInfo getFromPool(SecurityBeanEm beanEm) {
        Assert.isTrue(beanEm.isBond());
        return bondInfoPool.get(beanEm);
    }
}
