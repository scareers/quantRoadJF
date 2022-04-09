package com.scareers.tools.stockplan.stock.bean.selector;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.log.LogUtil;
import lombok.Data;

/**
 * description: 匹配 StockOfPlan 的选股抽象类;
 * 匹配其 selectorMap 属性;
 *
 * @author: admin
 * @date: 2022/4/9/009-18:53:48
 */
@Data
public abstract class StockSelector {
    protected String name;
    protected String description;
    protected String scoreRule; // 打分规则

    public StockSelector() {
        this.name = this.getClass().getSimpleName();
        this.description = "";
        this.scoreRule = "";
    }
    protected static final Log log = LogUtil.getLogger();

    public abstract void stockSelect(); // 执行选股, 一般将所有结果保存在 子类新属性中;

    /**
     * code为单只个股, 得到本选股器的选股结果 json对象!
     * 一般将结果保存在 map中. 读取map即可
     *
     * @noti : 结果一般有 score 键, 表示选择该股分数 0-100
     */
    public abstract JSONObject getSelectResultOf(String code);

    public abstract void showAllSelectRes(); // 显示所有选股结果
}
