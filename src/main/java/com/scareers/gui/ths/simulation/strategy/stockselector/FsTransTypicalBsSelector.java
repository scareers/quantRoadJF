package com.scareers.gui.ths.simulation.strategy.stockselector;

import cn.hutool.core.lang.Console;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;

import java.util.*;

import static com.scareers.sqlapi.EastMoneyDbApi.getFsTransByDateAndQuoteId;

/**
 * description: 读取分时成交, 筛选其中的典型成交量(即成交量-平均成交量>标准差*某倍数), 分类这些典型成交量, 买盘和买盘比例情况
 * --> 常规的所谓资金流入流出不太合理
 *
 * @author: admin
 * @date: 2022/3/26/026-19:03:44
 */
public class FsTransTypicalBsSelector {
    public static void main(String[] args) throws Exception {
        double factor = typicalVolBsRateFactor("2022-03-25", SecurityBeanEm.createStock("海南高速"), 0.5);
        Console.log(factor);

//        main0();
    }

    private static void main0() throws Exception {
        DataFrame<Object> dataFrame = WenCaiApi
                .wenCaiQuery("近5日的区间涨跌幅>-10%且近5日的区间涨跌幅<10%;连续5日的振幅<8%;深市主板或沪市主板;非st的股票；成交额大于1亿;非科创板;非创业板");
        Console.log(dataFrame.columns());
        HashMap<String, Double> codeWithChgPMap = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            codeWithChgPMap.put(dataFrame.get(i, "code").toString(),
                    Double.valueOf(dataFrame.get(i, "最新涨跌幅").toString())
            );
        }

        List<String> stockCodeList = DataFrameS.getColAsStringList(dataFrame, "code")
                .subList(0, 100);

        Console.log("获取bean");
        List<SecurityBeanEm> beanEms = SecurityBeanEm.createStockList(stockCodeList, true);

        ArrayList<ArrayList<Object>> res = new ArrayList<>();
        Console.log("开始解析");
        for (SecurityBeanEm beanEm : beanEms) {
            ArrayList<Object> objects = new ArrayList<>();
            objects.add(beanEm.getSecCode());
            objects.add(typicalVolBsRateFactor("2022-03-25", beanEm, 0.5));
            objects.add(codeWithChgPMap.get(beanEm.getSecCode()));
            objects.add(beanEm.getName());
            res.add(objects);
        }
        Collections.sort(res, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                ArrayList<Object> o11 = (ArrayList<Object>) o1;
                ArrayList<Object> o22 = (ArrayList<Object>) o2;
                Double d1 = (Double) o11.get(1);
                Double d2 = (Double) o22.get(1);

//                Double chg1 = (Double) o11.get(2);
//                Double chg2 = (Double) o22.get(2);


                if (d1 > d2) { // 反序
                    return -1;
                } else if (d1 < d2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (ArrayList<Object> re : res) {
            Console.log(re);
        }
    }

    /**
     * --> 典型成交量买卖方向 比例因子
     * 读取单日分时成交数据, bs字段表示买卖方向;
     * 读取成交量字段, 计算 平均成交量, 和成交量标准差;
     * 设定标准差倍数, 取得 成交量 - 平均成交量 >= 标准差倍数*标准差 的那些成交量列表, 视为 "典型影响股价的成交量";
     * 简而言之: 获取大成交量,它们更能影响股价
     * 遍历典型成交量列表,
     * 区分买卖盘, 计算所有卖盘成交量总和, 卖盘成交量总和
     * 计算 两个总和的差值
     */
    public static double typicalVolBsRateFactor(String date, SecurityBeanEm bean, double typicalStdRate) {
        String quoteId = bean.getQuoteId();
        DataFrame<Object> dfFs = getFsTransByDateAndQuoteId(date, quoteId);

        List<Long> volList = DataFrameS.getColAsLongList(dfFs, "vol");
        List<Double> priceList = DataFrameS.getColAsDoubleList(dfFs, "price");
        List<Object> amountList = new ArrayList<>();
        for (int i = 0; i < volList.size(); i++) {
            amountList.add(priceList.get(i) * volList.get(i) * 100);
        }
        dfFs = dfFs.add("amount", amountList);

        List<Double> amounts = DataFrameS.getColAsDoubleList(dfFs, "amount");
        double avg = CommonUtil.avgOfListNumberUseLoop(amounts);
        double std = CommonUtil.stdOfListNumberUseLoop(amounts, avg);

        double buyAmounts = 0;
        double sellAmounts = 0;
        for (int i = 0; i < dfFs.length(); i++) {
            double amount = Double.parseDouble(dfFs.get(i, "amount").toString());
            if (!(Math.abs(amount - avg) >= typicalStdRate * std)) { // 要求成交额 -avg >= 标准差*倍率
                continue;
            }

            if ("09:25:00".equals(dfFs.get(i, "time_tick").toString())) {
                continue; // 竞价无视
            }
            long bs = Long.parseLong(dfFs.get(i, "bs").toString());

            if (bs == 2) { // 买
                buyAmounts += amount;
            } else if (bs == 1) { // 卖出
                sellAmounts += amount;
            }
        }

        return (buyAmounts - sellAmounts) / (buyAmounts + sellAmounts);

    }
}
