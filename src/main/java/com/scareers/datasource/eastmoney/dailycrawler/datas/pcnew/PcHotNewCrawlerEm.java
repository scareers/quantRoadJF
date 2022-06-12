package com.scareers.datasource.eastmoney.dailycrawler.datas.pcnew;

import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.datasource.eastmoney.quotecenter.EmNewsApi;
import com.scareers.datasource.eastmoney.quotecenter.EmNewsApi.EmPcNewestHotNew;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import joinery.DataFrame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * description: pc 东财弹窗热点新闻! 最重要!
 *
 * @author: admin
 * @date: 2022/3/12/012-19:04:43
 */
public class PcHotNewCrawlerEm extends CrawlerEm {
    public static String tableName = "pc_new_hots";

    public static void main(String[] args) {
        PcHotNewCrawlerEm crawlerEm = new PcHotNewCrawlerEm();
        crawlerEm.run();
    }

    public PcHotNewCrawlerEm() {
        super(tableName);
    }


    public static List<String> columns = Arrays.asList(
            "code", "title", "digest", "simtitle", "titlecolor",
            "showtime", "ordertime", "pushtime", "url", "image",
            "author", "source", "columns", "channels", "interact",
            "sort", "type"
    );

    @Override
    protected void runCore() {
        List<EmPcNewestHotNew> newestHotEmPc = EmNewsApi.getNewestHotEmPc(500, 1);
        if (newestHotEmPc == null || newestHotEmPc.size() == 0) {
            logApiError("EmNewsApi.getNewestHotEmPc");
            success = false;
            return;
        }
        // @update: 数据库已存在的
        List<String> alreadyHas = EastMoneyDbApi.getNewestHotEmPcNewTitleSet(500);
        if (alreadyHas == null) {
            logApiError("EastMoneyDbApi.getNewestHotEmPcNewTitleSet");
            success = false;
            return;
        }

        HashSet<String> excludeTitles = new HashSet<>(alreadyHas);

        DataFrame<Object> tempDf = new DataFrame<>(columns);
        for (EmPcNewestHotNew newestHotNew : newestHotEmPc) {
            if (excludeTitles.contains(newestHotNew.getTitle())) {
                continue; // 包含则排除
            }

            List<Object> row = new ArrayList<>();

            row.add(newestHotNew.getCode());
            row.add(newestHotNew.getTitle());
            row.add(newestHotNew.getDigest());
            row.add(newestHotNew.getSimtitle());
            row.add(newestHotNew.getTitlecolor());
            row.add(newestHotNew.getShowtime());
            row.add(newestHotNew.getOrdertime());
            row.add(newestHotNew.getPushtime());
            row.add(newestHotNew.getUrl());
            row.add(newestHotNew.getImage());
            row.add(newestHotNew.getAuthor());
            row.add(newestHotNew.getSource());
            row.add(newestHotNew.getColumns());
            row.add(newestHotNew.getChannels());
            row.add(newestHotNew.getInteract());
            row.add(newestHotNew.getSort());
            row.add(newestHotNew.getType());
            tempDf.append(row);
        }

        try {
            DataFrameS.toSql(tempDf, tableName, conn, "append", sqlCreateTable);
        } catch (SQLException e) {
            e.printStackTrace();
            logSaveError();
            success = false;
            return;
        }
        success = true;
    }

    @Override
    protected void clear() {

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
                        + "code varchar(64) null,"
                        + "title text  null,"
                        + "digest text  null,"
                        + "simtitle text  null,"
                        + "titlecolor text  null,"
                        + "showtime varchar(64)  null,"
                        + "ordertime varchar(64)  null,"
                        + "pushtime varchar(64)  null,"
                        + "url text  null,"
                        + "image text  null,"
                        + "author text  null,"
                        + "source text  null,"
                        + "columns text  null,"
                        + "channels text  null,"
                        + "interact text  null,"
                        + "sort long  null,"
                        + "type long  null,"


                        + "INDEX showtime_index (showtime ASC),\n"
                        + "INDEX ordertime_index (ordertime ASC),\n"
                        + "INDEX pushtime_index (pushtime ASC)\n"

                        + "\n)"
                , tableName);
    }


}
