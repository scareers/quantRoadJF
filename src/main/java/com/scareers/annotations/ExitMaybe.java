package com.scareers.annotations;

import java.lang.annotation.*;

/**
 * 仅仅标记方法为: 可能导致程序强退
 * @author admin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
@Documented
public @interface ExitMaybe {

}
