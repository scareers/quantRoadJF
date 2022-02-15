package com.scareers.utils.log;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.dialect.log4j.Log4jLogFactory;
import cn.hutool.log.dialect.slf4j.Slf4jLogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.management.ManagementFactory;

/**
 * description: 日志对象. 常规使用 hutool搭配log4j;
 * 其他某些第三方库可能使用slf4j, 一般搭配 logback框架.
 * 因此有 log4j 以及 logback 两种配置, 目标不同
 *
 * @author: admin
 * @date: 2021/12/20/020-16:15
 */
public class LogUtil {
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
        LogFactory.setCurrentLogFactory(new Slf4jLogFactory());
        return LogFactory.get();
    }

    public static Log getLogger(String name) {
        LogFactory.setCurrentLogFactory(new Log4jLogFactory());
        return LogFactory.get(name);
    }


}
