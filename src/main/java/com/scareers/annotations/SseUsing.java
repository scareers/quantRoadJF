package com.scareers.annotations;

import java.lang.annotation.*;

/**
 * 仅仅标记方法为使用 SSE (server send event) 推送技术
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface SseUsing {
    String description() default "---";

    String callbackType() default "---"; // 对应SseCallback 的参数的类型.
}
