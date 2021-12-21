package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolFactory;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.utils.StrUtil;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.*;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class StockApi {
    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) {
        List<StockBean> stocks = new StockPoolForFSTransaction().createStockPool();
        for (StockBean stock : stocks) {
            Console.log(getFSTransaction(100, stock.getStockCodeSimple(), stock.getMarket()));
        }

    }

    /**
     * dfcf, 获取分时成交api.
     * <p>
     * // @deprecated
     * String keyUrlTemplate = "https://push2ex.eastmoney.com/getStockFenShi" +
     * "?pagesize={}" +  // 单页数量, 调整至5000获取所有. 标准 240分钟*(60/3) 最多 4800条
     * "&ut=7eea3edcaed734bea9cbfc24409ed989&dpt=wzfscj" +
     * "&cb=jQuery112405998333817754311_{}" + // 时间戳1, 毫秒
     * "&pageindex=0" +  // 为了方便, 此参数不变. 均访问第0页
     * "&id={}1" + // code+1
     * "&sort={}&ft=1" + // 升序
     * "&code={}" + // code2
     * "&market={}" +  // 市场代码, 0深1沪
     * "&_={}"; // 时间戳2
     * <p>
     * // @using
     * https://push2.eastmoney.com/api/qt/stock/details/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1400&secid=0.000153&cb=jQuery112409885675811656662_1640099646776&_=1640099646777
     * // @noti: 升序, 但是最新 x 条数据
     *
     * @param lastRecordAmounts 单页数量,
     * @param stockCodeSimple   股票/指数简单代码, 不再赘述
     * @param market            0 深市  1 沪市    (上交所暂时 0)
     * @return
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market) {
        DataFrame<Object> res = new DataFrame<>(
                Arrays.asList("stock_code", "market", "time_tick", "price", "vol", "bs"));
        String keyUrlTemplate = "https://push2.eastmoney.com/api/qt/stock/details/get" +
                "?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55" +
                "&pos=-{}" +
                "&secid={}.{}" +
                "&cb=jQuery112409885675811656662_{}" +
                "&_={}";

        String fullUrl = StrUtil.format(keyUrlTemplate,
                lastRecordAmounts,
                market,
                stockCodeSimple,
                System.currentTimeMillis() - RandomUtil.randomInt(1000),
                System.currentTimeMillis()
        );
        String response = getAsStr(fullUrl);
        response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
        JSONObject responseJson = JSONUtil.parseObj(response);
        JSONObject data = null;
        try {
            data = (JSONObject) responseJson.get("data");
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("get exception: 获取数据错误.常因 data字段为null");
            return res;
        }
        log.info("获取json成功: {}.{}", market, stockCodeSimple);
        JSONArray dataCore = data.getJSONArray("details");
        Console.log(dataCore);
        for (Object o : dataCore) {
            String line = o.toString();
            List<String> values = StrUtil.split(line, ",");
            values.remove(3); // 删除倒数第二字段
            List<Object> row = new ArrayList<>(Arrays.asList(stockCodeSimple, market));
            row.addAll(values);
            res.append(row);
        }
        return res;
    }


}
