package com.middleware.zeus.util;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtils.applicationContext = applicationContext;
    }

    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        return applicationContext.getBeansWithAnnotation(annotation);
    }

    public static Object getBean(String beanId) throws BeansException {
        return applicationContext.getBean(beanId);
    }

    public static <T> T getBeanIgnoreNotFound(Class<T> clazz) {
        T result = null;
        try {
            result = applicationContext.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {

        }
        return result;
    }

    public static String getProperty(String path) {
        return applicationContext.getEnvironment().getProperty(path);
    }

    public static String getProperty(String path, String defaultConfig) {
        String config = applicationContext.getEnvironment().getProperty(path);
        return config == null ? defaultConfig : config;
    }
}
