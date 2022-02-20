package com.scareers.gui.ths.simulation;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.StrUtilS;

import java.util.HashMap;
import java.util.Map;

/**
 * description: 对python响应对象简单封装. JSONObject 别名
 *
 * @author: admin
 * @date: 2022/1/10/010-20:50:31
 */
public class Response extends JSONObject {
    //public class JSONObject implements JSON, JSONGetter<String>, Map<String, Object>
    public static void main(String[] args) {
        JSONObject x = new JSONObject();
        x.put("x", "y");

        Response response = new Response(x);

        Console.log(response);
    }

    public Response(JSONObject source) {
        super(source);
    }

    @Override
    public String toString() {
        return StrUtilS.format("Response From Python: \n{}", super.toString());
    }
}
