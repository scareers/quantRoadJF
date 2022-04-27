package com.scareers.datasource.eastmoney;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * description: 可转债相关api
 *
 * @author: admin
 * @date: 2022/4/27/027-00:06:56
 */
public class BondUtil {
    public static void main(String[] args) throws Exception {
//        main0();


        /*
        中矿转债
        小康转债
        天壕转债
        温氏转债
        拓尔转债
        韦尔转债
        科伦转债
        博世转债
         */
//        List<StockBondBean> temp = getAllStockWithBondWithMajorIssue("2022-04-26");
//        for (StockBondBean stockBondBean : temp) {
//            Console.log(stockBondBean.getBondCode());
//        }

//        List<StockBondBean> allStockWithBondWithAnn = getAllStockWithBondWithAnn();
//        Console.log(allStockWithBondWithAnn.size());
//        for (StockBondBean stockBondBean : allStockWithBondWithAnn) {
//            Console.log(stockBondBean);
//            Console.log(stockBondBean.getStockCode());
//        }

        List<String> allStockWithBondWithAnn2 = getAllStockWithBondWithAnnUseWenCai(DateUtil.date());
        for (String s : allStockWithBondWithAnn2) {
//            Console.log(stockBondBean);
            Console.log(s);
        }
    }


    /**
     * 给定日期字符串, 返回 东财, 当日的 重大事项 收录的所有股票, 带可转债的
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<StockBondBean> getAllStockWithBondWithMajorIssue(String dateStr) throws SQLException {
        List<StockBondBean> hotStockWithBond = getHotStockWithBond0();
        DataFrame<Object> dataFrame1 = DataFrame.readSql(ConnectionFactory.getConnLocalEastmoney(),
                StrUtil.format("select *\n" +
                        "from company_major_issue cmi\n" +
                        "where dateStr = '{}'", dateStr));
        List<String> stockNameList = DataFrameS.getColAsStringList(dataFrame1, "name");
        List<StockBondBean> hotStockWithBondWithMajorIssue = new ArrayList<>();
        for (StockBondBean stockBondBean : hotStockWithBond) {
            if (stockNameList.contains(stockBondBean.getStockName())) {
                hotStockWithBondWithMajorIssue.add(stockBondBean);
            }
        }
        return hotStockWithBondWithMajorIssue;
    }

    /**
     * 给定日期字符串, 使用问财, 访问该日期最新带可转债的股票, 发布了公告的 股票代码列表
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<String> getAllStockWithBondWithAnnUseWenCai(Date date) throws SQLException {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery(StrUtil.format("{}发布公告;带可转债",
                DateUtil.format(date, "yyyy年MM月dd日")
                ),
                WenCaiApi.TypeStr.ASTOCK);
        if (dataFrame == null) {
            return null;
        }
        return DataFrameS.getColAsStringList(dataFrame, "code");
    }

    /**
     * 给定日期字符串, 返回 东财, 当日 发步过公告的 所有股票, 带可转债的
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<StockBondBean> getAllStockWithBondWithAnn() throws SQLException {
        List<StockBondBean> hotStockWithBond = getHotStockWithBond0();

        DateTime stdDate = DateUtil.date(); // 此刻
        if (DateUtil.hour(stdDate, true) >= 15) {
            // 下一天, 当然可能下一天不是公告日, 也没关系
            stdDate = DateUtil.offset(stdDate, DateField.DAY_OF_MONTH, 1);
        }
        stdDate = DateUtil.parseDate(DateUtil.format(stdDate, DatePattern.NORM_DATE_PATTERN)); // 只保留年月日

        List<StockBondBean> res = new ArrayList<>();
        for (StockBondBean stockBondBean : hotStockWithBond) {
            String stockCode = stockBondBean.getStockCode();
            List<Date> announcementOfOneStockDateTimes = EmDataApi
                    .getAnnouncementOfOneStockDateTimes(stockCode, 1, 3000, 2);
            if (announcementOfOneStockDateTimes == null || announcementOfOneStockDateTimes.size() == 0) {
                continue;
            }

            Date latestDate = announcementOfOneStockDateTimes.get(0); // 某股票, 最新一篇公告的 日期, 往往 00:00:00 最新的一天开始
            if (DateUtil.compare(latestDate, stdDate) >= 0) { // 新发布公告>=标准日期(某天的凌晨0点)
                res.add(stockBondBean);
            }

        }
        return res;
    }

    /**
     * 构建所有可转债的列表, 使用问财
     *
     * @return
     */
    private static List<StockBondBean> getHotStockWithBond0() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("正股代码;正股简称;成交额;",
                WenCaiApi.TypeStr.BOND);
        List<StockBondBean> stockBondBeanList = new ArrayList<>();
//        Console.log(dataFrame);
        for (int i = 0; i < dataFrame.length(); i++) {

            try {
                stockBondBeanList.add(new StockBondBean(
                        dataFrame.get(i, "可转债@正股简称").toString(),
                        dataFrame.get(i, "可转债@正股代码").toString().substring(0, 6),
                        dataFrame.get(i, "可转债@可转债简称").toString(),
                        dataFrame.get(i, "code").toString(),
                        -1
                ));
            } catch (Exception e) {
//                e.printStackTrace();
                // 某些转债会 某些值会null, 引发异常, 这里无视
            }

        }
        Console.log(stockBondBeanList.get(0));
        Console.log(stockBondBeanList.size());
        return stockBondBeanList;
    }
}
