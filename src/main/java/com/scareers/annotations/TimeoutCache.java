package com.scareers.annotations;

import java.lang.annotation.*;

/**
 * @author admin
 * 仅仅标记使用了带过期时间的缓存
 */
@Retention(RetentionPolicy.RUNTIME)
//@Target注解指定注解能修饰的目标(只能是方法)
@Target(ElementType.METHOD)
@Documented
public @interface TimeoutCache {
    String description() default "---";

    String timeout() default ""; // 过期时间ms

    String notes() default "this annotation means the method annotated is using Cache from hutool";
}
