package com.harmonycloud.zeus.annotation;

import java.lang.annotation.*;

/**
 * @author xutianhong
 * @Date 2022/3/28 2:57 下午
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authority {

    String resource() default "";

    String operator() default "";

    int power() default 0;

}
