package com.scareers.datasource.ths.wencai;

import com.alibaba.fastjson.JSONArray;
import joinery.DataFrame;
import lombok.Data;

/**
 * description: 问财api响应结果的 解析结果; 可自定义解析项目; 向后兼容
 *
 * @author: admin
 * @date: 2022/4/3/003-04:00:44
 */
@Data
public class WenCaiResult {
    JSONArray chunksInfo; // 问句解析结果列表: ["同花顺行业指数 (306)","涨跌幅>7% (2)"]
    DataFrame<Object> dfResult; // 详细数据df
}
