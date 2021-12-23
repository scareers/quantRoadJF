package com.scareers.gui.ths.simulation.strategy;

import com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import joinery.DataFrame;

import static com.scareers.datasource.eastmoney.fstransaction.FSTransactionFetcher.fsTransactionDatas;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/22/022-20:54:25
 */
public class LowBuyHighSellStrategy {


    public static void dealWith() {
        while (true) {
            for (StockBean stock : fsTransactionDatas.keySet()) {
                DataFrame<Object> fsTransactions = fsTransactionDatas.get(stock);


            }
        }
    }
}
