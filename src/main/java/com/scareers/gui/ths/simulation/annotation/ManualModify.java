package com.scareers.gui.ths.simulation.annotation;

import java.lang.annotation.*;

/**
 * trader相关, 仅标记 配置值带有默认值, 并可以手动修改
 *
 * @author admin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface ManualModify {

}
