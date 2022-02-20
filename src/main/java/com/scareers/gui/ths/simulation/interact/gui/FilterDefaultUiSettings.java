package com.scareers.gui.ths.simulation.interact.gui;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;

import java.util.HashMap;

/**
 * description: 筛选默认的 ui设置 json, 需要给定 key前缀, 常为组件类名
 *
 * @author: admin
 * @date: 2022/1/12/012-21:29:28
 */
public class FilterDefaultUiSettings {
    public static void main(String[] args) {
        Console.log(filter("InternalFrame"));

    }

    public static String filter(String filterStr) {
        String settings = ResourceUtil.readUtf8Str("default_ui/UIDefaults.json");
        JSONObject json = JSONUtilS.parseObj(settings);

        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        for (String key : json.keySet()) {
//            if (key.contains(filterStr) || key.contains(filterStr.toLowerCase())) {
            if (key.toLowerCase().startsWith(filterStr.toLowerCase())) {
                stringObjectHashMap.put(key, json.get(key));
            }
        }
        return JSONUtilS.toJsonPrettyStr(stringObjectHashMap);
    }
}
