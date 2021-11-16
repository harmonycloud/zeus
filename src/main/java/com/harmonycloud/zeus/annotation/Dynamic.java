package com.harmonycloud.zeus.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author xutianhong
 * @Date 2021/11/12 11:31 上午
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface Dynamic {
}
