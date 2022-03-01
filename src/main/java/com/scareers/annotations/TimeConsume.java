package com.scareers.annotations;

import java.lang.annotation.*;

/**
 * 仅仅标记方法为: 执行可能比较消耗时间
 *
 * @author admin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface TimeConsume {
    String description() default "---";
}
