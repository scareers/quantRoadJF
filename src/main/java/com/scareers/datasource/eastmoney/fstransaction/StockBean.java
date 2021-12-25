package com.scareers.datasource.eastmoney.fstransaction;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-20:51:45
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StockBean {
    String stockCodeSimple;
    Integer market; // 0 深市,  1 沪市.   北交所目前数量少, 算 0.

    public StockBean(String fullSrcId) {
        Objects.requireNonNull(fullSrcId);
        List<String> fragments = StrUtil.split(fullSrcId, ".");
        this.stockCodeSimple = fragments.get(1);
        this.market = Integer.valueOf(fragments.get(0));
    }
}
