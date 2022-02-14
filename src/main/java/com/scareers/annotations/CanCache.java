package com.scareers.annotations;

import java.lang.annotation.*;

/**
 * @author admin
 * 仅仅标记 可传递参数,决定是否使用缓存.
 */
@Retention(RetentionPolicy.RUNTIME)
//@Target注解指定注解能修饰的目标(只能是方法)
@Target(ElementType.METHOD)
@Documented
public @interface CanCache {
    String description() default "---";

    String notes() default "this annotation means the method annotated is using Cache from hutool";
}
