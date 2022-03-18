package com.scareers.datasource.ths.wencai;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * description: 问财破解尝试. 相关常用查询api.
 *
 * @author: admin
 * @date: 2022/3/19/019-01:03:16
 */
public class WenCaiApi {
    public static void main(String[] args) throws Exception {

//        DataFrame<Object> dataFrame = wenCaiQuery("同花顺概念指数", 5000, 1);
//        Console.log(dataFrame.toString(1000));
//        Console.log(dataFrame.length());

    }



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

    public static String vCode;

    /**
     * data_json["data"]["answer"][0]["txt"][0]["content"]["components"][0]["data"][
     * "datas"
     * ]  为列表, 单项为 {}, key为表头, value为值
     *
     * @throws Exception
     */
    public static DataFrame<Object> wenCaiQuery(String question, int perPage, int page) throws Exception {
        checkVCode();
        String url = "http://www.iwencai.com/unifiedwap/unified-wap/v2/result/get-robot-data";
        getVCode();
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

        String body = request.execute().body();
        JSONObject jsonObject = JSONUtilS.parseObj(body);
        JSONArray datas = jsonObject.getJSONObject("data").getJSONArray("answer").getJSONObject(0)
                .getJSONArray("txt").getJSONObject(0)
                .getJSONObject("content").getJSONArray("components").getJSONObject(0).getJSONObject("data")
                .getJSONArray("datas");

        if (datas.size() == 0) {
            return new DataFrame<>();
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

    private static void checkVCode() {
        if (vCode == null) {
            try {
                vCode = getVCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
