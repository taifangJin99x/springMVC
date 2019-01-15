package com.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    /**
     * 表示访问该类注入的名字
     * @return
     */
    String value() default "";
}
