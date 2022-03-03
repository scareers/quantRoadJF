package com.scareers.datasource.jsl;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.scareers.utils.JSONUtilS;
import com.scareers.utils.log.LogUtil;
import joinery.DataFrame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.scareers.utils.CommonUtil.waitUtil;

/**
 * description: 集思录可转债相关api.
 * 登录逻辑: 访问登录api,拿到cookie后, 以此为身份凭证访问其他.
 * 因考虑到登录过期问题. 当拿到cookie后, 放入 过期Map, 每个api均从Map获取身份凭证.
 * 当身份凭证过期(自定义时间, 模拟关闭浏览器等)后, 当再次访问登录api
 *
 * @key3 实测sessionId 或 cookie的访问api, 需要在非主线程执行, 等待设置好值后, 访问数据API才可成功. 否则视为登录不成功
 * --> getLoginInfo() 异步登录, 不可同步
 * @noti 需要提供摘要算法后的用户名和密码
 * @noti 登录时使用的其他(非用户信息)cookie, 需要和访问数据api时使用的cookie匹配!!! *********
 * @author: admin
 * @date: 2022/3/2/002-22:58:35
 * @see
 */
public class JslBondApi {

    public static final String username = "66d227d0ce9040f7256469b5a7cbb846";
    public static final String password = "7285fd1d53396f996b332e09589d3d05";
    public static String sessionId; // 保存回话id
    public static final LRUCache<String, String> loginInfoCache = CacheUtil.newLRUCache(1, 24 * 3600 * 1000);
    // 保存用户登录cookie; 1天过期,默认7天

    public static void main(String[] args) {

        Console.log(getBondsDataList());
        Console.log(getBondsDataList());
        Console.log(getBondsDataList());
        Console.log(getBondsDataList());
    }

    private static final Log log = LogUtil.getLogger();

    /**
     * 需要先访问首页, 拿到 session_id,
     * 再用 session_id, 登录, 拿到用户信息
     */
    private static void accessHomePage() {
        String url = "https://www.jisilu.cn/";


        HashMap<String, String> headers = new HashMap<>();
        headers.put("host", "www.jisilu.cn");
        headers.put("cache-control", "max-age=0");
        headers.put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("upgrade-insecure-requests", "1");
        headers.put("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");

        headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp," +
                "image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        headers.put("sec-fetch-site", "none");
        headers.put("sec-fetch-mode", "navigate");
        headers.put("sec-fetch-user", "?1");
        headers.put("sec-fetch-dest", "document");
        headers.put("accept-encoding", "gzip");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("cookie",
                "kbz_newcookie=1; kbzw_r_uname=scareers; Hm_lvt_164fe01b1433a19b507595a43bf58262=1646224708,1646233496,1646238629,1646240712"
        );
        headers.put("Connection", "keep-alive");

        HttpRequest request = HttpUtil.createPost(url);
        for (String s : headers.keySet()) {
            request = request.header(s, headers.get(s));
        }

        HttpResponse response = request.execute();

        List<String> newCookies = response.headerList("Set-Cookie");
        // kbzw__Session=91sshmrkebfot94cr8be6vqsj1; path=/
        for (String newCookie : newCookies) {
            if (newCookie.startsWith("kbzw__Session")) {
                String info = newCookie.substring(newCookie.indexOf("=") + 1);
                info = info.substring(0, info.indexOf("; "));
                if (info.length() > 0) {
                    sessionId = info;

                    log.info("show: 集思录首页访问成功...");
                    return;
                }
            }
        }
    }

    /**
     * 实际登录逻辑, 将访问登录api, 后将登录信息放入 loginInfoCache
     */
    private static void loginActual() {
        accessHomePage();

        String url = "https://www.jisilu.cn/account/ajax/login_process/";
        HttpRequest request = HttpUtil.createPost(url);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("host", "www.jisilu.cn");
        headers.put("content-length", "185");
        headers.put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"");
        headers.put("accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("x-requested-with", "XMLHttpRequest");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("origin", "https://www.jisilu.cn");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-dest", "empty");
        headers.put("referer", "https://www.jisilu.cn/account/login/");
        headers.put("accept-encoding", "gzip");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("cookie",
                StrUtil.format("bz_newcookie=1; kbzw_r_uname=scareers; kbzw__Session={}; " +
                        "Hm_lvt_164fe01b1433a19b507595a43bf58262=1646224606,1646224708,1646233496,1646238629; " +
                        "Hm_lpvt_164fe01b1433a19b507595a43bf58262=1646239208", sessionId)
        );
        headers.put("Connection", "close");

        for (String s : headers.keySet()) {
            request = request.header(s, headers.get(s));
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("return_url", "https://www.jisilu.cn/");
        params.put("user_name", username);
        params.put("password", password);
        params.put("net_auto_login", "1");
        params.put("agreement_chk", "agree");
        params.put("_post_type", "ajax");
        params.put("aes", "1");
        request = request.form(params);


        HttpResponse response = request.execute();

        List<String> newCookies = response.headerList("Set-Cookie");
        for (String newCookie : newCookies) {
            if (newCookie.startsWith("kbzw__user_login")) {
                String info = newCookie.substring(newCookie.indexOf("=") + 1);
                info = info.substring(0, info.indexOf("; "));
                if (info.length() > 0) {
                    loginInfoCache.put("user_cookie", info);
                    ThreadUtil.sleep(1000);
                    log.info("show: 集思录登录成功...");
                    return;
                }
            }
        }
        log.error("集思录登录失败...");
    }

    private static String getLoginInfo() {
        while (true) {
            String res = loginInfoCache.get("user_cookie"); // 默认7天, 自定义为1天
            if (res != null) {
                return res;
            } else {
                ThreadUtil.execAsync(new Runnable() {
                    @Override
                    public void run() {
                        loginActual();
                    }
                }, false);

                try {
                    waitUtil(() -> loginInfoCache.get("user_cookie") != null, 3000, 10, null, false);
                } catch (TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /**
     * 核心api: 获取可转债列表信息.
     *
     * @return
     */
    private static List<String> bondDataFields = StrUtil
            .split("bond_id,bond_nm,bond_py,price,increase_rt,stock_id,stock_nm," +
                            "stock_py,sprice,sincrease_rt,pb,convert_price,convert_value,convert_dt,premium_rt,dblow,adjust_condition,sw_cd,market_cd,btype,list_dt,qflag2,owned,hold,bond_value,rating_cd,option_value,put_convert_price,force_redeem_price,convert_amt_ratio,fund_rt,short_maturity_dt,year_left,curr_iss_amt,volume,svolume,turnover_rt,ytm_rt,put_ytm_rt,notes,noted,bond_nm_tip,redeem_icon,last_time,qstatus,margin_flg,sqflag,pb_flag,adj_cnt,adj_scnt,convert_price_valid,convert_price_tips,convert_cd_tip,ref_yield_info,adjusted,orig_iss_amt,price_tips,redeem_dt,real_force_redeem_price,option_tip"
                    , ",");

    public static DataFrame<Object> getBondsDataList() {
        String loginInfo = getLoginInfo();

        String url = "https://www.jisilu.cn/webapi/cb/list_new/";
        HashMap<String, String> headers = new HashMap<>();
        headers.put("host", "www.jisilu.cn");
        headers.put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\"");
        headers.put("accept", "application/json, text/plain, */*");
        headers.put("init", "1");
        headers.put("columns", "1,70,2,3,5,6,11,12,14,15,16,29,30,32,34,35,44,46,47,52,53,54,56,57,58,59,60,62,63,67");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-dest", "empty");
        headers.put("referer", "https://www.jisilu.cn/web/data/cb/list");
        headers.put("accept-encoding", "gzip");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        String cookieStr = "kbz_newcookie=1; kbzw_r_uname=scareers; " +
                "Hm_lvt_164fe01b1433a19b507595a43bf58262=1646224708,1646233496,1646238629,1646240712; " +
                "kbzw__user_login=" + loginInfo +
                "; kbzw__Session=" + sessionId;
        headers.put("cookie", cookieStr);
        headers.put("if-modified-since", "Wed, 02 Mar 2022 16:02:48 GMT");
        headers.put("Connection", "close");

        HttpRequest request = HttpUtil.createGet(url);

        for (String s : headers.keySet()) {
            request = request.header(s, headers.get(s));
        }

        HttpResponse response = request.execute();

        DataFrame<Object> dataFrame = JSONUtilS
                .jsonStrToDf(response.body(), null, null, bondDataFields, Arrays.asList("data"), JSONObject.class,
                        Arrays.asList(), Arrays.asList());
        return dataFrame;
    }

}
