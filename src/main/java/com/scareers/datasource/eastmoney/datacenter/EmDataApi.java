package com.scareers.datasource.eastmoney.datacenter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.datasource.eastmoney.EastMoneyUtil.getAsStrUseHutool;
import static com.scareers.utils.JSONUtilS.jsonStrToDf;

/**
 * description: 东财数据中心api
 *
 * @author: admin
 * @date: 2022/2/25/025-19:12:51
 */
public class EmDataApi {
    public static Map<Object, Object> SuspensionFields = new ConcurrentHashMap<>(); // 停牌字段

    static {
        initSuspensionFields();
    }


    public static void main(String[] args) {
        Console.log("获取停牌股票代码列表");
        Console.log(getSuspensionStockCodes(DateUtil.today(), 2000, 3));
    }

    /**
     * 今日停牌 股票列表 --> 来自数据中心 -- 停复牌
     * https://datacenter-web.eastmoney.com/api/data/v1/get?callback=jQuery112309079454213619864_1645787131827&sortColumns=SUSPEND_START_DATE&sortTypes=-1&pageSize=500&pageNumber=1&reportName=RPT_CUSTOM_SUSPEND_DATA_INTERFACE&columns=ALL&source=WEB&client=WEB&filter=(MARKET%3D%22%E5%85%A8%E9%83%A8%22)(DATETIME%3D%272022-02-25%27)
     *
     * @param date 必须 yyyy-MM-dd 形式
     * @return
     * @cols [资产代码, 资产名称, 停牌开始时间, 停牌结束时间, 停牌期限, 停牌原因, 所属市场, 停牌开始日期, 预计复牌日期, 交易市场代码, 资产类型代码] SuspensionFields
     */
    public static DataFrame<Object> getSuspensions(String date, int timeout, int retry) {
        String url = "https://datacenter-web.eastmoney.com/api/data/v1/get";

        HashMap<String, Object> params = new HashMap<>();
        params.put("callback", "jQuery112309079454213619864_" + (System.currentTimeMillis() - 1));
        params.put("sortColumns", "SUSPEND_START_DATE");
        params.put("sortTypes", "-1");
        params.put("pageSize", "5000");
        params.put("pageNumber", "1");
        params.put("reportName", "RPT_CUSTOM_SUSPEND_DATA_INTERFACE");
        params.put("columns", "ALL");
        params.put("source", "WEB");
        params.put("client", "WEB");
        params.put("filter", StrUtil.format("(MARKET=\"全部\")(DATETIME='{}')", date));

        String response;
        try {
            response = getAsStrUseHutool(url, params, timeout, retry);
        } catch (Exception e) {
            return null;
        }

        List<String> fields = Arrays.asList("SECURITY_CODE", "SECURITY_NAME_ABBR", "SUSPEND_START_TIME",
                "SUSPEND_END_TIME", "SUSPEND_EXPIRE", "SUSPEND_REASON", "TRADE_MARKET", "SUSPEND_START_DATE",
                "PREDICT_RESUME_DATE", "TRADE_MARKET_CODE", "SECURITY_TYPE_CODE");
        DataFrame<Object> dfTemp = jsonStrToDf(response, "(", ")",
                fields,
                Arrays.asList("result", "data"), JSONObject.class, Arrays.asList(),
                Arrays.asList());
        dfTemp = dfTemp.rename(SuspensionFields);
        return dfTemp;
    }

    /**
     * 仅获取停牌股票代码列表
     *
     * @param date
     * @param timeout
     * @param retry
     * @return
     */
    public static List<String> getSuspensionStockCodes(String date, int timeout, int retry) {
        DataFrame<Object> suspensions = getSuspensions(date, timeout, retry);
        if (suspensions == null) {
            return null;
        } else {
            return DataFrameS.getColAsStringList(suspensions, "资产代码");
        }
    }

    private static void initSuspensionFields() {
        SuspensionFields.putAll(Dict.create()
                .set("SECURITY_CODE", "资产代码")
                .set("SECURITY_NAME_ABBR", "资产名称")
                .set("SUSPEND_START_TIME", "停牌开始时间")
                .set("SUSPEND_END_TIME", "停牌结束时间")
                .set("SUSPEND_EXPIRE", "停牌期限")
                .set("SUSPEND_REASON", "停牌原因")
                .set("TRADE_MARKET", "所属市场")
                .set("SUSPEND_START_DATE", "停牌开始日期")
                .set("PREDICT_RESUME_DATE", "预计复牌日期")
                .set("TRADE_MARKET_CODE", "交易市场代码")
                .set("SECURITY_TYPE_CODE", "资产类型代码")
        );
    }
}
