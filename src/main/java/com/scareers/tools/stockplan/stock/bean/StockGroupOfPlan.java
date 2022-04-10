package com.scareers.tools.stockplan.stock.bean;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scareers.sqlapi.ThsDbApi;
import com.scareers.tools.stockplan.indusconcep.bean.IndustryConceptThsOfPlan;
import com.scareers.tools.stockplan.indusconcep.bean.dao.IndustryConceptThsOfPlanDao;
import com.scareers.tools.stockplan.stock.bean.selector.StockSelector;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.JSONUtilS;
import joinery.DataFrame;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * description: 自定义板块, 个股组合; 组合维护包含个股, 个股不维护自身所属组! 成分股使用集合!
 *
 * @word: hype: 炒作 / hazy 朦胧 / ebb 退潮 / revival 复兴再起
 * @author: admin
 * @date: 2022/3/28/028-21:38:16
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "plan_of_stock_group",
        indexes = {@Index(name = "dateStr_Index", columnList = "dateStr"),
                @Index(name = "name_Index", columnList = "name")})
public class StockGroupOfPlan {

    public static void main(String[] args) {
        StockGroupOfPlan stockGroupOfPlan = newInstance("测试组1", "测试", "2022-04-10", Arrays.asList("000001"));

        Console.log(stockGroupOfPlan);
    }

    public static StockGroupOfPlan newInstance(String name, String description, String dateStr,
                                               Collection<String> initStockCodes) {
        StockGroupOfPlan bean = new StockGroupOfPlan();
        bean.setName(name);
        bean.setDescription(description);
        bean.setDateStr(dateStr);
        bean.addStockBatch(initStockCodes);
        bean.setGeneratedTime(DateUtil.date());
        return bean;
    }


    /*
    基本字段: 都来自与 ths. industry_list 数据表
     */
    @Id
    @GeneratedValue // 默认就是auto
    @Column(name = "id")
    Long id;
    @Column(name = "name", columnDefinition = "varchar(256)", unique = true)
    String name;// 组名称
    @Column(name = "description", columnDefinition = "varchar(256)", unique = true)
    String description; // 组描述

    @Transient
    HashSet<String> includeStockCodes = new HashSet<>(); // 成分股, 维护 code; 因只包含个股, 所以只需要个股代码即可
    @Column(name = "includeStockCodes", columnDefinition = "longtext")
    String includeStockCodesJsonStr = "[]";

    // 股票组, 在启动程序时, 若当日无bean, 将自动复制上一日所有股票组!
    @Column(name = "dateStr", columnDefinition = "varchar(32)")
    String dateStr; // 视为对哪一个交易日做计划?


    // 自定义字段
    // 1.生成时间和修改时间
    @Column(name = "generatedTime", columnDefinition = "datetime")
    Date generatedTime; // 首次初始化 (new) 时间
    @Column(name = "lastModified", columnDefinition = "datetime")
    Date lastModified; // 手动修改最后时间; 仅仅代表个股添加删除的时间!

    /**
     * 从数据表获取bean时, 需要自动填充 transient 字段: 当前 1
     * 它无视你是否再最新刷新相关字段
     */
    public void initTransientAttrsWhenBeanFromDb() {
        this.initIncludeStockCodesJsonStrWhenBeanFromDb();
    }

    private void initIncludeStockCodesJsonStrWhenBeanFromDb() {
        JSONArray objects = JSONUtilS.parseArray(this.includeStockCodesJsonStr);
        HashSet<String> stringSet = new HashSet<>();
        for (Object object : objects) {
            stringSet.add(object.toString());
        }
        this.includeStockCodes = stringSet;
    }

    /*
    数据api
     */
    public void addStock(String stockCode) {
        this.includeStockCodes.add(stockCode);
        syncJsonStrAttrs();
    }

    public void addStockBatch(Collection<String> stockCodes) {
        if (stockCodes != null) {
            this.includeStockCodes.addAll(stockCodes);
            syncJsonStrAttrs();
        }
    }

    public void removeStock(String stock) {
        try {
            this.includeStockCodes.remove(stock);
            syncJsonStrAttrs();
        } catch (Exception e) {

        }
    }

    public void removeStockBatch(Collection<String> stockCodes) {
        try {
            this.includeStockCodes.removeAll(stockCodes);
            syncJsonStrAttrs();
        } catch (Exception e) {

        }
    }

    private void syncJsonStrAttrs() {
        this.includeStockCodesJsonStr = JSONUtilS.toJsonPrettyStr(includeStockCodes);
        this.lastModified = DateUtil.date();
    }

}