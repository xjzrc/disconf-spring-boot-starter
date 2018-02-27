package com.zen.disconf.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * Disconf属性配置注解
 *
 * @author xinjingziranchan@gmail.com
 * @version 1.0.0
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisconfConfigAnnotation {

    String springBootName();

    String disconfName();

    String defaultValue() default "";
}
