package com.scareers.datasource.eastmoney.dailycrawler.datas;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.dailycrawler.Crawler;
import com.scareers.datasource.selfdb.ConnectionFactory;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.tools.stockplan.bean.SimpleNewEm;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.List;

import static com.scareers.tools.stockplan.bean.SimpleNewEm.buildDfOfBeanList;

/**
 * description: 资讯精华 数据抓取
 *
 * @author: admin
 * @date: 2022/3/12/012-19:04:43
 */
public class ZiXunJingHuaCrawler extends Crawler {
    public static String tableName = "simple_new";

    public ZiXunJingHuaCrawler() {
        super(tableName);
    }

    @Override
    protected void runCore() {

    }

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
                        + "type int  null,"
                        + "urlRawHtml longtext  null,"
                        + "briefly varchar(2048)  null,"
                        + "relatedObject varchar(2048)  null,"
                        + "trend double  null," // 振幅
                        + "remark longtext  null,"
                        + "lastModified datetime  null,"

                        + "INDEX dateTime_index (dateTime ASC)\n"

                        + "\n)"
                , tableName);
    }

    /**
     * 批量添加到数据库
     *
     * @param news
     */
    public static void saveToDbBatch(List<SimpleNewEm> news) {
        DataFrame<Object> dataFrame = buildDfOfBeanList(news);
        try {
            DataFrameS.toSql(dataFrame, tableName, ConnectionFactory.getConnLocalEastmoney(), "append",
                    getsqlCreateTable());
        } catch (SQLException e) {
            log.error("东财新闻bean列表: 保存到数据库失败");
            e.printStackTrace();
        }
    }
}
