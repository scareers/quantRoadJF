package com.scareers.tools.stockplan.stock.bean.selector;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.utils.CommonUtil;
import joinery.DataFrame;

import java.util.*;

import static com.scareers.sqlapi.EastMoneyDbApi.getFsTransByDateAndQuoteId;

/**
 * description: 读取分时成交, 筛选其中的典型成交量(即成交量-平均成交量>标准差*某倍数), 分类这些典型成交量, 买盘和买盘比例情况
 * --> 常规的所谓资金流入流出不可控
 * // todo: 完善
 *
 * @author: admin
 * @date: 2022/3/26/026-19:03:44
 */
public class FsTransTypicalBsSelector extends StockSelector {

    public FsTransTypicalBsSelector() {
        this.name = "典型分时成交量买卖方向选股";
        this.description = "读取东财分时成交, 筛选其中的典型成交量(即成交量-平均成交量>标准差*某倍数), " +
                "分类这些典型成交量, 买盘和买盘比例情况";
        this.scoreRule = "计算典型成交量中, (买方向-卖方向) /(买方向+卖方向), 得到净买值比例, 映射到[0,100]即得到score; 可多日,则取平均值";
    }

    int nDays;

    @Override
    public void stockSelect() {

    }

    @Override
    public JSONObject getSelectResultOf(String code) {
        return null;
    }

    @Override
    public void showAllSelectRes() {

    }


    public static void main(String[] args) throws Exception {
//        double factor = typicalVolBsRateFactor("2022-03-25", SecurityBeanEm.createStock("浩洋股份"), 0.5);
//        Console.log(factor);

        main0(1);
    }

    private static void main0(int ndays) throws Exception {
        DataFrame<Object> dataFrame = WenCaiApi
                .wenCaiQuery("个股人气排名前500;流通市值<300亿;非st;非科创板;非创业板");
//        Console.log(dataFrame.columns());
        HashMap<String, Double> codeWithChgPMap = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {

            try {
                if (dataFrame.get(i, "最新涨跌幅") == null) {
                    continue;
                }
                Double chgP = Double.valueOf(dataFrame.get(i, "最新涨跌幅").toString());
                codeWithChgPMap.put(dataFrame.get(i, "code").toString(),
                        chgP
                );
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        List<String> stockCodeList = DataFrameS.getColAsStringList(dataFrame, "code");

        Console.log("获取bean");
        List<SecurityBeanEm> beanEms = SecurityBeanEm.createStockList(stockCodeList, true);
        HashMap<String, List<Double>> res = new HashMap<>();

        Console.log("确定日期");
        String dateStr;
        DateTime now = DateUtil.date();
        if (EastMoneyDbApi.isTradeDate(DateUtil.today())) {
            if (DateUtil.hour(now, true) <= 8) { // 9点之前, 首个日期为上个交易日
                dateStr = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
            } else {
                dateStr = DateUtil.today();
            }
        } else {
            dateStr = EastMoneyDbApi.getPreNTradeDateStrict(DateUtil.today(), 1);
        }

        List<String> dates = new ArrayList<>();
        dates.add(dateStr);
        for (int i = 1; i < ndays; i++) {
            dates.add(EastMoneyDbApi.getPreNTradeDateStrict(dateStr, i));
        }
        Console.log("开始解析");
        // 开始解析
        for (SecurityBeanEm beanEm : beanEms) {
            List<Double> factors = new ArrayList<>();
            for (String date : dates) {

                try {
                    factors.add(typicalVolBsRateFactor(date, beanEm, 0.0));
                } catch (Exception e) {
                    continue;
                }
            }
            res.put(StrUtil.format("{}, __{}", beanEm.getSecCode(), beanEm.getName()), factors);
        }

        List<List<Object>> resList = new ArrayList<>();
        for (String s : res.keySet()) {
            resList.add(Arrays.asList(s, res.get(s)));
        }

        List<List<Object>> res2 = new ArrayList<>();
        for (List<Object> objects : resList) {
            List<Double> doubles1 = (List<Double>) objects.get(1);
            res2.add(Arrays.asList(objects.get(0), CommonUtil.avgOfListNumberUseLoop(doubles1)));
        }

        res2.sort(new Comparator<>() {
            @Override
            public int compare(List<Object> o1, List<Object> o2) {
                Double v1 = (Double) o1.get(1);
                Double v2 = (Double) o2.get(1);
                if (v1 > v2) {
                    return -1;
                } else if (v1 < v2) {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public boolean equals(Object obj) {
                return obj.toString().equals(this.toString());
            }
        });
        for (List<Object> objects : res2) {
            Console.log(objects);
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
            if (amount - avg < typicalStdRate * std) { // 要求成交额 -avg >= 标准差*倍率
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
        if ((buyAmounts + sellAmounts) <= 0) {
            return -2; // 无典型成交量时,
        }
        return (buyAmounts - sellAmounts) / (buyAmounts + sellAmounts);

    }


}
