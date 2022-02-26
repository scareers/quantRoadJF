package com.scareers.gui.ths.simulation.strategy.adapter.state.hs;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static com.scareers.utils.SqlUtil.execSql;

/**
 * description: 自定义高卖参数池, 与 DefaultStateArgsPoolHs 配合
 * 对于可自定义的参数, 将先尝试从本池获取, 若无则从 DefaultStateArgsPoolHs 获取全局默认值
 * 对于单天的参数配置, 将json保存到数据库.
 * 初始时, 从数据库获取今日的所有自定义配置项
 *
 * @noti 配置对象需要可json
 * @author: admin
 * @date: 2022/2/25/025-18:45:26
 */
public class CustomizeStateArgsPoolHs {
    public static Connection connOfCustomizeArgs = ConnectionFactory.getConnLocalKlineForms();
    public static String today = DateUtil.today();
    public static String tableNameOfArgsSave = "ths_trader_customize_args";
    private static final Log log = LogUtil.getLogger();


    public static void main(String[] args) {
        cdfRateForPositionHsMap.put("yy", 4.0);
        affectedByIndexHsMap.put("yy", Boolean.TRUE);
        saveAllConfig();
    }


    /*
    个股
     */

    // 1.高卖时的cdf倍率. 可对单只将卖股票设置 --> 股票代码: 倍率值
    public static ConcurrentHashMap<String, Double> cdfRateForPositionHsMap = new ConcurrentHashMap<>();
    public static String cdfRateForPositionHsMapType = "cdfRateForPositionHsMap";

    private static void initCdfRateForPositionHsConfig() {
        initConfigOfMapSD(cdfRateForPositionHsMapType, cdfRateForPositionHsMap);
    }

    public static void saveCdfRateForPositionHsConfig() {
        saveConfig(cdfRateForPositionHsMapType, cdfRateForPositionHsMap);
    }

    // 2.最终额外高卖分布 平移量
    public static ConcurrentHashMap<String, Double> manualMoveDistanceFinallyMap = new ConcurrentHashMap<>();
    public static String manualMoveDistanceFinallyMapType = "manualMoveDistanceFinallyMap";

    private static void initManualMoveDistanceFinallyMapConfig() {
        initConfigOfMapSD(manualMoveDistanceFinallyMapType, manualMoveDistanceFinallyMap);
    }


    public static void saveManualMoveDistanceFinallyMapConfig() {
        saveConfig(manualMoveDistanceFinallyMapType, manualMoveDistanceFinallyMap);
    }


    /*
    指数
     */
    // 1.指数是否影响个股?
    public static ConcurrentHashMap<String, Boolean> affectedByIndexHsMap = new ConcurrentHashMap<>();
    public static String affectedByIndexHsMapType = "affectedByIndexHsMap";

    private static void initAffectedByIndexHsMapConfig() {
        initConfigOfMapSB(manualMoveDistanceFinallyMapType, affectedByIndexHsMap);
    }


    public static void saveAffectedByIndexHsMapConfig() {
        saveConfig(affectedByIndexHsMapType, affectedByIndexHsMap);
    }


    /**
     * 载入配置, 这里仅仅读取 json数据, 实际怎么解析, 由各个具体方法自行解析
     *
     * @param type
     * @param config
     */
    private static String getConfigOf(String type) {
        String sql = StrUtil.format("select * from `{}` where date='{}' and type='{}'", tableNameOfArgsSave, today,
                type);
        try {
            DataFrame<Object> dataFrame = DataFrame.readSql(connOfCustomizeArgs, sql);
            if (dataFrame.length() == 0 || "".equals(dataFrame.get(0, "args_json").toString())) {
                return null;
            } else {
                return dataFrame.get(0, "args_json").toString();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 载入配置, 给定了配置对象的具体java类型, 各个具体方法.
     * 后缀:
     * S -> String
     * D -> Double
     * B -> Boolean
     *
     * @param configType
     * @param configMap
     */
    public static void initConfigOfMapSD(String configType, ConcurrentHashMap<String, Double> configMap) {
        String config = getConfigOf(configType);
        if (config != null) {
            JSONObject jsonObject = JSONUtilS.parseObj(config);
            for (String s : jsonObject.keySet()) {
                configMap.put(s, jsonObject.getDouble(s));
            }
        }
    }

    public static void initConfigOfMapSB(String configType, ConcurrentHashMap<String, Boolean> configMap) {
        String config = getConfigOf(configType);
        if (config != null) {
            JSONObject jsonObject = JSONUtilS.parseObj(config);
            for (String s : jsonObject.keySet()) {
                configMap.put(s, jsonObject.getBoolean(s));
            }
        }
    }

    private static void saveConfig(String type, Object config) {
        String sqlDelete = StrUtil
                .format("delete  from `{}` where date='{}' and type='{}'", tableNameOfArgsSave, today,
                        type);
        try {
            execSql(sqlDelete, connOfCustomizeArgs);
            log.info("删除原配置成功: {}", type);
        } catch (Exception e) {
            log.info("删除原配置失败: {}", type);
            e.printStackTrace();
        }

        DataFrame<Object> dataFrame = new DataFrame<>(Arrays.asList("date", "type", "args_json", "notes"));
        dataFrame.append(Arrays
                .asList(today, type, JSONUtilS.toJsonStr(config), ""));
        try {
            DataFrameS.toSql(dataFrame, tableNameOfArgsSave, connOfCustomizeArgs, "append", null);
            log.info("保存新配置成功: {}", type);
        } catch (SQLException e) {
            log.info("保存新配置失败: {}", type);
            e.printStackTrace();
        }
    }


    static {
        createTableIfNecessary();
        initAllConfig();

        // affectedByIndexHsMap.put("002530", Boolean.FALSE); // 测试

    }

    public static void initAllConfig() {
        initCdfRateForPositionHsConfig();
        initAffectedByIndexHsMapConfig();
        initManualMoveDistanceFinallyMapConfig();
    }

    public static void saveAllConfig() {
        saveCdfRateForPositionHsConfig();
        saveAffectedByIndexHsMapConfig();
        saveManualMoveDistanceFinallyMapConfig();
    }

    /**
     * 不存在则建表, 保存自定义参数的表
     */
    public static void createTableIfNecessary() {
        String sql = StrUtil.format("create table if not exists `{}` \n" +
                "(\n" +
                "    id        int           primary key auto_increment,\n" +
                "    date      varchar(32)   null,\n" +
                "    type      varchar(1024) null,\n" +
                "    args_json longtext      null,\n" +
                "    notes     text          null\n" +
                ")\n" +
                "    comment '交易程序自定义参数保存'", tableNameOfArgsSave);
        try {
            execSql(sql, connOfCustomizeArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
