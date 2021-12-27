package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.datasource.eastmoney.stock.StockApi;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.scareers.datasource.eastmoney.stock.StockApi.getRealtimeQuotes;
import static com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.SettingsOfLowBuyFS.keyInts;
import static com.scareers.utils.CommonUtils.subtractionOfList;
import static com.scareers.utils.SqlUtil.execSql;


/**
 * description: 虚拟的策略, 随机生成订单, 放入队列执行. check 逻辑也相同
 *
 * @author: admin
 * @date: 2021/12/26/026-03:21:08
 */
public class DummyStrategy extends Strategy {
    public static String stockSelectResultSaveTableName = "stock_select_result_of_lbhs_test";
    public static Connection connOfStockSelectResult = ConnectionFactory.getConnLocalKlineForms();
    public static long hasStockSelectResultTodayThreshold = 1000; // 当今日选股结果记录数量>此值,视为已执行选股.今日不再执行
    public static String SIMPLE_DATE_FORMAT = "yyyyMMdd";
    public static final List<Integer> keyInts = Arrays.asList(0, 1); // 核心设定
    public static final List<String> fieldsOfDfRaw = Arrays
            // @update: 新增了 amount列, 对主程序没有影响, 但是在 lbhs时, 可以读取到 amount 列, 成交额比成交量方便计算百分比
            .asList("trade_date", "open", "close", "high", "low", "vol", "amount"); // 股票日k线数据列
    // 日期	   开盘	   收盘	   最高	   最低	    成交量	          成交额	  振幅	  涨跌幅	  涨跌额	 换手率	  股票代码	股票名称
    public static Map<String, Object> fieldsRenameDict = Dict.create().set("日期", "trade_date").set("开盘", "open")
            .set("收盘", "close")
            .set("最高", "high").set("最低", "low").set("成交量", "vol").set("成交额", "amount");


    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws Exception {
//        new DummyStrategy("xx").stockSelect0();
        stockSelect0();
    }

    @Override
    protected void checkBuyOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected void checkSellOrder(Order order, List<JSONObject> responses, String orderType) {
        checkOtherOrder(order, responses, orderType);
    }

    @Override
    protected List<String> stockSelect() throws Exception {
        execSql(StrUtil.format(sqlCreateStockSelectResultSaveTableTemplate, stockSelectResultSaveTableName),
                connOfStockSelectResult); // 不存在则建表
        String sqlIsStockSelectedToday = StrUtil.format("select count(*) from `{}` where trade_date='{}'",
                stockSelectResultSaveTableName, DateUtil.format(DateUtil.date(), SIMPLE_DATE_FORMAT));
        DataFrame<Object> dfTemp = DataFrame.readSql(connOfStockSelectResult, sqlIsStockSelectedToday);
        long resultCountOfToday = Long.valueOf(dfTemp.get(0, 0).toString());
        if (resultCountOfToday <= hasStockSelectResultTodayThreshold) {
            stockSelect0(); // 真实今日选股并存入数据库, 需要从各大分析研究程序调用对应函数
        }

        return null;
    }

    /**
     * 单日选股逻辑, 参考:
     *
     * @see com.scareers.formals.kline.basemorphology.usesingleklinebasepercent.fs.lowbuy.FSAnalyzeLowDistributionOfLowBuyNextHighSell
     * :320 行
     */
    private static void stockSelect0() throws ExecutionException, InterruptedException {
        /*
                            List<String> concreteTodayFormStrs = parseConditionsAsStrs(stock, dfWindow, pre5dayKlineRow,
                            yesterdayKlineRow, todayKlineRow, stockWithStDateRanges, stockWithBoard);

                                     if (parallelOnlyStockSelectResult) {
                        // 该设置控制 只执行选股. lowBuy, HighSell均无视, 因此逻辑体后continue.
                        saveStockSelectResult(stock, todayTemp, belongToFormsetIds);
                        // 只需要保存股票,日期,所属形态集合  因此数据库 有 stock*date 条记录. 具体值为 形态集合--id 列表
                        continue; // 将造成 原有结果为空map, 因此执行保存也无所谓.
                    }
         */

        List<String> allHSAstock = DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("沪深A股")), "股票代码");
        List<String> pioneerMarket = DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("创业板")), "股票代码");
        List<String> scientificCreationMarket =
                DataFrameSelf.getColAsStringList(getRealtimeQuotes(Arrays.asList("科创板")),
                        "股票代码");
        HashSet<String> mainboardStocks = subtractionOfList(new ArrayList<>(subtractionOfList(allHSAstock,
                pioneerMarket)),
                scientificCreationMarket); // 两次差集操作, 获取所有主板股票
        // 开始选股. 首先获取近 几日 window数据, 当然, 这里我们多获取1日, 截取最后 n个.  截取2个月
        String today = DateUtil.today(); // yyyy-MM-dd
        String pre7TradeDate = StockApi.getPreNTradeDateStrict(today, 7); // 6足够, 冗余1.  // yyyy-MM-dd
        // 所有主板股票 3000+, 近2个月 日k线, 前复权.  key为stock, value为df
        ConcurrentHashMap<String, DataFrame<Object>> datasMap =
                StockApi.getQuoteHistory(new ArrayList<>(mainboardStocks),
                        pre7TradeDate, today, "101", "1", 2, false);

        Console.log(datasMap);
        int windowUsePeriodsCoreArg = keyInts.get(1) + 7; // 等价于原来高卖那一天. 这里8, 理论上, 应当获取最后6日数据, 拼接几行空值
        String preTradeDate = StockApi.getPreNTradeDateStrict(today, 1);
        for (String stock : datasMap.keySet()) {
            DataFrame<Object> dfRaw = datasMap.get(stock); // dfRaw的日期默认为 yyyy-MM-dd, 虽然起止参数是 yyyyMMdd 形式
            dfRaw = dfRaw.rename(new HashMap<>(fieldsRenameDict));
            if (dfRaw.size() < 6) { // 两个月至少需要 6天数据
                continue;
            }
            if (!(dfRaw.get(dfRaw.length() - 1, 0).toString().equals(preTradeDate))) {
                continue; // 该股票上一交易日, 并非上一个开盘日, 例如停牌的无视
            }
            DataFrame<Object> dfWindow = dfRaw.slice(dfRaw.length() - 6, dfRaw.length());
            Console.log(dfWindow);
            Console.log(dfWindow.length()); // 6
            for (int i = 0; i < windowUsePeriodsCoreArg - dfWindow.length(); i++) {
                dfWindow.append(Arrays.asList("")); // 添加几行空值, 模拟未来数据, 以便调用原方法
            }

        }
    }


    @Override
    protected List<StockBean> initStockPool() {
        log.warn("start init stockPool: 开始初始化股票池...");
        List<StockBean> res = StockPoolForFSTransaction.stockPoolTest();
        log.warn("finish init stockPool: 完成初始化股票池...");
        return res;
    }

    @Override
    protected void checkOtherOrder(Order order, List<JSONObject> responses, String orderType) {
        JSONObject response = responses.get(responses.size() - 1);
        if ("success".equals(response.getStr("state"))) {
            log.info("执行成功: {}", order.getRawOrderId());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行成功");
        } else {
            log.error("执行失败: {}", order.getRawOrderId());
            log.info(JSONUtil.parseArray(responses).toStringPretty());
            order.addLifePoint(Order.LifePointStatus.CHECK_TRANSACTION_STATUS, "执行失败");
        }
        Trader.successFinishOrder(order, responses);
    }

    @Override
    protected void startCore() throws Exception {
        while (true) {
            int sleep = RandomUtil.randomInt(1, 10); // 睡眠n秒
            Thread.sleep(sleep * 1000);
            Order order = null;
            int type = RandomUtil.randomInt(12);
            if (type < 3) {
                order = OrderFactory.generateBuyOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGHEST);
            } else if (type < 6) {
                order = OrderFactory.generateSellOrderQuick("600090", 100, 1.21, Order.PRIORITY_HIGH);
            } else if (type < 8) {
                order = OrderFactory.generateCancelAllOrder("600090", Order.PRIORITY_MEDIUM);
            } else if (type < 10) {
                order = OrderFactory.generateCancelSellOrder("600090", Order.PRIORITY_LOWEST);
            } else {
                order = OrderFactory.generateCancelBuyOrder("600090", Order.PRIORITY_HIGH);
            }
            Trader.putOrderToWaitExecute(order);
        }
    }

    public DummyStrategy(String strategyName) {
        super(strategyName);
    }

    public static String sqlCreateStockSelectResultSaveTableTemplate = "create table if not exists " +
            "`{}`\n" +
            "(\n" +
            "    id           int auto_increment comment 'id'\n" +
            "        primary key,\n" +
            "    trade_date   varchar(1024) null comment 'today: 选股日期',\n" +
            "    ts_code      varchar(1024) null comment '某只股票',\n" +
            "    form_set_ids longtext      null comment '该股票,该日, 所属的形态集合, 即被那些形态集合选中. json字符串Long列表',\n" +
            "    self_notes   varchar(2048) null comment '其他备注',\n" +
            "\n" +
            "    INDEX trade_date_index (trade_date ASC),\n" +
            "    INDEX ts_code_index (ts_code ASC)\n" +
            ")\n" +
            "    comment '选股结果: 日期-股票-所属形态集合id列表';\n";
}
