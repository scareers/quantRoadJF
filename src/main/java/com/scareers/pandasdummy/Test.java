package com.scareers.pandasdummy;

/* 参考原代码
package com.scareers.pandasdummy;

import com.fasterxml.jackson.databind.JsonNode;
import com.japycpp.stock_genealogy.stock.util.CrawlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Component
public class SinaTicksTask {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerTestTasks.class);
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final long stockListFecthDelay = (long) (0.3 * 1000); // 毫秒; 控制单次url访问间隔
    private static final long stockTicksFecthDelay = (long) (0.0 * 1000); // 毫秒

    private static Connection conn;
    private static final String URL = "jdbc:mysql://localhost:3306/sina_ticks?useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "123";

    private static final String ticksTableNamePrefix = "sds_sina_ticks";
    private static final String stockListTableName = "sds_stock_list_sina";
    private static final String ticksUrlPrefix = "http://hq.sinajs.cn/list=";
    private static final String stockListUrl = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=%s&num=80&sort=symbol&asc=1&node=hs_a&symbol=&_s_r_a=page";

    private static ArrayList<String> stockList; // 每次抓取了新的股票列表, 就更新此静态属性
    private static String ticksTableCreateSql = "create table if not exists `" +
            ticksTableNamePrefix +
            "_%s`\n" +
            "(\n" +
            "    symbol         varchar(20) null,\n" +
            "    name_cn         varchar(20) null,\n" +
            "    open            double      null,\n" +
            "    yesterday_close double      null,\n" +
            "    current_price   double      null,\n" +
            "    high            double      null,\n" +
            "    low             double      null,\n" +
            "    buy_auction     double      null,\n" +
            "    sell_auction    double      null,\n" +
            "    volume          double      null,\n" +
            "    turnover        double      null,\n" +
            "    buy1_amount     double      null,\n" +
            "    buy1            double      null,\n" +
            "    buy2_amount     double      null,\n" +
            "    buy2            double      null,\n" +
            "    buy3_amount     double      null,\n" +
            "    buy3            double      null,\n" +
            "    buy4_amount     double      null,\n" +
            "    buy4            double      null,\n" +
            "    buy5_amount     double      null,\n" +
            "    buy5            double      null,\n" +
            "    sell1_amount    double      null,\n" +
            "    sell1           double      null,\n" +
            "    sell2_amount    double      null,\n" +
            "    sell2           double      null,\n" +
            "    sell3_amount    double      null,\n" +
            "    sell3           double      null,\n" +
            "    sell4_amount    double      null,\n" +
            "    sell4           double      null,\n" +
            "    sell5_amount    double      null,\n" +
            "    sell5           double      null,\n" +
            "    date            date        null,\n" +
            "    time            time        null,\n" +
            "    index(symbol)\n" +
            ")\n" +
            "    comment '新浪tick数据,每日一数据表';\n";

    public static String stockListTableCreateSql = "create table if not exists " +
            stockListTableName +
            "\n" +
            "(\n" +
            "    symbol varchar(20) null,\n" +
            "    code   varchar(20) null,\n" +
            "    date   date null\n" +
            ");";

    public static String stockListTempTableCreateSql = "create table if not exists " +
            stockListTableName + "_temp" +
            "\n" +
            "(\n" +
            "    symbol varchar(20) null,\n" +
            "    code   varchar(20) null,\n" +
            "    date   date null\n" +
            ");";

    private static String stockListDateSql = "select max(date) as date from " +
            stockListTableName;

    private void createStockListTable() throws Exception {
        logger.debug("try to create table: " + stockListTableName);
        execSql(stockListTableCreateSql);
    }

    private void createStockListTempTable() throws Exception {
        logger.debug("try to create temp table: " + stockListTableName + "_temp");
        String dropTempTableSql = "drop table if exists " + stockListTableName + "_temp";
        try {
            execSql(dropTempTableSql);
            execSql(stockListTempTableCreateSql);
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    private void createTodayTicksTable() throws Exception {
        String today = dateOnlyFormat.format(new java.util.Date());
        String fullCreateSql = String.format(ticksTableCreateSql, today);
        logger.info("try to create the table of today ticks and add symbol column as index");
        execSql(fullCreateSql);
    }

    private Date getRecordDateFromStockListTable() throws Exception {
        logger.info("get the last record date from stock list");
        ResultSet res = execSqlQuery(stockListDateSql);
        if (res.next()) {
            return res.getDate("date");
        }
        return null;
    }

    private void updateStockListTable() throws Exception {
        Date dateLastRecord = Date.valueOf(LocalDate.of(1990, 1, 1));
        try {
            Date temp = getRecordDateFromStockListTable();
            if (temp != null) {
                dateLastRecord = temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date today = Date.valueOf(LocalDate.now());
        if (today.toString().compareTo(dateLastRecord.toString()) > 0) {
            updateStockListTableActually(); // 更新表
        } else {
            logger.info("do not need to update stock list table");
        }
    }

    private boolean execSql(String sql) throws Exception {
        connectToDb();
        Statement s = conn.createStatement();
        boolean rs = s.execute(sql);
//        s.close();
        return rs;
    }

    private void connectToDb() throws ClassNotFoundException, SQLException {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(URL, USER, PASSWORD);
        logger.debug("connect db: sina_ticks; url settings: " + URL);
    }

    private ResultSet execSqlQuery(String sql) throws Exception {
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery(sql);
//        s.close();
        return rs;
    }

    private int execSqlUpdate(String sql) throws Exception {
        Statement s = conn.createStatement();
        int rs = s.executeUpdate(sql);
//        s.close();
        return rs;
    }

    private String buildFullTicksInsertSql() {
        ArrayList<String> commas = new ArrayList<>();
        for (int i = 0; i < 33; i++) { // 32列 + 第1列symbol == 33
            commas.add("?");
        }
        String today = dateOnlyFormat.format(new java.util.Date());
        // @key: 当数据表含有数字时, 需要加入 `` 符号
        String sql = "insert into `" +
                ticksTableNamePrefix +
                "_%s` values (%s)";
        return String.format(sql, today, String.join(",", commas));

//        "insert into `sds_sina_tick_20211103` values (?,?,?,?)"
    }

    public ArrayList<String> getStockList() throws Exception {
        if (stockList == null) {
            updateStockListDaily();
        }
        return stockList;
    }

    @Scheduled(
            initialDelay = 600 * 1000,
            fixedDelay = 20 * 60 * 1000)
    // 10分钟后调用; 20 分钟更新一次静态属性: 股票列表
    // 此函数在 此一次 ticks时,由于静态属性为null, 将被强制调用1次
    private void updateStockListDaily() throws Exception {
        connectToDb();
        createStockListTable();
        createTodayTicksTable();
        updateStockListTable();

        String sqlGetStockList = "select distinct symbol from " +
                stockListTableName +
                " order by symbol";
        ResultSet symbols = execSqlQuery(sqlGetStockList);
        ArrayList<String> stockListTemp = new ArrayList<>();
        while (symbols.next()) {
            stockListTemp.add(symbols.getString("symbol"));
        }
        stockList = stockListTemp;
        logger.info("update the stock list (static attr)");
    }

    @Scheduled(cron = "0/5 * * * * *") // 此为测试用打开, 任何时候均 5s一次,默认关闭
    // 2-6表示星期1到5, 星期日是 1
    @Scheduled(cron = "0/5 14-59 9 * * 2-6") // 9点14-59 , 5s抓一次
    @Scheduled(cron = "0/5 0-59 10 * * 2-6")
    @Scheduled(cron = "0/5 0-30 11 * * 2-6")
    @Scheduled(cron = "0/5 59 12 * * 2-6")
    @Scheduled(cron = "0/5 0-59 13-14 * * 2-6")
    @Scheduled(cron = "0/5 0-1 15 * * 2-6")
    private void fetchSinaTicks() throws Exception {
        long start = System.currentTimeMillis();

        // 连接数据库,尝试创建2表,尝试更新股票列表数据表, 每次都会调用
        // stockList 的获取有 缓存机制. 最开始强制拉取, 然后每隔20分钟刷新
        connectToDb();
        createStockListTable();
        createTodayTicksTable();
        updateStockListTable();
        ArrayList<String> stockList = getStockList();

        int perStocks = 500;
        int epochs = stockList.size() / perStocks;
        if ((stockList.size() % perStocks) != 0) {
            epochs += 1;
        }

        String fullTicksInsertSql = buildFullTicksInsertSql();
        conn.setAutoCommit(false);
        PreparedStatement prest = conn.prepareStatement(fullTicksInsertSql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

        for (int epoch = 0; epoch < epochs; epoch++) {
            int end = Math.min(stockList.size(), perStocks * (epoch + 1));
            List<String> stockListEpoch = stockList.subList(perStocks * epoch, end);
            String commaSeprateStocks = String.join(",", stockListEpoch);
            Thread.sleep(stockTicksFecthDelay);
            String res = CrawlerUtils.getFromUrlAsString(ticksUrlPrefix + commaSeprateStocks);
            String[] eachStock = res.split(";");

            for (int i = 0; i < eachStock.length - 1; i++) { // 最后有一个空字符串.
                String stock = stockListEpoch.get(i);
                prest.setString(1, stock);
                String s = eachStock[i];
                s = s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""));
                String[] columns = s.split(",");
                for (int k = 0; k < 32; k++) {
                    if (k < 2) { // 最开始两个是字符串
                        prest.setString(k + 2, columns[k]);
                    } else if (k < 30) { // 接下来28个都是 double
                        prest.setDouble(k + 2, Double.valueOf(columns[k]));
                    } else if (k == 30) { // 倒数第二是 日期
                        prest.setDate(k + 2, Date.valueOf(columns[k]));
                    } else { // 倒数第一是 时间
                        prest.setTime(k + 2, Time.valueOf(columns[k]));
                    }
                }
                prest.addBatch();
            }
            logger.info("ticks current epoch: " + epoch);
        }

        prest.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);
        logger.info("commit all stock ticks finished");
        logger.info("elapsed time: " + ((double) (System.currentTimeMillis() - start)) / 1000 + "s");
    }


    private void updateStockListTableActually() throws Exception {
        logger.info("need to update stock list table,begin:");
        createStockListTempTable();
        String sql = "insert into " +
                stockListTableName + "_temp" +
                " values(?,?,?)";
        Date today = Date.valueOf(LocalDate.now());
        conn.setAutoCommit(false);
        PreparedStatement prest = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        int pageNum = -1;
        while (true) {
            pageNum++;
            logger.info("current page: " + pageNum);
            String url_temp = String.format(stockListUrl, pageNum);
            Thread.sleep(stockListFecthDelay);
            String jsonStr = CrawlerUtils.getFromUrlAsString(url_temp);
            if (jsonStr.length() < 3) {
                break; // 页数超额
            }
            JsonNode root = CrawlerUtils.parseJsonString(jsonStr);
            int amount = root.size();
            for (int i = 0; i < amount; i++) {
                JsonNode per = root.get(i);
                String symbol = per.get("symbol").asText();
                String code = per.get("code").asText();
                prest.setString(1, per.get("symbol").asText());
                prest.setString(2, per.get("code").asText());
                prest.setDate(3, today);
                prest.addBatch();
            }
            // 单次循环提交的方式
            prest.executeBatch();
            conn.commit();
        }

        try { // 删除原表并 替换temp为新表
            String dropOriginalTable = "drop table " + stockListTableName;
            logger.info("drop the original stock list table");
            execSql(dropOriginalTable);

            String replaceTempTable = "rename table " + stockListTableName + "_temp" + " to " + stockListTableName;
            execSql(replaceTempTable);
        } catch (Exception e) {
            e.printStackTrace();
            conn.rollback();
            logger.error(e.toString());
        }

        conn.setAutoCommit(true);
        logger.info("stock list table, update finished");
    }
}
 */

/**
 * 类描述:
 * author: admin
 * date: 2021/11/3  0003-19:27
 */
public class Test {
}


