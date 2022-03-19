package com.scareers.datasource.eastmoney.dailycrawler.datas;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import joinery.DataFrame;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.scareers.datasource.eastmoney.datacenter.EmDataApi.maxPageOfZiXunJingHua;
import static com.scareers.tools.stockplan.bean.SimpleNewEm.buildDfFromBeanListWithoutIdAndSaveTime;

/**
 * description: 简单新闻 抓取, 例如 资讯精华0, 财经导读1,   由数据库 type字段区分. 由Em api 设定;
 * 首先按照类型和 时间(越大) 取最后 500条记录; HashSet
 * 按照 1-25 顺序调用API, 获取每页的新闻;
 * 判定新闻是否已经 在HashSet中;  // 逻辑仅仅是 时间和标题字段相等
 * 将没有保存过的新闻, 保存;
 *
 * @noti 一旦碰到已经保存过的新闻, 因为时间先后顺序, 立即退出爬虫, 因为可以推断 后面页面的已经被保存过了
 * @author: admin
 * @date: 2022/3/12/012-19:04:43
 */
public abstract class SimpleNewCrawlerEm extends CrawlerEm {
    public static String tableName = "simple_new";
    // 按页访问过程中, 如果重复的新闻数量超过 20, 则不再访问后面的页, 结束本次爬取; 实测因为东财机制, 该值不能太少,因为新闻顺序不固定
    public static int repeatCountThreshold = 500;

    public SimpleNewCrawlerEm() {
        super(tableName);
    }

    // 每次爬取时持有, 完成后清除. clear()方法
    protected List<SimpleNewEm> saveBeans = new ArrayList<>(); // 持有所有可能保存过的bean;


    @Override
    protected void runCore() {
        HashSet<SimpleNewEm> last500News;
        try {
            last500News = initLastTimeFetchSaveBeansExpect500();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("{}: 初始化上次抓取新闻列表失败! ", this.toString());
            success = false;
            return;
        }

        int repeatCount = 0;
        for (int i = 1; i <= maxPageOfZiXunJingHua; i++) {
            List<SimpleNewEm> beansPerPage = getPage(i);
            if (beansPerPage.size() == 0) {
                success = false; // api自带log, 足够
                return;
            }

            List<SimpleNewEm> shouldSave = new ArrayList<>();
            for (SimpleNewEm simpleNewEm : beansPerPage) {
                if (!last500News.contains(simpleNewEm)) {
                    shouldSave.add(simpleNewEm);
                } else {
                    repeatCount++;
                }
            }
            if (repeatCount >= repeatCountThreshold) { // 超出阈值, 判定为后面页数都曾经访问过了
                success = true;
                return;
            }

            try {
                if (shouldSave.size() > 0) {
                    this.saveBeans.addAll(shouldSave);
                    saveToDbBatch(shouldSave); // 必然保存
                }
            } catch (SQLException e) {
                e.printStackTrace();
                logSaveError();
                success = false;
            }

        }
        success = true;
    }

    @Override
    protected void clear() {
        saveBeans.clear();
    }

    /**
     * 需要获取某类型新闻, 此前最后一次爬虫运行时, 已经保存的那些新闻记录; 最多成功保存 20*25==500条;
     * 由 saveTime倒序limit 500获取, 当然, 需要注意type
     *
     * @return
     */
    protected abstract HashSet<SimpleNewEm> initLastTimeFetchSaveBeansExpect500() throws Exception;

    protected abstract List<SimpleNewEm> getPage(int page);


    @Override
    protected void setDb() {
        this.setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        sqlCreateTable = getsqlCreateTable();
    }

    public static String getsqlCreateTable() {
        return StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "dateTime datetime null,"
                        + "title text  null,"
                        + "url text  null,"
                        + "detailTitle text  null,"
                        + "saveTime datetime  null,"
                        + "type int  null,"
                        + "urlRawHtml longtext  null,"

                        + "briefly varchar(2048)  null,"
                        + "relatedObject varchar(2048)  null,"
                        + "trend double  null," // 振幅
                        + "remark longtext  null,"
                        + "lastModified datetime  null,"
                        + "marked boolean  null,"

                        + "INDEX dateTime_index (dateTime ASC)\n"

                        + "\n)"
                , tableName);
    }

    /**
     * 批量添加到数据库
     *
     * @param news
     */
    public static void saveToDbBatch(List<SimpleNewEm> news) throws SQLException {
        DataFrame<Object> dataFrame = buildDfFromBeanListWithoutIdAndSaveTime(news);

        // saveTime 初始化
        for (int i = 0; i < dataFrame.length(); i++) {
            dataFrame.set(i, "saveTime", Timestamp.valueOf(DateUtil.date().toLocalDateTime()));
        }
        DataFrameS.toSql(dataFrame, tableName, ConnectionFactory.getConnLocalEastmoney(), "append",
                getsqlCreateTable());
    }
}
