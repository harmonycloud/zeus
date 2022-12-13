package com.middleware.zeus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.middleware.caas.common.model.middleware.Middleware;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface Operator {

    Class<?>[] paramTypes4One() default {Middleware.class};

    Class<?>[] paramTypes4Many() default {};

}
