package com.harmonycloud.zeus.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @description 根据Bean名称或Class获取Bean
 * @author  liyinlong
 * @since 2022/6/20 2:58 下午
 */
@Component
public class ApplicationContextGetBeanHelper implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public static Object getBean(String className) throws BeansException, IllegalArgumentException {
        if (className == null || className.length() <= 0) {
            throw new IllegalArgumentException("className为空");
        }

        String beanName;
        if (className.length() > 1) {
            beanName = className.substring(0, 1).toLowerCase() + className.substring(1);
        } else {
            beanName = className.toLowerCase();
        }
        return applicationContext != null ? applicationContext.getBean(beanName) : null;
    }

    public static <T> T getBean(Class<T> beanClass) throws BeansException, IllegalArgumentException {
        if (beanClass == null) {
            throw new IllegalArgumentException("beanClass为空");
        }
        return applicationContext != null ? applicationContext.getBean(beanClass) : null;
    }

}
