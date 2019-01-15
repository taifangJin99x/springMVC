package com.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    /**
     * 表示访问该类注入的名字
     * @return
     */
    String value() default "";
}
