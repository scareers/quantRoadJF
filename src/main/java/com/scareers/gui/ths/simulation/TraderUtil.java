package com.scareers.gui.ths.simulation;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;
import org.checkerframework.common.reflection.qual.NewInstance;

import java.util.List;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/26/026-00:13:09
 */
public class TraderUtil {
    private static final Log log = LogUtils.getLogger();

    /**
     * 单个订单可能有多个响应, 多个 retrying + 1个正式响应. 给定响应列表, 找到响应
     *
     * @param responses
     * @return
     */
    public static JSONObject findFinalResponse(List<JSONObject> responses) {
        JSONObject resFinal = null;
        try {
            for (JSONObject response : responses) {
                if (!"retrying".equals(response.getByPath("state").toString())) {
                    resFinal = response;
                    break; // 找到非retrying 响应
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("json parse error: 查找非retrying响应失败: ", responses);
        }
        return resFinal; // 失败依然返回null
    }

    /**
     * 用于各查询函数, 返回payload字段放入二维数组, 转换df. 约定: 第一行为表头!
     *
     * @return
     */
    public static DataFrame<Object> payloadArrayToDf(JSONObject response) {
        JSONArray datas = response.getJSONArray("payload");
        if (JSONNull.NULL.equals(datas)) { // 注意 NULL 为单例, equals方法将判定null以及是否为解析的单例 JSONNull.NULL
            log.warn("payload empty: 响应payload字段为空, 无法解析为df");
            return null; // 返回null, 调用方应判定
        }
        // 因python api的保证, 必然至少有1行数据, 为表头, 因此不对此做判定
        return payloadArrayToDf(datas);
    }

    public static DataFrame<Object> payloadArrayToDf(JSONArray datas) {
        DataFrame<Object> res;
        // 因python api的保证, 必然至少有1行数据, 为表头, 因此不对此做判定
        List<Object> headers = datas.getJSONArray(0);
        res = new DataFrame<>(headers);
        if (datas.size() > 1) {
            for (int i = 1; i < datas.size(); i++) {
                res.append(datas.getJSONArray(i));
            }
        }
        return res;
    }
}
