package com.scareers.sqlapi;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * description: 同花顺数据库sql api
 *
 * @author: admin
 * @date: 2022/3/20/020-01:32:09
 */
public class ThsDbApi {
    public static Connection connection = ConnectionFactory.getConnLocalThs();
    private static Cache<String, Boolean> isTradeDateCache = CacheUtil.newLRUCache(2048);


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
}
