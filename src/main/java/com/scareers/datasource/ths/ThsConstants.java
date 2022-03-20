package com.scareers.datasource.ths;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * description: 同花顺常量
 * 典型的解析js而获得 v_code.
 *
 * @author: admin
 * @date: 2022/3/20/020-17:07:49
 */
public class ThsConstants {
    public static void main(String[] args) {
        Console.log(getNewVCode());

    }


    private static Invocable inv;

    public static String getNewVCode() {
        synchronized (ThsConstants.class) { //同步
            while (inv == null) { // 死循环初始化执行器
                try {
                    initInv();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            return inv.invokeFunction("v").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void initInv() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("js"); // 得到脚本引擎

        String str = ResourceUtil.readUtf8Str("ths/wencai/ths.js"); // 将会自动查找类路径下; 别绝对路径
        engine.eval(str);

        inv = (Invocable) engine;

    }
}
