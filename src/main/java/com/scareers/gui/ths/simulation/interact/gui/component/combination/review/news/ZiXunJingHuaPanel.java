package com.scareers.gui.ths.simulation.interact.gui.component.combination.review.news;

import cn.hutool.core.lang.Console;

/**
 * description:
 *
 * @author: admin
 * @date: 2022/3/13/013-08:57:08
 */
public class ZiXunJingHuaPanel extends SimpleNewListPanel {
    public static ZiXunJingHuaPanel INSTANCE;

    public static ZiXunJingHuaPanel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZiXunJingHuaPanel();
        }
        return INSTANCE;
    }
    @Override
    protected void update() {
        Console.log("资讯精华被电击");
    }
}
