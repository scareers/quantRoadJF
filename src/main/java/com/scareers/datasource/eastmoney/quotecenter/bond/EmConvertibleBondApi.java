package com.scareers.datasource.eastmoney.quotecenter.bond;

import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.eastmoney.quotecenter.EmQuoteApi;
import com.scareers.datasource.eastmoney.quotecenter.SseCallback;
import com.scareers.datasource.eastmoney.quotecenter.bean.BondBaseInfo;
import com.scareers.datasource.eastmoney.quotecenter.bean.StockBondHandicap;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.getAsStrUseHutool;

/**
 * description: 可转债独特api
 * 可转债盘口,行情, fs, fs成交, 均同个股, 见 EmQuoteApi相关 api
 * 本类维护转债较为特殊的api
 *
 * @author: admin
 * @date: 2022/3/1/001-21:18:31
 */
public class EmConvertibleBondApi {
    private static final Log log = LogUtil.getLogger();
    public static ConcurrentHashMap<Object, Object> comparePriceTableFields = new ConcurrentHashMap<>();// 比价表字段
    public static ConcurrentHashMap<Object, Object> allBondBaseDataFields = new ConcurrentHashMap<>();// 比价表字段

    public static void main(String[] args) throws Exception {
//        Console.log(EmQuoteApi.getRealtimeQuotes(Arrays.asList("可转债")));
//        Console.log(getBondComparePriceTable(3000, 3));

//        Console.log(getAllBondBaseData(3000, 3));

        Console.log(getRealtimeQuotes());
//        Console.log(getBondBaseInfo(SecurityBeanEm.createBond("湖广转债"), 3000, 3));
    }

    static {
        initComparePriceTableFields();
        initAllBondBaseDataFields();
    }


    /**
     * 单个可转债基本信息api -- 建议使用BondInfoPool机制,  需要显式调用 BondBaseInfo.initBondInfoPool(), 可多次覆盖.再查询 Map
     * 可转债详细数据页: https://data.eastmoney.com/kzz/detail/110058.html
     * 有五大api; 1为k线获取, 其余4均为 可转债基本数据获取!
     * 1.大量基本信息
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112307136368641068562_1646152066176&reportName=RPT_BOND_CB_LIST&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22110058%22)&_=1646152066177
     * https://datacenter-web.eastmoney.com/api/data/v1/get?
     * callback=jQuery112307136368641068562_1646152066176
     * &reportName=RPT_BOND_CB_LIST&
     * columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22110058%22)&_=1646152066177
     * 2.??
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112307136368641068562_1646152066178&reportName=RPT_CB_BALLOTNUM&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22110058%22)&_=1646152066179
     * 3.发行状况说明?
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112307136368641068562_1646152066180&reportName=RPT_BOND_BS_OPRFINVESTITEM&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22110058%22)&sortColumns=SORT&sortTypes=1&_=1646152066181
     * 4.重要日期列表
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112307136368641068562_1646152066182&reportName=RPT_CB_IMPORTANTDATE&columns=ALL&quoteColumns=&source=WEB&client=WEB&filter=(SECURITY_CODE%3D%22110058%22)&_=1646152066183
     * todo: 补齐4大api
     *
     * @noti: python.efinance 可转债基本信息api, 使用的 1; 全部可转债信息api, 使用的 可转债数据表一览
     */
    public static BondBaseInfo getBondBaseInfo(SecurityBeanEm beanEm, int timeout, int retry) {
        String url = "https://datacenter-web.eastmoney.com/api/data/v1/get";
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("callback", StrUtil.format("jQuery112307136368641068562_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("reportName", "RPT_BOND_CB_LIST");
        params.put("columns", "ALL");
        params.put("sortColumns", "PUBLIC_START_DATE");
        params.put("source", "WEB");
        params.put("client", "WEB"); // 所有可转债
        params.put("filter", StrUtil.format("(SECURITY_CODE=\"{}\")", beanEm.getSecCode()));
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get exception: 获取可转债比价表: 访问http失败");
            return null;
        }

        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        JSONObject jsonObject = JSONUtilS.parseObj(response);
        JSONArray byPath = (JSONArray) JSONUtilS.getByPath(jsonObject, "result.data");
        return new BondBaseInfo((JSONObject) byPath.get(0));
    }


    public static List<String> baseDataRawFields = StrUtil
            .split("SECURITY_CODE,SECUCODE,TRADE_MARKET,SECURITY_NAME_ABBR,DELIST_DATE,LISTING_DATE,CONVERT_STOCK_CODE,BOND_EXPIRE,RATING,VALUE_DATE,ISSUE_YEAR,CEASE_DATE,EXPIRE_DATE,PAY_INTEREST_DAY,INTEREST_RATE_EXPLAIN,BOND_COMBINE_CODE,ACTUAL_ISSUE_SCALE,ISSUE_PRICE,REMARK,PAR_VALUE,ISSUE_OBJECT,REDEEM_TYPE,EXECUTE_REASON_HS,NOTICE_DATE_HS,NOTICE_DATE_SH,EXECUTE_PRICE_HS,EXECUTE_PRICE_SH,RECORD_DATE_SH,EXECUTE_START_DATESH,EXECUTE_START_DATEHS,EXECUTE_END_DATE,CORRECODE,CORRECODE_NAME_ABBR,PUBLIC_START_DATE,CORRECODEO,CORRECODE_NAME_ABBRO,BOND_START_DATE,SECURITY_START_DATE,SECURITY_SHORT_NAME,FIRST_PER_PREPLACING,ONLINE_GENERAL_AAU,ONLINE_GENERAL_LWR,INITIAL_TRANSFER_PRICE,TRANSFER_END_DATE,TRANSFER_START_DATE,RESALE_CLAUSE,REDEEM_CLAUSE,PARTY_NAME,CONVERT_STOCK_PRICE,TRANSFER_PRICE,TRANSFER_VALUE,CURRENT_BOND_PRICE,TRANSFER_PREMIUM_RATIO,CONVERT_STOCK_PRICEHQ,MARKET,RESALE_TRIG_PRICE,REDEEM_TRIG_PRICE,PBV_RATIO,IB_START_DATE,IB_END_DATE,CASHFLOW_DATE,COUPON_IR,PARAM_NAME,ISSUE_TYPE,EXECUTE_REASON_SH,PAYDAYNEW,CURRENT_BOND_PRICENEW",
                    ",");

    /**
     * 行情中心 -- 可转债(全部)数据一览表 -- 包含较为详细的基本信息字段.  可转债主api之1, 包含更多的退市可转债
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112307470764971864268_1646152531625&sortColumns=PUBLIC_START_DATE&sortTypes=-1&pageSize=50&pageNumber=1&reportName=RPT_BOND_CB_LIST&columns=ALL&quoteColumns=f2~01~CONVERT_STOCK_CODE~CONVERT_STOCK_PRICE%2Cf235~10~SECURITY_CODE~TRANSFER_PRICE%2Cf236~10~SECURITY_CODE~TRANSFER_VALUE%2Cf2~10~SECURITY_CODE~CURRENT_BOND_PRICE%2Cf237~10~SECURITY_CODE~TRANSFER_PREMIUM_RATIO%2Cf239~10~SECURITY_CODE~RESALE_TRIG_PRICE%2Cf240~10~SECURITY_CODE~REDEEM_TRIG_PRICE%2Cf23~01~CONVERT_STOCK_CODE~PBV_RATIO&source=WEB&client=WEB
     *
     * @return
     * @noti 单页上限500; 目前加上退市有2页
     */
    public static DataFrame<Object> getAllBondBaseData(int timeout, int retry) {
        String url = "https://datacenter-web.eastmoney.com/api/data/v1/get";
        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("callback", StrUtil.format("jQuery112307470764971864268_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));
        params.put("sortColumns", "PUBLIC_START_DATE");
        params.put("sortTypes", "-1");
        params.put("pageSize", "500");

        params.put("reportName", "RPT_BOND_CB_LIST");
        params.put("columns", "ALL");
        params.put("quoteColumns",
                "f2~01~CONVERT_STOCK_CODE~CONVERT_STOCK_PRICE,f235~10~SECURITY_CODE~TRANSFER_PRICE,f236~10~SECURITY_CODE~TRANSFER_VALUE,f2~10~SECURITY_CODE~CURRENT_BOND_PRICE,f237~10~SECURITY_CODE~TRANSFER_PREMIUM_RATIO,f239~10~SECURITY_CODE~RESALE_TRIG_PRICE,f240~10~SECURITY_CODE~REDEEM_TRIG_PRICE,f23~01~CONVERT_STOCK_CODE~PBV_RATIO");
        params.put("source", "WEB");
        params.put("client", "WEB"); // 所有可转债
        int page = 1;

        DataFrame<Object> res = null;
        while (true) {
            params.put("pageNumber", page); // 改变页码.

            String response;
            try {
                response = getAsStrUseHutool(url, params, timeout, retry);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("get exception: 获取可转债比价表: 访问http失败");
                return null;
            }

            DataFrame<Object> dataFrame = JSONUtilS
                    .jsonStrToDf(response, "(", ")",
                            baseDataRawFields,
                            Arrays.asList("result", "data"), JSONObject.class,
                            Arrays.asList(),
                            Arrays.asList());
            if (dataFrame == null || dataFrame.length() == 0) {
                break;// 当某页没有结果, 跳出返回已有结果
            }

            if (res == null) {
                res = dataFrame; // 首页
            } else {
                res = res.concat(dataFrame); // 非首页
            }
            page++;
        }
        if (res != null) {
            res = res.rename(allBondBaseDataFields);
        }
        return res;
    }


    /**
     * 行情中心 -- 可转债比价表 -- 包含可转债基本信息字段 + 少量正股价格字段;  可转债主api之2
     * 比价表列数与 "可转债"截面数据 相同
     * 比价表 https://quote.eastmoney.com/center/fullscreenlist.html#convertible_comparison
     * 数据一览 https://data.eastmoney.com/kzz/default.html
     * <p>
     * https://2.push2.eastmoney.com/api/qt/clist/get?cb=jQuery1124032789927949996756_1646143092648&pn=1&pz=50&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f243&fs=b:MK0354&fields=f1,f152,f2,f3,f12,f13,f14,f227,f228,f229,f230,f231,f232,f233,f234,f235,f236,f237,f238,f239,f240,f241,f242,f26,f243&_=1646143092649
     * 实测每页数量可10000, 即获得全部数据
     * 失败返回null
     *
     * @return
     */
    public static DataFrame<Object> getBondComparePriceTable(int timeout, int retry) {
        String url = "https://2.push2.eastmoney.com/api/qt/clist/get";
        String fields = "f1,f152,f2,f3,f12,f13,f14,f227,f228,f229,f230,f231,f232,f233,f234,f235,f236,f237,f238,f239," +
                "f240,f241,f242,f26,f243";

        Map<String, Object> params = new HashMap<>(); // 参数map
        params.put("cb", StrUtil.format("jjQuery1124032789927949996756_{}",
                System.currentTimeMillis() - RandomUtil.randomInt(1000)));


        params.put("pn", "1");
        params.put("pz", "100000");
        params.put("po", "1");
        params.put("np", "1");
        params.put("ut", "bd1d9ddb04089700cf9c27f6f7426281");
        params.put("fltt", "2");
        params.put("invt", "2");
        params.put("fid", "f243");
        params.put("fs", "b:MK0354"); // 所有可转债
        params.put("fields", fields); // 所有可转债
        params.put("_", System.currentTimeMillis());

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get exception: 获取可转债比价表: 访问http失败");
            return null;
        }

        DataFrame<Object> dataFrame = JSONUtilS
                .jsonStrToDf(response, "(", ")", StrUtil.split(fields, ","),
                        Arrays.asList("data", "diff"), JSONObject.class,
                        Arrays.asList(),
                        Arrays.asList());
        dataFrame = dataFrame.rename(comparePriceTableFields);
        return dataFrame;
    }


    /*
     * 以下为  EmQuoteApi 相关方法包装
     */

    /**
     * 可转债昨收今开
     *
     * @param beanEm
     * @param timeout
     * @param retry
     * @param useCache
     * @return
     */
    public static List<Double> getBondPreCloseAndTodayOpen(SecurityBeanEm beanEm, int timeout, int retry,
                                                           boolean useCache) {
        return EmQuoteApi.getStockBondPreCloseAndTodayOpen(beanEm, timeout, retry, useCache);
    }

    /**
     * 可转债盘口
     *
     * @param beanEm
     * @param timeout
     * @param retry
     * @return
     */
    public static StockBondHandicap getBondHandicap(SecurityBeanEm beanEm, int timeout, int retry) {
        return EmQuoteApi.getStockOrBondHandicap(beanEm, timeout, retry);
    }

    /**
     * 分时成交
     *
     * @param lastRecordAmounts
     * @param bean
     * @param retry
     * @param timeout
     * @return
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts,
                                                     SecurityBeanEm bean,
                                                     int retry, int timeout) {
        return EmQuoteApi.getFSTransaction(lastRecordAmounts, bean, retry, timeout);
    }

    /**
     * 所有可转债, 实时截面数据
     * 列数与 东财 :可转债比价表相同
     *
     * @param markets
     * @return
     */
    public static DataFrame<Object> getRealtimeQuotes() {
        return EmQuoteApi.getRealtimeQuotes(Collections.singletonList("可转债"));
    }

    /**
     * 批量获取历史k线行情
     */
    public static ConcurrentHashMap<SecurityBeanEm, DataFrame<Object>> getQuoteHistoryBatch(
            List<SecurityBeanEm> beanEmList,
            String begDate,
            String endDate,
            String klType, String fq,
            int retrySingle,
            int timeoutOfReq,
            boolean useCache
    ) throws ExecutionException, InterruptedException {
        return EmQuoteApi
                .getQuoteHistoryBatch(beanEmList, begDate, endDate, klType, fq, retrySingle, timeoutOfReq, useCache);
    }

    /**
     * 获取单只可转债k线行情
     *
     * @param useCache
     * @param bean
     * @param begDate
     * @param endDate
     * @param klType
     * @param fq
     * @param retry
     * @param timeout
     * @return
     */
    public static DataFrame<Object> getQuoteHistorySingle(boolean useCache,
                                                          SecurityBeanEm bean, String begDate,
                                                          String endDate, String klType, String fq,
                                                          int retry,
                                                          int timeout) {
        return EmQuoteApi.getQuoteHistorySingle(useCache, bean, begDate, endDate, klType, fq, retry, timeout);
    }

    /**
     * sse接口. 今日历史1分钟分时 + 不断推送的3s tick
     *
     * @param bean
     * @param callback
     */
    public static void getNDayFsAndRealTimePushWithLeadPriceSSEAsync(SecurityBeanEm bean,
                                                                     SseCallback<DataFrame<Object>> callback) {
        EmQuoteApi.getNDayFsAndRealTimePushWithLeadPriceSSEAsync(bean, callback);
    }

    /**
     * 今日1分钟分时
     *
     * @param bean
     * @param retrySingle
     * @param timeout
     * @param useCache
     * @return
     */
    public static DataFrame<Object> getFs1MToday(SecurityBeanEm bean, int retrySingle,
                                                 int timeout,
                                                 boolean useCache) {
        return EmQuoteApi.getFs1MToday(bean, retrySingle, timeout, useCache);
    }


    private static void initComparePriceTableFields() {
        comparePriceTableFields.putAll(Dict.create()
                .set("f2", "最新")
                .set("f3", "涨跌幅")
                .set("f12", "资产代码")
                .set("f14", "资产名称")
                .set("f26", "上市日期")
                .set("f227", "纯债价值")
                .set("f228", "正股昨收")
                .set("f229", "正股最新")
                .set("f230", "正股涨跌幅")
                .set("f232", "正股代码")
                .set("f234", "正股名称")
                .set("f235", "转股价")
                .set("f236", "转股价值")
                .set("f237", "纯股溢价率")
                .set("f238", "纯债溢价率")
                .set("f239", "回售触发价")
                .set("f240", "强赎触发价")
                .set("f241", "到期赎回价")
                .set("f242", "开始转股日")
                .set("f243", "申购日期")
        );

        /*
        {
          "f1": 3,
          "f2": 136.33, 最新
          "f3": 36.33, 涨跌幅
          "f12": "118005", 代码
          "f13": 1,
          "f14": "N天奈转", 名称
          "f26": 20220301, 上市日期
          "f152": 2,
          "f227": 78.6516, 纯债价值
          "f228": 150.8, 正股昨收
          "f229": 152.6, 正股最新价
          "f230": 1.19, 正股涨跌幅
          "f231": 2,
          "f232": "688116", 正股代码
          "f233": 1,
          "f234": "天奈科技", 正股名称
          "f235": 153.67, 转股价
          "f236": 99.3037, 转股价值
          "f237": 37.29, 转股溢价率
          "f238": 73.33, 纯债溢价率
          "f239": 107.57, 回售触发价
          "f240": 199.77, 强赎触发价
          "f241": 110.0, 到期赎回价
          "f242": 20220809, 开始转股日
          "f243": 20220127 申购日期
        }
         */
    }


    private static void initAllBondBaseDataFields() {
        //所有行
        // ['SECURITY_CODE', 'SECUCODE', 'TRADE_MARKET', 'SECURITY_NAME_ABBR', 'DELIST_DATE', 'LISTING_DATE','CONVERT_STOCK_CODE', 'BOND_EXPIRE', 'RATING', 'VALUE_DATE', 'ISSUE_YEAR', 'CEASE_DATE', 'EXPIRE_DATE', 'PAY_INTEREST_DAY', 'INTEREST_RATE_EXPLAIN', 'BOND_COMBINE_CODE', 'ACTUAL_ISSUE_SCALE', 'ISSUE_PRICE', 'REMARK', 'PAR_VALUE', 'ISSUE_OBJECT', 'REDEEM_TYPE', 'EXECUTE_REASON_HS', 'NOTICE_DATE_HS', 'NOTICE_DATE_SH', 'EXECUTE_PRICE_HS', 'EXECUTE_PRICE_SH', 'RECORD_DATE_SH', 'EXECUTE_START_DATESH', 'EXECUTE_START_DATEHS', 'EXECUTE_END_DATE', 'CORRECODE', 'CORRECODE_NAME_ABBR', 'PUBLIC_START_DATE', 'CORRECODEO', 'CORRECODE_NAME_ABBRO', 'BOND_START_DATE', 'SECURITY_START_DATE', 'SECURITY_SHORT_NAME', 'FIRST_PER_PREPLACING', 'ONLINE_GENERAL_AAU', 'ONLINE_GENERAL_LWR', 'INITIAL_TRANSFER_PRICE', 'TRANSFER_END_DATE', 'TRANSFER_START_DATE', 'RESALE_CLAUSE', 'REDEEM_CLAUSE', 'PARTY_NAME', 'CONVERT_STOCK_PRICE', 'TRANSFER_PRICE', 'TRANSFER_VALUE', 'CURRENT_BOND_PRICE', 'TRANSFER_PREMIUM_RATIO', 'CONVERT_STOCK_PRICEHQ', 'MARKET', 'RESALE_TRIG_PRICE', 'REDEEM_TRIG_PRICE', 'PBV_RATIO', 'IB_START_DATE', 'IB_END_DATE', 'CASHFLOW_DATE', 'COUPON_IR', 'PARAM_NAME', 'ISSUE_TYPE', 'EXECUTE_REASON_SH', 'PAYDAYNEW', 'CURRENT_BOND_PRICENEW']
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
                .set("REDEEM_CLAUSE", "强赎条款")
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
    }

}
