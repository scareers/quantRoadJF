package com.scareers.utils;

/* 替换
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONArray;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONObject;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSON;

import com.scareers.utils.JSONUtilS;
import com.scareers.utils.JSONUtilS;

import cn.hutool.json.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;
import com.scareers.utils.JSONUtilS;




 */


import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * description: 对hutool JSONUtilS相关再封装
 *
 * @author: admin
 * @date: 2021/12/22/022-16:30:37
 */
public class JSONUtilS {
    /**
     * 将json字符串, 解析为df<Object>, 不负责类型转换. 可对原始字符串进行截取. 提供的paths, 即["attr'],
     * 最终索引字符串内容应为 [], 单元素可以是字符串 ","分割(典型dc返回值), 也可 JSONArray.
     * // @noti: 逻辑严谨性由调用方保证, json解析异常等异常, 由调用方捕获处理
     *
     * @param jsonStr              原始需要解析的字符串
     * @param startIntercept       可对原始字符串进行截取, 需要提供开始字符
     * @param endIntercept         可对原始字符串进行截取, 需要提供结束字符
     * @param paths                访问到核心字段的 属性列表
     * @param columns              df 列名称列表
     * @param clazz                核心列表类型为 String.class(需要split), 或者 JSONArray(索引访问)
     * @param dropIndexes          可不保留全部字段, 去掉相关字段的索引列表, 不能为null
     * @param constantColumnValues 可放多个固定值列, 于最前方, 因此必须保证 columns 最前方有相关的列名
     *                             // @noti: 相关参数不可null, 需传递空列表
     * @return
     */
    public static DataFrame<Object> jsonStrToDf(String jsonStr, String startIntercept, String endIntercept,
                                                List<String> columns,
                                                List<String> paths,
                                                Class clazz, List<Integer> dropIndexes,
                                                List<Object> constantColumnValues
    ) {
        DataFrame<Object> res = new DataFrame<>(columns);
        int start = 0, end = jsonStr.length();
        if (startIntercept != null) {
            start = jsonStr.indexOf(startIntercept) + startIntercept.length();
        }
        if (endIntercept != null) {
            end = jsonStr.lastIndexOf(endIntercept);
        }
        jsonStr = jsonStr.substring(start, end);
        JSONObject json = JSON.parseObject(jsonStr);


        JSONArray datas = null;
        try {
//            datas = json.getByPath(StrUtil.join(",", paths), JSONArray.class);
            for (int i = 0; i < paths.size() - 1; i++) {
                json = json.getJSONObject(paths.get(i));
            }
            datas = json.getJSONArray(paths.get(paths.size() - 1));
        } catch (Exception e) {
            Console.log(json); // 典型的json没有数据的情况下
//            throw e;
            return res;
        }


        if (clazz == String.class) {
            for (Object o : datas) {
                String row = o.toString();
                List<String> fields = StrUtil.split(row, ",");
                for (int drop : dropIndexes) {
                    fields.remove(drop);
                }
                List<Object> rowFinal = new ArrayList<>(constantColumnValues);
                rowFinal.addAll(fields);
                res.append(rowFinal);
            }
        } else if (clazz == JSONArray.class) {
            for (int i = 0; i < datas.size(); i++) {
                JSONArray row = datas.getJSONArray(i);
                for (Integer drop : dropIndexes) {
                    row.remove(drop);
                }
                List<Object> rowFinal = new ArrayList<>(constantColumnValues);
                rowFinal.addAll(row);
                res.append(rowFinal);
            }
        } else if (clazz == JSONObject.class) { // 单条记录为 dict的情况, 读取每个字段
            List<String> attrs = columns.subList(constantColumnValues.size(), columns.size()); // 实际的列们
            for (int i = 0; i < datas.size(); i++) {
                JSONObject rowDict = datas.getJSONObject(i); // 字典则无需删除某些列
                List<Object> rowFinal = new ArrayList<>(constantColumnValues);
                for (String attr : attrs) {
                    rowFinal.add(rowDict.get(attr));
                }
                res.append(rowFinal);
            }
        } else {
            LogUtil.log.error("parse type error: 尚未支持的解析类型: {}", clazz);
        }
        return res;
    }

    public static JSONObject parseObj(String jsonStr) {
        return JSON.parseObject(jsonStr);
    }

    public static JSONArray parseArray(String jsonStr) {
        return JSON.parseArray(jsonStr);
    }

    public static String toJsonStr(Object o) {
        return JSON.toJSONString(o);
    }

    public static String toJsonPrettyStr(Object o) {
        return JSON.toJSONString(o, true);
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * fastjson 的 JSONObject 适配 hutool 的 JSONObject.getByPath()
     *
     * @param pathSplitByPoint 形如 "a.b.c" 的路径
     * @return
     */
    public static Object getByPath(JSONObject jsonObject, String pathSplitByPoint) {
        try {
            List<String> paths = StrUtil.split(pathSplitByPoint, ".");
            if (paths.size() == 1) {
                return jsonObject.get(paths.get(0)); // 单个路径, 获取"值"
            }
            // 否则, 多个路径, 最后一路径获取Object, 此前所有路径, 都获取 JSONObject, 持续获取

            JSONObject temp = jsonObject;
            for (int i = 0; i < paths.size() - 1; i++) {
                temp = temp.getJSONObject(paths.get(i));
            }
            return temp.get(paths.get(paths.size() - 1));
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("JSONUtilS getByPath: 返回null,可能路径参数错误: {}", pathSplitByPoint);
        }
        return null;
    }
}