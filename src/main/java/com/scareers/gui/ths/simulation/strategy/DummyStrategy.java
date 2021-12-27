package com.scareers.gui.ths.simulation.strategy;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.gui.rabbitmq.OrderFactory;
import com.scareers.gui.rabbitmq.order.Order;
import com.scareers.gui.ths.simulation.Trader;
import com.scareers.pandasdummy.DataFrameSelf;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.sql.Connection;
import java.util.List;

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

    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws Exception {
        new DummyStrategy("xx").stockSelect();
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
                stockSelectResultSaveTableName, DateUtil.today().replace("-", ""));
        DataFrame<Object> dfTemp = DataFrame.readSql(connOfStockSelectResult, sqlIsStockSelectedToday);
        long resultCountOfToday = Long.valueOf(dfTemp.get(0, 0).toString());
        if (resultCountOfToday <= hasStockSelectResultTodayThreshold) {
            stockSelect0(); // 真实今日选股并存入数据库, 需要从各大分析研究程序调用对应函数
        }

        return null;
    }

    private void stockSelect0() {

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
