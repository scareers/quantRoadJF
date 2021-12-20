package com.scareers.utils.log;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.log4j.Log4jLogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.management.ManagementFactory;

/**
 * description:
 *
 * @author: admin
 * @date: 2021/12/20/020-16:15
 */
public class LogUtils {
    public static Log log = getLogger();

    public static void main(String[] args) {
        // 常态四级即可
        log.debug("xxx");
        log.info("xxx");
        log.warn("xxx");
        log.error("xxx");
    }

    public static Log getLogger(Class clazz) {
        LogFactory.setCurrentLogFactory(new Log4jLogFactory());
        // 使用 log4j实现
        return LogFactory.get(clazz);
    }

    public static Log getLogger() {
        LogFactory.setCurrentLogFactory(new Log4jLogFactory());
        return LogFactory.get();
    }

    public static Log getLogger(String name) {
        LogFactory.setCurrentLogFactory(new Log4jLogFactory());
        return LogFactory.get(name);
    }
}
