package com.scareers.datasource.eastmoney.fstransaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
