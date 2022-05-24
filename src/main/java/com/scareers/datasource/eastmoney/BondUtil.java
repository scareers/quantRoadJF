package com.scareers.datasource.eastmoney;

import cn.hutool.core.date.*;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.datacenter.EmDataApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.datasource.ths.wencai.WenCaiApi;
import com.scareers.gui.ths.simulation.trader.StockBondBean;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
         * 重大事项带债
         */
//        List<StockBondBean> temp = getAllStockWithBondWithMajorIssueNow();
//        for (StockBondBean stockBondBean : temp) {
//            Console.log(stockBondBean.getStockCode());
//        }
//
//        Console.log("xxxxxxxxxxxxxx");
//
//
//
//        /*
//         * 给定转债名称列表, 打印对应股票名称列表
//         */
//        printStockNameListOfCareBonds();

//        Console.log(getStockCodeWithBondNameFromUseWenCai());

        generateCSVForRecite1();
    }

    /**
     * 背诵时股票信息, 维护字段
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockInfo {
        String code;
        String name;
        String industries;
        String concepts;
        Double pe;
        Double marketValue; // 流通
    }

    /**
     * 背诵csv生成
     */
    public static void generateCSVForRecite1() {
        // 1.所有带转债股票代码; 股票代码:转债名称, 这里只用到key
        ConcurrentHashMap<String, String> stockCodeWithBondNameFromUseWenCai = getStockCodeWithBondNameFromUseWenCai();
        String dateStr = DateUtil.format(DateUtil.offset(DateUtil.date(), DateField.DAY_OF_MONTH, -1), "yyyyMMdd"); //
        // 问财形式日期

        // 2. 构建股票代码: 股票背诵字段对象map, 并解析填充
        HashMap<String, StockInfo> stockWithInfoMap = new HashMap<>();
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("所属概念;所属行业", WenCaiApi.TypeStr.ASTOCK);
        // Console.log(dataFrame.columns());
        // code,股票简称,所属同花顺行业,所属概念,市盈率(pe)[20220524],a股市值(不含限售股)[20220524]  -> 股票基本
        for (int i = 0; i < dataFrame.length(); i++) {
            try {
                StockInfo info = new StockInfo();
                String code = dataFrame.get(i, "code").toString().substring(0, 6);
                if (!stockCodeWithBondNameFromUseWenCai.containsKey(code)) {
                    continue;
                }
                info.setCode(code);
                info.setName(dataFrame.get(i, "股票简称").toString());
                info.setIndustries(dataFrame.get(i, "所属同花顺行业").toString());
                info.setConcepts(dataFrame.get(i, "所属概念").toString());

                Double pe = null;
                try {
                    pe = Double.valueOf(dataFrame.get(i, StrUtil.format("市盈率(pe)[{}]", dateStr)).toString());
                } catch (NumberFormatException e) {

                }
                info.setPe(pe);

                Double marketValue = null;
                try {
                    marketValue = Double
                            .valueOf(dataFrame.get(i, StrUtil.format("a股市值(不含限售股)[{}]", dateStr)).toString());
                } catch (NumberFormatException e) {

                }
                info.setMarketValue(marketValue);
                stockWithInfoMap.put(code, info);
            } catch (Exception e) {
                //                e.printStackTrace();
                // 某些转债会 某些值会null, 引发异常, 这里无视
            }
        }
        // 带债股票信息map生成完成
        // Console.log(stockWithInfoMap.size());

        // 3. 转债信息map构造, 需要持有股票 code


    }

    /**
     * 读取关注转债文件列表, 打印正股列表
     */
    public static void printStockNameListOfCareBonds() {
        String s = ResourceUtil.readUtf8Str("bonds.txt");
        List<String> bonds = StrUtil.split(s, "\r\n");
        bonds.remove("");
        printStockNameListOfBonds(bonds);
    }

    /**
     * 给定转债名称列表, 打印出对应的股票名称列表, 以供复制让同花顺识别
     */
    public static void printStockNameListOfBonds(List<String> bonds) {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("剩余规模<100亿;上一交易日成交额排名从大到小前500;正股代码",
                WenCaiApi.TypeStr.BOND); // 全部转债
        HashMap<String, String> bondWithStockName = new HashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {
            try {
                bondWithStockName.put(
                        dataFrame.get(i, "可转债@可转债简称").toString(),
                        dataFrame.get(i, "可转债@正股代码").toString().substring(0, 6)
                );
            } catch (Exception e) {

            }
        }

        for (String bond : bonds) {
            Console.log(bondWithStockName.get(bond));
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
        List<StockBondBean> hotStockWithBond = getAllStockWithBond0();
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

        /*
                String today = DateUtil.format(equivalenceNow, DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(equivalenceNow, true) >= 15) { // 超过下午3点
                return getNewsForTradePlanByDate(today);
            }
        }
        // 其他情况均获取上一交易日. 因为非交易日没有数据
        String preDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        List<CompanyGoodNew> res = new ArrayList<>();
        DateRange range = new DateRange(DateUtil.parse(preDate), DateUtil.parse(today), DateField.DAY_OF_MONTH,
                1, true, false); // 此时不包含今日
        for (DateTime dateTime : range) { // 这里将是 上一交易日 到 绝对的 昨天, 0点.
            res.addAll(getNewsForTradePlanByDate(DateUtil.format(dateTime, DatePattern.NORM_DATE_PATTERN)));
        }
         */
    }

    /**
     * 结合重大事项新闻的发布时间, 以及当前时间, 获取 适当日期列表的 带可转债的带重大事项的 可转债股票对象
     * 合适的日期列表, 算法同 MajorIssueDao 中的算法
     *
     * @param dateStr
     * @return
     * @throws SQLException
     */
    public static List<StockBondBean> getAllStockWithBondWithMajorIssueNow() throws SQLException {
        String today = DateUtil.format(DateUtil.date(), DatePattern.NORM_DATE_PATTERN);
        if (EastMoneyDbApi.isTradeDate(today)) {
            if (DateUtil.hour(DateUtil.date(), true) >= 15) { // 超过下午3点
                return getAllStockWithBondWithMajorIssue(today);
            }
        }
        // 其他情况均获取上一交易日. 到今日
        String preDate = EastMoneyDbApi.getPreNTradeDateStrict(today, 1);
        List<StockBondBean> res = new ArrayList<>();
        DateRange range = new DateRange(DateUtil.parse(preDate), DateUtil.parse(today), DateField.DAY_OF_MONTH,
                1, true, false); // 此时不包含今日
        for (DateTime dateTime : range) { // 这里将是 上一交易日 到 绝对的 昨天, 0点.
            res.addAll(getAllStockWithBondWithMajorIssue(DateUtil.format(dateTime, DatePattern.NORM_DATE_PATTERN)));
        }
        return res;
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
        List<StockBondBean> hotStockWithBond = getAllStockWithBond0();

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
    private static List<StockBondBean> getAllStockWithBond0() {
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
        return stockBondBeanList;
    }


    /**
     * 问财获取所有转债; 返回 股票代码: 转债名称 字典
     *
     * @return
     */
    public static ConcurrentHashMap<String, String> getStockCodeWithBondNameFromUseWenCai() {
        DataFrame<Object> dataFrame = WenCaiApi.wenCaiQuery("正股代码;正股简称;成交额;",
                WenCaiApi.TypeStr.BOND);
        ConcurrentHashMap<String, String> res = new ConcurrentHashMap<>();
        for (int i = 0; i < dataFrame.length(); i++) {


            try {
                res.put(
                        dataFrame.get(i, "可转债@正股代码").toString().substring(0, 6),
                        dataFrame.get(i, "可转债@可转债简称").toString()
                );
            } catch (Exception e) {
//                e.printStackTrace();
                // 某些转债会 某些值会null, 引发异常, 这里无视
            }

        }

        return res;
    }
}
