package com.scareers.datasource.eastmoney.stock;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.scareers.datasource.eastmoney.fstransaction.StockBean;
import com.scareers.datasource.eastmoney.fstransaction.StockPoolForFSTransaction;
import com.scareers.utils.log.LogUtils;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.scareers.datasource.eastmoney.EastMoneyUtils.getAsStr;
import static com.scareers.datasource.eastmoney.SettingsOfEastMoney.DEFAULT_TIMEOUT;
import static com.scareers.utils.JsonUtil.jsonStrToDf;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/21/021-22:10:19
 */
public class StockApi {
    private static final Log log = LogUtils.getLogger();

    public static void main(String[] args) throws Exception {
        List<StockBean> stocks = new StockPoolForFSTransaction().createStockPool();
        for (StockBean stock : stocks) {
            Console.log(getFSTransaction(100, stock.getStockCodeSimple(), stock.getMarket()));
        }
    }

    /**
     * 获取分时成交
     * // @using
     * https://push2.eastmoney.com/api/qt/stock/details/get?ut=fa5fd1943c7b386f172d6893dbfba10b&fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55&pos=-1400&secid=0.000153&cb=jQuery112409885675811656662_1640099646776&_=1640099646777
     * // @noti: 升序, 但是最新 x 条数据
     *
     * @param lastRecordAmounts 单页数量,
     * @param stockCodeSimple   股票/指数简单代码, 不再赘述
     * @param market            0 深市  1 沪市    (上交所暂时 0)
     * @return 出错则返回空df, 不抛出异常
     */
    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market, int timeout) {
        List<String> columns = Arrays.asList("stock_code", "market", "time_tick", "price", "vol", "bs");
        DataFrame<Object> res = new DataFrame<>(columns);
        String keyUrl = "https://push2.eastmoney.com/api/qt/stock/details/get";
        String response;
        try {
            Map<String, Object> params = new HashMap<>(); // 参数map
            params.put("ut", "fa5fd1943c7b386f172d6893dbfba10b");
            params.put("fields1", "f1,f2,f3,f4");
            params.put("fields2", "f51,f52,f53,f54,f55");
            params.put("pos", StrUtil.format("-{}", lastRecordAmounts));
            params.put("secid", StrUtil.format("{}.{}", market, stockCodeSimple));
            params.put("cb", StrUtil.format("jQuery112409885675811656662_{}",
                    System.currentTimeMillis() - RandomUtil.randomInt(1000)));
            params.put("_", System.currentTimeMillis());

            response = getAsStr(keyUrl, params, timeout);
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("get exception: 访问http失败: stock: {}.{}", market, stockCodeSimple);
            return res;
        }

        try {
            res = jsonStrToDf(response, "(", ")", columns,
                    Arrays.asList("data", "details"), String.class, Arrays.asList(3),
                    Arrays.asList(stockCodeSimple, market));
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("get exception: 获取数据错误. stock: {}.{}", market, stockCodeSimple);
            log.warn("raw data: 原始响应字符串: {}", response);
        }
        return res;
    }


    public static DataFrame<Object> getFSTransaction(Integer lastRecordAmounts, String stockCodeSimple,
                                                     Integer market) {
        return getFSTransaction(lastRecordAmounts, stockCodeSimple, market, DEFAULT_TIMEOUT);
    }


}
