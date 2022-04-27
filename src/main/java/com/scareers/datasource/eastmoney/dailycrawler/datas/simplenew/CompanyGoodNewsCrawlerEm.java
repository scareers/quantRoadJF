package com.scareers.datasource.eastmoney.dailycrawler.datas.simplenew;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import com.scareers.datasource.eastmoney.dailycrawler.CrawlerEm;
import com.scareers.pandasdummy.DataFrameS;
import com.scareers.sqlapi.EastMoneyDbApi;
import com.scareers.tools.stockplan.news.bean.CompanyGoodNew;
import com.scareers.tools.stockplan.news.bean.SimpleNewEm;
import joinery.DataFrame;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;

/**
 * description: 交易日, 公司重大事项归纳; 来自于财经导读;
 *
 * @noti :机制: 读取近500条 最近财经导读数据, 从数据获取, 查找里面的该类新闻, 访问url解析
 * @author: admin
 * @date: 2022/3/16/016-21:43:13
 */
public class CompanyGoodNewsCrawlerEm extends CrawlerEm {
    public static void main(String[] args) {
        new CompanyGoodNewsCrawlerEm().run();
    }


    public CompanyGoodNewsCrawlerEm(String tableName) {
        super(tableName);
    }

    public CompanyGoodNewsCrawlerEm() {
        this("company_good_new");
    }

    @Override
    protected void runCore() {
        try {
            for (SimpleNewEm saveBean : this.initLastTimeFetchSaveBeansExpect500()) {
                if (!saveBean.isCompanyGoodNew()) { // 是重大事项
                    continue;
                }
                // 尝试访问数据库, 该日是否被解析过? 得到该日的解析结果数量.
                // 依据保存逻辑, 日期字段需要对应
                String dateStr = DateUtil.format(saveBean.getDateTime(), DatePattern.NORM_DATE_PATTERN);
                String sql = StrUtil.format("select count(*) from {} where dateStr='{}'",
                        tableName,
                        dateStr);
                DataFrame<Object> dataFrame = DataFrame.readSql(conn, sql);
                int count = Integer.parseInt(dataFrame.get(0, 0).toString());
                if (count == 0) {
                    // 首次解析保存
                    // 调用解析 api
                    actualSaveCompanyGoodNews(saveBean);
                    // 其他情况均不保存. 以免覆盖自定义字段!!!
                }
            }
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }
    }

    @Override
    protected void setDb() {
        this.setSaveToMainDb();
    }

    @Override
    protected void initSqlCreateTable() {
        sqlCreateTable = getSqlCreateCompanyGoodNewsTable();
    }

    private void actualSaveCompanyGoodNews(SimpleNewEm saveBean) throws SQLException {
        List<CompanyGoodNew> companyGoodNews = CompanyGoodNew.parseCompanyGoodNews(saveBean);
        if (companyGoodNews == null) {
            return;
        }

        // 保存逻辑
        DataFrame<Object> dataFrame1 = CompanyGoodNew.buildDfFromBeanListWithoutIdAndSaveTime(companyGoodNews);
        dataFrame1.add("saveTime");
        // saveTime 初始化
        for (int i = 0; i < dataFrame1.length(); i++) {
            dataFrame1.set(i, "saveTime", Timestamp.valueOf(DateUtil.date().toLocalDateTime()));
        }
        DataFrameS.toSql(dataFrame1, tableName, conn, "append",
                sqlCreateTable);
    }


    public String getSqlCreateCompanyGoodNewsTable() {
        return StrUtil.format(
                "create table if not exists `{}`(\n"
                        + "id bigint primary key auto_increment,"
                        + "name text  null,"
                        + "quoteUrl text  null,"
                        + "title text  null,"
                        + "content longtext  null,"
                        + "dateStr varchar(32)  null,"
                        + "saveTime datetime  null,"

                        + "briefly varchar(2048)  null,"
                        + "trend double  null," // 振幅
                        + "remark longtext  null,"
                        + "lastModified datetime  null,"
                        + "marked boolean  null,"

                        + "INDEX dateStr_index (dateStr ASC)\n"

                        + "\n)"
                , tableName);
    }


    /**
     * 查找 财经导读 最新520条记录, 尝试抓取里面的 公司重大事项.
     *
     * @return
     * @throws Exception
     */
    protected HashSet<SimpleNewEm> initLastTimeFetchSaveBeansExpect500() throws Exception {
        HashSet<SimpleNewEm> simpleNewEms = new HashSet<>(
                EastMoneyDbApi.getLatestSaveBeanByType(SimpleNewEm.CAI_JING_DAO_DU_TYPE, 520));
        simpleNewEms.addAll(EastMoneyDbApi.getLatestSaveBeanByType(SimpleNewEm.ZI_XUN_JINH_HUA_TYPE, 520));
        // @key: 两种都找
        return simpleNewEms;
    }
}
