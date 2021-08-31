package com.harmonycloud.zeus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 不必要的操作审计方法
 * @author liyinlong
 * @date 2021/7/29 3:41 下午
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeAuditMethod {
    String notes() default "";
}
