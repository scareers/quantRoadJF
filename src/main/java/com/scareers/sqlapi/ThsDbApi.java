package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * description: 同花顺数据库sql api
 *
 * @author: admin
 * @date: 2022/3/20/020-01:32:09
 */
public class ThsDbApi {
    public static Connection connection = ConnectionFactory.getConnLocalThs();
    private static Cache<String, Boolean> isTradeDateCache = CacheUtil.newLRUCache(2048);

    public static void main(String[] args) {
        Console.log(getStockBelongToConceptsWithName(DateUtil.today()));
    }
    /**
     * @param dateStr 给定日期, 可以不给定, 一般可以给定今天; 标准日期格式
     * @return 读取同花顺概念列表, 返回概念名称列表; 失败返回null ; 已经去重和去null
     */
    public static List<String> getConceptNameList(String dateStr) {
        String sql = StrUtil.format("select name from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }

        List<String> names = DataFrameS.getColAsStringList(dataFrame, "name");
        HashSet<String> nameSet = new HashSet<>();
        for (String name : names) {
            if (name != null) {
                nameSet.add(name);
            }
        }
        return new ArrayList<>(nameSet);
    }

    /**
     * 同上, 返回所有概念 6位简单代码列表
     *
     * @param dateStr
     * @return
     */
    public static List<String> getConceptSimpleCodeList(String dateStr) {
        String sql = StrUtil.format("select code from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }

        List<String> names = DataFrameS.getColAsStringList(dataFrame, "code");
        HashSet<String> nameSet = new HashSet<>();
        for (String name : names) {
            if (name != null) {
                nameSet.add(name);
            }
        }
        return new ArrayList<>(nameSet);
    }

    /**
     * 同上, 返回所有概念map.  key:value --> 概念代码简单: 概念名称
     *
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getConceptCodeWithNameMap(String dateStr) {
        String sql = StrUtil.format("select code,name from concept_list where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        HashMap<String, String> res = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            res.put(dataFrame.get(i, 0).toString(), dataFrame.get(i, 1).toString());
        }
        return res;
    }

    /**
     * 同上反向, name作为key, code为value
     * @param dateStr
     * @return
     */
    public static HashMap<String, String> getConceptNameWithCodeMap(String dateStr) {
        HashMap<String, String> conceptCodeWithNameMap = getConceptCodeWithNameMap(dateStr);
        HashMap<String, String> res = new HashMap<>();
        for (String s : conceptCodeWithNameMap.keySet()) {
            res.put(conceptCodeWithNameMap.get(s),s);
        }
        return res;
    }

    /**
     * 给定日期, 返回所有 股票名称,代码, 所属概念列表
     * 实测概念包含非常规概念
     *
     * @param dateStr
     * @return
     * @cols code	  name	  belongToConceptAll
     */
    public static DataFrame<Object> getStockBelongToConceptsWithName(String dateStr) {
        String sql = StrUtil.format("select code,name,belongToConceptAll from stock_belong_to_industry_and_concept " +
                "where dateStr='{}'", dateStr);
        DataFrame<Object> dataFrame;
        try {
            dataFrame = DataFrame.readSql(connection, sql);
        } catch (SQLException e) {
            return null;
        }
        return dataFrame;
    }
}
