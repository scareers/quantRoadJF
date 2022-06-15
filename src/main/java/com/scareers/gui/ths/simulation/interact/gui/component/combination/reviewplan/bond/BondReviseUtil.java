package com.scareers.gui.ths.simulation.interact.gui.component.combination.reviewplan.bond;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.BondUtil;
import com.scareers.datasource.eastmoney.SecurityBeanEm;
import com.scareers.gui.ths.simulation.interact.gui.component.combination.DisplayPanel;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.PcHotNewEm;
import com.scareers.tools.stockplan.news.bean.dao.PcHotNewEmDao;
import com.scareers.utils.CommonUtil;
import com.scareers.utils.ai.tts.Tts;
import com.scareers.utils.charts.EmChartFs;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * description: 重载, 减少主类行数, 降低idea负担
 *
 * @author: admin
 * @date: 2022/6/11/011-14:39:17
 */
public class BondReviseUtil {

    public static final boolean infoRightAndFsLeft = true; // 控制分时在左,信息在右, 同同花顺; 否则左右反转

    public static final int tick3sLogPanelWidth = EmChartFs.DynamicEmFs1MV2ChartForRevise.tickLogPanelWidthDefault; // 3stick数据显示组件宽度
    public static final double timeRateDefault = 1.0; // 默认复盘时间倍率
    // 转债全列表, 是否使用问财实时列表; 若不, 则使用数据库对应日期列表; @noti: 目前问财的成交额排名, 似乎有bug, 无法排名正确
    public static final boolean bondListUseRealTimeWenCai = true;
    public static final boolean loadAllFsDataFromDbWhenFlushBondList = true; // @key: 更新转债列表显示时, 是否载入所有fs数据
    public static List<String> bondTableColNames = Arrays.asList("代码", "名称", "涨跌幅", "成交额", "短速", "短额");
    public static int bondShortTermChgPctSecondRange = 30; // 转债短期的 涨跌幅成交额, 使用近 多少秒内的df, 计算涨跌幅
    public static int bondShortTermAmountSecondRange = 30; // 转债短期的 总成交额, 使用近 多少秒内的df, 计算成交额!

    // 主循环因代码执行而损耗的时间, 修正每次循环的sleep值, 以符合理论! 将自动调整sleep值! <-- 2毫秒约等于循环的代码执行(排除sleep)时间,
    public static volatile long codeExecLossSleepFixSimulation = 2;
    public static final int kLineAmountHope = 170; // k线图希望的数量
    // 今天多少点钟后, 复盘默认日期设置为今天(一般这时爬虫运行过了,数据库有数据了) , 否则设置默认复盘日期为上一交易日
    public static final int afterTodayNHDefaultDateAsToday = 20;
    public static final double accountInitMoney = 10 * 10000;
    public static final double accountStateFlushSleep = 1500; // 子线程不断刷新账户状态, 时间间隔
    public static final double buySellPriceBias = 5.0; // 买卖时, 挂单价格, 在最新价格的偏移价格! 元
    public static final String nuclearKeyBoardSettingDstDir = "C:\\Users\\admin\\Desktop\\自设键盘"; // 核按钮键盘配置文件目标文件夹
    public static final String nuclearKeyBoardSettingOfThs = "ths/nuclear/nuclear/raw"; // 核按钮键盘配置文件 -- classpath中路径 --    // 同花顺原配置
    public static final String nuclearKeyBoardSettingOfRevise = "ths/nuclear/nuclear/revise"; // 核按钮键盘配置文件 -- classpath中路径 -- 复盘时使用配置
    public static final long dummyBuySellOperationSleep = 400; // 模拟交易的弹窗持续的时间
    public static final long dummyClinchOccurSleep = 1500; // 模拟买卖后, 到成交时间,大约sleep多久, 在上个sleep之后

    public static final int indexKlineAmountHopeY = 120; // 查看昨日指数时,k线期望的数量

    public static void main(String[] args) {
//        playClinchSuccessSound();
//        playClinchFailSound();

        notifyNewestHotNewRevise("06:32:48", "2022-06-14");
    }


    // 复盘日期字符串: 当日所有时间的 热门资讯列表 --> 推送时间tick: 新闻对象!
    // 因开盘时间 9:30 -15:00 , 直接按照日期读取, 符合逻辑!
    // 而复盘时, 盘前的 热门资讯阅读, 自行 查看大势资讯, (锁定大势资讯的时间, 为复盘时间即可)
    public static ConcurrentHashMap<String, HashMap<String, PcHotNewEm>> hotNewMapOfDate = new ConcurrentHashMap<>(); // 充当数据缓存
    public static int hotNewDelay = 2000; // 检测到最新热门资讯时, 强制延迟2s才真正提示! 模拟现实延迟


    /**
     * @key3 因为有模拟延迟, 请在子线程中调用!
     * 复盘环境下, 最新热门资讯 尝试提示.
     * 需要给定 "当前复盘时间"!
     */
    public static void notifyNewestHotNewRevise(String reviseTimeTick, String reviseDateStr) {
        // 1.数据载入!
        if (hotNewMapOfDate.get(reviseDateStr) == null) {
            List<PcHotNewEm> newsOfThisDate;
            try {
                newsOfThisDate = PcHotNewEmDao.getNewsOfThisDate(reviseDateStr);
            } catch (SQLException e) {
                CommonUtil.notifyError("获取当日所有热门资讯失败: " + reviseDateStr);
                return;
            }
            if (newsOfThisDate == null) {
                CommonUtil.notifyError("获取当日所有热门资讯失败: " + reviseDateStr);
                return;
            }
            HashMap<String, PcHotNewEm> pushTimeToBeanMap = new HashMap<>();
            for (PcHotNewEm pcHotNewEm : newsOfThisDate) {
                pushTimeToBeanMap.put(pcHotNewEm.getPushtime(), pcHotNewEm);
            }
            hotNewMapOfDate.put(reviseDateStr, pushTimeToBeanMap);
        }
        HashMap<String, PcHotNewEm> stringPcHotNewEmHashMap = hotNewMapOfDate.get(reviseDateStr);
        PcHotNewEm pcHotNewEm = stringPcHotNewEmHashMap.get(reviseDateStr + " " + reviseTimeTick);
        if (pcHotNewEm != null) { // 当前复盘时间, 恰好有最新消息!!! 应当给与提示
            ThreadUtil.sleep(hotNewDelay);
            Tts.playSound("热", true);
            CommonUtil.notifyInfo("最新热门资讯:\n" + pcHotNewEm.toString()); //
            guiNotify("最新热门资讯", pcHotNewEm);
            ThreadUtil.sleep(3000);
        }
    }

    /**
     * 最新热门资讯, gui提示
     *
     * @param type 是财经导读或资讯精华
     * @param item
     */
    public static void guiNotify(String type, PcHotNewEm item) {
        ThreadUtil.execAsync(new Runnable() {
            @Override
            public void run() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<html>");
                stringBuilder.append("<h3 color=\"red\">");
                stringBuilder.append(item.getTitle());
                stringBuilder.append("</h3>");
                stringBuilder.append("<br>");
                stringBuilder.append("<p color=\"black\">");

                // 分行加入
                String content = item.getDigest();
                int line = (int) Math.ceil(content.length() / 30.0); // 行数
                for (int i = 0; i < line; i++) {
                    stringBuilder.append(content, i * 30, Math.min((i + 1) * 30, content.length()));
                    stringBuilder.append("<br>");
                }

                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<br>");

                stringBuilder.append("<p color=\"red\">");
                stringBuilder.append("--- ").append(item.getShowtime());
                stringBuilder.append("</p>");
                stringBuilder.append("<p color=\"red\">");
                stringBuilder.append("--- ").append(item.getPushtime());
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<p color=\"red\">");
                stringBuilder.append("--- ").append(item.getPushtime());
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("<p color=\"red\">");
//                <a href="url">链接文本</a>
                stringBuilder.append(StrUtil.format("<a href=\"{}\">", item.getUrl()));
                stringBuilder.append(item.getUrl());
                stringBuilder.append("</a>");
                stringBuilder.append("</p>");
                stringBuilder.append("<br>");
                stringBuilder.append("</html>");
                JOptionPane
                        .showMessageDialog(null, stringBuilder.toString(), type + "-- 新闻发现", JOptionPane.PLAIN_MESSAGE);
            }
        }, true);
    }


    /**
     * 初始化复盘软件使用的 核按钮配置!
     */
    public static void initNuclearKeyBoardSettingForRevise() {
        try {
            String x = CommonUtil.getFullPathOfClassPathFileOrDir(nuclearKeyBoardSettingOfRevise);
            FileUtil.copyFilesFromDir(FileUtil.file(x), FileUtil.file(nuclearKeyBoardSettingDstDir), true);
            CommonUtil.notifyInfo("核按钮键盘已更改配置: 复盘配置");
        } catch (IORuntimeException e) {
            CommonUtil.notifyError("核按钮键盘更改配置失败 --> 复盘配置");
        }
    }

    /**
     * 恢复核按钮配置, 到同花顺配置
     */
    public static void recoverNuclearKeyBoardSettingToThs() {
        try {
            String x = CommonUtil.getFullPathOfClassPathFileOrDir(nuclearKeyBoardSettingOfThs);
            FileUtil.copyFilesFromDir(FileUtil.file(x), FileUtil.file(nuclearKeyBoardSettingDstDir), true);
            CommonUtil.notifyInfo("核按钮键盘已恢复配置: 同花顺配置");
        } catch (IORuntimeException e) {
            CommonUtil.notifyError("核按钮键盘恢复配置失败 --> 同花顺配置");
        }
    }

    /**
     * //@key: 重要方法, 给定完整的 fs成交Df, 给定两个常态时间tick字符串, 返回 介于两个tick之间的部分df,
     * 新建的df对象; 且两个tick都可能包括(如果恰好有数据的话)!
     *
     * @param fsTransDfFull
     * @param startTick
     * @param endTick
     * @return
     */
    public static DataFrame<Object> getEffectDfByTickRange(DataFrame<Object> fsTransDfFull, String startTick,
                                                           String endTick) {
        if (fsTransDfFull == null) {
            return null;
        }
        // @update: 需要>=9:30:00, 否则竞价的全部进去了, 不合事实
        int shouldStartIndex = -1;
        for (int i = 0; i < fsTransDfFull.length(); i++) {
            String timeTick1 = fsTransDfFull.get(i, "time_tick").toString();
            if (timeTick1.compareTo(startTick) > 0) {
                break; // 不要>=, 本索引包含, 将包含 09:30:00 这个tick;
            } else {
                shouldStartIndex = i; // 本质上, 会包含 <=9:30:00 的第一个tick, 符合事实!
            }
        }
        if (shouldStartIndex == -1) {
            return null;
        }

        // 3. 筛选有效分时成交! time_tick 列
        int shouldIndex = -1;
        for (int i = 0; i < fsTransDfFull.length(); i++) {
            String timeTick1 = fsTransDfFull.get(i, "time_tick").toString();
            if (timeTick1.compareTo(endTick) <= 0) {
                shouldIndex = i; // 找到截断索引
            } else {
                break;
            }
        }
        if (shouldIndex == -1) {
            return null; // 筛选不到
        }
        if (shouldStartIndex > shouldIndex) { // 例如时间< 9:30时, 将会
            return null;
        }
        return DataFrameS.copy(fsTransDfFull
                .slice(shouldStartIndex, Math.min(shouldIndex + 1, fsTransDfFull.length())));
    }

    /**
     * 复盘期间, 给定转债列表, 给定 日期和时间, 生成 转债"实时"数据截面列表df, 被table展示
     * 模拟实盘下, 点击全债券列表的展示页面, 对整个市场有个宏观展示
     * 当前仅展示 涨幅(很好计算) 和 当前总成交额(需要分时成交求和, 计算量稍大)
     *
     * @param bondList
     * @return
     */
    public static DataFrame<Object> getReviseTimeBondListOverviewDataDf(List<SecurityBeanEm> bondList, String date,
                                                                        String timeTick) {
        HashMap<SecurityBeanEm, Double> chgPctRealTime = new HashMap<>(); // 涨跌幅
        HashMap<SecurityBeanEm, Double> amountRealTime = new HashMap<>(); // 成交额

        // @add: 短期涨跌幅, 以及短期的 成交额总和!
        HashMap<SecurityBeanEm, Double> chgPctShortTermRealTime = new HashMap<>(); // 短期涨跌幅变化, 以昨收为基准
        HashMap<SecurityBeanEm, Double> amountShortTermRealTime = new HashMap<>(); // 短期成交额汇总

        DateTime shortTermChgPctStartTick = getStdDateTimeOffset(date, timeTick, -bondShortTermChgPctSecondRange);
        DateTime shortTermAmountStartTick = getStdDateTimeOffset(date, timeTick, -bondShortTermAmountSecondRange);


        for (SecurityBeanEm bondBean : bondList) {
            // 1.昨收, 计算涨跌幅
            Double preClose = EastMoneyDbApi.getPreCloseOf(date, bondBean.getQuoteId());
            if (preClose == null) {
                continue;
            }
            // 2.分时成交
            DataFrame<Object> fsTransDf = EastMoneyDbApi
                    .getFsTransByDateAndQuoteIdS(date, bondBean.getQuoteId(), false);
            if (fsTransDf == null) {
                continue;
            }

            DataFrame<Object> effectDf = getEffectDfByTickRange(fsTransDf, "09:29:59", timeTick);
            if (effectDf == null || effectDf.length() == 0) {
                continue;
            }
            // 4.涨跌幅很好计算
            Double newestPrice = Double.valueOf(effectDf.get(effectDf.length() - 1, "price").toString());
            chgPctRealTime.put(bondBean, newestPrice / preClose - 1);

            // 5.总计成交额, 需要强行计算! price 和 vol 列, vol手数 需要转换为 张数, *10
            amountRealTime.put(bondBean, getAmountOfEffectDf(bondBean, effectDf));

            // 6.短期的涨速
            DataFrame<Object> shortEffectDfOfChgPct = getEffectDfByTickRange(fsTransDf,
                    DateUtil.format(shortTermChgPctStartTick,
                            DatePattern.NORM_TIME_PATTERN)
                    , timeTick);
            if (shortEffectDfOfChgPct == null || shortEffectDfOfChgPct.length() == 0) {
                continue;
            }
            Double price0 = Double.valueOf(shortEffectDfOfChgPct.get(0, "price").toString());
            chgPctShortTermRealTime.put(bondBean, (newestPrice - price0) / preClose);

            // 7.短期的成交额
            DataFrame<Object> shortEffectDfOfAmount = getEffectDfByTickRange(fsTransDf,
                    DateUtil.format(shortTermAmountStartTick,
                            DatePattern.NORM_TIME_PATTERN)
                    , timeTick);
            if (shortEffectDfOfAmount == null || shortEffectDfOfAmount.length() == 0) {
                continue;
            }
            amountShortTermRealTime.put(bondBean, getAmountOfEffectDf(bondBean, shortEffectDfOfAmount));
        }


        // 1.构建结果df! 列简单: 代码,名称, 涨跌幅, 当前总成交额!
        DataFrame<Object> res = new DataFrame<>(bondTableColNames);
        for (SecurityBeanEm beanEm : bondList) {
            List<Object> row = new ArrayList<>();
            row.add(beanEm.getSecCode());
            row.add(beanEm.getName());
            row.add(chgPctRealTime.get(beanEm));
            row.add(amountRealTime.get(beanEm)); // 涨跌幅成交额都可能是 null, 但保证需要所有转债;
            row.add(chgPctShortTermRealTime.get(beanEm)); // 涨跌幅成交额都可能是 null, 但保证需要所有转债;
            row.add(amountShortTermRealTime.get(beanEm)); // 涨跌幅成交额都可能是 null, 但保证需要所有转债;
            res.append(row);
        }

        // 2. 无需排序, 自行使用 JXTable 的排序功能! 但转换为数字排序, 是需要重新一下排序逻辑的, 默认按照字符串排序
        //Console.log(res.toString(10));
        return res;
    }

    /**
     * 给定日期, 给定改变的秒数, (一般负数), 返回在 09:30:00 - 15:00:00 之间的合理 时间
     */
    public static DateTime getStdDateTimeOffset(String date,
                                                String timeTick,
                                                int offsetSecond) {
        DateTime parse = DateUtil.parse(date + " " + timeTick);
        DateTime offset = DateUtil.offset(parse, DateField.SECOND, offsetSecond);
        String format = DateUtil.format(offset, DatePattern.NORM_TIME_PATTERN);
        if (format.compareTo("09:30:00") < 0) {
            format = "09:30:00";
        } else if (format.compareTo("15:00:00") > 0) {
            format = "15:00:00";
        }
        return DateUtil.parse(DateUtil.format(offset, DatePattern.NORM_DATE_PATTERN) + " " + format);
    }


    /*
    对分时成交df, 的筛选, 然后求 最新价, 最高,最低, 总成交额 的简单方法
     */

    /**
     * 求筛选出的部分分时成交df, 的总计成交量
     *
     * @param bondBean
     * @param effectDf
     * @return
     */
    public static Double getAmountOfEffectDf(SecurityBeanEm bondBean, DataFrame<Object> effectDf) {
        int volRate = bondBean.isBond() ? 10 : 100;
        List<Double> tickAmountList = new ArrayList<>();
        for (int i = 0; i < effectDf.length(); i++) {
            Object price = effectDf.get(i, "price");
            Object vol = effectDf.get(i, "vol");
            tickAmountList.add(Double.parseDouble(price.toString()) * Double.parseDouble(vol.toString()) * volRate);
        }
        return CommonUtil.sumOfListNumberUseLoop(tickAmountList);
    }


    /**
     * 求筛选出的部分分时成交df, 的 阶段性最大价格
     *
     * @param effectDf
     * @return
     */
    public static Double getHighOfEffectDf(DataFrame<Object> effectDf) {
        return CommonUtil.maxOfListDouble(DataFrameS.getColAsDoubleList(effectDf, "price"));
    }

    public static Double getLowOfEffectDf(DataFrame<Object> effectDf) {
        return CommonUtil.minOfListDouble(DataFrameS.getColAsDoubleList(effectDf, "price"));
    }

    public static Double getCloseOfEffectDf(DataFrame<Object> effectDf) {
        return Double.valueOf(effectDf.get(effectDf.length() - 1, "price").toString());
    }

    /**
     * sleep衰减设置值队列, 求平均值, 去掉最大和最小
     * 仅一次循环求3值, 尽量最快速度! 不调用常规 求和/max/min  api
     *
     * @param deque
     * @return
     */
    public static long getTheAvgOfDequeExcludeMaxAndMin(ArrayDeque<Long> deque) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Long value : deque) {
            sum = sum + value;
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        // 默认四舍五入
        return NumberUtil.round((sum - min - max) * 1.0 / (deque.size() - 2), 0).longValue();
    }


    public static void removeEnterKeyDefaultAction(JXTable jxTable) {
        ActionMap am = jxTable.getActionMap();
        am.getParent().remove("selectNextRowCell"); // 取消默认的: 按下回车键将移动到下一行
        jxTable.setActionMap(am);
    }

    /**
     * 表格列宽自适应
     *
     * @param myTable
     */
    public static void fitTableColumns(JTable myTable) {
//        JTableHeader header = myTable.getTableHeader();
//        int rowCount = myTable.getRowCount();
//
//        Enumeration columns = myTable.getColumnModel().getColumns();
//
//        int dummyIndex = 0;
//        while (columns.hasMoreElements()) {
//            TableColumn column = (TableColumn) columns.nextElement();
//            int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
//            int width = (int) myTable.getTableHeader().getDefaultRenderer()
//                    .getTableCellRendererComponent(myTable, column.getIdentifier()
//                            , false, false, -1, col).getPreferredSize().getWidth();
//            for (int row = 0; row < rowCount; row++) {
//                int preferedWidth = (int) myTable.getCellRenderer(row, col).getTableCellRendererComponent(myTable,
//                        myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
//                width = Math.max(width, preferedWidth);
//            }
//            header.setResizingColumn(column); // 此行很重要
//
//            int actualWidth = width + myTable.getIntercellSpacing().width + 2;
//            actualWidth = Math.min(700, actualWidth); // 单列最大宽度
//            column.setWidth(actualWidth); // 多5
////            break; // 仅第一列日期. 其他的平均
//
//            if (dummyIndex > -1) {
//                column.setWidth(60); //  每行宽度
//            }
//            dummyIndex++;
//        }
    }

    public static void openSecurityQuoteUrl(SecurityBeanEm.SecurityEmPo po) {
        String url = null;
        SecurityBeanEm bean = po.getBean();
        if (bean.isBK()) {
            url = StrUtil.format("http://quote.eastmoney.com/bk/{}.html", bean.getQuoteId());
        } else if (bean.isIndex()) {
            url = StrUtil.format("http://quote.eastmoney.com/zs{}.html", bean.getSecCode());
        } else if (bean.isStock()) {
            if (bean.isHuA() || bean.isHuB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sh", po.getSecCode());
            } else if (bean.isShenA() || bean.isShenB()) {
                url = StrUtil.format("https://quote.eastmoney.com/{}{}.html", "sz", po.getSecCode());
            } else if (bean.isJingA()) {
                url = StrUtil.format("http://quote.eastmoney.com/bj/{}.html", po.getSecCode());
            } else if (bean.isXSB()) {
                url = StrUtil.format("http://xinsanban.eastmoney.com/QuoteCenter/{}.html", po.getSecCode());
            } else if (bean.isKCB()) {
                url = StrUtil.format("http://quote.eastmoney.com/kcb/{}.html", po.getSecCode());
            }
        } else if (bean.isBond()) {
            if (bean.getMarket() == 0) {
                url = StrUtil.format("http://quote.eastmoney.com/sz{}.html", po.getSecCode());
            } else if (bean.getMarket() == 1) {
                url = StrUtil.format("http://quote.eastmoney.com/sh{}.html", po.getSecCode());
            }
        }

        if (url == null) {
            log.warn("未知资产类别, 无法打开行情页面: {}", bean.getName(), bean.getSecurityTypeName());
            return;
        }
        CommonUtil.openUrlWithDefaultBrowser(url);
    }

    /**
     * 转换 mp3 格式才行
     */
    public static void playClinchSuccessSound() {
//        String fullPathOfClassPathFileOrDir = CommonUtil.getFullPathOfClassPathFileOrDir("revise/clinch_success.mp3");
        String fullPathOfClassPathFileOrDir = CommonUtil.getFullPathOfClassPathFileOrDir("revise/cj.mp3");
        Console.log(fullPathOfClassPathFileOrDir);
        Tts.playSound(FileUtil.file(fullPathOfClassPathFileOrDir), true, false);
    }

    /**
     * 声音是同花顺撤单声音, 因为复盘如果订单失败, 视为全自动撤单!
     */
    public static void playClinchFailSound() {
//        String fullPathOfClassPathFileOrDir = CommonUtil.getFullPathOfClassPathFileOrDir("revise/clinch_fail.mp3");
        String fullPathOfClassPathFileOrDir = CommonUtil.getFullPathOfClassPathFileOrDir("revise/cd.mp3");
        Tts.playSound(FileUtil.file(fullPathOfClassPathFileOrDir), true, false);
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 功能区上方, 显示 当前选中的bean 的基本信息; 例如概念, 行业,余额等;  --> 最新背诵信息
     * 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
     * 113537	文灿转债	278.03	1.4亿	20190705	40.6	603348	文灿股份	交运设备-汽车零部件-汽车零部件Ⅲ	蔚来汽车概念;新能源汽车;特斯拉	41.8	130.49亿
     */
    public static class SelectBeanDisplayPanel extends DisplayPanel {
        public static DataFrame<Object> allBondInfoDfForRevise = null; // 背诵字段df; 仅载入一次
        public static ConcurrentHashMap<String, List<Object>> allBondInfoForReviseMap = new ConcurrentHashMap<>(); //

        SecurityBeanEm bondBean;

        JLabel bondInfoLabel = getCommonLabel();
        JLabel stockInfoLabel = getCommonLabel();
        JLabel industryInfoLabel = getCommonLabel();
        JLabel conceptInfoLabel = getCommonLabel();

        public SelectBeanDisplayPanel() {
            this.setLayout(new GridLayout(4, 1, -1, -1)); // 4行1列;
            this.add(bondInfoLabel);
            this.add(stockInfoLabel);
            this.add(industryInfoLabel);
            this.add(conceptInfoLabel);

            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 100) {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            allBondInfoDfForRevise = BondUtil.generateCSVForRecite1(); // 首次
                            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                                ThreadUtil.sleep(30000); // 30秒后再次尝试将
                                continue; // 失败将重试
                            }
                            // 成功则载入然后跳出循环
                            // 载入到map里面, key为转债代码, value 为df单行!, 带有这种代码列, 因此注意索引!
                            for (int i = 0; i < allBondInfoDfForRevise.length(); i++) {
                                allBondInfoForReviseMap.put(allBondInfoDfForRevise.get(i, 0).toString(),
                                        allBondInfoDfForRevise.row(i));
                            }
                            break; //
                        }
                    }
                }, true);
            }
        }

        public static JLabel getCommonLabel() {
            JLabel jLabel = new JLabel();
            jLabel.setForeground(Color.red);
            jLabel.setBackground(Color.black);
            return jLabel;
        }

        /**
         * 给定df一行, 给定索引列表, 创建显示内容, 使用 / 间隔, 且null显示null
         *
         * @param objects
         * @param indexes
         * @return
         */
        public static String buildStrForLabelShow(List<Object> objects, List<Integer> indexes) {
            if (objects == null) {
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder("");
            for (Integer index : indexes) {
                stringBuilder.append(" / "); // 最后去除
                Object o = objects.get(index);
                if (o == null) {
                    stringBuilder.append("null");
                } else {
                    stringBuilder.append(o.toString());
                }
            }
            return StrUtil.sub(stringBuilder.toString(), 3, stringBuilder.length());
        }


        @Override
        public void update() {
            if (this.bondBean == null) {
                return;
            }
            if (allBondInfoDfForRevise == null || allBondInfoDfForRevise.length() < 200) {
                return; // 要有全数据
            }
            String bondCode = bondBean.getSecCode();
            // 转债代码	转债名称	价格	剩余规模	上市日期	20日振幅	正股代码	正股名称	行业	概念	pe动	流值
            List<Object> infos = allBondInfoForReviseMap.get(bondCode);
            String s = buildStrForLabelShow(infos, Arrays.asList(0, 1, 2, 3));
            bondInfoLabel.setText(s);
            bondInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(6, 7, 10, 11));
            stockInfoLabel.setText(s);
            stockInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(8, 4, 5));
            industryInfoLabel.setText(s);
            industryInfoLabel.setToolTipText(s);

            s = buildStrForLabelShow(infos, Arrays.asList(9));
            conceptInfoLabel.setText(s);
            conceptInfoLabel.setToolTipText(s);
            // conceptInfoLabel.setText(infos.get(9).toString());
        }

        public void update(SecurityBeanEm beanEm) {
            this.bondBean = beanEm;
            this.update();
        }
    }

    /**
     * 转债 涨跌幅列, 的表格 render --> 主要是子类 text 显示改变
     * 第1,2列字体和 中对齐; 其余右对齐
     */
    public static class TableCellRendererForBondTable extends DefaultTableCellRenderer {
        public static Font font1 = new Font("微软雅黑", Font.PLAIN, 14);
        //        public static Font font2 = new Font("楷体", Font.BOLD, 12);
        public static Font font2 = new Font("楷体", Font.PLAIN, 14);

        Color selectBackground;

        public TableCellRendererForBondTable(Color color) {
            this.selectBackground = color;
        }

        public TableCellRendererForBondTable() {
            this(new Color(64, 0, 128));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            try {
                if (isSelected) {
                    this.setBackground(selectBackground); // 同花顺
                } else {
                    this.setBackground(Color.black); // 同花顺
                }
                if (value != null) {
                    this.setText(value.toString());
                }
                if (column == 0 || column == 1) {
                    this.setHorizontalAlignment(SwingConstants.CENTER);
                    this.setFont(font1);
                } else {
                    this.setHorizontalAlignment(SwingConstants.RIGHT);
                    this.setFont(font2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this;
        }
    }

    /**
     * 涨跌幅列渲染器,  将格式化 文字显示 --> 推广到任意 百分比 列
     */
    public static class TableCellRendererForBondTableForPercent extends TableCellRendererForBondTable {
        public static DecimalFormat dfOfChgPct = new DecimalFormat("####0.00%");

        public TableCellRendererForBondTableForPercent(Color color) {
            super(color);
        }

        public TableCellRendererForBondTableForPercent() {
        }

        @Override
        public void setText(String text) { // 涨跌幅列, 再加功能, 需要改写文字形式!
            Double price = null;
            try {
                price = Double.valueOf(text);
            } catch (NumberFormatException e) {
                // 例如null, 可能转换失败, 很正常!
            }
            if (price != null) { // 转换为涨跌幅成功, 则格式化显示!
                // this.setForeground(Color.red); // 全红, 然后用高亮接口, - 开头的 绿色, 实现红绿区分!
                super.setText(dfOfChgPct.format(price));
            } else {
                super.setText(text);
            }
        }
    }


    /**
     * 成交额列渲染器,  将把数字转换为 万/亿后缀显示! --> 推广到任意 数值大列
     */
    public static class TableCellRendererForBondTableForBigNumber extends TableCellRendererForBondTable {
        public TableCellRendererForBondTableForBigNumber(Color color) {
            super(color);
        }

        public TableCellRendererForBondTableForBigNumber() {
        }

        @Override
        public void setText(String text) { // 涨跌幅列, 再加功能, 需要改写文字形式!
            Double amount = null;
            try {
                amount = Double.valueOf(text);
            } catch (NumberFormatException e) {
                // 例如null, 可能转换失败, 很正常!
            }
            if (amount != null) { // 转换为涨跌幅成功, 则格式化显示!
                super.setText(CommonUtil.formatNumberWithSuitable(amount, 1));
            } else {
                super.setText(text);
            }
        }
    }

    /**
     * 列渲染器, 数值自动保留 2 位小数
     */
    public static class TableCellRendererForBondTableFor2Scale extends TableCellRendererForBondTable {
        public TableCellRendererForBondTableFor2Scale(Color color) {
            super(color);
        }

        public TableCellRendererForBondTableFor2Scale() {
        }

        public static DecimalFormat dfOfMoney = new DecimalFormat("####0.00");

        @Override
        public void setText(String text) { // 涨跌幅列, 再加功能, 需要改写文字形式!
            Double money = null;
            try {
                money = Double.valueOf(text);
            } catch (NumberFormatException e) {
                // 例如null, 可能转换失败, 很正常!
            }
            if (money != null) { // 转换为涨跌幅成功, 则格式化显示!
                super.setText(dfOfMoney.format(money));
            } else {
                super.setText(text);
            }
        }
    }


    /**
     * 本行的 转债代码 和转债名称, 在给定集合之中, 则高亮它们; 用于当前持仓债!
     * 与同花顺相同, 只要是曾经持仓过的, 都加入, 即使已经全部卖出了!
     */
    public static class HoldBondHighLighterPredicate implements HighlightPredicate {
        private volatile CopyOnWriteArraySet<String> bondCodes = new CopyOnWriteArraySet<>();

        public HoldBondHighLighterPredicate(Collection<String> initCodes) {
            if (initCodes != null) {
                this.bondCodes.addAll(initCodes);
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
            Object value = adapter.getValue(0);
            if (value == null) {
                return false;
            }
            if (bondCodes.contains(value.toString())) {
                if (adapter.column == 0 || adapter.column == 1) { // 只对代码和名称改颜色
                    return true;
                }
            }
            return false;
        }

        public void addHoldBondCode(String newHoldBondCode) {
            this.bondCodes.add(newHoldBondCode);
        }

        public void addHoldBondCodes(Collection<String> newHoldBondCodes) {
            this.bondCodes.addAll(newHoldBondCodes);
        }
    }

    /**
     * 本行的 涨跌幅数值, >0.0 , 涨跌幅文字将红色
     */
    public static class ChgPctGt0HighLighterPredicate implements HighlightPredicate {
        int colIndex;

        public ChgPctGt0HighLighterPredicate(int colIndex) {
            this.colIndex = colIndex;
        }

        @Override
        public boolean isHighlighted(Component renderer, org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
            Object value = adapter.getValue(colIndex); // 涨跌幅
            if (value == null) {
                return false;
            }
            double v;
            try {
                v = Double.parseDouble(value.toString());
            } catch (Exception e) {
                return false;
            }
            if (v > 0 && adapter.column == colIndex) { // 只改变涨跌幅列
                return true;
            }
            return false;
        }
    }

    /**
     * 每行的 某列数值, >0.0 , 全行都将 某色(例如红色)
     */
    public static class SingleColGt0HighLighterPredicate implements HighlightPredicate {
        int referColIndex;

        public SingleColGt0HighLighterPredicate(int referColIndex) {
            this.referColIndex = referColIndex;
        }

        @Override
        public boolean isHighlighted(Component renderer, org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
            Object value = adapter.getValue(referColIndex); // 涨跌幅
            if (value == null) {
                return false;
            }
            double v;
            try {
                v = Double.parseDouble(value.toString());
            } catch (Exception e) {
                return false;
            }
            if (v > 0) { // 只改变涨跌幅列
                return true;
            }
            return false;
        }
    }

    /**
     * 每行的 某列数值, <0.0 , 全行都将 某色(例如绿色)
     */
    public static class SingleColLt0HighLighterPredicate implements HighlightPredicate {
        int referColIndex;

        public SingleColLt0HighLighterPredicate(int referColIndex) {
            this.referColIndex = referColIndex;
        }

        @Override
        public boolean isHighlighted(Component renderer, org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
            Object value = adapter.getValue(referColIndex); // 涨跌幅
            if (value == null) {
                return false;
            }
            double v;
            try {
                v = Double.parseDouble(value.toString());
            } catch (Exception e) {
                return false;
            }
            if (v < 0) { // 只改变涨跌幅列
                return true;
            }
            return false;
        }
    }

    /**
     * 本行的 涨跌幅数值, <0.0 , 涨跌幅文字将绿色
     */
    public static class ChgPctLt0HighLighterPredicate implements HighlightPredicate {
        int colIndex;

        public ChgPctLt0HighLighterPredicate(int colIndex) {
            this.colIndex = colIndex;
        }


        @Override
        public boolean isHighlighted(Component renderer, org.jdesktop.swingx.decorator.ComponentAdapter adapter) {
            Object value = adapter.getValue(colIndex); // 涨跌幅
            if (value == null) {
                return false;
            }
            double v;
            try {
                v = Double.parseDouble(value.toString());
            } catch (Exception e) {
                return false;
            }
            if (v < 0 && adapter.column == colIndex) { // 只改变涨跌幅列
                return true;
            }
            return false;
        }
    }
}
