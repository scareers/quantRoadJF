package com.scareers.utils.log;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/20/020-16:20
 */
public class ColorPatternLayout extends PatternLayout {

    /**
     * none         = "\033[0m"
     * black        = "\033[0;30m"
     * dark_gray    = "\033[1;30m"
     * red          = "\033[0;31m"
     * light_red    = "\033[1;31m"
     * green        = "\033[0;32m"
     * light_green -= "\033[1;32m"
     * brown        = "\033[0;33m"
     * yellow       = "\033[1;33m"
     * blue         = "\033[0;34m"
     * light_blue   = "\033[1;34m"
     * purple       = "\033[0;35m"
     * light_purple = "\033[1;35m"
     * cyan         = "\033[0;36m"
     * light_cyan   = "\033[1;36m"
     * light_gray   = "\033[0;37m"
     * white        = "\033[1;37m"
     * <p>
     * 31: 红
     * 32: 绿
     * 33: 黄
     * 34: 蓝
     * 35: 紫
     * 36: 深绿
     * 37: 白色
     *
     * @param event
     * @return
     */
    @Override
    public String format(LoggingEvent event) {
        Level level = event.getLevel();
        String prefix = "\033[33m";
        String suffix = "\033[0m";
        /*
          public final static int OFF_INT = Integer.MAX_VALUE;
          public final static int FATAL_INT = 50000;
          public final static int ERROR_INT = 40000;
          public final static int WARN_INT  = 30000;
          public final static int INFO_INT  = 20000;
          public final static int DEBUG_INT = 10000;
            //public final static int FINE_INT = DEBUG_INT;
          public final static int ALL_INT = Integer.MIN_VALUE;
         */

        switch (level.toInt()) {
            case Level.DEBUG_INT:
                prefix = ""; // 默认无附加颜色
                break;
            case Level.INFO_INT:
                prefix = "\033[35m";
                break;
            case Level.WARN_INT:
                prefix = "\033[1:33m";
                break;
            case Level.ERROR_INT:
                prefix = "\033[31m";
                break;
            default:
                prefix = "\033[30m";
        }
        return prefix + super.format(event) + suffix;
    }
}
