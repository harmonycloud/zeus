package com.middleware.zeus.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author liyinlong
 * @since 2021/11/18 5:18 下午
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface MiddlewareBackup {

}
